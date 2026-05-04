package com.demo.minesweeper;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mine-sweeper")
@SessionAttributes("game")
public class MineSweeperController {

    @ModelAttribute("game")
    public MineSweeperGame game(HttpSession session) {
        MineSweeperGame game = (MineSweeperGame) session.getAttribute("game");
        if (game == null) {
            game = new MineSweeperGame();
            session.setAttribute("game", game);
        }
        return game;
    }

    @PostMapping("/click")
    public MineSweeperGame.ClickResult click(@ModelAttribute("game") MineSweeperGame game,
                                            @RequestParam int row,
                                            @RequestParam int col) {
        return game.click(row, col);
    }

    @PostMapping("/flag")
    public MineSweeperGame.FlagResult flag(@ModelAttribute("game") MineSweeperGame game,
                                           @RequestParam int row,
                                           @RequestParam int col) {
        return game.flag(row, col);
    }

    @GetMapping("/board")
    public int[][] board(@ModelAttribute("game") MineSweeperGame game) {
        return game.getBoard();
    }

    @PostMapping("/reset")
    public void reset(@ModelAttribute("game") MineSweeperGame game) {
        game.reset();
    }
}