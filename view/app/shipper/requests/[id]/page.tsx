"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ErrorAlert } from "@/components/ErrorAlert";
import { RoleGuard } from "@/components/RoleGuard";
import { Spinner } from "@/components/Spinner";
import {
  DispatchStatusBadge,
  ShipmentStatusBadge,
} from "@/components/StatusBadge";
import { ApiError } from "@/lib/api";
import { shipmentApi } from "@/lib/endpoints";
import {
  VEHICLE_TYPE_LABELS,
  formatDateTime,
  formatFare,
  formatWeight,
} from "@/lib/labels";
import type {
  DispatchResponse,
  MatchCandidateResponse,
  ShipmentRequestResponse,
} from "@/lib/types";

function RequestDetail({ id }: { id: number }) {
  const [request, setRequest] = useState<ShipmentRequestResponse | null>(null);
  const [dispatch, setDispatch] = useState<DispatchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // 매칭 후보 목록 상태
  const [candidates, setCandidates] = useState<MatchCandidateResponse[] | null>(
    null,
  );
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [selectingDriverId, setSelectingDriverId] = useState<number | null>(
    null,
  );

  const load = useCallback(() => {
    shipmentApi
      .get(id)
      .then(setRequest)
      .catch((err) =>
        setError(
          err instanceof ApiError
            ? err.message
            : "요청을 불러오지 못했습니다.",
        ),
      );
  }, [id]);

  useEffect(load, [load]);

  // 1단계: 매칭 후보 목록 조회
  const handleFindCandidates = async () => {
    setActionError(null);
    setLoadingCandidates(true);
    try {
      const list = await shipmentApi.matchCandidates(id);
      setCandidates(list);
    } catch (err) {
      setActionError(
        err instanceof ApiError
          ? err.message
          : "매칭 후보를 불러오지 못했습니다.",
      );
    } finally {
      setLoadingCandidates(false);
    }
  };

  // 2단계: 선택한 기사로 배차 생성
  const handleDispatchTo = async (driverId: number) => {
    setActionError(null);
    setSelectingDriverId(driverId);
    setBusy(true);
    try {
      const result = await shipmentApi.dispatch(id, driverId);
      setDispatch(result);
      setCandidates(null);
      load();
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : "배차 요청에 실패했습니다.",
      );
    } finally {
      setBusy(false);
      setSelectingDriverId(null);
    }
  };

  const handleCancel = async () => {
    setActionError(null);
    setBusy(true);
    try {
      const updated = await shipmentApi.cancel(id);
      setRequest(updated);
      setCandidates(null);
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : "취소에 실패했습니다.",
      );
    } finally {
      setBusy(false);
    }
  };

  if (error) {
    return (
      <div className="mx-auto max-w-2xl">
        <ErrorAlert message={error} />
        <Link
          href="/shipper/dashboard"
          className="mt-4 inline-block text-sm text-[var(--cj-red)]"
        >
          ← 대시보드로 돌아가기
        </Link>
      </div>
    );
  }

  if (!request) return <Spinner />;

  const canDispatch = request.status === "REQUESTED";
  const canCancel =
    request.status === "REQUESTED" || request.status === "MATCHING";

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        href="/shipper/dashboard"
        className="text-sm text-zinc-500 hover:text-zinc-700"
      >
        ← 대시보드
      </Link>

      <div className="mt-3 flex items-start justify-between gap-3">
        <h1 className="text-2xl font-bold text-zinc-900">
          {request.originRegion} → {request.destinationRegion}
        </h1>
        <ShipmentStatusBadge status={request.status} />
      </div>
      <p className="mt-1 text-xs text-zinc-400">
        #{request.id} · {formatDateTime(request.createdAt)}
      </p>

      {/* 요청 정보 */}
      <section className="mt-6 rounded-xl border border-zinc-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-zinc-900">요청 정보</h2>
        <dl className="mt-3 grid grid-cols-2 gap-y-3 text-sm">
          <dt className="text-zinc-500">필요 차량</dt>
          <dd className="text-zinc-800">
            {VEHICLE_TYPE_LABELS[request.requiredVehicleType]}
          </dd>
          <dt className="text-zinc-500">총 무게</dt>
          <dd className="text-zinc-800">
            {formatWeight(request.totalCargoWeightKg)}
          </dd>
          {request.estimatedFare != null ? (
            <>
              <dt className="text-zinc-500">예상 운임</dt>
              <dd className="font-semibold text-zinc-900">
                {formatFare(request.estimatedFare)}
              </dd>
            </>
          ) : null}
        </dl>
      </section>

      {/* 화물 항목 */}
      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-5">
        <h2 className="text-sm font-semibold text-zinc-900">
          화물 항목 ({request.cargoItems.length})
        </h2>
        <ul className="mt-3 divide-y divide-zinc-100">
          {request.cargoItems.map((item) => (
            <li
              key={item.id}
              className="flex items-center justify-between py-2 text-sm"
            >
              <span className="text-zinc-700">{item.description}</span>
              <span className="text-zinc-500">{formatWeight(item.weightKg)}</span>
            </li>
          ))}
        </ul>
      </section>

      {/* 배차 정보 (이번 세션에서 배차한 경우) */}
      {dispatch ? (
        <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-5">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-zinc-900">배차 정보</h2>
            <DispatchStatusBadge status={dispatch.status} />
          </div>
          <dl className="mt-3 grid grid-cols-2 gap-y-3 text-sm">
            <dt className="text-zinc-500">배차 번호</dt>
            <dd className="text-zinc-800">#{dispatch.id}</dd>
            <dt className="text-zinc-500">배정 기사 ID</dt>
            <dd className="text-zinc-800">#{dispatch.driverId}</dd>
            <dt className="text-zinc-500">매칭 점수</dt>
            <dd className="text-zinc-800">{dispatch.matchScore.toFixed(1)}</dd>
            <dt className="text-zinc-500">운임</dt>
            <dd className="font-semibold text-zinc-900">
              {formatFare(dispatch.fare)}
            </dd>
          </dl>
        </section>
      ) : null}

      {/* 매칭 후보 목록 */}
      {candidates !== null ? (
        <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-5">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-zinc-900">
              매칭 후보 기사 ({candidates.length})
            </h2>
            <button
              type="button"
              onClick={() => setCandidates(null)}
              className="text-xs text-zinc-400 hover:text-zinc-600"
            >
              닫기
            </button>
          </div>

          {candidates.length === 0 ? (
            <p className="mt-4 rounded-lg bg-zinc-50 px-4 py-6 text-center text-sm text-zinc-500">
              조건에 맞는 기사가 없습니다. (차량 종류/적재량이 맞고 운행 가능한
              기사가 필요합니다.)
            </p>
          ) : (
            <>
              {candidates[0].estimatedFare != null ? (
                <p className="mt-3 rounded-lg bg-zinc-50 px-3 py-2 text-sm text-zinc-600">
                  예상 운임{" "}
                  <span className="font-semibold text-zinc-900">
                    {formatFare(candidates[0].estimatedFare)}
                  </span>
                </p>
              ) : null}
              <ul className="mt-3 space-y-3">
              {candidates.map((c) => (
                <li
                  key={c.driverId}
                  className="rounded-lg border border-zinc-200 p-4"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-zinc-900">
                        {c.name}{" "}
                        <span className="text-xs font-normal text-zinc-400">
                          #{c.driverId}
                        </span>
                      </p>
                      <p className="mt-1 text-sm text-zinc-500">
                        {VEHICLE_TYPE_LABELS[c.vehicle.vehicleType]} · 적재{" "}
                        {formatWeight(c.vehicle.capacityKg)}
                      </p>
                      {c.preferredRoutes.length > 0 ? (
                        <p className="mt-1 text-xs text-zinc-400">
                          선호 노선:{" "}
                          {c.preferredRoutes
                            .map(
                              (r) =>
                                `${r.originRegion}→${r.destinationRegion}`,
                            )
                            .join(", ")}
                        </p>
                      ) : null}
                    </div>
                    <span className="shrink-0 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-[var(--cj-red)]">
                      점수 {c.matchScore.toFixed(1)}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleDispatchTo(c.driverId)}
                    disabled={busy}
                    className="mt-3 h-9 w-full rounded-lg bg-[var(--cj-red)] text-sm font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
                  >
                    {selectingDriverId === c.driverId
                      ? "배차 중..."
                      : "이 기사로 배차 요청"}
                  </button>
                </li>
              ))}
              </ul>
            </>
          )}
        </section>
      ) : null}

      <div className="mt-4">
        <ErrorAlert message={actionError} />
      </div>

      {/* 액션 */}
      {canDispatch || canCancel ? (
        <div className="mt-4 flex gap-3">
          {canDispatch ? (
            <button
              type="button"
              onClick={handleFindCandidates}
              disabled={loadingCandidates || busy}
              className="h-11 flex-1 rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
            >
              {loadingCandidates
                ? "후보 조회 중..."
                : candidates !== null
                  ? "후보 다시 조회"
                  : "기사 매칭 요청"}
            </button>
          ) : null}
          {canCancel ? (
            <button
              type="button"
              onClick={handleCancel}
              disabled={busy}
              className="h-11 flex-1 rounded-lg border border-zinc-300 bg-white font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-60"
            >
              요청 취소
            </button>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

export default function Page() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  return (
    <RoleGuard role="SHIPPER">
      {Number.isNaN(id) ? (
        <ErrorAlert message="잘못된 요청입니다." />
      ) : (
        <RequestDetail id={id} />
      )}
    </RoleGuard>
  );
}
