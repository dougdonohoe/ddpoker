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
 * GameButton.java
 *
 * Created on March 23, 2005, 4:13 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.*;

import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class EngineButtonListener implements ActionListener
{
    //static Logger logger = LogManager.getLogger(EngineButtonListener.class);

    GameContext context_;
    GameButton button_;
    Phase phase_;
    GameEngine engine_;
    
    public EngineButtonListener(GameContext context, Phase phase, String sButtonName)
    {
        this(context, phase, new GameButton(sButtonName));
    }

    public EngineButtonListener(GameContext context, Phase phase, GameButton button)
    {
        context_ = context;
        phase_ = phase;
        engine_ = phase_.getGameEngine();
        button_ = button;
    }

    /**
     * Called when button pressed - calls phase_.processButton(), which if it returns true
     * then calls gameengine_.processPhase(button_.getGotoPhase)
     */
    public void actionPerformed(ActionEvent e) 
    {
        //logger.debug("Button pressed: " + button_.getName() +
        //            " phase: " + button_.getGotoPhase() +
        //             " param: " + button_.getGenericParam());
        //logger.debug("at " + Utils.formatExceptionText(new Throwable()));
        context_.buttonPressed(button_, phase_);
    }

    public GameButton getGameButton()
    {
        return button_;
    }
}
