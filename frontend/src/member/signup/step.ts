// 회원가입 단계(Step) 제어 및 최종 가입 요청 처리
document.addEventListener('DOMContentLoaded', () => {

    const steps: Record<number, HTMLElement | null> = {
        1: document.getElementById('step-1'),
        2: document.getElementById('step-2'),
        3: document.getElementById('step-3')
    };
    const dots: Record<number, HTMLElement | null> = {
        1: document.getElementById('dot-1'),
        2: document.getElementById('dot-2'),
        3: document.getElementById('dot-3')
    };
    const title = document.getElementById('page-title');

    const checkAll = document.getElementById('agree_all') as HTMLInputElement;
    const checkRequired = document.getElementById('agreement_required') as HTMLInputElement;
    const checkLocation = document.getElementById('location_agreement') as HTMLInputElement;
    const btnNext1 = document.getElementById('btn-step1-next') as HTMLButtonElement;

    const updateAgreementState = (): void => {
        const isAllChecked = checkRequired.checked && checkLocation.checked;

        if (checkAll) checkAll.checked = isAllChecked;

        btnNext1.disabled = !isAllChecked;
        btnNext1.innerText = isAllChecked ? "다음 단계로" : "약관에 모두 동의해주세요";
    };

    // 약관 모달(modal.ts)에서 호출할 수 있도록 전역에 노출
    window.updateAgreementState = updateAgreementState;

    checkAll?.addEventListener('change', (e) => {
        const checked = (e.target as HTMLInputElement).checked;
        checkRequired.checked = checked;
        checkLocation.checked = checked;
        updateAgreementState();
    });

    [checkRequired, checkLocation].forEach(el => {
        el?.addEventListener('change', updateAgreementState);
    });

    const moveStep = (current: number, next: number, titleText: string): void => {
        steps[current]?.classList.add('kb-hidden');
        steps[next]?.classList.remove('kb-hidden');
        steps[next]?.classList.add('fade-in');

        dots[current]?.classList.remove('active');
        dots[next]?.classList.add('active');

        if (title) title.innerText = titleText;
    };

    document.getElementById('btn-step1-next')?.addEventListener('click', () =>
        moveStep(1, 2, "계정 정보를 입력해주세요"));

    document.getElementById('btn-step2-prev')?.addEventListener('click', () =>
        moveStep(2, 1, "서비스 이용 약관에 동의해주세요"));

    document.getElementById('btn-step3-prev')?.addEventListener('click', () =>
        moveStep(3, 2, "계정 정보를 입력해주세요"));

    // 다음 단계로 넘어가기 전 계정 정보 입력값 검증
    document.getElementById('btn-step2-next')?.addEventListener('click', () => {
        const idInput = document.getElementById('user_id') as HTMLInputElement;
        const emailInput = document.getElementById('email') as HTMLInputElement;
        const pwInput = document.getElementById('password') as HTMLInputElement;
        const pwConfirm = document.getElementById('password-confirm') as HTMLInputElement;

        if (!idInput.value || !emailInput.value || !pwInput.value || !pwConfirm.value) {
            alert("필수 정보를 모두 입력해주세요.");
            return;
        }

        // validation.ts가 통과 시 붙여둔 'valid' 클래스로 중복확인까지 끝났는지 판단
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

    // 최종 가입 요청
    document.getElementById('signup-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();

        const gv = (id: string): string => (document.getElementById(id) as HTMLInputElement).value;
        const formData = {
            userId: gv('user_id'),
            email: gv('email'),
            password: gv('password'),
            name: gv('name'),
            nickname: gv('nickname'),
            address: gv('address'),
            detailAddress: gv('detailAddress'),
            areaName: gv('areaName'),
            latitude: parseFloat(gv('lat')) || null,
            longitude: parseFloat(gv('lon')) || null,
            emergencyPhone: gv('emergency_contact'),
            pwQuestion: parseInt(gv('pw_question')),
            pwAnswer: gv('pw_answer')
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

            alert("회원가입이 완료되었습니다!\n로그인 페이지로 이동합니다.");
            window.location.href = "/login";

        } catch (error) {
            console.error('Signup Error:', error);
            alert(`가입 실패: ${error instanceof Error ? error.message : '알 수 없는 오류'}`);
        }
    });
});
