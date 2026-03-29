document.addEventListener("DOMContentLoaded", async () => {
    const accountId = localStorage.getItem("accountId");
    const urlParams = new URLSearchParams(window.location.search);
    const gameId = urlParams.get("gameId");
    const layoutMapId = urlParams.get("mapId") || "1";
    const isLiveGame = Boolean(gameId);
    const boardElement = document.getElementById("monopolyBoard");
    const tokensElement = document.getElementById("playerTokens");
    const chatMessagesElement = document.getElementById("chatMessages");
    const chatForm = document.getElementById("chatForm");
    const chatInput = document.getElementById("chatInput");
    const die1 = document.getElementById("die1");
    const die2 = document.getElementById("die2");
    const diceTotal = document.getElementById("diceTotal");
    const rollDiceButton = document.getElementById("rollDiceButton");
    const endTurnButton = document.getElementById("endTurnButton");
    const buyPropertyButton = document.getElementById("buyPropertyButton");
    const upgradePropertyButton = document.getElementById("upgradePropertyButton");
    const gameActionStatus = document.getElementById("gameActionStatus");
    const currentCellName = document.getElementById("currentCellName");
    const currentCellPriceTax = document.getElementById("currentCellPriceTax");
    const currentCellUpgrade = document.getElementById("currentCellUpgrade");
    const currentCellOwnerBlock = document.getElementById("currentCellOwnerBlock");
    const turnTimerLabel = document.getElementById("turnTimerLabel");
    const turnBannerCurrent = document.getElementById("turnBannerCurrent");
    const turnBannerNext = document.getElementById("turnBannerNext");
    const diceArea = document.getElementById("diceArea");
    const yourTurnToast = document.getElementById("yourTurnToast");
    const turnTopBanner = document.getElementById("turnTopBanner");
    const moneyFxLayer = document.getElementById("moneyFxLayer");
    const boardStageEl = document.querySelector(".board-stage");

    let turnTimerInterval = null;
    let lastLiveState = null;
    let diceRollingTimer = null;
    let yourTurnHideTimer = null;
    let yourTurnHideTimer2 = null;
    let prevMyTurnCouldRoll = false;
    let tokenStepAnimRunning = false;
    let tokenStepAnimGen = 0;
    /** Đỏ, xanh dương, vàng, xanh lá — theo turnOrder 1..4 */
    const PLAYER_PALETTE = ["#e53935", "#1e88e5", "#fdd835", "#43a047"];
    let stateApplyChain = Promise.resolve();
    /** null = hiển thị ô của bạn / ô chính; số = đang hover ô đó */
    let hoverBoardIndex = null;

    const prefersReducedMotion = () =>
        typeof window.matchMedia === "function" &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    /** Ô bàn: { name, type, price, color } — nạp từ API / file tĩnh */
    let cells = [];

    const escapeAttr = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/"/g, "&quot;")
            .replace(/</g, "&lt;");

    const escapeHtml = (value) =>
        String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

    const safePortraitSrc = (url) => {
        if (!url || typeof url !== "string") return "";
        const t = url.trim();
        if (t.startsWith("/") || t.startsWith("http://") || t.startsWith("https://")) return t;
        return "";
    };

    const portraitForPlayer = (player) =>
        safePortraitSrc(player.heroImageUrl) || safePortraitSrc(player.avatarUrl) || "";

    const PIP_SLOTS = {
        1: [4],
        2: [0, 8],
        3: [0, 4, 8],
        4: [0, 2, 6, 8],
        5: [0, 2, 4, 6, 8],
        6: [0, 2, 3, 5, 6, 8]
    };

    const clampDie = (n) => {
        const v = Number(n);
        if (v >= 1 && v <= 6) return v;
        return 1;
    };

    const buildPipFaceHtml = (n) => {
        const v = clampDie(n);
        const on = PIP_SLOTS[v] || PIP_SLOTS[1];
        let html = '<div class="die-pip-grid">';
        for (let i = 0; i < 9; i += 1) {
            const dot = on.includes(i) ? '<span class="pip-dot"></span>' : "";
            html += `<span class="pip-cell">${dot}</span>`;
        }
        html += "</div>";
        return html;
    };

    const setDieVisual = (dieEl, value) => {
        if (!dieEl) return;
        const v = clampDie(value);
        let root = dieEl.querySelector(".die-pip-root");
        if (!root) {
            root = document.createElement("div");
            root.className = "die-pip-root";
            root.setAttribute("aria-hidden", "true");
            dieEl.appendChild(root);
        }
        root.innerHTML = buildPipFaceHtml(v);
        dieEl.setAttribute("data-face", String(v));
    };

    const restartTokenHeroIdle = () => {
        if (!window.HeroSystem) return;
        window.requestAnimationFrame(() => {
            players.forEach((player, idx) => {
                if (!portraitForPlayer(player) && player.heroName) {
                    window.HeroSystem.startIdle(`tok-${player.id}`, idx * 0.09);
                }
            });
        });
    };

    const animateTokenSteps = (plans) => {
        const L = cells.length;
        if (!plans.length || !L) return Promise.resolve();

        if (prefersReducedMotion()) {
            plans.forEach((plan) => {
                const p = players.find((x) => x.id === plan.playerId);
                if (p) p.position = plan.to;
            });
            renderTokens();
            restartTokenHeroIdle();
            return Promise.resolve();
        }

        const runGen = tokenStepAnimGen;
        tokenStepAnimRunning = true;
        document.querySelectorAll(".player-token").forEach((el) => el.classList.add("is-step-moving"));

        /** Mỗi ô một bước: cố định thời gian chuyển sang ô kế — nhìn rõ tiến từng ô. */
        const perStepMs = 500;
        const stepDelayMs = perStepMs;
        const stepTransitionMs = Math.max(280, perStepMs - 45);

        const run = async () => {
            for (const plan of plans) {
                if (runGen !== tokenStepAnimGen) break;
                await new Promise((resolve) => {
                    let n = 0;
                    const tick = () => {
                        if (runGen !== tokenStepAnimGen) {
                            resolve();
                            return;
                        }
                        if (n >= plan.steps) {
                            const p = players.find((x) => x.id === plan.playerId);
                            if (p) p.position = plan.to;
                            renderTokens();
                            resolve();
                            return;
                        }
                        n += 1;
                        const p = players.find((x) => x.id === plan.playerId);
                        if (p) p.position = (plan.from + n + L) % L;
                        renderTokens({
                            stepTransitionMs,
                            movingPlayerId: plan.playerId
                        });
                        window.setTimeout(tick, stepDelayMs);
                    };
                    tick();
                });
            }
        };

        return run()
            .catch(() => {})
            .finally(() => {
                document
                    .querySelectorAll(".player-token.is-step-moving")
                    .forEach((el) => el.classList.remove("is-step-moving"));
                tokenStepAnimRunning = false;
                restartTokenHeroIdle();
            });
    };

    const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

    const waitUntilAnimationsIdle = async () => {
        const deadline = Date.now() + 45000;
        while (Date.now() < deadline) {
            if (!tokenStepAnimRunning && !diceRollingTimer) return;
            await sleep(24);
        }
    };

    const maybeMoneyFx = (prevBal, list) => {
        if (!moneyFxLayer || !boardStageEl || prefersReducedMotion()) return;
        const stageRect = boardStageEl.getBoundingClientRect();
        list.forEach((p) => {
            const ob = prevBal[p.id];
            if (ob == null) return;
            const diff = p.money - ob;
            if (diff === 0) return;
            const cardIdx = players.findIndex((x) => x.id === p.id);
            if (cardIdx < 0) return;
            const card = document.getElementById(`playerCard${cardIdx + 1}`);
            if (!card) return;
            const crect = card.getBoundingClientRect();
            const centerX = stageRect.left + stageRect.width / 2;
            const centerY = stageRect.top + stageRect.height * 0.42;
            const fromX = crect.left + crect.width / 2;
            const fromY = crect.top + crect.height / 2;
            const spend = diff < 0;
            const startX = spend ? fromX : centerX;
            const startY = spend ? fromY : centerY;
            const endX = spend ? centerX : fromX;
            const endY = spend ? centerY : fromY;
            const count = Math.min(16, Math.max(5, Math.floor(Math.abs(diff) / 180) + 5));
            for (let i = 0; i < count; i += 1) {
                const coin = document.createElement("span");
                coin.className = "money-coin-particle";
                coin.textContent = "$";
                const ox = (Math.random() - 0.5) * 40;
                const oy = (Math.random() - 0.5) * 28;
                coin.style.left = `${startX - stageRect.left + ox}px`;
                coin.style.top = `${startY - stageRect.top + oy}px`;
                moneyFxLayer.appendChild(coin);
                window.requestAnimationFrame(() => {
                    coin.style.setProperty("--tx", `${endX - startX - ox}px`);
                    coin.style.setProperty("--ty", `${endY - startY - oy}px`);
                    coin.classList.add("money-coin-particle--fly");
                });
                window.setTimeout(() => coin.remove(), 720);
            }
        });
    };

    const stopTurnTimer = () => {
        if (turnTimerInterval) {
            clearInterval(turnTimerInterval);
            turnTimerInterval = null;
        }
    };

    const updateTurnTimerDisplay = (seconds, show) => {
        if (!turnTimerLabel) return;
        const timerRow = turnTimerLabel.closest(".turn-top-timer");
        if (!show || seconds == null) {
            turnTimerLabel.textContent = "";
            turnTimerLabel.classList.remove("urgent");
            if (timerRow) timerRow.hidden = true;
            return;
        }
        const s = Math.max(0, seconds);
        if (timerRow) timerRow.hidden = false;
        turnTimerLabel.textContent = s <= 0 ? "Hết giờ" : `Còn ${s} giây`;
        turnTimerLabel.classList.toggle("urgent", s <= 5);
    };

    const mapRowTypeToDisplay = (t) => {
        if (!t) return "property";
        const u = String(t).toUpperCase();
        if (u.includes("START") || u === "GO") return "start";
        if (u.includes("CHANCE") || u.includes("COMMUNITY")) return "chance";
        if (u.includes("TAX")) return "tax";
        if (u.includes("JAIL")) return "jail";
        if (u.includes("FREE")) return "free";
        return "property";
    };

    const layoutItemToCell = (item) => {
        const displayType = item.displayType || mapRowTypeToDisplay(item.type);
        const price =
            item.priceLabel != null && item.priceLabel !== ""
                ? item.priceLabel
                : item.price != null
                  ? `$${item.price}`
                  : "";
        return {
            name: item.name,
            type: displayType,
            price,
            color: item.colorHex || ""
        };
    };

    const loadBoardLayout = async () => {
        try {
            const api = await fetch(`/api/board/maps/${layoutMapId}/layout`);
            if (api.ok) {
                const data = await api.json();
                if (Array.isArray(data) && data.length > 0) {
                    cells = data.map(layoutItemToCell);
                    return;
                }
            }
        } catch (e) {
            /* fallback */
        }
        try {
            const st = await fetch("/seed/board-classic.json");
            if (st.ok) {
                const raw = await st.json();
                if (Array.isArray(raw) && raw.length > 0) {
                    cells = raw.map((row) => layoutItemToCell(row));
                }
            }
        } catch (e2) {
            /* ignore */
        }
    };

    const defaultPlayers = [
        {
            id: 1,
            username: "Player1",
            money: 1000,
            color: PLAYER_PALETTE[0],
            avatar: "P1",
            position: 0,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        },
        {
            id: 2,
            username: "Player2",
            money: 1000,
            color: PLAYER_PALETTE[1],
            avatar: "P2",
            position: 7,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        },
        {
            id: 3,
            username: "Player3",
            money: 1000,
            color: PLAYER_PALETTE[2],
            avatar: "P3",
            position: 18,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        },
        {
            id: 4,
            username: "Player4",
            money: 1000,
            color: PLAYER_PALETTE[3],
            avatar: "P4",
            position: 28,
            avatarUrl: "/images/avatar-default.png",
            heroImageUrl: null,
            heroName: null,
            isBot: false
        }
    ];
    const isBotMode = urlParams.get("vsBot") === "1";
    const difficulty = (urlParams.get("difficulty") || localStorage.getItem("aiDifficulty") || "easy").toLowerCase();
    const botLabel = difficulty === "hard" ? "Bot khó" : "Bot dễ";

    const players = isBotMode
        ? [
            {
                id: 1,
                username: "You",
                money: 1000,
                color: PLAYER_PALETTE[0],
                avatar: "ME",
                position: 0,
                avatarUrl: "/images/avatar-default.png",
                heroImageUrl: null,
                heroName: null,
                isBot: false
            },
            {
                id: 2,
                username: botLabel,
                money: difficulty === "hard" ? 1200 : 1000,
                color: PLAYER_PALETTE[1],
                avatar: difficulty === "hard" ? "H" : "E",
                position: 7,
                avatarUrl: null,
                heroImageUrl: "/images/heroes/bot.png",
                heroName: null,
                isBot: true
            }
        ]
        : defaultPlayers;

    const chatMessages = [];

    let activePlayerIndex = 0;

    const setActionStatus = (message, isError = false) => {
        if (!gameActionStatus) return;
        gameActionStatus.textContent = message;
        gameActionStatus.style.color = isError ? "#ffd2d2" : "#fff8ed";
    };

    const authHeaders = (includeJson = false) => {
        const headers = {};
        if (accountId) {
            headers["X-Account-Id"] = accountId;
        }
        if (includeJson) {
            headers["Content-Type"] = "application/json";
        }
        return headers;
    };

    const formatMoney = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const applyBoardOwnership = (state) => {
        if (!boardElement) return;
        boardElement.querySelectorAll(".board-cell[data-cell-index]").forEach((el) => {
            el.classList.remove("board-cell--owned");
            el.style.removeProperty("--owner-tint");
        });
        if (!state?.ownedCells?.length) return;
        const byIdx = new Map(state.ownedCells.map((o) => [o.boardIndex, o.ownerTurnOrder]));
        boardElement.querySelectorAll(".board-cell[data-cell-index]").forEach((el) => {
            const idx = parseInt(el.getAttribute("data-cell-index"), 10);
            const turn = byIdx.get(idx);
            if (turn == null) return;
            const c = PLAYER_PALETTE[Math.max(0, turn - 1) % PLAYER_PALETTE.length];
            el.classList.add("board-cell--owned");
            el.style.setProperty("--owner-tint", c);
        });
    };

    const updateActiveTurnHighlight = (state) => {
        for (let i = 1; i <= 4; i += 1) {
            const el = document.getElementById(`playerCard${i}`);
            if (!el) continue;
            el.classList.remove("player-info--active-turn");
            el.style.removeProperty("--turn-ring");
        }
        const playing = String(state?.status || "").toUpperCase() === "PLAYING";
        if (!playing || state?.currentPlayerOrder == null) return;
        const idx = players.findIndex((p) => p.id === state.currentPlayerOrder);
        if (idx < 0) return;
        const card = document.getElementById(`playerCard${idx + 1}`);
        if (!card) return;
        const p = players[idx];
        card.classList.add("player-info--active-turn");
        card.style.setProperty("--turn-ring", p.color || PLAYER_PALETTE[idx % PLAYER_PALETTE.length]);
    };

    const getPlayerLabelByOrder = (order) => {
        const p = players.find((x) => x.id === order);
        if (p) return p.username;
        return `Player ${order}`;
    };

    /** Người “bạn” trên client: ưu tiên người không phải bot. */
    const getMyPlayer = () => players.find((p) => !p.isBot) || players[0];

    const parsePriceFromLayout = (priceStr) => {
        if (priceStr == null) return null;
        const m = String(priceStr).replace(/,/g, "").match(/(\d+)/);
        return m ? parseInt(m[1], 10) : null;
    };

    const ownerTurnAtIndex = (state, boardIdx) => {
        const hit = state?.ownedCells?.find((o) => o.boardIndex === boardIdx);
        return hit?.ownerTurnOrder ?? null;
    };

    /**
     * currentCell API chỉ mô tả đúng ô này khi trùng vị trí người đang tới lượt (server).
     */
    const apiCellMatchesBoardIndex = (state, boardIdx) => {
        if (!state?.currentCell) return false;
        const cur = players.find((p) => p.id === state.currentPlayerOrder);
        if (!cur || cur.position !== boardIdx) return false;
        return true;
    };

    const refreshCellInfoPanel = (state) => {
        const s = state ?? lastLiveState;
        const L = cells.length;
        const idx =
            hoverBoardIndex != null && hoverBoardIndex >= 0
                ? hoverBoardIndex
                : isLiveGame && getMyPlayer()
                  ? getMyPlayer().position
                  : Math.max(0, players[activePlayerIndex]?.position ?? 0);
        const safeIdx = L ? ((idx % L) + L) % L : 0;
        const layout = cells[safeIdx];

        if (!layout) {
            if (currentCellName) currentCellName.textContent = "—";
            if (currentCellPriceTax) currentCellPriceTax.textContent = "—";
            if (currentCellUpgrade) currentCellUpgrade.textContent = "—";
            if (currentCellOwnerBlock) currentCellOwnerBlock.textContent = "—";
            return;
        }

        const useApi = apiCellMatchesBoardIndex(s, safeIdx);
        const api = useApi ? s.currentCell : null;

        if (currentCellName) {
            currentCellName.textContent = api?.name || layout.name || "—";
        }

        if (currentCellPriceTax) {
            if (api) {
                if (layout.type === "tax" || (api.type && String(api.type).toUpperCase().includes("TAX"))) {
                    currentCellPriceTax.textContent = `Thuế / phí: ${formatMoney(api.price)}`;
                } else if (api.price != null && api.price > 0) {
                    currentCellPriceTax.textContent = `Giá mua đất: ${formatMoney(api.price)}`;
                } else {
                    currentCellPriceTax.textContent = layout.price || "Không áp dụng giá mua";
                }
            } else if (layout.type === "tax") {
                currentCellPriceTax.textContent = layout.price
                    ? `Thuế / phí (ước tính): ${layout.price}`
                    : "Thuế — xem chi tiết khi chơi online";
            } else {
                const n = parsePriceFromLayout(layout.price);
                currentCellPriceTax.textContent =
                    n != null ? `Giá (bố cục): ${formatMoney(n)}` : layout.price || "—";
            }
        }

        if (currentCellUpgrade) {
            if (api && layout.type === "property") {
                currentCellUpgrade.textContent = `Cấp hiện tại: ${api.houseLevel ?? 0} · Nâng cấp kế: ${formatMoney(api.upgradeCost ?? 0)}`;
            } else if (layout.type === "property") {
                const n = parsePriceFromLayout(layout.price);
                const guess = n != null ? Math.max(100, Math.floor(n * 0.5)) : null;
                currentCellUpgrade.textContent =
                    guess != null
                        ? `Cấp: — · Ước phí nâng cấp: ${formatMoney(guess)} (khi tới lượt)`
                        : "Chi tiết cấp khi bạn đứng trên ô này trong lượt online";
            } else {
                currentCellUpgrade.textContent = "Không nâng cấp";
            }
        }

        if (currentCellOwnerBlock) {
            const ot = api?.ownerTurnOrder ?? ownerTurnAtIndex(s, safeIdx);
            currentCellOwnerBlock.textContent = ot ? getPlayerLabelByOrder(ot) : "Chưa có";
        }
    };

    const attachBoardCellHover = () => {
        if (!boardElement) return;
        boardElement.querySelectorAll(".board-cell[data-cell-index]").forEach((el) => {
            if (el.dataset.hoverBound === "1") return;
            el.dataset.hoverBound = "1";
            el.addEventListener("mouseenter", () => {
                const v = parseInt(el.getAttribute("data-cell-index"), 10);
                hoverBoardIndex = Number.isFinite(v) ? v : null;
                refreshCellInfoPanel(lastLiveState);
            });
            el.addEventListener("mouseleave", () => {
                hoverBoardIndex = null;
                refreshCellInfoPanel(lastLiveState);
            });
        });
    };

    const updateTurnBanner = (state) => {
        if (!turnBannerCurrent || !turnBannerNext) return;
        const s = state !== undefined && state !== null ? state : lastLiveState;
        if (s && String(s.status || "").toUpperCase() === "FINISHED") {
            turnBannerCurrent.textContent = "Ván kết thúc";
            turnBannerNext.textContent = "—";
            return;
        }
        const curOrder =
            s?.currentPlayerOrder ??
            (players.length ? players[Math.min(activePlayerIndex, players.length - 1)]?.id : null);
        if (curOrder == null || !players.length) {
            turnBannerCurrent.textContent = "—";
            turnBannerNext.textContent = "—";
            return;
        }
        const orders = [...new Set(players.map((p) => p.id))].sort((a, b) => a - b);
        const idx = Math.max(0, orders.indexOf(curOrder));
        const nextOrder = orders.length ? orders[(idx + 1) % orders.length] : curOrder;
        turnBannerCurrent.textContent = getPlayerLabelByOrder(curOrder);
        turnBannerNext.textContent = getPlayerLabelByOrder(nextOrder);
    };

    /**
     * Đúng 40 ô viền lưới 11×11 (hàng 1 = trên, cột 1 = trái, hàng 11 = dưới).
     * Đi ngược chiều kim đồng hồ từ GO (11,11): cạnh dưới đủ 11 ô | trái 10 | trên 10 | phải 9 → 11+10+10+9 = 40.
     */
    const getBoardPlacement = (index) => {
        const i = ((index % 40) + 40) % 40;
        if (i <= 10) {
            return { row: 11, col: 11 - i };
        }
        if (i <= 20) {
            return { row: 21 - i, col: 1 };
        }
        if (i <= 30) {
            return { row: 1, col: i - 19 };
        }
        return { row: i - 29, col: 11 };
    };

    const getCellClass = (cell) => {
        if (cell.type === "property") return "cell-property";
        if (cell.type === "chance") return "cell-chance";
        if (cell.type === "tax") return "cell-tax";
        if (cell.type === "jail") return "cell-jail";
        if (cell.type === "free") return "cell-free";
        return "cell-start";
    };

    const buildBoard = () => {
        if (!boardElement) return;
        if (!cells.length) {
            boardElement.classList.add("board-is-empty");
            boardElement.querySelectorAll(".board-cell").forEach((n) => n.remove());
            if (!boardElement.querySelector(".board-empty-msg")) {
                const msg = document.createElement("div");
                msg.className = "board-empty-msg";
                msg.textContent = "Không tải được dữ liệu bàn cờ. Hãy chạy backend và seed map.";
                boardElement.appendChild(msg);
            }
            if (tokensElement) boardElement.appendChild(tokensElement);
            return;
        }
        boardElement.classList.remove("board-is-empty");
        boardElement.querySelector(".board-empty-msg")?.remove();
        boardElement.querySelectorAll(".board-cell").forEach((n) => n.remove());

        cells.forEach((cell, index) => {
            const placement = getBoardPlacement(index);
            const div = document.createElement("div");
            div.className = `board-cell ${getCellClass(cell)}`;
            div.setAttribute("data-cell-index", String(index));
            div.style.gridRow = String(placement.row);
            div.style.gridColumn = String(placement.col);
            const sub = cell.price || String(cell.type || "").toUpperCase();
            div.innerHTML = `
                <strong>${escapeHtml(cell.name)}</strong>
                <small>${escapeHtml(sub)}</small>
            `;
            boardElement.appendChild(div);
        });

        if (tokensElement) boardElement.appendChild(tokensElement);
        attachBoardCellHover();
        refreshCellInfoPanel(lastLiveState);
    };

    const buildPlayerCards = () => {
        for (let index = 0; index < 4; index += 1) {
            const card = document.getElementById(`playerCard${index + 1}`);
            if (!card) continue;
            if (index >= players.length) {
                card.innerHTML = "";
                card.classList.remove("player-info--active-turn");
                card.style.removeProperty("--turn-ring");
                card.style.visibility = "hidden";
                continue;
            }
            card.style.visibility = "";
            const player = players[index];
            const src = portraitForPlayer(player);
            let chipInner;
            if (src) {
                chipInner = `<img class="player-chip-img" src="${escapeAttr(src)}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'"><span class="player-chip-fallback" style="display:none">${escapeHtml(player.avatar)}</span>`;
            } else if (window.HeroSystem && player.heroName) {
                chipInner = `<div class="player-chip-svg">${window.HeroSystem.getSVG(player.heroName, `card-${player.id}`)}</div>`;
            } else {
                chipInner = `<span class="player-chip-fallback">${escapeHtml(player.avatar)}</span>`;
            }

            card.innerHTML = `
                <div class="player-chip-wrap player-chip-wrap--animated">
                    <div class="player-chip player-chip--portrait" style="--chip-ring:${player.color}">
                        ${chipInner}
                    </div>
                </div>
                <div class="player-meta">
                    <strong>${escapeHtml(player.username)}</strong>
                    <span>$${player.money.toLocaleString("en-US")}</span>
                </div>
            `;
        }

        if (window.HeroSystem) {
            window.requestAnimationFrame(() => {
                players.forEach((player, idx) => {
                    if (!portraitForPlayer(player) && player.heroName) {
                        window.HeroSystem.startIdle(`card-${player.id}`, idx * 0.08);
                    }
                });
            });
        }
    };

    const renderChat = () => {
        if (!chatMessagesElement) return;
        chatMessagesElement.innerHTML = chatMessages
            .map((item) => {
                const sys = item.system ? " chat-bubble--system" : "";
                return `
            <article class="chat-bubble${sys}">
                <strong>${escapeHtml(item.user)}</strong>
                <p>${escapeHtml(item.message)}</p>
            </article>`;
            })
            .join("");
        chatMessagesElement.scrollTop = chatMessagesElement.scrollHeight;
    };

    const appendRentNoticesToChat = (state) => {
        const list = state?.rentNotices;
        if (!list || !list.length) return;
        for (const n of list) {
            const payer = n.payerName || "?";
            const owner = n.ownerName || "?";
            const cell = n.cellName || "ô";
            const amt = n.amountPaid ?? 0;
            const msg = `${payer} đã tiêu ${formatMoney(amt)} tiền ở ${cell} (trả cho ${owner}).`;
            chatMessages.push({ user: "Hệ thống", message: msg, system: true });
        }
        renderChat();
    };

    /** Tọa độ layout (px) từ góc trên-trái của ancestor (bỏ qua transform màn hình). */
    const offsetRelToAncestor = (el, ancestor) => {
        let left = 0;
        let top = 0;
        let n = el;
        while (n && n !== ancestor) {
            left += n.offsetLeft;
            top += n.offsetTop;
            n = n.offsetParent;
        }
        return n === ancestor ? { left, top } : null;
    };

    /** Tọa độ ô trong lớp #playerTokens (cùng hệ với left/top của quân). */
    const getCellRect = (index) => {
        const cell = boardElement.querySelector(`[data-cell-index="${index}"]`);
        if (!cell || !tokensElement) {
            return null;
        }
        const cellRel = offsetRelToAncestor(cell, boardElement);
        const tokRel = offsetRelToAncestor(tokensElement, boardElement);
        if (cellRel && tokRel) {
            return {
                left: cellRel.left - tokRel.left,
                top: cellRel.top - tokRel.top,
                width: cell.offsetWidth,
                height: cell.offsetHeight
            };
        }
        const tr = tokensElement.getBoundingClientRect();
        const cr = cell.getBoundingClientRect();
        return {
            left: cr.left - tr.left,
            top: cr.top - tr.top,
            width: cr.width,
            height: cr.height
        };
    };

    const buildTokenLayouts = () => {
        const byPos = {};
        players.forEach((p) => {
            byPos[p.position] = byPos[p.position] || [];
            byPos[p.position].push(p);
        });
        const out = [];
        for (const player of players) {
            const rect = getCellRect(player.position);
            if (!rect) continue;
            const stack = byPos[player.position] || [player];
            const stackIdx = stack.indexOf(player);
            const n = stack.length;
            const cellCx = rect.left + rect.width / 2;
            const cellCy = rect.top + rect.height / 2;
            let offX = 0;
            let offY = 0;
            if (n === 2) {
                const d = Math.min(16, rect.width * 0.12);
                if (stackIdx === 0) {
                    offX = -d;
                    offY = -d;
                } else {
                    offX = d;
                    offY = d;
                }
            } else if (n > 2) {
                const r = Math.min(18, rect.width * 0.14);
                const angle = (stackIdx / n) * Math.PI * 2 - Math.PI / 2;
                offX = Math.cos(angle) * r;
                offY = Math.sin(angle) * r;
            }
            out.push({
                player,
                cx: cellCx + offX,
                cy: cellCy + offY,
                bobDelay: (player.id % 6) * 0.11
            });
        }
        return out;
    };

    const mountPlayerTokenContent = (el, player, bobDelay) => {
        const src = portraitForPlayer(player);
        let inner;
        if (src) {
            inner = `<img class="token-pawn__img" src="${escapeAttr(src)}" alt="" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'"><span class="token-pawn__fallback" style="display:none">${escapeHtml(
                player.avatar
            )}</span>`;
        } else if (window.HeroSystem && player.heroName) {
            inner = `<div class="token-pawn__svg">${window.HeroSystem.getSVG(player.heroName, `tok-${player.id}`)}</div>`;
        } else {
            inner = `<span class="token-pawn__fallback">${escapeHtml(player.avatar)}</span>`;
        }
        el.innerHTML = `<div class="token-pawn__body">${inner}</div><div class="token-pawn__shadow" aria-hidden="true"></div>`;
        el.style.setProperty("--bob-delay", `${bobDelay}s`);
        el.style.setProperty("--token-color", player.color);
    };

    const renderTokens = (opts = {}) => {
        if (!tokensElement) return;
        const stepTransitionMs = opts.stepTransitionMs;
        const movingPlayerId = opts.movingPlayerId;

        const layouts = buildTokenLayouts();
        const wanted = new Set(layouts.map((l) => String(l.player.id)));

        tokensElement.querySelectorAll(".player-token").forEach((el) => {
            const id = el.getAttribute("data-player-order");
            if (!wanted.has(id)) el.remove();
        });

        for (const { player, cx, cy, bobDelay } of layouts) {
            let el = tokensElement.querySelector(`[data-player-order="${player.id}"]`);
            if (!el) {
                el = document.createElement("div");
                el.setAttribute("data-player-order", String(player.id));
                el.className = "player-token player-token--pawn";
                mountPlayerTokenContent(el, player, bobDelay);
                tokensElement.appendChild(el);
            }
            if (stepTransitionMs != null && movingPlayerId === player.id) {
                el.style.transition = `left ${stepTransitionMs}ms linear, top ${stepTransitionMs}ms linear`;
            } else {
                el.style.removeProperty("transition");
            }
            el.style.left = `${cx}px`;
            el.style.top = `${cy}px`;
            el.style.setProperty("--token-color", player.color);
            el.style.setProperty("--bob-delay", `${bobDelay}s`);
        }

        if (!tokenStepAnimRunning) {
            restartTokenHeroIdle();
        }
    };

    const randomDieFace = () => Math.floor(Math.random() * 6) + 1;

    const stopDiceRolling = () => {
        if (diceRollingTimer) {
            clearInterval(diceRollingTimer);
            diceRollingTimer = null;
        }
        die1?.classList.remove("rolling", "just-landed");
        die2?.classList.remove("rolling", "just-landed");
        diceArea?.classList.remove("is-rolling");
    };

    const startDiceRolling = () => {
        stopDiceRolling();
        diceArea?.classList.add("is-rolling");
        die1?.classList.add("rolling");
        die2?.classList.add("rolling");
        diceRollingTimer = window.setInterval(() => {
            setDieVisual(die1, randomDieFace());
            setDieVisual(die2, randomDieFace());
        }, 68);
    };

    const playDiceLandEffect = () => {
        if (!die1 || !die2) return;
        die1.classList.remove("just-landed");
        die2.classList.remove("just-landed");
        void die1.offsetWidth;
        void die2.offsetWidth;
        die1.classList.add("just-landed");
        die2.classList.add("just-landed");
        diceTotal?.classList.remove("pop");
        void diceTotal?.offsetWidth;
        diceTotal?.classList.add("pop");
        window.setTimeout(() => {
            die1?.classList.remove("just-landed");
            die2?.classList.remove("just-landed");
            diceTotal?.classList.remove("pop");
        }, 620);
    };

    const showYourTurnNotice = () => {
        if (!yourTurnToast || !turnTopBanner) return;
        if (yourTurnHideTimer) clearTimeout(yourTurnHideTimer);
        if (yourTurnHideTimer2) clearTimeout(yourTurnHideTimer2);
        yourTurnHideTimer = null;
        yourTurnHideTimer2 = null;
        yourTurnToast.hidden = false;
        yourTurnToast.setAttribute("aria-hidden", "false");
        requestAnimationFrame(() => {
            yourTurnToast.classList.add("is-visible");
        });
        turnTopBanner.classList.add("your-turn-spotlight");
        yourTurnHideTimer = window.setTimeout(() => {
            yourTurnToast.classList.remove("is-visible");
            turnTopBanner.classList.remove("your-turn-spotlight");
            yourTurnHideTimer = null;
            yourTurnHideTimer2 = window.setTimeout(() => {
                yourTurnToast.hidden = true;
                yourTurnToast.setAttribute("aria-hidden", "true");
                yourTurnHideTimer2 = null;
            }, 420);
        }, 2600);
    };

    const animateDiceOffline = () => {
        const L = cells.length;
        if (!L) return;
        if (rollDiceButton) rollDiceButton.disabled = true;
        startDiceRolling();
        window.setTimeout(() => {
            stopDiceRolling();
            const value1 = 3;
            const value2 = 5;
            setDieVisual(die1, value1);
            setDieVisual(die2, value2);
            diceTotal.textContent = `Tổng: ${value1 + value2}`;
            playDiceLandEffect();
            const activePlayer = players[activePlayerIndex];
            const from = activePlayer.position;
            const steps = value1 + value2;
            const to = (from + steps) % L;

            tokenStepAnimGen += 1;
            tokenStepAnimRunning = false;
            document.querySelectorAll(".player-token.is-step-moving").forEach((el) =>
                el.classList.remove("is-step-moving")
            );

            activePlayer.position = from;
            renderTokens();

            void animateTokenSteps([{ playerId: activePlayer.id, from, to, steps }]).then(() => {
                activePlayerIndex = (activePlayerIndex + 1) % players.length;
                updateTurnBanner(null);
                refreshCellInfoPanel(null);
                if (rollDiceButton) rollDiceButton.disabled = false;
            });
        }, 1050);
    };

    async function applyStateInternal(state, opts = {}) {
        stopTurnTimer();
        lastLiveState = state;
        const playing = String(state.status || "").toUpperCase() === "PLAYING";
        const myTurn = state.myTurn === true;
        const ts = state.turnState || "";

        const oldBalances = {};
        const oldPositions = {};
        players.forEach((p) => {
            oldBalances[p.id] = p.money;
            oldPositions[p.id] = p.position;
        });

        const nextPlayers = [];
        state.players.forEach((p) => {
            const label =
                p.username ||
                (p.isBot ? `Bot ${p.turnOrder}` : `Player ${p.turnOrder}`);
            const orderIdx = Math.max(0, (p.turnOrder || 1) - 1);
            nextPlayers.push({
                id: p.turnOrder,
                username: label,
                money: p.balance || 0,
                color: PLAYER_PALETTE[orderIdx % PLAYER_PALETTE.length],
                avatar: p.isBot ? "BOT" : `P${p.turnOrder}`,
                position: p.position || 0,
                avatarUrl: p.avatarUrl || null,
                heroImageUrl: p.heroImageUrl || null,
                heroName: p.heroName || null,
                isBot: Boolean(p.isBot)
            });
        });

        const L = cells.length;
        const animPlans = [];
        if (isLiveGame && L) {
            const d1 = state.lastDice1;
            const d2 = state.lastDice2;
            const diceSum =
                d1 != null && d2 != null && d1 >= 1 && d1 <= 6 && d2 >= 1 && d2 <= 6 ? d1 + d2 : null;

            nextPlayers.forEach((np) => {
                const oldPos = oldPositions[np.id];
                if (oldPos == null) return;
                const to = np.position;
                let steps = (to - oldPos + L) % L;
                if (steps === 0) return;
                /* Nếu khớp tổng xúc xắc → chắc chắn là đi bộ theo đường vòng (tránh nhảy ô kiểu đi tù / thẻ) */
                if (diceSum != null && (oldPos + diceSum) % L === to) {
                    steps = diceSum;
                } else if (steps > 12) {
                    return;
                }
                if (steps >= 1 && steps < L) {
                    animPlans.push({ playerId: np.id, from: oldPos, to, steps });
                }
            });
        }

        players.length = 0;
        nextPlayers.forEach((np) => players.push({ ...np }));

        if (animPlans.length && isLiveGame) {
            animPlans.forEach((plan) => {
                const p = players.find((x) => x.id === plan.playerId);
                if (p) p.position = plan.from;
            });
        }

        activePlayerIndex = Math.max((state.currentPlayerOrder || 1) - 1, 0);
        if (state.lastDice1 != null && state.lastDice2 != null) {
            setDieVisual(die1, state.lastDice1);
            setDieVisual(die2, state.lastDice2);
            diceTotal.textContent = `Tổng: ${state.lastDice1 + state.lastDice2}`;
        }

        if (isLiveGame) {
            if (rollDiceButton) {
                rollDiceButton.disabled = !myTurn || ts !== "WAIT_ROLL" || !playing;
            }
            if (endTurnButton) {
                endTurnButton.disabled = !myTurn || ts !== "ACTION_REQUIRED" || !playing;
            }
            if (buyPropertyButton) {
                buyPropertyButton.disabled =
                    !myTurn || ts !== "ACTION_REQUIRED" || !playing || !state.currentCell?.canBuy;
            }
            if (upgradePropertyButton) {
                upgradePropertyButton.disabled =
                    !myTurn || ts !== "ACTION_REQUIRED" || !playing || !state.currentCell?.canUpgrade;
            }

            const showTimer =
                myTurn && playing && typeof state.turnSecondsRemaining === "number";
            updateTurnTimerDisplay(state.turnSecondsRemaining, showTimer);
            if (showTimer) {
                let localSec = state.turnSecondsRemaining;
                turnTimerInterval = window.setInterval(() => {
                    localSec -= 1;
                    if (localSec <= 0) {
                        stopTurnTimer();
                        updateTurnTimerDisplay(0, true);
                        loadLiveState();
                        return;
                    }
                    updateTurnTimerDisplay(localSec, true);
                }, 1000);
            }
        } else {
            if (rollDiceButton) rollDiceButton.disabled = false;
            if (endTurnButton) endTurnButton.disabled = false;
            if (buyPropertyButton) buyPropertyButton.disabled = false;
            if (upgradePropertyButton) upgradePropertyButton.disabled = false;
            updateTurnTimerDisplay(null, false);
        }

        if (isLiveGame && playing) {
            const canRollNow = myTurn && ts === "WAIT_ROLL";
            if (canRollNow && !prevMyTurnCouldRoll) {
                showYourTurnNotice();
            }
            prevMyTurnCouldRoll = canRollNow;
        } else {
            prevMyTurnCouldRoll = false;
        }

        buildPlayerCards();
        applyBoardOwnership(state);
        updateActiveTurnHighlight(state);
        maybeMoneyFx(oldBalances, players);
        renderTokens();
        if (opts.diceLand) {
            playDiceLandEffect();
        }
        if (animPlans.length && isLiveGame) {
            await animateTokenSteps(animPlans);
        }
        updateTurnBanner(state);
        refreshCellInfoPanel(state);
        appendRentNoticesToChat(state);
    }

    const syncFromState = (state, opts = {}) => {
        if (!state || !state.players) return Promise.resolve();
        stateApplyChain = stateApplyChain
            .then(() => waitUntilAnimationsIdle())
            .then(() => applyStateInternal(state, opts))
            .catch(() => {});
        return stateApplyChain;
    };

    const callAction = async (actionPath) => {
        const response = await fetch(`/api/gameplay/${gameId}/${actionPath}`, {
            method: "POST",
            headers: authHeaders(true)
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Thao tác thất bại");
        }
        return response.json();
    };

    const loadLiveState = async () => {
        const response = await fetch(`/api/gameplay/${gameId}/state`, {
            headers: authHeaders()
        });
        if (!response.ok) return;
        const state = await response.json();
        syncFromState(state);
    };

    rollDiceButton.addEventListener("click", async () => {
        if (!isLiveGame) {
            animateDiceOffline();
            return;
        }
        if (rollDiceButton.disabled) return;
        const startedAt = Date.now();
        const minRollMs = 720;
        startDiceRolling();
        rollDiceButton.disabled = true;
        try {
            const data = await callAction("roll");
            const wait = Math.max(0, minRollMs - (Date.now() - startedAt));
            await new Promise((r) => setTimeout(r, wait));
            stopDiceRolling();
            await syncFromState(data.state, { diceLand: true });
            setActionStatus(data.message || "Đã tung xúc xắc");
        } catch (error) {
            stopDiceRolling();
            if (lastLiveState) {
                await syncFromState(lastLiveState);
            }
            setActionStatus(error.message || "Tung xúc xắc thất bại", true);
        }
    });
    endTurnButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callAction("end-turn");
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã kết thúc lượt");
        } catch (error) {
            setActionStatus(error.message || "Kết thúc lượt thất bại", true);
        }
    });
    buyPropertyButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callAction("buy");
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã mua đất");
        } catch (error) {
            setActionStatus(error.message || "Mua đất thất bại", true);
        }
    });
    upgradePropertyButton?.addEventListener("click", async () => {
        if (!isLiveGame) return;
        try {
            const data = await callAction("upgrade");
            await syncFromState(data.state);
            setActionStatus(data.message || "Đã nâng cấp");
        } catch (error) {
            setActionStatus(error.message || "Nâng cấp thất bại", true);
        }
    });

    chatForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const message = chatInput.value.trim();
        if (!message) {
            return;
        }

        chatMessages.push({
            user: players[activePlayerIndex].username,
            message
        });
        chatInput.value = "";
        renderChat();
    });

    await loadBoardLayout();
    buildBoard();
    refreshCellInfoPanel(null);
    setDieVisual(die1, 3);
    setDieVisual(die2, 5);
    if (diceTotal) diceTotal.textContent = "Tổng: 8";
    buildPlayerCards();
    if (!isLiveGame && players.length) {
        updateActiveTurnHighlight({
            status: "PLAYING",
            currentPlayerOrder: players[Math.min(activePlayerIndex, players.length - 1)].id
        });
    }
    renderChat();
    window.setTimeout(renderTokens, 50);
    window.addEventListener("resize", renderTokens);
    updateTurnBanner(null);
    if (isLiveGame) {
        loadLiveState();
        window.setInterval(loadLiveState, 3000);
    }
});
