package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.figure.FigureWithFirstMove;
import com.internship.model.figure.Position;
import com.internship.model.game.Board;

import java.util.List;

public class Rook extends FigureWithFirstMove {
    public Rook(Position position, Team team) {
        super(position, team);
        name = "R";
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        return findPossibleMovesOnVerticalAndHorizontal(
                board,
                Board.WIDTH,
                Board.HEIGHT
        );
    }
}
