// HTML 이스케이프 공용 유틸, innerHTML에 사용자/서버 데이터를 꽂기 전에 거른다
const ENTITIES: Record<string, string> = {
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
};

// & < > " ' 를 엔티티로 치환. null/undefined도 안전하게 빈 문자열로
export function escapeHtml(v: any): string {
    if (v === null || v === undefined) return "";
    return String(v).replace(/[&<>"']/g, s => ENTITIES[s] ?? s);
}
