package com.demo.gomoku;

/**
 * 棋型评估器 - 计算各棋型的分值
 */
public class PatternEvaluator {
    
    // 棋型分数（提高差距，让AI更重视高级棋型）
    public static final int SCORE_FIVE = 10000000;
    public static final int SCORE_FOUR = 2000000;
    public static final int SCORE_RUSH_FOUR = 500000;
    public static final int SCORE_LIVE_THREE = 100000;
    public static final int SCORE_SLEEP_THREE = 15000;
    public static final int SCORE_LIVE_TWO = 3000;
    public static final int SCORE_SLEEP_TWO = 300;
    public static final int SCORE_ONE = 20;
    
    /**
     * 评估某个位置在四个方向上的价值
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
     * 分析一条线上的棋型
     * @return [连续数, 左空格, 右空格, 左阻塞, 右阻塞]
     */
    public int[] analyzeLine(int[][] board, int row, int col, int player, int dRow, int dCol) {
        int count = 1;
        int leftSpace = 0, rightSpace = 0;
        int leftBlocked = 0, rightBlocked = 0;
        
        // 正方向
        int r = row + dRow, c = col + dCol;
        int maxCheck = 4;
        while (maxCheck > 0 && r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE) {
            if (board[r][c] == player) {
                count++;
                r += dRow;
                c += dCol;
            } else if (board[r][c] == GomokuBoard.EMPTY) {
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
        while (maxCheck > 0 && r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE) {
            if (board[r][c] == player) {
                count++;
                r -= dRow;
                c -= dCol;
            } else if (board[r][c] == GomokuBoard.EMPTY) {
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
    public int getLineScore(int[] pattern) {
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
    public int quickEvaluate(int[][] board, int row, int col) {
        int score = 0;
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int[] pattern = analyzeLine(board, row, col, GomokuBoard.WHITE, dir[0], dir[1]);
            score += getLineScore(pattern);
            int[] patternB = analyzeLine(board, row, col, GomokuBoard.BLACK, dir[0], dir[1]);
            score += getLineScore(patternB);
        }
        return score;
    }
    
    /**
     * 评估整个棋盘
     */
    public int evaluateBoard(int[][] board) {
        int score = 0;
        int center = GomokuBoard.BOARD_SIZE / 2;
        
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    int player = board[i][j];
                    int dirScore = evaluateCellDirections(board, i, j, player);
                    
                    // 位置权重：靠近中心得分更高
                    int dist = Math.abs(i - center) + Math.abs(j - center);
                    int posWeight = Math.max(0, 14 - dist);
                    
                    if (player == GomokuBoard.WHITE) {
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
    
    /**
     * 在指定方向上连续计数
     */
    public int countInDirection(int[][] board, int row, int col, int player, int dRow, int dCol) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;
        
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
        return row >= 0 && row < GomokuBoard.BOARD_SIZE && col >= 0 && col < GomokuBoard.BOARD_SIZE && board[row][col] == GomokuBoard.EMPTY;
    }
}
