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

    /**
     * 布雷：以点击位置为中心，周围9格不放置雷
     */
    public void placeMines(int clickedRow, int clickedCol) {
        Random random = new Random();

        boolean[][] safeZone = new boolean[ROWS][COLS];
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                int safeRow = clickedRow + rowOffset;
                int safeCol = clickedCol + colOffset;
                if (inBounds(safeRow, safeCol)) {
                    safeZone[safeRow][safeCol] = true;
                }
            }
        }

        int mineCount = 0;
        while (mineCount < MINES) {
            int randomRow = random.nextInt(ROWS);
            int randomCol = random.nextInt(COLS);
            if (!safeZone[randomRow][randomCol] && !isMine[randomRow][randomCol]) {
                isMine[randomRow][randomCol] = true;
                mineCount++;
            }
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (isMine[row][col]) {
                    board[row][col] = -1;
                } else {
                    board[row][col] = countNeighborMines(row, col);
                }
            }
        }

        firstClick = false;
    }

    /**
     * 处理点击：翻开格子或标记旗
     */
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
            return ClickResult.mine(getBoardWithMines());
        }

        floodFill(row, col);
        return checkWin() ? ClickResult.win(getBoard()) : ClickResult.continueGame(getBoard());
    }

    /**
     * 标记/取消标记旗
     */
    public FlagResult flag(int row, int col) {
        if (gameOver || revealed[row][col]) {
            return FlagResult.invalid();
        }
        flagged[row][col] = !flagged[row][col];
        return FlagResult.ok(flagged[row][col], countFlags());
    }

    /**
     * 计算指定格子周围8格有多少颗雷
     */
    private int countNeighborMines(int row, int col) {
        int count = 0;
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                int neighborRow = row + rowOffset;
                int neighborCol = col + colOffset;
                if (inBounds(neighborRow, neighborCol) && isMine[neighborRow][neighborCol]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 判断坐标是否在棋盘范围内
     */
    private boolean inBounds(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    /**
     * 洪水填充：空白格自动展开周围安全格
     */
    private void floodFill(int row, int col) {
        if (!inBounds(row, col)) {
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

    /**
     * 统计当前标记旗的数量
     */
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

    /**
     * 检查是否胜利：所有非雷格子都被翻开
     */
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

    /**
     * 获取当前棋盘状态
     */
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

    /**
     * 获取棋盘状态（游戏结束时显示所有雷的位置）
     * 返回值：-3=雷（游戏结束时显示），-2=旗，-1=未翻开，0-8=已翻开数字
     */
    public int[][] getBoardWithMines() {
        int[][] result = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (isMine[r][c]) {
                    result[r][c] = -3;  // 雷：游戏结束时显示
                } else if (revealed[r][c]) {
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

    /**
     * 重置游戏为初始状态
     */
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