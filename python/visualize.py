"""학습 결과 시각화 — 워드클라우드 + 데이터 분포 + 모델 성능을 PNG로 뽑는다.
train.py 의 전처리·모델·폰트 설정을 그대로 재사용한다(중복 안 만들게).
실행: python visualize.py "<csv경로>"   (출력: ../src/main/resources/static/img/ml)
"""
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")  # 디스플레이 없이 파일로만 저장
import matplotlib.pyplot as plt
from wordcloud import WordCloud
from sklearn.model_selection import train_test_split, StratifiedGroupKFold
from sklearn.metrics import classification_report, confusion_matrix

import train  # 같은 폴더 train.py 재사용 (clean_text·to_risk·make_*_pipe·한글 폰트)

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src" / "main" / "resources" / "static" / "img" / "ml"
OUT.mkdir(parents=True, exist_ok=True)

PRIMARY, DANGER, SAFE, GRAY = "#2563EB", "#E5484D", "#30A46C", "#94A3B8"
SEED = train.SEED


def _save(name):
    plt.tight_layout()
    plt.savefig(OUT / name, dpi=150, bbox_inches="tight")
    plt.close()
    print(f"  저장: {name}")


def wordcloud_chart(texts):
    # 한글 불용어(자주 나오는 보일러플레이트)는 빼서 의미있는 단어가 도드라지게
    stop = {"행정안전부", "바랍니다", "하시기", "안전", "안내", "주시기", "여러분",
            "있습니다", "하세요", "해주세요", "당부", "협조", "참고", "관련"}
    wc = WordCloud(font_path=train.FONT_PATH, width=1200, height=600,
                   background_color="white", colormap="tab10",
                   stopwords=stop, max_words=150, prefer_horizontal=0.95,
                   regexp=r"[가-힣]+|[a-zA-Z]{3,}")  # 한글은 다 살리고 짧은 라틴 조각은 버림
    wc.generate(" ".join(texts))
    plt.figure(figsize=(12, 6))
    plt.imshow(wc, interpolation="bilinear")
    plt.axis("off")
    plt.title("재난문자 본문 워드클라우드", fontsize=16, fontweight="bold", pad=12)
    _save("wordcloud.png")


def type_distribution_chart(types):
    c = types.value_counts().head(15)[::-1]
    plt.figure(figsize=(9, 6))
    plt.barh(c.index, c.values, color=PRIMARY)
    for i, v in enumerate(c.values):
        plt.text(v, i, f" {v:,}", va="center", fontsize=9)
    plt.title("재난 유형 분포 (상위 15)  ·  2023.9~2024.8 행안부 긴급재난문자",
              fontsize=13, fontweight="bold")
    plt.xlabel("건수")
    plt.margins(x=0.12)
    _save("type_distribution.png")


def risk_distribution_chart(levels):
    risk = levels.apply(train.to_risk)
    c = risk.value_counts()
    danger, safe = int(c.get("DANGER", 0)), int(c.get("SAFE", 0))
    total = danger + safe
    plt.figure(figsize=(6, 5))
    bars = plt.bar(["DANGER\n(위급·긴급)", "SAFE\n(안전안내)"], [danger, safe], color=[DANGER, SAFE])
    for b, v in zip(bars, [danger, safe]):
        plt.text(b.get_x() + b.get_width() / 2, v, f"{v:,}\n({v / total * 100:.1f}%)",
                 ha="center", va="bottom", fontsize=11, fontweight="bold")
    plt.title("위험도 라벨 분포  ·  DANGER는 0.8%뿐 (극단적 불균형)", fontsize=12, fontweight="bold")
    plt.ylabel("건수")
    plt.ylim(0, safe * 1.18)
    _save("risk_distribution.png")


