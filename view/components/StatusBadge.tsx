import {
  DISPATCH_STATUS_LABELS,
  SHIPMENT_STATUS_LABELS,
} from "@/lib/labels";
import type { DispatchStatus, ShipmentStatus } from "@/lib/types";

type Tone = "gray" | "blue" | "amber" | "green" | "red" | "purple";

const TONE_CLASSES: Record<Tone, string> = {
  gray: "bg-zinc-100 text-zinc-600",
  blue: "bg-blue-50 text-blue-700",
  amber: "bg-amber-50 text-amber-700",
  green: "bg-green-50 text-green-700",
  red: "bg-red-50 text-red-700",
  purple: "bg-purple-50 text-purple-700",
};

const SHIPMENT_TONE: Record<ShipmentStatus, Tone> = {
  REQUESTED: "gray",
  MATCHING: "amber",
  DISPATCHED: "blue",
  EN_ROUTE_TO_PICKUP: "blue",
  PICKED_UP: "purple",
  IN_TRANSIT: "purple",
  COMPLETED: "green",
  CANCELED: "red",
};

const DISPATCH_TONE: Record<DispatchStatus, Tone> = {
  PROPOSED: "amber",
  ACCEPTED: "blue",
  REJECTED: "red",
  EXPIRED: "gray",
  CANCELED: "red",
  COMPLETED: "green",
};

function Badge({ tone, label }: { tone: Tone; label: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${TONE_CLASSES[tone]}`}
    >
      {label}
    </span>
  );
}

export function ShipmentStatusBadge({ status }: { status: ShipmentStatus }) {
  return <Badge tone={SHIPMENT_TONE[status]} label={SHIPMENT_STATUS_LABELS[status]} />;
}

export function DispatchStatusBadge({ status }: { status: DispatchStatus }) {
  return <Badge tone={DISPATCH_TONE[status]} label={DISPATCH_STATUS_LABELS[status]} />;
}
