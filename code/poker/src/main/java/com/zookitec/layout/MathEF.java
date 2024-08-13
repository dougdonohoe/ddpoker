/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/MathEF.java,v $
 * $Revision: 1.2 $
 * $Date: 2003/04/27 21:25:15 $
 *
 * Copyright (c) 2002 Zooki Technologies. All rights reserved.
 *
 * http://www.zookitec.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 *  Bug fixes, suggestions and comments should be sent to: alex@zookitec.com
 */
 
package com.zookitec.layout;


import java.awt.*;
import java.util.*;
import java.lang.ref.*;

/**
 * An expression factory used to create expressions for common mathematical operations.
 */
public class MathEF {

    private static final int ADD = 0;
    private static final int SUB = 1;
    private static final int MUL = 2;
    private static final int DIV = 3;
    private static final int MIN = 4;
    private static final int MAX = 5;
    private static final int SUM = 6;

    private static WeakHashMap cache = new WeakHashMap();

    /**
     * A constant Expression whose value is 0.0.
     */
    public static final Expression ZERO = new Expression(0.0);

    private MathEF() {}


    private static Expression getCacheExpression(Expression expr) {
        WeakReference exprRef = (WeakReference)cache.get(expr);
        if (exprRef == null) {
            cache.put(expr, new WeakReference(expr));
        } else {
            expr = (Expression)exprRef.get();
        }
        return expr;
    }

    /**
     *
     * Creates a constant expression equal to the specified value.
     *
     * @param value the value of the constant expression.
     * @return an expression whose value is as specified.
     */
    public static Expression constant(double value) {
        return getCacheExpression(new Expression(value));
    }


    /**
     * Creates an expression for expr1 + expr2.
     *
     * @param expr1 an expression
     * @param expr2 an expression
     *
     * @return an expression for expr1 + expr2
     */
    public static Expression add(Expression expr1, Expression expr2) {
        return getCacheExpression(new MathExpression(ADD, new Expression[] {expr1, expr2}));
    }

    /**
     * Creates an expression for expr1 + expr2Value.
     * This is equivalent to <code>add(expr1, MathEF.constant(expr2Value))</code>.
     *
     * @param expr1 an expression
     * @param expr2Value a value
     *
     * @return an expression for expr1 + expr2Value
     *
     */
    public static Expression add(Expression expr1, double expr2Value) {
        return add(expr1, constant(expr2Value));
    }


    /**
     * Creates an expression for expr1 - expr2.
     *
     * @param expr1 an expression
     * @param expr2 an expression
     *
     * @return an expression for expr1 - expr2
     */
    public static Expression subtract(Expression expr1, Expression expr2) {
        return getCacheExpression(new MathExpression(SUB, new Expression[] {expr1, expr2}));
    }


    /**
     * Creates an expression for expr1 - expr2Value.
     * This is equivalent to <code>subtract(expr1, MathEF.constant(expr2Value))</code>.
     *
     * @param expr1 an expression
     * @param expr2 a value
     *
     * @return an expression for expr1 - expr2Value
     */
    public static Expression subtract(Expression expr1, double expr2Value) {
        return subtract(expr1, constant(expr2Value));
    }


    /**
     * Creates an expression for expr1 * expr2.
     *
     * @param expr1 an expression
     * @param expr2 an expression
     *
     * @return an expression for expr1 * expr2
     */
    public static Expression multiply(Expression expr1, Expression expr2) {
        return getCacheExpression(new MathExpression(MUL, new Expression[] {expr1, expr2}));
    }

    /**
     * Creates an expression for expr1 * expr2Value.
     * This is equivalent to <code>multiply(expr1, MathEF.constant(expr2Value))</code>.
     *
     * @param expr1 an expression
     * @param expr2Value a value
     *
     * @return an expression for expr1 * expr2Value
     */
    public static Expression multiply(Expression expr1, double expr2Value) {
        return multiply(expr1, constant(expr2Value));
    }


    /**
     * Creates an expression for expr1 / expr2.
     *
     * @param expr1 an expression
     * @param expr2 an expression
     *
     * @return an expression for expr1 / expr2
     */
    public static Expression divide(Expression expr1, Expression expr2) {
        return getCacheExpression(new MathExpression(DIV, new Expression[] {expr1, expr2}));
    }

    /**
     * Creates an expression for expr1 / expr2Value.
     * This is equivalent to <code>divide(expr1, MathEF.constant(expr2Value))</code>.
     *
     * @param expr1 an expression
     * @param expr2Value a value
     *
     * @return an expression for expr1 / expr2Value
     */
    public static Expression divide(Expression expr1, double expr2Value) {
        return divide(expr1, constant(expr2Value));
    }

