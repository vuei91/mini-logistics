package com.cjlogistics.mini.notification;

import com.cjlogistics.mini.notification.dto.NotificationResponse;
import com.cjlogistics.mini.notification.dto.UnreadCountResponse;
import com.cjlogistics.mini.security.AuthenticatedMember;
import com.cjlogistics.mini.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 알림 조회/읽음 처리 API. 화주/기사 공통으로 사용하며,
 * 수신자는 인증 토큰의 role + profileId 로 식별한다.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final JwtTokenService jwtTokenService;

    /**
     * 실시간 알림 SSE 구독. EventSource 는 커스텀 헤더를 보낼 수 없어
     * JWT 를 token 쿼리 파라미터로 받아 인증한다.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token) {
        AuthenticatedMember member;
        try {
            member = jwtTokenService.parse(token);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
        RecipientType recipientType = RecipientType.fromRole(member.role());
        return notificationSseService.subscribe(recipientType, member.profileId());
    }

    /** 내 알림 목록 (최신순) */
    @GetMapping
    public List<NotificationResponse> listMine(@AuthenticationPrincipal AuthenticatedMember member) {
        RecipientType recipientType = RecipientType.fromRole(member.role());
        return notificationService.list(recipientType, member.profileId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** 읽지 않은 알림 개수 (뱃지용) */
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthenticatedMember member) {
        RecipientType recipientType = RecipientType.fromRole(member.role());
        return new UnreadCountResponse(notificationService.unreadCount(recipientType, member.profileId()));
    }

    /** 단건 읽음 처리 (알림 선택 시 뱃지 수 감소) */
    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        RecipientType recipientType = RecipientType.fromRole(member.role());
        return NotificationResponse.from(
                notificationService.markRead(id, recipientType, member.profileId()));
    }

    /** 모두 읽음 처리 */
    @PatchMapping("/read-all")
    public UnreadCountResponse markAllRead(@AuthenticationPrincipal AuthenticatedMember member) {
        RecipientType recipientType = RecipientType.fromRole(member.role());
        notificationService.markAllRead(recipientType, member.profileId());
        return new UnreadCountResponse(0);
    }
}
