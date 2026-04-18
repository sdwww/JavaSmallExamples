package com.demo.gomoku;

import java.util.*;

/**
 * 残局库 - 基于棋型检测的残局判断
 * 当出现关键棋型时介入，提供最优着法
 */
public class EndgameTablebase {

    private static final int BOARD_SIZE = GomokuBoard.BOARD_SIZE;
    private static final int CENTER = BOARD_SIZE / 2;

    private final PatternEvaluator evaluator;

    public EndgameTablebase() {
        this.evaluator = PatternEvaluator.getInstance();
    }

    /**
     * 检测是否进入残局阶段
     * 基于棋型而非棋子数量
     */
    public boolean isEndgame(int[][] board) {
        int pieces = countPieces(board);

        // 太早或太晚都不算残局
        if (pieces < 6 || pieces > 40) {
            return false;
        }

        // 检查是否有关键棋型出现
        boolean hasThreat = hasSignificantThreat(board);
        boolean hasFormation = hasFormation(board);

        // 有威胁棋型 或 有明确阵型 = 残局
        if (hasThreat || hasFormation) {
            return true;
        }

        // 棋子较多但无明确阵型，可能是中盘纠缠
        if (pieces >= 25 && !hasFormation) {
            return true;
        }

        return false;
    }

    /**
     * 是否有重要威胁棋型
     */
    private boolean hasSignificantThreat(int[][] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    // 检查落子后是否能形成活三及以上
                    int maxLevel = getMoveThreatLevel(board, i, j, GomokuBoard.WHITE);
                    if (maxLevel >= 4) return true; // 活四及以上

                    maxLevel = getMoveThreatLevel(board, i, j, GomokuBoard.BLACK);
                    if (maxLevel >= 4) return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否有明确阵型（阵型 = 多子连续或跳跃棋型）
     */
    private boolean hasFormation(int[][] board) {
        int whiteFormations = 0;
        int blackFormations = 0;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    int player = board[i][j];
                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        int[] pattern = evaluator.analyzeLine(board, i, j, player, dir[0], dir[1]);
                        int lineScore = evaluator.getLineScore(pattern);

                        // 活二及以上算阵型
                        if (lineScore >= PatternEvaluator.SCORE_LIVE_TWO) {
                            if (player == GomokuBoard.WHITE) {
                                whiteFormations++;
                            } else {
                                blackFormations++;
                            }
                        }
                    }
                }
            }
        }

        // 任何一方有2个及以上阵型 = 明确进入残局
        return whiteFormations >= 2 || blackFormations >= 2;
    }

    /**
     * 获取落子的威胁等级
     */
    private int getMoveThreatLevel(int[][] board, int row, int col, int player) {
        board[row][col] = player;

        int maxLevel = 0;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, player, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);

            int level = 0;
            if (lineScore >= PatternEvaluator.SCORE_FIVE) level = 6;
            else if (lineScore >= PatternEvaluator.SCORE_FOUR) level = 5;
            else if (lineScore >= PatternEvaluator.SCORE_RUSH_FOUR) level = 5;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) level = 4;
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) level = 3;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_TWO) level = 2;

            maxLevel = Math.max(maxLevel, level);
        }

        board[row][col] = GomokuBoard.EMPTY;
        return maxLevel;
    }

    /**
     * 获取残局着法
     */
    public int[] getEndgameMove(int[][] board) {
        // 检查必胜/必防
        int[] move = searchExhaustive(board, GomokuBoard.WHITE);
        if (move != null) {
            return move;
        }

        return null;
    }

    /**
     * 穷举搜索 - 找最优着法
     */
    private int[] searchExhaustive(int[][] board, int player) {
        int opponent = (player == GomokuBoard.WHITE) ? GomokuBoard.BLACK : GomokuBoard.WHITE;

        // 获取候选位置（只在有棋子的地方附近找）
        List<int[]> candidates = getCandidateSpots(board, player);
        if (candidates.isEmpty()) {
            candidates = getAllEmptySpots(board);
        }

        if (candidates.isEmpty()) return null;

        // 检查必胜着法
        for (int[] spot : candidates) {
            board[spot[0]][spot[1]] = player;
            if (checkWin(board, spot[0], spot[1], player)) {
                board[spot[0]][spot[1]] = GomokuBoard.EMPTY;
                return spot;
            }
            board[spot[0]][spot[1]] = GomokuBoard.EMPTY;
        }

        // 检查必防着法
        for (int[] spot : candidates) {
            board[spot[0]][spot[1]] = opponent;
            if (checkWin(board, spot[0], spot[1], opponent)) {
                board[spot[0]][spot[1]] = GomokuBoard.EMPTY;
                return spot;
            }
            board[spot[0]][spot[1]] = GomokuBoard.EMPTY;
        }

        // 评估每个候选位置
        return evaluateCandidates(board, candidates, player);
    }

    /**
     * 获取候选落子位置
     */
    private List<int[]> getCandidateSpots(int[][] board, int player) {
        Set<String> candidates = new HashSet<>();

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    // 在棋子周围2格内找空位
                    for (int di = -2; di <= 2; di++) {
                        for (int dj = -2; dj <= 2; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < BOARD_SIZE && nj >= 0 && nj < BOARD_SIZE
                                    && board[ni][nj] == GomokuBoard.EMPTY) {
                                candidates.add(ni + "," + nj);
                            }
                        }
                    }
                }
            }
        }

        List<int[]> result = new ArrayList<>();
        for (String pos : candidates) {
            String[] parts = pos.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    private List<int[]> getAllEmptySpots(int[][] board) {
        List<int[]> spots = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    spots.add(new int[]{i, j});
                }
            }
        }
        return spots;
    }

    /**
     * 评估候选位置
     */
    private int[] evaluateCandidates(int[][] board, List<int[]> candidates, int player) {
        int opponent = (player == GomokuBoard.WHITE) ? GomokuBoard.BLACK : GomokuBoard.WHITE;
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int[] spot : candidates) {
            int score = 0;

            // 评估落子后的棋型
            board[spot[0]][spot[1]] = player;
            int attackScore = evaluator.evaluatePosition(board, spot[0], spot[1], player);
            board[spot[0]][spot[1]] = GomokuBoard.EMPTY;

            board[spot[0]][spot[1]] = opponent;
            int defenseScore = evaluator.evaluatePosition(board, spot[0], spot[1], opponent);
            board[spot[0]][spot[1]] = GomokuBoard.EMPTY;

            // 残局侧重攻轻守
            score = attackScore * 2 + defenseScore;

            // 距离中心加权
            int dist = Math.abs(spot[0] - CENTER) + Math.abs(spot[1] - CENTER);
            score += Math.max(0, 20 - dist);

            if (score > bestScore) {
                bestScore = score;
                bestMove = spot;
            }
        }

        return bestMove;
    }

    // ===== 工具方法 =====

    private int countPieces(int[][] board) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) count++;
            }
        }
        return count;
    }

    private boolean checkWin(int[][] board, int row, int col, int player) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1;
            count += countDir(board, row, col, player, dir[0], dir[1]);
            count += countDir(board, row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countDir(int[][] board, int row, int col, int player, int dR, int dC) {
        int count = 0;
        int r = row + dR, c = col + dC;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dR;
            c += dC;
        }
        return count;
    }
}
