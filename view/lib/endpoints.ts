import { apiFetch } from "./api";
import type {
  DispatchResponse,
  DispatchStatusUpdateRequest,
  MatchCandidateResponse,
  DriverResponse,
  DriverSignupRequest,
  DriverUpdateRequest,
  LoginRequest,
  ShipmentRequestCreateRequest,
  ShipmentRequestResponse,
  ShipperResponse,
  ShipperSignupRequest,
  ShipperUpdateRequest,
  ShipmentStatus,
  TokenResponse,
} from "./types";

/* ------------------------------- 인증 ------------------------------- */

export const authApi = {
  shipperSignup: (body: ShipperSignupRequest) =>
    apiFetch<ShipperResponse>("/auth/shippers/signup", {
      method: "POST",
      body,
      auth: false,
    }),

  driverSignup: (body: DriverSignupRequest) =>
    apiFetch<DriverResponse>("/auth/drivers/signup", {
      method: "POST",
      body,
      auth: false,
    }),

  shipperLogin: (body: LoginRequest) =>
    apiFetch<TokenResponse>("/auth/shippers/login", {
      method: "POST",
      body,
      auth: false,
    }),

  driverLogin: (body: LoginRequest) =>
    apiFetch<TokenResponse>("/auth/drivers/login", {
      method: "POST",
      body,
      auth: false,
    }),
};

/* ---------------------------- 화주 / 기사 ---------------------------- */

export const shipperApi = {
  get: (id: number) => apiFetch<ShipperResponse>(`/shippers/${id}`),

  /** 로그인한 화주 본인 프로필 조회 */
  getMe: () => apiFetch<ShipperResponse>("/shippers/me"),

  /** 로그인한 화주 본인 프로필 수정 */
  updateMe: (body: ShipperUpdateRequest) =>
    apiFetch<ShipperResponse>("/shippers/me", { method: "PATCH", body }),
};

export const driverApi = {
  get: (id: number) => apiFetch<DriverResponse>(`/drivers/${id}`),

  /** 로그인한 기사 본인 프로필 조회 */
  getMe: () => apiFetch<DriverResponse>("/drivers/me"),

  /** 로그인한 기사 본인 프로필 수정 */
  updateMe: (body: DriverUpdateRequest) =>
    apiFetch<DriverResponse>("/drivers/me", { method: "PATCH", body }),
};

/* ------------------------------ 화물 요청 ---------------------------- */

export const shipmentApi = {
  /** 내(로그인한 화주) 화물 요청 목록 */
  listMine: () => apiFetch<ShipmentRequestResponse[]>("/shipment-requests"),

  get: (id: number) =>
    apiFetch<ShipmentRequestResponse>(`/shipment-requests/${id}`),

  create: (body: ShipmentRequestCreateRequest) =>
    apiFetch<ShipmentRequestResponse>("/shipment-requests", {
      method: "POST",
      body,
    }),

  cancel: (id: number) =>
    apiFetch<ShipmentRequestResponse>(`/shipment-requests/${id}/cancel`, {
      method: "POST",
    }),

  /** 매칭 후보 기사 목록 조회 (배차 생성 없음) */
  matchCandidates: (shipmentRequestId: number) =>
    apiFetch<MatchCandidateResponse[]>(
      `/shipment-requests/${shipmentRequestId}/match-candidates`,
    ),

  /** 배차 생성. driverId 지정 시 해당 기사로, 미지정 시 최적 후보 자동 선택 */
  dispatch: (shipmentRequestId: number, driverId?: number) =>
    apiFetch<DispatchResponse>(
      `/shipment-requests/${shipmentRequestId}/dispatch`,
      {
        method: "POST",
        body: driverId != null ? { driverId } : undefined,
      },
    ),
};

/* -------------------------------- 배차 ------------------------------- */

export const dispatchApi = {
  /** 내(로그인한 기사) 배차 목록 */
  listMine: () => apiFetch<DispatchResponse[]>("/dispatches"),

  get: (id: number) => apiFetch<DispatchResponse>(`/dispatches/${id}`),

  accept: (id: number) =>
    apiFetch<DispatchResponse>(`/dispatches/${id}/accept`, { method: "POST" }),

  reject: (id: number) =>
    apiFetch<DispatchResponse>(`/dispatches/${id}/reject`, { method: "POST" }),

  updateStatus: (id: number, status: ShipmentStatus) =>
    apiFetch<DispatchResponse>(`/dispatches/${id}/status`, {
      method: "PATCH",
      body: { status } satisfies DispatchStatusUpdateRequest,
    }),
};
