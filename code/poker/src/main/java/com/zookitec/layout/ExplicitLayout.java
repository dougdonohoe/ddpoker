/*
 * $Source: e:\\cvshome/explicit3/src/com/zookitec/layout/ExplicitLayout.java,v $
 * $Revision: 1.4 $
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
import java.util.*;
import java.io.*;

/**
 * A layout manager that provides explicit control over the layout of components.
 */
public class ExplicitLayout implements LayoutManager2, Serializable {


    private static final Dimension MINIMUM_SIZE = new Dimension(0,0);
    private static final Dimension MAXIMUM_SIZE = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static final Dimension PREFERRED_SIZE = new Dimension(100,100);


    /**
     * The container being layed out.
     *
     */
    private transient Container container;

    /**
     * Hashtable mapping components to constraints.
     *
     * @serial
     */
    private Hashtable component2constraints;


    /**
     * Hashtable mapping component names to constraints.
     *
     * @serial
     */
    private Hashtable name2constraints;



    /**
     * An expression for the maximum size of this layout; this can be null.
     *
     * @serial
     */
    private DimensionExpression maximumSize;

    /**
     * An expression for the preferred size of this layout; this can be null.
     *
     * @serial
     */
    private DimensionExpression preferredSize;

    /**
     * An expression for the minimum size of this layout; this can be null.
     *
     * @serial
     */
    private DimensionExpression minimumSize;

// REMOVE FOR STANDARD
    private LayoutListener layoutListener;
// END REMOVE FOR STANDARD


    /**
     * Constructs an ExplicitLayout
     */
    public ExplicitLayout() {
        component2constraints = new Hashtable();
        name2constraints = new Hashtable();
    }


    /**
     * Gets the container being layed out.
     *
     * @return the container
     */
    public Container getContainer() {
        return container;
    }

    /**
     * Gets the constraints object for the specified <code>component</code>.
     *
     * @param component the component
     *
     * @return the contraints for the specified <code>component</code>; null if component unknown.
     *
     */
    public ExplicitConstraints getConstraints(Component component) {
        return (ExplicitConstraints)component2constraints.get(component);
    }


    /**
     * Gets the constraints object for the named component by name.
     *
     * @param name the name of the component
     *
     * @return the coonstraints; null if component unknown.
     *
     */
    public ExplicitConstraints getConstraints(String name) {
        return (ExplicitConstraints)name2constraints.get(name);
    }


    /**
     * Gets an enumeration of all the named constraints.
     * These are constraints constructed using ExplicitConstraints(Component component, String name)
     * for which ExplicitConstraints.getName is not null.
     */
    public Enumeration getNamedConstraints() {
        return name2constraints.elements();
    }


    /**
     * Used to represent expressions for the minimum, preferred and maximum size.
     */
    static class DimensionExpression {
        private Expression width;
        private Expression height;

        private Dimension dimension;

        private boolean valid = false;

        /**
         * Construct a DimensionExpression from a width and height expression.
         */
        public DimensionExpression(Expression width, Expression height) {
            if (width == null || height == null) {
                throw new IllegalArgumentException("Both width and height must be specified");
            }
            this.width = width;
            this.height = height;
            dimension = new Dimension();
        }


        public Dimension getDimension(ExplicitLayout layout) {
            if (!valid) {
                Insets insets = layout.getContainer().getInsets();
                dimension.width = (int)width.getValue(layout) + insets.left + insets.right;
                dimension.height = (int)height.getValue(layout) + insets.top + insets.bottom;
                valid = true;
            }
            return dimension;
        }

        /**
         * Invalidate the width and height expressions.
         */
        public void invalidate() {
            width.invalidate();
            height.invalidate();
            valid = false;
        }
    }


    /**
     * Not supported. Use <code>addLayoutComponent(Component , Object)</code> instead.
     *
     * @param name the component name
     * @param comp the component to be added
     *
     * @see #addLayoutComponent(Component , Object)
     *
     * @throws IllegalArgumentException
     */
    public void addLayoutComponent(String name, Component comp) {
        throw new IllegalArgumentException("No constraints specified");
    }


    /**
     * Adds the specified component to the layout using the specified constraints.
     * The constraints must be an instance of <code>ExplicitConstraints</code>.
     *
     * If constraints is null, the component's size and location will not be changed
     * by ExplicitLayout. This can be used when migrating to ExplicitLayout from a
     * prototype null layout; direct calls to component.setBounds will still be
     * effective if constrains is null.
     *
     * @param component the component to add to the layout
     * @param constraints the constraints of the component
     *
     * @throws IllegalArgumentException if constraints is not an instance of ExplicitConstraints
     * @throws IllegalArgumentException if the component specified by the constraints is null or different from the
     * component being added.
     * @see ExplicitConstraints
     *
     */
    public void addLayoutComponent(Component component, Object constraints) {
        String name;
        if (constraints == null) {
            constraints = new AbsoluteConstraints(component);
        } else {
            if (!(constraints instanceof ExplicitConstraints)) {
                throw new IllegalArgumentException("constraints must be an instance of ExplicitConstraints, not '" + constraints + "'");
            }
            if (((ExplicitConstraints)constraints).getComponent() != component) {
                throw new IllegalArgumentException("component does not match component specifed in the constraints");
            }
        }
        component2constraints.put(component, constraints);
        if ((name = ((ExplicitConstraints)constraints).getName()) != null) {
            name2constraints.put(name, constraints);
        }

    }



    /**
     * Removes the specified component from the layout.
     *
     * @param component the component to remove
     */
    public void removeLayoutComponent(Component component) {
        ExplicitConstraints constraints = getConstraints(component);
        if (constraints != null) {
            component2constraints.remove(component);
        }

    }


