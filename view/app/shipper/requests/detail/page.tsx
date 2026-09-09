import { Suspense } from "react";
import { RequestDetailClient } from "./RequestDetailClient";

// 쿼리스트링(?id=) 방식이므로 동적 세그먼트가 없다.
// 정적 export 시 이 페이지 하나(/shipper/requests/detail/)만 생성되고,
// 실제 id 는 클라이언트가 useSearchParams 로 읽는다.
export default function Page() {
  return (
    <Suspense fallback={null}>
      <RequestDetailClient />
    </Suspense>
  );
}
