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
package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.service.*;
import static com.donohoedigital.games.poker.service.OnlineGameService.OrderByType.*;
import org.apache.logging.log4j.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Oct 21, 2005
 * Time: 1:11:41 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class PokerRankUpdater extends BaseCommandLineApp
{
    private static Logger logger = LogManager.getLogger(PokerRankUpdater.class);

    private OnlineGameService gameService;
    private TournamentHistoryService histService;

    /**
     * Run analyzer
     */
    public static void main(String[] args)
    {
        try {
            new PokerRankUpdater("poker", args);
        }
        catch (ApplicationError ae)
        {
            System.err.println("PokerRankUpdater ending due to ApplicationError: " + ae.toString());
        }
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
        }
        catch (Throwable t)
        {
            System.err.println(Utils.formatExceptionText(t));
        }

        System.exit(0);
    }

    public PokerRankUpdater(String sConfigName, String[] args)
    {
        // init app
        super(sConfigName, args);

        // get the service from spring
        ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-pokertools.xml");
        gameService = (OnlineGameService) ctx.getBean("onlineGameService");
        histService = (TournamentHistoryService) ctx.getBean("tournamentHistoryService");

        // do the work
        long time = System.currentTimeMillis();
        doRank();
        logger.debug("Elapsed time: " + (System.currentTimeMillis() - time));
    }

    ///
    /// GUTS of the Program...
    ///

    private static int GAMES_CHUNK = 1000;

    /**
     * do work
     */
    private void doRank()
    {
        Integer modes[] = new Integer[2];
        modes[0] = OnlineGame.MODE_END;
        modes[1] = OnlineGame.MODE_STOP;
        int offset = 0;
        int count = 0;

        Integer total = gameService.getOnlineGamesCount(modes, null, null, null);
        OnlineGameList list = gameService.getOnlineGames(total, offset, GAMES_CHUNK, modes, null, null, null, date);
        while (!list.isEmpty())
        {
            for (OnlineGame game : list)
            {
                count++;
                doRank(game);
                // TODO: make sure debug off (next line)
                //if (count > 200) System.exit(0); // DEBUG - limit to first records for testing
            }

            offset += GAMES_CHUNK;
            list = gameService.getOnlineGames(total, offset, GAMES_CHUNK, modes, null, null, null, date);
        }

        logger.debug("Processed " + count + " of " + total);
    }

    private void doRank(OnlineGame game)
    {
        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        logger.debug("id #" + game.getId() + ": " + game.getTournament().getName() + " hosted by " + game.getHostPlayer() +
                     " with " + count + " total players");

        histService.upgradeAllTournamentHistoriesForGame(game, null);//logger);        
    }
}