def type_f1_chart(prepared):
    # 종류 모델 클래스별 F1 — 누수 없는 평가(메시지 단위 그룹 분리)
    sgkf = StratifiedGroupKFold(n_splits=5, shuffle=True, random_state=SEED)
    tr, te = next(sgkf.split(prepared["content"], prepared["disastertype"], groups=prepared["msg_id"]))
    pipe = train.make_type_pipe()
    pipe.fit(prepared.iloc[tr]["content"], prepared.iloc[tr]["disastertype"])
    rep = classification_report(prepared.iloc[te]["disastertype"],
                                pipe.predict(prepared.iloc[te]["content"]),
                                output_dict=True, zero_division=0)
    rows = [(k, v["f1-score"], v["support"]) for k, v in rep.items()
            if k not in ("accuracy", "macro avg", "weighted avg")]
    rows = sorted(rows, key=lambda x: x[2], reverse=True)[:12][::-1]
    labels = [r[0] for r in rows]
    f1s = [r[1] for r in rows]
    colors = [SAFE if f >= 0.8 else (PRIMARY if f >= 0.5 else GRAY) for f in f1s]
    plt.figure(figsize=(9, 6))
    plt.barh(labels, f1s, color=colors)
    for i, f in enumerate(f1s):
        plt.text(f, i, f" {f:.2f}", va="center", fontsize=9)
    plt.title(f"재난 유형 분류 — 클래스별 F1  (정확도 {rep['accuracy']:.0%}, 표본 많은 12개)",
              fontsize=12, fontweight="bold")
    plt.xlim(0, 1.1)
    plt.xlabel("F1-score")
    _save("type_f1.png")


def risk_confusion_chart(prepared):
    # 위험도 모델 혼동행렬 — 불균형 그대로 홀드아웃(class_weight가 학습에서 처리)
    risk_df = prepared[prepared["emergency_level"].notna()
                       & (prepared["emergency_level"].astype(str).str.strip() != "")]
    X_tr, X_te, y_tr, y_te = train_test_split(
        risk_df["content"], risk_df["risk_label"],
        test_size=0.2, random_state=SEED, stratify=risk_df["risk_label"])
    pipe = train.make_risk_pipe()
    pipe.fit(X_tr, y_tr)
    pred = pipe.predict(X_te)
    labels = ["DANGER", "SAFE"]
    cm = confusion_matrix(y_te, pred, labels=labels)
    rep = classification_report(y_te, pred, output_dict=True, zero_division=0)
    plt.figure(figsize=(5.6, 5))
    plt.imshow(cm, cmap="Blues")
    for i in range(2):
        for j in range(2):
            plt.text(j, i, f"{cm[i, j]:,}", ha="center", va="center", fontsize=16,
                     color="white" if cm[i, j] > cm.max() / 2 else "black")
    plt.xticks([0, 1], labels)
    plt.yticks([0, 1], labels)
    plt.xlabel("예측")
    plt.ylabel("실제")
    dr, dp = rep["DANGER"]["recall"], rep["DANGER"]["precision"]
    plt.title(f"위험도 혼동행렬 (실제 분포 유지)\nDANGER recall {dr:.2f} · precision {dp:.2f}",
              fontsize=12, fontweight="bold")
    _save("risk_confusion.png")


def main():
    csv = sys.argv[1] if len(sys.argv) > 1 else train.DATA_CSV
    if not csv:
        print("CSV 경로를 인자로 주세요: python visualize.py <csv>")
        return
    print("데이터 로드...")
    raw = train.fetch_data_from_csv(csv)
    prepared = train.prepare_training_dataframe(train.fetch_data_from_csv(csv))

    print("시각화 생성 중...")
    wordcloud_chart(prepared["content"].tolist())
    type_distribution_chart(raw["DISASTERTYPE"])
    risk_distribution_chart(raw["EMERGENCY_LEVEL"])
    type_f1_chart(prepared)
    risk_confusion_chart(prepared)

    print(f"\n완료 → {OUT}")


if __name__ == "__main__":
    main()
