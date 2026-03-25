document.addEventListener("DOMContentLoaded", () => {
    const boardElement = document.getElementById("monopolyBoard");
    const tokensElement = document.getElementById("playerTokens");
    const chatMessagesElement = document.getElementById("chatMessages");
    const chatForm = document.getElementById("chatForm");
    const chatInput = document.getElementById("chatInput");
    const die1 = document.getElementById("die1");
    const die2 = document.getElementById("die2");
    const diceTotal = document.getElementById("diceTotal");
    const rollDiceButton = document.getElementById("rollDiceButton");
    const currentTurnLabel = document.getElementById("currentTurnLabel");

    const cells = [
        { name: "GO", type: "start", price: "" },
        { name: "Mediterranean", type: "property", price: "$60", color: "#8b4f2b" },
        { name: "Community", type: "chance", price: "" },
        { name: "Baltic", type: "property", price: "$60", color: "#8b4f2b" },
        { name: "Income Tax", type: "tax", price: "$200" },
        { name: "Reading RR", type: "property", price: "$200", color: "#111827" },
        { name: "Oriental", type: "property", price: "$100", color: "#9ad9ff" },
        { name: "Chance", type: "chance", price: "" },
        { name: "Vermont", type: "property", price: "$100", color: "#9ad9ff" },
        { name: "Jail", type: "jail", price: "" },
        { name: "St. Charles", type: "property", price: "$140", color: "#d45cff" },
        { name: "Electric", type: "property", price: "$150", color: "#f3c623" },
        { name: "States", type: "property", price: "$140", color: "#d45cff" },
        { name: "Virginia", type: "property", price: "$160", color: "#d45cff" },
        { name: "Penn RR", type: "property", price: "$200", color: "#111827" },
        { name: "St. James", type: "property", price: "$180", color: "#ff8b43" },
        { name: "Community", type: "chance", price: "" },
        { name: "Tennessee", type: "property", price: "$180", color: "#ff8b43" },
        { name: "New York", type: "property", price: "$200", color: "#ff8b43" },
        { name: "Free Park", type: "free", price: "" },
        { name: "Kentucky", type: "property", price: "$220", color: "#e84343" },
        { name: "Chance", type: "chance", price: "" },
        { name: "Indiana", type: "property", price: "$220", color: "#e84343" },
        { name: "Illinois", type: "property", price: "$240", color: "#e84343" },
        { name: "B&O RR", type: "property", price: "$200", color: "#111827" },
        { name: "Atlantic", type: "property", price: "$260", color: "#f0d000" },
        { name: "Ventnor", type: "property", price: "$260", color: "#f0d000" },
        { name: "Water Works", type: "property", price: "$150", color: "#f3c623" },
        { name: "Marvin", type: "property", price: "$280", color: "#f0d000" },
        { name: "Go To Jail", type: "jail", price: "" },
        { name: "Pacific", type: "property", price: "$300", color: "#1ea85b" },
        { name: "North Carolina", type: "property", price: "$300", color: "#1ea85b" },
        { name: "Community", type: "chance", price: "" },
        { name: "Pennsylvania", type: "property", price: "$320", color: "#1ea85b" },
        { name: "Luxury Tax", type: "tax", price: "$100" },
        { name: "Boardwalk", type: "property", price: "$400", color: "#2563eb" }
    ];

    const players = [
        { id: 1, username: "Player1", money: 2500, color: "#ff5d73", avatar: "P1", position: 0 },
        { id: 2, username: "Player2", money: 1800, color: "#32b8ff", avatar: "P2", position: 7 },
        { id: 3, username: "Player3", money: 3200, color: "#ffd44d", avatar: "P3", position: 18 },
        { id: 4, username: "Player4", money: 1400, color: "#b86bff", avatar: "P4", position: 28 }
    ];

    const chatMessages = [
        { user: "Player1", message: "hello" },
        { user: "Player2", message: "let's start" }
    ];

    let activePlayerIndex = 0;

    const getBoardPlacement = (index) => {
        if (index <= 9) {
            return { row: 11, col: 11 - index };
        }
        if (index <= 18) {
            return { row: 20 - index, col: 1 };
        }
        if (index <= 27) {
            return { row: 1, col: index - 17 };
        }
        return { row: index - 26, col: 11 };
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
        boardElement.innerHTML = cells.map((cell, index) => {
            const placement = getBoardPlacement(index);
            return `
            <div
                class="board-cell ${getCellClass(cell)}"
                data-cell-index="${index}"
                style="grid-row:${placement.row};grid-column:${placement.col};"
            >
                <span class="cell-color" style="background:${cell.color || "transparent"}"></span>
                <strong>${cell.name}</strong>
                <small>${cell.price || cell.type.toUpperCase()}</small>
            </div>
        `;
        }).join("");
    };

    const buildPlayerCards = () => {
        players.forEach((player, index) => {
            const card = document.getElementById(`playerCard${index + 1}`);
            if (!card) {
                return;
            }

            card.innerHTML = `
                <div class="player-chip" style="background:${player.color}">${player.avatar}</div>
                <div class="player-meta">
                    <strong>${player.username}</strong>
                    <span>$${player.money.toLocaleString("en-US")}</span>
                </div>
            `;
        });
        currentTurnLabel.textContent = players[activePlayerIndex].username;
    };

    const renderChat = () => {
        chatMessagesElement.innerHTML = chatMessages.map((item) => `
            <article class="chat-bubble">
                <strong>${item.user}</strong>
                <p>${item.message}</p>
            </article>
        `).join("");
        chatMessagesElement.scrollTop = chatMessagesElement.scrollHeight;
    };

    const getCellRect = (index) => {
        const cell = boardElement.querySelector(`[data-cell-index="${index}"]`);
        if (!cell) {
            return null;
        }
        const boardRect = boardElement.getBoundingClientRect();
        const cellRect = cell.getBoundingClientRect();
        return {
            left: cellRect.left - boardRect.left,
            top: cellRect.top - boardRect.top,
            width: cellRect.width,
            height: cellRect.height
        };
    };

    const renderTokens = () => {
        tokensElement.innerHTML = players.map((player) => {
            const rect = getCellRect(player.position);
            if (!rect) {
                return "";
            }
            return `
                <div
                    class="player-token"
                    style="
                        --token-color:${player.color};
                        left:${rect.left + rect.width / 2 - 16}px;
                        top:${rect.top + rect.height / 2 - 16}px;
                    "
                >
                    ${player.id}
                </div>
            `;
        }).join("");
    };

    const animateDice = () => {
        die1.classList.add("rolling");
        die2.classList.add("rolling");

        let tick = 0;
        const rolling = window.setInterval(() => {
            tick += 1;
            die1.textContent = String((tick % 6) + 1);
            die2.textContent = String(((tick + 2) % 6) + 1);
        }, 90);

        window.setTimeout(() => {
            window.clearInterval(rolling);
            die1.classList.remove("rolling");
            die2.classList.remove("rolling");

            const value1 = 3;
            const value2 = 5;
            die1.textContent = String(value1);
            die2.textContent = String(value2);
            diceTotal.textContent = `Tong: ${value1 + value2}`;

            const activePlayer = players[activePlayerIndex];
            activePlayer.position = (activePlayer.position + value1 + value2) % cells.length;
            renderTokens();

            activePlayerIndex = (activePlayerIndex + 1) % players.length;
            currentTurnLabel.textContent = players[activePlayerIndex].username;
        }, 1100);
    };

    rollDiceButton.addEventListener("click", animateDice);

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

    buildBoard();
    buildPlayerCards();
    renderChat();
    window.setTimeout(renderTokens, 50);
    window.addEventListener("resize", renderTokens);
});
