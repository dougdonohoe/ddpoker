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
package com.donohoedigital.games.server.dao.impl;

import com.donohoedigital.db.DBUtils;
import com.donohoedigital.db.PagedList;
import com.donohoedigital.db.dao.impl.JpaBaseDao;
import com.donohoedigital.games.server.RegDayOfYearCount;
import com.donohoedigital.games.server.RegHourCount;
import com.donohoedigital.games.server.dao.RegistrationDao;
import com.donohoedigital.games.server.model.Registration;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 13, 2008
 * Time: 10:48:53 AM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class RegistrationImplJpa extends JpaBaseDao<Registration, Long> implements RegistrationDao
{
    public int countOperatingSystem(String keyStart, String os)
    {
        os = '%' + os + '%';
        return countWhere(keyStart, "t.operatingSystem like :param", os);
    }

    public int countRegistrationType(String keyStart, Registration.Type type)
    {
        return countWhere(keyStart, "t.type = :param", type);
    }

    public int countKeysInUse(String keyStart)
    {
        return countWhere(keyStart, null, null);
    }

    private int countWhere(String keyStart, String sWhere, Object param)
    {
        if (sWhere == null) sWhere = "1 = 1";
        if (keyStart == null) keyStart = "%";
        else keyStart += "%";
        Query query = entityManager.createQuery(
                "select count(distinct t.licenseKey) from Registration t " +
                "where t.licenseKey not in " +
                "(select b.key from BannedKey b) " +
                "AND t.duplicate = FALSE " +
                "AND t.licenseKey like :key " +
                "AND " + sWhere
        );
        query.setParameter("key", keyStart);
        if (param != null) query.setParameter("param", param);
        Long count = (Long) query.getSingleResult();
        return count.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public List<RegDayOfYearCount> countByDayOfYear(String keyStart)
    {
        if (keyStart == null) keyStart = "%";
        else keyStart += "%";
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(distinct reg_license_key), DAYOFYEAR(reg_server_time), YEAR(reg_server_time)\n" +
                "FROM registration\n" +
                "where reg_is_duplicate = false\n" +
                "and reg_license_key not in (select ban_key from banned_key)\n" +
                "and reg_license_key like :key\n" +
                "GROUP BY DAYOFYEAR(reg_server_time), YEAR(reg_server_time)\n" +
                "ORDER BY reg_server_time;"
        );
        query.setParameter("key", keyStart);
        List<Object[]> results = query.getResultList();
        List<RegDayOfYearCount> list = new ArrayList<RegDayOfYearCount>(results.size());
        for (Object[] objs : results)
        {
            list.add(new RegDayOfYearCount(
                    ((Number) objs[0]).intValue(),
                    ((Number) objs[1]).intValue(),
                    ((Number) objs[2]).intValue()
            ));
        }
        return list;
    }

    @SuppressWarnings({"unchecked"})
    public List<RegHourCount> countByHour(String keyStart)
    {
        // To limit to a range, compare reg_server_time > date
        //long now = System.currentTimeMillis(); //
        //long limit = 1000l * 60l * 60l * 24l * 35l; // 35 days
        //Calendar c = Calendar.getInstance();
        //c.setTime(new Date(now - limit));

        if (keyStart == null) keyStart = "%";
        else keyStart += "%";
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(distinct reg_license_key), HOUR(reg_server_time)\n" +
                "FROM registration\n" +
                "where reg_is_duplicate = false\n" +
                "and reg_license_key not in (select ban_key from banned_key)\n" +
                "and reg_license_key like :key\n" +
                "GROUP BY HOUR(reg_server_time)"
        );
        query.setParameter("key", keyStart);
        List<Object[]> results = query.getResultList();
        List<RegHourCount> list = new ArrayList<RegHourCount>(results.size());
        for (Object[] objs : results)
        {
            list.add(new RegHourCount(
                    ((Number) objs[0]).intValue(),
                    ((Number) objs[1]).intValue()
            ));
        }
        return list;
    }

    public int getMatchingCount(String nameSearch, String emailSearch, String keySearch, String addressSearch)
    {
        Query countQuery = entityManager.createQuery(
                "select count(t) from Registration t " +
                "where t.licenseKey like :key " +
                "  AND t.name like :name " +
                "  AND t.email like :email " +
                "  AND t.address like :address "
        );
        countQuery.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        countQuery.setParameter("email", DBUtils.sqlWildcard(emailSearch));
        countQuery.setParameter("key", DBUtils.sqlWildcard(keySearch));
        countQuery.setParameter("address", DBUtils.sqlWildcard(addressSearch));
        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    @SuppressWarnings({"unchecked"})
    public PagedList<Registration> getMatching(Integer count, int offset, int pagesize,
                                               String nameSearch, String emailSearch, String keySearch, String addressSearch)
    {
        if (count == null)
        {
            count = getMatchingCount(nameSearch, emailSearch, keySearch, addressSearch);
        }

        Query query = entityManager.createQuery(
                "select t from Registration t " +
                "where t.licenseKey like :key " +
                "  AND t.name like :name " +
                "  AND t.email like :email " +
                "  AND t.address like :address " +
                " order by t.licenseKey"
        );
        query.setParameter("name", DBUtils.sqlWildcard(nameSearch));
        query.setParameter("email", DBUtils.sqlWildcard(emailSearch));
        query.setParameter("key", DBUtils.sqlWildcard(keySearch));
        query.setParameter("address", DBUtils.sqlWildcard(addressSearch));
        query.setFirstResult(offset);
        query.setMaxResults(pagesize);

        List<Registration> list = (List<Registration>) query.getResultList();

        PagedList<Registration> pList = new PagedList<Registration>(list.size());
        pList.addAll(list);
        pList.setTotalSize(count);
        return pList;
    }

    @SuppressWarnings({"unchecked"})
    public List<Registration> getAllForKey(String sKey)
    {
        Query query = entityManager.createQuery(
                "select t from Registration t " +
                "where t.licenseKey = :key"
        );
        query.setParameter("key", sKey);

        return (List<Registration>) query.getResultList();
    }

    @SuppressWarnings({"unchecked"})
    public List<Registration> getAllBanned()
    {
        Query query = entityManager.createQuery(
                "select t from Registration t " +
                "where t.licenseKey in " +
                "(select b.key from BannedKey b)"
        );
        return (List<Registration>) query.getResultList();
    }

    @SuppressWarnings({"unchecked", "JpaQueryApiInspection"})
    public List<String> getAllSuspectKeys(int nMin)
    {
        String intelliJisDumb = "";
        Query query = entityManager.createQuery(
                "select t.licenseKey from Registration t " +
                "where t.licenseKey not in " +
                "(select b.key from BannedKey b) " +
                "and t.duplicate = false " +
                "group by t.licenseKey " +
                "having count(t.licenseKey) >= :min" + // IntelliJ wrongly flags an error here as of 7.0.3
                intelliJisDumb                         // so add an empty string to disable error checking
        );
        query.setParameter("min", (long) nMin);
        return (List<String>) query.getResultList();
    }

    public boolean isDuplicate(Registration reg)
    {
        Query query;
        if (reg.isRegistration())
        {
            query = entityManager.createQuery(
                    "select count(t.id) from Registration t " +
                    "where t.licenseKey = :key and t.duplicate = false " +
                    "and t.type = :type " +
                    "and (    t.email = :email " +
                    "or t.ip = :ip " +
                    "or t.hostNameModified = :host " +
                    ')');
            query.setParameter("key", reg.getLicenseKey());
            query.setParameter("type", Registration.Type.REGISTRATION);
            query.setParameter("email", reg.getEmail());
            query.setParameter("ip", reg.getIp());
            // okay if host is null since (hostNameModified = null) is never true in SQL
            query.setParameter("host", reg.getHostNameModified());
        }
        else
        {
            query = entityManager.createQuery(
                    "select count(t.id) from Registration t " +
                    "where t.licenseKey = :key and t.duplicate = false " +
                    "and (    t.ip = :ip " +
                    "      or t.hostNameModified = :host " +
                    ')');
            query.setParameter("key", reg.getLicenseKey());
            query.setParameter("ip", reg.getIp());
            query.setParameter("host", reg.getHostNameModified());
        }
        long count = (Long) query.getSingleResult();
        return count > 0;
    }

    public int markDuplicates(Registration reg)
    {
        if (!reg.isRegistration() || reg.isDuplicate()) return 0;

        Query query = entityManager.createQuery(
                "update Registration t set t.duplicate = true " +
                "where t.licenseKey = :key and t.duplicate = false " +
                "and t.id <> :id " +
                "and (    t.email = :email " +
                "or t.ip = :ip " +
                "or t.hostNameModified = :host " +
                ')');
        Long id = reg.getId();
        if (id == null) id = -1L; // need to specify id since (id <> null) is always false
        query.setParameter("id", id);
        query.setParameter("key", reg.getLicenseKey());
        query.setParameter("email", reg.getEmail());
        query.setParameter("ip", reg.getIp());
        query.setParameter("host", reg.getHostNameModified());
        return query.executeUpdate();
    }
}