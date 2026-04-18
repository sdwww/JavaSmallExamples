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
    @DisplayName("评估位置价值")
    void testEvaluatePosition() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        int posScore = evaluator.evaluatePosition(board, 7, 9, GomokuBoard.BLACK);
        assertTrue(posScore > 0, "延伸二连应该有正分");
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
    @DisplayName("检测活三")
    void testIsLiveThree() {
        int[][] board = new int[15][15];
        board[7][6] = GomokuBoard.BLACK;
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        assertTrue(evaluator.isLiveThree(board, 7, 7, GomokuBoard.BLACK));
    }
}
