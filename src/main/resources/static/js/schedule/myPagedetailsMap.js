// 전역 변수들
var parsedData = null; // 일정 JSON 파싱 결과
var map; // kakao.maps.Map
var ps; // kakao.maps.services.Places
var infowindow; // kakao.maps.InfoWindow
var customOverlay; // 커스텀 오버레이

// 날짜별 마커
var markersByDay = [];
// 날짜별 Polyline
var polylinesByDay = [];
// 현재 선택된 날짜 인덱스
var currentDayIndex = 0;
// 경로 보기 on/off 상태
var showRoute = false;

document.addEventListener("DOMContentLoaded", function () {
  // ----------------------
  // (1) 일정 데이터 로드/파싱
  // ----------------------
  const scheduleJsonData =
    document.getElementById("scheduleJsonData").textContent;
  const itineraryContainer = document.getElementById("itineraryContainer");

  try {
    // 1) JSON 파싱
    parsedData = JSON.parse(scheduleJsonData);
    console.log("파싱된 데이터:", parsedData);

    // 2) 파싱한 데이터가 어느 구조인지 확인 후, itineraryData 준비
    let itineraryData;
    if (parsedData.days && parsedData.days.length > 0) {
      // 구조가 { "days": [...] } 인 경우
      itineraryData = parsedData;
    } else if (parsedData.travelPlan && parsedData.travelPlan.itinerary) {
      // 구조가 { "travelPlan": { ... , "itinerary": [...] } } 인 경우
      // => days 형태로 변환
      itineraryData = {
        days: parsedData.travelPlan.itinerary.map((day) => ({
          date: day.date,
          schedules: day.schedule.map((item) => ({
            time: item.departureTime,
            content: `${item.category} - ${item.location} (${item.duration}, ${item.cost})`,
            location: item.location,
          })),
        })),
      };
    } else {
      // 둘 다 아닌 경우
      throw new Error("지원되지 않는 데이터 형식입니다.");
    }

    // ----------------------
    // (2) 일정 HTML 렌더링
    // ----------------------
    renderItinerary(itineraryData, itineraryContainer);

    // ----------------------
    // (3) 지도 초기화
    // ----------------------
    initMap();

    // ----------------------
    // (4) 날짜별 마커 및 Polyline 생성 (비동기 검색 완료 후 changeDay(0))
    // ----------------------
    createDayMarkersAndPolylines(itineraryData);

    // ----------------------
    // (5) 날짜 탭(버튼) 생성
    // ----------------------
    createDayTabs(itineraryData);
  } catch (err) {
    console.error("일정 데이터 로드 오류:", err);
    itineraryContainer.innerHTML = `
                <div class="error-message">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p>일정 데이터를 불러오는 중 오류가 발생했습니다.</p>
                    <p>${err.message}</p>
                </div>
            `;

    // 지도 로딩 표시 숨기기
    document.getElementById("mapLoading").style.display = "none";
  }
});

// -------------------------------
// (A) 일정 HTML 구조 렌더링
// -------------------------------
function renderItinerary(itineraryData, container) {
  // 로딩 스피너 제거 후 일정 보여주기
  setTimeout(() => {
    container.innerHTML = "";
    if (!itineraryData.days || itineraryData.days.length === 0) {
      container.innerHTML = `
                                    <div class="empty-itinerary">
                                        <i class="fas fa-calendar-times"></i>
                                        <p>일정 데이터가 없습니다.</p>
                                    </div>
                                `;
      return;
    }

    // 날짜별 일정 렌더링
    itineraryData.days.forEach((day, index) => {
      const dayCard = createDayElement(day, index + 1);
      // 첫 번째 날짜만 보이고 나머지는 숨김
      if (index === 0) {
        dayCard.classList.add('active-day');
      } else {
        dayCard.style.display = 'none';
      }
      dayCard.dataset.day = index;
      container.appendChild(dayCard);
    });
  }, 500);
}

