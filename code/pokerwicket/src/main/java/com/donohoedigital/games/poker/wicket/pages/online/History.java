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
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.service.*;
import com.donohoedigital.games.poker.wicket.*;
import com.donohoedigital.games.poker.wicket.pages.error.*;
import com.donohoedigital.games.poker.wicket.panels.*;
import com.donohoedigital.games.poker.wicket.util.*;
import com.donohoedigital.wicket.annotations.*;
import com.donohoedigital.wicket.common.*;
import com.donohoedigital.wicket.components.*;
import com.donohoedigital.wicket.converters.*;
import com.donohoedigital.wicket.labels.*;
import com.donohoedigital.wicket.models.*;
import com.donohoedigital.wicket.panels.*;
import org.apache.wicket.*;
import org.apache.wicket.datetime.markup.html.basic.*;
import org.apache.wicket.markup.html.*;
import org.apache.wicket.markup.html.link.*;
import org.apache.wicket.markup.html.panel.*;
import org.apache.wicket.markup.repeater.*;
import org.apache.wicket.spring.injection.annot.*;
import org.wicketstuff.annotation.mount.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 18, 2008
 * Time: 12:19:02 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath(path = "history")
@MountFixedMixedParam(parameterNames = {History.PARAM_NAME, History.PARAM_BEGIN, History.PARAM_END,
        History.PARAM_GAME_NAME, History.PARAM_PAGE, History.PARAM_SIZE})
