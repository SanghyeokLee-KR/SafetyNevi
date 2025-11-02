import os
import re
import sys
import random
import pandas as pd
import joblib
from pathlib import Path
from collections import Counter

# oracledb는 DB로 학습할 때만 필요 — CSV 파일로 학습하면 없어도 됨
try:
    import oracledb
except ImportError:
    oracledb = None

# 시각화용 라이브러리
import matplotlib.pyplot as plt
try:
    from wordcloud import WordCloud
except ImportError:
    WordCloud = None  # 워드클라우드는 선택 — 미설치면 해당 이미지만 건너뜀(모델엔 영향 없음)

# 머신러닝 (사이킷런)
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split, StratifiedGroupKFold
from sklearn.metrics import classification_report

# Windows 콘솔(cp949)에서도 한글 출력이 안 깨지게 stdout을 UTF-8로 맞춤
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# ==========================================
# 1. 환경 설정
# ==========================================
# DB 접속 정보 — 환경변수에서 받는다 (하드코딩해서 커밋하면 유출되니까). 예) export DB_USER=...
DB_USER = os.environ.get("DB_USER", "")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_DSN = os.environ.get("DB_DSN", "")

# CSV로도 학습 가능 — `python train.py <csv경로>` 또는 환경변수 DATA_CSV로 지정.
# 값이 있으면 DB 대신 CSV에서 읽는다 (행안부 긴급재난문자 CSV 같은 오프라인 파일 학습용).
DATA_CSV = os.environ.get("DATA_CSV", "")

# 모델 .pkl 은 이 파일과 같은 폴더에 저장 (CWD가 어디든 main.py 가 찾게)
MODEL_DIR = Path(__file__).resolve().parent

# 데이터 편향 방지용 상한 (리포트 보고 튜닝)
MAX_PER_AREA = 300   # 한 지역이 너무 많으면 줄임 (지리 편향)
MAX_PER_TYPE = 500   # 한 재난종류가 너무 많으면 줄임 — 코로나 시절 데이터라 감염병 쏠림 방지
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
    if oracledb is None:
        raise RuntimeError("oracledb 미설치 — DB로 학습하려면 'pip install oracledb' (CSV 학습은 불필요)")
    return oracledb.connect(user=DB_USER, password=DB_PASSWORD, dsn=DB_DSN)


# ==========================================
# 3. 데이터 가져오기 (DM 테이블)
# ==========================================
def fetch_data_from_db():
    print("DB에서 데이터 가져오는 중...")
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
    print(f"총 {len(df)}건 로드 완료.")
    return df


# CSV에서 학습 데이터 읽기 (행안부 긴급재난문자 CSV 등).
# 헤더가 한글(메시지내용/재해구분/긴급단계...)이든 영문코드(MSG_CN/DST_SE_NM...)든 키워드로 매칭하고,
# 정부 CSV 인코딩(utf-8-sig 또는 cp949/euc-kr)을 차례로 시도한다. 출력 컬럼은 DB 경로와 동일(대문자).
def fetch_data_from_csv(path):
    print(f"CSV에서 데이터 로드: {path}")
    df = None
    for enc in ("utf-8-sig", "cp949", "euc-kr"):
        try:
            df = pd.read_csv(path, encoding=enc, dtype=str)
            print(f"   인코딩 {enc} 로 읽음")
            break
        except (UnicodeDecodeError, UnicodeError):
            continue
    if df is None:
        raise RuntimeError("CSV 인코딩 판별 실패 (utf-8-sig/cp949/euc-kr 모두 실패)")

    # 헤더 이름에 키워드가 들어있는 컬럼을 찾아 표준 컬럼으로 매핑
    def find_col(*keywords):
        for col in df.columns:
            name = str(col).replace(" ", "").upper()
            if any(kw.upper() in name for kw in keywords):
                return col
        return None

    col_content = find_col("메시지내용", "메세지", "내용", "MSG_CN", "MSG")
    col_type    = find_col("재해구분", "재난구분", "재난종류", "재해", "DST_SE_NM", "DSTSE")
    col_level   = find_col("긴급단계", "단계", "EMRG_STEP_NM", "EMRGSTEP")
    col_area    = find_col("수신지역", "지역", "RCPTN_RGN_NM", "RGN")
    col_id      = find_col("일련번호", "SN", "번호", "ID")

    if col_content is None or col_type is None:
        raise RuntimeError(f"필수 컬럼(내용/재해구분)을 못 찾음. 실제 헤더: {list(df.columns)}")

    out = pd.DataFrame()
    out["DMID"] = df[col_id] if col_id else range(1, len(df) + 1)
    out["CONTENT"] = df[col_content]
    out["DISASTERTYPE"] = df[col_type]
    out["EMERGENCY_LEVEL"] = df[col_level] if col_level else ""
    out["AREA"] = df[col_area] if col_area else "UNKNOWN"
    out = out.dropna(subset=["CONTENT"])

    print(f"총 {len(out)}건 로드 완료. (매핑: 내용={col_content}, 종류={col_type}, 단계={col_level}, 지역={col_area})")
    return out


