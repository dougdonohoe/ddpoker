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
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.service.*;
import com.rometools.rome.io.*;
import org.apache.wicket.markup.html.*;
import org.apache.wicket.request.target.resource.*;
import org.apache.wicket.spring.injection.annot.*;
import org.apache.wicket.util.resource.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
public abstract class GamesListRss extends WebPage
{
    private static final long serialVersionUID = 42L;

    @SuppressWarnings({"NonSerializableFieldInSerializableClass", "unused"})
    @SpringBean
    protected OnlineGameService gameService;

    public GamesListRss()
    {
        Date end = new Date();
        Date begin = Utils.getDateDays(end, -3); // Get 3 days worth of data

        // fetch data
        OnlineGameList games = gameService.getOnlineGamesAndHistoriesForDay(getModes(), begin, end);

        // encode it
        GamesListFeed feed = new GamesListFeed(getTitle(), getUrl(), games);
        WireFeedOutput out = new WireFeedOutput();
        String xml;
        try
        {
            xml = out.outputString(feed);
        }
        catch (FeedException e)
        {
            throw new ApplicationError(e);
        }

        // create target
        ResourceStreamRequestTarget target = new ResourceStreamRequestTarget(
                new StringResourceStream(xml, "text/xml"));

        // respond with target
        getRequestCycle().setRequestTarget(target);
    }

    /**
     * Get mode of games to list
     */
    protected abstract Integer[] getModes();

    /**
     * Get URL for associated page
     */
    protected abstract String getUrl();

    /**
     * Get type of games for comment
     */
    protected String getTitle()
    {
        return FeedTitle.getTitle(getClass());
    }
}