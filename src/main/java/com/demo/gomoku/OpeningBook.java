package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 开局库
 * 支持常见开局定式：花月、浦月、疏星、流星、云月、雨月、银河、星辰
 */
public class OpeningBook {

    private static final int CENTER = GomokuBoard.BOARD_SIZE / 2;
    private static final int BOARD_SIZE = GomokuBoard.BOARD_SIZE;

    // 开局类型常量
    public static final int OPENING_UNKNOWN = -1;
    public static final int OPENING_HUAYUE = 0;    // 花月
    public static final int OPENING_QIANYUE = 1;   // 浦月
    public static final int OPENING_SHUSHU = 2;    // 疏星
    public static final int OPENING_LIUXING = 3;   // 流星
    public static final int OPENING_YUNYUE = 4;    // 云月
    public static final int OPENING_YUYUE = 5;     // 雨月
    public static final int OPENING_YINHE = 6;     // 银河
    public static final int OPENING_XINGCHEN = 7; // 星辰

    /**
     * 获取开局着法
     * @param board 棋盘
     * @param moveCount 棋子总数
     * @return 着法坐标或null
     */
    public int[] getMove(int[][] board, int moveCount) {
        if (moveCount == 0) {
            return new int[]{CENTER, CENTER};
        }

        List<int[]> blackPositions = new ArrayList<>();
        List<int[]> whitePositions = new ArrayList<>();
        collectPieces(board, blackPositions, whitePositions);

        // AI先手（白方）的开局应对
        if (blackPositions.size() == 1 && whitePositions.isEmpty()) {
            return getSecondMoveResponse(board, blackPositions.get(0));
        }

        // AI先手，第三步
        if (blackPositions.size() == 1 && whitePositions.size() == 1) {
            return getThirdMoveStandard(board, blackPositions.get(0), whitePositions.get(0));
        }

        // 前4步走开局定式
        if (moveCount >= 3 && moveCount <= 4) {
            int[] openingMove = recognizeAndPlayOpening(board, moveCount, blackPositions, whitePositions);
            if (openingMove != null && isEmpty(board, openingMove[0], openingMove[1])) {
                return openingMove;
            }
        }

        return null;
    }