    /**
     * Not used.
     */
    public float getLayoutAlignmentX(Container target) {
        return 0.0f;
    }


    /**
     * Not used.
     */
    public float getLayoutAlignmentY(Container target) {
        return 0.0f;
    }


    /**
     * Discards all cached information used by this layout manager.
     */
    public void invalidateLayout(Container parent) {
        synchronized (parent.getTreeLock()) {
            if (minimumSize != null) {
                minimumSize.invalidate();
            }
            if (maximumSize != null) {
                maximumSize.invalidate();
            }
            if (preferredSize != null) {
                preferredSize.invalidate();
            }
            Enumeration _enum = component2constraints.elements();
            while (_enum.hasMoreElements()) {
                ((ExplicitConstraints)_enum.nextElement()).invalidate();
            }
        }
    }


    /**
     * Sets expressions for the maximum width and height of the container.
     * The expressions should not include the container's internal borders as
     * returned by the Container getInsets method.
     *
     * @param width an expression for the maximum width of the container.
     * @param height an expression for the maximum height of the container.
     */
    public void setMaximumLayoutSize(Expression width, Expression height) {
        maximumSize = new DimensionExpression(width, height);
    }

    /**
     * Gets the maximum layout size for the container calculated by adding the
     * container's insets to the maximum size expressions specified by the
     * setMaximumLayoutSize method. If the setMaximumLayoutSize method has not
     * been called, return dimension Integer.MAX_VALUE, Integer.MAX_VALUE.
     *
     * @param parent the container to which this layout manager belongs.
     *
     */
    public Dimension maximumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            container = parent;
            if (maximumSize == null) {
                return MAXIMUM_SIZE;
            } else {
                return maximumSize.getDimension(this);
            }
        }
    }


    /**
     * Sets expressions for the preferred width and height of the container.
     * The expressions should not include insets.
     *
     * @param width an expression for the preferred width of the container.
     * @param height an expression for the preferred height of the container.
     */
    public void setPreferredLayoutSize(Expression width, Expression height) {
        preferredSize = new DimensionExpression(width, height);
    }

    /**
     * Gets the preferred layout size for the container calculated by adding the
     * container's insets to the preferred size expressions specified by the
     * setPreferredLayoutSize method. If the setPreferredLayoutSize method has not
     * been called, return dimension 100,100.
     *
     * @param parent the container to which this layout manager belongs.
     *
     */
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            container = parent;
            if (preferredSize == null) {
                return PREFERRED_SIZE;
            } else {
                return preferredSize.getDimension(this);
            }
        }
    }


    /**
     * Sets expressions for the minimum width and height of the container.
     *
     * @param width an expression for the minimum width of the container.
     * @param height an expression for the minimum height of the container.
     */
    public void setMinimumLayoutSize(Expression width, Expression height) {
        minimumSize = new DimensionExpression(width, height);
    }

    /**
     * Gets the minimum layout size for the container calculated by adding the
     * container's insets to the minimum size expressions specified by the
     * setMinimumLayoutSize method. If the setMinimumLayoutSize method has not
     * been called, return dimension 0,0.
     *
     * @param parent the container to which this layout manager belongs.
     *
     */
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            container = parent;
            if (minimumSize == null) {
                return MINIMUM_SIZE;
            } else {
                return minimumSize.getDimension(this);
            }
        }
    }

    /**
     * This method sets the size and position of each component as defined in the component's
     * ExplicitConstraints object.
     *
     * @param parent the container to which this layout manager belongs.
     */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            container = parent;

            if (layoutListener != null) {
                layoutListener.beforeLayout(this);
            }

            Component [] components = parent.getComponents();
            ExplicitConstraints constraints;
            int x,y,w,h;
            for (int i = 0; i < components.length; i++) {
                //get the constraints for the component
                constraints = (ExplicitConstraints)component2constraints.get(components[i]);
                if (constraints != null) {
                    //set the component bounds based on the constraints
                    w = constraints.getWidthValue(this);
                    h = constraints.getHeightValue(this);
                    x = constraints.getXValue(this);
                    y = constraints.getYValue(this);
                    components[i].setBounds(x,y,w,h);
                }
            }
        }
    }


    /**
     * Class used in place of null constraints. This doesn't have any
     * effect on the size and location of the specified component but allows
     * other component's to be positioned relative to the specified component.
     *
     */
    class AbsoluteConstraints extends ExplicitConstraints {
        public AbsoluteConstraints(Component component) {
            super(component);
        }

        public int getXValue(ExplicitLayout layout) {
            return getComponent().getLocation().x;
        }

        public int getYValue(ExplicitLayout layout) {
            return getComponent().getLocation().y;
        }

        public int getWidthValue(ExplicitLayout layout) {
            return getComponent().getSize().width;
        }

        public int getHeightValue(ExplicitLayout layout) {
            return getComponent().getSize().height;
        }
    }



    /**
     * Sets a LayoutListener.
     *
     * <P>The LayoutListener.beforeLayout method is called by
     * layoutContainer before the container is layed out. This gives the the LayoutListener
     * opprotunity to modify the constraints before the container is layed out.</P>
     *
     * <P>For example, a LayoutListener could be used to set different layout constraints
     * depending on the container's width / height ratio.</P?
     *
     * @param layoutListener a object that implements LayoutListener; null to remove the layout listener.
     */
    public void setLayoutListener(LayoutListener layoutListener) {
        this.layoutListener = layoutListener;
    }

    /**
     * Gets the current layout listener.
     *
     * @return the layout listener or null.
     */
    public LayoutListener getLayoutListener() {
        return layoutListener;
    }




}