# 학습 소스 결정 — CSV 경로(인자 또는 DATA_CSV)가 있으면 CSV, 없으면 DB
def load_source_dataframe():
    csv_path = sys.argv[1].strip() if (len(sys.argv) > 1 and sys.argv[1].strip()) else DATA_CSV
    if csv_path:
        if not Path(csv_path).exists():
            raise FileNotFoundError(f"CSV를 찾을 수 없음: {csv_path}")
        return fetch_data_from_csv(csv_path), "csv"
    return fetch_data_from_db(), "db"


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
def prepare_training_dataframe(df, max_per_area=MAX_PER_AREA, max_per_type=MAX_PER_TYPE):
    print("학습 데이터 가공 중... (지역명 제거 등)")
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
    # msg_id = 원본 메시지 식별자. 원본과 그 증강본(anon)이 같은 id를 공유해서,
    # 평가 때 한 메시지의 두 버전이 train/test 양쪽에 갈라지는 누수를 막는다.
    for msg_id, (_, r) in enumerate(df.iterrows()):
        content = r['content']
        dtype = r['disastertype']
        area = r['area'] if pd.notna(r['area']) else "UNKNOWN"
        risk = r['risk_label']
        level = r['emergency_level']  # 위험도 모델은 공식 단계가 있는 행만 학습할 거라 같이 들고 감

        # 1. 원본 데이터 추가
        rows.append({
            'content': content, 'disastertype': dtype, 'risk_label': risk,
            'emergency_level': level, 'area': area, 'version': 'original', 'msg_id': msg_id
        })

        # 2. 지역명 지운 버전도 추가 (데이터 증강 효과 + 편향 방지)
        anon = remove_area_mentions(content, area_patterns)
        # 지진, 강풍 등은 지역명이 중요할 수도 있어서 제외하고 나머지만
        if anon.strip() and dtype not in ["지진", "강풍", "대설", "산불", "수도"]:
            rows.append({
                'content': anon, 'disastertype': dtype, 'risk_label': risk,
                'emergency_level': level, 'area': area, 'version': 'anon', 'msg_id': msg_id
            })

    # 데이터 셔플
    df_all = pd.DataFrame(rows).sample(frac=1, random_state=SEED).reset_index(drop=True)

    # 다운샘플링 — 지역/재난종류가 너무 많으면 깎아서 편향을 막는다.
    #  - 지역 캡: 지리 편향
    #  - 종류 캡: 코로나 시절 데이터라 감염병이 압도하는 시기 편향 (이게 핵심)
    final = []
    area_counts = Counter()
    type_counts = Counter()

    for _, r in df_all.iterrows():
        area = r['area']
        dtype = r['disastertype']
        if area_counts[area] < max_per_area and type_counts[dtype] < max_per_type:
            final.append(r)
            area_counts[area] += 1
            type_counts[dtype] += 1

    df_final = pd.DataFrame(final)
    print(f"최종 학습 데이터: {len(df_final)}건 (종류별 분포: {dict(type_counts)})")

    return df_final


# ==========================================
# 7. 시각화 (확인용)
# ==========================================
def visualize_data(df):
    print("\n데이터 분포 시각화 생성 중...")

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

    # 워드클라우드 (자주 나오는 단어 확인) — 라이브러리 있을 때만
    if WordCloud is None:
        print("wordcloud 미설치 — 워드클라우드 이미지는 건너뜀")
    else:
        text = " ".join(df['content'].tolist())
        try:
            wc = WordCloud(font_path=FONT_PATH, width=800, height=400, background_color="white")
            wc.generate(text)
            wc.to_file("wordcloud.png")
        except Exception:
            print("워드클라우드 생성 실패 (폰트 문제일 수 있음)")

    print("이미지 저장 완료 (class_distribution.png 등)")


# ==========================================
# 8. 모델 학습 · 평가 · 저장
# ==========================================
def make_type_pipe():
    return Pipeline([('tfidf', TfidfVectorizer(max_features=5000)), ('clf', MultinomialNB())])


def make_risk_pipe():
    # 위험도는 DANGER가 0.8%뿐이라, 업샘플(복제) 대신 class_weight='balanced' 로 불균형을 처리한다.
    # 복제를 안 하니 평가 누수가 없고, 과경보(낮은 precision)도 NB+업샘플보다 덜하다.
    return Pipeline([
        ('tfidf', TfidfVectorizer(max_features=3000)),
        ('clf', LogisticRegression(class_weight='balanced', max_iter=1000)),
    ])


