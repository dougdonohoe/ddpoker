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
 * PokerDisplayAdapter.java
 *
 * Created on January 7, 2004, 4:51 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;

import java.awt.*;

/**
 *
 * @author  donohoe
 */
public class PokerDisplayAdapter extends TerritoryDisplayApapter
{
    
    static Color cCurrent_ = StylesConfig.getColor("territory.label.current");
    static Font fCurrent_ = StylesConfig.getFont("territory.label.current");
    
    static Color cPot_ = StylesConfig.getColor("territory.label.pot");
    static Font fPot_ = StylesConfig.getFont("territory.label.pot");
    
    /** Creates a new instance of PokerDisplayAdapter */
    public PokerDisplayAdapter() {
    }
    
    /**
     * Return "territory.label.current" font
     * if t corresponds to current player
     */
    public Font getTerritoryLabelFont(GameContext context, Territory t)
    {
        if (PokerUtils.isPot(t))
        {
            return fPot_;
        }
        
        if (PokerUtils.isCurrent(context, t))
        {
            return fCurrent_;
        }
        
        return super.getTerritoryLabelFont(context, t);
    }
    
    /**
     * Return "territory.label.fg.selected" color
     * if t corresponds to current player
     */
    public Color getTerritoryLabelColor(GameContext context, Territory t)
    {
        if (PokerUtils.isPot(t))
        {
            return cPot_;
        }
                
        if (PokerUtils.isCurrent(context, t))
        {
            return cCurrent_;
        }
        
        return super.getTerritoryLabelColor(context, t);
    }
}
