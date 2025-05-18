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
package com.donohoedigital.games.poker.wicket.panels;

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.wicket.pages.online.*;
import com.donohoedigital.wicket.components.*;
import com.donohoedigital.wicket.labels.*;
import org.apache.wicket.markup.html.link.*;
import org.apache.wicket.markup.html.list.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 21, 2008
 * Time: 12:38:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerList extends VoidPanel
{
    private static final long serialVersionUID = 42L;

    public PlayerList(String id, List<String> players)
    {
        super(id);

        // list of aliases
        add(new ListView<String>("list", players)
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                String name = item.getModelObject();

                // comma
                item.add(new VoidContainer("comma").setVisible(item.getIndex() > 0));

                // link
                Link<?> link = History.getHistoryLink("playerLink", name);
                item.add(link);

                // display name with &nbsp; spaces so they don't wrap
                link.add(new StringLabel("playerName", Utils.encodeHTMLWhitespace(name)).setEscapeModelStrings(false));
            }
        });
    }
}