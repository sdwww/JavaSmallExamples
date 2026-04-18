package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 置换表 + Zobrist哈希
 * 避免重复计算局面分数，加速搜索
 */
public class TranspositionTable {

    private static final int TT_SIZE = 1 << 22; // ~4M slots
    private static final int TT_MASK = TT_SIZE - 1;
    private static final int BOARD_SIZE = GomokuBoard.BOARD_SIZE;

    private final long[] ttKeys = new long[TT_SIZE];
    private final int[] ttScores = new int[TT_SIZE];
    private final int[] ttDepths = new int[TT_SIZE];
    private final byte[] ttFlags = new byte[TT_SIZE]; // 0=空, 1=精确, 2=下界, 3=上界
    private final ReentrantReadWriteLock ttLock = new ReentrantReadWriteLock();

    // Zobrist表
    private static final long[][][] ZOBRIST_TABLE = initZobristTable();

    private static long[][][] initZobristTable() {
        long[][][] table = new long[BOARD_SIZE][BOARD_SIZE][3];
        Random rand = new Random(42);
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                table[i][j][1] = rand.nextLong(); // BLACK = 1
                table[i][j][2] = rand.nextLong(); // WHITE = 2
            }
        }
        return table;
    }

    public static long[][][] getZobristTable() {
        return ZOBRIST_TABLE;
    }

    public void clear() {
        Arrays.fill(ttKeys, 0);
        Arrays.fill(ttScores, 0);
        Arrays.fill(ttDepths, 0);
        Arrays.fill(ttFlags, (byte) 0);
    }

    /**
     * 查找置换表
     * @return 分数或 Integer.MIN_VALUE 表示未命中
     */
    public int lookup(long hash, int depth, int alpha, int beta) {
        ttLock.readLock().lock();
        try {
            int idx = (int) (hash & TT_MASK);
            if (ttKeys[idx] == hash && ttDepths[idx] >= depth) {
                int score = ttScores[idx];
                byte flag = ttFlags[idx];
                if (flag == 1) return score;                  // 精确值
                if (flag == 2 && score >= beta) return score;  // 下界
                if (flag == 3 && score <= alpha) return score; // 上界
            }
            return Integer.MIN_VALUE;
        } finally {
            ttLock.readLock().unlock();
        }
    }

    /**
     * 存储到置换表
     */
    public void store(long hash, int depth, int score, byte flag) {
        ttLock.writeLock().lock();
        try {
            int idx = (int) (hash & TT_MASK);
            if (ttDepths[idx] == 0) {
                ttKeys[idx] = hash;
                ttScores[idx] = score;
                ttDepths[idx] = depth;
                ttFlags[idx] = flag;
            } else if (flag == 1) {
                if (ttFlags[idx] != 1 || depth >= ttDepths[idx]) {
                    ttKeys[idx] = hash;
                    ttScores[idx] = score;
                    ttDepths[idx] = depth;
                    ttFlags[idx] = flag;
                }
            } else if (depth > ttDepths[idx]) {
                ttKeys[idx] = hash;
                ttScores[idx] = score;
                ttDepths[idx] = depth;
                ttFlags[idx] = flag;
            } else if (ttFlags[idx] != 1 && depth >= ttDepths[idx]) {
                ttKeys[idx] = hash;
                ttScores[idx] = score;
                ttDepths[idx] = depth;
                ttFlags[idx] = flag;
            }
        } finally {
            ttLock.writeLock().unlock();
        }
    }

    /**
     * 计算棋盘Zobrist哈希值
     */
    public static long computeHash(int[][] board) {
        long hash = 0;
        for (int i = 0; i < BOARD_SIZE && i < board.length; i++) {
            for (int j = 0; j < BOARD_SIZE && j < board[i].length; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    hash ^= ZOBRIST_TABLE[i][j][board[i][j]];
                }
            }
        }
        return hash;
    }
}
