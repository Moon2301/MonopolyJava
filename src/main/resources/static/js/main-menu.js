window.onload = () => {
    const token = localStorage.getItem("token") || localStorage.getItem("authToken");
    const playerAvatar = document.getElementById("playerAvatar");
    const playerName = document.getElementById("playerName");
    const playerCoins = document.getElementById("playerCoins");
    const playerTickets = document.getElementById("playerTickets");
    const tournamentTitle = document.getElementById("tournamentTitle");
    const tournamentCountdown = document.getElementById("tournamentCountdown");
    const roomCodeInput = document.getElementById("roomCodeInput");
    const createRoomButton = document.getElementById("createRoomButton");
    const joinRoomButton = document.getElementById("joinRoomButton");
    const roomActionStatus = document.getElementById("roomActionStatus");
    const routeTargets = document.querySelectorAll("[data-route]");

    let countdownTimer = null;

    const setRoomStatus = (message, isError = false) => {
        if (!roomActionStatus) {
            return;
        }
        roomActionStatus.textContent = message;
        roomActionStatus.style.color = isError ? "#ffd7d7" : "#fff8ed";
    };

    const getHeaders = (includeJson = false) => {
        const headers = {};
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
        if (includeJson) {
            headers["Content-Type"] = "application/json";
        }
        return headers;
    };

    const handleUnauthorized = (response, shouldRedirect = false) => {
        if (response.status === 401 || response.status === 403) {
            if (shouldRedirect) {
                window.location.href = "/login";
            }
            return true;
        }
        return false;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const formatCountdown = (endsAt) => {
        const diffMs = new Date(endsAt).getTime() - Date.now();
        if (Number.isNaN(diffMs) || diffMs <= 0) {
            return "00D:00H:00M";
        }

        const totalMinutes = Math.floor(diffMs / 60000);
        const days = Math.floor(totalMinutes / (24 * 60));
        const hours = Math.floor((totalMinutes % (24 * 60)) / 60);
        const minutes = totalMinutes % 60;
        return `${String(days).padStart(2, "0")}D:${String(hours).padStart(2, "0")}H:${String(minutes).padStart(2, "0")}M`;
    };

    const startCountdown = (endsAt) => {
        if (!tournamentCountdown) {
            return;
        }
        if (countdownTimer) {
            window.clearInterval(countdownTimer);
        }

        const updateCountdown = () => {
            tournamentCountdown.textContent = formatCountdown(endsAt);
        };

        updateCountdown();
        countdownTimer = window.setInterval(updateCountdown, 1000);
    };

    const bindHomeSummary = (data) => {
        const player = data.player || {};
        const tournament = data.tournament || {};

        if (playerAvatar) {
            playerAvatar.src = player.avatarUrl || "/images/avatar-default.png";
        }
        if (playerName) {
            playerName.textContent = player.username || "Nguoi choi";
        }
        if (playerCoins) {
            playerCoins.textContent = formatNumber(player.coins);
        }
        if (playerTickets) {
            playerTickets.textContent = formatNumber(player.tickets);
        }
        if (tournamentTitle) {
            tournamentTitle.textContent = tournament.title || "Tournament";
        }
        if (tournament.endsAt) {
            startCountdown(tournament.endsAt);
        } else if (tournamentCountdown) {
            tournamentCountdown.textContent = "00D:00H:00M";
        }
    };

    const loadHomeSummary = async () => {
        if (!token) {
            window.location.href = "/login";
            return;
        }

        try {
            const response = await fetch("/api/home/summary", {
                headers: getHeaders()
            });

            if (handleUnauthorized(response, true)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai thong tin trang chu.");
            }

            const data = await response.json();
            bindHomeSummary(data);
        } catch (error) {
            console.error(error);
        }
    };

    const createRoom = async () => {
        createRoomButton.disabled = true;
        setRoomStatus("Dang tao phong...");

        try {
            const response = await fetch("/api/rooms", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({
                    name: "My Room",
                    visibility: "PUBLIC",
                    password: "",
                    mode: "QUICK_GAME",
                    maxPlayers: 4
                })
            });

            if (handleUnauthorized(response, false)) {
                throw new Error("Khong co quyen tao phong hoac token khong hop le.");
            }
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Tao phong that bai.");
            }

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            setRoomStatus(error.message || "Tao phong that bai.", true);
        } finally {
            createRoomButton.disabled = false;
        }
    };

    const joinRoom = async () => {
        const roomCode = roomCodeInput?.value?.trim().toUpperCase();
        if (!roomCode) {
            setRoomStatus("Vui long nhap room code.", true);
            return;
        }

        joinRoomButton.disabled = true;
        setRoomStatus("Dang tham gia phong...");

        try {
            const response = await fetch("/api/rooms/join", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({
                    roomCode,
                    password: ""
                })
            });

            if (handleUnauthorized(response, false)) {
                throw new Error("Khong co quyen tham gia phong hoac token khong hop le.");
            }
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Khong the tham gia phong.");
            }

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            setRoomStatus(error.message || "Khong the tham gia phong.", true);
        } finally {
            joinRoomButton.disabled = false;
        }
    };

    routeTargets.forEach((target) => {
        target.addEventListener("click", (event) => {
            const route = event.currentTarget.getAttribute("data-route");
            if (route) {
                window.location.href = route;
            }
        });
    });

    createRoomButton?.addEventListener("click", createRoom);
    joinRoomButton?.addEventListener("click", joinRoom);
    roomCodeInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            joinRoom();
        }
    });

    loadHomeSummary();
};
