/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.TournamentProfileHtml;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.service.TournamentHistoryService;
import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.pages.error.ErrorPage;
import com.donohoedigital.games.poker.wicket.panels.GameUrl;
import com.donohoedigital.games.poker.wicket.util.PokerCurrencyLabel;
import com.donohoedigital.wicket.WicketUtils;
import com.donohoedigital.wicket.annotations.MountMixedParam;
import com.donohoedigital.wicket.annotations.MountPath;
import com.donohoedigital.wicket.common.PageableServiceProvider;
import com.donohoedigital.wicket.components.CountDataView;
import com.donohoedigital.wicket.components.VoidContainer;
import com.donohoedigital.wicket.labels.*;
import com.donohoedigital.wicket.models.StringModel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.Date;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 18, 2008
 * Time: 12:19:02 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("unused")
@MountPath("game")
@MountMixedParam(parameterNames = {GameDetail.PARAM_GAME_ID, GameDetail.PARAM_HISTORY_ID})
public class GameDetail extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    //private static Logger logger = LogManager.getLogger(GameDetail.class);

    public static final String PARAM_HISTORY_ID = "histId";
    public static final String PARAM_GAME_ID = "gameId";

    @SpringBean
    private OnlineGameService gameService;

    @SpringBean
    private TournamentHistoryService histService;

    /**
     * Create page
     */
    public GameDetail(PageParameters params)
    {
        super(params);

        OnlineGame game = getOnlineGame(params);
        if (game == null) return;

        // game info
        int mode = game.getMode();
        TournamentProfile tournament = game.getTournament();

        add(new StringLabel("titleTournamentName", tournament.getName()).setRenderBodyOnly(true));
        add(new StringLabel("tournamentName", tournament.getName()));
        Link<?> link = RecentGames.getHostLink("hostLink", game.getHostPlayer(), null, null);
        link.add(new StringLabel("hostName", game.getHostPlayer()));
        add(link);
        add(new StringLabel("status", getMessage(mode)));
        add(DateLabel.forDatePattern("date", new Model<>(getDate(game)), PropertyConfig.getMessage("msg.format.datetime")));

        // game url if reg/play mode
        add(new GameUrl("gameUrl", game, PokerSession.get().isLoggedIn(), this).setVisible(mode == OnlineGame.MODE_REG || mode == OnlineGame.MODE_PLAY));

        // show history list if done
        if (mode == OnlineGame.MODE_STOP || mode == OnlineGame.MODE_END)
        {
            add(new FinishTable("tournamentFinish", game));
        }
        // otherwise, just blank
        else
        {
            add(new HiddenComponent("tournamentFinish"));
        }

        add(new StringLabel("tournamentProfile", new TournamentProfileHtml(tournament).toHTML(null)).setEscapeModelStrings(false));
    }

    private class FinishTable extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private FinishTable(String id, OnlineGame game)
        {
            super(id, "table", GameDetail.this);

            add(new VoidContainer("col-ddr1").setVisible(game.getMode() == OnlineGame.MODE_END));
            add(new VoidContainer("col-chips").setVisible(game.getMode() != OnlineGame.MODE_END));

            FinishTableView dataView = new FinishTableView("row", new FinishData(game.getId()));
            add(dataView);
        }
    }

    private class FinishData extends PageableServiceProvider<TournamentHistory>
    {
        private static final long serialVersionUID = 42L;

        private final Long id;
        private transient TournamentHistoryList histories = null;

        private FinishData(Long id)
        {
            this.id = id;
        }

        @Override
        public Iterator<TournamentHistory> iterator(long first, long pagesize)
        {
            return getList().iterator();
        }

        @Override
        public int calculateSize()
        {
            return getList().getTotalSize();
        }

        private TournamentHistoryList getList()
        {
            if (histories == null)
            {
                histories = histService.getAllTournamentHistoriesForGame(null, 0, -1, id);
            }
            return histories;
        }

        @Override
        public void detach()
        {
            histories = null;
        }
    }

    private class FinishTableView extends CountDataView<TournamentHistory>
    {
        private static final long serialVersionUID = 42L;

        private FinishTableView(String id, FinishData data)
        {
            super(id, data, (int) data.size() + 1); // add one in case size is 0
        }

        @Override
        protected void populateItem(Item<TournamentHistory> row)
        {
            TournamentHistory history = row.getModelObject();

            // CSS class
            row.add(new AttributeModifier("class",
                                          new StringModel(PokerSession.isLoggedInUser(history.getPlayerName()) ? "highlight" :
                                                          row.getIndex() % 2 == 0 ? "odd" : "even")));
            // place
            boolean bShowRank = !history.isEnded() && history.isAlive();
            row.add(new PlaceLabel("rank").setVisible(bShowRank));
            row.add(new PlaceLabel("place").setVisible(!bShowRank));

            // link to player history
            Link<?> link = History.getHistoryLink("playerLink", history.getPlayerName());
            link.setEnabled(history.getPlayerType() == TournamentHistory.PLAYER_TYPE_ONLINE);
            row.add(link);

            // player name (in link)
            link.add(new StringLabel("playerName"));

            // player type
            row.add(new VoidContainer("ai").setVisible(history.getPlayerType() == TournamentHistory.PLAYER_TYPE_AI));

            // last two columns different based on state of game and player
            if (history.isEnded())
            {
                row.add(new PokerCurrencyLabel("prize"));
                row.add(new GroupingIntegerLabel("ddr1"));
                row.add(new HiddenComponent("numChips"));
                row.add(new HiddenComponent("bustedOut"));
            }
            else
            {
                if (history.isAlive())
                {
                    row.add(new PokerCurrencyLabel("prize", 0));
                    row.add(new PokerCurrencyLabel("numChips"));
                    row.add(new HiddenComponent("bustedOut"));
                }
                else // busted
                {
                    row.add(new PokerCurrencyLabel("prize"));
                    row.add(new HiddenComponent("numChips"));
                    row.add(new StringLabel("bustedOut", "Busted out&nbsp;").setEscapeModelStrings(false)); // FIX: use properties
                }
                row.add(new HiddenComponent("ddr1"));
            }
        }
    }

    private Date getDate(OnlineGame game)
    {
        switch (game.getMode())
        {
            case OnlineGame.MODE_REG:
                return game.getCreateDate();

            case OnlineGame.MODE_PLAY:
                return game.getStartDate();

            case OnlineGame.MODE_STOP:
            case OnlineGame.MODE_END:
                return game.getEndDate();
        }

        return null;
    }

    private String getMessage(int mode)
    {
        switch (mode)
        {
            case OnlineGame.MODE_REG:
                return "Game opened for registration on";// FIX: use properties

            case OnlineGame.MODE_PLAY:
                return "Game in-progress, started on";// FIX: use properties

            case OnlineGame.MODE_STOP:
                return "Game did not finish, stopped on";// FIX: use properties

            case OnlineGame.MODE_END:
                return "Game completed on";// FIX: use properties
        }

        return null;
    }

    /**
     * Get OnlineGame identified from page parameters.  Returns null
     * and sets error response page if none is found.
     */
    private OnlineGame getOnlineGame(PageParameters params)
    {
        OnlineGame game = null;

        // look for history id first (if there, PARAM_GAME_ID might be set to a '-'
        // depending on the url coding strategy)
        long historyId = WicketUtils.getAsLong(params, PARAM_HISTORY_ID, -1L);
        if (historyId != -1L)
        {
            game = gameService.getOnlineGameByTournamentHistoryId(historyId);
            if (game == null)
            {
                setResponsePage(new ErrorPage("Unable to find game using tournament history id " + historyId + '.')); // FIX: property?
                return null;
            }
        }

        // then look for game id
        long gameId = WicketUtils.getAsLong(params, PARAM_GAME_ID, -1L);
        if (gameId != -1L)
        {
            game = gameService.getOnlineGameById(gameId);
            if (game == null)
            {
                setResponsePage(new ErrorPage("Unable to find game using online game id " + gameId + '.'));
                return null;
            }
        }

        if (game == null)
        {
            setResponsePage(new ErrorPage("A valid tournament history id or game id must be specified."));
        }
        return game;
    }

    ////
    //// Links
    ////

    public static BookmarkablePageLink<GameDetail> getGameIdLink(String id, long gameId)
    {
        BookmarkablePageLink<GameDetail> link = new BookmarkablePageLink<>(id, GameDetail.class);
        link.getPageParameters().set(PARAM_GAME_ID, gameId);
        return link;
    }

    public static BookmarkablePageLink<GameDetail> getHistoryIdLink(String id, long histId)
    {
        BookmarkablePageLink<GameDetail> link = new BookmarkablePageLink<>(id, GameDetail.class);
        link.getPageParameters().set(PARAM_HISTORY_ID, histId);
        return link;
    }

    public static String absoluteUrlFor(long gameId)
    {
        PageParameters params = new PageParameters();
        params.set(PARAM_GAME_ID, gameId);
        return WicketUtils.absoluteUrlFor(GameDetail.class, params);
    }
}