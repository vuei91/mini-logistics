package com.cjlogistics.mini.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipientType,recipientId,isRead")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 수신자 유형 (화주/기사) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipientType recipientType;

    /** 수신자 프로필 ID (shipperId 또는 driverId) */
    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** 연관된 배차 ID (알림 클릭 시 상세 이동 등에 사용, 없을 수 있음) */
    @Column
    private Long relatedDispatchId;

    @Column(nullable = false)
    private boolean isRead;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification(
            RecipientType recipientType,
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            Long relatedDispatchId
    ) {
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedDispatchId = relatedDispatchId;
        this.isRead = false;
    }

    public void markRead() {
        this.isRead = true;
    }
}
