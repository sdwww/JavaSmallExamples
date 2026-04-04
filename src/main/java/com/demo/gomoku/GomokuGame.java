package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 五子棋游戏逻辑类（优化版）
 * 优化点：
 * 1. 使用 Difficulty 枚举替代魔法数字
 * 2. 合并重复的候选位置生成逻辑
 * 3. AI 算法使用迭代加深优化
 * 4. 添加棋盘评估缓存
 * 5. 使用常量统一管理
 */
public class GomokuGame {
    
    public static final int BOARD_SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;  // 黑方（玩家）
    public static final int WHITE = 2;  // 白方（AI）
    
    // 方向数组（复用，减少对象创建）
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    
    // 棋型分数（提高差距，让AI更重视高级棋型）
    private static final int SCORE_FIVE = 10000000;
    private static final int SCORE_FOUR = 2000000;
    private static final int SCORE_RUSH_FOUR = 500000;
    private static final int SCORE_LIVE_THREE = 100000;
    private static final int SCORE_SLEEP_THREE = 15000;
    private static final int SCORE_LIVE_TWO = 3000;
    private static final int SCORE_SLEEP_TWO = 300;
    private static final int SCORE_ONE = 20;
    
    // AI 搜索配置（优化后）
    private static final int MAX_SEARCH_TIME_MS = 12000; // 增加思考时间至 12 秒
    private static final int MAX_DEPTH = 12; // 困难模式最大深度增至 12
    
    private int[][] board;
    private int currentPlayer;
    private boolean gameOver;
    private int winner;
    private int moveCount;
    private Difficulty difficulty;
    
    // 棋盘评估缓存（优化性能）
    private final Map<Long, Integer> evaluationCache = new ConcurrentHashMap<>();
    
    // 杀手启发（优化搜索顺序）
    private int[] killerMoveA = null;
    private int[] killerMoveB = null;
    
    public GomokuGame() {
        this(Difficulty.MEDIUM);
    }
    
    public GomokuGame(Difficulty difficulty) {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        currentPlayer = BLACK;
        gameOver = false;
        winner = EMPTY;
        moveCount = 0;
        this.difficulty = difficulty;
    }
    
    public GomokuGame(int difficultyLevel) {
        this(Difficulty.fromLevel(difficultyLevel));
    }
    
    /**
     * 落子
     */
    public boolean makeMove(int row, int col) {
        return makeMove(row, col, currentPlayer);
    }
    
    /**
     * 指定玩家落子
     */
    public boolean makeMove(int row, int col, int player) {
        if (gameOver || row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return false;
        }
        if (board[row][col] != EMPTY) {
            return false;
        }
        
        board[row][col] = player;
        moveCount++;
        evaluationCache.clear(); // 清空缓存
        
        if (checkWin(row, col)) {
            gameOver = true;
            winner = player;
        } else if (moveCount == BOARD_SIZE * BOARD_SIZE) {
            gameOver = true;
            winner = EMPTY;
        } else {
            currentPlayer = (currentPlayer == BLACK) ? WHITE : BLACK;
        }
        
        return true;
    }
    
    /**
     * AI落子（主入口）
     */
    public int[] aiMove() {
        if (gameOver || currentPlayer != WHITE) {
            return null;
        }
        
        long startTime = System.currentTimeMillis();
        int[] move = calculateBestMove(startTime);
        
        if (move != null) {
            makeMove(move[0], move[1], WHITE);
        }
        return move;
    }
    
    /**
     * 计算AI最佳落子位置
     */
    private int[] calculateBestMove(long startTime) {
        List<int[]> candidates = getCandidateMoves();
        
        if (candidates.isEmpty()) {
            return new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2};
        }
        
        // 第一步下中间
        if (moveCount <= 1) {
            int center = BOARD_SIZE / 2;
            // 如果中间已被占用，选择附近位置
            if (board[center][center] != EMPTY) {
                return candidates.get(0);
            }
            return new int[]{center, center};
        }
        
