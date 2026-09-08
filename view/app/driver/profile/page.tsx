"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";
import { ErrorAlert } from "@/components/ErrorAlert";
import { RoleGuard } from "@/components/RoleGuard";
import { Spinner } from "@/components/Spinner";
import { ApiError } from "@/lib/api";
import { driverApi } from "@/lib/endpoints";
import {
  DRIVER_STATUS_LABELS,
  VEHICLE_TYPES,
  VEHICLE_TYPE_LABELS,
  formatWeight,
} from "@/lib/labels";
import { formatPhone } from "@/lib/phone";
import type { DriverResponse, RouteData, VehicleType } from "@/lib/types";
import { Field } from "../../login/page";

function DriverProfile() {
  const [driver, setDriver] = useState<DriverResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    driverApi
      .getMe()
      .then(setDriver)
      .catch((err) =>
        setLoadError(
          err instanceof ApiError
            ? err.message
            : "프로필을 불러오지 못했습니다.",
        ),
      );
  }, []);

  if (loadError) {
    return (
      <div className="mx-auto max-w-2xl">
        <ErrorAlert message={loadError} />
        <Link
          href="/driver/dashboard"
          className="mt-4 inline-block text-sm text-[var(--cj-red)]"
        >
          ← 대시보드로 돌아가기
        </Link>
      </div>
    );
  }

  if (!driver) return <Spinner />;

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        href="/driver/dashboard"
        className="text-sm text-zinc-500 hover:text-zinc-700"
      >
        ← 대시보드
      </Link>

      <div className="mt-3 flex items-start justify-between gap-3">
        <h1 className="text-2xl font-bold text-zinc-900">내 프로필</h1>
        {!editing ? (
          <button
            type="button"
            onClick={() => setEditing(true)}
            className="h-9 rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
          >
            수정
          </button>
        ) : null}
      </div>

      {editing ? (
        <ProfileEditForm
          driver={driver}
          onCancel={() => setEditing(false)}
          onSaved={(updated) => {
            setDriver(updated);
            setEditing(false);
          }}
        />
      ) : (
        <ProfileView driver={driver} />
      )}
    </div>
  );
}

function ProfileView({ driver }: { driver: DriverResponse }) {
  return (
    <div className="mt-6 space-y-4">
      <section className="rounded-xl border border-zinc-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-zinc-900">기본 정보</h2>
        <dl className="mt-3 grid grid-cols-2 gap-y-3 text-sm">
          <dt className="text-zinc-500">이름</dt>
          <dd className="text-zinc-800">{driver.name}</dd>
          <dt className="text-zinc-500">전화번호</dt>
          <dd className="text-zinc-800">{driver.phone}</dd>
          <dt className="text-zinc-500">운행 상태</dt>
          <dd className="text-zinc-800">
            {DRIVER_STATUS_LABELS[driver.status]}
          </dd>
        </dl>
      </section>

      <section className="rounded-xl border border-zinc-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-zinc-900">차량 정보</h2>
        <dl className="mt-3 grid grid-cols-2 gap-y-3 text-sm">
          <dt className="text-zinc-500">차량 종류</dt>
          <dd className="text-zinc-800">
            {VEHICLE_TYPE_LABELS[driver.vehicle.vehicleType]}
          </dd>
          <dt className="text-zinc-500">적재량</dt>
          <dd className="text-zinc-800">
            {formatWeight(driver.vehicle.capacityKg)}
          </dd>
        </dl>
      </section>

      <section className="rounded-xl border border-zinc-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-zinc-900">
          선호 노선 ({driver.preferredRoutes.length})
        </h2>
        {driver.preferredRoutes.length === 0 ? (
          <p className="mt-3 text-sm text-zinc-400">등록된 선호 노선이 없습니다.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {driver.preferredRoutes.map((r, i) => (
              <li key={i} className="text-sm text-zinc-700">
                {r.originRegion} → {r.destinationRegion}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function ProfileEditForm({
  driver,
  onCancel,
  onSaved,
}: {
  driver: DriverResponse;
  onCancel: () => void;
  onSaved: (updated: DriverResponse) => void;
}) {
  const [name, setName] = useState(driver.name);
  const [phone, setPhone] = useState(driver.phone);
  const [vehicleType, setVehicleType] = useState<VehicleType>(
    driver.vehicle.vehicleType,
  );
  const [capacityKg, setCapacityKg] = useState(String(driver.vehicle.capacityKg));
  const [routes, setRoutes] = useState<RouteData[]>(driver.preferredRoutes);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const updateRoute = (index: number, patch: Partial<RouteData>) =>
    setRoutes(routes.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  const addRoute = () =>
    setRoutes([...routes, { originRegion: "", destinationRegion: "" }]);
  const removeRoute = (index: number) =>
    setRoutes(routes.filter((_, i) => i !== index));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const updated = await driverApi.updateMe({
        name,
        phone,
        vehicle: { vehicleType, capacityKg: Number(capacityKg) },
        preferredRoutes: routes.filter(
          (r) => r.originRegion.trim() && r.destinationRegion.trim(),
        ),
      });
      onSaved(updated);
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "프로필 수정에 실패했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="mt-6 space-y-5 rounded-xl border border-zinc-200 bg-white p-6"
    >
      <Field label="이름">
        <input
          required
          maxLength={100}
          className="input"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </Field>
      <Field label="전화번호">
        <input
          required
          type="tel"
          inputMode="numeric"
          maxLength={13}
          className="input"
          value={phone}
          onChange={(e) => setPhone(formatPhone(e.target.value))}
        />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="차량 종류">
          <select
            className="input"
            value={vehicleType}
            onChange={(e) => setVehicleType(e.target.value as VehicleType)}
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
            className="input"
            value={capacityKg}
            onChange={(e) => setCapacityKg(e.target.value)}
          />
        </Field>
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-zinc-700">선호 노선</span>
          <button
            type="button"
            onClick={addRoute}
            className="rounded-md border border-zinc-300 px-2.5 py-1 text-xs font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
          >
            + 노선 추가
          </button>
        </div>
        {routes.length === 0 ? (
          <p className="text-sm text-zinc-400">
            선호 노선이 없습니다. 추가하면 매칭 점수에 반영됩니다.
          </p>
        ) : (
          <div className="space-y-2">
            {routes.map((r, index) => (
              <div key={index} className="flex items-center gap-2">
                <input
                  className="input flex-1"
                  placeholder="출발지"
                  maxLength={50}
                  value={r.originRegion}
                  onChange={(e) =>
                    updateRoute(index, { originRegion: e.target.value })
                  }
                />
                <span className="text-zinc-400">→</span>
                <input
                  className="input flex-1"
                  placeholder="도착지"
                  maxLength={50}
                  value={r.destinationRegion}
                  onChange={(e) =>
                    updateRoute(index, { destinationRegion: e.target.value })
                  }
                />
                <button
                  type="button"
                  onClick={() => removeRoute(index)}
                  className="rounded-md px-2 py-1 text-sm text-zinc-400 transition-colors hover:text-red-600"
                  aria-label="노선 삭제"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <ErrorAlert message={error} />

      <div className="flex gap-3">
        <button
          type="button"
          onClick={onCancel}
          className="h-11 flex-1 rounded-lg border border-zinc-300 bg-white font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
        >
          취소
        </button>
        <button
          type="submit"
          disabled={saving}
          className="h-11 flex-1 rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
        >
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
    </form>
  );
}

export default function Page() {
  return (
    <RoleGuard role="DRIVER">
      <DriverProfile />
    </RoleGuard>
  );
}
