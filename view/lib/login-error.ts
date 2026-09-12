import type { ApiError } from "./api";

export function getLoginErrorMessage(error: unknown): string {
  if (!isApiError(error)) {
    return "로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
  }

  const codeMessage = messageForCode(error.body?.code);
  if (codeMessage) return codeMessage;

  if (error.status === 0) {
    return "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.";
  }
  if (error.status === 429) {
    return "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.";
  }
  if (error.status >= 500) {
    return "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
  }

  return "로그인에 실패했습니다. 입력한 정보를 확인해주세요.";
}

function messageForCode(code: string | undefined): string | undefined {
  switch (code) {
    case "INVALID_CREDENTIALS":
      return "이메일 또는 비밀번호가 올바르지 않습니다.";
    case "VALIDATION_FAILED":
      return "이메일 형식과 비밀번호를 확인해주세요.";
    case "MALFORMED_REQUEST":
      return "로그인 요청 형식이 올바르지 않습니다.";
    case "TOO_MANY_REQUESTS":
      return "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.";
    default:
      return undefined;
  }
}

function isApiError(error: unknown): error is ApiError {
  return (
    error instanceof Error &&
    "status" in error &&
    typeof error.status === "number"
  );
}
