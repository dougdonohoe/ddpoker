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
package com.donohoedigital.games.server.dao;

import com.donohoedigital.db.*;
import com.donohoedigital.db.dao.*;
import com.donohoedigital.games.server.*;
import com.donohoedigital.games.server.model.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 13, 2008
 * Time: 10:38:30 AM
 * <p/>
 * Registration DAO
 */
public interface RegistrationDao extends BaseDao<Registration, Long>
{
    /**
     * count logic
     */
    int countOperatingSystem(String keyStart, String os);

    int countRegistrationType(String keyStart, Registration.Type type);

    int countKeysInUse(String keyStart);

    List<RegDayOfYearCount> countByDayOfYear(String keyStart);

    List<RegHourCount> countByHour(String keyStart);

    /**
     * return count of matching registrations
     */
    int getMatchingCount(String nameSearch, String emailSearch, String keySearch, String addressSearch);

    /**
     * Return matching registrations
     */
    PagedList<Registration> getMatching(Integer count, int offset, int pagesize,
                                        String nameSearch, String emailSearch, String keySearch, String addressSearch);

    /**
     * Return all registrations for key
     */
    List<Registration> getAllForKey(String sKey);

    /**
     * Get registration records where key is banned
     */
    List<Registration> getAllBanned();

    /**
     * Get suspect keys where number of non-duplicate registrations is
     * greater and or equal to the passed in minimum
     */
    List<String> getAllSuspectKeys(int nMin);

    /**
     * Return whether the given registration is a duplicate of any in the database
     */
    boolean isDuplicate(Registration reg);

    /**
     * Mark rows in database that match given registartion as duplicates.  Given
     * registration must be of type REGISTRATION and must not be a duplicate
     * itself. (if it is not, nthing is done and 0 is returned).
     */
    int markDuplicates(Registration reg);
}