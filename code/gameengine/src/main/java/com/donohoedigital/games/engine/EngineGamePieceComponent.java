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
 * EngineGamePieceComponent.java
 *
 * Created on January 18, 2003, 8:34 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.gui.ImageComponent;

import javax.swing.*;
import java.awt.*;

/**
 * Class to draw given piece in its own component
 *
 * @author  Doug Donohoe
 */
public class EngineGamePieceComponent extends JComponent 
{
    //static Logger logger = LogManager.getLogger(EngineGamePieceComponent.class);

    EngineGamePiece piece_;
    double dScale_;
    double dLabelScale_;
    Integer nQuantityToDisplay_ = null;
    boolean bDisabledShowX_ = false;
    boolean bScaleToSize_ = false;

    public EngineGamePieceComponent(EngineGamePiece piece)
    {
        this(piece, 1.0d, 1.0d);
    }
    
    public EngineGamePieceComponent(EngineGamePiece piece, double dScale, double dLabelScale)
    {
        piece_ = piece;
        dScale_ = dScale;
        dLabelScale_ = dLabelScale;
        
        if (piece != null)
        {
            ImageComponent ic = piece.getImageComponent();
            setPreferredSize(new Dimension((int)(ic.getWidth()*dScale), (int)(ic.getHeight()*dScale)));
        }
        setOpaque(false);
    }
    
    public void setDisabledShowX(boolean b)
    {
        bDisabledShowX_ = b;
    }
    
    public void setScaleToSize(boolean b)
    {
        bScaleToSize_ = b;
    }
    
    public void setQuantityToDisplay(int n)
    {
        nQuantityToDisplay_ = n;
    }
    
    public void setEngineGamePiece(EngineGamePiece piece)
    {
        piece_ = piece;
        this.paintImmediately(0,0,getWidth(),getHeight());
    }
    
    public EngineGamePiece getEngineGamePiece()
    {
        return piece_;
    }

    public void paintComponent(Graphics g1) 
    {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;

        drawImage(g);
    }

    private static BasicStroke stroke_ = new BasicStroke(4.0f);
    
    /**
     * Draw icon of image centered in image
     */
    public void drawImage(Graphics2D g)
    {
        if (piece_ == null) return;
        /// draw image of piece
        
        
        ImageComponent ic = piece_.getImageComponent();
        
        boolean bOld = piece_.isEnabled();
        piece_.setEnabled(isEnabled());
        
        double width = getWidth();
        double height = getHeight();

        double x = 0;
        double y = 0;
        double imagewidth = ic.getWidth();
        double imageheight = ic.getHeight();
        
        int IN = 10;
        
        if (bScaleToSize_)
        {
            dScale_ =  Math.min((width-IN) / imagewidth,
                                (height-IN) / imageheight);      
        }

        imagewidth = imagewidth * dScale_;
        imageheight = imageheight * dScale_;

        if (imagewidth < width)
        {
            x = (width - imagewidth) / 2;
        }

        if (imageheight < height)
        {
            y = (height - imageheight) / 2;
        }     

        // pass moving num as 1 so quantity not drawn highlighted
        piece_.drawImageAt(g, ic, 
                            (nQuantityToDisplay_ == null) ? piece_.getQuantity() : nQuantityToDisplay_.intValue(),
                            0, 
                            (nQuantityToDisplay_ == null) ? 1 : nQuantityToDisplay_.intValue(),
                            x, y, imagewidth, imageheight, dLabelScale_);
        
        piece_.setEnabled(bOld);
        
        if (bDisabledShowX_ && !isEnabled())
        {
            Stroke oldStroke = g.getStroke();
            g.setColor(Color.red);
            g.setStroke(stroke_);
            g.drawLine(IN, IN, ((int)width)-IN, ((int)height)-IN);
            g.drawLine(IN, ((int)height)-IN, ((int)width)-IN, IN);
            g.setStroke(oldStroke);
            
        }
    }   
}
