package com.demo.minesweeper;

public class ClickResult {
    public String type;
    public int[][] board;

    public static ClickResult continueGame(int[][] board) {
        ClickResult r = new ClickResult();
        r.type = "continue";
        r.board = board;
        return r;
    }

    public static ClickResult mine(int[][] board) {
        ClickResult r = new ClickResult();
        r.type = "mine";
        r.board = board;
        return r;
    }

    public static ClickResult win(int[][] board) {
        ClickResult r = new ClickResult();
        r.type = "win";
        r.board = board;
        return r;
    }

    public static ClickResult gameOver() {
        ClickResult r = new ClickResult();
        r.type = "gameover";
        return r;
    }
}