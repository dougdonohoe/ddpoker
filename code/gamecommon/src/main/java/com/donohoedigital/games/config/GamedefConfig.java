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
 * GamedefConfig.java
 *
 * Created on October 11, 2002, 6:02 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.jdom2.*;

import java.net.*;

/**
 *
 * @author  donohoe
 */
public class GamedefConfig extends XMLConfigFileLoader
{
    //static Logger logger = Logger.getLogger(GamedefConfig.class);
    static final String GAMEDEF_CONFIG = "gamedef.xml";

    private String sStartPhase_;
    private GamePhases phases_;

    /** 
     * Load GamedefConfig (gamedef.xml) from the given module
     */
    public GamedefConfig(String sModule) throws ApplicationError
    {
        init(sModule);
    }
    
    /**
     * Read in config file from given module
     */
    private void init(String sModule) throws ApplicationError
    {
        // get gamedef url, throws exception if missing
        URL url = new MatchingResources("classpath*:config/" + sModule + "/" + GAMEDEF_CONFIG).getSingleRequiredResourceURL();
        
        Document doc = this.loadXMLUrl(url, "gamedef.xsd");
        init(doc);
    }
    
    /**
     * Initialize from JDOM doc
     */
    private void init(Document doc) throws ApplicationError
    {
        Element root = doc.getRootElement();
        
        // startphase
        sStartPhase_ = getChildStringValueTrimmed(root, "startphase", ns_, true, GAMEDEF_CONFIG);
        
        // phases
        phases_ = new GamePhases(root, ns_);
        
        // verify startphase exists
        GamePhase phase = phases_.get(sStartPhase_);
        if (phase == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                        "Start phase " + sStartPhase_ + " not found in list of phases",
                                        "Make sure start phase is spelled correctly and exists as an actual phase");
        }
    }

    /**
     * get starting phase
     */
    public String getStartPhaseName()
    {
        return sStartPhase_;
    }
    
    /**
     * Get phases
     */
    public GamePhases getGamePhases()
    {
        return phases_;
    }
}
