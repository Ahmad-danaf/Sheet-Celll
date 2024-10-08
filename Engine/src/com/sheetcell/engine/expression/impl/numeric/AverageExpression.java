package com.sheetcell.engine.expression.impl.numeric;

import com.sheetcell.engine.cell.Cell;
import com.sheetcell.engine.cell.CellType;
import com.sheetcell.engine.cell.EffectiveValue;
import com.sheetcell.engine.coordinate.Coordinate;
import com.sheetcell.engine.sheet.api.SheetReadActions;

import com.sheetcell.engine.expression.api.RangeExpression;

import java.util.Set;

public class AverageExpression implements RangeExpression {
    private final String rangeName;

    public AverageExpression(String rangeName) {
        this.rangeName = rangeName;
    }

    @Override
    public String getRange() {
        return rangeName;
    }

    @Override
    public EffectiveValue eval(SheetReadActions sheet, Cell callingCell) {
        if (sheet.isOnLoad()) {
            if (!sheet.isRangeExists(rangeName)) {
                String message = "Range " + rangeName + " does not exist";
                String cellId = callingCell != null ? "at cell " + callingCell.getCoordinate() : "";
                throw new IllegalArgumentException(message + " " + cellId);
            }
        }
        // Retrieve the coordinates of cells in the specified range
        Set<Coordinate> coordinates = sheet.getRangeCoordinates(rangeName);

        if (coordinates.isEmpty()) {
            return new EffectiveValue(CellType.NUMERIC, Double.NaN);
        }

        double sum = 0;
        int count = 0;
        for (Coordinate coordinate : coordinates) {
            Cell cell = sheet.getCell(coordinate.getRow(), coordinate.getColumn());
            if (cell != null && cell.getEffectiveValue() != null) {
                Double numericValue = cell.getEffectiveValue().castValueTo(Double.class);
                if (numericValue != null && !numericValue.isNaN()) {
                    sum += numericValue;
                    count++;
                }
            }
        }

        if (count == 0) {
            return new EffectiveValue(CellType.NUMERIC, Double.NaN);
        }
        sheet.markRangeAsUsed(rangeName);
        double average = sum / count;
        return new EffectiveValue(CellType.NUMERIC, average);
    }

    @Override
    public CellType getFunctionResultType() {
        return CellType.NUMERIC;
    }
}
