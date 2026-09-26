const passwordInput = document.getElementById("password");
const togglePassword = document.getElementById("togglePassword");
const loginCard = document.querySelector(".login-card");
const signupCard = document.getElementById("signupCard");
const loginMessage = document.getElementById("loginMessage");
const signupMessage = document.getElementById("signupMessage");
const apiRoot = `${window.location.protocol}//${window.location.hostname}:8080/api/workspace-auth`;
const selectedRole = localStorage.getItem("selectedRole") || "Authority";
// Access.html routes the Authority choice here; authentication creates an authority session.
const authRole = "authority";

async function authRequest(action, payload) {
    let response;
    try {
        response = await fetch(`${apiRoot}/${authRole}/${action}`, {
            method: "POST", credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
    } catch {
        throw new Error("Could not reach the backend. Start Spring Boot and try again.");
    }
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || result.detail || result.title || "Request failed.");
    return result;
}

function showSignup(show) {
    loginCard.hidden = show;
    signupCard.hidden = !show;
    loginMessage.hidden = true;
    signupMessage.hidden = true;
}
document.getElementById("showSignup").addEventListener("click", (event) => { event.preventDefault(); showSignup(true); });
document.getElementById("showLogin").addEventListener("click", (event) => { event.preventDefault(); showSignup(false); });

togglePassword.addEventListener("click", function () {
    const visible = passwordInput.type === "password";
    passwordInput.type = visible ? "text" : "password";
    togglePassword.innerHTML = visible ? '<i class="fa-regular fa-eye-slash"></i>' : '<i class="fa-regular fa-eye"></i>';
});

document.getElementById("signupForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const username = String(data.get("username")).trim();
    const email = String(data.get("email")).trim();
    const password = String(data.get("password"));
    const confirmPassword = String(data.get("confirmPassword"));
    signupMessage.hidden = true;
    if (password.length < 8) { signupMessage.textContent = "Password must be at least 8 characters."; signupMessage.hidden = false; return; }
    if (password !== confirmPassword) { signupMessage.textContent = "Passwords do not match."; signupMessage.hidden = false; return; }
    try {
        await authRequest("register", { identity: email, username, password, confirmPassword });
        document.getElementById("username").value = username;
        document.getElementById("email").value = email;
        showSignup(false);
        const message = document.createElement("p");
        message.textContent = "Account created. Log in with your email and password.";
        message.setAttribute("role", "status");
        message.style.cssText = "color:#087f5b;font-size:13px;margin:0 0 14px";
        document.getElementById("loginForm").prepend(message);
    } catch (error) { signupMessage.textContent = error.message; signupMessage.hidden = false; }
});

document.getElementById("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const error = loginMessage;
    const email = document.getElementById("email").value.trim();
    const password = passwordInput.value;
    error.hidden = true;
    try {
        await authRequest("login", { identity: email, password });
        // index.html confirms the active session and offers the dashboard as the next step.
        window.location.assign("index.html");
    } catch (cause) { error.textContent = cause.message; error.hidden = false; }
});

// Retain the selected role through the requested Access -> Login -> index -> dashboard flow.
document.body.dataset.selectedRole = selectedRole;
