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
package com.donohoedigital.games.poker.wicket.panels;

import com.donohoedigital.games.poker.wicket.pages.*;
import com.donohoedigital.games.poker.wicket.pages.online.*;
import com.donohoedigital.wicket.common.*;
import com.donohoedigital.wicket.models.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;

/**
 * @author Doug Donohoe
 */
public class LeaderboardForm extends NameRangeSearchForm
{
    private static final long serialVersionUID = 42L;

    private StringModel type = new StringModel();
    private DropDownChoice<IntegerSelectChoice> gamesC;

    /**
     * @see org.apache.wicket.Component#Component(String)
     */
    public LeaderboardForm(String id, PageParameters params, final Class<? extends BasePokerPage> clazz,
                           final Leaderboard.LeaderData data, final String paramName, final String paramBegin,
                           final String paramEnd)
    {
        super(id, params, clazz, data, paramName, paramBegin, paramEnd, "Player");

        // get # of games
        int games = params.getAsInteger(Leaderboard.PARAM_GAMES, 5);
        data.setGames(games);

        // games list of choices
        SelectChoiceList<IntegerSelectChoice> gamesValues = new SelectChoiceList<IntegerSelectChoice>();
        for (int g : new int[]{1, 5, 10, 15, 20, 25, 30, 40, 50, 100, 150, 200, 250, 300, 500, 1000})
        {
            gamesValues.add(new IntegerSelectChoice(g));
        }
        IntegerSelectChoice gameChoice = gamesValues.addSorted(new IntegerSelectChoice(games));

        // model and drop down choice
        Model<IntegerSelectChoice> gameModel = new Model<IntegerSelectChoice>(gameChoice);
        gamesC = new DropDownChoice<IntegerSelectChoice>("games", gameModel, gamesValues, gamesValues);
        form.add(gamesC);

        // hidden field for ROI
        type.setObject(data.getType().name());
        HiddenField<String> typeH = new HiddenField<String>("type", type);
        form.add(typeH);
    }

    /**
     * add games selection & type to params (on submit)
     */
    @Override
    protected void addCustomPageParameters(PageParameters p)
    {
        p.put(Leaderboard.PARAM_GAMES, gamesC.getDefaultModelObjectAsString());
        p.put(Leaderboard.PARAM_TYPE, type.getObject());
    }
}
