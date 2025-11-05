<div align="center">
  <img src="src/main/resources/static/img/logo/로고.png" width="140" alt="SafetyNevi"/>
  <h1>SafetyNevi · 안전네비</h1>
  <p><b>재난문자를 지도 위 위험 구역으로, 가장 가까운 운영 대피소까지 — AI · GIS 재난 대피 플랫폼</b></p>

  <p>
    <img src="https://img.shields.io/badge/Java_21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21"/>
    <img src="https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot"/>
    <img src="https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white" alt="Redis"/>
    <img src="https://img.shields.io/badge/Kafka-231F20?logo=apachekafka&logoColor=white" alt="Kafka"/>
    <img src="https://img.shields.io/badge/Python-3776AB?logo=python&logoColor=white" alt="Python"/>
    <img src="https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white" alt="FastAPI"/>
    <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="Docker"/>
    <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?logo=githubactions&logoColor=white" alt="CI/CD"/>
    <img src="https://img.shields.io/badge/Oracle-F80000?logo=oracle&logoColor=white" alt="Oracle"/>
  </p>

  <img src="src/main/resources/static/img/screenshots/map.png" width="860" alt="SafetyNevi 지도 화면"/>
  <br/>
  <sub>현재 위치 주변 시설을 클러스터링으로 묶어 보여주고, 재난 발생 시 위험 구역을 지도에 그린다</sub>
</div>

<br/>

> 재난문자는 텍스트라 "어디가 위험하고 어디로 가야 하는지"가 한눈에 안 들어온다. SafetyNevi는 공식 재난문자를 **지도 위 위험 구역(폴리곤)** 으로 바꾸고, 현재 위치에서 **'지금 운영 중'인 대피소까지 경로**를 안내한다.

> **이 저장소에 대해** — 인하공전 졸업작품으로 만든 재난 대피 플랫폼을, 졸작 시연 이후 **실서비스 수준으로 다듬어 온 기록**입니다. 공식 API 전환 · HA 인프라(Redis/Kafka) · CI/CD · 관측 · 보안 · AI 재설계까지, "동작하는 데모"를 "운영 가능한 서비스"로 끌어올리는 과정이 이 README의 중심입니다.

