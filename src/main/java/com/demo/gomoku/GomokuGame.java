package com.demo.gomoku;

import java.util.*;

/**
 * 五子棋游戏逻辑类（支持AI对战）
 */
public class GomokuGame {
    
    public static final int BOARD_SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;  // 黑方（玩家）
    public static final int WHITE = 2;  // 白方（AI）
    
    // 难度级别
    public static final int EASY = 1;
    public static final int MEDIUM = 2;
    public static final int HARD = 3;
    
    // 棋型分数
    private static final int SCORE_FIVE = 10000000;
    private static final int SCORE_FOUR = 1000000;
    private static final int SCORE_RUSH_FOUR = 100000;
    private static final int SCORE_LIVE_THREE = 50000;
    private static final int SCORE_SLEEP_THREE_A = 5000;
    private static final int SCORE_SLEEP_THREE_B = 1000;
    private static final int SCORE_LIVE_TWO = 2000;
    private static final int SCORE_SLEEP_TWO = 200;
    private static final int SCORE_ONE = 10;
    
    private int[][] board;
    private int currentPlayer;
    private boolean gameOver;
    private int winner;
    private int moveCount;
    private int difficulty;
    
    public GomokuGame() {
        this(EASY);
    }
    
    public GomokuGame(int difficulty) {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        currentPlayer = BLACK;
        gameOver = false;
        winner = EMPTY;
        moveCount = 0;
        this.difficulty = difficulty;
    }
    
    public boolean makeMove(int row, int col) {
        return makeMove(row, col, currentPlayer);
    }
    
    public boolean makeMove(int row, int col, int player) {
        if (gameOver || row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return false;
        }
        if (board[row][col] != EMPTY) {
            return false;
        }
        
        board[row][col] = player;
        moveCount++;
        
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
     * AI落子
     */
    public int[] aiMove() {
        if (gameOver || currentPlayer != WHITE) {
            return null;
        }
        
        int[] move = calculateBestMove();
        if (move != null) {
            makeMove(move[0], move[1], WHITE);
        }
        return move;
    }
    
    /**
     * 计算AI最佳落子位置
     */
    private int[] calculateBestMove() {
        List<int[]> candidates = getCandidateMoves();
        
        if (candidates.isEmpty()) {
            return new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2};
        }
        
        int center = BOARD_SIZE / 2;
        
        // 第一步下中间
        if (moveCount == 0) {
            return new int[]{center, center};
        }
        
        // 困难模式：使用极大极小算法
        if (difficulty == HARD) {
            return calculateHardMove(candidates, center);
        }
        
        // 中等/简单模式
        return calculateEasyMove(candidates, center);
    }
    