function createDayElement(day, dayNumber) {
  const dayElement = document.createElement("div");
  dayElement.className = "day-card";

  const dayHeader = document.createElement("div");
  dayHeader.className = "day-header";
  dayHeader.innerHTML = `
                    <h3 class="day-title">Day ${dayNumber} <span class="day-date">${day.date}</span></h3>
                `;
  dayElement.appendChild(dayHeader);

  const scheduleContainer = document.createElement("div");
  scheduleContainer.className = "schedule-container";

  day.schedules.forEach((schedule, idx) => {
    const scheduleItem = createScheduleElement(schedule, idx + 1);
    scheduleContainer.appendChild(scheduleItem);
  });

  dayElement.appendChild(scheduleContainer);
  return dayElement;
}

function createScheduleElement(schedule, idx) {
  const scheduleElement = document.createElement("div");
  scheduleElement.className = "schedule-item";

  const indexElement = document.createElement("div");
  indexElement.className = "schedule-index";
  indexElement.textContent = idx;

  const timeElement = document.createElement("div");
  timeElement.className = "schedule-time";
  timeElement.innerHTML = `<span>${schedule.time || "00:00"}</span>`;

  const contentElement = document.createElement("div");
  contentElement.className = "schedule-content";
  contentElement.innerHTML = `<p>${schedule.content || ""}</p>`;

  scheduleElement.appendChild(indexElement);
  scheduleElement.appendChild(timeElement);
  scheduleElement.appendChild(contentElement);

  return scheduleElement;
}

// -------------------------------
// (B) 지도 초기화 (개선됨)
// -------------------------------
function initMap() {
  var mapContainer = document.getElementById("map");
  var mapOption = {
    center: new kakao.maps.LatLng(37.566826, 126.9786567),
    level: 3,
  };

  // 지도 생성
  map = new kakao.maps.Map(mapContainer, mapOption);

  // 장소 검색 객체 생성
  ps = new kakao.maps.services.Places();

  // 인포윈도우 생성
  infowindow = new kakao.maps.InfoWindow({
    zIndex: 1,
    disableAutoPan: false,
  });

  // 지도 컨트롤러 추가
  var zoomControl = new kakao.maps.ZoomControl();
  map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

  // 지도 타입 컨트롤러 추가
  var mapTypeControl = new kakao.maps.MapTypeControl();
  map.addControl(mapTypeControl, kakao.maps.ControlPosition.TOPRIGHT);

  // "경로 보기" 버튼 이벤트
  document.getElementById("routeButton").addEventListener("click", function () {
    toggleRoute();

    // 버튼 상태 변경
    this.classList.toggle("active");
    if (this.classList.contains("active")) {
      this.innerHTML = '<i class="fas fa-route"></i> 경로 숨기기';
    } else {
      this.innerHTML = '<i class="fas fa-route"></i> 경로 보기';
    }
  });

  // "전체 보기" 버튼 이벤트
  document
    .getElementById("fitBoundsButton")
    .addEventListener("click", function () {
      fitMapToAllMarkers();
    });

  // 줌 컨트롤 기능
  document.getElementById("zoomIn").addEventListener("click", function () {
    map.setLevel(map.getLevel() - 1);
  });

  document.getElementById("zoomOut").addEventListener("click", function () {
    map.setLevel(map.getLevel() + 1);
  });
}

// 모든 마커를 포함하는 영역으로 지도 범위 재설정
function fitMapToAllMarkers() {
  var bounds = new kakao.maps.LatLngBounds();
  var hasMarkers = false;

  markersByDay.forEach((markers) => {
    markers.forEach((marker) => {
      bounds.extend(marker.getPosition());
      hasMarkers = true;
    });
  });

  if (hasMarkers) {
    map.setBounds(bounds, 100); // 패딩 적용
  }
}

