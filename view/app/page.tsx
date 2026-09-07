"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";

export default function LandingPage() {
  const { user, initialized } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (initialized && user) {
      router.replace(
        user.role === "SHIPPER" ? "/shipper/dashboard" : "/driver/dashboard",
      );
    }
  }, [initialized, user, router]);

  return (
    <div className="flex flex-col items-center py-10 text-center">
      <span className="rounded-full bg-red-50 px-3 py-1 text-xs font-medium text-[var(--cj-red)]">
        화주 · 기사 화물 매칭 데모
      </span>
      <h1 className="mt-5 max-w-2xl text-3xl font-bold leading-tight tracking-tight text-zinc-900 sm:text-4xl">
        화물 요청부터 배차, 운송 추적까지
        <br />한 곳에서
      </h1>
      <p className="mt-4 max-w-xl text-base leading-7 text-zinc-600">
        화주는 화물 요청을 등록하고 기사 매칭을 요청하며, 기사는 배정된 배차를
        수락하고 운송 상태를 단계별로 업데이트할 수 있습니다.
      </p>

      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <Link
          href="/signup"
          className="flex h-11 items-center justify-center rounded-lg bg-[var(--cj-red)] px-6 font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)]"
        >
          시작하기
        </Link>
        <Link
          href="/login"
          className="flex h-11 items-center justify-center rounded-lg border border-zinc-300 bg-white px-6 font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
        >
          로그인
        </Link>
      </div>

      <div className="mt-14 grid w-full max-w-3xl gap-4 text-left sm:grid-cols-2">
        <div className="rounded-xl border border-zinc-200 bg-white p-5">
          <h2 className="text-sm font-semibold text-zinc-900">화주</h2>
          <p className="mt-2 text-sm leading-6 text-zinc-600">
            출발지·도착지와 화물 정보를 입력해 요청을 생성하고, 버튼 한 번으로
            기사 매칭을 실행합니다.
          </p>
        </div>
        <div className="rounded-xl border border-zinc-200 bg-white p-5">
          <h2 className="text-sm font-semibold text-zinc-900">기사</h2>
          <p className="mt-2 text-sm leading-6 text-zinc-600">
            제안된 배차를 확인해 수락/거절하고, 픽업·운송·완료까지 상태를
            단계별로 갱신합니다.
          </p>
        </div>
      </div>
    </div>
  );
}
