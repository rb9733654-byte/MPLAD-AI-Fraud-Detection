const AUTH_API = `${window.location.protocol}//${window.location.hostname}:8080/api/workspace-auth`;
const role = window.location.pathname.includes("contractor-auth") ? "contractor" : "authority";
const frontendPage = (page) => new URL(page, window.location.href).href;
const loginTab = document.querySelector("#loginTab"), registerTab = document.querySelector("#registerTab");
const loginPanel = document.querySelector("#loginPanel"), registerPanel = document.querySelector("#registerPanel");
function showMode(register) { loginPanel.hidden = register; registerPanel.hidden = !register; loginTab.classList.toggle("active", !register); registerTab.classList.toggle("active", register); loginTab.setAttribute("aria-selected", String(!register)); registerTab.setAttribute("aria-selected", String(register)); }
async function send(path, payload) {
  let response;
  try { response = await fetch(`${AUTH_API}/${role}/${path}`, { method: "POST", credentials: "include", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }); }
  catch { throw new Error("The backend is unavailable. Start Spring Boot and try again."); }
  const result = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(result.message || result.detail || result.title || "The request could not be completed.");
  return result;
}
loginTab.addEventListener("click", () => showMode(false));
registerTab.addEventListener("click", () => showMode(true));
document.querySelector("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault(); const form = new FormData(event.currentTarget); const identity = String(form.get("identity")).trim(); const password = String(form.get("password"));
  const error = document.querySelector("#loginError"); error.hidden = true;
  if (!identity || !password) { error.textContent = "Enter your account ID/email and password."; error.hidden = false; return; }
  try {
    const account = await send("login", { identity, password });
    if (account.role !== role.toUpperCase()) throw new Error("This account does not have access to the selected workspace.");
    window.location.assign(frontendPage(role === "authority" ? "dashboard.html" : "contractor.html"));
  }
  catch (cause) { error.textContent = cause.message; error.hidden = false; }
});
document.querySelector("#registerForm").addEventListener("submit", async (event) => {
  event.preventDefault(); const form = new FormData(event.currentTarget); const identity = String(form.get("identity")).trim(); const password = String(form.get("password")); const confirmPassword = String(form.get("confirmPassword"));
  const error = document.querySelector("#registerError"), message = document.querySelector("#registerMessage"); error.hidden = true; message.hidden = true;
  if (role === "authority" && !event.currentTarget.elements.identity.validity.valid) { error.textContent = "Enter a valid email address."; error.hidden = false; return; }
  if (password.length < 8) { error.textContent = "Password must be at least 8 characters."; error.hidden = false; return; }
  if (password !== confirmPassword) { error.textContent = "Passwords do not match."; error.hidden = false; return; }
  try { const result = await send("register", { identity, password, confirmPassword }); message.textContent = result.message; message.hidden = false; }
  catch (cause) { error.textContent = cause.message; error.hidden = false; }
});
showMode(false);
