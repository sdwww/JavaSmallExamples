package com.demo.minesweeper;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStorage {

    private static final long EXPIRE_TIME_MS = 24 * 60 * 60 * 1000L; // 24 小时

    /**
     * 一局游戏的会话快照：游戏实例 + 最近访问时间，整体不可变。
     * 每次续命都生成新对象 → cleanup 用 remove(key, value) CAS 删除时可识别是否被续命。
     */
    private static final class GameSession {
        final MineSweeperGame game;
        final long lastAccess;

        GameSession(MineSweeperGame game, long lastAccess) {
            this.game = game;
            this.lastAccess = lastAccess;
        }
    }

    private final ConcurrentHashMap<String, GameSession> games = new ConcurrentHashMap<>();

    public String createGame(Difficulty difficulty) {
        String gameId = UUID.randomUUID().toString();
        games.put(gameId, new GameSession(new MineSweeperGame(difficulty), System.currentTimeMillis()));
        return gameId;
    }

    public MineSweeperGame getGame(String gameId) {
        // computeIfPresent 是 ConcurrentHashMap 的原子复合操作：读 + 改 + 写在同一锁段内完成。
        // 此处用"换 session 引用"代替修改字段，让 cleanup 能用 CAS remove(k, v) 区分"读到时" vs "删除时"是否被续命。
        GameSession session = games.computeIfPresent(gameId,
                (k, old) -> new GameSession(old.game, System.currentTimeMillis()));
        return session == null ? null : session.game;
    }

    @Scheduled(fixedRate = 3600000) // 每小时清理一次
    public void cleanup() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (var entry : games.entrySet()) {
            GameSession snapshot = entry.getValue();
            if (now - snapshot.lastAccess > EXPIRE_TIME_MS) {
                // CAS 删除：只有 snapshot 引用没换才删；
                // 若期间被 getGame 续命，session 对象已替换为新实例，remove 返回 false → 跳过
                if (games.remove(entry.getKey(), snapshot)) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            System.out.println("清理了 " + removed + " 个过期游戏");
        }
    }
}
