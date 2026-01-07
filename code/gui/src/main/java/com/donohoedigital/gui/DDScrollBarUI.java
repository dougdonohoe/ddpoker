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
package com.donohoedigital.gui;

import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2005
 * Time: 8:25:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDScrollBarUI extends MetalScrollBarUI
{
    static Logger logger = LogManager.getLogger(DDScrollBarUI.class);

    // we override paintThumb() because metal stores its thumb
    // colors statically.
    private DDMetalBumps ddbumps_;
    private Color thumbColor;
    private Color thumbShadow;
    private Color thumbHighlightColor;

    /**
     * Our own scrollbar ui
     */
    public DDScrollBarUI()
    {
        super();
    }

    /**
     * override to ensure buttons are around when
     * configureScrollBarColors is called (JDK1.5 change)
     */
    protected void installDefaults()
    {
        createButtons();
        super.installDefaults();
    }

    /**
     * Use our own colors
     */
    protected void configureScrollBarColors()
    {
        // set colors based on background
        thumbColor = scrollbar.getBackground();
        if (thumbColor == null) thumbColor = Color.gray;
        thumbShadow = thumbColor.darker();
        thumbHighlightColor = thumbColor.brighter();

        // our bumps
        ddbumps_ = new DDMetalBumps( 10, 10, thumbHighlightColor, thumbShadow, thumbColor );

        // configure colors
        super.configureScrollBarColors();
        setForeground(scrollbar.getForeground());
        setBackground(scrollbar.getBackground());
    }

    /**
     * Override to not paint track
     */
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
    {
    }

    /**
     * override to set thumb size to full if empty
     */
    protected void setThumbBounds(int x, int y, int width, int height)
    {
        // if vertical and no thumb needed, we make it full size
        if (height == 0 && scrollbar.getOrientation() == JScrollBar.VERTICAL)
        {
            Dimension sbSize = scrollbar.getSize();
            Insets sbInsets = scrollbar.getInsets();
            int decrButtonH = decrButton.getPreferredSize().height;
            int incrButtonH = incrButton.getPreferredSize().height;
            int sbInsetsH = sbInsets.top + sbInsets.bottom;
            int sbButtonsH = decrButtonH + incrButtonH;
            float trackH = sbSize.height - (sbInsetsH + sbButtonsH);

            y = decrButtonH;
            height = (int) trackH;
            width = scrollbar.getWidth();
        }
        // ditto horizontal
        else if (width == 0 && scrollbar.getOrientation() == JScrollBar.HORIZONTAL)
        {
            Dimension sbSize = scrollbar.getSize();
            Insets sbInsets = scrollbar.getInsets();
            int decrButtonW = decrButton.getPreferredSize().width;
            int incrButtonW = incrButton.getPreferredSize().width;
            int sbInsetsW = sbInsets.left + sbInsets.right;
            int sbButtonsW = decrButtonW + incrButtonW;
            float trackW = sbSize.width - (sbInsetsW + sbButtonsW);

            x = decrButtonW;
            width = (int) trackW;
            height = scrollbar.getHeight();
        }

        super.setThumbBounds(x,y,width,height);
    }
    /**
     * Override to paint full thumb when no thumb needed.  Moved to this
     * method for JDK1.5 (makes more sense here anyway).
     */
    protected void paintThumb( Graphics g, JComponent c, Rectangle tb )
    {
        // actual paint logic
        superPaintThumb(g, c, tb);

        // if sb has focus, overlay focus color
        if (scrollbar.hasFocus())
        {
            Color focus = ((DDScrollBar) scrollbar).getThumbFocusColor();
            if (focus != null)
            {
                g.setColor(focus);
                g.fillRect(tb.x+2,  tb.y+2, tb.width-5, tb.height-4);
            }
        }
	}

    /**
     * Copied verbatim from MetalScrollBarUI because they use static
     * colors for thumb, which is dumb
     */
    protected void superPaintThumb(Graphics g, JComponent c, Rectangle thumbBounds )
    {
        if (!c.isEnabled()) {
            return;
        }

            boolean leftToRight = true;

            g.translate( thumbBounds.x, thumbBounds.y );

        if ( scrollbar.getOrientation() == JScrollBar.VERTICAL )
        {
            if ( !isFreeStanding ) {
                    if ( !leftToRight ) {
                        thumbBounds.width += 1;
                        g.translate( -1, 0 );
            } else {
                        thumbBounds.width += 2;
                    }

            }

            g.setColor( thumbColor );
            g.fillRect( 0, 0, thumbBounds.width - 2, thumbBounds.height - 1 );

            g.setColor( thumbShadow );
            g.drawRect( 0, 0, thumbBounds.width - 2, thumbBounds.height - 1 );

            g.setColor( thumbHighlightColor );
            g.drawLine( 1, 1, thumbBounds.width - 3, 1 );
            g.drawLine( 1, 1, 1, thumbBounds.height - 2 );

            ddbumps_.setBumpArea( thumbBounds.width - 6, thumbBounds.height - 7 );
            ddbumps_.paintIcon( c, g, 3, 4 );

            if ( !isFreeStanding ) {
                    if ( !leftToRight ) {
                        thumbBounds.width -= 1;
                        g.translate( 1, 0 );
            } else {
                        thumbBounds.width -= 2;
                    }
            }
        }
        else  // HORIZONTAL
        {
            if ( !isFreeStanding ) {
                thumbBounds.height += 2;
            }

            g.setColor( thumbColor );
            g.fillRect( 0, 0, thumbBounds.width - 1, thumbBounds.height - 2 );

            g.setColor( thumbShadow );
            g.drawRect( 0, 0, thumbBounds.width - 1, thumbBounds.height - 2 );

            g.setColor( thumbHighlightColor );
            g.drawLine( 1, 1, thumbBounds.width - 3, 1 );
            g.drawLine( 1, 1, 1, thumbBounds.height - 3 );

            ddbumps_.setBumpArea( thumbBounds.width - 7, thumbBounds.height - 6 );
            ddbumps_.paintIcon( c, g, 4, 3 );

            if ( !isFreeStanding ) {
                thumbBounds.height -= 2;
            }
        }

        g.translate( -thumbBounds.x, -thumbBounds.y );
    }

    /**
     * create buttons early so they are around when
     * we set colors (JDK1.5 change - they changed
     * order of creation)
     */
    protected void createButtons()
    {
       switch (scrollbar.getOrientation()) {
       case JScrollBar.VERTICAL:
           createIncreaseButton(SOUTH);
           createDecreaseButton(NORTH);
           break;

       case JScrollBar.HORIZONTAL:
           if (scrollbar.getComponentOrientation().isLeftToRight()) {
               createIncreaseButton(EAST);
               createDecreaseButton(WEST);
           } else {
               createIncreaseButton(WEST);
               createDecreaseButton(EAST);
           }
           break;
       }
   }

    /**
     * Create our own button
     */
    protected JButton createDecreaseButton(int orientation)
    {
        if (decrButton == null)
        {
            decrButton = new ScrollButton(orientation);
        }
        return decrButton;
    }

    /**
     * create our own button
     */
    protected JButton createIncreaseButton(int orientation)
    {
        if (incrButton == null)
        {
            incrButton = new ScrollButton(orientation);
        }
        return incrButton;
    }

    /**
     *  Set foreground color of incr/decr buttons
     */
    private void setForeground(Color c)
    {
        incrButton.setForeground(c);
        decrButton.setForeground(c);
    }

    /**
     *  Set background color of incr/decr buttons
     */
    private void setBackground(Color c)
    {
        incrButton.setBackground(c);
        decrButton.setBackground(c);
    }

    /**
     * incr/decr buttons
     */
    private class ScrollButton extends JButton
    {
        int orientation;

        ScrollButton(int orientation)
        {
            super();
            setPreferredSize(new Dimension(11,11));
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            this.orientation = orientation;
        }

        public void paint(Graphics g1)
        {
            Graphics2D g = (Graphics2D) g1;
            ButtonModel model = getModel();

            GeneralPath pp = getPath();

            Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            Shape s = pp;
            AffineTransform tx = new AffineTransform();
            if (model.isArmed())
            {
                tx.translate(1,1);
            }

            float center = 5.5f; // based on center of 12x12 path drawn below
            switch (orientation)
            {
                case NORTH:
                    tx.rotate(-Math.PI, center, center);
                    break;

                case SOUTH:
                    // path drawn in this orientation
                    break;

                case WEST:
                    tx.rotate(Math.PI/2, center, center);
                    break;

                case EAST:
                    tx.rotate(-Math.PI/2, center, center);
                    break;
            }

            s = pp.createTransformedShape(tx);
            g.setColor(getForeground());
            g.fill(s);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
        }

        /**
         * Swing doesn't exactly do semi-transparent correctly unless
         * you start with the hightest parent w/ no transparency
         */
        public void repaint(long tm, int x, int y, int width, int height)
        {
            Component foo = GuiUtils.getSolidRepaintComponent(this);
            if (foo != null && foo != this)
            {
                Point pRepaint = SwingUtilities.convertPoint(this, x, y, foo);
                foo.repaint(pRepaint.x, pRepaint.y, width, height);
                return;
            }

            super.repaint(tm, x, y, width, height);
        }
    }

    /**
     * path for arrow
     */
    private static GeneralPath path_;

    private static GeneralPath getPath()
    {
        if (path_ == null)
        {
            // based on a 12x12 grid
            path_ = new GeneralPath();
            path_.moveTo(0,1);
            path_.lineTo(11,1);
            path_.lineTo(5.5f,11);
            path_.closePath();
        }
        return path_;
    }
}
