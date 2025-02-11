package com.internship.model.game;

import com.internship.model.KingStatus;
import com.internship.model.Team;
import com.internship.model.figure.Figure;
import com.internship.model.figure.FigureWithFirstMove;
import com.internship.model.figure.Position;
import com.internship.model.figure.impl.King;
import com.internship.model.figure.impl.Knight;
import com.internship.model.figure.impl.Pawn;
import com.internship.model.figure.impl.Rook;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static com.internship.model.CellStatus.getCellStatus;

public class Game {
    private final Lock lock = new ReentrantLock();
    private final Condition canMove = lock.newCondition();
    private Player lastPlayer;
    private final Board board = new Board();
    private final Player[] players = new Player[2];
    private boolean gameInProcess = false;

    public boolean isGameInProcess() {
        return gameInProcess;
    }

    public void prepareGame() {
        board.prepareBoard();
        players[0] = new Player(
                this,
                Team.WHITE,
                new HashSet<>(
                        Arrays.stream(board.getCells())
                                .flatMap(Arrays::stream)
                                .filter(figure -> figure != null && figure.getTeam().equals(Team.WHITE))
                                .toList()
                )
        );
        players[1] = new Player(
                this,
                Team.BLACK,
                new HashSet<>(
                        Arrays.stream(board.getCells())
                                .flatMap(Arrays::stream)
                                .filter(figure -> figure != null && figure.getTeam().equals(Team.BLACK))
                                .toList()
                )
        );
        lastPlayer = players[1];
    }

    public void startGame() {
        gameInProcess = true;
        for (Player player : players) {
            new Thread(player).start();
        }
    }

    public void tryMakeMove(Player player) {
        lock.lock();
        try {
            while (player.equals(lastPlayer)) {
                canMove.await();
            }
            if (!gameInProcess) return;
            Player opponent = player.equals(players[0]) ? players[1] : players[0];
            KingStatus kingStatus = getKingStatus(player, opponent);
            if (onlyKingsOnBoard(player, opponent) || kingStatus.equals(KingStatus.CHECKMATED)) {
                gameInProcess = false;
                lastPlayer = player;
                canMove.signal();
                lock.unlock();
                return;
            } else if (kingStatus.equals(KingStatus.PROTECTED)) {
                promotePawns(player);
                return;
            }
            Figure figure = getRandomFigureWhichCanMove(player);
            Position position = null;
            if (figure != null) {
                position = getPositionToMove(player, figure, opponent);
            }
            if (position == null) {
                System.out.printf("%s king is checkmated, %s team won", player.team(), opponent.team());
                gameInProcess = false;
                lastPlayer = player;
                canMove.signal();
                lock.unlock();
                return;
            }
            makeMove(player, opponent, figure, position);
            promotePawns(player);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lastPlayer = player;
            if (gameInProcess) {
                canMove.signal();
                lock.unlock();
            }
        }
    }

    private Position getPositionToMove(Player player, Figure figure, Player opponent) {
        Position position;
        if (figure.getClass().equals(King.class)) {
            position = getKingMoves(
                    figure.findPossibleMoves(board),
                    getPossibleMovesByFigures(board, opponent.figures())
            );
            if (position == null) {
                King king = (King) figure;
                player.figures().removeIf(element -> element.getClass().equals(King.class));
                Figure goalFigure = getRandomFigureWhichCanMove(player);
                if (goalFigure != null) {
                    position = getRandomMoveOfFigure(goalFigure);
                }
                player.figures().add(king);
            }
        } else {
            position = getRandomMoveOfFigure(figure);
        }
        return position;
    }

    private boolean onlyKingsOnBoard(Player player, Player opponent) {
        if (player.figures().size() == 1 && opponent.figures().size() == 1) {
            int lot = Math.abs(ThreadLocalRandom.current().nextInt() % 2);
            System.out.printf(
                    "%s team gave up, %s team won!\n",
                    players[lot].team(),
                    players[Math.abs(lot - 1)].team()
            );
            return true;
        }
        return false;
    }

