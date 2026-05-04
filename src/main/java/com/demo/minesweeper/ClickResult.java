package com.demo.minesweeper;

public class ClickResult {
    public String type;
    public int row;
    public int col;

    public static ClickResult continueGame() {
        ClickResult r = new ClickResult();
        r.type = "continue";
        return r;
    }

    public static ClickResult mine(int row, int col) {
        ClickResult r = new ClickResult();
        r.type = "mine";
        r.row = row;
        r.col = col;
        return r;
    }

    public static ClickResult win() {
        ClickResult r = new ClickResult();
        r.type = "win";
        return r;
    }

    public static ClickResult gameOver() {
        ClickResult r = new ClickResult();
        r.type = "gameover";
        return r;
    }
}