/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/ComponentEF.java,v $
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
 * An expression factory used to create expressions for attributes of a component.
 */
public class ComponentEF {

    static final int TOP         =  0;
    static final int BOTTOM      =  1;
    static final int LEFT        =  2;
    static final int RIGHT       =  3;
    static final int WIDTH       =  4;
    static final int HEIGHT      =  5;

    /**
     * All expressions for attributes with a value less
     * than ROUND_FLOOR_MAX have the rounding mode set to ROUND_FLOOR.
     */
    private static final int ROUND_FLOOR_MAX = 5;

    static final int MIN_WIDTH   =  6;
    static final int MIN_HEIGHT  =  7;
    static final int MAX_WIDTH   =  8;
    static final int MAX_HEIGHT  =  9;
    static final int PREF_WIDTH  = 10;
    static final int PREF_HEIGHT = 11;

    private static final int ATTRIBUTE_COUNT = 12;

    /**
     * array of maps from component to expression for each attribte
     */
    private static WeakHashMap [] cache = new WeakHashMap[ATTRIBUTE_COUNT];

    private ComponentEF() {}


    static Expression getExpression(Component component, int attribute) {
        if (attribute < 0 || attribute >= ATTRIBUTE_COUNT) {
            throw new IllegalArgumentException("Illegal attribute " + attribute);
        }
        Expression expression;
        if (cache[attribute] == null) {
            cache[attribute] = new WeakHashMap();
            expression = new ComponentExpression(component, attribute);
            cache[attribute].put(component, expression);
        } else if ((expression = (Expression)cache[attribute].get(component)) == null) {
            expression = new ComponentExpression(component, attribute);
            cache[attribute].put(component, expression);
        }
        return expression;
    }

    /**
     * Creates an expression for the preferred width of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression preferredWidth(Component component) {
        return getExpression(component, PREF_WIDTH);
    }

    /**
     * Creates an expression for the preferred height of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression preferredHeight(Component component) {
        return getExpression(component, PREF_HEIGHT);
    }

    /**
     * Creates an expression for the minimum width of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression minimumWidth(Component component) {
        return getExpression(component, MIN_WIDTH);
    }

    /**
     * Creates an expression for the minimum height of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression minimumHeight(Component component) {
        return getExpression(component, MIN_HEIGHT);
    }

    /**
     * Creates an expression for the maximum width of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression maximumWidth(Component component) {
        return getExpression(component, MAX_WIDTH);
    }

    /**
     * Creates an expression for the maximum height of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression maximumHeight(Component component) {
        return getExpression(component, MAX_HEIGHT);
    }


    /**
     * Creates an expression for the width of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression width(Component component) {
        return getExpression(component, WIDTH);
    }


    /**
     * Creates an expression for the height of the specified component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression height(Component component) {
        return getExpression(component, HEIGHT);
    }

    /**
     * Creates an expression for the x coordinate of the left side of the component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression left(Component component) {
        return getExpression(component, LEFT);
    }

    /**
     * Creates an expression for the y coordinate of the top of the component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression top(Component component) {
        return getExpression(component, TOP);
    }

    /**
     * Creates an expression for the x coordinate of the right side of the component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */

    public static Expression right(Component component) {
        return getExpression(component, RIGHT);
    }

    /**
     * Creates an expression for the y coordinate of the bottom of the component.
     *
     * @param component the component; this can be null when used to create an expression group.
     *
     * @return the expression
     */
    public static Expression bottom(Component component) {
        return getExpression(component, BOTTOM);
    }

    /**
     * Creates an expression for a fraction of a components width.
     *
     * @param the component
     * @param the fraction
     *
     * @return the expression
     */
    public static Expression widthFraction(Component component, double fraction) {
        Expression expr = width(component).multiply(fraction);
        expr.setRoundingMode(Expression.ROUND_FLOOR);
        return expr;
    }

    /**
     * Creates an expression for a fraction of a components height.
     *
     * @param the component
     * @param the fraction
     *
     * @return the expression
     */
    public static Expression heightFraction(Component component, double fraction) {
        Expression expr = height(component).multiply(fraction);
        expr.setRoundingMode(Expression.ROUND_FLOOR);
        return expr;
    }

    /**
     * Creates an expression for the x coordinate of a fraction of a components width.
     *
     * @param the component
     * @param the fraction
     *
     * @return the expression
     */
    public static Expression xFraction(Component component, double fraction) {
        Expression expr = MathEF.add(left(component), widthFraction(component, fraction));
        return expr;
    }

    /**
     * Creates an expression for the y coordinate of a fraction of a components height.
     *
     * @param the component
     * @param the fraction
     *
     * @return the expression
     */
    public static Expression yFraction(Component component, double fraction) {
        Expression expr = MathEF.add(top(component), heightFraction(component, fraction));
        return expr;
    }


    public static Expression centerX(Component component) {
        return xFraction(component, 0.5);
    }

    public static Expression centerY(Component component) {
        return yFraction(component, 0.5);
    }


    /**
    * An expression whose value depends on the attributes of a component.
    *
    */
    static class ComponentExpression extends Expression implements Cloneable {

        private WeakReference componentRef;
        private int attribute;


        /**
         * Construct a new component expression.
         *
         * @param component the component whose attribute provides the value of this expression.
         */
        public ComponentExpression(Component component, int attribute) {
            componentRef = new WeakReference(component);
            this.attribute = attribute;
            if (attribute <= ROUND_FLOOR_MAX) {
                setRoundingMode(Expression.ROUND_FLOOR);
            }
        }

        public Component getComponent() {
            return (Component)componentRef.get();
        }

        public int getAttribute() {
            return attribute;
        }


        protected double computeValue(ExplicitLayout layout) {
            ExplicitConstraints constraints;
            Component component = getComponent();
            switch (attribute) {
                case TOP :
                    constraints = layout.getConstraints(component);
                    return (constraints == null) ? 0 : constraints.getYValue(layout);
                case BOTTOM :
                    constraints = layout.getConstraints(component);
                    return (constraints == null) ? 0 : (constraints.getYValue(layout) + constraints.getHeightValue(layout));
                case LEFT :
                    constraints = layout.getConstraints(component);
                    return (constraints == null) ? 0 : constraints.getXValue(layout);
                case RIGHT :
                    constraints = layout.getConstraints(component);
                    return (constraints == null) ? 0 : constraints.getXValue(layout) + constraints.getWidthValue(layout);
                case WIDTH :
                    constraints = layout.getConstraints(component);
                    return (constraints == null) ? 0 : constraints.getWidthValue(layout);
                case HEIGHT :
                    constraints = layout.getConstraints(component);
                    return (constraints == null) ? 0 : constraints.getHeightValue(layout);
                case PREF_WIDTH :
                    return component.getPreferredSize().width;
                case PREF_HEIGHT :
                    return component.getPreferredSize().height;
                case MIN_WIDTH :
                    return component.getMinimumSize().width;
                case MIN_HEIGHT :
                    return component.getMinimumSize().height;
                case MAX_WIDTH :
                    return component.getMaximumSize().width;
                case MAX_HEIGHT :
                    return component.getMaximumSize().height;
                default : return 0;
            }

        }
    }

}
