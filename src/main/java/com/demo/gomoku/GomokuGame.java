package com.demo.gomoku;

import java.util.ArrayList;
import java.util.List;

/**
 * 五子棋游戏逻辑类（精简版）
 * 使用 GomokuBoard 存储棋盘数据
 * 使用 GomokuAI 处理AI逻辑
 */
public class GomokuGame {
    
    private GomokuBoard board;
    private GomokuAI ai;
    private int currentPlayer;
    private boolean gameOver;
    private int winner;
    private Difficulty difficulty;
    
    // 落子历史记录 [row, col, player]
    private final List<int[]> moveHistory = new ArrayList<>();
    
    public GomokuGame() {
        this(Difficulty.MEDIUM);
    }
    
    public GomokuGame(Difficulty difficulty) {
        this.board = new GomokuBoard();
        this.ai = new GomokuAI(difficulty);
        this.currentPlayer = GomokuBoard.BLACK;
        this.gameOver = false;
        this.winner = GomokuBoard.EMPTY;
        this.difficulty = difficulty;
    }
    
    /**
     * 设置先手方
     * @param firstPlayer GomokuBoard.BLACK(玩家先手) 或 GomokuBoard.WHITE(AI先手)
     */
    public void setFirstPlayer(int firstPlayer) {
        if (firstPlayer == GomokuBoard.BLACK || firstPlayer == GomokuBoard.WHITE) {
            this.currentPlayer = firstPlayer;
        }
    }
    
    public GomokuGame(int difficultyLevel) {
        this(Difficulty.fromLevel(difficultyLevel));
    }
    
    /**
     * 落子
     */
    public synchronized boolean makeMove(int row, int col) {
        return makeMove(row, col, currentPlayer);
    }
    
    /**
     * 指定玩家落子
     */
    public synchronized boolean makeMove(int row, int col, int player) {
        if (gameOver || !board.isValidMove(row, col)) {
            return false;
        }
        
        board.makeMove(row, col, player);
        moveHistory.add(new int[]{row, col, player}); // 记录历史
        
        if (board.checkWin(row, col)) {
            gameOver = true;
            winner = player;
        } else if (board.isFull()) {
            gameOver = true;
            winner = GomokuBoard.EMPTY;
        } else {
            currentPlayer = (currentPlayer == GomokuBoard.BLACK) ? GomokuBoard.WHITE : GomokuBoard.BLACK;
        }
        
        return true;
    }
    
    /**
     * AI落子
     */
    public synchronized int[] aiMove() {
        if (gameOver || currentPlayer != GomokuBoard.WHITE) {
            return null;
        }
        
        int[] move = ai.calculateMove(board.getBoard());
        
        if (move != null) {
            makeMove(move[0], move[1], GomokuBoard.WHITE);
        }
        return move;
    }
    
    /**
     * 重置游戏
     */
    public synchronized void reset() {
        board.reset();
        currentPlayer = GomokuBoard.BLACK;
        gameOver = false;
        winner = GomokuBoard.EMPTY;
        moveHistory.clear();
        ai.clearTranspositionTable();
    }
    
    public synchronized void reset(Difficulty difficulty) {
        reset();
        this.difficulty = difficulty;
        this.ai.setDifficulty(difficulty);
    }
    
    public synchronized void reset(int level) {
        reset(Difficulty.fromLevel(level));
    }
    
    /**
     * 悔棋（撤回玩家和AI的最后一步）
     * @return 是否悔棋成功
     */
    public synchronized boolean undoMove() {
        // 游戏结束时不允许悔棋
        if (gameOver) {
            return false;
        }
        
        if (moveHistory.size() < 2) {
            return false; // 至少需要两步（玩家+AI）才能悔棋
        }
        
        // 撤回AI的最后一步
        int[] lastAiMove = moveHistory.remove(moveHistory.size() - 1);
        board.undoMove(lastAiMove[0], lastAiMove[1]);
        
        // 撤回玩家的最后一步
        int[] lastPlayerMove = moveHistory.remove(moveHistory.size() - 1);
        board.undoMove(lastPlayerMove[0], lastPlayerMove[1]);
        
        // 恢复为玩家回合
        currentPlayer = GomokuBoard.BLACK;
        
        return true;
    }
    
    /**
     * 获取落子历史
     */
    public List<int[]> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }
    
    /**
     * 获取落子历史（简化格式，用于前端显示）
     */
    public List<String> getMoveHistoryText() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < moveHistory.size(); i++) {
            int[] move = moveHistory.get(i);
            char colChar = (char) ('A' + move[1]);
            int rowNum = move[0] + 1;
            String playerName = move[2] == GomokuBoard.BLACK ? "黑" : "白";
            result.add(String.format("%d. %s %c%d", i + 1, playerName, colChar, rowNum));
        }
        return result;
    }
    
    // Getters
    public int[][] getBoard() {
        return board.getBoard();
    }
    
    public int getCurrentPlayer() {
        return currentPlayer;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public int getWinner() {
        return winner;
    }
    
    public int getMoveCount() {
        return board.getMoveCount();
    }
    
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.ai.setDifficulty(difficulty);
    }
    
    public void setDifficulty(int level) {
        setDifficulty(Difficulty.fromLevel(level));
    }
}
