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

import javax.swing.plaf.metal.*;
import javax.swing.plaf.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 22, 2005
 * Time: 5:34:49 PM
 *
 * Override to paint our own thumb.
 */
public class DDSliderUI extends MetalSliderUI
{
    private Icon horizThumbIcon_;
    private Icon vertThumbIcon_;

    public void installUI( JComponent c )
    {
        horizThumbIcon_ = new HorizontalSlider(c);
        vertThumbIcon_ = new VerticalSlider(c);
        super.installUI(c);
    }

    public void paintTrack(Graphics g)
    {
        super.paintTrack(g);

        int trackLeft = 0;
        int trackTop = 0;
        int trackRight = 0;
        int trackBottom = 0;

        g.translate( trackRect.x, trackRect.y );

          // Draw the track
        if ( slider.getOrientation() == JSlider.HORIZONTAL )
        {
                trackBottom = (trackRect.height - 1) - getThumbOverhang();
                trackTop = trackBottom - (getTrackWidth() - 1);
                trackRight = trackRect.width - 1;
        }
        else
        {
            trackLeft = (trackRect.width - getThumbOverhang()) -
                                                     getTrackWidth();
            trackRight = (trackRect.width - getThumbOverhang()) - 1;
            trackBottom = trackRect.height - 1;
        }

        // Draw the fill
        if ( true ) {
            int middleOfThumb = 0;
            int fillTop = 0;
            int fillLeft = 0;
            int fillBottom = 0;
            int fillRight = 0;

            if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
                middleOfThumb = thumbRect.x + (thumbRect.width / 2);
            middleOfThumb -= trackRect.x; // To compensate for the g.translate()
            fillTop = !slider.isEnabled() ? trackTop : trackTop + 1;
            fillBottom = !slider.isEnabled() ? trackBottom - 1 : trackBottom - 2;

            if ( !drawInverted() ) {
                fillLeft = !slider.isEnabled() ? trackLeft : trackLeft + 1;
                fillRight = middleOfThumb;
            }
            else {
                fillLeft = middleOfThumb;
                fillRight = !slider.isEnabled() ? trackRight - 1 : trackRight - 2;
            }
            }
            else {
                middleOfThumb = thumbRect.y + (thumbRect.height / 2);
            middleOfThumb -= trackRect.y; // To compensate for the g.translate()
            fillLeft = !slider.isEnabled() ? trackLeft : trackLeft + 1;
            fillRight = !slider.isEnabled() ? trackRight - 1 : trackRight - 2;

            if ( !drawInverted() ) {
                fillTop = middleOfThumb;
                fillBottom = !slider.isEnabled() ? trackBottom - 1 : trackBottom - 2;
            }
            else {
                fillTop = !slider.isEnabled() ? trackTop : trackTop + 1;
                fillBottom = middleOfThumb;
            }
            }

            if ( slider.isEnabled() ) {
                //g.setColor( slider.getBackground() );
                //g.drawLine( fillLeft, fillTop, fillRight, fillTop );
                //g.drawLine( fillLeft, fillTop, fillLeft, fillBottom );

                g.setColor(slider.getBackground());
                g.fillRect( fillLeft + 1, fillTop + 1,
                    fillRight - fillLeft+1, fillBottom - fillTop +1 );
            }
            else {
//                g.setColor( MetalLookAndFeel.getControlShadow() );
//                g.fillRect( fillLeft, fillTop,
//                    fillRight - fillLeft, trackBottom - trackTop );
            }
        }

        //g.setColor(Color.red);
        //g.fillRect(trackLeft, trackTop, trackRight, trackBottom);

