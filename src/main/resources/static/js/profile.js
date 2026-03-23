document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".stat-card").forEach((card) => {
        card.addEventListener("mouseenter", () => {
            card.style.transform = "translateY(-2px)";
            card.style.transition = "transform 0.2s ease";
        });
        card.addEventListener("mouseleave", () => {
            card.style.transform = "translateY(0)";
        });
    });
});
