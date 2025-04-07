// 기상청 API 관련 상수
const KMA_API_CONSTANTS = {
  API_URL: "/api/weather", // 백엔드 프록시 엔드포인트
  DEFAULT_LOCATION: {
    LAT: 35.1798, // 부산시청 위도
    LON: 129.075, // 부산시청 경도
  },
};

// 날씨 카테고리 코드 매핑
const CATEGORY_MAP = {
  PTY: {
    name: "강수형태",
    unit: "",
    codeValues: { 0: "없음", 1: "비", 2: "비/눈", 3: "눈", 4: "소나기" },
  },
  REH: { name: "습도", unit: "%" },
  RN1: { name: "1시간 강수량", unit: "mm" },
  T1H: { name: "기온", unit: "°C" },
  UUU: { name: "동서바람성분", unit: "m/s" },
  VEC: { name: "풍향", unit: "deg" },
  VVV: { name: "남북바람성분", unit: "m/s" },
  WSD: { name: "풍속", unit: "m/s" },
};

// 날짜 및 시간 포맷 함수
function formatDateTime() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  const hours = String(now.getHours()).padStart(2, "0");
  const minutes = String(now.getMinutes()).padStart(2, "0");

  return {
    formatted: `${year}년 ${month}월 ${day}일 ${hours}:${minutes}`,
    time: `${hours}:${minutes}`,
  };
}

// 가장 가까운 발표 시각 구하기
function getNearestBaseTime(date) {
  const hour = date.getHours();
  // 초단기실황 발표시각은 매시 30분이므로, 현재 시각의 정시를 base_time으로 사용
  return String(hour).padStart(2, "0") + "00";
}

// 풍향 각도를 방향으로 변환
function getWindDirection(degree) {
  const directions = [
    "북",
    "북동",
    "동",
    "남동",
    "남",
    "남서",
    "서",
    "북서",
    "북",
  ];
  const index = Math.round(parseInt(degree) / 45) % 8;
  return directions[index];
}

// 강수형태 코드를 텍스트로 변환
function getPrecipitationType(code) {
  return CATEGORY_MAP.PTY.codeValues[code] || "알 수 없음";
}

// 위도/경도를 기상청 좌표로 변환 (LCC DFS 좌표변환)
function convertToGridCoord(lat, lon) {
  const RE = 6371.00877; // 지구 반경(km)
  const GRID = 5.0; // 격자 간격(km)
  const SLAT1 = 30.0; // 투영 위도1(degree)
  const SLAT2 = 60.0; // 투영 위도2(degree)
  const OLON = 126.0; // 기준점 경도(degree)
  const OLAT = 38.0; // 기준점 위도(degree)
  const XO = 43; // 기준점 X좌표(GRID)
  const YO = 136; // 기준점 Y좌표(GRID)

  const DEGRAD = Math.PI / 180.0;
  const RADDEG = 180.0 / Math.PI;

  const re = RE / GRID;
  const slat1 = SLAT1 * DEGRAD;
  const slat2 = SLAT2 * DEGRAD;
  const olon = OLON * DEGRAD;
  const olat = OLAT * DEGRAD;

  let sn =
    Math.tan(Math.PI * 0.25 + slat2 * 0.5) /
    Math.tan(Math.PI * 0.25 + slat1 * 0.5);
  sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
  let sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
  sf = (Math.pow(sf, sn) * Math.cos(slat1)) / sn;
  let ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
  ro = (re * sf) / Math.pow(ro, sn);

  let ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
  ra = (re * sf) / Math.pow(ra, sn);
  let theta = lon * DEGRAD - olon;
  if (theta > Math.PI) theta -= 2.0 * Math.PI;
  if (theta < -Math.PI) theta += 2.0 * Math.PI;
  theta *= sn;

  let nx = Math.floor(ra * Math.sin(theta) + XO + 0.5);
  let ny = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

  return { nx, ny };
}

