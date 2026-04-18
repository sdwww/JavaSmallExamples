package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GomokuGame 游戏逻辑测试")
class GomokuGameTest {

    @Test
    @DisplayName("玩家落子")
    void testPlayerMove() {
        GomokuGame game = new GomokuGame();
        assertTrue(game.makeMove(7, 7));
        assertEquals(1, game.getMoveCount());
        assertEquals(GomokuBoard.WHITE, game.getCurrentPlayer());
    }

    @Test
    @DisplayName("AI落子")
    void testAiMove() {
        GomokuGame game = new GomokuGame();
        game.makeMove(7, 7);
        int[] aiMove = game.aiMove();
        assertNotNull(aiMove);
        assertEquals(2, game.getMoveCount());
        assertEquals(GomokuBoard.BLACK, game.getCurrentPlayer());
    }

    @Test
    @DisplayName("重复位置落子")
    void testDuplicateMove() {
        GomokuGame game = new GomokuGame();
        game.makeMove(7, 7);
        assertFalse(game.makeMove(7, 7));
    }

    @Test
    @DisplayName("重置游戏")
    void testReset() {
        GomokuGame game = new GomokuGame();
        game.makeMove(7, 7);
        game.aiMove();
        assertEquals(2, game.getMoveCount());
        game.reset();
        assertEquals(0, game.getMoveCount());
        assertFalse(game.isGameOver());
    }

    @Test
    @DisplayName("悔棋")
    void testUndoMove() {
        GomokuGame game = new GomokuGame();
        game.makeMove(7, 7);
        game.aiMove();
        assertEquals(2, game.getMoveCount());
        assertTrue(game.undoMove());
        assertEquals(0, game.getMoveCount());
    }

    @Test
    @DisplayName("设置先手方")
    void testSetFirstPlayer() {
        GomokuGame game = new GomokuGame();
        game.setFirstPlayer(GomokuBoard.WHITE);
        assertEquals(GomokuBoard.WHITE, game.getCurrentPlayer());
    }

    @Test
    @DisplayName("切换难度")
    void testChangeDifficulty() {
        GomokuGame game = new GomokuGame(Difficulty.EASY);
        assertEquals(Difficulty.EASY, game.getDifficulty());
        game.setDifficulty(Difficulty.HARD);
        assertEquals(Difficulty.HARD, game.getDifficulty());
    }
}
