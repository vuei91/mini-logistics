"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { EmptyState } from "@/components/EmptyState";
import { ErrorAlert } from "@/components/ErrorAlert";
import { RoleGuard } from "@/components/RoleGuard";
import { Spinner } from "@/components/Spinner";
import { ShipmentStatusBadge } from "@/components/StatusBadge";
import { ApiError } from "@/lib/api";
import { shipmentApi } from "@/lib/endpoints";
import {
  VEHICLE_TYPE_LABELS,
  formatDateTime,
  formatFare,
  formatWeight,
} from "@/lib/labels";
import type { ShipmentRequestResponse } from "@/lib/types";

function ShipperDashboard() {
  const [requests, setRequests] = useState<ShipmentRequestResponse[] | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    shipmentApi
      .listMine()
      .then(setRequests)
      .catch((err) => {
        setError(
          err instanceof ApiError ? err.message : "목록을 불러오지 못했습니다.",
        );
        setRequests([]);
      });
  }, []);

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-zinc-900">내 화물 요청</h1>
          <p className="mt-1 text-sm text-zinc-500">
            생성한 화물 요청과 진행 상태를 확인하세요.
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Link
            href="/shipper/profile"
            className="flex h-10 items-center rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
          >
            내 프로필
          </Link>
          <Link
            href="/shipper/requests/new"
            className="flex h-10 items-center rounded-lg bg-[var(--cj-red)] px-4 text-sm font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)]"
          >
            + 새 화물 요청
          </Link>
        </div>
      </div>

      <div className="mt-6">
        <ErrorAlert message={error} />

        {requests === null ? (
          <Spinner />
        ) : requests.length === 0 ? (
          <EmptyState
            title="아직 화물 요청이 없습니다"
            description="첫 화물 요청을 생성하고 기사 매칭을 시작해보세요."
            action={
              <Link
                href="/shipper/requests/new"
                className="inline-flex h-10 items-center rounded-lg bg-[var(--cj-red)] px-4 text-sm font-medium text-white hover:bg-[var(--cj-red-dark)]"
              >
                새 화물 요청 만들기
              </Link>
            }
          />
        ) : (
          <ul className="space-y-3">
            {requests.map((req) => (
              <li key={req.id}>
                <Link
                  href={`/shipper/requests/detail?id=${req.id}`}
                  className="block rounded-xl border border-zinc-200 bg-white p-5 transition-colors hover:border-zinc-300 hover:bg-zinc-50"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-base font-semibold text-zinc-900">
                        {req.originRegion} → {req.destinationRegion}
                      </p>
                      <p className="mt-1 text-sm text-zinc-500">
                        {VEHICLE_TYPE_LABELS[req.requiredVehicleType]} ·{" "}
                        {formatWeight(req.totalCargoWeightKg)} ·{" "}
                        화물 {req.cargoItems.length}건
                      </p>
                      {req.estimatedFare != null ? (
                        <p className="mt-1 text-sm text-zinc-600">
                          예상 운임{" "}
                          <span className="font-semibold text-zinc-900">
                            {formatFare(req.estimatedFare)}
                          </span>
                        </p>
                      ) : null}
                    </div>
                    <ShipmentStatusBadge status={req.status} />
                  </div>
                  <p className="mt-3 text-xs text-zinc-400">
                    #{req.id} · {formatDateTime(req.createdAt)}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default function Page() {
  return (
    <RoleGuard role="SHIPPER">
      <ShipperDashboard />
    </RoleGuard>
  );
}
