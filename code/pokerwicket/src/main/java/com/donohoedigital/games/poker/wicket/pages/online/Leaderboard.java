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

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.service.*;
import static com.donohoedigital.games.poker.service.TournamentHistoryService.LeaderboardType.*;
import com.donohoedigital.games.poker.wicket.*;
import com.donohoedigital.games.poker.wicket.panels.*;
import com.donohoedigital.games.poker.wicket.util.*;
import com.donohoedigital.wicket.annotations.*;
import com.donohoedigital.wicket.common.*;
import com.donohoedigital.wicket.components.*;
import com.donohoedigital.wicket.labels.*;
import com.donohoedigital.wicket.models.*;
import com.donohoedigital.wicket.panels.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.html.*;
import org.apache.wicket.markup.html.basic.*;
import org.apache.wicket.markup.html.link.*;
import org.apache.wicket.markup.html.panel.*;
import org.apache.wicket.markup.repeater.*;
import org.apache.wicket.spring.injection.annot.*;
import org.wicketstuff.annotation.mount.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 19, 2008
 * Time: 2:48:39 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath(path = "leaderboard")
@MountFixedMixedParam(parameterNames = {Leaderboard.PARAM_TYPE, Leaderboard.PARAM_GAMES,
        Leaderboard.PARAM_BEGIN, Leaderboard.PARAM_END,
        Leaderboard.PARAM_NAME, Leaderboard.PARAM_PAGE,
        Leaderboard.PARAM_SIZE})
