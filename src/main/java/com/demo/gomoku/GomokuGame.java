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
    private static final int SCORE_FOUR = 1000000;
    private static final int SCORE_RUSH_FOUR = 100000;
    private static final int SCORE_LIVE_THREE = 50000;
    private static final int SCORE_SLEEP_THREE = 5000;
    private static final int SCORE_LIVE_TWO = 2000;
    private static final int SCORE_SLEEP_TWO = 200;
    private static final int SCORE_ONE = 10;
    
    // AI 搜索配置（优化后）
    private static final int MAX_SEARCH_TIME_MS = 3000; // 增加思考时间至 3 秒
    private static final int MAX_DEPTH = 6; // 困难模式最大深度增至 6
    
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
        
        // 困难模式：使用极大极小 + Alpha-Beta剪枝 + 迭代加深
        if (difficulty == Difficulty.HARD) {
            // 1. 先检查是否有必胜/必防的一步
            int[] winMove = findImmediateWinOrBlock();
            if (winMove != null) return winMove;
            
            return calculateHardMove(candidates, startTime);
        }
        
        // 中等/简单模式
        return calculateEasyMove(candidates);
    }
    
    /**
     * 检查是否有必胜或必防的棋步（优化棋力）
     */
    private int[] findImmediateWinOrBlock() {
        List<int[]> candidates = getCandidateMoves(difficulty.getSearchRange(), false, 0);
        
        // 1. 检查 AI 是否能一步获胜
        for (int[] move : candidates) {
            board[move[0]][move[1]] = WHITE;
            if (checkWin(move[0], move[1])) {
                board[move[0]][move[1]] = EMPTY;
                return move;
            }
            board[move[0]][move[1]] = EMPTY;
        }
        
        // 2. 检查玩家是否能一步获胜，必须拦截
        for (int[] move : candidates) {
            board[move[0]][move[1]] = BLACK;
            if (checkWin(move[0], move[1])) {
                board[move[0]][move[1]] = EMPTY;
                return move;
            }
            board[move[0]][move[1]] = EMPTY;
        }
        
        return null;
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
            
            // 限制候选数量以保证性能（放宽至 25 个）
            int limit = Math.min(candidates.size(), 25);
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
        
        // 限制搜索数量（放宽至 20 个）
        int limit = Math.min(candidates.size(), 20);
        
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
        return getCandidateMoves(difficulty.getSearchRange(), true, 18);
    }
    
    /**
     * 评估整个棋盘
     */
    private int evaluateBoard() {
        // 使用 Zobrist Hash 作为缓存键（简化版）
        long boardHash = calculateBoardHash();
        if (evaluationCache.containsKey(boardHash)) {
            return evaluationCache.get(boardHash);
        }
        
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
        
        evaluationCache.put(boardHash, score);
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
            if (difficulty == Difficulty.HARD) defenseWeight = 1.8;
            int totalScore = attackScore + (int)(defenseScore * defenseWeight);
            
            // 位置权重
            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 10 - dist);
            
            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }
        
        // 收集高分候选（避免单一最优，增加随机性）
        List<int[]> bestMoves = new ArrayList<>();
        double threshold = difficulty == Difficulty.EASY ? 0.6 : 0.85;
        for (int[] move : candidates) {
            int score = scores.get(move[0] + "," + move[1]);
            if (score >= maxScore * threshold) {
                bestMoves.add(move);
            }
        }
        
        Random rand = new Random();
        return bestMoves.get(rand.nextInt(bestMoves.size()));
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
