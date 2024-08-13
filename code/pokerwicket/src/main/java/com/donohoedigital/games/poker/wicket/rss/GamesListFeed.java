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
package com.donohoedigital.games.poker.wicket.rss;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.wicket.pages.online.*;
import com.sun.syndication.feed.rss.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
public class GamesListFeed extends Channel
{
    //private static Logger logger = Logger.getLogger(GamesListFeed.class);

    private static final long serialVersionUID = 42L;

    public GamesListFeed(String title, String url, OnlineGameList list)
    {
        super("rss_2.0");

        Date date = new Date();
        if (!list.isEmpty())
        {
            date = list.get(0).getModifyDate();
        }

        String description = "DD Poker Online Games Portal";
        setTitle(title);
        setLink(url);
        setDescription(description);
        setLanguage("en-us");
        setCopyright("Copyright (c) 2004-" + Utils.getDateYear() + ".  Donohoe Digital LLC.");
        setLastBuildDate(date);
        setPubDate(date);
        setTtl(30); // time to live

        // category
        List<Category> cats = new ArrayList<Category>();
        setCategories(cats);

        // image
        Image image = new Image();
        image.setUrl("http://www.ddpoker.com/images/pokericon32.jpg");
        image.setHeight(32);
        image.setWidth(32);
        image.setLink(url);
        image.setTitle(title);
        image.setDescription("DD Poker");
        setImage(image);

        // items
        List<Item> items = new ArrayList<Item>();
        for (OnlineGame game : list)
        {
            Item item = new Item();
            items.add(item);

            // link
            Guid guid = new Guid();
            guid.setPermaLink(true);
            guid.setValue(GameDetail.absoluteUrlFor(game.getId()));
            item.setGuid(guid);

            // title & date
            item.setTitle(Utils.encodeXML(game.getTournament().getName() + " hosted by " + game.getHostPlayer() + " (" + getMode(game.getMode()) + ')'));
            item.setPubDate(game.getModifyDate());

            // Description
            Description desc = new Description();
            if (game.getMode() == OnlineGame.MODE_END)
            {
                TournamentHistory hist = game.getHistories().get(0);
                desc.setValue(hist.getPlayerName() + " finished 1st, winning $" + PropertyConfig.getAmount(hist.getPrize(), false, false));
            }
            else
            {
                desc.setValue(Utils.encodeXML(game.getTournament().getDescription()));
            }
            item.setDescription(desc);

            // category
            Category itemcat = new Category();
            itemcat.setValue(getCategory(game.getMode()));
            List<Category> icats = new ArrayList<Category>();
            icats.add(itemcat);
            item.setCategories(icats);

            if (!cats.contains(itemcat))
            {
                cats.add(itemcat);
            }
        }
        setItems(items);
    }

    private String getCategory(int mode)
    {
        switch (mode)
        {
            case OnlineGame.MODE_REG:
                return "Available Games";

            case OnlineGame.MODE_PLAY:
                return "Running Games";

            case OnlineGame.MODE_END:
                return "Finished Games";

            case OnlineGame.MODE_STOP:
                return "Stopped Games";
        }

        throw new ApplicationError("Unknown mode: " + mode);
    }

    private String getMode(int mode)
    {
        switch (mode)
        {
            case OnlineGame.MODE_REG:
                return "available";

            case OnlineGame.MODE_PLAY:
                return "running";

            case OnlineGame.MODE_END:
                return "finished";

            case OnlineGame.MODE_STOP:
                return "stopped";
        }

        throw new ApplicationError("Unknown mode: " + mode);
    }
}
