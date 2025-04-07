// 드롭다운 메뉴 토글 함수
function toggleMenu() {
  const dropdownMenu = document.getElementById("dropdownMenu");
  dropdownMenu.style.display =
    dropdownMenu.style.display === "block" ? "none" : "block";
}

document.addEventListener("DOMContentLoaded", function () {
  // 현재 페이지에 해당하는 네비게이션 링크에 active 클래스 추가
  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll(".nav-item");

  navLinks.forEach((link) => {
    const href = link.getAttribute("href");
    if (href && currentPath.includes(href.replace("@{", "").replace("}", ""))) {
      link.classList.add("active");
    }
  });

  // 드롭다운 메뉴 처리
  const userIcon = document.querySelector(".user-icon");
  const dropdownMenu = document.getElementById("dropdownMenu");

  if (userIcon && dropdownMenu) {
    let hideTimeout;

    userIcon.addEventListener("mouseenter", function () {
      clearTimeout(hideTimeout); // 숨기는 타이머 제거
      dropdownMenu.style.display = "block"; // 드롭다운 표시
    });

    userIcon.addEventListener("mouseleave", function () {
      hideTimeout = setTimeout(function () {
        if (!dropdownMenu.matches(":hover")) {
          dropdownMenu.style.display = "none";
        }
      }, 300); // 0.3초의 지연 후 숨김 처리
    });

    dropdownMenu.addEventListener("mouseenter", function () {
      clearTimeout(hideTimeout); // 마우스가 메뉴 안으로 들어오면 숨김 방지
    });

    dropdownMenu.addEventListener("mouseleave", function () {
      hideTimeout = setTimeout(function () {
        dropdownMenu.style.display = "none";
      }, 300); // 0.3초 후 숨김 처리
    });
  }
});

// 드롭다운 메뉴 토글 함수
function toggleMenu() {
  const dropdownMenu = document.getElementById("dropdownMenu");
  dropdownMenu.style.display =
    dropdownMenu.style.display === "block" ? "none" : "block";
}

// 알림 메뉴 토글 함수
function toggleNotifications() {
  const notificationDropdown = document.getElementById("notificationDropdown");
  notificationDropdown.style.display =
    notificationDropdown.style.display === "block" ? "none" : "block";
}