// 카카오맵 API 사용 전에 동적 로드 시도
function loadKakaoMapAPI(callback) {
  // 이미 로드되었는지 확인
  if (typeof kakao !== "undefined" && kakao.maps) {
    callback(true);
    return;
  }

  // 스크립트 동적 로드
  const script = document.createElement("script");
  script.src =
    "//dapi.kakao.com/v2/maps/sdk.js?appkey=YOUR_KAKAO_API_KEY&libraries=services&autoload=false";
  script.onload = function () {
    kakao.maps.load(function () {
      callback(true);
    });
  };
  script.onerror = function () {
    callback(false);
  };
  document.head.appendChild(script);
}

// 현재 위치 가져오기
function getCurrentLocation() {
  const weatherInfo = document.getElementById("weather-info");

  weatherInfo.innerHTML =
    '<div class="weather-loading"><i class="fas fa-spinner fa-spin"></i> 현재 위치와 날씨 정보를 불러오는 중...</div>';

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      // 성공 시 처리
      (position) => {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;
        console.log("위치 정보 수신 성공:", lat, lon);
        // 좌표로 주소 정보 가져오기
        getAddressFromCoords(lat, lon, (address) => {
          fetchWeatherData(lat, lon, address || "현재 위치");
        });
      },
      // 실패 시 처리
      (error) => {
        console.error("위치 정보를 가져오는데 실패했습니다:", error);
        // 기본 위치 (부산시청 좌표)로 대체
        const defaultLat = KMA_API_CONSTANTS.DEFAULT_LOCATION.LAT;
        const defaultLon = KMA_API_CONSTANTS.DEFAULT_LOCATION.LON;

        // 기본 위치의 주소 정보 가져오기
        getAddressFromCoords(defaultLat, defaultLon, (address) => {
          fetchWeatherData(
            defaultLat,
            defaultLon,
            address || "부산시청 (기본값)"
          );
        });

        weatherInfo.innerHTML += `<small class="weather-notice">※ 위치 정보에 접근할 수 없어 부산시청 기준으로 표시합니다.</small>`;
      },
      // 옵션
      {
        enableHighAccuracy: false,
        timeout: 5000,
        maximumAge: 0,
      }
    );
  } else {
    console.error("이 브라우저에서는 위치 정보를 지원하지 않습니다.");
    // 기본 위치 (부산시청 좌표)로 대체
    const defaultLat = KMA_API_CONSTANTS.DEFAULT_LOCATION.LAT;
    const defaultLon = KMA_API_CONSTANTS.DEFAULT_LOCATION.LON;

    // 기본 위치의 주소 정보 가져오기
    getAddressFromCoords(defaultLat, defaultLon, (address) => {
      fetchWeatherData(defaultLat, defaultLon, address || "부산시청 (기본값)");
    });

    weatherInfo.innerHTML += `<small class="weather-notice">※ 위치 정보 기능을 지원하지 않는 브라우저입니다. 부산시청 기준으로 표시합니다.</small>`;
  }
}

