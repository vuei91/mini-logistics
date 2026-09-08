# AWS 배포 가이드

CJ Logistics Mini 를 AWS에 배포하기 위한 순서 정리다.

## 아키텍처

```
[사용자]
   │
   ├─ 프론트(Next.js 정적)  ── S3 + CloudFront (HTTPS/CDN)
   │        │  NEXT_PUBLIC_API_BASE_URL → 백엔드 API
   ▼
[EC2 t3.small] ── docker-compose.prod.yml
   ├─ backend   (Spring Boot, profile=rabbitmq, :8080)
   └─ rabbitmq  (내부 네트워크 전용)
   │
   ▼
[RDS PostgreSQL 16]  (db.t3.micro, 프라이빗)
```

- **EC2**: 백엔드 + RabbitMQ 컨테이너 (상태 있는 브로커를 직접 운영)
- **RDS**: PostgreSQL 16, DB명 `cjlogistics`
- **S3 + CloudFront**: 프론트 정적 파일 + HTTPS + SPA 라우팅 폴백

---

## 1. RDS PostgreSQL 생성

1. RDS 콘솔 → 데이터베이스 생성 → PostgreSQL 16
2. 템플릿: **프리 티어**, 인스턴스: `db.t3.micro`
3. 설정
   - DB 인스턴스 식별자: `cjlogistics`
   - 마스터 사용자: `cjadmin` / 강력한 비밀번호
   - 초기 DB 이름: `cjlogistics` (추가 구성에서 지정)
4. 네트워크: EC2와 **같은 VPC**, 퍼블릭 액세스 **아니오**
5. 보안 그룹: 인바운드 5432 를 **EC2 보안그룹**에서만 허용 (아래 4단계에서 연결)
6. 생성 후 **엔드포인트**를 기록 → `.env.prod` 의 `DB_URL` 에 사용

---

## 2. S3 버킷 + CloudFront (프론트)

### 2-1. 프론트 빌드

```bash
cd view
cp .env.production.example .env.production
# .env.production 의 NEXT_PUBLIC_API_BASE_URL 을 백엔드 API 주소로 수정
npm ci
npm run build
# 정적 결과물은 view/out/ 에 생성된다
```

### 2-2. S3 업로드

1. S3 버킷 생성 (예: `cjlogistics-web`), 퍼블릭 액세스는 차단 유지
2. `view/out/` 내용을 업로드
   ```bash
   aws s3 sync out/ s3://cjlogistics-web/ --delete
   ```

### 2-3. CloudFront 배포

1. 배포 생성 → 오리진: 위 S3 버킷 (OAC 사용, S3 퍼블릭 비공개 유지)
2. 기본 루트 객체: `index.html`
3. **SPA 폴백** (동적 라우트 `[id]` 클라이언트 라우팅 처리)
   - 오류 페이지 설정에 아래 두 개 추가:
     - HTTP 오류 코드 `403` → 응답 페이지 `/index.html`, 응답 코드 `200`
     - HTTP 오류 코드 `404` → 응답 페이지 `/index.html`, 응답 코드 `200`
4. 배포 도메인(`dxxxx.cloudfront.net`) 기록 → 백엔드 `CORS_ALLOWED_ORIGINS` 에 사용
5. 재배포 시 캐시 무효화:
   ```bash
   aws cloudfront create-invalidation --distribution-id <DIST_ID> --paths "/*"
   ```

---

## 3. EC2 생성

1. EC2 콘솔 → 인스턴스 시작
   - AMI: Ubuntu 22.04 LTS
   - 타입: **t3.small** (백엔드+RabbitMQ에 2GB 권장, 프리티어만 원하면 t2.micro + swap)
   - 스토리지: 20GB
   - RDS와 **같은 VPC**
2. 보안 그룹 인바운드
   - `22` (SSH): **내 IP** 만
   - `80`, `443`: 0.0.0.0/0 (nginx/HTTPS를 붙이는 경우)
   - `8080`: 임시 검증용으로만 열고, 운영에서는 nginx 뒤로 숨기는 것을 권장
   - `5672`, `15672`, `5432` 는 **열지 않는다** (내부 전용)

### 3-1. Docker 설치

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc > /dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
# 재로그인 후 docker 명령을 sudo 없이 사용
```

---

## 4. 보안 그룹 연결 (RDS ← EC2)

RDS 보안 그룹 인바운드 규칙에 `PostgreSQL(5432)` 을 추가하고, 소스를 **EC2 보안 그룹 ID** 로 지정한다. (IP가 아니라 SG 참조로 연결하면 EC2 IP가 바뀌어도 유지됨)

---

## 5. 백엔드 + RabbitMQ 배포

```bash
git clone <이 저장소 URL>
cd cj-logistics
cp .env.prod.example .env.prod
# .env.prod 값 채우기: DB_URL(RDS 엔드포인트), 비밀번호들, CORS(CloudFront 도메인)

docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml logs -f backend
```

확인:
- `curl http://localhost:8080/actuator/health` (또는 Swagger: `http://<EC2_IP>:8080/swagger-ui.html`)
- RabbitMQ 관리 UI가 필요하면 SSH 터널: `ssh -L 15672:localhost:15672 ubuntu@<EC2_IP>` 후 브라우저에서 `http://localhost:15672`

