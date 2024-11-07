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
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.db.DBUtils;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.PokerWicketApplication;
import com.donohoedigital.games.poker.wicket.panels.GameUrl;
import com.donohoedigital.games.poker.wicket.panels.NameRangeSearchForm;
import com.donohoedigital.games.poker.wicket.panels.PlayerList;
import com.donohoedigital.games.poker.wicket.rss.GamesListRss;
import com.donohoedigital.games.poker.wicket.rss.RssLink;
import com.donohoedigital.games.poker.wicket.util.DateRange;
import com.donohoedigital.games.poker.wicket.util.NameRangeSearch;
import com.donohoedigital.wicket.annotations.MountMixedParam;
import com.donohoedigital.wicket.common.PageableServiceProvider;
import com.donohoedigital.wicket.components.CountDataView;
import com.donohoedigital.wicket.components.VoidContainer;
import com.donohoedigital.wicket.labels.BasicPluralLabelProvider;
import com.donohoedigital.wicket.labels.DateLabel;
import com.donohoedigital.wicket.labels.StringLabel;
import com.donohoedigital.wicket.models.DateModel;
import com.donohoedigital.wicket.models.StringModel;
import com.donohoedigital.wicket.panels.BookmarkablePagingNavigator;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.donohoedigital.games.poker.service.OnlineGameService.OrderByType.date;
import static com.donohoedigital.games.poker.wicket.pages.online.GamesList.Category.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 1, 2008
 * Time: 1:36:58 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"PublicStaticArrayField", "unused", "rawtypes", "unchecked"})
@MountMixedParam(parameterNames = {GamesList.PARAM_BEGIN, GamesList.PARAM_END, GamesList.PARAM_NAME,
        GamesList.PARAM_PAGE, GamesList.PARAM_SIZE})
