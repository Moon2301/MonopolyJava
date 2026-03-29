window.onload = () => {
    const accountId = localStorage.getItem("accountId");
    const playerAvatar = document.getElementById("playerAvatar");
    const playerName = document.getElementById("playerName");
    const playerCoins = document.getElementById("playerCoins");
    const playerTickets = document.getElementById("playerTickets");
    const playerProfileBanner = document.getElementById("playerProfileBanner");
    const roomCodeInput = document.getElementById("roomCodeInput");
    const createRoomButton = document.getElementById("createRoomButton");
    const joinRoomButton = document.getElementById("joinRoomButton");
    const roomActionStatus = document.getElementById("roomActionStatus");
    const routeTargets = document.querySelectorAll("[data-route]");
    const friendBadge = document.getElementById("friendBadge");

    const setRoomStatus = (message, isError = false) => {
        if (!roomActionStatus) {
            return;
        }
        roomActionStatus.textContent = message;
        roomActionStatus.style.color = isError ? "#ffd7d7" : "#fff8ed";
    };

    const getHeaders = (includeJson = false) => {
        const headers = {};
        if (accountId) {
            headers["X-Account-Id"] = accountId;
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

    const bindHomeSummary = (data) => {
        const player = data.player || {};

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
    };

    const loadHomeSummary = async () => {
        if (!accountId) {
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

    /** Số lời mời kết bạn chờ chấp nhận (badge nút bạn bè). */
    const loadFriendBadge = async () => {
        if (!friendBadge || !accountId) {
            return;
        }
        try {
            const response = await fetch("/api/social/friends", { headers: getHeaders() });
            if (!response.ok) {
                return;
            }
            const list = await response.json();
            const pending = Array.isArray(list) ? list.filter((f) => f.canAccept === true).length : 0;
            if (pending > 0) {
                friendBadge.hidden = false;
                friendBadge.textContent = pending > 9 ? "9+" : String(pending);
            } else {
                friendBadge.hidden = true;
            }
        } catch (e) {
            console.warn(e);
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

    const openProfile = () => {
        window.location.href = "/profile";
    };
    playerProfileBanner?.addEventListener("click", openProfile);
    playerProfileBanner?.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openProfile();
        }
    });

    createRoomButton?.addEventListener("click", createRoom);
    joinRoomButton?.addEventListener("click", joinRoom);
    roomCodeInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            joinRoom();
        }
    });

    loadHomeSummary();
    loadFriendBadge();
};
