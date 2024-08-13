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
package com.donohoedigital.games.poker.service;

import com.donohoedigital.db.*;
import com.donohoedigital.games.poker.model.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2008
 * Time: 1:37:55 PM
 * To change this template use File | Settings | File Templates.
 */
public interface OnlineProfileService
{
    boolean isNameValid(String sName);

    String generatePassword();

    @Transactional(readOnly = true)
    OnlineProfile getOnlineProfileById(Long id);

    @Transactional(readOnly = true)
    OnlineProfile getOnlineProfileByName(String sName);

    @Transactional(readOnly = true)
    int getMatchingOnlineProfilesCount(String nameSearch, String emailSearch, String keySearch, boolean includeRetired);

    @Transactional(readOnly = true)
    List<OnlineProfile> getMatchingOnlineProfiles(Integer count, int offset, int pagesize, String nameSearch, String emailSearch, String keySearch, boolean includeRetired);

    @Transactional(readOnly = true)
    List<OnlineProfile> getAllOnlineProfilesForEmail(String sEmail, String sExcludeName);

    @Transactional(readOnly = true)
    List<OnlineProfileSummary> getOnlineProfileSummariesForEmail(String sEmail);

    @Transactional
    void retire(String name);

    @Transactional
    boolean saveOnlineProfile(OnlineProfile profile);

    @Transactional
    void deleteOnlineProfile(OnlineProfile p);

    @Transactional
    void deleteOnlineProfiles(List<OnlineProfile> list);

    @Transactional
    OnlineProfile updateOnlineProfile(OnlineProfile profile);

    @Transactional
    OnlineProfile authenticateOnlineProfile(OnlineProfile profile);

    @Transactional
    PagedList<OnlineProfilePurgeSummary> getOnlineProfilePurgeSummary(Integer count, int offset, int pagesize);
}