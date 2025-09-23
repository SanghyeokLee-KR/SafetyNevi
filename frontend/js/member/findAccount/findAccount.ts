// 계정 찾기 프로세스
document.addEventListener('DOMContentLoaded', () => {

    const QUESTIONS = {
        1: "인생 좌우명?",
        2: "보물 1호?",
        3: "기억에 남는 선생님?",
        4: "졸업한 초등학교?",
        5: "다시 태어나면 되고싶은 것?"
    };

    let currentUserId = '';

    // DOM Elements
    const step1 = document.getElementById('step-1');
    const step2 = document.getElementById('step-2');
    const step3 = document.getElementById('step-3');

    const nextStep = (curr, next) => {
        curr.classList.add('hidden-step');
        next.classList.remove('hidden-step');
        next.classList.add('fade-in');
    };

    // Step 1: 아이디/이메일 조회
    document.getElementById('btn-step1')?.addEventListener('click', async () => {
        const userId = document.getElementById('find_id').value;
        const email = document.getElementById('find_email').value;

        if (!userId || !email) return alert("정보를 모두 입력해주세요.");

        try {
            const res = await fetch('/api/find/question', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ userId, email })
            });

            if (!res.ok) throw new Error("User Not Found");

            const data = await res.json();
            currentUserId = userId;

            document.getElementById('question-display').innerText = `Q. ${QUESTIONS[data.question]}`;
            nextStep(step1, step2);

        } catch (e) {
            alert("일치하는 회원 정보가 없습니다.");
        }
    });

    // Step 2: 본인확인 질문 검증
    document.getElementById('btn-step2')?.addEventListener('click', async () => {
        const answer = document.getElementById('find_answer').value;
        if (!answer) return alert("답변을 입력해주세요.");

        try {
            const res = await fetch('/api/find/verify', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ userId: currentUserId, answer })
            });

            if (!res.ok) throw new Error("Wrong Answer");

            alert("본인 인증에 성공했습니다.\n새로운 비밀번호를 설정해주세요.");
            nextStep(step2, step3);

        } catch (e) {
            alert("답변이 일치하지 않습니다. 다시 확인해주세요.");
        }
    });

    // Step 3: 비밀번호 재설정
    const newPwInput = document.getElementById('new_pw');
    const confirmPwInput = document.getElementById('new_pw_confirm');
    const matchMsg = document.getElementById('pw-match-msg');

    const isValidPassword = (pw) => {
        return /^(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(pw);
    };

    const checkMatch = () => {
        const pw = newPwInput.value;
        const confirm = confirmPwInput.value;

        if (!confirm) {
            confirmPwInput.classList.remove('valid', 'invalid');
            matchMsg.innerText = "";
            return;
        }

        const isMatch = (pw === confirm);
        confirmPwInput.classList.toggle('valid', isMatch);
        confirmPwInput.classList.toggle('invalid', !isMatch);

        matchMsg.className = isMatch ? 'kb-input-msg success' : 'kb-input-msg error';
        matchMsg.innerText = isMatch ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다.";
    };

    newPwInput?.addEventListener('input', checkMatch);
    confirmPwInput?.addEventListener('input', checkMatch);

    document.getElementById('btn-step3')?.addEventListener('click', async () => {
        const pw = newPwInput.value;
        const confirm = confirmPwInput.value;

        if (!isValidPassword(pw)) {
            alert("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 반드시 포함해야 합니다.");
            newPwInput.focus(); return;
        }
        if (pw !== confirm) {
            alert("비밀번호가 일치하지 않습니다.");
            confirmPwInput.focus(); return;
        }

        try {
            const res = await fetch('/api/find/reset', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ userId: currentUserId, password: pw })
            });

            if (!res.ok) throw new Error("Reset Failed");

            alert("비밀번호가 성공적으로 변경되었습니다. 🎉\n로그인 페이지로 이동합니다.");
            window.location.href = "/login";

        } catch (e) {
            alert("비밀번호 변경 중 시스템 오류가 발생했습니다.");
        }
    });
});