package com.demo.gomoku;

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
    }
    
    public synchronized void reset(Difficulty difficulty) {
        reset();
        this.difficulty = difficulty;
        this.ai.setDifficulty(difficulty);
    }
    
    public synchronized void reset(int level) {
        reset(Difficulty.fromLevel(level));
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
