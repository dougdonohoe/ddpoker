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
 * DDHtmlArea.java
 *
 * Created on November 16, 2002, 4:06 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

/**
 * @author Doug Donohoe
 */
public class DDHtmlArea extends JEditorPane implements DDTextVisibleComponent
{
    static Logger logger = LogManager.getLogger(DDHtmlArea.class);

    private DDHtmlEditorKit htmlKit_;
    private Caret cNormal_;
    private final Caret cNothing_ = new DoNothingCaret();

    /**
     *
     */
    public DDHtmlArea()
    {
        this(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }

    public DDHtmlArea(String sName)
    {
        this(sName, GuiManager.DEFAULT);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDHtmlArea(String sName, String sStyleName)
    {
        this(sName, sStyleName, null);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDHtmlArea(String sName, String sStyleName, String sBevelStyle)
    {
        super();
        init(sName, sStyleName, sBevelStyle, null);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDHtmlArea(String sName, String sStyleName, String sBevelStyle, DDHtmlArea styleSheetProto)
    {
        super();
        init(sName, sStyleName, sBevelStyle, styleSheetProto);
    }

    /**
     * init colors, borders, etc
     */
    private void init(String sName, String sStyleName, String sBevelStyle, DDHtmlArea styleSheetProto)
    {
        StyleSheet proto = null;
        if (styleSheetProto != null)
        {
            proto = styleSheetProto.htmlKit_.getStyleSheet();
        }
        htmlKit_ = new DDHtmlEditorKit(proto);
        setEditorKit(htmlKit_);
        GuiManager.init(this, sName, sStyleName);
        setDisplayOnly(true);
        if (sBevelStyle == null)
        {
            setBorder(DDTextField.TEXTBORDER);
        }
        else
        {
            setBorder(DDTextField.createTextBorder(sBevelStyle));
        }
        cNormal_ = getCaret();
        if (proto == null)
        {
            setStyles();
        }

        if (Utils.ISMAC) JTextComponent.loadKeymap(getKeymap(), GuiUtils.MAC_CUT_COPY_PASTE, getActions());
    }

    /**
     * Set font/color from this widget into HTML style sheet
     */
    private void setStyles()
    {
        StyleSheet sheet = htmlKit_.getStyleSheet();
        Font font = getFont();
        Color bg = getForeground();

        // create rule like body { font-family: Lucida Sans Regular; font-size: 12pt; color: #ffffff}
        // which sets font for entire HTML body
        StringBuilder sb = new StringBuilder();
        sb.append("body {");
        sb.append("font-family: ");
        sb.append(font.getFamily());
        sb.append("; ");
        sb.append("font-size: ");
        sb.append(font.getSize());
        sb.append("; color: ");
        sb.append(Utils.getHtmlColor(bg));
        sb.append("}");
        // BUG 256 - catch NPE to handle bad fonts (typically on Mac)
        // need to see if there is a way to do HTML font without using
        // style sheets.  Sigh.
        try
        {
            sheet.addRule(sb.toString());
        }
        catch (NullPointerException npe)
        {
            logger.warn("Caught NPE trying to add rule: {}", sb.toString());
            logger.warn(Utils.formatExceptionText(npe));
        }
    }

    /**
     * Return our type
     */
    public String getType()
    {
        return "htmlarea";
    }

    @Override
    public void setText(String s)
    {
        GuiUtils.requireSwingThread();

        super.setText(s);
    }

    /**
     * Set this html area as a display area that:
     * can't take focus, is not opaque and can't drag.
     * Set to true by default.
     */
    public void setDisplayOnly(boolean bDisplayOnly)
    {
        setFocusable(!bDisplayOnly);
        setOpaque(!bDisplayOnly);
        setEditable(!bDisplayOnly);
        setDragEnabled(!bDisplayOnly);
        if (bDisplayOnly)
        {
            setCaret(cNothing_);
        }
        else
        {
            setCaret(cNormal_);
        }
    }

    @Override
    public void setForeground(Color c)
    {
        super.setForeground(c);
        this.setCaretColor(c);
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    @Override
    public void repaint()
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, 0, 0, foo);
            foo.repaint(pRepaint.x, pRepaint.y, getWidth(), getHeight());
        }
        else
        {
            super.repaint();
        }
    }

    private boolean skipNextRepaint = false;

    public void setSkipNextRepaint(boolean skipNextRepaint)
    {
        this.skipNextRepaint = skipNextRepaint;
    }

    /**
     * Override to honor skipNextRepaint
     */
    @Override
    public void paintComponent(Graphics g1)
    {
        if (skipNextRepaint)
        {
            skipNextRepaint = false;
            return;
        }
        super.paintComponent(g1);
    }

    /**
     * This class overrides caret so that no painting is done -- needed
     * for text areas where opaque=false and their parent has
     * a semi-transparent background.  Fixs a swing bug.
     */
    private static class DoNothingCaret extends javax.swing.text.DefaultCaret
    {
        @Override
        public void paint(Graphics g)
        {
        }

        @Override
        protected synchronized void damage(Rectangle r)
        {
        }

        // override behavior where changing text adjusts a parent's visibility
        @Override
        protected void adjustVisibility(Rectangle nloc)
        {
        }
    }
}
