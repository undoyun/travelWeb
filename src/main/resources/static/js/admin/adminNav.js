function toggleMenu() {
  const dropdown = document.getElementById("dropdownMenu");
  if (dropdown) {
    dropdown.classList.toggle("show");
  }
}

// 페이지 로드 시 hover 이벤트 추가
document.addEventListener("DOMContentLoaded", function () {
  const userMenu = document.querySelector(".user-menu");
  const dropdown = document.getElementById("dropdownMenu");

  if (userMenu && dropdown) {
    // 마우스 오버 시 드롭다운 표시
    userMenu.addEventListener("mouseenter", function () {
      dropdown.classList.add("show");
    });

    // 마우스가 떠날 때 드롭다운 숨김
    userMenu.addEventListener("mouseleave", function () {
      dropdown.classList.remove("show");
    });
  }
});
