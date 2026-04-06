package com.demo.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 威胁检测器 - 检测棋盘上的关键威胁（支持跳跃棋型）
 */
public class ThreatDetector {

    private final PatternEvaluator evaluator;

    public ThreatDetector() {
        this.evaluator = PatternEvaluator.getInstance();
    }
    
    public ThreatDetector(PatternEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * 检查是否有必胜或必防的棋步（优化版：使用候选列表）
     * @return 需要落子的位置，或 null
     */
    public int[] findImmediateWinOrBlock(int[][] board) {
        // 获取威胁候选位置
        List<int[]> candidates = getThreatCandidates(board);
        
        // 1. AI一步获胜
        int[] winMove = findOneMoveWin(board, GomokuBoard.WHITE, candidates);
        if (winMove != null) return winMove;

        // 2. 玩家一步获胜，必须拦截
        int[] blockWin = findOneMoveWin(board, GomokuBoard.BLACK, candidates);
        if (blockWin != null) return blockWin;

        // 3. AI的跳跃四连（填空即五连）
        int[] aiJumpFour = findJumpFour(board, GomokuBoard.WHITE);
        if (aiJumpFour != null) return aiJumpFour;

        // 4. 玩家的跳跃四连（必须拦截）
        int[] blockJumpFour = findJumpFour(board, GomokuBoard.BLACK);
        if (blockJumpFour != null) return blockJumpFour;

        // 5. 玩家的三连威胁（活三/眠三）- 必须拦截
        int[] blockThree = findExistingThree(board, GomokuBoard.BLACK);
        if (blockThree != null) return blockThree;

        // 6. 玩家的组合威胁（双活三/四三）
        int[] comboThreat = findComboThreat(board, GomokuBoard.BLACK);
        if (comboThreat != null) return comboThreat;

        // 7. 玩家的冲四威胁
        int[] rushFour = findRushFourThreat(board, GomokuBoard.BLACK);
        if (rushFour != null) return rushFour;

        // 8. AI的冲四进攻
        int[] aiRushFour = findRushFourThreat(board, GomokuBoard.WHITE);
        if (aiRushFour != null) return aiRushFour;

        // 9. 玩家的活三威胁（落子后形成活三）
        int[] liveThree = findLiveThreeThreat(board, GomokuBoard.BLACK);
        if (liveThree != null) return liveThree;

        return null;
    }

    /**
     * 获取威胁候选位置：与棋子相邻的空位
     * 这些位置是唯一可能形成威胁的位置
     */
    private List<int[]> getThreatCandidates(int[][] board) {
        Set<String> candidates = new HashSet<>();
        int n = GomokuBoard.BOARD_SIZE;
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    // 检查周围1格内的空位
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if (di == 0 && dj == 0) continue;
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < n && nj >= 0 && nj < n && board[ni][nj] == GomokuBoard.EMPTY) {
                                candidates.add(ni + "," + nj);
                            }
                        }
                    }
                }
            }
        }
        
        if (candidates.isEmpty()) {
            candidates.add((n / 2) + "," + (n / 2));
        }
        
        List<int[]> result = new ArrayList<>();
        for (String pos : candidates) {
            String[] parts = pos.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    /**
     * 查找一步获胜的位置（优化版：使用候选列表）
     */
    public int[] findOneMoveWin(int[][] board, int player, List<int[]> candidates) {
        for (int[] pos : candidates) {
            int i = pos[0], j = pos[1];
            board[i][j] = player;
            boolean hasWin = checkWin(board, i, j);
            board[i][j] = GomokuBoard.EMPTY;
            if (hasWin) return pos;
        }
        return null;
    }

    /**
     * 查找一步获胜的位置（全盘扫描版）
     */
    public int[] findOneMoveWin(int[][] board, int player) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = player;
                    boolean hasWin = checkWin(board, i, j);
                    board[i][j] = GomokuBoard.EMPTY;
                    if (hasWin) {
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找跳跃四连（如 XX_XX, X_XXX, PPP_P 等）
     * 填补空位即可形成五连
     */
    public int[] findJumpFour(int[][] board, int player) {
        int n = GomokuBoard.BOARD_SIZE;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) continue;
                if (player == GomokuBoard.EMPTY) continue;

                // 尝试在此处落子，检查是否形成跳跃四连模式
                // 即：该位置是某个四子(含一空)组的空位
                board[i][j] = player;
                // 检查四个方向是否存在"四子含此位"的模式
                for (int[] dir : GomokuBoard.DIRECTIONS) {
                    int total = 1;
                    // 正方向
                    int r = i + dir[0], c = j + dir[1];
                    while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                        total++;
                        r += dir[0];
                        c += dir[1];
                    }
                    // 反方向
                    r = i - dir[0];
                    c = j - dir[1];
                    while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                        total++;
                        r -= dir[0];
                        c -= dir[1];
                    }
                    if (total >= 4) {
                        board[i][j] = GomokuBoard.EMPTY;
                        return new int[]{i, j};
                    }
                }
                board[i][j] = GomokuBoard.EMPTY;
            }
        }
        return null;
    }

    /**
     * 查找冲四威胁
     */
    public int[] findRushFourThreat(int[][] board, int opponent) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = opponent;
                    if (hasPatternWithCheck(board, i, j, opponent, 4, false)) {
                        board[i][j] = GomokuBoard.EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = GomokuBoard.EMPTY;
                }
            }
        }
        return null;
    }

    /**
     * 查找棋盘上已有的三连（包括活三、眠三和跳跃三连）
     * 增强版：优先检测并防守活三
     */
    public int[] findExistingThree(int[][] board, int player) {
        int n = GomokuBoard.BOARD_SIZE;
        int[] bestDefensePos = null;
        int bestThreatLevel = 0; // 0=无威胁, 1=眠三, 2=活三, 3=跳跃三连
        
        // 1. 先检测连续三连（OOO）
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == player) {
                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        int count = 1;
                        int openEnds = 0;
                        int emptyEnd1Row = -1, emptyEnd1Col = -1;
                        int emptyEnd2Row = -1, emptyEnd2Col = -1;
                        
                        // 正方向计数
                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
                            openEnds++;
                            emptyEnd1Row = r; 
                            emptyEnd1Col = c;
                        }
                        
                        // 反方向计数
                        r = i - dir[0];
                        c = j - dir[1];
                        while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == player) {
                            count++;
                            r -= dir[0];
                            c -= dir[1];
                        }
                        if (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == GomokuBoard.EMPTY) {
                            openEnds++;
                            if (emptyEnd1Row == -1) {
                                emptyEnd1Row = r; 
                                emptyEnd1Col = c;
                            } else {
                                emptyEnd2Row = r; 
                                emptyEnd2Col = c;
                            }
                        }
                        
                        // 找到连续三连（count == 3）
                        if (count == 3 && openEnds >= 1) {
                            // 找到活三或眠三，优先防守活三
                            int[] defensePos = null;
                            if (openEnds == 2) {
                                // 活三：两端都开放，防守任意一端
                                defensePos = new int[]{emptyEnd1Row, emptyEnd1Col};
                            } else if (openEnds == 1) {
                                // 眠三：一端开放，返回开放端位置
                                defensePos = new int[]{emptyEnd1Row, emptyEnd1Col};
                            }
                            
                            if (defensePos != null) {
                                if (openEnds == 2 && bestThreatLevel < 2) {
                                    bestDefensePos = defensePos;
                                    bestThreatLevel = 2; // 活三
                                } else if (openEnds == 1 && bestThreatLevel < 1) {
                                    bestDefensePos = defensePos;
                                    bestThreatLevel = 1; // 眠三
                                }
                            }
                        }
                        
                        // 检测四连（被堵一端）- 冲四
                        if (count == 4 && openEnds == 1) {
                            if (emptyEnd1Row >= 0 && bestThreatLevel < 4) {
                                bestDefensePos = new int[]{emptyEnd1Row, emptyEnd1Col};
                                bestThreatLevel = 4; // 冲四
                            }
                        }
                    }
                }
            }
        }
        
        // 2. 再检测跳跃三连（如 O_OO, OO_O, O_OO_ 等）
        int[] jumpThree = findJumpThree(board, player);
        if (jumpThree != null && bestThreatLevel < 3) {
            return jumpThree; // 跳跃三连是较高威胁
        }
        
        return bestDefensePos;
    }
    
    /**
     * 查找跳跃三连（如 O_OO, OO_O 等）
     * 这是一种间接三连威胁
     * 只有当落子后能形成五连时才防守
     */
    private int[] findJumpThree(int[][] board, int player) {
        int n = GomokuBoard.BOARD_SIZE;
            
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == player) {
                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        // 模式: 棋子 + 空位 + 棋子 + 棋子 (O_OO)
                        int r1 = i + dir[0], c1 = j + dir[1];  // 空位位置
                        int r2 = i + 2 * dir[0], c2 = j + 2 * dir[1];  // 棋子
                        int r3 = i + 3 * dir[0], c3 = j + 3 * dir[1];  // 棋子
                            
                        // 基础条件：O_OO 模式
                        if (r1 >= 0 && r1 < n && c1 >= 0 && c1 < n && 
                            board[r1][c1] == GomokuBoard.EMPTY &&
                            r2 >= 0 && r2 < n && c2 >= 0 && c2 < n && board[r2][c2] == player &&
                            r3 >= 0 && r3 < n && c3 >= 0 && c3 < n && board[r3][c3] == player) {
                                
                            // 检查落子后能否形成五连
                            // 方案A: O_OO_ (右边有空位)
                            int r4 = i + 4 * dir[0], c4 = j + 4 * dir[1];
                            if (r4 >= 0 && r4 < n && c4 >= 0 && c4 < n && board[r4][c4] == GomokuBoard.EMPTY) {
                                // 右边有空位，可以形成五连
                                return new int[]{r1, c1};
                            }
                                
                            // 方案B: _O_OO (左边有连续棋子)
                            // 检查从 i-1 向左有多少连续棋子
                            int leftCount = 0;
                            int nr = i - dir[0], nc = j - dir[1];
                            while (nr >= 0 && nr < n && nc >= 0 && nc < n && board[nr][nc] == player) {
                                leftCount++;
                                nr -= dir[0];
                                nc -= dir[1];
                            }
                            if (leftCount >= 2) {
                                // 左边有至少2个连续棋子，可以形成五连
                                return new int[]{r1, c1};
                            }
                        }
                            
                        // 模式2: OO_O (两棋子 + 空位 + 棋子)
                        // _ _ O _ O O
                        int r_1 = i - dir[0], c_1 = j - dir[1];  // 反方向棋子
                        int r_2 = i - 2 * dir[0], c_2 = j - 2 * dir[1];  // 反方向第二棋子
                            
                        if (r_1 >= 0 && r_1 < n && c_1 >= 0 && c_1 < n && board[r_1][c_1] == player &&
                            r_2 >= 0 && r_2 < n && c_2 >= 0 && c_2 < n && board[r_2][c_2] == player &&
                            r1 >= 0 && r1 < n && c1 >= 0 && c1 < n && board[r1][c1] == GomokuBoard.EMPTY &&
                            r2 >= 0 && r2 < n && c2 >= 0 && c2 < n && board[r2][c2] == player) {
                            // 方案A: OO_OO (右边有空位)
                            int r3f = i + 3 * dir[0], c3f = j + 3 * dir[1];
                            if (r3f >= 0 && r3f < n && c3f >= 0 && c3f < n && board[r3f][c3f] == GomokuBoard.EMPTY) {
                                return new int[]{r1, c1};
                            }
                                
                            // 方案B: _OO_O_ (右边被堵，检查左边)
                            int r_3 = i - 3 * dir[0], c_3 = j - 3 * dir[1];
                            if (r_3 >= 0 && r_3 < n && c_3 >= 0 && c_3 < n && board[r_3][c_3] == GomokuBoard.EMPTY) {
                                // 检查从 r_3 向左是否至少1个空位（形成 _ _ O _ O O _）
                                int r_4 = i - 4 * dir[0], c_4 = j - 4 * dir[1];
                                if (r_4 >= 0 && r_4 < n && c_4 >= 0 && c_4 < n && board[r_4][c_4] == GomokuBoard.EMPTY) {
                                    return new int[]{r1, c1};
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找活三威胁
     */
    public int[] findLiveThreeThreat(int[][] board, int opponent) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = opponent;
                    if (hasPatternWithCheck(board, i, j, opponent, 3, true)) {
                        board[i][j] = GomokuBoard.EMPTY;
                        return new int[]{i, j};
                    }
                    board[i][j] = GomokuBoard.EMPTY;
                }
            }
        }
        return null;
    }

    /**
     * 检查落子后是否形成指定棋型
     */
    private boolean hasPatternWithCheck(int[][] board, int row, int col, int player, int targetCount, boolean requireLive) {
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1;
            int emptyEnds = 0;

            int r = row + dir[0], c = col + dir[1];
            while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                count++;
                r += dir[0];
                c += dir[1];
            }
            if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                emptyEnds++;
            }

            r = row - dir[0];
            c = col - dir[1];
            while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                count++;
                r -= dir[0];
                c -= dir[1];
            }
            if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                emptyEnds++;
            }

            if (count >= targetCount) {
                if (!requireLive || emptyEnds == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查找AI的必胜机会
     * 如果AI落子后能形成必胜组合（如双活三、四三等），直接返回该位置
     */
    public int[] findAIWinOpportunity(int[][] board, int player) {
        // 先检查AI能否一步获胜
        int[] immediateWin = findOneMoveWin(board, player);
        if (immediateWin != null) {
            return immediateWin;
        }
        
        // 检查AI的组合必胜机会
        return findComboWinOpportunity(board, player);
    }
    
    /**
     * 查找AI的组合必胜机会（双活三、四三等）
     */
    private int[] findComboWinOpportunity(int[][] board, int player) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    // 检查落子后能否形成必胜组合
                    board[i][j] = player;
                    
                    int liveThreeCount = 0;      // 活三数量
                    int rushFourCount = 0;        // 冲四数量
                    int jumpFourCount = 0;         // 跳跃四数量
                    
                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        // 检测连续四连
                        int count = 1;
                        int emptyEnds = 0;
                        
                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                        }
                        
                        r = i - dir[0];
                        c = j - dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == player) {
                            count++;
                            r -= dir[0];
                            c -= dir[1];
                        }
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                        }
                        
                        if (count == 4 && emptyEnds >= 1) {
                            rushFourCount++;
                        }
                        
                        // 检测跳跃四
                        int r1 = i + dir[0], c1 = j + dir[1];
                        int r2 = i + 2 * dir[0], c2 = j + 2 * dir[1];
                        int r3 = i + 3 * dir[0], c3 = j + 3 * dir[1];
                        int r4 = i + 4 * dir[0], c4 = j + 4 * dir[1];
                        
                        // O_OOO 模式
                        if (r1 >= 0 && r1 < GomokuBoard.BOARD_SIZE && c1 >= 0 && c1 < GomokuBoard.BOARD_SIZE && board[r1][c1] == player &&
                            r2 >= 0 && r2 < GomokuBoard.BOARD_SIZE && c2 >= 0 && c2 < GomokuBoard.BOARD_SIZE && board[r2][c2] == GomokuBoard.EMPTY &&
                            r3 >= 0 && r3 < GomokuBoard.BOARD_SIZE && c3 >= 0 && c3 < GomokuBoard.BOARD_SIZE && board[r3][c3] == player &&
                            r4 >= 0 && r4 < GomokuBoard.BOARD_SIZE && c4 >= 0 && c4 < GomokuBoard.BOARD_SIZE && board[r4][c4] == player) {
                            jumpFourCount++;
                        }
                    }
                    
                    board[i][j] = GomokuBoard.EMPTY;
                    
                    // 必胜组合判断
                    // 1. 双冲四（含跳跃四）
                    if (rushFourCount + jumpFourCount >= 2) {
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 检测双活三/四三/活三+眠三组合（包括跳跃棋型）
     */
    public int[] findComboThreat(int[][] board, int opponent) {
        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] == GomokuBoard.EMPTY) {
                    board[i][j] = opponent;

                    int liveThreeCount = 0;      // 活三数量（两端开放）
                    int sleepThreeCount = 0;      // 眠三数量（一端开放）
                    int rushFourCount = 0;        // 冲四数量（连续四连）
                    int jumpFourCount = 0;         // 跳跃四数量（隔一位的四连）
                    int jumpThreeCount = 0;        // 跳跃三数量（隔一位的三连）

                    for (int[] dir : GomokuBoard.DIRECTIONS) {
                        // ===== 检测连续棋型 =====
                        int count = 1;
                        int emptyEnds = 0;

                        int r = i + dir[0], c = j + dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r += dir[0];
                            c += dir[1];
                        }
                        boolean end1Empty = false;
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                            end1Empty = true;
                        }

                        r = i - dir[0];
                        c = j - dir[1];
                        while (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == opponent) {
                            count++;
                            r -= dir[0];
                            c -= dir[1];
                        }
                        boolean end2Empty = false;
                        if (r >= 0 && r < GomokuBoard.BOARD_SIZE && c >= 0 && c < GomokuBoard.BOARD_SIZE && board[r][c] == GomokuBoard.EMPTY) {
                            emptyEnds++;
                            end2Empty = true;
                        }

                        if (count >= 5) {
                            // 五连或更多，立即返回
                            board[i][j] = GomokuBoard.EMPTY;
                            return new int[]{i, j};
                        } else if (count == 4 && emptyEnds >= 1) {
                            // 冲四（连续四连）
                            rushFourCount++;
                        } else if (count == 3) {
                            // 三连
                            if (emptyEnds == 2) {
                                liveThreeCount++; // 活三
                            } else if (emptyEnds == 1) {
                                sleepThreeCount++; // 眠三
                            }
                        }
                        
                        // ===== 检测跳跃四 O_OOO（隔一位的四连）=====
                        // 模式：棋子 + 空位 + 棋子 + 棋子 + 棋子
                        int r1 = i + dir[0], c1 = j + dir[1];           // 第一个棋子
                        int r2 = i + 2 * dir[0], c2 = j + 2 * dir[1];   // 空位
                        int r3 = i + 3 * dir[0], c3 = j + 3 * dir[1];   // 棋子
                        int r4 = i + 4 * dir[0], c4 = j + 4 * dir[1];   // 棋子
                        int r_1 = i - dir[0], c_1 = j - dir[1];          // 反方向棋子
                        
                        // 模式1: O_OOO + 一端有棋子
                        if (r1 >= 0 && r1 < GomokuBoard.BOARD_SIZE && c1 >= 0 && c1 < GomokuBoard.BOARD_SIZE && board[r1][c1] == opponent &&
                            r2 >= 0 && r2 < GomokuBoard.BOARD_SIZE && c2 >= 0 && c2 < GomokuBoard.BOARD_SIZE && board[r2][c2] == GomokuBoard.EMPTY &&
                            r3 >= 0 && r3 < GomokuBoard.BOARD_SIZE && c3 >= 0 && c3 < GomokuBoard.BOARD_SIZE && board[r3][c3] == opponent &&
                            r4 >= 0 && r4 < GomokuBoard.BOARD_SIZE && c4 >= 0 && c4 < GomokuBoard.BOARD_SIZE && board[r4][c4] == opponent) {
                            // 检查 r2 空位的两端是否都有棋子
                            if (r_1 >= 0 && r_1 < GomokuBoard.BOARD_SIZE && c_1 >= 0 && c_1 < GomokuBoard.BOARD_SIZE && board[r_1][c_1] == opponent) {
                                jumpFourCount++;
                            }
                            // 或者检查 r4 后面是否还有空间
                            int r5 = i + 5 * dir[0], c5 = j + 5 * dir[1];
                            if (r5 >= 0 && r5 < GomokuBoard.BOARD_SIZE && c5 >= 0 && c5 < GomokuBoard.BOARD_SIZE && board[r5][c5] == GomokuBoard.EMPTY) {
                                jumpFourCount++;
                            }
                        }
                        
                        // 模式2: OO_OO（两端各两棋子中间空位）
                        if (r2 >= 0 && r2 < GomokuBoard.BOARD_SIZE && c2 >= 0 && c2 < GomokuBoard.BOARD_SIZE && board[r2][c2] == GomokuBoard.EMPTY) {
                            if (r1 >= 0 && r1 < GomokuBoard.BOARD_SIZE && c1 >= 0 && c1 < GomokuBoard.BOARD_SIZE && board[r1][c1] == opponent &&
                                r_1 >= 0 && r_1 < GomokuBoard.BOARD_SIZE && c_1 >= 0 && c_1 < GomokuBoard.BOARD_SIZE && board[r_1][c_1] == opponent) {
                                if (r3 >= 0 && r3 < GomokuBoard.BOARD_SIZE && c3 >= 0 && c3 < GomokuBoard.BOARD_SIZE && board[r3][c3] == opponent &&
                                    r4 >= 0 && r4 < GomokuBoard.BOARD_SIZE && c4 >= 0 && c4 < GomokuBoard.BOARD_SIZE && board[r4][c4] == opponent) {
                                    jumpFourCount++;
                                }
                            }
                        }
                        
                        // ===== 检测跳跃三 O_OO（隔一位的三连）=====
                        // 模式：棋子 + 空位 + 棋子 + 棋子
                        // r1=空位, r2=棋子, r3=棋子
                        if (r1 >= 0 && r1 < GomokuBoard.BOARD_SIZE && c1 >= 0 && c1 < GomokuBoard.BOARD_SIZE && board[r1][c1] == GomokuBoard.EMPTY &&
                            r2 >= 0 && r2 < GomokuBoard.BOARD_SIZE && c2 >= 0 && c2 < GomokuBoard.BOARD_SIZE && board[r2][c2] == opponent &&
                            r3 >= 0 && r3 < GomokuBoard.BOARD_SIZE && c3 >= 0 && c3 < GomokuBoard.BOARD_SIZE && board[r3][c3] == opponent) {
                            
                            // 方案A: O_OO_ (右边有空位)
                            if (r4 >= 0 && r4 < GomokuBoard.BOARD_SIZE && c4 >= 0 && c4 < GomokuBoard.BOARD_SIZE && board[r4][c4] == GomokuBoard.EMPTY) {
                                // 检查左边是否有棋子能形成五连
                                if (r_1 >= 0 && r_1 < GomokuBoard.BOARD_SIZE && c_1 >= 0 && c_1 < GomokuBoard.BOARD_SIZE && board[r_1][c_1] == opponent) {
                                    jumpThreeCount++;
                                }
                            }
                            
                            // 方案B: _O_OO (左边有连续棋子)
                            int leftCount = 0;
                            int nr = i - dir[0], nc = j - dir[1];
                            while (nr >= 0 && nr < GomokuBoard.BOARD_SIZE && nc >= 0 && nc < GomokuBoard.BOARD_SIZE && board[nr][nc] == opponent) {
                                leftCount++;
                                nr -= dir[0];
                                nc -= dir[1];
                            }
                            if (leftCount >= 2) {
                                // 左边有至少2个连续棋子，可以形成五连
                                jumpThreeCount++;
                            }
                        }
                        
                        // ===== 检测跳跃三 OO_O（两棋子 + 空位 + 棋子）=====
                        // _ _ O _ O O
                        int r_2 = i - 2 * dir[0], c_2 = j - 2 * dir[1];  // 反方向第二棋子
                        if (r_1 >= 0 && r_1 < GomokuBoard.BOARD_SIZE && c_1 >= 0 && c_1 < GomokuBoard.BOARD_SIZE && board[r_1][c_1] == opponent &&
                            r_2 >= 0 && r_2 < GomokuBoard.BOARD_SIZE && c_2 >= 0 && c_2 < GomokuBoard.BOARD_SIZE && board[r_2][c_2] == opponent &&
                            r1 >= 0 && r1 < GomokuBoard.BOARD_SIZE && c1 >= 0 && c1 < GomokuBoard.BOARD_SIZE && board[r1][c1] == GomokuBoard.EMPTY &&
                            r2 >= 0 && r2 < GomokuBoard.BOARD_SIZE && c2 >= 0 && c2 < GomokuBoard.BOARD_SIZE && board[r2][c2] == opponent) {
                            // 检查 r1 空位后面是否还有空间
                            if (r3 >= 0 && r3 < GomokuBoard.BOARD_SIZE && c3 >= 0 && c3 < GomokuBoard.BOARD_SIZE && board[r3][c3] == GomokuBoard.EMPTY) {
                                jumpThreeCount++;
                            }
                            // 检查 r_2 左边是否还有空间
                            int r_3 = i - 3 * dir[0], c_3 = j - 3 * dir[1];
                            if (r_3 >= 0 && r_3 < GomokuBoard.BOARD_SIZE && c_3 >= 0 && c_3 < GomokuBoard.BOARD_SIZE && board[r_3][c_3] == GomokuBoard.EMPTY) {
                                jumpThreeCount++;
                            }
                        }
                    }

                    board[i][j] = GomokuBoard.EMPTY;

                    // 检测各种组合威胁（按优先级）：
                    // 1. 五连（已在上方处理）
                    
                    // 2. 双活三（必防）
                    if (liveThreeCount >= 2) {
                        return new int[]{i, j};
                    }
                    // 3. 四三组合（冲四 + 活三，包括跳跃四）
                    if ((rushFourCount >= 1 || jumpFourCount >= 1) && liveThreeCount >= 1) {
                        return new int[]{i, j};
                    }
                    // 4. 双冲四（包括跳跃四）
                    if (rushFourCount + jumpFourCount >= 2) {
                        return new int[]{i, j};
                    }
                    // 5. 冲四 + 眠三
                    if ((rushFourCount >= 1 || jumpFourCount >= 1) && sleepThreeCount >= 1) {
                        return new int[]{i, j};
                    }
                    // 6. 活三 + 眠三组合（必防）
                    if (liveThreeCount >= 1 && sleepThreeCount >= 1) {
                        return new int[]{i, j};
                    }
                    // 7. 跳跃三 + 眠三组合
                    if (jumpThreeCount >= 1 && sleepThreeCount >= 1) {
                        return new int[]{i, j};
                    }
                    // 8. 双跳跃三
                    if (jumpThreeCount >= 2) {
                        return new int[]{i, j};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取候选落子位置
     */
    public List<int[]> getCandidateMoves(int[][] board, int searchRange, boolean sortByScore, int limit) {
        Set<String> candidates = new HashSet<>();

        for (int i = 0; i < GomokuBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < GomokuBoard.BOARD_SIZE; j++) {
                if (board[i][j] != GomokuBoard.EMPTY) {
                    for (int di = -searchRange; di <= searchRange; di++) {
                        for (int dj = -searchRange; dj <= searchRange; dj++) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < GomokuBoard.BOARD_SIZE && nj >= 0 && nj < GomokuBoard.BOARD_SIZE
                                    && board[ni][nj] == GomokuBoard.EMPTY) {
                                candidates.add(ni + "," + nj);
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            int center = GomokuBoard.BOARD_SIZE / 2;
            for (int di = -2; di <= 2; di++) {
                for (int dj = -2; dj <= 2; dj++) {
                    int ni = center + di;
                    int nj = center + dj;
                    if (ni >= 0 && ni < GomokuBoard.BOARD_SIZE && nj >= 0 && nj < GomokuBoard.BOARD_SIZE) {
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

        if (sortByScore) {
            PatternEvaluator eval = this.evaluator;
            int center = GomokuBoard.BOARD_SIZE / 2;
            result.sort((a, b) -> {
                int scoreA = eval.quickEvaluate(board, a[0], a[1]);
                int scoreB = eval.quickEvaluate(board, b[0], b[1]);
                int distA = Math.abs(a[0] - center) + Math.abs(a[1] - center);
                int distB = Math.abs(b[0] - center) + Math.abs(b[1] - center);
                return (scoreB + Math.max(0, 15 - distB) * 10) -
                       (scoreA + Math.max(0, 15 - distA) * 10);
            });
        }

        if (limit > 0 && result.size() > limit) {
            result = result.subList(0, limit);
        }

        return result;
    }

    /**
     * 检查是否五子连珠
     */
    public boolean checkWin(int[][] board, int row, int col) {
        int player = board[row][col];
        for (int[] dir : GomokuBoard.DIRECTIONS) {
            int count = 1;
            count += evaluator.countInDirection(board, row, col, player, dir[0], dir[1]);
            count += evaluator.countInDirection(board, row, col, player, -dir[0], -dir[1]);
            if (count >= 5) return true;
        }
        return false;
    }
}
