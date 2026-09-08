package com.cjlogistics.mini.notification;

import com.cjlogistics.mini.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    /**
     * 알림을 생성한다. 요청/승낙/상태 변경 등 도메인 이벤트 발생 시 호출한다.
     * 저장 후 SSE 로 수신자에게 실시간 푸시한다.
     */
    @Transactional
    public Notification notify(
            RecipientType recipientType,
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            Long relatedDispatchId
    ) {
        Notification saved = notificationRepository.save(
                new Notification(recipientType, recipientId, type, title, message, relatedDispatchId));

        long unread = notificationRepository
                .countByRecipientTypeAndRecipientIdAndIsReadFalse(recipientType, recipientId);
        notificationSseService.push(recipientType, recipientId, NotificationResponse.from(saved), unread);

        return saved;
    }

    public List<Notification> list(RecipientType recipientType, Long recipientId) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId);
    }

    public long unreadCount(RecipientType recipientType, Long recipientId) {
        return notificationRepository
                .countByRecipientTypeAndRecipientIdAndIsReadFalse(recipientType, recipientId);
    }

    /**
     * 단건 읽음 처리. 본인 소유의 알림만 대상으로 한다.
     */
    @Transactional
    public Notification markRead(Long id, RecipientType recipientType, Long recipientId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientTypeAndRecipientId(id, recipientType, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.markRead();
        return notification;
    }

    /**
     * 수신자의 모든 알림을 읽음 처리한다.
     */
    @Transactional
    public void markAllRead(RecipientType recipientType, Long recipientId) {
        notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId)
                .forEach(Notification::markRead);
    }
}
