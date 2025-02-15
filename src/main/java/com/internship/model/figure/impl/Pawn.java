package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.CellStatus;
import com.internship.model.figure.Figure;
import com.internship.model.figure.FigureWithFirstMove;
import com.internship.model.figure.Position;
import com.internship.model.game.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BinaryOperator;

import static com.internship.model.CellStatus.getCellStatus;

public class Pawn extends FigureWithFirstMove {
    private boolean readyForCaptureByOpponentPawn = false;

    public Pawn(Position position, Team team) {
        super(position, team);
        name = "";
    }

    public boolean isReadyForCaptureByOpponentPawn() {
        return readyForCaptureByOpponentPawn;
    }

    public void setReadyForCaptureByOpponentPawn(boolean readyForCaptureByOpponentPawn) {
        this.readyForCaptureByOpponentPawn = readyForCaptureByOpponentPawn;
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        List<Position> possibleMoves = new ArrayList<>();
        BinaryOperator<Integer> moveByHeight = team.equals(Team.WHITE) ? Integer::sum : (a, b) -> a - b;
        if ((team.equals(Team.WHITE) && position.y() < Board.HEIGHT - 1)
                || (team.equals(Team.BLACK) && position.y() > 0)) {
            if (getCellStatus(board.getCells()[position.x()][moveByHeight.apply(position.y(), 1)], team)
                    .equals(CellStatus.EMPTY)) {
                possibleMoves.add(new Position(position.x(), moveByHeight.apply(position.y(), 1)));
                if (firstMove
                        && getCellStatus(board.getCells()[position.x()][moveByHeight.apply(position.y(), 2)], team)
                        .equals(CellStatus.EMPTY)) {
                    possibleMoves.add(new Position(position.x(), moveByHeight.apply(position.y(), 2)));
                }
            }
            if (position.x() > 0
                    && (getCellStatus(board.getCells()[position.x() - 1][moveByHeight.apply(position.y(), 1)], team)
                    .equals(CellStatus.OTHER_TEAM)
                    || getCellStatus(board.getCells()[position.x() - 1][moveByHeight.apply(position.y(), 1)], team)
                    .equals(CellStatus.OPPONENT_KING)
                    || canCapturePawnByRightOrLeftSide(board, position.x() - 1, position.y()))) {
                possibleMoves.add(new Position(position.x() - 1, moveByHeight.apply(position.y(), 1)));
            }
            if (position.x() < Board.WIDTH - 1
                    && (getCellStatus(board.getCells()[position.x() + 1][moveByHeight.apply(position.y(), 1)], team)
                    .equals(CellStatus.OTHER_TEAM)
                    || getCellStatus(board.getCells()[position.x() + 1][moveByHeight.apply(position.y(), 1)], team)
                    .equals(CellStatus.OPPONENT_KING)
                    || canCapturePawnByRightOrLeftSide(board, position.x() + 1, position.y()))) {
                possibleMoves.add(new Position(position.x() + 1, moveByHeight.apply(position.y(), 1)));
            }
        }
        return possibleMoves;
    }

    private boolean canCapturePawnByRightOrLeftSide(Board board, int x, int y) {
        int opponentPawnStartHeight = team.equals(Team.WHITE) ? Board.HEIGHT - 2 : 1;
        return getCellStatus(board.getCells()[x][y], team).equals(CellStatus.OTHER_TEAM)
                && board.getCells()[x][y].getClass().equals(Pawn.class)
                && ((Pawn) board.getCells()[x][y]).isReadyForCaptureByOpponentPawn()
                && board.getCells()[x][y].getLastPosition().y() == opponentPawnStartHeight
                && board.getCells()[x][y].getLastPosition().y() - opponentPawnStartHeight == 2;
    }

    public Figure promote() {
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> {
                Knight knight = new Knight(getLastPosition(), team);
                knight.setPosition(position);
                return knight;
            }
            case 1 -> {
                Bishop bishop = new Bishop(getLastPosition(), team);
                bishop.setPosition(position);
                return bishop;
            }
            case 2 -> {
                Rook rook = new Rook(getLastPosition(), team);
                rook.setPosition(position);
                return rook;
            }
            case 3 -> {
                Queen queen = new Queen(getLastPosition(), team);
                queen.setPosition(position);
                return queen;
            }
        }
        return null;
    }

    public boolean canPromote() {
        return team.equals(Team.WHITE) ? position.y() == Board.HEIGHT - 1 : position.y() == 0;
    }
}
