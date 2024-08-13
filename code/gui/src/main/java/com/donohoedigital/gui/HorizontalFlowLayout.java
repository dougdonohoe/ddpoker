/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * 
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials) 
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 * 
 * For inquiries regarding commercial licensing of this source code or 
 * the use of names, logos, images, text, or other assets, please contact 
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
/*
 * HorizontalFlowLayout.java
 *
 * Created on November 24, 2002, 1:21 PM
 */

package com.donohoedigital.gui;

import java.awt.*;
import java.io.*;

/**
 * A flow layout arranges components in a left-to-right flow, much
 * like lines of text in a paragraph. Flow layouts are typically used
 * to arrange buttons in a panel. It will arrange
 * buttons left to right until no more buttons fit on the same line.
 * Each line is centered.
 * <p>
 * For example, the following picture shows an applet using the flow
 * layout manager (its default layout manager) to position three buttons:
 * <p>
 * <img src="doc-files/HorizontalFlowLayout-1.gif"
 * ALT="Graphic of Layout for Three Buttons"
 * ALIGN=center HSPACE=10 VSPACE=7>
 * <p>
 * Here is the code for this applet:
 * <p>
 * <hr><blockquote><pre>
 * import java.awt.*;
 * import java.applet.Applet;
 *
 * public class myButtons extends Applet {
 *     Button button1, button2, button3;
 *     public void init() {
 *         button1 = new Button("Ok");
 *         button2 = new Button("Open");
 *         button3 = new Button("Close");
 *         add(button1);
 *         add(button2);
 *         add(button3);
 *     }
 * }
 * </pre></blockquote><hr>
 * <p>
 * A flow layout lets each component assume its natural (preferred) size.
 *
 */
public class HorizontalFlowLayout implements LayoutManager, java.io.Serializable 
{
    /**
     * This value indicates that each row of components
     * should be left-justified.
     */
    public static final int LEFT 	= 0;

    /**
     * This value indicates that each row of components
     * should be centered.
     */
    public static final int CENTER 	= 1;

    /**
     * This value indicates that each row of components
     * should be right-justified.
     */
    public static final int RIGHT 	= 2;

    /**
     * This value indicates that each row of components
     * should be justified to the leading edge of the container's
     * orientation, for example, to the left in left-to-right orientations.
     *
     * @see     java.awt.Component#getComponentOrientation
     * @see     java.awt.ComponentOrientation
     * @since   1.2
     * Package-private pending API change approval
     */
    public static final int LEADING	= 3;

    /**
     * This value indicates that each row of components
     * should be justified to the trailing edge of the container's
     * orientation, for example, to the right in left-to-right orientations.
     *
     * @see     java.awt.Component#getComponentOrientation
     * @see     java.awt.ComponentOrientation
     * @since   1.2
     * Package-private pending API change approval
     */
    public static final int TRAILING = 4;
    
    /**
    * This value indicates that each subcomponents
    * should be top-justified. 
    * @since   JDK1.0 
    */
    public static final int TOP 	= 5;

    /**
    * This value indicates that each subcomponents
    * should be bottom-justified. 
    * @since   JDK1.0
    */
    public static final int BOTTOM 	= 6;

    // subalignment
    int subAlign = CENTER;

    /**
     * <code>align</code> is the property that determines
     * how each row distributes empty space.
     * It can be one of the following values:
     * <ul>
     * <code>LEFT</code>
     * <code>RIGHT</code>
     * <code>CENTER</code>
     * <code>LEADING</code>
     * <code>TRAILING</code>
     * </ul>
     *
     * @serial
     * @see #getAlignment
     * @see #setAlignment
     */
    int align;          // This is for 1.1 serialization compatibility

    /**
     * <code>newAlign</code> is the property that determines
     * how each row distributes empty space for the Java 2 platform,
     * v1.2 and greater.
     * It can be one of the following three values:
     * <ul>
     * <code>LEFT</code>
     * <code>RIGHT</code>
     * <code>CENTER</code>
     * <code>LEADING</code>
     * <code>TRAILING</code>
     * </ul>
     *
     * @serial
     * @since 1.2
     * @see #getAlignment
     * @see #setAlignment
     */
    int newAlign;       // This is the one we actually use

