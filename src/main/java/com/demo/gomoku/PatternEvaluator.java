package com.demo.gomoku;

/**
 * 棋型评估器 - 单例模式，支持跳跃棋型识别与线型扫描
 * 跳跃棋型示例：XX_XX(跳跃四)、X_XXX(跳跃四)、P_PP(跳跃活三) 等
 */
public class PatternEvaluator {

    private static final PatternEvaluator INSTANCE = new PatternEvaluator();
    
    public static PatternEvaluator getInstance() {
        return INSTANCE;
    }
    
    // 私有构造函数防止外部实例化
    private PatternEvaluator() {}

    // 棋型分数（基于线型单次计分，数值已调整）
    public static final int SCORE_FIVE = 10000000;
    public static final int SCORE_FOUR = 5000000;
    public static final int SCORE_RUSH_FOUR = 1000000;
    public static final int SCORE_LIVE_THREE = 500000;
    public static final int SCORE_SLEEP_THREE = 30000;
    public static final int SCORE_LIVE_TWO = 10000;
    public static final int SCORE_SLEEP_TWO = 500;
    public static final int SCORE_ONE = 50;

    /**
     * 分析一条线上的棋型（支持跳跃检测）
     * @return [连续数, 总棋子数(含跳跃), 开放端数, 阻塞端数, 是否跳跃(0/1)]
     */
    public int[] analyzeLine(int[][] board, int row, int col, int player, int dRow, int dCol) {
        int n = GomokuBoard.BOARD_SIZE;

        // 正方向扫描
        int forwardConsec = 0;
        int forwardJump = 0;
        boolean forwardOpen = false;

        int r = row + dRow, c = col + dCol;
        while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
            forwardConsec++;
            r += dRow;
            c += dCol;
        }
        if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
            int jr = r + dRow, jc = c + dCol;
            while (jr >= 0 && jr < n && jc >= 0 && jc < n && board[jr][jc] == player) {
                forwardJump++;
                jr += dRow;
                jc += dCol;
            }
            if (forwardJump > 0) {
                // 有跳跃棋子：以跳跃后的末端判断开放性
                forwardOpen = (jr >= 0 && jr < n && jc >= 0 && jc < n && board[jr][jc] == GomokuBoard.EMPTY);
            } else {
                forwardOpen = true;
            }
        }

        // 反方向扫描
        int backwardConsec = 0;
        int backwardJump = 0;
        boolean backwardOpen = false;

        r = row - dRow;
        c = col - dCol;
        while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
            backwardConsec++;
            r -= dRow;
            c -= dCol;
        }
        if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
            int jr = r - dRow, jc = c - dCol;
            while (jr >= 0 && jr < n && jc >= 0 && jc < n && board[jr][jc] == player) {
                backwardJump++;
                jr -= dRow;
                jc -= dCol;
            }
            if (backwardJump > 0) {
                backwardOpen = (jr >= 0 && jr < n && jc >= 0 && jc < n && board[jr][jc] == GomokuBoard.EMPTY);
            } else {
                backwardOpen = true;
            }
        }

        int consec = 1 + forwardConsec + backwardConsec;
        int totalPieces = consec + forwardJump + backwardJump;
        int openEnds = (forwardOpen ? 1 : 0) + (backwardOpen ? 1 : 0);
        int blockedEnds = 2 - openEnds;
        int hasJump = (forwardJump > 0 || backwardJump > 0) ? 1 : 0;

        return new int[]{consec, totalPieces, openEnds, blockedEnds, hasJump};
    }

    /**
     * 根据棋型计算分数（支持跳跃棋型）
     */
    public int getLineScore(int[] pattern) {
        int consec = pattern[0];
        int total = pattern[1];
        int openEnds = pattern[2];
        boolean hasJump = pattern[4] == 1;

        return scorePattern(total, consec, openEnds, hasJump);
    }

    /**
     * 核心棋型评分
     */
    private int scorePattern(int total, int consec, int openEnds, boolean hasJump) {
        if (total >= 5) return SCORE_FIVE;

        // 跳跃四连：填空即五连（无论开放端多少，填空必胜）
        if (total == 4 && hasJump) return SCORE_RUSH_FOUR;

        // 连续四连
        if (consec == 4) {
            if (openEnds == 2) return SCORE_FOUR;      // 活四（不可防守）
            if (openEnds == 1) return SCORE_RUSH_FOUR;  // 冲四
            return 0;
        }

        // 三连
        if (total == 3) {
            if (openEnds >= 2) return SCORE_LIVE_THREE;  // 活三（含跳跃活三）
            if (openEnds == 1) return SCORE_SLEEP_THREE; // 眠三
            return 0;
        }

        // 二连
        if (total == 2) {
            if (openEnds >= 2) return SCORE_LIVE_TWO;
            if (openEnds == 1) return SCORE_SLEEP_TWO;
            return 0;
        }

        // 单子
        if (openEnds >= 1) return SCORE_ONE;
        return 0;
    }

    /**
     * 评估整个棋盘（线型扫描，避免重复计分）
     */
    public int evaluateBoard(int[][] board) {
        int score = 0;
        int n = GomokuBoard.BOARD_SIZE;

        // 扫描所有行
        for (int i = 0; i < n; i++) {
            score += scanLineScore(board, i, 0, 0, 1, n);
        }
        // 扫描所有列
        for (int j = 0; j < n; j++) {
            score += scanLineScore(board, 0, j, 1, 0, n);
        }
        // 主对角线（左上→右下）
        for (int k = -(n - 1); k <= n - 1; k++) {
            int sr = Math.max(0, -k);
            int sc = Math.max(0, k);
            int len = Math.min(n - sr, n - sc);
            score += scanLineScore(board, sr, sc, 1, 1, len);
        }
        // 副对角线（右上→左下）
        for (int k = 0; k <= 2 * (n - 1); k++) {
            int sr = Math.max(0, k - (n - 1));
            int sc = Math.min(n - 1, k);
            int len = Math.min(n - sr, sc + 1);
            score += scanLineScore(board, sr, sc, 1, -1, len);
        }

        // 中心位置加权
        int center = n / 2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    int dist = Math.abs(i - center) + Math.abs(j - center);
                    int posWeight = Math.max(0, 14 - dist);
                    score += (board[i][j] == GomokuBoard.WHITE) ? posWeight * 2 : -posWeight * 2;
                }
            }
        }

        return score;
    }

    /**
     * 扫描一条线并评分（每组棋型只计分一次）
     */
    private int scanLineScore(int[][] board, int startR, int startC, int dR, int dC, int len) {
        int score = 0;
        int idx = 0;

        while (idx < len) {
            int r = startR + idx * dR;
            int c = startC + idx * dC;
            if (board[r][c] == GomokuBoard.EMPTY) {
                idx++;
                continue;
            }

            int player = board[r][c];

            // 左端开放性
            boolean leftOpen = false;
            if (idx > 0) {
                int br = startR + (idx - 1) * dR;
                int bc = startC + (idx - 1) * dC;
                leftOpen = board[br][bc] == GomokuBoard.EMPTY;
            }

            // 计数连续棋子
            int consec = 0;
            while (idx < len) {
                int cr = startR + idx * dR;
                int cc = startC + idx * dC;
                if (board[cr][cc] == player) {
                    consec++;
                    idx++;
                } else {
                    break;
                }
            }

            // 检查跳跃（一个空格后的同色棋子）
            int jumpCount = 0;
            boolean hasJump = false;
            if (idx < len) {
                int gr = startR + idx * dR;
                int gc = startC + idx * dC;
                if (board[gr][gc] == GomokuBoard.EMPTY) {
                    int ji = idx + 1;
                    while (ji < len) {
                        int jr = startR + ji * dR;
                        int jc = startC + ji * dC;
                        if (board[jr][jc] == player) {
                            jumpCount++;
                            ji++;
                        } else {
                            break;
                        }
                    }
                    if (jumpCount > 0) {
                        hasJump = true;
                        idx = ji; // 跳过跳跃棋子组
                    }
                }
            }

            // 右端开放性
            boolean rightOpen = false;
            if (idx < len) {
                int ar = startR + idx * dR;
                int ac = startC + idx * dC;
                rightOpen = board[ar][ac] == GomokuBoard.EMPTY;
            }

            int openEnds = (leftOpen ? 1 : 0) + (rightOpen ? 1 : 0);
            int totalPieces = consec + jumpCount;
            int patternScore = scorePattern(totalPieces, consec, openEnds, hasJump);

            score += (player == GomokuBoard.WHITE) ? patternScore : -patternScore;
        }

        return score;
    }

    /**
     * 评估某个空位的价值（模拟落子后评估）
     */
    public int evaluatePosition(int[][] board, int row, int col, int player) {
        int score = 0;
        board[row][col] = player;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = analyzeLine(board, row, col, player, dir[0], dir[1]);
            score += getLineScore(pattern);
        }
        board[row][col] = GomokuBoard.EMPTY;
        return score;
    }

    /**
     * 快速评估某个空位的综合价值（攻+防）
     */
    public int quickEvaluate(int[][] board, int row, int col) {
        return evaluatePosition(board, row, col, GomokuBoard.WHITE)
             + evaluatePosition(board, row, col, GomokuBoard.BLACK);
    }

    /**
     * 评估某个已有棋子在四个方向上的价值
     */
    public int evaluateCellDirections(int[][] board, int row, int col, int player) {
        int score = 0;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = analyzeLine(board, row, col, player, dir[0], dir[1]);
            score += getLineScore(pattern);
        }
        return score;
    }

    /**
     * 在指定方向上连续计数
     */
    public int countInDirection(int[][] board, int row, int col, int player, int dRow, int dCol) {
        int count = 0;
        int r = row + dRow, c = col + dCol;
        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }

    /**
     * 检查位置是否有效且为空
     */
    public boolean isValidEmpty(int[][] board, int row, int col) {
        return row >= 0 && row < GomokuBoard.BOARD_SIZE
            && col >= 0 && col < GomokuBoard.BOARD_SIZE
            && board[row][col] == GomokuBoard.EMPTY;
    }

    /**
     * 检查是否是活三（两端无阻碍）
     */
    public boolean isLiveThree(int[][] board, int row, int col, int player) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int forward = countInDirection(board, row, col, player, dir[0], dir[1]);
            int backward = countInDirection(board, row, col, player, -dir[0], -dir[1]);

            if (forward + backward >= 2) {
                int frontR = row + (forward + 1) * dir[0];
                int frontC = col + (forward + 1) * dir[1];
                int backR = row - (backward + 1) * dir[0];
                int backC = col - (backward + 1) * dir[1];

                boolean frontEmpty = isValidEmpty(board, frontR, frontC);
                boolean backEmpty = isValidEmpty(board, backR, backC);

                if (frontEmpty && backEmpty) return true;
            }
        }
        return false;
    }
}
