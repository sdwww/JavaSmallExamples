package com.demo.gomoku;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 五子棋游戏控制器（优化版）
 * 优化点：
 * 1. 使用 GameService 管理游戏状态，线程安全
 * 2. 使用 Difficulty 枚举替代魔法数字
 * 3. 简化代码逻辑
 */
@RestController
@RequestMapping("/api/gomoku")
@CrossOrigin(origins = "*")
public class GomokuController {

    @Autowired
    private GameService gameService;

    /**
     * 创建新游戏
     */
    @GetMapping("/new")
    public Map<String, Object> newGame(@RequestParam(defaultValue = "2") int difficulty) {
        Difficulty diff = Difficulty.fromLevel(difficulty);
        String sessionId = java.util.UUID.randomUUID().toString();
        GomokuGame game = gameService.getOrCreateGame(sessionId, diff); // 创建并注册游戏
        return buildState(game, sessionId);
    }

    /**
     * 获取游戏状态
     */
    @GetMapping("/state/{sessionId}")
    public Map<String, Object> getGameState(@PathVariable String sessionId,
                                            @RequestParam(defaultValue = "2") int difficulty) {
        Difficulty diff = Difficulty.fromLevel(difficulty);
        GomokuGame game = gameService.getOrCreateGame(sessionId, diff);
        return buildState(game, sessionId);
    }

    /**
     * 玩家落子（自动触发AI）
     */
    @PostMapping("/move/{sessionId}")
    public Map<String, Object> move(@PathVariable String sessionId,
                                    @RequestParam int row,
                                    @RequestParam int col,
                                    @RequestParam(defaultValue = "2") int difficulty) {
        Difficulty diff = Difficulty.fromLevel(difficulty);
        GomokuGame game = gameService.getOrCreateGame(sessionId, diff);

        Map<String, Object> result = new HashMap<>();
        
        // 默认值
        result.put("playerMove", null);
        result.put("playerMoveSuccess", false);
        result.put("aiMove", null);
        result.put("aiNeedPoll", false);
        
        // 参数边界检查
        if (row < 0 || row >= GomokuBoard.BOARD_SIZE || col < 0 || col >= GomokuBoard.BOARD_SIZE) {
            result.putAll(buildState(game, sessionId));
            result.put("error", "无效的落子位置");
            return result;
        }
        
        if (game.isGameOver()) {
            result.putAll(buildState(game, sessionId));
            result.put("error", "游戏已结束");
            return result;
        }

        if (game.getCurrentPlayer() != GomokuBoard.BLACK) {
            result.putAll(buildState(game, sessionId));
            result.put("error", "等待AI落子");
            return result;
        }

        boolean playerSuccess = game.makeMove(row, col);
        result.put("playerMove", playerSuccess ? new int[]{row, col} : null);
        result.put("playerMoveSuccess", playerSuccess);

        if (!playerSuccess) {
            result.put("error", "无效的落子位置");
        }

        // AI落子 - 立即启动异步计算，玩家落子后立即返回
        if (playerSuccess && !game.isGameOver()) {
            result.put("aiNeedPoll", true);
            gameService.startAsyncAiMove(sessionId); // 立即启动AI计算
        }
        result.putAll(buildState(game, sessionId));

        return result;
    }

    /**
     * AI异步落子（前端轮询获取，非阻塞）
     */
    @GetMapping("/ai-move/{sessionId}")
    public Map<String, Object> aiMove(@PathVariable String sessionId) {
        GomokuGame game = gameService.getGame(sessionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("aiMove", null);
        
        if (game == null) {
            result.put("error", "游戏不存在");
            result.put("sessionId", sessionId);
            result.put("board", new int[15][15]);
            result.put("currentPlayer", 1);
            result.put("gameOver", false);
            result.put("winnerText", "");
            return result;
        }
        
        // 非阻塞检查异步AI计算结果
        int[] aiMoveResult = gameService.getAsyncAiMoveResult(sessionId);
        if (aiMoveResult != null) {
            result.put("aiMove", aiMoveResult);
        }
        
        // 标记AI是否还在计算
        result.put("aiThinking", gameService.isAiThinking(sessionId));
        
        result.putAll(buildState(game, sessionId));
        return result;
    }

    /**
     * 重置游戏
     */
    @PostMapping("/reset/{sessionId}")
    public synchronized Map<String, Object> reset(@PathVariable String sessionId,
                                     @RequestParam(defaultValue = "2") int difficulty) {
        Difficulty diff = Difficulty.fromLevel(difficulty);
        gameService.cancelAiMove(sessionId); // 取消正在进行的AI计算
        gameService.resetGame(sessionId, diff);
        GomokuGame game = gameService.getGame(sessionId);
        return buildState(game, sessionId);
    }

    /**
     * 设置难度
     */
    @PostMapping("/difficulty/{sessionId}")
    public Map<String, Object> setDifficulty(@PathVariable String sessionId,
                                             @RequestParam int difficulty) {
        Difficulty diff = Difficulty.fromLevel(difficulty);
        gameService.setDifficulty(sessionId, diff);
        GomokuGame game = gameService.getGame(sessionId);
        return buildState(game, sessionId);
    }

    /**
     * 悔棋
     */
    @PostMapping("/undo/{sessionId}")
    public Map<String, Object> undo(@PathVariable String sessionId) {
        GomokuGame game = gameService.getGame(sessionId);
        Map<String, Object> result = new HashMap<>();
        
        if (game == null) {
            result.put("error", "游戏不存在");
            result.put("undoSuccess", false);
            return result;
        }
        
        boolean success = game.undoMove();
        if (success) {
            gameService.cancelAiMove(sessionId); // 取消正在进行的AI计算
        }
        result.put("undoSuccess", success);
        if (!success) {
            result.put("error", "无法悔棋（步数不足）");
        }
        result.putAll(buildState(game, sessionId));
        return result;
    }

    /**
     * 构建状态响应
     */
    private Map<String, Object> buildState(GomokuGame game, String sessionId) {
        Map<String, Object> state = new HashMap<>();
        state.put("sessionId", sessionId);
        state.put("board", game.getBoard());
        state.put("currentPlayer", game.getCurrentPlayer());
        state.put("currentPlayerText", game.getCurrentPlayer() == GomokuBoard.BLACK ? "黑方(你)" : "白方(AI)");
        state.put("gameOver", game.isGameOver());
        state.put("winner", game.getWinner());
        state.put("moveCount", game.getMoveCount());
        state.put("difficulty", game.getDifficulty().getLevel());
        state.put("moveHistory", game.getMoveHistoryText());
        state.put("canUndo", game.getMoveHistory().size() >= 2);

        // 确保 winnerText 始终存在
        if (game.isGameOver()) {
            if (game.getWinner() == GomokuBoard.BLACK) {
                state.put("winnerText", "你赢了！");
            } else if (game.getWinner() == GomokuBoard.WHITE) {
                state.put("winnerText", "AI获胜！");
            } else {
                state.put("winnerText", "平局！");
            }
        } else {
            state.put("winnerText", "");
        }

        return state;
    }
}
