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

    const accountId = localStorage.getItem("accountId");
    const redirectToLogin = () => {
        window.location.href = "/login";
    };

    if (!accountId) {
        redirectToLogin();
        return;
    }

    const profileAvatar = document.getElementById("profileAvatar");
    const profileUsername = document.getElementById("profileUsername");
    const profileCoins = document.getElementById("profileCoins");
    const profileTickets = document.getElementById("profileTickets");
    const statMatches = document.getElementById("statMatches");
    const statWinRate = document.getElementById("statWinRate");
    const statWonAssets = document.getElementById("statWonAssets");
    const statRankTier = document.getElementById("statRankTier");

    const equippedCharacterImage = document.getElementById("equippedCharacterImage");
    const equippedCharacterName = document.getElementById("equippedCharacterName");

    const editAvatarButton = document.getElementById("editAvatarButton");
    const avatarFileInput = document.getElementById("avatarFileInput");
    const avatarStatus = document.getElementById("avatarStatus");
    const logoutButton = document.getElementById("logoutButton");

    const getHeaders = () => {
        const headers = {};
        headers["X-Account-Id"] = accountId;
        return headers;
    };

    const formatNumber = (value) => new Intl.NumberFormat("vi-VN").format(value || 0);

    const handleUnauthorized = (response) => {
        if (response.status === 401 || response.status === 403) {
            redirectToLogin();
            return true;
        }
        return false;
    };

    const loadSummary = async () => {
        try {
            const response = await fetch("/api/user/me/summary", { headers: getHeaders() });
            if (handleUnauthorized(response)) {
                return;
            }
            if (!response.ok) {
                throw new Error("Khong the tai thong tin ho so.");
            }

            const data = await response.json();

            if (profileUsername) {
                profileUsername.textContent = data.username || "Nguoi choi";
            }
            if (profileCoins) {
                profileCoins.textContent = `${formatNumber(data.coins)} Xu`;
            }
            if (profileTickets) {
                profileTickets.textContent = `${formatNumber(data.tickets)} Ve`;
            }
            if (statMatches) {
                statMatches.textContent = data.matches ?? 0;
            }
            if (statWinRate) {
                statWinRate.textContent = `${data.winRate ?? 0}%`;
            }
            if (statWonAssets) {
                statWonAssets.textContent = formatNumber(data.totalWonAssets);
            }
            if (statRankTier) {
                statRankTier.textContent = data.rankTier || "-";
            }

            if (equippedCharacterName) {
                equippedCharacterName.textContent = data.equippedCharacterName || "-";
            }
            if (equippedCharacterImage) {
                if (data.equippedCharacterImageUrl) {
                    equippedCharacterImage.style.display = "block";
                    equippedCharacterImage.src = data.equippedCharacterImageUrl;
                    // Clear alt text fallback; keep name in text below.
                    equippedCharacterImage.alt = data.equippedCharacterName || "Hình nhân vật";
                } else {
                    equippedCharacterImage.style.display = "none";
                    equippedCharacterImage.src = "";
                }
            }

            const fallbackAvatarText = () => {
                if (!profileAvatar || !data.username) return;
                profileAvatar.textContent = String(data.username).slice(0, 1).toUpperCase();
                // Keep CSS gradient by clearing inline background override.
                profileAvatar.style.background = "";
            };

            if (profileAvatar && data.avatarUrl) {
                const img = new Image();
                img.onload = () => {
                    profileAvatar.textContent = "";
                    profileAvatar.style.background = `url('${data.avatarUrl}') center/cover no-repeat`;
                };
                img.onerror = () => {
                    fallbackAvatarText();
                };
                img.src = data.avatarUrl;
            } else if (profileAvatar && data.username) {
                fallbackAvatarText();
            }
        } catch (error) {
            console.error(error);
        }
    };

    const setAvatarStatus = (message, isError = false) => {
        if (!avatarStatus) return;
        avatarStatus.textContent = message || "";
        avatarStatus.style.color = isError ? "#ffd7d7" : "";
    };

    const uploadAvatar = async (file) => {
        if (!file) {
            setAvatarStatus("Chưa chọn ảnh.", true);
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            setAvatarStatus("Ảnh quá lớn... Giới hạn 5MB.", true);
            return;
        }

        try {
            setAvatarStatus("Dang cap nhat avatar...");
            const formData = new FormData();
            formData.append("avatar", file);

            const response = await fetch("/api/user/me/avatar", {
                method: "POST",
                headers: getHeaders(),
                body: formData
            });

            if (handleUnauthorized(response)) {
                return;
            }

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Cap nhat avatar that bai.");
            }

            setAvatarStatus("Đã cập nhật avatar!");
            await loadSummary();
        } catch (error) {
            console.error(error);
            setAvatarStatus(error.message || "Cập nhật avatar thất bại.", true);
        } finally {
            if (avatarFileInput) {
                avatarFileInput.value = "";
            }
        }
    };

    if (editAvatarButton && avatarFileInput) {
        editAvatarButton.addEventListener("click", () => {
            avatarFileInput.click();
        });

        avatarFileInput.addEventListener("change", (e) => {
            const file = e.target.files && e.target.files[0] ? e.target.files[0] : null;
            uploadAvatar(file);
        });
    }

    logoutButton?.addEventListener("click", () => {
        localStorage.removeItem("accountId");
        localStorage.removeItem("userRole");
        redirectToLogin();
    });

    loadSummary();
});