    /**
     * Creates an expression equal in value to the operand expression with the largest value.
     *
     * @param operands the operands of this expression
     *
     * @return an expression equal in value to the operand expression with the largest value.
     */
    public static Expression max(Expression [] operands) {
        return getCacheExpression(new MathExpression(MAX, operands));
    }

    /**
     * Equivalent to <code>max(Expression [] operands)</code> where operands[0] = expr1
     * and operands[1] = expr2. This method is provided for convenience when the maximum
     * of just two expressions is required.
     *
     * @param expr1 an expression
     * @param expr2 an expression
     *
     * @see #max(Expression [])
     */
    public static Expression max(Expression expr1, Expression expr2) {
        return max(new Expression[]{expr1, expr2});
    }

    /**
     * Creates an expression equal in value to the operand expression with the smallest value.
     *
     * @param operands the operands of this expression
     *
     * @return an expression equal in value to the operand expression with the smallest value.
     */
    public static Expression min(Expression [] operands) {
        return getCacheExpression(new MathExpression(MIN, operands));
    }

    /**
     * Equivalent to <code>min(Expression [] operands)</code> where operands[0] = expr1
     * and operands[1] = expr2. This method is provided for convenience when the minimum
     * of just two expressions is required.
     *
     * @param expr1 an expression
     * @param expr2 an expression
     *
     * @see #min(Expression [])
     */
    public static Expression min(Expression expr1, Expression expr2) {
        return min(new Expression[]{expr1, expr2});
    }

    /**
     * Creates an expression for the sum of the specified operand expressions.
     *
     * @param operands the operands of this expression
     *
     * @return an expression for the sum of the specified operand expressions.
     */
    public static Expression sum(Expression [] operands) {
        return getCacheExpression(new MathExpression(SUM, operands));
    }


    public static Expression bound(Expression min, Expression value, Expression max) {
        if (min != null && max != null) {
            return max(min, min(value, max));
        } else if (min == null && max != null) {
            return min(value, max);
        } else if (min != null && max == null) {
            return max(value, min);
        } else {
            return value;
        }
    }


    /**
     * An expression whose value is calculated from one or more operand expressions.
     */
    static class MathExpression extends Expression {

        private Expression [] operands;
        private int op;

        /**
         * Construct a new multi-operand expression.
         *
         * @param expressions An array of expressions on which the value of this expression depends; this cannot be null.
         */
        public MathExpression(int op, Expression [] operands) {
            if (operands == null) {
                throw new NullPointerException("operands array must not be null");
            }
            this.op = op;
            this.operands = operands;
        }


        /**
         * Invalidates this expression and each of its operand expressions.
         */
        public void invalidate() {
            super.invalidate();
            for (int i = 0; i < operands.length; i++) {
                operands[i].invalidate();
            }
        }


        protected double computeValue(ExplicitLayout layout) {
            double value;
            switch (op) {
                case ADD:
                    return operands[0].getValue(layout) + operands[1].getValue(layout);

                case SUB:
                    return operands[0].getValue(layout) - operands[1].getValue(layout);


                case MUL:
                    return operands[0].getValue(layout) * operands[1].getValue(layout);


                case DIV:
                    return operands[0].getValue(layout) / operands[1].getValue(layout);

                case MAX:
                    if (operands.length == 0) {
                        return 0;
                    } else {
                        double max = Double.MIN_VALUE;
                        for (int i = 0; i < operands.length; i++) {
                            value = operands[i].getValue(layout);
                            if (value > max) {
                                max = value;
                            }
                        }
                        return max;
                    }
                case MIN:
                    if (operands.length == 0) {
                        return 0;
                    } else {
                        double min = Double.MAX_VALUE;
                        for (int i = 0; i < operands.length; i++) {
                            value = operands[i].getValue(layout);
                            if (value < min) {
                                min = value;
                            }
                        }
                        return min;
                    }
                case SUM:
                    double sum = 0;
                    for (int i = 0; i < operands.length; i++) {
                        sum += operands[i].getValue(layout);
                    }
                    return sum;

                default:
                    return 0;

            }
        }

        public boolean equals(Object o) {
            if (o instanceof MathExpression) {
                MathExpression expr = (MathExpression)o;
                if (op == expr.op && operands.length == expr.operands.length) {
                    for (int i = 0; i < operands.length; i++) {
                        if (operands[i] != expr.operands[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }


        public int hashCode() {
            int hash = 1000 * op;
            for (int i = 0; i < operands.length; i++) {
                hash += operands[i].hashCode();
            }
            return hash;
        }
    }

}
