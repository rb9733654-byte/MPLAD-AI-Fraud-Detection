/* Demo-data adapter: replace these functions with Spring Boot REST API calls later. */
const demoProjects = [
  {
    id: "MPL-2601",
    name: "Solar Street Lighting Network",
    location: "Sundargarh, Odisha",
    category: "Infrastructure",
    allocated: 48,
    spent: 41,
    status: "In Progress",
    risk: 24,
    level: "Low",
  },
  {
    id: "MPL-2602",
    name: "Community Health Centre Upgrade",
    location: "Nandurbar, Maharashtra",
    category: "Health",
    allocated: 85,
    spent: 79,
    status: "Delayed",
    risk: 78,
    level: "High",
  },
  {
    id: "MPL-2603",
    name: "Girls’ Hostel Renovation",
    location: "Purnia, Bihar",
    category: "Education",
    allocated: 62,
    spent: 62,
    status: "Completed",
    risk: 18,
    level: "Low",
  },
  {
    id: "MPL-2604",
    name: "Rural Drinking Water Scheme",
    location: "Barmer, Rajasthan",
    category: "Water & Sanitation",
    allocated: 110,
    spent: 108,
    status: "In Progress",
    risk: 67,
    level: "High",
  },
  {
    id: "MPL-2605",
    name: "Public Library and Learning Centre",
    location: "Karimganj, Assam",
    category: "Education",
    allocated: 32,
    spent: 9,
    status: "Not Started",
    risk: 52,
    level: "Medium",
  },
  {
    id: "MPL-2606",
    name: "Primary Road Resurfacing",
    location: "Chitradurga, Karnataka",
    category: "Infrastructure",
    allocated: 96,
    spent: 121,
    status: "Delayed",
    risk: 92,
    level: "Critical",
  },
  {
    id: "MPL-2607",
    name: "Sports Ground Development",
    location: "Dindigul, Tamil Nadu",
    category: "Sports",
    allocated: 27,
    spent: 25,
    status: "Completed",
    risk: 31,
    level: "Low",
  },
  {
    id: "MPL-2608",
    name: "Anganwadi Centre Modernisation",
    location: "Banda, Uttar Pradesh",
    category: "Health",
    allocated: 38,
    spent: 30,
    status: "In Progress",
    risk: 45,
    level: "Medium",
  },
];
const demoAlerts = [
  {
    title: "Expenditure exceeds allocation",
    text: "MPL-2606 shows sample expenditure above its approved allocation. Review supporting records.",
    level: "high",
  },
  {
    title: "Delivery delay indicator",
    text: "MPL-2602 is marked delayed in this demo dataset and warrants a progress review.",
    level: "high",
  },
  {
    title: "Unusual spending pattern",
    text: "MPL-2604 illustrates a variance pattern that a future model could surface for review.",
    level: "medium",
  },
  {
    title: "Repeated vendor pattern",
    text: "A fictional pattern alert for demonstration only; no real contractor or project is implicated.",
    level: "medium",
  },
];
async function fetchProjects() {
  return demoProjects;
}
async function fetchDashboardStats() {
  return {
    total: 48,
    allocated: "₹ 24.8 Cr",
    completed: 19,
    pending: 11,
    suspicious: 8,
    average: 62,
  };
}
async function fetchRiskAnalysis() {
  return { score: 62, anomalies: 8, level: "Moderate" };
}
const PROJECTS_API_URL = "http://localhost:8080/api/projects";

async function analyzeProject(projectId) {
  const response = await fetch(`${PROJECTS_API_URL}/${encodeURIComponent(projectId)}/analyze`, {
    method: "POST",
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.message || "AI analysis could not be completed.");
  }
  return payload;
}

