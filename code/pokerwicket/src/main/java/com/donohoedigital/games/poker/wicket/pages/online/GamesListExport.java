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
import com.donohoedigital.games.poker.model.util.*;
import com.donohoedigital.games.poker.service.*;
import com.donohoedigital.wicket.*;
import com.donohoedigital.wicket.annotations.*;
import com.donohoedigital.wicket.converters.*;
import com.donohoedigital.xml.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.html.*;
import org.apache.wicket.request.target.resource.*;
import org.apache.wicket.spring.injection.annot.*;
import org.apache.wicket.util.resource.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
@MountFixedMixedParam(parameterNames = {GamesListExport.PARAM_DAYS_AGO, GamesListExport.PARAM_DATE,
        GamesListExport.PARAM_FILE_NAME})
public abstract class GamesListExport extends WebPage
{
    private static final long serialVersionUID = 42L;

    public static final String PARAM_DATE = "date";
    public static final String PARAM_DAYS_AGO = "days";
    public static final String PARAM_FILE_NAME = "file";

    @SuppressWarnings({"NonSerializableFieldInSerializableClass"})
    @SpringBean
    private OnlineGameService gameService;

    @SuppressWarnings({"AbstractMethodCallInConstructor"})
    public GamesListExport(PageParameters params)
    {
        ParamDateConverter CONVERTER = new ParamDateConverter();
        Date day;
        Integer daysago;
        String file;

        if (params == null) params = new PageParameters();

        // get params
        daysago = params.getAsInteger(PARAM_DAYS_AGO);
        file = params.getString(PARAM_FILE_NAME);
        if (daysago != null)
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
        StringBuilder comment = new StringBuilder();

        comment.append("Copyright (c) 2004-").append(Utils.getDateYear()).append(". Donohoe Digital LLC\n");
        comment.append("DD Poker ").append(getGameTypeForComment())
                .append(" games export from ").append(begin).append(" to ").append(end);

        encoder.addComment(comment.toString(), true);
        encoder.setCurrentObject("ddpoker");
        encoder.add(games);
        encoder.finishCurrentObject();

        // create target
        ResourceStreamRequestTarget target = new ResourceStreamRequestTarget(
                new StringResourceStream(encoder.toString(), "text/xml"));

        // set file name if provided
        if (file != null)
        {
            target.setFileName(file);
        }

        // respond with target
        getRequestCycle().setRequestTarget(target);
    }

    /**
     * Subclass to return true if only should use current day only (from midnight to midnight).  Alternative is
     * to use 24 hour period preceding date.  Default is false.
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
