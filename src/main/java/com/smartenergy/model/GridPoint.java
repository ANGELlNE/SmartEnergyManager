package com.smartenergy.model;

import java.io.Serializable;

public class GridPoint implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int col, row;

    public GridPoint(int col, int row) {
        this.col = col;
        this.row = row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridPoint that)) return false;
        return col == that.col && row == that.row;
    }

    @Override
    public int hashCode() { return 31 * col + row; }

    @Override
    public String toString() { return "(" + col + "," + row + ")"; }
}
