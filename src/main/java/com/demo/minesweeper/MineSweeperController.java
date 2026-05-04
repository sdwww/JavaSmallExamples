package com.demo.minesweeper;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mine-sweeper")
public class MineSweeperController {

    private final GameStorage gameStorage;

    public MineSweeperController(GameStorage gameStorage) {
        this.gameStorage = gameStorage;
    }

    @GetMapping("/new")
    public String newGame() {
        return gameStorage.createGame();
    }

    @GetMapping("/board")
    public int[][] board(@RequestParam String gameId) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        return game.getBoard();
    }

    @PostMapping("/click")
    public ClickResult click(@RequestParam String gameId,
                             @RequestParam int row,
                             @RequestParam int col) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        return game.click(row, col);
    }

    @PostMapping("/flag")
    public FlagResult flag(@RequestParam String gameId,
                           @RequestParam int row,
                           @RequestParam int col) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        return game.flag(row, col);
    }

    @PostMapping("/reset")
    public void reset(@RequestParam String gameId) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        game.reset();
    }
}