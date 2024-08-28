package com.sheetcell.engine.expression.impl;

import com.sheetcell.engine.cell.Cell;
import com.sheetcell.engine.cell.CellType;
import com.sheetcell.engine.cell.EffectiveValue;
import com.sheetcell.engine.expression.api.BinaryExpression;
import com.sheetcell.engine.expression.api.Expression;
import com.sheetcell.engine.sheet.api.SheetReadActions;

public class PowExpression implements BinaryExpression {
    private final Expression base;
    private final Expression exponent;

    public PowExpression(Expression base, Expression exponent) {
        this.base = base;
        this.exponent = exponent;
    }

    @Override
    public Expression getLeft() {
        return base;
    }

    @Override
    public Expression getRight() {
        return exponent;
    }

    @Override
    public EffectiveValue eval(SheetReadActions sheet, Cell callingCell) {
        // Evaluate the expressions
        EffectiveValue baseValue = base.eval(sheet, callingCell);
        EffectiveValue exponentValue = exponent.eval(sheet, callingCell);

        // Attempt to cast the base and exponent to Double
        Double baseNumeric = baseValue.castValueTo(Double.class);
        Double exponentNumeric = exponentValue.castValueTo(Double.class);

        // If either value is null, it means the cast failed (not a numeric type)
        if (baseNumeric == null || exponentNumeric == null) {
            String cellCoordinates = (callingCell != null && callingCell.getCoordinate() != null)
                    ? callingCell.getCoordinate().toString()
                    : "unknown";
            if ("unknown".equals(cellCoordinates)) {
                throw new IllegalArgumentException("Error: The arguments provided to the POW function are not numeric.\n" +
                        "Please ensure that both arguments are valid numeric values.");
            } else {
                throw new IllegalArgumentException("Error: The arguments provided to the POW function are not numeric.\n" +
                        "Please ensure that both arguments are valid numeric values. Cell: " + cellCoordinates);
            }
        }

        // Perform the power operation
        double result = Math.pow(baseNumeric, exponentNumeric);

        // Return the result as a new EffectiveValue
        return new EffectiveValue(CellType.NUMERIC, result);
    }


    @Override
    public CellType getFunctionResultType() {
        return CellType.NUMERIC;
    }
}

