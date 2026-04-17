package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GomokuBoard 棋盘测试")
class GomokuBoardTest {

    @Test
    @DisplayName("创建空棋盘")
    void testCreateEmptyBoard() {
        GomokuBoard board = new GomokuBoard();
        assertEquals(15, GomokuBoard.BOARD_SIZE);
        assertTrue(board.isEmpty());
        assertEquals(0, board.getMoveCount());
    }

    @Test
    @DisplayName("落子和撤销")
    void testMakeAndUndoMove() {
        GomokuBoard board = new GomokuBoard();
        assertTrue(board.makeMove(7, 7, GomokuBoard.BLACK));
        assertEquals(1, board.getMoveCount());
        assertFalse(board.isEmpty());
        board.undoMove(7, 7);
        assertEquals(0, board.getMoveCount());
        assertTrue(board.isEmpty());
    }

    @Test
    @DisplayName("无效位置落子")
    void testInvalidMove() {
        GomokuBoard board = new GomokuBoard();
        assertFalse(board.makeMove(-1, 7, GomokuBoard.BLACK));
        assertFalse(board.makeMove(15, 7, GomokuBoard.BLACK));
        assertFalse(board.makeMove(7, -1, GomokuBoard.BLACK));
        assertFalse(board.makeMove(7, 15, GomokuBoard.BLACK));
    }

    @Test
    @DisplayName("已有棋子位置落子")
    void testOccupiedMove() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 7, GomokuBoard.BLACK);
        assertFalse(board.makeMove(7, 7, GomokuBoard.WHITE));
    }

    @Test
    @DisplayName("五子连珠检测 - 横向")
    void testWinHorizontal() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 5; i++) {
            board.makeMove(7, i, GomokuBoard.BLACK);
        }
        assertTrue(board.checkWin(7, 2));
    }

    @Test
    @DisplayName("五子连珠检测 - 纵向")
    void testWinVertical() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 5; i++) {
            board.makeMove(i, 7, GomokuBoard.WHITE);
        }
        assertTrue(board.checkWin(2, 7));
    }

    @Test
    @DisplayName("五子连珠检测 - 主对角线")
    void testWinMainDiagonal() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 5; i++) {
            board.makeMove(i, i, GomokuBoard.BLACK);
        }
        assertTrue(board.checkWin(2, 2));
    }

    @Test
    @DisplayName("五子连珠检测 - 副对角线")
    void testWinAntiDiagonal() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 5; i++) {
            board.makeMove(i, 14 - i, GomokuBoard.WHITE);
        }
        assertTrue(board.checkWin(2, 12));
    }

    @Test
    @DisplayName("不足五子不获胜")
    void testNoWinWithFour() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 4; i++) {
            board.makeMove(7, i, GomokuBoard.BLACK);
        }
        assertFalse(board.checkWin(7, 2));
    }

    @Test
    @DisplayName("中断的五子不获胜")
    void testNoWinWithGap() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 0, GomokuBoard.BLACK);
        board.makeMove(7, 1, GomokuBoard.BLACK);
        board.makeMove(7, 2, GomokuBoard.WHITE);
        board.makeMove(7, 3, GomokuBoard.BLACK);
        board.makeMove(7, 4, GomokuBoard.BLACK);
        assertFalse(board.checkWin(7, 1));
    }

    @Test
    @DisplayName("边界外返回-1")
    void testGetPieceOutOfBounds() {
        GomokuBoard board = new GomokuBoard();
        assertEquals(-1, board.getPiece(-1, 7));
        assertEquals(-1, board.getPiece(15, 7));
    }

    @Test
    @DisplayName("棋盘是否已满")
    void testIsFull() {
        GomokuBoard board = new GomokuBoard();
        assertFalse(board.isFull());
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                if (i == 14 && j == 14) break;
                board.makeMove(i, j, (i + j) % 2 == 0 ? GomokuBoard.BLACK : GomokuBoard.WHITE);
            }
        }
        assertFalse(board.isFull());
        board.makeMove(14, 14, GomokuBoard.WHITE);
        assertTrue(board.isFull());
    }

    @Test
    @DisplayName("重置棋盘")
    void testReset() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 7, GomokuBoard.BLACK);
        board.makeMove(8, 8, GomokuBoard.WHITE);
        board.reset();
        assertTrue(board.isEmpty());
        assertEquals(0, board.getMoveCount());
    }

    @Test
    @DisplayName("获取棋盘副本")
    void testGetBoard() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 7, GomokuBoard.BLACK);
        int[][] copy = board.getBoard();
        assertEquals(GomokuBoard.BLACK, copy[7][7]);
        copy[7][7] = GomokuBoard.WHITE;
        assertEquals(GomokuBoard.BLACK, board.getPiece(7, 7));
    }

    @Test
    @DisplayName("checkBoardWin 有胜者")
    void testCheckBoardWinWithWinner() {
        GomokuBoard board = new GomokuBoard();
        for (int i = 0; i < 5; i++) {
            board.makeMove(7, i, GomokuBoard.WHITE);
        }
        assertEquals(GomokuBoard.WHITE, board.checkBoardWin());
    }

    @Test
    @DisplayName("checkBoardWin 无胜者")
    void testCheckBoardWinNoWinner() {
        GomokuBoard board = new GomokuBoard();
        board.makeMove(7, 7, GomokuBoard.WHITE);
        board.makeMove(7, 8, GomokuBoard.WHITE);
        assertEquals(GomokuBoard.EMPTY, board.checkBoardWin());
    }
}
