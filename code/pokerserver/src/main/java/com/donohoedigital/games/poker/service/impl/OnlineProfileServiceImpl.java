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

import com.donohoedigital.base.*;
import com.donohoedigital.db.*;
import com.donohoedigital.games.poker.dao.*;
import com.donohoedigital.games.poker.model.*;
import static com.donohoedigital.games.poker.model.OnlineProfile.*;
import com.donohoedigital.games.poker.service.*;
import com.donohoedigital.games.poker.service.helper.*;
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
 * Online profile service
 */
@Service("onlineProfileService")
public class OnlineProfileServiceImpl implements OnlineProfileService
{
    @Autowired
    private OnlineProfileDao dao;

    private DisallowedManager disallowed = new DisallowedManager();

    /**
     * Is name valid?
     */
    public boolean isNameValid(String sName)
    {
        return disallowed.isNameValid(sName);
    }

    /**
     * Generate a profile password.
     */
    public String generatePassword()
    {
        return passwordGenerator_.generatePassword(PASSWORD_LENGTH);
    }

    @Transactional(readOnly = true)
    public OnlineProfile getOnlineProfileById(Long id)
    {
        return dao.get(id);
    }

    @Transactional(readOnly = true)
    public OnlineProfile getOnlineProfileByName(String sName)
    {
        ensureDummysLoaded(); // ensure dummy's are created
        return dao.getByName(sName);
    }

    @Transactional(readOnly = true)
    public int getMatchingOnlineProfilesCount(String nameSearch, String emailSearch, String keySearch, boolean includeRetired)
    {
        return dao.getMatchingCount(nameSearch, emailSearch, keySearch, includeRetired);
    }

    @Transactional(readOnly = true)
    public List<OnlineProfile> getMatchingOnlineProfiles(Integer count, int offset, int pagesize,
                                                         String nameSearch, String emailSearch, String keySearch,
                                                         boolean includeRetired)
    {
        return dao.getMatching(count, offset, pagesize, nameSearch, emailSearch, keySearch, includeRetired);
    }

    @Transactional(readOnly = true)
    public List<OnlineProfile> getAllOnlineProfilesForEmail(String sEmail, String sExcludeName)
    {
        return dao.getAllForEmail(sEmail, sExcludeName);
    }

    @Transactional(readOnly = true)
    public List<OnlineProfileSummary> getOnlineProfileSummariesForEmail(String sEmail)
    {
        return dao.getOnlineProfileSummaryForEmail(sEmail);
    }

    @Transactional
    public void retire(String name)
    {
        OnlineProfile profile = dao.getByName(name);
        profile.setRetired(true);
        profile.setActivated(false);
        profile.setPassword("__retired__");
    }

    @Transactional
    public boolean saveOnlineProfile(OnlineProfile profile)
    {
        ensureDummysLoaded(); // ensure dummy's are created to prevent profile taking the name

        // if a profile exists that matches, return false
        OnlineProfile exist = dao.getByName(profile.getName());
        if (exist != null)
        {
            return false;
        }
        dao.save(profile);
        return true;
    }

    @Transactional
    public void deleteOnlineProfile(OnlineProfile p)
    {
        dao.delete(p);
    }

    @Transactional
    public void deleteOnlineProfiles(List<OnlineProfile> list)
    {
        if (list == null) return;

        for (OnlineProfile p : list)
        {
            dao.delete(p);
        }
    }

    @Transactional
    public OnlineProfile updateOnlineProfile(OnlineProfile profile)
    {
        return dao.update(profile);
    }

    @Transactional(readOnly = true)
    public OnlineProfile authenticateOnlineProfile(OnlineProfile profile)
    {
        OnlineProfile lookup = getOnlineProfileByName(profile.getName());
        if (lookup != null && (lookup.getPassword().equals(profile.getPassword()) && !lookup.isRetired()))
        {
            return lookup;
        }
        return null;
    }

    @Transactional
    public PagedList<OnlineProfilePurgeSummary> getOnlineProfilePurgeSummary(Integer count, int offset, int pagesize)
    {
        return dao.getOnlineProfilePurgeSummary(count, offset, pagesize);
    }

    ///
    /// internal support methods
    ///

    private static final int PASSWORD_LENGTH = 8;

    private boolean dummysLoaded = false;
    private PasswordGenerator passwordGenerator_ = new PasswordGenerator(
            (PasswordGenerator.OPTION_INCLUDE_LETTERS |
             PasswordGenerator.OPTION_INCLUDE_NUMBERS |
             PasswordGenerator.OPTION_EXCLUDE_SIMILAR), null);

    /**
     * Get dummys ... lazily create if null.  Synchronized
     * so thread safe.
     */
    private synchronized void ensureDummysLoaded()
    {
        if (!dummysLoaded)
        {
            for (Dummy dummy : Dummy.values())
            {
                dao.getDummy(dummy);
            }
            dummysLoaded = true;
        }
    }
}