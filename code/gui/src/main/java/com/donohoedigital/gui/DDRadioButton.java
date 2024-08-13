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
 * DDRadioButton.java
 *
 * Created on March 30, 2003, 3:19 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDRadioButton extends JRadioButton implements DDHasLabelComponent
{
    //static Logger logger = Logger.getLogger(DDRadioButton.class);
    private Color cDotColor_ = null;
    
    /** 
     * Creates a new instance of DDRadioButton 
     */
    public DDRadioButton() {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    /** 
     * Creates a new instance of DDRadioButton 
     */
    public DDRadioButton(String sName) {
        super();
        init(sName, GuiManager.DEFAULT);
    }
    
    /** 
     * Creates a new instance of DDRadioButton - sets name to sName
     */
    public DDRadioButton(String sName, String sStyleName) {
        super();
        init(sName, sStyleName);
    }
    
    /**
     * Return our type
     */
    public String getType() 
    {
        return "radio";
    }
    
    /**
     * Set the UI to DDRadioButtonUI
     */
    protected void init(String sName, String sStyleName)
    {
        GuiManager.init(this, sName, sStyleName);
        setOpaque(false);
        setIcon(new RadioButtonIcon());
        setIconTextGap(8);
    }
    
    /**
     * Override dot color
     */
    public void setDotColor(Color c)
    {
        cDotColor_ = c;
    }
    
    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
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

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    // Radio button code
    private class RadioButtonIcon implements Icon 
	{
        public void paintIcon(Component c, Graphics g, int x, int y) 
		{
	    	JRadioButton rb = (JRadioButton)c;
	    	ButtonModel model = rb.getModel();
	    	boolean drawDot = model.isSelected();
	
	    	Color background = c.getBackground();
	    	Color dotColor = (cDotColor_ != null) ? cDotColor_ : c.getForeground();  // JDD
	    	Color shadow = background.darker();//JDD MetalLookAndFeel.getControlShadow();
	    	Color darkCircle = background.darker(); //JDDMetalLookAndFeel.getControlDarkShadow();
	    	Color whiteInnerLeftArc = background.brighter();//c.getBackground().darker(); //JDD MetalLookAndFeel.getControlHighlight();
	    	Color whiteOuterRightArc = whiteInnerLeftArc;//c.getBackground().darker(); //JDD MetalLookAndFeel.getControlHighlight();
	    	Color interiorColor = background;
	
	    	// Set up colors per RadioButtonModel condition
	    	if ( !model.isEnabled() ) {
				whiteInnerLeftArc = whiteOuterRightArc = background;
				darkCircle = dotColor = shadow;
                
	    	}
	    	else if (model.isPressed() && model.isArmed() ) {
				//whiteInnerLeftArc = interiorColor = shadow;
                //if (!drawDot) whiteInnerLeftArc = dotColor; // JDD
                //interiorColor = (drawDot ? background : dotColor);// JDD
                
                whiteInnerLeftArc = dotColor;
                interiorColor = dotColor;
	    	}
	    
	    	g.translate(x, y);
	
	    	// fill interior
            g.setColor(interiorColor);
            g.fillRect(2,2, 9,9);
	  	 
	    	// draw Dark Circle (start at top, go clockwise)
	    	g.setColor(darkCircle);
	    	g.drawLine( 4, 0,  7, 0);
	    	g.drawLine( 8, 1,  9, 1);
	    	g.drawLine(10, 2, 10, 3);
	    	g.drawLine(11, 4, 11, 7);
	    	g.drawLine(10, 8, 10, 9);
	    	g.drawLine( 9,10,  8,10);
	    	g.drawLine( 7,11,  4,11);
	    	g.drawLine( 3,10,  2,10);
	    	g.drawLine( 1, 9,  1, 8);
	    	g.drawLine( 0, 7,  0, 4);
	    	g.drawLine( 1, 3,  1, 2);
	    	g.drawLine( 2, 1,  3, 1);
	
	    	// draw Inner Left (usually) White Arc
	    	//  start at lower left corner, go clockwise
	    	g.setColor(whiteInnerLeftArc);
	    	g.drawLine( 2, 9,  2, 8);
	    	g.drawLine( 1, 7,  1, 4);
	    	g.drawLine( 2, 2,  2, 3);
	    	g.drawLine( 2, 2,  3, 2);
	    	g.drawLine( 4, 1,  7, 1);
	    	g.drawLine( 8, 2,  9, 2);
	    	// draw Outer Right White Arc
	    	//  start at upper right corner, go clockwise
	    	g.setColor(whiteOuterRightArc);
	    	g.drawLine(10, 1, 10, 1);
	    	g.drawLine(11, 2, 11, 3);
	    	g.drawLine(12, 4, 12, 7);
	    	g.drawLine(11, 8, 11, 9);
	    	g.drawLine(10,10, 10,10);
	    	g.drawLine( 9,11,  8,11);
	    	g.drawLine( 7,12,  4,12);
	    	g.drawLine( 3,11,  2,11);

	    	// selected dot
	    	if ( drawDot ) {
				g.setColor(dotColor);
				g.fillRect( 4, 4,  4, 4);
				g.drawLine( 4, 3,  7, 3);
				g.drawLine( 8, 4,  8, 7);
				g.drawLine( 7, 8,  4, 8);
				g.drawLine( 3, 7,  3, 4);
	    	}

	    	g.translate(-x, -y);
		}
	
		public int getIconWidth() {
	    	return 13;
		}
	
		public int getIconHeight() {
	    	return 13;
		}
    }  // End class RadioButtonIcon
}
