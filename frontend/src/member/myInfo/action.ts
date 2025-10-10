// 마이페이지 기능 모음
document.addEventListener('DOMContentLoaded', () => {

    // Regex & Utils
    const REGEX = {
        nickname: /^[가-힣a-zA-Z0-9]{2,10}$/,
        phone: /^010\d{8}$/,
        password: /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/
    };

    const setMsg = (elem: HTMLElement | null, isSuccess: boolean, text: string): void => {
        if (!elem) return;
        elem.className = isSuccess ? 'kb-val-msg success' : 'kb-val-msg error';
        elem.innerText = text;
    };

    // 내 정보 수정
    const infoForm = document.getElementById('info-update-form') as HTMLFormElement | null;
    const nickInput = document.getElementById('nickname') as HTMLInputElement;
    const phoneInput = document.getElementById('phone') as HTMLInputElement;

    nickInput?.addEventListener('input', function (this: HTMLInputElement) {
        const isValid = REGEX.nickname.test(this.value);
        setMsg(document.getElementById('nick-msg'), isValid, isValid ? "사용 가능" : "특수문자 제외 2~10자");
    });

    phoneInput?.addEventListener('input', function (this: HTMLInputElement) {
        this.value = this.value.replace(/[^0-9]/g, '');
        const isValid = REGEX.phone.test(this.value);
        setMsg(document.getElementById('phone-msg'), isValid, isValid ? "올바른 형식" : "010XXXXXXXX 형식");
    });

    infoForm?.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!REGEX.nickname.test(nickInput.value)) { nickInput.focus(); return alert("닉네임을 확인해주세요."); }
        if (!REGEX.phone.test(phoneInput.value)) { phoneInput.focus(); return alert("전화번호를 확인해주세요."); }

        const data = {
            nickname: nickInput.value,
            phone: phoneInput.value,
            address: (document.getElementById('address') as HTMLInputElement).value,
            detailAddress: (document.getElementById('detailAddress') as HTMLInputElement).value
        };

        try {
            const res = await fetch('/api/myinfo/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            if (res.ok) alert("정보가 수정되었습니다.");
            else throw new Error();
        } catch {
            alert("정보 수정에 실패했습니다.");
        }
    });

    // 주소 검색
    document.getElementById('search-addr-btn')?.addEventListener('click', () => {
        new daum.Postcode({
            oncomplete: (data: any) => {
                (document.getElementById('address') as HTMLInputElement).value = data.address;
                document.getElementById('detailAddress')?.focus();
            }
        }).open();
    });

    // 비밀번호 변경
    const pwForm = document.getElementById('pw-change-form') as HTMLFormElement | null;
    const newPw = document.getElementById('new-pw') as HTMLInputElement;
    const confirmPw = document.getElementById('confirm-pw') as HTMLInputElement;

    const checkPwMatch = (): void => {
        const matchMsg = document.getElementById('pw-match-msg');
        if (!confirmPw.value) { if (matchMsg) matchMsg.innerText = ''; return; }

        const isMatch = newPw.value === confirmPw.value;
        setMsg(matchMsg, isMatch, isMatch ? "일치합니다." : "일치하지 않습니다.");
    };

    newPw?.addEventListener('input', function (this: HTMLInputElement) {
        const isValid = REGEX.password.test(this.value);
        setMsg(document.getElementById('pw-msg'), isValid, isValid ? "사용 가능" : "8자 이상, 대문자/숫자/특수문자 포함");
        checkPwMatch();
    });

    confirmPw?.addEventListener('input', checkPwMatch);

    pwForm?.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (newPw.value !== confirmPw.value) return alert("새 비밀번호가 일치하지 않습니다.");
        if (!REGEX.password.test(newPw.value)) return alert("비밀번호 형식이 올바르지 않습니다.");

        const data = {
            currentPassword: (document.getElementById('current-pw') as HTMLInputElement).value,
            securityAnswer: (document.getElementById('security-answer') as HTMLInputElement).value,
            newPassword: newPw.value
        };

        try {
            const res = await fetch('/api/myinfo/change-pw', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg);
            }
            alert("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
            // CSRF 보호가 켜져 있어 로그아웃은 토큰을 실은 POST 폼으로 전송한다
            const logoutForm = document.createElement('form');
            logoutForm.method = 'POST';
            logoutForm.action = '/logout';
            const csrfMeta = document.querySelector('meta[name="_csrf"]');
            if (csrfMeta) {
                const tokenInput = document.createElement('input');
                tokenInput.type = 'hidden';
                tokenInput.name = '_csrf';
                tokenInput.value = csrfMeta.getAttribute('content') ?? '';
                logoutForm.appendChild(tokenInput);
            }
            document.body.appendChild(logoutForm);
            logoutForm.submit();

        } catch (err) {
            alert((err instanceof Error ? err.message : '') || "비밀번호 변경 실패");
        }
    });

    // 문의하기 (Toggle View)
    const inquiryForm = document.getElementById('inquiry-form') as HTMLFormElement | null;
    const listView = document.getElementById('inquiry-view-list');
    const writeView = document.getElementById('inquiry-view-write');

    document.querySelectorAll<HTMLElement>('.btn-toggle-view').forEach(btn => {
        btn.addEventListener('click', () => {
            const mode = btn.dataset.mode;
            if (mode === 'write') {
                if (listView) listView.style.display = 'none';
                if (writeView) writeView.style.display = 'block';
            } else {
                if (writeView) writeView.style.display = 'none';
                if (listView) listView.style.display = 'block';
                inquiryForm?.reset();
            }
        });
    });

    inquiryForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(inquiryForm);

        try {
            const res = await fetch('/inquiry/write', { method: 'POST', body: formData });
            if (res.ok) {
                alert("문의가 등록되었습니다.");
                location.reload();
            } else {
                alert("등록 실패");
            }
        } catch {
            alert("오류가 발생했습니다.");
        }
    });

    // 게시글 관리 (Event Delegation)
    const modal = document.getElementById('post-view-modal');
    const modalBody = document.getElementById('post-view-body');
    const closeModal = (): void => { if (modal) modal.style.display = 'none'; };

    document.getElementById('btn-modal-close')?.addEventListener('click', closeModal);

    const boardList = document.querySelector('.kb-board-list');

    boardList?.addEventListener('click', async (e) => {
        const targetEl = e.target as HTMLElement;

        // 삭제
        const delBtn = targetEl.closest('.btn-delete-post') as HTMLElement | null;
        if (delBtn) {
            e.stopPropagation();
            if (!confirm("정말로 삭제하시겠습니까?")) return;

            const boardId = delBtn.dataset.id;
            try {
                const res = await fetch(`/api/board/${boardId}`, { method: 'DELETE' });
                if (res.ok) { alert("삭제되었습니다."); location.reload(); }
            } catch { alert("삭제 중 오류 발생"); }
            return;
        }

        // 상세보기
        const item = targetEl.closest('.kb-board-item') as HTMLElement | null;
        if (item) {
            const boardId = item.dataset.id;
            try {
                const res = await fetch(`/api/board/${boardId}`);
                if (res.ok) {
                    const data = await res.json();
                    renderPostModal(data);
                }
            } catch (err) { console.error(err); }
        }
    });

    function renderPostModal(data: any): void {
        if (!modal || !modalBody) return;

        const badgeClass = data.category === '제보' ? 'badge-report' : (data.category === '질문' ? 'badge-qna' : 'badge-talk');

        const commentsHtml = data.comments?.length
            ? data.comments.map((c: any) =>
                `<li class="kb-post-comment-item">
                    <span class="writer">${c.writer}</span>
                    <span>${c.content}</span>
                </li>`).join('')
            : '<li style="text-align:center; color:#999; padding:10px;">댓글이 없습니다.</li>';

        modalBody.innerHTML = `
            <div class="kb-post-header">
                <span class="kb-badge ${badgeClass}">${data.category}</span>
                <span class="kb-date">${data.date}</span>
            </div>
            <h3 class="kb-post-title">${data.title}</h3>
            ${data.imageUrl ? `<img src="${data.imageUrl}" class="kb-post-img" alt="image">` : ''}
            <div class="kb-post-content">${data.content}</div>

            <div class="kb-comments-section">
                <h6>댓글 (${data.comments?.length || 0})</h6>
                <ul class="kb-comment-list">${commentsHtml}</ul>
            </div>
        `;
        modal.style.display = 'flex';
    }

    // 회원 탈퇴
    document.getElementById('withdrawal-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const agree = document.getElementById('withdrawal-agree') as HTMLInputElement;
        const pw = document.getElementById('withdrawal-pw') as HTMLInputElement;

        if (!agree.checked) return alert("안내 사항에 동의해주세요.");
        if (!pw.value) return alert("비밀번호를 입력해주세요.");
        if (!confirm("정말 탈퇴하시겠습니까? (복구 불가)")) return;

        try {
            const res = await fetch('/api/member/withdraw', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password: pw.value })
            });

            if (res.ok) {
                alert("탈퇴 처리가 완료되었습니다.");
                window.location.href = "/";
            } else {
                alert("비밀번호가 일치하지 않거나 오류가 발생했습니다.");
            }
        } catch {
            alert("서버 오류가 발생했습니다.");
        }
    });
});
