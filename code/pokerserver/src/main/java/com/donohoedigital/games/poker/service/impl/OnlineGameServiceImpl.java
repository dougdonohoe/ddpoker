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
package com.donohoedigital.games.poker.service.impl;

import com.donohoedigital.db.*;
import com.donohoedigital.games.poker.dao.*;
import com.donohoedigital.games.poker.model.*;
import static com.donohoedigital.games.poker.model.OnlineProfile.*;
import static com.donohoedigital.games.poker.model.TournamentHistory.*;
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.service.*;
import org.apache.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 23, 2008
 * Time: 3:22:57 PM
 * <p/>
 * Online game service operates on OnlineGames that come from the client,
 * which doesn't pass down the database id.  Hence, most of the writable
 * modifications first check for existence using the unique key of
 * url and license key.
 */
@Service("onlineGameService")
public class OnlineGameServiceImpl implements OnlineGameService
{
    private Logger logger = Logger.getLogger(OnlineGameServiceImpl.class);

    private OnlineGameDao gameDao;
    private OnlineProfileDao profileDao;
    private TournamentHistoryDao histDao;

    @Autowired
    public void setOnlineGameDao(OnlineGameDao dao)
    {
        gameDao = dao;
    }

    @Autowired
    public void setOnlineGameDao(OnlineProfileDao dao)
    {
        profileDao = dao;
    }

    @Autowired
    public void setTournamentHistoryDao(TournamentHistoryDao dao)
    {
        histDao = dao;
    }

    @Transactional(readOnly = true)
    public int getOnlineGamesCount(Integer[] modes, String nameSearch, Date begin, Date end)
    {
        return gameDao.getByModeCount(modes, nameSearch, begin, end);
    }

    @Transactional(readOnly = true)
    public OnlineGameList getOnlineGames(Integer count, int offset, int pagesize, Integer[] modes,
                                         String nameSearch, Date begin, Date end, OrderByType orderByType)
    {
        return gameDao.getByMode(count, offset, pagesize, modes, nameSearch, begin, end, orderByType == OrderByType.mode);
    }

    @Transactional(readOnly = true)
    public OnlineGameList getOnlineGamesAndHistoriesForDay(Integer[] modes, Date begin, Date end)
    {
        OnlineGameList list = gameDao.getByMode(null, 0, 10000, modes, null, begin, end, false);

        // make sure histories are loaded (need to invoke a method on them to be sure)
        for (OnlineGame game : list)
        {
            game.getHistories().size();
        }

        return list;
    }

    @Transactional(readOnly = true)
    public OnlineGame getOnlineGameById(Long id)
    {
        return gameDao.get(id);
    }

    @Transactional(readOnly = true)
    public OnlineGame getOnlineGameByTournamentHistoryId(Long id)
    {
        return gameDao.getByTournamentHistoryId(id);
    }

    @Transactional(readOnly = true)
    public int getHostSummaryCount(String nameSearch, Date begin, Date end)
    {
        return gameDao.getHostSummaryCount(nameSearch, begin, end);
    }

    @Transactional(readOnly = true)
    public PagedList<HostSummary> getHostSummary(Integer count, int offset, int pagesize, String nameSearch, Date begin, Date end)
    {
        return gameDao.getHostSummary(count, offset, pagesize, nameSearch, begin, end);
    }

    @Transactional
    public void saveOnlineGame(OnlineGame game)
    {
        // if a game exists that matches, delete it first
        OnlineGame exist = gameDao.getByKeyAndUrl(game.getLicenseKey(), game.getUrl());
        if (exist != null)
        {
            gameDao.delete(exist);
            gameDao.flush(); // flush otherwise we'll get a db constraint violation
        }
        gameDao.save(game);
    }

    @Transactional
    public OnlineGame updateOnlineGame(OnlineGame game)
    {
        // if nothing exists to update, return null.  This can happen due
        // to an existing bug in 2.5p3 and below where non-hosts send updates
        // when other players quit (OnlineManager.processQuit() bug)
        OnlineGame exist = gameDao.getByKeyAndUrl(game.getLicenseKey(), game.getUrl());
        if (exist == null)
        {
            return null;
        }
        else
        {
            // copy over attributes ... do this since client
            // doesn't send down all attributes, so some can be
            // nulled out
            exist.merge(game);
            return gameDao.update(exist);
        }
    }

    @Transactional
    public OnlineGame updateOnlineGame(OnlineGame game, TournamentHistoryList list)
    {
        game = updateOnlineGame(game);
        if (game != null)
        {
            insertTournamentHistories(game, list);
        }
        return game;
    }

    @Transactional
    public void deleteOnlineGame(OnlineGame game)
    {
        // only delete if a game exists that matches
        OnlineGame exist = gameDao.getByKeyAndUrl(game.getLicenseKey(), game.getUrl());
        if (exist != null)
        {
            gameDao.delete(exist);
        }
    }

    @Transactional
    public int purgeGames(Date date, Integer mode)
    {
        return gameDao.purge(date, mode);
    }

    ////
    //// helper methods
    ////

    private void insertTournamentHistories(OnlineGame game, TournamentHistoryList histories)
    {
        if (histories == null || histories.isEmpty()) return;

        // calculate rank
        histories.calculateInfo(game.getMode() == OnlineGame.MODE_END, false);

        // remove existing histories
        histDao.deleteAllForGame(game);

        // insert each
        boolean bFirstAI = true;
        for (TournamentHistory history : histories)
        {
            OnlineProfile profile = null;

            if (history.getPlayerType() == PLAYER_TYPE_ONLINE)
            {
                profile = profileDao.getByName(history.getPlayerName());

                if (profile == null)
                {
                    // No associated profile.  Will only happen if the client profile becomes out of
                    // sync with the server profile or someone altered their profile definition.
                    logger.warn("Missing profile in history: " + history.getPlayerName() +
                                " for game id=" + game.getId());
                    history.setPlayerType(PLAYER_TYPE_LOCAL); // force local
                }
            }

            // no profile - either an AI or a player that joined from a direct URL
            if (profile == null)
            {
                if (history.getPlayerType() == PLAYER_TYPE_AI)
                {
                    if (bFirstAI)
                    {
                        bFirstAI = false;
                        profile = profileDao.getDummy(Dummy.AI_BEST);
                    }
                    else
                    {
                        profile = profileDao.getDummy(Dummy.AI_REST);
                    }
                }
                else // TournamentHistory.PLAYER_TYPE_LOCAL
                {
                    profile = profileDao.getDummy(Dummy.HUMAN);
                }
            }

            history.setId(null); // clear id sent from client
            history.setEndDate(new Date()); // override date sent from client
            history.setGame(game);
            history.setTournamentName(game.getTournament().getName().trim());
            history.setNumPlayers(histories.size());
            history.setProfile(profile);

            histDao.save(history);
        }
    }
}
