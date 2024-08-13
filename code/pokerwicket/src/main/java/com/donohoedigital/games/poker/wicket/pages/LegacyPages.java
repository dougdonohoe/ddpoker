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
package com.donohoedigital.games.poker.wicket.pages;

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.model.*;
import static com.donohoedigital.games.poker.service.TournamentHistoryService.LeaderboardType.*;
import com.donohoedigital.games.poker.wicket.pages.about.*;
import com.donohoedigital.games.poker.wicket.pages.download.*;
import com.donohoedigital.games.poker.wicket.pages.home.*;
import com.donohoedigital.games.poker.wicket.pages.online.*;
import com.donohoedigital.games.poker.wicket.pages.store.*;
import com.donohoedigital.games.poker.wicket.pages.support.*;
import com.donohoedigital.wicket.*;
import com.donohoedigital.wicket.annotations.*;
import com.donohoedigital.wicket.converters.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.html.*;
import org.apache.wicket.protocol.http.*;
import org.wicketstuff.annotation.mount.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
@MountPath(path = "poker", alt = {
        "index.html",
        "terms.html",
        "demo.html",
        "order.html",
        "ordermain.html",
        "orderbox.html",
        "ordermainbox.html",
        "orderupgrade.html",
        "orderaffiliate.html",
        "dl.html",
        "dl_v1.html",
        "patch1.html",
        "patch2.html",
        "online.html",
        "upgrade.html",
        "support.html",
        "support-v1.html",
        "support-v2.html",
        "support-passwords.html",
        "support-firewall.html",
        "support-activation-steps.html",
        "support-win-badfonts.html",
        "support-win-install-logfiles.html",
        "support-win-logfiles.html",
        "support-win-logfiles-old.html",
        "support-win-steps.html",
        "support-mac-logfiles.html",
        "determineversion.html",
        "customerscan.html",
        "firewall.html",
        "findactivation.html",
        "details.html",
        "details-v2.html",
        "details-analysis.html",
        "details-competition.html",
        "details-online.html",
        "details-pokernight.html",
        "details-screenshots.html",
        "details-techspecs.html",
        "details-tournaments.html",
        "faq.html",
        "faq-links.html",
        "faq-poker.html",
        "faq-schedule.html",
        "faq-software.html",
        "faq-tournaments.html",
        "dl_pub",
        "dl_paid_priv",
        "javadoc",
        "gamehelp"
})
@MountFixedMixedParam(parameterNames = {LegacyPages.PARAM_JSP_PAGE})
public class LegacyPages extends WebPage
{
    private static final long serialVersionUID = 42L;

    private static final String[] OLDPAGES;

    private static final Map<String, Class<? extends WebPage>> PAGEMAP;


    static
    {
        // TODO: figure how to fix mount alt logic so we don't have to repeat each legacy page
        Map<String, Class<? extends WebPage>> pages = new HashMap<String, Class<? extends WebPage>>();
        pages.put("index.html", HomeHome.class);
        pages.put("terms.html", Terms.class);
        pages.put("demo.html", DownloadHome.class);
        pages.put("order.html", DownloadHome.class);
        pages.put("ordermain.html", DownloadHome.class);
        pages.put("orderbox.html", DownloadHome.class);
        pages.put("ordermainbox.html", DownloadHome.class);
        pages.put("orderupgrade.html", DownloadHome.class);
        pages.put("orderaffiliate.html", DownloadHome.class);
        pages.put("dl.html", DownloadHome.class);
        pages.put("dl_v1.html", DownloadHome.class);
        pages.put("patch1.html", DownloadHome.class);
        pages.put("patch2.html", DownloadHome.class);
        pages.put("online.html", OnlineHome.class);
        pages.put("upgrade.html", DownloadHome.class);
        pages.put("support.html", SupportHome.class);
        pages.put("support-v1.html", SupportHome.class);
        pages.put("support-v2.html", SupportHome.class);
        pages.put("support-passwords.html", PasswordHelp.class);
        pages.put("support-firewall.html", OnlineSupplement.class);
        pages.put("support-activation-steps.html", SupportHome.class);
        pages.put("support-win-badfonts.html", SupportHome.class);
        pages.put("support-win-install-logfiles.html", SupportHome.class);
        pages.put("support-win-logfiles.html", SupportHome.class);
        pages.put("support-win-logfiles-old.html", SupportHome.class);
        pages.put("support-win-steps.html", SupportHome.class);
        pages.put("support-mac-logfiles.html", SupportHome.class);
        pages.put("determineversion.html", SupportHome.class);
        pages.put("customerscan.html", DownloadHome.class);
        pages.put("firewall.html", AboutHome.class);
        pages.put("findactivation.html", DownloadHome.class);
        pages.put("details.html", AboutHome.class);
        pages.put("details-v2.html", AboutHome.class);
        pages.put("details-analysis.html", AboutAnalysis.class);
        pages.put("details-competition.html", AboutCompetition.class);
        pages.put("details-online.html", AboutOnline.class);
        pages.put("details-pokernight.html", AboutPokerClock.class);
        pages.put("details-screenshots.html", AboutScreenshots.class);
        pages.put("details-techspecs.html", DownloadHome.class);
        pages.put("details-tournaments.html", AboutPractice.class);
        pages.put("faq.html", AboutFaq.class);
        pages.put("faq-links.html", AboutFaq.class);
        pages.put("faq-poker.html", AboutFaq.class);
        pages.put("faq-schedule.html", AboutFaq.class);
        pages.put("faq-software.html", AboutFaq.class);
        pages.put("faq-tournaments.html", AboutFaq.class);

        PAGEMAP = pages;
        OLDPAGES = pages.keySet().toArray(new String[pages.size()]);
    }

