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

import com.donohoedigital.db.dao.impl.*;
import com.donohoedigital.games.server.dao.*;
import com.donohoedigital.games.server.model.*;
import org.springframework.stereotype.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 14, 2008
 * Time: 10:56:58 PM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class BannedKeyImplJpa extends JpaBaseDao<BannedKey, Long> implements BannedKeyDao
{
    @Override
    public List<BannedKey> getAll()
    {
        return getList("select b from BannedKey b order by b.id desc");
    }

    public List<BannedKey> getByKeys(String... keys)
    {
        if (keys.length == 0) return new ArrayList<BannedKey>();

        return getList("select b from BannedKey b " +
                       "where b.key in " + getInClause(1, keys.length) + " " +
                       // 2020: warning advised casting to (Object) to do a varags call
                       // not sure if this is correct, but who knows if this code will ever
                       // actually be called again.
                       "order by b.until desc", (Object) keys);
    }
}