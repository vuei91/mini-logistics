package com.cjlogistics.mini.notification.dto;

import com.cjlogistics.mini.notification.Notification;
import com.cjlogistics.mini.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long relatedDispatchId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getRelatedDispatchId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
