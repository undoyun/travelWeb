document.addEventListener("DOMContentLoaded", function () {
  // Parse the schedule JSON
  const scheduleJson = /*[[${scheduleItem.scheduleJson}]]*/ '{"days":[]}';
  try {
    const itinerary = JSON.parse(scheduleJson);
    renderItinerary(itinerary);
  } catch (e) {
    console.error("Failed to parse itinerary JSON:", e);
    document.getElementById("itineraryContainer").innerHTML =
      '<p style="color: #777; text-align: center;">일정 정보를 불러올 수 없습니다.</p>';
  }
});

function renderItinerary(itinerary) {
  const container = document.getElementById("itineraryContainer");
  container.innerHTML = "";

  if (!itinerary.days || itinerary.days.length === 0) {
    container.innerHTML =
      '<p style="color: #777; text-align: center;">일정 정보가 없습니다.</p>';
    return;
  }

  itinerary.days.forEach((day, index) => {
    const dayCard = document.createElement("div");
    dayCard.className = "day-card";

    const dayHeader = document.createElement("div");
    dayHeader.className = "day-header";
    dayHeader.textContent = `Day ${index + 1}${
      day.date ? " - " + day.date : ""
    }`;
    dayCard.appendChild(dayHeader);

    if (day.schedule && day.schedule.length > 0) {
      day.schedule.forEach((item) => {
        const scheduleItem = document.createElement("div");
        scheduleItem.className = "schedule-item";

        const timeDiv = document.createElement("div");
        timeDiv.className = "schedule-time";
        timeDiv.textContent = item.departureTime || "";

        const detailsDiv = document.createElement("div");
        detailsDiv.className = "schedule-details";

        const locationDiv = document.createElement("div");
        locationDiv.className = "schedule-location";
        locationDiv.textContent = item.location || "장소 미정";

        const categoryDiv = document.createElement("div");
        categoryDiv.className = "schedule-category";
        categoryDiv.textContent = item.category || "";

        const metaDiv = document.createElement("div");

        if (item.duration) {
          const durationSpan = document.createElement("span");
          durationSpan.className = "schedule-duration";
          durationSpan.innerHTML = `<i class="far fa-clock"></i> ${item.duration}`;
          metaDiv.appendChild(durationSpan);
        }

        if (item.cost) {
          const costSpan = document.createElement("span");
          costSpan.className = "schedule-cost";
          costSpan.innerHTML = `<i class="fas fa-coins"></i> ${item.cost}`;
          metaDiv.appendChild(costSpan);
        }

        detailsDiv.appendChild(locationDiv);
        detailsDiv.appendChild(categoryDiv);
        detailsDiv.appendChild(metaDiv);

        scheduleItem.appendChild(timeDiv);
        scheduleItem.appendChild(detailsDiv);
        dayCard.appendChild(scheduleItem);
      });
    } else {
      const emptyMessage = document.createElement("div");
      emptyMessage.className = "schedule-item";
      emptyMessage.textContent = "이 날의 일정이 없습니다.";
      emptyMessage.style.color = "#777";
      emptyMessage.style.textAlign = "center";
      dayCard.appendChild(emptyMessage);
    }

    container.appendChild(dayCard);
  });
}