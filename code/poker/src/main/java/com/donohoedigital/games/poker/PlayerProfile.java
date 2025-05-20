/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * PlayerProfile.java
 *
 * Created on January 23, 2004, 12:37 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.model.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 * @author donohoe
 */
public class PlayerProfile extends BaseProfile
{
    private static Logger logger = LogManager.getLogger(PlayerProfile.class);

    // defines
    public static final String PROFILE_BEGIN = "profile";

    // online info
    private boolean bActivated_;
    private String sEmail_;
    private String sPassword_;

    // stats on hands seen and other stuff.  package private for quicker access
    int nWins_;
    int nActionCnt_;
    int[] rounds_;
    int[] flops_;
    int[] actions_;
    int[] nRoundActionCnt_;
    int[][] roundactions_;

    DMTypedHashMap map;

    /**
     * debug
     */
    public void debugPrint()
    {
        logger.debug("");
        logger.debug("************ PROFILE for " + getName());
        logger.debug("Wins: " + nWins_);

        // number of times each round is seen
        for (int i = 0; i < rounds_.length; i++)
        {
            logger.debug("Round " + HoldemHand.getRoundName(i) + ": " + rounds_[i]);
            // flops seen - which position
            if (i == HoldemHand.ROUND_FLOP)
            {
                for (int j = 0; j < flops_.length; j++)
                {
                    logger.debug("   Flops called from " + PokerPlayer.getPositionName(j) + ": " + flops_[j]);
                }
            }
        }

        // actions
        logger.debug("Total Actions: " + nActionCnt_);
        for (int i = 0; i < actions_.length; i++)
        {
            logger.debug("   Action " + HandAction.getActionName(i) + ": " + actions_[i]);
        }

        // actions per round
        for (int i = 0; i < roundactions_.length; i++)
        {
            logger.debug("Total Actions it round " + HoldemHand.getRoundName(i) + ": " + nRoundActionCnt_[i]);
            for (int j = 0; j < roundactions_[0].length; j++)
            {
                logger.debug("   Round " + HoldemHand.getRoundName(i) + ", Action " + HandAction.getActionName(j) + ": " + roundactions_[i][j]);
            }
        }
    }

    /**
     * Load profile from file
     */
    public PlayerProfile(File file, boolean bFull)
    {
        super(file, bFull);
    }

    /**
     * New profile with given name
     */
    public PlayerProfile(String sName)
    {
        super(sName);
    }

    /**
     * New profile copied from given profile, using new name
     */
    public PlayerProfile(PlayerProfile tp, String sName)
    {
        super(sName);

        // use same objects - when copying since they
        // aren't modified in the way they are used
        // we don't ever copy - we just edit
        bActivated_ = tp.bActivated_;
        sEmail_ = tp.sEmail_;
        sPassword_ = tp.sPassword_;
        nWins_ = tp.nWins_;
        nActionCnt_ = tp.nActionCnt_;
        rounds_ = tp.rounds_;
        flops_ = tp.flops_;
        actions_ = tp.actions_;
        nRoundActionCnt_ = tp.nRoundActionCnt_;
        roundactions_ = tp.roundactions_;
    }

    /**
     * check if init'd (for lazily eval)
     */
    public void initCheck()
    {
        if (rounds_ == null)
        {
            init();
        }
    }

