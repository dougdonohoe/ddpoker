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

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.comms.DataMarshaller;
import com.donohoedigital.db.model.BaseModel;
import com.donohoedigital.xml.SimpleXMLEncodable;
import com.donohoedigital.xml.SimpleXMLEncoder;
import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

/**
 * Wrapper for WAN games
 */
@Entity
@Table(name = "wan_game")
public class OnlineGame implements BaseModel<Long>, SimpleXMLEncodable
{
    //private static Logger logger = LogManager.getLogger(OnlineGame.class);

    private DMTypedHashMap data_;

    public static final String WAN_ID = "id";
    public static final String WAN_LICENSE_KEY = "licensekey";
    public static final String WAN_URL = "url";
    public static final String WAN_HOST_PLAYER = "hostplayer";
    public static final String WAN_MODE = "mode";
    public static final String WAN_START_DATE = "startdate";
    public static final String WAN_END_DATE = "enddate";
    public static final String WAN_CREATE_DATE = "createdate";
    public static final String WAN_MODIFY_DATE = "modifyddate";
    public static final String WAN_TOURNAMENT = "profile";

    // Web table columns
    public static final String WAN_TOURNAMENT_NAME = "tournamentname";
    public static final String WAN_STATUS = "status";

    // modes of play
    public static final int MODE_REG = 1;
    public static final int MODE_PLAY = 2;
    public static final int MODE_STOP = 3;
    public static final int MODE_END = 4;

    // used for list fetching (kind of a hack - would be nice if brian used ^2)
    public static final int FETCH_MODE_REG_PLAY = 102;

    /**
     * Creates an uninitialized instance of WanGame
     */
    public OnlineGame()
    {
        data_ = new DMTypedHashMap();
    }

    /**
     * Creates a new instance of WanGame
     * with the given source data
     */
    public OnlineGame(DMTypedHashMap data)
    {
        data_ = data;
        updateAfterDataChanged();
    }

    /**
     * merge contents of given game into this game
     */
    public void merge(OnlineGame game)
    {
        data_.putAll(game.getData());
        updateAfterDataChanged();
    }

    /**
     * do one-off updates required after bulk changes to data_
     */
    private void updateAfterDataChanged()
    {
        TournamentProfile tp = getTournament();
        if (tp != null) setTournament(tp); // do to ensure tournamentAsString is set
    }

    /**
     * Get internal hash map that holds data
     */
    @Transient
    public DMTypedHashMap getData() // FIX: eliminate this API
    {
        return data_;
    }

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wgm_id", nullable = false)
    public Long getId()
    {
        return data_.getLong(WAN_ID);
    }

    public void setId(Long id)
    {
        data_.setLong(WAN_ID, id);
    }

    @Column(name = "wgm_license_key", nullable = false)
    public String getLicenseKey()
    {
        return data_.getString(WAN_LICENSE_KEY);
    }

    public void setLicenseKey(String s)
    {
        data_.setString(WAN_LICENSE_KEY, s);
    }

    @Column(name = "wgm_url", nullable = false)
    public String getUrl()
    {
        return data_.getString(WAN_URL);
    }

    public void setUrl(String s)
    {
        data_.setString(WAN_URL, s);
    }

    @Column(name = "wgm_host_player", nullable = false)
    public String getHostPlayer()
    {
        return data_.getString(WAN_HOST_PLAYER);
    }

    public void setHostPlayer(String s)
    {
        data_.setString(WAN_HOST_PLAYER, s);
    }

    @Column(name = "wgm_mode", nullable = false)
    public int getMode()
    {
        return data_.getInteger(WAN_MODE);
    }

    public void setMode(int mode)
    {
        data_.setInteger(WAN_MODE, mode);
    }

