package com.internship.model.game;

import com.internship.model.Team;
import com.internship.model.figure.Figure;
import com.internship.model.figure.Position;
import com.internship.model.figure.impl.*;

public class Board {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    private final Figure[][] cells = new Figure[WIDTH][HEIGHT];

    public Figure[][] getCells() {
        return cells;
    }

    public void prepareBoard() {
        for (int height = 0; height < Board.HEIGHT; height++) {
            for (int width = 0; width < Board.WIDTH; width++) {
                switch (height) {
                    case 0:
                        cells[width][height] = placeFigure(width, height, Team.WHITE);
                        break;
                    case Board.HEIGHT - 1:
                        cells[width][height] = placeFigure(width, height, Team.BLACK);
                        break;
                    case 1, Board.HEIGHT - 2:
                        cells[width][height] = new Pawn(
                                new Position(width, height),
                                height == 1 ? Team.WHITE : Team.BLACK
                        );
                        break;
                }
            }
        }
    }

    public Figure placeFigure(int width, int height, Team team) {
        return switch (width) {
            case 0, Board.WIDTH - 1 -> new Rook(new Position(width, height), team);
            case 1, Board.WIDTH - 2 -> new Knight(new Position(width, height), team);
            case 2, Board.WIDTH - 3 -> new Bishop(new Position(width, height), team);
            case 3 -> new Queen(new Position(width, height), team);
            case 4 -> new King(new Position(width, height), team);
            default -> null;
        };
    }
}
