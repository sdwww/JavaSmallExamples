package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 五子棋AI引擎 - 增强版
 * 优化点：
 * 1. 支持 Zobrist 置换表，避免重复计算
 * 2. minmax 使用 O(20) 快速胜负检测替代 O(900) 全盘扫描
 * 3. 杀手启发优化搜索顺序
 * 4. 开局库 - 常见开局快速响应
 * 5. 中等难度增加搜索深度
 * 6. Quiescence Search - 解决水平线效应
 */
public class GomokuAI {

    private static final int MAX_SEARCH_TIME_MS = 5000;
    private static final int MAX_DEPTH = 10;
    private static final int BOARD_SIZE = 15;
    private static final int MEDIUM_MAX_DEPTH = 4; // 中等难度搜索深度
    private static final int MEDIUM_MAX_TIME_MS = 3000; // 中等难度时间限制
    private static final int HARD_CANDIDATE_LIMIT = 35; // 困难模式顶层候选数
    private static final int MINMAX_CANDIDATE_LIMIT = 18; // minmax内部每层候选数

    // Quiescence Search 配置
    private static final int QUIESCENCE_DEPTH = 8; // Quiescence 最大搜索深度
    private static final int QUIESCENCE_MINIMAX_DEPTH = 12; // 结合 minmax 时的最大总深度

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

    // ===== 置换表（线程安全）=====
    private static final int TT_SIZE = 1 << 22; // ~4M slots
    private static final int TT_MASK = TT_SIZE - 1;
    private final long[] ttKeys = new long[TT_SIZE];
    private final int[] ttScores = new int[TT_SIZE];
    private final int[] ttDepths = new int[TT_SIZE];
    private final byte[] ttFlags = new byte[TT_SIZE]; // 0=空, 1=精确, 2=下界, 3=上界
    private final ReentrantReadWriteLock ttLock = new ReentrantReadWriteLock();

    // ===== 杀手启发 =====
    private final int[][] killerMoves = new int[MAX_DEPTH + 1][2];

    // ===== 历史启发 =====
    private final int[][] historyTable = new int[BOARD_SIZE][BOARD_SIZE];

    private final PatternEvaluator evaluator;
    private final ThreatDetector threatDetector;
    private Difficulty difficulty;

    // 并行搜索结果容器
    private volatile int[] parallelBestMove = null;
    private volatile int parallelBestScore = Integer.MIN_VALUE;
    private volatile boolean searchCompleted = false;
    
    // 引用到 GameGame 以支持取消检查
    private volatile GomokuGame game;
    
    private volatile long searchStartTime = 0; // 搜索开始时间（用于minmax内部超时检查）

