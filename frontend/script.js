const API_ROOT = `${window.location.protocol}//${window.location.hostname}:8080/api`;
const PROJECTS_API_URL = `${API_ROOT}/projects`;
const FUND_UTILIZATION_API_URL = `${API_ROOT}/fund-utilization`;
const AI_DISCLAIMER = "AI-generated results are monitoring signals and do not confirm fraud. Final decisions remain with authorized human reviewers.";
let authorityCsrfToken = "";


const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const riskClass = (level) => `risk-${["low", "medium", "high", "critical"].includes(String(level || "").toLowerCase()) ? String(level).toLowerCase() : "low"}`;
const statusClass = (status) => `status-${["completed", "in progress", "delayed", "not started"].includes(String(status || "").toLowerCase()) ? String(status).toLowerCase().replaceAll(" ", "-") : "unknown"}`;
function formatMoney(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? `₹ ${amount.toLocaleString("en-IN", { maximumFractionDigits: 2 })} L` : "Not available";
}
function formatPercent(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? `${amount.toFixed(1)}%` : "Not available";
}
async function getJson(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (authorityCsrfToken && !["GET", "HEAD", "OPTIONS"].includes((options.method || "GET").toUpperCase())) headers["X-CSRF-Token"] = authorityCsrfToken;
  const response = await fetch(url, { credentials: "include", ...options, headers });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message || payload.detail || payload.title || "The request could not be completed.");
  return payload;
}
async function loadProjects() {
  const records = await getJson(PROJECTS_API_URL);
  if (!Array.isArray(records)) throw new Error("Project records could not be loaded.");
  return records.map((project) => {
    const allocated = Number(project.sanctionedAmount) || 0;
    const spent = Number(project.actualExpenditure) || 0;
    const completion = Number(project.completionPercentage) || 0;
    const delayDays = Number(project.delayDays) || 0;
    const completed = completion >= 100;
    const derivedIndicator = Math.min(100, Math.round((Math.max(0, spent - allocated) / Math.max(allocated, 1)) * 35 + Math.min(delayDays * 2, 45) + (completed ? 0 : completion < 25 ? 20 : completion < 60 ? 10 : 0)));
    const derivedLevel = derivedIndicator >= 75 ? "Critical" : derivedIndicator >= 55 ? "High" : derivedIndicator >= 30 ? "Medium" : "Low";
    const hasSavedAnalysis = project.lastAnalysisScore != null && project.lastAnalysisLevel;
    const indicator = hasSavedAnalysis ? Number(project.lastAnalysisScore) : derivedIndicator;
    const level = hasSavedAnalysis ? project.lastAnalysisLevel[0].toUpperCase() + project.lastAnalysisLevel.slice(1).toLowerCase() : derivedLevel;
    const status = completion >= 100 ? "Completed" : "In Progress";
    const analysis = hasSavedAnalysis ? { anomaly_score: indicator, risk_level: level, anomaly_status: "Previously generated AI signal", model_output: "Saved result from the existing anomaly service." } : null;
    return { ...project, analysis, isCompleted: completed, databaseId: project.id, id: project.projectId || String(project.id), name: project.projectType || "MPLAD Project", location: [project.district, project.constituency].filter(Boolean).join(", "), category: project.projectType || "Uncategorised", allocated, spent, completion, delayDays, status, risk: indicator, level };
  });
}
async function analyzeProject(projectId) {
  return getJson(`${PROJECTS_API_URL}/${encodeURIComponent(projectId)}/analyze`, { method: "POST" });
}
async function loadFundOverview() { return getJson(`${FUND_UTILIZATION_API_URL}/overview`); }
async function loadProjectFundUtilization(id) { return getJson(`${PROJECTS_API_URL}/${encodeURIComponent(id)}/fund-utilization`); }
async function loadProgressEvidence(id) { return getJson(`${PROJECTS_API_URL}/${encodeURIComponent(id)}/progress-evidence`); }
async function loadProjectHistory(id) { return getJson(`${PROJECTS_API_URL}/${encodeURIComponent(id)}/history`); }
async function loadReviews(id) { return getJson(`${PROJECTS_API_URL}/${encodeURIComponent(id)}/reviews`); }
async function loadPublicFeedback(id) { return getJson(`${API_ROOT}/public/projects/${encodeURIComponent(id)}/feedback`); }

