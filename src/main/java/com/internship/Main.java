package com.internship;

import com.internship.model.game.Game;

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        game.prepareGame();
        game.startGame();
    }
}