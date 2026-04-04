package com.demo.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 威胁检测器 - 检测棋盘上的关键威胁（支持跳跃棋型）
 */
public class ThreatDetector {

    private final PatternEvaluator evaluator;

    public ThreatDetector() {
        this.evaluator = PatternEvaluator.getInstance();
    }
    
    public ThreatDetector(PatternEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * 检查是否有必胜或必防的棋步（优化版：使用候选列表）
     * @return 需要落子的位置，或 null
     */
    public int[] findImmediateWinOrBlock(int[][] board) {
        // 获取威胁候选位置
        List<int[]> candidates = getThreatCandidates(board);
        
        // 1. AI一步获胜
        int[] winMove = findOneMoveWin(board, GomokuBoard.WHITE, candidates);
        if (winMove != null) return winMove;

        // 2. 玩家一步获胜，必须拦截
        int[] blockWin = findOneMoveWin(board, GomokuBoard.BLACK, candidates);
        if (blockWin != null) return blockWin;

        // 3. AI的跳跃四连（填空即五连）
        int[] aiJumpFour = findJumpFour(board, GomokuBoard.WHITE);
        if (aiJumpFour != null) return aiJumpFour;

        // 4. 玩家的跳跃四连（必须拦截）
        int[] blockJumpFour = findJumpFour(board, GomokuBoard.BLACK);
        if (blockJumpFour != null) return blockJumpFour;

        // 5. 玩家的三连威胁（活三/眠三）- 必须拦截
        int[] blockThree = findExistingThree(board, GomokuBoard.BLACK);
        if (blockThree != null) return blockThree;

        // 6. 玩家的组合威胁（双活三/四三）
        int[] comboThreat = findComboThreat(board, GomokuBoard.BLACK);
        if (comboThreat != null) return comboThreat;

        // 7. 玩家的冲四威胁
        int[] rushFour = findRushFourThreat(board, GomokuBoard.BLACK);
        if (rushFour != null) return rushFour;

        // 8. AI的冲四进攻
        int[] aiRushFour = findRushFourThreat(board, GomokuBoard.WHITE);
        if (aiRushFour != null) return aiRushFour;

        // 9. 玩家的活三威胁（落子后形成活三）
        int[] liveThree = findLiveThreeThreat(board, GomokuBoard.BLACK);
        if (liveThree != null) return liveThree;

        return null;
    }

    /**
     * 获取威胁候选位置：与棋子相邻的空位
     * 这些位置是唯一可能形成威胁的位置
     */
    private List<int[]> getThreatCandidates(int[][] board) {
        Set<String> candidates = new HashSet<>();
        int n = GomokuBoard.BOARD_SIZE;
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    // 检查周围1格内的空位
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if (di == 0 && dj == 0) continue;
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < n && nj >= 0 && nj < n && board[ni][nj] == GomokuBoard.EMPTY) {
                                candidates.add(ni + "," + nj);
                            }
                        }
                    }
                }
            }
        }
        
        if (candidates.isEmpty()) {
            candidates.add((n / 2) + "," + (n / 2));
        }
        
        List<int[]> result = new ArrayList<>();
        for (String pos : candidates) {
            String[] parts = pos.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    /**
     * 查找一步获胜的位置（优化版：使用候选列表）
     */
    public int[] findOneMoveWin(int[][] board, int player, List<int[]> candidates) {
        for (int[] pos : candidates) {
            int i = pos[0], j = pos[1];
            board[i][j] = player;
            boolean hasWin = checkWin(board, i, j);
            board[i][j] = GomokuBoard.EMPTY;
            if (hasWin) return pos;
        }
        return null;
    }

    /**
     * 查找一步获胜的位置（全盘扫描版）
     */
    public int[] findOneMoveWin(int[][] board, int player) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = player;
                    boolean hasWin = checkWin(board, i, j);
                    board[i][j] = GomokuBoard.EMPTY;
                    if (hasWin) {
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找跳跃四连（如 XX_XX, X_XXX, PPP_P 等）
     * 填补空位即可形成五连
     */
    public int[] findJumpFour(int[][] board, int player) {
        int n = GomokuBoard.BOARD_SIZE;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) continue;
                if (player == GomokuBoard.EMPTY) continue;

                // 尝试在此处落子，检查是否形成跳跃四连模式
                // 即：该位置是某个四子(含一空)组的空位
                board[i][j] = player;
                // 检查四个方向是否存在"四子含此位"的模式
                for (int[] dir : GomokuBoard.DIRECTIONS) {
                    int total = 1;
                    // 正方向
                    int r = i + dir[0], c = j + dir[1];
                    while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                        total++;
                        r += dir[0];
                        c += dir[1];
                    }
                    // 反方向
                    r = i - dir[0];
                    c = j - dir[1];
                    while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                        total++;
                        r -= dir[0];
                        c -= dir[1];
                    }
                    if (total >= 4) {
                        board[i][j] = GomokuBoard.EMPTY;
                        return new int[]{i, j};
                    }
                }
                board[i][j] = GomokuBoard.EMPTY;
            }
        }
        return null;
    }

    /**
     * 查找冲四威胁
     */
    public int[] findRushFourThreat(int[][] board, int opponent) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = opponent;
                    if (hasPatternWithCheck(board, i, j, opponent, 4, false)) {
                        board[i][j] = GomokuBoard.EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = GomokuBoard.EMPTY;
                }
            }
        }
        return null;
    }

    /**
     * 查找棋盘上已有的三连（包括活三和眠三）
     * 这是真正的威胁检测
     */
    public int[] findExistingThree(int[][] board, int player) {
        int n = GomokuBoard.BOARD_SIZE;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == player) {
                    // 检查四个方向
                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        int count = 1;
                        int openEnds = 0;
                        int blockedEnds = 0;
                        
                        // 正方向计数
                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
                            openEnds++;
                        } else if (r >= 0 && r < n && c >= 0 && c < n) {
                            blockedEnds++;
                        }
                        
                        // 反方向计数
                        r = i - dir[0];
                        c = j - dir[1];
                        while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                            count++;
                            r -= dir[0];
                            c -= dir[1];
                        }
                        if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
                            openEnds++;
                        } else if (r >= 0 && r < n && c >= 0 && c < n) {
                            blockedEnds++;
                        }
                        
                        // 找到三连
                        if (count == 3) {
                            // 找到活三或眠三的防守位置
                            // 活三：至少一端开放
                            if (openEnds >= 1) {
                                // 返回开放端的位置
                                r = i + dir[0];
                                c = j + dir[1];
                                int r2 = i - dir[0];
                                int c2 = j - dir[1];
                                
                                // 找开放的那一端
                                while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                                    r += dir[0];
                                    c += dir[1];
                                }
                                if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
                                    return new int[]{r, c};
                                }
                                
                                while (r2 >= 0 && r2 < n && c2 >= 0 && c2 < n && board[r2][c2] == player) {
                                    r2 -= dir[0];
                                    c2 -= dir[1];
                                }
                                if (r2 >= 0 && r2 < n && c2 >= 0 && c2 < n && board[r2][c2] == GomokuBoard.EMPTY) {
                                    return new int[]{r2, c2};
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找活三威胁
     */
    public int[] findLiveThreeThreat(int[][] board, int opponent) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = opponent;
                    if (hasPatternWithCheck(board, i, j, opponent, 3, true)) {
                        board[i][j] = GomokuBoard.EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = GomokuBoard.EMPTY;
                }
            }
        }
        return null;
    }

    /**
     * 检查落子后是否形成指定棋型
     */
    private boolean hasPatternWithCheck(int[][] board, int row, int col, int player, int targetCount, boolean requireLive) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1;
            int emptyEnds = 0;

            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                emptyEnds++;
            }

            r = row - dir[0];
            c = col - dir[1];
            while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                count++;
                r -= dir[0];
                c -= dir[1];
            }
            if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                emptyEnds++;
            }

            if (count >= targetCount) {
                if (!requireLive || emptyEnds == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检测双活三/四三组合
     */
    public int[] findComboThreat(int[][] board, int opponent) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = opponent;

                    int liveThreeCount = 0;
                    int rushFourCount = 0;

                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        int count = 1;
                        int emptyEnds = 0;

                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                        }

                        r = i - dir[0];
                        c = j - dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r -= dir[0];
                            c -= dir[1];
                        }
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                        }

                        if (count >= 4 && emptyEnds >= 1) rushFourCount++;
                        if (count == 3 && emptyEnds == 2) liveThreeCount++;
                    }

                    board[i][j] = GomokuBoard.EMPTY;

                    // 双活三或四三组合
                    if (liveThreeCount >= 2 || (rushFourCount >= 1 && liveThreeCount >= 1)) {
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取候选落子位置
     */
    public List<int[]> getCandidateMoves(int[][] board, int searchRange, boolean sortByScore, int limit) {
        Set<String> candidates = new HashSet<>();

        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    for (int di = -searchRange; di <= searchRange; di++) {
                        for (int dj = -searchRange; dj <= searchRange; dj++) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < GomokuBoard.BOARD_SIZE && nj >= 0 && nj < GomokuBoard.BOARD_SIZE
                                    && board[ni][nj] == GomokuBoard.EMPTY) {
                                candidates.add(ni + "," + nj);
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            int center = GomokuBoard.BOARD_SIZE / 2;
            for (int di = -2; di <= 2; di++) {
                for (int dj = -2; dj <= 2; dj++) {
                    int ni = center + di;
                    int nj = center + dj;
                    if (ni >= 0 && ni < GomokuBoard.BOARD_SIZE && nj >= 0 && nj < GomokuBoard.BOARD_SIZE) {
                        candidates.add(ni + "," + nj);
                    }
                }
            }
        }

        List<int[]> result = new ArrayList<>();
        for (String pos : candidates) {
            String[] parts = pos.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }

        if (sortByScore) {
            PatternEvaluator eval = this.evaluator;
            int center = GomokuBoard.BOARD_SIZE / 2;
            result.sort((a, b) -> {
                int scoreA = eval.quickEvaluate(board, a[0], a[1]);
                int scoreB = eval.quickEvaluate(board, b[0], b[1]);
                int distA = Math.abs(a[0] - center) + Math.abs(a[1] - center);
                int distB = Math.abs(b[0] - center) + Math.abs(b[1] - center);
                return (scoreB + Math.max(0, 15 - distB) * 10) -
                       (scoreA + Math.max(0, 15 - distA) * 10);
            });
        }

        if (limit > 0 && result.size() > limit) {
            result = result.subList(0, limit);
        }

        return result;
    }

    /**
     * 检查是否五子连珠
     */
    public boolean checkWin(int[][] board, int row, int col) {
        int player = board[row][col];
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1;
            count += evaluator.countInDirection(board, row, col, player, dir[0], dir[1]);
            count += evaluator.countInDirection(board, row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }
}
