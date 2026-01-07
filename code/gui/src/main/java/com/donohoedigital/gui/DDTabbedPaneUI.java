/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 * DDTabbedPaneUI.java
 *
 * Created on June 16, 2003, 8:55 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import javax.swing.text.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDTabbedPaneUI extends MetalTabbedPaneUI 
{
    //static Logger logger = LogManager.getLogger(DDTabbedPaneUI.class);
    
    DDTabbedPane tab_;
    DDBevelBorder bevel_;

    /**
     * Creates a new instance of DDTabedPaneUI 
     */
    public DDTabbedPaneUI(DDTabbedPane tab, String sBevelStyle) {
        tab_ = tab;
        if (sBevelStyle != null) bevel_ = new DDBevelBorder(sBevelStyle, DDBevelBorder.RAISED); // used for colors
    }
    
    
    /**
     * Override to set highlight color
     */
    protected void installDefaults()
    {
        super.installDefaults();

        selectColor = tab_.getSelectedTabColor();

        if (bevel_ != null)
        {
            highlight = bevel_.getHighlightInnerColor();
            lightHighlight = bevel_.getHighlightOuterColor();
            selectHighlight = lightHighlight;
            shadow = bevel_.getShadowInnerColor();
            darkShadow = bevel_.getShadowOuterColor();
        }

        if (tab_.getTabPlacement() == JTabbedPane.BOTTOM || tab_.getTabPlacement() == JTabbedPane.TOP)
        {
            selectedTabPadInsets = new Insets(0, 0, 0, 10);
        }
        else
        {
            selectedTabPadInsets = new Insets(2, 0, 2, 7);
        }
        tabAreaInsets = selectedTabPadInsets;
        tabInsets = selectedTabPadInsets;
        if (tab_.getTabPlacement() == JTabbedPane.LEFT)
        {
            contentBorderInsets = new Insets(2,2,2,2);

        }

    }
    
    /**
     * Override to not paint background
     */
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) 
    {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets insets = tabPane.getInsets();

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        switch(tabPlacement) {
          case LEFT:
              x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
              w -= (x - insets.left);
              break;
          case RIGHT:
              w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
              break;            
          case BOTTOM: 
              h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
              break;
          case TOP:
          default:
              y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
              h -= (y - insets.top);
        } 
//	// Fill region behind content area
//        if (selectedColor == null) {
//            g.setColor(tabPane.getBackground());
//        }
//        else {
//            g.setColor(selectedColor);
//        }
//	//g.fillRect(x,y,w,h);

        paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h); 
        paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    // can't find the logic for the gap, so hack it here (to paint bg, border all the way to content)
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected)
    {
        if (tabPlacement == LEFT) w += 2;
        if (tabPlacement == LEFT && tabIndex == tab_.getTabCount() - 1) h += 2;
        super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
    }

    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected)
    {
        if (tabPlacement == LEFT) w += 2;
        if (tabPlacement == LEFT && tabIndex == tab_.getTabCount() - 1) h += 2;
        super.paintTabBorder(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
    }

    /**
     * Override to left-justify label
     */
    protected void layoutLabel(int tabPlacement, 
                               FontMetrics metrics, int tabIndex,
                               String title, Icon icon,
                               Rectangle tabRect, Rectangle iconRect, 
                               Rectangle textRect, boolean isSelected ) {
        textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            tabPane.putClientProperty("html", v);
        }

        SwingUtilities.layoutCompoundLabel(tabPane,
                                           metrics, title, icon,
                                           SwingUtilities.CENTER,
                                           tabPlacement == JTabbedPane.LEFT ? SwingUtilities.LEFT : SwingUtilities.CENTER,//JDD
                                           SwingUtilities.CENTER,
                                           SwingUtilities.TRAILING,
                                           tabRect,
                                           iconRect,
                                           textRect,
                                           textIconGap);

	    tabPane.putClientProperty("html", null);

        int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;
    }

    /**
     * left tab text adjust
     */
    protected int getTabLabelShiftX( int tabPlacement, int tabIndex, boolean isSelected )
    {
        if (tabPlacement == JTabbedPane.LEFT) return 8;
        return 0;
    }

    protected void paintText(Graphics g1, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected)
    {
        Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (tab_.isAlwaysAntiAlias() || GuiUtils.drawAntiAlias(tab_))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);

    }

    /**
     * Override to not paint focus
     */
    protected void paintFocusIndicator(Graphics g1, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected)
    {
        if ( tab_.hasFocus() && isSelected )
        {
            Graphics2D g = (Graphics2D) g1;
            Color focus = tab_.getForeground();
            focus = new Color(focus.getRed(), focus.getGreen(), focus.getBlue(), 150);
            g.setColor(focus);
            int xgap = tabPlacement == LEFT ? 1 : 2;
            int ygap = tabPlacement == LEFT ? -1 : 1;
            int wadj = tabPlacement == LEFT ? -1 : 0;
            Stroke old = g.getStroke();
            g.setStroke(DDButtonUI.FOCUS_STROKE);
            Rectangle focusRect = textRect;
            g.drawRect((focusRect.x-xgap), (focusRect.y-ygap), focusRect.width+(xgap*2)-wadj, focusRect.height+(ygap));
            g.setStroke(old);
        }
    }
}
