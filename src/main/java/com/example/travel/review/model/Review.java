package com.example.travel.review.model;

import com.example.travel.schedule.model.Schedule;
import com.example.travel.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Double rating;

    @Column(name = "visit_date")
    private LocalDateTime visitDate;

    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @ElementCollection
    @CollectionTable(name = "review_report_reasons", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "reason")
    @Builder.Default
    private List<String> reportReasons = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    // 태그 목록을 저장
    @ElementCollection
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    // orphanRemoval = true : 부모 엔티티가 삭제될 때 자식 엔티티도 함께 삭제
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // 리뷰 생성 시 시간 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.viewCount = 0;
        this.reportCount = 0;
    }

    // 리뷰 수정 시 시간 설정
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 이미지 url 추가
    public void addImage(String imageUrl) {
        this.imageUrls.add(imageUrl);
    }

    // 이미지 url 목록 조회
    public List<String> getImages() {
        return this.imageUrls;
    }

    // 태그 추가
    public void addTag(String tag) {
        this.tags.add(tag);
    }

    // 댓글 추가
    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setReview(this);
    }

    // 댓글 삭제
    public void removeComment(Comment comment) {
        this.comments.remove(comment);
        comment.setReview(null);
    }

    // 조회수 증가
    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 1;
        } else {
            this.viewCount++;
        }
    }

    // 신고 횟수 증가
    public void incrementReportCount() {
        if (this.reportCount == null) {
            this.reportCount = 1;
        } else {
            this.reportCount++;
        }
    }

    // 신고 초기화
    public void resetReportCount() {
        this.reportCount = 0;
    }

    /**
     * 리뷰가 신고되었는지 확인
     */
    public boolean isReported() {
        return reportCount > 0;
    }

    /**
     * 리뷰 신고 추가
     */
    public void addReport(String reason) {
        this.reportCount++;
        if (reason != null && !reason.isEmpty()) {
            this.reportReasons.add(reason);
        }
    }

    /**
     * 신고 카운트 초기화
     */
    public void clearReports() {
        this.reportCount = 0;
        this.reportReasons.clear();
    }
}