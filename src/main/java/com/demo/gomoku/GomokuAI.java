package com.demo.gomoku;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 五子棋AI主类
 * 协调各个模块：开局库、搜索引擎、威胁检测、评估器
 */
public class GomokuAI {

    private static final int BOARD_SIZE = 15;

    private final PatternEvaluator evaluator;
    private final ThreatDetector threatDetector;
    private final OpeningBook openingBook;
    private final GomokuSearcher searcher;
    private final TranspositionTable tt;
    private final SearchHeuristics heuristics;

    private Difficulty difficulty;

    // 引用到Game以支持取消检查
    private volatile GomokuGame game;

    public GomokuAI(Difficulty difficulty) {
        this.evaluator = PatternEvaluator.getInstance();
        this.threatDetector = new ThreatDetector(evaluator);
        this.openingBook = new OpeningBook();
        this.tt = new TranspositionTable();
        this.heuristics = new SearchHeuristics();
        this.searcher = new GomokuSearcher(evaluator, threatDetector, tt, heuristics);
        this.difficulty = difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setGame(GomokuGame game) {
        this.game = game;
        searcher.setGame(game);
    }

    public void clearGame() {
        this.game = null;
        searcher.clearGame();
    }

    public void clearTranspositionTable() {
        tt.clear();
        heuristics.clear();
    }

    /**
     * AI落子主入口
     */
    public int[] calculateMove(int[][] board) {
        if (game != null && game.isSearchCancelled()) {
            return null;
        }

        int moveCount = countPieces(board);

        if (moveCount == 0) {
            return new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2};
        }

        // 开局库查询
        int[] openingMove = openingBook.getMove(board, moveCount);
        if (openingMove != null) {
            return openingMove;
        }

        List<int[]> candidates = threatDetector.getCandidateMoves(
                board,
                difficulty.getSearchRange(),
                difficulty == Difficulty.HARD,
                difficulty.getCandidateLimit()
        );

        if (candidates.isEmpty()) {
            return new int[]{BOARD_SIZE / 2, BOARD_SIZE / 2};
        }

        // 必胜/必防检查（所有难度都必须检查）
        int[] aiWinMove = threatDetector.findAIWinOpportunity(board, GomokuBoard.WHITE);
        if (aiWinMove != null) return aiWinMove;

        int[] opponentWin = threatDetector.findOneMoveWin(board, GomokuBoard.BLACK);
        if (opponentWin != null) return opponentWin;

        int[] opponentJumpFour = threatDetector.findJumpFour(board, GomokuBoard.BLACK);
        if (opponentJumpFour != null) return opponentJumpFour;

        int[] opponentCombo = threatDetector.findComboThreat(board, GomokuBoard.BLACK);
        if (opponentCombo != null) return opponentCombo;

        int[] opponentThree = threatDetector.findExistingThree(board, GomokuBoard.BLACK);
        if (opponentThree != null) return opponentThree;

        // 根据难度选择搜索策略
        if (difficulty == Difficulty.HARD) {
            return searcher.search(board, candidates);
        }
        if (difficulty == Difficulty.MEDIUM) {
            return calculateMediumMove(board);
        }
        return calculateEasyMove(board);
    }

    // ===== 简单/中等难度实现 =====

    private int[] calculateEasyMove(int[][] board) {
        int[] dualMove = findDualPurposeMove(board, GomokuBoard.WHITE);
        if (dualMove != null) return dualMove;

        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = BOARD_SIZE / 2;

        List<int[]> candidates = threatDetector.getCandidateMoves(board, 3, true, 50);

        for (int[] move : candidates) {
            int attackScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.WHITE);
            int defenseScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.BLACK);
            double defenseWeight = difficulty.getDefenseWeight();
            int totalScore = attackScore + (int) (defenseScore * defenseWeight);

