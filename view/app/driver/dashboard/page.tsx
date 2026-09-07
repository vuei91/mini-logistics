"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { EmptyState } from "@/components/EmptyState";
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
import type { DispatchResponse } from "@/lib/types";

function DispatchCard({ dispatch }: { dispatch: DispatchResponse }) {
  return (
    <Link
      href={`/driver/dispatches/${dispatch.id}`}
      className="block rounded-xl border border-zinc-200 bg-white p-5 transition-colors hover:border-zinc-300 hover:bg-zinc-50"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-base font-semibold text-zinc-900">
            배차 #{dispatch.id}
          </p>
          <p className="mt-1 text-sm text-zinc-500">
            화물요청 #{dispatch.shipmentRequestId} · 매칭점수{" "}
            {dispatch.matchScore.toFixed(1)} · {formatFare(dispatch.fare)}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <DispatchStatusBadge status={dispatch.status} />
          {dispatch.shipmentStatus ? (
            <ShipmentStatusBadge status={dispatch.shipmentStatus} />
          ) : null}
        </div>
      </div>
      <p className="mt-3 text-xs text-zinc-400">
        {formatDateTime(dispatch.createdAt)}
      </p>
    </Link>
  );
}

function DriverDashboard() {
  const [dispatches, setDispatches] = useState<DispatchResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dispatchApi
      .listMine()
      .then(setDispatches)
      .catch((err) => {
        setError(
          err instanceof ApiError ? err.message : "목록을 불러오지 못했습니다.",
        );
        setDispatches([]);
      });
  }, []);

  const proposed = dispatches?.filter((d) => d.status === "PROPOSED") ?? [];
  const others = dispatches?.filter((d) => d.status !== "PROPOSED") ?? [];

  return (
    <div>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-zinc-900">내 배차</h1>
          <p className="mt-1 text-sm text-zinc-500">
            제안된 배차를 확인하고 운송 상태를 관리하세요.
          </p>
        </div>
        <Link
          href="/driver/profile"
          className="flex h-10 shrink-0 items-center rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
        >
          내 프로필
        </Link>
      </div>

      <div className="mt-6 space-y-8">
        <ErrorAlert message={error} />

        {dispatches === null ? (
          <Spinner />
        ) : dispatches.length === 0 ? (
          <EmptyState
            title="배정된 배차가 없습니다"
            description="화주가 매칭을 실행하면 여기에 배차가 표시됩니다."
          />
        ) : (
          <>
            <section>
              <h2 className="mb-3 text-sm font-semibold text-zinc-700">
                제안된 배차{" "}
                <span className="text-[var(--cj-red)]">({proposed.length})</span>
              </h2>
              {proposed.length === 0 ? (
                <p className="rounded-lg border border-dashed border-zinc-200 bg-white px-4 py-6 text-center text-sm text-zinc-400">
                  새로 제안된 배차가 없습니다.
                </p>
              ) : (
                <ul className="space-y-3">
                  {proposed.map((d) => (
                    <li key={d.id}>
                      <DispatchCard dispatch={d} />
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section>
              <h2 className="mb-3 text-sm font-semibold text-zinc-700">
                진행/완료 ({others.length})
              </h2>
              {others.length === 0 ? (
                <p className="rounded-lg border border-dashed border-zinc-200 bg-white px-4 py-6 text-center text-sm text-zinc-400">
                  진행 중이거나 완료된 배차가 없습니다.
                </p>
              ) : (
                <ul className="space-y-3">
                  {others.map((d) => (
                    <li key={d.id}>
                      <DispatchCard dispatch={d} />
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

export default function Page() {
  return (
    <RoleGuard role="DRIVER">
      <DriverDashboard />
    </RoleGuard>
  );
}
