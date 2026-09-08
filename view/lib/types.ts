// 백엔드(Spring Boot) DTO와 1:1 매칭되는 TypeScript 타입 정의.
// 참고: app/src/main/java/com/cjlogistics/mini/**

/* ------------------------------------------------------------------ */
/* 공통 / 열거형                                                        */
/* ------------------------------------------------------------------ */

export type VehicleType = "TRUCK_1T" | "TRUCK_2_5T" | "TRUCK_5T" | "TRUCK_11T";

export type DriverStatus = "AVAILABLE" | "BUSY" | "OFFLINE";

export type ShipmentStatus =
  | "REQUESTED"
  | "MATCHING"
  | "DISPATCHED"
  | "EN_ROUTE_TO_PICKUP"
  | "PICKED_UP"
  | "IN_TRANSIT"
  | "COMPLETED"
  | "CANCELED";

export type DispatchStatus =
  | "PROPOSED"
  | "ACCEPTED"
  | "REJECTED"
  | "EXPIRED"
  | "CANCELED"
  | "COMPLETED";

export type Role = "SHIPPER" | "DRIVER";

/* ------------------------------------------------------------------ */
/* 인증                                                                 */
/* ------------------------------------------------------------------ */

export interface ShipperSignupRequest {
  name: string;
  phone: string;
  email: string;
  password: string;
}

export interface VehicleData {
  vehicleType: VehicleType;
  capacityKg: number;
}

export interface RouteData {
  originRegion: string;
  destinationRegion: string;
}

export interface DriverSignupRequest {
  name: string;
  phone: string;
  email: string;
  password: string;
  vehicle: VehicleData;
  preferredRoutes?: RouteData[];
}

export interface DriverUpdateRequest {
  name: string;
  phone: string;
  vehicle: VehicleData;
  preferredRoutes?: RouteData[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
}

/** JWT payload 구조 (JwtTokenService 기준) */
export interface JwtPayload {
  sub: string; // email
  role: Role;
  profileId: number;
  iat: number;
  exp: number;
}

/* ------------------------------------------------------------------ */
/* 화주 / 기사                                                          */
/* ------------------------------------------------------------------ */

export interface ShipperResponse {
  id: number;
  name: string;
  phone: string;
}

export interface ShipperUpdateRequest {
  name: string;
  phone: string;
}

export interface DriverResponse {
  id: number;
  name: string;
  phone: string;
  status: DriverStatus;
  vehicle: { id: number; vehicleType: VehicleType; capacityKg: number };
  preferredRoutes: RouteData[];
}

/* ------------------------------------------------------------------ */
/* 화물 요청                                                            */
/* ------------------------------------------------------------------ */

export interface CargoItemInput {
  description: string;
  weightKg: number;
}

export interface CargoItem {
  id: number;
  description: string;
  weightKg: number;
}

export interface ShipmentRequestCreateRequest {
  shipperId: number;
  originRegion: string;
  destinationRegion: string;
  cargoItems: CargoItemInput[];
  requiredVehicleType: VehicleType;
}

export interface ShipmentRequestResponse {
  id: number;
  shipperId: number;
  originRegion: string;
  destinationRegion: string;
  cargoItems: CargoItem[];
  totalCargoWeightKg: number;
  requiredVehicleType: VehicleType;
  status: ShipmentStatus;
  /** 예상 운임 */
  estimatedFare: number | null;
  createdAt: string; // ISO LocalDateTime
}

/* ------------------------------------------------------------------ */
/* 배차                                                                 */
/* ------------------------------------------------------------------ */

export interface DispatchResponse {
  id: number;
  shipmentRequestId: number;
  driverId: number;
  matchScore: number;
  fare: number | null;
  status: DispatchStatus;
  /** 연결된 화물요청의 운송 상태 (기사 상세 스텝퍼용) */
  shipmentStatus: ShipmentStatus | null;
  createdAt: string;
}

export interface DispatchStatusUpdateRequest {
  status: ShipmentStatus;
}

/** 매칭 후보 기사 (배차 생성 전 조회 결과) */
export interface MatchCandidateResponse {
  driverId: number;
  name: string;
  status: DriverStatus;
  vehicle: { vehicleType: VehicleType; capacityKg: number };
  preferredRoutes: RouteData[];
  matchScore: number;
  /** 예상 운임 (화물 요청 기준, 후보 공통) */
  estimatedFare: number | null;
}

/* ------------------------------------------------------------------ */
/* 알림                                                                 */
/* ------------------------------------------------------------------ */

export type NotificationType =
  | "DISPATCH_REQUESTED"
  | "DISPATCH_ACCEPTED"
  | "DISPATCH_REJECTED"
  | "STATUS_CHANGED";

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  relatedDispatchId: number | null;
  read: boolean;
  createdAt: string; // ISO LocalDateTime
}

export interface UnreadCountResponse {
  unreadCount: number;
}

/* ------------------------------------------------------------------ */
/* 에러                                                                 */
/* ------------------------------------------------------------------ */

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
