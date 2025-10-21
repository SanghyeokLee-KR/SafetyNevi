// 마이페이지 탭(메뉴) 전환 제어
document.addEventListener('DOMContentLoaded', () => {
    const menuItems = document.querySelectorAll<HTMLElement>('.kb-menu-item');
    const sections = document.querySelectorAll<HTMLElement>('.kb-content-section');

    const switchTab = (targetId: string): void => {
        sections.forEach(sec => sec.style.display = 'none');
        menuItems.forEach(item => item.classList.remove('kb-active'));

        const targetSection = document.getElementById(`${targetId}-section`);
        const targetMenu = document.querySelector(`.kb-menu-item[data-target="${targetId}"]`);

        if (targetSection) targetSection.style.display = 'block';
        if (targetMenu) targetMenu.classList.add('kb-active');
    };

    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const target = item.getAttribute('data-target');
            if (!target) return;
            switchTab(target);

            // URL 해시 변경 (새로고침 해도 탭 유지용)
            history.replaceState(null, '', `#${target}`);
        });
    });

    // 초기 로드 시 URL 해시 확인 (예: /mypage#password)
    const hash = window.location.hash.substring(1);
    if (hash) switchTab(hash);
});
