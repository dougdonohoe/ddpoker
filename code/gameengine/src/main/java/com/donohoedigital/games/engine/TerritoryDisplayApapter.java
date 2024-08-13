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
 * TerritoryDisplayApapter.java
 *
 * Created on December 2, 2002, 2:45 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;

import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class TerritoryDisplayApapter implements TerritoryDisplayListener {
    
    /** 
     * Creates a new instance of TerritoryDisplayApapter 
     */
    public TerritoryDisplayApapter() 
    {
    }

    /**
     * Return null
     */
    public Color getTerritoryColor(Territory t) 
    {
        return null;
    }
    
    protected Color selectedBorderColor_ = StylesConfig.getColor("territory.border.selected");
    
    /**
     * If t.isSelected(), return "territory.border.selected" color
     */
    public Color getTerritoryBorderColor(Territory t) 
    {
        if (t.isSelected())
        {
            return selectedBorderColor_;
        }
        return null;
    }
    
    // line used to draw borders (draw at normal scale)
    protected BasicStroke borderStroke_ = new BasicStroke((float)1.0, BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_ROUND);
    /**
     * Get stroke for drawing border
     */
    public BasicStroke getTerritoryBorderStroke(Territory t) 
    {
        if (t.isSelected()) return borderStroke_;
        return null;
    }
    
    protected Font textFont_ = StylesConfig.getFont("territory.label");
    
    /**
     * Return "territory.label" font.  Returns null if t.isDecoration()
     */
    public Font getTerritoryLabelFont(GameContext context, Territory t)
    {
        if (t.isDecoration()) return null; // no labels on decoration
        return textFont_;
    }
    
    protected Color textColor_ = StylesConfig.getColor("territory.label.fg");
    protected Color shadowColor_ = StylesConfig.getColor("territory.label.shadow");
    
    /**
     * Return "territory.label.fg" color
     */
    public Color getTerritoryLabelColor(GameContext context, Territory t)
    {
        return textColor_;
    }
    
    /**
     * Return "territory.label.shadow" color
     */
    public Color getTerritoryLabelShadowColor(Territory t) 
    {
        return shadowColor_;
    }
    

}
