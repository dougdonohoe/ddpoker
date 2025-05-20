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
package com.donohoedigital.games.poker.wicket.rss;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.wicket.WicketUtils;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedOutput;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

import java.util.Date;

/**
 * @author Doug Donohoe
 */
public abstract class GamesListRss extends WebPage
{
    private static final long serialVersionUID = 42L;

    @SuppressWarnings({"unused"})
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
        IResourceStream resourceStream = new StringResourceStream(xml, "text/xml");
        ResourceStreamRequestHandler target = new ResourceStreamRequestHandler(resourceStream);

        // Set the response to use the new handler
        WicketUtils.getRequestCycle().scheduleRequestHandlerAfterCurrent(target);
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