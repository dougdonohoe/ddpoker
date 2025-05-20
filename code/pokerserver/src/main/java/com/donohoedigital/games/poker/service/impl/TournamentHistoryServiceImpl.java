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
package com.donohoedigital.games.poker.service.impl;

import com.donohoedigital.games.poker.dao.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.service.*;
import org.apache.logging.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 25, 2008
 * Time: 5:13:18 PM
 * To change this template use File | Settings | File Templates.
 */
@Service("tournamentHistoryService")
public class TournamentHistoryServiceImpl implements TournamentHistoryService
{
    private TournamentHistoryDao dao;

    @Autowired
    public void setTournamentHistoryDao(TournamentHistoryDao dao)
    {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public int getAllTournamentHistoriesForGameCount(Long gameId)
    {
        return dao.getAllForGameCount(gameId);
    }

    @Transactional(readOnly = true)
    public TournamentHistoryList getAllTournamentHistoriesForGame(Integer count, int offset, int pagesize, Long gameId)
    {
        return dao.getAllForGame(count, offset, pagesize, gameId);
    }

    @Transactional
    public void upgradeAllTournamentHistoriesForGame(OnlineGame game, Logger logger)
    {
        TournamentHistoryList list = dao.getAllForGame(null, 0, -1, game.getId());

        String name = game.getTournament().getName().trim();
        int num = dao.getAllForGameCount(game.getId());

        // determine game name and count of players
        for (TournamentHistory hist : list)
        {
            if (hist.getNumPlayers() != num || !hist.getTournamentName().equals(name))
            {
                if (logger != null) logger.info("  UPDATING history " + hist.getId() + ": "+ name + " ("+num+" players)");
                hist.setNumPlayers(num);
                hist.setTournamentName(name);
            }
        }
    }

    @Transactional(readOnly = true)
    public int getAllTournamentHistoriesForProfileCount(Long id, String nameSearch, Date begin, Date end)
    {
        return dao.getAllForProfileCount(id, nameSearch, begin, end);
    }

    @Transactional(readOnly = true)
    public TournamentHistoryList getAllTournamentHistoriesForProfile(Integer count, int offset, int pagesize, Long id,
                                                                     String nameSearch, Date begin, Date end)
    {
        return dao.getAllForProfile(count, offset, pagesize, id, nameSearch, begin, end);
    }

    @Transactional(readOnly = true)
    public int getLeaderboardCount(int games_limit, String nameSearch, Date begin, Date end)
    {
        return dao.getLeaderboardCount(games_limit, nameSearch, begin, end);
    }

    @Transactional(readOnly = true)
    public LeaderboardSummaryList getLeaderboard(Integer count, int offset, int pagesize, LeaderboardType type, int games_limit, String nameSearch, Date begin, Date end)
    {
        return dao.getLeaderboard(count, offset, pagesize, type == LeaderboardType.roi, games_limit, nameSearch, begin, end);
    }
}
