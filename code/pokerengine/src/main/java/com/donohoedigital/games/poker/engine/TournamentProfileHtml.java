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
package com.donohoedigital.games.poker.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.model.*;

import java.io.*;
import java.util.*;

import static com.donohoedigital.games.poker.model.TournamentProfile.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 2, 2008
 * Time: 1:00:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class TournamentProfileHtml
{
    private TournamentProfile profile;

    // cache for quick display
    private String htmlCache_;
    private String htmlSummaryCache_;
    private boolean htmlSummaryMode_ = false;

    /**
     * Create new profile HTML given a profile.
     */
    public TournamentProfileHtml(TournamentProfile profile)
    {
        this.profile = profile;
    }

    /**
     * Get profile
     */
    public TournamentProfile getProfile()
    {
        return profile;
    }

    /**
     * Get HTML summary of this tournament (used in summary tab in-game)
     */
    public String toHTMLSummary(boolean bListMode, String sLocale)
    {
        if (htmlSummaryCache_ != null && htmlSummaryMode_ == bListMode) return htmlSummaryCache_;

        String sDate = PropertyConfig.getDateFormat(sLocale).format(new Date(profile.getCreateDate()));
        Object params[] = new Object[16];

        params[0] = DataElement.getDisplayValue(DATA_ELEMENT_GAMETYPE, profile.getDefaultGameTypeString());
        params[1] = Utils.encodeHTML(profile.getDescription());
        params[2] = PropertyConfig.getMessage("msg.numplayers", profile.getMap().get(PARAM_NUM_PLAYERS),
                                              profile.getSeats());
        params[3] = profile.getMap().get(PARAM_BUYIN);
        params[4] = profile.getMap().get(PARAM_BUYINCHIPS);

        // rebuys
        if (profile.isRebuys())
        {
            String sKey = (profile.getMaxRebuys() == 0) ? "msg.rebuyinfo.unlimited" : "msg.rebuyinfo.limited";

            String sExpr = null;
            switch (profile.getRebuyExpressionType())
            {
                case PokerConstants.REBUY_LT:
                    sExpr = "&lt;";
                    break;

                case PokerConstants.REBUY_LTE:
                    sExpr = "&lt;=";

            }
            params[5] = PropertyConfig.getMessage(sKey,
                                                  profile.getMap().get(PARAM_REBUYCOST),
                                                  profile.getMap().get(PARAM_REBUYCHIPS),
                                                  profile.getMap().get(PARAM_REBUY_UNTIL),
                                                  profile.getMap().get(PARAM_MAXREBUYS),
                                                  sExpr,
                                                  profile.getRebuyChipCount());

        }
        else
        {
            params[5] = PropertyConfig.getMessage("msg.rebuyinfo.none");
        }

        // addons
        if (profile.isAddons())
        {
            params[6] = PropertyConfig.getMessage("msg.addoninfo",
                                                  profile.getMap().get(PARAM_ADDONCOST),
                                                  profile.getMap().get(PARAM_ADDONCHIPS),
                                                  profile.getMap().get(PARAM_ADDONLEVEL));
        }
        else
        {
            params[6] = PropertyConfig.getMessage("msg.rebuyinfo.none");
        }

        // minute per level
        params[7] = profile.getMap().get(PARAM_MINPERLEVEL_DEFAULT);

        // date
        params[8] = sDate;

        // name
        if (bListMode)
        {
            params[9] = PropertyConfig.getMessage("msg.tournamentname", Utils.encodeHTML(profile.getName()));
        }
        else
        {
            params[9] = "";
        }

        // limit info
        if (profile.hasLimitLevels())
        {
            params[10] = PropertyConfig.getMessage("msg.limitraises",
                                                   profile.getMaxRaises(10, false),
                                                   profile.isRaiseCapIgnoredHeadsUp() ?
                                                   PropertyConfig.getMessage("msg.ignoreheads") : "");
        }
        else
        {
            params[10] = "";
        }

        if (profile.getFile() != null)
        {
            String sFile = GameConfigUtils.SAVE_DIR + File.separator + TOURNAMENT_DIR + File.separator + profile.getFile().getName();
            params[11] = PropertyConfig.getMessage("msg.savefile", sFile);
        }
        else
        {
            params[11] = "";
        }

        // player list
        List<String> names = profile.getPlayers();
        if (names.isEmpty())
        {
            params[12] = "";
        }
        else
        {
            String sNames = Utils.toString(names);
            params[12] = PropertyConfig.getMessage("msg.players", Utils.encodeHTML(sNames));
        }

        // invite list
        if (bListMode)
        {
            String sInfo;
            if (!profile.isInviteOnly()) sInfo = PropertyConfig.getYesNo(false);
            else sInfo = PropertyConfig.getMessage("msg.inviteonly." + profile.isInviteObserversPublic(),
                                                   profile.getInvitees().toCSV());
            params[13] = PropertyConfig.getMessage("msg.inviteonlyrow", sInfo);
        }
        else
        {
            params[13] = "";
        }

        // get message
        htmlSummaryCache_ = PropertyConfig.getMessage("msg.tournamentsummary", params);
        htmlSummaryMode_ = bListMode;
        return htmlSummaryCache_;
    }

    /**
     * Get HTML summary of this tournament (used on server for jsp)
     *
     * @param sLocale
     */
    public String toHTML(String sLocale)
    {
        if (htmlCache_ != null) return htmlCache_;

        String sDate = PropertyConfig.getDateFormat(sLocale).format(new Date(profile.getCreateDate()));


        Object params[] = new Object[18];

        params[0] = Utils.encodeHTML(profile.getName());
        params[1] = sDate;
        params[2] = PropertyConfig.getMessage("msg.numplayers", profile.getMap().get(PARAM_NUM_PLAYERS),
                                              profile.getSeats());
        params[3] = profile.getMap().get(PARAM_BUYIN);
        params[4] = profile.getMap().get(PARAM_BUYINCHIPS);

        // rebuys
        if (profile.isRebuys())
        {
            String sKey = (profile.getMaxRebuys() == 0) ? "msg.rebuyinfo.unlimited" : "msg.rebuyinfo.limited";

            String sExpr = null;
            switch (profile.getRebuyExpressionType())
            {
                case PokerConstants.REBUY_LT:
                    sExpr = "&lt;";
                    break;

                case PokerConstants.REBUY_LTE:
                    sExpr = "&lt;=";

            }
            params[5] = PropertyConfig.getMessage(sKey,
                                                  profile.getMap().get(PARAM_REBUYCOST),
                                                  profile.getMap().get(PARAM_REBUYCHIPS),
                                                  profile.getMap().get(PARAM_REBUY_UNTIL),
                                                  profile.getMap().get(PARAM_MAXREBUYS),
                                                  sExpr,
                                                  profile.getRebuyChipCount());

        }
        else
        {
            params[5] = PropertyConfig.getMessage("msg.rebuyinfo.none");
        }

        // addons
        if (profile.isAddons())
        {
            params[6] = PropertyConfig.getMessage("msg.addoninfo",
                                                  profile.getMap().get(PARAM_ADDONCOST),
                                                  profile.getMap().get(PARAM_ADDONCHIPS),
                                                  profile.getMap().get(PARAM_ADDONLEVEL));
        }
        else
        {
            params[6] = PropertyConfig.getMessage("msg.rebuyinfo.none");
        }

        // minute per level
        params[7] = profile.getMap().get(PARAM_MINPERLEVEL_DEFAULT);

        // levels
        StringBuilder sb = new StringBuilder();
        int nLevel = 0;
        String sMinutes;
        String sGame;
        String sGameTypeDefault = profile.getDefaultGameTypeString();
        int nNum = profile.getLastLevel();
        boolean odd = true;
        for (int i = 1; i <= nNum; i++)
        {
            sMinutes = profile.getMap().getString(PARAM_MINUTES + i, "");
            sGame = profile.getGameTypeDisplay(i);

            nLevel++;

            if (profile.isBreak(i))
            {
                sb.append(PropertyConfig.getMessage("msg.breakinfo", nLevel, sMinutes, cssClass(odd)));
            }
            else
            {
                sb.append(PropertyConfig.getMessage("msg.levelinfo",
                                                    nLevel, getDollar(profile.getAnte(i)),
                                                    getDollar(profile.getSmallBlind(i)),
                                                    getDollar(profile.getBigBlind(i)),
                                                    sMinutes, sGame, cssClass(odd)));
            }

            odd = !odd;
        }

        // double blinds/antes
        nLevel++;
        sb.append(PropertyConfig.getMessage("msg.doubleinfo", nLevel,
                                            profile.isDoubleAfterLastLevel() ?
                                            PropertyConfig.getMessage("msg.double.true") :
                                            PropertyConfig.getMessage("msg.double.false", nLevel - 1),
                                            cssClass(odd)));

        params[8] = sb.toString();

        // prize pool
        int nPool = profile.getPrizePool();
        String sKey = "msg.pool.none";
        if (profile.isRebuys() && profile.isAddons()) sKey = "msg.pool.both";
        else if (profile.isRebuys()) sKey = "msg.pool.rebuys";
        else if (profile.isAddons()) sKey = "msg.pool.addons";
        params[9] = PropertyConfig.getMessage(sKey, nPool);

        // places
        params[10] = getSpotsHtml(true);

        // description
        params[11] = Utils.encodeHTML(profile.getDescription());

        // game type
        params[12] = DataElement.getDisplayValue(DATA_ELEMENT_GAMETYPE, sGameTypeDefault);

        // online info
        params[13] = toHTMLOnline();

        // limit info
        if (profile.hasLimitLevels())
        {
            params[14] = PropertyConfig.getMessage("msg.limitraises",
                                                   profile.getMaxRaises(10, false),
                                                   profile.isRaiseCapIgnoredHeadsUp() ?
                                                   PropertyConfig.getMessage("msg.ignoreheads") : "");
        }
        else
        {
            params[14] = "";
        }

        // player list
        List<String> names = profile.getPlayers();
        if (names.isEmpty())
        {
            params[15] = "";
        }
        else
        {
            String sNames = Utils.toString(names);
            params[15] = Utils.encodeHTML(sNames);
        }

        // fill
        params[16] = PropertyConfig.getMessage("msg.fillai." + (profile.isFillComputer() ? "on" : "off"));

        // invite list
        if (!profile.isInviteOnly()) params[17] = PropertyConfig.getYesNo(false);
        else params[17] = PropertyConfig.getMessage("msg.inviteonly." + profile.isInviteObserversPublic(),
                                                    profile.getInvitees().toCSV());

        // get message
        htmlCache_ = PropertyConfig.getMessage("msg.tournamentsettings", params);
        return htmlCache_;
    }

    private String cssClass(boolean odd)
    {
        return odd ? "odd" : "even";
    }

    private String getDollar(int n)
    {
        if (n == 0) return "";
        return PropertyConfig.getMessage("msg.blind.amount", n);

    }

    /**
     * Html of online options
     */
    public String toHTMLOnline()
    {
        String sBootDis;
        String sBootSit;
        String sInviteOnly;

        if (!profile.isBootDisconnect()) sBootDis = PropertyConfig.getYesNo(false);
        else sBootDis = PropertyConfig.getMessage("msg.boot", profile.getBootDisconnectCount());

        if (!profile.isBootSitout()) sBootSit = PropertyConfig.getYesNo(false);
        else sBootSit = PropertyConfig.getMessage("msg.boot", profile.getBootSitoutCount());

        if (!profile.isInviteOnly()) sInviteOnly = PropertyConfig.getYesNo(false);
        else sInviteOnly = PropertyConfig.getMessage("msg.inviteonly." + profile.isInviteObserversPublic(),
                                                     profile.getInvitees().toCSV());

        return PropertyConfig.getMessage("msg.tournamentonline",
                                         PropertyConfig.getYesNo(profile.isAllowDash()),
                                         PropertyConfig.getYesNo(profile.isFillComputer()),
                                         PropertyConfig.getYesNo(profile.isAllowDemo()),
                                         profile.getMaxObservers(),
                                         profile.getTimeoutSeconds(),
                                         profile.getThinkBankSeconds(),
                                         sBootDis,
                                         sBootSit,
                                         sInviteOnly,
                                         PropertyConfig.getYesNo(profile.isAllowAdvisor()),
                                         PropertyConfig.getYesNo(profile.isOnlineActivatedPlayersOnly())
        );
    }

    /**
     * Html of spot payment
     */
    public String toHTMLSpots()
    {
        return PropertyConfig.getMessage("msg.tournamentsettings.pool", getSpotsHtml(false));
    }


    /**
     * Get html for payout only
     */
    private String getSpotsHtml(boolean bShowPercAndEstimate)
    {
        StringBuilder sb = new StringBuilder();
        String sAmount;
        boolean odd = true;
        for (int i = 1; i <= profile.getNumSpots(); i++)
        {
            sAmount = getSpotHTML(i, bShowPercAndEstimate, null);
            sb.append(PropertyConfig.getMessage("msg.payoutinfo", i, sAmount, cssClass(odd)));
            odd = !odd;
        }

        return sb.toString();
    }

    /**
     * Get HTML for a single spot
     */
    public String getSpotHTML(int i, boolean bShowPercAndEstimate, String sExtraKey)
    {
        if (sExtraKey == null) sExtraKey = "";
        int payout = profile.getPayout(i);
        String sAmount;

        if (profile.isAllocPercent() && bShowPercAndEstimate)
        {
            sAmount = PropertyConfig.getMessage("msg.allocperc" + sExtraKey, profile.getSpotAsString(i), payout);
        }
        else
        {
            sAmount = PropertyConfig.getMessage("msg.allocnum" + sExtraKey, payout);
        }
        return sAmount;
    }

    public String getBlindsText(String prefix, int nLevel, boolean briefAmounts)
    {
        // show gametype if different from default
        String sGameType = profile.getGameTypeDisplay(nLevel);
        if (sGameType.length() > 0) sGameType = PropertyConfig.getMessage(prefix + "gametype", sGameType);

        int nAnte = profile.getAnte(nLevel);
        int nBig = profile.getBigBlind(nLevel);
        int nSmall = profile.getSmallBlind(nLevel);

        return PropertyConfig.getMessage(nAnte == 0 ? prefix + "blinds" : prefix + "blinds.a",
                                         briefAmounts ? PropertyConfig.getAmount(nSmall, true, true) : Integer.toString(nSmall),
                                         briefAmounts ? PropertyConfig.getAmount(nBig, true, true) : Integer.toString(nBig),
                                         nAnte == 0 ? null : (briefAmounts ? PropertyConfig.getAmount(nAnte, true, true) : Integer.toString(nAnte)),
                                         sGameType);
    }
}
