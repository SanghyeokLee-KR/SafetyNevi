// 약관 및 위치정보 동의 모달 제어
document.addEventListener('DOMContentLoaded', () => {

    interface ModalConfig {
        modalId: string; checkboxId: string; openBtnId: string;
        closeXId: string; closeNoId: string; closeYesId: string;
    }

    const initModal = (config: ModalConfig): void => {
        const modal = document.getElementById(config.modalId);
        const checkbox = document.getElementById(config.checkboxId) as HTMLInputElement | null;
        const openBtn = document.getElementById(config.openBtnId);

        if (!modal || !checkbox || !openBtn) return;

        openBtn.addEventListener('click', (e) => {
            e.preventDefault();
            modal.style.display = 'flex';
        });

        const closeModal = (): void => { modal.style.display = 'none'; };

        document.getElementById(config.closeXId)?.addEventListener('click', closeModal);

        document.getElementById(config.closeNoId)?.addEventListener('click', (e) => {
            e.preventDefault();
            closeModal();
            checkbox.checked = false;
            window.updateAgreementState?.(); // step.ts에 등록된 전역 상태 갱신 호출
        });

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
