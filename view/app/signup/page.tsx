"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ErrorAlert } from "@/components/ErrorAlert";
import { useAuth } from "@/contexts/AuthContext";
import { ApiError } from "@/lib/api";
import { authApi } from "@/lib/endpoints";
import { VEHICLE_TYPES, VEHICLE_TYPE_LABELS } from "@/lib/labels";
import type { Role, VehicleType } from "@/lib/types";
import { Field, RoleTabs } from "../login/page";

export default function SignupPage() {
  const router = useRouter();
  const { login } = useAuth();

  const [role, setRole] = useState<Role>("SHIPPER");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // 기사 전용 차량 정보
  const [vehicleType, setVehicleType] = useState<VehicleType>("TRUCK_1T");
  const [capacityKg, setCapacityKg] = useState("1000");

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (role === "SHIPPER") {
        await authApi.shipperSignup({ name, phone, email, password });
      } else {
        await authApi.driverSignup({
          name,
          phone,
          email,
          password,
          vehicle: {
            vehicleType,
            capacityKg: Number(capacityKg),
          },
        });
      }

      // 가입 후 자동 로그인
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
          : "회원가입 중 오류가 발생했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-2xl font-bold text-zinc-900">회원가입</h1>
      <p className="mt-1 text-sm text-zinc-500">
        계정 유형을 선택해 가입하세요. 가입 후 자동으로 로그인됩니다.
      </p>

      <RoleTabs role={role} onChange={setRole} />

      <form
        onSubmit={handleSubmit}
        className="mt-6 space-y-4 rounded-xl border border-zinc-200 bg-white p-6"
      >
        <Field label="이름">
          <input
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="input"
            placeholder="홍길동"
            maxLength={100}
          />
        </Field>
        <Field label="전화번호">
          <input
            required
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className="input"
            placeholder="010-1234-5678"
            maxLength={20}
          />
        </Field>
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
        <Field label="비밀번호 (8자 이상)">
          <input
            type="password"
            required
            minLength={8}
            maxLength={72}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="input"
            placeholder="••••••••"
            autoComplete="new-password"
          />
        </Field>

        {role === "DRIVER" ? (
          <div className="space-y-4 rounded-lg border border-zinc-200 bg-zinc-50 p-4">
            <p className="text-sm font-semibold text-zinc-700">차량 정보</p>
            <Field label="차량 종류">
              <select
                value={vehicleType}
                onChange={(e) => setVehicleType(e.target.value as VehicleType)}
                className="input"
              >
                {VEHICLE_TYPES.map((v) => (
                  <option key={v} value={v}>
                    {VEHICLE_TYPE_LABELS[v]}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="적재량 (kg)">
              <input
                type="number"
                required
                min={1}
                value={capacityKg}
                onChange={(e) => setCapacityKg(e.target.value)}
                className="input"
                placeholder="1000"
              />
            </Field>
          </div>
        ) : null}

        <ErrorAlert message={error} />

        <button
          type="submit"
          disabled={loading}
          className="h-11 w-full rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
        >
          {loading ? "가입 중..." : "회원가입"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-zinc-500">
        이미 계정이 있으신가요?{" "}
        <Link href="/login" className="font-medium text-[var(--cj-red)]">
          로그인
        </Link>
      </p>
    </div>
  );
}
