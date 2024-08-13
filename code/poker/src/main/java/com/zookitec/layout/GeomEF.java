/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/GeomEF.java,v $
 * $Revision: 1.3 $
 * $Date: 2003/05/05 23:27:53 $
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
 * An expression factory used to create expressions for points on geometric shapes.
 *
 */
public class GeomEF {


    private GeomEF() {}

    /**
     * Creates an expression for the x coordinate of a point on an ellipse.
     * <BR>
     *
     * The angle has an equivalent meaning to the start angle of the Graphics.drawArc method.
     * Angles are interpreted such that 0 degrees is at the 3 o'clock position. A positive value indicates a
     * counter-clockwise rotation while a negative value indicates a clockwise rotation.
     * The angle is only a true angle when the ellipse is actually a circle (radiusX == radiusY).
     * For an ellipse the 45 degrees falls on the line from the center of the ellipse
     * to the upper right corner of the bounding rectangle. This is a consequense of the
     * parametric equation used. When specifying a point on an ellipse, you must
     * use both ellipseX and ellipseY methods with the same angle.
     *
     * @param centerX an expression for the x coordinate of the center of the ellipse
     * @param radiusX an expression for radius of the ellipse along the x axis
     * @param angle an angle of the point around the ellipse
     */
    public static Expression ellipseX(Expression centerX, Expression radiusX, double angle) {
        return new EllipseExpression(centerX, radiusX, angle, true);
    }

    /**
     * Creates an expression for the Y coordinate of a point on an ellipse.
     * <BR>
     *
     * The angle has an equivalent meaning to the start angle of the Graphics.drawArc method.
     * See ellipseX for details. When specifying a point on an ellipse, you must
     * use both ellipseX and ellipseY methods with the same angle.
     *
     * @param centerX an expression for the x coordinate of the center of the ellipse
     * @param radiusX an expression for radius of the ellipse along the x axis
     * @param angle an angle of the point around the ellipse
     */
    public static Expression ellipseY(Expression centerY, Expression radiusY, double angle) {
        return new EllipseExpression(centerY, radiusY, angle, false);
    }

    /**
     * Creates an expression for the x coordinate of a point a fraction of
     * the way along a line.
     * <BR>
     *
     * To define a point on the line, you must
     * use both lineX and lineY methods using the same value for p.
     *
     * @param x1 an expression for the x coordinate of the line start point
     * @param x2 an expression for the x coordinate of the line end point
     * @param p a fraction of the line between 0.0 and 1.0
     *
     */
    public static Expression lineX(Expression x1, Expression x2, double p) {
        return new LineExpression(x1, x2, p);
    }


    /**
     * Creates an expression for the y coordinate of a point a fraction of
     * the way along a line.
     * <BR>
     *
     * To define a point on the line, you must
     * use both lineX and lineY methods using the same value for p.
     *
     * @param y1 an expression for the y coordinate of line start point
     * @param y2 an expression for the y coordinate of line end point
     * @param p a fraction of the line between 0.0 and 1.0
     */
    public static Expression lineY(Expression y1, Expression y2, double p) {
        return new LineExpression(y1, y2, p);
    }


    static class LineExpression extends Expression {

        Expression start;
        Expression end;
        double p;

        public LineExpression(Expression start, Expression end, double p) {
            this.start = start;
            this.end = end;
            this.p = p;
            setRoundingMode(Expression.ROUND_NEAREST);
        }

        protected double computeValue(ExplicitLayout layout) {
            return (1 - p) * start.getValue(layout) + p * end.getValue(layout);
        }


        public void invalidate() {
            super.invalidate();
            start.invalidate();
            end.invalidate();
        }


    }




    static class EllipseExpression extends Expression {

        Expression center;
        Expression radius;
        double angle;
        boolean isX;

        public EllipseExpression(Expression center, Expression radius, double angle, boolean isX) {
            this.center = center;
            this.radius = radius;
            this.angle = Math.toRadians(angle);
            this.isX = isX;
            setRoundingMode(Expression.ROUND_NEAREST);
        }

        protected double computeValue(ExplicitLayout layout) {
            if (isX) {
                return center.getValue(layout) + Math.cos(angle) * radius.getValue(layout);
            } else {
                return center.getValue(layout) - Math.sin(angle) * radius.getValue(layout);
            }
        }


        public void invalidate() {
            super.invalidate();
            center.invalidate();
            radius.invalidate();
        }

    }

}
