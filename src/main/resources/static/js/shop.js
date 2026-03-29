document.addEventListener("DOMContentLoaded", async () => {
    const shopGrid = document.querySelector(".shop-grid");
    const loading = document.getElementById("shop-loading");
    const coinsNode = document.getElementById("shop-coins");
    const backButton = document.getElementById("shopBackButton");
    const gradients = ["hero-a", "hero-b", "hero-c", "hero-d", "hero-e", "hero-f", "hero-g"];
    const accountId = sessionStorage.getItem("accountId");
    const ownedHeroIds = new Set();
    let currentCoins = 0;

    const formatPrice = (price) => `${new Intl.NumberFormat("vi-VN").format(price || 0)} Xu`;
    const formatCoins = (coins) => `Xu: ${new Intl.NumberFormat("vi-VN").format(coins || 0)}`;

    backButton?.addEventListener("click", () => {
        if (window.history.length > 1) {
            window.history.back();
            return;
        }
        window.location.href = "/home";
    });

    const loadShopState = async () => {
        if (!accountId) {
            coinsNode.textContent = "Xu: vui lòng đăng nhập";
            return;
        }
        const stateResponse = await fetch("/api/user/shop/state", {
            headers: { "X-Account-Id": accountId }
        });
        if (!stateResponse.ok) {
            throw new Error("Không thể tải trạng thái shop");
        }
        const state = await stateResponse.json();
        currentCoins = state.coins || 0;
        (state.ownedHeroIds || []).forEach((id) => ownedHeroIds.add(id));
        coinsNode.textContent = formatCoins(currentCoins);
    };

    const purchaseHero = async (heroId) => {
        const response = await fetch("/api/user/shop/purchase", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Account-Id": accountId
            },
            body: JSON.stringify({ heroId })
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "Mua thất bại");
        }
        return data;
    };

    const shopModal = document.getElementById("shopModal");
    const modalConfirm = document.getElementById("modalConfirm");
    const modalCancel = document.getElementById("modalCancel");
    const modalDescription = document.getElementById("modalDescription");
    let pendingPurchaseHeroId = null;
    let pendingPurchaseButton = null;

    const showModal = (heroName, heroId, button) => {
        pendingPurchaseHeroId = heroId;
        pendingPurchaseButton = button;
        modalDescription.textContent = `Bạn có chắc chắn muốn dùng ${formatPrice(getHeroPriceById(heroId))} để sở hữu nhân vật ${heroName}?`;
        shopModal.hidden = false;
        shopModal.setAttribute("aria-hidden", "false");
    };

    const hideModal = () => {
        shopModal.hidden = true;
        shopModal.setAttribute("aria-hidden", "true");
        pendingPurchaseHeroId = null;
        pendingPurchaseButton = null;
    };

    modalCancel?.addEventListener("click", hideModal);
    document.querySelector(".shop-modal__backdrop")?.addEventListener("click", hideModal);

    const getHeroPriceById = (id) => {
        const h = heroesData.find(x => x.heroId === id);
        return h ? h.price : 0;
    };

    let heroesData = [];

    modalConfirm?.addEventListener("click", async () => {
        if (!pendingPurchaseHeroId || !pendingPurchaseButton) return;
        
        const heroId = pendingPurchaseHeroId;
        const button = pendingPurchaseButton;
        hideModal();

        button.disabled = true;
        button.textContent = "Đang mua...";
        try {
            const result = await purchaseHero(heroId);
            if (result.purchased) {
                ownedHeroIds.add(heroId);
                currentCoins = result.remainingCoins || currentCoins;
                coinsNode.textContent = formatCoins(currentCoins);
                button.textContent = "Đã sở hữu";
                button.disabled = true;
            } else {
                button.disabled = false;
                button.textContent = "Mua";
                alert(result.message || "Không thể mua nhân vật");
            }
        } catch (error) {
            button.disabled = false;
            button.textContent = "Mua";
            alert(error.message || "Mua thất bại");
        }
    });

    try {
        await loadShopState();

        const response = await fetch("/api/heroes", {
            headers: accountId ? { "X-Account-Id": accountId } : {}
        });
        if (!response.ok) {
            throw new Error("Không thể tải danh sách hero");
        }

        const heroes = await response.json();
        heroesData = heroes;
        shopGrid.innerHTML = "";

        heroes.forEach((hero, index) => {
            const card = document.createElement("article");
            card.className = "shop-card";

            const gradientClass = gradients[index % gradients.length];
            const isOwned = ownedHeroIds.has(hero.heroId);
            const isDefaultHero = hero.defaultUnlocked === true;
            const skillPrefix = hero.skillCooldown && hero.skillCooldown > 0
                ? `<span class="cd-badge">CD ${hero.skillCooldown}</span>`
                : `<span class="cd-badge">Passive</span>`;
            const buttonLabel = isOwned ? (isDefaultHero ? "Mặc định" : "Đã sở hữu") : "Mua";
            const heroSvg = window.HeroSystem ? window.HeroSystem.getSVG(hero.name, `shop-${hero.heroId}`) : '';

            card.innerHTML = `
                <div class="hero-image ${gradientClass}">
                    <div style="transform: scale(1.3); transform-origin: center bottom;">
                        ${heroSvg}
                    </div>
                </div>
                <h2>${hero.name || "Chưa đặt tên"}</h2>
                <p class="price">${formatPrice(hero.price)}</p>
                <p class="hero-look">${hero.appearanceDescription || "Đang cập nhật ngoại hình."}</p>
                <p class="hero-skill">${skillPrefix}${hero.skillDescription || "Đang cập nhật skill."}</p>
                <button type="button" ${isOwned ? "disabled" : ""}>${buttonLabel}</button>
            `;

            const button = card.querySelector("button");
            button.addEventListener("click", () => {
                button.blur();
                if (!accountId) {
                    alert("Vui lòng đăng nhập để mua nhân vật.");
                    return;
                }
                if (ownedHeroIds.has(hero.heroId)) {
                    return;
                }
                showModal(hero.name, hero.heroId, button);
            });
            shopGrid.appendChild(card);
            
            if (window.HeroSystem) {
                window.HeroSystem.startIdle(`shop-${hero.heroId}`, index * 0.15);
            }
        });

        if (!heroes.length) {
            shopGrid.innerHTML = `<p class="shop-state">Chưa có nhân vật nào trong cửa hàng.</p>`;
        }
    } catch (error) {
        if (loading) {
            loading.textContent = "Tải danh sách thất bại. Vui lòng thử lại sau.";
        } else {
            shopGrid.innerHTML = `<p class="shop-state">Tải danh sách thất bại. Vui lòng thử lại sau.</p>`;
        }
    }
});