    /**
     * init arrays
     */
    public void init()
    {
        nWins_ = 0;
        nActionCnt_ = 0;
        rounds_ = new int[HoldemHand.ROUND_SHOWDOWN + 1];
        flops_ = new int[PokerPlayer.BIG + 1];
        actions_ = new int[HandAction.ACTION_RAISE + 1];
        nRoundActionCnt_ = new int[HoldemHand.ROUND_SHOWDOWN];
        roundactions_ = new int[HoldemHand.ROUND_SHOWDOWN][HandAction.ACTION_RAISE + 1];

        // seed data with expected amount of folding, calling, raising, etc
        roundactions_[HoldemHand.ROUND_PRE_FLOP][HandAction.ACTION_FOLD] = 14;
        roundactions_[HoldemHand.ROUND_PRE_FLOP][HandAction.ACTION_CHECK] = 1;
        roundactions_[HoldemHand.ROUND_PRE_FLOP][HandAction.ACTION_CALL] = 3;
        roundactions_[HoldemHand.ROUND_PRE_FLOP][HandAction.ACTION_BET] = 0;
        roundactions_[HoldemHand.ROUND_PRE_FLOP][HandAction.ACTION_RAISE] = 2;
        nRoundActionCnt_[HoldemHand.ROUND_PRE_FLOP] = 20;

        roundactions_[HoldemHand.ROUND_FLOP][HandAction.ACTION_FOLD] = 6;
        roundactions_[HoldemHand.ROUND_FLOP][HandAction.ACTION_CHECK] = 4;
        roundactions_[HoldemHand.ROUND_FLOP][HandAction.ACTION_CALL] = 2;
        roundactions_[HoldemHand.ROUND_FLOP][HandAction.ACTION_BET] = 4;
        roundactions_[HoldemHand.ROUND_FLOP][HandAction.ACTION_RAISE] = 4;
        nRoundActionCnt_[HoldemHand.ROUND_FLOP] = 20;

        roundactions_[HoldemHand.ROUND_TURN][HandAction.ACTION_FOLD] = 6;
        roundactions_[HoldemHand.ROUND_TURN][HandAction.ACTION_CHECK] = 4;
        roundactions_[HoldemHand.ROUND_TURN][HandAction.ACTION_CALL] = 2;
        roundactions_[HoldemHand.ROUND_TURN][HandAction.ACTION_BET] = 4;
        roundactions_[HoldemHand.ROUND_TURN][HandAction.ACTION_RAISE] = 4;
        nRoundActionCnt_[HoldemHand.ROUND_TURN] = 20;

        roundactions_[HoldemHand.ROUND_RIVER][HandAction.ACTION_FOLD] = 6;
        roundactions_[HoldemHand.ROUND_RIVER][HandAction.ACTION_CHECK] = 4;
        roundactions_[HoldemHand.ROUND_RIVER][HandAction.ACTION_CALL] = 2;
        roundactions_[HoldemHand.ROUND_RIVER][HandAction.ACTION_BET] = 4;
        roundactions_[HoldemHand.ROUND_RIVER][HandAction.ACTION_RAISE] = 4;
        nRoundActionCnt_[HoldemHand.ROUND_RIVER] = 20;

    }

    /**
     * Is this an online profile
     */
    public boolean isOnline()
    {
        return (getEmail() != null);
    }

    /**
     * Is this an activated online profile
     */
    public boolean isActivated()
    {
        return bActivated_;
    }

    /**
     * Set activated flag
     */
    public void setActivated(boolean bActivated)
    {
        bActivated_ = bActivated;
    }

    /**
     * Get player email
     */
    public String getEmail()
    {
        return sEmail_;
    }

    /**
     * Set player email
     */
    public void setEmail(String sEmail)
    {
        sEmail_ = sEmail;
    }

    /**
     * Get player password
     */
    public String getPassword()
    {
        return (sPassword_ != null) ? Utils.decode(SecurityUtils.decrypt(sPassword_, k())) : null;
    }

    /**
     * Set player password
     */
    public void setPassword(String sPassword)
    {
        if (sPassword != null) sPassword_ = SecurityUtils.encrypt(Utils.encode(sPassword), k());
    }

    /**
     * Check matching password
     */
    public boolean isMatchingPassword(String s)
    {
        if ((s == null) || (sPassword_ == null)) return false;
        s = SecurityUtils.encrypt(Utils.encode(s), k());
        return s.equals(sPassword_);
    }

    /**
     * Get stats map
     */
    public DMTypedHashMap getStatsMap()
    {
        return map;
    }

    /**
     * Create an Online Profile for the given local profile
     */
    public OnlineProfile toOnlineProfile()
    {
        OnlineProfile profile = new OnlineProfile(getName());
        profile.setPassword(getPassword());
        profile.setActivated(isActivated());
        profile.setEmail(getEmail());

        return profile;
    }

    /**
     * Get how often action happens in round (0-100%)
     */
    public int getFrequency(int nRound, int nAction)
    {
        return (int) ((100.0f * roundactions_[nRound][nAction]) / (float) nRoundActionCnt_[nRound]);
    }

    /**
     * Add history and save if possible
     */
    public void addTournamentHistory(PokerGame game, PokerPlayer player)
    {
        PokerDatabase.storeTournamentFinish(game, player);
    }

    /**
     * Get list of history
     */
    public List<TournamentHistory> getHistory()
    {
        return PokerDatabase.getTournamentHistory(this);
    }

    /**
     * Get overall history
     */
    public TournamentHistory getOverallHistory()
    {
        return PokerDatabase.getOverallHistory(this);
    }

    /**
     * test database (using during startup)
     */
    public void testDB()
    {
        PokerDatabase.testConnection();
    }

