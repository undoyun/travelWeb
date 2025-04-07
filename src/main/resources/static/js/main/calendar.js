document.addEventListener("DOMContentLoaded", function () {
  var calendarEl = document.getElementById("calendar");

  if (calendarEl) {
    // 캘린더 초기화 에러 처리 추가
    try {
      var calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: "dayGridMonth",
        headerToolbar: {
          left: "prev",
          center: "title",
          right: "next today yearGrid",
        },
        customButtons: {
          yearGrid: {
            text: "연간 보기",
            click: function () {
              showYearCalendar();
            },
          },
        },
        dayHeaderFormat: { weekday: "short" }, // 요일 표시 형식 (월, 화, 수...)
        locale: "ko", // 한국어 설정
        editable: true,
        eventClick: function (info) {
          showEventModal(info.event, calendar);
        },
        // 이벤트 드래그 앤 드롭 처리
        eventDrop: function (info) {
          updateEventDates(info.event, calendar);
        },
        // 이벤트 리사이징 처리
        eventResize: function (info) {
          updateEventDates(info.event, calendar);
        },
        height: "auto",
        fixedWeekCount: false,
        showNonCurrentDates: true,
        // 에러 처리 추가
        eventDidMount: function (info) {
          // 이벤트가 성공적으로 마운트됐을 때의 처리 (필요시 활용)
        },
        eventSourceFailure: function (error) {
          console.warn("이벤트 소스 로딩 실패:", error);
          showToast("일정 데이터를 불러오는데 문제가 발생했습니다.", "warning");
        },
      });

      calendar.render();

      // 일정 로딩을 try-catch로 감싸서 실패해도 UI가 그려지도록 함
      try {
        loadEvents(calendar);
      } catch (error) {
        console.error("일정 로딩 중 오류 발생:", error);
        showToast("일정을 불러오는 중 오류가 발생했습니다.", "error");
      }

      // 오늘 버튼 클릭 이벤트
      document
        .querySelector(".btn-today")
        .addEventListener("click", function () {
          calendar.today();
        });

      // 뷰 선택 탭 이벤트 리스너
      document.querySelectorAll(".view-btn").forEach(function (btn) {
        btn.addEventListener("click", function () {
          // 활성 클래스 제거
          document.querySelectorAll(".view-btn").forEach(function (b) {
            b.classList.remove("active");
          });

          // 클릭한 버튼에 활성 클래스 추가
          this.classList.add("active");

          // 캘린더 뷰 변경
          calendar.changeView(this.getAttribute("data-view"));
        });
      });
    } catch (error) {
      console.error("캘린더 초기화 중 오류 발생:", error);
      // 캘린더 초기화 실패 시 대체 UI 표시
      calendarEl.innerHTML = `
        <div class="calendar-error">
          <i class="fas fa-calendar-times"></i>
          <h3>캘린더를 불러올 수 없습니다</h3>
          <p>새로고침을 시도하거나 잠시 후 다시 시도해주세요.</p>
          <button onclick="window.location.reload()" class="retry-btn">
            <i class="fas fa-sync-alt"></i> 새로고침
          </button>
        </div>
      `;
    }
  }
});

// 연간 달력 표시 함수
function showYearCalendar() {
  // 기존 캘린더 숨기기
  document.getElementById("calendar").style.display = "none";

  // 연간 달력 컨테이너가 없으면 생성
  let yearCalendarContainer = document.getElementById(
    "year-calendar-container"
  );
  if (!yearCalendarContainer) {
    yearCalendarContainer = document.createElement("div");
    yearCalendarContainer.id = "year-calendar-container";
    yearCalendarContainer.className = "year-calendar-container";
    document
      .getElementById("calendar-container")
      .appendChild(yearCalendarContainer);
  } else {
    yearCalendarContainer.innerHTML = "";
    yearCalendarContainer.style.display = "grid";
  }

  // 현재 연도 가져오기
  const currentYear = new Date().getFullYear();

  // 연도 선택 헤더 추가
  const yearHeader = document.createElement("div");
  yearHeader.className = "year-calendar-header";
  yearHeader.innerHTML = `
    <button class="year-prev-btn"><i class="fas fa-chevron-left"></i></button>
    <h2 class="year-title">${currentYear}년</h2>
    <button class="year-next-btn"><i class="fas fa-chevron-right"></i></button>
    <button class="year-close-btn">월별 보기로 돌아가기</button>
  `;
  yearCalendarContainer.appendChild(yearHeader);

  // 12개월 달력 그리드 생성
  const monthsGrid = document.createElement("div");
  monthsGrid.className = "months-grid";
  yearCalendarContainer.appendChild(monthsGrid);

  // 12개월 달력 생성
  for (let month = 0; month < 12; month++) {
    const monthCalendarEl = document.createElement("div");
    monthCalendarEl.className = "month-calendar";
    monthsGrid.appendChild(monthCalendarEl);

    const monthCalendar = new FullCalendar.Calendar(monthCalendarEl, {
      initialDate: new Date(currentYear, month, 1),
      initialView: "dayGridMonth",
      headerToolbar: {
        left: "",
        center: "title",
        right: "",
      },
      locale: "ko",
      height: "auto",
      dayHeaderFormat: { weekday: "short" },
      fixedWeekCount: false,
      showNonCurrentDates: false,
    });

    monthCalendar.render();
  }

  // 이벤트 리스너 추가
  document
    .querySelector(".year-close-btn")
    .addEventListener("click", function () {
      yearCalendarContainer.style.display = "none";
      document.getElementById("calendar").style.display = "block";
    });

  document
    .querySelector(".year-prev-btn")
    .addEventListener("click", function () {
      updateYearCalendar(
        parseInt(document.querySelector(".year-title").textContent) - 1
      );
    });

  document
    .querySelector(".year-next-btn")
    .addEventListener("click", function () {
      updateYearCalendar(
        parseInt(document.querySelector(".year-title").textContent) + 1
      );
    });
}

// 연도 변경 시 연간 달력 업데이트
function updateYearCalendar(year) {
  document.querySelector(".year-title").textContent = year + "년";

  const monthCalendars = document.querySelectorAll(".month-calendar");
  monthCalendars.forEach((el, month) => {
    const calendar = new FullCalendar.Calendar(el, {
      initialDate: new Date(year, month, 1),
      initialView: "dayGridMonth",
      headerToolbar: {
        left: "",
        center: "title",
        right: "",
      },
      locale: "ko",
      height: "auto",
      dayHeaderFormat: { weekday: "short" },
      fixedWeekCount: false,
      showNonCurrentDates: false,
    });

    el.innerHTML = "";
    calendar.render();
  });
}
