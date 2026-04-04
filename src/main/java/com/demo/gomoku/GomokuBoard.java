package com.demo.gomoku;

/**
 * 五子棋棋盘数据结构和基础操作
 */
public class GomokuBoard {
    
    public static final int BOARD_SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;  // 黑方（玩家）
    public static final int WHITE = 2;  // 白方（AI）
    
    // 方向数组
    public static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    
    protected int[][] board;
    protected int moveCount;
    
    public GomokuBoard() {
        this.board = new int[BOARD_SIZE][BOARD_SIZE];
        this.moveCount = 0;
    }
    
    public GomokuBoard(GomokuBoard other) {
        this.board = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(other.board[i], 0, this.board[i], 0, BOARD_SIZE);
        }
        this.moveCount = other.moveCount;
    }
    
    /**
     * 落子
     */
    public boolean makeMove(int row, int col, int player) {
        if (!isValidMove(row, col)) {
            return false;
        }
        board[row][col] = player;
        moveCount++;
        return true;
    }
    
    /**
     * 撤销落子
     */
    public void undoMove(int row, int col) {
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            if (board[row][col] != EMPTY) {
                board[row][col] = EMPTY;
                moveCount--;
            }
        }
    }
    
    /**
     * 检查是否有效空位
     */
    public boolean isValidMove(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] == EMPTY;
    }
    
    /**
     * 检查是否五子连珠
     */
    public boolean checkWin(int row, int col) {
        int player = board[row][col];
        for (int[] dir : DIRECTIONS) {
            int count = 1;
            count += countInDirection(row, col, player, dir[0], dir[1]);
            count += countInDirection(row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }
    
    /**
     * 检查棋盘上是否已有胜者
     */
    public int checkBoardWin() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    if (checkWin(i, j)) {
                        return board[i][j];
                    }
                }
            }
        }
        return EMPTY;
    }
    
    /**
     * 在指定方向上连续计数
     */
    public int countInDirection(int row, int col, int player, int dRow, int dCol) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;
        
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }
    
    /**
     * 获取棋子颜色
     */
    public int getPiece(int row, int col) {
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            return board[row][col];
        }
        return -1;
    }
    
    /**
     * 检查棋盘是否为空
     */
    public boolean isEmpty() {
        return moveCount == 0;
    }
    
    /**
     * 检查棋盘是否已满
     */
    public boolean isFull() {
        return moveCount >= BOARD_SIZE * BOARD_SIZE;
    }
    
    /**
     * 重置棋盘
     */
    public void reset() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        moveCount = 0;
    }
    
    /**
     * 获取棋盘副本
     */
    public int[][] getBoard() {
        int[][] copy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, BOARD_SIZE);
        }
        return copy;
    }
    
    public int getMoveCount() {
        return moveCount;
    }
}
