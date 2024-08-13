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
 * DDButtonUI.java
 *
 * Created on November 16, 2002, 12:57 PM
 */

package com.donohoedigital.gui;

import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDButtonUI extends MetalButtonUI 
{
    static Logger logger = Logger.getLogger(DDButtonUI.class);
    
    private final static DDButtonUI ddButtonUI = new DDButtonUI(); 
 
    public static ComponentUI createUI(JComponent c) {
        return ddButtonUI;
    }    
    
    protected Color getFocusColor() {
        return Color.yellow;
    }
    
    /**
     * Overridden to paint image background
     */
    public void paint(Graphics g, JComponent c) 
    {
        DDButton button = (DDButton) c;
        
        // fill background with image if it exists
        drawBackgroundImage(g, button);
        
        // paint all of button
        super.paint(g, c);
    }
    
    /**
     * Overridden to fill background with background color when pressed
     */
    protected void paintButtonPressed(Graphics g, AbstractButton b) 
    {
        if ( b.isContentAreaFilled() ) {
            if (!drawBackgroundImage(g, (DDButton) b))
            {
                g.setColor(b.getBackground());
                g.fillRect(0, 0, b.getWidth(), b.getHeight());
            }
        }
    }  
    
    static BasicStroke FOCUS_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                                                            BasicStroke.JOIN_ROUND,
                                                        1.0f, new float[] {1.0f,1.0f}, 0.0f);
    
    protected void paintFocus(Graphics g1, AbstractButton b,
			      Rectangle viewRect, Rectangle textRect, Rectangle iconRect)
    {
        Graphics2D g = (Graphics2D) g1;
        Rectangle focusRect = new Rectangle();
        String text = b.getText();
        boolean isIcon = b.getIcon() != null;

        // If there is text
        if ( text != null && !text.equals( "" ) ) {
  	    if ( !isIcon ) {
	        focusRect.setBounds( textRect );
	    }
	    else {
	        focusRect.setBounds( iconRect.union( textRect ) );
	    }
        }
        // If there is an icon and no text
        else if (isIcon) 
        {
            focusRect.setBounds( iconRect );
        }

        Color focus = b.getForeground();
        focus = new Color(focus.getRed(), focus.getGreen(), focus.getBlue(), 150);
        g.setColor(focus);
        int xgap = 3;
        int ygap = 1;
        Stroke old = g.getStroke();
        g.setStroke(FOCUS_STROKE);
        g.drawRect((focusRect.x-xgap), (focusRect.y-ygap), focusRect.width+(xgap*2)-1, focusRect.height+(ygap));
        g.setStroke(old);
    }


    /**
     * Draw background image of button, if it exists
     */
    private boolean drawBackgroundImage(Graphics g, DDButton button)
    {
        Image img = button.getBackgroundImage();
        if (img != null)
        {
            int drawwidth = button.getWidth();
            int drawheight = button.getHeight();
            if (drawwidth > img.getWidth(null)) drawwidth = img.getWidth(null);
            if (drawheight > img.getHeight(null)) drawheight = img.getHeight(null);
            g.drawImage(img, 0, 0, button.getWidth(), button.getHeight(), 
                             0, 0, drawwidth,drawheight, button);
            
            return true;
        }
        
        return false;
    }

    /**
     * Overriden to shift text when button armed (and color when mouse over)
     */
    protected void paintText(Graphics g1, JComponent c, Rectangle textRect, String text) 
    {
        Graphics2D g = (Graphics2D) g1;
        DDButton b = (DDButton) c;
        ButtonModel model = b.getModel();
        FontMetrics fm = g.getFontMetrics();
        int mnemIndex = b.getDisplayedMnemonicIndex();

        int nOffset = 0;
        if (model.isArmed()) nOffset = 1;

        // we want font to look nice
 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (b.isAlwaysAntiAlias() || GuiUtils.drawAntiAlias(c))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }

        /* Draw the Text */
        if (model.isEnabled() || ((b.getDisableMode() & DDButton.DISABLED_GRAY) == 0))
        {
            /*** paint the text normally */
            // JDD - added mouse over color
            Color cMouseOver = b.getMouseOverForeground();
            if (cMouseOver != null && model.isRollover())
            {
                g.setColor(cMouseOver);
            }
            else
            {
                g.setColor(b.getForeground());
            }
        }
        else
        {
            g.setColor(getDisabledTextColor());
        }

        if (model.isEnabled())
        {
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text, mnemIndex,
					  textRect.x + nOffset,
					  textRect.y + nOffset + fm.getAscent());
        }
        else 
        {
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text,mnemIndex,
					  textRect.x, textRect.y + fm.getAscent());
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    protected BasicButtonListener createButtonListener(AbstractButton b) {

        if (b instanceof DDImageButton) return new DDBasicButtonListener((DDImageButton) b);

        return super.createButtonListener(b);
    }

    /**
     * for image buttons, we don't want mouse entered/exit/click on the transparent
     * parts of the image
     */
    public class DDBasicButtonListener extends BasicButtonListener
    {
        DDImageButton button;
        boolean TESTING = GuiUtils.TESTING_BUTTON;
        boolean bMouseOver = false;
        BufferedImage bimage;

        public DDBasicButtonListener(DDImageButton button)
        {
            super(button);
            this.button = button;
        }

        public void mousePressed(MouseEvent e)
        {
           if (!button.isTransparentIgnored()) { super.mousePressed(e); return; }

           if (!isMouseOverVisible(e)) return;
           if (TESTING) GuiUtils.log(""+isMouseOverVisible(e)+":"+button.getName()+" mousePressed()", e.toString()/* +
                                " " + Utils.formatExceptionText(new Throwable())*/);
           super.mousePressed(e);
        }

        public void mouseReleased(MouseEvent e)
        {
           if (!button.isTransparentIgnored()) { super.mouseReleased(e); return; }

           //if (!isMouseOverVisible(e)) return;
           if (TESTING) GuiUtils.log(""+isMouseOverVisible(e)+":"+button.getName()+" mouseReleased()", e.toString());
           super.mouseReleased(e);
        }

        public void mouseEntered(MouseEvent e)
        {
           if (!button.isTransparentIgnored()) { super.mouseEntered(e); return; }

           if (!isMouseOverVisible(e)) return;
           bMouseOver = true;
           if (TESTING) GuiUtils.log(""+isMouseOverVisible(e)+":"+button.getName()+" mouseEntered()", e.toString());
           super.mouseEntered(e);
        }

        public void mouseExited(MouseEvent e)
        {
            if (!button.isTransparentIgnored()) { super.mouseExited(e); return; }

            bMouseOver = false;
            if (TESTING) GuiUtils.log(""+isMouseOverVisible(e)+":"+button.getName()+" mouseExited()", e.toString());
            super.mouseExited(e);
        }

        public void mouseMoved(MouseEvent e)
        {
            if (!button.isTransparentIgnored()) { super.mouseMoved(e); return; }

            if (!isMouseOverVisible(e))
            {
                if (bMouseOver) super.mouseExited(e);
                bMouseOver = false;
                return;
            }
            if (!bMouseOver)
            {
                super.mouseEntered(e);
                bMouseOver = true;
            }
            if (TESTING) GuiUtils.log(""+isMouseOverVisible(e)+":"+button.getName()+" mouseMoved()", e.toString());
            super.mouseMoved(e);
        };

        public void mouseDragged(MouseEvent e)
        {
            if (!button.isTransparentIgnored()) { super.mouseDragged(e); return; }

            if (!isMouseOverVisible(e))
            {
                if (bMouseOver) super.mouseExited(e);
                bMouseOver = false;
                return;
            }
            if (!bMouseOver)
            {
                super.mouseEntered(e);
                bMouseOver = true;
            }
            if (TESTING) GuiUtils.log(""+isMouseOverVisible(e)+":"+button.getName()+" mouseDragged()", e.toString());
            super.mouseDragged(e);
        };

        /**
         * return true if mouse is over a visible portion of the image button
         */
        private boolean isMouseOverVisible(MouseEvent e)
        {
            // TODO: if button is scaled larger than icon, this doesn't account for placement of icon
            if (bimage == null)
            {
                ImageIcon icon = (ImageIcon) button.getIcon();
                bimage = (BufferedImage) icon.getImage();
            }
            return ImageComponent.isNonTransparent(bimage, e.getX(), e.getY());
        }
    }
}
