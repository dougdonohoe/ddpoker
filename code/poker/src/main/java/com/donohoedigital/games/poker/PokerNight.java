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
 * PokerNight.java
 *
 * Created on February 1, 2004, 8:32 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.config.AudioConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.BasePhase;
import com.donohoedigital.games.engine.DisplayMessage;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerNight extends BasePhase implements GameClockListener
{
    static Logger logger = LogManager.getLogger(PokerNight.class);

    public static int DEMO_MAX = 3;
    private PokerGame game_;

    public void start()
    {
        context_.setSpecialSavePhase(gamephase_.getName());

        game_ = (PokerGame) context_.getGame();

        GameClock clock = game_.getGameClock();
        clock.setFlash(true);
        clock.addGameClockListener(this);

        // makes sure *** PAUSED *** text is displayed for loaded tournaments (may lose a few ms, but who cares)
        if (game_.getLevel() != 1 || game_.getSecondsInLevel(game_.getLevel()) != clock.getSecondsRemaining())
        {
            clock.start();
            clock.stop();
        }
    }

    /**
     * Clean up
     */
    public void finish()
    {
        context_.setSpecialSavePhase(null);
        GameClock clock = game_.getGameClock();
        clock.stop();
        clock.removeGameClockListener(this);
		super.finish();
    }

    /**
     * Start the clock
     */
    void startClock()
    {
        if (engine_.isDemo() && game_.getLevel() > DEMO_MAX)
        {
            return;
        }

        game_.getGameClock().start();
    }

    /**
     * Stop the clock
     */
    void stopClock()
    {
        game_.getGameClock().stop();
    }

    /**
     * ring bell at certain times
     */
    private void bell(int nLeft)
    {
        if (nLeft <= 10)
        {
            AudioConfig.playFX("bell");
        }
    }

    public void gameClockSet(GameClock clock)
    {
        return;
    }

    public void gameClockStarted(GameClock clock)
    {
        return;
    }

    public void gameClockStopped(GameClock clock)
    {
        return;
    }

    public void gameClockTicked(GameClock clock)
    {
        synchronized(clock)
        {
            int nLeft = game_.getGameClock().getSecondsRemaining();

            if (nLeft > 0)
            {
                bell(nLeft);
            }
            else
            {
                int nLevel = game_.getLevel();
                TournamentProfile profile = game_.getProfile();
                int nLastRebuy = profile.getLastRebuyLevel();
                int nAddon = profile.getAddonLevel();

                // rebuy, addon
                boolean bRebuy = profile.isRebuys() && nLastRebuy == nLevel;
                boolean bAddon = profile.isAddons() && nAddon == nLevel;

                // chip race?
                String sChipRace = null;
                int nMinBefore = game_.getMinChip();
                game_.nextLevel();

                boolean bDemoOver = engine_.isDemo() && game_.getLevel() >= DEMO_MAX;

                if (PokerUtils.isOptionOn(PokerConstants.OPTION_CLOCK_COLOUP) &&
                    game_.getMinChip() > nMinBefore)
                {
                    sChipRace = PropertyConfig.getMessage("msg.finish.color",
                            game_.getLastMinChip(),
                            game_.getMinChip());
                    if (bRebuy || bAddon) sChipRace = "<BR><BR>" + sChipRace;
                }

                if (bRebuy || bAddon || sChipRace != null || bDemoOver)
                {
                    if (sChipRace == null) sChipRace = "";
                    stopClock();

                    final String sMsg;

                    if (bDemoOver)
                    {
                        sMsg = PropertyConfig.getMessage("msg.pokernight.demo");
                    }
                    else if (bRebuy && bAddon)
                    {
                        sMsg = PropertyConfig.getMessage("msg.finish.both", nLevel, sChipRace);
                    }
                    else if (bRebuy)
                    {
                        sMsg = PropertyConfig.getMessage("msg.finish.rebuy", nLevel, sChipRace);
                    }
                    else if (bAddon)
                    {
                        sMsg = PropertyConfig.getMessage("msg.finish.addon", nLevel, sChipRace);
                    }
                    else
                    {
                        sMsg = PropertyConfig.getMessage("msg.finish.colorup", nLevel, sChipRace);
                    }

                    // show message
                    SwingUtilities.invokeLater(
                            new Runnable()
                            {
                                public void run()
                                {
                                    AudioConfig.playFX("attention");
                                    TypedHashMap params = new TypedHashMap();
                                    params.setString(DisplayMessage.PARAM_MESSAGE, sMsg);
                                    context_.processPhaseNow("PokerNightMessage", params);
                                }
                            });
                }
                else
                {
                    if (PokerUtils.isOptionOn(PokerConstants.OPTION_CLOCK_PAUSE))
                    {
                        stopClock();
                    }
                    else
                    {
                        startClock();
                    }
                }
            }
        }

        return;
    }
 }
