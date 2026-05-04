package com.demo.minesweeper;

import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStorage {

    private final ConcurrentHashMap<String, MineSweeperGame> games = new ConcurrentHashMap<>();

    public String createGame() {
        String gameId = UUID.randomUUID().toString();
        games.put(gameId, new MineSweeperGame());
        return gameId;
    }

    public MineSweeperGame getGame(String gameId) {
        return games.get(gameId);
    }
}