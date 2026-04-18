package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThreatDetector 威胁检测测试")
class ThreatDetectorTest {

    private ThreatDetector detector = new ThreatDetector();

    @Test
    @DisplayName("检测一步获胜")
    void testFindOneMoveWin() {
        int[][] board = new int[15][15];
        for (int i = 0; i < 4; i++) {
            board[7][i] = GomokuBoard.BLACK;
        }
        int[] winMove = detector.findOneMoveWin(board, GomokuBoard.BLACK);
        assertNotNull(winMove);
        assertEquals(7, winMove[0]);
        assertEquals(4, winMove[1]);
    }

    @Test
    @DisplayName("检测跳跃四连")
    void testFindJumpFour() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][9] = GomokuBoard.BLACK;
        board[7][10] = GomokuBoard.BLACK;
        board[7][11] = GomokuBoard.BLACK;
        int[] move = detector.findJumpFour(board, GomokuBoard.BLACK);
        assertNotNull(move);
        assertEquals(7, move[0]);
        assertEquals(8, move[1]);
    }

    @Test
    @DisplayName("检测活三")
    void testFindLiveThree() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        board[7][9] = GomokuBoard.BLACK;
        int[] defense = detector.findExistingThree(board, GomokuBoard.BLACK);
        assertNotNull(defense);
    }

    @Test
    @DisplayName("检测冲四威胁")
    void testFindRushFourThreat() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        board[7][8] = GomokuBoard.BLACK;
        board[7][9] = GomokuBoard.BLACK;
        board[7][10] = GomokuBoard.BLACK;
        board[7][6] = GomokuBoard.WHITE;
        int[] threat = detector.findRushFourThreat(board, GomokuBoard.BLACK);
        assertNotNull(threat);
        assertEquals(7, threat[0]);
        assertEquals(11, threat[1]);
    }

    @Test
    @DisplayName("检测AI获胜机会")
    void testFindAIWinOpportunity() {
        int[][] board = new int[15][15];
        for (int i = 0; i < 4; i++) {
            board[7][i] = GomokuBoard.WHITE;
        }
        int[] winMove = detector.findAIWinOpportunity(board, GomokuBoard.WHITE);
        assertNotNull(winMove);
    }

    @Test
    @DisplayName("检测组合威胁")
    void testFindComboThreat() {
        int[][] board = new int[15][15];
        board[7][6] = GomokuBoard.BLACK;
        board[7][7] = GomokuBoard.BLACK;
        board[7][9] = GomokuBoard.BLACK;
        board[7][10] = GomokuBoard.BLACK;
        int[] combo = detector.findComboThreat(board, GomokuBoard.BLACK);
        assertNotNull(combo);
        assertEquals(7, combo[0]);
        assertEquals(8, combo[1]);
    }

    @Test
    @DisplayName("获取候选落子")
    void testGetCandidateMoves() {
        int[][] board = new int[15][15];
        board[7][7] = GomokuBoard.BLACK;
        var candidates = detector.getCandidateMoves(board, 2, true, 10);
        assertFalse(candidates.isEmpty());
        boolean found = false;
        for (int[] c : candidates) {
            if (Math.abs(c[0] - 7) <= 2 && Math.abs(c[1] - 7) <= 2) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    @DisplayName("候选落子限制")
    void testCandidateLimit() {
        int[][] board = new int[15][15];
        for (int i = 0; i < 5; i++) {
            board[0][i] = GomokuBoard.BLACK;
        }
        var candidates = detector.getCandidateMoves(board, 3, false, 5);
        assertTrue(candidates.size() <= 5);
    }
}
