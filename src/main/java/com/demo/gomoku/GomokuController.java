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
        
        if (game.isGameOver()) {
            result.putAll(buildState(game, sessionId));
            result.put("success", false);
            result.put("error", "游戏已结束");
            return result;
        }

        if (game.getCurrentPlayer() != GomokuGame.BLACK) {
            result.putAll(buildState(game, sessionId));
            result.put("success", false);
            result.put("error", "等待AI落子");
            return result;
        }

        boolean playerSuccess = game.makeMove(row, col);
        result.put("playerMove", playerSuccess ? new int[]{row, col} : null);
        result.put("playerMoveSuccess", playerSuccess);

        if (!playerSuccess) {
            result.put("error", "无效的落子位置");
        }

        // AI落子
        int[] aiMove = null;
        if (playerSuccess && !game.isGameOver()) {
            aiMove = game.aiMove();
        }

        result.put("aiMove", aiMove);
        result.putAll(buildState(game, sessionId));

        return result;
    }

    /**
     * 重置游戏
     */
    @PostMapping("/reset/{sessionId}")
    public Map<String, Object> reset(@PathVariable String sessionId,
                                     @RequestParam(defaultValue = "2") int difficulty) {
        Difficulty diff = Difficulty.fromLevel(difficulty);
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
     * 构建状态响应
     */
    private Map<String, Object> buildState(GomokuGame game, String sessionId) {
        Map<String, Object> state = new HashMap<>();
        state.put("sessionId", sessionId);
        state.put("board", game.getBoard());
        state.put("currentPlayer", game.getCurrentPlayer());
        state.put("currentPlayerText", game.getCurrentPlayer() == GomokuGame.BLACK ? "黑方(你)" : "白方(AI)");
        state.put("gameOver", game.isGameOver());
        state.put("winner", game.getWinner());
        state.put("moveCount", game.getMoveCount());
        state.put("difficulty", game.getDifficulty().getLevel());

        if (game.isGameOver()) {
            if (game.getWinner() == GomokuGame.BLACK) {
                state.put("winnerText", "你赢了!");
            } else if (game.getWinner() == GomokuGame.WHITE) {
                state.put("winnerText", "AI获胜");
            } else {
                state.put("winnerText", "平局");
            }
        }

        return state;
    }
}
