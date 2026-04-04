package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.*;


/**
 * 五子棋AI引擎 - 包含所有搜索算法
 */
public class GomokuAI {
    
    // AI 搜索配置
    private static final int MAX_SEARCH_TIME_MS = 12000;
    private static final int MAX_DEPTH = 12;
    
    // 并行搜索配置
    private static final int PARALLEL_THREAD_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    private static final ExecutorService parallelExecutor = Executors.newFixedThreadPool(PARALLEL_THREAD_COUNT, r -> {
        Thread t = new Thread(r, "GomokuAI-Worker");
        t.setDaemon(true);
        return t;
    });
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(parallelExecutor::shutdown));
    }
    
    private final PatternEvaluator evaluator;
    private final ThreatDetector threatDetector;
    private Difficulty difficulty;
    
    // 并行搜索结果容器
    private volatile int[] parallelBestMove = null;
    private volatile int parallelBestScore = Integer.MIN_VALUE;
    private volatile boolean searchCompleted = false;
    
    public GomokuAI(Difficulty difficulty) {
        this.evaluator = new PatternEvaluator();
        this.threatDetector = new ThreatDetector();
        this.difficulty = difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * AI落子（主入口）
     */
    public int[] calculateMove(int[][] board) {
        int moveCount = countPieces(board);
        
        // 第一步下中间
        if (moveCount <= 1) {
            return new int[]{GomokuBoard.BOARD_SIZE / 2, GomokuBoard.BOARD_SIZE / 2};
        }
        
        List<int[]> candidates = threatDetector.getCandidateMoves(
            board, 
            difficulty.getSearchRange(), 
            difficulty == Difficulty.HARD, 
            difficulty.getCandidateLimit()
        );
        
        if (candidates.isEmpty()) {
            return new int[]{GomokuBoard.BOARD_SIZE / 2, GomokuBoard.BOARD_SIZE / 2};
        }
        
        // 【所有难度都必须检查】必胜/必防/关键威胁
        int[] winMove = threatDetector.findImmediateWinOrBlock(board);
        if (winMove != null) return winMove;
        
        // 困难模式：使用极大极小 + Alpha-Beta剪枝 + 迭代加深
        if (difficulty == Difficulty.HARD) {
            return calculateHardMove(board, candidates);
        }
        
        // 中等/简单模式
        return calculateEasyMove(board, candidates);
    }
    
    /**
     * 困难模式AI - 迭代加深 + 极大极小 + Alpha-Beta剪枝
     */
    private int[] calculateHardMove(int[][] board, List<int[]> candidates) {
        int[] bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        
        // 深拷贝棋盘用于搜索
        int[][] searchBoard = copyBoard(board);
        
        // 迭代加深：逐步增加搜索深度
        long startTime = System.currentTimeMillis();
        int maxTime = MAX_SEARCH_TIME_MS;
        int depth = 1;
        
        while (depth <= MAX_DEPTH) {
            long remainingTime = maxTime - (System.currentTimeMillis() - startTime);
            if (remainingTime < 200) break;
            
            int limit = Math.min(candidates.size(), 60);
            List<int[]> searchCandidates = candidates.subList(0, limit);
            
            parallelBestMove = bestMove;
            parallelBestScore = Integer.MIN_VALUE;
            searchCompleted = false;
            
            if (depth >= 3 && PARALLEL_THREAD_COUNT > 1) {
                int[] result = parallelMinmax(searchBoard, searchCandidates, depth, startTime, maxTime);
                if (result != null) {
                    bestMove = result;
                    bestScore = parallelBestScore;
                }
            } else {
                int[] currentBestMove = bestMove;
                int currentBestScore = Integer.MIN_VALUE;
                
                for (int[] move : searchCandidates) {
                    searchBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                    int score = minmax(searchBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                    searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                    
                    if (score > currentBestScore) {
                        currentBestScore = score;
                        currentBestMove = move;
                    }
                    
                    if (System.currentTimeMillis() - startTime > maxTime * 0.8) {
                        break;
                    }
                }
                
                bestMove = currentBestMove;
                bestScore = currentBestScore;
            }
            
            depth++;
            
            if (Math.abs(bestScore) >= PatternEvaluator.SCORE_FIVE) {
                break;
            }
        }
        
        return bestMove;
    }
    
    /**
     * 并行极大极小搜索
     */
    private int[] parallelMinmax(int[][] baseBoard, List<int[]> candidates, int depth, long startTime, int maxTime) {
        int threadCount = Math.min(PARALLEL_THREAD_COUNT, candidates.size());
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        int batchSize = Math.max(1, candidates.size() / threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            final int startIdx = t * batchSize;
            final int endIdx = (t == threadCount - 1) ? candidates.size() : Math.min(startIdx + batchSize, candidates.size());
            
            parallelExecutor.submit(() -> {
                // 每个线程使用独立的棋盘副本
                int[][] threadBoard = copyBoard(baseBoard);
                
                try {
                    int localBestScore = Integer.MIN_VALUE;
                    int[] localBestMove = null;
                    
                    for (int i = startIdx; i < endIdx; i++) {
                        if (searchCompleted) break;
                        
                        int[] move = candidates.get(i);
                        threadBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                        int score = minmax(threadBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                        threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                        
                        if (score > localBestScore) {
                            localBestScore = score;
                            localBestMove = move;
                        }
                        
                        if (System.currentTimeMillis() - startTime > maxTime * 0.75) {
                            break;
                        }
                    }
                    
                    synchronized (this) {
                        if (localBestScore > parallelBestScore) {
                            parallelBestScore = localBestScore;
                            parallelBestMove = localBestMove;
                        }
                        if (parallelBestScore >= PatternEvaluator.SCORE_FIVE * 0.9) {
                            searchCompleted = true;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            long waitTime = maxTime - (System.currentTimeMillis() - startTime);
            latch.await(Math.max(100, waitTime * 80 / 100), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        searchCompleted = true;
        return parallelBestMove;
    }
    
    /**
     * 极大极小搜索（带 Alpha-Beta 剪枝）
     */
    private int minmax(int[][] board, int depth, int alpha, int beta, boolean isMaximizing) {
        int winner = threatDetector.findOneMoveWin(board, GomokuBoard.WHITE) != null ? GomokuBoard.WHITE : 
                     threatDetector.findOneMoveWin(board, GomokuBoard.BLACK) != null ? GomokuBoard.BLACK : GomokuBoard.EMPTY;
        if (winner == GomokuBoard.WHITE) return PatternEvaluator.SCORE_FIVE + depth;
        if (winner == GomokuBoard.BLACK) return -PatternEvaluator.SCORE_FIVE - depth;
        if (depth == 0) return evaluator.evaluateBoard(board);
        
        List<int[]> candidates = threatDetector.getCandidateMoves(board, difficulty.getSearchRange(), true, 70);
        if (candidates.isEmpty()) {
            return evaluator.evaluateBoard(board);
        }
        
        int limit = Math.min(candidates.size(), 70);
        
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < limit; i++) {
                int[] move = candidates.get(i);
                board[move[0]][move[1]] = GomokuBoard.WHITE;
                int eval = minmax(board, depth - 1, alpha, beta, false);
                board[move[0]][move[1]] = GomokuBoard.EMPTY;
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < limit; i++) {
                int[] move = candidates.get(i);
                board[move[0]][move[1]] = GomokuBoard.BLACK;
                int eval = minmax(board, depth - 1, alpha, beta, true);
                board[move[0]][move[1]] = GomokuBoard.EMPTY;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }
    
    /**
     * 中等/简单模式AI
     */
    private int[] calculateEasyMove(int[][] board, List<int[]> candidates) {
        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = GomokuBoard.BOARD_SIZE / 2;
        
        for (int[] move : candidates) {
            int attackScore = evaluatePositionQuick(board, move[0], move[1], GomokuBoard.WHITE);
            int defenseScore = evaluatePositionQuick(board, move[0], move[1], GomokuBoard.BLACK);
            
            double defenseWeight = difficulty.getDefenseWeight();
            
            int totalScore = attackScore + (int)(defenseScore * defenseWeight);
            
            // 对关键棋型额外加分
            int criticalBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.BLACK);
            totalScore += criticalBonus;
            
            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 10 - dist);
            
            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }
        
        List<int[]> bestMoves = new ArrayList<>();
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
     * 快速评估某个位置
     */
    private int evaluatePositionQuick(int[][] board, int row, int col, int player) {
        int score = 0;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, player, dir[0], dir[1]);
            score += evaluator.getLineScore(pattern);
        }
        return score;
    }
    
    /**
     * 评估关键棋型奖励
     */
    private int evaluateCriticalBonus(int[][] board, int row, int col, int opponent) {
        int bonus = 0;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, opponent, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);
            
            if (lineScore >= PatternEvaluator.SCORE_FOUR) bonus += 500000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) bonus += 100000;
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) bonus += 20000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_TWO) bonus += 5000;
        }
        return bonus;
    }
    
    /**
     * 复制棋盘
     */
    private int[][] copyBoard(int[][] board) {
        int[][] copy = new int[GomokuBoard.BOARD_SIZE][GomokuBoard.BOARD_SIZE];
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, GomokuBoard.BOARD_SIZE);
        }
        return copy;
    }
    
    /**
     * 统计棋盘上棋子数量
     */
    private int countPieces(int[][] board) {
        int count = 0;
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) count++;
            }
        }
        return count;
    }
}
