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
package com.donohoedigital.games.poker.model.util;

import com.donohoedigital.comms.*;
import com.donohoedigital.db.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.xml.*;

/**
 * OnlineGame list
 */
public class OnlineGameList extends PagedList<OnlineGame> implements SimpleXMLEncodable
{
    //static Logger logger = LogManager.getLogger(OnlineGameList.class);

    /**
     * Return this list as a DMArrayList to allow for data marshalling
     */
    public DMArrayList<DMTypedHashMap> getAsDMList()
    {
        DMArrayList<DMTypedHashMap> dmList = new DMArrayList<DMTypedHashMap>(size());

        for (OnlineGame game : this) {
            dmList.add(game.getData());
        }

        return dmList;
    }

    /**
     * debug representation of list
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        OnlineGame msg;

        for (int i = 0; i < size(); ++i)
        {
            msg = get(i);
            sb.append("Entry #").append(i).append(" (")
              .append(msg.getLicenseKey()).append(msg.getUrl()).append("): ")
              .append(msg).append('\n');
        }
        return sb.toString();
    }

    public void encodeXML(SimpleXMLEncoder encoder)
    {
        encoder.setCurrentObject("games");
        for (OnlineGame game : this)
        {
            game.encodeXML(encoder);
        }
        encoder.finishCurrentObject();
    }
}
