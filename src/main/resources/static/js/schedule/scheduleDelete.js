document.addEventListener('DOMContentLoaded', function() {
    // 삭제 모달 관련 요소
    const deleteModal = document.getElementById('deleteModal');
    const btnDeleteSchedule = document.getElementById('btnDeleteSchedule');
    const closeDeleteModal = document.getElementById('closeDeleteModal');
    const btnCancelDelete = document.getElementById('btnCancelDelete');
    const deleteForm = document.querySelector('#deleteModal form');

    // 삭제 모달 열기
    btnDeleteSchedule.addEventListener('click', () => {
        deleteModal.style.display = 'block';
    });

    // 삭제 모달 닫기
    function closeDeleteModalHandler() {
        deleteModal.style.display = 'none';
    }

    closeDeleteModal.addEventListener('click', closeDeleteModalHandler);
    btnCancelDelete.addEventListener('click', closeDeleteModalHandler);

    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', (event) => {
        if (event.target === deleteModal) {
            closeDeleteModalHandler();
        }
    });

    // 삭제 폼 제출 시 처리
    if (deleteForm) {
        deleteForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // 폼을 직접 제출
            deleteForm.submit();
        });
    }
}); 