public class Leaderboard extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    //private static Logger logger = Logger.getLogger(Leaderboard.class);

    private static boolean DEBUG_ZERO_RESULTS = false;

    public static final String PARAM_TYPE = "type";
    public static final String PARAM_GAMES = "games";
    public static final String PARAM_BEGIN = "b";
    public static final String PARAM_END = "e";
    public static final String PARAM_NAME = "player";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "s";

    private boolean showFootnote = false;

    @SuppressWarnings({"NonSerializableFieldInSerializableClass"})
    @SpringBean
    private TournamentHistoryService histService;

    public Leaderboard()
    {
        this(new PageParameters());
    }

    public Leaderboard(PageParameters params)
    {
        super(params);
        init(params);
    }

    private void init(PageParameters params)
    {
        final TournamentHistoryService.LeaderboardType type = params.getAsEnum(PARAM_TYPE, TournamentHistoryService.LeaderboardType.class, ddr1);

        // title
        add(new VoidContainer("ddr1Title").setVisible(type == ddr1));
        add(new VoidContainer("roiTitle").setVisible(type == roi));

        // links
        add(getDDR1Link("ddr1Link", params).setEnabled(type != ddr1));
        add(getRoiLink("roiLink", params).setEnabled(type != roi));

        // description
        switch (type)
        {
            case ddr1:
                add(new Fragment("description", "ddr1Description", this));
                add(new StringLabel("col7header", "ROI")); // FIX: properties
                add(new StringLabel("col8header", "DDR1"));// FIX: properties
                break;

            case roi:
                add(new Fragment("description", "roiDescription", this));
                add(new StringLabel("col7header", "DDR1"));// FIX: properties
                add(new StringLabel("col8header", "ROI")); // FIX: properties
                break;
        }

        // leaderboard data
        LeaderData data = new LeaderData(type);

        // search form
        LeaderboardForm form = new LeaderboardForm("form", params, getClass(), data, PARAM_NAME, PARAM_BEGIN, PARAM_END);
        add(form);

        // process size after form data read
        data.processSizeFromParams(params, PARAM_SIZE);

        // table of players
        LeaderboardTableView dataView = new LeaderboardTableView("row", data);
        add(dataView);
        add(new BookmarkablePagingNavigator("navigator", dataView, new BasicPluralLabelProvider("player", "players"), Leaderboard.class, params,
                                            PARAM_PAGE));

        // no results found
        add(new StringLabel("begin", form.getBeginDateAsUserSeesIt()).setVisible(data.isEmpty()));
        add(new StringLabel("end", form.getEndDateAsUserSeesIt()));
        add(new StringLabel("nameSearch", data.getName()));
        add(new GroupingIntegerLabel("games", data.getGames()));
        add(new PluralLabel("gamesLabel", data.getGames(), new BasicPluralLabelProvider("game", "games")));

        // footnote
        add(new WebMarkupContainer("footnote")
        {
            private static final long serialVersionUID = 42L;

            @Override
            public boolean isVisible()
            {
                return showFootnote;
            }
        });
    }

    /**
     * leader data, fetched from TouranmentHistoryService
     */
    @SuppressWarnings({"PublicInnerClass"})
    public class LeaderData extends AliasedPageableServiceProvider<LeaderboardSummary> implements NameRangeSearch
    {
        private static final long serialVersionUID = 42L;

        private int games;
        private String name;
        private Date begin = null;
        private Date end = null;
        private Date beginDefault = Utils.getDateDays(-89);
        private Date endDefault = Utils.getDateEndOfDay(new Date());

        private TournamentHistoryService.LeaderboardType type;

        private LeaderData(TournamentHistoryService.LeaderboardType type)
        {
            this.type = type;
        }

        @Override
        public Iterator<LeaderboardSummary> iterator(int first, int pagesize)
        {
            if (DEBUG_ZERO_RESULTS) return new ArrayList<LeaderboardSummary>(0).iterator();
            DateRange dr = new DateRange(this, false);
            return histService.getLeaderboard(size(), first, pagesize, type, games, name, dr.getBegin(), dr.getEnd()).iterator();
        }

        @Override
        public int calculateSize()
        {
            if (DEBUG_ZERO_RESULTS) return 0;
            DateRange dr = new DateRange(this, false);
            return histService.getLeaderboardCount(games, name, dr.getBegin(), dr.getEnd());
        }

        public TournamentHistoryService.LeaderboardType getType()
        {
            return type;
        }

        public int getGames()
        {
            return games;
        }

        public void setGames(int games)
        {
            this.games = games;
        }

        public Date getBegin()
        {
            return begin == null ? beginDefault : begin;
        }

        public void setBegin(Date begin)
        {
            this.begin = begin;
        }

        public Date getEnd()
        {
            return end == null ? endDefault : end;
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
    private class LeaderboardTableView extends CountDataView<LeaderboardSummary>
    {
        private static final long serialVersionUID = 42L;

        private static final int ITEMS_PER_PAGE = 100;

        private int lastPercent = -1;

        private LeaderboardTableView(String id, LeaderData data)
        {
            super(id, data, ITEMS_PER_PAGE);
        }

        @Override
        protected void populateItem(Item<LeaderboardSummary> row)
        {
            AliasedCompoundPropertyModel<LeaderboardSummary> model = (AliasedCompoundPropertyModel<LeaderboardSummary>) row.getModel();
            LeaderboardSummary history = row.getModelObject();
            LeaderData data = getLeaderData();

            // CSS class
            row.add(new AttributeModifier("class", true,
                                          new StringModel(PokerSession.isLoggedInUser(history.getPlayerName()) ? "highlight" :
                                                          row.getIndex() % 2 == 0 ? "odd" : "even")));

            // rank and percentile
            row.add(new PlaceLabel("rank"));
            row.add(new PlaceLabel("percentile").setVisible(history.getPercentile() != lastPercent));
            lastPercent = history.getPercentile();

            // modify name if AI player
            boolean bSkipHistoryLinkForAi = false;
            String name = history.getPlayerName();
            boolean bAI = false;
            if (name.equals(OnlineProfile.Dummy.AI_BEST.getName()))
            {
                showFootnote = true;
                bAI = true;
                history.setPlayerName("DD Poker AI (best)*");
            }
            else if (name.equals(OnlineProfile.Dummy.AI_REST.getName()))
            {
                showFootnote = true;
                bSkipHistoryLinkForAi = true;
                bAI = true;
                history.setPlayerName("DD Poker AI (rest)*");
            }

            // link to tournament details
            Link<?> link = History.getHistoryLink("playerLink", name, data.getBegin(), data.getEnd());
            link.setEnabled(!bSkipHistoryLinkForAi);
            row.add(link);

            // player name (in link)
            Label nameLabel = new HighlightLabel("playerName", data.getName(), PokerWicketApplication.SEARCH_HIGHLIGHT, true);
            if (bAI)
            {
                AttributeModifier clazz = new AttributeModifier("class", true, new StringModel("ai"));
                nameLabel.add(clazz);
                link.add(clazz);
            }
            else
            {

                if (PokerSession.isLoggedInUser(history.getPlayerName()))
                {
                    AttributeModifier clazz = new AttributeModifier("class", true, new StringModel("current"));
                    nameLabel.add(clazz);
                    link.add(clazz);
                }
            }
            link.add(nameLabel);

            // amounts
            row.add(new GroupingIntegerLabel("gamesPlayed"));
            row.add(new PokerCurrencyLabel("totalSpent"));
            row.add(new PokerCurrencyLabel("totalPrizes"));
            row.add(new PokerCurrencyLabel("net"));

            // last two columns
            String col7 = null, col8 = null;
            String ddr1 = null, roi = null;
            String ROI = "roi";
            String DDR1 = "ddr1";
            String COL7 = "col7";
            String COL8 = "col8";
            switch (data.getType())
            {
                case ddr1:
                    col7 = ROI;
                    col8 = DDR1;
                    roi = COL7;
                    ddr1 = COL8;
                    break;

                case roi:
                    col7 = DDR1;
                    col8 = ROI;
                    ddr1 = COL7;
                    roi = COL8;
                    break;
            }

            row.add(new PokerPercentLabel(roi, 1));
            row.add(new GroupingIntegerLabel(ddr1));
            model.alias(COL7, col7);
            model.alias(COL8, col8);
        }

        private LeaderData getLeaderData()
        {
            return (LeaderData) getDataProvider();
        }
    }

    ////
    //// Links
    ////

    public static BookmarkablePageLink<Leaderboard> getDDR1Link(String id, PageParameters params)
    {
        BookmarkablePageLink<Leaderboard> link = new BookmarkablePageLink<Leaderboard>(id, Leaderboard.class, params);
        link.setParameter(PARAM_TYPE, ddr1.toString());
        return link;
    }

    public static BookmarkablePageLink<Leaderboard> getRoiLink(String id, PageParameters params)
    {
        BookmarkablePageLink<Leaderboard> link = new BookmarkablePageLink<Leaderboard>(id, Leaderboard.class, params);
        link.setParameter(PARAM_TYPE, roi.toString());
        return link;
    }
}
