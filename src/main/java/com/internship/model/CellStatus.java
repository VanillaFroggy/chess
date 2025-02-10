package com.internship.model;

import com.internship.model.figure.Figure;
import com.internship.model.figure.impl.King;

public enum CellStatus {
    EMPTY, SAME_TEAM, OTHER_TEAM, OPPONENT_KING;

    public static CellStatus getCellStatus(Figure figure, Team team) {
        return (figure == null)
                ? (CellStatus.EMPTY)
                : ((figure.getTeam().equals(team)) ? (CellStatus.SAME_TEAM)
                : ((figure.getClass().equals(King.class)) ? (CellStatus.OPPONENT_KING) : (CellStatus.OTHER_TEAM)));
    }
}
