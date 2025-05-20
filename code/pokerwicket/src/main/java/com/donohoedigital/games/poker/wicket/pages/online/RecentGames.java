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
package com.donohoedigital.games.poker.wicket.pages.online;

import com.donohoedigital.db.DBUtils;
import com.donohoedigital.games.poker.wicket.rss.GamesListRss;
import com.donohoedigital.games.poker.wicket.rss.RssRecent;
import com.donohoedigital.wicket.annotations.MountPath;
import com.donohoedigital.wicket.converters.ParamDateConverter;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Date;

import static com.donohoedigital.games.poker.wicket.pages.online.GamesList.Category.recent;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 1, 2008
 * Time: 2:26:32 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("unused")
@MountPath("completed")
public class RecentGames extends GamesList
{
    private static final long serialVersionUID = 42L;

    private static final Category CAT = recent;

    public RecentGames()
    {
        super(CAT);
    }

    public RecentGames(PageParameters params)
    {
        super(CAT, params);
    }

    @Override
    protected String getTitle()
    {
        return "Completed DD Poker Public Games";
    }

    @Override
    protected String getNoGamesFound()
    {
        return null; // not used
    }

    @Override
    protected Class<? extends GamesListRss> getRssClass()
    {
        return RssRecent.class;
    }

    public static BookmarkablePageLink<RecentGames> getHostLink(String id, String host, Date begin, Date end)
    {
        ParamDateConverter CONVERTER = new ParamDateConverter();
        PageParameters params = new PageParameters();
        params.set(PARAM_NAME, DBUtils.sqlExactMatch(host));
        params.set(PARAM_BEGIN, CONVERTER.convertToString(begin));
        params.set(PARAM_END, CONVERTER.convertToString(end));
        return new BookmarkablePageLink<>(id, RecentGames.class, params);
    }
}
