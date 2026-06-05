package com.demo.minesweeper;

import com.demo.common.api.BusinessException;
import com.demo.common.api.ErrorCode;
import com.demo.common.api.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mine-sweeper")
public class MineSweeperController {

    private final GameStorage gameStorage;

    public MineSweeperController(GameStorage gameStorage) {
        this.gameStorage = gameStorage;
    }

    @GetMapping("/new")
    public Result<NewGameResult> newGame(@RequestParam(defaultValue = "EASY") Difficulty difficulty) {
        String gameId = gameStorage.createGame(difficulty);
        MineSweeperGame game = gameStorage.getGame(gameId);
        return Result.ok(new NewGameResult(
                gameId, game.getRows(), game.getCols(), game.getMines(), game.getBoard()));
    }

    @PostMapping("/click")
    public Result<ClickResult> click(@RequestParam String gameId,
                                     @RequestParam int row,
                                     @RequestParam int col) {
        return Result.ok(requireGame(gameId).click(row, col));
    }

    @PostMapping("/flag")
    public Result<FlagResult> flag(@RequestParam String gameId,
                                   @RequestParam int row,
                                   @RequestParam int col) {
        return Result.ok(requireGame(gameId).flag(row, col));
    }

    @PostMapping("/reset")
    public Result<int[][]> reset(@RequestParam String gameId) {
        MineSweeperGame game = requireGame(gameId);
        game.reset();
        return Result.ok(game.getBoard());
    }

    @PostMapping("/chord")
    public Result<ClickResult> chord(@RequestParam String gameId,
                                     @RequestParam int row,
                                     @RequestParam int col) {
        return Result.ok(requireGame(gameId).chord(row, col));
    }

    /** 取 game；不存在则抛业务异常，由 GlobalExceptionHandler 转 Result.fail */
    private MineSweeperGame requireGame(String gameId) {
        MineSweeperGame game = gameStorage.getGame(gameId);
        if (game == null) {
            throw new BusinessException(ErrorCode.GAME_NOT_FOUND);
        }
        return game;
    }
}
