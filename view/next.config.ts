import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // S3 + CloudFront 정적 호스팅을 위한 정적 내보내기(SPA) 모드
  output: "export",
  // 정적 호스팅에서 경로 일관성을 위해 폴더형 URL(/path/) 사용
  trailingSlash: true,
  // 정적 내보내기에서는 Next 이미지 최적화 서버를 쓸 수 없음
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
