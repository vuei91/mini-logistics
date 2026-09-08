package com.cjlogistics.mini.notification;

import com.cjlogistics.mini.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE(Server-Sent Events) 기반 실시간 알림 푸시 서비스.
 * 수신자(역할 + 프로필 ID) 단위로 연결된 SseEmitter 들을 관리하고,
 * 새 알림이 생성되면 해당 수신자에게 즉시 푸시한다.
 */
@Slf4j
@Service
public class NotificationSseService {

    /** SSE 연결 타임아웃 (30분). 클라이언트는 만료 시 자동 재연결한다. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    /** 수신자 키 -> 활성 emitter 목록 (한 사용자가 여러 탭을 열 수 있으므로 리스트) */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static String key(RecipientType recipientType, Long recipientId) {
        return recipientType.name() + ":" + recipientId;
    }

    public SseEmitter subscribe(RecipientType recipientType, Long recipientId) {
        String key = key(recipientType, recipientId);
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(e -> remove(key, emitter));

        // 최초 연결 확인용 이벤트 (프록시 버퍼링 방지 및 연결 확립)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(key, emitter);
        }
        return emitter;
    }

    /** 새 알림 + 안읽은 개수를 해당 수신자의 모든 연결로 푸시한다. */
    public void push(RecipientType recipientType, Long recipientId, NotificationResponse notification, long unreadCount) {
        String key = key(recipientType, recipientId);
        List<SseEmitter> targets = emitters.get(key);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
                emitter.send(SseEmitter.event().name("unread-count").data(unreadCount));
            } catch (IOException | IllegalStateException e) {
                remove(key, emitter);
            }
        }
    }

    private void remove(String key, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(key);
            }
        }
    }
}
