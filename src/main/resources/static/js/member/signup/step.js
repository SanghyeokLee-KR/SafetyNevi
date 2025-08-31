// 회원가입 단계(Step) 제어 및 최종 가입 요청 처리
document.addEventListener('DOMContentLoaded', () => {

    // 단계별 요소 및 버튼
    const steps = {
        1: document.getElementById('step-1'),
        2: document.getElementById('step-2'),
        3: document.getElementById('step-3')
    };
    const dots = {
        1: document.getElementById('dot-1'),
        2: document.getElementById('dot-2'),
        3: document.getElementById('dot-3')
    };
    const title = document.getElementById('page-title');

    // 약관 동의 관련
    const checkAll = document.getElementById('agree_all');
    const checkRequired = document.getElementById('agreement_required');
    const checkLocation = document.getElementById('location_agreement');
    const btnNext1 = document.getElementById('btn-step1-next');

    // 1. 약관 동의 상태 업데이트
    const updateAgreementState = () => {
        const isAllChecked = checkRequired.checked && checkLocation.checked;

        if (checkAll) checkAll.checked = isAllChecked;

        btnNext1.disabled = !isAllChecked;
        btnNext1.innerText = isAllChecked ? "다음 단계로" : "약관에 모두 동의해주세요";
    };

    // 전역 함수로 등록 (모달에서 호출하기 위함)
    window.updateAgreementState = updateAgreementState;

    // 체크박스 이벤트 바인딩
    checkAll?.addEventListener('change', (e) => {
        const checked = e.target.checked;
        checkRequired.checked = checked;
        checkLocation.checked = checked;
        updateAgreementState();
    });

    [checkRequired, checkLocation].forEach(el => {
        el?.addEventListener('change', updateAgreementState);
    });

    // 2. 단계 이동 헬퍼 함수
    const moveStep = (current, next, titleText) => {
        steps[current].classList.add('kb-hidden');
        steps[next].classList.remove('kb-hidden');
        steps[next].classList.add('fade-in');

        dots[current].classList.remove('active');
        dots[next].classList.add('active');

        if (title) title.innerText = titleText;
    };

    // 버튼 이벤트 리스너
    document.getElementById('btn-step1-next')?.addEventListener('click', () =>
        moveStep(1, 2, "계정 정보를 입력해주세요"));

    document.getElementById('btn-step2-prev')?.addEventListener('click', () =>
        moveStep(2, 1, "서비스 이용 약관에 동의해주세요"));

    document.getElementById('btn-step3-prev')?.addEventListener('click', () =>
        moveStep(3, 2, "계정 정보를 입력해주세요"));

    // Step 2 -> 3 이동 시 유효성 검사
    document.getElementById('btn-step2-next')?.addEventListener('click', () => {
        const idInput = document.getElementById('user_id');
        const emailInput = document.getElementById('email');
        const pwInput = document.getElementById('password');
        const pwConfirm = document.getElementById('password-confirm');

        // 빈 값 체크
        if (!idInput.value || !emailInput.value || !pwInput.value || !pwConfirm.value) {
            alert("필수 정보를 모두 입력해주세요.");
            return;
        }

        // 유효성(valid 클래스) 체크
        if (!idInput.classList.contains('valid')) {
            alert("아이디 중복 확인을 완료해주세요.");
            idInput.focus(); return;
        }
        if (!emailInput.classList.contains('valid')) {
            alert("이메일 중복 확인을 완료해주세요.");
            emailInput.focus(); return;
        }
        if (!pwInput.classList.contains('valid') || !pwConfirm.classList.contains('valid')) {
            alert("비밀번호 조건을 다시 확인해주세요.");
            pwInput.focus(); return;
        }

        moveStep(2, 3, "프로필 정보를 입력해주세요");
    });

    // 3. 최종 가입 요청 (async/await 적용)
    document.getElementById('signup-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();

        const formData = {
            userId: document.getElementById('user_id').value,
            email: document.getElementById('email').value,
            password: document.getElementById('password').value,
            name: document.getElementById('name').value,
            nickname: document.getElementById('nickname').value,
            address: document.getElementById('address').value,
            detailAddress: document.getElementById('detailAddress').value,
            areaName: document.getElementById('areaName').value,
            latitude: parseFloat(document.getElementById('lat').value) || null,
            longitude: parseFloat(document.getElementById('lon').value) || null,
            emergencyPhone: document.getElementById('emergency_contact').value,
            pwQuestion: parseInt(document.getElementById('pw_question').value),
            pwAnswer: document.getElementById('pw_answer').value
        };

        try {
            const response = await fetch('/signup', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                const errorMsg = await response.text();
                throw new Error(errorMsg || '회원가입 처리 중 오류가 발생했습니다.');
            }

            alert("회원가입이 완료되었습니다! 🎉\n로그인 페이지로 이동합니다.");
            window.location.href = "/login";

        } catch (error) {
            console.error('Signup Error:', error);
            alert(`가입 실패: ${error.message}`);
        }
    });
});