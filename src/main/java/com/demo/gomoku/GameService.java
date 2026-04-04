package com.demo.gomoku;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 五子棋游戏服务（线程安全）
 * 优化点：
 * 1. 使用 ConcurrentHashMap 保证线程安全
 * 2. 添加定时清理机制，防止内存泄漏
 * 3. 统一管理游戏状态
 */
@Service
public class GameService {
    
    // 使用线程安全的 ConcurrentHashMap
    private final Map<String, GomokuGame> games = new ConcurrentHashMap<>();
    
    // 游戏最后活跃时间（用于清理）
    private final Map<String, Long> lastActiveTime = new ConcurrentHashMap<>();
    
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
    public void resetGame(String sessionId, Difficulty difficulty) {
        GomokuGame game = games.get(sessionId);
        if (game != null) {
            game.reset(difficulty);
            updateActiveTime(sessionId);
        } else {
            createGame(difficulty);
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
}