function calculateDashboardStats(projects) {
  return {
    total: projects.length,
    allocated: formatMoney(projects.reduce((sum, project) => sum + project.allocated, 0)),
    expenditure: formatMoney(projects.reduce((sum, project) => sum + project.spent, 0)),
    completed: projects.filter((project) => project.status === "Completed").length,
    pending: projects.filter((project) => project.status !== "Completed").length,
    elevated: projects.filter((project) => ["High", "Critical"].includes(project.level)).length,
    average: projects.length ? Math.round(projects.reduce((sum, project) => sum + project.risk, 0) / projects.length) : 0,
  };
}
function renderStats(stats) {
  const cards = [["Total projects", stats.total], ["Completed", stats.completed], ["Under monitoring", stats.pending], ["Elevated indicators", stats.elevated, "critical"], ["Total sanctioned", stats.allocated], ["Total expenditure", stats.expenditure]];
  document.querySelector("#summaryCards").innerHTML = cards.map(([label, value, kind]) => `<article class="stat-card ${kind || ""}"><small>${label}</small><b>${value}</b></article>`).join("");
}
function renderProjects(projects) {
  document.querySelector("#projectsTableBody").innerHTML = projects.map((p) => `<tr><td><span class="project-name">${escapeHtml(p.name)}</span><span class="project-id">${escapeHtml(p.id)}</span></td><td>${escapeHtml(p.location || "Location not recorded")}</td><td>${escapeHtml(p.category)}</td><td class="amount">${formatMoney(p.allocated)}</td><td class="amount">${formatMoney(p.spent)}</td><td><span class="badge ${statusClass(p.status)}">${escapeHtml(p.status)}</span></td><td><span class="badge ${riskClass(p.level)}">${escapeHtml(p.level)} · ${p.risk}/100</span></td><td class="project-actions"><button class="details-button" data-id="${escapeHtml(p.id)}">${p.status === "Completed" ? "Review completion" : "View progress"}</button><button class="details-button" data-analyze-id="${escapeHtml(p.databaseId)}">Run AI analysis</button></td></tr>`).join("");
  document.querySelector("#visibleCount").textContent = `${projects.length} project${projects.length === 1 ? "" : "s"} shown`;
  document.querySelector("#emptyState").hidden = projects.length !== 0;
}
function renderBarChart(selector, values) {
  const max = Math.max(1, ...values.map((item) => item.value));
  document.querySelector(selector).innerHTML = values.map((item) => `<div class="chart-line"><label>${escapeHtml(item.label)}</label><div class="chart-track"><div class="chart-fill" style="width:${(item.value / max) * 100}%"></div></div><b>${item.value}</b></div>`).join("");
}
function renderBudgetChart(projects) {
  const visible = projects.slice(0, 5);
  const max = Math.max(1, ...visible.map((p) => Math.max(p.allocated, p.spent)));
  document.querySelector("#budgetChart").innerHTML = visible.map((p) => `<div class="comparison-group" data-label="${escapeHtml(p.id.slice(-2))}"><i class="compare-bar" style="height:${(p.allocated / max) * 100}%"></i><i class="compare-bar actual" style="height:${(p.spent / max) * 100}%"></i></div>`).join("");
}
function renderFundOverview(overview) {
  const cards = [["Total released", formatMoney(overview.totalReleasedAmount)], ["Actual expenditure", formatMoney(overview.totalActualExpenditure)], ["Remaining amount", formatMoney(overview.totalRemainingAmount)], ["Overall utilization", formatPercent(overview.overallUtilizationPercentage)], ["Review suggested", overview.projectsRequiringReview ?? 0]];
  document.querySelector("#fundSummaryCards").innerHTML = cards.map(([label, value]) => `<article class="fund-stat-card"><small>${label}</small><b>${value}</b></article>`).join("");
  const states = overview.stateUtilization || [];
  document.querySelector("#stateUtilization").innerHTML = states.length ? states.map((state) => {
    const percentage = Math.max(0, Math.min(100, Number(state.utilizationPercentage) || 0));
    return `<div class="state-row"><div><b>${escapeHtml(state.state)}</b><small>${formatMoney(state.totalActualExpenditure)} spent of ${formatMoney(state.totalReleasedAmount)} released</small></div><div class="state-meter" role="progressbar" aria-valuenow="${percentage}" aria-valuemin="0" aria-valuemax="100" aria-label="${escapeHtml(state.state)} utilization"><i style="width:${percentage}%"></i></div><strong>${formatPercent(state.utilizationPercentage)}</strong></div>`;
  }).join("") : `<p class="empty-state">No state utilization data is available.</p>`;
  const reviews = overview.projectsRequiringReviewDetails || [];
  document.querySelector("#fundReviewList").innerHTML = reviews.length ? reviews.map((review) => `<article class="fund-review"><span class="badge fund-review-badge">Review suggested</span><div><b>${escapeHtml(review.projectType)}</b><small>${escapeHtml(review.projectCode)} · ${escapeHtml(review.state)}</small><p>${escapeHtml(review.reviewSignal)}</p><small>${escapeHtml(review.reviewReason)}</small></div><button class="details-button" data-id="${escapeHtml(review.projectCode)}">View</button></article>`).join("") : `<p class="empty-state">No projects currently require financial monitoring.</p>`;
  if (!overview.financialDataValid) { const notice = document.querySelector("#fundOverviewError"); notice.textContent = "Financial records need reconciliation; review the source data before relying on portfolio totals."; notice.hidden = false; }
}
function renderExpenditureTrend(trend) {
  if (!trend?.length) return `<p class="trend-empty">No historical expenditure updates are available.</p>`;
  const max = Math.max(1, ...trend.map((point) => Number(point.expenditure) || 0));
  return `<p class="section-intro">Dated expenditure entries recorded for this project.</p><div class="trend-chart" aria-label="Historical expenditure trend">${trend.map((point) => `<div class="trend-point"><i style="height:${Math.max(4, ((Number(point.expenditure) || 0) / max) * 100)}%"></i><span>${escapeHtml(new Date(`${point.updateDate}T00:00:00`).toLocaleDateString("en-IN", { month: "short", year: "2-digit" }))}</span><small>${formatMoney(point.expenditure)}</small></div>`).join("")}</div>`;
}
function renderProjectHistory(history) {
  if (!history?.length) return `<p class="trend-empty">No historical progress entries are available.</p>`;
  return `<ol class="history-timeline">${history.map((entry) => `<li><time>${escapeHtml(entry.updateDate ? new Date(`${entry.updateDate}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "Date not recorded")}</time><div><b>${escapeHtml(entry.status || "Progress update")}</b><p>${entry.completionPercentage == null ? "Completion not recorded" : `${formatPercent(entry.completionPercentage)} complete`} · ${entry.expenditure == null ? "Expenditure not recorded" : formatMoney(entry.expenditure)}${entry.delayDays ? ` · ${escapeHtml(entry.delayDays)} days delayed` : ""}${entry.indicatorScore == null ? "" : ` · Previous indicator: ${escapeHtml(entry.indicatorLevel || "AI signal")} · ${escapeHtml(entry.indicatorScore)}/100`}</p></div></li>`).join("")}</ol>`;
}
function populateCategories(projects) {
  const select = document.querySelector("#categoryFilter");
  [...new Set(projects.map((p) => p.category))].sort().forEach((category) => select.insertAdjacentHTML("beforeend", `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`));
}
function renderAnalysisResult(project) {
  if (!project.analysis) return `<p>AI analysis has not been run for this project in this session.</p><p class="ai-disclaimer">${AI_DISCLAIMER}</p>`;
  const result = project.analysis;
  const score = Number(result.anomaly_score);
  return `<h3>AI-assisted anomaly analysis</h3><p>Model output is a monitoring signal to consider with financial, historical and field evidence.</p><div class="detail-grid"><div><small>Anomaly status</small>${escapeHtml(result.anomaly_status || "Potential Anomaly")}</div><div><small>Anomaly score</small>${Number.isFinite(score) ? score.toFixed(2) : "Not available"} / 100</div><div><small>Risk level</small><span class="badge ${riskClass(result.risk_level)}">${escapeHtml(result.risk_level || "Requires human review")}</span></div><div><small>Model output</small>${escapeHtml(result.model_output || "No additional model explanation")}</div></div><p class="ai-disclaimer">${AI_DISCLAIMER}</p>`;
}
async function openProjectModal(project) {
  if (!project) return;
  document.querySelector("#modalTitle").textContent = project.name;
  document.querySelector("#modalBody").innerHTML = `<div class="detail-overview"><div class="detail-grid"><div><small>Project ID</small>${escapeHtml(project.id)}</div><div><small>District</small>${escapeHtml(project.district || "Not recorded")}</div><div><small>Constituency</small>${escapeHtml(project.constituency || "Not recorded")}</div><div><small>Project type</small>${escapeHtml(project.category)}</div><div><small>Project status · current</small><span class="badge ${statusClass(project.status)}">${escapeHtml(project.status)}</span></div><div><small>Current accepted progress</small>${formatPercent(project.completion)}</div><div><small>Current expenditure</small>${formatMoney(project.spent)}</div><div><small>Delay</small>${project.delayDays ? `${project.delayDays} days` : "No delay recorded"}</div><div><small>Delivery monitoring indicator</small><span class="badge ${riskClass(project.level)}">${escapeHtml(project.level)} · ${project.risk}/100</span></div></div><p class="section-intro">Current status follows the project’s persisted completion value. Field evidence below is the historical submission record.</p></div><section id="fundUtilizationDetail" class="fund-detail"><h3>Financial monitoring</h3><p>Loading project fund utilization…</p></section><section id="historicalDetail" class="fund-detail"><h3>Historical progress analysis</h3><p>Loading project history…</p></section><section id="evidenceDetail" class="fund-detail"><h3>Field evidence history</h3><p>Loading progress evidence…</p></section><section id="publicFeedbackDetail" class="fund-detail"><h3>Public feedback</h3><p>Loading ratings and comments…</p></section><section class="analysis-result" id="analysisResult">${renderAnalysisResult(project)}</section><button class="button button-small analysis-button" data-analyze-id="${escapeHtml(project.databaseId)}">Run AI-assisted analysis</button><section class="fund-detail authority-review"><h3>Authority decision</h3><p>AI provides evidence for review; the authorized reviewer records the decision.</p><form id="reviewForm" data-project="${escapeHtml(project.databaseId)}"><label>Review status<select name="reviewStatus"><option>Reviewed</option><option>Monitoring Required</option><option>Progress Update Required</option><option>Further Verification Required</option><option>Closed / No Further Action</option></select></label><label>Authority remark<textarea name="remark" required maxlength="1000" rows="3" placeholder="Record the review finding"></textarea></label><label>Required action (optional)<input name="requiredAction" maxlength="300" placeholder="Action requested from contractor"></label><button class="button button-small" type="submit">Save authority review</button><p id="reviewSubmitStatus" role="status"></p></form><div id="reviewHistory"><p class="trend-empty">Loading review history…</p></div></section>`;
  const modal = document.querySelector("#projectModal"); modal.hidden = false; document.body.classList.add("modal-open");
  const results = await Promise.allSettled([loadProjectFundUtilization(project.databaseId), loadProjectHistory(project.databaseId), loadProgressEvidence(project.databaseId), loadReviews(project.databaseId), loadPublicFeedback(project.databaseId)]);
  if (modal.hidden) return;
  const [fundResult, historyResult, evidenceResult, reviewsResult, feedbackResult] = results;
  if (fundResult.status === "fulfilled") {
    const fund = fundResult.value;
    document.querySelector("#fundUtilizationDetail").innerHTML = `<h3>Financial monitoring</h3><div class="detail-grid"><div><small>Sanctioned amount</small>${formatMoney(fund.sanctionedAmount)}</div><div><small>Released amount</small>${formatMoney(fund.releasedAmount)}</div><div><small>Actual expenditure</small>${formatMoney(fund.actualExpenditure)}</div><div><small>Remaining amount</small>${formatMoney(fund.remainingAmount)}</div><div><small>Utilization</small>${formatPercent(fund.utilizationPercentage)}</div><div><small>Financial monitoring signal</small><span class="badge fund-signal">${escapeHtml(fund.reviewSignal || "No signal")}</span></div></div><p class="fund-reason">${escapeHtml(fund.reviewReason || "No additional financial note.")}</p><h3>Historical expenditure trend</h3>${renderExpenditureTrend(fund.expenditureTrend)}`;
  } else document.querySelector("#fundUtilizationDetail").insertAdjacentHTML("beforeend", `<p class="analysis-error">${escapeHtml(fundResult.reason.message)}</p>`);
  if (historyResult.status === "fulfilled") document.querySelector("#historicalDetail").innerHTML = `<h3>Historical progress analysis</h3><p class="section-intro">Recorded changes in completion, expenditure and delay over time.</p>${renderProjectHistory(historyResult.value)}`;
  else document.querySelector("#historicalDetail").innerHTML = `<h3>Historical progress analysis</h3><p class="analysis-error">${escapeHtml(historyResult.reason.message)}</p>`;
  if (evidenceResult.status === "fulfilled") {
    const evidence = evidenceResult.value;
    document.querySelector("#evidenceDetail").innerHTML = `<h3>Historical field evidence</h3><p class="section-intro">Submissions remain in history. A location mismatch is not applied to current project state.</p>${evidence.length ? evidence.map((item) => `<article class="evidence-entry"><div class="detail-grid"><div><small>Submitted</small>${escapeHtml(item.submittedAt ? new Date(item.submittedAt).toLocaleString("en-IN") : "Not recorded")}</div><div><small>Contractor</small>${escapeHtml(item.contractorId || "Not recorded")}</div><div><small>Location status</small><span class="evidence-status">${escapeHtml(item.verificationStatus || "Verification Required")}</span></div><div><small>State effect</small>${item.acceptedForProjectState ? "Accepted when submitted" : "Historical only · not applied"}</div><div><small>Distance from project</small>${item.distanceMetres == null ? "Verification Required" : `${escapeHtml(item.distanceMetres)} metres`}</div><div><small>Progress reported</small>${item.completionPercentage == null ? "Not recorded" : formatPercent(item.completionPercentage)}</div><div><small>Expenditure reported</small>${item.expenditure == null ? "Not recorded" : formatMoney(item.expenditure)}</div><div><small>Photo evidence</small>${item.photoAvailable ? "Photo submitted" : "No photo attached"}</div></div><p class="fund-reason">Location checks provide a proximity signal; they do not establish photo authenticity.</p></article>`).join("") : `<p class="trend-empty">No progress evidence has been submitted.</p>`}`;
  } else document.querySelector("#evidenceDetail").innerHTML = `<h3>Field evidence and GPS verification</h3><p class="analysis-error">${escapeHtml(evidenceResult.reason.message)}</p>`;
  if (reviewsResult.status === "fulfilled") {
    const reviews = reviewsResult.value;
    document.querySelector("#reviewHistory").innerHTML = reviews.length ? `<h3>Review history · newest first</h3>${reviews.map((review) => `<article class="review-entry"><b>${escapeHtml(review.reviewStatus)}</b><time>${escapeHtml(review.reviewDate ? new Date(review.reviewDate).toLocaleString("en-IN") : "Date not recorded")}</time><p>${escapeHtml(review.remark)}</p>${review.requiredAction ? `<small>Required action: ${escapeHtml(review.requiredAction)}</small>` : ""}</article>`).join("")}` : `<p class="trend-empty">No authority reviews recorded yet.</p>`;
  } else document.querySelector("#reviewHistory").innerHTML = `<p class="analysis-error">${escapeHtml(reviewsResult.reason.message)}</p>`;
  if (feedbackResult.status === "fulfilled") {
    const entries = feedbackResult.value;
    const avg = entries.length ? entries.reduce((sum, item) => sum + item.rating, 0) / entries.length : null;
    document.querySelector("#publicFeedbackDetail").innerHTML = `<h3>Public feedback</h3><p>${avg == null ? "No ratings yet" : `Average rating: ${avg.toFixed(1)} / 5 · ${entries.length} entr${entries.length === 1 ? "y" : "ies"}`}</p>${entries.length ? entries.map((item) => `<article class="review-entry"><b>${"★".repeat(item.rating)}${"☆".repeat(5 - item.rating)} · ${item.rating}/5</b><time>${escapeHtml(item.createdAt ? new Date(item.createdAt).toLocaleString("en-IN") : "Date not recorded")}</time><p>${escapeHtml(item.comment)}</p></article>`).join("") : `<p class="trend-empty">No public feedback has been submitted.</p>`}`;
  } else document.querySelector("#publicFeedbackDetail").innerHTML = `<h3>Public feedback</h3><p class="analysis-error">${escapeHtml(feedbackResult.reason.message)}</p>`;
}
function closeModal() { const modal = document.querySelector("#projectModal"); if (modal) modal.hidden = true; document.body.classList.remove("modal-open"); }
function showDataState(message) { document.querySelector("#projectsTableBody").innerHTML = ""; document.querySelector("#visibleCount").textContent = message; const empty = document.querySelector("#emptyState"); empty.textContent = message; empty.hidden = false; }

document.addEventListener("DOMContentLoaded", async () => {
  if (!document.querySelector("#projectsTableBody")) return;
  try {
    const auth = await fetch(`${API_ROOT}/workspace-auth/authority/me`, { credentials: "include" });
    if (!auth.ok) { window.location.replace("authority-auth.html"); return; }
    authorityCsrfToken = (await auth.json()).csrfToken;
  } catch { window.location.replace("authority-auth.html"); return; }
  document.querySelector("#authorityLogout")?.addEventListener("click", async () => {
    try { await fetch(`${API_ROOT}/workspace-auth/logout`, { method: "POST", credentials: "include", headers: { "X-CSRF-Token": authorityCsrfToken } }); } catch { /* Session may already have expired. */ }
    window.location.replace("authority-auth.html");
  });
  closeModal(); showDataState("Loading project records…");
  let projects;
  try { projects = await loadProjects(); } catch (error) { showDataState("Unable to load project records. Ensure the backend is running and try again."); return; }
  const stats = calculateDashboardStats(projects); renderStats(stats);
  document.querySelector("#overallRiskScore").textContent = stats.average;
  document.querySelector("#anomalyCount").textContent = stats.elevated;
  document.querySelector("#gaugeValue").innerHTML = `${stats.average}<span>/100</span>`;
  document.querySelector(".risk-gauge").style.setProperty("--score", `${stats.average}%`);
  populateCategories(projects);
  renderBarChart("#statusChart", ["Completed", "In Progress", "Delayed", "Not Started"].map((label) => ({ label, value: projects.filter((p) => p.status === label).length })));
  renderBarChart("#riskChart", ["Low", "Medium", "High", "Critical"].map((label) => ({ label, value: projects.filter((p) => p.level === label).length })));
  renderBudgetChart(projects);
  try { renderFundOverview(await loadFundOverview()); } catch (error) { const notice = document.querySelector("#fundOverviewError"); notice.textContent = error.message; notice.hidden = false; }
  const applyFilters = () => {
    const term = document.querySelector("#projectSearch").value.toLowerCase().trim();
    const status = document.querySelector("#statusFilter").value;
    const level = document.querySelector("#riskFilter").value;
    const category = document.querySelector("#categoryFilter").value;
    renderProjects(projects.filter((p) => `${p.id} ${p.name} ${p.location}`.toLowerCase().includes(term) && (status === "all" || p.status === status) && (level === "all" || p.level === level) && (category === "all" || p.category === category)));
  };
  const refreshCurrentProjects = async () => {
    try {
      // The API is authoritative for saved analysis fields. Reusing an older
      // in-memory result here can resurrect an analysis cleared by a contractor update.
      projects = await loadProjects();
      const stats = calculateDashboardStats(projects);
      renderStats(stats);
      document.querySelector("#overallRiskScore").textContent = stats.average;
      document.querySelector("#anomalyCount").textContent = stats.elevated;
      document.querySelector("#gaugeValue").innerHTML = `${stats.average}<span>/100</span>`;
      document.querySelector(".risk-gauge").style.setProperty("--score", `${stats.average}%`);
      renderBarChart("#statusChart", ["Completed", "In Progress", "Delayed", "Not Started"].map((label) => ({ label, value: projects.filter((p) => p.status === label).length })));
      renderBarChart("#riskChart", ["Low", "Medium", "High", "Critical"].map((label) => ({ label, value: projects.filter((p) => p.level === label).length })));
      renderBudgetChart(projects);
      applyFilters();
    } catch { /* Keep the last successfully loaded dashboard state if the API is temporarily unavailable. */ }
  };
  let refreshPending = false;
  const refreshWhenVisible = () => {
    if (document.visibilityState !== "visible" || refreshPending) return;
    refreshPending = true;
    refreshCurrentProjects().finally(() => { refreshPending = false; });
  };
  window.addEventListener("focus", refreshWhenVisible);
  document.addEventListener("visibilitychange", refreshWhenVisible);
  ["projectSearch", "statusFilter", "riskFilter", "categoryFilter"].forEach((id) => document.querySelector(`#${id}`).addEventListener(id === "projectSearch" ? "input" : "change", applyFilters));
  renderProjects(projects);
  document.addEventListener("click", async (event) => {
    const target = event.target.closest("button, [data-close-modal]"); if (!target) return;
    if (target.matches("[data-close-modal]")) { closeModal(); return; }
    if (target.hasAttribute("data-id")) { openProjectModal(projects.find((p) => p.id === target.dataset.id)); return; }
    if (target.hasAttribute("data-analyze-id")) {
      const project = projects.find((p) => String(p.databaseId) === target.dataset.analyzeId); if (!project) return;
      target.disabled = true; const original = target.textContent; target.textContent = "Analyzing…";
      try { project.analysis = await analyzeProject(project.databaseId); const score = Number(project.analysis.anomaly_score); if (Number.isFinite(score)) project.risk = score; project.level = project.analysis.risk_level ? project.analysis.risk_level[0].toUpperCase() + project.analysis.risk_level.slice(1).toLowerCase() : project.level; renderProjects(projects); await openProjectModal(project); }
      catch (error) { const result = document.querySelector("#analysisResult"); if (result) result.innerHTML = `<p class="analysis-error">${escapeHtml(error.message)}</p>`; else window.alert(error.message); target.disabled = false; target.textContent = original; }
    }
  });
  document.addEventListener("keydown", (event) => { if (event.key === "Escape") closeModal(); });
  document.addEventListener("submit", async (event) => {
    if (event.target.id !== "reviewForm") return;
    event.preventDefault(); const form = event.target; const button = form.querySelector("button[type=submit]"); const status = document.querySelector("#reviewSubmitStatus");
    button.disabled = true; status.textContent = "Saving review…";
    try { const result = await getJson(`${PROJECTS_API_URL}/${encodeURIComponent(form.dataset.project)}/reviews`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(Object.fromEntries(new FormData(form))) }); status.textContent = result.notificationCreated ? "Review saved. The assigned contractor was notified." : "Review saved. No contractor notification was created because none is assigned."; form.reset(); const history = await loadReviews(form.dataset.project); document.querySelector("#reviewHistory").innerHTML = history.map((review) => `<article class="review-entry"><b>${escapeHtml(review.reviewStatus)}</b><time>${escapeHtml(review.reviewDate ? new Date(review.reviewDate).toLocaleString("en-IN") : "Date not recorded")}</time><p>${escapeHtml(review.remark)}</p>${review.requiredAction ? `<small>Required action: ${escapeHtml(review.requiredAction)}</small>` : ""}</article>`).join(""); }
    catch (error) { status.textContent = error.message; } finally { button.disabled = false; }
  });
});
