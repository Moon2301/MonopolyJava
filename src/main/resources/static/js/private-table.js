document.addEventListener("DOMContentLoaded", () => {
    const accountId = sessionStorage.getItem("accountId");
    const roomId = new URLSearchParams(window.location.search).get("roomId");

    const playerName = document.getElementById("playerName");
    const playerAvatar = document.getElementById("playerAvatar");
    const playerCoins = document.getElementById("playerCoins");
    const playerTickets = document.getElementById("playerTickets");
    const roomCode = document.getElementById("roomCode");
    const roomVisibilityIndicator = document.getElementById("roomVisibilityIndicator");
    const selectedHeroImage = document.getElementById("selectedHeroImage");
    const selectedHeroSvgHost = document.getElementById("selectedHeroSvgHost");
    const selectedHeroName = document.getElementById("selectedHeroName");
    const heroEmptyState = document.getElementById("heroEmptyState");
    const heroList = document.getElementById("heroList");
    const playerSlots = document.getElementById("playerSlots");
    const readyButton = document.getElementById("readyButton");
    const startButton = document.getElementById("startButton");
    const playerCount = document.getElementById("playerCount");
    const tableStatus = document.getElementById("tableStatus");

    let roomState = null;
    let heroes = [];
    let pollingHandle = null;
    let lastSelectedHeroUid = null;

    const escapeHtml = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

    const escapeAttr = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/"/g, "&quot;")
            .replace(/</g, "&lt;");

    const setStatus = (message, isError = false) => {
        if (!tableStatus) {
            return;
        }
        tableStatus.textContent = message;
        tableStatus.style.color = isError ? "#ffd7d7" : "#fff8eb";
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

    const handleUnauthorized = (response) => {
        if (response.status === 401 || response.status === 403) {
            setStatus("Phien dang nhap khong hop le. Vui long dang nhap lai.", true);
            window.setTimeout(() => {
                window.location.href = "/login";
            }, 1200);
            return true;
        }
        return false;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const getHeroById = (heroId) => heroes.find((hero) => hero.heroId === heroId) || null;

    const applyAvatar = (element, username, avatarUrl) => {
        if (!element) {
            return;
        }

        if (avatarUrl) {
            element.textContent = "";
            element.style.backgroundImage = `url('${avatarUrl}')`;
            element.style.backgroundSize = "cover";
            element.style.backgroundPosition = "center";
            return;
        }

        element.style.backgroundImage = "";
        element.textContent = (username || "P").slice(0, 2).toUpperCase();
    };

    const renderSelectedHero = () => {
        const hero = roomState?.currentPlayer?.selectedHero;

        if (selectedHeroName) {
            selectedHeroName.textContent = hero?.name || "No hero selected";
        }

        if (!selectedHeroImage || !heroEmptyState) {
            return;
        }

        if (window.HeroSystem && lastSelectedHeroUid) {
            window.HeroSystem.stopIdle(lastSelectedHeroUid);
            lastSelectedHeroUid = null;
        }

        if (selectedHeroSvgHost) {
            selectedHeroSvgHost.innerHTML = "";
            selectedHeroSvgHost.hidden = true;
        }

        if (hero?.imageUrl) {
            selectedHeroImage.src = hero.imageUrl;
            selectedHeroImage.style.display = "block";
            heroEmptyState.style.display = "none";
            selectedHeroImage.onerror = () => {
                selectedHeroImage.style.display = "none";
                if (hero?.name && window.HeroSystem && selectedHeroSvgHost) {
                    lastSelectedHeroUid = `sel-${hero.heroId}`;
                    selectedHeroSvgHost.innerHTML = window.HeroSystem.getSVG(hero.name, lastSelectedHeroUid);
                    selectedHeroSvgHost.hidden = false;
                    heroEmptyState.style.display = "none";
                    window.HeroSystem.startIdle(lastSelectedHeroUid, 0);
                } else {
                    heroEmptyState.style.display = "grid";
                }
            };
            return;
        }

        if (hero?.name && window.HeroSystem && selectedHeroSvgHost) {
            lastSelectedHeroUid = `sel-${hero.heroId}`;
            selectedHeroSvgHost.innerHTML = window.HeroSystem.getSVG(hero.name, lastSelectedHeroUid);
            selectedHeroSvgHost.hidden = false;
            selectedHeroImage.style.display = "none";
            heroEmptyState.style.display = "none";
            selectedHeroImage.removeAttribute("src");
            window.HeroSystem.startIdle(lastSelectedHeroUid, 0);
            return;
        }

        selectedHeroImage.removeAttribute("src");
        selectedHeroImage.style.display = "none";
        heroEmptyState.style.display = "grid";
    };

    const renderHeroList = () => {
        if (!heroList) {
            return;
        }

        if (!heroes.length) {
            heroList.innerHTML = "";
            return;
        }

        const currentHeroId = roomState?.currentPlayer?.selectedHero?.heroId;
        const useSvg = typeof window.HeroSystem !== "undefined";

        heroList.innerHTML = heroes
            .map((hero) => {
                const uid = `list-${hero.heroId}`;
                const svgBlock =
                    useSvg && hero.name
                        ? `<div class="hero-option-media hero-option-media--svg">${window.HeroSystem.getSVG(hero.name, uid)}</div>`
                        : `<div class="hero-option-media">
                    <img
                        src="${escapeAttr(hero.imageUrl || "")}"
                        alt="${escapeAttr(hero.name)}"
                        onerror="this.style.display='none';this.nextElementSibling.style.display='grid';"
                    >
                    <div class="hero-option-fallback" style="display:none;">${escapeHtml(hero.name)}</div>
                </div>`;
                return `
            <button
                type="button"
                class="hero-option ${currentHeroId === hero.heroId ? "active" : ""}"
                data-action="select-hero"
                data-hero-id="${hero.heroId}"
            >
                ${svgBlock}
                <div class="hero-option-name">${escapeHtml(hero.name)}</div>
            </button>
        `;
            })
            .join("");

        if (useSvg) {
            window.requestAnimationFrame(() => {
                heroes.forEach((hero, idx) => {
                    window.HeroSystem.startIdle(`list-${hero.heroId}`, idx * 0.1);
                });
            });
        }
    };

    const resolveSlotHero = (player) => {
        if (!player?.selectedHeroId) {
            return null;
        }

        if (roomState?.currentPlayer?.playerId === player.playerId && roomState?.currentPlayer?.selectedHero) {
            return roomState.currentPlayer.selectedHero;
        }

        return getHeroById(player.selectedHeroId);
    };

    const renderOccupiedSlot = (player) => {
        const hero = resolveSlotHero(player);
        const heroName = hero?.name || "No hero selected";
        const heroImageUrl = hero?.imageUrl || "";
        let readyText;
        let readyClass;
        if (player.isHost) {
            readyText = "Chủ phòng";
            readyClass = "ready-host";
        } else {
            readyText = player.isReady ? "READY" : "NOT READY";
            readyClass = player.isReady ? "ready-on" : "ready-off";
        }
        const avatarMarkup = player.avatarUrl
            ? `<img class="slot-avatar-image" src="${player.avatarUrl}" alt="${player.username}">`
            : `<span>${(player.username || "P").slice(0, 2).toUpperCase()}</span>`;

        return `
            <article class="slot-card occupied combined-card">
                <div class="slot-card-header">
                    <div class="slot-host-avatar">${avatarMarkup}</div>
                    <div class="slot-meta">
                        <strong>${player.username}</strong>
                        <span>${player.isHost ? "Host" : "Player"}</span>
                    </div>
                </div>
                <div class="slot-hero-media ${window.HeroSystem && hero?.name && !heroImageUrl ? "slot-hero-media--svg" : ""}">
                    ${
                        heroImageUrl
                            ? `
                            <img
                                class="slot-hero-image"
                                src="${escapeAttr(heroImageUrl)}"
                                alt="${escapeAttr(heroName)}"
                                onerror="this.style.display='none';this.nextElementSibling.style.display='grid';"
                            >
                            <div class="slot-hero-placeholder" style="display:none;">${escapeHtml(heroName)}</div>
                        `
                            : window.HeroSystem && hero?.name
                              ? `<div class="slot-hero-svg-wrap">${window.HeroSystem.getSVG(hero.name, `slot-hero-${player.playerId}`)}</div>`
                              : `<div class="slot-hero-placeholder">${escapeHtml(heroName)}</div>`
                    }
                </div>
                <div class="slot-hero-name">${heroName}</div>
                <div class="slot-ready ${readyClass}">${readyText}</div>
            </article>
        `;
    };

    const renderPlayerSlots = () => {
        if (!playerSlots || !roomState?.room) {
            return;
        }

        const playersBySlot = new Map((roomState.players || []).map((player) => [player.slotIndex, player]));
        const maxPlayers = roomState.room.maxPlayers || 4;
        const slotMarkup = [];

        for (let slotIndex = 1; slotIndex <= maxPlayers; slotIndex += 1) {
            const player = playersBySlot.get(slotIndex);
            if (player) {
                slotMarkup.push(renderOccupiedSlot(player));
            } else {
                slotMarkup.push(`
                    <article class="slot-card empty">
                        <button type="button" class="add-slot-button" data-action="add-player">+</button>
                    </article>
                `);
            }
        }

        playerSlots.innerHTML = slotMarkup.join("");
        if (window.HeroSystem && playerSlots) {
            window.requestAnimationFrame(() => {
                (roomState?.players || []).forEach((pl, idx) => {
                    window.HeroSystem.startIdle(`slot-hero-${pl.playerId}`, idx * 0.07);
                });
            });
        }
    };

    const renderRoom = () => {
        const currentPlayer = roomState?.currentPlayer || {};
        const currentPlayerState = (roomState?.players || []).find((player) => player.playerId === currentPlayer.playerId);

        if (playerName) {
            playerName.textContent = currentPlayer.username || "Nguoi choi";
        }
        applyAvatar(playerAvatar, currentPlayer.username, currentPlayer.avatarUrl);
        if (playerCoins) {
            playerCoins.textContent = formatNumber(currentPlayer.coins);
        }
        if (playerTickets) {
            playerTickets.textContent = currentPlayer.tickets ?? 0;
        }
        if (roomCode) {
            const currentRoomId = roomState?.room?.roomId ?? "-";
            const currentRoomCode = roomState?.room?.roomCode ?? "-";
            roomCode.textContent = `#${currentRoomId} | ${currentRoomCode}`;
        }
        if (roomVisibilityIndicator) {
            roomVisibilityIndicator.textContent = roomState?.room?.visibility === "PRIVATE" ? "🔒" : "🌐";
        }
        if (readyButton) {
            readyButton.textContent = currentPlayerState?.isReady ? "UNREADY" : "READY";
            if (startButton) {
                const isHost = currentPlayerState?.isHost;
                const allPlayersReady = roomState?.players?.length >= 2 && roomState?.players?.every(p => Boolean(p.isReady));
                if (isHost && allPlayersReady) {
                    readyButton.style.display = "none";
                    startButton.style.display = "inline-block";
                } else {
                    readyButton.style.display = "inline-block";
                    startButton.style.display = "none";
                }
            }
        }
        if (playerCount) {
            playerCount.textContent = `${roomState?.players?.length || 0}/${roomState?.room?.maxPlayers || 4} players`;
        }

        renderSelectedHero();
        renderHeroList();
        renderPlayerSlots();
        setStatus(`Phong ${roomState?.room?.name || ""}`);
    };

    const loadHeroes = async () => {
        try {
            const response = await fetch("/api/heroes/owned", {
                headers: getHeaders()
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                heroes = [];
                return;
            }

            heroes = await response.json();
        } catch (error) {
            console.error(error);
            heroes = [];
        }
    };

    const loadRoom = async () => {
        if (!accountId) {
            setStatus("Ban chua dang nhap.", true);
            return;
        }
        if (!roomId) {
            setStatus("Thieu roomId tren URL.", true);
            return;
        }

        try {
            const response = await fetch(`/api/rooms/${roomId}?t=${Date.now()}`, {
                headers: { ...getHeaders(), 'Cache-Control': 'no-store' }
            });

            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai thong tin phong.");
            }

            roomState = await response.json();
            
            if (roomState?.room?.status === "IN_GAME" || roomState?.room?.status === "STARTING") {
                if (roomState.room.gameId) {
                    window.location.href = "/game-board?gameId=" + roomState.room.gameId;
                    return;
                }
            }

            renderRoom();
        } catch (error) {
            const netFail =
                error instanceof TypeError &&
                (String(error.message).includes("fetch") || String(error.message).includes("Failed to fetch"));
            const msg = netFail
                ? "Khong ket noi duoc server. Hay chay Spring Boot (vd: port 8080)."
                : error.message || "Khong the tai thong tin phong.";
            setStatus(msg, true);
        }
    };

    const postHeroSelection = async (heroId) => {
        const response = await fetch(`/api/rooms/${roomId}/hero`, {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({ heroId: Number(heroId) })
        });

        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            throw new Error("Khong the chon hero.");
        }

        await loadRoom();
    };

    const postReady = async () => {
        const currentPlayer = roomState?.currentPlayer;
        const currentPlayerState = (roomState?.players || []).find((player) => player.playerId === currentPlayer?.playerId);
        const nextReadyValue = !currentPlayerState?.isReady;

        const response = await fetch(`/api/rooms/${roomId}/ready`, {
            method: "POST",
            headers: getHeaders(true),
            body: JSON.stringify({ ready: nextReadyValue })
        });

        if (handleUnauthorized(response)) {
            return;
        }
        if (!response.ok) {
            const data = await response.json().catch(()=>({}));
            throw new Error(data.message || data.error || "Khong the cap nhat trang thai san sang.");
        }

        await loadRoom();
    };

    const inviteModal = document.getElementById("inviteModal");
    const inviteUsernameInput = document.getElementById("inviteUsernameInput");
    const cancelInviteBtn = document.getElementById("cancelInviteBtn");
    const confirmInviteBtn = document.getElementById("confirmInviteBtn");
    const inviteError = document.getElementById("inviteError");
    const friendsInviteList = document.getElementById("friendsInviteList");
    const closeInviteModalBtn = document.getElementById("closeInviteModalBtn");

    const closeInviteModal = () => {
        if (inviteModal) {
            inviteModal.style.display = "none";
            if (inviteUsernameInput) inviteUsernameInput.value = "";
            if (inviteError) inviteError.style.display = "none";
        }
    };

    cancelInviteBtn?.addEventListener("click", closeInviteModal);
    closeInviteModalBtn?.addEventListener("click", closeInviteModal);

    const loadFriendsForInvite = async () => {
        try {
            if (friendsInviteList) {
                friendsInviteList.innerHTML = '<div style="color: #888; font-style: italic; text-align: center;">Đang tải danh sách...</div>';
            }
            const res = await fetch("/api/friends?t=" + Date.now(), { headers: { ...getHeaders(), 'Cache-Control': 'no-store' } });
            if (!res.ok) throw new Error("Khong the tai ban be");
            const data = await res.json();
            if (friendsInviteList) {
                if (data.friends && data.friends.length > 0) {
                    friendsInviteList.innerHTML = data.friends.map(f => `
                        <div style="display: flex; justify-content: space-between; align-items: center; background: #1a1a2e; padding: 10px 16px; border-radius: 8px;">
                            <span style="font-weight: 500;">${f.username}</span>
                            <button type="button" onclick="window.inviteFriendToRoom('${f.username}')" style="background: #3b82f6; border: none; color: white; padding: 6px 16px; border-radius: 6px; cursor: pointer; font-family: Outfit; font-weight: 600;">Mời</button>
                        </div>
                    `).join('');
                } else {
                    friendsInviteList.innerHTML = '<div style="color: #888; font-style: italic; text-align: center;">Bạn chưa có người bạn nào</div>';
                }
            }
        } catch (e) {
            console.error(e);
            if (friendsInviteList) friendsInviteList.innerHTML = '<div style="color: #ff5252; text-align: center;">Lỗi tải danh sách bạn bè</div>';
        }
    };

    window.inviteFriendToRoom = async (username) => {
        try {
            if (inviteError) inviteError.style.display = "none";
            const response = await fetch(`/api/rooms/${roomId}/invite`, {
                method: "POST", headers: getHeaders(true), body: JSON.stringify({ username })
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || data.error || "Lỗi gửi lời mời");
            
            alert(data.message || "Đã gửi lời mời thành công!");
            closeInviteModal();
        } catch (error) {
            if (inviteError) {
                inviteError.textContent = error.message;
                inviteError.style.display = "block";
            }
        }
    };

    cancelInviteBtn?.addEventListener("click", closeInviteModal);

    confirmInviteBtn?.addEventListener("click", async () => {
        const username = inviteUsernameInput ? inviteUsernameInput.value.trim() : "";
        if (!username) {
            inviteError.textContent = "Vui lòng nhập tên người chơi.";
            inviteError.style.display = "block";
            return;
        }

        try {
            confirmInviteBtn.disabled = true;
            confirmInviteBtn.textContent = "...";
            const response = await fetch(`/api/rooms/${roomId}/invite`, {
                method: "POST",
                headers: getHeaders(true),
                body: JSON.stringify({ username })
            });

            if (handleUnauthorized(response)) return;

            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                throw new Error(err.error || err.message || "Không thể gửi lời mời.");
            }

            const data = await response.json();
            alert(data.message || "Đã gửi lời mời thành công!");
            closeInviteModal();
        } catch (error) {
            console.error(error);
            inviteError.textContent = error.message;
            inviteError.style.display = "block";
        } finally {
            if (confirmInviteBtn) {
                confirmInviteBtn.disabled = false;
                confirmInviteBtn.textContent = "Gửi lời mời";
            }
        }
    });

    document.addEventListener("click", async (event) => {
        const target = event.target.closest("[data-action]");
        if (!target) {
            return;
        }

        const action = target.getAttribute("data-action");

        try {
            if (action === "invite" || action === "add-player") {
                if (inviteModal) {
                    if (inviteError) inviteError.style.display = "none";
                    inviteModal.style.display = "flex";
                    if (inviteUsernameInput) {
                        inviteUsernameInput.value = "";
                        inviteUsernameInput.focus();
                    }
                    loadFriendsForInvite();
                }
                return;
            }
            if (action === "select-hero") {
                const heroId = target.getAttribute("data-hero-id");
                await postHeroSelection(heroId);
            }

            if (action === "ready") {
                await postReady();
            }

            if (action === "start") {
                if (startButton) startButton.disabled = true;
                const response = await fetch(`/api/rooms/${roomId}/start`, {
                    method: "POST",
                    headers: getHeaders(false)
                });
                if (handleUnauthorized(response)) return;
                if (!response.ok) {
                    if (startButton) startButton.disabled = false;
                    const err = await response.json().catch(() => ({}));
                    setStatus(err.message || "Không thể bắt đầu game.", true);
                    return;
                }
                const data = await response.json();
                window.location.href = data.redirectUrl;
            }

            if (action === "copy-code" && roomCode?.textContent && navigator.clipboard) {
                await navigator.clipboard.writeText(roomCode.textContent);
                setStatus("Da sao chep ma phong.");
            }

            if (action === "home") {
                window.location.href = "/home";
            }

            if (action === "shop") {
                window.location.href = "/shop";
            }

            if (action === "settings") {
                window.location.href = "/settings";
            }

            if (action === "menu") {
                window.location.href = "/lobby";
            }

            if (action === "close") {
                window.location.href = "/login";
            }
        } catch (error) {
            console.error(error);
            setStatus(error.message || "Thao tac that bai.", true);
        }
    });

    Promise.all([loadHeroes(), loadRoom()]);
    pollingHandle = window.setInterval(loadRoom, 2000);
    window.addEventListener("beforeunload", () => {
        if (pollingHandle) {
            window.clearInterval(pollingHandle);
        }
    });
});
