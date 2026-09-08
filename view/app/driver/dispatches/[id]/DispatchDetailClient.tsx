"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { DispatchStepper } from "@/components/DispatchStepper";
import { ErrorAlert } from "@/components/ErrorAlert";
import { RoleGuard } from "@/components/RoleGuard";
import { Spinner } from "@/components/Spinner";
import {
  DispatchStatusBadge,
  ShipmentStatusBadge,
} from "@/components/StatusBadge";
import { ApiError } from "@/lib/api";
import { dispatchApi } from "@/lib/endpoints";
import { formatDateTime, formatFare } from "@/lib/labels";
import type { DispatchResponse, ShipmentStatus } from "@/lib/types";

function DispatchDetail({ id }: { id: number }) {
  const [dispatch, setDispatch] = useState<DispatchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    dispatchApi
      .get(id)
      .then(setDispatch)
      .catch((err) =>
        setError(
          err instanceof ApiError
            ? err.message
            : "배차를 불러오지 못했습니다.",
        ),
      );
  }, [id]);

  useEffect(load, [load]);

  const run = async (fn: () => Promise<DispatchResponse>) => {
    setActionError(null);
    setBusy(true);
    try {
      setDispatch(await fn());
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : "처리에 실패했습니다.",
      );
    } finally {
      setBusy(false);
    }
  };

  const handleAccept = () => run(() => dispatchApi.accept(id));
  const handleReject = () => run(() => dispatchApi.reject(id));
  const handleAdvance = (next: ShipmentStatus) =>
    run(() => dispatchApi.updateStatus(id, next));

  if (error) {
    return (
      <div className="mx-auto max-w-2xl">
        <ErrorAlert message={error} />
        <Link
          href="/driver/dashboard"
          className="mt-4 inline-block text-sm text-[var(--cj-red)]"
        >
          ← 대시보드로 돌아가기
        </Link>
      </div>
    );
  }

  if (!dispatch) return <Spinner />;

  const isProposed = dispatch.status === "PROPOSED";
  const isAccepted = dispatch.status === "ACCEPTED";

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        href="/driver/dashboard"
        className="text-sm text-zinc-500 hover:text-zinc-700"
      >
        ← 대시보드
      </Link>

      <div className="mt-3 flex items-start justify-between gap-3">
        <h1 className="text-2xl font-bold text-zinc-900">
          배차 #{dispatch.id}
        </h1>
        <DispatchStatusBadge status={dispatch.status} />
      </div>
      <p className="mt-1 text-xs text-zinc-400">
        {formatDateTime(dispatch.createdAt)}
      </p>

      {/* 배차 정보 */}
      <section className="mt-6 rounded-xl border border-zinc-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-zinc-900">배차 정보</h2>
        <dl className="mt-3 grid grid-cols-2 gap-y-3 text-sm">
          <dt className="text-zinc-500">연결된 화물요청</dt>
          <dd className="text-zinc-800">#{dispatch.shipmentRequestId}</dd>
          <dt className="text-zinc-500">매칭 점수</dt>
          <dd className="text-zinc-800">{dispatch.matchScore.toFixed(1)}</dd>
          <dt className="text-zinc-500">운임</dt>
          <dd className="font-semibold text-zinc-900">
            {formatFare(dispatch.fare)}
          </dd>
          <dt className="text-zinc-500">운송 상태</dt>
          <dd>
            {dispatch.shipmentStatus ? (
              <ShipmentStatusBadge status={dispatch.shipmentStatus} />
            ) : (
              <span className="text-zinc-400">-</span>
            )}
          </dd>
        </dl>
      </section>

      <div className="mt-4">
        <ErrorAlert message={actionError} />
      </div>

      {/* PROPOSED: 수락/거절 */}
      {isProposed ? (
        <div className="mt-4 flex gap-3">
          <button
            type="button"
            onClick={handleAccept}
            disabled={busy}
            className="h-11 flex-1 rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
          >
            {busy ? "처리 중..." : "수락"}
          </button>
          <button
            type="button"
            onClick={handleReject}
            disabled={busy}
            className="h-11 flex-1 rounded-lg border border-zinc-300 bg-white font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-60"
          >
            거절
          </button>
        </div>
      ) : null}

      {/* ACCEPTED: 운송 상태 스텝퍼 */}
      {isAccepted && dispatch.shipmentStatus ? (
        <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-5">
          <h2 className="mb-4 text-sm font-semibold text-zinc-900">
            운송 상태 변경
          </h2>
          <DispatchStepper
            current={dispatch.shipmentStatus}
            onAdvance={handleAdvance}
            busy={busy}
          />
        </section>
      ) : null}
    </div>
  );
}

export function DispatchDetailClient() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  return (
    <RoleGuard role="DRIVER">
      {Number.isNaN(id) ? (
        <ErrorAlert message="잘못된 배차입니다." />
      ) : (
        <DispatchDetail id={id} />
      )}
    </RoleGuard>
  );
}
