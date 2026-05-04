package com.demo.minesweeper;

public class NewGameResult {
    public String gameId;
    public int[][] board;

    public NewGameResult(String gameId, int[][] board) {
        this.gameId = gameId;
        this.board = board;
    }
}