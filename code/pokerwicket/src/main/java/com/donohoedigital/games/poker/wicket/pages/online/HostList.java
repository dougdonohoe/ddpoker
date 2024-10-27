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
package com.donohoedigital.games.poker.wicket.pages.online;

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.model.HostSummary;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.PokerWicketApplication;
import com.donohoedigital.games.poker.wicket.panels.NameRangeSearchForm;
import com.donohoedigital.games.poker.wicket.util.DateRange;
import com.donohoedigital.games.poker.wicket.util.NameRangeSearch;
import com.donohoedigital.wicket.annotations.MountFixedMixedParam;
import com.donohoedigital.wicket.common.PageableServiceProvider;
import com.donohoedigital.wicket.components.CountDataView;
import com.donohoedigital.wicket.labels.*;
import com.donohoedigital.wicket.models.IntegerModel;
import com.donohoedigital.wicket.models.StringModel;
import com.donohoedigital.wicket.panels.BookmarkablePagingNavigator;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import java.util.Date;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 1, 2008
 * Time: 1:36:58 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath("hosts")
@MountFixedMixedParam(parameterNames = {HostList.PARAM_BEGIN, HostList.PARAM_END, HostList.PARAM_NAME,
        HostList.PARAM_PAGE, HostList.PARAM_SIZE})
public class HostList extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    //private static Logger logger = LogManager.getLogger(Search.class);

    public static final String PARAM_BEGIN = "b";
    public static final String PARAM_END = "e";
    public static final String PARAM_NAME = "host";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "s";

    public static final int ITEMS_PER_PAGE = 25;

    @SuppressWarnings({"NonSerializableFieldInSerializableClass"})
    @SpringBean
    private OnlineGameService gameService;

    public HostList()
    {
        this(new PageParameters());
    }

    public HostList(PageParameters params)
    {
        super(params);
        init(params);
    }

    private void init(PageParameters params)
    {
        // host data
        HostData data = new HostData();

        // search form
        NameRangeSearchForm form = new NameRangeSearchForm("form", params, getClass(), data, PARAM_NAME, PARAM_BEGIN, PARAM_END, "Host");
        add(form);

        // process size after form data read
        data.processSizeFromParams(params, PARAM_SIZE);

        // table of hosts
        GameListTableView dataView = new GameListTableView("row", data);
        add(dataView);
        add(new BookmarkablePagingNavigator("navigator", dataView, 1, true,
                                            new BasicPluralLabelProvider("host", "hosts"),
                                            getClass(), params,
                                            PARAM_PAGE));

        // no results found
        add(new StringLabel("begin", form.getBeginDateAsUserSeesIt()).setVisible(data.isEmpty()));
        add(new StringLabel("end", form.getEndDateAsUserSeesIt()));
        add(new StringLabel("nameSearch", data.getName()));
    }

    ////
    //// List
    ////

    private class HostData extends PageableServiceProvider<HostSummary> implements NameRangeSearch
    {
        private static final long serialVersionUID = 42L;

        private String name;
        private Date begin;
        private Date end;
        private Date beginDefault = PokerWicketApplication.START_OF_TIME;
        private Date endDefault = Utils.getDateEndOfDay(new Date());

        @Override
        public Iterator<HostSummary> iterator(int first, int pagesize)
        {
            DateRange dr = new DateRange(this);
            return gameService.getHostSummary(size(), first, pagesize, name, dr.getBegin(), dr.getEnd()).iterator();
        }

        @Override
        public int calculateSize()
        {
            DateRange dr = new DateRange(this);
            return gameService.getHostSummaryCount(name, dr.getBegin(), dr.getEnd());
        }

        public Date getBegin()
        {
            return begin;
        }

        public void setBegin(Date begin)
        {
            this.begin = begin;
        }

        public Date getEnd()
        {
            return end;
        }

        public void setEnd(Date end)
        {
            this.end = Utils.getDateEndOfDay(end);
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Date getBeginDefault()
        {
            return beginDefault;
        }

        public Date getEndDefault()
        {
            return endDefault;
        }
    }

    /**
     * The leaderboard table
     */
    private class GameListTableView extends CountDataView<HostSummary>
    {
        private static final long serialVersionUID = 42L;

        private GameListTableView(String id, HostData data)
        {
            super(id, data, ITEMS_PER_PAGE);
        }

        @Override
        protected void populateItem(Item<HostSummary> row)
        {
            HostSummary summary = row.getModelObject();
            HostList.HostData data = getSearchData();

            // CSS class
            row.add(new AttributeModifier("class",
                                          new StringModel(PokerSession.isLoggedInUser(summary.getHostName()) ? "highlight" :
                                                          row.getIndex() % 2 == 0 ? "odd" : "even")));

            // num
            row.add(new PlaceLabel("num", new IntegerModel(getCurrentPage() * getPageSize() + row.getIndex() + 1)));
            // link to player details
            Link<?> link = RecentGames.getHostLink("hostLink", summary.getHostName(), data.getBegin(), data.getEnd());
            row.add(link);

            // player name (in link)
            link.add(new HighlightLabel("hostName", data.getName(), PokerWicketApplication.SEARCH_HIGHLIGHT, true));
            row.add(new WebMarkupContainer("retired").setVisible(summary.isRetired()));

            // games hosted
            row.add(new GroupingIntegerLabel("gamesHosted"));
        }

        private HostData getSearchData()
        {
            return (HostData) getDataProvider();
        }
    }
}