        g.translate( -trackRect.x, -trackRect.y );
    }

    public void paintThumb(Graphics g)
    {
        Rectangle knobBounds = thumbRect;

        g.translate( knobBounds.x, knobBounds.y );

        if ( slider.getOrientation() == JSlider.HORIZONTAL )
        {
            horizThumbIcon_.paintIcon( slider, g, 0, 0 );
        }
        else
        {
            vertThumbIcon_.paintIcon( slider, g, 0, 0 );
        }

        g.translate( -knobBounds.x, -knobBounds.y );
    }

    protected Dimension getThumbSize()
    {
        Dimension size = new Dimension();

        if ( slider.getOrientation() == JSlider.VERTICAL )
        {
            size.width = vertThumbIcon_.getIconWidth();
            size.height = vertThumbIcon_.getIconHeight();
        }
        else
        {
            size.width = horizThumbIcon_.getIconWidth();
            size.height = horizThumbIcon_.getIconHeight();
        }

        return size;
    }

    ///
    /// ICONS: copied from MetalIconFactory since they are private.  Dumbasses.
    ///

    private static class VerticalSlider implements Icon, Serializable, UIResource
    {
        private DDMetalBumps controlBumps;
        private DDMetalBumps primaryBumps;

        public VerticalSlider(Component c)
        {
            DDSlider slider = (DDSlider)c;

            Color thumbColor = slider.getBackground();
            if (thumbColor == null) thumbColor = Color.gray;
            Color thumbShadow = thumbColor.darker();
            Color thumbHighlightColor = thumbColor.brighter();

            // our bumps
            controlBumps = new DDMetalBumps( 6, 10, thumbHighlightColor, thumbShadow, thumbColor );
            primaryBumps = controlBumps;
        }

        public void paintIcon( Component c, Graphics g, int x, int y )
        {
            DDSlider slider = (DDSlider)c;

            boolean leftToRight = true;

            g.translate( x, y );

            // Draw the frame
            g.setColor(Color.black);
//            if ( slider.hasFocus() ) {
//                g.setColor( MetalLookAndFeel.getPrimaryControlInfo() );
//            }
//            else {
//                g.setColor( slider.isEnabled() ? MetalLookAndFeel.getPrimaryControlInfo() :
//                                         MetalLookAndFeel.getControlDarkShadow() );
//            }

            if (leftToRight) {
                g.drawLine(  1,0  ,  8,0  ); // top
                g.drawLine(  0,1  ,  0,13 ); // left
                g.drawLine(  1,14 ,  8,14 ); // bottom
                g.drawLine(  9,1  , 15,7  ); // top slant
                g.drawLine(  9,13 , 15,7  ); // bottom slant
            }
            else {
                g.drawLine(  7,0  , 14,0  ); // top
                g.drawLine( 15,1  , 15,13 ); // right
                g.drawLine(  7,14 , 14,14 ); // bottom
                g.drawLine(  0,7  ,  6,1  ); // top slant
                g.drawLine(  0,7  ,  6,13 ); // bottom slant
            }

            // Fill in the background
            if ( slider.hasFocus() ) {
                g.setColor( slider.getThumbBackgroundColor() );
            }
            else {
                g.setColor( slider.getThumbBackgroundColor()  );
            }
            drawBG(leftToRight, g);

            // Draw the bumps
            int offset = (leftToRight) ? 2 : 8;
            if ( slider.isEnabled() ) {
                if ( slider.hasFocus() ) {
                    primaryBumps.paintIcon( c, g, offset, 2 );
                }
                else {
                    controlBumps.paintIcon( c, g, offset, 2 );
                }
            }

            // overlay focus (JDD)
            if (slider.hasFocus())
            {
                Color focus = slider.getThumbFocusColor();
                if (focus != null)
                {
                    g.setColor(focus);
                    drawBG(leftToRight, g);
                }
            }


            // Draw the highlight
            if ( slider.isEnabled() ) {
                g.setColor( slider.getThumbBackgroundColor().brighter());//slider.hasFocus() ? MetalLookAndFeel.getPrimaryControl()
                    //: MetalLookAndFeel.getControlHighlight() );
                if (leftToRight) {
                    g.drawLine( 1, 1, 8, 1 );
                g.drawLine( 1, 1, 1, 13 );
                }
                else {
                    g.drawLine(  8,1  , 14,1  ); // top
                g.drawLine(  1,7  ,  7,1  ); // top slant
                }
            }

            g.translate( -x, -y );
        }

        private void drawBG(boolean leftToRight, Graphics g)
        {
            if (leftToRight) {
                g.fillRect(  1,1 ,  8,13 );

                g.drawLine(  9,2 ,  9,12 );
                g.drawLine( 10,3 , 10,11 );
                g.drawLine( 11,4 , 11,10 );
                g.drawLine( 12,5 , 12,9 );
                g.drawLine( 13,6 , 13,8 );
                g.drawLine( 14,7 , 14,7 );
            }
            else {
                g.fillRect(  7,1,   8,13 );

                g.drawLine(  6,3 ,  6,12 );
                g.drawLine(  5,4 ,  5,11 );
                g.drawLine(  4,5 ,  4,10 );
                g.drawLine(  3,6 ,  3,9 );
                g.drawLine(  2,7 ,  2,8 );
            }
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 15;
        }
    }

    private static class HorizontalSlider implements Icon, Serializable, UIResource
    {
        private DDMetalBumps controlBumps;
        private DDMetalBumps primaryBumps;

        public HorizontalSlider(Component c)
        {
            DDSlider slider = (DDSlider)c;

            Color thumbColor = slider.getThumbBackgroundColor();
            if (thumbColor == null) thumbColor = Color.gray;
            Color thumbShadow = thumbColor.darker();
            Color thumbHighlightColor = thumbColor.brighter();

            // our bumps
            controlBumps = new DDMetalBumps( 10, 6, thumbHighlightColor, thumbShadow, thumbColor );
            primaryBumps = controlBumps;
        }

        public void paintIcon( Component c, Graphics g, int x, int y )
        {
            DDSlider slider = (DDSlider)c;

            g.translate( x, y );

            // Draw the frame
            g.setColor(Color.black);
//            if ( slider.hasFocus() ) {
//                g.setColor( Color.red );//MetalLookAndFeel.getPrimaryControlInfo() );
//            }
//            else {
//                g.setColor( Color.blue );//slider.isEnabled() ? MetalLookAndFeel.getPrimaryControlInfo() :
//                                         //MetalLookAndFeel.getControlDarkShadow() );
//            }

            g.drawLine(  1,0  , 13,0 );  // top
            g.drawLine(  0,1  ,  0,8 );  // left
            g.drawLine( 14,1  , 14,8 );  // right
            g.drawLine(  1,9  ,  7,15 ); // left slant
            g.drawLine(  7,15 , 14,8 );  // right slant

            // Fill in the background
            if ( slider.hasFocus() ) {
                g.setColor( slider.getThumbBackgroundColor() );
            }
            else {
                g.setColor( slider.getThumbBackgroundColor()  );
            }
            drawBG(g);

            // Draw the bumps
            if ( slider.isEnabled() ) {
                if ( slider.hasFocus() ) {
                    primaryBumps.paintIcon( c, g, 3, 3 );
                }
                else {
                    controlBumps.paintIcon( c, g, 3, 3 );
                }
            }

            // overlay focus (JDD)
            if (slider.hasFocus())
            {
                Color focus = slider.getThumbFocusColor();
                if (focus != null)
                {
                    g.setColor(focus);
                    drawBG(g);
                }
            }

            // Draw the highlight
            if ( slider.isEnabled() ) {
                g.setColor( slider.getThumbBackgroundColor().brighter());//slider.hasFocus() ? MetalLookAndFeel.getPrimaryControl()
                                            //: MetalLookAndFeel.getControlHighlight() );
                g.drawLine( 1, 1, 13, 1 );
                g.drawLine( 1, 1, 1, 8 );
            }

            g.translate( -x, -y );
        }

        private void drawBG(Graphics g)
        {
            g.fillRect( 1,1, 13, 8 );

            g.drawLine( 2,9  , 12,9 );
            g.drawLine( 3,10 , 11,10 );
            g.drawLine( 4,11 , 10,11 );
            g.drawLine( 5,12 ,  9,12 );
            g.drawLine( 6,13 ,  8,13 );
            g.drawLine( 7,14 ,  7,14 );
        }

        public int getIconWidth() {
            return 15;
        }

        public int getIconHeight() {
            return 16;
        }
    }

}
