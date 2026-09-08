"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ErrorAlert } from "@/components/ErrorAlert";
import { useAuth } from "@/contexts/AuthContext";
import { ApiError } from "@/lib/api";
import { authApi } from "@/lib/endpoints";
import type { Role } from "@/lib/types";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();

  const [role, setRole] = useState<Role>("SHIPPER");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleRoleChange = (next: Role) => {
    if (next === role) return;
    setRole(next);
    setEmail("");
    setPassword("");
    setError(null);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const tokenRes =
        role === "SHIPPER"
          ? await authApi.shipperLogin({ email, password })
          : await authApi.driverLogin({ email, password });
      const authUser = login(tokenRes.accessToken);
      router.push(
        authUser.role === "SHIPPER"
          ? "/shipper/dashboard"
          : "/driver/dashboard",
      );
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "로그인 중 오류가 발생했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-2xl font-bold text-zinc-900">로그인</h1>
      <p className="mt-1 text-sm text-zinc-500">
        계정 유형을 선택하고 로그인하세요.
      </p>

      <RoleTabs role={role} onChange={handleRoleChange} />

      <form
        onSubmit={handleSubmit}
        className="mt-6 space-y-4 rounded-xl border border-zinc-200 bg-white p-6"
      >
        <Field label="이메일">
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="input"
            placeholder="you@example.com"
            autoComplete="email"
          />
        </Field>
        <Field label="비밀번호">
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="input"
            placeholder="••••••••"
            autoComplete="current-password"
          />
        </Field>

        <ErrorAlert message={error} />

        <button
          type="submit"
          disabled={loading}
          className="h-11 w-full rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
        >
          {loading ? "로그인 중..." : "로그인"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-zinc-500">
        아직 계정이 없으신가요?{" "}
        <Link href="/signup" className="font-medium text-[var(--cj-red)]">
          회원가입
        </Link>
      </p>
    </div>
  );
}

export function RoleTabs({
  role,
  onChange,
}: {
  role: Role;
  onChange: (role: Role) => void;
}) {
  return (
    <div className="mt-5 grid grid-cols-2 gap-1 rounded-lg bg-zinc-100 p-1">
      {(["SHIPPER", "DRIVER"] as Role[]).map((r) => (
        <button
          key={r}
          type="button"
          onClick={() => onChange(r)}
          className={`h-9 rounded-md text-sm font-medium transition-colors ${
            role === r
              ? "bg-white text-zinc-900 shadow-sm"
              : "text-zinc-500 hover:text-zinc-700"
          }`}
        >
          {r === "SHIPPER" ? "화주" : "기사"}
        </button>
      ))}
    </div>
  );
}

export function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-zinc-700">
        {label}
      </span>
      {children}
    </label>
  );
}
