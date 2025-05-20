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
package com.donohoedigital.games.server.service.impl;

import com.donohoedigital.games.server.service.*;
import com.donohoedigital.games.server.dao.*;
import com.donohoedigital.games.server.model.*;
import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 27, 2008
 * Time: 1:18:32 PM
 * To change this template use File | Settings | File Templates.
 */
@Service("upgradedKeyService")
public class UpgradedKeyServiceImpl implements UpgradedKeyService
{
    private UpgradedKeyDao dao;

    @Autowired
    public void setUpgradedKeyDao(UpgradedKeyDao dao)
    {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public UpgradedKey getUpgradedKeyByKey(String sKey)
    {
        return dao.getByKey(sKey);
    }

    @Transactional(readOnly = true)
    public List<UpgradedKey> getAllUpgradedKeys()
    {
        return dao.getAll();
    }

    @Transactional
    public void saveUpgradedKey(UpgradedKey key)
    {
        dao.save(key);
    }

    @Transactional
    public UpgradedKey updateUpgradedKey(UpgradedKey key)
    {
        return dao.update(key);
    }
}