    public GomokuAI(Difficulty difficulty) {
        this.evaluator = PatternEvaluator.getInstance();
        this.threatDetector = new ThreatDetector(evaluator);
        this.difficulty = difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * 设置Game引用（用于取消检查）
     */
    public void setGame(GomokuGame game) {
        this.game = game;
    }
    
    /**
     * 清除Game引用
     */
    public void clearGame() {
        this.game = null;
    }
    
    /**
     * 检查是否被取消
     */
    private boolean isSearchCancelled() {
        return game != null && game.isSearchCancelled();
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
        // 清空历史启发表
        for (int i = 0; i < BOARD_SIZE; i++) {
            Arrays.fill(historyTable[i], 0);
        }
    }

    // ===== 开局库 =====
    private static final int CENTER = GomokuBoard.BOARD_SIZE / 2;
    
    /**
     * 开局库 - 根据棋盘状态快速返回最佳开局
     */
    private int[] getOpeningMove(int[][] board, int moveCount) {
        int center = CENTER;
        
        // AI第一步（AI先手）: 下中心点
        if (moveCount == 0) {
            // AI先手时选择中心点，这是标准开局
            return new int[]{center, center};
        }
        
        // AI第二步（作为白方，第二手）: 贴近黑子
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
                int[] offset = offsets[ThreadLocalRandom.current().nextInt(offsets.length)];
                int nr = br + offset[0], nc = bc + offset[1];
                if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE && board[nr][nc] == GomokuBoard.EMPTY) {
                    return new int[]{nr, nc};
                }
            }
            return new int[]{center, center};
        }
        
        // AI第三步（第三手）: 跟随定式
        if (moveCount == 2) {
            // 找到已有棋子
            List<int[]> whites = new ArrayList<>();
            List<int[]> blacks = new ArrayList<>();
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == GomokuBoard.WHITE) whites.add(new int[]{i, j});
                    if (board[i][j] == GomokuBoard.BLACK) blacks.add(new int[]{i, j});
                }
            }
            if (whites.size() == 1 && blacks.size() == 1) {
                int[] w = whites.get(0);
                int[] b = blacks.get(0);
                // 黑子和白子的方向
                int dirR = b[0] - w[0];
                int dirC = b[1] - w[1];
                
                // 常见开局：跟随型 - 在白子另一侧放置
                int nr = w[0] - dirR;
                int nc = w[1] - dirC;
                if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE && board[nr][nc] == GomokuBoard.EMPTY) {
                    return new int[]{nr, nc};
                }
                
                // 或者在白子附近选择一个位置
                int[][] nearOffsets = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
                List<int[]> goodMoves = new ArrayList<>();
                for (int[] off : nearOffsets) {
                    int tr = w[0] + off[0], tc = w[1] + off[1];
                    if (tr >= 0 && tr < BOARD_SIZE && tc >= 0 && tc < BOARD_SIZE && board[tr][tc] == GomokuBoard.EMPTY) {
                        goodMoves.add(new int[]{tr, tc});
                    }
                }
                if (!goodMoves.isEmpty()) {
                    return goodMoves.get(ThreadLocalRandom.current().nextInt(goodMoves.size()));
                }
            }
        }
        
        // AI第四步（第四手）: 常见开局定式
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
            if (whites.size() == 2 && blacks.size() == 2) {
                int[] w = whites.get(0);
                // 在白子附近的空位中选择一个形成潜在威胁的位置
                int[][] nearOffsets = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
                List<int[]> goodMoves = new ArrayList<>();
                for (int[] off : nearOffsets) {
                    int nr = w[0] + off[0], nc = w[1] + off[1];
                    if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE && board[nr][nc] == GomokuBoard.EMPTY) {
                        goodMoves.add(new int[]{nr, nc});
                    }
                }
                if (!goodMoves.isEmpty()) {
                    return goodMoves.get(ThreadLocalRandom.current().nextInt(goodMoves.size()));
                }
            }
        }
        
        return null; // 不在开局库范围内
    }

    /**
     * AI落子（主入口）
     */
    public int[] calculateMove(int[][] board) {
        // 检查是否被取消
        if (isSearchCancelled()) {
            return null;
        }
        
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

        // 必胜/必防/关键威胁（所有难度都必须检查，包括简单模式）
        
        // ===== 优先级1：AI的必胜机会 =====
        // 如果AI有必胜机会（如双活三、四三等），直接进攻，不需要防守
        int[] aiWinMove = threatDetector.findAIWinOpportunity(board, GomokuBoard.WHITE);
        if (aiWinMove != null) {
            return aiWinMove;
        }
        
        // ===== 优先级2：对手的必杀威胁 =====
        // 检查对手是否有一击必杀
        int[] opponentWin = threatDetector.findOneMoveWin(board, GomokuBoard.BLACK);
        if (opponentWin != null) {
            return opponentWin;
        }
        
        // 检查对手的跳跃四连
        int[] opponentJumpFour = threatDetector.findJumpFour(board, GomokuBoard.BLACK);
        if (opponentJumpFour != null) {
            return opponentJumpFour;
        }
        
        // 检查对手的组合威胁（双活三、四三等）
        int[] opponentCombo = threatDetector.findComboThreat(board, GomokuBoard.BLACK);
        if (opponentCombo != null) {
            return opponentCombo;
        }
        
        // 检查对手的三连威胁
        int[] opponentThree = threatDetector.findExistingThree(board, GomokuBoard.BLACK);
        if (opponentThree != null) {
            return opponentThree;
        }

        // 困难模式：极大极小 + Alpha-Beta + 置换表 + 迭代加深
        if (difficulty == Difficulty.HARD) {
            return calculateHardMove(board, candidates);
        }

        // 中等模式：浅层搜索 + 评估
        if (difficulty == Difficulty.MEDIUM) {
            return calculateMediumMove(board);
        }

        // 简单模式
        return calculateEasyMove(board);
    }

    /**
     * 困难模式 - 迭代加深 + 极大极小 + Alpha-Beta + 置换表 + Aspiration Windows
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

        // Aspiration Window 参数
        int ASPIRATION_WINDOW = 30;
        int windowAlpha = Integer.MIN_VALUE;
        int windowBeta = Integer.MAX_VALUE;

        while (depth <= MAX_DEPTH) {
            long remainingTime = maxTime - (System.currentTimeMillis() - startTime);
            if (remainingTime < 200) break;

            int limit = Math.min(candidates.size(), HARD_CANDIDATE_LIMIT);
            List<int[]> searchCandidates = candidates.subList(0, limit);

            parallelBestMove = bestMove;
            parallelBestScore = Integer.MIN_VALUE;
            searchCompleted = false;

            // Aspiration Windows：第一层使用全窗口，后续层使用前一层评估值作为窗口
            if (depth > 1 && Math.abs(bestScore) < PatternEvaluator.SCORE_FIVE) {
                windowAlpha = bestScore - ASPIRATION_WINDOW;
                windowBeta = bestScore + ASPIRATION_WINDOW;
            } else {
                windowAlpha = Integer.MIN_VALUE;
                windowBeta = Integer.MAX_VALUE;
            }

            if (depth >= 3 && PARALLEL_THREAD_COUNT > 1) {
                int[] result = parallelMinmax(searchBoard, searchCandidates, depth, startTime, maxTime, hash, windowAlpha, windowBeta);
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
                        // 更新历史启发
                        historyTable[move[0]][move[1]] += depth * depth;
                        return move; // 直接获胜
                    }

                    int score = minmax(searchBoard, newHash, depth - 1, windowAlpha, windowBeta, false, depth - 1);
                    searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;

                    // Aspiration Window 溢出检测
                    if (score <= windowAlpha || score >= windowBeta) {
                        // 窗口溢出，重新搜索全窗口
                        score = minmax(searchBoard, newHash, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, depth - 1);
                    }

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
     * Quiescence Search（静态搜索）- 解决水平线效应
     * 只搜索"不稳定"的着法直到局面稳定
     */
    private int quiescenceSearch(int[][] board, long hash, int alpha, int beta, boolean isMaximizing, int depth) {
        // 取消检查
        if (isSearchCancelled()) {
            return evaluator.evaluateBoard(board);
        }

        int standPat = evaluator.evaluateBoard(board);

        if (isMaximizing) {
            if (standPat > alpha) {
                alpha = standPat;
            }
            if (alpha >= beta) {
                return beta; // 剪枝
            }
        } else {
            if (standPat < beta) {
                beta = standPat;
            }
            if (alpha >= beta) {
                return alpha; // 剪枝
            }
        }

        if (depth <= 0) {
            return isMaximizing ? alpha : beta;
        }

        // 获取需要继续搜索的"不稳定"着法
        int currentPlayer = isMaximizing ? GomokuBoard.WHITE : GomokuBoard.BLACK;
        List<int[]> quiMoves = getQuiescenceMoves(board, currentPlayer, isMaximizing);

        if (quiMoves.isEmpty()) {
            return isMaximizing ? alpha : beta;
        }

        // 按评估值排序，优先搜索好的着法
        final int ply = 0; // Quiescence 内层 ply
        quiMoves.sort((a, b) -> {
            int scoreA = historyTable[a[0]][a[1]];
            int scoreB = historyTable[b[0]][b[1]];
            return scoreB - scoreA;
        });

        int limit = Math.min(quiMoves.size(), 16); // 限制 Quiescence 候选数

        for (int i = 0; i < limit; i++) {
            int[] move = quiMoves.get(i);

            board[move[0]][move[1]] = currentPlayer;
            long newHash = hash ^ getZobrist()[move[0]][move[1]][currentPlayer];

            // 快速胜负检测
            if (checkWinAt(board, move[0], move[1], currentPlayer)) {
                board[move[0]][move[1]] = GomokuBoard.EMPTY;
                int winScore = currentPlayer == GomokuBoard.WHITE
                        ? (PatternEvaluator.SCORE_FIVE + depth)
                        : (-PatternEvaluator.SCORE_FIVE - depth);
                return winScore;
            }

            int eval = quiescenceSearch(board, newHash, alpha, beta, !isMaximizing, depth - 1);

            board[move[0]][move[1]] = GomokuBoard.EMPTY;

            if (isMaximizing) {
                if (eval > alpha) {
                    alpha = eval;
                }
                if (alpha >= beta) {
                    // 记录好的着法到历史表
                    historyTable[move[0]][move[1]] += depth * depth;
                    break;
                }
            } else {
                if (eval < beta) {
                    beta = eval;
                }
                if (alpha >= beta) {
                    historyTable[move[0]][move[1]] += depth * depth;
                    break;
                }
            }
        }

        return isMaximizing ? alpha : beta;
    }

    /**
     * 获取 Quiescence Search 需要的候选着法
     * 只返回"不稳定"的着法：能形成威胁或吃子的位置
     */
    private List<int[]> getQuiescenceMoves(int[][] board, int player, boolean isAttacking) {
        List<int[]> moves = new ArrayList<>();
        int opponent = (player == GomokuBoard.WHITE) ? GomokuBoard.BLACK : GomokuBoard.WHITE;

        // 获取威胁候选位置
        List<int[]> candidates = threatDetector.getCandidateMoves(board, 2, false, 50);

        for (int[] pos : candidates) {
            int r = pos[0], c = pos[1];

            // 检查落子后是否能形成威胁（四连及以上）
            int threatLevel = getMoveThreatLevel(board, r, c, player);
            if (threatLevel >= 3) { // 眠三及以上
                moves.add(pos);
                continue;
            }

            // 检查落子后是否能阻挡对手的威胁
            int defenseLevel = getMoveThreatLevel(board, r, c, opponent);
            if (defenseLevel >= 4) { // 冲四及以上必须防守
                moves.add(pos);
                continue;
            }

            // 检查是否形成跳跃威胁
            if (isJumpThreat(board, r, c, player)) {
                moves.add(pos);
                continue;
            }

            // 检查是否形成活二（作为进攻扩展）
            if (isAttacking && threatLevel >= 2) {
                moves.add(pos);
            }
        }

        return moves;
    }

    /**
     * 评估落子的威胁等级
     * @return 0=无威胁, 1=眠二, 2=活二, 3=眠三, 4=活三, 5=冲四/跳跃四, 6=五连
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
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_TWO) level = 1;

            maxLevel = Math.max(maxLevel, level);
        }

        board[row][col] = GomokuBoard.EMPTY;
        return maxLevel;
    }

    /**
     * 检查是否形成跳跃威胁
     */
    private boolean isJumpThreat(int[][] board, int row, int col, int player) {
        board[row][col] = player;

        boolean isJump = false;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            // 检测 O_OOO 模式（跳跃四）
            int r1 = row + dir[0], c1 = col + dir[1];
            int r2 = row + 2 * dir[0], c2 = col + 2 * dir[1];
            int r3 = row + 3 * dir[0], c3 = col + 3 * dir[1];
            int r4 = row + 4 * dir[0], c4 = col + 4 * dir[1];

            if (r1 >= 0 && r1 < GomokuBoard.BOARD_SIZE && c1 >= 0 && c1 < GomokuBoard.BOARD_SIZE &&
                r2 >= 0 && r2 < GomokuBoard.BOARD_SIZE && c2 >= 0 && c2 < GomokuBoard.BOARD_SIZE &&
                r3 >= 0 && r3 < GomokuBoard.BOARD_SIZE && c3 >= 0 && c3 < GomokuBoard.BOARD_SIZE &&
                r4 >= 0 && r4 < GomokuBoard.BOARD_SIZE && c4 >= 0 && c4 < GomokuBoard.BOARD_SIZE) {

                if (board[r1][c1] == GomokuBoard.EMPTY &&
                    board[r2][c2] == player &&
                    board[r3][c3] == player &&
                    board[r4][c4] == player) {
                    isJump = true;
                    break;
                }
            }
        }

        board[row][col] = GomokuBoard.EMPTY;
        return isJump;
    }

    /**
     * 并行极大极小搜索（支持 Aspiration Windows）
     */
    private int[] parallelMinmax(int[][] baseBoard, List<int[]> candidates, int depth, long startTime, int maxTime, long baseHash, int windowAlpha, int windowBeta) {
        int threadCount = Math.min(PARALLEL_THREAD_COUNT, candidates.size());
        CountDownLatch latch = new CountDownLatch(threadCount);
        int batchSize = Math.max(1, candidates.size() / threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int startIdx = t * batchSize;
            final int endIdx = (t == threadCount - 1) ? candidates.size() : Math.min(startIdx + batchSize, candidates.size());

            parallelExecutor.submit(() -> {
                int[][] threadBoard = copyBoard(baseBoard);
                // 正确：为每个线程维护独立的哈希状态
                long threadHash = baseHash;

                try {
                    int localBestScore = Integer.MIN_VALUE;
                    int[] localBestMove = null;

                    for (int i = startIdx; i < endIdx; i++) {
                        // 检查全局取消标志
                        if (searchCompleted || isSearchCancelled()) break;

                        int[] move = candidates.get(i);
                        threadBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                        // 正确更新哈希值
                        long newHash = threadHash ^ getZobrist()[move[0]][move[1]][GomokuBoard.WHITE];

                        if (checkWinAt(threadBoard, move[0], move[1], GomokuBoard.WHITE)) {
                            threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                            localBestScore = PatternEvaluator.SCORE_FIVE + depth;
                            localBestMove = move;
                            // 更新历史启发
                            historyTable[move[0]][move[1]] += depth * depth;
                            break;
                        }

                        int score = minmax(threadBoard, newHash, depth - 1, windowAlpha, windowBeta, false, depth - 1);

                        // Aspiration Window 溢出检测
                        if (score <= windowAlpha || score >= windowBeta) {
                            // 窗口溢出，重新搜索全窗口
                            score = minmax(threadBoard, newHash, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, depth - 1);
                        }

                        threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                        // 更新线程本地哈希值，为下一个候选位置做准备
                        threadHash = newHash ^ getZobrist()[move[0]][move[1]][GomokuBoard.WHITE];

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
     * 极大极小搜索（Alpha-Beta + 置换表 + 快速胜负检测 + 历史启发）
     */
    private int minmax(int[][] board, long hash, int depth, int alpha, int beta, boolean isMaximizing, int ply) {
        // 取消检查
        if (isSearchCancelled()) {
            return evaluator.evaluateBoard(board);
        }

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

        // 历史启发排序：将候选着法按历史分数排序（分数高的优先搜索）
        final int fPly = ply;
        candidates.sort((a, b) -> {
            int scoreA = historyTable[a[0]][a[1]];
            int scoreB = historyTable[b[0]][b[1]];
            if (scoreB != scoreA) return scoreB - scoreA; // 历史分数高的在前

            // 杀手启发：杀手走法优先
            int killerR = killerMoves[fPly][0];
            int killerC = killerMoves[fPly][1];
            if (a[0] == killerR && a[1] == killerC) return 1;
            if (b[0] == killerR && b[1] == killerC) return -1;

            return 0;
        });

        for (int i = 0; i < limit; i++) {
            int[] move = candidates.get(i);

            board[move[0]][move[1]] = currentPlayer;
            long newHash = hash ^ getZobrist()[move[0]][move[1]][currentPlayer];

            // 快速胜负检测 O(20)，替代全盘扫描 O(900)
            if (checkWinAt(board, move[0], move[1], currentPlayer)) {
                board[move[0]][move[1]] = GomokuBoard.EMPTY;
                int winScore = currentPlayer == GomokuBoard.WHITE
                        ? (PatternEvaluator.SCORE_FIVE + depth)
                        : (-PatternEvaluator.SCORE_FIVE - depth);
                ttStore(hash, depth, winScore, (byte) 1);
                // 更新历史启发
                historyTable[move[0]][move[1]] += depth * depth;
                return winScore;
            }

            int eval;
            if (depth > 1) {
                eval = minmax(board, newHash, depth - 1, alpha, beta, !isMaximizing, ply + 1);
            } else {
                // depth == 1 时进入 Quiescence Search，避免水平线效应
                eval = quiescenceSearch(board, hash, alpha, beta, isMaximizing, QUIESCENCE_DEPTH);
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
                // Alpha-Beta 剪枝：记录杀手走法 + 更新历史启发表
                if (bestMoveInNode != null) {
                    killerMoves[ply][0] = bestMoveInNode[0];
                    killerMoves[ply][1] = bestMoveInNode[1];
                    // 历史启发：更新产生剪枝的着法分数
                    historyTable[bestMoveInNode[0]][bestMoveInNode[1]] += depth * depth;
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
     * 简单模式AI - 增强版
     * 改进：防守时优先选择既能防守又能进攻的位置
     */
    private int[] calculateEasyMove(int[][] board) {
        // 优先检查攻防兼备的机会
        int[] dualMove = findDualPurposeMove(board, GomokuBoard.WHITE);
        if (dualMove != null) {
            return dualMove;
        }
        
        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = GomokuBoard.BOARD_SIZE / 2;
        
        // 增强版：使用更大的搜索范围
        List<int[]> candidates = threatDetector.getCandidateMoves(board, 3, true, 50);
        
        for (int[] move : candidates) {
            int attackScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.WHITE);
            int defenseScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.BLACK);
            
            // 大幅提高防守权重：简单模式也要认真防守
            double defenseWeight = difficulty.getDefenseWeight();
            int totalScore = attackScore + (int) (defenseScore * defenseWeight);
            
            // 关键棋型额外加分（防守优先）
            int defenseBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.BLACK);
            int attackBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.WHITE);
            
            // 攻防兼备奖励
            if (attackBonus > 0 && defenseBonus > 0) {
                totalScore += (attackBonus + defenseBonus) * 0.5;
            }
            
            // 防守奖励权重更高
            totalScore += defenseBonus * 2;  // 防守奖励翻倍
            totalScore += attackBonus;
            
            // 距离中心的惩罚（简单模式不希望走太远）
            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 15 - dist);
            
            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }

        // 选择最高分的落子，不再随机
        List<int[]> bestMoves = new ArrayList<>();
        for (int[] move : candidates) {
            int score = scores.get(move[0] + "," + move[1]);
            if (score >= maxScore * 0.9) {  // 只选择接近最高分的
                bestMoves.add(move);
            }
        }

        // 返回最佳落子（优先选择防守位置）
        return bestMoves.get(ThreadLocalRandom.current().nextInt(bestMoves.size()));
    }
    
    /**
     * 中等模式AI - 平衡攻防
     * 改进：优先选择既能防守又能进攻的位置
     */
    private int[] calculateMediumMove(int[][] board) {
        // 优先检查攻防兼备的机会
        int[] dualMove = findDualPurposeMove(board, GomokuBoard.WHITE);
        if (dualMove != null) {
            return dualMove;
        }
        
        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = GomokuBoard.BOARD_SIZE / 2;
        
        // 中等难度使用更大的候选集
        List<int[]> mediumCandidates = threatDetector.getCandidateMoves(board, 2, true, 25);
        
        for (int[] move : mediumCandidates) {
            int attackScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.WHITE);
            int defenseScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.BLACK);
            
            double defenseWeight = difficulty.getDefenseWeight();
            int totalScore = attackScore + (int) (defenseScore * defenseWeight);
            
            // 关键棋型评估
            int defenseBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.BLACK);
            int attackBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.WHITE);
            
            // 攻防兼备奖励
            if (attackBonus > 0 && defenseBonus > 0) {
                totalScore += (attackBonus + defenseBonus) * 0.5;
            }
            
            totalScore += defenseBonus * 1.5;
            totalScore += attackBonus * 1.2;
            
            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 12 - dist);
            
            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }

        List<int[]> bestMoves = new ArrayList<>();
        for (int[] move : mediumCandidates) {
            int score = scores.get(move[0] + "," + move[1]);
            if (score >= maxScore * 0.85) {
                bestMoves.add(move);
            }
        }

        return bestMoves.get(ThreadLocalRandom.current().nextInt(bestMoves.size()));
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
    
    /**
     * 查找既能防守又能进攻的位置（攻防兼备）
     * 策略：评估每个候选位置同时具备的进攻价值和防守价值
     */
    private int[] findDualPurposeMove(int[][] board, int aiPlayer) {
        int opponent = (aiPlayer == GomokuBoard.WHITE) ? GomokuBoard.BLACK : GomokuBoard.WHITE;
        
        // 获取所有候选位置
        List<int[]> candidates = threatDetector.getCandidateMoves(board, 2, false, 30);
        if (candidates.isEmpty()) return null;
        
        int[] bestDualMove = null;
        int bestDualScore = -1;  // 综合评分
        
        for (int[] move : candidates) {
            // 检查落子后是否能形成进攻棋型
            int attackValue = evaluateAttackValue(board, move[0], move[1], aiPlayer);
            
            // 检查落子后是否能防守对手威胁
            int defenseValue = evaluateDefenseValue(board, move[0], move[1], opponent);
            
            // 计算攻防兼备的综合评分
            // 如果既有进攻价值又有防守价值，给予额外奖励
            int dualScore = attackValue + defenseValue;
            if (attackValue > 0 && defenseValue > 0) {
                dualScore += Math.min(attackValue, defenseValue) * 2; // 攻防兼备额外奖励
            }
            
            // 优先选择攻防兼备的位置
            if (dualScore > bestDualScore) {
                bestDualScore = dualScore;
                bestDualMove = move;
            }
        }
        
        // 只有当攻防兼备的价值超过阈值时才返回
        if (bestDualScore > 50000) {  // 阈值：活三级别的价值
            return bestDualMove;
        }
        
        return null;
    }
    
    /**
     * 评估落子位置的进攻价值
     */
    private int evaluateAttackValue(int[][] board, int row, int col, int player) {
        int value = 0;
        board[row][col] = player;
        
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, player, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);
            
            // 活四/冲四：最高价值
            if (lineScore >= PatternEvaluator.SCORE_FOUR) {
                value += 5000000;
            }
            // 跳跃四/眠四
            else if (lineScore >= PatternEvaluator.SCORE_RUSH_FOUR) {
                value += 1000000;
            }
            // 活三
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) {
                value += 500000;
            }
            // 眠三
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) {
                value += 30000;
            }
            // 活二
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_TWO) {
                value += 10000;
            }
        }
        
        board[row][col] = GomokuBoard.EMPTY;
        return value;
    }
    
    /**
     * 评估落子位置的防守价值
     * 检查落子后能否阻挡对手的关键棋型
     */
    private int evaluateDefenseValue(int[][] board, int row, int col, int opponent) {
        int value = 0;
        board[row][col] = opponent;
        
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, opponent, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);
            
            // 阻挡对手五连
            if (lineScore >= PatternEvaluator.SCORE_FIVE) {
                value += 5000000;
            }
            // 阻挡对手活四或冲四
            else if (lineScore >= PatternEvaluator.SCORE_FOUR) {
                value += 2000000;
            }
            // 阻挡对手跳跃四
            else if (lineScore >= PatternEvaluator.SCORE_RUSH_FOUR) {
                value += 1000000;
            }
            // 阻挡对手活三
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) {
                value += 500000;
            }
            // 阻挡对手眠三
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) {
                value += 30000;
            }
        }
        
        board[row][col] = GomokuBoard.EMPTY;
        return value;
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

    // ===== 置换表（线程安全）=====

    private int ttLookup(long hash, int depth, int alpha, int beta) {
        ttLock.readLock().lock();
        try {
            int idx = (int) (hash & TT_MASK);
            if (ttKeys[idx] == hash && ttDepths[idx] >= depth) {
                int score = ttScores[idx];
                byte flag = ttFlags[idx];
                if (flag == 1) return score;                  // 精确值
                if (flag == 2 && score >= beta) return score;  // 下界
                if (flag == 3 && score <= alpha) return score; // 上界
            }
            return Integer.MIN_VALUE;
        } finally {
            ttLock.readLock().unlock();
        }
    }

    private void ttStore(long hash, int depth, int score, byte flag) {
        ttLock.writeLock().lock();
        try {
            int idx = (int) (hash & TT_MASK);
            // 改进的替换策略：
            // 1. 如果条目为空，直接存储
            // 2. 如果新条目是精确分数，优先替换
            // 3. 如果深度更深，优先替换
            // 4. 如果是下界/上界分数且当前是精确分数，不替换
            if (ttDepths[idx] == 0) {
                // 空槽，直接存储
                ttKeys[idx] = hash;
                ttScores[idx] = score;
                ttDepths[idx] = depth;
                ttFlags[idx] = flag;
            } else if (flag == 1) {
                // 新条目是精确分数，优先存储
                if (ttFlags[idx] != 1 || depth >= ttDepths[idx]) {
                    ttKeys[idx] = hash;
                    ttScores[idx] = score;
                    ttDepths[idx] = depth;
                    ttFlags[idx] = flag;
                }
            } else if (depth > ttDepths[idx]) {
                // 新条目深度更深，可以替换非精确分数条目
                ttKeys[idx] = hash;
                ttScores[idx] = score;
                ttDepths[idx] = depth;
                ttFlags[idx] = flag;
            } else if (ttFlags[idx] != 1 && depth >= ttDepths[idx]) {
                // 当前条目也是非精确分数，深度相当或更深时替换
                ttKeys[idx] = hash;
                ttScores[idx] = score;
                ttDepths[idx] = depth;
                ttFlags[idx] = flag;
            }
        } finally {
            ttLock.writeLock().unlock();
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
