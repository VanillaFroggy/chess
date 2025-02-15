package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.figure.Figure;
import com.internship.model.figure.FigureWithFirstMove;
import com.internship.model.figure.Position;
import com.internship.model.game.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.internship.model.CellStatus.getCellStatus;

public class King extends FigureWithFirstMove {
    public King(Position position, Team team) {
        super(position, team);
        name = "K";
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        List<Position> possibleMoves = new ArrayList<>(
                findPossibleMovesOnAllDiagonals(
                        board,
                        position.x() < Board.WIDTH - 1 ? position.x() + 2 : position.x() + 1,
                        position.y() < Board.HEIGHT - 1 ? position.y() + 2 : position.y() + 1
                )
        );
        possibleMoves.addAll(findPossibleMovesOnVerticalAndHorizontal(
                board,
                position.x() < Board.WIDTH - 1 ? position.x() + 2 : position.x() + 1,
                position.y() < Board.HEIGHT - 1 ? position.y() + 2 : position.y() + 1
        ));
        possibleMoves.removeIf(movement -> movement.x() < position.x() - 1 || movement.y() < position.y() - 1);
        if (firstMove) {
            possibleMoves.addAll(findCastlingMoves(board));
        }
        return possibleMoves;
    }

    private List<Position> findCastlingMoves(Board board) {
        List<Position> figures = new ArrayList<>();
        for (int width = position.x() + 1; width < Board.WIDTH; width++) {
            if (shouldBreakCastlingLoop(figures, board.getCells()[width][position.y()])) {
                break;
            }
        }
        for (int width = position.x() - 1; width >= 0; width--) {
            if (shouldBreakCastlingLoop(figures, board.getCells()[width][position.y()])) {
                break;
            }
        }
        return figures;
    }

    private boolean shouldBreakCastlingLoop(List<Position> figures, Figure figure) {
        switch (getCellStatus(figure, team)) {
            case SAME_TEAM -> {
                if (figure.getClass().equals(Rook.class)) {
                    figures.add(figure.getPosition());
                }
                return true;
            }
            case OTHER_TEAM, OPPONENT_KING -> {
                return true;
            }
            case EMPTY -> {
                return false;
            }
        }
        return true;
    }

    public boolean opponentCoversAllMoves(
            Map<Figure, List<Position>> opponentPossibleMovesByFigures,
            List<Position> possibleMoves
    ) {
        return opponentPossibleMovesByFigures.entrySet()
                .stream()
                .flatMap(figureListEntry -> figureListEntry.getValue().stream())
                .collect(Collectors.toSet())
                .containsAll(possibleMoves);
    }

    public boolean isCheckmated(Map<Figure, List<Position>> opponentPossibleMovesByFigures) {
        return opponentPossibleMovesByFigures.entrySet()
                .stream()
                .flatMap(figureListEntry -> figureListEntry.getValue().stream())
                .anyMatch(element -> element.equals(position));
    }
}
