from pathlib import Path
import logging

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
        safety = model_safety.predict([text])[0]
        # confidence 는 '위험도 결정'의 확신도만 쓴다. 종류모델 확률과 섞지 않음.
        safety_conf = float(max(model_safety.predict_proba([text])[0]))
        return Res(disasterType=str(disaster), safety=str(safety), confidence=safety_conf)
    except Exception as e:
        log.error("추론 오류: %s", e)
        # 추론이 깨져도 서버는 죽지 않게 안전 기본값 (자바도 SAFE 폴백과 일치)
        return Res(disasterType="UNKNOWN", safety="SAFE", confidence=0.0)


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
