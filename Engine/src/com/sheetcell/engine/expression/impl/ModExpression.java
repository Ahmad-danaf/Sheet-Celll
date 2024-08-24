package com.sheetcell.engine.expression.impl;

import com.sheetcell.engine.cell.Cell;
import com.sheetcell.engine.cell.CellType;
import com.sheetcell.engine.cell.EffectiveValue;
import com.sheetcell.engine.expression.api.BinaryExpression;
import com.sheetcell.engine.expression.api.Expression;
import com.sheetcell.engine.sheet.api.SheetReadActions;

public class ModExpression implements BinaryExpression {
    private final Expression left;
    private final Expression right;

    public ModExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Expression getLeft() {
        return left;
    }

    @Override
    public Expression getRight() {
        return right;
    }

    @Override
    public EffectiveValue eval(SheetReadActions sheet, Cell callingCell) {
        // Evaluate the expressions
        EffectiveValue leftValue = left.eval(sheet, callingCell);
        EffectiveValue rightValue = right.eval(sheet, callingCell);

        // Attempt to cast the right value (divisor) to Double
        Double divisor = rightValue.castValueTo(Double.class);
        if (divisor == null) {
            throw new IllegalArgumentException("Error: The divisor provided to the MOD function is not numeric. " +
                    "Please ensure that the divisor is a valid numeric value.");
        }

        // Check for division by zero
        if (divisor == 0.0) {
            return new EffectiveValue(CellType.NUMERIC, Double.NaN); // Return NaN for modulo by zero
        }

        // Attempt to cast the left value (dividend) to Double
        Double dividend = leftValue.castValueTo(Double.class);
        if (dividend == null) {
            throw new IllegalArgumentException("Error: The dividend provided to the MOD function is not numeric. " +
                    "Please ensure that the dividend is a valid numeric value.");
        }

        // Perform the modulo operation
        double result = dividend % divisor;

        // Return the result as a new EffectiveValue
        return new EffectiveValue(CellType.NUMERIC, result);
    }


    @Override
    public CellType getFunctionResultType() {
        return CellType.NUMERIC;
    }
}

