import { RequestDetailClient } from "./RequestDetailClient";

// 정적 내보내기(output: 'export')에서 동적 라우트는 최소 1개의 경로가 필요하다.
// 실제 id는 클라이언트(useParams)에서 읽고, 그 외 경로는 CloudFront SPA 폴백(404→index.html)이 처리한다.
export function generateStaticParams() {
  return [{ id: "placeholder" }];
}

export default function Page() {
  return <RequestDetailClient />;
}
