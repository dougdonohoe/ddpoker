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
package com.donohoedigital.games.poker.service;

import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.db.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2008
 * Time: 1:37:55 PM
 * To change this template use File | Settings | File Templates.
 */
public interface OnlineGameService
{
    @Transactional(readOnly = true)
    int getOnlineGamesCount(Integer[] modes, String nameSearch, Date begin, Date end);

    enum OrderByType
    {
        mode, date
    }

    @Transactional(readOnly = true)
    OnlineGameList getOnlineGames(Integer count, int offset, int pagesize, Integer[] modes, 
                                  String nameSearch, Date begin, Date end, OrderByType orderByType);

    @Transactional(readOnly = true)
    OnlineGameList getOnlineGamesAndHistoriesForDay(Integer[] modes, Date begin, Date end);

    @Transactional(readOnly = true)
    OnlineGame getOnlineGameById(Long id);

    @Transactional(readOnly = true)
    OnlineGame getOnlineGameByTournamentHistoryId(Long id);

    @Transactional(readOnly = true)
    int getHostSummaryCount(String nameSearch, Date begin, Date end);

    @Transactional(readOnly = true)
    PagedList<HostSummary> getHostSummary(Integer count, int offset, int pagesize, String nameSearch, Date begin, Date end);

    @Transactional
    void saveOnlineGame(OnlineGame game);

    @Transactional
    OnlineGame updateOnlineGame(OnlineGame game);

    @Transactional
    OnlineGame updateOnlineGame(OnlineGame game, TournamentHistoryList list);

    @Transactional
    void deleteOnlineGame(OnlineGame game);

    @Transactional
    int purgeGames(Date date, Integer mode);
}