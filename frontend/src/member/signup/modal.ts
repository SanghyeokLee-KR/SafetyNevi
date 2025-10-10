// 약관 및 위치정보 동의 모달 제어
document.addEventListener('DOMContentLoaded', () => {

    interface ModalConfig {
        modalId: string; checkboxId: string; openBtnId: string;
        closeXId: string; closeNoId: string; closeYesId: string;
    }

    // 모달 초기화 함수 (설정 객체 사용)
    const initModal = (config: ModalConfig): void => {
        const modal = document.getElementById(config.modalId);
        const checkbox = document.getElementById(config.checkboxId) as HTMLInputElement | null;
        const openBtn = document.getElementById(config.openBtnId);

        if (!modal || !checkbox || !openBtn) return;

        // Open
        openBtn.addEventListener('click', (e) => {
            e.preventDefault();
            modal.style.display = 'flex';
        });

        // Close Helper
        const closeModal = (): void => { modal.style.display = 'none'; };

        // X Button
        document.getElementById(config.closeXId)?.addEventListener('click', closeModal);

        // '동의하지 않음' Button
        document.getElementById(config.closeNoId)?.addEventListener('click', (e) => {
            e.preventDefault();
            closeModal();
            checkbox.checked = false;
            window.updateAgreementState?.(); // step.js의 상태 업데이트 호출
        });

        // '동의함' Button
        document.getElementById(config.closeYesId)?.addEventListener('click', (e) => {
            e.preventDefault();
            closeModal();
            checkbox.checked = true;
            window.updateAgreementState?.();
        });
    };

    // 서비스 이용약관 모달 연결
    initModal({
        openBtnId: 'open-modal-btn',
        modalId: 'agreement-modal',
        closeXId: 'close-modal-x',
        closeNoId: 'close-modal-no',
        closeYesId: 'close-modal-yes',
        checkboxId: 'agreement_required'
    });

    // 위치정보 이용약관 모달 연결
    initModal({
        openBtnId: 'open-loc-modal-btn',
        modalId: 'location-modal',
        closeXId: 'close-loc-modal-x',
        closeNoId: 'close-loc-modal-no',
        closeYesId: 'close-loc-modal-yes',
        checkboxId: 'location_agreement'
    });
});