// -------------------------------
// (C) 날짜별 마커 & Polyline 생성 (개선됨)
// -------------------------------
function createDayMarkersAndPolylines(itineraryData) {
  // 날짜 수만큼 배열 초기화
  markersByDay = itineraryData.days.map(() => []);
  polylinesByDay = itineraryData.days.map(() => null);

  // "일자별 schedulePromises"를 모아서 한 번에 Promise.all
  let allDayPromises = [];

  itineraryData.days.forEach((day, dayIndex) => {
    // 당일 경로를 구성할 LatLng 배열
    var latLngArray = [];

    // 스케줄마다 장소 검색(Promise)
    let schedulePromises = day.schedules.map((schedule, scheduleIndex) => {
      var locationKeyword = schedule.location || "";
      if (!locationKeyword.trim()) {
        return Promise.resolve(null);
      }

      return new Promise((resolve) => {
        ps.keywordSearch(locationKeyword, function (data, status) {
          if (status === kakao.maps.services.Status.OK && data.length > 0) {
            // 검색 결과 중 첫 번째만 사용
            var place = data[0];
            // 마커 생성시 인덱스 전달 (1부터 시작)
            var marker = createCustomMarker(place, scheduleIndex + 1, schedule);
            markersByDay[dayIndex].push(marker);

            var latLng = new kakao.maps.LatLng(place.y, place.x);
            latLngArray.push(latLng);
            resolve(true);
          } else {
            // 검색 실패/결과없음 등
            resolve(false);
          }
        });
      });
    });

    // 이 날짜 모든 검색이 끝난 후 → Polyline 생성
    let dayPromise = Promise.all(schedulePromises).then(() => {
      if (latLngArray.length >= 2) {
        polylinesByDay[dayIndex] = createPolyline(latLngArray);
      }
    });

    allDayPromises.push(dayPromise);
  });

  // 모든 날짜(Day1~N) 검색이 끝난 뒤 → Day1 표시
  Promise.all(allDayPromises).then(() => {
    changeDay(0); // 처음에는 1일차 표시

    // 지도 로딩 표시 숨기기
    document.getElementById("mapLoading").style.display = "none";
  });
}

// 숫자가 표시되는 커스텀 마커 생성
function createCustomMarker(place, index, schedule) {
  // 마커 위치
  var position = new kakao.maps.LatLng(place.y, place.x);

  // 커스텀 오버레이에 표시할 내용
  var content = `
            <div class="custom-marker">
                ${index}
            </div>
        `;

  // 커스텀 오버레이 생성
  var marker = new kakao.maps.CustomOverlay({
    position: position,
    content: content,
    xAnchor: 0.5,
    yAnchor: 0.5,
    zIndex: 3,
  });

  // 클릭 이벤트: 장소 정보 표시
  kakao.maps.event.addListener(marker, "click", function () {
    // 기존 인포윈도우 닫기
    infowindow.close();

    // 새 인포윈도우 표시
    var infoContent = `
                <div class="custom-infowindow">
                    <div class="place-name">${place.place_name}</div>
                    <div class="place-address">${place.address_name}</div>
                    ${
                      schedule.time
                        ? `<div class="place-time">Time: ${schedule.time}</div>`
                        : ""
                    }
                </div>
            `;

    infowindow.setContent(infoContent);
    infowindow.setPosition(position);
    infowindow.open(map);
  });

  return marker;
}

// 개선된 Polyline 생성
function createPolyline(latLngArray) {
  var polyline = new kakao.maps.Polyline({
    path: latLngArray,
    strokeWeight: 4,
    strokeColor: "#1e88e5",
    strokeOpacity: 0.7,
    strokeStyle: "solid",
  });
  // 기본은 지도 표시 X
  polyline.setMap(null);
  return polyline;
}

