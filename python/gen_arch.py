"""운영 아키텍처(AWS) draw.io 생성기.
- AWS 서비스: draw.io 내장 aws4 도형(ALB·EC2·ElastiCache·RDS) + AWS Cloud/VPC 그룹 박스
- 자체 서비스/외부: 기술 로고(devicon)·일반 아이콘(Tabler) SVG 임베드
- 선은 노드보다 먼저 그려서(z-order) 아이콘 뒤로 깔린다
실행: python gen_arch.py
"""
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DIA = ROOT / "src" / "main" / "resources" / "static" / "img" / "다이어그램"
ICONS = DIA / "icons"
GENERIC = {"monitor", "gov", "weather", "map"}
STROKE = "#37474f"


def img_uri(name):
    svg = (ICONS / f"{name}.svg").read_text(encoding="utf-8")
    if name in GENERIC:
        svg = svg.replace("currentColor", STROKE)
    return "data:image/svg+xml," + urllib.parse.quote(svg)


# AWS resourceIcon: resIcon, 카테고리 색
AWS = {
    "alb":   ("application_load_balancer", "#8C4FFF"),
    "ec2":   ("ec2", "#ED7100"),
    "cache": ("elasticache", "#C925D1"),
    "rds":   ("rds", "#C925D1"),
}

# 그룹 박스: id, label, grIcon, color, x, y, w, h
groups = [
    ("cloud", "AWS Cloud", "group_aws_cloud_alt", "#232F3E", 330, 165, 1115, 590),
    ("vpc",   "VPC",       "group_vpc",            "#248814", 372, 225, 1050, 510),
]

# 노드: id, label, kind('aws'|'img'), key, x, y, size
nodes = [
    ("user",  "Browser · 사용자",                       "img", "monitor",  40,  450, 58),
    ("gov",   "행안부&#10;재난문자 API",                  "img", "gov",      480,  95,  48),
    ("kma",   "기상청&#10;단기예보 API",                  "img", "weather",  700,  95,  48),
    ("kakao", "Kakao&#10;Map · Mobility",                 "img", "map",      920,  95,  48),
    ("alb",   "Application&#10;Load Balancer",            "aws", "alb",      430,  450, 76),
    ("ec1",   "EC2 · Spring Boot #1&#10;:9091",           "aws", "ec2",      690,  330, 76),
    ("ec2n",  "EC2 · Spring Boot #2&#10;:9092",           "aws", "ec2",      690,  590, 76),
    ("kafka", "Apache Kafka&#10;disaster-topic",          "img", "kafka",    950,  300, 64),
    ("cache", "Redis Cluster&#10;세션·캐시·Rate Limit",    "img", "redis",    950,  500, 64),
    ("fast",  "FastAPI&#10;scikit-learn 추론",            "img", "fastapi",  1210, 330, 64),
    ("rds",   "Oracle&#10;운영 DB",                        "img", "oracle",   1210, 530, 64),
]

# 엣지: src, tgt, label, kind('solid'|'dashed'|'thick'), bidir
edges = [
    ("user", "alb", "HTTPS · WebSocket", "solid", True),
    ("gov", "ec1", "1분 주기 수집", "dashed", False),
    ("alb", "ec1", "LB", "solid", False),
    ("alb", "ec2n", "", "solid", False),
    ("ec1", "kafka", "publish", "thick", False),
    ("kafka", "ec1", "UUID-A", "thick", False),
    ("kafka", "ec2n", "UUID-B", "thick", False),
    ("ec1", "cache", "", "solid", True),
    ("ec2n", "cache", "", "solid", True),
    ("ec1", "rds", "JDBC", "solid", False),
    ("ec1", "fast", "HTTP 추론", "solid", True),
    ("ec1", "kma", "HTTP", "solid", False),
    ("ec1", "kakao", "HTTP", "solid", False),
]

GROUP_PTS = ("points=[[0,0],[0.25,0],[0.5,0],[0.75,0],[1,0],[1,0.25],[1,0.5],[1,0.75],"
             "[1,1],[0.75,1],[0.5,1],[0.25,1],[0,1],[0,0.75],[0,0.5],[0,0.25]];")


