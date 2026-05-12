package com.demo.minesweeper;

public class ClickResult {
    public ClickResultType type;
    public int[][] board;

    public static ClickResult continueGame(int[][] board) {
        ClickResult r = new ClickResult();
        r.type = ClickResultType.CONTINUE;
        r.board = board;
        return r;
    }

    public static ClickResult mine(int[][] board) {
        ClickResult r = new ClickResult();
        r.type = ClickResultType.MINE;
        r.board = board;
        return r;
    }

    public static ClickResult win(int[][] board) {
        ClickResult r = new ClickResult();
        r.type = ClickResultType.WIN;
        r.board = board;
        return r;
    }

    public static ClickResult gameOver() {
        ClickResult r = new ClickResult();
        r.type = ClickResultType.GAME_OVER;
        return r;
    }
}