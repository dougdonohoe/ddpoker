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
 * GamePhases.java
 *
 * Created on October 28, 2002, 4:48 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.jdom.*;

import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GamePhases extends HashMap<String, GamePhase> {
    
    //static Logger logger = Logger.getLogger(GamePhases.class);
    
    MapPoints allPoints_;
    
    /** 
     * Creates a new instance of GamePhases 
     */
    public GamePhases(Element root, Namespace ns)
                    throws ApplicationError
    {
        GamePhase.setGamePhases(this);
        // get <phase> children
        List<Element> children = XMLConfigFileLoader.getChildren(root, "phase", ns, false, null);
        int nSize = children.size();
        
        if (nSize != 0) 
        {
            String sAttrErrorDesc;
            Element phase;
            GamePhase gamePhase;
            for (int i = 0; i < nSize; i++)
            {
                sAttrErrorDesc = "Phase #" +(i+1)+" in " + GamedefConfig.GAMEDEF_CONFIG;
                phase = children.get(i);
                gamePhase = new GamePhase(phase, ns, sAttrErrorDesc);
                put(gamePhase.getName(), gamePhase);
            }
        }    
    }
}
