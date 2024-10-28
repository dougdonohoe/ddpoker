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
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.wicket.WicketUtils;
import com.donohoedigital.wicket.annotations.MountMixedParam;
import com.donohoedigital.wicket.converters.ParamDateConverter;
import com.donohoedigital.xml.SimpleXMLEncoder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

import java.util.Date;

/**
 * @author Doug Donohoe
 */
@MountMixedParam(parameterNames = {GamesListExport.PARAM_DAYS_AGO, GamesListExport.PARAM_DATE,
        GamesListExport.PARAM_FILE_NAME})
public abstract class GamesListExport extends WebPage
{
    private static final long serialVersionUID = 42L;

    public static final String PARAM_DATE = "date";
    public static final String PARAM_DAYS_AGO = "days";
    public static final String PARAM_FILE_NAME = "file";

    @SuppressWarnings("unused")
    @SpringBean
    private OnlineGameService gameService;

    public GamesListExport(PageParameters params)
    {
        ParamDateConverter CONVERTER = new ParamDateConverter();
        Date day;
        int daysago;
        String file;

        if (params == null) params = new PageParameters();

        // get params
        daysago = WicketUtils.getAsInt(params, PARAM_DAYS_AGO, -1);
        file = params.get(PARAM_FILE_NAME).toString();
        if (daysago != -1)
        {
            day = Utils.getDateDays(-daysago);
        }
        else
        {
            day = WicketUtils.getAsDate(params, PARAM_DATE, null, CONVERTER);
            if (day == null)
            {
                day = new Date();
            }
        }

        Date begin;
        Date end;

        if (getUseCalendarDayOnly())
        {
            begin = Utils.getDateZeroTime(day);
            end = Utils.getDateEndOfDay(day);
        }
        else
        {
            begin = Utils.getDateDays(day, -1);
            end = day;
        }

        // fetch data
        OnlineGameList games = gameService.getOnlineGamesAndHistoriesForDay(getModes(), begin, end);

        // encode it
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();

        String comment = "Copyright (c) 2004-" + Utils.getDateYear() + ". Donohoe Digital LLC\n" +
                "DD Poker " + getGameTypeForComment() +
                " games export from " + begin + " to " + end;

        encoder.addComment(comment, true);
        encoder.setCurrentObject("ddpoker");
        encoder.add(games);
        encoder.finishCurrentObject();

        // create target
        IResourceStream resourceStream = new StringResourceStream(encoder.toString(), "text/xml");
        ResourceStreamRequestHandler target = new ResourceStreamRequestHandler(resourceStream);

        // set file name if provided
        if (file != null)
        {
            target.setFileName(file);
        }

        // Set the response to use the new handler
        WicketUtils.getRequestCycle().scheduleRequestHandlerAfterCurrent(target);
    }

    /**
     * Subclass to return true if only should use current day only (from midnight to midnight).  Alternative is
     * to use 24-hour period preceding date.  Default is false.
     */
    protected boolean getUseCalendarDayOnly()
    {
        return false;
    }

    /**
     * Get mode of games to list
     */
    protected abstract Integer[] getModes();

    /**
     * Get type of games for comment
     */
    protected abstract String getGameTypeForComment();
}
