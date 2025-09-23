// 관리자 신고 관리
document.addEventListener('DOMContentLoaded', () => {

    // Toast UI
    const Toast = {
        show: (msg, isError = false) => {
            let t = document.getElementById('admin-toast');
            if (!t) {
                t = document.createElement('div');
                t.id = 'admin-toast';
                Object.assign(t.style, {
                    position: 'fixed', left: '50%', bottom: '40px', transform: 'translateX(-50%)',
                    padding: '10px 16px', borderRadius: '6px', boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
                    zIndex: 99999, fontSize: '13px', transition: 'opacity 0.3s', color: '#fff'
                });
                document.body.appendChild(t);
            }
            t.innerText = msg;
            t.style.background = isError ? '#dc2626' : '#0f172a';
            t.style.opacity = '1';
            setTimeout(() => t.style.opacity = '0', 2200);
        }
    };

    const escapeHtml = (str) => {
        if (!str) return "";
        return String(str).replace(/[&<>"'`=/]/g, s => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;", "/": "&#x2F;"
        }[s]));
    };

    // Modal Logic
    const modal = document.getElementById('postModal');
    const modalContent = document.getElementById('modal-content');

    const closeModal = () => { if(modal) modal.style.display = 'none'; };

    document.getElementById('btn-modal-close')?.addEventListener('click', closeModal);
    modal?.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    const openPostModal = async (boardId) => {
        if (!modal || !modalContent) return;

        modal.style.display = 'flex';
        modalContent.innerHTML = `<div style="color:#6b7280; padding:20px;">데이터를 불러오는 중...</div>`;

        try {
            const res = await fetch(`/api/admin/board/${boardId}`);
            if (!res.ok) throw new Error("게시글 로드 실패");

            const data = await res.json();
            renderModalContent(data);
        } catch (e) {
            modalContent.innerHTML = `<div style="color:#dc2626; padding:20px;">불러오기 실패: ${e.message}</div>`;
            Toast.show("게시글 정보를 가져오지 못했습니다.", true);
        }
    };

    const renderModalContent = (b) => {
        const imageHtml = b.imageUrl ? `<img src="${escapeHtml(b.imageUrl)}" style="max-width:100%; margin-top:10px; border-radius:4px;">` : '';
        const locHtml = b.locationType ? `<div class="meta" style="margin-top:5px; color:#666; font-size:12px;">📍 위치유형: ${escapeHtml(b.locationType)}</div>` : '';

        modalContent.innerHTML = `
            <div>
                <div class="title" style="font-size:18px; font-weight:bold; margin-bottom:5px;">${escapeHtml(b.title)}</div>
                <div class="meta" style="font-size:13px; color:#888; margin-bottom:15px;">
                    작성자: ${escapeHtml(b.writer)} · ${escapeHtml(b.createdAt)}
                </div>
                <div class="body" style="line-height:1.6; color:#333;">${escapeHtml(b.content)}</div>
                ${imageHtml}
                ${locHtml}
                <div style="margin-top:20px; text-align:right;">
                    <button class="btn-action-sm" id="internal-close-btn">닫기</button>
                </div>
            </div>
        `;
        document.getElementById('internal-close-btn').addEventListener('click', closeModal);
    };

    // Update Status
    const updateStatus = async (reportId, newStatus) => {
        if (!confirm("상태를 변경하시겠습니까?")) return;

        try {
            const res = await fetch(`/api/admin/reports/${reportId}/status`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ status: newStatus })
            });

            if (!res.ok) throw new Error();

            Toast.show("처리되었습니다.");
            setTimeout(() => location.reload(), 600);
        } catch {
            Toast.show("처리 중 오류가 발생했습니다.", true);
        }
    };

    // Event Delegation (Table)
    const reportTable = document.querySelector('.kb-table-reports');

    reportTable?.addEventListener('click', (e) => {
        const target = e.target;

        if (target.classList.contains('btn-view-post')) {
            const id = target.dataset.id;
            openPostModal(id);
        }

        if (target.classList.contains('btn-update-status')) {
            const id = target.dataset.id;
            const status = target.dataset.status;
            updateStatus(id, status);
        }
    });
});