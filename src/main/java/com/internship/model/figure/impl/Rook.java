package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.game.Board;
import com.internship.model.figure.Position;
import com.internship.model.figure.Figure;

import java.util.List;

public class Rook extends Figure {
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