public class History extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    //private static Logger logger = Logger.getLogger(History.class);

    public static final String PARAM_NAME = "name";
    public static final String PARAM_BEGIN = "b";
    public static final String PARAM_END = "e";
    public static final String PARAM_GAME_NAME = "game";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "s";

    @SuppressWarnings({"NonSerializableFieldInSerializableClass"})
    @SpringBean
    private OnlineProfileService profileService;

    @SuppressWarnings({"NonSerializableFieldInSerializableClass"})
    @SpringBean
    private TournamentHistoryService histService;

    private static final PokerUser error = new PokerUser(-1L, "error", null, "error@error.err", false);

    /**
     * Default page - use logged in user
     */
    public History()
    {
        this(new PageParameters());
    }

    /**
     * Create page using user from id or name params (in that order).
     * Defaults to logged in user if neither of those is valid.
     */
    public History(PageParameters params)
    {
        super(params);
        init(params);
    }

    /**
     * Init based on user
     */
    private void init(PageParameters params)
    {
        PokerUser user = getUser(params);
        if (user == error) return;
        boolean hasUser = user != null;

        // title
        add(new StringLabel("titleName", hasUser ? user.getDisplayName() : null).setVisible(hasUser).setRenderBodyOnly(true));
        add(new StringLabel("name", hasUser ? user.getDisplayName() : null).setVisible(hasUser));
        add(new WebMarkupContainer("retired").setVisible(hasUser && user.isRetired()));

        // description and history
        if (hasUser)
        {
            final HistoryData data = new HistoryData(user);

            // search form (create now so data search terms are set)
            NameRangeSearchForm searchForm = new NameRangeSearchForm("form", params, getClass(), data,
                                                                     PARAM_GAME_NAME, PARAM_BEGIN, PARAM_END,
                                                                     "Tournament")
            {
                private static final long serialVersionUID = 42L;

                @Override
                protected void addCustomPageParameters(PageParameters p)
                {
                    p.add(PARAM_NAME, data.getUser().getName());
                }
            };

            // process size after form data read
            data.processSizeFromParams(params, PARAM_SIZE);

            // aliases
            add(new Aliases("aliases", user, data.getBegin(), data.getEnd()));

            // description and table
            add(new Fragment("description", "loggedIn", this));
            add(new HistoryTable("history", searchForm, data, params));
        }
        else
        {
            // no aliases
            add(new HiddenComponent("aliases"));

            // description
            Fragment desc = new Fragment("description", "notLoggedIn", this);
            desc.add(getCurrentProfile().getLoginLink("loginLink"));
            add(desc);

            // no table
            add(new HiddenComponent("history"));
        }
    }

    /**
     * Get user based on page params
     */
    private PokerUser getUser(PageParameters params)
    {
        PokerUser user = null;
        String name = params.getString(PARAM_NAME, null);

        if (name != null)
        {
            OnlineProfile profile = profileService.getOnlineProfileByName(name);
            if (profile != null)
            {
                user = new PokerUser(profile);
            }
            else
            {
                setResponsePage(new ErrorPage('\'' + name + "' is not a known Online Profile (it could be a local player " +
                                              "who joined a public game via a direct invitation of the host).")); // FIX: property?
                return error;
            }
        }

        if (user == null)
        {
            user = PokerSession.get().getLoggedInUser();
        }

        if (user != null)
        {
            // ignore AI Rest or Human
            if (user.getName().equals(OnlineProfile.Dummy.AI_REST.getName()) ||
                user.getName().equals(OnlineProfile.Dummy.HUMAN.getName()))
            {
                user = null;
            }
        }

        return user;
    }

    ////
    //// List
    ////

    private class HistoryTable extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private HistoryTable(String id, NameRangeSearchForm form, HistoryData data, PageParameters params)
        {
            super(id, "table", History.this);

            add(form);

            // table of histories
            HistoryTableView dataView = new HistoryTableView("row", data);
            add(dataView);
            add(new BookmarkablePagingNavigator("navigator", dataView, new BasicPluralLabelProvider("game", "games"),
                                                History.class, params, PARAM_PAGE));

            // no results found
            add(new StringLabel("player", data.getUser().getName()).setVisible(data.isEmpty()));
            add(new StringLabel("begin", form.getBeginDateAsUserSeesIt()));
            add(new StringLabel("end", form.getEndDateAsUserSeesIt()));
            add(new StringLabel("nameSearch", data.getName()));
        }
    }

    private class HistoryData extends PageableServiceProvider<TournamentHistory> implements NameRangeSearch
    {
        private static final long serialVersionUID = 42L;

        private PokerUser user;
        private String name;
        private Date begin;
        private Date end;
        private Date beginDefault = PokerWicketApplication.START_OF_TIME;
        private Date endDefault = Utils.getDateEndOfDay(new Date());

        private HistoryData(PokerUser user)
        {
            this.user = user;
        }

        @Override
        public Iterator<TournamentHistory> iterator(int first, int pagesize)
        {
            DateRange dr = new DateRange(this);
            return histService.getAllTournamentHistoriesForProfile(size(), first, pagesize, user.getId(), name, dr.getBegin(), dr.getEnd()).iterator();
        }

        @Override
        public int calculateSize()
        {
            DateRange dr = new DateRange(this);
            return histService.getAllTournamentHistoriesForProfileCount(user.getId(), name, dr.getBegin(), dr.getEnd());
        }

        public PokerUser getUser()
        {
            return user;
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

    private class HistoryTableView extends CountDataView<TournamentHistory>
    {
        private static final long serialVersionUID = 42L;
        private static final int ITEMS_PER_PAGE = 20;

        private HistoryTableView(String id, HistoryData data)
        {
            super(id, data, ITEMS_PER_PAGE);
        }

        @Override
        protected void populateItem(Item<TournamentHistory> row)
        {
            TournamentHistory history = row.getModelObject();

            // CSS class
            row.add(new AttributeModifier("class", true, new StringModel(row.getIndex() % 2 == 0 ? "odd" : "even")));

            // link to tournament details
            Link<?> link = GameDetail.getHistoryIdLink("detailsLink", history.getId());
            row.add(link);

            // tourament name (in link)
            link.add(new StringLabel("tournamentName"));

            // last hand label if game not ended
            row.add(new VoidContainer("lastHand").setVisible(!history.isEnded()));

            // end date
            row.add(DateLabel.forDatePattern("endDate", PropertyConfig.getMessage("msg.format.datetime")));

            // amounts
            row.add(new PokerCurrencyLabel("buyin"));
            row.add(new PokerCurrencyLabel("rebuy"));
            row.add(new PokerCurrencyLabel("addon"));
            row.add(new PokerCurrencyLabel("totalSpent"));

            // final columns in table (unique columns or col-spanned depending on game state)
            WebMarkupContainer finalColumns;
            if (history.isEnded())
            {
                finalColumns = new Finished();
            }
            else
            {
                if (history.isAlive())
                {
                    finalColumns = new StoppedChips();
                }
                else
                {
                    finalColumns = new StoppedBusted();
                }

            }
            row.add(finalColumns);
        }
    }

    ////
    //// Fragments
    ////

    private class FinishFragment extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private FinishFragment(String frag)
        {
            super("finalColumns", frag, History.this);

            add(new GroupingIntegerLabel("numPlayers"));
        }
    }

    private class StoppedBusted extends FinishFragment
    {
        private static final long serialVersionUID = 42L;

        private StoppedBusted()
        {
            super("stopped-busted");

            add(new PokerCurrencyLabel("prize"));
            add(new PlaceLabel("place"));
        }
    }

    private class StoppedChips extends FinishFragment
    {
        private static final long serialVersionUID = 42L;

        private StoppedChips()
        {
            super("stopped-chips");

            add(new PlaceLabel("rank"));
            add(new PokerCurrencyLabel("numChips"));
        }
    }

    private class Finished extends FinishFragment
    {
        private static final long serialVersionUID = 42L;

        private Finished()
        {
            super("finished");

            add(new PokerCurrencyLabel("prize"));
            add(new PlaceLabel("place"));
            add(new PokerCurrencyLabel("net"));
            add(new GroupingIntegerLabel("ddr1"));
        }
    }

    ////
    //// Links
    ////

    public static BookmarkablePageLink<History> getHistoryLink(String id, String userName)
    {
        BookmarkablePageLink<History> link = new BookmarkablePageLink<History>(id, History.class);
        link.setParameter(PARAM_NAME, userName);
        return link;
    }

    public static BookmarkablePageLink<History> getHistoryLink(String id, String userName, Date begin, Date end)
    {
        BookmarkablePageLink<History> link = new BookmarkablePageLink<History>(id, History.class);
        ParamDateConverter conv = new ParamDateConverter();
        link.setParameter(PARAM_NAME, userName);
        link.setParameter(PARAM_BEGIN, conv.convertToString(begin));
        link.setParameter(PARAM_END, conv.convertToString(end));
        return link;
    }
}