// 좌표를 주소로 변환하는 함수 (카카오맵 API 사용)
function getAddressFromCoords(lat, lon, callback) {
  // 카카오맵 API가 로드되었는지 확인
  if (typeof kakao === "undefined" || !kakao.maps || !kakao.maps.services) {
    // 콘솔 오류 메시지를 제거하고 간단한 로그만 남김
    console.log(
      "카카오맵 API를 사용할 수 없어 위치 정보 상세 변환을 건너뜁니다"
    );

    // 좌표 정보를 기반으로 간단한 위치 표시 (위도, 경도만 표시)
    const simpleLocation = `부산 (${lat.toFixed(6)}, ${lon.toFixed(6)})`;
    callback(simpleLocation);
    return;
  }

  // 카카오맵 API 로드 확인
  loadKakaoMapAPI(function (isLoaded) {
    if (isLoaded) {
      // 카카오맵 API가 로드된 경우
      // 좌표를 주소로 변환하는 객체 생성
      const geocoder = new kakao.maps.services.Geocoder();

      // 좌표를 주소로 변환
      geocoder.coord2Address(lon, lat, (result, status) => {
        if (status === kakao.maps.services.Status.OK) {
          // 도로명 주소 또는 지번 주소 가져오기
          let addressDetail = "";

          if (result[0].road_address) {
            // 도로명 주소가 있을 경우 (도로명 주소 우선 사용)
            const roadAddress = result[0].road_address;
            addressDetail = roadAddress.address_name;

            // 건물명이 있으면 추가
            if (roadAddress.building_name) {
              addressDetail += ` (${roadAddress.building_name})`;
            }
          } else if (result[0].address) {
            // 지번 주소만 있을 경우
            const jibunAddress = result[0].address;
            addressDetail = jibunAddress.address_name;
          }

          // 행정구역 정보 가져오기
          if (addressDetail) {
            callback(addressDetail);
          } else {
            callback(`부산 (${lat.toFixed(6)}, ${lon.toFixed(6)})`);
          }
        } else {
          console.log("주소 변환 실패:", status);
          callback(`부산 (${lat.toFixed(6)}, ${lon.toFixed(6)})`);
        }
      });

      // 행정구역 정보도 가져와 보다 상세한 정보 제공 (오류 발생 시 조용히 넘어가도록 try-catch 추가)
      try {
        geocoder.coord2RegionCode(lon, lat, (result, status) => {
          if (status === kakao.maps.services.Status.OK) {
            console.log("행정구역 정보:", result);
          }
        });
      } catch (error) {
        console.log("행정구역 정보 조회 중 오류 발생");
      }
    } else {
      // 로드 실패 시 기본 좌표 값 사용
      callback(`부산 (${lat.toFixed(6)}, ${lon.toFixed(6)})`);
    }
  });
}

// 날씨 데이터 가져오기
function fetchWeatherData(lat, lon, locationName) {
  const weatherInfo = document.getElementById("weather-info");

  console.log("날씨 데이터 요청:", lat, lon, locationName);

  weatherInfo.innerHTML = `
    <div class="weather-loading">
      <i class="fas fa-spinner fa-spin"></i> 날씨 정보를 불러오는 중...
      <div class="loading-location">현재 위치: ${locationName}</div>
    </div>
  `;

  fetch(`${KMA_API_CONSTANTS.API_URL}?lat=${lat}&lon=${lon}`)
    .then((response) => {
      if (!response.ok) {
        throw new Error("날씨 정보를 가져오는데 실패했습니다");
      }
      return response.json();
    })
    .then((data) => {
      console.log("날씨 데이터 수신:", data);
      if (data && data.condition && data.temperature) {
        displayWeather(data, locationName);
      } else {
        throw new Error("날씨 데이터 형식이 올바르지 않습니다");
      }
    })
    .catch((error) => {
      console.error("날씨 정보 에러:", error);
      weatherInfo.innerHTML = `
        <div class="weather-error">
          <i class="fas fa-cloud-rain"></i>
          <p>날씨 정보를 불러올 수 없습니다.<br>잠시 후 다시 시도해주세요.</p>
          <div class="weather-location-error">현재 위치: ${locationName}</div>
          <button id="retryWeather" class="weather-retry-btn">
            <i class="fas fa-sync-alt"></i> 다시 시도
          </button>
        </div>
      `;

      // 다시 시도 버튼 이벤트 추가
      document
        .getElementById("retryWeather")
        .addEventListener("click", function () {
          getCurrentLocation();
        });
    });
}

