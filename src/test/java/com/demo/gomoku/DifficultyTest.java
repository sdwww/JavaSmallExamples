package com.demo.gomoku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Difficulty 难度枚举测试")
class DifficultyTest {

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    @DisplayName("所有难度枚举值")
    void testAllDifficulties(Difficulty difficulty) {
        assertNotNull(difficulty.getName());
        assertTrue(difficulty.getLevel() >= 1);
        assertTrue(difficulty.getDefenseWeight() > 0);
        assertTrue(difficulty.getSearchRange() >= 1);
        assertTrue(difficulty.getCandidateLimit() >= 1);
    }

    @Test
    @DisplayName("从等级获取难度")
    void testFromLevel() {
        assertEquals(Difficulty.EASY, Difficulty.fromLevel(1));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromLevel(2));
        assertEquals(Difficulty.HARD, Difficulty.fromLevel(3));
    }

    @Test
    @DisplayName("无效等级返回中等")
    void testFromLevelInvalid() {
        assertEquals(Difficulty.MEDIUM, Difficulty.fromLevel(0));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromLevel(-1));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromLevel(100));
    }

    @Test
    @DisplayName("难度等级递增")
    void testDifficultyLevelIncreasing() {
        assertTrue(Difficulty.EASY.getLevel() < Difficulty.MEDIUM.getLevel());
        assertTrue(Difficulty.MEDIUM.getLevel() < Difficulty.HARD.getLevel());
        assertTrue(Difficulty.HARD.getSearchRange() > Difficulty.EASY.getSearchRange());
        assertTrue(Difficulty.HARD.getCandidateLimit() > Difficulty.EASY.getCandidateLimit());
    }
}