async function loadProjects() {
  const response = await fetch(PROJECTS_API_URL);
  if (!response.ok) throw new Error("Project records could not be loaded.");

  const projects = await response.json();
  if (!Array.isArray(projects)) throw new Error("Project records could not be loaded.");

  return projects.map(toDashboardProject);
}
function toDashboardProject(project) {
  const allocated = Number(project.sanctionedAmount) || 0;
  const spent = Number(project.actualExpenditure) || 0;
  const completion = Number(project.completionPercentage) || 0;
  const delayDays = Number(project.delayDays) || 0;
  const risk = Math.min(
    100,
    Math.round(
      (Math.max(0, spent - allocated) / Math.max(allocated, 1)) * 35 +
        Math.min(delayDays * 2, 45) +
        (project.isCompleted ? 0 : completion < 25 ? 20 : completion < 60 ? 10 : 0),
    ),
  );
  const level = risk >= 75 ? "Critical" : risk >= 55 ? "High" : risk >= 30 ? "Medium" : "Low";
  const status = project.isCompleted
    ? "Completed"
    : delayDays > 0
      ? "Delayed"
      : completion === 0
        ? "Not Started"
        : "In Progress";

  return {
    ...project,
    databaseId: project.id,
    id: project.projectId || String(project.id),
    name: project.projectType || "MPLAD Project",
    location: [project.district, project.constituency].filter(Boolean).join(", "),
    category: project.projectType || "Uncategorised",
    allocated,
    spent,
    status,
    risk,
    level,
  };
}
function calculateDashboardStats(projects) {
  const allocated = projects.reduce((total, project) => total + project.allocated, 0);
  const suspicious = projects.filter((project) => ["High", "Critical"].includes(project.level)).length;
  return {
    total: projects.length,
    allocated: formatMoney(allocated),
    completed: projects.filter((project) => project.status === "Completed").length,
    pending: projects.filter((project) => project.status !== "Completed").length,
    suspicious,
    average: projects.length
      ? Math.round(projects.reduce((total, project) => total + project.risk, 0) / projects.length)
      : 0,
  };
}
const riskClass = (level) => `risk-${level.toLowerCase()}`;
const statusClass = (status) =>
  `status-${status.toLowerCase().replaceAll(" ", "-")}`;
