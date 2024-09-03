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

import com.donohoedigital.db.*;
import com.donohoedigital.db.dao.impl.*;
import com.donohoedigital.games.poker.dao.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import org.springframework.stereotype.*;

import javax.persistence.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 14, 2008
 * Time: 10:56:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class OnlineGameImplJpa extends JpaBaseDao<OnlineGame, Long> implements OnlineGameDao
{
    //private static final Logger logger = Logger.getLogger(OnlineGameImplJpa.class);

    private static final Date END_OF_TIME = new GregorianCalendar(2099, Calendar.DECEMBER, 31, 23, 23, 59).getTime();
    private static final Date BEGINNING_OF_TIME = new Date(0);

    @SuppressWarnings({"unchecked"})
    public OnlineGame getByKeyAndUrl(String sKey, String sUrl)
    {
        Query query = entityManager.createQuery(
                "select o from OnlineGame o " +
                "where o.licenseKey = :key and o.url = :url");
        query.setParameter("key", sKey);
        query.setParameter("url", sUrl);

        List<OnlineGame> list = (List<OnlineGame>) query.getResultList();
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    @SuppressWarnings({"unchecked"})
    public OnlineGame getByTournamentHistoryId(Long id)
    {
        Query query = entityManager.createQuery(
                "select o from OnlineGame o, TournamentHistory h " +
                "where h.id = :tid and h.onlineGame = o"
        );
        query.setParameter("tid", id);

        List<OnlineGame> list = (List<OnlineGame>) query.getResultList();
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    @SuppressWarnings({"JpaQlInspection"})
    public int getByModeCount(Integer[] modes, String nameSearch, Date begin, Date end)
    {
        String sModes = getModes(modes);
        String dateColumn = getDateColumn(modes);

        // always use dates to take advantage of index on dates
        if (begin == null) begin = BEGINNING_OF_TIME;
        if (end == null) end = END_OF_TIME;

        Query countQuery = entityManager.createQuery(
                "select count(o) from OnlineGame o " +
                "where o.mode in (" + sModes + ") " +
                ((nameSearch != null) ? "and o.hostPlayer like :name " : "") +
                "and o." + dateColumn + " between :begin and :end "
        );

        // specify params
        for (int i = 0; i < modes.length; i++)
        {
            countQuery.setParameter(i + 1, modes[i]);
        }
        countQuery.setParameter("begin", begin);
        countQuery.setParameter("end", end);
        if (nameSearch != null) countQuery.setParameter("name", DBUtils.sqlWildcard(nameSearch));

        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    @SuppressWarnings({"unchecked", "JpaQlInspection"})
    public OnlineGameList getByMode(Integer count, int offset, int pagesize, Integer[] modes,
                                    String nameSearch, Date begin, Date end, boolean bOrderByModeName)
    {
        // always use dates to take advantage of index on dates
        if (begin == null) begin = BEGINNING_OF_TIME;
        if (end == null) end = END_OF_TIME;

        // get count if null
        if (count == null)
        {
            count = getByModeCount(modes, nameSearch, begin, end);
        }

        String sModes = getModes(modes);
        String dateColumn = getDateColumn(modes);
        String sOrderBy = getOrderByClause(modes, bOrderByModeName);

        // create query and set start, max results
        Query query = entityManager.createQuery(
                "select o from OnlineGame o " +
                "where o.mode in (" + sModes + ") " +
                ((nameSearch != null) ? "and o.hostPlayer like :name " : "") +
                "and o." + dateColumn + " between :begin and :end " +
                sOrderBy
        );

        // specify params
        for (int i = 0; i < modes.length; i++)
        {
            query.setParameter(i + 1, modes[i]);
        }
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        if (nameSearch != null) query.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        if (pagesize > 0) query.setMaxResults(pagesize);
        query.setFirstResult(offset);

        // run the query
        List<OnlineGame> results = (List<OnlineGame>) query.getResultList();

        // build results
        OnlineGameList list = new OnlineGameList();
        list.addAll(results);
        list.setTotalSize(count);
        return list;
    }

    /**
     * find earliest type of mode (in the lifecycle of a game)
     */
    private int getEarliestMode(Integer[] modes)
    {
        int earliest = OnlineGame.MODE_END + 1;
        for (int mode : modes)
        {
            if (mode < earliest) earliest = mode;
        }
        return earliest;
    }


    /**
     * turn modes into a string for 'in' clause
     */
    private String getModes(Integer[] modes)
    {
        // number of modes for in 'in' clause
        StringBuilder sbModes = new StringBuilder();
        for (int i = 0; i < modes.length; i++)
        {
            if (sbModes.length() > 0) sbModes.append(", ");
            sbModes.append('?').append(i + 1);
        }
        return sbModes.toString();
    }

    /**
     * Get order by clause
     */
    private String getOrderByClause(Integer[] modes, boolean bOrderByModeName)
    {
        String sOrderBy;
        if (bOrderByModeName)
        {
            sOrderBy = "order by o.mode, o.hostPlayer ";
        }
        else
        {
            // earliest type determines sort order (because start/end date can be null
            // for games not started/not ended)
            switch (getEarliestMode(modes))
            {
                case OnlineGame.MODE_PLAY:
                    sOrderBy = "order by o.startDate desc";
                    break;

                case OnlineGame.MODE_END:
                case OnlineGame.MODE_STOP:
                    sOrderBy = "order by o.endDate desc";
                    break;

                case OnlineGame.MODE_REG:
                default:
                    sOrderBy = "order by o.createDate desc";
            }

        }
        return sOrderBy;
    }

    /**
     * Get date column for query
     */
    private String getDateColumn(Integer[] modes)
    {
        String dateColumn;

        // earliest type determines sort order (because start/end date can be null
        // for games not started/not ended)
        switch (getEarliestMode(modes))
        {
            case OnlineGame.MODE_PLAY:
                dateColumn = "startDate";
                break;

            case OnlineGame.MODE_END:
            case OnlineGame.MODE_STOP:
                dateColumn = "endDate";
                break;

            case OnlineGame.MODE_REG:
            default:
                dateColumn = "createDate";
        }

        return dateColumn;
    }

    /**
     * count of host games
     */
    public int getHostSummaryCount(String nameSearch, Date begin, Date end)
    {
        Query countQuery = entityManager.createQuery(
                "select count(distinct o.hostPlayer) from OnlineGame o " +
                "where o.hostPlayer like :name " +
                ((begin != null && end != null) ? "and o.modifyDate >= :begin and o.modifyDate <= :end " : "")
        );
        if (begin != null && end != null)
        {
            countQuery.setParameter("begin", begin);
            countQuery.setParameter("end", end);
        }
        countQuery.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    /**
     * Online game summary
     */
    @SuppressWarnings({"unchecked"})
    public PagedList<HostSummary> getHostSummary(Integer count, int offset, int pagesize, String nameSearch, Date begin, Date end)
    {
        if (count == null)
        {
            count = getHostSummaryCount(nameSearch, begin, end);
        }

        // query natively since JPA doesn't do order by count well
        Query query = entityManager.createNativeQuery(
                "select wgm_host_player, count(wgm_id) 'cnt', wpr_is_retired " +
                "from wan_game, wan_profile " +
                "where wgm_host_player = wpr_name " +
                "and wgm_host_player like :name " +
                ((begin != null && end != null) ? "and wgm_modify_date >= :begin and wgm_modify_date <= :end " : "") +
                "group by wgm_host_player " +
                "order by cnt desc, wgm_host_player"
        );
        if (begin != null && end != null)
        {
            query.setParameter("begin", begin);
            query.setParameter("end", end);
        }
        query.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        query.setFirstResult(offset);
        query.setMaxResults(pagesize);

        // get results
        List<Object[]> results = (List<Object[]>) query.getResultList();

        // create summary list, set size and translate results
        PagedList<HostSummary> list = new PagedList<>();
        list.setTotalSize(count);
        for (Object[] a : results)
        {
            HostSummary sum = new HostSummary();
            sum.setHostName((String) a[0]);
            sum.setGamesHosted(((Number) a[1]).intValue());
            sum.setRetired(((Boolean) a[2]));
            list.add(sum);
        }
        return list;
    }

    /**
     * Delete all games before given date with given mode.
     */
    public int purge(Date beforeThisDate, Integer mode)
    {
        // delete all tournament histories first
        // need to do this since cascading doesn't
        // work when doing a batch delete
        Query query = entityManager.createQuery(
                "delete from TournamentHistory h " +
                "where h.onlineGame in " +
                "    (select o from OnlineGame o " +
                "     where o.createDate < :date " +
                "     and o.mode = :mode " +
                "    )"
        );
        query.setParameter("date", beforeThisDate);
        query.setParameter("mode", mode);
        query.executeUpdate();

        // delete all games
        query = entityManager.createQuery(
                "delete from OnlineGame o " +
                "where o.createDate < :date " +
                "and o.mode = :mode "
        );
        query.setParameter("date", beforeThisDate);
        query.setParameter("mode", mode);
        return query.executeUpdate();
    }
}