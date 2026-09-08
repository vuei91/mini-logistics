"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";
import { ErrorAlert } from "@/components/ErrorAlert";
import { RoleGuard } from "@/components/RoleGuard";
import { Spinner } from "@/components/Spinner";
import { useAuth } from "@/contexts/AuthContext";
import { ApiError } from "@/lib/api";
import { shipperApi } from "@/lib/endpoints";
import { formatPhone } from "@/lib/phone";
import type { ShipperResponse } from "@/lib/types";
import { Field } from "../../login/page";

function ShipperProfile() {
  const { user } = useAuth();
  const [shipper, setShipper] = useState<ShipperResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    shipperApi
      .getMe()
      .then(setShipper)
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
          href="/shipper/dashboard"
          className="mt-4 inline-block text-sm text-[var(--cj-red)]"
        >
          ← 대시보드로 돌아가기
        </Link>
      </div>
    );
  }

  if (!shipper) return <Spinner />;

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        href="/shipper/dashboard"
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
          shipper={shipper}
          onCancel={() => setEditing(false)}
          onSaved={(updated) => {
            setShipper(updated);
            setEditing(false);
          }}
        />
      ) : (
        <section className="mt-6 rounded-xl border border-zinc-200 bg-white p-5">
          <h2 className="text-sm font-semibold text-zinc-900">기본 정보</h2>
          <dl className="mt-3 grid grid-cols-2 gap-y-3 text-sm">
            <dt className="text-zinc-500">이름</dt>
            <dd className="text-zinc-800">{shipper.name}</dd>
            <dt className="text-zinc-500">전화번호</dt>
            <dd className="text-zinc-800">{shipper.phone}</dd>
            <dt className="text-zinc-500">이메일</dt>
            <dd className="text-zinc-800">{user?.email ?? "-"}</dd>
          </dl>
        </section>
      )}
    </div>
  );
}

function ProfileEditForm({
  shipper,
  onCancel,
  onSaved,
}: {
  shipper: ShipperResponse;
  onCancel: () => void;
  onSaved: (updated: ShipperResponse) => void;
}) {
  const [name, setName] = useState(shipper.name);
  const [phone, setPhone] = useState(shipper.phone);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const updated = await shipperApi.updateMe({ name, phone });
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
    <RoleGuard role="SHIPPER">
      <ShipperProfile />
    </RoleGuard>
  );
}
