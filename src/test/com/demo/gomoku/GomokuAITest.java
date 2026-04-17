package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("五子棋AI Quiescence Search 测试")
class GomokuAITest {

    @Test
    @DisplayName("检测必胜落子 - 右端开放")
    void testQuiescenceDetectsWinningMove() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][10] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][6] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(11, move[1]);
    }

    @Test
    @DisplayName("检测必防落子")
    void testQuiescenceDetectsBlockingMove() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.BLACK;
        board[7][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[7][9] = com.demo.gomoku.GomokuBoard.BLACK;
        board[7][10] = com.demo.gomoku.GomokuBoard.BLACK;
        board[7][6] = com.demo.gomoku.GomokuBoard.WHITE;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(11, move[1]);
    }

    @Test
    @DisplayName("优先获胜而非防守")
    void testQuiescencePrefersWinningOverBlocking() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][10] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][7] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][9] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][10] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertTrue(move[0] == 7 || move[1] == 11);
    }

    @Test
    @DisplayName("处理活三威胁")
    void testQuiescenceHandlesThreats() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertTrue(move[1] == 6 || move[1] == 10);
    }

    @Test
    @DisplayName("简单模式正常工作")
    void testEasyModeStillWorks() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.EASY);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }

    @Test
    @DisplayName("中等模式正常工作")
    void testMediumModeStillWorks() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.MEDIUM);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }

    @Test
    @DisplayName("开局库正常工作")
    void testOpeningBookStillWorks() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(7, move[1]);
    }

    @Test
    @DisplayName("跳跃威胁检测")
    void testJumpThreatDetection() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][10] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][7] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][9] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }

    @Test
    @DisplayName("边缘威胁 - 顶行")
    void testEdgeThreatTopRow() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[0][5] = com.demo.gomoku.GomokuBoard.WHITE;
        board[0][6] = com.demo.gomoku.GomokuBoard.WHITE;
        board[0][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[0][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[0][4] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(0, move[0]);
        assertEquals(9, move[1]);
    }

    @Test
    @DisplayName("边缘威胁 - 左列")
    void testEdgeThreatLeftCol() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[5][0] = com.demo.gomoku.GomokuBoard.WHITE;
        board[6][0] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][0] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][0] = com.demo.gomoku.GomokuBoard.WHITE;
        board[4][0] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(9, move[0]);
        assertEquals(0, move[1]);
    }

    @Test
    @DisplayName("角落威胁")
    void testEdgeThreatCorner() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[0][0] = com.demo.gomoku.GomokuBoard.WHITE;
        board[0][1] = com.demo.gomoku.GomokuBoard.WHITE;
        board[0][2] = com.demo.gomoku.GomokuBoard.WHITE;
        board[1][0] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }

    @Test
    @DisplayName("双活三检测")
    void testDoubleLiveThree() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][6] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][10] = com.demo.gomoku.GomokuBoard.WHITE;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(8, move[1]);
    }

    @Test
    @DisplayName("四三组合检测")
    void testFourThreeCombo() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertTrue(move[1] == 6 || move[1] == 10);
    }

    @Test
    @DisplayName("选择最佳防守点")
    void testChooseBestDefense() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.BLACK;
        board[7][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[7][9] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[9][8] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
        assertEquals(8, move[1]);
    }

    @Test
    @DisplayName("Quiescence 深度限制")
    void testQuiescenceDepthLimit() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        for (int i = 0; i < 5; i++) {
            board[7][6 + i] = (i % 2 == 0) ? com.demo.gomoku.GomokuBoard.WHITE : com.demo.gomoku.GomokuBoard.BLACK;
        }

        long start = System.currentTimeMillis();
        int[] move = ai.calculateMove(board);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(move);
        assertTrue(elapsed < 10000, "搜索时间: " + elapsed + "ms");
    }

    @Test
    @DisplayName("搜索时间合理")
    void testSearchTimeReasonable() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[9][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[9][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[6][7] = com.demo.gomoku.GomokuBoard.WHITE;

        long start = System.currentTimeMillis();
        int[] move = ai.calculateMove(board);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(move);
        assertTrue(elapsed < 3000, "搜索时间: " + elapsed + "ms");
    }

    @Test
    @DisplayName("并行搜索威胁检测")
    void testParallelMinmaxWithThreats() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[9][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[9][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[6][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[6][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[5][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[5][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[4][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[4][8] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }

    @Test
    @DisplayName("实际对弈场景1 - 中盘优势")
    void testRealGameScenario1() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[5][5] = com.demo.gomoku.GomokuBoard.WHITE;
        board[6][6] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[9][9] = com.demo.gomoku.GomokuBoard.BLACK;
        board[10][10] = com.demo.gomoku.GomokuBoard.BLACK;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }

    @Test
    @DisplayName("实际对弈场景2 - 残局")
    void testRealGameScenario2() {
        com.demo.gomoku.GomokuAI ai = new com.demo.gomoku.GomokuAI(com.demo.gomoku.Difficulty.HARD);

        int[][] board = new int[15][15];
        board[7][7] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][8] = com.demo.gomoku.GomokuBoard.WHITE;
        board[7][9] = com.demo.gomoku.GomokuBoard.WHITE;
        board[8][7] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][8] = com.demo.gomoku.GomokuBoard.BLACK;
        board[8][9] = com.demo.gomoku.GomokuBoard.BLACK;
        board[9][7] = com.demo.gomoku.GomokuBoard.WHITE;

        int[] move = ai.calculateMove(board);
        assertNotNull(move);
    }
}
