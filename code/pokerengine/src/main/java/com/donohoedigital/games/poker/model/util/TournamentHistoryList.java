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
package com.donohoedigital.games.poker.model.util;

import com.donohoedigital.comms.*;
import com.donohoedigital.games.poker.model.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * WAN history list
 */
public class TournamentHistoryList extends DMArrayList<TournamentHistory>
{
    static Logger logger = LogManager.getLogger(TournamentHistoryList.class);

    private int totalSize_ = 0;

    /**
     * Default constructor
     */
    public TournamentHistoryList()
    {

    }

    /**
     * Creates a new instance of WanHistoryList
     */
    public TournamentHistoryList(List<TournamentHistory> list)
    {
        if (list != null) addAll(list);
    }

    /**
     * debug representation of list
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        int size = size();
        TournamentHistory msg;

        for (int i = 0; i < size; ++i)
        {
            msg = get(i);
            sb.append("Entry #").append(i).append(": ").append(msg).append('\n');
        }
        return sb.toString();
    }

    /**
     * Get the total number of available objects.
     */
    public int getTotalSize()
    {
        return totalSize_;
    }

    /**
     * Set the total number of available objects.
     */
    public void setTotalSize(int size)
    {
        totalSize_ = size;
    }

    /**
     * calculate rank for each player in this tournament history
     * Assumes items in list are sorted properly.  This is okay since
     * we send them down in ordr (WanManager) or pull them from the
     * DB ordered.
     *
     * @param bEnded
     * @param bDebug
     */
    public void calculateInfo(boolean bEnded, boolean bDebug)
    {
        TournamentHistory hist;
        int nAI = 0;

        for (int i = size() - 1; i >= 0; i--)
        {
            hist = get(i);
            if (hist.isComputer()) nAI++;
        }

        double aiReduction = .5d;
        double numPlayers = size();
        double exp = 3;
        double scaleTo = Math.sqrt(numPlayers-(aiReduction * nAI)) / Math.sqrt(10) * 5000;
        double max = Math.pow(numPlayers - 1, exp);
        double scale = scaleTo / max;
        double rank;

        for (int i = size() - 1; i >= 0; i--)
        {
            hist = get(i);
            rank = Math.pow(numPlayers - hist.getPlace(), exp) * scale;
            hist.setRank1(rank);
            hist.setEnded(bEnded);
            if (bDebug)
            {
                logger.debug("  ==> "+ hist.getPlayerName() + (hist.isComputer() ? " (ai)" : "") +
                         " finished " + hist.getPlace() + " and won " + hist.getPrize() +
                         " num ai: "+ nAI + "  rank: " + hist.getRank1() +
                         " ended: "+ hist.isEnded());
            }
        }
    }
}
