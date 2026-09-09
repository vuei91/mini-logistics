import { chromium } from "playwright";

const BASE = "https://d1igl0x2mkyq92.cloudfront.net";
const OUT = "../docs/images";

const SHIPPER = { email: "a@yopmail.com", password: "1q2w3e4r!" };
const DRIVER = { email: "c@yopmail.com", password: "1q2w3e4r!" };

function log(...a) { console.log("[capture]", ...a); }

async function shot(page, name) {
  await page.waitForTimeout(1200);
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: true });
  log("saved", name);
}

async function tryLogin(page, email, password, role) {
  await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
  await page.waitForTimeout(800);
  // 역할 탭 선택 (SHIPPER / DRIVER). 텍스트 대신 순서로 클릭(0=SHIPPER, 1=DRIVER).
  const tabs = page.locator('button[type="button"]');
  if (role === "DRIVER") {
    await tabs.nth(1).click();
    await page.waitForTimeout(300);
  }
  const emailInput = page.locator('input[type="email"]').first();
  const pwInput = page.locator('input[type="password"]').first();
  await emailInput.fill(email);
  await pwInput.fill(password);
  const btn = page.locator('button[type="submit"]').first();
  await btn.click();
  await page.waitForTimeout(3000);
}

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 });
  const page = await context.newPage();

  // 1) 랜딩/로그인
  await page.goto(BASE, { waitUntil: "networkidle" });
  await shot(page, "01-landing");

  await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
  await shot(page, "02-login");

  await page.goto(`${BASE}/signup`, { waitUntil: "networkidle" });
  await shot(page, "03-signup");

  // 2) 화주 로그인 후
  try {
    await tryLogin(page, SHIPPER.email, SHIPPER.password, "SHIPPER");
    await page.goto(`${BASE}/shipper/dashboard`, { waitUntil: "networkidle" });
    await shot(page, "04-shipper-dashboard");
    await page.goto(`${BASE}/shipper/requests/new`, { waitUntil: "networkidle" });
    await shot(page, "05-shipper-request-new");
    await page.goto(`${BASE}/shipper/notifications`, { waitUntil: "networkidle" });
    await shot(page, "06-shipper-notifications");
  } catch (e) {
    log("shipper flow error:", e.message);
  }

  // 3) 기사 로그인 후 (새 컨텍스트로 세션 분리)
  const ctx2 = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 });
  const page2 = await ctx2.newPage();
  try {
    await tryLogin(page2, DRIVER.email, DRIVER.password, "DRIVER");
    await page2.goto(`${BASE}/driver/dashboard`, { waitUntil: "networkidle" });
    await shot(page2, "07-driver-dashboard");
    await page2.goto(`${BASE}/driver/notifications`, { waitUntil: "networkidle" });
    await shot(page2, "08-driver-notifications");
  } catch (e) {
    log("driver flow error:", e.message);
  }

  await browser.close();
  log("done");
})();
