package com.internship.model.game;

import com.internship.model.Team;
import com.internship.model.figure.Figure;
import com.internship.model.figure.Position;
import com.internship.model.figure.impl.Pawn;
import com.internship.model.figure.impl.Rook;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    public void makeMove(Player player) {
        lock.lock();
        try {
            while (player.equals(lastPlayer)) {
                canMove.await();
            }
            if (!gameInProcess) return;
            promotePawns(player);
            Figure figure = player.figures()
                    .stream()
                    .filter(element -> !element.findPossibleMoves(board).isEmpty())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                        Collections.shuffle(collected);
                        return collected;
                    })).getFirst();
            Player opponent = player.equals(players[0]) ? players[1] : players[0];
            if (player.figures().size() == 1 && opponent.figures().size() == 1) {
                gameInProcess = false;
                int lot = Math.abs(ThreadLocalRandom.current().nextInt() % 2);
                System.out.printf(
                        "%s team gave up, %s team won!\n",
                        players[lot].team(),
                        players[Math.abs(lot - 1)].team()
                );
                lastPlayer = player;
                canMove.signal();
                lock.unlock();
                return;
            }
            Position position = figure.findPossibleMoves(board)
                    .stream()
                    .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                        Collections.shuffle(collected);
                        return collected;
                    })).getFirst();
            Figure goalFigure = board.getCells()[position.x()][position.y()];
            switch (getCellStatus(goalFigure, player.team())) {
                case EMPTY -> board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = null;
                case OTHER_TEAM -> {
                    board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = null;
                    opponent.figures().removeIf(element -> element.equals(goalFigure));
                }
                case SAME_TEAM -> {
                    if (goalFigure.getClass().equals(Rook.class)) {
                        board.getCells()[figure.getPosition().x()][figure.getPosition().y()] = goalFigure;
                        goalFigure.setPosition(figure.getPosition());
                    }
                }
            }
            board.getCells()[position.x()][position.y()] = figure;
            printPlayerMove(player, figure, goalFigure, position);
            figure.setPosition(position);
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

    private void promotePawns(Player player) {
        player.figures().addAll(
                player.figures()
                        .stream()
                        .filter(figure -> figure.getClass().equals(Pawn.class) && ((Pawn) figure).canPromote())
                        .map(figure -> ((Pawn) figure).promote())
                        .peek(figure -> System.out.printf(
                                "%s pawn on %c%d now is %s\n",
                                figure.getTeam(),
                                figure.getPosition().x() + 'a',
                                figure.getPosition().y() + 1,
                                figure.getName()
                        ))
                        .collect(Collectors.toSet())
        );
        player.figures().removeIf(figure -> figure.getClass().equals(Pawn.class) && ((Pawn) figure).canPromote());
    }

    private static void printPlayerMove(Player player, Figure figure, Figure opponentFigure, Position position) {
        System.out.printf(
                "%s team move [%s%c%d - %s%c%d]\n",
                player.team().equals(Team.WHITE) ? Team.WHITE : Team.BLACK,
                figure.getName(),
                figure.getPosition().x() + 'a',
                figure.getPosition().y() + 1,
                opponentFigure != null ? figure.getName() + "x" : figure.getName(),
                position.x() + 'a',
                position.y() + 1
        );
    }
}