    @Column(name = "wgm_start_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getStartDate()
    {
        return data_.getLongAsDate(WAN_START_DATE);
    }

    public void setStartDate(Date date)
    {
        data_.setLongFromDate(WAN_START_DATE, date);
    }

    @Column(name = "wgm_end_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getEndDate()
    {
        return data_.getLongAsDate(WAN_END_DATE);
    }

    public void setEndDate(Date date)
    {
        data_.setLongFromDate(WAN_END_DATE, date);
    }

    @Column(name = "wgm_create_date", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate()
    {
        return data_.getLongAsDate(WAN_CREATE_DATE);
    }

    public void setCreateDate(Date date)
    {
        data_.setLongFromDate(WAN_CREATE_DATE, date);
    }

    @Column(name = "wgm_modify_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModifyDate()
    {
        return data_.getLongAsDate(WAN_MODIFY_DATE);
    }

    public void setModifyDate(Date date)
    {
        data_.setLongFromDate(WAN_MODIFY_DATE, date);
    }

    /**
     * Store tournament internally for database marshalling
     */
    private String tournamentAsString;
    private boolean ignoreSet = false;

    @Column(name = "wgm_tournament_data", nullable = false)
    String getTournamentAsString()
    {
        return tournamentAsString;
    }

    /**
     * Demarshal.  Calls setTournament (unless called from there)
     */
    synchronized void setTournamentAsString(String s)
    {
        tournamentAsString = s;
        if (!ignoreSet)
        {
            ignoreSet = true;
            setTournament(s == null ? null : (TournamentProfile) DataMarshaller.demarshal(s));
            ignoreSet = false;
        }
    }

    @Transient
    public TournamentProfile getTournament()
    {
        return (TournamentProfile) data_.getObject(WAN_TOURNAMENT);
    }

    /**
     * Set Tournament.  Calls setTournamentAsString (unless called from there)
     */
    public synchronized void setTournament(TournamentProfile p)
    {
        data_.setObject(WAN_TOURNAMENT, p);
        if (!ignoreSet)
        {
            ignoreSet = true;
            setTournamentAsString(p == null ? null : DataMarshaller.marshal(p));
            ignoreSet = false;
        }
    }

    private List<TournamentHistory> histories;

    /**
     * Get tournament history for this game (server side only).
     */
    @OneToMany(mappedBy = "onlineGame", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    public List<TournamentHistory> getHistories()
    {
        return histories;
    }

    /**
     * Set this tournament history
     */
    public void setHistories(List<TournamentHistory> histories)
    {
        this.histories = histories;
    }

    /**
     * override equals - uses URL and license key for equality
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof OnlineGame)) return false;
        final OnlineGame other = (OnlineGame) o;

        // FIX: handle null
        return getLicenseKey().equals(other.getLicenseKey()) &&
               getUrl().equals(other.getUrl());
    }

    /**
     * override hashcode
     */
    @Override
    public int hashCode()
    {
        String sKey = getLicenseKey();
        String sURL = getUrl();
        if (sKey == null || sURL == null) return super.hashCode();

        int hash = 31 * super.hashCode() + sKey.hashCode();
        hash = 31 * hash + sURL.hashCode();

        return hash;
    }

    /**
     * Auto set create/modify date on insert
     */
    @PrePersist
    private void onInsert()
    {
        setCreateDate(new Date());
        setModifyDate(new Date());
    }

    /**
     * Auto set modify date on update
     */
    @PreUpdate
    private void onUpdate()
    {
        setModifyDate(new Date());
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return "OnlineGame: " + data_;
    }

    ////
    //// XML Encoding
    ////

    public void encodeXML(SimpleXMLEncoder encoder)
    {
        // game
        encoder.setCurrentObject(this, "game");
        encoder.addTags("id", "url", "hostPlayer", "mode", "startDate", "endDate");

        // tournament format
        getTournament().encodeXML(encoder);

        // histories
        List<TournamentHistory> hists = getHistories();
        if (hists != null && !hists.isEmpty())
        {
            encoder.setCurrentObject("results");
            for (TournamentHistory hist : getHistories())
            {
                hist.encodeXML(encoder);
            }
            encoder.finishCurrentObject(); // histories
        }

        // finish game
        encoder.finishCurrentObject();
    }
}
