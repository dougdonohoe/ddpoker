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
/*
 * TournamentHistory.java
 *
 * Created on April 25, 2004, 10:04 AM
 */

package com.donohoedigital.games.poker.model;

import com.donohoedigital.comms.*;
import com.donohoedigital.db.model.*;
import com.donohoedigital.xml.*;

import javax.persistence.*;
import java.util.*;

/**
 * @author donohoe
 */
@Entity
@Table(name = "wan_history")
@DataCoder('T')
public class TournamentHistory implements BaseModel<Long>, DataMarshal, SimpleXMLEncodable
{
    // members
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "whi_id", nullable = false)
    private Long id;

    @Column(name = "whi_buy_in", nullable = false)
    private int buyin;

    @Column(name = "whi_total_rebuy", nullable = false)
    private int rebuys;

    @Column(name = "whi_total_add_on", nullable = false)
    private int addons;

    @Column(name = "whi_finish_place", nullable = false)
    private int place;

    @Column(name = "whi_prize", nullable = false)
    private int prize;

    @Column(name = "whi_player_name", nullable = false)
    private String playerName;

    @Column(name = "whi_player_type", nullable = false)
    private int playerType;

    @Column(name = "whi_end_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "whi_game_id", nullable = false, updatable = false)
    private OnlineGame onlineGame;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "whi_profile_id", nullable = false, updatable = false)
    private OnlineProfile profile;

    @Column(name = "whi_is_ended", nullable = false)
    private boolean ended;   // server side only

    @Column(name = "whi_rank_1", nullable = false)
    private double rank1;    // server side only

    @Column(name = "whi_disco", nullable = false)
    private int disconnects;

    @Column(name = "whi_tournament_name", nullable = false)
    private String tournamentName;

    @Column(name = "whi_num_players", nullable = false)
    private int numPlayers;

    @Transient
    // used in client database only
    private long gameId;

    @Transient
    // used in client database only (ONLINE vs PRACTICE)
    private String tournamentType;

    @Transient
    // used in client database only
    private Date startDate;

    @Transient
    // used in client database only
    private int numRemaining;

    // player types
    public static final int PLAYER_TYPE_AI = 1;
    public static final int PLAYER_TYPE_ONLINE = 2;
    public static final int PLAYER_TYPE_LOCAL = 3;

    /**
     * Get id
     */
    public Long getId()
    {
        return id;
    }

    /**
     * Set id
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Get tournament name
     */
    public String getTournamentName()
    {
        return tournamentName;
    }

    /**
     * Set tournament name
     */
    public void setTournamentName(String sTournamentName)
    {
        tournamentName = sTournamentName;
    }

    /**
     * Get player name
     */
    public String getPlayerName()
    {
        return playerName;
    }

    /**
     * Set player name
     */
    public void setPlayerName(String sPlayerName)
    {
        playerName = sPlayerName;
    }

    /**
     * Return online profile (JPA usage only)
     */
    public OnlineProfile getProfile()
    {
        return profile;
    }

    /**
     * Set online profile (JPA usage only)
     */
    public void setProfile(OnlineProfile profile)
    {
        this.profile = profile;
    }

    /**
     * Get player type
     */
    public int getPlayerType()
    {
        return playerType;
    }

    /**
     * is AI
     */
    public boolean isComputer()
    {
        return playerType == PLAYER_TYPE_AI;
    }

    /**
     * Set player type
     */
    public void setPlayerType(int nPlayerType)
    {
        playerType = nPlayerType;
    }

    /**
     * Get end date
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     * Set end date
     */
    public void setEndDate(Date date)
    {
        endDate = date;
    }

    /**
     * Get total spent
     */
    public int getTotalSpent()
    {
        return getBuyin() + getRebuy() + getAddon();
    }

    /**
     * Get buyin
     */
    public int getBuyin()
    {
        return buyin;
    }

    /**
     * Set buyin
     */
    public void setBuyin(int nBuyin)
    {
        buyin = nBuyin;
    }

    /**
     * Get addon
     */
    public int getAddon()
    {
        return addons;
    }

    /**
     * Set addon
     */
    public void setAddon(int nAddon)
    {
        addons = nAddon;
    }

    /**
     * Get rebuys
     */
    public int getRebuy()
    {
        return rebuys;
    }

    /**
     * Set rebuys
     */
    public void setRebuy(int nRebuys)
    {
        rebuys = nRebuys;
    }

    /**
     * record place, prize and num players.  Deal with ended vesus non-ended games (when nPlace == 0,
     * place is set to 'nRank - (numPlayers + 1)' and nPrize is set to '-nChipCount')
     */
    public void setPlacePrizeNumPlayers(int nPlace, int nPrize, int numPlayers, int nRank, int nChipCount)
    {
        // if place is zero, use rank adjusted by num players + 1 so it is negative,
        // and order by retains same order
        if (nPlace == 0)
        {
            nPlace = nRank - (numPlayers + 1);
            nPrize = -nChipCount;
        }

        setPlace(nPlace);
        setPrize(nPrize);
        setNumPlayers(numPlayers);
    }

    /**
     * If a game is not finished, return whether player is still alive (e.g., has chips)
     */
    public boolean isAlive()
    {
        return !isEnded() && getPlace() < 0;
    }

    /**
     * If a game is not finished, get rank (i.e., their standing when game stopped)
     */
    public int getRank()
    {
        return place + (numPlayers + 1);
    }

    /**
     * If a game is not finished and the player is still alive, this returns
     * the number of chips a player had left.  We store this value as the a negative prize,
     * thus returns getPrize() * -1.
     */
    public int getNumChips()
    {
        return getPrize() * -1;
    }

    /**
     * Get Net
     */
    public int getNet()
    {
        return prize - getTotalSpent();
    }

    /**
     * Get place
     */
    public int getPlace()
    {
        return place;
    }

    /**
     * Set place
     */
    public void setPlace(int nPlace)
    {
        place = nPlace;
    }

    /**
     * Get prize
     */
    public int getPrize()
    {
        return prize;
    }

    /**
     * Set prize
     */
    public void setPrize(int nPrize)
    {
        prize = nPrize;
    }

    /**
     * Get num players in tournament
     */
    public int getNumPlayers()
    {
        return numPlayers;
    }

    /**
     * Set num players in tournament
     */
    public void setNumPlayers(int nNum)
    {
        numPlayers = nNum;
    }

    /**
     * Return online game (JPA usage only)
     */
    public OnlineGame getGame()
    {
        return onlineGame;
    }

    /**
     * Set online game (JPA usage only)
     */
    public void setGame(OnlineGame game)
    {
        onlineGame = game;
    }

    /**
     * discos
     */
    public int getDisconnects()
    {
        return disconnects;
    }

    /**
     * discos
     */
    public void setDisconnects(int n)
    {
        disconnects = n;
    }

    /**
     * set rank 1 (public player ranking 1)
     */
    public void setRank1(double d)
    {
        rank1 = d;
    }

    /**
     * get rank 1 (public player ranking 1)
     */
    public double getRank1()
    {
        return rank1;
    }

    /**
     * get DDR1 (rank1 as integer)
     */
    public int getDdr1()
    {
        return (int) rank1;
    }

    /**
     * get whether tournament ended
     */
    public boolean isEnded()
    {
        return ended;
    }

    /**
     * set whether tournament ended
     */
    public void setEnded(boolean b)
    {
        ended = b;
    }

    /////
    ///// Client-only methods
    /////

    /**
     * id
     */
    public long getGameId()
    {
        return gameId;
    }

    /**
     * id
     */
    public void setGameId(long gameId)
    {
        this.gameId = gameId;
    }

    /**
     * Get tournament type
     */
    public String getTournamentType()
    {
        return tournamentType;
    }

    /**
     * Set tournament type
     */
    public void setTournamentType(String sTournamentType)
    {
        tournamentType = sTournamentType;
    }

    /**
     * Get start date
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Set start date
     */
    public void setStartDate(Date date)
    {
        startDate = date;
    }

    /**
     * Get num players remaining in tournament
     */
    public int getNumRemaining()
    {
        return numRemaining;
    }

    /**
     * Set num players remaining in tournament
     */
    public void setNumRemaining(int nNum)
    {
        numRemaining = nNum;
    }

    /////
    ///// equality / marshalling
    /////

    /**
     * Equality based on id_
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof TournamentHistory)) return false;
        TournamentHistory h = (TournamentHistory) o;
        return id.equals(h.getId());
    }

    /**
     * hash
     */
    @Override
    public int hashCode()
    {
        if (id == null) return super.hashCode();
        else return (int) id.longValue();
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("Player=").append(getPlayerName());
        sb.append(", id=").append(getId());
        sb.append(", type=").append(getPlayerType());
        sb.append(", disco=").append(getDisconnects());
        sb.append(", place=").append(getPlace());
        sb.append(", rank=").append(getRank1());
        sb.append('}');

        return sb.toString();
    }

    /**
     * demarshal
     */
    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        tournamentName = list.removeStringToken();
        playerName = list.removeStringToken();
        playerType = list.removeIntToken();
        id = list.removeLongToken();
        endDate = new Date(list.removeLongToken());
        place = list.removeIntToken();
        prize = list.removeIntToken();
        buyin = list.removeIntToken();
        rebuys = list.removeIntToken();
        addons = list.removeIntToken();
        numPlayers = list.removeIntToken();

        // 2.5p1
        if (list.hasMoreTokens())
        {
            disconnects = list.removeIntToken();
        }
    }

    /**
     * marshal
     */
    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(tournamentName);
        list.addToken(playerName);
        list.addToken(playerType);
        list.addToken(id == null ? 0 : id.longValue());
        list.addToken(endDate.getTime());
        list.addToken(place);
        list.addToken(prize);
        list.addToken(buyin);
        list.addToken(rebuys);
        list.addToken(addons);
        list.addToken(numPlayers);
        list.addToken(disconnects);
        return list.marshal(state);
    }

    public void encodeXML(SimpleXMLEncoder encoder)
    {
        encoder.setCurrentObject(this, "result");

        // rank
        if (!isEnded() && isAlive())
        {
            encoder.addTags("rank");
        }
        // vs. finish place
        else
        {
            encoder.addTags("place");
        }

        // show prize when ended
        if (isEnded())
        {
            encoder.addTags("prize", "ddr1");
        }
        // otherwise show chips (if alive) or prize (if busted)
        else
        {
            encoder.addTags("alive");
            if (isAlive())
            {
                encoder.addTags("numChips");
            }
            else // busted
            {
                encoder.addTags("prize");
            }
        }

        // add rest
        encoder.addAllTagsExcept("id", "tournamentName", "numPlayers", "numRemaining",
                                 "disconnects", "game", "gameId", "profile",
                                 "totalSpent", "net", "rank1", "endDate",
                                 "alive", "rank", "place", "prize", "numChips", "ddr1");


        encoder.finishCurrentObject();
    }
}
