package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GomokuAI 人工智能测试")
class GomokuAITest {

    private GomokuAI aiHard;
    private GomokuAI aiMedium;
    private GomokuAI aiEasy;

    @BeforeEach
    void setUp() {
        aiHard = new GomokuAI(Difficulty.HARD);
        aiMedium = new GomokuAI(Difficulty.MEDIUM);
        aiEasy = new GomokuAI(Difficulty.EASY);
    }

    @Test
    @DisplayName("检测必胜落子")
    void testQuiescenceWinningMove() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.WHITE;
        board[7][8] = GomokuBoard.WHITE;
        board[7][9] = GomokuBoard.WHITE;
        board[7][10] = GomokuBoard.WHITE;
        board[7][6] = GomokuBoard.BLACK;
        int[] move = aiHard.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(11, move[1]);
    }

    @Test
    @DisplayName("检测必防落子")
    void testQuiescenceBlockingMove() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        board[7][9] = GomokuBoard.BLACK;
        board[7][10] = GomokuBoard.BLACK;
        board[7][6] = GomokuBoard.WHITE;
        int[] move = aiHard.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(11, move[1]);
    }

    @Test
    @DisplayName("优先获胜而非防守")
    void testQuiescencePrefersWinning() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.WHITE;
        board[7][8] = GomokuBoard.WHITE;
        board[7][9] = GomokuBoard.WHITE;
        board[7][10] = GomokuBoard.WHITE;
        board[8][7] = GomokuBoard.BLACK;
        board[8][8] = GomokuBoard.BLACK;
        board[8][9] = GomokuBoard.BLACK;
        board[8][10] = GomokuBoard.BLACK;
        int[] move = aiHard.calculateMove(board);
        assertNotNull(move);
        assertTrue(move[0] == 7 || move[1] == 11);
    }

    @Test
    @DisplayName("双活三检测")
    void testDoubleLiveThree() {
        int[][] board = new int[15][15];
        board[7][6] = GomokuBoard.WHITE;
        board[7][7] = GomokuBoard.WHITE;
        board[7][9] = GomokuBoard.WHITE;
        board[7][10] = GomokuBoard.WHITE;
        int[] move = aiHard.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(8, move[1]);
    }

    @Test
    @DisplayName("开局第一步下中心")
    void testOpeningBookFirstMove() {
        int[][] board = new int[15][15];
        int[] move = aiHard.calculateMove(board);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(7, move[1]);
    }

    @Test
    @DisplayName("切换难度")
    void testChangeDifficulty() {
        aiEasy.setDifficulty(Difficulty.HARD);
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.WHITE;
        int[] move = aiEasy.calculateMove(board);
        assertNotNull(move);
    }
}