    private void collectPieces(int[][] board, List<int[]> blacks, List<int[]> whites) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.BLACK) blacks.add(new int[]{i, j});
                else if (board[i][j] == GomokuBoard.WHITE) whites.add(new int[]{i, j});
            }
        }
    }

    /**
     * 第二手应对
     */
    private int[] getSecondMoveResponse(int[][] board, int[] blackFirst) {
        int bc = blackFirst[0], bj = blackFirst[1];
        int dist = Math.abs(bc - CENTER) + Math.abs(bj - CENTER);

        if (dist <= 1) {
            int[][] diagonals = {{CENTER - 1, CENTER - 1}, {CENTER - 1, CENTER + 1},
                                 {CENTER + 1, CENTER - 1}, {CENTER + 1, CENTER + 1}};
            List<int[]> valid = new ArrayList<>();
            for (int[] d : diagonals) {
                if (isEmpty(board, d[0], d[1])) valid.add(d);
            }
            if (!valid.isEmpty()) {
                return valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
            }
        }

        int[][] neighbors = {
            {bc - 1, bj - 1}, {bc - 1, bj + 1},
            {bc + 1, bj - 1}, {bc + 1, bj + 1},
            {bc, bj - 1}, {bc, bj + 1},
            {bc - 1, bj}, {bc + 1, bj}
        };
        List<int[]> valid = new ArrayList<>();
        for (int[] n : neighbors) {
            if (isEmpty(board, n[0], n[1])) valid.add(n);
        }
        if (!valid.isEmpty()) {
            return valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
        }

        return new int[]{CENTER, CENTER};
    }

    /**
     * 第三手 - 标准开局定式
     */
    private int[] getThirdMoveStandard(int[][] board, int[] blackFirst, int[] whiteFirst) {
        int bc = blackFirst[0], bj = blackFirst[1];
        int wc = whiteFirst[0], wj = whiteFirst[1];
        int openType = classifyOpening(bc, bj, wc, wj);
        List<int[]> candidates = new ArrayList<>();

        switch (openType) {
            case OPENING_QIANYUE:
                addIfEmpty(candidates, board, wc - 1, wj - 1);
                addIfEmpty(candidates, board, wc - 1, wj + 1);
                addIfEmpty(candidates, board, wc + 1, wj - 1);
                addIfEmpty(candidates, board, wc + 1, wj + 1);
                break;
            case OPENING_HUAYUE:
                addIfEmpty(candidates, board, wc - 2, wj);
                addIfEmpty(candidates, board, wc + 2, wj);
                addIfEmpty(candidates, board, wc, wj - 2);
                addIfEmpty(candidates, board, wc, wj + 2);
                break;
            case OPENING_SHUSHU:
                addIfEmpty(candidates, board, wc - 2, wj - 2);
                addIfEmpty(candidates, board, wc - 2, wj + 2);
                addIfEmpty(candidates, board, wc + 2, wj - 2);
                addIfEmpty(candidates, board, wc + 2, wj + 2);
                break;
            case OPENING_LIUXING:
                addIfEmpty(candidates, board, wc - 2, wj);
                addIfEmpty(candidates, board, wc + 2, wj);
                addIfEmpty(candidates, board, wc, wj - 2);
                addIfEmpty(candidates, board, wc, wj + 2);
                break;
            case OPENING_YUNYUE:
                addIfEmpty(candidates, board, bc - 2, bj);
                addIfEmpty(candidates, board, bc, bj - 2);
                addIfEmpty(candidates, board, bc - 1, bj - 1);
                break;
            default:
                if (wc == CENTER && wj == CENTER) {
                    addIfEmpty(candidates, board, wc - 2, wj);
                    addIfEmpty(candidates, board, wc + 2, wj);
                    addIfEmpty(candidates, board, wc, wj - 2);
                    addIfEmpty(candidates, board, wc, wj + 2);
                } else {
                    addIfEmpty(candidates, board, wc - 1, wj);
                    addIfEmpty(candidates, board, wc + 1, wj);
                    addIfEmpty(candidates, board, wc, wj - 1);
                    addIfEmpty(candidates, board, wc, wj + 1);
                }
                break;
        }

        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> {
                int distA = Math.abs(a[0] - CENTER) + Math.abs(a[1] - CENTER);
                int distB = Math.abs(b[0] - CENTER) + Math.abs(b[1] - CENTER);
                return distB - distA;
            });
            return candidates.get(0);
        }

        return new int[]{CENTER, CENTER};
    }

    /**
     * 判断开局类型
     */
    public int classifyOpening(int bc, int bj, int wc, int wj) {
        int bDist = Math.abs(bc - CENTER) + Math.abs(bj - CENTER);
        int wDist = Math.abs(wc - CENTER) + Math.abs(wj - CENTER);

        if (bDist <= 1 && wDist <= 1) {
            if (wc == CENTER && wj == CENTER) return OPENING_HUAYUE;
            return OPENING_QIANYUE;
        }
        if (bDist <= 1) return OPENING_SHUSHU;
        if (bDist >= 3) return OPENING_YUNYUE;
        return OPENING_UNKNOWN;
    }

    /**
     * 识别并走开局定式
     */
    private int[] recognizeAndPlayOpening(int[][] board, int moveCount,
            List<int[]> blacks, List<int[]> whites) {
        if (blacks.size() < 2 || whites.size() < 1) return null;

        int openType = classifyOpening(blacks.get(0)[0], blacks.get(0)[1],
                                      whites.get(0)[0], whites.get(0)[1]);
        if (openType == OPENING_UNKNOWN) return null;

        int aiStep = moveCount / 2;
        if (aiStep < 2 || aiStep > 4) return null;

        return getOpeningMoveFromBook(openType, blacks, whites, aiStep - 2);
    }

    /**
     * 从开局库获取着法
     */
    private int[] getOpeningMoveFromBook(int openType, List<int[]> blacks, List<int[]> whites, int step) {
        int bc = blacks.get(0)[0], bj = blacks.get(0)[1];
        int wc = whites.get(0)[0], wj = whites.get(0)[1];

        switch (openType) {
            case OPENING_QIANYUE: return getQianyueMove(bc, bj, wc, wj, step);
            case OPENING_HUAYUE: return getHuayueMove(wc, wj, step);
            case OPENING_SHUSHU: return getShushuMove(wc, wj, step);
            case OPENING_LIUXING: return getLiuxingMove(wc, wj, step);
            case OPENING_YUNYUE: return getYunyueMove(bc, bj, step);
            default: return null;
        }
    }

    // ===== 各开局定式 =====

    private int[] getQianyueMove(int bc, int bj, int wc, int wj, int step) {
        switch (step) {
            case 0: return new int[]{CENTER - 1, CENTER - 1};
            case 1: return new int[]{wc - 2, wj - 2};
            case 2: return new int[]{CENTER + 1, CENTER + 1};
            default: return null;
        }
    }

    private int[] getHuayueMove(int wc, int wj, int step) {
        switch (step) {
            case 0: return new int[]{CENTER - 1, CENTER - 1};
            case 1: return new int[]{wc - 2, wj};
            case 2: return new int[]{CENTER - 2, CENTER - 2};
            default: return null;
        }
    }

    private int[] getShushuMove(int wc, int wj, int step) {
        switch (step) {
            case 0: return new int[]{CENTER - 1, CENTER - 1};
            case 1: return new int[]{wc - 2, wj - 2};
            case 2: return new int[]{CENTER - 3, CENTER - 1};
            default: return null;
        }
    }

    private int[] getLiuxingMove(int wc, int wj, int step) {
        switch (step) {
            case 0: return new int[]{CENTER - 1, CENTER - 1};
            case 1: return new int[]{CENTER + 1, CENTER + 1};
            case 2: return new int[]{CENTER - 2, CENTER};
            default: return null;
        }
    }

    private int[] getYunyueMove(int bc, int bj, int step) {
        switch (step) {
            case 0:
                if (bc > CENTER) return new int[]{bc - 1, bj};
                if (bc < CENTER) return new int[]{bc + 1, bj};
                return new int[]{bc, bj - 1};
            case 1: return new int[]{bc - 2, bj};
            case 2: return new int[]{bc + 1, bj + 1};
            default: return null;
        }
    }

    // ===== 辅助方法 =====

    private boolean isEmpty(int[][] board, int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY;
    }

    private void addIfEmpty(List<int[]> list, int[][] board, int r, int c) {
        if (isEmpty(board, r, c)) {
            list.add(new int[]{r, c});
        }
    }
}
