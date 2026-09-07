import { apiFetch } from "./api";
import type {
  DispatchResponse,
  DispatchStatusUpdateRequest,
  DriverResponse,
  DriverSignupRequest,
  LoginRequest,
  ShipmentRequestCreateRequest,
  ShipmentRequestResponse,
  ShipperResponse,
  ShipperSignupRequest,
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
};

export const driverApi = {
  get: (id: number) => apiFetch<DriverResponse>(`/drivers/${id}`),
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

  /** 매칭 실행 + 배차 생성 */
  dispatch: (shipmentRequestId: number) =>
    apiFetch<DispatchResponse>(
      `/shipment-requests/${shipmentRequestId}/dispatch`,
      { method: "POST" },
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
