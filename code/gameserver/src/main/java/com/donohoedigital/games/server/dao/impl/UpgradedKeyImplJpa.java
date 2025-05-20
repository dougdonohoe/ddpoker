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
package com.donohoedigital.games.server.dao.impl;

import com.donohoedigital.db.dao.impl.JpaBaseDao;
import com.donohoedigital.games.server.dao.UpgradedKeyDao;
import com.donohoedigital.games.server.model.UpgradedKey;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 14, 2008
 * Time: 10:56:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class UpgradedKeyImplJpa extends JpaBaseDao<UpgradedKey, Long> implements UpgradedKeyDao
{
    @SuppressWarnings({"unchecked"})
    public UpgradedKey getByKey(String sKey)
    {
        if (sKey == null) return null;

        Query query = entityManager.createQuery(
                "select u from UpgradedKey u " +
                "where u.licenseKey = :key");
        query.setParameter("key", sKey);

        List<UpgradedKey> list = (List<UpgradedKey>) query.getResultList();
        if (list.size() == 0) return null;
        return list.get(0);
    }

    /**
     * Override to sort by count and modify date
     */
    @SuppressWarnings({"unchecked"})
    public List<UpgradedKey> getAll()
    {
        return entityManager.createQuery("select u from UpgradedKey u " +
                                         "order by u.count desc, u.modifyDate desc").getResultList();
    }
}