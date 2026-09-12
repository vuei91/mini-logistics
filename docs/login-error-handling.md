# 로그인 오류 처리 목록

로그인 실패 응답은 HTTP 상태만 보지 않고 `ErrorResponse.code`를 함께 사용한다. 계정 존재 여부는 노출하지 않기 위해 이메일 미등록과 비밀번호 불일치를 모두 `INVALID_CREDENTIALS`로 응답한다.

| 상황 | HTTP / 코드 | 프론트엔드 처리 |
|---|---|---|
| 이메일 형식 오류, 빈 비밀번호 | `400 VALIDATION_FAILED` | 입력 형식 확인 안내 |
| JSON 등 요청 본문 파싱 실패 | `400 MALFORMED_REQUEST` | 요청 형식 오류 안내 |
| 이메일 미등록 또는 비밀번호 불일치 | `401 INVALID_CREDENTIALS` | 동일한 자격 증명 오류 안내, 토큰 삭제/리다이렉트 안 함 |
| 보호 API의 토큰 없음·만료·위조 | `401 AUTHENTICATION_REQUIRED` | 저장 토큰 삭제 후 로그인 화면으로 이동 |
| 로그인했지만 역할/소유권 불일치 | `403 ACCESS_DENIED` | 권한 없음 안내 |
| 지원하지 않는 HTTP 메서드 | `405 METHOD_NOT_ALLOWED` | 요청 방식 오류 안내 |
| 외부 API 게이트웨이의 로그인 시도 제한 | `429 TOO_MANY_REQUESTS` | 잠시 후 재시도 안내 |
| 백엔드의 예상하지 못한 오류 | `500` | 응답 상세를 노출하지 않고 일시 오류 안내 |
| 서버 미실행, DNS/CORS/네트워크 단절 | 프론트 `status: 0` | 연결 실패와 재시도 안내 |
| 성공 응답의 JWT가 손상·만료됨 | 프론트 일반 처리 오류 | 토큰을 저장하지 않고 재시도 안내 |

오류 응답 형식:

```json
{
  "timestamp": "2026-09-12T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "path": "/api/auth/shippers/login"
}
```

로그인 UI는 서버의 임의 메시지를 그대로 노출하지 않고 알려진 코드와 상태를 사용자용 문구로 변환한다. 이로써 내부 오류 정보 노출과 백엔드 문구 변경에 따른 UI 결합을 피한다.

현재 Spring 애플리케이션 자체에는 로그인 시도 제한기가 없다. `429` 처리는 API 게이트웨이·WAF 등 외부 계층에서 제한을 적용할 때를 위한 프론트 계약이며, 운영 전 해당 계층의 rate limit 설정이 필요하다.
