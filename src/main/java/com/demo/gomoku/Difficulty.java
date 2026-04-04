package com.demo.gomoku;

/**
 * 五子棋难度枚举
 */
public enum Difficulty {
    EASY(1, "简单", 0.9, 3, 12),
    MEDIUM(2, "中等", 1.1, 3, 15),
    HARD(3, "困难", 1.5, 4, 18);

    private final int level;
    private final String name;
    private final double defenseWeight;
    private final int searchRange;
    private final int candidateLimit;

    Difficulty(int level, String name, double defenseWeight, int searchRange, int candidateLimit) {
        this.level = level;
        this.name = name;
        this.defenseWeight = defenseWeight;
        this.searchRange = searchRange;
        this.candidateLimit = candidateLimit;
    }

    public int getLevel() { return level; }
    public String getName() { return name; }
    public double getDefenseWeight() { return defenseWeight; }
    public int getSearchRange() { return searchRange; }
    public int getCandidateLimit() { return candidateLimit; }

    public static Difficulty fromLevel(int level) {
        for (Difficulty d : values()) {
            if (d.level == level) return d;
        }
        return MEDIUM; // 默认中等难度
    }
}
