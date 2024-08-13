/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/ContainerEF.java,v $
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

/**
 * An expression factory used to create expressions for
 * attributes of the container being laid out.
 */
public class ContainerEF {

    private static final int TOP         =  0;
    private static final int BOTTOM      =  1;
    private static final int LEFT        =  2;
    private static final int RIGHT       =  3;
    private static final int WIDTH       =  4;
    private static final int HEIGHT      =  5;
    private static final int ATTRIBUTE_COUNT = 6;


    /**
     * array of maps from container to expression for each attribte
     */
    private static WeakHashMap [] cache = new WeakHashMap[ATTRIBUTE_COUNT];


    private ContainerEF() {}


    private static Expression getExpression(Container container, int attribute) {
        if (attribute < 0 || attribute >= ATTRIBUTE_COUNT) {
            throw new IllegalArgumentException("Illegal attribute " + attribute);
        }
        Expression expression;
        if (cache[attribute] == null) {
            cache[attribute] = new WeakHashMap();
            expression = new ContainerExpression(attribute);
            if (container != null) {
                cache[attribute].put(container, expression);
            }
        } else if ((expression = (Expression)cache[attribute].get(container)) == null) {
            expression = new ContainerExpression(attribute);
            if (container != null) {
                cache[attribute].put(container, expression);
            }
        }
        return expression;
    }




    /**
     * Creates an expression for the container left coordinate. This is the left inset of the container.
     *
     * @return expression for the container left coordinate
     */
    public static Expression left(Container container) {
        return getExpression(container, LEFT);
    }

    /**
     * Creates an expression for the container top coordinate. This is the top inset of the container.
     *
     * @return expression for the container top coordinate
     */
    public static Expression top(Container container) {
        return getExpression(container, TOP);
    }

    /**
     * Creates an expression for the container right coordinate taking into account the insets.
     * This is the width of the container minus the right inset.
     *
     * @return expression for the container right coordinate
     */
    public static Expression right(Container container) {
        return getExpression(container, RIGHT);
    }


    /**
     * Creates an expression for the container bottom coordinate taking into account the insets.
     * This is the height of the container minus the bottom inset.
     *
     * @return expression for the container bottom coordinate
     */
    public static Expression bottom(Container container) {
        return getExpression(container, BOTTOM);
    }



    /**
     * Creates an expression for the container width minus the left and right inset.
     *
     * @return container width expression
     */
    public static Expression width(Container container) {
        return getExpression(container, WIDTH);
    }

    /**
     * Creates an expression for the container height minus top and bottom insets.
     *
     * @return container height expression
     */
    public static Expression height(Container container) {
        return getExpression(container, HEIGHT);
    }


    static class ContainerExpression extends Expression {
        private int attribute;

        public ContainerExpression(int attribute) {
            this.attribute = attribute;
        }

        protected double computeValue(ExplicitLayout layout) {
            Container container = layout.getContainer();
            Insets insets = container.getInsets();
            switch (attribute) {
                case TOP :
                    return insets.top;
                case BOTTOM :
                    return container.getSize().height - insets.bottom;
                case LEFT :
                    return insets.left;
                case RIGHT :
                    return container.getSize().width - insets.right;
                case WIDTH :
                    return  container.getSize().width - insets.left - insets.right;
                case HEIGHT :
                    return  container.getSize().height - insets.top - insets.bottom;
                default : return 0;
            }
        }
    }


    /**
     * Creates an expression for a fraction of the container width.
     *
     * @param the fraction
     *
     * @return an expression
     */
    public static Expression widthFraction(Container container, double fraction) {
        Expression expr = MathEF.multiply(width(container), fraction);
        expr.setRoundingMode(Expression.ROUND_FLOOR);
        return expr;
    }

    /**
     * Creates an expression for a fraction of the container height.
     *
     * @param the fraction
     *
     * @return an expression
     */
    public static Expression heightFraction(Container container, double fraction) {
        Expression expr = MathEF.multiply(height(container), fraction);
        expr.setRoundingMode(Expression.ROUND_FLOOR);
        return expr;
    }


   /**
     * Creates an expression for the x coordinate of the specified fraction of the
     * container's width minus insets.
     *
     * @param the fraction
     *
     * @return an expression
     */
    public static Expression xFraction(Container container, double fraction) {
        Expression expr = MathEF.add(left(container), widthFraction(container, fraction));
        return expr;
    }

    /**
     * Creates an expression for y coordinate of the specified fraction of the
     * containers height minus insets.
     *
     * @return an expression
     */
    public static Expression yFraction(Container container, double fraction) {
        Expression expr = MathEF.add(top(container), heightFraction(container, fraction));
        return expr;
    }

    public static Expression centerX(Container container) {
        return xFraction(container, 0.5);
    }

    public static Expression centerY(Container container) {
        return yFraction(container, 0.5);
    }



    /**
     * Gets an expression for the left x coordinate of the grid column specified by <code>gridColumn</code>.
     * The grid is positioned at the left of the container with cell width defined
     * by the specified <code>cellWidth</code> expression.<br>
     * For example, to get an expression for the left x coordinate of column 3 of a
     * 6 column grid that fills the container, you could call this method as follows:<br><br>
     * <code>ContainerEF.gridX(3, ContainerEF.widthFraction(1 / 6.0));</code>
     *
     * @param gridColumn the grid column index starting at 0.
     * @param cellWidth an expression for the grid cell width.
     *
     * @return an expression
     */
    public static Expression gridX(Container container, int gridColumn, Expression cellWidth) {
        Expression expr = MathEF.add(left(container), MathEF.multiply(cellWidth, (double)gridColumn));
        return expr;
    }

    /**
     * Gets an expression for the top y coordinate of the grid row specified by <code>gridRow</code>.
     * The grid is positioned at the top of the container with cell height defined
     * by the specified <code>cellHeight</code> expression.
     *
     * @param gridRow the grid row index starting at 0.
     * @param cellHeight an expression for the grid cell height.
     *
     * @return an expression
     */
    public static Expression gridY(Container container, int gridRow, Expression cellHeight) {
        Expression expr = MathEF.add(top(container), MathEF.multiply(cellHeight, (double)gridRow));
        return expr;
    }



}
