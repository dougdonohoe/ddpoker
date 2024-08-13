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
package com.donohoedigital.games.poker.model;

/**
 * Leaderboard Summary data
 */
public class LeaderboardSummary
{    
    private int rank;
    private int percentile;
    private int gamesPlayed;
    private long profileId;
    private String playerName;
    private int ddr1;
    private int totalBuyin;
    private int totalAddon;
    private int totalRebuys;
    private int totalPrizes;

    public int getRank()
    {
        return rank;
    }

    public void setRank(int rank)
    {
        this.rank = rank;
    }

    public int getPercentile()
    {
        return percentile;
    }

    public void setPercentile(int percentile)
    {
        this.percentile = percentile;
    }

    public int getGamesPlayed()
    {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed)
    {
        this.gamesPlayed = gamesPlayed;
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }

    public long getProfileId()
    {
        return profileId;
    }

    public void setProfileId(long profileId)
    {
        this.profileId = profileId;
    }

    public int getDdr1()
    {
        return ddr1;
    }

    public void setDdr1(int ddr1)
    {
        this.ddr1 = ddr1;
    }

    public int getTotalAddon()
    {
        return totalAddon;
    }

    public void setTotalAddon(int totalAddon)
    {
        this.totalAddon = totalAddon;
    }

    public int getTotalBuyin()
    {
        return totalBuyin;
    }

    public void setTotalBuyin(int totalBuyin)
    {
        this.totalBuyin = totalBuyin;
    }

    public int getTotalPrizes()
    {
        return totalPrizes;
    }

    public void setTotalPrizes(int totalPrizes)
    {
        this.totalPrizes = totalPrizes;
    }

    public int getTotalRebuys()
    {
        return totalRebuys;
    }

    public void setTotalRebuys(int totalRebuys)
    {
        this.totalRebuys = totalRebuys;
    }

    public int getNet()
    {
        return getTotalPrizes() - getTotalSpent();
    }

    public int getTotalSpent()
    {
        return getTotalBuyin() + getTotalRebuys() + getTotalAddon();
    }

    public double getRoi()
    {
        return getNet() / (double) getTotalSpent();
    }
}