// -------------------------------
// (D) 날짜 탭(버튼) 생성 (개선됨)
// -------------------------------
function createDayTabs(itineraryData) {
  const container = document.getElementById("dayTabs");
  container.innerHTML = "";

  // All 버튼 추가
  const allBtn = document.createElement("div");
  allBtn.className = "day-tab active";
  allBtn.textContent = "All";
  allBtn.addEventListener("click", function () {
    // 모든 마커 표시 및 모든 일정 표시 로직
    showAllDays();
    fitMapToAllMarkers();
    document.querySelector(".current-day-title h3").textContent = "전체 일정";
    document.querySelector(".current-day-title .date").textContent = `${
      itineraryData.days[0].date
    } ~ ${itineraryData.days[itineraryData.days.length - 1].date}`;

    // 탭 활성화 상태 변경
    document
      .querySelectorAll(".day-tab")
      .forEach((tab) => tab.classList.remove("active"));
    this.classList.add("active");
  });
  container.appendChild(allBtn);

  // 날짜별 탭 추가
  itineraryData.days.forEach((day, index) => {
    const tab = document.createElement("div");
    tab.className = "day-tab";
    tab.textContent = `Day${index + 1}`;
    tab.addEventListener("click", function () {
      // 날짜 변경
      changeDay(index);

      // 탭 활성화 상태 변경
      document
        .querySelectorAll(".day-tab")
        .forEach((tab) => tab.classList.remove("active"));
      this.classList.add("active");

      // 일정 제목 업데이트
      document.querySelector(".current-day-title h3").textContent = `Day ${
        index + 1
      }`;
      document.querySelector(".current-day-title .date").textContent = day.date;
    });
    container.appendChild(tab);
  });

  // 초기 현재 날짜 설정
  if (itineraryData.days.length > 0) {
    document.querySelector(".current-day-title h3").textContent = "전체 일정";
    document.querySelector(".current-day-title .date").textContent = 
      `${itineraryData.days[0].date} ~ ${itineraryData.days[itineraryData.days.length - 1].date}`;
  }
}

// -------------------------------
// (E) 날짜 변경 시 마커/Polyline 업데이트 (개선됨)
// -------------------------------
function changeDay(dayIndex) {
  // 1) 기존 모든 마커 지도에서 제거
  markersByDay.forEach((markerArray) => {
    markerArray.forEach((marker) => marker.setMap(null));
  });
  // 2) 기존 모든 Polyline 지도에서 제거
  polylinesByDay.forEach((polyline) => {
    if (polyline) {
      polyline.setMap(null);
    }
  });

  // 3) 선택된 날짜 마커만 지도에 표시
  markersByDay[dayIndex].forEach((marker) => {
    marker.setMap(map);
  });

  // 4) 경로 보기 상태(showRoute=true)면, 해당 날짜의 Polyline 표시
  if (showRoute && polylinesByDay[dayIndex]) {
    polylinesByDay[dayIndex].setMap(map);
  }

  // 5) 지도 범위 재설정
  if (markersByDay[dayIndex].length > 0) {
    var bounds = new kakao.maps.LatLngBounds();
    markersByDay[dayIndex].forEach((marker) => {
      bounds.extend(marker.getPosition());
    });
    map.setBounds(bounds, 100); // 패딩 100px 적용
  }

  // 6) 일정 목록에서 해당 날짜만 표시하도록 수정
  const dayCards = document.querySelectorAll(".day-card");
  dayCards.forEach((card) => {
    if (parseInt(card.dataset.day) === dayIndex) {
      card.style.display = 'block';
      card.classList.add("active-day");
    } else {
      card.style.display = 'none';
      card.classList.remove("active-day");
    }
  });

  // 현재 선택 일자 갱신
  currentDayIndex = dayIndex;
}

// 경로 보기 토글 (개선됨)
function toggleRoute() {
  showRoute = !showRoute;

  // 모든 Polyline 제거
  polylinesByDay.forEach((polyline) => {
    if (polyline) {
      polyline.setMap(null);
    }
  });

  // showRoute = true 인 경우, 현재 선택 일자의 Polyline만 지도에 표시
  if (showRoute && polylinesByDay[currentDayIndex]) {
    polylinesByDay[currentDayIndex].setMap(map);
  }
}

// 모든 날짜의 일정 표시
function showAllDays() {
  // 모든 day-card 표시
  const dayCards = document.querySelectorAll(".day-card");
  dayCards.forEach(card => {
    card.style.display = 'block';
    card.classList.remove('active-day');
  });

  // 모든 마커 표시
  markersByDay.forEach((markers, index) => {
    markers.forEach(marker => {
      marker.setMap(map);
    });
  });

  // 경로는 표시하지 않음
  polylinesByDay.forEach(polyline => {
    if (polyline) polyline.setMap(null);
  });
}
