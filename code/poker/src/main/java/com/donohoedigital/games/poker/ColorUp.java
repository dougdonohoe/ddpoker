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
 * ColorUp.java
 *
 * Created on May 13, 2004, 3:03 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.ChainPhase;
import com.donohoedigital.games.engine.EngineUtils;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 *
 * @author  Doug Donohoe
 */
public class ColorUp extends ChainPhase 
{
    static Logger logger = LogManager.getLogger(ColorUp.class);

    PokerGame game_;
    PokerTable table_;

    /**
     * Override to control nextPhase call
     */
    public void start()
    {
        process();
    }

    /**
     * Deal high card
     */
    public void process()
    {
        // get info
        game_ = (PokerGame) context_.getGame();
        table_ = game_.getCurrentTable();

        // if doing current table, show pop-up
        if (table_.isColoringUp())
        {
            // display information
            if (!TESTING(PokerConstants.TESTING_AUTOPILOT) && !game_.isOnlineGame())
            {
                EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.colorup",
                            table_.getMinChip(), table_.getNextMinChip()),
                            "msg.windowtitle.colorup", "colorup", "colorup");
            }

            // note that we are now displaying the color-up (used in PokerGameboard)
            table_.setColoringUpDisplay(true);

            // repaint before deal so excess chips are shown
            PokerUtils.getPokerGameboard().repaintAll();

            // do rest after repaint occurs
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    nextPhase();
                }
            });
        }
        else
        {
            nextPhase();
        }
    }
}