            int defenseBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.BLACK);
            int attackBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.WHITE);

            if (attackBonus > 0 && defenseBonus > 0) {
                totalScore += (attackBonus + defenseBonus) * 0.5;
            }
            totalScore += defenseBonus * 2;
            totalScore += attackBonus;

            int dist = Math.abs(move[0] - center) + Math.abs(move[1] - center);
            totalScore += Math.max(0, 15 - dist);

            scores.put(move[0] + "," + move[1], totalScore);
            maxScore = Math.max(maxScore, totalScore);
        }

        List<int[]> bestMoves = new ArrayList<>();
        for (int[] move : candidates) {
            int score = scores.get(move[0] + "," + move[1]);
            if (score >= maxScore * 0.9) {
                bestMoves.add(move);
            }
        }

        return bestMoves.get(ThreadLocalRandom.current().nextInt(bestMoves.size()));
    }

    private int[] calculateMediumMove(int[][] board) {
        int[] dualMove = findDualPurposeMove(board, GomokuBoard.WHITE);
        if (dualMove != null) return dualMove;

        Map<String, Integer> scores = new HashMap<>();
        int maxScore = Integer.MIN_VALUE;
        int center = BOARD_SIZE / 2;

        List<int[]> mediumCandidates = threatDetector.getCandidateMoves(board, 2, true, 25);

        for (int[] move : mediumCandidates) {
            int attackScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.WHITE);
            int defenseScore = evaluator.evaluatePosition(board, move[0], move[1], GomokuBoard.BLACK);
            double defenseWeight = difficulty.getDefenseWeight();
            int totalScore = attackScore + (int) (defenseScore * defenseWeight);

            int defenseBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.BLACK);
            int attackBonus = evaluateCriticalBonus(board, move[0], move[1], GomokuBoard.WHITE);

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

    private int[] findDualPurposeMove(int[][] board, int aiPlayer) {
        int opponent = (aiPlayer == GomokuBoard.WHITE) ? GomokuBoard.BLACK : GomokuBoard.WHITE;
        List<int[]> candidates = threatDetector.getCandidateMoves(board, 2, false, 30);
        if (candidates.isEmpty()) return null;

        int[] bestDualMove = null;
        int bestDualScore = -1;

        for (int[] move : candidates) {
            int attackValue = evaluateAttackValue(board, move[0], move[1], aiPlayer);
            int defenseValue = evaluateDefenseValue(board, move[0], move[1], opponent);

            int dualScore = attackValue + defenseValue;
            if (attackValue > 0 && defenseValue > 0) {
                dualScore += Math.min(attackValue, defenseValue) * 2;
            }

            if (dualScore > bestDualScore) {
                bestDualScore = dualScore;
                bestDualMove = move;
            }
        }

        if (bestDualScore > 50000) {
            return bestDualMove;
        }
        return null;
    }

    private int evaluateAttackValue(int[][] board, int row, int col, int player) {
        int value = 0;
        board[row][col] = player;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, player, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);
            if (lineScore >= PatternEvaluator.SCORE_FOUR) value += 5000000;
            else if (lineScore >= PatternEvaluator.SCORE_RUSH_FOUR) value += 1000000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) value += 500000;
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) value += 30000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_TWO) value += 10000;
        }
        board[row][col] = GomokuBoard.EMPTY;
        return value;
    }

    private int evaluateDefenseValue(int[][] board, int row, int col, int opponent) {
        int value = 0;
        board[row][col] = opponent;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = evaluator.analyzeLine(board, row, col, opponent, dir[0], dir[1]);
            int lineScore = evaluator.getLineScore(pattern);
            if (lineScore >= PatternEvaluator.SCORE_FIVE) value += 5000000;
            else if (lineScore >= PatternEvaluator.SCORE_FOUR) value += 2000000;
            else if (lineScore >= PatternEvaluator.SCORE_RUSH_FOUR) value += 1000000;
            else if (lineScore >= PatternEvaluator.SCORE_LIVE_THREE) value += 500000;
            else if (lineScore >= PatternEvaluator.SCORE_SLEEP_THREE) value += 30000;
        }
        board[row][col] = GomokuBoard.EMPTY;
        return value;
    }

    private int countPieces(int[][] board) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) count++;
            }
        }
        return count;
    }
}