public abstract class GamesList extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    //private static Logger logger = LogManager.getLogger(GamesList.class);

    public static final String PARAM_BEGIN = "b";
    public static final String PARAM_END = "e";
    public static final String PARAM_NAME = "host";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "s";

    public static final Integer[] MODE_AVAILABLE = new Integer[]{OnlineGame.MODE_REG};
    public static final Integer[] MODE_RUNNING = new Integer[]{OnlineGame.MODE_PLAY};
    public static final Integer[] MODE_ENDED = new Integer[]{OnlineGame.MODE_END};
    public static final Integer[] MODE_STOPPED = new Integer[]{OnlineGame.MODE_STOP};
    public static final Integer[] MODE_CURRENT = new Integer[]{OnlineGame.MODE_REG, OnlineGame.MODE_PLAY};
    public static final Integer[] MODES_RECENT = new Integer[]{OnlineGame.MODE_STOP, OnlineGame.MODE_END};
    public static final Integer[] MODES_ALL = new Integer[]{OnlineGame.MODE_REG, OnlineGame.MODE_PLAY,
            OnlineGame.MODE_STOP, OnlineGame.MODE_END};

    public enum Category
    {
        available, running, recent, current
    }

    @SpringBean
    private OnlineGameService gameService;

    private boolean loggedIn;

    public GamesList(Category category)
    {
        this(category, new PageParameters());
    }

    public GamesList(Category category, PageParameters params)
    {
        super(params);
        init(category, params);
    }

    private void init(Category category, PageParameters params)
    {
        loggedIn = PokerSession.get().isLoggedIn();

        // title
        add(new StringLabel("headTitle", getTitle()));
        add(new StringLabel("title", getTitle()));
        add(DateLabel.forDatePattern("date", new DateModel(new Date()), PropertyConfig.getMessage("msg.format.datetime")));
        add(new BookmarkablePageLink("refresh", getClass()));
        add(new RssLink("rss", getRssClass()));

        // description
        switch (category)
        {
            case recent:
                add(new RecentGamesDescription());
                break;

            case running:
            case available:
            case current:
                if (loggedIn)
                {
                    add(new CurrentGameLoggedInDescription());
                }
                else
                {
                    add(new CurrentGameNotLoggedInDescription());
                }
        }

        // game data
        GameData data = new GameData(category);

        // search form
        NameRangeSearchForm form = new NameRangeSearchForm("form", params, getClass(), data,
                                                           PARAM_NAME, PARAM_BEGIN, PARAM_END, "Host");
        add(form.setVisible(category == recent));

        // process size after form data read
        data.processSizeFromParams(params, PARAM_SIZE);

        // title info if host search
        add(new StringLabel("hostName", getNameSearchTitle(data.getName())).setVisible(!Strings.isEmpty(data.getName())));

        GameListTableView dataView = new GameListTableView("row", data);
        add(dataView);
        add(new BookmarkablePagingNavigator("navigator", dataView, new BasicPluralLabelProvider("game", "games"), getClass(), params,
                                            PARAM_PAGE));

        // no results found (recent)
        add(new StringLabel("begin", form.getBeginDateAsUserSeesIt()).setVisible(category == recent && data.isEmpty()));
        add(new StringLabel("end", form.getEndDateAsUserSeesIt()));
        add(new StringLabel("nameSearch", getNameSearchNoResults(data.getName())).setEscapeModelStrings(false).setRenderBodyOnly(true));

        // no results found (available/running)
        add(new StringLabel("none", getNoGamesFound()).setVisible(category != recent && data.isEmpty()));

        // table header columns
        add(new VoidContainer("playersJoinedHeader").setVisible(category == available));
        add(new VoidContainer("playersRemainingHeader").setVisible(category == running));
        add(new VoidContainer("playersHeader").setVisible(category == current));
        add(new VoidContainer("gameUrlHeader").setVisible(category != recent));
        add(new VoidContainer("endDateHeader").setVisible(category == recent));
        add(new VoidContainer("statusHeader").setVisible(category == recent));
    }

    private String getNameSearchTitle(String sValue)
    {
        if (sValue == null) return null;

        if (sValue.startsWith(DBUtils.SQL_EXACT_MATCH))
        {
            return "Hosted by " + sValue.substring(1);
        }
        else
        {
            return "Hosts matching '" + sValue + '\'';
        }
    }

    private String getNameSearchNoResults(String sValue)
    {
        if (sValue == null) return null;

        if (sValue.startsWith(DBUtils.SQL_EXACT_MATCH))
        {
            return "hosted by <span>" + sValue.substring(1) + "</span>";
        }
        else
        {
            return "with hosts matching \"<span>" + sValue + "</span>\"";
        }
    }

    /**
     * title for page - subclass must implement
     */
    protected abstract String getTitle();

    /**
     * get no games found message
     */
    protected abstract String getNoGamesFound();

    /**
     * Get RSS feed class
     */
    protected abstract Class<? extends GamesListRss> getRssClass();

    /**
     * get modes
     */
    private Integer[] getModes(Category category)
    {
        switch (category)
        {
            case available:
                return MODE_AVAILABLE;

            case running:
                return MODE_RUNNING;

            case current:
                return MODE_CURRENT;

            case recent:
                return MODES_RECENT;
        }

        return null; // won't get here - why is compiler complaining?        
    }

    ////
    //// List
    ////

    private class GameData extends PageableServiceProvider<OnlineGame> implements NameRangeSearch
    {
        private static final long serialVersionUID = 42L;

        private final Category category;
        private String name;
        private Date begin;
        private Date end;
        private final Date beginDefault = PokerWicketApplication.START_OF_TIME;
        private final Date endDefault = Utils.getDateEndOfDay(new Date());

        private GameData(Category category)
        {
            this.category = category;
        }

        @Override
        public Iterator<OnlineGame> iterator(long first, long pagesize)
        {
            DateRange dr = new DateRange(this);
            return gameService.getOnlineGames((int) size(), (int) first, (int) pagesize, getModes(category),
                                              name, dr.getBegin(), dr.getEnd(), date).iterator();
        }

        @Override
        public int calculateSize()
        {
            DateRange dr = new DateRange(this);
            return gameService.getOnlineGamesCount(getModes(category), name, dr.getBegin(), dr.getEnd());
        }

        public Category getCategory()
        {
            return category;
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
    private class GameListTableView extends CountDataView<OnlineGame>
    {
        private static final long serialVersionUID = 42L;

        private static final int ITEMS_PER_PAGE = 25;

        private GameListTableView(String id, GameData data)
        {
            super(id, data, ITEMS_PER_PAGE);
        }

        @Override
        protected void populateItem(Item<OnlineGame> row)
        {
            OnlineGame game = row.getModelObject();
            GameData data = getGameData();
            Category category = data.getCategory();

            // CSS class
            row.add(new AttributeModifier("class",
                                          new StringModel(PokerSession.isLoggedInUser(game.getHostPlayer()) ? "highlight" :
                                                          row.getIndex() % 2 == 0 ? "odd" : "even")));

            // link to tournament details
            Link<?> link = GameDetail.getGameIdLink("detailsLink", game.getId());
            row.add(link);

            // tournament name (in link)
            link.add(new StringLabel("tournament.name"));

            // invite only
            row.add(new VoidContainer("invite-only").setVisible(game.getTournament().isInviteOnly()));

            // start date (running games only)
            row.add(DateLabel.forDatePattern("startDate", PropertyConfig.getMessage("msg.format.datetime")).setVisible(game.getMode() == OnlineGame.MODE_PLAY));

            // link to host details
            // link to tournament details
            link = RecentGames.getHostLink("hostLink", game.getHostPlayer(), null, null);
            row.add(link);

            // player name (in link)
            link.add(new StringLabel("hostPlayer"));

            // player list
            List<String> players = null;
            if (category != recent)
            {
                players = game.getTournament().getPlayers();
            }
            if (players == null) players = new ArrayList<>();
            row.add(new PlayerList("playerList", players).setVisible(category != recent));

            // url link
            row.add(new GameUrl("gameUrl", game, loggedIn, GamesList.this).setVisible(category != recent));

            // status, end-date
            row.add(DateLabel.forDatePattern("endDate", PropertyConfig.getMessage("msg.format.datetime")).setVisible(category == recent));
            row.add(new StringLabel("status", (game.getMode() == OnlineGame.MODE_STOP) ? "Stopped" : "Ended").setVisible(category == recent));
        }

        private GameData getGameData()
        {
            return (GameData) getDataProvider();
        }
    }

    ////
    //// Fragments
    ////

    private class RecentGamesDescription extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private RecentGamesDescription()
        {
            super("description", "recentGamesDescription", GamesList.this);
        }
    }

    private class CurrentGameLoggedInDescription extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private CurrentGameLoggedInDescription()
        {
            super("description", "currentGamesLoggedIn", GamesList.this);
        }
    }

    private class CurrentGameNotLoggedInDescription extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private CurrentGameNotLoggedInDescription()
        {
            super("description", "currentGamesNotLoggedIn", GamesList.this);
            add(getCurrentProfile().getLoginLink("loginLink"));
        }
    }
}
