/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/GroupEF.java,v $
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

/**
 * An expression factory used to create expressions
 * that depend on attributes of a group of components.
 *
 */
public class GroupEF {


    private GroupEF() {}

    /**
     * Creates an expression for the top coordinate of the component group; this is the minimum top
     * y coordinate of all components in the group.
     *
     * @param group an array of components
     * @return an Expression for the top coordinate of the group.
     */
    public static Expression top(Component [] group) {
        return MathEF.min(getExpressions(group, ComponentEF.TOP));
    }

    /**
     * Creates an expression for the bottom coordinate of the component group; this is the maximum bottom
     * y coordinate of all components in the group.
     *
     * @param group an array of components
     * @return an Expression for the bottom coordinate of the group.
     */
    public static Expression bottom(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.BOTTOM));
    }

    /**
     * Creates an expression for the left coordinate of the component group; this is the minimum left
     * x coordinate of all components in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression left(Component [] group) {
        return MathEF.min(getExpressions(group, ComponentEF.LEFT));
    }

    /**
     * Creates an expression for the right coordinate of the component group; this is the maximum right
     * x coordinate of all components in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression right(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.RIGHT));
    }



    /**
     * Creates an expession for the height of the component group.
     * This is the bottom coordinate of the group minus the top y coordinate of the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression height(Component [] group) {
        return MathEF.subtract(bottom(group), top(group));
    }


    /**
     * Creates an expession for a fraction of the height of the component group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression heightFraction(Component [] group, double fraction) {
        Expression expr = MathEF.multiply(height(group), fraction);
        expr.setRoundingMode(Expression.ROUND_FLOOR);
        return expr;
    }

    /**
     * Creates an expession for the y coordinate of a fraction of the height of the component group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression yFraction(Component [] group, double fraction) {
        return MathEF.add(top(group), heightFraction(group, fraction));
    }


    public static Expression centerY(Component [] group) {
        return yFraction(group, 0.5);
    }


    /**
     * Creates an expression for the sum of the component heights in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     *
     */
    public static Expression heightSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.HEIGHT));
    }


    /**
     * Creates an expression for the maximum component height in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     *
     */
    public static Expression heightMax(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.HEIGHT));
    }

    /**
     * Creates an expression for the minimum component height in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     *
     */
    public static Expression heightMin(Component [] group) {
        return MathEF.min(getExpressions(group, ComponentEF.HEIGHT));
    }



    /**
     * Creates an expression for the sum of the component preferred heights in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression preferredHeightSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.PREF_HEIGHT));
    }

    /**
     * Creates an expression for the maximum component preferred height in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression preferredHeightMax(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.PREF_HEIGHT));
    }

    /**
     * Creates an expression for the minimum component preferred height in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression preferredHeightMin(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.PREF_HEIGHT));
    }


    /**
     * Create an expression for the sum of the component minimum heights in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression minimumHeightSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.MIN_HEIGHT));
    }

    /**
     * Create an expression for the sum of the component maximum heights in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression maximumHeightSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.MAX_HEIGHT));
    }

    /**
     * Create an expession for the width of the component group.
     * This is the rightmost component right x coordinate minus the
     * leftmost component left x coordinate.
     *
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression width(Component [] group) {
        return MathEF.subtract(right(group), left(group));
    }

    /**
     * Creates an expession for a fraction of the width of the component group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression widthFraction(Component [] group, double fraction) {
        Expression expr = MathEF.multiply(width(group), fraction);
        expr.setRoundingMode(Expression.ROUND_FLOOR);
        return expr;
    }

    /**
     * Creates an expession for the x coordinate of a fraction of the width of the component group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression xFraction(Component [] group, double fraction) {
        return MathEF.add(left(group), widthFraction(group, fraction));
    }


    public static Expression centerX(Component [] group) {
        return xFraction(group, 0.5);
    }



    /**
     * Creates an expression for the sum of the component widths in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression widthSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.WIDTH));
    }

    /**
     * Creates an expression for the minimum component width in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     *
     */
    public static Expression widthMin(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.WIDTH));
    }


    /**
     * Creates an expression for the maximum component width in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     *
     */
    public static Expression widthMax(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.WIDTH));
    }




    /**
     * Create an expression for the sum of the component preferred widths in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression preferredWidthSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.PREF_WIDTH));
    }

    /**
     * Create an expression for the maximum component preferred width in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression preferredWidthMax(Component [] group) {
        return MathEF.max(getExpressions(group, ComponentEF.PREF_WIDTH));
    }

    /**
     * Create an expression for the minimum component preferred width in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression preferredWidthMin(Component [] group) {
        return MathEF.min(getExpressions(group, ComponentEF.PREF_WIDTH));
    }



    /**
     * Creates an expression for the sum of the component minimum widths in the group.
     *
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression minimumWidthSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.MIN_WIDTH));
    }

    /**
     * Creates an expression for the sum of the component maximum widths in the group.
     *
     * @param group an array of components in the group
     * @return an Expression
     */
    public static Expression maximumWidthSum(Component [] group) {
        return MathEF.sum(getExpressions(group, ComponentEF.MAX_WIDTH));
    }

    /**
     * Creates an array of expressions for the specified attibute of each
     * component in the specified array.
     *
     * @param components an array of components
     * @param attribute a component attribute
     *
     * @return an array of expressions
     */
    static Expression [] getExpressions(Component [] components, int attribute) {
        Expression [] expressions = new Expression[components.length];
        for (int i = 0; i < components.length; i++) {
            expressions[i] = ComponentEF.getExpression(components[i], attribute);
        }
        return expressions;
    }

}