// 날씨 정보 표시
function displayWeather(data, locationName) {
  const weatherInfo = document.getElementById("weather-info");
  const dateTime = formatDateTime();

  // 날씨 아이콘 결정
  let weatherIcon, weatherClass;
  switch (data.condition) {
    case "맑음":
      weatherIcon = '<i class="fas fa-sun" style="color: #FFD700;"></i>';
      weatherClass = "weather-clear";
      break;
    case "구름많음":
      weatherIcon = '<i class="fas fa-cloud-sun" style="color: #87CEEB;"></i>';
      weatherClass = "weather-cloudy";
      break;
    case "흐림":
      weatherIcon = '<i class="fas fa-cloud" style="color: #808080;"></i>';
      weatherClass = "weather-cloudy";
      break;
    case "비":
      weatherIcon = '<i class="fas fa-cloud-rain" style="color: #4682B4;"></i>';
      weatherClass = "weather-rain";
      break;
    case "비/눈":
      weatherIcon = '<i class="fas fa-cloud-rain" style="color: #4682B4;"></i>';
      weatherClass = "weather-rain";
      break;
    case "눈":
      weatherIcon = '<i class="fas fa-snowflake" style="color: #87CEEB;"></i>';
      weatherClass = "weather-snow";
      break;
    default:
      weatherIcon = '<i class="fas fa-cloud" style="color: #808080;"></i>';
      weatherClass = "weather-cloudy";
  }

  // 강수량이 0이 아닌지 확인
  const hasRain =
    data.precipitation &&
    data.precipitation !== "0.0" &&
    data.precipitation !== "강수없음";

  // 주소 표시를 위해 길이가 너무 길 경우 축약
  let displayLocation = locationName;
  if (displayLocation.length > 30) {
    displayLocation = displayLocation.substring(0, 30) + "...";
  }

  // 날씨 HTML 구성 - 향상된 디자인 및 더 많은 정보
  weatherInfo.innerHTML = `
    <div class="weather-container ${weatherClass}">
      <div class="weather-header">
        <div class="weather-location" title="${locationName}">
          <span class="location-label">현재 위치:</span> <i class="fas fa-map-marker-alt"></i> ${displayLocation}
        </div>
        <div class="weather-time">${dateTime.time} 기준</div>
      </div>
      
      <div class="weather-content">
        <div class="weather-primary">
          <div class="weather-icon-large">${weatherIcon}</div>
          <div class="weather-info">
            <div class="weather-condition">${data.condition}</div>
            <div class="weather-temp">${data.temperature}°C</div>
          </div>
        </div>
        
        <div class="weather-details">
          <div class="weather-detail">
            <i class="fas fa-tint"></i>
            <span>습도 ${data.humidity}%</span>
          </div>
          
          <div class="weather-detail">
            <i class="fas fa-wind"></i>
            <span>풍속 ${
              data.windSpeed !== "N/A" ? data.windSpeed + "m/s" : "N/A"
            }</span>
          </div>
          
          <div class="weather-detail">
            <i class="fas fa-compass"></i>
            <span>풍향 ${data.windDirection}</span>
          </div>
          
          ${
            hasRain
              ? `
          <div class="weather-detail">
            <i class="fas fa-cloud-showers-heavy"></i>
            <span>강수량 ${data.precipitation}mm</span>
          </div>
          `
              : ""
          }
        </div>
      </div>

      <div class="weather-footer">
        <button id="refreshWeather" class="weather-refresh-btn">
          <i class="fas fa-sync-alt"></i> 새로고침
        </button>
      </div>
    </div>
  `;

  // 새로고침 버튼 이벤트 리스너 추가
  document
    .getElementById("refreshWeather")
    .addEventListener("click", function () {
      getCurrentLocation();
    });
}

// 페이지 로드 시 날씨 정보 가져오기
document.addEventListener("DOMContentLoaded", function () {
  // 페이지 로드 후 1초 지연시켜 날씨 정보 요청 (다른 리소스 로딩을 방해하지 않도록)
  setTimeout(function () {
    getCurrentLocation();
  }, 1000);

  // 30분마다 날씨 정보 업데이트
  setInterval(getCurrentLocation, 1800000);
});
