document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".shop-card button").forEach((button) => {
        button.addEventListener("click", () => button.blur());
    });
});