    /**
     * The flow layout manager allows a seperation of
     * components with gaps.  The horizontal gap will
     * specify the space between components.
     *
     * @serial
     * @see getHgap
     * @see setHgap
     */
    int hgap;

    /**
     * The flow layout manager allows a seperation of
     * components with gaps.  The vertical gap will
     * specify the space between rows.
     *
     * @serial
     * @see getVgap
     * @see setVgap
     */
    int vgap;

    /*
     * JDK 1.1 serialVersionUID
     */
     private static final long serialVersionUID = -7262534875583282631L;

    /**
     * Constructs a new <code>HorizontalFlowLayout</code> with a centered alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public HorizontalFlowLayout() {
	this(CENTER, 5, 5);
    }

    /**
     * Constructs a new <code>HorizontalFlowLayout</code> with the specified
     * alignment and a default 5-unit horizontal and vertical gap.
     * The value of the alignment argument must be one of
     * <code>HorizontalFlowLayout.LEFT</code>, <code>HorizontalFlowLayout.RIGHT</code>,
     * or <code>HorizontalFlowLayout.CENTER</code>.
     * @param align the alignment value
     */
    public HorizontalFlowLayout(int align) {
	this(align, 5, 5);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * <code>HorizontalFlowLayout.LEFT</code>, <code>HorizontalFlowLayout.RIGHT</code>,
     * or <code>HorizontalFlowLayout.CENTER</code>.
     * @param      align   the alignment value
     * @param      hgap    the horizontal gap between components
     * @param      vgap    the vertical gap between components
     */
    public HorizontalFlowLayout(int align, int hgap, int vgap) {
	this.hgap = hgap;
	this.vgap = vgap;
        setAlignment(align);
    }
    
    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * <code>HorizontalFlowLayout.LEFT</code>, <code>HorizontalFlowLayout.RIGHT</code>,
     * or <code>HorizontalFlowLayout.CENTER</code>.
     * @param      align   the alignment value
     * @param      hgap    the horizontal gap between components
     * @param      vgap    the vertical gap between components
     * @param      subalign    the alignment of components within this (TOP/CENTER/BOTTOM)
     */
    public HorizontalFlowLayout(int align, int hgap, int vgap, int subAlign)
     {
        this.hgap = hgap;
	this.vgap = vgap;
        setAlignment(align);
        this.subAlign = subAlign;
    }

    /**
     * Gets the alignment for this layout.
     * Possible values are <code>HorizontalFlowLayout.LEFT</code>,
     * <code>HorizontalFlowLayout.RIGHT</code>, <code>HorizontalFlowLayout.CENTER</code>,
     * <code>HorizontalFlowLayout.LEADING</code>,
     * or <code>HorizontalFlowLayout.TRAILING</code>.
     * @return     the alignment value for this layout
     * @see        java.awt.HorizontalFlowLayout#setAlignment
     * @since      JDK1.1
     */
    public int getAlignment() {
	return newAlign;
    }

    /**
     * Sets the alignment for this layout.
     * Possible values are
     * <ul>
     * <li><code>HorizontalFlowLayout.LEFT</code>
     * <li><code>HorizontalFlowLayout.RIGHT</code>
     * <li><code>HorizontalFlowLayout.CENTER</code>
     * <li><code>HorizontalFlowLayout.LEADING</code>
     * <li><code>HorizontalFlowLayout.TRAILING</code>
     * </ul>
     * @param      align one of the alignment values shown above
     * @see        #getAlignment()
     * @since      JDK1.1
     */
    public void setAlignment(int align) {
	this.newAlign = align;

        // this.align is used only for serialization compatibility,
        // so set it to a value compatible with the 1.1 version
        // of the class

        switch (align) {
	case LEADING:
            this.align = LEFT;
	    break;
	case TRAILING:
            this.align = RIGHT;
	    break;
        default:
            this.align = align;
	    break;
        }
    }
    

    /**
    * Gets the sub-alignment for this layout.
    * Possible values are <code>HorizontalFlowLayout.TOP</code>,  
    * <code>HorizontalFlowLayout.BOTTOM</code>, or <code>HorizontalFlowLayout.CENTER</code>.  
    * @return     the sub-alignment value for this layout.
    * @see        HorizontalFlowLayout#setSubAlignment
    * @since      JDK1.1
    */
    public int getSubAlignment() {
        return subAlign;
    }

    /**
    * Sets the sub-alignment for this layout.
    * Possible values are <code>HorizontalFlowLayout.TOP</code>,  
    * <code>HorizontalFlowLayout.BOTTOM</code>, and <code>HorizontalFlowLayout.CENTER</code>.  
    * @param      subAlign the sub-alignment value.
    * @see        HorizontalFlowLayout#getSubAlignment
    * @since      JDK1.1
    */
    public void setSubAlignment(int subAlign) {
        this.subAlign = subAlign;
    }

    /**
     * Gets the horizontal gap between components.
     * @return     the horizontal gap between components
     * @see        java.awt.HorizontalFlowLayout#setHgap
     * @since      JDK1.1
     */
    public int getHgap() {
	return hgap;
    }

    /**
     * Sets the horizontal gap between components.
     * @param hgap the horizontal gap between components
     * @see        java.awt.HorizontalFlowLayout#getHgap
     * @since      JDK1.1
     */
    public void setHgap(int hgap) {
	this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components.
     * @return     the vertical gap between components
     * @see        java.awt.HorizontalFlowLayout#setVgap
     * @since      JDK1.1
     */
    public int getVgap() {
	return vgap;
    }

    /**
     * Sets the vertical gap between components.
     * @param vgap the vertical gap between components
     * @see        java.awt.HorizontalFlowLayout#getVgap
     * @since      JDK1.1
     */
    public void setVgap(int vgap) {
	this.vgap = vgap;
    }

    /**
     * Adds the specified component to the layout. Not used by this class.
     * @param name the name of the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout. Not used by
     * this class.
     * @param comp the component to remove
     * @see       java.awt.Container#removeAll
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Returns the preferred dimensions for this layout given the 
     * <i>visible</i> components in the specified target container.
     * @param target the component which needs to be laid out
     * @return    the preferred dimensions to lay out the
     *            subcomponents of the specified container
     * @see Container
     * @see #minimumLayoutSize
     * @see       java.awt.Container#getPreferredSize
     */
    public Dimension preferredLayoutSize(Container target) {
      synchronized (target.getTreeLock()) {
	Dimension dim = new Dimension(0, 0);
	int nmembers = target.getComponentCount();
        boolean firstVisibleComponent = true;

	for (int i = 0 ; i < nmembers ; i++) {
	    Component m = target.getComponent(i);
	    if (m.isVisible()) {
		Dimension d = m.getPreferredSize();
		dim.height = Math.max(dim.height, d.height);
                if (firstVisibleComponent) {
                    firstVisibleComponent = false;
                } else {
                    dim.width += hgap;
                }
		dim.width += d.width;
	    }
	}
	Insets insets = target.getInsets();
	dim.width += insets.left + insets.right + hgap*2;
	dim.height += insets.top + insets.bottom + vgap*2;
	return dim;
      }
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i>
     * components contained in the specified target container.
     * @param target the component which needs to be laid out
     * @return    the minimum dimensions to lay out the
     *            subcomponents of the specified container
     * @see #preferredLayoutSize
     * @see       java.awt.Container
     * @see       java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container target) {
      synchronized (target.getTreeLock()) {
	Dimension dim = new Dimension(0, 0);
	int nmembers = target.getComponentCount();

	for (int i = 0 ; i < nmembers ; i++) {
	    Component m = target.getComponent(i);
	    if (m.isVisible()) {
		Dimension d = m.getMinimumSize();
		dim.height = Math.max(dim.height, d.height);
		if (i > 0) {
		    dim.width += hgap;
		}
		dim.width += d.width;
	    }
	}
	Insets insets = target.getInsets();
	dim.width += insets.left + insets.right + hgap*2;
	dim.height += insets.top + insets.bottom + vgap*2;
	return dim;
      }
    }

    /**
     * Centers the elements in the specified row, if there is any slack.
     * @param target the component which needs to be moved
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width dimensions
     * @param height the height dimensions
     * @param rowStart the beginning of the row
     * @param rowEnd the the ending of the row
     */
    private void moveComponents(Container target, int x, int y, int width, int height,
                                int rowStart, int rowEnd, boolean ltr) {
      synchronized (target.getTreeLock()) {
	switch (newAlign) {
	case LEFT:
	    x += ltr ? 0 : width;
	    break;
	case CENTER:
	    x += width / 2;
	    break;
	case RIGHT:
	    x += ltr ? width : 0;
	    break;
	case LEADING:
	    break;
	case TRAILING:
	    x += width;
	    break;
	}
	for (int i = rowStart ; i < rowEnd ; i++) {
	    Component m = target.getComponent(i);
	    if (m.isVisible()) {
//	        if (ltr) {
//        	    m.setLocation(x, y + (height - m.getWidth()) / 2);
//	        } else {
//	            m.setLocation(target.getWidth() - x - m.getWidth(), y + (height - m.getHeight()) / 2);
//                }
//                x += m.getWidth() + hgap;
                int dy;
                switch (subAlign) {
                    case CENTER:
                        
                        dy = (height - m.getSize().height) / 2; 
                        break;
                    case TOP:
                        dy = 0; break;
                    case BOTTOM:
                        dy = height - m.getSize().height; break;
                    default:
                        throw new IllegalStateException("subAlign = "+ subAlign);
                }
                m.setLocation(x,  y+dy);
                x += hgap + m.getSize().width;
	    }
	}
      }
    }

    /**
     * Lays out the container. This method lets each component take
     * its preferred size by reshaping the components in the
     * target container in order to satisfy the alignment of
     * this <code>HorizontalFlowLayout</code> object.
     * @param target the specified component being laid out
     * @see Container
     * @see       java.awt.Container#doLayout
     */
    public void layoutContainer(Container target) {
      synchronized (target.getTreeLock()) {
	Insets insets = target.getInsets();
	int maxwidth = target.getWidth() - (insets.left + insets.right + hgap*2);
	int nmembers = target.getComponentCount();
	int x = 0, y = insets.top;// + vgap;
	int rowh = 0, start = 0;

        boolean ltr = target.getComponentOrientation().isLeftToRight();

	for (int i = 0 ; i < nmembers ; i++) {
	    Component m = target.getComponent(i);
	    if (m.isVisible()) {
		Dimension d = m.getPreferredSize();
		m.setSize(d.width, d.height);

		if ((x == 0) || ((x + d.width) <= maxwidth)) {
		    if (x > 0) {
			x += hgap;
		    }
		    x += d.width;
		    rowh = Math.max(rowh, d.height);
		} else {
		    moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh, start, i, ltr);
		    x = d.width;
		    y += rowh;//vgap + rowh;
		    rowh = d.height;
		    start = i;
		}
	    }
	}
	moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh, start, nmembers, ltr);
      }
    }

