package com.demo.minesweeper;

import java.util.Random;

public class MineSweeperGame {

    public static final int ROWS = 16;
    public static final int COLS = 16;
    public static final int MINES = 40;

    private final int[][] board;      // 每个格子的数字（-1=雷，0-8=周围雷数）
    private final boolean[][] revealed;  // 是否已翻开
    private final boolean[][] flagged;  // 是否标记了旗
    private final boolean[][] isMine;   // 是否是雷
    private boolean gameOver;           // 游戏是否结束
    private boolean firstClick;         // 是否首次点击（首次点击不炸）

    public MineSweeperGame() {
        this.board = new int[ROWS][COLS];
        this.revealed = new boolean[ROWS][COLS];
        this.flagged = new boolean[ROWS][COLS];
        this.isMine = new boolean[ROWS][COLS];
        this.gameOver = false;
        this.firstClick = true;
    }

    public void placeMines(int excludeRow, int excludeCol) {
        Random random = new Random();

        boolean[][] exclude = new boolean[ROWS][COLS];
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = excludeRow + dr;
                int c = excludeCol + dc;
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    exclude[r][c] = true;
                }
            }
        }

        int placed = 0;
        while (placed < MINES) {
            int r = random.nextInt(ROWS);
            int c = random.nextInt(COLS);
            if (!exclude[r][c] && !isMine[r][c]) {
                isMine[r][c] = true;
                placed++;
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (isMine[r][c]) {
                    board[r][c] = -1;
                } else {
                    int count = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = r + dr, nc = c + dc;
                            if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && isMine[nr][nc]) {
                                count++;
                            }
                        }
                    }
                    board[r][c] = count;
                }
            }
        }

        firstClick = false;
    }

    public ClickResult click(int row, int col) {
        if (gameOver) {
            return ClickResult.gameOver();
        }
        if (revealed[row][col] || flagged[row][col]) {
            return ClickResult.continueGame(getBoard());
        }
        if (firstClick) {
            placeMines(row, col);
        }

        if (isMine[row][col]) {
            gameOver = true;
            return ClickResult.mine(row, col, getBoard());
        }

        floodFill(row, col);
        return checkWin() ? ClickResult.win(getBoard()) : ClickResult.continueGame(getBoard());
    }

    public FlagResult flag(int row, int col) {
        if (gameOver || revealed[row][col]) {
            return FlagResult.invalid();
        }
        flagged[row][col] = !flagged[row][col];
        return FlagResult.ok(flagged[row][col], countFlags());
    }

    private int countFlags() {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (flagged[r][c]) {
                    count++;
                }
            }
        }
        return count;
    }

    private void floodFill(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return;
        }
        if (revealed[row][col] || flagged[row][col]) {
            return;
        }

        revealed[row][col] = true;
        if (board[row][col] > 0) {
            return;
        }

        for (int rowDelta = -1; rowDelta <= 1; rowDelta++) {
            for (int colDelta = -1; colDelta <= 1; colDelta++) {
                if (rowDelta == 0 && colDelta == 0) {
                    continue;
                }
                floodFill(row + rowDelta, col + colDelta);
            }
        }
    }

    private boolean checkWin() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!revealed[r][c] && !isMine[r][c]) {
                    return false;
                }
            }
        }
        gameOver = true;
        return true;
    }

    public int[][] getBoard() {
        int[][] result = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (revealed[r][c]) {
                    result[r][c] = board[r][c];
                } else if (flagged[r][c]) {
                    result[r][c] = -2;
                } else {
                    result[r][c] = -1;
                }
            }
        }
        return result;
    }

    public void reset() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = 0;
                revealed[r][c] = false;
                flagged[r][c] = false;
                isMine[r][c] = false;
            }
        }
        gameOver = false;
        firstClick = true;
    }
}