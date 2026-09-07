"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { CargoItemForm } from "@/components/CargoItemForm";
import { ErrorAlert } from "@/components/ErrorAlert";
import { RoleGuard } from "@/components/RoleGuard";
import { useAuth } from "@/contexts/AuthContext";
import { ApiError } from "@/lib/api";
import { shipmentApi } from "@/lib/endpoints";
import { VEHICLE_TYPES, VEHICLE_TYPE_LABELS } from "@/lib/labels";
import type { CargoItemInput, VehicleType } from "@/lib/types";
import { Field } from "../../../login/page";

function NewRequestForm() {
  const router = useRouter();
  const { user } = useAuth();

  const [originRegion, setOriginRegion] = useState("");
  const [destinationRegion, setDestinationRegion] = useState("");
  const [requiredVehicleType, setRequiredVehicleType] =
    useState<VehicleType>("TRUCK_1T");
  const [cargoItems, setCargoItems] = useState<CargoItemInput[]>([
    { description: "", weightKg: 0 },
  ]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setError(null);
    setLoading(true);
    try {
      const created = await shipmentApi.create({
        shipperId: user.profileId,
        originRegion,
        destinationRegion,
        requiredVehicleType,
        cargoItems: cargoItems.map((it) => ({
          description: it.description,
          weightKg: Number(it.weightKg),
        })),
      });
      router.push(`/shipper/requests/${created.id}`);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "화물 요청 생성 중 오류가 발생했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-2xl font-bold text-zinc-900">새 화물 요청</h1>
      <p className="mt-1 text-sm text-zinc-500">
        출발지·도착지, 필요 차량, 화물 항목을 입력하세요.
      </p>

      <form
        onSubmit={handleSubmit}
        className="mt-6 space-y-5 rounded-xl border border-zinc-200 bg-white p-6"
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="출발지">
            <input
              required
              className="input"
              placeholder="서울"
              maxLength={50}
              value={originRegion}
              onChange={(e) => setOriginRegion(e.target.value)}
            />
          </Field>
          <Field label="도착지">
            <input
              required
              className="input"
              placeholder="부산"
              maxLength={50}
              value={destinationRegion}
              onChange={(e) => setDestinationRegion(e.target.value)}
            />
          </Field>
        </div>

        <Field label="필요 차량 종류">
          <select
            className="input"
            value={requiredVehicleType}
            onChange={(e) =>
              setRequiredVehicleType(e.target.value as VehicleType)
            }
          >
            {VEHICLE_TYPES.map((v) => (
              <option key={v} value={v}>
                {VEHICLE_TYPE_LABELS[v]}
              </option>
            ))}
          </select>
        </Field>

        <CargoItemForm items={cargoItems} onChange={setCargoItems} />

        <ErrorAlert message={error} />

        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => router.back()}
            className="h-11 flex-1 rounded-lg border border-zinc-300 bg-white font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={loading}
            className="h-11 flex-1 rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
          >
            {loading ? "생성 중..." : "요청 생성"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default function Page() {
  return (
    <RoleGuard role="SHIPPER">
      <NewRequestForm />
    </RoleGuard>
  );
}
