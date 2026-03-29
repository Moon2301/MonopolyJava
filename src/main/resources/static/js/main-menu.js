window.onload = () => {
    const accountId = sessionStorage.getItem("accountId");
    const homeMenu = document.querySelector(".home-menu");
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
                if (route === "/login") {
                    sessionStorage.removeItem("accountId");
                }
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

    // --- FRIENDS FEATURE ---
    const friendBadge = document.getElementById("friendBadge");
    const openFriendsBtn = document.getElementById("openFriendsBtn");
    const closeFriendsBtn = document.getElementById("closeFriendsBtn");
    const friendsModal = document.getElementById("friendsModal");
    const addFriendInput = document.getElementById("addFriendInput");
    const sendFriendRequestBtn = document.getElementById("sendFriendRequestBtn");
    const friendActionStatus = document.getElementById("friendActionStatus");
    const pendingRequestsList = document.getElementById("pendingRequestsList");
    const friendsList = document.getElementById("friendsList");

    const setFriendStatus = (msg, isErr = false) => {
        if (!friendActionStatus) return;
        friendActionStatus.textContent = msg;
        friendActionStatus.style.color = isErr ? "#ff5252" : "#67b738";
        friendActionStatus.style.display = "block";
        setTimeout(() => { if (friendActionStatus.textContent === msg) friendActionStatus.style.display = "none"; }, 3000);
    };

    const loadFriends = async () => {
        try {
            const response = await fetch("/api/friends", { headers: getHeaders() });
            if (!response.ok) throw new Error("Lỗi tải danh sách bạn bè");
            const data = await response.json();
            
            if (pendingRequestsList) {
                if (data.pendingRequests && data.pendingRequests.length > 0) {
                    pendingRequestsList.innerHTML = data.pendingRequests.map(req => `
                        <div style="display: flex; justify-content: space-between; align-items: center; background: #1a1a2e; padding: 10px 16px; border-radius: 8px;">
                            <span style="font-weight: 500;">${req.username}</span>
                            <div style="display: flex; gap: 8px;">
                                <button type="button" onclick="window.acceptFriend(${req.friendshipId})" style="background: #3b82f6; border: none; color: white; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-family: Outfit;">Đồng ý</button>
                                <button type="button" onclick="window.declineFriend(${req.friendshipId})" style="background: transparent; border: 1px solid #ea4335; color: #ea4335; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-family: Outfit;">Từ chối</button>
                            </div>
                        </div>
                    `).join('');
                } else {
                    pendingRequestsList.innerHTML = '<div style="color: #666; font-style: italic;">Không có lời mời nào</div>';
                }
            }

            if (friendsList) {
                if (data.friends && data.friends.length > 0) {
                    friendsList.innerHTML = data.friends.map(f => `
                        <div style="display: flex; justify-content: space-between; align-items: center; background: #1a1a2e; padding: 10px 16px; border-radius: 8px;">
                            <span style="font-weight: 500;">${f.username}</span>
                            <button type="button" onclick="window.declineFriend(${f.friendshipId})" style="background: transparent; border: 1px solid #ea4335; color: #ea4335; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-family: Outfit;">Xóa bạn</button>
                        </div>
                    `).join('');
                } else {
                    friendsList.innerHTML = '<div style="color: #666; font-style: italic;">Chưa có bạn bè</div>';
                }
            }
        } catch (error) {
            console.error(error);
        }
    };

    window.acceptFriend = async (id) => {
        try {
            await fetch(`/api/friends/accept/${id}`, { method: "POST", headers: getHeaders() });
            loadFriends();
        } catch(e) {}
    };

    window.declineFriend = async (id) => {
        try {
            await fetch(`/api/friends/decline/${id}`, { method: "POST", headers: getHeaders() });
            loadFriends();
        } catch(e) {}
    };

    openFriendsBtn?.addEventListener("click", () => {
        if (friendsModal) {
            friendsModal.style.display = "flex";
            loadFriends();
        }
    });

    closeFriendsBtn?.addEventListener("click", () => {
        if (friendsModal) friendsModal.style.display = "none";
    });

    sendFriendRequestBtn?.addEventListener("click", async () => {
        const username = addFriendInput?.value?.trim();
        if (!username) {
            setFriendStatus("Vui lòng nhập tên người dùng", true);
            return;
        }
        sendFriendRequestBtn.disabled = true;
        try {
            const response = await fetch("/api/friends/request", {
                method: "POST", headers: getHeaders(true), body: JSON.stringify({ username })
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || data.error || "Lỗi gửi yêu cầu");
            
            setFriendStatus(data.message || "Đã gửi lời mời", false);
            if (addFriendInput) addFriendInput.value = "";
            loadFriends();
            updateFriendBadge();
        } catch (error) {
            setFriendStatus(error.message, true);
        } finally {
            sendFriendRequestBtn.disabled = false;
        }
    });

    const updateFriendBadge = async () => {
        try {
            const res = await fetch("/api/friends/pending-count?t=" + Date.now(), { headers: { ...getHeaders(), "Cache-Control": "no-cache" } });
            if (!res.ok) return;
            const data = await res.json();
            if (friendBadge) {
                if (data.count > 0) {
                    friendBadge.textContent = data.count > 9 ? "9+" : data.count;
                    friendBadge.style.display = "flex";
                } else {
                    friendBadge.style.display = "none";
                }
            }
        } catch (e) {}
    };

    updateFriendBadge();
    setInterval(updateFriendBadge, 5000);

    // --- ROOM INVITE FEATURE ---
    const roomInviteModal = document.getElementById("roomInviteModal");
    const inviterNameDisplay = document.getElementById("inviterNameDisplay");
    const rejectRoomInviteBtn = document.getElementById("rejectRoomInviteBtn");
    const acceptRoomInviteBtn = document.getElementById("acceptRoomInviteBtn");
    
    let currentRoomInvite = null;

    const pollRoomInvites = async () => {
        if (roomInviteModal && roomInviteModal.style.display === "flex") return;
        try {
            const res = await fetch("/api/rooms/invitations/pending?t=" + Date.now(), { headers: { ...getHeaders(), "Cache-Control": "no-cache" } });
            if (!res.ok) return;
            const data = await res.json();
            if (data && data.length > 0) {
                currentRoomInvite = data[0]; 
                if (inviterNameDisplay) inviterNameDisplay.textContent = currentRoomInvite.inviterName;
                if (roomInviteModal) roomInviteModal.style.display = "flex";
            }
        } catch (e) {}
    };

    setInterval(pollRoomInvites, 2000);

    rejectRoomInviteBtn?.addEventListener("click", async () => {
        if (!currentRoomInvite) return;
        try {
            await fetch(`/api/rooms/invitations/${currentRoomInvite.id}`, { method: "DELETE", headers: getHeaders() });
        } catch(e) {}
        if (roomInviteModal) roomInviteModal.style.display = "none";
        currentRoomInvite = null;
    });

    acceptRoomInviteBtn?.addEventListener("click", async () => {
        if (!currentRoomInvite) return;
        acceptRoomInviteBtn.disabled = true;
        acceptRoomInviteBtn.textContent = "Đang vào...";
        try {
            const response = await fetch("/api/rooms/join", {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({ roomCode: currentRoomInvite.roomCode, password: "" })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Khong the tham gia phong.");
            }

            await fetch(`/api/rooms/invitations/${currentRoomInvite.id}`, { method: "DELETE", headers: getHeaders() });

            const data = await response.json();
            window.location.href = data.redirectUrl;
        } catch (error) {
            console.error(error);
            alert(error.message);
            if (roomInviteModal) roomInviteModal.style.display = "none";
        } finally {
            acceptRoomInviteBtn.disabled = false;
            acceptRoomInviteBtn.textContent = "Vào phòng ngay";
            currentRoomInvite = null;
        }
    });

    loadHomeSummary();
};