    private KingStatus getKingStatus(Player player, Player opponent) {
        Map<Figure, List<Position>> opponentPossibleMovesByFigures = getPossibleMovesByFigures(board, opponent.figures());
        King king = (King) player.figures()
                .stream()
                .filter(figure -> figure.getClass().equals(King.class))
                .findFirst()
                .orElseThrow(NullPointerException::new);
        if (!king.isCheckmated(opponentPossibleMovesByFigures)) {
            return KingStatus.SAFE;
        }
        Map<Figure, List<Position>> playerPossibleMovesByFigures = getPossibleMovesByFigures(board, player.figures());
        List<Position> possibleMoves = playerPossibleMovesByFigures.get(king);
        possibleMoves.add(king.getPosition());
        if (king.opponentCoversAllMoves(opponentPossibleMovesByFigures, possibleMoves)) {
            Map<Figure, List<Position>> foundProtectors = findWhoCanProtectTheKing(
                    king,
                    playerPossibleMovesByFigures,
                    opponentPossibleMovesByFigures
            );
            if (foundProtectors.isEmpty()) {
                return KingStatus.CHECKMATED;
            } else {
                protectTheKing(foundProtectors, player, opponent);
                return KingStatus.PROTECTED;
            }
        } else {
            return KingStatus.SAFE;
        }
    }

