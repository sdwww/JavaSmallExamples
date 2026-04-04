package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.*;

/**
 * 五子棋AI引擎 - 增强版
 * 优化点：
 * 1. 支持 Zobrist 置换表，避免重复计算
 * 2. minmax 使用 O(20) 快速胜负检测替代 O(900) 全盘扫描
 * 3. 杀手启发优化搜索顺序
 * 4. 开局库 - 常见开局快速响应
 * 5. 中等难度增加搜索深度
 */
public class GomokuAI {

    private static final int MAX_SEARCH_TIME_MS = 5000;
    private static final int MAX_DEPTH = 10;
    private static final int BOARD_SIZE = 15;
    private static final int MEDIUM_MAX_DEPTH = 4; // 中等难度搜索深度
    private static final int MEDIUM_MAX_TIME_MS = 3000; // 中等难度时间限制
    private static final int HARD_CANDIDATE_LIMIT = 35; // 困难模式顶层候选数
    private static final int MINMAX_CANDIDATE_LIMIT = 18; // minmax内部每层候选数

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

    // ===== Zobrist 哈希 - 使用 Holder 模式确保正确初始化 =====
    private static class ZobristHolder {
        static final long[][][] TABLE = initZobristTable();
        
        private static long[][][] initZobristTable() {
            long[][][] table = new long[BOARD_SIZE][BOARD_SIZE][3];
            Random rand = new Random(42);
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    table[i][j][1] = rand.nextLong(); // BLACK = 1
                    table[i][j][2] = rand.nextLong(); // WHITE = 2
                }
            }
            return table;
        }
    }
    
    private static long[][][] getZobrist() {
        return ZobristHolder.TABLE;
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

    private volatile long searchStartTime = 0; // 搜索开始时间（用于minmax内部超时检查）

    public GomokuAI(Difficulty difficulty) {
        this.evaluator = new PatternEvaluator();
        this.threatDetector = new ThreatDetector();
        this.difficulty = difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * 清空置换表（每局新游戏时调用）
     */
    public void clearTranspositionTable() {
        Arrays.fill(ttKeys, 0);
        Arrays.fill(ttScores, 0);
        Arrays.fill(ttDepths, 0);
        Arrays.fill(ttFlags, (byte) 0);
        for (int i = 0; i <= MAX_DEPTH; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1;
        }
    }

    // ===== 开局库 =====
    private static final int CENTER = GomokuBoard.BOARD_SIZE / 2;
    
    /**
     * 开局库 - 根据棋盘状态快速返回最佳开局
     */
    private int[] getOpeningMove(int[][] board, int moveCount) {
        // AI第一步（作为白方，第二手）: 贴近黑子
        if (moveCount == 1) {
            // 找到黑子位置
            int br = -1, bc = -1;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == GomokuBoard.BLACK) {
                        br = i; bc = j; break;
                    }
                }
                if (br >= 0) break;
            }
            if (br >= 0) {
                // 在黑子周围随机选一个对角位置
                int[][] offsets = {{1,1},{1,-1},{-1,1},{-1,-1}};
                Random rand = new Random();
                int[] offset = offsets[rand.nextInt(offsets.length)];
                int nr = br + offset[0], nc = bc + offset[1];
                if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE && board[nr][nc] == GomokuBoard.EMPTY) {
                    return new int[]{nr, nc};
                }
            }
            return new int[]{CENTER, CENTER};
        }
        
        // AI第二步（第四手）: 常见开局定式
        if (moveCount == 3) {
            // 找到已有棋子
            List<int[]> whites = new ArrayList<>();
            List<int[]> blacks = new ArrayList<>();
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == GomokuBoard.WHITE) whites.add(new int[]{i, j});
                    if (board[i][j] == GomokuBoard.BLACK) blacks.add(new int[]{i, j});
                }
            }
            if (whites.size() == 1 && blacks.size() == 2) {
                int[] w = whites.get(0);
                // 在白子附近的空位中选择一个形成潜在威胁的位置
                int[][] nearOffsets = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
                Random rand = new Random();
                List<int[]> goodMoves = new ArrayList<>();
                for (int[] off : nearOffsets) {
                    int nr = w[0] + off[0], nc = w[1] + off[1];
                    if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE && board[nr][nc] == GomokuBoard.EMPTY) {
                        goodMoves.add(new int[]{nr, nc});
                    }
                }
                if (!goodMoves.isEmpty()) {
                    return goodMoves.get(rand.nextInt(goodMoves.size()));
                }
            }
        }
        
        return null; // 不在开局库范围内
    }

    /**
     * AI落子（主入口）
     */
    public int[] calculateMove(int[][] board) {
        int moveCount = countPieces(board);

        // 第一步下中间（黑方先手时，通常不会走到这里）
        if (moveCount == 0) {
            return new int[]{CENTER, CENTER};
        }
        
        // 开局库查询
        int[] openingMove = getOpeningMove(board, moveCount);
        if (openingMove != null) return openingMove;

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

        // 中等模式：浅层搜索 + 评估
        if (difficulty == Difficulty.MEDIUM) {
            return calculateMediumMove(board, candidates);
        }

        // 简单模式
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
        searchStartTime = startTime; // 设置搜索开始时间（供minmax内部检查）
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

            int limit = Math.min(candidates.size(), HARD_CANDIDATE_LIMIT);
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
                    long newHash = hash ^ getZobrist()[move[0]][move[1]][GomokuBoard.WHITE];

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
                        long newHash = threadHash ^ getZobrist()[move[0]][move[1]][GomokuBoard.WHITE];

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
        
        // 超时检查 - 避免搜索过久
        if (searchStartTime > 0 && System.currentTimeMillis() - searchStartTime > MAX_SEARCH_TIME_MS * 0.9) {
            return evaluator.evaluateBoard(board);
        }
        
        List<int[]> candidates = threatDetector.getCandidateMoves(board, difficulty.getSearchRange(), true, MINMAX_CANDIDATE_LIMIT);
        if (candidates.isEmpty()) return evaluator.evaluateBoard(board);

        int limit = Math.min(candidates.size(), MINMAX_CANDIDATE_LIMIT);
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
            long newHash = hash ^ getZobrist()[move[0]][move[1]][currentPlayer];

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
     * 中等难度 - 浅层迭代加深搜索
     */
    private int[] calculateMediumMove(int[][] board, List<int[]> candidates) {
        int[] bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;

        int[][] searchBoard = copyBoard(board);
        long hash = computeHash(board);
        long startTime = System.currentTimeMillis();

        // 清空杀手启发
        for (int i = 0; i <= MAX_DEPTH; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1;
        }

        int limit = Math.min(candidates.size(), 30);
        List<int[]> searchCandidates = candidates.subList(0, limit);

        for (int depth = 1; depth <= MEDIUM_MAX_DEPTH; depth++) {
            long remainingTime = MEDIUM_MAX_TIME_MS - (System.currentTimeMillis() - startTime);
            if (remainingTime < 100) break;

            int currentBestScore = Integer.MIN_VALUE;
            int[] currentBestMove = bestMove;

            for (int[] move : searchCandidates) {
                searchBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                long newHash = hash ^ getZobrist()[move[0]][move[1]][GomokuBoard.WHITE];

                if (checkWinAt(searchBoard, move[0], move[1], GomokuBoard.WHITE)) {
                    searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                    return move;
                }

                int score = minmax(searchBoard, newHash, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, depth - 1);
                searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;

                if (score > currentBestScore) {
                    currentBestScore = score;
                    currentBestMove = move;
                }

                if (System.currentTimeMillis() - startTime > MEDIUM_MAX_TIME_MS * 0.8) break;
            }

            bestMove = currentBestMove;
            bestScore = currentBestScore;

            if (Math.abs(bestScore) >= PatternEvaluator.SCORE_FIVE) break;
        }

        return bestMove;
    }

    /**
     * 简单模式AI
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
        for (int i = 0; i < BOARD_SIZE && i < board.length; i++) {
            for (int j = 0; j < BOARD_SIZE && j < board[i].length; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    hash ^= getZobrist()[i][j][board[i][j]];
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
