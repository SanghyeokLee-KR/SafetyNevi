from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import uvicorn

# FastAPI 앱 생성
app = FastAPI()

# 저장된 모델 로드 (같은 폴더에 pkl 파일이 있어야 합니다)
print("Loading Models...")
model_disaster = joblib.load("model_disaster.pkl")
model_safety = joblib.load("model_safety.pkl")
print("Models Loaded!")

# 요청 데이터 구조 정의
class Req(BaseModel):
    text: str

# 응답 데이터 구조 정의
class Res(BaseModel):
    disasterType: str
    safety: str
    confidence: float

@app.post("/predict", response_model=Res)
def predict(req: Req):
    text = req.text

    # 1) 재난 종류 분류
    disaster = model_disaster.predict([text])[0]
    # 확률 계산 (가장 높은 확률값)
    proba1 = max(model_disaster.predict_proba([text])[0])

    # 2) 위험도 분류
    safety = model_safety.predict([text])[0]
    # 확률 계산
    proba2 = max(model_safety.predict_proba([text])[0])

    return Res(
        disasterType=disaster,
        safety=safety,
        confidence=float(max(proba1, proba2))
    )

# 이 파일이 직접 실행될 때만 서버를 켭니다.
if __name__ == "__main__":
    # host="0.0.0.0"은 외부 접속 허용, port=8000번 사용
    uvicorn.run(app, host="0.0.0.0", port=8000)