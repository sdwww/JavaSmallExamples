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
    public NewGameResult newGame() {
        String gameId = gameStorage.createGame();
        MineSweeperGame game = gameStorage.getGame(gameId);
        return new NewGameResult(gameId, game.getBoard());
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
    public int[][] reset(@RequestParam String gameId) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        game.reset();
        return game.getBoard();
    }
}