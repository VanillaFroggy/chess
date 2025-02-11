package com.internship.model.figure.impl;

import com.internship.model.Team;
import com.internship.model.figure.Figure;
import com.internship.model.figure.Position;
import com.internship.model.game.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static com.internship.model.CellStatus.getCellStatus;

public class Knight extends Figure {
    public Knight(Position position, Team team) {
        super(position, team);
        name = "N";
    }

    @Override
    public List<Position> findPossibleMoves(Board board) {
        List<Position> possibleMoves = findPossibleMovesForOneSide(
                board,
                position.x() + 1,
                (width) -> width < position.x() + 3,
                (width) -> width + 1
        );
        possibleMoves.addAll(findPossibleMovesForOneSide(
                board,
                position.x() - 1,
                (width) -> width > position.x() - 3,
                (width) -> width - 1
        ));
        return possibleMoves;
    }

    private List<Position> findPossibleMovesForOneSide(
            Board board,
            int startWidth,
            Predicate<Integer> widthPredicate,
            UnaryOperator<Integer> widthOperation
    ) {
        List<Position> possibleMoves = new ArrayList<>();
        for (int width = startWidth, index = 2;
             widthPredicate.test(width);
             width = widthOperation.apply(width), index--) {
            if (correctWidth(width)) {
                if (correctHeight(position.y() + index)) {
                    shouldBreakLoop(
                            possibleMoves,
                            getCellStatus(board.getCells()[width][position.y() + index], team),
                            new Position(width, position.y() + index)
                    );
                }
                if (correctHeight(position.y() - index)) {
                    shouldBreakLoop(
                            possibleMoves,
                            getCellStatus(board.getCells()[width][position.y() - index], team),
                            new Position(width, position.y() - index)
                    );
                }
            }
        }
        return possibleMoves;
    }

    private static boolean correctWidth(int width) {
        return correctCoordinate(width, Board.WIDTH);
    }

    private static boolean correctHeight(int height) {
        return correctCoordinate(height, Board.HEIGHT);
    }

    private static boolean correctCoordinate(int coordinate, int maxCoordinate) {
        return coordinate < maxCoordinate && coordinate >= 0;
    }
}
