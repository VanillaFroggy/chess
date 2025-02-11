package com.internship.model;

import com.internship.model.figure.Figure;
import com.internship.model.figure.impl.King;

public enum CellStatus {
    EMPTY, SAME_TEAM, OTHER_TEAM, OPPONENT_KING;

    public static CellStatus getCellStatus(Figure figure, Team team) {
        if (figure == null) {
            return CellStatus.EMPTY;
        }
        if (figure.getTeam().equals(team)) {
            return CellStatus.SAME_TEAM;
        }
        if (figure.getClass().equals(King.class)) {
            return CellStatus.OPPONENT_KING;
        }
        return CellStatus.OTHER_TEAM;
    }
}