function formatMoney(value) {
  return `₹ ${value.toFixed(1)} L`;
}
function renderStats(stats) {
  const cards = [
    ["Total Projects", stats.total],
    ["Allocated Amount", stats.allocated],
    ["Completed Projects", stats.completed],
    ["Pending Projects", stats.pending],
    ["Suspicious Projects", stats.suspicious, "critical"],
    ["Average Risk Score", `${stats.average}/100`],
  ];
  document.querySelector("#summaryCards").innerHTML = cards
    .map(
      ([label, value, kind]) =>
        `<article class="stat-card ${kind || ""}"><small>${label}</small><b>${value}</b></article>`,
    )
    .join("");
}
function renderProjects(projects) {
  const body = document.querySelector("#projectsTableBody");
  const empty = document.querySelector("#emptyState");
  body.innerHTML = projects
    .map(
      (p) =>
        `<tr><td><span class="project-name">${p.name}</span><span class="project-id">${p.id}</span></td><td>${p.location}</td><td>${p.category}</td><td class="amount">${formatMoney(p.allocated)}</td><td class="amount">${formatMoney(p.spent)}</td><td><span class="badge ${statusClass(p.status)}">${p.status}</span></td><td><span class="badge ${riskClass(p.level)}">${p.level} · ${p.risk}</span></td><td class="project-actions"><button class="details-button" data-id="${p.id}">Details</button><button class="details-button" data-analyze-id="${p.databaseId}">Analyze risk</button></td></tr>`,
    )
    .join("");
  document.querySelector("#visibleCount").textContent =
    `${projects.length} project${projects.length === 1 ? "" : "s"} shown`;
  empty.hidden = projects.length !== 0;
}
function renderAlerts() {
  document.querySelector("#alertsList").innerHTML = demoAlerts
    .map(
      (a, index) =>
        `<article class="alert-item ${a.level}"><div><h3>${a.title}</h3><p>${a.text}</p></div><button data-alert="${index}">Mark reviewed</button></article>`,
    )
    .join("");
}
function renderBarChart(id, values) {
  const max = Math.max(1, ...values.map((v) => v.value));
  document.querySelector(id).innerHTML = values
    .map(
      (v) =>
        `<div class="chart-line"><label>${v.label}</label><div class="chart-track"><div class="chart-fill" style="width:${(v.value / max) * 100}%"></div></div><b>${v.value}</b></div>`,
    )
    .join("");
}
function renderBudgetChart(projects) {
  const max = Math.max(1, ...projects.map((p) => Math.max(p.allocated, p.spent)));
  document.querySelector("#budgetChart").innerHTML = projects
    .slice(0, 5)
    .map(
      (p) =>
        `<div class="comparison-group" data-label="${p.id.slice(-2)}"><i class="compare-bar" style="height:${(p.allocated / max) * 100}%"></i><i class="compare-bar actual" style="height:${(p.spent / max) * 100}%"></i></div>`,
    )
    .join("");
}
function populateCategories(projects) {
  const select = document.querySelector("#categoryFilter");
  [...new Set(projects.map((p) => p.category))]
    .sort()
    .forEach((c) =>
      select.insertAdjacentHTML("beforeend", `<option>${c}</option>`),
    );
}
function openProjectModal(project) {
  if (!project) return;
  document.querySelector("#modalTitle").textContent = project.name;
  document.querySelector("#modalBody").innerHTML =
    `<p>AI results are review signals only and do not make fraud determinations.</p><div class="detail-grid"><div><small>Project ID</small>${project.id}</div><div><small>Location</small>${project.location}</div><div><small>Financial position</small>${formatMoney(project.spent)} of ${formatMoney(project.allocated)}</div><div><small>Current risk signal</small><span class="badge ${riskClass(project.level)}">${project.level} · ${project.risk}/100</span></div></div><section class="analysis-result" id="analysisResult">${renderAnalysisResult(project)}</section><button class="details-button analysis-button" data-analyze-id="${project.databaseId}">Analyze risk with AI</button>`;
  const modal = document.querySelector("#projectModal");
  modal.hidden = false;
  document.body.classList.add("modal-open");
}
function renderAnalysisResult(project) {
  if (!project.analysis) {
    return "<p>No AI analysis has been run for this project in this session.</p>";
  }
  const result = project.analysis;
  return `<div class="detail-grid"><div><small>Anomaly status</small>${result.anomaly_status}</div><div><small>Anomaly score</small>${Number(result.anomaly_score).toFixed(2)} / 100</div><div><small>Risk level</small><span class="badge ${riskClass(result.risk_level)}">${result.risk_level}</span></div><div><small>Model output</small>${result.model_output}</div></div>`;
}
function closeModal() {
  const modal = document.querySelector("#projectModal");
  if (!modal) return;
  modal.hidden = true;
  document.body.classList.remove("modal-open");
}
function showDataState(message) {
  document.querySelector("#projectsTableBody").innerHTML = "";
  document.querySelector("#visibleCount").textContent = message;
  const empty = document.querySelector("#emptyState");
  empty.textContent = message;
  empty.hidden = false;
}
document.addEventListener("DOMContentLoaded", async () => {
  if (!document.querySelector("#projectsTableBody")) return;
  closeModal();
  showDataState("Loading project records…");
  let projects;
  try {
    projects = await loadProjects();
  } catch (error) {
    showDataState("Unable to load project records. Please ensure the backend is running and try again.");
    return;
  }
  const stats = calculateDashboardStats(projects);
  const risk = { score: stats.average, anomalies: stats.suspicious };
  renderStats(stats);
  document.querySelector("#overallRiskScore").textContent = risk.score;
  document.querySelector("#anomalyCount").textContent = risk.anomalies;
  populateCategories(projects);
  renderAlerts();
  renderBarChart(
    "#statusChart",
    ["Completed", "In Progress", "Delayed", "Not Started"].map((label) => ({
      label,
      value: projects.filter((p) => p.status === label).length,
    })),
  );
  renderBarChart(
    "#riskChart",
    ["Low", "Medium", "High", "Critical"].map((label) => ({
      label,
      value: projects.filter((p) => p.level === label).length,
    })),
  );
  renderBudgetChart(projects);
  const applyFilters = () => {
    const term = document
        .querySelector("#projectSearch")
        .value.toLowerCase()
        .trim(),
      status = document.querySelector("#statusFilter").value,
      riskLevel = document.querySelector("#riskFilter").value,
      category = document.querySelector("#categoryFilter").value;
    renderProjects(
      projects.filter(
        (p) =>
          `${p.id} ${p.name} ${p.location}`.toLowerCase().includes(term) &&
          (status === "all" || p.status === status) &&
          (riskLevel === "all" || p.level === riskLevel) &&
          (category === "all" || p.category === category),
      ),
    );
  };
  ["projectSearch", "statusFilter", "riskFilter", "categoryFilter"].forEach(
    (id) =>
      document
        .querySelector(`#${id}`)
        .addEventListener(
          id === "projectSearch" ? "input" : "change",
          applyFilters,
        ),
  );
  renderProjects(projects);
  document.addEventListener("click", async (event) => {
    const detail = event.target.closest("[data-id]");
    if (detail)
      openProjectModal(projects.find((p) => p.id === detail.dataset.id));
    const analyze = event.target.closest("[data-analyze-id]");
    if (analyze) {
      const project = projects.find((p) => String(p.databaseId) === analyze.dataset.analyzeId);
      if (!project) return;
      analyze.disabled = true;
      analyze.textContent = "Analyzing…";
      try {
        const result = await analyzeProject(project.databaseId);
        project.analysis = result;
        project.risk = Number(result.anomaly_score);
        project.level = result.risk_level.charAt(0) + result.risk_level.slice(1).toLowerCase();
        renderProjects(projects);
        openProjectModal(project);
      } catch (error) {
        const container = document.querySelector("#analysisResult");
        if (container) container.innerHTML = `<p class="analysis-error">${error.message}</p>`;
        analyze.disabled = false;
        analyze.textContent = "Analyze risk";
      }
    }
    if (event.target.closest("[data-close-modal]")) closeModal();
    const alertButton = event.target.closest("[data-alert]");
    if (alertButton) {
      alertButton.textContent = "Reviewed";
      alertButton.disabled = true;
    }
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeModal();
  });
});
