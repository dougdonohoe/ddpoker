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
package com.donohoedigital.games.poker.dao.impl;

import com.donohoedigital.base.*;
import com.donohoedigital.db.*;
import com.donohoedigital.db.dao.impl.*;
import com.donohoedigital.games.poker.dao.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import org.springframework.stereotype.*;

import javax.persistence.*;
import java.math.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 14, 2008
 * Time: 10:56:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class TournamentHistoryImplJpa extends JpaBaseDao<TournamentHistory, Long> implements TournamentHistoryDao
{
    //private Logger logger = LogManager.getLogger(TournamentHistoryImplJpa.class);

    public int deleteAllForGame(OnlineGame game)
    {
        Query query = entityManager.createQuery(
                "delete from TournamentHistory t " +
                "where t.onlineGame = :game"
        );
        query.setParameter("game", game);
        return query.executeUpdate();
    }

    public int getAllForGameCount(Long gameId)
    {
        Query countQuery = entityManager.createQuery(
                "select count(t) from TournamentHistory t " +
                "where t.onlineGame.id = :id"
        );
        countQuery.setParameter("id", gameId);
        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public TournamentHistoryList getAllForGame(Integer count, int offset, int pagesize, Long gameId)
    {
        Query query = entityManager.createQuery(
                "select t from TournamentHistory t " +
                "where t.onlineGame.id = :id " +
                "order by t.place"
        );
        query.setParameter("id", gameId);
        query.setFirstResult(offset);
        if (pagesize > 0) query.setMaxResults(pagesize);

        // run the queries
        List<TournamentHistory> results = (List<TournamentHistory>) query.getResultList();

        // determine count if not provided
        if (count == null)
        {
            if (pagesize > 0)
            {
                // only run count if we are limiting resultset
                count = getAllForGameCount(gameId);
            }
            else
            {
                count = results.size();
            }
        }

        // return all results
        TournamentHistoryList list = new TournamentHistoryList(results);
        list.setTotalSize(count);

        return list;
    }

    public int getAllForProfileCount(Long id, String nameSearch, Date begin, Date end)
    {
        Query countQuery = entityManager.createQuery(
                "select count(t) from TournamentHistory t " +
                "where t.profile.id = :id " +
                "and t.tournamentName like :name " +
                ((begin != null && end != null) ? "and t.endDate >= :begin and t.endDate <= :end " : "")
        );
        countQuery.setParameter("id", id);
        if (begin != null && end != null)
        {
            countQuery.setParameter("begin", begin);
            countQuery.setParameter("end", end);
        }
        countQuery.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public TournamentHistoryList getAllForProfile(Integer count, int offset, int pagesize, Long id,
                                                  String nameSearch, Date begin, Date end)
    {
        Query query = entityManager.createQuery(
                "select t from TournamentHistory t " +
                "where t.profile.id = :id " +
                "and t.tournamentName like :name " +
                ((begin != null && end != null) ? "and t.endDate >= :begin and t.endDate <= :end " : "") +
                "order by t.endDate desc"
        );
        query.setParameter("id", id);
        if (begin != null && end != null)
        {
            query.setParameter("begin", begin);
            query.setParameter("end", end);
        }
        query.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        query.setFirstResult(offset);
        if (pagesize > 0) query.setMaxResults(pagesize);

        // run the queries
        List<TournamentHistory> results = (List<TournamentHistory>) query.getResultList();

        // determine count if not provided
        if (count == null)
        {
            if (pagesize > 0)
            {
                // only run count if we are limiting resultset
                count = getAllForProfileCount(id, nameSearch, begin, end);
            }
            else
            {
                count = results.size();
            }
        }

        // return all results
        TournamentHistoryList list = new TournamentHistoryList(results);
        list.setTotalSize(count);

        return list;
    }

    public int getLeaderboardCount(int games_limit, String nameSearch, Date begin, Date end)
    {
        boolean empty = Utils.isEmpty(nameSearch);

        // use native query since JPA Query doesn't support subclause in FROM
        Query countQuery = entityManager.createNativeQuery(
                "select count(*) from " +
                "  (select count(1) " +
                "   FROM wan_history, wan_profile " +
                "   WHERE wan_history.whi_profile_id = wan_profile.wpr_id " +
                "     AND whi_player_type in (:type1, :type2) " +
                "     AND whi_is_ended = TRUE " +
                "     AND wpr_is_retired = FALSE " +
                "     AND whi_end_date >= :begin AND whi_end_date <= :end " +
                (!empty ? " AND wpr_name like :name " : "") +
                "   GROUP BY wpr_name " +
                "   HAVING count(whi_profile_id) >= :game_limit" +
                "  ) foo"
        );
        countQuery.setParameter("type1", TournamentHistory.PLAYER_TYPE_ONLINE);
        countQuery.setParameter("type2", TournamentHistory.PLAYER_TYPE_AI);
        countQuery.setParameter("begin", begin);
        countQuery.setParameter("end", end);
        countQuery.setParameter("game_limit", (long) games_limit);
        if (!empty) countQuery.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        BigInteger countResult = (BigInteger) countQuery.getSingleResult();

        return countResult.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public LeaderboardSummaryList getLeaderboard(Integer count, int offset, int pagesize, boolean sortByROI, int games_limit,
                                                 String nameSearch, Date begin, Date end)
    {
        boolean empty = Utils.isEmpty(nameSearch);

        if (count == null)
        {
            count = getLeaderboardCount(games_limit, nameSearch, begin, end);
        }

        // user native query since a pain in the ass in JPA Query Language
        // note: need to join with wan_profile to get correct profile name (e.g., for AI players)
        Query query = entityManager.createNativeQuery(
                "SELECT count(whi_profile_id) 'gamesplayed', " +
                "       whi_profile_id, " +
                "       wpr_name, \n" +
                "       avg(whi_rank_1) 'rank1', \n" +
                "       sum(whi_prize-(whi_buy_in+whi_total_rebuy+whi_total_add_on)) / \n" +
                "           sum(whi_buy_in+whi_total_rebuy+whi_total_add_on)*100 'roi', \n" +
                "       sum(whi_total_add_on), \n" +
                "       sum(whi_total_rebuy), \n" +
                "       sum(whi_buy_in),\n" +
                "       sum(whi_prize) \n" +
                "FROM wan_history, wan_profile\n" +
                "WHERE wan_history.whi_profile_id = wan_profile.wpr_id \n" +
                "  AND whi_player_type in (:type1, :type2) " +
                "  AND whi_is_ended = TRUE " +
                "  AND wpr_is_retired = FALSE " +
                "  AND whi_end_date >= :begin AND whi_end_date <= :end " +
                (!empty ? " AND wpr_name like :name " : "") +
                "GROUP BY wpr_name " +
                "HAVING count(whi_profile_id) >= :game_limit " +
                (sortByROI ? "ORDER BY roi DESC, wpr_name" :
                 "ORDER BY rank1 DESC, wpr_name")
        );
        query.setParameter("type1", TournamentHistory.PLAYER_TYPE_ONLINE);
        query.setParameter("type2", TournamentHistory.PLAYER_TYPE_AI);
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        query.setParameter("game_limit", (long) games_limit);
        if (!empty) query.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        query.setFirstResult(offset);
        query.setMaxResults(pagesize);

        // get results
        List<Object[]> results = query.getResultList();

        // create summary list, set size and translate results
        LeaderboardSummaryList list = new LeaderboardSummaryList();
        list.setTotalSize(count);
        int row = 0;
        for (Object[] a : results)
        {
            LeaderboardSummary sum = new LeaderboardSummary();
            sum.setGamesPlayed(((Number) a[0]).intValue());
            sum.setProfileId((Integer) a[1]);
            sum.setPlayerName((String) a[2]);
            sum.setDdr1(((Number) a[3]).intValue());
            // ignore roi a[4]
            sum.setTotalBuyin(((Number) a[5]).intValue());
            sum.setTotalAddon(((Number) a[6]).intValue());
            sum.setTotalRebuys(((Number) a[7]).intValue());
            sum.setTotalPrizes(((Number) a[8]).intValue());

            int rank = offset + row + 1;
            int percentile = 100 - (rank * 100 / count);

            sum.setRank(rank);
            sum.setPercentile(percentile);

            list.add(sum);
            row++;
        }

        return list;
    }
}
