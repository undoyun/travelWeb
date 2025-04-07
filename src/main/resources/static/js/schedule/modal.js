// 리뷰 모달과 버튼 기능 초기화
document.addEventListener("DOMContentLoaded", function () {
  console.log("모달 스크립트 초기화 중...");

  // 모달 요소 참조
  const reviewModal = document.getElementById("reviewModal");
  const reviewBtn = document.getElementById("reviewBtn");
  const closeBtn = document.querySelector("#reviewModal .close");
  const cancelBtn = document.getElementById("cancelBtn");
  const reviewForm = document.getElementById("reviewForm");
  const reviewAlert = document.getElementById("reviewAlert");

  // 이미지 미리보기 관련
  const photoInput = document.getElementById("photos");
  const imagePreview = document.getElementById("imagePreview");

  // 다른 버튼 참조
  const editBtn = document.querySelector(".btn-primary");
  const shareBtn = document.querySelector(".btn-secondary");

  console.log("리뷰 버튼:", reviewBtn);
  console.log("수정 버튼:", editBtn);
  console.log("공유 버튼:", shareBtn);

  // 리뷰 버튼 클릭 이벤트
  if (reviewBtn) {
    console.log("리뷰 버튼에 이벤트 리스너 추가");
    reviewBtn.addEventListener("click", function () {
      console.log("리뷰 버튼 클릭됨");

      // 리뷰 작성 가능 여부 체크 (이미 작성한 리뷰가 있는지 확인)
      const scheduleId = getScheduleId();
      if (scheduleId) {
        checkCanReview(scheduleId);
      } else {
        alert("일정 ID를 찾을 수 없습니다.");
      }
    });
  }

  // 닫기 버튼 클릭 이벤트
  if (closeBtn) {
    closeBtn.addEventListener("click", function () {
      console.log("닫기 버튼 클릭됨");
      reviewModal.style.display = "none";
      resetForm();
    });
  }

  // 취소 버튼 클릭 이벤트
  if (cancelBtn) {
    cancelBtn.addEventListener("click", function () {
      console.log("취소 버튼 클릭됨");
      reviewModal.style.display = "none";
      resetForm();
    });
  }

  // 모달 외부 클릭 시 닫기
  window.addEventListener("click", function (e) {
    if (e.target === reviewModal) {
      console.log("모달 외부 클릭됨");
      reviewModal.style.display = "none";
      resetForm();
    }
  });

  // 별점 기능
  const stars = document.querySelectorAll("#reviewModal .star");
  const ratingInput = document.getElementById("rating");

  stars.forEach(function (star) {
    star.addEventListener("click", function () {
      const rating = parseInt(this.dataset.rating);
      console.log("별점 선택:", rating);
      ratingInput.value = rating;

      // 별점 시각화
      stars.forEach(function (s, index) {
        if (index < rating) {
          s.classList.add("active");
        } else {
          s.classList.remove("active");
        }
      });
    });

    // 호버 효과
    star.addEventListener("mouseover", function () {
      const rating = parseInt(this.dataset.rating);

      stars.forEach(function (s, index) {
        if (index < rating) {
          s.classList.add("hover");
        } else {
          s.classList.remove("hover");
        }
      });
    });

    // 호버 해제
    star.addEventListener("mouseout", function () {
      stars.forEach(function (s) {
        s.classList.remove("hover");
      });
    });
  });

  // 이미지 미리보기 기능
  if (photoInput) {
    photoInput.addEventListener("change", function () {
      console.log("이미지 선택됨");
      // 기존 미리보기 초기화
      imagePreview.innerHTML = "";

      // 최대 3개 파일 제한
      const maxFiles = 3;
      const files = Array.from(this.files).slice(0, maxFiles);

      if (this.files.length > maxFiles) {
        alert(`최대 ${maxFiles}개의 파일만 선택 가능합니다.`);
      }

      // 각 파일마다 미리보기 생성
      files.forEach(function (file) {
        if (!file.type.startsWith("image/")) {
          return;
        }

        const reader = new FileReader();

        reader.onload = function (e) {
          const previewItem = document.createElement("div");
          previewItem.className = "preview-item";

          // 이미지 요소
          const img = document.createElement("img");
          img.src = e.target.result;
          img.alt = file.name;

          // 삭제 버튼
          const removeBtn = document.createElement("div");
          removeBtn.className = "remove-image";
          removeBtn.innerHTML = '<i class="fas fa-times"></i>';
          removeBtn.addEventListener("click", function () {
            previewItem.remove();
          });

          previewItem.appendChild(img);
          previewItem.appendChild(removeBtn);
          imagePreview.appendChild(previewItem);
        };

        reader.readAsDataURL(file);
      });
    });
  }

  // 폼 제출 처리
  if (reviewForm) {
    console.log("리뷰 폼에 이벤트 리스너 추가");
    reviewForm.addEventListener("submit", function (e) {
      e.preventDefault(); // 기본 제출 방지
      console.log("폼 제출 시작");

      // 별점 검증
      const rating = parseInt(ratingInput.value);
      if (rating === 0) {
        alert("평점을 선택해주세요.");
        return;
      }

      // 제목 검증
      const title = document.getElementById("title").value.trim();
      if (!title) {
        alert("리뷰 제목을 입력해주세요.");
        return;
      }

      // 내용 검증
      const content = document.getElementById("content").value.trim();
      if (!content) {
        alert("리뷰 내용을 입력해주세요.");
        return;
      }

      console.log("폼 유효성 검사 통과, 데이터 준비 중");

      // FormData 객체 생성하여 폼 데이터 수집
      const formData = new FormData(reviewForm);

      // 사진 파일 수정
      if (photoInput.files.length > 0) {
        // photos로 전송되는 파일 확인
        console.log("선택된 파일 수:", photoInput.files.length);
        for (let i = 0; i < photoInput.files.length; i++) {
          console.log(
            `파일 ${i + 1}:`,
            photoInput.files[i].name,
            photoInput.files[i].size
          );
        }
      } else {
        console.log("선택된 파일 없음");
      }

      // 전송되는 폼 데이터 확인
      for (let pair of formData.entries()) {
        console.log(
          pair[0] + ": " + (pair[1] instanceof File ? pair[1].name : pair[1])
        );
      }

      // 서버에 전송
      fetch("/reviews/save", {
        method: "POST",
        body: formData,
        // credentials 포함 (쿠키와 인증 정보)
        credentials: "same-origin",
      })
        .then(response => {
          console.log("서버 응답 상태:", response.status);
          if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
              throw new Error('인증 오류: 로그인이 필요합니다.');
            }
            return response.text().then(text => { throw new Error(text) });
          }
          // 리다이렉트 URL 추출하기 위해 전체 응답 가져오기
          return response;
        })
        .then(response => {
          // 서버가 리다이렉트 URL을 헤더로 보냈는지 확인
          const location = response.headers.get('Location');
          if (location) {
            console.log("리다이렉트 위치:", location);
            window.location.href = location;
            return;
          }
          
          // 응답 본문도 확인
          return response.text().then(data => {
            console.log("서버 응답 데이터:", data);
            
            // 응답에 리다이렉트 URL이 포함되어 있는지 확인
            if (data.includes("redirect:")) {
              const redirectUrl = data.split("redirect:")[1].trim();
              console.log("리다이렉트 URL 추출:", redirectUrl);
              window.location.href = redirectUrl;
            }
            // HTML 응답인 경우 (서버가 리다이렉트 대신 HTML을 반환)
            else if (data.includes("<!DOCTYPE html>")) {
              console.log("서버가 HTML을 반환함");
              
              // 리뷰 ID 추출 시도
              const match = data.match(/\/reviews\/(\d+)/);
              if (match && match[1]) {
                const reviewId = match[1];
                console.log("추출된 리뷰 ID:", reviewId);
                window.location.href = `/reviews/${reviewId}`;
              } else {
                // 리뷰 목록 페이지로 리다이렉트
                window.location.href = '/reviews/list';
              }
            } 
            else {
              alert('리뷰가 성공적으로 등록되었습니다.');
              // 리뷰 목록 페이지로 이동
              window.location.href = '/reviews/list';
            }
          });
        })
        .catch((error) => {
          console.error("리뷰 저장 오류:", error);
          alert("리뷰 저장 중 오류가 발생했습니다: " + error.message);
        });
    });
  }

  // 일정 수정 버튼
  if (editBtn) {
    console.log("수정 버튼에 이벤트 리스너 추가");
    editBtn.addEventListener("click", function () {
      console.log("수정 버튼 클릭됨");
      // scheduleId 가져오기
      const scheduleId = getScheduleId();
      if (scheduleId) {
        window.location.href = `/schedule/edit/${scheduleId}`;
      } else {
        alert("일정 ID를 찾을 수 없습니다.");
      }
    });
  }

  // 공유하기 버튼
  if (shareBtn) {
    console.log("공유 버튼에 이벤트 리스너 추가");
    shareBtn.addEventListener("click", function () {
      console.log("공유 버튼 클릭됨");
      // 현재 페이지 URL
      const url = window.location.href;

      // 모바일 네이티브 공유 API
      if (navigator.share) {
        navigator
          .share({
            title: document.title,
            text: "여행 일정을 확인해보세요!",
            url: url,
          })
          .then(() => console.log("공유 성공"))
          .catch((error) => console.log("공유 오류:", error));
      } else {
        // 클립보드 복사
        copyToClipboard(url);
        alert("URL이 클립보드에 복사되었습니다.");
      }
    });
  }

  // 리뷰 작성 가능 여부 체크 함수
  function checkCanReview(scheduleId) {
    fetch(`/reviews/check?scheduleId=${scheduleId}`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("인증 오류: 로그인이 필요합니다.");
        }
        return response.json();
      })
      .then((data) => {
        if (data.canReview) {
          // 리뷰 작성 가능
          reviewAlert.style.display = "none";
          reviewModal.style.display = "block";
        } else {
          // 이미 리뷰를 작성했거나 권한이 없음
          reviewAlert.style.display = "block";
          reviewModal.style.display = "block";
          // 폼을 비활성화
          disableForm();
        }
      })
      .catch((error) => {
        console.error("리뷰 가능 여부 확인 중 오류:", error);
        // 로그인 페이지로 리다이렉트
        if (error.message.includes("인증 오류")) {
          alert("리뷰를 작성하려면 로그인이 필요합니다.");
          window.location.href =
            "/login?redirect=" + encodeURIComponent(window.location.href);
        } else {
          alert("오류가 발생했습니다: " + error.message);
        }
      });
  }

  // 폼 비활성화 함수
  function disableForm() {
    // 폼 요소들을 읽기 전용으로 변경
    const formElements = reviewForm.querySelectorAll(
      'input, textarea, button[type="submit"]'
    );
    formElements.forEach((element) => {
      if (element.type === "submit") {
        element.disabled = true;
      } else {
        element.readOnly = true;
      }
    });

    // 별점 클릭 비활성화
    stars.forEach((star) => {
      star.style.pointerEvents = "none";
      star.style.opacity = "0.5";
    });

    // 파일 업로드 비활성화
    if (photoInput) {
      photoInput.disabled = true;
      document.querySelector(".custom-file-upload").style.opacity = "0.5";
      document.querySelector(".custom-file-upload").style.pointerEvents =
        "none";
    }
  }

  // 폼 리셋 함수
  function resetForm() {
    if (reviewForm) {
      reviewForm.reset();
    }

    // 별점 초기화
    stars.forEach(function (star) {
      star.classList.remove("active");
    });

    if (ratingInput) {
      ratingInput.value = 0;
    }

    // 이미지 미리보기 초기화
    if (imagePreview) {
      imagePreview.innerHTML = "";
    }
  }

  // 클립보드 복사 함수
  function copyToClipboard(text) {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.style.position = "fixed";
    document.body.appendChild(textarea);
    textarea.select();

    try {
      document.execCommand("copy");
    } catch (err) {
      console.error("클립보드 복사 실패:", err);
    }

    document.body.removeChild(textarea);
  }

  // scheduleId 가져오기
  function getScheduleId() {
    // URL에서 추출
    const pathParts = window.location.pathname.split("/");
    const id = pathParts[pathParts.length - 1];

    if (id && !isNaN(id)) {
      return id;
    }

    // hidden input에서 추출
    const hiddenInput = document.querySelector('input[name="scheduleId"]');
    if (hiddenInput) {
      return hiddenInput.value;
    }

    return null;
  }

  console.log("모달 스크립트 초기화 완료");
});
