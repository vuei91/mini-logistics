"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useAuth } from "@/contexts/AuthContext";
import type { Role } from "@/lib/types";
import { Spinner } from "./Spinner";

/**
 * 클라이언트 사이드 역할 가드.
 * - 미로그인: /login 이동
 * - 역할 불일치: 본인 역할 대시보드로 이동
 */
export function RoleGuard({
  role,
  children,
}: {
  role: Role;
  children: ReactNode;
}) {
  const { user, initialized } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!initialized) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (user.role !== role) {
      router.replace(
        user.role === "SHIPPER" ? "/shipper/dashboard" : "/driver/dashboard",
      );
    }
  }, [initialized, user, role, router]);

  if (!initialized || !user || user.role !== role) {
    return <Spinner label="접근 권한 확인 중..." />;
  }

  return <>{children}</>;
}
