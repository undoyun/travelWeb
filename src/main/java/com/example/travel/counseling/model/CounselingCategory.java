package com.example.travel.counseling.model;

public enum CounselingCategory {
    GENERAL("일반 문의"),
    TRAVEL_PLAN("여행 계획"),
    ATTRACTION("관광 명소"),
    ACCOUNT("계정 관련"),
    SYSTEM("시스템 오류"),
    SUGGESTION("기능 제안"),
    OTHER("기타");

    private final String displayName;

    CounselingCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}