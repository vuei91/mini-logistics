"use client";

import { SHIPMENT_PROGRESS, SHIPMENT_STATUS_LABELS } from "@/lib/labels";
import type { ShipmentStatus } from "@/lib/types";

interface Props {
  /** 현재 화물 운송 상태 */
  current: ShipmentStatus;
  /** 다음 단계로 진행 */
  onAdvance: (next: ShipmentStatus) => void;
  busy?: boolean;
}

export function DispatchStepper({ current, onAdvance, busy }: Props) {
  const currentIndex = SHIPMENT_PROGRESS.indexOf(current);
  const nextStatus =
    currentIndex >= 0 && currentIndex < SHIPMENT_PROGRESS.length - 1
      ? SHIPMENT_PROGRESS[currentIndex + 1]
      : null;

  return (
    <div>
      <ol className="space-y-3">
        {SHIPMENT_PROGRESS.map((status, index) => {
          const done = currentIndex >= 0 && index <= currentIndex;
          const isCurrent = index === currentIndex;
          return (
            <li key={status} className="flex items-center gap-3">
              <span
                className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                  done
                    ? "bg-[var(--cj-red)] text-white"
                    : "border border-zinc-300 bg-white text-zinc-400"
                }`}
              >
                {done ? "✓" : index + 1}
              </span>
              <span
                className={`text-sm ${
                  isCurrent
                    ? "font-semibold text-zinc-900"
                    : done
                      ? "text-zinc-600"
                      : "text-zinc-400"
                }`}
              >
                {SHIPMENT_STATUS_LABELS[status]}
              </span>
            </li>
          );
        })}
      </ol>

      {nextStatus ? (
        <button
          type="button"
          onClick={() => onAdvance(nextStatus)}
          disabled={busy}
          className="mt-5 h-11 w-full rounded-lg bg-[var(--cj-red)] font-medium text-white transition-colors hover:bg-[var(--cj-red-dark)] disabled:opacity-60"
        >
          {busy
            ? "처리 중..."
            : `다음 단계: ${SHIPMENT_STATUS_LABELS[nextStatus]}`}
        </button>
      ) : (
        <p className="mt-5 rounded-lg bg-green-50 px-4 py-3 text-center text-sm font-medium text-green-700">
          운송이 완료되었습니다.
        </p>
      )}
    </div>
  );
}
