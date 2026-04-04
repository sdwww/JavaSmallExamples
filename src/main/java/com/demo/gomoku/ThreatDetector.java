package com.demo.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 威胁检测器 - 检测棋盘上的关键威胁
 */
public class ThreatDetector {
    
    private final PatternEvaluator evaluator;
    
    public ThreatDetector() {
        this.evaluator = new PatternEvaluator();
    }
    
    /**
     * 检查是否有必胜或必防的棋步（增强版）
     * @param board 棋盘
     * @return 需要落子的位置，或 null
     */
    public int[] findImmediateWinOrBlock(int[][] board) {
        // 1. 检查 AI 是否能一步获胜
        int[] winMove = findOneMoveWin(board, GomokuBoard.WHITE);
        if (winMove != null) return winMove;
        
        // 2. 检查玩家是否能一步获胜，必须拦截
        int[] blockWin = findOneMoveWin(board, GomokuBoard.BLACK);
        if (blockWin != null) return blockWin;
        
        // 3. 检查玩家的组合威胁（双活三/四三）
        int[] comboThreat = findComboThreat(board, GomokuBoard.BLACK);
        if (comboThreat != null) return comboThreat;
        
        // 4. 检查玩家的冲四威胁
        int[] rushFour = findRushFourThreat(board, GomokuBoard.BLACK);
        if (rushFour != null) return rushFour;
        
        // 5. 检查 AI 的冲四进攻机会
        int[] aiRushFour = findRushFourThreat(board, GomokuBoard.WHITE);
        if (aiRushFour != null) return aiRushFour;
        
        // 6. 检查玩家的活三威胁
        int[] liveThree = findLiveThreeThreat(board, GomokuBoard.BLACK);
        if (liveThree != null) return liveThree;
        
        return null;
    }
    
    /**
     * 查找一步就能获胜的位置
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
     * 查找冲四威胁（落子后形成4连，一端被封）
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
     * 查找活三威胁（落子后形成活三）
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
     * @param requireLive 是否要求是活棋型（两端都空）
     */
    private boolean hasPatternWithCheck(int[][] board, int row, int col, int player, int targetCount, boolean requireLive) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1;
            int emptyEnds = 0;
            
            // 正方向
            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                emptyEnds++;
            }
            
            // 反方向
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
     * 检测双活三/四三组合（极高威胁）
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
                        
                        // 正方向
                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                        }
                        
                        // 反方向
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
     * 扫描棋盘查找关键威胁（4 连或活 3）
     * 直接分析现有棋子，而不是模拟落子
     */
    public int[] findCriticalThreat(int[][] board, int player) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] != player) continue;
                
                for (int[] dir : GomokuBoard.DIRECTIONS) {
                    int countForward = 0;
                    int r = i + dir[0], c = j + dir[1];
                    while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                        countForward++;
                        r += dir[0];
                        c += dir[1];
                    }
                    
                    int countBackward = 0;
                    r = i - dir[0];
                    c = j - dir[1];
                    while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                        countBackward++;
                        r -= dir[0];
                        c -= dir[1];
                    }
                    
                    int totalCount = 1 + countForward + countBackward;
                    
                    // 如果有 4 个或更多连续棋子
                    if (totalCount >= 4) {
                        int frontR = i + countForward * dir[0];
                        int frontC = j + countForward * dir[1];
                        int backR = i - countBackward * dir[0];
                        int backC = j - countBackward * dir[1];
                        
                        int nextFrontR = frontR + dir[0];
                        int nextFrontC = frontC + dir[1];
                        if (nextFrontR >= 0 && nextFrontR < GomokuBoard.BOARD_SIZE && nextFrontC >= 0 && nextFrontC < GomokuBoard.BOARD_SIZE 
                            && board[nextFrontR][nextFrontC] == GomokuBoard.EMPTY) {
                            return new int[]{nextFrontR, nextFrontC};
                        }
                        
                        int nextBackR = backR - dir[0];
                        int nextBackC = backC - dir[1];
                        if (nextBackR >= 0 && nextBackR < GomokuBoard.BOARD_SIZE && nextBackC >= 0 && nextBackC < GomokuBoard.BOARD_SIZE 
                            && board[nextBackR][nextBackC] == GomokuBoard.EMPTY) {
                            return new int[]{nextBackR, nextBackC};
                        }
                    }
                    
                    // 如果有 3 个连续棋子，检查是否是活三
                    if (totalCount == 3) {
                        int frontR = i + countForward * dir[0];
                        int frontC = j + countForward * dir[1];
                        int backR = i - countBackward * dir[0];
                        int backC = j - countBackward * dir[1];
                        
                        int nextFrontR = frontR + dir[0];
                        int nextFrontC = frontC + dir[1];
                        int nextBackR = backR - dir[0];
                        int nextBackC = backC - dir[1];
                        
                        boolean frontEmpty = nextFrontR >= 0 && nextFrontR < GomokuBoard.BOARD_SIZE && nextFrontC >= 0 && nextFrontC < GomokuBoard.BOARD_SIZE 
                                          && board[nextFrontR][nextFrontC] == GomokuBoard.EMPTY;
                        boolean backEmpty = nextBackR >= 0 && nextBackR < GomokuBoard.BOARD_SIZE && nextBackC >= 0 && nextBackC < GomokuBoard.BOARD_SIZE 
                                         && board[nextBackR][nextBackC] == GomokuBoard.EMPTY;
                        
                        if (frontEmpty && backEmpty) {
                            int center = GomokuBoard.BOARD_SIZE / 2;
                            int frontDist = Math.abs(nextFrontR - center) + Math.abs(nextFrontC - center);
                            int backDist = Math.abs(nextBackR - center) + Math.abs(nextBackC - center);
                            return frontDist < backDist ? new int[]{nextFrontR, nextFrontC} : new int[]{nextBackR, nextBackC};
                        }
                        
                        if (frontEmpty) return new int[]{nextFrontR, nextFrontC};
                        if (backEmpty) return new int[]{nextBackR, nextBackC};
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取候选落子位置
     * @param searchRange 搜索范围
     * @param sortByScore 是否按评分排序
     * @param limit 限制返回数量（0表示不限制）
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
        
        // 如果棋盘为空，返回中心区域
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
        
        // 按评分排序
        if (sortByScore) {
            PatternEvaluator eval = new PatternEvaluator();
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
        
        // 限制返回数量
        if (limit > 0 && result.size() > limit) {
            result = result.subList(0, limit);
        }
        
        return result;
    }
    
    /**
     * 检查某个位置是否形成了指定数量的连子（任意方向）
     */
    public boolean hasPattern(int[][] board, int row, int col, int player, int count) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int c = 1;
            c += evaluator.countInDirection(board, row, col, player, dir[0], dir[1]);
            c += evaluator.countInDirection(board, row, col, player, -dir[0], -dir[1]);
            if (c >= count) return true;
        }
        return false;
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
    
    /**
     * 检查是否活三
     */
    public boolean isLiveThree(int[][] board, int row, int col, int player) {
        return evaluator.isLiveThree(board, row, col, player);
    }
}
