package com.demo.minesweeper;

import java.util.Random;

public class MineSweeperGame {

    private final int rows;
    private final int cols;
    private final int mines;

    private final int[][] board;
    private final boolean[][] revealed;
    private final boolean[][] flagged;
    private final boolean[][] isMine;
    private boolean gameOver;
    private boolean firstClick;

    public MineSweeperGame(Difficulty difficulty) {
        this.rows = difficulty.getRows();
        this.cols = difficulty.getCols();
        this.mines = difficulty.getMines();
        this.board = new int[rows][cols];
        this.revealed = new boolean[rows][cols];
        this.flagged = new boolean[rows][cols];
        this.isMine = new boolean[rows][cols];
        this.gameOver = false;
        this.firstClick = true;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getMines() { return mines; }

    /**
     * 布雷：以点击位置为中心，周围9格不放置雷
     */
    public void placeMines(int clickedRow, int clickedCol) {
        Random random = new Random();

        boolean[][] safeZone = new boolean[rows][cols];
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
        while (mineCount < mines) {
            int randomRow = random.nextInt(rows);
            int randomCol = random.nextInt(cols);
            if (!safeZone[randomRow][randomCol] && !isMine[randomRow][randomCol]) {
                isMine[randomRow][randomCol] = true;
                mineCount++;
            }
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
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
        validateCoordinates(row, col);
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
        validateCoordinates(row, col);
        if (gameOver || revealed[row][col]) {
            return FlagResult.invalid();
        }
        flagged[row][col] = !flagged[row][col];
        return FlagResult.ok(flagged[row][col], countFlags());
    }

    /**
     * 双击翻开（chording）：当周围旗子数等于格子数字时，自动翻开周围未标记的格子
     */
    public ClickResult chord(int row, int col) {
        validateCoordinates(row, col);
        if (gameOver) {
            return ClickResult.gameOver();
        }
        // 必须是已翻开的数字格
        if (!revealed[row][col] || board[row][col] <= 0) {
            return ClickResult.continueGame(getBoard());
        }

        int neighborFlagCount = countNeighborFlags(row, col);
        // 周围旗子数必须等于格子数字才执行
        if (neighborFlagCount != board[row][col]) {
            return ClickResult.continueGame(getBoard());
        }

        // 自动翻开周围所有未翻开且未标记的格子
        boolean hitMine = chordExpand(row, col);
        if (hitMine) {
            gameOver = true;
            return ClickResult.mine(getBoardWithMines());
        }

        return checkWin() ? ClickResult.win(getBoard()) : ClickResult.continueGame(getBoard());
    }

    /**
     * 双击时展开周围格，遇到雷返回 true
     */
    private boolean chordExpand(int row, int col) {
        for (int rowDelta = -1; rowDelta <= 1; rowDelta++) {
            for (int colDelta = -1; colDelta <= 1; colDelta++) {
                if (rowDelta == 0 && colDelta == 0) {
                    continue;
                }
                int neighborRow = row + rowDelta;
                int neighborCol = col + colDelta;
                if (!inBounds(neighborRow, neighborCol)) {
                    continue;
                }
                if (flagged[neighborRow][neighborCol] || revealed[neighborRow][neighborCol]) {
                    continue;
                }

                revealed[neighborRow][neighborCol] = true;
                if (isMine[neighborRow][neighborCol]) {
                    return true;
                }
                // 如果翻开的是空白格，继续洪水填充
                if (board[neighborRow][neighborCol] == 0) {
                    floodFill(neighborRow, neighborCol);
                }
            }
        }
        return false;
    }

    /**
     * 计算指定格子周围8格有多少个旗子
     */
    private int countNeighborFlags(int row, int col) {
        int count = 0;
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                int neighborRow = row + rowOffset;
                int neighborCol = col + colOffset;
                if (inBounds(neighborRow, neighborCol) && flagged[neighborRow][neighborCol]) {
                    count++;
                }
            }
        }
        return count;
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
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /**
     * 校验坐标是否有效
     */
    private void validateCoordinates(int row, int col) {
        if (!inBounds(row, col)) {
            throw new IllegalArgumentException("Invalid coordinates: row=" + row + ", col=" + col);
        }
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
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
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
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
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
        int[][] result = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (revealed[r][c]) {
                    result[r][c] = board[r][c];
                } else if (flagged[r][c]) {
                    result[r][c] = CellValue.FLAG.getCode();
                } else {
                    result[r][c] = CellValue.HIDDEN.getCode();
                }
            }
        }
        return result;
    }

    /**
     * 获取棋盘状态（游戏结束时显示所有雷的位置）
     */
    public int[][] getBoardWithMines() {
        int[][] result = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (isMine[r][c]) {
                    result[r][c] = CellValue.MINE.getCode();
                } else if (revealed[r][c]) {
                    result[r][c] = board[r][c];
                } else if (flagged[r][c]) {
                    result[r][c] = CellValue.FLAG.getCode();
                } else {
                    result[r][c] = CellValue.HIDDEN.getCode();
                }
            }
        }
        return result;
    }

    /**
     * 重置游戏为初始状态
     */
    public void reset() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
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