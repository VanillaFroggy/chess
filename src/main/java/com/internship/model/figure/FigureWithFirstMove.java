package com.internship.model.figure;

import com.internship.model.Team;

public abstract class FigureWithFirstMove extends Figure {
    protected boolean firstMove;

    public FigureWithFirstMove(Position position, Team team) {
        super(position, team);
        firstMove = true;
    }

    public boolean isFirstMove() {
        return firstMove;
    }

    public void setFirstMove(boolean firstMove) {
        this.firstMove = firstMove;
    }
}