---

## 6. (선택) HTTPS / 도메인

8080 을 직접 노출하는 대신 nginx 리버스 프록시 + certbot 으로 HTTPS 를 붙이는 것을 권장한다.
- `api.your-domain.com` → EC2 → nginx(443) → backend(8080)
- 프론트 `.env.production` 의 `NEXT_PUBLIC_API_BASE_URL` 을 이 API 도메인으로 설정 후 재빌드/재배포
- 백엔드 `CORS_ALLOWED_ORIGINS` 에 CloudFront/프론트 도메인 지정

---

## 배포 후 값 연결 요약

| 값 | 어디서 얻나 | 어디에 넣나 |
|---|---|---|
| RDS 엔드포인트 | RDS 콘솔 | `.env.prod` 의 `DB_URL` |
| CloudFront 도메인 | CloudFront 콘솔 | `.env.prod` 의 `CORS_ALLOWED_ORIGINS` |
| 백엔드 API 주소 | EC2 IP/도메인 | `view/.env.production` 의 `NEXT_PUBLIC_API_BASE_URL` |

---

## 7. CI/CD (GitHub Actions)

리모트가 GitHub(`vuei91/mini-logistics`)이므로 GitHub Actions로 배포를 자동화한다.
`main` 브랜치에 머지되면 변경된 경로에 따라 프론트/백엔드가 각각 배포된다.

- `.github/workflows/deploy-frontend.yml` — `view/**` 변경 시: 정적 빌드 → S3 sync → CloudFront 무효화 (AWS 인증은 OIDC, 키 없음)
- `.github/workflows/deploy-backend.yml` — `app/**` 또는 `docker-compose.prod.yml` 변경 시: Gradle 빌드/테스트 → SSH로 EC2 접속 → `git pull` + `docker compose up -d --build`

### 7-1. 프론트 배포용 AWS OIDC 역할 만들기

액세스 키를 저장하지 않고, GitHub Actions가 임시 자격증명을 발급받도록 OIDC를 사용한다.

1. IAM → 자격 증명 공급자 → 공급자 추가 → OpenID Connect
   - 공급자 URL: `https://token.actions.githubusercontent.com`
   - 대상(Audience): `sts.amazonaws.com`
2. IAM 역할 생성 (웹 자격 증명) → 위 공급자 선택
   - 신뢰 정책의 `sub` 조건을 이 저장소로 제한:
     `repo:vuei91/mini-logistics:ref:refs/heads/main`
3. 역할에 최소 권한 정책 부여 (해당 버킷/배포로 한정 권장):
   - `s3:ListBucket`, `s3:PutObject`, `s3:DeleteObject` (대상: `cjlogistics-web` 및 그 객체)
   - `cloudfront:CreateInvalidation` (대상: 해당 배포)
4. 생성된 역할 ARN을 `AWS_DEPLOY_ROLE_ARN` 시크릿에 저장

### 7-2. 백엔드 배포용 SSH 키

1. 배포 전용 SSH 키페어 생성: `ssh-keygen -t ed25519 -f deploy_key -N ""`
2. 공개키(`deploy_key.pub`)를 EC2의 `~/.ssh/authorized_keys`에 추가
3. 개인키(`deploy_key`) 전체 내용을 `EC2_SSH_KEY` 시크릿에 저장
4. EC2에는 이 저장소가 `EC2_APP_DIR` 경로에 clone 되어 있고, `.env.prod`가 채워져 있어야 한다

### 7-3. GitHub Secrets 목록

저장소 → Settings → Secrets and variables → Actions 에 등록:

| Secret | 설명 | 예시 |
|---|---|---|
| `AWS_REGION` | 리전 | `ap-northeast-2` |
| `AWS_DEPLOY_ROLE_ARN` | 프론트 배포용 OIDC 역할 ARN | `arn:aws:iam::123456789012:role/gha-deploy` |
| `S3_BUCKET` | 프론트 정적 버킷 | `cjlogistics-web` |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront 배포 ID | `E123ABC456DEF` |
| `NEXT_PUBLIC_API_BASE_URL` | 백엔드 API 주소(빌드 시 주입) | `https://api.your-domain.com` |
| `EC2_HOST` | EC2 공인 IP/도메인 | `13.xxx.xxx.xxx` |
| `EC2_USER` | SSH 사용자 | `ubuntu` |
| `EC2_SSH_KEY` | 배포용 SSH 개인키(전체 내용) | `-----BEGIN OPENSSH PRIVATE KEY----- ...` |
| `EC2_APP_DIR` | EC2의 저장소 경로 | `/home/ubuntu/cj-logistics` |

### 7-4. 동작 방식

- `view/` 만 바꾸면 프론트 워크플로만, `app/` 만 바꾸면 백엔드 워크플로만 실행된다.
- 수동 실행은 각 워크플로의 "Run workflow"(workflow_dispatch)로 가능하다.
- 백엔드는 빌드/테스트가 통과해야 배포 단계로 넘어간다.
