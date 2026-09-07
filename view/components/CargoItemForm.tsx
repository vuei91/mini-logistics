"use client";

import { formatWeight } from "@/lib/labels";
import type { CargoItemInput } from "@/lib/types";

interface Props {
  items: CargoItemInput[];
  onChange: (items: CargoItemInput[]) => void;
}

export function CargoItemForm({ items, onChange }: Props) {
  const totalWeight = items.reduce(
    (sum, item) => sum + (Number(item.weightKg) || 0),
    0,
  );

  const update = (index: number, patch: Partial<CargoItemInput>) => {
    onChange(items.map((it, i) => (i === index ? { ...it, ...patch } : it)));
  };

  const add = () => onChange([...items, { description: "", weightKg: 0 }]);

  const remove = (index: number) =>
    onChange(items.filter((_, i) => i !== index));

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-zinc-700">화물 항목</span>
        <button
          type="button"
          onClick={add}
          className="rounded-md border border-zinc-300 px-2.5 py-1 text-xs font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
        >
          + 항목 추가
        </button>
      </div>

      <div className="space-y-2">
        {items.map((item, index) => (
          <div key={index} className="flex items-start gap-2">
            <input
              className="input flex-1"
              placeholder="화물 설명 (예: 냉장 식품)"
              value={item.description}
              maxLength={200}
              required
              onChange={(e) => update(index, { description: e.target.value })}
            />
            <div className="w-32">
              <div className="flex items-center gap-1">
                <input
                  className="input"
                  type="number"
                  min={1}
                  placeholder="무게"
                  value={item.weightKg || ""}
                  required
                  onChange={(e) =>
                    update(index, { weightKg: Number(e.target.value) })
                  }
                />
                <span className="text-xs text-zinc-500">kg</span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => remove(index)}
              disabled={items.length === 1}
              className="mt-1 rounded-md px-2 py-1 text-sm text-zinc-400 transition-colors hover:text-red-600 disabled:opacity-40"
              aria-label="항목 삭제"
            >
              ✕
            </button>
          </div>
        ))}
      </div>

      <div className="flex justify-end text-sm text-zinc-600">
        총 무게:{" "}
        <span className="ml-1 font-semibold text-zinc-900">
          {formatWeight(totalWeight)}
        </span>
      </div>
    </div>
  );
}
