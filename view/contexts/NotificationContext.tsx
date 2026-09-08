"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { API_BASE_URL, getToken } from "@/lib/api";
import { notificationApi } from "@/lib/endpoints";
import type { NotificationResponse } from "@/lib/types";
import { useAuth } from "./AuthContext";

interface NotificationContextValue {
  notifications: NotificationResponse[];
  unreadCount: number;
  loading: boolean;
  /** 서버에서 목록/뱃지 새로고침 */
  refresh: () => Promise<void>;
  /** 단건 읽음 처리 (선택 시 뱃지 감소) */
  markRead: (id: number) => Promise<void>;
  /** 모두 읽음 처리 */
  markAllRead: () => Promise<void>;
  /**
   * 목록 새로고침 요청 신호. 벨 버튼 클릭 시 증가한다.
   * 이미 알림 화면에 있어도(같은 경로 재클릭) 목록이 갱신되도록 하기 위한 값.
   */
  refreshSignal: number;
  /** 새로고침 신호를 올린다 (벨 버튼 클릭 시 호출) */
  requestRefresh: () => void;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

/**
 * 뱃지 수 백업 폴링 간격 (ms).
 * 실시간 갱신은 SSE 로 처리하고, 폴링은 SSE 연결이 끊긴 경우의 안전망이다.
 */
const POLL_INTERVAL_MS = 60_000;

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { user, initialized } = useAuth();
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [refreshSignal, setRefreshSignal] = useState(0);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const requestRefresh = useCallback(() => {
    setRefreshSignal((prev) => prev + 1);
  }, []);

  const refreshUnread = useCallback(async () => {
    // 비로그인 상태면 뱃지/목록을 비운다.
    if (!getToken()) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    try {
      const { unreadCount: count } = await notificationApi.unreadCount();
      setUnreadCount(count);
    } catch {
      // 뱃지 조회 실패는 조용히 무시 (헤더 UX 방해 방지)
    }
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const list = await notificationApi.listMine();
      setNotifications(list);
      setUnreadCount(list.filter((n) => !n.read).length);
    } catch {
      // 목록 페이지에서 별도 처리
    } finally {
      setLoading(false);
    }
  }, []);

  const markRead = useCallback(async (id: number) => {
    // 낙관적 업데이트: 즉시 반영 후 서버 호출
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
    );
    setUnreadCount((prev) => Math.max(0, prev - 1));
    try {
      await notificationApi.markRead(id);
    } catch {
      // 실패 시 서버 상태로 재동기화
      await refresh();
    }
  }, [refresh]);

  const markAllRead = useCallback(async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
    try {
      await notificationApi.markAllRead();
    } catch {
      await refresh();
    }
  }, [refresh]);

  // 로그인 상태에 따라 폴링 시작/중단. (비로그인 시 refreshUnread 가 상태를 비운다)
  useEffect(() => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
    if (!initialized) return;
    void refreshUnread();
    if (!user) return;
    pollRef.current = setInterval(() => {
      void refreshUnread();
    }, POLL_INTERVAL_MS);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [user, initialized, refreshUnread]);

  // SSE 실시간 구독. 새 알림이 생성되면 서버가 즉시 푸시한다.
  useEffect(() => {
    if (!initialized || !user) return;
    const token = getToken();
    if (!token) return;

    const url = `${API_BASE_URL}/notifications/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);
    eventSourceRef.current = es;

    es.addEventListener("notification", (event) => {
      try {
        const incoming = JSON.parse((event as MessageEvent).data) as NotificationResponse;
        setNotifications((prev) => {
          // 이미 목록에 있으면(중복 푸시) 갱신, 없으면 최상단에 추가
          if (prev.some((n) => n.id === incoming.id)) {
            return prev.map((n) => (n.id === incoming.id ? incoming : n));
          }
          return [incoming, ...prev];
        });
      } catch {
        // 파싱 실패 시 무시 (다음 폴링/새로고침이 보정)
      }
    });

    es.addEventListener("unread-count", (event) => {
      const count = Number((event as MessageEvent).data);
      if (!Number.isNaN(count)) {
        setUnreadCount(count);
      }
    });

    es.onerror = () => {
      // EventSource 는 자동 재연결한다. 연결 문제는 폴링이 백업한다.
    };

    return () => {
      es.close();
      eventSourceRef.current = null;
    };
  }, [user, initialized]);

  const value = useMemo(
    () => ({
      notifications,
      unreadCount,
      loading,
      refresh,
      markRead,
      markAllRead,
      refreshSignal,
      requestRefresh,
    }),
    [
      notifications,
      unreadCount,
      loading,
      refresh,
      markRead,
      markAllRead,
      refreshSignal,
      requestRefresh,
    ],
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications(): NotificationContextValue {
  const ctx = useContext(NotificationContext);
  if (!ctx) {
    throw new Error("useNotifications must be used within a NotificationProvider");
  }
  return ctx;
}
