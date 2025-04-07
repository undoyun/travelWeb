package com.example.travel.activity.model;

public enum ActivityType {
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    REGISTER("회원가입"),
    REVIEW("리뷰"),
    COMMENT("댓글"),
    SCHEDULE("일정"),
    INQUIRY("문의"),
    PROFILE_UPDATE("프로필 수정"),
    PASSWORD_CHANGE("비밀번호 변경"),

    // 여행 계획 관련
    TRAVEL_PLAN_CREATE("여행 계획 생성"),
    TRAVEL_PLAN_UPDATE("여행 계획 수정"),
    TRAVEL_PLAN_DELETE("여행 계획 삭제"),
    TRAVEL_PLAN_COMPLETE("여행 계획 확정"),

    // 일정 관련
    SCHEDULE_CREATE("일정 생성"),
    SCHEDULE_UPDATE("일정 수정"),
    SCHEDULE_DELETE("일정 삭제"),

    // 리뷰 관련
    REVIEW_CREATE("리뷰 작성"),
    REVIEW_UPDATE("리뷰 수정"),
    REVIEW_DELETE("리뷰 삭제"),
    REVIEW_LIKE("리뷰 좋아요"),

    // 댓글 관련
    COMMENT_CREATE("댓글 작성"),
    COMMENT_UPDATE("댓글 수정"),
    COMMENT_DELETE("댓글 삭제"),

    // 문의 관련
    COUNSELING_CREATE("문의 작성"),
    COUNSELING_UPDATE("문의 수정"),
    COUNSELING_DELETE("문의 삭제"),

    // 관광지 관련
    SPOT_REQUEST("관광지 추가 요청"),

    // 기타
    SEARCH("검색"),
    VIEW("조회"),
    OTHER("기타");

    private final String displayName;

    ActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}