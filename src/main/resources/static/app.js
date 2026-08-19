const users = {
  "wx-chen": { name: "陈先生" },
  "wx-liu": { name: "刘女士" }
};

let currentUser = "wx-chen";
let currentAgentUser = "";

const customerMsgs = document.getElementById("customerMsgs");
const agentMsgs = document.getElementById("agentMsgs");
const sessionList = document.getElementById("sessionList");
const ticketBody = document.getElementById("ticketBody");
const agentTitle = document.getElementById("agentTitle");
const agentMode = document.getElementById("agentMode");
const llmPill = document.getElementById("llmPill");

function roleClass(role) {
  if (role === "user") return "me";
  if (role === "agent") return "agent";
  if (role === "system") return "system";
  return "ai";
}

function roleLabel(role) {
  if (role === "user") return "业主";
  if (role === "agent") return "坐席";
  if (role === "system") return "系统";
  return "助手";
}

function renderMessages(el, messages, mineRole) {
  el.innerHTML = "";
  (messages || []).forEach((m) => {
    const row = document.createElement("div");
    row.className = "row " + (m.role === mineRole ? "me" : roleClass(m.role));
    const wrap = document.createElement("div");
    const meta = document.createElement("div");
    meta.className = "meta";
    meta.textContent = roleLabel(m.role);
    const bubble = document.createElement("div");
    bubble.className = "bubble";
    bubble.textContent = m.content;
    wrap.appendChild(meta);
    wrap.appendChild(bubble);
    row.appendChild(wrap);
    el.appendChild(row);
  });
  el.scrollTop = el.scrollHeight;
}

async function loadMeta() {
  const res = await fetch("/api/meta");
  const data = await res.json();
  llmPill.textContent = data.liveModel ? ("模型：" + data.llm) : ("模型：Mock（未配置 DASHSCOPE_API_KEY）");
}

async function loadCustomer() {
  const res = await fetch("/api/customer/" + currentUser);
  const data = await res.json();
  renderMessages(customerMsgs, data.messages, "user");
}

async function loadSessions() {
  const res = await fetch("/api/sessions");
  const list = await res.json();
  sessionList.innerHTML = "";
  list.forEach((s) => {
    const item = document.createElement("div");
    item.className = "session" + (s.userId === currentAgentUser ? " active" : "");
    item.innerHTML = "<div class='name'>" + s.userName + " · " + s.mode + "</div>"
      + "<div class='preview'>" + (s.lastMessage || "暂无消息") + "</div>";
    item.onclick = () => selectSession(s.userId);
    sessionList.appendChild(item);
  });
}

async function selectSession(userId) {
  currentAgentUser = userId;
  const res = await fetch("/api/sessions/" + userId);
  const data = await res.json();
  agentTitle.textContent = data.userName;
  agentMode.textContent = data.mode === "HUMAN" ? "人工接管中" : "AI 辅助中";
  renderMessages(agentMsgs, data.messages, "agent");
  loadSessions();
}

async function loadTickets() {
  const res = await fetch("/api/tickets");
  const list = await res.json();
  if (!list.length) {
    ticketBody.innerHTML = "<tr><td colspan='6' class='empty'>还没有工单。业主说「报修」或「投诉」会走工具调用。</td></tr>";
    return;
  }
  ticketBody.innerHTML = list.map((t) =>
    "<tr><td>" + t.id + "</td><td>" + t.type + "</td><td>" + t.title + "</td><td>"
    + t.level + "</td><td>" + t.status + "</td><td>" + (t.assignee || "-") + "</td></tr>"
  ).join("");
}

async function sendCustomer(text) {
  await fetch("/api/customer/send", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId: currentUser,
      userName: users[currentUser].name,
      content: text,
      channel: "wechat"
    })
  });
  currentAgentUser = currentUser;
  setTimeout(() => {
    loadCustomer();
    loadSessions();
    selectSession(currentUser);
    loadTickets();
  }, 250);
}

document.getElementById("userSelect").onchange = (e) => {
  currentUser = e.target.value;
  loadCustomer();
};

document.querySelectorAll(".chips button").forEach((btn) => {
  btn.onclick = () => sendCustomer(btn.getAttribute("data-text"));
});

document.getElementById("customerForm").onsubmit = (e) => {
  e.preventDefault();
  const input = document.getElementById("customerInput");
  const text = input.value.trim();
  if (!text) return;
  input.value = "";
  sendCustomer(text);
};

document.getElementById("takeoverBtn").onclick = async () => {
  if (!currentAgentUser) return;
  await fetch("/api/sessions/" + currentAgentUser + "/takeover", { method: "POST" });
  selectSession(currentAgentUser);
  loadCustomer();
};

document.getElementById("releaseBtn").onclick = async () => {
  if (!currentAgentUser) return;
  await fetch("/api/sessions/" + currentAgentUser + "/release", { method: "POST" });
  selectSession(currentAgentUser);
  loadCustomer();
};

document.getElementById("agentForm").onsubmit = async (e) => {
  e.preventDefault();
  if (!currentAgentUser) return;
  const input = document.getElementById("agentInput");
  const text = input.value.trim();
  if (!text) return;
  input.value = "";
  await fetch("/api/sessions/" + currentAgentUser + "/reply", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content: text })
  });
  selectSession(currentAgentUser);
  loadCustomer();
};

const sse = new EventSource("/api/events");
sse.addEventListener("session", () => {
  loadCustomer();
  loadSessions();
  if (currentAgentUser) selectSession(currentAgentUser);
});
sse.addEventListener("ticket", () => loadTickets());

loadMeta();
loadCustomer();
loadSessions();
loadTickets();
