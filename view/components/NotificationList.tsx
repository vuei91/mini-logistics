"use client";

import { useEffect } from "react";
import { useNotifications } from "@/contexts/NotificationContext";
import { formatDateTime, NOTIFICATION_TYPE_LABELS } from "@/lib/labels";
import type { NotificationResponse } from "@/lib/types";
import { EmptyState } from "./EmptyState";
import { Spinner } from "./Spinner";

/**
 * 알림 목록 화면. 화주/기사 공통.
 * 항목 선택 시 읽음 처리되어 헤더 벨의 뱃지 수가 줄어든다.
 */
/** 알림 화면에 머무는 동안 목록을 갱신하는 폴링 간격 (ms) */
const LIST_POLL_INTERVAL_MS = 15_000;

export function NotificationList() {
  const {
    notifications,
    unreadCount,
    loading,
    refresh,
    markRead,
    markAllRead,
    refreshSignal,
  } = useNotifications();

  // 화면 진입 시 + 벨 버튼 재클릭(refreshSignal 변경) 시 최신 목록 로드
  useEffect(() => {
    void refresh();
  }, [refresh, refreshSignal]);

  // 화면에 머무는 동안 주기적으로 목록 갱신 (새 알림 반영)
  useEffect(() => {
    const timer = setInterval(() => {
      void refresh();
    }, LIST_POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [refresh]);

  const handleSelect = (n: NotificationResponse) => {
    if (!n.read) {
      void markRead(n.id);
    }
  };

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-zinc-900">알림</h1>
          <p className="mt-0.5 text-sm text-zinc-500">
            읽지 않은 알림 {unreadCount}건
          </p>
        </div>
        <button
          type="button"
          onClick={() => void markAllRead()}
          disabled={unreadCount === 0}
          className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 transition-colors hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-50"
        >
          모두 읽음
        </button>
      </div>

      {loading && notifications.length === 0 ? (
        <Spinner label="알림 불러오는 중..." />
      ) : notifications.length === 0 ? (
        <EmptyState
          title="알림이 없습니다"
          description="요청, 수락, 상태 변경이 발생하면 이곳에 표시됩니다."
        />
      ) : (
        <ul className="space-y-2">
          {notifications.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                onClick={() => handleSelect(n)}
                className={`flex w-full flex-col items-start gap-1 rounded-lg border px-4 py-3 text-left transition-colors ${
                  n.read
                    ? "border-zinc-200 bg-white hover:bg-zinc-50"
                    : "border-red-200 bg-red-50/60 hover:bg-red-50"
                }`}
              >
                <div className="flex w-full items-center justify-between gap-2">
                  <span className="flex items-center gap-2">
                    {!n.read ? (
                      <span
                        className="inline-block h-2 w-2 shrink-0 rounded-full bg-[var(--cj-red)]"
                        aria-hidden
                      />
                    ) : null}
                    <span className="text-sm font-medium text-zinc-900">
                      {n.title}
                    </span>
                    {n.type !== "STATUS_CHANGED" ? (
                      <span className="rounded bg-zinc-100 px-1.5 py-0.5 text-[11px] text-zinc-600">
                        {NOTIFICATION_TYPE_LABELS[n.type]}
                      </span>
                    ) : null}
                  </span>
                  <span className="shrink-0 text-xs text-zinc-400">
                    {formatDateTime(n.createdAt)}
                  </span>
                </div>
                <p className="text-sm text-zinc-600">{n.message}</p>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
