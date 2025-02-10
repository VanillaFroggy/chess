package com.internship.model.figure;

import com.internship.model.CellStatus;
import com.internship.model.Team;
import com.internship.model.game.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.internship.model.CellStatus.getCellStatus;

public abstract class Figure {
    protected Position position;
    protected final Team team;
    protected String name;

    public Figure(Position position, Team team) {
        this.position = position;
        this.team = team;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Team getTeam() {
        return team;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Figure figure = (Figure) o;
        return Objects.equals(position, figure.position) && team == figure.team && Objects.equals(name, figure.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, team, name);
    }

    @Override
    public String toString() {
        return "Figure{" +
                "position=" + position +
                ", team=" + team +
                '}';
    }

    public abstract List<Position> findPossibleMoves(Board board);

    protected List<Position> findPossibleMovesOnVerticalAndHorizontal(Board board, int maxWidth, int maxHeight) {
        List<Position> possibleMoves = new ArrayList<>(findPossibleMovesOnVerticalOrHorizontal(
                position.x(),
                maxWidth,
                (width) -> board.getCells()[width][position.y()],
                (width) -> new Position(width, position.y())
        ));
        possibleMoves.addAll(findPossibleMovesOnVerticalOrHorizontal(
                position.y(),
                maxHeight,
                (height) -> board.getCells()[position.x()][height],
                (height) -> new Position(position.x(), height)
        ));
        return possibleMoves;
    }

    private List<Position> findPossibleMovesOnVerticalOrHorizontal(
            int currentCoordinate,
            int maxCoordinate,
            Function<Integer, Figure> figureGetter,
            Function<Integer, Position> positionGetter
    ) {
        List<Position> possibleMoves = new ArrayList<>();
        for (int coordinate = currentCoordinate + 1; coordinate < maxCoordinate; coordinate++) {
            if (shouldBreakLoop(
                    possibleMoves,
                    getCellStatus(figureGetter.apply(coordinate), team),
                    positionGetter.apply(coordinate)
            )) break;
        }
        for (int coordinate = currentCoordinate - 1; coordinate >= 0; coordinate--) {
            if (shouldBreakLoop(
                    possibleMoves,
                    getCellStatus(figureGetter.apply(coordinate), team),
                    positionGetter.apply(coordinate)
            )) break;
        }
        return possibleMoves;
    }

    protected List<Position> findPossibleMovesOnAllDiagonals(Board board, int maxWidth, int maxHeight) {
        List<Position> possibleMoves = new ArrayList<>(findPossibleMovesOnDiagonal(
                board,
                maxWidth,
                maxHeight,
                (index) -> position.y() - index
        ));
        possibleMoves.addAll(findPossibleMovesOnDiagonal(
                board,
                maxWidth,
                maxHeight,
                (index) -> position.y() + index
        ));
        return possibleMoves;
    }

    private List<Position> findPossibleMovesOnDiagonal(
            Board board,
            int maxWidth,
            int maxHeight,
            Function<Integer, Integer> heightGetter
    ) {
        List<Position> possibleMoves = new ArrayList<>();
        for (int width = position.x() + 1; width < maxWidth; width++) {
            int height = heightGetter.apply(width - position.x());
            if (height < 0 || height >= maxHeight
                    || shouldBreakLoop(possibleMoves,
                    getCellStatus(board.getCells()[width][height], team),
                    new Position(width, height))
            ) {
                break;
            }
        }
        for (int width = position.x() - 1; width >= 0; width--) {
            int height = heightGetter.apply(position.x() - width);
            if (height < 0 || height >= maxHeight
                    || shouldBreakLoop(possibleMoves,
                    getCellStatus(board.getCells()[width][height], team),
                    new Position(width, height))
            ) {
                break;
            }
        }
        return possibleMoves;
    }

    protected boolean shouldBreakLoop(List<Position> possibleMoves, CellStatus cellStatus, Position position) {
        switch (cellStatus) {
            case EMPTY -> {
                possibleMoves.add(position);
                return false;
            }
            case OTHER_TEAM -> {
                return possibleMoves.add(position);
            }
            case SAME_TEAM -> {
                return true;
            }
            case OPPONENT_KING -> {
                return false;
            }
        }
        return false;
    }

//    protected CellStatus getCellStatus(Figure figure) {
//        return (figure == null)
//                ? (CellStatus.EMPTY)
//                : ((figure.getTeam().equals(team)) ? (CellStatus.SAME_TEAM)
//                : ((figure.getClass().equals(King.class)) ? (CellStatus.OPPONENT_KING) : (CellStatus.OTHER_TEAM)));
//    }
}
