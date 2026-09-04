const state = { data: null, busy: false };
const steps = ["PROPOSED", "ACCEPTED", "EN_ROUTE_TO_PICKUP", "PICKED_UP", "IN_TRANSIT", "COMPLETED"];
const actions = {
  PROPOSED: { label: "배차 수락하기", title: "배차 수락 대기", description: "수락하면 RabbitMQ 배차 확정 이벤트가 생성됩니다.", method: "POST", path: id => `/dispatches/${id}/accept` },
  ACCEPTED: { label: "픽업지로 이동", title: "픽업지 이동 시작", description: "차주가 픽업지로 이동하는 단계입니다.", status: "EN_ROUTE_TO_PICKUP" },
  EN_ROUTE_TO_PICKUP: { label: "상차 완료", title: "화물 상차 대기", description: "상차가 완료되면 운송을 시작할 수 있습니다.", status: "PICKED_UP" },
  PICKED_UP: { label: "운송 시작", title: "운송 시작 대기", description: "화물이 상차되어 목적지로 출발합니다.", status: "IN_TRANSIT" },
  IN_TRANSIT: { label: "운송 완료", title: "운송 완료 처리", description: "완료하면 RabbitMQ 운송 완료 이벤트가 생성됩니다.", status: "COMPLETED" },
  COMPLETED: { label: "운송 완료", title: "모든 데모 흐름 완료", description: "배차·운송 흐름과 이벤트 발행을 완료했습니다." }
};

const $ = selector => document.querySelector(selector);

function workflowStatus({ shipmentRequest, dispatch }) {
  if (dispatch.status === "PROPOSED") return "PROPOSED";
  return shipmentRequest.status === "DISPATCHED" ? "ACCEPTED" : shipmentRequest.status;
}

async function request(path, options = {}) {
  const response = await fetch(path, { headers: { "Content-Type": "application/json" }, ...options });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || "요청 처리에 실패했습니다.");
  }
  return response.status === 204 ? null : response.json();
}

function render() {
  const { shipper, driver, shipmentRequest: shipment, dispatch } = state.data;
  $("#shipper").innerHTML = `<div class="name">${shipper.name}</div><div class="detail">${shipper.phone}</div>`;
  $("#driver").innerHTML = `<div class="name">${driver.name}</div><div class="detail">${driver.vehicle.vehicleType} · ${driver.vehicle.capacityKg.toLocaleString()}kg</div><span class="badge">${driver.status}</span>`;
  const cargoSummary = shipment.cargoItems.map(item => item.description).join(" · ");
  $("#shipment").innerHTML = `<div class="name">${shipment.originRegion} → ${shipment.destinationRegion}</div><div class="detail">${cargoSummary} · 총 ${shipment.totalCargoWeightKg.toLocaleString()}kg</div><span class="badge">${shipment.status}</span>`;
  $("#dispatch").innerHTML = `<div class="name">매칭 점수 ${dispatch.matchScore}</div><div class="detail">차주 #${dispatch.driverId} · 배차 #${dispatch.id}</div><span class="badge">${dispatch.status}</span>`;

  const status = workflowStatus(state.data);
  const currentIndex = steps.indexOf(status);
  document.querySelectorAll(".step").forEach((step, index) => step.className = `step ${index < currentIndex ? "done" : index === currentIndex ? "active" : ""}`);
  const action = actions[status];
  $("#action-title").textContent = action.title;
  $("#action-description").textContent = action.description;
  $("#action-button").textContent = action.label;
  $("#action-button").disabled = state.busy || status === "COMPLETED";
  $("#event-message").textContent = status === "ACCEPTED" || status === "COMPLETED"
    ? "이벤트가 Outbox에 저장되었습니다. RabbitMQ 프로필에서는 약 1초 뒤 Consumer 로그와 관리 화면에서 확인할 수 있습니다."
    : "`demo,rabbitmq` 프로필로 실행하면 배차 수락·운송 완료 시 이벤트가 발행됩니다.";
}

async function loadState() {
  try {
    state.data = await request("/demo/state");
    $("#connection-dot").parentElement.classList.add("connected");
    $("#connection-text").textContent = "데모 데이터가 준비되었습니다";
    render();
  } catch (error) {
    $("#connection-text").textContent = "데모 프로필로 애플리케이션을 실행하세요";
    $("#action-title").textContent = "데모 데이터를 찾을 수 없습니다";
    $("#action-description").textContent = "gradlew.bat bootRun --args=\"--spring.profiles.active=demo,rabbitmq\"";
    $("#action-button").textContent = "데모 실행 필요";
  }
}

$("#action-button").addEventListener("click", async () => {
  if (!state.data || state.busy) return;
  const { dispatch } = state.data;
  const status = workflowStatus(state.data);
  const action = actions[status];
  if (!action || status === "COMPLETED") return;
  state.busy = true;
  render();
  try {
    if (action.status) {
      await request(`/dispatches/${dispatch.id}/status`, { method: "PATCH", body: JSON.stringify({ status: action.status }) });
    } else {
      await request(action.path(dispatch.id), { method: action.method });
    }
    await new Promise(resolve => setTimeout(resolve, 150));
    await loadState();
  } catch (error) {
    alert(error.message);
  } finally {
    state.busy = false;
    if (state.data) render();
  }
});

loadState();
