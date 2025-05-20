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
/*
 * InitializeGame.java
 *
 * Created on November 15, 2002, 3:41 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class InitializeGame extends ChainPhase
{
    //static Logger logger = LogManager.getLogger(InitializeGame.class);
    
    private InitLabel label_;
    private boolean bNextPhaseCalled_;
    private static final String INIT_GAME = "initgame";

    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        AudioConfig.stopBackgroundMusic();
        String STYLE = gamephase_.getString("style", "default");
        String sLabel =  gamephase_.getString("label", INIT_GAME);
        // no save file loading in demo, and if they attempt to,
        // it shows this screen before the engine aborts when
        // they try and run LoadSavedGame.  in this case,
        // don't show the "loading save file..." message and
        // show the default
        if (engine.isDemo())
        {
            sLabel = INIT_GAME;
        }
        label_ = new InitLabel(sLabel, STYLE);
    }
    
    /**
     * Override to control when nextPhase is called (when paint happens)
     */
    public void start()
    {
        bNextPhaseCalled_ = false;
        process();
    }
        
    /** 
     * chain phase process
     */
    public void process() {
        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, label_, false, null);
    }
    
    /**
     * Private class used to ensure this phase is displayed
     * before nextPhase() is called
     */
    private class InitLabel extends DDLabel
    {
        public InitLabel(String sName, String sStyle)
        {
            super(sName, sStyle);
        }
        
        public void paintComponent(Graphics g1)
        {
            super.paintComponent(g1);
            if (!bNextPhaseCalled_)
            {
                bNextPhaseCalled_ = true;
                nextPhase();
            }
        }
    }
}
