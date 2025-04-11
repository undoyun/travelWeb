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

// 부산 주요 지역 기본 좌표 정의 (전역 변수로 추가)
var busanDefaultCoords = {
  "해운대": { lat: 35.1631, lng: 129.1597 },
  "서면": { lat: 35.1569, lng: 129.0594 },
  "광안리": { lat: 35.1535, lng: 129.1185 },
  "남포동": { lat: 35.0975, lng: 129.0403 },
  "송정": { lat: 35.1787, lng: 129.2003 },
  "기장": { lat: 35.2461, lng: 129.2242 },
  "영도": { lat: 35.0897, lng: 129.0758 },
  "사상": { lat: 35.1669, lng: 128.9874 },
  "동래": { lat: 35.2062, lng: 129.0794 },
  "부산역": { lat: 35.1154, lng: 129.0404 },
  "김해공항": { lat: 35.1765, lng: 128.9476 },
  "센텀시티": { lat: 35.1706, lng: 129.1323 },
  "중구": { lat: 35.1061, lng: 129.0325 },
  "해운대구": { lat: 35.1631, lng: 129.1597 },
  "동래구": { lat: 35.2061, lng: 129.0806 },
  "수영구": { lat: 35.1453, lng: 129.1141 },
  "남구": { lat: 35.1364, lng: 129.0845 },
  "서구": { lat: 35.0975, lng: 129.0243 },
  "북구": { lat: 35.1974, lng: 128.9918 },
  "금정구": { lat: 35.2841, lng: 129.0939 },
  "사상구": { lat: 35.1669, lng: 128.9874 },
  "사하구": { lat: 35.1056, lng: 128.9742 },
  "영도구": { lat: 35.0897, lng: 129.0758 },
  "부산진구": { lat: 35.1527, lng: 129.0532 },
  "부산": { lat: 35.1796, lng: 129.0756 } // 부산 중심 좌표
};

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
            // address 필드가 없으면 location을 주소로 사용
            address: item.location,
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
        dayCard.classList.add("active-day");
      } else {
        dayCard.style.display = "none";
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
      // 먼저 주소가 있으면 정제해서 사용, 없으면 장소명 사용
      var addressKeyword = "";
      var locationKeyword = schedule.location || "";

      // 주소가 location과 동일하면 부산 키워드 추가하기
      if (schedule.address && schedule.address === schedule.location) {
        // 이미 '부산'이 포함되어 있는지 확인
        if (!schedule.address.includes("부산")) {
          addressKeyword = "부산 " + schedule.address;
        } else {
          addressKeyword = schedule.address;
        }
      } else if (schedule.address) {
        addressKeyword = refineAddress(schedule.address);
      }

      if (!addressKeyword.trim() && !locationKeyword.trim()) {
        console.warn("검색할 주소/장소명이 없음");
        return Promise.resolve(null);
      }

      return new Promise((resolve) => {
        // 검색 시도 함수 (재귀적으로 호출됨)
        const trySearch = (keyword, isAddressSearch, retryCount = 0) => {
          if (!keyword || retryCount > 2) {
            console.warn("검색 시도 최대치 도달:", keyword, retryCount);
            resolve(false);
            return;
          }

          console.log(
            `${retryCount > 0 ? "재시도" : "검색"} ${
              isAddressSearch ? "주소" : "장소명"
            }:`,
            keyword
          );

          ps.keywordSearch(keyword, function (data, status) {
            console.log(
              "키워드 검색 결과:",
              keyword,
              "상태:",
              status,
              "결과 개수:",
              data ? data.length : 0
            );

            if (status === kakao.maps.services.Status.OK && data.length > 0) {
              var place = data[0];
              var marker = createCustomMarker(
                place,
                scheduleIndex + 1,
                schedule
              );
              markersByDay[dayIndex].push(marker);
              var latLng = new kakao.maps.LatLng(place.y, place.x);
              latLngArray.push(latLng);
              console.log(
                "마커 생성 성공:",
                place.place_name,
                place.address_name,
                `(${place.y}, ${place.x})`
              );
              resolve(true);
            } else {
              console.warn("검색 실패:", keyword, "상태:", status);
              
              // 주소 검색 실패 시, 장소명으로 시도
              if (isAddressSearch && locationKeyword) {
                console.log("주소 검색 실패, 장소명으로 재시도:", locationKeyword);
                // 부산 키워드 추가해서 시도
                if (!locationKeyword.includes("부산")) {
                  trySearch("부산 " + locationKeyword, false, 0);
                } else {
                  trySearch(locationKeyword, false, 0);
                }
              } 
              // 장소명에 '부산'이 포함되어 있지 않으면 '부산 + 장소명'으로 시도
              else if (!isAddressSearch && !keyword.includes("부산") && retryCount === 0) {
                trySearch("부산 " + keyword, false, retryCount + 1);
              }
              // 장소명에 구 이름이 없으면 임의의 구를 추가해서 시도 (예: 해운대구, 중구 등)
              else if (!isAddressSearch && !keyword.includes("구") && retryCount === 1) {
                // 부산 주요 구: 해운대구, 남구, 중구, 부산진구, 사하구 등
                trySearch("부산 해운대구 " + locationKeyword, false, retryCount + 1);
              } else {
                // 모든 검색 시도가 실패한 경우, 지역명 추출하여 기본 좌표 사용
                console.log("모든 검색 실패, 기본 좌표 사용:", locationKeyword);
                const regionName = extractRegionFromLocation(locationKeyword || keyword);
                const defaultCoord = busanDefaultCoords[regionName] || busanDefaultCoords["부산"];
                
                console.log(`지역 [${regionName}] 기본 좌표 사용:`, defaultCoord);
                
                // 기본 좌표로 가상 place 객체 생성
                const virtualPlace = {
                  place_name: locationKeyword || keyword,
                  address_name: `부산 ${regionName} 일대`,
                  x: defaultCoord.lng.toString(),
                  y: defaultCoord.lat.toString()
                };
                
                // 가상 위치에 마커 생성
                var marker = createCustomMarker(virtualPlace, scheduleIndex + 1, schedule);
                markersByDay[dayIndex].push(marker);
                var latLng = new kakao.maps.LatLng(defaultCoord.lat, defaultCoord.lng);
                latLngArray.push(latLng);
                console.log("기본 좌표 마커 생성:", virtualPlace.place_name, `(${defaultCoord.lat}, ${defaultCoord.lng})`);
                resolve(true);
              }
            }
          });
        };

        // 주소가 있으면 먼저 주소로 검색, 없으면 장소명으로 검색
        if (addressKeyword.trim()) {
          trySearch(addressKeyword, true);
        } else {
          trySearch(locationKeyword, false);
        }
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
    document.querySelector(".current-day-title .date").textContent = `${
      itineraryData.days[0].date
    } ~ ${itineraryData.days[itineraryData.days.length - 1].date}`;
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
      card.style.display = "block";
      card.classList.add("active-day");
    } else {
      card.style.display = "none";
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
  dayCards.forEach((card) => {
    card.style.display = "block";
    card.classList.remove("active-day");
  });

  // 모든 마커 표시
  markersByDay.forEach((markers, index) => {
    markers.forEach((marker) => {
      marker.setMap(map);
    });
  });

  // 경로는 표시하지 않음
  polylinesByDay.forEach((polyline) => {
    if (polyline) polyline.setMap(null);
  });
}

// 주소 정제 함수 추가
function refineAddress(address) {
  if (!address) return "";

  // "N/A" 문자열 체크
  if (address.trim().toUpperCase() === "N/A") return "";

  // 주소에서 불필요한 상세 번호 제거 (예: 1-1, 산7-9 등)
  let refined = address;

  // 도로명 주소에서 번지수 제거 (해운대해변로 264 -> 해운대해변로)
  if (refined.includes("로 ")) {
    refined = refined.replace(/로 \d+(-\d+)?/, "로");
  }

  // 동/가 주소에서 번지수 제거 (우동 140-9 -> 우동)
  if (refined.includes("동 ") || refined.includes("가 ")) {
    refined = refined.replace(/[동가] \d+(-\d+)?/, (match) =>
      match.substring(0, 1)
    );
  }

  // "산" 제거 (산7-9, 산 88-1 등)
  refined = refined.replace(/산\s*\d+(-\d+)?/, "");

  console.log("주소 정제:", address, "->", refined);
  return refined;
}

// 장소명이나 주소에서 지역명 추출 함수 추가
function extractRegionFromLocation(location) {
  if (!location) return "부산";
  
  const keywords = [
    "해운대", "서면", "광안리", "남포동", "송정", "기장", "영도", "사상", "동래", "부산역", 
    "김해공항", "센텀시티", "중구", "해운대구", "동래구", "수영구", "남구", "서구", "북구", 
    "금정구", "사상구", "사하구", "영도구", "부산진구"
  ];
  
  for (const keyword of keywords) {
    if (location.includes(keyword)) {
      return keyword;
    }
  }
  
  return "부산"; // 지역명을 추출할 수 없으면 부산 중심 좌표 사용
}
