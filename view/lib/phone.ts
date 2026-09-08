/**
 * 휴대폰 번호 입력값을 010-1234-5678 형태로 자동 포맷팅합니다.
 * 숫자만 추출한 뒤 자릿수에 맞춰 하이픈을 삽입합니다.
 */
export function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 11);

  if (digits.length < 4) {
    return digits;
  }
  if (digits.length < 8) {
    return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  }
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}
