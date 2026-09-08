"use client";

import Link from "next/link";
import { useNotifications } from "@/contexts/NotificationContext";
import type { Role } from "@/lib/types";

/**
 * 헤더의 벨 모양 알림 버튼. 읽지 않은 알림 개수를 뱃지로 표시한다.
 * 터치(클릭) 시 역할별 알림 목록 화면으로 이동한다.
 */
export function NotificationBell({ role }: { role: Role }) {
  const { unreadCount, requestRefresh } = useNotifications();
  const href =
    role === "SHIPPER" ? "/shipper/notifications" : "/driver/notifications";
  const badge = unreadCount > 99 ? "99+" : String(unreadCount);

  return (
    <Link
      href={href}
      onClick={requestRefresh}
      aria-label={`알림${unreadCount > 0 ? ` (읽지 않음 ${unreadCount}건)` : ""}`}
      className="relative inline-flex h-9 w-9 items-center justify-center rounded-md text-zinc-600 transition-colors hover:bg-zinc-100 hover:text-zinc-900"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-5 w-5"
        aria-hidden
      >
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      {unreadCount > 0 ? (
        <span className="absolute -right-0.5 -top-0.5 flex min-w-[18px] items-center justify-center rounded-full bg-[var(--cj-red)] px-1 text-[10px] font-bold leading-4 text-white">
          {badge}
        </span>
      ) : null}
    </Link>
  );
}
