const PUBLIC_API_BASE = `${window.location.protocol}//${window.location.hostname}:8080/api/public`;
const publicList = document.querySelector("#publicProjectList");
const filtersForm = document.querySelector("#publicFilters");
let publicProjects = [];
let currentUser = null;

async function publicRequest(path, options = {}) {
  let response;
  try { response = await fetch(`${PUBLIC_API_BASE}${path}`, { credentials: "include", ...options }); }
  catch { throw new Error("The project service is unavailable. Please try again shortly."); }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (response.status === 401) { window.location.replace("public-auth.html"); throw new Error("Please log in to continue."); }
  if (!response.ok) throw new Error(data?.message || data?.detail || data?.title || "The request could not be completed.");
  return data;
}
function safeText(node, value) { node.textContent = value ?? ""; }
function optionList(name, values) {
  const select = filtersForm.elements[name]; const selected = select.value;
  for (const option of [...select.options].slice(1)) option.remove();
  [...new Set(values.filter(Boolean))].sort((a, b) => a.localeCompare(b)).forEach((value) => { const option = document.createElement("option"); option.value = value; option.textContent = value; select.append(option); });
  select.value = selected;
}
function renderProjects(projects) {
  publicList.replaceChildren();
  projects.forEach((project) => {
    const article = document.createElement("article"); article.className = "public-project-card";
    const id = document.createElement("p"); id.className = "eyebrow"; id.textContent = project.projectId;
    const heading = document.createElement("h3"); heading.textContent = project.projectType || "MPLAD Project";
    const location = document.createElement("p"); location.textContent = [project.constituency, project.district, project.state].filter(Boolean).join(" · ");
    const button = document.createElement("button"); button.type = "button"; button.className = "details-button"; button.textContent = "View details and feedback"; button.addEventListener("click", () => showProject(project.id));
    article.append(id, heading, location, button); publicList.append(article);
  });
  document.querySelector("#publicCount").textContent = `${projects.length} project${projects.length === 1 ? "" : "s"}`;
  document.querySelector("#publicEmpty").hidden = projects.length > 0;
}
function queryString() {
  const params = new URLSearchParams();
  for (const key of ["search", "state", "district", "constituency", "projectType"]) { const value = filtersForm.elements[key].value.trim(); if (value) params.set(key, value); }
  return params.toString();
}
async function loadProjects() {
  document.querySelector("#publicError").hidden = true;
  try {
    const params = queryString(); const records = await publicRequest(`/projects${params ? `?${params}` : ""}`);
    publicProjects = records; renderProjects(records);
    optionList("state", records.map((p) => p.state)); optionList("district", records.map((p) => p.district)); optionList("constituency", records.map((p) => p.constituency)); optionList("projectType", records.map((p) => p.projectType));
  } catch (error) { const box = document.querySelector("#publicError"); box.textContent = error.message; box.hidden = false; }
}
async function showProject(id) {
  const detail = document.querySelector("#publicProjectDetail"); detail.hidden = false; detail.replaceChildren();
  try {
    const [project, feedback] = await Promise.all([publicRequest(`/projects/${id}`), publicRequest(`/projects/${id}/feedback`)]);
    const heading = document.createElement("h2"); heading.textContent = project.projectType || "MPLAD Project";
    const sub = document.createElement("p"); sub.className = "eyebrow"; sub.textContent = project.projectId;
    const facts = document.createElement("dl"); facts.className = "public-detail-facts";
    [["State", project.state], ["District", project.district], ["Constituency", project.constituency], ["Project type", project.projectType], ["Public location", project.publicLocation]].forEach(([label, value]) => { const term = document.createElement("dt"); term.textContent = label; const desc = document.createElement("dd"); desc.textContent = value || "Not available"; facts.append(term, desc); });
    const feedbackHeading = document.createElement("h3"); feedbackHeading.textContent = "Public feedback";
    const feedbackList = document.createElement("ul"); feedbackList.className = "public-feedback-list";
    feedback.forEach((item) => { const li = document.createElement("li"); li.textContent = `${"★".repeat(item.rating)}${"☆".repeat(5 - item.rating)} · ${item.comment}`; feedbackList.append(li); });
    if (!feedback.length) { const empty = document.createElement("p"); empty.textContent = "No feedback has been submitted yet."; feedbackList.append(empty); }
    const form = document.createElement("form"); form.className = "feedback-form";
    const formTitle = document.createElement("h3"); formTitle.textContent = "Share your feedback";
    const ratingLabel = document.createElement("label"); ratingLabel.textContent = "Rating"; const rating = document.createElement("select"); rating.name = "rating"; rating.required = true;
    for (let n = 1; n <= 5; n++) { const opt = document.createElement("option"); opt.value = String(n); opt.textContent = `${n} star${n > 1 ? "s" : ""}`; rating.append(opt); } ratingLabel.append(rating);
    const commentLabel = document.createElement("label"); commentLabel.textContent = "Comment"; const comment = document.createElement("textarea"); comment.name = "comment"; comment.maxLength = 1000; comment.required = true; comment.rows = 4; commentLabel.append(comment);
    const submit = document.createElement("button"); submit.type = "submit"; submit.className = "button"; submit.textContent = "Submit feedback";
    const message = document.createElement("p"); message.className = "auth-message"; message.hidden = true;
    form.append(formTitle, ratingLabel, commentLabel, submit, message);
    form.addEventListener("submit", async (event) => {
      event.preventDefault(); message.hidden = true;
      try {
        const result = await publicRequest(`/projects/${id}/feedback`, { method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-Token": currentUser.csrfToken }, body: JSON.stringify({ rating: Number(rating.value), comment: comment.value }) });
        const li = document.createElement("li"); li.textContent = `${"★".repeat(result.rating)}${"☆".repeat(5 - result.rating)} · ${result.comment}`; feedbackList.prepend(li); comment.value = ""; message.textContent = "Feedback submitted."; message.hidden = false;
      } catch (error) { message.textContent = error.message; message.hidden = false; }
    });
    const close = document.createElement("button"); close.type = "button"; close.className = "button button-secondary"; close.textContent = "Close details"; close.addEventListener("click", () => { detail.hidden = true; });
    detail.append(sub, heading, facts, feedbackHeading, feedbackList, form, close); detail.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) { const message = document.createElement("p"); message.className = "access-error"; message.textContent = error.message; detail.append(message); }
}
filtersForm.addEventListener("input", () => { window.clearTimeout(filtersForm.searchTimer); filtersForm.searchTimer = window.setTimeout(loadProjects, 250); });
filtersForm.addEventListener("change", loadProjects);
filtersForm.addEventListener("reset", () => window.setTimeout(loadProjects, 0));
document.querySelector("#publicLogout").addEventListener("click", async () => {
  try { await publicRequest("/auth/logout", { method: "POST", headers: { "X-CSRF-Token": currentUser.csrfToken } }); } catch { /* Continue to the access page even if the server session already expired. */ }
  window.location.replace("public-auth.html");
});
(async function initializePublicPortal() {
  try {
    currentUser = await publicRequest("/auth/me"); safeText(document.querySelector("#signedInAs"), currentUser.email);
    const all = await publicRequest("/projects");
    optionList("state", all.map((p) => p.state)); optionList("district", all.map((p) => p.district)); optionList("constituency", all.map((p) => p.constituency)); optionList("projectType", all.map((p) => p.projectType));
    await loadProjects();
  } catch (error) { if (error.message !== "Please log in to continue.") { const box = document.querySelector("#publicError"); box.textContent = error.message; box.hidden = false; } }
})();
