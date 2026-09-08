package com.cjlogistics.mini.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            RecipientType recipientType, Long recipientId);

    long countByRecipientTypeAndRecipientIdAndIsReadFalse(
            RecipientType recipientType, Long recipientId);

    Optional<Notification> findByIdAndRecipientTypeAndRecipientId(
            Long id, RecipientType recipientType, Long recipientId);
}
