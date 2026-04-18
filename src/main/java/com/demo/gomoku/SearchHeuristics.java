package com.demo.gomoku;

/**
 * 搜索启发式算法
 * 包含杀手启发和历史启发
 */
public class SearchHeuristics {

    private static final int BOARD_SIZE = GomokuBoard.BOARD_SIZE;
    private static final int MAX_DEPTH = 10;

    // 杀手启发：每层记录产生剪枝的走法
    private final int[][] killerMoves = new int[MAX_DEPTH + 1][2];

    // 历史启发：记录着法历史分数
    private final int[][] historyTable = new int[BOARD_SIZE][BOARD_SIZE];

    public void clear() {
        for (int i = 0; i <= MAX_DEPTH; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1;
        }
        for (int i = 0; i < BOARD_SIZE; i++) {
            java.util.Arrays.fill(historyTable[i], 0);
        }
    }

    /**
     * 获取杀手走法
     */
    public int[] getKillerMove(int ply) {
        return killerMoves[ply];
    }

    /**
     * 设置杀手走法
     */
    public void setKillerMove(int ply, int row, int col) {
        killerMoves[ply][0] = row;
        killerMoves[ply][1] = col;
    }

    /**
     * 获取历史分数
     */
    public int getHistoryScore(int row, int col) {
        return historyTable[row][col];
    }

    /**
     * 增加历史分数
     */
    public void addHistoryScore(int row, int col, int depth) {
        historyTable[row][col] += depth * depth;
    }

    /**
     * 着法排序：优先历史分数高的，杀手走法优先
     */
    public java.util.Comparator<int[]> moveComparator(int ply) {
        return (a, b) -> {
            int scoreA = historyTable[a[0]][a[1]];
            int scoreB = historyTable[b[0]][b[1]];
            if (scoreB != scoreA) return scoreB - scoreA;

            int killerR = killerMoves[ply][0];
            int killerC = killerMoves[ply][1];
            if (a[0] == killerR && a[1] == killerC) return 1;
            if (b[0] == killerR && b[1] == killerC) return -1;

            return 0;
        };
    }
}
