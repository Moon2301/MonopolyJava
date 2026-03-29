package com.game.monopoly.service;

import com.game.monopoly.dto.GameActionResponse;
import com.game.monopoly.dto.GameStateResponse;
import com.game.monopoly.dto.StartBotGameRequest;
import com.game.monopoly.dto.StartBotGameResponse;
import com.game.monopoly.MonopolyGameRules;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.inGameData.PlayerProperty;
import com.game.monopoly.model.inGameData.PlayerPropertyId;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.MapCell;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GamePlayService {

    private static final long UPGRADE_BASE_COST = 100L;
    private static final int HUMAN_WAIT_ROLL_SECONDS = 25;
    private static final int HUMAN_ACTION_SECONDS = 35;

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPropertyRepository playerPropertyRepository;
    private final BoardCellRepository boardCellRepository;
    private final MapCellRepository mapCellRepository;
    private final BoardClassicMapBootstrapService boardClassicMapBootstrapService;
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final HeroRepository heroRepository;
    private final Random random = new Random();

    private final Map<Long, Integer[]> lastDiceByGame = new HashMap<>();
    private final Map<Long, String> botDifficultyByGame = new HashMap<>();
    /** Trả về trong getState một lần rồi xóa — để client hiển thị chat hệ thống. */
    private final Map<Long, List<GameStateResponse.RentNoticeDto>> pendingRentNotices = new ConcurrentHashMap<>();

    @Transactional
    public StartBotGameResponse startBotGame(Long accountId, StartBotGameRequest request) {
        UserProfile profile = getProfileByAccountId(accountId);
        String difficulty = normalizeDifficulty(request != null ? request.getDifficulty() : null);
        int botCount = 1;
        if (request != null && request.getBotCount() != null) {
            botCount = Math.min(3, Math.max(1, request.getBotCount()));
        }
        int maxPlayers = 1 + botCount;

        Game game = Game.builder()
                .mapId(1)
                .createdBy(accountId)
                .status(GameStatus.PLAYING)
                .maxPlayers(maxPlayers)
                .currentTurn(1)
                .currentPlayerOrder(1)
                .turnState("WAIT_ROLL")
                .version(1)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .humanTurnStartedAt(LocalDateTime.now())
                .build();
        game = gameRepository.save(game);

        Integer heroId = request != null ? request.getHeroId() : null;
        if (heroId != null && heroRepository.findById(heroId).isEmpty()) {
            heroId = null;
        }

        GamePlayer human = GamePlayer.builder()
                .gameId(game.getGameId())
                .userProfileId(profile.getUserProfileId())
                .characterId(heroId)
                .turnOrder(1)
                .balance(MonopolyGameRules.IN_GAME_STARTING_BALANCE)
                .position(0)
                .isBankrupt(false)
                .isBot(false)
                .build();

        List<GamePlayer> toSave = new ArrayList<>();
        toSave.add(human);
        long botStartBalance =
                "hard".equals(difficulty)
                        ? MonopolyGameRules.IN_GAME_STARTING_BALANCE + MonopolyGameRules.IN_GAME_BOT_HARD_EXTRA
                        : MonopolyGameRules.IN_GAME_STARTING_BALANCE;
        for (int b = 1; b <= botCount; b++) {
            GamePlayer bot = GamePlayer.builder()
                    .gameId(game.getGameId())
                    .userProfileId(null)
                    .characterId(null)
                    .turnOrder(1 + b)
                    .balance(botStartBalance)
                    .position(0)
                    .isBankrupt(false)
                    .isBot(true)
                    .build();
            toSave.add(bot);
        }

        gamePlayerRepository.saveAll(toSave);
        botDifficultyByGame.put(game.getGameId(), difficulty);

        return StartBotGameResponse.builder()
                .gameId(game.getGameId())
                .difficulty(difficulty)
                .botCount(botCount)
                .redirectUrl(
                        "/game-board?gameId="
                                + game.getGameId()
                                + "&vsBot=1&difficulty="
                                + difficulty
                                + "&bots="
                                + botCount)
                .build();
    }

    @Transactional
    public GameStateResponse getState(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        ensureHumanTurnClockStarted(game);
        maybeResolveExpiredHumanTurn(game);

        List<GamePlayer> players = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(gameId);
        Integer[] dice = lastDiceByGame.getOrDefault(gameId, new Integer[]{null, null});
        GamePlayer currentPlayer = getCurrentTurnPlayer(game);
        BoardCell currentCell = getCellByPosition(game, currentPlayer.getPosition());
        Optional<PlayerProperty> currentCellProperty = playerPropertyRepository
                .findByGame_GameIdAndBoardCell_CellId(gameId, currentCell.getCellId());

        Integer turnSecondsRemaining = computeTurnSecondsRemaining(game, currentPlayer);
        Boolean myTurn = computeMyTurn(accountId, currentPlayer);

        List<GameStateResponse.RentNoticeDto> rentNotices =
                Optional.ofNullable(pendingRentNotices.remove(gameId)).orElseGet(List::of);

        return GameStateResponse.builder()
                .gameId(game.getGameId())
                .status(game.getStatus().name())
                .currentTurn(game.getCurrentTurn())
                .currentPlayerOrder(game.getCurrentPlayerOrder())
                .turnState(game.getTurnState())
                .lastDice1(dice[0])
                .lastDice2(dice[1])
                .turnSecondsRemaining(turnSecondsRemaining)
                .myTurn(myTurn)
                .totalBoardCells(getBoardCellCount(game))
                .currentCell(toCellInfo(currentCell, currentCellProperty.orElse(null), currentPlayer, game.getTurnState()))
                .players(players.stream().map(this::toPlayerState).toList())
                .ownedCells(buildOwnedCellsSnapshot(game))
                .rentNotices(rentNotices)
                .build();
    }

    private List<GameStateResponse.OwnedCellDto> buildOwnedCellsSnapshot(Game game) {
        Long gameId = game.getGameId();
        List<BoardCell> ordered = listBoardCellsInPlayOrder(game);
        List<PlayerProperty> props = playerPropertyRepository.findByGame_GameId(gameId);
        Map<Integer, PlayerProperty> byCellId = new HashMap<>(Math.max(16, props.size() * 2));
        for (PlayerProperty pp : props) {
            if (pp.getBoardCell() != null) {
                byCellId.put(pp.getBoardCell().getCellId(), pp);
            }
        }
        List<GameStateResponse.OwnedCellDto> out = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            BoardCell bc = ordered.get(i);
            PlayerProperty pp = byCellId.get(bc.getCellId());
            if (pp != null
                    && pp.getOwnerPlayer() != null
                    && pp.getOwnerPlayer().getTurnOrder() != null) {
                out.add(
                        GameStateResponse.OwnedCellDto.builder()
                                .boardIndex(i)
                                .ownerTurnOrder(pp.getOwnerPlayer().getTurnOrder())
                                .build());
            }
        }
        return out;
    }

    @Transactional
    public GameActionResponse rollDice(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        if (!"WAIT_ROLL".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Lượt hiện tại không thể tung xúc xắc");
        }

        RollDiceOutcome rolled = performRollAndMove(game, player);
        String message = "Bạn tung được " + rolled.d1() + " + " + rolled.d2();
        if (rolled.passGoBonus() > 0) {
            message += " · Qua ô xuất phát +" + rolled.passGoBonus();
        }
        maybeAutoRunBotTurns(game);
        return actionResult(game.getGameId(), message, accountId);
    }

    @Transactional
    public GameActionResponse buyCurrentCell(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        if (!"ACTION_REQUIRED".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Không thể mua ô ở thời điểm hiện tại");
        }

        BoardCell cell = getCellByPosition(game, player.getPosition());
        if (!isPurchasableCell(cell)) {
            throw new RuntimeException("Ô hiện tại không thể mua");
        }

        Optional<PlayerProperty> existing = playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gameId, cell.getCellId());
        if (existing.isPresent() && existing.get().getOwnerPlayer() != null) {
            throw new RuntimeException("Ô này đã có chủ");
        }

        long price = getCellPrice(cell);
        if (player.getBalance() < price) {
            throw new RuntimeException("Không đủ tiền để mua ô");
        }

        player.setBalance(player.getBalance() - price);
        gamePlayerRepository.save(player);

        PlayerProperty property = existing.orElseGet(() -> {
            PlayerProperty pp = new PlayerProperty();
            pp.setId(new PlayerPropertyId(gameId, cell.getCellId()));
            pp.setGame(game);
            pp.setBoardCell(cell);
            pp.setHouseLevel(0);
            return pp;
        });
        property.setOwnerPlayer(player);
        playerPropertyRepository.save(property);

        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        maybeAutoRunBotTurns(game);
        return actionResult(gameId, "Mua ô " + cell.getName() + " thành công", accountId);
    }

    @Transactional
    public GameActionResponse upgradeCurrentCell(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        if (!"ACTION_REQUIRED".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Không thể nâng cấp ở thời điểm hiện tại");
        }

        BoardCell cell = getCellByPosition(game, player.getPosition());
        PlayerProperty property = playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gameId, cell.getCellId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa sở hữu ô này"));

        if (property.getOwnerPlayer() == null || !Objects.equals(property.getOwnerPlayer().getGamePlayerId(), player.getGamePlayerId())) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể nâng cấp");
        }

        int maxLevel = cell.getMaxHouseLevel() == null ? 5 : cell.getMaxHouseLevel();
        int currentLevel = property.getHouseLevel() == null ? 0 : property.getHouseLevel();
        if (currentLevel >= maxLevel) {
            throw new RuntimeException("Ô đã đạt cấp tối đa");
        }

        long upgradeCost = Math.max(UPGRADE_BASE_COST, (long) (getCellPrice(cell) * 0.5));
        if (player.getBalance() < upgradeCost) {
            throw new RuntimeException("Không đủ tiền để nâng cấp");
        }

        player.setBalance(player.getBalance() - upgradeCost);
        property.setHouseLevel(currentLevel + 1);
        gamePlayerRepository.save(player);
        playerPropertyRepository.save(property);

        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        maybeAutoRunBotTurns(game);
        return actionResult(gameId, "Nâng cấp ô " + cell.getName() + " lên cấp " + property.getHouseLevel(), accountId);
    }

    @Transactional
    public GameActionResponse endTurn(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        if ("WAIT_ROLL".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Hãy tung xúc xắc trước khi kết thúc lượt");
        }

        advanceTurn(game);
        maybeAutoRunBotTurns(game);
        return actionResult(gameId, "Kết thúc lượt", accountId);
    }

    private record RollDiceOutcome(int d1, int d2, long passGoBonus) {
        Integer[] dicePair() {
            return new Integer[]{d1, d2};
        }
    }

    private RollDiceOutcome performRollAndMove(Game game, GamePlayer player) {
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        lastDiceByGame.put(game.getGameId(), new Integer[]{d1, d2});

        int boardCells = getBoardCellCount(game);
        int oldPos = player.getPosition() == null ? 0 : player.getPosition();
        int delta = d1 + d2;
        int raw = oldPos + delta;
        int nextPos = boardCells == 0 ? 0 : raw % boardCells;

        long balance = player.getBalance() == null ? 0L : player.getBalance();
        long laps = boardCells <= 0 ? 0L : raw / boardCells;
        long passGoBonus = laps * MonopolyGameRules.PASS_GO_BONUS;
        player.setBalance(balance + passGoBonus);
        player.setPosition(nextPos);
        gamePlayerRepository.save(player);

        applyLandingEffect(game, player);
        game.setTurnState("ACTION_REQUIRED");
        if (!Boolean.TRUE.equals(player.getIsBot())) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
        } else {
            game.setHumanTurnStartedAt(null);
        }
        gameRepository.save(game);
        return new RollDiceOutcome(d1, d2, passGoBonus);
    }

    private void applyLandingEffect(Game game, GamePlayer currentPlayer) {
        BoardCell cell = getCellByPosition(game, currentPlayer.getPosition());
        Optional<PlayerProperty> pp =
                playerPropertyRepository.findByGameIdAndCellIdWithOwner(game.getGameId(), cell.getCellId());
        if (pp.isEmpty()) {
            return;
        }

        PlayerProperty property = pp.get();
        GamePlayer owner = property.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        if (Objects.equals(owner.getGamePlayerId(), currentPlayer.getGamePlayerId())) {
            return;
        }
        if (Boolean.TRUE.equals(owner.getIsBankrupt())) {
            return;
        }

        long rent = calculateRent(cell, property.getHouseLevel());
        long payerBalance = currentPlayer.getBalance() == null ? 0L : currentPlayer.getBalance();
        long ownerBalance = owner.getBalance() == null ? 0L : owner.getBalance();

        long paid = Math.min(payerBalance, rent);
        currentPlayer.setBalance(payerBalance - paid);
        owner.setBalance(ownerBalance + paid);
        if (currentPlayer.getBalance() <= 0) {
            currentPlayer.setIsBankrupt(true);
        }
        gamePlayerRepository.save(currentPlayer);
        gamePlayerRepository.save(owner);

        if (paid > 0) {
            String cellLabel = cell.getName() != null && !cell.getName().isBlank() ? cell.getName() : "Ô";
            GameStateResponse.RentNoticeDto notice =
                    GameStateResponse.RentNoticeDto.builder()
                            .payerName(displayNameForPlayer(currentPlayer))
                            .amountPaid(paid)
                            .cellName(cellLabel)
                            .ownerName(displayNameForPlayer(owner))
                            .build();
            pendingRentNotices
                    .compute(game.getGameId(), (k, v) -> {
                        List<GameStateResponse.RentNoticeDto> list =
                                v != null ? v : new ArrayList<>();
                        list.add(notice);
                        return list;
                    });
        }
    }

    private long calculateRent(BoardCell cell, Integer houseLevel) {
        long base = cell.getBaseRent() == null ? Math.max(20, getCellPrice(cell) / 5) : cell.getBaseRent();
        int level = houseLevel == null ? 0 : houseLevel;
        return base * (1L + level);
    }

    private void maybeAutoRunBotTurns(Game game) {
        int guard = 0;
        while (guard < 48) {
            guard++;
            GamePlayer current = getCurrentTurnPlayer(game);
            if (!Boolean.TRUE.equals(current.getIsBot())) {
                break;
            }
            runBotTurn(game, current);
        }
    }

    private void runBotTurn(Game game, GamePlayer botPlayer) {
        String difficulty = botDifficultyByGame.getOrDefault(game.getGameId(), "easy");
        performRollAndMove(game, botPlayer);

        BoardCell cell = getCellByPosition(game, botPlayer.getPosition());
        Optional<PlayerProperty> existing = playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(game.getGameId(), cell.getCellId());

        if (isPurchasableCell(cell) && (existing.isEmpty() || existing.get().getOwnerPlayer() == null)) {
            boolean shouldBuy = "hard".equals(difficulty) || random.nextInt(100) < 55;
            if (shouldBuy && botPlayer.getBalance() >= getCellPrice(cell)) {
                PlayerProperty pp = existing.orElseGet(() -> {
                    PlayerProperty p = new PlayerProperty();
                    p.setId(new PlayerPropertyId(game.getGameId(), cell.getCellId()));
                    p.setGame(game);
                    p.setBoardCell(cell);
                    p.setHouseLevel(0);
                    return p;
                });
                botPlayer.setBalance(botPlayer.getBalance() - getCellPrice(cell));
                pp.setOwnerPlayer(botPlayer);
                gamePlayerRepository.save(botPlayer);
                playerPropertyRepository.save(pp);
            }
        } else if (existing.isPresent() && existing.get().getOwnerPlayer() != null
                && Objects.equals(existing.get().getOwnerPlayer().getGamePlayerId(), botPlayer.getGamePlayerId())) {
            int level = existing.get().getHouseLevel() == null ? 0 : existing.get().getHouseLevel();
            int max = cell.getMaxHouseLevel() == null ? 5 : cell.getMaxHouseLevel();
            long cost = Math.max(UPGRADE_BASE_COST, (long) (getCellPrice(cell) * 0.5));
            boolean shouldUpgrade = "hard".equals(difficulty) ? random.nextInt(100) < 70 : random.nextInt(100) < 35;
            if (level < max && shouldUpgrade && botPlayer.getBalance() >= cost) {
                botPlayer.setBalance(botPlayer.getBalance() - cost);
                existing.get().setHouseLevel(level + 1);
                gamePlayerRepository.save(botPlayer);
                playerPropertyRepository.save(existing.get());
            }
        }

        advanceTurn(game);
    }

    private void advanceTurn(Game game) {
        List<GamePlayer> players = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(game.getGameId());
        if (players.isEmpty()) {
            throw new RuntimeException("Game has no players");
        }

        List<GamePlayer> activePlayers = players.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsBankrupt()))
                .toList();
        if (activePlayers.size() <= 1) {
            game.setStatus(GameStatus.FINISHED);
            game.setTurnState("END_TURN");
            if (!activePlayers.isEmpty()) {
                game.setWinnerPlayerId(activePlayers.get(0).getGamePlayerId());
            }
            game.setEndedAt(LocalDateTime.now());
            game.setHumanTurnStartedAt(null);
            gameRepository.save(game);
            return;
        }

        int currentOrder = game.getCurrentPlayerOrder() == null ? 1 : game.getCurrentPlayerOrder();
        int nextOrder = currentOrder;
        for (int i = 0; i < players.size(); i++) {
            nextOrder = nextOrder >= players.size() ? 1 : nextOrder + 1;
            GamePlayer candidate = null;
            for (GamePlayer p : players) {
                if (Objects.equals(p.getTurnOrder(), nextOrder)) {
                    candidate = p;
                    break;
                }
            }
            if (candidate != null && !Boolean.TRUE.equals(candidate.getIsBankrupt())) {
                break;
            }
        }

        game.setCurrentPlayerOrder(nextOrder);
        game.setCurrentTurn((game.getCurrentTurn() == null ? 1 : game.getCurrentTurn()) + 1);
        game.setTurnState("WAIT_ROLL");

        GamePlayer nextPlayer = getCurrentTurnPlayer(game);
        if (!Boolean.TRUE.equals(nextPlayer.getIsBot())) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
        } else {
            game.setHumanTurnStartedAt(null);
        }
        gameRepository.save(game);
    }

    /**
     * Ô bàn cờ theo vị trí (0..n-1): ưu tiên thứ tự {@link MapCell} của map trong game,
     * nếu chưa cấu hình map thì dùng {@link BoardCell} trong DB sắp xếp theo {@code cellId}.
     * Luôn trả về entity đã persist để FK {@code PlayerProperty} hợp lệ.
     */
    private BoardCell getCellByPosition(Game game, Integer position) {
        List<BoardCell> ordered = listBoardCellsInPlayOrder(game);
        int safePos = (position == null ? 0 : position) % ordered.size();
        return ordered.get(safePos);
    }

    private List<BoardCell> listBoardCellsInPlayOrder(Game game) {
        Integer mapId = game.getMapId() != null ? game.getMapId() : 1;
        List<MapCell> mapCells = mapCellRepository.findAllByMapIdOrderByPositionWithCell(mapId);
        if (mapCells.size() < BoardClassicMapBootstrapService.EXPECTED_CELLS) {
            boardClassicMapBootstrapService.ensureClassicBoardIfMissing();
            mapCells = mapCellRepository.findAllByMapIdOrderByPositionWithCell(mapId);
        }
        if (!mapCells.isEmpty()) {
            List<BoardCell> out = new ArrayList<>(mapCells.size());
            for (MapCell mc : mapCells) {
                if (mc.getBoardCell() != null) {
                    out.add(mc.getBoardCell());
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        List<BoardCell> cells = boardCellRepository.findAll(Sort.by(Sort.Direction.ASC, "cellId"));
        if (cells.isEmpty()) {
            throw new RuntimeException(
                    "Bàn cờ chưa có dữ liệu ô (BoardCell). Kiểm tra file classpath seed/board-classic.json hoặc static/seed/board-classic.json.");
        }
        return cells;
    }

    private int getBoardCellCount(Game game) {
        return listBoardCellsInPlayOrder(game).size();
    }

    private long getCellPrice(BoardCell cell) {
        return cell.getPrice() == null ? 200L : Math.max(cell.getPrice(), 1);
    }

    private boolean isPurchasableCell(BoardCell cell) {
        if (cell.getName() != null && "GO".equalsIgnoreCase(cell.getName().trim())) {
            return false;
        }
        if (cell.getType() == null) {
            return true;
        }
        String type = cell.getType().toUpperCase(Locale.ROOT);
        if (type.contains("START") || "GO".equals(type)) {
            return false;
        }
        return type.contains("PROPERTY") || type.contains("LAND");
    }

    private void validateHumanTurn(GamePlayer currentTurnPlayer, Long accountId) {
        UserProfile profile = getProfileByAccountId(accountId);
        if (Boolean.TRUE.equals(currentTurnPlayer.getIsBot())) {
            throw new RuntimeException("Đây là lượt của bot");
        }
        if (!Objects.equals(currentTurnPlayer.getUserProfileId(), profile.getUserProfileId())) {
            throw new RuntimeException("Không phải lượt của bạn");
        }
    }

    private GamePlayer getCurrentTurnPlayer(Game game) {
        Integer order = game.getCurrentPlayerOrder();
        if (order == null) {
            throw new RuntimeException("Game currentPlayerOrder is missing");
        }
        return gamePlayerRepository.findByGameIdAndTurnOrder(game.getGameId(), order)
                .orElseThrow(() -> new RuntimeException("Current turn player not found"));
    }

    private Game getGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }

    private UserProfile getProfileByAccountId(Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("Missing X-Account-Id header");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    private String normalizeDifficulty(String raw) {
        if (raw == null) return "easy";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "hard".equals(value) ? "hard" : "easy";
    }

    private GameActionResponse actionResult(Long gameId, String message, Long accountId) {
        return GameActionResponse.builder()
                .message(message)
                .state(getState(gameId, accountId))
                .build();
    }

    private void ensureHumanTurnClockStarted(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        GamePlayer current = getCurrentTurnPlayer(game);
        if (Boolean.TRUE.equals(current.getIsBot())) {
            return;
        }
        if (game.getHumanTurnStartedAt() == null) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
            gameRepository.save(game);
        }
    }

    private void maybeResolveExpiredHumanTurn(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        GamePlayer cur = getCurrentTurnPlayer(game);
        if (Boolean.TRUE.equals(cur.getIsBot())) {
            return;
        }
        if (game.getHumanTurnStartedAt() == null) {
            return;
        }
        String ts = game.getTurnState();
        int limit;
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            limit = HUMAN_WAIT_ROLL_SECONDS;
        } else if ("ACTION_REQUIRED".equalsIgnoreCase(ts)) {
            limit = HUMAN_ACTION_SECONDS;
        } else {
            return;
        }
        long elapsed = ChronoUnit.SECONDS.between(game.getHumanTurnStartedAt(), LocalDateTime.now());
        if (elapsed < limit) {
            return;
        }
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            performRollAndMove(game, cur);
            maybeAutoRunBotTurns(game);
        } else {
            advanceTurn(game);
            maybeAutoRunBotTurns(game);
        }
    }

    private Integer computeTurnSecondsRemaining(Game game, GamePlayer currentPlayer) {
        if (game.getStatus() != GameStatus.PLAYING || Boolean.TRUE.equals(currentPlayer.getIsBot())) {
            return null;
        }
        if (game.getHumanTurnStartedAt() == null) {
            return null;
        }
        String ts = game.getTurnState();
        int limit;
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            limit = HUMAN_WAIT_ROLL_SECONDS;
        } else if ("ACTION_REQUIRED".equalsIgnoreCase(ts)) {
            limit = HUMAN_ACTION_SECONDS;
        } else {
            return null;
        }
        long elapsed = ChronoUnit.SECONDS.between(game.getHumanTurnStartedAt(), LocalDateTime.now());
        return (int) Math.max(0, limit - elapsed);
    }

    private Boolean computeMyTurn(Long accountId, GamePlayer currentPlayer) {
        if (accountId == null || Boolean.TRUE.equals(currentPlayer.getIsBot())) {
            return accountId == null ? null : Boolean.FALSE;
        }
        try {
            UserProfile profile = getProfileByAccountId(accountId);
            return Objects.equals(currentPlayer.getUserProfileId(), profile.getUserProfileId());
        } catch (RuntimeException ignored) {
            return Boolean.FALSE;
        }
    }

    private String displayNameForPlayer(GamePlayer player) {
        if (Boolean.TRUE.equals(player.getIsBot())) {
            return "Bot " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
        }
        if (player.getUserProfileId() != null) {
            UserProfile profile = userProfileRepository.findById(player.getUserProfileId()).orElse(null);
            if (profile != null && profile.getUsername() != null && !profile.getUsername().isBlank()) {
                return profile.getUsername();
            }
        }
        return "Người chơi " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
    }

    private GameStateResponse.PlayerStateDto toPlayerState(GamePlayer player) {
        String username = displayNameForPlayer(player);
        String avatarUrl = null;
        String heroImageUrl = null;
        String heroName = null;

        if (!Boolean.TRUE.equals(player.getIsBot()) && player.getUserProfileId() != null) {
            UserProfile profile = userProfileRepository.findById(player.getUserProfileId()).orElse(null);
            if (profile != null) {
                avatarUrl = profile.getAvatarUrl();
            }
            if (player.getCharacterId() != null) {
                Optional<Hero> ho = heroRepository.findById(player.getCharacterId());
                if (ho.isPresent()) {
                    Hero h = ho.get();
                    heroName = h.getName();
                    heroImageUrl = null;
                }
            }
        }

        return GameStateResponse.PlayerStateDto.builder()
                .gamePlayerId(player.getGamePlayerId())
                .userProfileId(player.getUserProfileId())
                .turnOrder(player.getTurnOrder())
                .position(player.getPosition())
                .balance(player.getBalance())
                .isBot(player.getIsBot())
                .isBankrupt(player.getIsBankrupt())
                .username(username)
                .avatarUrl(avatarUrl)
                .heroImageUrl(heroImageUrl)
                .heroName(heroName)
                .build();
    }

    private GameStateResponse.CellInfoDto toCellInfo(BoardCell cell, PlayerProperty property, GamePlayer currentPlayer, String turnState) {
        Long ownerGamePlayerId = null;
        Integer ownerTurnOrder = null;
        Integer houseLevel = 0;
        if (property != null) {
            houseLevel = property.getHouseLevel() == null ? 0 : property.getHouseLevel();
            if (property.getOwnerPlayer() != null) {
                ownerGamePlayerId = property.getOwnerPlayer().getGamePlayerId();
                ownerTurnOrder = property.getOwnerPlayer().getTurnOrder();
            }
        }

        long price = getCellPrice(cell);
        long upgradeCost = Math.max(UPGRADE_BASE_COST, (long) (price * 0.5));
        boolean actionPhase = "ACTION_REQUIRED".equalsIgnoreCase(turnState);
        boolean canBuy = actionPhase
                && isPurchasableCell(cell)
                && (property == null || property.getOwnerPlayer() == null)
                && currentPlayer.getBalance() != null
                && currentPlayer.getBalance() >= price;
        boolean canUpgrade = actionPhase
                && property != null
                && property.getOwnerPlayer() != null
                && Objects.equals(property.getOwnerPlayer().getGamePlayerId(), currentPlayer.getGamePlayerId())
                && currentPlayer.getBalance() != null
                && currentPlayer.getBalance() >= upgradeCost
                && (cell.getMaxHouseLevel() == null || houseLevel < cell.getMaxHouseLevel());

        return GameStateResponse.CellInfoDto.builder()
                .cellId(cell.getCellId())
                .name(cell.getName())
                .type(cell.getType())
                .price(price)
                .upgradeCost(upgradeCost)
                .estimatedRent(calculateRent(cell, houseLevel))
                .ownerGamePlayerId(ownerGamePlayerId)
                .ownerTurnOrder(ownerTurnOrder)
                .houseLevel(houseLevel)
                .canBuy(canBuy)
                .canUpgrade(canUpgrade)
                .build();
    }
}