> **운영 고지** — 졸업작품 평가용으로 2025.12.07 ~ 12.14 (7일) `safety.inhatc.com` 에 배포·시연했고, 현재 데모 서버는 내려간 상태입니다. 위험도 AI는 한계가 분명하므로([9. 한계](#9-한계와-트레이드오프)) **안전 판단의 최종 근거가 아니라**, 공식 경보를 빠르게 지도에 띄우고 대피를 돕는 보조 도구로 봐주세요.

<details open>
<summary><b>목차</b></summary>

<br>

**소개**
* [1. 개요](#1-개요)
* [2. 팀](#2-팀)

**설계 · 기능**
* [3. 시스템 아키텍처](#3-시스템-아키텍처)
* [4. 주요 기능](#4-주요-기능)
* [5. AI 파이프라인](#5-ai-파이프라인)
* [6. 졸작에서 실서비스로](#6-졸작에서-실서비스로)
* [7. 설계 다이어그램](#7-설계-다이어그램)

**스택 · 한계**
* [8. 기술 스택](#8-기술-스택)
* [9. 한계와 트레이드오프](#9-한계와-트레이드오프)

**실행**
* [10. 디렉터리 구조](#10-디렉터리-구조)
* [11. 설치 및 실행](#11-설치-및-실행)

</details>

---

## 1. 개요

기후변화·도시화로 재난은 잦아지는데, 재난문자는 텍스트라 "내 주변이 위험한지, 어디로 가야 하는지"가 직관적이지 않습니다. SafetyNevi는 **재난문자를 지도 위 위험 구역으로 바꾸고, 현재 위치에서 운영 중인 대피소까지 경로를 안내**합니다.

```
재난문자 / 기상 데이터  →  분류(재난유형) · 공식 긴급단계  →  지도 폴리곤 + WebSocket 실시간 알림  →  운영 중 대피소 경로 안내
```

메인 서버(Spring Boot)와 AI 서버(Python FastAPI)를 분리해, 무거운 텍스트 추론이 웹 응답을 막지 않도록 했습니다. 전국 경찰·소방·병원·대피소 **수천 건**을 기동 시 적재해 지도에 클러스터링으로 띄웁니다.

<div align="center">
  <img src="src/main/resources/static/img/screenshots/landing.png" width="680" alt="메인 화면"/>
  <br/>
  <sub>로그인 · 지도 · 공지 · 재난 대피요령으로 진입하는 메인</sub>
</div>

---

## 2. 팀

| 이름 | 포지션 | 주요 기여 |
| :--- | :--- | :--- |
| **[이상혁](https://github.com/SanghyeokLee-KR)** | Tech Lead · PM | Python AI 서버·모델, 지도 핵심(폴리곤·경로 탐색)·WebSocket, Oracle 스키마·관리자, 배포(Docker·Nginx/HTTPS) |
| **유기민** | Backend | 재난문자 수집 스케줄러, 게시판·공지·문의 REST API, DB 스키마 |
| **김보겸** | Frontend Lead | 전체 퍼블리싱·반응형, 로고·아이콘·발표자료, 회원 UX |
| **이진혁** | Frontend | 재난 행동요령 콘텐츠, 테스트 데이터셋 |

> 졸업작품 이후의 실서비스화 작업(공식 API 전환 · Redis/Kafka · CI/CD · 관측 · 보안 · AI 재설계)은 [이상혁](https://github.com/SanghyeokLee-KR)이 이어서 진행했습니다.

---

## 3. 시스템 아키텍처

```mermaid
flowchart LR
    User["사용자 브라우저"] -->|HTTPS| Nginx["Nginx 리버스 프록시"]
    Nginx --> App["Spring Boot 메인 서버<br/>지도·재난·커뮤니티·관리자"]
    App <-->|HTTP 추론| AI["FastAPI AI 서버<br/>재난유형·위험도 분류"]
    App --> Oracle[("Oracle 운영 DB")]
    App -->|수집·경로·날씨| Ext["행안부·기상청·Kakao API"]
    App -.->|WebSocket 알림| User
    App --> Redis[("Redis · prod 전용<br/>캐시·세션·레이트리밋")]
    App --> Kafka["Kafka · prod 전용<br/>재난 이벤트 fan-out"]
```

- **메인 (Spring Boot)** — 사용자·지도·시설·게시판·공지·관리자·재난 도메인 + WebSocket(STOMP) 실시간 알림.
- **AI (FastAPI)** — 재난문자 텍스트를 받아 재난유형·위험도를 분류해 돌려줍니다. 메인 서버와 HTTP로 통신.
- **데이터** — 운영은 Oracle, 로컬은 H2 인메모리(프로파일로 전환). 시설 수천 건은 기동 시 CSV로 적재.

---

## 4. 주요 기능

**1. 실시간 긴급 알림 + GIS 시각화**
공식 긴급단계가 위급·긴급이면 WebSocket으로 접속 중인 모든 사용자에게 모달 알림을 보내고, 해당 지역을 지도에 붉은 폴리곤(위험 구역)으로 그립니다.

**2. 운영 중 대피소 경로 안내**
단순 최단거리가 아니라 **'지금 운영 중'인 시설만** 필터링하고, Kakao Mobility API로 도보/차량 실제 경로와 소요시간을 계산합니다.

**3. 시설물 클러스터링**
전국 경찰서·소방서·병원·대피소 수천 개를 마커 클러스터링으로 묶어 보여주고, 줌 레벨에 따라 집약합니다. 화면에 보이는 범위만 조회하도록 박스 쿼리 + 상한을 둬서 **시설 조회를 1.8s → 0.27s (약 6.6배)** 로 줄였습니다.

**4. 위치 기반 안전 커뮤니티 · 관리자 콘솔**
지도에서 직접 위치를 찍어 제보 글을 쓰고 댓글·좋아요로 공유합니다. 관리자는 재난 발령·회원·게시물·신고·문의를 한곳에서 관리합니다.

**재난 경보가 모든 사용자에게 닿기까지** — 인스턴스를 여러 대 띄워도 알림이 빠짐없이 가도록, Kafka 컨슈머 그룹을 인스턴스마다 고유(UUID)하게 둬서 **모든 인스턴스가 모든 이벤트를 받아** 각자 붙은 WebSocket 클라이언트로 fan-out 합니다.

```mermaid
flowchart TB
    Gov["행안부 긴급재난문자 API"] --> Crawl["수집 스케줄러 · 1분 간격"]
    Crawl --> Judge{"공식 긴급단계 위급·긴급?"}
    Judge -->|예| Zone["위험 구역 폴리곤 생성"]
    Judge -->|단계 없음| AI["AI 보조 판정"]
    AI --> Zone
    Zone --> Topic["Kafka · disaster 토픽"]
    Topic --> I1["인스턴스 1<br/>컨슈머 UUID-a"]
    Topic --> I2["인스턴스 2<br/>컨슈머 UUID-b"]
    I1 -.->|WebSocket| U1["접속 클라이언트"]
    I2 -.->|WebSocket| U2["접속 클라이언트"]
```

<div align="center">
  <img src="src/main/resources/static/img/screenshots/admin.png" width="780" alt="관리자 대시보드"/>
  <br/>
  <sub>관리자 콘솔 — 통계 · 재난 시뮬레이션 발령 · 회원/게시물/신고 관리</sub>
</div>

---

## 5. AI 파이프라인

위험도는 **AI 추정이 아니라 정부 공식 긴급단계(위급 · 긴급 · 안전안내)를 그대로 중계**합니다 — 권위 있는 출처가 이미 분류해 준 걸 쓰는 게 맞다고 봤습니다. AI는 **공식 단계가 없는 텍스트**(시민 제보 등) 보조에만 씁니다.

| 단계 | 내용 |
| :--- | :--- |
| **수집** | 1분 간격 공식 API(행안부 긴급재난문자) — 종류 · **긴급단계** · 내용 · 지역 |
| **위험 판정** | 공식 긴급단계가 **위급·긴급**이면 지역 기준 60분 위험 폴리곤 + WebSocket 전파. 안전안내는 표시만 |
| **AI 보조** | 긴급단계가 **비어 있는** 메시지·시민 제보 텍스트만 scikit-learn으로 보조 추정 |

모델은 **행안부 공식 재난문자 18,399건(2023.9 ~ 2024.8)** 으로 학습했습니다. 라벨이 본문 키워드가 아니라 공식 분류라서, "텍스트 → 공식 판정"을 배우는 진짜 지도학습입니다.

### 데이터

<table>
  <tr>
    <td width="55%" align="center" valign="top">
      <img src="src/main/resources/static/img/ml/type_distribution.png" alt="재난 유형 분포"/>
    </td>
    <td width="45%" align="center" valign="top">
      <img src="src/main/resources/static/img/ml/wordcloud.png" alt="재난문자 워드클라우드"/>
    </td>
  </tr>
</table>

폭염·호우·대설 등 **기상 재난이 대부분**이고, 전염병은 85건(0.5%)에 불과합니다. 2023년 상반기에 두드러졌던 코로나(감염병) 쏠림이 이 기간 데이터에선 자연스럽게 해소돼, 모델이 특정 시기에 편향되지 않습니다.

### 문제 — 극단적 불균형

<div align="center">
  <img src="src/main/resources/static/img/ml/risk_distribution.png" width="460" alt="위험도 라벨 불균형"/>
</div>

위험(DANGER)은 전체의 **0.8%(143건)** 뿐입니다. 그냥 학습하면 전부 SAFE로 찍어도 99% 정확도가 나오는 함정이 있어서, 위험도 모델은 업샘플 대신 **클래스 가중치(`class_weight='balanced'`)** 로 불균형을 처리했습니다.

### 모델과 정직한 성능

- **재난 종류** — TF-IDF + MultinomialNB, 정확도 **78%**
- **위험도** — TF-IDF + LogisticRegression(`class_weight='balanced'`)

<table>
  <tr>
    <td width="50%" align="center" valign="top">
      <b>재난 종류 · 클래스별 F1</b><br/>
      <img src="src/main/resources/static/img/ml/type_f1.png" alt="종류 분류 F1"/>
    </td>
    <td width="50%" align="center" valign="top">
      <b>위험도 · 혼동행렬</b><br/>
      <img src="src/main/resources/static/img/ml/risk_confusion.png" alt="위험도 혼동행렬"/>
    </td>
  </tr>
</table>

위험도 모델은 실제 긴급·위급을 **하나도 놓치지 않고(recall 1.00)**, 대신 과경보(precision 0.57)가 있습니다. 재난 안전에서는 "놓치느니 과경보"가 합리적인 편향이라 보고 recall을 우선했습니다.

> **정직한 평가에 신경 썼습니다.** 처음엔 위험도 정확도가 99%로 나왔는데, 이는 소수 클래스를 업샘플(복제)한 **뒤에** 학습/평가를 나눠 같은 데이터가 양쪽에 새는 누수였습니다. **데이터를 먼저 나눈 다음 학습셋만 가중**하도록 고쳐 실제 분포로 측정했고(위 혼동행렬), 종류 모델도 증강본을 원본과 같은 그룹으로 묶어 평가했습니다.

> **순환 라벨 문제도 재설계로 해결.** 원래 위험도 라벨은 본문 키워드 규칙으로 만들어서, 모델이 그 규칙을 그대로 모사하는 순환 구조였습니다. 정부 공식 긴급단계를 정답으로 쓰도록 바꿔 비순환으로 만들었습니다.

---

## 6. 졸작에서 실서비스로

졸작은 "동작하는 데모"였습니다. 그 뒤로 실제 운영을 가정하고 다음을 보강했습니다 — **포트폴리오에서 제일 신경 쓴 부분**입니다.

| 영역 | 한 일 | 왜 |
| :--- | :--- | :--- |
| **데이터 소스** | 네이버 HTML 크롤링 → **공식 OpenAPI**(행안부 재난문자 · 기상청 단기예보) | 크롤링은 페이지 구조 바뀌면 깨지고 비공식. 공식 API가 안정적·합법 |
| **캐시 · 세션 · 레이트리밋** | 운영은 **Redis**, 로컬은 인메모리 — **프로파일로 분리** | 인스턴스를 늘려도 캐시·세션·남용카운터가 공유돼야 HA. 단 로컬은 Docker 없이 그대로 떠야 해서 갈라둠 |
| **이벤트 전파** | 재난 알림을 **Kafka**로 발행→소비, 컨슈머 그룹을 인스턴스마다 고유(UUID)하게 | 인스턴스가 여러 대면 모든 인스턴스가 각자 붙은 WebSocket 클라이언트로 fan-out 해야 알림이 다 감 |
| **배포 (CI/CD)** | **GitHub Actions** → Docker 이미지 빌드 → **GHCR** push → 서버 `pull` · 재시작 | 푸시하면 테스트·이미지·배포까지 자동. 서버 시크릿 없으면 배포 단계는 graceful skip |
| **관측** | Actuator **health / liveness / readiness** + **Prometheus** 메트릭 | 로드밸런서·모니터링이 앱 상태를 읽을 수 있게 |
| **보안** | 보안 헤더 + **CSP** · IP 레이트리밋 · 출력 escape(XSS) · CSRF · graceful shutdown | XSS·API 남용 방어, 배포 중 처리 중이던 요청 유실 방지 |
| **테스트** | 단위·슬라이스 테스트 **30개** (보안 규칙 · 권한 · 입력검증 · 동시성) | 보안·정합성 수정마다 회귀 방지 잠금 |
| **프론트 빌드** | 중복 JS 제거 → **TypeScript**(tsc) + Gradle 빌드 통합 | 타입 안전 + 빌드 산출물 일원화(`frontend/src` → `static/js` 자동 컴파일) |

> 운영 인프라(Redis · Kafka)는 **`prod` 프로파일에서만** 켜집니다. 로컬(`h2`)·테스트는 관련 오토컨피그를 빼서 **Docker 없이 그대로** 동작합니다. (운영 검증 상태는 [9. 한계](#9-한계와-트레이드오프) 참고)

**CI/CD 파이프라인** — `master` push 한 번으로 테스트부터 배포까지 자동으로 흐릅니다.

```mermaid
flowchart LR
    Push["master push"] --> CI["테스트 + 빌드<br/>gradlew test"]
    CI --> Img["Docker 이미지 빌드"]
    Img --> GHCR[("GHCR 이미지 레지스트리")]
    GHCR --> Deploy["서버 SSH<br/>pull + 재시작"]
    Deploy -.->|시크릿 없으면| Skip["graceful skip"]
```

---

## 7. 설계 다이어그램

이미지를 누르면 원본 크기로 열립니다.

<table>
  <tr>
    <td colspan="2" align="center" valign="top">
      <b>시스템 구성 (캡스톤 설계 원안)</b><br/>
      <a href="src/main/resources/static/img/다이어그램/시스템%20아키텍처.png"><img src="src/main/resources/static/img/다이어그램/시스템%20아키텍처.png" width="540" alt="시스템 아키텍처"/></a>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top">
      <b>유스케이스</b><br/>
      <a href="src/main/resources/static/img/다이어그램/유스케이스%20다이어그램.png"><img src="src/main/resources/static/img/다이어그램/유스케이스%20다이어그램.png" width="400" alt="유스케이스 다이어그램"/></a>
    </td>
    <td width="50%" align="center" valign="top">
      <b>클래스</b><br/>
      <a href="src/main/resources/static/img/다이어그램/클래스%20다이어그램.png"><img src="src/main/resources/static/img/다이어그램/클래스%20다이어그램.png" width="400" alt="클래스 다이어그램"/></a>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top">
      <b>데이터베이스 ERD</b><br/>
      <a href="src/main/resources/static/img/다이어그램/erd다이어그램.png"><img src="src/main/resources/static/img/다이어그램/erd다이어그램.png" width="400" alt="ERD"/></a>
    </td>
    <td width="50%" align="center" valign="top">
      <b>데이터 처리 시퀀스</b><br/>
      <a href="src/main/resources/static/img/다이어그램/시퀀스%20다이어그램.png"><img src="src/main/resources/static/img/다이어그램/시퀀스%20다이어그램.png" width="400" alt="시퀀스 다이어그램"/></a>
    </td>
  </tr>
</table>

---

## 8. 기술 스택

| 분류 | 스택 |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.5, Spring Security, JPA, WebSocket(STOMP) |
| **AI 서버** | Python, FastAPI, scikit-learn, Pandas, Joblib |
| **Frontend** | TypeScript, Thymeleaf, HTML/CSS, Kakao Map/Mobility |
| **운영 인프라** | Redis(캐시·세션·레이트리밋), Kafka(이벤트), Docker, Nginx |
| **DB** | Oracle (운영) / H2 (로컬·테스트) |
| **CI/CD · 관측** | GitHub Actions, GHCR, Actuator, Prometheus |
| **외부 API** | 행안부 긴급재난문자, 기상청 단기예보, Kakao |

---

## 9. 한계와 트레이드오프

실서비스 기준으로 솔직하게 남은 한계입니다.

- **위험도 AI는 "희귀 격상 탐지기"에 가깝습니다.** 공식 위급·긴급 격상이 워낙 드물어(0.8%) 모델을 recall에 치우치게 학습했습니다. 실제 긴급은 다 잡지만 과경보(precision 0.57)가 있고, 학습 표본 자체가 적어 일반화에는 한계가 있습니다. 그래서 위험 판정의 1차 근거는 항상 **공식 긴급단계**이고 AI는 보조입니다. (남은 일: 긴급단계 포함 재크롤링 후 재학습 — 운영 API 키 발급 대기 중)
- **운영 인프라(Redis · Kafka)는 코드·설정까지지만 실부하 검증 전입니다.** 프로파일 분리·`docker-compose`·fan-out 컨슈머 설계는 끝났지만, 실제 다중 인스턴스·부하·장애 검증은 아직입니다(로컬에 Docker가 없어 운영 런타임은 CI/서버 몫). 운영해 본 건 단일 인스턴스 데모뿐입니다.
- **세션 직렬화 미검증** — 운영 Redis 세션은 세션 속성 직렬화에 의존하는데, 실서버에서 끝까지 확인하진 않았습니다.
- **데모 종료** — 7일 평가 운영 후 서버는 내렸습니다. 재배포하려면 서버 · GitHub 시크릿 세팅이 필요합니다.

---

## 10. 디렉터리 구조

```
SafetyNevi/
├── src/main/java/.../safetynevi/   # Spring Boot — 도메인별 controller·service·dto·entity·config
├── src/main/resources/
│   ├── templates/                  # Thymeleaf 뷰
│   └── static/{css,js,img}         # 정적 리소스 (js 는 TypeScript 빌드 산출물)
├── frontend/src/                   # TypeScript 소스 (→ static/js 로 컴파일)
├── python/                         # FastAPI AI 서버 (main.py · train.py · visualize.py · *.pkl)
├── Dockerfile · docker-compose.yml # 이미지 빌드 + Redis/Kafka 포함 배포 구성
├── .github/workflows/ci.yml        # CI/CD (테스트 → 이미지 → GHCR → 배포)
└── build.gradle
```

---

## 11. 설치 및 실행

### 1) Python AI 서버

```bash
cd python
pip install fastapi uvicorn scikit-learn pandas joblib
uvicorn main:app --host 0.0.0.0 --port 8000
```

재난문자 CSV로 모델을 새로 학습하려면 `python train.py "<csv경로>"`, 학습 결과 시각화는 `python visualize.py "<csv경로>"` 입니다.

### 2) 메인 서버 (Spring Boot) — 요구사항: Java 21

**설정 파일 준비** — 실제 키가 든 설정은 git에 포함하지 않습니다. 예시를 복사해 채워주세요.

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
# Kakao · 기상청 · 재난문자 API 키, DB 접속 정보 입력
```

**로컬 실행** — H2 인메모리로 Oracle·Docker 없이 바로 뜹니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

- 접속: `http://localhost:9090`
- 테스트 계정: `admin / admin1234` (관리자), `test / test1234` (일반) — 기동 시 자동 생성
- 프론트엔드는 자동 빌드됩니다. TypeScript(`frontend/src/*.ts`)가 `bootRun`·`build` 때 `static/js`로 컴파일됩니다(Node는 최초 1회 자동 다운로드).

### 3) 운영 배포 (참고)

`docker-compose.yml`이 GHCR 이미지 + Redis + Kafka를 함께 띄웁니다. 서버에 Docker와 `application-prod.properties`(실제 키)를 두고:

```bash
docker compose pull && docker compose up -d
```

`master`에 push하면 GitHub Actions가 테스트 → 이미지 빌드 → GHCR push → 서버 배포까지 자동으로 처리합니다.

---

## 라이선스

[MIT](LICENSE)
