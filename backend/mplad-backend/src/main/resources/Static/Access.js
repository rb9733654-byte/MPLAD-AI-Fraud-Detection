// =========================================
// GET ALL CONTINUE BUTTONS
// =========================================

const continueButtons =
    document.querySelectorAll(".continue-button");
const frontendBase = `${window.location.protocol}//${window.location.hostname}:5500/`;

function openRolePage(role) {
    try { localStorage.setItem("selectedRole", role); } catch { /* Navigation still works when storage is disabled. */ }
    const destinations = {
        authority: "Login.html",
        contractor: "contractor-auth.html",
        public: "public-auth.html"
    };
    const page = destinations[String(role).toLowerCase()] || "Login.html";
    window.location.assign(new URL(page, frontendBase).href);
}


// =========================================
// HANDLE ROLE SELECTION
// =========================================

continueButtons.forEach(function(button) {

    button.addEventListener("click", function(event) {

        // Prevent the card click from firing twice
        event.stopPropagation();

        openRolePage(button.getAttribute("data-role"));

    });

});


// =========================================
// CARD CLICK
// =========================================

const cards =
    document.querySelectorAll(".access-card");


cards.forEach(function(card) {

    card.addEventListener("click", function() {

        openRolePage(card.querySelector(".continue-button").getAttribute("data-role"));

    });

});
