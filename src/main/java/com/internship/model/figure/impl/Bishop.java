package com.internship.model.figure.impl;

import com.internship.model.game.Board;
import com.internship.model.figure.Position;
import com.internship.model.Team;
import com.internship.model.figure.Figure;

import java.util.List;

public class Bishop extends Figure {
    public Bishop(Position position, Team team) {
        super(position, team);
        name = "B";
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        return findPossibleMovesOnAllDiagonals(board, Board.WIDTH, Board.HEIGHT);
    }
}