        // 【所有难度都必须检查】必胜/必防/关键威胁
        int[] winMove = findImmediateWinOrBlock();
        if (winMove != null) return winMove;
        
        // 困难模式：使用极大极小 + Alpha-Beta剪枝 + 迭代加深
        if (difficulty == Difficulty.HARD) {
            return calculateHardMove(candidates, startTime);
        }
        
        // 中等/简单模式
        return calculateEasyMove(candidates);
    }
    
    /**
     * 检查是否有必胜或必防的棋步（增强版）
     */
    private int[] findImmediateWinOrBlock() {
        // 1. 检查 AI 是否能一步获胜
        int[] winMove = findOneMoveWin(WHITE);
        if (winMove != null) return winMove;
        
        // 2. 检查玩家是否能一步获胜，必须拦截
        int[] blockWin = findOneMoveWin(BLACK);
        if (blockWin != null) return blockWin;
        
        // 3. 检查玩家的组合威胁（双活三/四三）
        int[] comboThreat = findComboThreat(BLACK);
        if (comboThreat != null) return comboThreat;
        
        // 4. 检查玩家的冲四威胁
        int[] rushFour = findRushFourThreat(BLACK);
        if (rushFour != null) return rushFour;
        
        // 5. 检查 AI 的冲四进攻机会
        int[] aiRushFour = findRushFourThreat(WHITE);
        if (aiRushFour != null) return aiRushFour;
        
        // 6. 检查玩家的活三威胁
        int[] liveThree = findLiveThreeThreat(BLACK);
        if (liveThree != null) return liveThree;
        
        return null;
    }
    
    /**
     * 查找一步就能获胜的位置
     */
    private int[] findOneMoveWin(int player) {
        // 全盘扫描所有空位
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = player;
                    if (checkWin(i, j)) {
                        board[i][j] = EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = EMPTY;
                }
            }
        }
        return null;
    }
    
    /**
     * 查找冲四威胁（落子后形成4连，一端被封）
     */
    private int[] findRushFourThreat(int opponent) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = opponent;
                    if (hasPatternWithCheck(i, j, opponent, 4, false)) {
                        board[i][j] = EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = EMPTY;
                }
            }
        }
        return null;
    }
    
    /**
     * 查找活三威胁（落子后形成活三）
     */
    private int[] findLiveThreeThreat(int opponent) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = opponent;
                    if (hasPatternWithCheck(i, j, opponent, 3, true)) {
                        board[i][j] = EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = EMPTY;
                }
            }
        }
        return null;
    }
    
    /**
     * 检查落子后是否形成指定棋型
     * @param requireLive 是否要求是活棋型（两端都空）
     */
    private boolean hasPatternWithCheck(int row, int col, int player, int targetCount, boolean requireLive) {
        for (int[] dir : DIRECTIONS) {
            int count = 1;
            int emptyEnds = 0;
            
            // 正方向
            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == EMPTY) {
                emptyEnds++;
            }
            
            // 反方向
            r = row - dir[0];
            c = col - dir[1];
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
                count++;
                r -= dir[0];
                c -= dir[1];
            }
            if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == EMPTY) {
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
     * 查找眠三威胁位置（增强防守）
     */
    private int[] findSleepThree(int opponent) {
        List<int[]> allCandidates = getCandidateMoves(2, false, 0);
        
        for (int[] move : allCandidates) {
            board[move[0]][move[1]] = opponent;
            
            // 检查是否形成眠三
            if (hasPattern(move[0], move[1], opponent, 3)) {
                // 检查是否活三（两端都空）
                if (!isLiveThree(move[0], move[1], opponent)) {
                    board[move[0]][move[1]] = EMPTY;
                    return move;
                }
            }
            board[move[0]][move[1]] = EMPTY;
        }
        return null;
    }
    
    /**
     * 检查是否是活三（两端无阻碍）
     */
    private boolean isLiveThree(int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int forward = countInDirection(row, col, player, dir[0], dir[1]);
            int backward = countInDirection(row, col, player, -dir[0], -dir[1]);
            
            if (forward + backward >= 2) {
                int frontR = row + (forward + 1) * dir[0];
                int frontC = col + (forward + 1) * dir[1];
                int backR = row - (backward + 1) * dir[0];
                int backC = col - (backward + 1) * dir[1];
                
                boolean frontEmpty = isValidEmpty(frontR, frontC);
                boolean backEmpty = isValidEmpty(backR, backC);
                
                if (frontEmpty && backEmpty) return true;
            }
        }
        return false;
    }
    
    /**
     * 检查位置是否有效且为空
     */
    private boolean isValidEmpty(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] == EMPTY;
    }
    
    /**
     * 扫描棋盘查找关键威胁（4 连或活 3）
     * 直接分析现有棋子，而不是模拟落子
     */
    private int[] findCriticalThreat(int player) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != player) continue;
                
                // 对每个棋子，检查四个方向的完整连线
                for (int[] dir : DIRECTIONS) {
                    // 向前数连续棋子数
                    int countForward = 0;
                    int r = i + dir[0], c = j + dir[1];
                    while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
                        countForward++;
                        r += dir[0];
                        c += dir[1];
                    }
                    
                    // 向后数连续棋子数
                    int countBackward = 0;
                    r = i - dir[0];
                    c = j - dir[1];
                    while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
                        countBackward++;
                        r -= dir[0];
                        c -= dir[1];
                    }
                    
                    int totalCount = 1 + countForward + countBackward;
                    
                    // 如果有 4 个或更多连续棋子
                    if (totalCount >= 4) {
                        // 找到这组棋子的两端
                        int frontR = i + countForward * dir[0];
                        int frontC = j + countForward * dir[1];
                        int backR = i - countBackward * dir[0];
                        int backC = j - countBackward * dir[1];
                        
                        // 检查前端是否空
                        int nextFrontR = frontR + dir[0];
                        int nextFrontC = frontC + dir[1];
                        if (nextFrontR >= 0 && nextFrontR < BOARD_SIZE && nextFrontC >= 0 && nextFrontC < BOARD_SIZE 
                            && board[nextFrontR][nextFrontC] == EMPTY) {
                            return new int[]{nextFrontR, nextFrontC};
                        }
                        
                        // 检查后端是否空
                        int nextBackR = backR - dir[0];
                        int nextBackC = backC - dir[1];
                        if (nextBackR >= 0 && nextBackR < BOARD_SIZE && nextBackC >= 0 && nextBackC < BOARD_SIZE 
                            && board[nextBackR][nextBackC] == EMPTY) {
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
                        
                        boolean frontEmpty = nextFrontR >= 0 && nextFrontR < BOARD_SIZE && nextFrontC >= 0 && nextFrontC < BOARD_SIZE 
                                          && board[nextFrontR][nextFrontC] == EMPTY;
                        boolean backEmpty = nextBackR >= 0 && nextBackR < BOARD_SIZE && nextBackC >= 0 && nextBackC < BOARD_SIZE 
                                         && board[nextBackR][nextBackC] == EMPTY;
                        
                        // 活三：两端都空，必须拦截一端
                        if (frontEmpty && backEmpty) {
                            int center = BOARD_SIZE / 2;
                            int frontDist = Math.abs(nextFrontR - center) + Math.abs(nextFrontC - center);
                            int backDist = Math.abs(nextBackR - center) + Math.abs(nextBackC - center);
                            return frontDist < backDist ? new int[]{nextFrontR, nextFrontC} : new int[]{nextBackR, nextBackC};
                        }
                        
                        // 眠三：只有一端空
                        if (frontEmpty) return new int[]{nextFrontR, nextFrontC};
                        if (backEmpty) return new int[]{nextBackR, nextBackC};
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 查找特定威胁棋步
     * @param player 检查的玩家
     * @param targetCount 目标连子数（4=活四/冲四，3=活三）
     * @param mustBlock 是否必须拦截（活四必须拦，冲四尽量拦）
     */
    private int[] findThreatMove(int player, int targetCount, boolean mustBlock) {
        List<int[]> allCandidates = getCandidateMoves(2, false, 0);
        
        for (int[] move : allCandidates) {
            board[move[0]][move[1]] = player;
            
            // 检查落子后是否形成目标棋型
            if (hasPattern(move[0], move[1], player, targetCount)) {
                board[move[0]][move[1]] = EMPTY;
                return move;
            }
            board[move[0]][move[1]] = EMPTY;
        }
        
        return null;
    }
    
    /**
     * 检查某个位置是否形成了指定数量的连子（任意方向）
     */
    private boolean hasPattern(int row, int col, int player, int count) {
        for (int[] dir : DIRECTIONS) {
            int c = 1;
            c += countInDirection(row, col, player, dir[0], dir[1]);
            c += countInDirection(row, col, player, -dir[0], -dir[1]);
            if (c >= count) return true;
        }
        return false;
    }

    /**
     * 困难模式AI - 迭代加深 + 极大极小 + Alpha-Beta剪枝
     */
    private int[] calculateHardMove(List<int[]> candidates, long startTime) {
        int[] bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        
        // 迭代加深：逐步增加搜索深度
        int maxTime = MAX_SEARCH_TIME_MS;
        int depth = 1;
        
        while (depth <= MAX_DEPTH) {
            long remainingTime = maxTime - (System.currentTimeMillis() - startTime);
            if (remainingTime < 200) break; // 预留200ms
            
            // 限制候选数量以保证性能（困难模式放宽至 80 个）
            int limit = Math.min(candidates.size(), 80);
            List<int[]> searchCandidates = candidates.subList(0, limit);
            
            int currentBestScore = Integer.MIN_VALUE;
            int[] currentBestMove = bestMove;
            
            for (int[] move : searchCandidates) {
                board[move[0]][move[1]] = WHITE;
                int score = minmax(depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                board[move[0]][move[1]] = EMPTY;
                
                if (score > currentBestScore) {
                    currentBestScore = score;
                    currentBestMove = move;
                }
                
                // 时间检查
                if (System.currentTimeMillis() - startTime > maxTime * 0.8) {
                    break;
                }
            }
            
            // 保存当前深度的最佳结果
            bestMove = currentBestMove;
            bestScore = currentBestScore;
            depth++;
            
            // 如果找到必胜/必败局面，提前终止
            if (Math.abs(bestScore) >= SCORE_FIVE) {
                break;
            }
        }
        
        return bestMove;
    }
    
    /**
     * 极大极小搜索（带 Alpha-Beta 剪枝）
     */
    private int minmax(int depth, int alpha, int beta, boolean isMaximizing) {
        // 检查终止条件
        int winner = checkBoardWin();
        if (winner == WHITE) return SCORE_FIVE + depth; // 越快赢越好
        if (winner == BLACK) return -SCORE_FIVE - depth; // 越快输越差
        if (depth == 0) return evaluateBoard();
        
        List<int[]> candidates = getCandidateMovesForSearch();
        if (candidates.isEmpty()) {
            return evaluateBoard();
        }
        
        // 限制搜索数量（困难模式放宽至 70 个）
        int limit = Math.min(candidates.size(), 70);
        
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < limit; i++) {
                int[] move = candidates.get(i);
                board[move[0]][move[1]] = WHITE;
                int eval = minmax(depth - 1, alpha, beta, false);
                board[move[0]][move[1]] = EMPTY;
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break; // Beta剪枝
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < limit; i++) {
                int[] move = candidates.get(i);
                board[move[0]][move[1]] = BLACK;
                int eval = minmax(depth - 1, alpha, beta, true);
                board[move[0]][move[1]] = EMPTY;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break; // Alpha剪枝
            }
            return minEval;
        }
    }
    
    /**
     * 获取搜索候选位置（带评估排序）
     */
    private List<int[]> getCandidateMovesForSearch() {
        return getCandidateMoves(difficulty.getSearchRange(), true, 80);
    }
    
    /**
     * 评估整个棋盘
     */
    private int evaluateBoard() {
        // 暂时禁用缓存以确保极高准确率
        // long boardHash = calculateBoardHash();
        // if (evaluationCache.containsKey(boardHash)) {
        //     return evaluationCache.get(boardHash);
        // }
        
        int score = 0;
        int center = BOARD_SIZE / 2;
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    int player = board[i][j];
                    int dirScore = evaluateCellDirections(i, j, player);
                    
                    // 位置权重：靠近中心得分更高
                    int dist = Math.abs(i - center) + Math.abs(j - center);
                    int posWeight = Math.max(0, 14 - dist);
                    
                    if (player == WHITE) {
                        score += dirScore + posWeight * 5;
                    } else {
                        score -= dirScore + posWeight * 5;
                    }
                }
            }
        }
        
        // evaluationCache.put(boardHash, score);
        return score;
    }
    
    /**
     * 简化的棋盘哈希（用于缓存）
     */
    private long calculateBoardHash() {
        long hash = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    // 简化的哈希计算
                    hash ^= ((long)(i * BOARD_SIZE + j) << 32) | board[i][j];
                }
            }
        }
        return hash;
    }
    
    /**
     * 评估某个位置在四个方向上的价值
     */
    private int evaluateCellDirections(int row, int col, int player) {
        int score = 0;
        for (int[] dir : DIRECTIONS) {
            int[] pattern = analyzeLine(row, col, player, dir[0], dir[1]);
            score += getLineScore(pattern);
        }
        return score;
    }
    
    /**
     * 分析一条线上的棋型
     */
    private int[] analyzeLine(int row, int col, int player, int dRow, int dCol) {
        // [连续数, 左空格, 右空格, 左阻塞, 右阻塞]
        int count = 1;
        int leftSpace = 0, rightSpace = 0;
        int leftBlocked = 0, rightBlocked = 0;
        
        // 正方向
        int r = row + dRow, c = col + dCol;
        int maxCheck = 4;
        while (maxCheck > 0 && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
            if (board[r][c] == player) {
                count++;
                r += dRow;
                c += dCol;
            } else if (board[r][c] == EMPTY) {
                rightSpace = 1;
                break;
            } else {
                rightBlocked = 1;
                break;
            }
            maxCheck--;
        }
        
        // 反方向
        r = row - dRow;
        c = col - dCol;
        maxCheck = 4;
        while (maxCheck > 0 && r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
            if (board[r][c] == player) {
                count++;
                r -= dRow;
                c -= dCol;
            } else if (board[r][c] == EMPTY) {
                leftSpace = 1;
                break;
            } else {
                leftBlocked = 1;
                break;
            }
            maxCheck--;
        }
        
        return new int[]{count, leftSpace, rightSpace, leftBlocked, rightBlocked};
    }
    
    /**
     * 根据棋型模式计算分数
     */
    private int getLineScore(int[] pattern) {
        int count = pattern[0];
        int spaceCount = pattern[1] + pattern[2];
        int blockCount = pattern[3] + pattern[4];
        
        // 五连
        if (count >= 5) return SCORE_FIVE;
        
        // 活四
        if (count == 4 && spaceCount == 2) return SCORE_FOUR;
        
        // 冲四
        if (count == 4 && spaceCount == 1) return SCORE_RUSH_FOUR;
        
        // 活三
        if (count == 3 && spaceCount == 2) return SCORE_LIVE_THREE;
        
        // 眠三
        if (count == 3 && spaceCount == 1 && blockCount == 1) return SCORE_SLEEP_THREE;
        
        // 活二
        if (count == 2 && spaceCount == 2) return SCORE_LIVE_TWO;
        
        // 眠二
        if (count == 2 && spaceCount == 1) return SCORE_SLEEP_TWO;
        
        // 活一
        if (count == 1 && spaceCount == 2) return SCORE_ONE;
        
        return count;
    }
    
    /**
     * 快速评估某个位置的价值
     */
    private int quickEvaluate(int row, int col) {
        int score = 0;
        for (int[] dir : DIRECTIONS) {
            int[] pattern = analyzeLine(row, col, WHITE, dir[0], dir[1]);
            score += getLineScore(pattern);
            int[] patternB = analyzeLine(row, col, BLACK, dir[0], dir[1]);
            score += getLineScore(patternB);
        }
        return score;
    }
    
    /**
     * 中等/简单模式AI
     */
    private int[] calculateEasyMove(List<int[]> candidates) {
        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = BOARD_SIZE / 2;
        
        for (int[] move : candidates) {
            int attackScore = evaluatePositionQuick(move[0], move[1], WHITE);
            int defenseScore = evaluatePositionQuick(move[0], move[1], BLACK);
            
            double defenseWeight = difficulty.getDefenseWeight();
            // 困难模式下大幅提高防守权重
            if (difficulty == Difficulty.HARD) defenseWeight = 3.0;
            // 简单/中等模式也提高防守权重以增强难度
            if (difficulty == Difficulty.EASY) defenseWeight = 1.5;
            if (difficulty == Difficulty.MEDIUM) defenseWeight = 1.7;
            
            int totalScore = attackScore + (int)(defenseScore * defenseWeight);
            
            // 【增强难度】对关键棋型额外加分：活三/冲四/活四
            int criticalBonus = evaluateCriticalBonus(move[0], move[1], BLACK);
            totalScore += criticalBonus;
            
            // 位置权重
            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 10 - dist);
            
            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }
        
        // 收集高分候选（避免单一最优，增加随机性）
        List<int[]> bestMoves = new ArrayList<>();
        // 降低阈值让AI选得更保守
        double threshold = difficulty == Difficulty.EASY ? 0.5 : 0.8;
        for (int[] move : candidates) {
            int score = scores.get(move[0] + "," + move[1]);
            if (score >= maxScore * threshold) {
                bestMoves.add(move);
            }
        }
        
        Random rand = new Random();
        return bestMoves.get(rand.nextInt(bestMoves.size()));
    }
    
    /**
     * 评估关键棋型奖励（增强防守）
     */
    private int evaluateCriticalBonus(int row, int col, int opponent) {
        int bonus = 0;
        for (int[] dir : DIRECTIONS) {
            int[] pattern = analyzeLine(row, col, opponent, dir[0], dir[1]);
            int lineScore = getLineScore(pattern);
            
            // 如果形成活四/冲四，巨额奖励
            if (lineScore >= SCORE_FOUR) bonus += 500000;
            // 如果形成活三，也要有较高奖励
            else if (lineScore >= SCORE_LIVE_THREE) bonus += 100000;
            // 如果形成眠三，也要奖励
            else if (lineScore >= SCORE_SLEEP_THREE) bonus += 20000;
            // 活二也有一定价值
            else if (lineScore >= SCORE_LIVE_TWO) bonus += 5000;
        }
        return bonus;
    }
    
    /**
     * 检测双活三/四三组合（极高威胁）
     */
    private int[] findComboThreat(int opponent) {
        // 全盘扫描所有空位
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = opponent;
                    
                    int liveThreeCount = 0;
                    int rushFourCount = 0;
                    
                    for (int[] dir : DIRECTIONS) {
                        int count = 1;
                        int emptyEnds = 0;
                        
                        // 正方向
                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == EMPTY) {
                            emptyEnds++;
                        }
                        
                        // 反方向
                        r = i - dir[0];
                        c = j - dir[1];
                        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r -= dir[0];
                            c -= dir[1];
                        }
                        if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == EMPTY) {
                            emptyEnds++;
                        }
                        
                        if (count >= 4 && emptyEnds >= 1) rushFourCount++;
                        if (count == 3 && emptyEnds == 2) liveThreeCount++;
                    }
                    
                    board[i][j] = EMPTY;
                    
                    // 双活三或四三组合
                    if (liveThreeCount >= 2 || (rushFourCount >= 1 && liveThreeCount >= 1)) {
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }
    
    private int evaluatePositionQuick(int row, int col, int player) {
        int score = 0;
        for (int[] dir : DIRECTIONS) {
            int[] pattern = analyzeLine(row, col, player, dir[0], dir[1]);
            score += getLineScore(pattern);
        }
        return score;
    }
    
    /**
     * 获取候选落子位置（统一实现）
     * @param searchRange 搜索范围
     * @param sortByScore 是否按评分排序
     * @param limit 限制返回数量（0表示不限制）
     */
    private List<int[]> getCandidateMoves(int searchRange, boolean sortByScore, int limit) {
        Set<String> candidates = new HashSet<>();
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    for (int di = -searchRange; di <= searchRange; di++) {
                        for (int dj = -searchRange; dj <= searchRange; dj++) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < BOARD_SIZE && nj >= 0 && nj < BOARD_SIZE 
                                && board[ni][nj] == EMPTY) {
                                candidates.add(ni + "," + nj);
                            }
                        }
                    }
                }
            }
        }
        
        // 如果棋盘为空，返回中心区域
        if (candidates.isEmpty()) {
            int center = BOARD_SIZE / 2;
            for (int di = -2; di <= 2; di++) {
                for (int dj = -2; dj <= 2; dj++) {
                    int ni = center + di;
                    int nj = center + dj;
                    if (ni >= 0 && ni < BOARD_SIZE && nj >= 0 && nj < BOARD_SIZE) {
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
            int center = BOARD_SIZE / 2;
            result.sort((a, b) -> {
                int scoreA = quickEvaluate(a[0], a[1]);
                int scoreB = quickEvaluate(b[0], b[1]);
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
     * 获取候选落子位置（默认调用）
     */
    private List<int[]> getCandidateMoves() {
        return getCandidateMoves(difficulty.getSearchRange(), 
                                 difficulty == Difficulty.HARD, 
                                 difficulty.getCandidateLimit());
    }
    
    /**
     * 检查是否五子连珠
     */
    private boolean checkWin(int row, int col) {
        int player = board[row][col];
        for (int[] dir : DIRECTIONS) {
            int count = 1;
            count += countInDirection(row, col, player, dir[0], dir[1]);
            count += countInDirection(row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }
    
    /**
     * 检查棋盘上是否已有胜者
     */
    private int checkBoardWin() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    if (checkWin(i, j)) {
                        return board[i][j];
                    }
                }
            }
        }
        return EMPTY;
    }
    
    /**
     * 在指定方向上连续计数
     */
    private int countInDirection(int row, int col, int player, int dRow, int dCol) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;
        
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }
    
    // Getters
    public int[][] getBoard() { return board; }
    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isGameOver() { return gameOver; }
    public int getWinner() { return winner; }
    public int getMoveCount() { return moveCount; }
    public Difficulty getDifficulty() { return difficulty; }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    public void setDifficulty(int level) {
        this.difficulty = Difficulty.fromLevel(level);
    }
    
    public void reset() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        currentPlayer = BLACK;
        gameOver = false;
        winner = EMPTY;
        moveCount = 0;
        evaluationCache.clear();
    }
    
    public void reset(Difficulty difficulty) {
        reset();
        this.difficulty = difficulty;
    }
    
    public void reset(int level) {
        reset(Difficulty.fromLevel(level));
    }
}