def group_cell(gid, label, gricon, color, x, y, w, h):
    s = (GROUP_PTS + "outlineConnect=0;gradientColor=none;html=1;whiteSpace=wrap;fontSize=13;"
         "fontStyle=0;container=1;pointerEvents=0;collapsible=0;recursiveResize=0;"
         f"shape=mxgraph.aws4.group;grIcon=mxgraph.aws4.{gricon};strokeColor={color};"
         f"fillColor=none;verticalAlign=top;align=left;spacingLeft=30;fontColor={color};dashed=0;")
    return (f'<mxCell id="{gid}" value="{label}" style="{s}" vertex="1" parent="1">'
            f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry" /></mxCell>')


def node_cell(nid, label, kind, key, x, y, sz):
    if kind == "aws":
        res, color = AWS[key]
        s = ("sketch=0;outlineConnect=0;fontColor=#232F3E;gradientColor=none;"
             f"fillColor={color};strokeColor=none;dashed=0;verticalLabelPosition=bottom;"
             "verticalAlign=top;align=center;html=1;fontSize=12;aspect=fixed;"
             f"shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.{res};")
    else:
        s = ("shape=image;html=1;verticalLabelPosition=bottom;verticalAlign=top;"
             "labelPosition=center;align=center;whiteSpace=wrap;spacingTop=2;fontSize=12;"
             f"image={img_uri(key)};")
    return (f'<mxCell id="{nid}" value="{label}" style="{s}" vertex="1" parent="1">'
            f'<mxGeometry x="{x}" y="{y}" width="{sz}" height="{sz}" as="geometry" /></mxCell>')


def edge_cell(eid, src, tgt, label, kind, bidir):
    s = "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;fontSize=11;jettySize=auto;"
    if bidir:
        s += "startArrow=block;"
    if kind == "dashed":
        s += "dashed=1;strokeColor=#9673a6;fontColor=#777777;"
    elif kind == "thick":
        s += "strokeColor=#8C4FFF;strokeWidth=2.5;fontColor=#6a4a8a;fontStyle=1;"
    else:
        s += "strokeColor=#555555;"
    return (f'<mxCell id="e{eid}" value="{label}" style="{s}" edge="1" parent="1" '
            f'source="{src}" target="{tgt}"><mxGeometry relative="1" as="geometry" /></mxCell>')


title = ('<mxCell id="title" value="SafetyNevi · 운영(Prod) 인프라 아키텍처" '
         'style="text;html=1;fontSize=22;fontStyle=1;align=left;verticalAlign=middle;fontColor=#232F3E;" '
         'vertex="1" parent="1"><mxGeometry x="28" y="16" width="640" height="30" as="geometry" /></mxCell>')
subtitle = ('<mxCell id="subtitle" value="ALB 로드밸런싱 → Spring Boot 다중 인스턴스(EC2) · '
            'Kafka 고유 Consumer Group fan-out · Redis 세션·캐시 공유" '
            'style="text;html=1;fontSize=13;align=left;verticalAlign=middle;fontColor=#7f8c8d;" '
            'vertex="1" parent="1"><mxGeometry x="28" y="50" width="900" height="22" as="geometry" /></mxCell>')

# z-order: 그룹(뒤) → 엣지 → 노드(앞) → 제목
cells = [group_cell(*g) for g in groups]
cells += [edge_cell(i + 1, *e) for i, e in enumerate(edges)]
cells += [node_cell(*n) for n in nodes]
cells += [title, subtitle]

xml = ('<mxfile host="app.diagrams.net" type="device">\n'
       '  <diagram name="운영 아키텍처" id="arch">\n'
       '    <mxGraphModel dx="1500" dy="900" grid="0" gridSize="10" guides="1" tooltips="1" '
       'connect="1" arrows="1" fold="1" page="0" pageScale="1" pageWidth="1480" pageHeight="790" math="0" shadow="0">\n'
       '      <root>\n'
       '        <mxCell id="0" />\n'
       '        <mxCell id="1" parent="0" />\n'
       + "".join("        " + c + "\n" for c in cells)
       + '      </root>\n'
       '    </mxGraphModel>\n'
       '  </diagram>\n'
       '</mxfile>\n')

(DIA / "architecture.drawio").write_text(xml, encoding="utf-8")
print("written:", (DIA / "architecture.drawio"), "| nodes:", len(nodes), "edges:", len(edges))
