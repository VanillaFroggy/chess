package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.figure.Figure;
import com.internship.model.figure.Position;
import com.internship.model.game.Board;

import java.util.List;

public class Queen extends Figure {
    public Queen(Position position, Team team) {
        super(position, team);
        name = "Q";
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        List<Position> possibleMoves = findPossibleMovesOnAllDiagonals(
                board,
                Board.WIDTH,
                Board.HEIGHT
        );
        possibleMoves.addAll(findPossibleMovesOnVerticalAndHorizontal(
                board,
                Board.WIDTH,
                Board.HEIGHT
        ));
        return possibleMoves;
    }
}
