/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/ExplicitConstraints.java,v $
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
 * This class contains expressions that define the location and size of a component.
 *
 * <p>
 * The location of a <code>java.awt.Component</code> is usually specified using the (x, y) coordinate of
 * its top-left corner. This class defines a component's location using expressions for the x and y coordinates
 * of some point on the component defined as the component origin. The component origin is specified
 * as fractions of the component's width and height. This is typically used to align the LEFT, RIGHT,
 * TOP, BOTTOM or CENTER of a group of components; constants have been conveniently defined
 * for this purpose.
 * </p>
 *
 * <p>
 * The size of the component is specified as a width and a height expression. The methods <code>setWidthZeroIfInvisible</code>
 * and <code>setHeightZeroIfInvisible</code> are used to set flags which determine whether the width and height is set to zero
 * if the component is not visible, regardless of the value of the corresponding expression. For example, these
 * flags could be used to determine whether components shuffle up to fill the gap when a component is made invisible.
 * </p>
 *
 */
public class ExplicitConstraints implements Serializable, Cloneable {

    /**
     * Constant used to set the origin x coordinate to the left side of the component.
     * <br>LEFT = 0
     */
    public static final double LEFT = 0.0;

    /**
     * Constant used to set the origin x coordinate to the right side of the component.
     * <br>RIGHT = 1
     */
    public static final double RIGHT = 1.0;

    /**
     * Constant used to set the origin y coordinate to the top of the component.
     * <br>TOP = 0
     */
    public static final double TOP = 0.0;

    /**
     * Constant used to set the origin y coordinate to the bottom of the component.
     * <br>BOTTOM = 1
     */
    public static final double BOTTOM = 1.0;

    /**
     * Constant used to set the origin x or y coordinate to the center of the component.
     * <br>CENTER = 0.5
     */
    public static final double CENTER = 0.5;



    /**
     * The component whose size and location is specified by this constraints object.
     *
     * @serial
     */
    private Component component;

    /**
     * The expression for the x coordinate of the component's origin.
     *
     * @serial
     */
    private Expression x;

    /**
     * The expression for the y coordinate of the component's originx
     *
     * @serial
     */
    private Expression y;

    /**
     * The expression for the component's width.
     *
     * @serial
     */
    private Expression width;

    /**
     * The expression for the component's height.
     *
     * @serial
     */
    private Expression height;


    /**
     * The expression for the component's right X coordinate.
     *
     * @serial
     */
    private Expression right;

    /**
     * The expression for the component's bottom Y coordinate.
     *
     * @serial
     */
    private Expression bottom;


    /**
     * The origin x coordinate as a fraction of the component's width.
     *
     * @serial
     */
    private float originX = 0.0F;

    /**
     * The origin y coordinate as a fraction of the component's height.
     *
     * @serial
     */
    private float originY = 0.0F;


    private boolean widthZeroIfInvisible = true;
    private boolean heightZeroIfInvisible = true;

    /**
     * A name for the component.
     *
     * @serial
     */
    private String name;

    private transient boolean inX = false;
    private transient boolean inY = false;
    private transient boolean inW = false;
    private transient boolean inH = false;

    /**
     * Constructs a new ExplicitConstraints object for the specified component.
     * <P>The attributes are initialised using defaults as follows:
     * <ul>
     * <li>X = ContainerEF.left()</li>
     * <li>Y = ContainerEF.top()</li>
     * <li>Width = null;<br>this indicates to constructor that the component's preferred width is default</li>
     * <li>Height = null;<br>this indicates to constructor that the component's preferred height is default</li>
     * <li>OriginX = LEFT</li>
     * <li>OriginY = TOP</li>
     * <li>WidthZeroIfInvisible = true</li>
     * <li>HeightZeroIfInvisible = true</li>
     * </ul>
     * </p>
     *
     * @param component the component; this cannot be null.
     */
    public ExplicitConstraints(Component component) {
        init(component, null);
    }

    /**
     * Constructs a new ExplicitConstraints object for the specified component.
     * The attributes are initialised using defaults as specified in the
     * no-args constructor. This constructor is intended for use when a
     * ConstraintsSource is used to define the constraints; the name is
     * used to identify the component.
     *
     *
     * @param component the component; this cannot be null.
     * @param name the name of the component.
     *
     */
    public ExplicitConstraints(Component component, String name) {
        init(component, name);
    }


