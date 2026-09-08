"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { NotificationBell } from "./NotificationBell";

export function Header() {
  const { user, initialized, logout } = useAuth();
  const router = useRouter();

  const dashboardHref =
    user?.role === "SHIPPER"
      ? "/shipper/dashboard"
      : user?.role === "DRIVER"
        ? "/driver/dashboard"
        : "/";

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <header className="sticky top-0 z-10 border-b border-zinc-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-14 w-full max-w-5xl items-center justify-between px-4">
        <Link href={dashboardHref} className="flex items-center gap-2">
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-[var(--cj-red)] text-sm font-bold text-white">
            CJ
          </span>
          <span className="text-sm font-semibold text-zinc-800">
            미니 물류 플랫폼
          </span>
        </Link>

        <nav className="flex items-center gap-3 text-sm">
          {!initialized ? null : user ? (
            <>
              <span className="hidden text-zinc-500 sm:inline">
                {user.email}
                <span className="ml-1 rounded bg-zinc-100 px-1.5 py-0.5 text-xs text-zinc-600">
                  {user.role === "SHIPPER" ? "화주" : "기사"}
                </span>
              </span>
              <Link
                href={dashboardHref}
                className="rounded-md px-2 py-1.5 text-zinc-600 hover:text-zinc-900"
              >
                대시보드
              </Link>
              <Link
                href={
                  user.role === "SHIPPER"
                    ? "/shipper/profile"
                    : "/driver/profile"
                }
                className="rounded-md px-2 py-1.5 text-zinc-600 hover:text-zinc-900"
              >
                내 프로필
              </Link>
              <NotificationBell role={user.role} />
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-md border border-zinc-300 px-3 py-1.5 text-zinc-700 transition-colors hover:bg-zinc-50"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="rounded-md px-3 py-1.5 text-zinc-700 hover:text-zinc-900"
              >
                로그인
              </Link>
              <Link
                href="/signup"
                className="rounded-md bg-[var(--cj-red)] px-3 py-1.5 font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)]"
              >
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
