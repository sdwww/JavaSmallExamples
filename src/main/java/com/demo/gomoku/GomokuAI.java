package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.*;

/**
 * 五子棋AI引擎 - 增强版
 * 优化点：
 * 1. 支持 Zobrist 置换表，避免重复计算
 * 2. minmax 使用 O(20) 快速胜负检测替代 O(900) 全盘扫描
 * 3. 杀手启发优化搜索顺序
 */
public class GomokuAI {

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
        initZobrist();
    }

    // ===== Zobrist 哈希 =====
    private static final long[][][] ZOBRIST = new long[GomokuBoard.BOARD_SIZE][GomokuBoard.BOARD_SIZE][3];

    private static void initZobrist() {
        Random rand = new Random(42);
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                ZOBRIST[i][j][GomokuBoard.BLACK] = rand.nextLong();
                ZOBRIST[i][j][GomokuBoard.WHITE] = rand.nextLong();
            }
        }
    }

    // ===== 置换表 =====
    private static final int TT_SIZE = 1 << 22; // ~4M slots
    private static final int TT_MASK = TT_SIZE - 1;
    private final long[] ttKeys = new long[TT_SIZE];
    private final int[] ttScores = new int[TT_SIZE];
    private final int[] ttDepths = new int[TT_SIZE];
    private final byte[] ttFlags = new byte[TT_SIZE]; // 0=空, 1=精确, 2=下界, 3=上界

    // ===== 杀手启发 =====
    private final int[][] killerMoves = new int[MAX_DEPTH + 1][2];

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

        // 必胜/必防/关键威胁（所有难度都必须检查）
        int[] winMove = threatDetector.findImmediateWinOrBlock(board);
        if (winMove != null) return winMove;

        // 困难模式：极大极小 + Alpha-Beta + 置换表 + 迭代加深
        if (difficulty == Difficulty.HARD) {
            return calculateHardMove(board, candidates);
        }

        // 中等/简单模式
        return calculateEasyMove(board, candidates);
    }

    /**
     * 困难模式 - 迭代加深 + 极大极小 + Alpha-Beta + 置换表
     */
    private int[] calculateHardMove(int[][] board, List<int[]> candidates) {
        int[] bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;

        int[][] searchBoard = copyBoard(board);
        long hash = computeHash(board);
        long startTime = System.currentTimeMillis();
        int maxTime = MAX_SEARCH_TIME_MS;
        int depth = 1;

        // 清空杀手启发
        for (int i = 0; i <= MAX_DEPTH; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1;
        }

        while (depth <= MAX_DEPTH) {
            long remainingTime = maxTime - (System.currentTimeMillis() - startTime);
            if (remainingTime < 200) break;

            int limit = Math.min(candidates.size(), 80);
            List<int[]> searchCandidates = candidates.subList(0, limit);

            parallelBestMove = bestMove;
            parallelBestScore = Integer.MIN_VALUE;
            searchCompleted = false;

            if (depth >= 3 && PARALLEL_THREAD_COUNT > 1) {
                int[] result = parallelMinmax(searchBoard, searchCandidates, depth, startTime, maxTime, hash);
                if (result != null) {
                    bestMove = result;
                    bestScore = parallelBestScore;
                }
            } else {
                int[] currentBestMove = bestMove;
                int currentBestScore = Integer.MIN_VALUE;

                for (int[] move : searchCandidates) {
                    searchBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                    long newHash = hash ^ ZOBRIST[move[0]][move[1]][GomokuBoard.WHITE];

                    if (checkWinAt(searchBoard, move[0], move[1], GomokuBoard.WHITE)) {
                        searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                        return move; // 直接获胜
                    }

                    int score = minmax(searchBoard, newHash, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, depth - 1);
                    searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;

                    if (score > currentBestScore) {
                        currentBestScore = score;
                        currentBestMove = move;
                    }

                    if (System.currentTimeMillis() - startTime > maxTime * 0.8) break;
                }

                bestMove = currentBestMove;
                bestScore = currentBestScore;
            }

            depth++;

            if (Math.abs(bestScore) >= PatternEvaluator.SCORE_FIVE) break;
        }

        return bestMove;
    }

    /**
     * 并行极大极小搜索
     */
    private int[] parallelMinmax(int[][] baseBoard, List<int[]> candidates, int depth, long startTime, int maxTime, long baseHash) {
        int threadCount = Math.min(PARALLEL_THREAD_COUNT, candidates.size());
        CountDownLatch latch = new CountDownLatch(threadCount);
        int batchSize = Math.max(1, candidates.size() / threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int startIdx = t * batchSize;
            final int endIdx = (t == threadCount - 1) ? candidates.size() : Math.min(startIdx + batchSize, candidates.size());

            parallelExecutor.submit(() -> {
                int[][] threadBoard = copyBoard(baseBoard);
                long threadHash = baseHash; // 同一起始哈希

                try {
                    int localBestScore = Integer.MIN_VALUE;
                    int[] localBestMove = null;

                    for (int i = startIdx; i < endIdx; i++) {
                        if (searchCompleted) break;

                        int[] move = candidates.get(i);
                        threadBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                        long newHash = threadHash ^ ZOBRIST[move[0]][move[1]][GomokuBoard.WHITE];

                        if (checkWinAt(threadBoard, move[0], move[1], GomokuBoard.WHITE)) {
                            threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                            localBestScore = PatternEvaluator.SCORE_FIVE + depth;
                            localBestMove = move;
                            break;
                        }

                        int score = minmax(threadBoard, newHash, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, depth - 1);
                        threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;

                        if (score > localBestScore) {
                            localBestScore = score;
                            localBestMove = move;
                        }

                        if (System.currentTimeMillis() - startTime > maxTime * 0.75) break;
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
     * 极大极小搜索（Alpha-Beta + 置换表 + 快速胜负检测）
     */
    private int minmax(int[][] board, long hash, int depth, int alpha, int beta, boolean isMaximizing, int ply) {
        // 置换表查找
        int ttScore = ttLookup(hash, depth, alpha, beta);
        if (ttScore != Integer.MIN_VALUE) return ttScore;

        int currentPlayer = isMaximizing ? GomokuBoard.WHITE : GomokuBoard.BLACK;
        List<int[]> candidates = threatDetector.getCandidateMoves(board, difficulty.getSearchRange(), true, 70);
        if (candidates.isEmpty()) return evaluator.evaluateBoard(board);

        int limit = Math.min(candidates.size(), 70);
        int origAlpha = alpha;
        int bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int[] bestMoveInNode = null;

        // 尝试杀手启发走法（排在最前）
        int killerR = killerMoves[ply][0];
        int killerC = killerMoves[ply][1];
        int killerIdx = -1;
        if (killerR >= 0) {
            for (int i = 0; i < limit; i++) {
                if (candidates.get(i)[0] == killerR && candidates.get(i)[1] == killerC) {
                    killerIdx = i;
                    break;
                }
            }
        }

        for (int i = 0; i < limit; i++) {
            // 杀手启发：优先尝试
            int moveIdx = (i == 0 && killerIdx > 0) ? killerIdx : i;
            int[] move = candidates.get(moveIdx);

            board[move[0]][move[1]] = currentPlayer;
            long newHash = hash ^ ZOBRIST[move[0]][move[1]][currentPlayer];

            // 快速胜负检测 O(20)，替代全盘扫描 O(900)
            if (checkWinAt(board, move[0], move[1], currentPlayer)) {
                board[move[0]][move[1]] = GomokuBoard.EMPTY;
                int winScore = currentPlayer == GomokuBoard.WHITE
                        ? (PatternEvaluator.SCORE_FIVE + depth)
                        : (-PatternEvaluator.SCORE_FIVE - depth);
                ttStore(hash, depth, winScore, (byte) 1);
                return winScore;
            }

            int eval;
            if (depth > 1) {
                eval = minmax(board, newHash, depth - 1, alpha, beta, !isMaximizing, ply + 1);
            } else {
                eval = evaluator.evaluateBoard(board);
            }

            board[move[0]][move[1]] = GomokuBoard.EMPTY;

            if (isMaximizing) {
                if (eval > bestScore) {
                    bestScore = eval;
                    bestMoveInNode = move;
                }
                alpha = Math.max(alpha, eval);
            } else {
                if (eval < bestScore) {
                    bestScore = eval;
                    bestMoveInNode = move;
                }
                beta = Math.min(beta, eval);
            }

            if (beta <= alpha) {
                // Alpha-Beta 剪枝：记录杀手走法
                if (bestMoveInNode != null) {
                    killerMoves[ply][0] = bestMoveInNode[0];
                    killerMoves[ply][1] = bestMoveInNode[1];
                }
                break;
            }
        }

        // 置换表存储
        byte flag;
        if (bestScore <= origAlpha) flag = 3;      // 上界
        else if (bestScore >= beta) flag = 2;       // 下界
        else flag = 1;                               // 精确值
        ttStore(hash, depth, bestScore, flag);

        return bestScore;
    }

    /**
     * 中等/简单模式AI
     */
    private int[] calculateEasyMove(int[][] board, List<int[]> candidates) {
        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = GomokuBoard.BOARD_SIZE / 2;

        for (int[] move : candidates) {
            int attackScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.WHITE);
            int defenseScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.BLACK);

            double defenseWeight = difficulty.getDefenseWeight();
            int totalScore = attackScore + (int) (defenseScore * defenseWeight);

            // 关键棋型额外加分
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
     * 评估关键棋型奖励
     */
    private int evaluateCriticalBonus(int[][] board, int row, int col, int opponent) {
        int bonus = 0;
        board[row][col] = opponent;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, opponent, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);

            if (lineScore >= PatternEvaluator.SCORE_FOUR) bonus += 5000000;
            else if (lineScore >= PatternEvaluator.SCORE_RUSH_FOUR) bonus += 1000000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) bonus += 500000;
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) bonus += 30000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_TWO) bonus += 10000;
        }
        board[row][col] = GomokuBoard.EMPTY;
        return bonus;
    }

    // ===== 快速胜负检测 O(20) =====

    /**
     * 检查指定位置落子后是否五子连珠
     */
    private boolean checkWinAt(int[][] board, int row, int col, int player) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1 + countDir(board, row, col, player, dir[0], dir[1])
                           + countDir(board, row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countDir(int[][] board, int row, int col, int player, int dR, int dC) {
        int count = 0;
        int r = row + dR, c = col + dC;
        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dR;
            c += dC;
        }
        return count;
    }

    // ===== Zobrist 哈希 =====

    private long computeHash(int[][] board) {
        long hash = 0;
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    hash ^= ZOBRIST[i][j][board[i][j]];
                }
            }
        }
        return hash;
    }

    // ===== 置换表 =====

    private int ttLookup(long hash, int depth, int alpha, int beta) {
        int idx = (int) (hash & TT_MASK);
        if (ttKeys[idx] == hash && ttDepths[idx] >= depth) {
            int score = ttScores[idx];
            byte flag = ttFlags[idx];
            if (flag == 1) return score;                  // 精确值
            if (flag == 2 && score >= beta) return score;  // 下界
            if (flag == 3 && score <= alpha) return score; // 上界
        }
        return Integer.MIN_VALUE;
    }

    private void ttStore(long hash, int depth, int score, byte flag) {
        int idx = (int) (hash & TT_MASK);
        // 深度优先替换策略
        if (ttDepths[idx] <= depth) {
            ttKeys[idx] = hash;
            ttScores[idx] = score;
            ttDepths[idx] = depth;
            ttFlags[idx] = flag;
        }
    }

    // ===== 工具方法 =====

    private int[][] copyBoard(int[][] board) {
        int[][] copy = new int[GomokuBoard.BOARD_SIZE][GomokuBoard.BOARD_SIZE];
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, GomokuBoard.BOARD_SIZE);
        }
        return copy;
    }

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
