// 입력값 실시간 유효성 검증 (중복확인 및 정규식 체크)
document.addEventListener('DOMContentLoaded', () => {

    // UI 상태 변경 헬퍼
    const setStatus = (input, msgElem, isValid, message) => {
        if (!msgElem) return;
        msgElem.style.display = 'block';
        msgElem.innerText = message;

        if (isValid) {
            input.classList.add('valid');
            input.classList.remove('invalid');
            msgElem.className = 'kb-input-msg success';
        } else {
            input.classList.add('invalid');
            input.classList.remove('valid');
            msgElem.className = 'kb-input-msg error';
        }
    };

    // 공통 API 중복 확인 함수
    async function checkDuplicate(url, input, msgElem, successMsg, failMsg) {
        try {
            const res = await fetch(url);
            const data = await res.json();

            if (data.available) setStatus(input, msgElem, true, successMsg);
            else setStatus(input, msgElem, false, failMsg);
        } catch (err) {
            console.error(err);
        }
    }

    // 초기화 이벤트 등록
    const resetListener = (input, msgElem) => {
        input?.addEventListener('input', () => {
            input.classList.remove('valid', 'invalid');
            if(msgElem) msgElem.style.display = 'none';
        });
    };

    /* 1. 아이디 체크 */
    const idInput = document.getElementById('user_id');
    const idMsg = document.getElementById('id-msg');
    resetListener(idInput, idMsg);

    document.getElementById('check-id-btn')?.addEventListener('click', () => {
        const val = idInput.value;
        if (!/^[A-Za-z0-9]{4,12}$/.test(val)) {
            alert("아이디는 영문/숫자 4~12자로 입력해주세요.");
            idInput.focus(); return;
        }
        checkDuplicate(`/api/check/id?userId=${encodeURIComponent(val)}`, idInput, idMsg, '사용 가능한 아이디입니다.', '이미 사용 중인 아이디입니다.');
    });

    /* 2. 이메일 체크 */
    const emailInput = document.getElementById('email');
    const emailMsg = document.getElementById('email-msg');
    resetListener(emailInput, emailMsg);

    document.getElementById('check-email-btn')?.addEventListener('click', () => {
        const val = emailInput.value;
        // 주요 포털 도메인 체크
        if (!/^[\w.+-]+@(naver\.com|gmail\.com|kakao\.com|daum\.net)$/.test(val)) {
            alert("네이버, 구글, 카카오, 다음 이메일만 사용 가능합니다.");
            emailInput.focus(); return;
        }
        checkDuplicate(`/api/check/email?email=${encodeURIComponent(val)}`, emailInput, emailMsg, '사용 가능한 이메일입니다.', '이미 가입된 이메일입니다.');
    });

    /* 3. 닉네임 체크 */
    const nickInput = document.getElementById('nickname');
    const nickMsg = document.getElementById('nick-msg');
    resetListener(nickInput, nickMsg);

    document.getElementById('check-nick-btn')?.addEventListener('click', () => {
        const val = nickInput.value;
        if (!/^[가-힣a-zA-Z0-9]{2,10}$/.test(val)) {
            alert("닉네임은 특수문자 제외 2~10자로 입력해주세요.");
            nickInput.focus(); return;
        }
        checkDuplicate(`/api/check/nickname?nickname=${encodeURIComponent(val)}`, nickInput, nickMsg, '사용 가능한 닉네임입니다.', '이미 사용 중인 닉네임입니다.');
    });

    /* 4. 이름 체크 (API X, 정규식 O) */
    const nameInput = document.getElementById('name');
    const nameMsg = document.getElementById('name-msg');

    nameInput?.addEventListener('input', function() {
        const val = this.value.trim();
        if(!val) {
            this.classList.remove('valid', 'invalid');
            if(nameMsg) nameMsg.style.display = 'none';
            return;
        }
        const isValid = /^[가-힣a-zA-Z]{2,20}$/.test(val);
        setStatus(this, nameMsg, isValid, isValid ? '올바른 이름 형식입니다.' : '이름은 한글/영문 2자 이상입니다.');
    });

    /* 5. 휴대전화 번호 체크 */
    const phoneInput = document.getElementById('emergency_contact');
    const phoneMsg = document.getElementById('phone-msg');

    phoneInput?.addEventListener('input', function() {
        this.value = this.value.replace(/[^0-9]/g, ''); // 숫자만 허용
        const val = this.value;

        if(!val) {
            this.classList.remove('valid', 'invalid');
            if(phoneMsg) phoneMsg.style.display = 'none';
            return;
        }

        const isValid = /^010\d{8}$/.test(val);
        setStatus(this, phoneMsg, isValid, isValid ? '올바른 전화번호입니다.' : '010으로 시작하는 11자리 숫자여야 합니다.');
    });

    /* 6. 비밀번호 로직 */
    const pwInput = document.getElementById('password');
    const pwConfirm = document.getElementById('password-confirm');
    const pwMatchMsg = document.getElementById('pw-match-msg');
    const pwBar = document.getElementById('pw-meter-bar');

    // 비밀번호 강도 시각화
    const updateMeter = (val) => {
        if(!pwBar) return;
        if(!val) { pwBar.style.width = '0%'; return; }

        let score = 0;
        if(val.length >= 8) score++;
        if(/[A-Za-z]/.test(val) && /[0-9]/.test(val)) score++;
        if(/[^A-Za-z0-9]/.test(val)) score++;

        if(val.length < 8) {
            pwBar.style.width = '20%'; pwBar.style.backgroundColor = '#dc3545';
        } else if(score < 3) {
            pwBar.style.width = '50%'; pwBar.style.backgroundColor = '#ffc107';
        } else {
            pwBar.style.width = '100%'; pwBar.style.backgroundColor = '#28a745';
        }
    };

    const checkPwMatch = () => {
        if (!pwConfirm || !pwConfirm.value) {
            if(pwMatchMsg) pwMatchMsg.innerText = '';
            pwConfirm?.classList.remove('valid', 'invalid');
            return;
        }
        const isMatch = (pwInput.value === pwConfirm.value) && pwInput.classList.contains('valid');
        setStatus(pwConfirm, pwMatchMsg, isMatch, isMatch ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.');
    };

    pwInput?.addEventListener('input', function() {
        const val = this.value;
        updateMeter(val);

        // 유효성: 8자 이상 + (영문/숫자/특수문자 중 2종 이상)
        let typeCnt = 0;
        if(/[A-Za-z]/.test(val)) typeCnt++;
        if(/[0-9]/.test(val)) typeCnt++;
        if(/[^A-Za-z0-9]/.test(val)) typeCnt++;

        const isValid = val.length >= 8 && typeCnt >= 2;

        if(isValid) {
            this.classList.add('valid');
            this.classList.remove('invalid');
        } else {
            this.classList.add('invalid');
            this.classList.remove('valid');
        }
        checkPwMatch();
    });

    pwConfirm?.addEventListener('input', checkPwMatch);
});