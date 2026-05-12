package com.demo.minesweeper;

public enum ClickResultType {
    CONTINUE("continue"), MINE("mine"), WIN("win"), GAME_OVER("gameover");

    private final String value;
    ClickResultType(String value) { this.value = value; }
    @Override public String toString() { return value; }
}