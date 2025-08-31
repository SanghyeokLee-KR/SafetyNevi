// 회원 강제 탈퇴 처리
function deleteMember(userId) {
    if (!confirm(`[${userId}] 회원을 정말 강제 탈퇴시키겠습니까?\n(작성한 게시글, 댓글 등 모든 데이터가 영구 삭제됩니다)`)) {
        return;
    }

    fetch('/api/admin/member/' + userId, {
        method: 'DELETE'
    })
        .then(res => {
            if (res.ok) {
                alert("회원이 정상적으로 탈퇴 처리되었습니다.");
                location.reload();
            } else {
                res.text().then(msg => alert("삭제 실패: " + msg));
            }
        })
        .catch(err => {
            console.error(err);
            alert("서버 통신 오류가 발생했습니다.");
        });
}