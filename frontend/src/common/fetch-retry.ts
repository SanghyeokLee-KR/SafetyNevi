// 일시적 실패(서버 콜드 스타트·재시작·짧은 네트워크 끊김)에 견디는 GET 재시도.
// res.ok면 Response 반환, 비정상 응답·예외는 백오프 후 재시도하고, 끝까지 실패하면 마지막 오류를 throw.
export async function fetchRetry(url: string, tries = 3, baseDelay = 600): Promise<Response> {
    let lastErr: any;
    for (let i = 0; i < tries; i++) {
        try {
            const res = await fetch(url);
            if (res.ok) return res;
            lastErr = new Error("status " + res.status);
        } catch (e) {
            lastErr = e;
        }
        if (i < tries - 1) {
            await new Promise(resolve => setTimeout(resolve, baseDelay * (i + 1)));
        }
    }
    throw lastErr;
}
