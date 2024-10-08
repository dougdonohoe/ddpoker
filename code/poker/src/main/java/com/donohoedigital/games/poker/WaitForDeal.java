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
 * WaitForDeal.java
 *
 * Created on January 4, 2004, 7:08 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

/**
 *
 * @author  Doug Donohoe
 */
public class WaitForDeal extends ChainPhase
{
    static Logger logger = LogManager.getLogger(WaitForDeal.class);

    /**
     * process
     */
    public void process()
    {
        PokerGame game = (PokerGame) context_.getGame();
        game.setInputMode(PokerTableInput.MODE_DEAL);

        // show dialog about pressing D to begin
        if (game.getCurrentTable().getHandNum() == 0)
        {
            // show info dialog
            if (!TESTING(PokerConstants.TESTING_AUTOPILOT_INIT))
            {
                String sMsg = PropertyConfig.getMessage("msg.waitfordeal");
                EngineUtils.displayInformationDialog(context_, sMsg, "msg.waitfordeal.title", "WaitForDeal");
            }
        }
    }
}
