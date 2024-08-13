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
 * DDInternalFrameTitlePane.java
 *
 * Created on August 21, 2003, 3:32 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.config.*;

import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 *
 * @author  Doug Donohoe
 */
public class DDInternalFrameTitlePane extends MetalInternalFrameTitlePane 
{    
    InternalDialog dialog_;
    
    private static Color modaltitle_ = StylesConfig.getColor("modal.title");
    
    /** Creates a new instance of DDInternalFrameTitlePane */
    public DDInternalFrameTitlePane(InternalDialog f) 
    {
        super(f);
        dialog_ = f;
    }

    public void paintComponent(Graphics g)
    {
        if(isPalette) {
            paintPalette(g);
            return;
        }

        boolean leftToRight = true;
        boolean isSelected = frame.isSelected();

        int width = getWidth();
        int height = getHeight();

        Color background = null;
        Color foreground = null;
        Color shadow = null;

        if (isSelected)
        {
            if (dialog_.isModal())
            {
                background = modaltitle_;
            }
            else
            {
                background = MetalLookAndFeel.getPrimaryControl();
            }
            foreground = Color.black;
        } else {
            background = MetalLookAndFeel.getControl();
            foreground = Color.black;
        }

        g.setColor(background);
        g.fillRect(0, 0, width, height);

        g.setColor( shadow );
        g.drawLine ( 0, height - 1, width, height -1);
        g.drawLine ( 0, 0, 0 ,0);
        g.drawLine ( width - 1, 0 , width -1, 0);


        int titleLength = 0;
        int xOffset = leftToRight ? 5 : width - 5;
        String frameTitle = frame.getTitle();

        Icon icon = frame.getFrameIcon();
        if ( icon != null ) {
            if( !leftToRight )
                xOffset -= icon.getIconWidth();
            int iconY = ((height / 2) - (icon.getIconHeight() /2));
            icon.paintIcon(frame, g, xOffset, iconY);
            xOffset += leftToRight ? icon.getIconWidth() + 5 : -5;
        }

        if(frameTitle != null) {
            Font f = getFont();
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();

            g.setColor(foreground);

            int yOffset = ( (height - fm.getHeight() ) / 2 ) + fm.getAscent();

            Rectangle rect = new Rectangle(0, 0, 0, 0);
            if (frame.isIconifiable()) { rect = iconButton.getBounds(); }
            else if (frame.isMaximizable()) { rect = maxButton.getBounds(); }
            else if (frame.isClosable()) { rect = closeButton.getBounds(); }
            int titleW;

            if( leftToRight ) {
              if (rect.x == 0) {
		rect.x = frame.getWidth()-frame.getInsets().right-2;
	      }
              titleW = rect.x - xOffset - 4;
              frameTitle = getTitle(frameTitle, fm, titleW);
            } else {
              titleW = xOffset - rect.x - rect.width - 4;
              frameTitle = getTitle(frameTitle, fm, titleW);
              xOffset -= SwingUtilities.computeStringWidth(fm, frameTitle);
            }

            titleLength = SwingUtilities.computeStringWidth(fm, frameTitle);
            g.drawString( frameTitle, xOffset, yOffset );
            xOffset += leftToRight ? titleLength + 5  : -5;
        }

        Color bumpShadow = background.darker();
        Color bumpHilite = background.brighter();

        int buttonsWidth = getButtonsWidth();

        // our bumps
        DDMetalBumps bumps = new DDMetalBumps( 10, 10, bumpHilite, bumpShadow, background );
        int bumpXOffset;
        int bumpLength;
        if( leftToRight ) {
            bumpLength = width - buttonsWidth - xOffset - 5;
            bumpXOffset = xOffset;
        } else {
            bumpLength = xOffset - buttonsWidth - 5;
            bumpXOffset = buttonsWidth + 5;
        }
        int bumpYOffset = 3;
        int bumpHeight = getHeight() - (2 * bumpYOffset);
        bumps.setBumpArea( bumpLength, bumpHeight );
        bumps.paintIcon(this, g, bumpXOffset, bumpYOffset);
    }

    protected void installDefaults()
    {
        super.installDefaults();
        maxIcon = new InternalFrameMaximizeIcon(16);
        minIcon = new InternalFrameAltMaximizeIcon(16);
        iconIcon = new InternalFrameMinimizeIcon(16);
        closeIcon = new InternalFrameCloseIcon(16);
    }