    /**
     * 困难模式AI - 极大极小 + Alpha-Beta剪枝
     */
    private int[] calculateHardMove(List<int[]> candidates, int center) {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = candidates.isEmpty() ? new int[]{center, center} : candidates.get(0);
        
        // 搜索深度增加到3
        int searchDepth = 3;
        
        // 限制候选数量以保证性能
        if (candidates.size() > 12) {
            candidates = candidates.subList(0, 12);
        }
        
        for (int[] move : candidates) {
            board[move[0]][move[1]] = WHITE;
            int score = minmax(searchDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            board[move[0]][move[1]] = EMPTY;
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove;
    }
    
    /**
     * 极大极小搜索（带杀手启发）
     */
    private int minmax(int depth, int alpha, int beta, boolean isMaximizing) {
        // 检查终止条件
        int winner = checkBoardWin();
        if (winner == WHITE) return SCORE_FIVE * depth;
        if (winner == BLACK) return -SCORE_FIVE * depth;
        if (depth == 0) return evaluateBoard();
        
        List<int[]> candidates = getCandidateMovesForSearch(depth);
        
        if (candidates.isEmpty()) {
            return evaluateBoard();
        }
        
        // 空步启发：在叶节点允许跳过
        if (depth <= 1) {
            int standPat = evaluateBoard();
            if (isMaximizing) {
                if (standPat >= beta) return beta;
                alpha = Math.max(alpha, standPat);
            } else {
                if (standPat <= alpha) return alpha;
                beta = Math.min(beta, standPat);
            }
        }
        
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < Math.min(candidates.size(), 15); i++) {
                int[] move = candidates.get(i);
                board[move[0]][move[1]] = WHITE;
                int eval = minmax(depth - 1, alpha, beta, false);
                board[move[0]][move[1]] = EMPTY;
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < Math.min(candidates.size(), 15); i++) {
                int[] move = candidates.get(i);
                board[move[0]][move[1]] = BLACK;
                int eval = minmax(depth - 1, alpha, beta, true);
                board[move[0]][move[1]] = EMPTY;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }
    
    /**
     * 获取搜索候选位置（带评估排序）
     */
    private List<int[]> getCandidateMovesForSearch(int depth) {
        Set<String> candidates = new HashSet<>();
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    for (int di = -3; di <= 3; di++) {
                        for (int dj = -3; dj <= 3; dj++) {
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
        
        List<int[]> result = new ArrayList<>();
        for (String pos : candidates) {
            String[] parts = pos.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        
        // 评估排序，优先搜索高价值位置
        result.sort((a, b) -> {
            int scoreA = quickEvaluate(a[0], a[1]);
            int scoreB = quickEvaluate(b[0], b[1]);
            return scoreB - scoreA;
        });
        
        return result.subList(0, Math.min(result.size(), 18));
    }
    
    /**
     * 评估整个棋盘
     */
    private int evaluateBoard() {
        int score = 0;
        int center = BOARD_SIZE / 2;
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    int player = board[i][j];
                    int dirScore = evaluateCellDirections(i, j, player);
                    
                    // 位置权重
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
        
        return score;
    }
    
    /**
     * 评估某个位置在四个方向上的价值
     */
    private int evaluateCellDirections(int row, int col, int player) {
        int score = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int[] pattern = analyzeLine(row, col, player, dir[0], dir[1]);
            score += getLineScore(pattern, player);
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
        
        // 正方向（连续统计）
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
        
        // 反方向（连续统计）
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
    
    private int getLineScore(int[] pattern, int player) {
        int count = pattern[0];
        int leftSpace = pattern[1];
        int rightSpace = pattern[2];
        int leftBlocked = pattern[3];
        int rightBlocked = pattern[4];
        int spaceCount = leftSpace + rightSpace;
        int blockCount = leftBlocked + rightBlocked;
        
        // 五连
        if (count >= 5) return SCORE_FIVE;
        
        // 活四
        if (count == 4 && spaceCount == 2) return SCORE_FOUR;
        
        // 冲四
        if (count == 4 && spaceCount == 1) return SCORE_RUSH_FOUR;
        
        // 活三
        if (count == 3 && spaceCount == 2) return SCORE_LIVE_THREE;
        
        // 眠三（一端有空格，一端被堵）
        if (count == 3 && spaceCount == 1 && blockCount == 1) return SCORE_SLEEP_THREE_A;
        
        // 活二（两边都有空格的两连）
        if (count == 2 && spaceCount == 2) return SCORE_LIVE_TWO;
        
        // 眠二
        if (count == 2 && spaceCount == 1) return SCORE_SLEEP_TWO;
        
        // 活一
        if (count == 1 && spaceCount == 2) return SCORE_ONE;
        
        return count;
    }
    
    /**
     * 检查棋盘上是否已有胜者
     */
    private int checkBoardWin() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    if (checkWinAt(i, j)) {
                        return board[i][j];
                    }
                }
            }
        }
        return EMPTY;
    }
    
    private boolean checkWinAt(int row, int col) {
        int player = board[row][col];
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int count = 1;
            count += countInDirection(row, col, player, dir[0], dir[1]);
            count += countInDirection(row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }
    
    /**
     * 获取搜索候选位置
     */
    private List<int[]> getCandidateMovesForSearch() {
        Set<String> candidates = new HashSet<>();
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != EMPTY) {
                    for (int di = -2; di <= 2; di++) {
                        for (int dj = -2; dj <= 2; dj++) {
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
        
        List<int[]> result = new ArrayList<>();
        for (String pos : candidates) {
            String[] parts = pos.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        
        // 按评估分数排序
        int center = BOARD_SIZE / 2;
        result.sort((a, b) -> {
            int scoreA = quickEvaluate(a[0], a[1]);
            int scoreB = quickEvaluate(b[0], b[1]);
            int distA = Math.abs(a[0] - center) + Math.abs(a[1] - center);
            int distB = Math.abs(b[0] - center) + Math.abs(b[1] - center);
            return (scoreB + Math.max(0, 10 - distB)) - (scoreA + Math.max(0, 10 - distA));
        });
        
        // 只返回前20个最佳位置
        if (result.size() > 20) {
            result = result.subList(0, 20);
        }
        
        return result;
    }
    
    private int quickEvaluate(int row, int col) {
        int score = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int[] pattern = analyzeLine(row, col, WHITE, dir[0], dir[1]);
            score += getLineScore(pattern, WHITE);
            int[] patternB = analyzeLine(row, col, BLACK, dir[0], dir[1]);
            score += getLineScore(patternB, BLACK);
        }
        
        return score;
    }
    
    /**
     * 中等/简单模式
     */
    private int[] calculateEasyMove(List<int[]> candidates, int center) {
        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        
        for (int[] move : candidates) {
            int attackScore = evaluatePositionQuick(move[0], move[1], WHITE);
            int defenseScore = evaluatePositionQuick(move[0], move[1], BLACK);
            
            double defenseWeight = difficulty == MEDIUM ? 1.1 : 0.9;
            int totalScore = attackScore + (int)(defenseScore * defenseWeight);
            
            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 10 - dist);
            
            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }
        
        List<int[]> bestMoves = new ArrayList<>();
        for (int[] move : candidates) {
            int score = scores.get(move[0] + "," + move[1]);
            double threshold = difficulty == MEDIUM ? 0.85 : 0.6;
            if (score >= maxScore * threshold) {
                bestMoves.add(move);
            }
        }
        
        Random rand = new Random();
        return bestMoves.get(rand.nextInt(bestMoves.size()));
    }
    
    private int evaluatePositionQuick(int row, int col, int player) {
        int score = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int[] pattern = analyzeLine(row, col, player, dir[0], dir[1]);
            score += getLineScore(pattern, player);
        }
        
        return score;
    }
    
    /**
     * 获取候选落子位置
     */
    private List<int[]> getCandidateMoves() {
        Set<String> candidates = new HashSet<>();
        int searchRange = difficulty == HARD ? 4 : 3;
        
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
        
        // 困难模式只保留评分最高的位置
        if (difficulty == HARD) {
            int center = BOARD_SIZE / 2;
            result.sort((a, b) -> {
                int scoreA = quickEvaluate(a[0], a[1]);
                int scoreB = quickEvaluate(b[0], b[1]);
                int distA = Math.abs(a[0] - center) + Math.abs(a[1] - center);
                int distB = Math.abs(b[0] - center) + Math.abs(b[1] - center);
                return (scoreB + Math.max(0, 15 - distB) * 10) - (scoreA + Math.max(0, 15 - distA) * 10);
            });
            if (result.size() > 15) {
                result = result.subList(0, 15);
            }
        }
        
        return result;
    }
    
    /**
     * 检查是否五子连珠
     */
    private boolean checkWin(int row, int col) {
        int player = board[row][col];
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int count = 1;
            count += countInDirection(row, col, player, dir[0], dir[1]);
            count += countInDirection(row, col, player, -dir[0], -dir[1]);
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }
    
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
    public int getDifficulty() { return difficulty; }
    
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
    
    public void reset() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        currentPlayer = BLACK;
        gameOver = false;
        winner = EMPTY;
        moveCount = 0;
    }
    
    public void reset(int difficulty) {
        reset();
        this.difficulty = difficulty;
    }
}
