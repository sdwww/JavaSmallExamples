package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PatternEvaluator 棋型评估测试")
class PatternEvaluatorTest {

    private PatternEvaluator evaluator = PatternEvaluator.getInstance();

    @Test
    @DisplayName("评估五连")
    void testEvaluateFive() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 5; i++) {
            board.makeMove(7, i, GomokuBoard.WHITE);
        }
        int score = evaluator.evaluateBoard(board.getBoard());
        assertTrue(score > 0, "白方五连应该有正分");
    }

    @Test
    @DisplayName("评估活三")
    void testEvaluateLiveThree() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 6, GomokuBoard.WHITE);
        board.makeMove(7, 7, GomokuBoard.WHITE);
        board.makeMove(7, 8, GomokuBoard.WHITE);
        int score = evaluator.evaluateBoard(board.getBoard());
        assertTrue(score > 0);
    }

    @Test
    @DisplayName("评估活二")
    void testEvaluateLiveTwo() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 7, GomokuBoard.WHITE);
        board.makeMove(7, 8, GomokuBoard.WHITE);
        int score = evaluator.evaluateBoard(board.getBoard());
        assertTrue(score > 0);
    }

    @Test
    @DisplayName("评估位置价值")
    void testEvaluatePosition() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        int posScore = evaluator.evaluatePosition(board, 7, 9, GomokuBoard.BLACK);
        assertTrue(posScore > 0, "延伸二连应该有正分");
    }

    @Test
    @DisplayName("评估中心位置加分")
    void testCenterBonus() {
        int[][] board = new int[15][15];
        int centerScore = evaluator.evaluatePosition(board, 7, 7, GomokuBoard.BLACK);
        int edgeScore = evaluator.evaluatePosition(board, 0, 0, GomokuBoard.BLACK);
        assertTrue(centerScore > edgeScore, "中心位置应该比边缘位置分数高");
    }

    @Test
    @DisplayName("检测跳跃棋型")
    void testJumpPattern() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][9] = GomokuBoard.BLACK;
        board[7][10] = GomokuBoard.BLACK;
        board[7][11] = GomokuBoard.BLACK;
        int[] pattern = evaluator.analyzeLine(board, 7, 10, GomokuBoard.BLACK, 0, 1);
        int score = evaluator.getLineScore(pattern);
        assertTrue(score >= PatternEvaluator.SCORE_RUSH_FOUR, "跳跃四应该被检测");
    }

    @Test
    @DisplayName("获取线条分数")
    void testGetLineScore() {
        int[] fivePattern = {5, 5, 2, 0, 0};
        assertEquals(PatternEvaluator.SCORE_FIVE, evaluator.getLineScore(fivePattern));
    }

    @Test
    @DisplayName("检测活三")
    void testIsLiveThree() {
        int[][] board = new int[15][15];
        board[7][6] = GomokuBoard.BLACK;
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        assertTrue(evaluator.isLiveThree(board, 7, 7, GomokuBoard.BLACK));
    }

    @Test
    @DisplayName("眠三不是活三")
    void testSleepThreeIsNotLiveThree() {
        int[][] board = new int[15][15];
        board[7][5] = GomokuBoard.WHITE;
        board[7][6] = GomokuBoard.BLACK;
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        assertFalse(evaluator.isLiveThree(board, 7, 7, GomokuBoard.BLACK));
    }

    @Test
    @DisplayName("空棋盘评估")
    void testEmptyBoardEvaluation() {
        int[][] board = new int[15][15];
        assertEquals(0, evaluator.evaluateBoard(board));
    }

    @Test
    @DisplayName("验证有效空位")
    void testIsValidEmpty() {
        int[][] board = new int[15][15];
        assertTrue(evaluator.isValidEmpty(board, 7, 7));
        board[7][7] = GomokuBoard.BLACK;
        assertFalse(evaluator.isValidEmpty(board, 7, 7));
        assertFalse(evaluator.isValidEmpty(board, -1, 7));
        assertFalse(evaluator.isValidEmpty(board, 15, 7));
    }
}
