/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
package com.donohoedigital.gui;

import java.awt.*;

/**
 * A vertical flow layout where each component assumes its natural (preferred) size.
 */
public class VerticalFlowLayout implements LayoutManager, java.io.Serializable {

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;
    public static final int TOP = 3;
    public static final int BOTTOM = 4;

    int align;
    int subAlign;
    int hgap;
    int vgap;

    public VerticalFlowLayout() {
        this(CENTER, 5, 5, CENTER);
    }

    public VerticalFlowLayout(int align) {
        this(align, 5, 5, CENTER);
    }

    public VerticalFlowLayout(int align, int hgap, int vgap) {
        this(align, hgap, vgap, CENTER);
    }

    public VerticalFlowLayout(int align, int hgap, int vgap, int subAlign) {
        this.align = align;
        this.hgap = hgap;
        this.vgap = vgap;
        this.subAlign = subAlign;
    }

    public int getAlignment() {
        return align;
    }

    public void setAlignment(int align) {
        this.align = align;
    }

    public int getSubAlignment() {
        return subAlign;
    }

    public void setSubAlignment(int subAlign) {
        this.subAlign = subAlign;
    }

    public int getHgap() {
        return hgap;
    }

    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    public int getVgap() {
        return vgap;
    }

    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    dim.width = Math.max(dim.width, d.width);
                    if (i > 0) {
                        dim.height += vgap;
                    }
                    dim.height += d.height;
                }
            }
            Insets insets = target.getInsets();
            dim.height += insets.top + insets.bottom + vgap * 2;
            dim.width += insets.left + insets.right + hgap * 2;
            return dim;
        }
    }

    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getMinimumSize();
                    dim.width = Math.max(dim.width, d.width);
                    if (i > 0) {
                        dim.height += vgap;
                    }
                    dim.height += d.height;
                }
            }
            Insets insets = target.getInsets();
            dim.height += insets.top + insets.bottom + vgap * 2;
            dim.width += insets.left + insets.right + hgap * 2;
            return dim;
        }
    }

    private void moveComponents(Container target, int x, int y, int width, int height, int columnStart, int columnEnd) {
        synchronized (target.getTreeLock()) {
            switch (align) {
                case TOP:
                    break;
                case CENTER:
                    y += height / 2;
                    break;
                case BOTTOM:
                    y += height;
                    break;
            }
            for (int i = columnStart; i < columnEnd; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    int dx;
                    switch (subAlign) {
                        case CENTER:
                            dx = (width - m.getSize().width) / 2;
                            break;
                        case LEFT:
                            dx = 0;
                            break;
                        case RIGHT:
                            dx = width - m.getSize().width;
                            break;
                        default:
                            throw new IllegalStateException("subAlign = " + subAlign);
                    }
                    m.setLocation(x + dx, y);
                    y += vgap + m.getSize().height;
                }
            }
        }
    }

    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxheight = target.getSize().height - (insets.top + insets.bottom + vgap * 2);
            int nmembers = target.getComponentCount();
            int y = 0, x = insets.left + hgap;
            int columnw = 0, start = 0;

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    m.setSize(d.width, d.height);

                    if ((y == 0) || ((y + d.height) <= maxheight)) {
                        if (y > 0) {
                            y += vgap;
                        }
                        y += d.height;
                        columnw = Math.max(columnw, d.width);
                    } else {
                        moveComponents(target, x, insets.top + vgap, columnw, maxheight - y, start, i);
                        y = d.height;
                        x += hgap + columnw;
                        columnw = d.width;
                        start = i;
                    }
                }
            }
            moveComponents(target, x, insets.top + vgap, columnw, maxheight - y, start, nmembers);
        }
    }

    public String toString() {
        String str = "";
        switch (align) {
            case TOP:
                str = ",align=top";
                break;
            case CENTER:
                str = ",align=center";
                break;
            case BOTTOM:
                str = ",align=bottom";
                break;
        }
        String subStr = "";
        switch (subAlign) {
            case LEFT:
                subStr = ",subAlign=left";
                break;
            case CENTER:
                subStr = ",subAlign=center";
                break;
            case RIGHT:
                subStr = ",subAlign=right";
                break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + subAlign + "]";
    }
}

