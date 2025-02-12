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
    private int moveNumber = 1;

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
            KingStatus kingStatus = getKingStatus(player);
            if (onlyKingsOnBoard(player) || kingStatus.equals(KingStatus.CHECKMATED)) {
                gameInProcess = false;
                lastPlayer = player;
                canMove.signal();
                lock.unlock();
                return;
            } else if (kingStatus.equals(KingStatus.PROTECTED) || kingStatus.equals(KingStatus.MOVED)) {
                return;
            }
            Figure figure = getRandomFigureWhichCanMove(player);
            Position position = null;
            if (figure != null) {
                position = getPositionToMove(player, figure);
            }
            if (position == null) {
                printGameResult(true);
                gameInProcess = false;
                lastPlayer = player;
                canMove.signal();
                lock.unlock();
                return;
            }
            makeMove(player, figure, position);
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

    private Position getPositionToMove(Player player, Figure figure) {
        Position position;
        if (figure.getClass().equals(King.class)) {
            position = getKingMoves(
                    figure.findPossibleMoves(board),
                    getPossibleMovesByFigures(board, lastPlayer.figures())
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

    private boolean onlyKingsOnBoard(Player player) {
        if (player.figures().size() == 1 && lastPlayer.figures().size() == 1) {
            printGameResult(true);
            return true;
        }
        return false;
    }

    private KingStatus getKingStatus(Player player) {
        Map<Figure, List<Position>> opponentPossibleMovesByFigures = getPossibleMovesByFigures(
                board,
                lastPlayer.figures()
        );
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
                printGameResult(false);
                return KingStatus.CHECKMATED;
            } else {
                protectTheKing(foundProtectors, player);
                return KingStatus.PROTECTED;
            }
        } else if (king.isCheckmated(opponentPossibleMovesByFigures)) {
            possibleMoves.removeAll(
                    opponentPossibleMovesByFigures.entrySet()
                            .stream()
                            .flatMap(figureListEntry -> figureListEntry.getValue().stream())
                            .collect(Collectors.toSet())
            );
            Collections.shuffle(possibleMoves);
            makeMove(player, king, possibleMoves.getFirst());
            return KingStatus.MOVED;
        } else {
            return KingStatus.SAFE;
        }
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

    private void protectTheKing(Map<Figure, List<Position>> foundProtectors, Player player) {
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
        makeMove(player, chosenProtector.getKey(), wayToProtect);
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

    private void makeMove(Player player, Figure figure, Position position) {
        Figure goalFigure = board.getCells()[position.x()][position.y()];
        switch (getCellStatus(goalFigure, player.team())) {
            case EMPTY -> {
                if (figure.getClass().equals(Pawn.class) && goalFigure != null
                        && board.getCells()[figure.getPosition().x()][goalFigure.getPosition().y()].getClass()
                        .equals(Pawn.class)
                        && ((Pawn) board.getCells()[figure.getPosition().x()][goalFigure.getPosition().y()])
                        .isReadyForCaptureByOpponentPawn()) {
                    Pawn opponentPawn = (Pawn) board.getCells()[figure.getPosition().x()][goalFigure.getPosition().y()];
                    lastPlayer.figures().removeIf(element -> element.equals(opponentPawn));
                    board.getCells()[figure.getPosition().x()][goalFigure.getPosition().y()] = null;
                }
                board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = null;
            }
            case OTHER_TEAM -> {
                board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = null;
                lastPlayer.figures().removeIf(element -> element.equals(goalFigure));
            }
            case SAME_TEAM -> {
                if (goalFigure.getClass().equals(Rook.class) && figure.getClass().equals(King.class)) {
                    castleKing((King) figure, (Rook) goalFigure);
                    return;
                }
            }
        }
        board.getCells()[position.x()][position.y()] = figure;
        figure.setPosition(position);
        setPawnsUnreadyToCapture(player);
        changePawnStatus(figure);
        if (figure instanceof FigureWithFirstMove && ((FigureWithFirstMove) figure).isFirstMove()) {
            ((FigureWithFirstMove) figure).setFirstMove(false);
        }
        boolean wasPromoted = false;
        if (figure.getClass().equals(Pawn.class) && ((Pawn) figure).canPromote()) {
            figure = promotePawn(player, (Pawn) figure);
            wasPromoted = true;
        }
        printPlayerMove(player, figure, goalFigure, wasPromoted);
    }

    private void castleKing(King king, Rook rook) {
        BinaryOperator<Integer> operator = king.getPosition().x() < rook.getPosition().x()
                ? Integer::sum
                : (a, b) -> a - b;
        board.getCells()[operator.apply(king.getPosition().x(), 2)][king.getPosition().y()] = king;
        board.getCells()[operator.apply(king.getPosition().x(), 1)][king.getPosition().y()] = rook;
        king.setPosition(new Position(operator.apply(king.getPosition().x(), 2), king.getPosition().y()));
        rook.setPosition(new Position(operator.apply(king.getPosition().x(), 1), king.getPosition().y()));
        king.setFirstMove(false);
        rook.setFirstMove(false);
    }

    private void setPawnsUnreadyToCapture(Player player) {
        player.figures().forEach(element -> {
            if (element.getClass().equals(Pawn.class) && ((Pawn) element).isReadyForCaptureByOpponentPawn()) {
                ((Pawn) element).setReadyForCaptureByOpponentPawn(false);
            }
        });
    }

    private void changePawnStatus(Figure figure) {
        if (figure.getClass().equals(Pawn.class)) {
            Pawn pawn = (Pawn) figure;
            if (pawn.isFirstMove()
                    && !pawn.isReadyForCaptureByOpponentPawn()
                    && Math.abs(pawn.getPosition().y() - pawn.getLastPosition().y()) == 2) {
                pawn.setReadyForCaptureByOpponentPawn(true);
            }
        }
    }

    private Figure promotePawn(Player player, Pawn pawn) {
        player.figures().removeIf(figure -> figure.equals(pawn));
        Figure figure = pawn.promote();
        player.figures().add(figure);
        return figure;
    }

    private void printPlayerMove(Player player, Figure figure, Figure goalFigure, boolean wasPromoted) {
        if (player.team().equals(Team.WHITE)) {
            System.out.printf("%6d. %7s ", moveNumber++, getInfoToPrint(figure, goalFigure, player, wasPromoted));
        } else {
            System.out.printf("%7s\n", getInfoToPrint(figure, goalFigure, player, wasPromoted));
        }
    }

    private String getInfoToPrint(Figure figure, Figure goalFigure, Player player, boolean wasPromoted) {
        StringBuilder stringBuilder = new StringBuilder();
        if (figure.getClass().equals(King.class) && goalFigure != null
                && goalFigure.getClass().equals(Rook.class)
                && figure.getTeam().equals(goalFigure.getTeam())) {
            stringBuilder.repeat(
                    "O-",
                    Integer.max(goalFigure.getLastPosition().x(), goalFigure.getPosition().x())
                            - Integer.min(goalFigure.getLastPosition().x(), goalFigure.getPosition().x())
            );
            if (!stringBuilder.isEmpty()) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            return stringBuilder.toString();
        }
        if ((figure.getClass().equals(Pawn.class) || wasPromoted) && goalFigure != null) {
            stringBuilder.append(String.format("%c", figure.getLastPosition().x() + 'a'));
        } else if (!wasPromoted) {
            stringBuilder.append(figure.getName());
        }
        if (goalFigure != null) {
            stringBuilder.append("x");
        }
        stringBuilder.append(String.format("%c", figure.getPosition().x() + 'a'))
                .append(figure.getPosition().y() + 1);
        if (wasPromoted) {
            stringBuilder.append("=").append(figure.getName());
        }
        King opponentKing = (King) lastPlayer.figures()
                .stream()
                .filter(element -> element.getClass().equals(King.class))
                .findFirst()
                .orElseThrow(NullPointerException::new);
        if (figure.findPossibleMoves(board).contains(opponentKing.getPosition())) {
            if (timeToGiveUpForOpponentKing(opponentKing, player)) {
                stringBuilder.append("#");
            } else {
                stringBuilder.append("+");
            }
        }
        return stringBuilder.toString();
    }

    private boolean timeToGiveUpForOpponentKing(King opponentKing, Player player) {
        Map<Figure, List<Position>> playerPossibleMoves = getPossibleMovesByFigures(board, player.figures());
        Map<Figure, List<Position>> opponentPossibleMoves = getPossibleMovesByFigures(board, lastPlayer.figures());
        List<Position> kingPossibleMoves = opponentKing.findPossibleMoves(board);
        return opponentKing.opponentCoversAllMoves(playerPossibleMoves, kingPossibleMoves)
                && findWhoCanProtectTheKing(opponentKing, opponentPossibleMoves, playerPossibleMoves).isEmpty();
    }

    private void printGameResult(boolean isDraw) {
        System.out.print("\n\n\n\tGAME RESULT: ");
        if (isDraw) {
            System.out.print("1/2-1/2");
        } else {
            System.out.printf("%d-%d\n",
                    lastPlayer.team().equals(Team.WHITE) ? 1 : 0,
                    lastPlayer.team().equals(Team.WHITE) ? 0 : 1
            );
        }
        System.out.println("\n\n");
    }
}
