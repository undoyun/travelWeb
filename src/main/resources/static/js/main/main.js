document.addEventListener("DOMContentLoaded", function () {
  // 문서 클릭 시 드롭다운 닫기
  document.addEventListener("click", function (event) {
    const notificationDropdown = document.getElementById("notificationDropdown");
    const notificationIcon = document.querySelector(".notification-icon");

    if (notificationDropdown && notificationIcon) {
      if (
        !notificationIcon.contains(event.target) &&
        !notificationDropdown.contains(event.target)
      ) {
        notificationDropdown.style.display = "none";
      }
    }
  });

  // 배너 슬라이더 초기화
  initBannerSlider();
});

// 배너 슬라이더 초기화 함수
function initBannerSlider() {
  const slides = document.querySelectorAll(".banner-slide");
  const nextSlideBtn = document.querySelector(".next-slide");
  const prevSlideBtn = document.querySelector(".prev-slide");
  const pauseSlideBtn = document.querySelector(".pause-slide");

  console.log("슬라이드 개수:", slides.length);
  console.log("다음 버튼:", nextSlideBtn);
  console.log("이전 버튼:", prevSlideBtn);
  console.log("일시정지 버튼:", pauseSlideBtn);

  if (slides.length > 0 && nextSlideBtn && prevSlideBtn && pauseSlideBtn) {
    let currentSlide = 0;
    let autoSlideInterval;
    let isPaused = false;

    // 슬라이드 표시 함수
    function showSlide(index) {
      console.log("슬라이드 표시:", index);
      
      // 모든 슬라이드 숨기기
      slides.forEach((slide) => {
        slide.classList.remove("active");
      });

      // 현재 슬라이드 표시
      slides[index].classList.add("active");

      // 페이지네이션 업데이트
      const currentIndicators = document.querySelectorAll(".current");
      currentIndicators.forEach((indicator) => {
        indicator.textContent = index + 1;
      });
    }

    // 자동 슬라이드 시작
    function startAutoSlide() {
      console.log("자동 슬라이드 시작");
      clearInterval(autoSlideInterval); // 기존 인터벌 제거
      autoSlideInterval = setInterval(() => {
        nextSlide();
      }, 5000);
    }

    // 다음 슬라이드
    function nextSlide() {
      console.log("다음 슬라이드");
      currentSlide = (currentSlide + 1) % slides.length;
      showSlide(currentSlide);
    }

    // 이전 슬라이드
    function prevSlide() {
      console.log("이전 슬라이드");
      currentSlide = (currentSlide - 1 + slides.length) % slides.length;
      showSlide(currentSlide);
    }

    // 슬라이드 일시정지/재생
    function togglePause() {
      console.log("일시정지/재생 토글");
      if (isPaused) {
        isPaused = false;
        pauseSlideBtn.innerHTML = '<i class="fas fa-pause"></i>';
        startAutoSlide();
      } else {
        isPaused = true;
        pauseSlideBtn.innerHTML = '<i class="fas fa-play"></i>';
        clearInterval(autoSlideInterval);
      }
    }

    // 이벤트 리스너 등록
    nextSlideBtn.addEventListener("click", function (e) {
      e.preventDefault();
      console.log("다음 버튼 클릭");
      clearInterval(autoSlideInterval);
      nextSlide();
      if (!isPaused) startAutoSlide();
    });

    prevSlideBtn.addEventListener("click", function (e) {
      e.preventDefault();
      console.log("이전 버튼 클릭");
      clearInterval(autoSlideInterval);
      prevSlide();
      if (!isPaused) startAutoSlide();
    });

    pauseSlideBtn.addEventListener("click", function (e) {
      e.preventDefault();
      console.log("일시정지 버튼 클릭");
      togglePause();
    });

    // 초기 슬라이드 표시
    showSlide(currentSlide);

    // 자동 슬라이드 시작
    startAutoSlide();
    
    console.log("배너 슬라이더 초기화 완료");
  } else {
    console.error("배너 슬라이더 요소를 찾을 수 없습니다.");
  }
}

document.addEventListener('DOMContentLoaded', function() {
  // FullCalendar 한국어 로케일 설정
  if (FullCalendar.globalLocales) {
      FullCalendar.globalLocales.push({
          code: 'ko',
          buttonText: {
              prev: '이전',
              next: '다음',
              today: '오늘',
              month: '월',
              week: '주',
              day: '일',
              list: '일정목록'
          },
          weekText: '주',
          allDayText: '종일',
          moreLinkText: '개 더보기',
          noEventsText: '일정이 없습니다',
          monthNames: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
          monthNamesShort: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
          dayNames: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
          dayNamesShort: ['일', '월', '화', '수', '목', '금', '토']
      });
  }
});