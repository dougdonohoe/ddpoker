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
 * DisplayTableMoves.java
 *
 * Created on January 25, 2005, 8:42 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.base.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
public class DisplayTableMoves extends ChainPhase
{
    private PokerGame game_;
    
    public void process()
    {
        game_ = (PokerGame) context_.getGame();
        PokerTable table = game_.getCurrentTable();
        List<PokerPlayer> moved = table.getAddedList();

        // moved players
        StringBuilder sb = new StringBuilder();
        for (PokerPlayer player : moved)
        {
            sb.append(PropertyConfig.getMessage("msg.moved", Utils.encodeHTML(player.getName()),
                                                player.getChipCount(),
                                                player.getSeat() + 1));
        }

        // create message with player, # chips and total tables left
        Integer nSIZE= moved.size();
        if (!TESTING(PokerConstants.TESTING_AUTOPILOT))
        {
            String sMsg = PropertyConfig.getMessage("msg.dialog.moved",
                                            moved.size() == 1 ? PropertyConfig.getMessage("msg.player.singular", nSIZE) :
                                                PropertyConfig.getMessage("msg.player.plural", nSIZE),
                                            sb.toString());
            EngineUtils.displayInformationDialog(context_, Utils.fixHtmlTextFor15(sMsg),
                                                 "msg.windowtitle.moved", "playermoved", "noplayermove");
        }
    }
    
    public void nextPhase()
    {
        // notify tournament director that cards have the player has
        // seen the cards dealt
        TournamentDirector td = (TournamentDirector) context_.getGameManager();
        td.removeFromWaitList(game_.getHumanPlayer());
        
        super.nextPhase();
    }
    
}
