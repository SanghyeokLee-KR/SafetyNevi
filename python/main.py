from pathlib import Path
import logging
import os

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import joblib
import uvicorn

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("safetynevi-ai")

app = FastAPI(title="SafetyNevi AI")

# 모델은 이 파일과 같은 폴더에서 절대경로로 로드한다 (어느 CWD에서 켜도 되게).
# 못 읽어도 서버는 뜨되 not-ready 상태로 두고, /predict 는 503 (자바 쪽이 타임아웃으로 폴백함).
BASE_DIR = Path(__file__).resolve().parent
# 위험(DANGER) 판정 임계값. 기본 0.5. 과경보가 신경 쓰이면 운영에서 올려(예: 0.7) precision↑/recall↓.
# train.py 가 임계값별 precision/recall 을 출력하니 데이터 보고 고르면 된다.
DANGER_THRESHOLD = float(os.getenv("SAFETY_DANGER_THRESHOLD", "0.5"))
model_disaster = None
model_safety = None


def load_models():
    global model_disaster, model_safety
    try:
        model_disaster = joblib.load(BASE_DIR / "model_disaster.pkl")
        model_safety = joblib.load(BASE_DIR / "model_safety.pkl")
        log.info("모델 로드 완료")
    except Exception as e:
        model_disaster = None
        model_safety = None
        log.error("모델 로드 실패 (학습부터 하세요): %s", e)


load_models()


class Req(BaseModel):
    text: str


class Res(BaseModel):
    disasterType: str
    safety: str        # DANGER | SAFE
    confidence: float  # 위험도 판단(safety)에 대한 확신도


@app.get("/health")
def health():
    ready = model_disaster is not None and model_safety is not None
    return JSONResponse(status_code=200 if ready else 503,
                        content={"status": "UP" if ready else "DOWN"})


@app.post("/predict", response_model=Res)
def predict(req: Req):
    if model_disaster is None or model_safety is None:
        raise HTTPException(status_code=503, detail="model not loaded")

    text = (req.text or "").strip()
    if not text:
        return Res(disasterType="UNKNOWN", safety="SAFE", confidence=0.0)

    try:
        disaster = model_disaster.predict([text])[0]
        # 위험도는 argmax(predict) 대신 DANGER 확률에 임계값을 적용한다 — 임계값을 올리면 과경보↓.
        proba = model_safety.predict_proba([text])[0]
        classes = list(model_safety.classes_)
        p_danger = float(proba[classes.index("DANGER")]) if "DANGER" in classes else 0.0
        safety = "DANGER" if p_danger >= DANGER_THRESHOLD else "SAFE"
        return Res(disasterType=str(disaster), safety=safety, confidence=p_danger)
    except Exception as e:
        log.error("추론 오류: %s", e)
        # 추론이 깨져도 서버는 죽지 않게 안전 기본값 (자바도 SAFE 폴백과 일치)
        return Res(disasterType="UNKNOWN", safety="SAFE", confidence=0.0)


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
