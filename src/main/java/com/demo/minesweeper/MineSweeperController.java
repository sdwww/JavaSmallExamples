package com.demo.minesweeper;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mine-sweeper")
public class MineSweeperController {

    private final GameStorage gameStorage;

    public MineSweeperController(GameStorage gameStorage) {
        this.gameStorage = gameStorage;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e) {
        return e.getMessage();
    }

    @GetMapping("/new")
    public NewGameResult newGame(@RequestParam(defaultValue = "EASY") Difficulty difficulty) {
        String gameId = gameStorage.createGame(difficulty);
        MineSweeperGame game = gameStorage.getGame(gameId);
        return new NewGameResult(gameId, game.getRows(), game.getCols(), game.getMines(), game.getBoard());
    }

    @PostMapping("/click")
    public ClickResult click(@RequestParam String gameId,
                             @RequestParam int row,
                             @RequestParam int col) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        return game.click(row, col);
    }

    @PostMapping("/flag")
    public FlagResult flag(@RequestParam String gameId,
                           @RequestParam int row,
                           @RequestParam int col) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        return game.flag(row, col);
    }

    @PostMapping("/reset")
    public int[][] reset(@RequestParam String gameId) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        game.reset();
        return game.getBoard();
    }

    @PostMapping("/chord")
    public ClickResult chord(@RequestParam String gameId,
                             @RequestParam int row,
                             @RequestParam int col) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        return game.chord(row, col);
    }
}