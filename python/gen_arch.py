"""운영 아키텍처 draw.io 생성기 — 기술 로고(devicon) + 일반 아이콘(Tabler)을 임베드.
SVG를 URL 인코딩해 image 데이터 URI로 넣는다(draw.io 네이티브 방식이라 ; 충돌·base64 문제 없음).
실행: python gen_arch.py   →  ../src/main/resources/static/img/다이어그램/architecture.drawio
"""
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DIA = ROOT / "src" / "main" / "resources" / "static" / "img" / "다이어그램"
ICONS = DIA / "icons"

# Tabler(일반) 아이콘은 currentColor라 색을 박아준다
GENERIC = {"monitor", "gov", "weather", "map"}
STROKE = "#37474f"


def img(name):
    svg = (ICONS / f"{name}.svg").read_text(encoding="utf-8")
    if name in GENERIC:
        svg = svg.replace("currentColor", STROKE)
    return "data:image/svg+xml," + urllib.parse.quote(svg)


# id, label, icon, x, y, size  — 좌:인그레스 / 상:외부API / 중앙:앱(HA) / 우:백엔드 의존성
nodes = [
    ("user",  "Browser · 사용자",                     "monitor", 60,   380, 56),
    ("nginx", "Nginx&#10;Reverse Proxy · LB",          "nginx",   250,  380, 62),
    ("gov",   "행안부&#10;재난문자 API",                "gov",     250,  110, 48),
    ("kma",   "기상청&#10;단기예보 API",                "weather", 480,  60,  48),
    ("kakao", "Kakao&#10;Map · Mobility",               "map",     700,  60,  48),
    ("app1",  "Spring Boot #1&#10;:9091",              "spring",  500,  290, 64),
    ("app2",  "Spring Boot #2&#10;:9092",              "spring",  500,  540, 64),
    ("kafka", "Apache Kafka&#10;disaster-topic",       "kafka",   780,  200, 62),
    ("redis", "Redis Cluster&#10;세션·캐시·Rate Limit","redis",   780,  430, 62),
    ("fast",  "FastAPI&#10;scikit-learn",              "fastapi", 1030, 290, 62),
    ("oracle","Oracle&#10;운영 DB",                     "oracle",  1030, 520, 62),
]

# src, tgt, label, kind(solid/dashed/thick), bidir
edges = [
    ("user", "nginx", "HTTP · WebSocket", "solid", True),
    ("nginx", "app1", "LB", "solid", True),
    ("nginx", "app2", "", "solid", True),
    ("gov", "app1", "1분 주기 수집", "dashed", False),
    ("app1", "kafka", "publish", "thick", False),
    ("kafka", "app1", "UUID-A", "thick", False),
    ("kafka", "app2", "UUID-B", "thick", False),
    ("app1", "redis", "", "solid", True),
    ("app2", "redis", "", "solid", True),
    ("app1", "oracle", "JDBC", "solid", False),
    ("app1", "fast", "HTTP 추론", "solid", True),
    ("app1", "kma", "HTTP", "solid", False),
    ("app1", "kakao", "HTTP", "solid", False),
]


def node_cell(nid, label, icon, x, y, sz):
    style = (f"shape=image;html=1;verticalLabelPosition=bottom;verticalAlign=top;"
             f"labelPosition=center;align=center;whiteSpace=wrap;spacingTop=2;fontSize=12;"
             f"image={img(icon)};")
    return (f'<mxCell id="{nid}" value="{label}" style="{style}" vertex="1" parent="1">'
            f'<mxGeometry x="{x}" y="{y}" width="{sz}" height="{sz}" as="geometry" /></mxCell>')


def edge_cell(eid, src, tgt, label, kind, bidir):
    s = "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;fontSize=11;"
    if bidir:
        s += "startArrow=block;"
    if kind == "dashed":
        s += "dashed=1;strokeColor=#9673a6;fontColor=#777777;"
    elif kind == "thick":
        s += "strokeColor=#9673a6;strokeWidth=2.5;fontColor=#6a4a8a;"
    else:
        s += "strokeColor=#555555;"
    return (f'<mxCell id="e{eid}" value="{label}" style="{s}" edge="1" parent="1" '
            f'source="{src}" target="{tgt}"><mxGeometry relative="1" as="geometry" /></mxCell>')


cells = [node_cell(*n) for n in nodes]
cells += [edge_cell(i + 1, *e) for i, e in enumerate(edges)]

xml = ('<mxfile host="app.diagrams.net" type="device">\n'
       '  <diagram name="운영 아키텍처" id="arch">\n'
       '    <mxGraphModel dx="1300" dy="820" grid="0" gridSize="10" guides="1" tooltips="1" '
       'connect="1" arrows="1" fold="1" page="0" pageScale="1" pageWidth="1280" pageHeight="800" math="0" shadow="0">\n'
       '      <root>\n'
       '        <mxCell id="0" />\n'
       '        <mxCell id="1" parent="0" />\n'
       + "".join("        " + c + "\n" for c in cells)
       + '      </root>\n'
       '    </mxGraphModel>\n'
       '  </diagram>\n'
       '</mxfile>\n')

(DIA / "architecture.drawio").write_text(xml, encoding="utf-8")
print("written:", (DIA / "architecture.drawio"))
print("nodes:", len(nodes), "edges:", len(edges))
