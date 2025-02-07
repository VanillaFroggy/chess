package com.internship.model.game;

import com.internship.model.Team;
import com.internship.model.figure.Figure;

import java.util.Set;

public record Player(Game game, Team team, Set<Figure> figures) implements Runnable {
    @Override
    public void run() {
        while (game.isGameInProcess()) {
            game.makeMove(this);
        }
    }
}