   public int getButtonsWidth()
   {
        boolean leftToRight = true;

        int w = getWidth();
        int x = leftToRight ? w : 0;
        int spacing;

        // assumes all buttons have the same dimensions
        // these dimensions include the borders
        int buttonWidth = closeButton.getIcon().getIconWidth();

        if(frame.isClosable()) {
            if (isPalette) {
                spacing = 3;
                x += leftToRight ? -spacing -(buttonWidth+2) : spacing;
                if( !leftToRight ) x += (buttonWidth+2);
            } else {
                spacing = 4;
                x += leftToRight ? -spacing -buttonWidth : spacing;
                if( !leftToRight ) x += buttonWidth;
            }
        }

        if(frame.isMaximizable() && !isPalette ) {
            spacing = frame.isClosable() ? 10 : 4;
            x += leftToRight ? -spacing -buttonWidth : spacing;
            if( !leftToRight ) x += buttonWidth;
        }

        if(frame.isIconifiable() && !isPalette ) {
            spacing = frame.isMaximizable() ? 2
                      : (frame.isClosable() ? 10 : 4);
            x += leftToRight ? -spacing -buttonWidth : spacing;
            if( !leftToRight ) x += buttonWidth;
        }

        return leftToRight ? w - x : x;
    }

    // Internal Frame Close code
    private class InternalFrameCloseIcon implements Icon, UIResource, Serializable 
    {
        int iconSize = 16;

            public InternalFrameCloseIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) 
        {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor = 
            MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor = 
            MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            Color xLightHighlightColor = MetalLookAndFeel.getWhite();
            Color boxLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the inactive window
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) 
                {
                    internalBackgroundColor =
                    MetalLookAndFeel.getControlShadow();
                    xLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if pressed
            else if (buttonModel.isPressed() && buttonModel.isArmed()) 
            {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                xLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            // Some calculations that are needed more than once later on.
            int oneHalf = (int)(iconSize / 2); // 16 -> 8

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // fill inside of box area
            g.setColor(internalBackgroundColor);
            g.fillRect(3,3, iconSize-6,iconSize-6);

            // THE BOX
            // the top/left dark higlight - some of this will get overwritten
            g.setColor(darkHighlightColor);
            g.drawRect(1,1, iconSize-3,iconSize-3);
            // draw the inside bottom/right highlight
            g.drawRect(2,2, iconSize-5,iconSize-5);
            // draw the light/outside, bottom/right highlight
            g.setColor(boxLightHighlightColor);
            g.drawRect(2,2, iconSize-3,iconSize-3);
            // draw the "normal" box
            g.setColor(mainItemColor);
            g.drawRect(2,2, iconSize-4,iconSize-4);
            g.drawLine(3,iconSize-3, 3,iconSize-3); // lower left
            g.drawLine(iconSize-3,3, iconSize-3,3); // up right

            // THE "X"
            // Dark highlight
            g.setColor(darkHighlightColor);
            g.drawLine(4,5, 5,4); // far up left
            g.drawLine(4,iconSize-6, iconSize-6,4); // against body of "X"
            // Light highlight
            g.setColor(xLightHighlightColor);
            g.drawLine(6,iconSize-5, iconSize-5,6); // against body of "X"
              // one pixel over from the body
            g.drawLine(oneHalf,oneHalf+2, oneHalf+2,oneHalf);
              // bottom right
            g.drawLine(iconSize-5,iconSize-5, iconSize-4,iconSize-5);
            g.drawLine(iconSize-5,iconSize-4, iconSize-5,iconSize-4);
            // Main color
            g.setColor(mainItemColor);
              // Upper left to lower right
            g.drawLine(5,5, iconSize-6,iconSize-6); // g.drawLine(5,5, 10,10);
            g.drawLine(6,5, iconSize-5,iconSize-6); // g.drawLine(6,5, 11,10);
            g.drawLine(5,6, iconSize-6,iconSize-5); // g.drawLine(5,6, 10,11);
              // Lower left to upper right
            g.drawLine(5,iconSize-5, iconSize-5,5); // g.drawLine(5,11, 11,5);
            g.drawLine(5,iconSize-6, iconSize-6,5); // g.drawLine(5,10, 10,5);

            g.translate(-x, -y);
        }
        
        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }
    
    // Internal Frame Maximize code
    private class InternalFrameMaximizeIcon implements Icon, UIResource, Serializable 
    {
        protected int iconSize = 16;

