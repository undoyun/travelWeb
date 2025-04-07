document.addEventListener('DOMContentLoaded', function () {
    // 전역 변수
    let itineraryData = null;
    const itineraryContainer = document.getElementById('itineraryContainer');
    const itineraryArea = document.getElementById('itineraryArea');
    const btnAddDay = document.getElementById('btnAddDay');

    // 모달 요소
    const confirmModal = document.getElementById('confirmModal');
    const btnOpenModal = document.getElementById('btnOpenModal');
    const btnCloseModal = document.getElementById('btnCloseModal');
    const btnCancelModal = document.getElementById('btnCancelModal');
    const btnSubmitModal = document.getElementById('btnSubmitModal');

    // 사진 업로드 관련 요소
    const btnSelectPhoto = document.getElementById('btnSelectPhoto');
    const planPhotoUpload = document.getElementById('planPhotoUpload');
    const photoPreview = document.getElementById('photoPreview');
    const planPhoto = document.getElementById('planPhoto');

    // 도움말 모달 제어
    const helpBtn = document.getElementById('helpBtn');
    const helpModal = document.getElementById('helpModal');
    const closeHelpModal = document.getElementById('closeHelpModal');

    helpBtn.addEventListener('click', function () {
        helpModal.style.display = 'block';
    });

    closeHelpModal.addEventListener('click', function () {
        helpModal.style.display = 'none';
    });

    window.addEventListener('click', function (event) {
        if (event.target === helpModal) {
            helpModal.style.display = 'none';
        }
    });

    // 새 날짜 추가 버튼 이벤트
    btnAddDay.addEventListener('click', function () {
        addNewDay();
    });

    // 일정 데이터 로드
    loadItineraryData();

    function loadItineraryData() {
        try {
            // 로딩 스피너 표시
            itineraryContainer.innerHTML = `
                <div class="loading-spinner">
                    <i class="fas fa-spinner"></i>
                </div>
            `;

            // JSON 문자열 가져오기
            const jsonData = itineraryArea.value;
            console.log("JSON 데이터:", jsonData);

            // JavaScript로 직접 파싱
            try {
                const parsedData = JSON.parse(jsonData);
                console.log("파싱된 데이터:", parsedData);
 
                // 데이터 구조 변환
                if (parsedData.travelPlan && parsedData.travelPlan.itinerary) {
                    // travelPlan.itinerary 구조를 days 구조로 변환
                    itineraryData = {
                        days: parsedData.travelPlan.itinerary.map(day => {
                            return {
                                date: day.date,
                                schedules: day.schedule.map(item => {
                                    return {
                                        time: item.departureTime,
                                        content: `${item.category} - ${item.location} (${item.duration}, ${item.cost})`
                                    };
                                })
                            };
                        }),
                        // 원본 데이터도 저장
                        originalData: parsedData
                    };
                    console.log("변환된 데이터:", itineraryData);
                } else {
                    // 다른 구조인 경우 기본 구조 생성
                    itineraryData = {
                        days: [{
                            date: new Date().toISOString().split('T')[0],
                            schedules: [{
                                time: '09:00',
                                content: '새 일정'
                            }]
                        }],
                        originalData: parsedData
                    };
                    console.error("지원되지 않는 데이터 구조:", parsedData);
                }

                // 로딩 스피너 제거 및 일정 렌더링
                setTimeout(() => {
                    renderItinerary();
                }, 500);
            } catch (parseError) {
                console.error('JSON 파싱 오류:', parseError);
                itineraryContainer.innerHTML = `
                    <div class="error-message">
                        <i class="fas fa-exclamation-triangle"></i>
                        <p>일정 데이터를 불러오는 중 오류가 발생했습니다.</p>
                        <p>다시 시도해주세요.</p>
                    </div>
                `;
            }
        } catch (error) {
            console.error('일정 데이터 로드 오류:', error);
            itineraryContainer.innerHTML = `
                <div class="error-message">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p>일정 데이터를 불러오는 중 오류가 발생했습니다.</p>
                    <p>다시 시도해주세요.</p>
                </div>
            `;
        }
    }

    // 일정 렌더링 함수
    function renderItinerary() {
        itineraryContainer.innerHTML = '';

        if (!itineraryData || !itineraryData.days || itineraryData.days.length === 0) {
            itineraryContainer.innerHTML = `
                <div class="empty-itinerary">
                    <i class="fas fa-calendar-times"></i>
                    <p>일정 데이터가 없습니다.</p>
                    <p>아래 버튼을 클릭하여 새 날짜를 추가해보세요.</p>
                </div>
            `;
            return;
        }

        // 각 날짜별 일정 렌더링
        itineraryData.days.forEach((day, dayIndex) => {
            const dayContainer = createDayElement(day, dayIndex);
            itineraryContainer.appendChild(dayContainer);

            // 일정 항목 렌더링
            const scheduleContainer = dayContainer.querySelector('.schedule-container');
            day.schedules.forEach((schedule, scheduleIndex) => {
                const scheduleItem = createScheduleElement(schedule, dayIndex, scheduleIndex);
                scheduleContainer.appendChild(scheduleItem);
            });

            // 드래그 앤 드롭 설정
            setupDragAndDrop(scheduleContainer);
        });

        // 날짜 컨테이너 드래그 앤 드롭 설정
        setupDaysDragAndDrop();
    }

    // 날짜 요소 생성 함수
    function createDayElement(day, dayIndex) {
        const template = document.getElementById('dayTemplate');
        const dayElement = template.content.cloneNode(true).querySelector('.day-container');

        // 날짜 정보 설정
        dayElement.dataset.dayIndex = dayIndex;
        dayElement.querySelector('.day-number').textContent = dayIndex + 1;
        dayElement.querySelector('.day-date').textContent = day.date;

        // 일정 추가 버튼 이벤트
        dayElement.querySelector('.add-schedule').addEventListener('click', function () {
            addNewSchedule(dayIndex);
        });

        // 날짜 삭제 버튼 이벤트
        dayElement.querySelector('.delete-day').addEventListener('click', function () {
            if (confirm('이 날짜의 모든 일정이 삭제됩니다. 계속하시겠습니까?')) {
                deleteDay(dayIndex);
            }
        });

        return dayElement;
    }

    // 일정 요소 생성 함수
    function createScheduleElement(schedule, dayIndex, scheduleIndex) {
        const template = document.getElementById('scheduleItemTemplate');
        const scheduleElement = template.content.cloneNode(true).querySelector('.schedule-item');

        // 일정 정보 설정
        scheduleElement.dataset.dayIndex = dayIndex;
        scheduleElement.dataset.scheduleIndex = scheduleIndex;

        // 시간 설정
        const timeInput = scheduleElement.querySelector('.time-input');
        timeInput.value = schedule.time || '09:00';
        timeInput.addEventListener('change', function () {
            updateScheduleData(dayIndex, scheduleIndex, 'time', this.value);
        });

        // 내용에서 카테고리, 위치, 소요시간, 비용 추출
        let category = "관광";
        let location = "";
        let duration = "1시간";
        let cost = "0 KRW";
        
        // 기존 내용 파싱
        if (schedule.content) {
            const contentParts = schedule.content.split(' - ');
            if (contentParts.length >= 2) {
                category = contentParts[0].trim();
                
                // 위치와 (시간, 비용) 분리
                const locationParts = contentParts[1].split(' (');
                if (locationParts.length >= 2) {
                    location = locationParts[0].trim();
                    
                    // (시간, 비용) 파싱
                    const detailsPart = locationParts[1].replace(')', '').split(', ');
                    if (detailsPart.length >= 2) {
                        duration = detailsPart[0].trim();
                        cost = detailsPart[1].trim();
                    } else if (detailsPart.length === 1) {
                        duration = detailsPart[0].trim();
                    }
                } else {
                    location = locationParts[0].trim();
                }
            } else {
                location = schedule.content;
            }
        }
        
        // 카테고리 설정
        const categorySelect = scheduleElement.querySelector('.category-select');
        categorySelect.value = category;
        categorySelect.addEventListener('change', function() {
            updateContentField(dayIndex, scheduleIndex, scheduleElement);
        });
        
        // 위치 설정
        const locationInput = scheduleElement.querySelector('.location-input');
        locationInput.value = location;
        locationInput.addEventListener('change', function() {
            updateContentField(dayIndex, scheduleIndex, scheduleElement);
        });
        
        // 소요시간 설정
        const durationInput = scheduleElement.querySelector('.duration-input');
        durationInput.value = duration;
        durationInput.addEventListener('change', function() {
            updateContentField(dayIndex, scheduleIndex, scheduleElement);
        });
        
        // 비용 설정
        const costInput = scheduleElement.querySelector('.cost-input');
        costInput.value = cost;
        costInput.addEventListener('change', function() {
            updateContentField(dayIndex, scheduleIndex, scheduleElement);
        });

        // 삭제 버튼 이벤트
        scheduleElement.querySelector('.delete-schedule').addEventListener('click', function () {
            deleteSchedule(dayIndex, scheduleIndex);
        });

        return scheduleElement;
    }
    
    // 내용 필드 업데이트 함수
    function updateContentField(dayIndex, scheduleIndex, scheduleElement) {
        const category = scheduleElement.querySelector('.category-select').value;
        const location = scheduleElement.querySelector('.location-input').value;
        const duration = scheduleElement.querySelector('.duration-input').value;
        const cost = scheduleElement.querySelector('.cost-input').value;
        
        // 내용 형식: "카테고리 - 위치 (시간, 비용)"
        const content = `${category} - ${location} (${duration}, ${cost})`;
        
        // 데이터 업데이트
        updateScheduleData(dayIndex, scheduleIndex, 'content', content);
    }

    // 일정 데이터 업데이트 함수
    function updateScheduleData(dayIndex, scheduleIndex, field, value) {
        itineraryData.days[dayIndex].schedules[scheduleIndex][field] = value;
        updateItineraryJson();
    }

    // 새 일정 추가 함수
    function addNewSchedule(dayIndex) {
        const newSchedule = {
            time: '12:00',
            content: '새 일정'
        };

        itineraryData.days[dayIndex].schedules.push(newSchedule);

        // 새 일정 요소 생성 및 추가
        const scheduleContainer = document.querySelector(`.day-container[data-day-index="${dayIndex}"] .schedule-container`);
        const scheduleElement = createScheduleElement(newSchedule, dayIndex, itineraryData.days[dayIndex].schedules.length - 1);
        scheduleContainer.appendChild(scheduleElement);

        updateItineraryJson();
    }

    // 일정 삭제 함수
    function deleteSchedule(dayIndex, scheduleIndex) {
        // 데이터에서 일정 삭제
        itineraryData.days[dayIndex].schedules.splice(scheduleIndex, 1);

        // UI 업데이트
        renderItinerary();
        updateItineraryJson();
    }

    // 날짜 삭제 함수
    function deleteDay(dayIndex) {
        // 데이터에서 날짜 삭제
        itineraryData.days.splice(dayIndex, 1);

        // UI 업데이트
        renderItinerary();
        updateItineraryJson();
    }

    // 새 날짜 추가 함수
    function addNewDay() {
        // 날짜 결정 (마지막 날 다음날 또는 오늘)
        let newDate = new Date();
        
        if (itineraryData && itineraryData.days && itineraryData.days.length > 0) {
            // 현재 일정의 날짜들 정렬
            const dates = itineraryData.days.map(d => new Date(d.date));
            const latestDate = new Date(Math.max(...dates));
            // 다음날로 설정
            latestDate.setDate(latestDate.getDate() + 1);
            newDate = latestDate;
        }
        
        const dateStr = newDate.toISOString().split('T')[0];

        const newDay = {
            date: dateStr,
            schedules: [{
                time: '09:00',
                content: '새 일정'
            }]
        };

        if (!itineraryData) {
            itineraryData = { days: [] };
        }

        if (!itineraryData.days) {
            itineraryData.days = [];
        }

        itineraryData.days.push(newDay);

        // UI 업데이트
        renderItinerary();
        updateItineraryJson();
    }

    // 일정 JSON 업데이트 함수
    function updateItineraryJson() {
        // 원본 데이터 사용
        let originalData = itineraryData.originalData || {};
        
        // 원본 travelPlan 정보 유지
        let travelPlanData = originalData.travelPlan || {};
        
        // 변환된 itinerary 데이터 생성
        const convertedItinerary = itineraryData.days.map(day => {
            // 원본 날짜 데이터 찾기 (날짜가 일치하는 항목)
            let originalDay = {
                weather: "맑음",
                totalCost: "0 KRW"
            };
            
            if (originalData.travelPlan && originalData.travelPlan.itinerary) {
                const matchingDay = originalData.travelPlan.itinerary.find(d => d.date === day.date);
                if (matchingDay) {
                    originalDay.weather = matchingDay.weather || originalDay.weather;
                    originalDay.totalCost = matchingDay.totalCost || originalDay.totalCost;
                }
            }
            
            // 일정 항목 변환
            const convertedSchedule = day.schedules.map(schedule => {
                // 내용에서 카테고리, 위치, 시간, 비용 추출
                let category = "관광";
                let location = "";
                let duration = "1시간";
                let cost = "0 KRW";
                
                // "카테고리 - 위치 (시간, 비용)" 형식 파싱
                if (schedule.content) {
                    const contentParts = schedule.content.split(' - ');
                    if (contentParts.length >= 2) {
                        category = contentParts[0].trim();
                        
                        // 위치와 (시간, 비용) 분리
                        const locationParts = contentParts[1].split(' (');
                        if (locationParts.length >= 2) {
                            location = locationParts[0].trim();
                            
                            // (시간, 비용) 파싱
                            const detailsPart = locationParts[1].replace(')', '').split(', ');
                            if (detailsPart.length >= 2) {
                                duration = detailsPart[0].trim();
                                cost = detailsPart[1].trim();
                            } else if (detailsPart.length === 1) {
                                duration = detailsPart[0].trim();
                            }
                        } else {
                            location = locationParts[0].trim();
                        }
                    } else {
                        location = schedule.content;
                    }
                }
                
                return {
                    departureTime: schedule.time || "09:00",
                    category: category,
                    location: location,
                    duration: duration,
                    cost: cost
                };
            });
            
            return {
                date: day.date,
                weather: originalDay.weather,
                totalCost: originalDay.totalCost,
                schedule: convertedSchedule
            };
        });
        
        // 최종 travelPlan 객체 구성
        travelPlanData.itinerary = convertedItinerary;
        
        // 결과 JSON 저장
        itineraryArea.value = JSON.stringify({ travelPlan: travelPlanData });
    }

    // 일정 항목 드래그 앤 드롭 설정
    function setupDragAndDrop(container) {
        new Sortable(container, {
            animation: 150,
            ghostClass: 'schedule-item-ghost',
            onEnd: function (evt) {
                const dayIndex = parseInt(evt.item.dataset.dayIndex);
                const oldIndex = parseInt(evt.item.dataset.scheduleIndex);
                const newIndex = evt.newIndex;

                // 데이터 순서 변경
                const movedItem = itineraryData.days[dayIndex].schedules.splice(oldIndex, 1)[0];
                itineraryData.days[dayIndex].schedules.splice(newIndex, 0, movedItem);

                // UI 업데이트
                renderItinerary();
                updateItineraryJson();
            }
        });
    }

    // 날짜 컨테이너 드래그 앤 드롭 설정
    function setupDaysDragAndDrop() {
        new Sortable(itineraryContainer, {
            animation: 150,
            handle: '.day-header',
            ghostClass: 'day-container-ghost',
            onEnd: function (evt) {
                const oldIndex = parseInt(evt.item.dataset.dayIndex);
                const newIndex = evt.newIndex;

                // 데이터 순서 변경
                const movedDay = itineraryData.days.splice(oldIndex, 1)[0];
                itineraryData.days.splice(newIndex, 0, movedDay);

                // UI 업데이트
                renderItinerary();
                updateItineraryJson();
            }
        });
    }

    // 모달 열기
    btnOpenModal.addEventListener('click', function () {
        // 모달 필드 초기화
        document.getElementById('planName').value = document.getElementById('planName').value || '';
        document.getElementById('planDescription').value = document.getElementById('planDescription').value || '';
        
        if (planPhoto.value) {
            photoPreview.innerHTML = '';
            photoPreview.style.backgroundImage = `url(${planPhoto.value})`;
        } else {
            photoPreview.innerHTML = '<i class="fas fa-image"></i><span>사진을 선택해주세요</span>';
            photoPreview.style.backgroundImage = '';
        }

        confirmModal.style.display = 'block';
    });

    // 모달 닫기
    btnCloseModal.addEventListener('click', function () {
        confirmModal.style.display = 'none';
    });

    btnCancelModal.addEventListener('click', function () {
        confirmModal.style.display = 'none';
    });

    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function (event) {
        if (event.target === confirmModal) {
            confirmModal.style.display = 'none';
        }
    });

    // 사진 선택 버튼 클릭
    btnSelectPhoto.addEventListener('click', function () {
        planPhotoUpload.click();
    });

    // 사진 업로드 처리
    planPhotoUpload.addEventListener('change', function () {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                photoPreview.innerHTML = '';
                photoPreview.style.backgroundImage = `url(${e.target.result})`;
                planPhoto.value = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    });

    // 모달 제출
    btnSubmitModal.addEventListener('click', function () {
        const planName = document.getElementById('planName').value.trim();

        if (!planName) {
            alert('여행 이름은 필수 입력 항목입니다.');
            return;
        }

        // 히든 필드에 값 설정
        document.getElementById('hiddenPlanName').value = planName;
        document.getElementById('hiddenPlanPhoto').value = planPhoto.value;
        document.getElementById('hiddenPlanDescription').value = document.getElementById('planDescription').value.trim();

        // 폼 제출
        document.getElementById('editForm').submit();
    });
}); 