document.addEventListener("DOMContentLoaded", function () {
  // 탭 전환 기능 추가
  const tabUpcoming = document.getElementById("tab-upcoming");
  const tabHistory = document.getElementById("tab-history");
  const contentUpcoming = document.getElementById("content-upcoming");
  const contentHistory = document.getElementById("content-history");

  // 일정 데이터 처리
  let schedules = [];
  try {
    // 인라인 스크립트에서 전달된 데이터 확인
    console.log("전달된 데이터 타입:", typeof schedulesJsonData);

    // 이미 객체인 경우 그대로 사용, 문자열인 경우 파싱
    if (typeof schedulesJsonData === "string") {
      schedules = JSON.parse(schedulesJsonData);
    } else {
      schedules = schedulesJsonData;
    }

    console.log("일정 데이터 로드 완료:", schedules.length + "개 일정");
  } catch (error) {
    console.error("일정 데이터 처리 오류:", error);
    // HTML에 직접 렌더링된 카드를 사용
    console.log("HTML에 렌더링된 카드를 사용합니다.");
    return; // 파싱 오류 시 함수 종료
  }

  // 현재 날짜 가져오기
  const currentDate = new Date();
  currentDate.setHours(0, 0, 0, 0); // 시간 부분 제거

  console.log("현재 날짜:", currentDate);

  // 예정된 여행과 지난 여행 분류
  const upcomingSchedules = [];
  const pastSchedules = [];

  schedules.forEach((schedule) => {
    // 종료일 파싱
    const endDateStr = schedule.endDate;
    const [year, month, day] = endDateStr
      .split("-")
      .map((num) => parseInt(num, 10));
    const endDate = new Date(year, month - 1, day); // 월은 0부터 시작하므로 -1
    endDate.setHours(0, 0, 0, 0);

    console.log("일정 종료일:", schedule.planName, endDateStr, "->", endDate);

    // 종료일이 현재 날짜보다 이전이면 지난 여행, 아니면 예정된 여행
    if (endDate < currentDate) {
      console.log("지난 여행으로 분류:", schedule.planName);
      pastSchedules.push(schedule);
    } else {
      console.log("예정된 여행으로 분류:", schedule.planName);
      upcomingSchedules.push(schedule);
    }
  });

  // 초기 정렬 (최신순)
  sortSchedules(upcomingSchedules, "newest");
  sortSchedules(pastSchedules, "newest");

  console.log("예정된 여행 수:", upcomingSchedules.length);
  console.log("지난 여행 수:", pastSchedules.length);

  // 예정된 여행 수와 지난 여행 수 업데이트
  const upcomingCountElement = document.querySelector("#tab-upcoming span");
  const historyCountElement = document.querySelector("#tab-history span");

  if (upcomingCountElement) {
    upcomingCountElement.textContent = upcomingSchedules.length;
  }

  if (historyCountElement) {
    historyCountElement.textContent = pastSchedules.length;
  }

  // 탭 전환 이벤트 리스너 등록
  if (tabUpcoming && tabHistory) {
    tabUpcoming.addEventListener("click", function () {
      tabUpcoming.classList.add("active");
      tabHistory.classList.remove("active");
      contentUpcoming.classList.add("active");
      contentHistory.classList.remove("active");
    });

    tabHistory.addEventListener("click", function () {
      tabHistory.classList.add("active");
      tabUpcoming.classList.remove("active");
      contentHistory.classList.add("active");
      contentUpcoming.classList.remove("active");

      // 히스토리 탭 클릭 시 지난 여행 렌더링
      renderHistoryCards(pastSchedules);
    });
  }

  // 초기 렌더링 - 페이지 로드 시 예정된 여행 표시
  renderTravelCards(upcomingSchedules);

  // 히스토리 데이터도 미리 렌더링 (탭이 숨겨져 있어도 데이터는 준비)
  renderHistoryCards(pastSchedules);

  // 정렬 옵션 이벤트 리스너
  const sortNewest = document.getElementById("sort-newest");
  const sortOldest = document.getElementById("sort-oldest");

  if (sortNewest && sortOldest) {
    sortNewest.addEventListener("click", function () {
      this.classList.add("active");
      sortOldest.classList.remove("active");
      sortSchedules(upcomingSchedules, "newest");
      renderTravelCards(upcomingSchedules);
    });

    sortOldest.addEventListener("click", function () {
      this.classList.add("active");
      sortNewest.classList.remove("active");
      sortSchedules(upcomingSchedules, "oldest");
      renderTravelCards(upcomingSchedules);
    });
  }

  // 히스토리 정렬 옵션 이벤트 리스너
  const sortHistoryNewest = document.getElementById("sort-history-newest");
  const sortHistoryOldest = document.getElementById("sort-history-oldest");

  if (sortHistoryNewest && sortHistoryOldest) {
    sortHistoryNewest.addEventListener("click", function () {
      this.classList.add("active");
      sortHistoryOldest.classList.remove("active");
      sortSchedules(pastSchedules, "newest");
      renderHistoryCards(pastSchedules);
    });

    sortHistoryOldest.addEventListener("click", function () {
      this.classList.add("active");
      sortHistoryNewest.classList.remove("active");
      sortSchedules(pastSchedules, "oldest");
      renderHistoryCards(pastSchedules);
    });
  }

  // 여행 카드 생성 함수
  function createTravelCard(schedule) {
    const card = document.createElement("div");
    card.className = "travel-card";
    card.setAttribute("data-date", schedule.startDate);
    card.setAttribute("data-end-date", schedule.endDate);
    card.onclick = function () {
      window.location = "/schedule/details/" + schedule.id;
    };

    const backgroundImage =
      schedule.planPhoto && schedule.planPhoto !== ""
        ? `background-image: url('${schedule.planPhoto}')`
        : `background-image: url('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=2073&auto=format&fit=crop')`;

    card.innerHTML = `
      <div class="card-image" style="${backgroundImage}"></div>
      <div class="card-content">
        <div class="card-label">${schedule.purpose || "여행"}</div>
        <h3 class="card-title">${schedule.planName}</h3>
        <div class="card-details">
          <span>출발: ${schedule.departLocation} | 목적지: ${
      schedule.destination
    }</span>
        </div>
        <div class="card-date">
          <i class="far fa-calendar-alt" style="margin-right: 5px;"></i>
          <span>${schedule.startDate} ~ ${schedule.endDate}</span>
        </div>
      </div>
    `;

    return card;
  }

  // 여행 카드 렌더링 함수
  function renderTravelCards(schedules) {
    const container = document.getElementById("travel-cards-container");
    if (!container) {
      console.error("travel-cards-container를 찾을 수 없습니다.");
      return;
    }

    console.log("여행 카드 렌더링 시작:", schedules.length + "개");

    container.innerHTML = "";

    if (schedules.length === 0) {
      container.innerHTML = `
        <div class="no-plans">
          <i class="fas fa-calendar-times"></i>
          <p>아직 확정된 여행 일정이 없습니다.</p>
          <a href="/itineraries/itineraryCreate" class="btn-create">새 여행 계획 만들기</a>
        </div>
      `;
      return;
    }

    schedules.forEach((schedule) => {
      const card = createTravelCard(schedule);
      container.appendChild(card);
    });

    console.log("여행 카드 렌더링 완료");
  }

  // 히스토리 카드 렌더링 함수
  function renderHistoryCards(schedules) {
    const container = document.getElementById("history-cards-container");
    if (!container) {
      console.error("history-cards-container를 찾을 수 없습니다.");
      return;
    }

    console.log("히스토리 카드 렌더링 시작:", schedules.length + "개");

    container.innerHTML = "";

    if (schedules.length === 0) {
      container.innerHTML = `
        <div class="no-plans">
          <i class="fas fa-history"></i>
          <p>아직 여행 히스토리가 없습니다.</p>
          <p style="margin-top: 10px; font-size: 14px;">여행을 다녀오신 후 기록이 자동으로 추가됩니다!</p>
        </div>
      `;
      return;
    }

    // 도시별로 그룹화하지 않고 각 여행을 개별 카드로 표시
    schedules.forEach((schedule) => {
      const tripCard = document.createElement("div");
      tripCard.className = "history-card";
      tripCard.setAttribute("data-date", schedule.startDate);
      tripCard.setAttribute("data-end-date", schedule.endDate);
      tripCard.onclick = function() {
        window.location = "/schedule/details/" + schedule.id;
      };

      const backgroundImage =
        schedule.planPhoto && schedule.planPhoto !== ""
          ? `background-image: url('${schedule.planPhoto}')`
          : `background-image: url('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=2073&auto=format&fit=crop')`;

      tripCard.innerHTML = `
        <div class="card-image" style="${backgroundImage}"></div>
        <div class="card-content">
          <div class="card-label">${schedule.purpose || "여행"}</div>
          <h3 class="card-title">${schedule.planName}</h3>
          <div class="card-details">
            <span>출발: ${schedule.departLocation} | 목적지: ${schedule.destination}</span>
          </div>
          <div class="card-date">
            <i class="far fa-calendar-alt" style="margin-right: 5px;"></i>
            <span>${schedule.startDate} ~ ${schedule.endDate}</span>
          </div>
        </div>
      `;

      container.appendChild(tripCard);
    });

    console.log("히스토리 카드 렌더링 완료");
  }

  // 여행 히스토리 모달 표시 함수 추가
  function showTripHistoryModal(cityName, schedules) {
    // 기존 모달이 있으면 제거
    const existingModal = document.getElementById("trip-history-modal");
    if (existingModal) {
      existingModal.remove();
    }

    // 모달 생성
    const modal = document.createElement("div");
    modal.id = "trip-history-modal";
    modal.className = "modal";

    // 최신순으로 정렬
    const sortedSchedules = [...schedules].sort(
      (a, b) => new Date(b.endDate) - new Date(a.endDate)
    );

    // 모달 내용 생성
    let modalContent = `
      <div class="modal-content">
        <div class="modal-header">
          <h2>${cityName} 여행 기록 (${schedules.length}회)</h2>
          <span class="close-modal">&times;</span>
        </div>
        <div class="modal-body">
    `;

    // 여행 목록 추가
    sortedSchedules.forEach((schedule) => {
      const tripImage =
        schedule.planPhoto && schedule.planPhoto !== ""
          ? schedule.planPhoto
          : "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=2073&auto=format&fit=crop";

      modalContent += `
        <div class="trip-item" data-id="${schedule.id}">
          <div class="trip-image" style="background-image: url('${tripImage}')"></div>
          <div class="trip-info">
            <h3>${schedule.planName}</h3>
            <div class="trip-purpose">${schedule.purpose || "여행"}</div>
            <div class="trip-date">${schedule.startDate} ~ ${
        schedule.endDate
      }</div>
            <div class="trip-details">출발: ${
              schedule.departLocation
            } | 목적지: ${schedule.destination}</div>
          </div>
        </div>
      `;
    });

    modalContent += `
        </div>
      </div>
    `;

    modal.innerHTML = modalContent;
    document.body.appendChild(modal);

    // 모달 표시
    setTimeout(() => {
      modal.style.display = "block";
    }, 10);

    // 닫기 버튼 이벤트
    const closeBtn = modal.querySelector(".close-modal");
    closeBtn.addEventListener("click", function () {
      modal.style.display = "none";
      setTimeout(() => {
        modal.remove();
      }, 300);
    });

    // 모달 외부 클릭 시 닫기
    window.addEventListener("click", function (event) {
      if (event.target === modal) {
        modal.style.display = "none";
        setTimeout(() => {
          modal.remove();
        }, 300);
      }
    });

    // 여행 항목 클릭 시 상세 페이지로 이동
    const tripItems = modal.querySelectorAll(".trip-item");
    tripItems.forEach((item) => {
      item.addEventListener("click", function () {
        const tripId = this.getAttribute("data-id");
        window.location = "/schedule/details/" + tripId;
      });
    });
  }

  // 일정 정렬 함수
  function sortSchedules(schedules, sortType) {
    schedules.sort((a, b) => {
      const dateA = new Date(a.startDate);
      const dateB = new Date(b.startDate);

      if (sortType === "newest") {
        return dateB - dateA;
      } else {
        return dateA - dateB;
      }
    });
  }
});
