import re
import oracledb
import pandas as pd
import joblib
import random
from collections import Counter
from datetime import datetime

# 시각화용 라이브러리
import matplotlib.pyplot as plt
from wordcloud import WordCloud

# 머신러닝 (사이킷런)
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
from sklearn.utils import resample

# ==========================================
# 1. 환경 설정
# ==========================================
# DB 접속 정보 (로컬 개발용)
DB_USER = ""
DB_PASSWORD = ""
DB_DSN = ""

# 한 지역 데이터가 너무 많으면 편향되니까 최대 개수 제한
MAX_PER_AREA = 300
SEED = 42
random.seed(SEED)

# 윈도우 한글 폰트 설정 (이거 안하면 그래프 글자 깨짐)
FONT_PATH = "C:/Windows/Fonts/malgun.ttf"
plt.rc('font', family='Malgun Gothic')
plt.rcParams['axes.unicode_minus'] = False


# ==========================================
# 2. DB 연결 유틸
# ==========================================
def get_connection():
    return oracledb.connect(user=DB_USER, password=DB_PASSWORD, dsn=DB_DSN)


# ==========================================
# 3. 데이터 가져오기 (DM 테이블)
# ==========================================
def fetch_data_from_db():
    print("📡 DB에서 데이터 긁어오는 중...")
    conn = get_connection()

    # 오라클 CLOB 타입은 그냥 읽으면 에러나서 String으로 변환해줘야 함
    def output_type_handler(cursor, name, default_type, size, precision, scale):
        if default_type == oracledb.CLOB:
            return cursor.var(oracledb.STRING, arraysize=cursor.arraysize)

    conn.outputtypehandler = output_type_handler

    # 필요한 컬럼만 조회 (EMERGENCY_LEVEL = 공식 긴급단계, 위험도 정답으로 씀)
    query = "SELECT DMID, CONTENT, DISASTERTYPE, EMERGENCY_LEVEL, AREA FROM DM"
    df = pd.read_sql(query, conn)
    conn.close()

    # 컬럼명 다 대문자로 맞춰줌 (나중에 헷갈리지 않게)
    df.columns = [c.upper() for c in df.columns]
    print(f"📦 총 {len(df)}건 로드 완료.")
    return df


# ==========================================
# 4. 위험도 라벨 (공식 긴급단계 기반)
# ==========================================
# 예전엔 본문 키워드로 라벨을 찍었는데, 그러면 모델이 그 키워드 규칙을 그대로 따라하는
# 순환 구조라 의미가 없었다. 그래서 정부 공식 긴급단계(EMRG_STEP_NM: 위급/긴급/안전안내)를 정답으로 쓴다.
# 라벨 출처가 '본문'이 아니라 '공식 단계 필드'라 비순환 → text -> 공식판정 을 배우는 진짜 지도학습.
# (출력은 DANGER/SAFE 로 둬서 FastAPI·자바 쪽 계약과 맞춤)
def to_risk(emergency_level):
    s = str(emergency_level)
    return "DANGER" if ("위급" in s or "긴급" in s) else "SAFE"


# ==========================================
# 5. 텍스트 전처리 (노이즈 제거)
# ==========================================
def clean_text(text):
    text = str(text)

    # 날짜, 시간 같은건 패턴 분석에 방해되니까 제거
    text = re.sub(r"\d{1,2}시|\d{1,2}분|\d{1,2}초", " ", text)
    text = re.sub(r"\d{2,4}년|\d{1,2}월|\d{1,2}일", " ", text)

    # 특수문자랑 괄호도 다 날림
    text = re.sub(r"[\[\]\(\)<>\"']", " ", text)
    text = re.sub(r"[^가-힣0-9a-zA-Z\s]", " ", text)

    # 공백 여러개는 하나로
    text = re.sub(r"\s+", " ", text).strip()
    return text


def build_area_patterns(area_series):
    # 지역명(예: 서울, 부산)을 텍스트에서 지우기 위해 패턴 미리 만듦
    # 지역명 때문에 AI가 편향되는걸 막으려고 함
    areas = area_series.dropna().astype(str).unique().tolist()
    tokens = set()

    for a in areas:
        for part in re.split(r'[,/]\s*', a):
            part = part.strip()
            if len(part) > 1:
                tokens.add(re.escape(part))

    token_list = sorted(tokens, key=lambda x: len(x), reverse=True)
    patterns = [re.compile(t) for t in token_list]
    return patterns