    private void protectTheKing(Map<Figure, List<Position>> foundProtectors, Player player, Player opponent) {
        Map.Entry<Figure, List<Position>> chosenProtector = foundProtectors.entrySet()
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    Collections.shuffle(collected);
                    return collected;
                })).getFirst();
        Position wayToProtect = chosenProtector.getValue()
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    Collections.shuffle(collected);
                    return collected;
                })).getFirst();
        makeMove(player, opponent, chosenProtector.getKey(), wayToProtect);
    }

    private Map<Figure, List<Position>> findWhoCanProtectTheKing(
            King king,
            Map<Figure, List<Position>> playerPossibleMovesByFigures,
            Map<Figure, List<Position>> opponentPossibleMovesByFigures
    ) {
        Map<Figure, List<Position>> priorityGoals = opponentPossibleMovesByFigures.entrySet()
                .stream()
                .filter(figureListEntry -> figureListEntry.getValue().contains(king.getPosition()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<Position> waysToProtect = findWaysToProtect(king, priorityGoals);
        Map<Figure, List<Position>> protectors = playerPossibleMovesByFigures.entrySet()
                .stream()
                .filter(figureListEntry -> figureListEntry.getValue()
                        .stream()
                        .anyMatch(waysToProtect::contains))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        protectors.forEach(((figure, positions) -> positions.removeIf(position -> !waysToProtect.contains(position))));
        return protectors;
    }

    private List<Position> findWaysToProtect(King king, Map<Figure, List<Position>> priorityGoals) {
        List<Position> waysToProtect = new ArrayList<>();
        priorityGoals.forEach((figure, positions) -> {
            if (figure.getPosition().x() == king.getPosition().x()) {
                waysToProtect.addAll(
                        positions.stream()
                                .filter(position -> position.x() == king.getPosition().x()
                                        && position.y() != king.getPosition().y()
                                        && position.y() >= Integer.min(king.getPosition().y(), figure.getPosition().y())
                                        && position.y() <= Integer.max(king.getPosition().y(), figure.getPosition().y())
                                ).toList()
                );
            } else if (figure.getPosition().y() == king.getPosition().y()) {
                waysToProtect.addAll(
                        positions.stream()
                                .filter(position -> position.y() == king.getPosition().y()
                                        && position.x() != king.getPosition().x()
                                        && position.x() >= Integer.min(king.getPosition().x(), figure.getPosition().x())
                                        && position.x() <= Integer.max(king.getPosition().x(), figure.getPosition().x())
                                ).toList()
                );
            } else if (figure.getClass().equals(Knight.class)) {
                waysToProtect.add(figure.getPosition());
            } else {
                waysToProtect.addAll(getDiagonalWay(
                        Integer.min(king.getPosition().x(), figure.getPosition().x()),
                        Integer.min(king.getPosition().y(), figure.getPosition().y()),
                        Integer.max(king.getPosition().x(), figure.getPosition().x())
                ));
                waysToProtect.removeIf(position -> position.equals(king.getPosition()));
                waysToProtect.add(figure.getPosition());
            }
        });
        return waysToProtect;
    }

    private List<Position> getDiagonalWay(int x, int y, int maxX) {
        List<Position> diagonalWay = new ArrayList<>();
        for (; x <= maxX; x++, y++) {
            diagonalWay.add(new Position(x, y));
        }
        return diagonalWay;
    }

    private Position getKingMoves(
            List<Position> possibleMoves,
            Map<Figure, List<Position>> opponentPossibleMovesByFigures
    ) {
        possibleMoves.removeAll(
                opponentPossibleMovesByFigures.entrySet()
                        .stream()
                        .flatMap(figureListEntry -> figureListEntry.getValue().stream())
                        .collect(Collectors.toSet())
        );
        Collections.shuffle(possibleMoves);
        return possibleMoves.isEmpty()
                ? null
                : possibleMoves.stream()
                .filter(Objects::nonNull)
                .toList()
                .getFirst();
    }

    private Map<Figure, List<Position>> getPossibleMovesByFigures(Board board, Set<Figure> playerFigures) {
        return playerFigures.stream()
                .collect(Collectors.toMap(figure -> figure, figure -> figure.findPossibleMoves(board)));
    }

    private Figure getRandomFigureWhichCanMove(Player player) {
        List<Figure> figures = player.figures()
                .stream()
                .filter(element -> !element.findPossibleMoves(board).isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    Collections.shuffle(collected);
                    return collected;
                }));
        return figures.isEmpty() ? null : figures.getFirst();
    }

    private Position getRandomMoveOfFigure(Figure figure) {
        List<Position> possibleMoves = figure.findPossibleMoves(board)
                .stream()
                .filter(position -> board.getCells()[position.x()][position.y()] == null
                        || !board.getCells()[position.x()][position.y()]
                        .getClass()
                        .equals(King.class)
                ).collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    Collections.shuffle(collected);
                    return collected;
                }));
        return possibleMoves.isEmpty() ? null : possibleMoves.getFirst();
    }

    private void makeMove(Player player, Player opponent, Figure figure, Position position) {
        Figure goalFigure = board.getCells()[position.x()][position.y()];
        switch (getCellStatus(goalFigure, player.team())) {
            case EMPTY -> board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = null;
            case OTHER_TEAM -> {
                board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = null;
                opponent.figures().removeIf(element -> element.equals(goalFigure));
            }
            case SAME_TEAM -> {
                if (goalFigure.getClass().equals(Rook.class) && figure.getClass().equals(King.class)) {
                    castleKing((King) figure, (Rook) goalFigure);
                    return;
                }
            }
        }
        board.getCells()[position.x()][position.y()] = figure;
        printPlayerMove(player, figure, goalFigure, position);
        figure.setPosition(position);
        if (figure instanceof FigureWithFirstMove && ((FigureWithFirstMove) figure).isFirstMove()) {
            ((FigureWithFirstMove) figure).setFirstMove(false);
        }
    }

    private void castleKing(King king, Rook rook) {
        BinaryOperator<Integer> operator = king.getPosition().x() < rook.getPosition().x()
                ? Integer::sum
                : (a, b) -> a - b;
        int cellsCount = rook.getPosition().x();
        board.getCells()[operator.apply(king.getPosition().x(), 2)][king.getPosition().y()] = king;
        board.getCells()[operator.apply(king.getPosition().x(), 1)][king.getPosition().y()] = rook;
        king.setPosition(new Position(operator.apply(king.getPosition().x(), 2), king.getPosition().y()));
        rook.setPosition(new Position(operator.apply(king.getPosition().x(), 1), king.getPosition().y()));
        king.setFirstMove(false);
        rook.setFirstMove(false);
        cellsCount = Integer.max(cellsCount, rook.getPosition().x()) - Integer.min(cellsCount, rook.getPosition().x());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.repeat("0-", cellsCount);
        if (!stringBuilder.isEmpty()) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        System.out.printf(
                "%s team made castling: %s",
                king.getTeam(),
                stringBuilder
        );
    }

    private static void printPlayerMove(Player player, Figure figure, Figure goalFigure, Position position) {
        System.out.printf(
                "%s team move [%s%c%d - %s%c%d]\n",
                player.team().equals(Team.WHITE) ? Team.WHITE : Team.BLACK,
                figure.getName(),
                figure.getPosition().x() + 'a',
                figure.getPosition().y() + 1,
                goalFigure != null ? figure.getName() + "x" : figure.getName(),
                position.x() + 'a',
                position.y() + 1
        );
    }

    private void promotePawns(Player player) {
        player.figures().addAll(
                player.figures()
                        .stream()
                        .filter(figure -> figure.getClass().equals(Pawn.class) && ((Pawn) figure).canPromote())
                        .map(figure -> ((Pawn) figure).promote())
                        .peek(figure -> System.out.printf(
                                "%s pawn %c%d promoted to %s%c%d\n",
                                figure.getTeam(),
                                figure.getPosition().x() + 'a',
                                figure.getPosition().y() + 1,
                                figure.getName(),
                                figure.getPosition().x() + 'a',
                                figure.getPosition().y() + 1
                        ))
                        .collect(Collectors.toSet())
        );
        player.figures().removeIf(figure -> figure.getClass().equals(Pawn.class) && ((Pawn) figure).canPromote());
    }
}
