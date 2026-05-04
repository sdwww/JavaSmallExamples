package com.demo.minesweeper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CellValue {
    MINE(-3, "雷"),
    FLAG(-2, "旗"),
    HIDDEN(-1, "未翻开"),
    ZERO(0, "周围无雷"),
    ONE(1, "周围1颗雷"),
    TWO(2, "周围2颗雷"),
    THREE(3, "周围3颗雷"),
    FOUR(4, "周围4颗雷"),
    FIVE(5, "周围5颗雷"),
    SIX(6, "周围6颗雷"),
    SEVEN(7, "周围7颗雷"),
    EIGHT(8, "周围8颗雷");

    private final int code;
    private final String desc;
}