    /**
     * Constructs a new ExplicitConstraints object for the specified component using the
     * specified attributes.
     *
     *
     * @param x the expression for the x coordinate of the component's origin.
     * @param y the expression for the y coordinate of the component's origin.
     * @param width the expression for the component's width or null for preferred width.
     * @param height the expression for the component's height or null for preferred height.
     * @param originX the origin x coordinate fraction in the range 0 .. 1 inclusive.
     * @param originY the origin y coordinate fraction in the range 0 .. 1 inclusive.
     * @param widthZero true if width is zero when component is not visible.
     * @param heightZero true if height is zero when component is not visible.
     *
     *
     */
    public ExplicitConstraints(Component component, Expression x, Expression y,
                               Expression width, Expression height, double originX, double originY,
                               boolean widthZero, boolean heightZero) {
        this(component, x, y, width, height);
        setOriginX(originX);
        setOriginY(originY);
        setWidthZeroIfInvisible(widthZero);
        setHeightZeroIfInvisible(heightZero);
    }


    public ExplicitConstraints(Component component, Expression x, Expression y) {
        this(component, x, y, null, null);
    }


    public ExplicitConstraints(Component component, Expression x, Expression y,
                               Expression width, Expression height) {
        this.component = component;
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /**
     * Constructs a new ExplicitConstraints object for the specified component using the
     * specified attributes.
     *
     * <P>
     * The boolean flags allow you to specify the following combinations of width, right,
     * height and bottom:</P>
     * <UL>
     * <LI>width, height
     * <LI>width, bottom
     * <LI>right, height
     * <LI>right, bottom
     * </UL>
     * <P>They are also necessary to distinguish the constructor signature from
     * the x,y,width,height constructor.</P>
     *
     * @param left the expression for the component's left x coordinate.
     * @param top the expression for the component's top y coordinate.
     * @param widthOrRight the expression for the component's width or right x coordinate depending on isWidth.
     * @param isWidth true if widthOrRight specifies width; false if widthOrRight specifies right.     *
     * @param hightOrBottom the expression for the component's height or bottom y coordinate depending on isHeight.
     * @param isHeight true if heightOrBottom specifies height; false if heightOrBottom specifies bottom.
     */
    public ExplicitConstraints(Component component, Expression left, Expression top,
                               Expression widthOrRight, boolean isWidth,
                               Expression heightOrBottom, boolean isHeight) {
        this.component = component;
        setX(left);
        setY(top);
        if (isWidth) {
            setWidth(widthOrRight);
        } else {
            setRight(widthOrRight);
        }
        if (isHeight) {
            setHeight(heightOrBottom);
        } else {
            setBottom(heightOrBottom);
        }
    }




    private void init(Component component, String name) {
        if (component == null) {
            throw new NullPointerException("component cannot be null");
        }
        this.component = component;
        this.name = name;
        restoreDefaults();
    }


    /**
     * Gets a name used to identify a component in a ConstraintsSource.
     *
     * @return the component name or null if name not specified.
     */
    public String getName() {
        return name;
    }


    /**
     * Gets the component whose size and location are defined by this constraints object.
     * This may return null if this is a default constraints object created using the
     * no-args constructor.
     *
     * @return the component
     */
    public Component getComponent() {
        return component;
    }


    /**
     * Sets the expression for the x coordinate of the component's origin.
     *
     * @param x the expression for the x coordinate of the component's origin.
     */
    public void setX(Expression x) {
        if (x == null) {
            throw new NullPointerException("x cannot be null");
        }
        this.x = x;
        if (right != null) {
            width = right.subtract(x);
        }
    }

    /**
     * Sets the expression for the y coordinate of the component's origin.
     *
     * @param y the expression for the y coordinate of the component's origin.
     */
    public void setY(Expression y) {
        if (y == null) {
            throw new NullPointerException("y cannot be null");
        }
        this.y = y;
        if (bottom != null) {
            height = bottom.subtract(y);
        }
    }

    /**
     * Sets the expression for the component's width.
     * <p>Warning: This width expression should not be dependent on the component's x coordinate if
     * originX != 0.0. This is because the x coordinate is dependent on the width expression
     * if originX != 0.0. Such a circular definition will result in a stack overflow error.</p>
     * If the width expression is null, an expression for the component's preferred
     * width is used. This overrides any previous call to setRight.
     *
     *
     * @param width the expression for the component's width
     */
    public void setWidth(Expression width) {
        if (width == null) {
            this.width = ComponentEF.preferredWidth(component);
        } else {
            this.width = width;
        }
        right = null;
    }

    /**
     * Sets the expression for the component's height.
     * <p>Warning: This height expression should not be dependent on the component's y coordinate if
     * originY != 0.0. This is because the y coordinate is dependent on the width expression
     * if originY != 0.0. Such a circular definition will result in a stack overflow error.</p>
     * If the height expression is null, an expression for the component's preferred
     * height is used. This overrides any previous call to setBottom.
     *
     * @param height the expression for the component's height.
     */
    public void setHeight(Expression height) {
        if (height == null) {
            this.height = ComponentEF.preferredHeight(component);
        } else {
            this.height = height;
        }
        bottom = null;
    }

    /**
     * Sets the expression for the component's right x coordinate.
     * This is an alternative to specifying the width expression.
     */
    public void setRight(Expression right) {
        this.right = right;
        if (right != null) {
            width = right.subtract(x);
            originX = 0.0F;
        }
    }

    /**
     * Sets the expression for the component's bottom y coordinate and sets originY to TOP.
     * This is an alternative to specifying the height expression.
     */
    public void setBottom(Expression bottom) {
        this.bottom = bottom;
        if (bottom != null) {
            height = bottom.subtract(y);
            originY = 0.0F;
        }
    }


    /**
     * This flag influences the value returned by getWidthValue() depending on whether
     * the component is visible.
     *
     * @param flag the value of the flag
     *
     * @see #getWidthValue(ExplicitLayout)
     */
    public void setWidthZeroIfInvisible(boolean flag) {
        widthZeroIfInvisible = flag;
    }


    /**
     * Sets a flag that influences the value returned by getHeightValue() depending on whether
     * the component is visible.
     *
     * @param flag the value of the flag.
     *
     * @see #getHeightValue(ExplicitLayout)
     */
    public void setHeightZeroIfInvisible(boolean flag) {
        heightZeroIfInvisible = flag;
    }

    /**
     * Determines whether the component width is set to zero if the component is not visible.
     *
     * @return true if the component width is set to zero if the component is not visible; false otherwise.
     *
     * @see #getWidthValue(ExplicitLayout)
     */
    public boolean isWidthZeroIfInvisible() {
        return widthZeroIfInvisible;
    }

   /**
     * Determines whether the component height is set to zero if the component is not visible.
     *
     * @return true if the component height is set to zero if the component is not visible; false otherwise.
     *
     * @see #getHeightValue(ExplicitLayout)
     */
    public boolean isHeightZeroIfInvisible() {
        return heightZeroIfInvisible;
    }

    /**
     * Gets the expression for the x coordinate of the component's origin.
     *
     * @return the x coordinate expression
     */
    public Expression getX() {
        return x;
    }

    /**
     * Gets the expression for the y coordinate of the component's origin.
     *
     * @return the y coordinate expression
     */
    public Expression getY() {
        return y;
    }

    /**
     * Gets the expression for the component's width.
     *
     * @return the width expression
     */
    public Expression getWidth() {
        return width;
    }

    /**
     * Gets the expression for the component's height.
     *
     * @return the height expression
     */
    public Expression getHeight() {
        return height;
    }

    public Expression getRight() {
        return right;
    }

    public Expression getBottom() {
        return bottom;
    }
    /**
     * Gets the x coordinate value for the top-left corner of the component.
     * <P>This is used by <code>ExplicitLayout</code> to set the component's location.</P>
     *
     * @param layout the explicit layout containing this constraints object
     *
     * @return the x coordinate value
     *
     */
    public int getXValue(ExplicitLayout layout) {
        try {
            if (inX) throw new IllegalStateException(infiniteMsg("x coordinate", layout));
            inX = true;
            double value = x.getValue(layout);
            if (originX != 0.0) {
                value -= originX * getWidthValue(layout);
            }
            return (int)Math.round(value);
        } finally {
            inX = false;
        }
    }

    /**
     * Gets the y coordinate value for the top-left corner of the component.
     * <P>This is used by <code>ExplicitLayout</code> to set the component's location.</P>
     *
     * @param layout the explicit layout containing this constraints object
     *
     * @return the y coordinate value
     *
     */
    public int getYValue(ExplicitLayout layout) {
        try {
            if (inY) throw new IllegalStateException(infiniteMsg("y coordinate", layout));
            inY = true;
            double value = y.getValue(layout);
            if (originY != 0.0) {
                value -= originY * getHeightValue(layout);
            }
            return (int)Math.round(value);
        } finally {
            inY = false;
        }
    }

    /**
     * Gets the width value for the component.
     *
     * <p>If widthZeroIfInvisible flag is true, this gets the value of the width expression
     * if the component is visible and 0 if the component is not visible.<br>
     * If widthZeroIfInvisible flag is false, this gets the value of the width expression
     * regardless of whether the component is visible or not.</p>
     *
     * <p>This is used by <code>ExplicitLayout</code> to set the component's width.</p>
     *
     * @param layout the explicit layout containing this constraints object
     *
     * @return the width value
     *
     */
    public int getWidthValue(ExplicitLayout layout) {
        try {
            if (inW) throw new IllegalStateException(infiniteMsg("width", layout));
            inW = true;
            return (widthZeroIfInvisible && !component.isVisible())
                ? 0
                : (int)Math.round(width.getValue(layout));
        } finally {
            inW = false;
        }
    }

    /**
     * Gets the height value for the component.
     *
     * <p>If heightZeroIfInvisible flag is true, this gets the value of the height expression
     * if the component is visible and 0 if the component is not visible.<br>
     * If heightZeroIfInvisible flag is false, this gets the value of the height expression
     * regardless of whether the component is visible or not.</p>
     *
     * <p>This is used by <code>ExplicitLayout</code> to set the component's height.</p>
     *
     * @param layout the explicit layout containing this constraints object
     *
     * @return the height value
     *
     */
    public int getHeightValue(ExplicitLayout layout) {
        try {
            if (inH) throw new IllegalStateException(infiniteMsg("height", layout));
            inH = true;
            return (heightZeroIfInvisible && !component.isVisible())
                ? 0
                : (int)Math.round(height.getValue(layout));
        } finally {
            inH = false;
        }
    }

    private String infiniteMsg(String attribute, ExplicitLayout layout) {
        StringBuffer sb = new StringBuffer();
        Component [] components;
        int index;
        components = layout.getContainer().getComponents();
        for (index = 0; index < components.length; index++) {
            if (components[index] == component) {
                break;
            }
        }
        return sb.append("Infinite recursion in ").append(attribute).
        append(" expression for component ").append(index).
        append(" of container\n").append(layout.getContainer()).toString();
    }

    /**
     * Sets the origin x coordinate as a fraction of the component's width.
     * OriginX cannot be set to anything other than LEFT if a right X expression is specified.
     *
     * @param originX the origin x coordinate fraction in the range 0 .. 1 inclusive.
     * The constants LEFT, CENTER and RIGHT can be used.
     *
     * @throws IllegalArgumentException if originX is out of range.
     */
    public void setOriginX(double originX) {
        if (originX < 0.0 || originX > 1.0) {
            throw new IllegalArgumentException("originX out of range 0 .. 1");
        }
        if (right == null) {
            this.originX = (float)originX;
        }
    }

    /**
     * Gets the origin x coordinate fraction.
     *
     * @return the origin x coordinate fraction.
     */
    public double getOriginX() {
        return originX;
    }

    /**
     * Sets the origin y coordinate as a fraction of the component's height.
     * OriginY cannot be set to anything other than TOP if a bottom X expression is specified.
     *
     * @param originY the origin y coordinate fraction in the range 0 .. 1 inclusive.
     * The constants TOP, CENTER and BOTTOM can be used.
     *
     * @throws IllegalArgumentException if originY is out of range.
     */
    public void setOriginY(double originY) {
        if (originY < 0.0 || originY > 1.0) {
            throw new IllegalArgumentException("originY out of range 0 .. 1");
        }
        if (bottom == null) {
            this.originY = (float)originY;
        }
    }

    /**
     * Gets the origin y coordinate fraction.
     *
     * @return the origin y coordinate fraction.
     */
    public double getOriginY() {
        return originY;
    }



    /**
     * Invalidate the the x, y, width and height expressions.
     */
    protected void invalidate() {
        x.invalidate();
        y.invalidate();
        width.invalidate();
        height.invalidate();
    }


    public void restoreDefaults() {
        x = ContainerEF.left(null);
        y = ContainerEF.top(null);
        width = ComponentEF.preferredWidth(component);
        height = ComponentEF.preferredHeight(component);;
        originX = 0.0F;
        originY = 0.0F;
        right = null;
        bottom = null;
        widthZeroIfInvisible = true;
        heightZeroIfInvisible = true;
    }

    /**
     * Copies the attributes from some other constraints object to this.
     * This copies the x, y, width and height, originX, originY,
     * widthZeroIfInvisible and heightZeroIfInvisible attributes. It does not
     * copy the component or name attribute.
     *
     * @param other some other constraints object
     */
    public void copy(ExplicitConstraints other) {
        x = other.x;
        y = other.y;
        width = other.width;
        height = other.height;
        right = other.right;
        bottom = other.bottom;
        originX = other.originX;
        originY = other.originY;
        widthZeroIfInvisible = other.widthZeroIfInvisible;
        heightZeroIfInvisible = other.heightZeroIfInvisible;
    }



    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.toString());
        }
    }

}
