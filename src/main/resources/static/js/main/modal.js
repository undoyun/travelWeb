// 일정 수정/삭제 모달 열기
function showEventModal(event, calendar) {
  if (!event.id) {
    showToast("일정 ID를 찾을 수 없습니다.", "error");
    return;
  }

  console.log("수정 요청 ID:", event.id);

  // 이벤트 ID를 폼에 설정 - 이 부분이 누락되어 있음!!!
  document.getElementById("eventId").value = event.id;

  // 이벤트 카테고리 가져오기 (없으면 기본값 1)
  const category = event.extendedProps.category || 1;

  // 읽기 모드 정보 설정
  document.getElementById("eventTitleView").textContent = event.title;

  // 날짜 표시 형식 설정
  const startDate = new Date(event.start);
  const endDate = event.end ? new Date(event.end) : startDate;
  const formattedStartDate = `${startDate.getFullYear()}년 ${
    startDate.getMonth() + 1
  }월 ${startDate.getDate()}일`;
  const formattedEndDate = `${endDate.getFullYear()}년 ${
    endDate.getMonth() + 1
  }월 ${endDate.getDate()}일`;

  document.getElementById("eventDateView").textContent =
    formattedStartDate === formattedEndDate
      ? formattedStartDate
      : `${formattedStartDate} ~ ${formattedEndDate}`;

  document.getElementById("eventDescriptionView").textContent =
    event.extendedProps.description || "설명이 없습니다.";

  // 수정 모드 폼 설정
  document.getElementById("eventTitle").value = event.title;
  document.getElementById("eventDescription").value =
    event.extendedProps.description || "";

  // 카테고리 선택 설정
  if (document.getElementById("eventCategory")) {
    document.getElementById("eventCategory").value = category;

    // 색상 미리보기 활성화
    document.querySelectorAll(".color-preview").forEach((preview) => {
      preview.classList.remove("active");
    });
    document
      .querySelector(`.color-preview-${category}`)
      .classList.add("active");
  }

  // 초기 상태: 읽기 모드 표시, 수정 모드 숨김
  document.getElementById("eventInfoView").style.display = "block";
  document.getElementById("eventEditForm").style.display = "none";

  // 버튼 상태 초기화
  document.getElementById("editEvent").style.display = "inline-flex"; // 수정 버튼 보이기
  document.getElementById("saveEvent").style.display = "none"; // 저장 버튼 숨김
  document.getElementById("deleteEvent").style.display = "inline-flex"; // 삭제 버튼 보이기

  // 모달 열기
  document.getElementById("eventModal").style.display = "block";

  // 수정 버튼 클릭 시 수정 모드로 전환
  document.getElementById("editEvent").onclick = function () {
    // 읽기 모드 숨김, 수정 모드 표시
    document.getElementById("eventInfoView").style.display = "none";
    document.getElementById("eventEditForm").style.display = "block";

    // 버튼 전환: "수정" → "저장"
    document.getElementById("editEvent").style.display = "none";
    document.getElementById("saveEvent").style.display = "inline-flex";
  };

  // 저장 버튼 클릭 시 서버로 업데이트 요청
  document.getElementById("saveEvent").onclick = function () {
    const eventId = document.getElementById("eventId").value;
    const eventTitle = document.getElementById("eventTitle").value;
    const eventDescription = document.getElementById("eventDescription").value;

    // 카테고리 값 가져오는 부분 수정
    let categoryValue = 1; // 기본값
    const selectedCategory = document.querySelector(
      'input[name="eventCategory"]:checked'
    );
    if (selectedCategory) {
      categoryValue = selectedCategory.value;
    } else if (document.getElementById("eventCategory")) {
      // select 요소인 경우
      categoryValue = document.getElementById("eventCategory").value;
    }

    console.log(
      "저장 요청:",
      eventId,
      eventTitle,
      eventDescription,
      categoryValue
    );

    // 이벤트 ID가 없으면 오류 메시지 표시
    if (!eventId) {
      showToast("일정 ID가 없어 수정할 수 없습니다.", "error");
      return;
    }

    // 저장 요청 전송
    fetch(`/api/events/${eventId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        planName: eventTitle,
        planDescription: eventDescription,
        category: categoryValue,
      }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("일정 수정 중 오류가 발생했습니다");
        }
        return response.json();
      })
      .then((data) => {
        console.log("일정 수정 성공:", data);

        // 캘린더 이벤트 업데이트
        const updatedEvent = calendar.getEventById(eventId);
        if (updatedEvent) {
          updatedEvent.setProp("title", eventTitle);
          updatedEvent.setExtendedProp("description", eventDescription);

          // 카테고리 변경 시 클래스 업데이트
          const newClassName = `event-category-${categoryValue}`;
          updatedEvent.setProp("classNames", [newClassName]);
        }

        // 모달 닫기
        closeModal("eventModal");

        // 성공 메시지 표시
        showToast("일정이 성공적으로 수정되었습니다", "success");
      })
      .catch((error) => {
        console.error("일정 수정 실패:", error);
        showToast("일정 수정 중 오류가 발생했습니다", "error");
      });
  };

  // 삭제 버튼 클릭 시 일정 삭제
  document.getElementById("deleteEvent").onclick = function () {
    if (confirm(`"${event.title}" 일정을 삭제하시겠습니까?`)) {
      const eventTitle = event.title; // 삭제 전 제목 저장

      fetch(`/api/events/${event.id}`, { method: "DELETE" })
        .then((response) => {
          if (!response.ok) throw new Error("삭제 실패");
          return response.text();
        })
        .then(() => {
          console.log("일정 삭제 완료");
          event.remove(); // 캘린더에서 이벤트 삭제
          closeModal("eventModal");

          // 성공 메시지 표시
          showToast("일정이 성공적으로 삭제되었습니다.");

          // 다가오는 여행 정보 업데이트
          fetch("/api/events")
            .then((response) => response.json())
            .then((events) => {
              updateUpcomingTrips(events);
            });
        })
        .catch((error) => {
          showToast("일정 삭제 실패: " + error.message, "error");
          console.error("일정 삭제 오류:", error);
        });
    }
  };

  // 취소 버튼 및 닫기 버튼 클릭 시 모달 닫기
  document.getElementById("cancelEvent").onclick = function () {
    closeModal("eventModal");
  };

  document.querySelectorAll(".close").forEach(function (closeBtn) {
    closeBtn.onclick = function () {
      closeModal(this.closest(".modal").id);
    };
  });
}

// 이벤트 날짜 업데이트 (드래그 앤 드롭, 리사이징)
function updateEventDates(event, calendar) {
  console.log("일정 날짜 업데이트:", event.id, event.start, event.end);

  fetch(`/api/events/${event.id}/dates`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      startDate: event.start.toISOString().slice(0, 10),
      endDate: event.end
        ? event.end.toISOString().slice(0, 10)
        : event.start.toISOString().slice(0, 10),
    }),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("날짜 업데이트 실패");
      }
      return response.json();
    })
    .then((updatedEvent) => {
      console.log("✅ 날짜 업데이트 완료:", updatedEvent);
      showToast("일정 날짜가 변경되었습니다.");

      // 다가오는 여행 정보 업데이트
      fetch("/api/events")
        .then((response) => response.json())
        .then((events) => {
          updateUpcomingTrips(events);
        });
    })
    .catch((error) => {
      console.error("❌ 날짜 업데이트 실패:", error);
      // 업데이트 실패 시 원래 위치로 되돌리기
      calendar.refetchEvents();
    });
}

// 모달 닫기 함수
function closeModal(modalId) {
  document.getElementById(modalId).style.display = "none";
}

// 일정 불러오기
function loadEvents(calendar) {
  console.log(
    "📌 일정 불러오기 시작 - 캘린더에 현재 로그인한 사용자의 일정만 표시됩니다"
  );

  fetch("/api/events")
    .then((response) => response.json())
    .then((events) => {
      console.log(
        `✅ 불러온 일정 데이터: 총 ${events.length}개의 일정이 로드되었습니다`
      );

      // events가 배열인지 확인하고, 배열이 아니면 빈 배열로 처리
      if (!Array.isArray(events)) {
        console.warn(
          "⚠️ 서버에서 반환된 일정 데이터가 배열이 아닙니다:",
          events
        );
        events = [];
      }

      events.forEach((event) => {
        console.log(`🛠 캘린더에 추가할 일정: ${event.id} - ${event.title}`);

        // 서버에서 저장된 카테고리 사용 (없으면 기본값 1)
        const categoryNum = event.category || 1;
        const className = `event-category-${categoryNum}`;

        calendar.addEvent({
          id: event.id,
          title: event.title || "제목 없음",
          start: event.start,
          end: event.end,
          className: className, // 색상 클래스 추가
          extendedProps: {
            description: event.description || "설명이 없습니다.",
            category: categoryNum, // 카테고리 정보 저장
          },
        });
      });

      // 다가오는 여행 정보 업데이트
      updateUpcomingTrips(events);

      // 일정이 없는 경우 메시지 표시 (첫 사용자용)
      if (events.length === 0) {
        console.log(
          "📅 일정이 없습니다. 로그인 상태와 사용자 권한을 확인하세요."
        );
        // 일정이 없을 때는 오류가 아니므로 일반 정보 메시지 표시
        showToast("등록된 일정이 없습니다. 새 일정을 추가해보세요.", "info");
      }
    })
    .catch((error) => {
      console.error("❌ 일정 불러오기 실패: ", error);
      // 에러 발생 시 빈 배열로 다가오는 여행 정보 초기화
      updateUpcomingTrips([]);
      showToast("일정을 불러오는 중 오류가 발생했습니다.", "error");
    });
}

// 다가오는 여행 정보 업데이트
function updateUpcomingTrips(events) {
  // 현재 날짜
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // 다가오는 여행만 필터링 (시작일이 오늘 이후인 것)
  const upcomingEvents = events
    .filter((event) => new Date(event.start) >= today)
    .sort((a, b) => new Date(a.start) - new Date(b.start))
    .slice(0, 3); // 최대 3개만 표시

  // 다가오는 여행 수 업데이트 (통계 카드)
  const upcomingTripsCount = document.querySelector(
    ".stat-card:nth-child(2) .stat-info p"
  );
  if (upcomingTripsCount) {
    upcomingTripsCount.textContent = upcomingEvents.length;
  }

  // 다음 여행까지 남은 일수 업데이트 (통계 카드)
  const daysToNextTrip = document.querySelector(
    ".stat-card:nth-child(4) .stat-info p"
  );
  if (daysToNextTrip && upcomingEvents.length > 0) {
    const nextTripDate = new Date(upcomingEvents[0].start);
    const daysUntil = Math.ceil((nextTripDate - today) / (1000 * 60 * 60 * 24));
    daysToNextTrip.textContent = `${daysUntil}일`;
  } else if (daysToNextTrip) {
    daysToNextTrip.textContent = "계획 없음";
  }

  // 다가오는 여행 카드 업데이트
  const tripsContainer = document.querySelector(".trips-container");
  if (tripsContainer) {
    if (upcomingEvents.length === 0) {
      // 다가오는 여행이 없는 경우
      tripsContainer.innerHTML = `
          <div class="empty-trips">
            <p>예정된 여행이 없습니다</p>
            <a href="/itineraries/itineraryCreate" class="create-trip-btn">새 여행 계획하기</a>
          </div>
        `;
    } else {
      // 다가오는 여행이 있는 경우
      let tripCardsHTML = '<div class="trip-cards">';

      upcomingEvents.forEach((event) => {
        const startDate = new Date(event.start);
        const endDate = new Date(event.end || event.start);
        const daysUntil = Math.ceil(
          (startDate - today) / (1000 * 60 * 60 * 24)
        );
        const duration = Math.ceil(
          (endDate - startDate) / (1000 * 60 * 60 * 24)
        );

        tripCardsHTML += `
            <div class="trip-card" data-id="${event.id}">
              <div class="trip-date">
                <span class="date-number">${startDate.getDate()}</span>
                <span class="date-month">${startDate.getMonth() + 1}월</span>
              </div>
              <div class="trip-info">
                <h4>${event.title || "제목 없음"}</h4>
                <p>
                  <i class="fas fa-clock"></i>
                  <span>${daysUntil}일 남음</span>
                </p>
                <p>
                  <i class="fas fa-calendar-day"></i>
                  <span>${duration}일간</span>
                </p>
              </div>
              <a href="/itineraries/${event.id}" class="trip-view-btn">
                <i class="fas fa-arrow-right"></i>
              </a>
            </div>
          `;
      });

      tripCardsHTML += "</div>";
      tripsContainer.innerHTML = tripCardsHTML;

      // 여행 카드 클릭 이벤트 추가
      document.querySelectorAll(".trip-card").forEach((card) => {
        card.addEventListener("click", function (e) {
          // 상세 페이지 링크 클릭 시 이벤트 전파 방지
          if (e.target.closest(".trip-view-btn")) {
            return;
          }

          const eventId = this.getAttribute("data-id");
          const event = calendar.getEventById(eventId);

          if (event) {
            calendar.gotoDate(event.start);
          }
        });
      });
    }
  }
}

// 토스트 메시지 표시 함수
function showToast(message, type = "success") {
  // 기존 토스트 제거
  const existingToast = document.querySelector(".toast");
  if (existingToast) {
    existingToast.remove();
  }

  // 새 토스트 생성
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;

  // 타입에 따른 아이콘 설정
  let icon = "check-circle";
  if (type === "error") icon = "exclamation-circle";
  else if (type === "warning") icon = "exclamation-triangle";
  else if (type === "info") icon = "info-circle";

  toast.innerHTML = `
          <i class="fas fa-${icon}"></i>
          <span>${message}</span>
      `;

  // 문서에 토스트 추가
  document.body.appendChild(toast);

  // 토스트 표시 애니메이션
  setTimeout(() => {
    toast.classList.add("show");
  }, 10);

  // 3초 후 토스트 제거
  setTimeout(() => {
    toast.classList.remove("show");
    setTimeout(() => {
      toast.remove();
    }, 300);
  }, 3000);
}

// 모달 외부 클릭 시 닫기
window.addEventListener("click", function (event) {
  const modals = document.querySelectorAll(".modal");
  modals.forEach((modal) => {
    if (event.target === modal) {
      closeModal(modal.id);
    }
  });
});

// ESC 키 누를 때 모달 닫기
window.addEventListener("keydown", function (event) {
  if (event.key === "Escape") {
    const openModals = document.querySelectorAll(
      '.modal[style*="display: block"]'
    );
    openModals.forEach((modal) => {
      closeModal(modal.id);
    });
  }
});

// 색상 미리보기 클릭 이벤트 처리
document.addEventListener("DOMContentLoaded", function () {
  // 새 일정 모달의 색상 미리보기
  document
    .querySelectorAll("#newEventModal .color-preview")
    .forEach((preview) => {
      preview.addEventListener("click", function () {
        const category = this.getAttribute("data-category");
        document.getElementById("newEventCategory").value = category;

        // 활성 클래스 변경
        document
          .querySelectorAll("#newEventModal .color-preview")
          .forEach((p) => {
            p.classList.remove("active");
          });
        this.classList.add("active");
      });
    });

  // 수정 모달의 색상 미리보기
  document.querySelectorAll("#eventModal .color-preview").forEach((preview) => {
    preview.addEventListener("click", function () {
      const category = this.getAttribute("data-category");
      document.getElementById("eventCategory").value = category;

      // 활성 클래스 변경
      document.querySelectorAll("#eventModal .color-preview").forEach((p) => {
        p.classList.remove("active");
      });
      this.classList.add("active");
    });
  });
});