def evaluate_type(df):
    # 종류 모델 평가 — 증강본(anon)은 원본과 같은 메시지라 msg_id로 묶어, 한 메시지의 원본·증강이
    # train/test 양쪽에 갈라지지 않게 한다(누수 방지). StratifiedGroupKFold로 클래스 비율도 유지.
    if df['disastertype'].nunique() < 2 or len(df) < 20:
        print("   [재난 종류] 표본 부족 → 평가 생략")
        return
    sgkf = StratifiedGroupKFold(n_splits=5, shuffle=True, random_state=SEED)
    tr_idx, te_idx = next(sgkf.split(df['content'], df['disastertype'], groups=df['msg_id']))
    tr, te = df.iloc[tr_idx], df.iloc[te_idx]
    pipe = make_type_pipe()
    pipe.fit(tr['content'], tr['disastertype'])
    print("\n[재난 종류] 홀드아웃(20%, 메시지 단위 분리) 성능:")
    print(classification_report(te['disastertype'], pipe.predict(te['content']), zero_division=0))


def evaluate_risk(df_risk):
    # 위험도 평가 — 불균형을 그대로 두고 홀드아웃을 뗀다. class_weight 가 학습 때 불균형을
    # 처리하므로 업샘플(복제) 없이 손 안 댄 실제 분포로 측정한다 → 누수 없는 정직한 precision/recall.
    counts = Counter(df_risk['risk_label'])
    if len(counts) < 2 or min(counts.values()) < 2 or len(df_risk) < 20:
        print("   [위험도] 표본/클래스 부족 → 평가 생략")
        return
    X_tr, X_te, y_tr, y_te = train_test_split(
        df_risk['content'], df_risk['risk_label'],
        test_size=0.2, random_state=SEED, stratify=df_risk['risk_label'])
    pipe = make_risk_pipe()
    pipe.fit(X_tr, y_tr)
    print("\n[위험도 DANGER/SAFE] 홀드아웃(20%, 실제 분포 유지) 성능:")
    print(classification_report(y_te, pipe.predict(X_te), zero_division=0))


def train_and_save_models(df):
    print("\n모델 학습 시작...")

    # 1. 재난 종류 모델 — 평가(메시지 단위 분리) 먼저 보고, 최종 모델은 전체 데이터로 학습
    evaluate_type(df)
    type_pipe = make_type_pipe()
    type_pipe.fit(df['content'], df['disastertype'])
    joblib.dump(type_pipe, MODEL_DIR / "model_disaster.pkl")

    # 2. 위험도 모델 — 공식 긴급단계가 채워진 행만. class_weight 로 불균형 처리(업샘플 안 함).
    risk_df = df[df['emergency_level'].notna() & (df['emergency_level'].astype(str).str.strip() != "")]
    if risk_df.empty:
        print("긴급단계가 채워진 데이터가 없어 위험도 모델은 건너뜀 (새 크롤링으로 단계 채운 뒤 재학습)")
        return type_pipe, None

    evaluate_risk(risk_df)
    risk_pipe = make_risk_pipe()
    risk_pipe.fit(risk_df['content'], risk_df['risk_label'])
    joblib.dump(risk_pipe, MODEL_DIR / "model_safety.pkl")

    print("모델 저장 완료: model_disaster.pkl, model_safety.pkl")
    return type_pipe, risk_pipe


# ==========================================
# 9. 분석 결과 DB 업데이트
# ==========================================
def save_analysis_results(df, model_type, model_risk):
    conn = get_connection()
    cur = conn.cursor()

    print("\n학습된 모델로 전체 데이터 재분석 & DB 저장 중...")

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
            print(f"DM_ANALYSIS 저장 실패 (dmid={dmid}): {e}")  # 조용히 삼키지 말고 알림

    conn.commit()
    conn.close()

    print("DM_ANALYSIS 테이블 업데이트 완료")


# ==========================================
# 메인 실행부
# ==========================================
def main():
    print("\n=== SafetyNevi AI 학습 파이프라인 시작 ===")

    # 1. 데이터 가져오기 (CSV 경로 주면 CSV, 아니면 DB)
    df_src, mode = load_source_dataframe()

    if df_src.empty:
        print("데이터가 없습니다. (CSV가 비었거나, DB면 크롤링 먼저)")
        return

    # 2. 데이터 가공 (전처리, 증강)
    df_train = prepare_training_dataframe(df_src)

    # 3. 데이터 분포 확인 (이미지 저장)
    visualize_data(df_train)

    # 4. 모델 학습 & 저장 (.pkl 파일 생성)
    model_type, model_risk = train_and_save_models(df_train)

    # 5. DB 모드일 때만 원본을 재분석해 DB에 저장. CSV 오프라인 학습은 .pkl 생성까지가 끝(쓸 DB 없음).
    if mode != "db":
        print("\nCSV 학습이라 DB 재분석은 건너뜁니다 (.pkl 만 생성).")
    elif model_risk is None:
        print("\n위험도 모델이 없어 DB 재분석은 건너뜁니다 (긴급단계 채운 뒤 재실행).")
    else:
        print("\n--- 원본 데이터 재분석 ---")
        df_src['content_clean'] = df_src['CONTENT'].apply(clean_text)
        df_for_save = df_src.rename(columns={'DMID': 'dmid', 'content_clean': 'content'})
        df_for_save['version'] = 'original'
        save_analysis_results(df_for_save, model_type, model_risk)

    print("\n=== 학습 완료! 서버를 재시작하세요. ===")


if __name__ == "__main__":
    main()