    /**
     * get total prize money earned
     */
    public int getTotalPrizeMoneyEarned()
    {
        int nTotalPrize = 0;

        List<TournamentHistory> history = getHistory();
        TournamentHistory hist;
        if (history.size() == 0)
        {
            return 0;
        }
        else
        {
            for (int i = history.size() - 1; i >= 0; i--)
            {
                hist = history.get(i);
                if (hist.getPlace() == 0) continue;
                nTotalPrize += hist.getPrize();
            }
        }

        return nTotalPrize;
    }

    /**
     * get total prize money earned
     */
    public int getTotalMoneySpent()
    {
        int nTotalSpent = 0;

        List<TournamentHistory> history = getHistory();
        TournamentHistory hist;
        if (history.size() == 0)
        {
            return 0;
        }
        else
        {
            for (int i = history.size() - 1; i >= 0; i--)
            {
                hist = history.get(i);
                if (hist.getPlace() == 0) continue;
                nTotalSpent += hist.getTotalSpent();
            }
        }

        return nTotalSpent;
    }

    /**
     * Get begin part of profile name
     */
    protected String getBegin()
    {
        return PROFILE_BEGIN;
    }

    /**
     * Get name of directory to store profiles in
     */
    protected String getProfileDirName()
    {
        return PROFILE_DIR;
    }

    /**
     * subclass implements to load its contents from the given reader
     */
    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        // init
        init();

        // online info
        TokenizedList info = new TokenizedList();
        info.demarshal(null, buf.readLine());
        bActivated_ = info.removeBooleanToken();
        sEmail_ = info.removeStringToken();
        sPassword_ = info.removeStringToken();

        // stats
        TokenizedList stats = new TokenizedList();
        stats.demarshal(null, buf.readLine());

        for (int i = 0; i < rounds_.length; i++)
        {
            rounds_[i] = stats.removeIntToken();
        }
        for (int i = 0; i < flops_.length; i++)
        {
            flops_[i] = stats.removeIntToken();
        }
        nWins_ = stats.removeIntToken();
        nActionCnt_ = stats.removeIntToken();
        for (int i = 0; i < actions_.length; i++)
        {
            actions_[i] = stats.removeIntToken();
        }
        for (int i = 0; i < roundactions_.length; i++)
        {
            nRoundActionCnt_[i] = stats.removeIntToken();
            for (int j = 0; j < roundactions_[0].length; j++)
            {
                roundactions_[i][j] = stats.removeIntToken();
            }
        }
        if (stats.hasMoreTokens() && (stats.peekToken() instanceof DMTypedHashMap))
        {
            map = (DMTypedHashMap) stats.removeToken();
        }
        else
        {
            map = new DMTypedHashMap();
        }
    }

    /**
     * subclass implements to save its contents to the given writer
     */
    public void write(Writer writer) throws IOException
    {
        super.write(writer);

        // online info
        TokenizedList info = new TokenizedList();
        info.addToken(bActivated_);
        info.addToken(sEmail_);
        info.addToken(sPassword_);
        writer.write(info.marshal(null));
        writeEndEntry(writer);

        // stats
        TokenizedList stats = new TokenizedList();
        for (int aRounds_ : rounds_)
        {
            stats.addToken(aRounds_);
        }
        for (int aFlops_ : flops_)
        {
            stats.addToken(aFlops_);
        }
        stats.addToken(nWins_);
        stats.addToken(nActionCnt_);
        for (int anActions_ : actions_)
        {
            stats.addToken(anActions_);
        }
        for (int i = 0; i < roundactions_.length; i++)
        {
            stats.addToken(nRoundActionCnt_[i]);
            for (int j = 0; j < roundactions_[0].length; j++)
            {
                stats.addToken(roundactions_[i][j]);
            }
        }
        stats.addToken(map);
        writer.write(stats.marshal(null));
        writeEndEntry(writer);
    }

    /**
     * Get profile list
     */
    protected List<BaseProfile> getProfileFileList()
    {
        return getProfileList();
    }

    /**
     * Get list of save files in save directory
     */
    public static List<BaseProfile> getProfileList()
    {
        return BaseProfile.getProfileList
                (PROFILE_DIR, Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), PlayerProfile.class, false);
    }

    /**
     * return File for given profile name
     */
    public static File getProfileFile(String sFileName)
    {
        return new File(PlayerProfile.getProfileDir(PlayerProfile.PROFILE_DIR), sFileName);
    }

    private byte[] k()
    {
        String s = "48349ad7a22d3b47445d309921323379";
        byte[] k = new byte[s.length() / 2];
        int n = 0;

        for (int i = 0; i < k.length; ++i)
        {
            n = i * 2;
            k[i] = (byte) Integer.parseInt(s.substring(n, n + 2), 16);
        }

        return SecurityUtils.hashRaw(Utils.encode(getName()), k, null);
    }
}