        public InternalFrameMaximizeIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor = 
            MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor = 
            MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            // ul = Upper Left and lr = Lower Right
            Color ulLightHighlightColor = MetalLookAndFeel.getWhite();
            Color lrLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the internal frame is inactive
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) 
                {
                    internalBackgroundColor =
                    MetalLookAndFeel.getControlShadow();
                    ulLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if the button is pressed and the mouse is over it
            else if (buttonModel.isPressed() && buttonModel.isArmed()) 
            {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                ulLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // BOX drawing
            // fill inside the box
            g.setColor(internalBackgroundColor);
            g.fillRect(3,7, iconSize-10,iconSize-10);

            // light highlight
            g.setColor(ulLightHighlightColor);
            g.drawRect(3,7, iconSize-10,iconSize-10); // up,left
            g.setColor(lrLightHighlightColor);
            g.drawRect(2,6, iconSize-7,iconSize-7); // low,right
            // dark highlight
            g.setColor(darkHighlightColor);
            g.drawRect(1,5, iconSize-7,iconSize-7); // outer
            g.drawRect(2,6, iconSize-9,iconSize-9); // inner
            // main box
            g.setColor(mainItemColor);
            g.drawRect(2,6, iconSize-8,iconSize-8); // g.drawRect(2,6, 8,8);

            // ARROW drawing
            // dark highlight
            g.setColor(darkHighlightColor);
              // down,left to up,right - inside box
            g.drawLine(3,iconSize-5, iconSize-9,7);
              // down,left to up,right - outside box
            g.drawLine(iconSize-6,4, iconSize-5,3);
              // outside edge of arrow head
            g.drawLine(iconSize-7,1, iconSize-7,2);
              // outside edge of arrow head
            g.drawLine(iconSize-6,1, iconSize-2,1);
            // light highlight
            g.setColor(ulLightHighlightColor);
              // down,left to up,right - inside box
            g.drawLine(5,iconSize-4, iconSize-8,9);
            g.setColor(lrLightHighlightColor);
            g.drawLine(iconSize-6,3, iconSize-4,5); // outside box
            g.drawLine(iconSize-4,5, iconSize-4,6); // one down from this
            g.drawLine(iconSize-2,7, iconSize-1,7); // outside edge arrow head
            g.drawLine(iconSize-1,2, iconSize-1,6); // outside edge arrow head
            // main part of arrow
            g.setColor(mainItemColor);
            g.drawLine(3,iconSize-4, iconSize-3,2); // top edge of staff
            g.drawLine(3,iconSize-3, iconSize-2,2); // bottom edge of staff
            g.drawLine(4,iconSize-3, 5,iconSize-3); // highlights inside of box
            g.drawLine(iconSize-7,8, iconSize-7,9); // highlights inside of box
            g.drawLine(iconSize-6,2, iconSize-4,2); // top of arrow head
            g.drawRect(iconSize-3,3, 1,3); // right of arrow head

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class InternalFrameMaximizeIcon

    // Internal Frame Minimize code
    private class InternalFrameMinimizeIcon implements Icon, UIResource, Serializable 
    {
        int iconSize = 16;

        public InternalFrameMinimizeIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) 
        {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();


            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            if (dialog_.isModal()) backgroundColor = modaltitle_;
            Color internalBackgroundColor = 
            MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor = 
            MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            // ul = Upper Left and lr = Lower Right
            Color ulLightHighlightColor = MetalLookAndFeel.getWhite();
            Color lrLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the internal frame is inactive
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) 
                {
                    internalBackgroundColor =
                    MetalLookAndFeel.getControlShadow();
                    ulLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if the button is pressed and the mouse is over it
            else if (buttonModel.isPressed() && buttonModel.isArmed()) 
            {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                ulLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // BOX drawing
            // fill inside the box
            g.setColor(internalBackgroundColor);
            g.fillRect(4,11, iconSize-13,iconSize-13);
            // light highlight
            g.setColor(lrLightHighlightColor);
            g.drawRect(2,10, iconSize-10,iconSize-11); // low,right
            g.setColor(ulLightHighlightColor);
            g.drawRect(3,10, iconSize-12,iconSize-12); // up,left
            // dark highlight
            g.setColor(darkHighlightColor);
            g.drawRect(1,8, iconSize-10,iconSize-10); // outer
            g.drawRect(2,9, iconSize-12,iconSize-12); // inner
            // main box
            g.setColor(mainItemColor);
            g.drawRect(2,9, iconSize-11,iconSize-11);
            g.drawLine(iconSize-10,10, iconSize-10,10); // up right highlight
            g.drawLine(3,iconSize-3, 3,iconSize-3); // low left highlight

            // ARROW
            // do the shaft first
            g.setColor(mainItemColor);
            g.fillRect(iconSize-7,3, 3,5); // do a big block
            g.drawLine(iconSize-6,5, iconSize-3,2); // top shaft
            g.drawLine(iconSize-6,6, iconSize-2,2); // bottom shaft
            g.drawLine(iconSize-6,7, iconSize-3,7); // bottom arrow head

            // draw the dark highlight
            g.setColor(darkHighlightColor);
            g.drawLine(iconSize-8,2, iconSize-7,2); // top of arrowhead
            g.drawLine(iconSize-8,3, iconSize-8,7); // left of arrowhead
            g.drawLine(iconSize-6,4, iconSize-3,1); // top of shaft
            g.drawLine(iconSize-4,6, iconSize-3,6); // top,right of arrowhead

            // draw the light highlight
            g.setColor(lrLightHighlightColor);
            g.drawLine(iconSize-6,3, iconSize-6,3); // top
            g.drawLine(iconSize-4,5, iconSize-2,3); // under shaft
            g.drawLine(iconSize-7,8, iconSize-3,8); // under arrowhead
            g.drawLine(iconSize-2,8, iconSize-2,7); // right of arrowhead

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class In
    
    // Internal Frame Alternate Maximize code (actually, the un-maximize icon)
    private class InternalFrameAltMaximizeIcon implements Icon, UIResource, Serializable 
    {
        int iconSize = 16;

        public InternalFrameAltMaximizeIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor = 
            MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor = 
            MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            // ul = Upper Left and lr = Lower Right
            Color ulLightHighlightColor = MetalLookAndFeel.getWhite();
            Color lrLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the internal frame is inactive
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) {
                    internalBackgroundColor =
                    MetalLookAndFeel.getControlShadow();
                    ulLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if the button is pressed and the mouse is over it
            else if (buttonModel.isPressed() && buttonModel.isArmed()) 
            {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                ulLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // BOX
            // fill inside the box
            g.setColor(internalBackgroundColor);
            g.fillRect(3,6, iconSize-9,iconSize-9);

            // draw dark highlight color
            g.setColor(darkHighlightColor);
            g.drawRect(1,5, iconSize-8,iconSize-8);
            g.drawLine(1,iconSize-2, 1,iconSize-2); // extra pixel on bottom

            // draw lower right light highlight
            g.setColor(lrLightHighlightColor);
            g.drawRect(2,6, iconSize-7,iconSize-7);
            // draw upper left light highlight
            g.setColor(ulLightHighlightColor);
            g.drawRect(3,7, iconSize-9,iconSize-9);

            // draw the main box
            g.setColor(mainItemColor);
            g.drawRect(2,6, iconSize-8,iconSize-8);

            // Six extraneous pixels to deal with
            g.setColor(ulLightHighlightColor);
            g.drawLine(iconSize-6,8,iconSize-6,8);
            g.drawLine(iconSize-9,6, iconSize-7,8);
            g.setColor(mainItemColor);
            g.drawLine(3,iconSize-3,3,iconSize-3);
            g.setColor(darkHighlightColor);
            g.drawLine(iconSize-6,9,iconSize-6,9);
            g.setColor(backgroundColor);
            g.drawLine(iconSize-9,5,iconSize-9,5);

            // ARROW
            // do the shaft first
            g.setColor(mainItemColor);
            g.fillRect(iconSize-7,3, 3,5); // do a big block
            g.drawLine(iconSize-6,5, iconSize-3,2); // top shaft
            g.drawLine(iconSize-6,6, iconSize-2,2); // bottom shaft
            g.drawLine(iconSize-6,7, iconSize-3,7); // bottom arrow head

            // draw the dark highlight
            g.setColor(darkHighlightColor);
            g.drawLine(iconSize-8,2, iconSize-7,2); // top of arrowhead
            g.drawLine(iconSize-8,3, iconSize-8,7); // left of arrowhead
            g.drawLine(iconSize-6,4, iconSize-3,1); // top of shaft
            g.drawLine(iconSize-4,6, iconSize-3,6); // top,right of arrowhead

            // draw the light highlight
            g.setColor(lrLightHighlightColor);
            g.drawLine(iconSize-6,3, iconSize-6,3); // top
            g.drawLine(iconSize-4,5, iconSize-2,3); // under shaft
            g.drawLine(iconSize-4,8, iconSize-3,8); // under arrowhead
            g.drawLine(iconSize-2,8, iconSize-2,7); // right of arrowhead

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class InternalFrameAltMaximizeIcon
}