    //
    // the internal serial version which says which version was written
    // - 0 (default) for versions before the Java 2 platform, v1.2
    // - 1 for version >= Java 2 platform v1.2, which includes "newAlign" field
    //
    private static final int currentSerialVersion = 1;
    /**
     * This represent the <code>currentSerialVersion</code>
     * which is bein used.  It will be one of two values :
     * <code>0</code> versions before Java 2 platform v1.2..
     * <code>1</code> versions after  Java 2 platform v1.2..
     *
     * @serial
     * @since 1.2
     */
    private int serialVersionOnStream = currentSerialVersion;

    /**
     * Reads this object out of a serialization stream, handling
     * objects written by older versions of the class that didn't contain all
     * of the fields we use now..
     */
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();

        if (serialVersionOnStream < 1) {
            // "newAlign" field wasn't present, so use the old "align" field.
            setAlignment(this.align);
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Returns a string representation of this <code>HorizontalFlowLayout</code>
     * object and its values.
     * @return     a string representation of this layout
     */
    public String toString() {
	String str = "";
	switch (align) {
	  case LEFT:        str = ",align=left"; break;
	  case CENTER:      str = ",align=center"; break;
	  case RIGHT:       str = ",align=right"; break;
	  case LEADING:     str = ",align=leading"; break;
	  case TRAILING:    str = ",align=trailing"; break;
	}
	return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + "]";
    }


}

