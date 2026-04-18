package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 五子棋搜索引擎
 * 实现：minmax + Alpha-Beta + Quiescence Search + 并行搜索 + Aspiration Windows
 */
public class GomokuSearcher {

    private static final int MAX_DEPTH = 10;
    private static final int BOARD_SIZE = 15;
    private static final int HARD_CANDIDATE_LIMIT = 35;
    private static final int MINMAX_CANDIDATE_LIMIT = 18;
    private static final int QUIESCENCE_DEPTH = 8;
    private static final int MAX_SEARCH_TIME_MS = 5000;

    private static final int PARALLEL_THREAD_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    private static final ExecutorService parallelExecutor = Executors.newFixedThreadPool(PARALLEL_THREAD_COUNT, r -> {
        Thread t = new Thread(r, "GomokuSearcher-Worker");
        t.setDaemon(true);
        return t;
    });

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(parallelExecutor::shutdown));
    }

    private final PatternEvaluator evaluator;
    private final ThreatDetector threatDetector;
    private final TranspositionTable tt;
    private final SearchHeuristics heuristics;

    // 并行搜索结果容器
    private volatile int[] parallelBestMove = null;
    private volatile int parallelBestScore = Integer.MIN_VALUE;
    private volatile boolean searchCompleted = false;

    // 引用到Game以支持取消检查
    private volatile GomokuGame game;
    private volatile long searchStartTime = 0;

    public GomokuSearcher(PatternEvaluator evaluator, ThreatDetector threatDetector,
                          TranspositionTable tt, SearchHeuristics heuristics) {
        this.evaluator = evaluator;
        this.threatDetector = threatDetector;
        this.tt = tt;
        this.heuristics = heuristics;
    }

    public void setGame(GomokuGame game) {
        this.game = game;
    }

    public void clearGame() {
        this.game = null;
    }

    private boolean isSearchCancelled() {
        return game != null && game.isSearchCancelled();
    }

    /**
     * 困难模式搜索入口
     */
    public int[] search(int[][] board, List<int[]> candidates) {
        int[] bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;

        int[][] searchBoard = copyBoard(board);
        long hash = TranspositionTable.computeHash(board);
        long startTime = System.currentTimeMillis();
        searchStartTime = startTime;
        int maxTime = MAX_SEARCH_TIME_MS;
        int depth = 1;

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
                    long newHash = hash ^ TranspositionTable.getZobristTable()[move[0]][move[1]][GomokuBoard.WHITE];

                    if (checkWinAt(searchBoard, move[0], move[1], GomokuBoard.WHITE)) {
                        searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                        heuristics.addHistoryScore(move[0], move[1], depth);
                        return move;
                    }

                    int score = minmax(searchBoard, newHash, depth - 1, windowAlpha, windowBeta, false, depth - 1);
                    searchBoard[move[0]][move[1]] = GomokuBoard.EMPTY;

                    if (score <= windowAlpha || score >= windowBeta) {
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
     * Quiescence Search - 解决水平线效应
     */
    public int quiescenceSearch(int[][] board, long hash, int alpha, int beta, boolean isMaximizing, int depth) {
        if (isSearchCancelled()) {
            return evaluator.evaluateBoard(board);
        }

        int standPat = evaluator.evaluateBoard(board);

        if (isMaximizing) {
            if (standPat > alpha) alpha = standPat;
            if (alpha >= beta) return beta;
        } else {
            if (standPat < beta) beta = standPat;
            if (alpha >= beta) return alpha;
        }

        if (depth <= 0) {
            return isMaximizing ? alpha : beta;
        }

        int currentPlayer = isMaximizing ? GomokuBoard.WHITE : GomokuBoard.BLACK;
        List<int[]> quiMoves = getQuiescenceMoves(board, currentPlayer, isMaximizing);

        if (quiMoves.isEmpty()) {
            return isMaximizing ? alpha : beta;
        }

        quiMoves.sort(Comparator.comparingInt((int[] a) -> heuristics.getHistoryScore(a[0], a[1])).reversed());

        int limit = Math.min(quiMoves.size(), 16);

        for (int i = 0; i < limit; i++) {
            int[] move = quiMoves.get(i);

            board[move[0]][move[1]] = currentPlayer;
            long newHash = hash ^ TranspositionTable.getZobristTable()[move[0]][move[1]][currentPlayer];

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
                if (eval > alpha) alpha = eval;
                if (alpha >= beta) {
                    heuristics.addHistoryScore(move[0], move[1], depth);
                    break;
                }
            } else {
                if (eval < beta) beta = eval;
                if (alpha >= beta) {
                    heuristics.addHistoryScore(move[0], move[1], depth);
                    break;
                }
            }
        }

        return isMaximizing ? alpha : beta;
    }

    /**
     * 获取Quiescence候选着法
     */
    private List<int[]> getQuiescenceMoves(int[][] board, int player, boolean isAttacking) {
        List<int[]> moves = new ArrayList<>();
        int opponent = (player == GomokuBoard.WHITE) ? GomokuBoard.BLACK : GomokuBoard.WHITE;

        List<int[]> candidates = threatDetector.getCandidateMoves(board, 2, false, 50);

        for (int[] pos : candidates) {
            int r = pos[0], c = pos[1];

            int threatLevel = getMoveThreatLevel(board, r, c, player);
            if (threatLevel >= 3) {
                moves.add(pos);
                continue;
            }

            int defenseLevel = getMoveThreatLevel(board, r, c, opponent);
            if (defenseLevel >= 4) {
                moves.add(pos);
                continue;
            }

            if (isJumpThreat(board, r, c, player)) {
                moves.add(pos);
                continue;
            }

            if (isAttacking && threatLevel >= 2) {
                moves.add(pos);
            }
        }

        return moves;
    }

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

    private boolean isJumpThreat(int[][] board, int row, int col, int player) {
        board[row][col] = player;

        boolean isJump = false;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
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
     * 并行minmax搜索
     */
    private int[] parallelMinmax(int[][] baseBoard, List<int[]> candidates, int depth,
                                 long startTime, int maxTime, long baseHash,
                                 int windowAlpha, int windowBeta) {
        int threadCount = Math.min(PARALLEL_THREAD_COUNT, candidates.size());
        CountDownLatch latch = new CountDownLatch(threadCount);
        int batchSize = Math.max(1, candidates.size() / threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int startIdx = t * batchSize;
            final int endIdx = (t == threadCount - 1) ? candidates.size() : Math.min(startIdx + batchSize, candidates.size());

            parallelExecutor.submit(() -> {
                int[][] threadBoard = copyBoard(baseBoard);
                long threadHash = baseHash;

                try {
                    int localBestScore = Integer.MIN_VALUE;
                    int[] localBestMove = null;

                    for (int i = startIdx; i < endIdx; i++) {
                        if (searchCompleted || isSearchCancelled()) break;

                        int[] move = candidates.get(i);
                        threadBoard[move[0]][move[1]] = GomokuBoard.WHITE;
                        long newHash = threadHash ^ TranspositionTable.getZobristTable()[move[0]][move[1]][GomokuBoard.WHITE];

                        if (checkWinAt(threadBoard, move[0], move[1], GomokuBoard.WHITE)) {
                            threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                            localBestScore = PatternEvaluator.SCORE_FIVE + depth;
                            localBestMove = move;
                            heuristics.addHistoryScore(move[0], move[1], depth);
                            break;
                        }

                        int score = minmax(threadBoard, newHash, depth - 1, windowAlpha, windowBeta, false, depth - 1);

                        if (score <= windowAlpha || score >= windowBeta) {
                            score = minmax(threadBoard, newHash, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, depth - 1);
                        }

                        threadBoard[move[0]][move[1]] = GomokuBoard.EMPTY;
                        threadHash = newHash ^ TranspositionTable.getZobristTable()[move[0]][move[1]][GomokuBoard.WHITE];

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
     * minmax + Alpha-Beta搜索
     */
    private int minmax(int[][] board, long hash, int depth, int alpha, int beta, boolean isMaximizing, int ply) {
        if (isSearchCancelled()) {
            return evaluator.evaluateBoard(board);
        }

        int ttScore = tt.lookup(hash, depth, alpha, beta);
        if (ttScore != Integer.MIN_VALUE) return ttScore;

        int currentPlayer = isMaximizing ? GomokuBoard.WHITE : GomokuBoard.BLACK;

        if (searchStartTime > 0 && System.currentTimeMillis() - searchStartTime > MAX_SEARCH_TIME_MS * 0.9) {
            return evaluator.evaluateBoard(board);
        }

        List<int[]> candidates = threatDetector.getCandidateMoves(board, 2, true, MINMAX_CANDIDATE_LIMIT);
        if (candidates.isEmpty()) return evaluator.evaluateBoard(board);

        int limit = Math.min(candidates.size(), MINMAX_CANDIDATE_LIMIT);
        int origAlpha = alpha;
        int bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int[] bestMoveInNode = null;

        candidates.sort(heuristics.moveComparator(ply));

        for (int i = 0; i < limit; i++) {
            int[] move = candidates.get(i);

            board[move[0]][move[1]] = currentPlayer;
            long newHash = hash ^ TranspositionTable.getZobristTable()[move[0]][move[1]][currentPlayer];

            if (checkWinAt(board, move[0], move[1], currentPlayer)) {
                board[move[0]][move[1]] = GomokuBoard.EMPTY;
                int winScore = currentPlayer == GomokuBoard.WHITE
                        ? (PatternEvaluator.SCORE_FIVE + depth)
                        : (-PatternEvaluator.SCORE_FIVE - depth);
                tt.store(hash, depth, winScore, (byte) 1);
                heuristics.addHistoryScore(move[0], move[1], depth);
                return winScore;
            }

            int eval;
            if (depth > 1) {
                eval = minmax(board, newHash, depth - 1, alpha, beta, !isMaximizing, ply + 1);
            } else {
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
                if (bestMoveInNode != null) {
                    heuristics.setKillerMove(ply, bestMoveInNode[0], bestMoveInNode[1]);
                    heuristics.addHistoryScore(bestMoveInNode[0], bestMoveInNode[1], depth);
                }
                break;
            }
        }

        byte flag;
        if (bestScore <= origAlpha) flag = 3;
        else if (bestScore >= beta) flag = 2;
        else flag = 1;
        tt.store(hash, depth, bestScore, flag);

        return bestScore;
    }

    // ===== 工具方法 =====

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

    public static int[][] copyBoard(int[][] board) {
        int[][] copy = new int[GomokuBoard.BOARD_SIZE][GomokuBoard.BOARD_SIZE];
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, GomokuBoard.BOARD_SIZE);
        }
        return copy;
    }
}
