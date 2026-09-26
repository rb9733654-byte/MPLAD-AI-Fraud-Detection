const API = `${window.location.protocol}//${window.location.hostname}:8080/api`;
let contractorId = "";
let contractorCsrf = "";
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const form = document.querySelector("#progressForm");
const locationStatus = document.querySelector("#locationStatus");
const assignedProject = document.querySelector("#assignedProject");

async function readJson(response) {
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message || payload.detail || payload.title || "The request could not be completed.");
  return payload;
}
async function loadAssignedProjects() {
  try {
    const projects = await readJson(await fetch(`${API}/contractors/${encodeURIComponent(contractorId)}/projects`, { credentials: "include" }));
    if (!Array.isArray(projects)) throw new Error("Assigned projects could not be loaded.");
    assignedProject.innerHTML = projects.length ? `<option value="">Select a project</option>${projects.map((project) => `<option value="${escapeHtml(project.id)}">${escapeHtml(project.projectId)} — ${escapeHtml(project.projectType)}</option>`).join("")}` : `<option value="">No assigned projects</option>`;
    assignedProject.disabled = !projects.length;
    renderAssignedProjects(projects);
  } catch (error) {
    assignedProject.innerHTML = `<option value="">Unable to load assigned projects</option>`;
    assignedProject.disabled = true;
    document.querySelector("#assignedProjectCount").textContent = "Unavailable";
    document.querySelector("#assignedActiveCount").textContent = "—";
    document.querySelector("#assignedCompletedCount").textContent = "—";
    document.querySelector("#assignedProjectList").innerHTML = `<p class="analysis-error">${escapeHtml(error.message)} Check that the monitoring service is running.</p>`;
    locationStatus.textContent = error.message;
  }
}
function renderAssignedProjects(projects) {
  const count = document.querySelector("#assignedProjectCount");
  const list = document.querySelector("#assignedProjectList");
  count.textContent = `${projects.length} ${projects.length === 1 ? "project" : "projects"}`;
  const completedCount = projects.filter((project) => project.isCompleted || Number(project.completionPercentage) >= 100).length;
  document.querySelector("#assignedCompletedCount").textContent = completedCount;
  document.querySelector("#assignedActiveCount").textContent = projects.length - completedCount;
  if (!projects.length) {
    list.innerHTML = `<p class="empty-state">No projects are assigned to this contractor account yet.</p>`;
    return;
  }
  list.innerHTML = projects.map((project) => {
    const completion = Number(project.completionPercentage ?? 0);
    const safeCompletion = Number.isFinite(completion) ? Math.min(100, Math.max(0, completion)) : 0;
    const amount = (value) => value == null ? "Not available" : `₹${Number(value).toLocaleString("en-IN", { maximumFractionDigits: 2 })} L`;
    const status = project.isCompleted || safeCompletion >= 100 ? "Completed" : "In progress";
    return `<article class="assigned-project-card"><div class="assigned-project-heading"><div><h3>${escapeHtml(project.projectId || `Project ${project.id}`)}</h3><p>${escapeHtml(project.projectType || "MPLAD Project")}</p></div><span>${escapeHtml(status)}</span></div><div class="project-progress-track" role="progressbar" aria-label="Project completion" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${safeCompletion}"><span style="width:${safeCompletion}%"></span></div><p class="project-completion-label">${safeCompletion}% complete</p><dl class="assigned-project-facts"><div><dt>Location</dt><dd>${escapeHtml([project.district, project.constituency].filter(Boolean).join(", ") || "Not available")}</dd></div><div><dt>Sanctioned amount</dt><dd>${amount(project.sanctionedAmount)}</dd></div><div><dt>Released amount</dt><dd>${amount(project.releasedAmount)}</dd></div><div><dt>Actual expenditure</dt><dd>${amount(project.actualExpenditure)}</dd></div><div><dt>Delay</dt><dd>${project.delayDays == null ? "Not available" : `${escapeHtml(project.delayDays)} days`}</dd></div></dl></article>`;
  }).join("");
}
function renderNotifications(notifications) {
  const unread = notifications.filter((item) => !item.read).length;
  document.querySelector("#unreadCount").textContent = `${unread} unread`;
  const list = document.querySelector("#notificationList");
  list.innerHTML = notifications.length ? notifications.map((item) => `<article class="notification-item ${item.read ? "is-read" : "is-unread"}"><div class="notification-meta"><span class="notification-state">${item.read ? "Read" : "Unread"}</span><time>${escapeHtml(item.createdAt ? new Date(item.createdAt).toLocaleString("en-IN") : "Date not recorded")}</time></div><h3>${escapeHtml(item.title)}</h3><p class="notification-project">${escapeHtml(item.projectCode)}</p><p>${escapeHtml(item.message)}</p>${item.read ? "" : `<button class="details-button" type="button" data-mark-read="${escapeHtml(item.id)}">Mark as read</button>`}</article>`).join("") : `<p class="empty-state">No authority notifications yet.</p>`;
}
async function loadNotifications() {
  const list = document.querySelector("#notificationList");
  try {
    const items = await readJson(await fetch(`${API}/contractors/${encodeURIComponent(contractorId)}/notifications`, { credentials: "include" }));
    if (!Array.isArray(items)) throw new Error("Notifications could not be loaded.");
    renderNotifications(items);
  } catch (error) {
    document.querySelector("#unreadCount").textContent = "Unavailable";
    list.innerHTML = `<p class="analysis-error">${escapeHtml(error.message)} Check that the monitoring service is running.</p>`;
  }
}
document.querySelector("#captureLocation").addEventListener("click", () => {
  if (!navigator.geolocation) { locationStatus.textContent = "Location unavailable — Verification Required"; return; }
  locationStatus.textContent = "Requesting device location…";
  navigator.geolocation.getCurrentPosition((position) => {
    form.elements.latitude.value = position.coords.latitude;
    form.elements.longitude.value = position.coords.longitude;
    locationStatus.textContent = "Location captured. Verification is calculated when the update is submitted.";
  }, () => { locationStatus.textContent = "Location unavailable — Verification Required"; }, { enableHighAccuracy: true, timeout: 10000 });
});
document.querySelector("#notificationList").addEventListener("click", async (event) => {
  const button = event.target.closest("[data-mark-read]");
  if (!button) return;
  button.disabled = true;
  try {
    const response = await fetch(`${API}/contractors/${encodeURIComponent(contractorId)}/notifications/${encodeURIComponent(button.dataset.markRead)}/read`, { method: "PATCH", credentials: "include", headers: { "X-CSRF-Token": contractorCsrf } });
    if (!response.ok) await readJson(response);
    await loadNotifications();
  } catch (error) {
    button.disabled = false;
    button.textContent = error.message || "Could not mark as read";
  }
});
form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const output = document.querySelector("#submissionStatus");
  const button = form.querySelector("button[type=submit]");
  if (!assignedProject.value) { output.textContent = "Select an assigned project before submitting."; return; }
  const data = new FormData(form);
  data.set("contractorId", contractorId);
  button.disabled = true; output.textContent = "Submitting progress update…";
  try {
    const result = await readJson(await fetch(`${API}/projects/${encodeURIComponent(assignedProject.value)}/progress-evidence`, { method: "POST", credentials: "include", headers: { "X-CSRF-Token": contractorCsrf }, body: data }));
    output.textContent = `${result.acceptedForProjectState ? "Update accepted and current project state synchronized." : "Evidence saved to history only; current project values were not changed because location verification is incomplete or mismatched."} Location status: ${result.verificationStatus || "Verification Required"}${result.distanceMetres != null ? ` · ${result.distanceMetres} metres from project location` : ""}.`;
    form.reset(); locationStatus.textContent = "Verification Required — capture a location where available.";
  } catch (error) { output.textContent = error.message; }
  finally { button.disabled = false; }
});
document.querySelector("#contractorLogout").addEventListener("click", async () => {
  try { await fetch(`${API}/workspace-auth/logout`, { method: "POST", credentials: "include", headers: { "X-CSRF-Token": contractorCsrf } }); } catch { /* Session may already have expired. */ }
  window.location.replace("contractor-auth.html");
});
(async function initializeContractorWorkspace() {
  try {
    const response = await fetch(`${API}/workspace-auth/contractor/me`, { credentials: "include" });
    if (!response.ok) { window.location.replace("contractor-auth.html"); return; }
    const account = await response.json(); contractorId = account.identity; contractorCsrf = account.csrfToken;
    document.querySelector(".portal-title .eyebrow").textContent = `Field monitoring workspace · ${contractorId}`;
    document.body.hidden = false;
    await Promise.all([loadAssignedProjects(), loadNotifications()]);
  } catch { window.location.replace("contractor-auth.html"); }
})();
