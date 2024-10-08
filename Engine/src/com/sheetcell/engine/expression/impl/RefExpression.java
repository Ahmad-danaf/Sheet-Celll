package com.sheetcell.engine.expression.impl;

import com.sheetcell.engine.cell.Cell;
import com.sheetcell.engine.cell.CellType;
import com.sheetcell.engine.cell.EffectiveValue;
import com.sheetcell.engine.coordinate.Coordinate;
import com.sheetcell.engine.expression.api.Expression;
import com.sheetcell.engine.expression.api.ReferenceExpression;
import com.sheetcell.engine.sheet.api.SheetReadActions;

public class RefExpression implements ReferenceExpression {

    private final Coordinate coordinate;

    public RefExpression(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public EffectiveValue eval(SheetReadActions sheet, Cell callingCell) {
        if (outOfBounds(coordinate, sheet.getMaxRows(), sheet.getMaxColumns())) {
            String message = "Reference '" + coordinate + "' is out of bounds" +
                    (callingCell != null ? " in cell " + callingCell.getCoordinate() : "");
            throw new IllegalArgumentException(message);
        }
        // Retrieve the cell from the sheet
        Cell referencedCell  = sheet.getCell(coordinate.getRow(), coordinate.getColumn());

        // Check if the cell is null (i.e., not found) or has no effective value
        if (referencedCell  == null || referencedCell.getEffectiveValue() == null) {
            return new EffectiveValue(CellType.UNKNOWN, null);
        }

        // Add this cell as a dependency to the referenced cell
        if (callingCell != null) {
            referencedCell.addInfluencedCell(callingCell);
            callingCell.addDependency(referencedCell);
        }

        // Return the effective value of the cell
        return referencedCell.getEffectiveValue();
    }

    @Override
    public CellType getFunctionResultType() {
        return CellType.UNKNOWN;
    }

    private boolean outOfBounds(Coordinate coordinate, int maxRow, int maxColumn) {
        return coordinate.getRow() < 0 || coordinate.getColumn() < 0 ||
                coordinate.getRow() >= maxRow || coordinate.getColumn() >= maxColumn;
    }
}
