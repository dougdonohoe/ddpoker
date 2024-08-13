/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/Expression.java,v $
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
import java.io.*;

/**
 * This class represents a constant expression and is the superclass of all variable expressions used by ExplicitLayout.
 * Subclasses should override the computeValue method to provide a variable value for the expression.
 *
 */
public class Expression implements Serializable {

  /*  static {
        a.go();
    }*/


    /**
     * Rounding mode to assert that no rounding is performed on the value of this expression.
     *
     */
    public static final byte ROUND_NONE = 1;

    /**
     * Rounding mode to round the value of this expression towards the nearest integer neighbour unless both
     * neighbours are equidistant, in which case round up.
     */
    public static final byte ROUND_NEAREST = 2;

    /**
     * Rounding mode to round the value of this expression towards positive infinity.
     */
    public static final byte ROUND_CEILING = 3;

    /**
     * Rounding mode to round the value of this expression towards negative infinity.
     */
    public static final byte ROUND_FLOOR = 4;

    /**
     * Flag indicating whether the value of this expression is currently valid.
     */
    private transient boolean valid;
    private transient double value;

    private byte roundingMode = ROUND_NONE;

    /**
     * Constructs an invalid expression. This constructor is intended for use by subclasses.
     * The default rounding mode is ROUND_NONE.
     */
    protected Expression() {
        valid = false;
    }

    /**
     * Constructs a constant expression. This can be used when the value of a variable
     * is known at design time.
     * @param value the value of the expression.
     */
    public Expression(double value) {
        this.value = value;
        valid = true;
    }


    /**
     * Gets the value of this expression. If the value is not already known,
     * it is set by calling computeValue. The value may be rounded to an integer depending
     * on the rounding mode.
     *
     * @param layout the ExplicitLayout manager
     *
     * @return the value of this expression
     */
    public double getValue(ExplicitLayout layout) {
        if (!valid) {
            value = computeValue(layout);
            switch (roundingMode) {
                case ROUND_NEAREST :
                    value = Math.round(value);
                break;
                case ROUND_FLOOR :
                    value = Math.floor(value);
                break;
                case ROUND_CEILING :
                    value = Math.ceil(value);
                break;
            }
            valid = true;
        }
        return value;
    }

    /**
     * Sets the rounding mode for the value returned by <code>getValue</code>.
     *
     * @param roundingMode the rounding mode: ROUND_NONE, ROUND_NEAREST, ROUND_FLOOR, ROUND_CEILING.
     */
    public void setRoundingMode(byte roundingMode) {
        this.roundingMode = roundingMode;
        invalidate();
    }

    /**
     * Gets the rounding mode for the value of this expression.
     *
     * @return the rounding mode: ROUND_NONE, ROUND_NEAREST, ROUND_FLOOR, ROUND_CEILING.
     */
    public byte getRoundingMode() {
        return roundingMode;
    }

    /**
     * Forces the value of this expression to be recalculated by calling computeValue
     * next time getValue is called. This only applies to subclasses as the value of
     * this expression is constant.
     */
    public void invalidate() {
        if (getClass() != Expression.class) {
            //set valid false for subclasses only - this is constant
            valid = false;
        }
    }



    /**
     * Computes the value of this expression. This implementation returns the default value 0.
     * For variable expressions, this is typically overridden by a subclass.
     *
     * @param layout the ExplicitLayout manager
     *
     * @return the value of this expression
     */
    protected double computeValue(ExplicitLayout layout) {
        return value;
    }


    /**
     * Equivalent to MathEF.add(this, other)
     */
    public Expression add(Expression other) {
        return MathEF.add(this, other);
    }

    /**
     * Equivalent to MathEF.add(this, other)
     */
    public Expression add(double other) {
        return MathEF.add(this, other);
    }


    /**
     * Equivalent to MathEF.subtract(this, other)
     */
    public Expression subtract(Expression other) {
        return MathEF.subtract(this, other);
    }

    /**
     * Equivalent to MathEF.subtract(this, other)
     */
    public Expression subtract(double other) {
        return MathEF.subtract(this, other);
    }



    /**
     * Equivalent to MathEF.multiply(this, other)
     */
    public Expression multiply(Expression other) {
        return MathEF.multiply(this, other);
    }

   /**
     * Equivalent to MathEF.multiply(this, other)
     */
    public Expression multiply(double other) {
        return MathEF.multiply(this, other);
    }

    /**
     * Equivalent to MathEF.divide(this, other)
     */
    public Expression divide(Expression other) {
        return MathEF.divide(this, other);
    }

    /**
     * Equivalent to MathEF.divide(this, other)
     */
    public Expression divide(double other) {
        return MathEF.divide(this, other);
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && getClass() == Expression.class && getClass().equals(o.getClass())) {
            return value == ((Expression)o).value;
        }
        return false;
    }

    public int hashCode() {
        return (getClass() == Expression.class) ? (int)(value * 100) : super.hashCode();
    }

}
