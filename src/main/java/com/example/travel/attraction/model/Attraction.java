package com.example.travel.attraction.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(indexes = {
        @Index(name = "idx_attraction_gugun_nm", columnList = "gugunNm"),
        @Index(name = "idx_attraction_main_title", columnList = "mainTitle"),
        @Index(name = "idx_attraction_uc_seq", columnList = "ucSeq", unique = true)
})
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ucSeq; // API에서의 고유 식별자 (UC_SEQ)
    private String mainTitle; // 관광지 이름
    private String subtitle; // 부제목
    private String title; // 제목
    private Double lng; // 경도
    private Double lat; // 위도
    private String middleSizeRm1; // 장애인 주차장 정보
    private String usageAmount; // 이용 요금
    private String cntctTel; // 연락처
    private String mainImgNormal; // 이미지 URL (일반)
    private String mainImgThumb; // 이미지 URL (썸네일)
    private String trfcInfo; // 교통 정보
    private String hldyInfo; // 휴일 정보

    @Column(columnDefinition = "TEXT")
    private String itemcntnts; // 상세 내용

    private String place; // 장소
    private String usageDay; // 이용 가능 일
    private String usageDayWeekAndTime; // 이용 가능 요일 및 시간
    private String gugunNm; // 구군 이름
    private String addr1; // 주소
    private String homepageUrl; // 홈페이지 URL

    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime updatedAt; // 업데이트 시간

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // API 데이터를 엔티티로 변환하기 위한 메서드
    public static Attraction fromApiData(AttractionApiData apiData) {
        Attraction attraction = new Attraction();
        attraction.setUcSeq(apiData.getUC_SEQ());
        attraction.setMainTitle(apiData.getMAIN_TITLE());
        attraction.setSubtitle(apiData.getSUBTITLE());
        attraction.setTitle(apiData.getTITLE());

        try {
            if (apiData.getLNG() != null && !apiData.getLNG().isEmpty()) {
                attraction.setLng(Double.parseDouble(apiData.getLNG()));
            }
            if (apiData.getLAT() != null && !apiData.getLAT().isEmpty()) {
                attraction.setLat(Double.parseDouble(apiData.getLAT()));
            }
        } catch (NumberFormatException e) {
            // 숫자 변환 실패 시 기본값 설정 또는 로깅
        }

        attraction.setMiddleSizeRm1(apiData.getMIDDLE_SIZE_RM1());
        attraction.setUsageAmount(apiData.getUSAGE_AMOUNT());
        attraction.setCntctTel(apiData.getCNTCT_TEL());
        attraction.setMainImgNormal(apiData.getMAIN_IMG_NORMAL());
        attraction.setMainImgThumb(apiData.getMAIN_IMG_THUMB());
        attraction.setTrfcInfo(apiData.getTRFC_INFO());
        attraction.setHldyInfo(apiData.getHLDY_INFO());
        attraction.setItemcntnts(apiData.getITEMCNTNTS());
        attraction.setPlace(apiData.getPLACE());
        attraction.setUsageDay(apiData.getUSAGE_DAY());
        attraction.setUsageDayWeekAndTime(apiData.getUSAGE_DAY_WEEK_AND_TIME());
        attraction.setGugunNm(apiData.getGUGUN_NM());
        attraction.setAddr1(apiData.getADDR1());
        attraction.setHomepageUrl(apiData.getHOMEPAGE_URL());

        return attraction;
    }

    // 기존 엔티티를 API 데이터로 업데이트하는 메서드
    public void updateFromApiData(AttractionApiData apiData) {
        this.setMainTitle(apiData.getMAIN_TITLE());
        this.setSubtitle(apiData.getSUBTITLE());
        this.setTitle(apiData.getTITLE());

        try {
            if (apiData.getLNG() != null && !apiData.getLNG().isEmpty()) {
                this.setLng(Double.parseDouble(apiData.getLNG()));
            }
            if (apiData.getLAT() != null && !apiData.getLAT().isEmpty()) {
                this.setLat(Double.parseDouble(apiData.getLAT()));
            }
        } catch (NumberFormatException e) {
            // 숫자 변환 실패 시 기본값 설정 또는 로깅
        }

        this.setMiddleSizeRm1(apiData.getMIDDLE_SIZE_RM1());
        this.setUsageAmount(apiData.getUSAGE_AMOUNT());
        this.setCntctTel(apiData.getCNTCT_TEL());
        this.setMainImgNormal(apiData.getMAIN_IMG_NORMAL());
        this.setMainImgThumb(apiData.getMAIN_IMG_THUMB());
        this.setTrfcInfo(apiData.getTRFC_INFO());
        this.setHldyInfo(apiData.getHLDY_INFO());
        this.setItemcntnts(apiData.getITEMCNTNTS());
        this.setPlace(apiData.getPLACE());
        this.setUsageDay(apiData.getUSAGE_DAY());
        this.setUsageDayWeekAndTime(apiData.getUSAGE_DAY_WEEK_AND_TIME());
        this.setGugunNm(apiData.getGUGUN_NM());
        this.setAddr1(apiData.getADDR1());
        this.setHomepageUrl(apiData.getHOMEPAGE_URL());
    }
}