package com.demo.minesweeper;

public class FlagResult {
    public boolean flagged;
    public int flagCount;
    public boolean valid;

    public static FlagResult ok(boolean flagged, int flagCount) {
        FlagResult r = new FlagResult();
        r.valid = true;
        r.flagged = flagged;
        r.flagCount = flagCount;
        return r;
    }

    public static FlagResult invalid() {
        FlagResult r = new FlagResult();
        r.valid = false;
        return r;
    }
}