def remove_area_mentions(text, area_patterns):
    # 본문에서 지역명 삭제하는 함수
    s = str(text)
    for p in area_patterns:
        s = p.sub(" ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


# ==========================================
# 6. 학습용 데이터셋 만들기
# ==========================================
def prepare_training_dataframe(df, max_per_area=MAX_PER_AREA):
    print("🛠️ 학습 데이터 가공 중... (지역명 제거 등)")
    df.columns = [c.lower() for c in df.columns]

    # 텍스트 청소
    df['content'] = df['content'].astype(str).apply(clean_text)
    df['disastertype'] = df['disastertype'].astype(str)

    # 위험도 라벨 = 공식 긴급단계 (본문 키워드가 아니라 정부 분류 → 순환 안 됨)
    # 주의: 긴급단계 비어있는(옛날 크롤링) 행은 SAFE로 떨어지니, 새 크롤링으로 다시 채운 뒤 재학습할 것
    df['risk_label'] = df['emergency_level'].apply(to_risk)

    # 지역명 패턴 추출
    area_patterns = build_area_patterns(df['area'])

    rows = []
    for _, r in df.iterrows():
        content = r['content']
        dtype = r['disastertype']
        area = r['area'] if pd.notna(r['area']) else "UNKNOWN"
        risk = r['risk_label']

        # 1. 원본 데이터 추가
        rows.append({
            'content': content,
            'disastertype': dtype,
            'risk_label': risk,
            'area': area,
            'version': 'original'
        })

        # 2. 지역명 지운 버전도 추가 (데이터 증강 효과 + 편향 방지)
        anon = remove_area_mentions(content, area_patterns)
        # 지진, 강풍 등은 지역명이 중요할 수도 있어서 제외하고 나머지만
        if anon.strip() and dtype not in ["지진", "강풍", "대설", "산불", "수도"]:
            rows.append({
                'content': anon,
                'disastertype': dtype,
                'risk_label': risk,
                'area': area,
                'version': 'anon'
            })

    # 데이터 셔플
    df_all = pd.DataFrame(rows).sample(frac=1, random_state=SEED).reset_index(drop=True)

    # 특정 지역 데이터가 너무 많으면 좀 줄임 (Downsampling)
    final = []
    area_counts = Counter()

    for _, r in df_all.iterrows():
        area = r['area']
        if area_counts[area] < max_per_area:
            final.append(r)
            area_counts[area] += 1

    df_final = pd.DataFrame(final)
    print(f"✅ 최종 학습 데이터: {len(df_final)}건 확보")

    return df_final


# ==========================================
# 7. 시각화 (확인용)
# ==========================================
def visualize_data(df):
    print("\n📊 데이터 분포 시각화 생성 중...")

    # 재난 종류별 분포 그래프
    plt.figure(figsize=(10, 5))
    df['disastertype'].value_counts().plot(kind='bar')
    plt.title("재난 종류 분포")
    plt.tight_layout()
    plt.savefig("class_distribution.png")
    plt.close()

    # 위험도 분포 그래프
    plt.figure(figsize=(6, 4))
    df['risk_label'].value_counts().plot(kind='bar')
    plt.title("위험도(위험/안전) 분포")
    plt.tight_layout()
    plt.savefig("risk_distribution.png")
    plt.close()

    # 워드클라우드 (자주 나오는 단어 확인)
    text = " ".join(df['content'].tolist())
    try:
        wc = WordCloud(font_path=FONT_PATH, width=800, height=400, background_color="white")
        wc.generate(text)
        wc.to_file("wordcloud.png")
    except:
        print("⚠️ 워드클라우드 생성 실패 (폰트 문제일 수 있음)")

    print("📁 이미지 저장 완료 (class_distribution.png 등)")


# ==========================================
# 8. 모델 학습 및 저장 (핵심)
# ==========================================
def train_and_save_models(df):
    print("\n🚀 AI 모델 학습 시작...")

    # 1차 모델: 재난 종류 분류 (Naive Bayes 사용)
    # TF-IDF로 텍스트를 벡터로 바꾸고 분류기에 넣음
    pipeline1 = Pipeline([
        ('tfidf', TfidfVectorizer(max_features=5000)),
        ('clf', MultinomialNB())
    ])

    print("   -> 재난 종류 모델 학습 중...")
    pipeline1.fit(df['content'], df['disastertype'])
    # ★ 저장 파일명 수정 (server.py랑 맞춤)
    joblib.dump(pipeline1, "model_disaster.pkl")

    # 2차 모델: 위험/안전 판별
    pipeline2 = Pipeline([
        ('tfidf', TfidfVectorizer(max_features=3000)),
        ('clf', MultinomialNB())
    ])

    print("   -> 위험도 판단 모델 학습 중...")
    pipeline2.fit(df['content'], df['risk_label'])
    # ★ 저장 파일명 수정 (server.py랑 맞춤)
    joblib.dump(pipeline2, "model_safety.pkl")

    print("✅ 모델 저장 완료: model_disaster.pkl, model_safety.pkl")

    return pipeline1, pipeline2


# ==========================================
# 9. 분석 결과 DB 업데이트
# ==========================================
def save_analysis_results(df, model_type, model_risk):
    conn = get_connection()
    cur = conn.cursor()

    print("\n💾 학습된 모델로 전체 데이터 재분석 & DB 저장 중...")

    # 전체 데이터 다시 예측해서 DB에 업데이트 (DM_ANALYSIS 테이블)
    for _, row in df.iterrows():
        # 'version'이 'original'인 것만 저장 (증강된 데이터는 제외)
        if row.get('version') != 'original':
            continue

        dmid = row.get('dmid')  # 원본 데이터 프레임에서 dmid 가져와야 함 (여기선 생략될 수 있으니 주의)

        # dmid가 없으면 패스 (증강 데이터일 경우)
        if pd.isna(dmid):
            continue

        content = row['content']

        # 예측 실행
        pred_type = model_type.predict([content])[0]
        pred_risk = model_risk.predict([content])[0]

        # MERGE문으로 있으면 업데이트, 없으면 삽입
        try:
            cur.execute("""
                MERGE INTO DM_ANALYSIS t
                USING (SELECT :dmid AS dmid FROM dual) s
                ON (t.dmid = s.dmid)
                WHEN MATCHED THEN
                    UPDATE SET t.pred_type = :pt, t.pred_risk = :pr, t.updated = SYSDATE
                WHEN NOT MATCHED THEN
                    INSERT (dmid, pred_type, pred_risk, created)
                    VALUES (:dmid, :pt, :pr, SYSDATE)
            """, {
                "dmid": int(dmid) if pd.notna(dmid) else 0,
                "pt": pred_type,
                "pr": pred_risk
            })
        except Exception as e:
            pass  # 에러나도 일단 진행

    conn.commit()
    conn.close()

    print("📌 DM_ANALYSIS 테이블 업데이트 완료")


# ==========================================
# 메인 실행부
# ==========================================
def main():
    print("\n=== 🔥 SafetyNevi AI 학습 파이프라인 시작 ===")

    # 1. DB에서 데이터 가져오기
    df_db = fetch_data_from_db()

    if df_db.empty:
        print("❌ 데이터가 없습니다. 크롤링 먼저 하세요.")
        return

    # 2. 데이터 가공 (전처리, 증강)
    df_train = prepare_training_dataframe(df_db)

    # 3. 데이터 분포 확인 (이미지 저장)
    visualize_data(df_train)

    # 4. 모델 학습 & 저장 (.pkl 파일 생성)
    model_type, model_risk = train_and_save_models(df_train)

    # 5. (선택사항) 분석 결과를 DB에 다시 저장
    # 주의: prepare_training_dataframe에서 dmid가 유실될 수 있으므로,
    # 실제로는 원본 df_db를 가지고 예측해서 저장하는게 더 정확함.
    # 여기서는 흐름상 df_db를 다시 활용
    print("\n--- 원본 데이터 재분석 ---")
    df_db['content_clean'] = df_db['CONTENT'].apply(clean_text)

    # save_analysis_results 함수를 df_db(원본) 기준으로 호출하도록 수정
    # 컬럼명을 소문자로 맞춰서 넘김
    df_for_save = df_db.rename(columns={'DMID': 'dmid', 'content_clean': 'content'})
    df_for_save['version'] = 'original'  # 강제로 마킹

    save_analysis_results(df_for_save, model_type, model_risk)

    print("\n=== 🎉 학습 완료! 서버를 재시작하세요. ===")


if __name__ == "__main__":
    main()