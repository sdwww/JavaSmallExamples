package com.demo.gomoku;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

/**
 * 五子棋游戏服务（线程安全）
 * 优化点：
 * 1. 使用 ConcurrentHashMap 保证线程安全
 * 2. 添加定时清理机制，防止内存泄漏
 * 3. 统一管理游戏状态
 * 4. CompletableFuture 异步AI计算
 */
@Service
public class GameService {
    
    // 使用线程安全的 ConcurrentHashMap
    private final Map<String, GomokuGame> games = new ConcurrentHashMap<>();
    
    // 游戏最后活跃时间（用于清理）
    private final Map<String, Long> lastActiveTime = new ConcurrentHashMap<>();
    
    // 异步AI计算任务
    private final Map<String, CompletableFuture<int[]>> pendingAiMoves = new ConcurrentHashMap<>();
    
    // AI计算线程池
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
        r -> {
            Thread t = new Thread(r, "AI-Async-Worker");
            t.setDaemon(true);
            return t;
        }
    );
    
    // 游戏超时时间：30分钟
    private static final long GAME_TIMEOUT_MS = 30 * 60 * 1000;
    
    /**
     * 创建新游戏
     */
    public GomokuGame createGame(Difficulty difficulty) {
        String sessionId = java.util.UUID.randomUUID().toString();
        GomokuGame game = new GomokuGame(difficulty);
        games.put(sessionId, game);
        lastActiveTime.put(sessionId, System.currentTimeMillis());
        return game;
    }
    
    /**
     * 创建新游戏（指定 sessionId）
     */
    public GomokuGame createGame(String sessionId, Difficulty difficulty) {
        GomokuGame game = new GomokuGame(difficulty);
        games.put(sessionId, game);
        lastActiveTime.put(sessionId, System.currentTimeMillis());
        return game;
    }
    
    /**
     * 获取或创建游戏
     */
    public GomokuGame getOrCreateGame(String sessionId, Difficulty difficulty) {
        updateActiveTime(sessionId);
        return games.computeIfAbsent(sessionId, k -> {
            lastActiveTime.put(k, System.currentTimeMillis());
            return new GomokuGame(difficulty);
        });
    }
    
    /**
     * 获取游戏
     */
    public GomokuGame getGame(String sessionId) {
        if (sessionId != null && games.containsKey(sessionId)) {
            updateActiveTime(sessionId);
            return games.get(sessionId);
        }
        return null;
    }
    
    /**
     * 重置游戏
     */
    public GomokuGame resetGame(String sessionId, Difficulty difficulty) {
        GomokuGame game = games.get(sessionId);
        if (game != null) {
            game.reset(difficulty);
            updateActiveTime(sessionId);
            return game;
        } else {
            return createGame(sessionId, difficulty);
        }
    }
    
    /**
     * 设置难度
     */
    public void setDifficulty(String sessionId, Difficulty difficulty) {
        GomokuGame game = games.get(sessionId);
        if (game != null) {
            game.setDifficulty(difficulty);
            updateActiveTime(sessionId);
        }
    }
    
    /**
     * 删除游戏
     */
    public void removeGame(String sessionId) {
        games.remove(sessionId);
        lastActiveTime.remove(sessionId);
        cancelAiMove(sessionId);
    }
    
    // ===== 异步AI计算 =====
    
    /**
     * 启动异步AI计算
     */
    public void startAsyncAiMove(String sessionId) {
        GomokuGame game = games.get(sessionId);
        if (game == null || game.isGameOver() || game.getCurrentPlayer() != GomokuBoard.WHITE) {
            return;
        }
        
        // 取消之前的计算（如果有）
        cancelAiMove(sessionId);
        
        CompletableFuture<int[]> future = CompletableFuture.supplyAsync(() -> {
            return game.aiMove();
        }, aiExecutor);
        
        pendingAiMoves.put(sessionId, future);
    }
    
    /**
     * 检查异步AI计算是否完成
     * @return AI落子位置，未完成返回null
     */
    public int[] getAsyncAiMoveResult(String sessionId) {
        CompletableFuture<int[]> future = pendingAiMoves.get(sessionId);
        if (future == null) {
            return null;
        }
        
        if (future.isDone()) {
            pendingAiMoves.remove(sessionId);
            try {
                return future.get();
            } catch (Exception e) {
                return null;
            }
        }
        
        return null; // 还在计算中
    }
    
    /**
     * AI是否正在计算
     */
    public boolean isAiThinking(String sessionId) {
        CompletableFuture<int[]> future = pendingAiMoves.get(sessionId);
        return future != null && !future.isDone();
    }
    
    /**
     * 取消异步AI计算
     */
    public void cancelAiMove(String sessionId) {
        CompletableFuture<int[]> future = pendingAiMoves.remove(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }
    
    /**
     * 更新活跃时间
     */
    private void updateActiveTime(String sessionId) {
        lastActiveTime.put(sessionId, System.currentTimeMillis());
    }
    
    /**
     * 定时清理过期游戏（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredGames() {
        long currentTime = System.currentTimeMillis();
        games.entrySet().removeIf(entry -> {
            String sessionId = entry.getKey();
            long lastActive = lastActiveTime.getOrDefault(sessionId, 0L);
            if (currentTime - lastActive > GAME_TIMEOUT_MS) {
                lastActiveTime.remove(sessionId);
                cancelAiMove(sessionId);
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取游戏数量（用于监控）
     */
    public int getGameCount() {
        return games.size();
    }
    
    /**
     * 优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        aiExecutor.shutdown();
        try {
            if (!aiExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                aiExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            aiExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
