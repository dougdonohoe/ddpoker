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
package com.donohoedigital.games.poker.dao.impl;

import com.donohoedigital.db.DBUtils;
import com.donohoedigital.db.PagedList;
import com.donohoedigital.db.dao.impl.JpaBaseDao;
import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.OnlineProfilePurgeSummary;
import com.donohoedigital.games.poker.model.OnlineProfileSummary;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 13, 2008
 * Time: 10:48:53 AM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class OnlineProfileImplJpa extends JpaBaseDao<OnlineProfile, Long> implements OnlineProfileDao
{
    //private static Logger logger = LogManager.getLogger(OnlineProfileImplJpa.class);

    @SuppressWarnings({"unchecked"})
    public OnlineProfile getByName(String sName)
    {
        if (sName == null) return null;

        Query query = entityManager.createQuery(
                "select o from OnlineProfile o " +
                "where o.name = :name");
        query.setParameter("name", sName);

        List<OnlineProfile> list = (List<OnlineProfile>) query.getResultList();
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    @SuppressWarnings({"unchecked"})
    public List<OnlineProfile> getAllForEmail(String sEmail, String sExcludeName)
    {
        if (sEmail == null) return null;
        if (sExcludeName == null) sExcludeName = "";

        Query query = entityManager.createQuery(
                "select o from OnlineProfile o " +
                "where o.email = :email " +
                "and o.name <> :name " +
                "and o.retired = false " +
                "order by o.name");
        query.setParameter("email", sEmail);
        query.setParameter("name", sExcludeName);

        return (List<OnlineProfile>) query.getResultList();
    }

    @SuppressWarnings({"unchecked"})
    public List<OnlineProfileSummary> getOnlineProfileSummaryForEmail(String email)
    {
        Query query = entityManager.createNativeQuery(
                "select wpr_name, count(whi_id) as count " +
                "from wan_profile left outer join wan_history on (wpr_id = whi_profile_id)" +
                "where wpr_email = :email " +
                "and wpr_is_retired = false " +
                "group by wpr_name order by count desc;");
        query.setParameter("email", email);

        // get results
        List<OnlineProfileSummary> list = new ArrayList<OnlineProfileSummary>();
        List<Object[]> results = query.getResultList();
        for (Object[] a : results)
        {
            OnlineProfileSummary sum = new OnlineProfileSummary(
                    (String) a[0],
                    ((Number) a[1]).intValue()
            );
            list.add(sum);
        }

        return list;
    }

    public int getMatchingCount(String nameSearch, String emailSearch, String keySearch, boolean includeRetired)
    {
        Query countQuery = entityManager.createQuery(
                "select count(o) from OnlineProfile o " +
                "where o.licenseKey like :key " +
                "  AND o.name like :name " +
                "  AND o.email like :email " +
                (!includeRetired ? " AND o.retired = false " : ""));
        countQuery.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        countQuery.setParameter("email", DBUtils.sqlWildcard(emailSearch));
        countQuery.setParameter("key", DBUtils.sqlWildcard(keySearch));

        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public PagedList<OnlineProfile> getMatching(Integer count, int offset, int pagesize,
                                                String nameSearch, String emailSearch, String keySearch, boolean includeRetired)
    {
        if (count == null)
        {
            count = getMatchingCount(nameSearch, emailSearch, keySearch, includeRetired);
        }

        Query query = entityManager.createQuery(
                "select o from OnlineProfile o " +
                "where o.licenseKey like :key " +
                "  AND o.name like :name " +
                "  AND o.email like :email " +
                (!includeRetired ? "  AND o.retired = false " : "") +
                "order by o.name");
        query.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        query.setParameter("email", DBUtils.sqlWildcard(emailSearch));
        query.setParameter("key", DBUtils.sqlWildcard(keySearch));
        query.setFirstResult(offset);
        query.setMaxResults(pagesize);

        List<OnlineProfile> list = (List<OnlineProfile>) query.getResultList();

        PagedList<OnlineProfile> pList = new PagedList<OnlineProfile>(list.size());
        pList.addAll(list);
        pList.setTotalSize(count);
        return pList;
    }

    public OnlineProfile getDummy(OnlineProfile.Dummy dummy)
    {
        OnlineProfile profile = getByName(dummy.getName());
        if (profile == null)
        {
            profile = new OnlineProfile(dummy.getName());
            profile.setLicenseKey(PokerConstants.DUMMY_PROFILE_KEY_START + dummy.ordinal());
            profile.setPassword("!!DUMMY!!");
            profile.setEmail("dummy-profile-" + dummy.ordinal() + "@example.com");
            profile.setActivated(true);

            save(profile);
        }
        return profile;
    }

    @SuppressWarnings({"unchecked"})
    public PagedList<OnlineProfilePurgeSummary> getOnlineProfilePurgeSummary(Integer count, int offset, int pagesize)
    {
        // count is simply all online profiles
        if (count == null)
        {
            Query countQuery = entityManager.createQuery(
                    "select count(o) from OnlineProfile o ");
            Long rowCount = (Long) countQuery.getSingleResult();
            count = rowCount.intValue();
        }

        // get profile info along with count of histories for that profile
        // using native since JPA queries are fraking impossible to write
        Query query = entityManager.createNativeQuery(
                "select wpr_id, wpr_license_key, wpr_name, wpr_modify_date, count(whi_profile_id) 'num'" +
                "        from wan_profile left outer join wan_history on (wpr_id = whi_profile_id)" +
                "        group by (wpr_id)" +
                "        order by wpr_license_key, num desc");
        query.setFirstResult(offset);
        query.setMaxResults(pagesize);

        // get results
        List<Object[]> results = query.getResultList();

        // create summary list, set size and translate results
        PagedList<OnlineProfilePurgeSummary> list = new PagedList<OnlineProfilePurgeSummary>(results.size());
        list.setTotalSize(count);

        for (Object[] a : results)
        {
            OnlineProfilePurgeSummary sum = new OnlineProfilePurgeSummary();
            OnlineProfile p = new OnlineProfile();

            p.setId((long) ((Integer) a[0]));
            p.setLicenseKey((String) a[1]);
            p.setName((String) a[2]);
            p.setModifyDate((Date) a[3]);
            sum.setOnlineProfile(p);
            sum.setHistoryCount(((Number) a[4]).intValue());
            list.add(sum);
        }

        return list;
    }
}
