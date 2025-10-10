/* 외부 SDK·전역 객체 — 점진적 타이핑을 위해 느슨하게 선언.
   (Kakao Maps / Daum Postcode / SockJS / Stomp / Chart.js / jQuery) */
declare const kakao: any;
declare const daum: any;
declare const SockJS: any;
declare const Stomp: any;
declare const Chart: any;
declare const $: any;
declare const jQuery: any;

interface Window {
    [key: string]: any;
}

// csrf.ts가 XHR 인스턴스에 붙이는 내부 플래그
interface XMLHttpRequest {
    __csrfNeeded?: boolean;
}
