document.addEventListener("DOMContentLoaded", () => {
    const accountId = localStorage.getItem("accountId");
    const difficultyButtons = document.querySelectorAll(".difficulty-button");
    const startButton = document.getElementById("startAiGameButton");
    const backButton = document.getElementById("backButton");
    const statusLine = document.getElementById("aiStatus");
    const botCountSelect = document.getElementById("botCountSelect");

    let selectedDifficulty = "easy";

    const setStatus = (message, isError = false) => {
        if (!statusLine) return;
        statusLine.textContent = message;
        statusLine.style.color = isError ? "#ffd2d2" : "rgba(247, 248, 255, 0.8)";
    };

    difficultyButtons.forEach((button) => {
        button.addEventListener("click", () => {
            difficultyButtons.forEach((item) => item.classList.remove("active"));
            button.classList.add("active");
            selectedDifficulty = button.getAttribute("data-difficulty") || "easy";
            setStatus(`Đã chọn AI ${selectedDifficulty === "hard" ? "Khó" : "Dễ"}.`);
        });
    });

    backButton?.addEventListener("click", () => {
        window.location.href = "/home";
    });

    startButton?.addEventListener("click", async () => {
        if (!accountId) {
            setStatus("Bạn chưa đăng nhập.", true);
            window.setTimeout(() => {
                window.location.href = "/login";
            }, 1000);
            return;
        }
        startButton.disabled = true;
        setStatus("Đang tạo trận với máy...");
        try {
            const botCount = Math.min(3, Math.max(1, parseInt(botCountSelect?.value || "1", 10) || 1));
            const response = await fetch("/api/gameplay/bot/start", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-Account-Id": accountId
                },
                body: JSON.stringify({ difficulty: selectedDifficulty, botCount })
            });
            if (!response.ok) {
                throw new Error("Không thể tạo trận với máy");
            }
            const data = await response.json();
            localStorage.setItem("aiDifficulty", selectedDifficulty);
            localStorage.setItem("aiBotCount", String(data.botCount ?? botCount));
            window.location.href =
                data.redirectUrl ||
                `/game-board?gameId=${data.gameId}&vsBot=1&difficulty=${selectedDifficulty}&bots=${data.botCount ?? botCount}`;
        } catch (error) {
            setStatus(error.message || "Tạo trận thất bại.", true);
            startButton.disabled = false;
        }
    });
});
