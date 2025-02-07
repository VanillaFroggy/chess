package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.figure.CellStatus;
import com.internship.model.figure.Figure;
import com.internship.model.figure.Position;
import com.internship.model.game.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BinaryOperator;

public class Pawn extends Figure {
    private boolean firstMove = true;

    public Pawn(Position position, Team team) {
        super(position, team);
        name = "";
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        List<Position> possibleMoves = new ArrayList<>();
        BinaryOperator<Integer> moveByHeight = team.equals(Team.WHITE) ? Integer::sum : (a, b) -> a - b;
        if ((team.equals(Team.WHITE) && position.y() < Board.HEIGHT - 1)
                || (team.equals(Team.BLACK) && position.y() > 0)) {
            if (getCellStatus(board.getCells()[position.x()][moveByHeight.apply(position.y(), 1)])
                    .equals(CellStatus.EMPTY)) {
                possibleMoves.add(new Position(position.x(), moveByHeight.apply(position.y(), 1)));
                if (firstMove && getCellStatus(board.getCells()[position.x()][moveByHeight.apply(position.y(), 2)])
                        .equals(CellStatus.EMPTY)) {
                    possibleMoves.add(new Position(position.x(), moveByHeight.apply(position.y(), 2)));
                    firstMove = false;
                }
            }
            if (position.x() > 0
                    && getCellStatus(board.getCells()[position.x() - 1][moveByHeight.apply(position.y(), 1)])
                    .equals(CellStatus.OTHER_TEAM)) {
                possibleMoves.add(new Position(position.x() - 1, moveByHeight.apply(position.y(), 1)));
            }
            if (position.x() < Board.WIDTH - 1
                    && getCellStatus(board.getCells()[position.x() + 1][moveByHeight.apply(position.y(), 1)])
                    .equals(CellStatus.OTHER_TEAM)) {
                possibleMoves.add(new Position(position.x() + 1, moveByHeight.apply(position.y(), 1)));
            }
        }
        return possibleMoves;
    }

    public Figure promote() {
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> {
                return new Knight(position, team);
            }
            case 1 -> {
                return new Bishop(position, team);
            }
            case 2 -> {
                return new Rook(position, team);
            }
            case 3 -> {
                return new Queen(position, team);
            }
        }
        return null;
    }

    public boolean canPromote() {
        return team.equals(Team.WHITE) ? position.y() == Board.HEIGHT - 1 : position.y() == 0;
    }
}
