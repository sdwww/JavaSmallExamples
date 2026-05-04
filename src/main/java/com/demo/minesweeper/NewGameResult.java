package com.demo.minesweeper;

public class NewGameResult {
    public String gameId;
    public int rows;
    public int cols;
    public int mines;
    public int[][] board;

    public NewGameResult(String gameId, int rows, int cols, int mines, int[][] board) {
        this.gameId = gameId;
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
        this.board = board;
    }
}