package com.example.travel.review.model;

/**
 * 리뷰 상태를 나타내는 열거형
 */
public enum ReviewStatus {
    /**
     * 승인됨: 관리자 검토 완료 및 공개 승인된 상태
     */
    APPROVED,

    /**
     * 대기중: 관리자 검토를 기다리는 상태
     */
    PENDING,

    /**
     * 거부됨: 관리자에 의해 공개가 거부된 상태
     */
    REJECTED,

    /**
     * 삭제됨: 삭제된 상태
     */
    DELETED
}