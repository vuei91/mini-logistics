import type {
  DispatchStatus,
  DriverStatus,
  ShipmentStatus,
  VehicleType,
} from "./types";

export const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  TRUCK_1T: "1톤 트럭",
  TRUCK_2_5T: "2.5톤 트럭",
  TRUCK_5T: "5톤 트럭",
  TRUCK_11T: "11톤 트럭",
};

export const VEHICLE_TYPES: VehicleType[] = [
  "TRUCK_1T",
  "TRUCK_2_5T",
  "TRUCK_5T",
  "TRUCK_11T",
];

export const SHIPMENT_STATUS_LABELS: Record<ShipmentStatus, string> = {
  REQUESTED: "요청됨",
  MATCHING: "매칭중",
  DISPATCHED: "배차완료",
  EN_ROUTE_TO_PICKUP: "픽업지 이동중",
  PICKED_UP: "픽업완료",
  IN_TRANSIT: "운송중",
  COMPLETED: "완료",
  CANCELED: "취소",
};

export const DISPATCH_STATUS_LABELS: Record<DispatchStatus, string> = {
  PROPOSED: "제안됨",
  ACCEPTED: "수락됨",
  REJECTED: "거절됨",
  EXPIRED: "만료됨",
  CANCELED: "취소됨",
  COMPLETED: "완료됨",
};

export const DRIVER_STATUS_LABELS: Record<DriverStatus, string> = {
  AVAILABLE: "운행 가능",
  BUSY: "운행중",
  OFFLINE: "오프라인",
};

/** 운송 상태 진행 순서 (스텝퍼용) */
export const SHIPMENT_PROGRESS: ShipmentStatus[] = [
  "DISPATCHED",
  "EN_ROUTE_TO_PICKUP",
  "PICKED_UP",
  "IN_TRANSIT",
  "COMPLETED",
];

export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatFare(fare: number | null | undefined): string {
  if (fare == null) return "산정 전";
  return `${Math.round(fare).toLocaleString("ko-KR")}원`;
}

export function formatWeight(kg: number): string {
  return `${kg.toLocaleString("ko-KR")}kg`;
}
