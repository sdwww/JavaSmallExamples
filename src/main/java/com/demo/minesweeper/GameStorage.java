package com.demo.minesweeper;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStorage {

    private final ConcurrentHashMap<String, MineSweeperGame> games = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    private static final long EXPIRE_TIME_MS = 24 * 60 * 60 * 1000; // 24小时

    public String createGame(Difficulty difficulty) {
        String gameId = UUID.randomUUID().toString();
        games.put(gameId, new MineSweeperGame(difficulty));
        lastAccessTime.put(gameId, System.currentTimeMillis());
        return gameId;
    }

    public MineSweeperGame getGame(String gameId) {
        MineSweeperGame game = games.get(gameId);
        if (game != null) {
            lastAccessTime.put(gameId, System.currentTimeMillis());
        }
        return game;
    }

    @Scheduled(fixedRate = 3600000) // 每小时清理一次
    public void cleanup() {
        long now = System.currentTimeMillis();
        int removed = 0;
        Iterator<String> iterator = games.keySet().iterator();
        while (iterator.hasNext()) {
            String gameId = iterator.next();
            Long lastAccess = lastAccessTime.get(gameId);
            if (lastAccess != null && now - lastAccess > EXPIRE_TIME_MS) {
                iterator.remove();
                lastAccessTime.remove(gameId);
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("清理了 " + removed + " 个过期游戏");
        }
    }
}