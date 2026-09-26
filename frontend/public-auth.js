const PUBLIC_API = `${window.location.protocol}//${window.location.hostname}:8080/api/public/auth`;
const publicWorkspaceUrl = new URL("public.html", window.location.href).href;
const $ = (selector) => document.querySelector(selector);
const loginTab = $("#loginTab"), registerTab = $("#registerTab");
const panels = { login: $("#loginPanel"), register: $("#registerPanel"), otp: $("#otpPanel") };
let pendingEmail = "";

function showPanel(name) {
  Object.entries(panels).forEach(([key, panel]) => { panel.hidden = key !== name; });
  const loginSelected = name === "login";
  loginTab.classList.toggle("active", loginSelected); registerTab.classList.toggle("active", name === "register");
  loginTab.setAttribute("aria-selected", String(loginSelected)); registerTab.setAttribute("aria-selected", String(name === "register"));
  if (name === "login") $("#loginForm [name=email]").focus();
  if (name === "register") $("#registerForm [name=email]").focus();
}
function setError(selector, message) { const node = $(selector); node.textContent = message; node.hidden = !message; }
function showDemoCode(result) {
  const notice = $("#otpMessage");
  notice.textContent = `${result.message || "Demo verification code generated."} Code: ${result.demoCode || "Unavailable"}`;
  notice.hidden = false;
}
async function api(path, body) {
  let response;
  try {
    response = await fetch(`${PUBLIC_API}${path}`, { method: body ? "POST" : "GET", credentials: "include", headers: body ? { "Content-Type": "application/json" } : {}, body: body ? JSON.stringify(body) : undefined });
  } catch { throw new Error("The backend is unavailable. Start the Spring Boot service and try again."); }
  const payload = response.status === 204 ? {} : await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message || payload.detail || payload.title || "The request could not be completed.");
  return payload;
}
loginTab.addEventListener("click", () => showPanel("login"));
registerTab.addEventListener("click", () => showPanel("register"));
$("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault(); setError("#loginError", "");
  const form = new FormData(event.currentTarget); const email = String(form.get("email")).trim(); const password = String(form.get("password"));
  if (!email || !event.currentTarget.elements.email.validity.valid || !password) return setError("#loginError", "Enter a valid email and password.");
  try {
    const account = await api("/login", { email, password });
    if (!account.id || account.email?.toLowerCase() !== email.toLowerCase()) throw new Error("Could not confirm the public account session.");
    window.location.assign(publicWorkspaceUrl);
  }
  catch (error) { setError("#loginError", error.message); }
});
$("#registerForm").addEventListener("submit", async (event) => {
  event.preventDefault(); setError("#registerError", "");
  const form = new FormData(event.currentTarget); const email = String(form.get("email")).trim(); const password = String(form.get("password")); const confirmPassword = String(form.get("confirmPassword"));
  if (!email || !event.currentTarget.elements.email.validity.valid) return setError("#registerError", "Enter a valid email address.");
  if (password.length < 8) return setError("#registerError", "Password must be at least 8 characters.");
  if (password !== confirmPassword) return setError("#registerError", "Passwords do not match.");
  try { const result = await api("/register", { email, password, confirmPassword }); pendingEmail = email; showDemoCode(result); showPanel("otp"); }
  catch (error) { setError("#registerError", error.message); }
});
$("#otpForm").addEventListener("submit", async (event) => {
  event.preventDefault(); setError("#otpError", "");
  const otp = String(new FormData(event.currentTarget).get("otp")).trim();
  if (!/^\d{6}$/.test(otp)) return setError("#otpError", "Enter the 6-digit code.");
  try { await api("/verify-otp", { email: pendingEmail, otp }); showPanel("login"); $("#loginForm [name=email]").value = pendingEmail; $("#loginSuccess").textContent = "Account verified. You can now log in."; $("#loginSuccess").hidden = false; setError("#loginError", ""); }
  catch (error) { setError("#otpError", error.message); }
});
$("#resendOtp").addEventListener("click", async () => {
  setError("#otpError", "");
  try { const result = await api("/resend-otp", { email: pendingEmail }); showDemoCode(result); }
  catch (error) { setError("#otpError", error.message); }
});
$("#backToLogin").addEventListener("click", () => showPanel("login"));
showPanel("login");
