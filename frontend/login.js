const accessForm = document.querySelector("#accessForm");
const accessError = document.querySelector("#accessError");
const authApi = `${window.location.protocol}//${window.location.hostname}:8080/api/workspace-auth`;

// After login, keep index.html visible and let the user continue to the dashboard.
fetch(`${authApi}/authority/me`, { credentials: "include" })
  .then((response) => {
    if (!response.ok) return;
    document.querySelector(".access-intro h1").innerHTML = "Login complete<br /><em>Welcome back.</em>";
    document.querySelector(".access-intro > p:not(.eyebrow)").textContent = "You are signed in to the authority workspace.";
    document.querySelector("#accessTitle").textContent = "Authority workspace";
    document.querySelector(".access-card-heading > p:last-child").textContent = "Continue to your project dashboard.";
    accessForm.hidden = true;
    document.querySelector(".prototype-note").textContent = "Your session is active. Select Continue to dashboard when you are ready.";
    const continueLink = document.createElement("a");
    continueLink.className = "button access-submit";
    continueLink.href = "dashboard.html";
    continueLink.textContent = "Continue to dashboard";
    document.querySelector(".access-card").append(continueLink);
  })
  .catch(() => {});

accessForm.addEventListener("change", () => { accessError.hidden = true; accessError.textContent = ""; });
accessForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const selectedRole = accessForm.querySelector('input[name="role"]:checked');
  if (!selectedRole) {
    accessError.textContent = "Select Authority, Contractor, or Public access to continue.";
    accessError.hidden = false;
    accessForm.querySelector('input[name="role"]').focus();
    return;
  }
  const destinations = { authority: "authority-auth.html", contractor: "contractor-auth.html", public: "public-auth.html" };
  window.location.assign(destinations[selectedRole.value]);
});