    public static final String PARAM_JSP_PAGE = "jsp";
    private BaseBufferedWebResponse webresponse;


    public LegacyPages(PageParameters params)
    {
        String jsp = params.getString(PARAM_JSP_PAGE);
        if (jsp == null) jsp = "/";

        // use our webresponse to do moved permantently
        webresponse = (BaseBufferedWebResponse) getWebRequestCycle().getWebResponse();

        // response page params
        PageParameters p = new PageParameters();

        // look for matches
        final String path = getWebRequestCycle().getWebRequest().getPath();
        Class<? extends Page> page = getLegacy(path);

        // first check javadoc
        if (path.contains("javadoc") || path.contains("gamehelp"))
        {
            moved("https://static.ddpoker.com/" + path);
        }
        else if (path.contains("dl_pub") || (path.contains("dl_paid_priv")))
        {
            redirectPermanent(DownloadHome.class);
        }
        else if (page != null)
        {
            redirectPermanent(page);
        }
        else if (jsp.equals("leaderboard_roi.jsp"))
        {
            int type = params.getAsInteger("type", 1);
            int days = params.getAsInteger("days", 90);
            int min = params.getAsInteger("min", 5);
            ParamDateConverter CONVERTER = new ParamDateConverter();

            p.add(Leaderboard.PARAM_TYPE, String.valueOf(type == 1 ? ddr1 : roi));
            p.add(Leaderboard.PARAM_GAMES, String.valueOf(min));
            p.add(Leaderboard.PARAM_BEGIN, CONVERTER.convertToString(Utils.getDateDays(-days)));

            redirectPermanent(Leaderboard.class, p);
        }
        else if (jsp.equals("game_list.jsp"))
        {
            int mode = params.getAsInteger("mode", OnlineGame.MODE_REG);

            switch (mode)
            {
                case OnlineGame.MODE_REG:
                    redirectPermanent(AvailableGames.class);
                    break;

                case OnlineGame.MODE_PLAY:
                    redirectPermanent(RunningGames.class);
                    break;

                case OnlineGame.MODE_END:
                case OnlineGame.MODE_STOP:
                default:
                    redirectPermanent(RecentGames.class);
                    break;
            }
        }
        else if (jsp.equals("history_list.jsp"))
        {
            p.add(History.PARAM_NAME, params.getString("name"));
            redirectPermanent(History.class, p);
        }
        else if (jsp.equals("history_detail.jsp") || jsp.equals("game_detail.jsp"))
        {
            p.add(GameDetail.PARAM_GAME_ID, params.getString("gameID"));
            p.add(GameDetail.PARAM_HISTORY_ID, params.getString("historyID"));
            redirectPermanent(GameDetail.class, p);
        }
        else if (jsp.equals("upgrade_request.jsp"))
        {
            redirectPermanent(Upgrade.class);
        }
        else
        {
            redirectPermanent(HomeHome.class);

        }
    }

    public static Class<? extends WebPage> getLegacy(String path)
    {
        for (String key : OLDPAGES)
        {
            if (path.endsWith(key))
            {
                return PAGEMAP.get(key);
            }
        }
        return null;
    }

    private <C extends Page> void redirectPermanent(final Class<C> cls)
    {
        redirectPermanent(cls, null);
    }

    private <C extends Page> void redirectPermanent(final Class<C> cls, PageParameters params)
    {
        final CharSequence relative = urlFor(cls, params);
        String url = RequestUtils.toAbsolutePath(relative.toString());
        moved(url);
    }

    private void moved(String url)
    {
        webresponse.moved(url);
        setRedirect(true); // TODO: why is this needed?
    }


}
