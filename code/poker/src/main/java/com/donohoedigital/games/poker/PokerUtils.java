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
/*
 * PokerUtils.java
 *
 * Created on December 7, 2003, 8:45 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.math.*;
import java.util.*;
import java.util.List;

/**
 * @author Doug Donohoe
 */
public class PokerUtils extends EngineUtils
{
    private static final Logger logger = LogManager.getLogger(PokerUtils.class);

    private static final BigInteger factorial_[] = new BigInteger[53];

    static Territory tPot_ = null;
    static Territory tFlop_ = null;

    static
    {
        factorial_[0] = new BigInteger("1");

        for (int i = 1; i < 53; ++i)
        {
            factorial_[i] = factorial_[i - 1].multiply(new BigInteger(Integer.toString(i)));
        }
    }

    /**
     * Set gameboard
     */
    public static void setPokerGameboard(Gameboard gameboard)
    {
        if (gameboard_ != null)
        {
            gameboard_.setTerritoryDisplayListener(null);
            gameboard_.setCustomTerritoryDrawer(null);
        }
        gameboard_ = gameboard;

        if (gameboard_ != null)
        {
            gameboard_.setTerritoryDisplayListener(new PokerDisplayAdapter());
            gameboard_.setCustomTerritoryDrawer(new PokerCustomTerritoryDrawer(gameboard_.getGameContext()));
            tPot_ = gameboard_.getGameboardConfig().getTerritories().getTerritory("Pot");
            ApplicationError.assertNotNull(tPot_, "No 'Pot' territory");
            tFlop_ = gameboard_.getGameboardConfig().getTerritories().getTerritory("Flop");
            ApplicationError.assertNotNull(tPot_, "No 'Flop' territory");
        }
    }

    /**
     * return poker gameboard
     */
    public static PokerGameboard getPokerGameboard()
    {
        return (PokerGameboard) gameboard_;
    }

    // territories
    private static Territories ts_ = null;

    /**
     * Get territories array
     */
    public static synchronized Territories getTerritories()
    {
        if (ts_ == null)
        {
            ts_ = GameEngine.getGameEngine().getGameboardConfig().getTerritories(); // TODO: need to chagne for multi-game
        }
        return ts_;
    }

    // chat
    private static ChatPanel gameChat_ = null;

    public static void setChat(ChatPanel chat)
    {
        gameChat_ = chat;
    }

    public static void updateChat()
    {
        if (gameChat_ != null) gameChat_.updatePrefs();
    }

    /**
     * Get territory associated with the given seat on the
     * poker table display
     */
    public static Territory getTerritoryForDisplaySeat(int nSeat)
    {
        String sName = "Seat " + (nSeat + 1);
        return getTerritories().getTerritory(sName);
    }

    /**
     * Get territory associated with the given seat
     */
    public static Territory getTerritoryForTableSeat(PokerTable table, int nSeat)
    {
        return getTerritoryForDisplaySeat(table.getDisplaySeat(nSeat));
    }

    /**
     * Get seat associated with the given territory
     */
    public static int getDisplaySeatForTerritory(Territory t)
    {
        try
        {
            String sNum = t.getName().substring(5); // eliminate "Seat "
            int nNum = Integer.parseInt(sNum);
            return nNum - 1;
        }
        catch (Exception ignored)
        {
            return -1;
        }
    }

    /**
     * Get seat associated with the given territory
     */
    public static int getTableSeatForTerritory(PokerTable table, Territory t)
    {
        int nDisplaySeat = getDisplaySeatForTerritory(t);
        if (nDisplaySeat == -1) return -1;
        return table.getTableSeat(nDisplaySeat);
    }

    /**
     * Get player at seat assoc with territory
     */
    public static PokerPlayer getPokerPlayer(GameContext context, Territory t)
    {
        if (t == null) return null;
        PokerGame game = (PokerGame) context.getGame();
        PokerTable table = game.getCurrentTable();
        int nDisplaySeat = t.getUserInt();
        if (nDisplaySeat == -1) return null;

        return table.getPlayer(table.getTableSeat(nDisplaySeat));
    }

    /**
     * Init user int as seat number for each territory
     */
    public static void initTerritories()
    {
        // init all territories to -1
        Territory[] ts = Territory.getTerritoryArrayCached();
        for (Territory t : ts)
        {
            t.setUserInt(-1);
        }

        // init seat territories to seat num
        Territory t;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            t = getTerritoryForDisplaySeat(i);
            t.setUserInt(i);
        }
    }

    /**
     * return true if territory is a seat
     */
    public static boolean isSeat(Territory t)
    {
        return t.getUserInt() != -1;
    }

    /**
     * Is this the pot territory?
     */
    public static boolean isPot(Territory t)
    {
        return t == tPot_;
    }

    /**
     * Repaint pot
     */
    public static void repaintPot()
    {
        if (tPot_ != null && gameboard_ != null)
        {
            gameboard_.repaintTerritory(tPot_);
        }
    }

    /**
     * Return flop
     */
    public static Territory getFlop()
    {
        return tFlop_;
    }


    /**
     * Is this the flop territory
     */
    public static boolean isFlop(Territory t)
    {
        return t == tFlop_;
    }

    /**
     * Return true if territory matches current player
     */
    public static boolean isCurrent(GameContext context, Territory t)
    {
        PokerGame game = (PokerGame) context.getGame();
        if (game != null)
        {
            if (game.isClockMode()) return false;

            PokerTable table = game.getCurrentTable();
            if (table == null) return false;

            HoldemHand hhand = table.getHoldemHand();
            if (hhand == null) return false;

            PokerPlayer current = hhand.getCurrentPlayer();
            if (current != null && t.getUserInt() == table.getDisplaySeat(current.getSeat()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean bFold_ = false;
    private static boolean bAcceptFold_ = false;

    /**
     * Start new hand
     */
    public static void setNewHand()
    {
        bFold_ = false;
        bAcceptFold_ = true;
    }

    /**
     * stop accepting fold
     */
    public static void setNoFoldKey()
    {
        bAcceptFold_ = false;
    }

    /**
     * remember advance folds if always allow fold on
     *
     * @param context
     */
    public static void setFoldKey(GameContext context)
    {
        if (!bAcceptFold_) return;

        GameEngine engine = GameEngine.getGameEngine();
        boolean bFoldCheck = context.getGame() != null && !context.getGame().isOnlineGame() &&
                             isOptionOn(PokerConstants.OPTION_CHECKFOLD);
        if (bFoldCheck)
        {
            bFold_ = true;
        }
    }

    /**
     * Was fold key hit?
     */
    public static boolean isFoldKey()
    {
        return bFold_;
    }

    /**
     * clear cards from all territories
     */
    public static void clearCards(boolean repaint)
    {
        clearPiece(PokerConstants.PIECE_CARD, repaint);
    }

    /**
     * clear results from all territories
     */
    public static void clearResults(GameContext context, boolean repaint)
    {
        PokerGame game = (PokerGame) context.getGame();

        PokerPlayer p;
        ResultsPiece piece;
        Territory[] ts = Territory.getTerritoryArrayCached();
        for (Territory t : ts)
        {
            piece = (ResultsPiece) t.getGamePiece(PokerConstants.PIECE_RESULTS, null);
            if (piece != null)
            {
                // online game - always need to check disconnected status
                p = getPokerPlayer(context, t);
                if (game.isOnlineGame() && p != null && p.isHuman())
                {
                    setConnectionStatus(context, p, true, repaint);
                }
                else if (piece.getResult() != ResultsPiece.HIDDEN)
                {
                    piece.setResult(ResultsPiece.HIDDEN, "");
                }
            }
        }

        if (repaint) getPokerGameboard().repaintAll();
    }

    /**
     * disconnected/sitting out player
     */
    public static void setConnectionStatus(GameContext context, PokerPlayer p, boolean bClearIfConnected)
    {
        setConnectionStatus(context, p, bClearIfConnected, true);
    }

    /**
     * disconnected/sitting out player logic with repaint flag
     */
    private static void setConnectionStatus(GameContext context, PokerPlayer p, boolean bClearIfConnected, boolean repaint)
    {
        PokerGame game = (PokerGame) context.getGame();
        PokerTable table = game.getCurrentTable();
        if (table == null || table != p.getTable()) return;

        boolean changed = false;

        Territory t = getTerritoryForTableSeat(table, p.getSeat());
        ResultsPiece piece = (ResultsPiece) t.getGamePiece(PokerConstants.PIECE_RESULTS, null);
        if (piece != null)
        {
            if (!p.isComputer() && (p.isDisconnected() || p.isSittingOut()))
            {
                piece.setResult(ResultsPiece.INFO, PropertyConfig.getMessage(p.isDisconnected() ?
                                                                             "msg.disconnected" : "msg.sittingout"));
                changed = true;
            }
            else
            {
                // only set to hidden if currently showing info (we don't want to remove hand results)
                // unless explicitly told to clear results
                if (piece.getResult() == ResultsPiece.INFO || bClearIfConnected)
                {
                    piece.setResult(ResultsPiece.HIDDEN, "");
                    changed = true;
                }
            }
        }

        if (changed && repaint) getPokerGameboard().repaintTerritory(t);
    }

    /**
     * Clear given piece type
     */
    private static void clearPiece(int nType, boolean repaint)
    {
        List<GamePiece> pieces;
        Territory[] ts = Territory.getTerritoryArrayCached();
        for (Territory t : ts)
        {
            synchronized (t.getMap())
            {
                pieces = EngineUtils.getMatchingPieces(t, nType);
                for (GamePiece piece : pieces)
                {
                    t.removeGamePiece(piece);
                }
            }
        }

        if (repaint) getPokerGameboard().repaintAll();
    }

    /**
     * turn all cards up - return true if changed a card
     */
    public static void showCards(PokerPlayer player, boolean bUp)
    {
        Territory t = getTerritoryForTableSeat(player.getTable(), player.getSeat());
        List<GamePiece> cards;
        CardPiece card;

        synchronized (t.getMap())
        {
            cards = EngineUtils.getMatchingPieces(t, PokerConstants.PIECE_CARD);
            for (GamePiece piece : cards)
            {
                card = (CardPiece) piece;
                if (card.isUp() != bUp)
                {
                    card.setUp(bUp);
                }
            }
        }
    }

    /**
     * Show dialog about what ai rebought/add-on
     */
    public static void showComputerBuys(GameContext context, PokerGame game, List<PokerPlayer> who, String sType)
    {
        if (who == null || who.isEmpty()) return;
        if (TESTING(PokerConstants.TESTING_AUTOPILOT_INIT)) return;
        Collections.sort(who, PokerPlayer.SORTBYNAME); // sort by name
        StringBuilder sb = new StringBuilder();
        PokerPlayer p;
        int nNum = who.size();
        for (int i = 0; i < nNum; i++)
        {
            p = who.get(i);
            sb.append(Utils.encodeHTML(p.getName()));

            if (game.isOnlineGame())
            {
                if (i < (nNum - 1)) sb.append(", ");
            }
            else
            {
                sb.append("<BR>");
            }
        }

        if (game.isOnlineGame())
        {
            TournamentDirector td = (TournamentDirector) context.getGameManager();
            td.sendDealerChatLocal(PokerConstants.CHAT_1, chatInformation(PropertyConfig.getMessage("chat." + sType + ".ai", sb.toString())));
        }
        else
        {
            EngineUtils.displayInformationDialog(context, PropertyConfig.getMessage("msg." + sType + ".ai", sb.toString()),
                                                 "msg.windowtitle." + sType, "ai" + sType, "noshowai" + sType);
        }

        // i forget why this is necessary...but leaving in
        getPokerGameboard().repaintVisible(false);
    }

    /////
    ///// Update display fields
    /////

    static Color fgNormal = StylesConfig.getColor("clock.fg");
    static Color bgNormal = StylesConfig.getColor("clock.bg");
    static Color fgFlash = StylesConfig.getColor("clock.fg.flash");
    static Color bgFlash = StylesConfig.getColor("clock.bg.flash");

    public static String getChipIcon(int nAmount)
    {
        String sIcon = "icon-blank";
        if (nAmount == 100000)
        {
            sIcon = "chip100k";
        }
        else if (nAmount == 50000)
        {
            sIcon = "chip50k";
        }
        else if (nAmount == 10000)
        {
            sIcon = "chip10k";
        }
        else if (nAmount == 5000)
        {
            sIcon = "chip5k";
        }
        else if (nAmount == 1000)
        {
            sIcon = "chip1k";
        }
        else if (nAmount == 500)
        {
            sIcon = "chip500";
        }
        else if (nAmount == 100)
        {
            sIcon = "chip100";
        }
        else if (nAmount == 25)
        {
            sIcon = "chip25";
        }
        else if (nAmount == 5)
        {
            sIcon = "chip5";
        }
        else if (nAmount == 1)
        {
            sIcon = "chip1";
        }
        return sIcon;
    }

    //////
    ////// AUDIO
    //////

    private static int lastAudio_ = -1;

    /**
     * audio - bet
     */
    public static void betAudio()
    {
        int n = lastAudio_;
        while (n == lastAudio_)
        {
            n = DiceRoller.rollDieInt(10);
        }
        lastAudio_ = n;
        AudioConfig.playFX("bet" + n, 0);
    }

    /**
     * audio - check
     */
    public static void checkAudio()
    {
        AudioConfig.playFX("check", 0);
    }

    /**
     * audio - raise
     */
    public static void raiseAudio()
    {
        // no check audio for now
        AudioConfig.playFX("raise", 0);
    }

    /**
     * audio - cheer
     */
    public static void cheerAudio()
    {
        int nNum = DiceRoller.rollDieInt(4);
        AudioConfig.playFX("cheers" + nNum);
    }

    /////
    ///// MISC
    /////

    public static int pow(int n, int p)
    {
        int res = 1;
        while (p-- > 0)
            res *= n;
        return res;
    }

    public static String getTimeString(PokerGame game)
    {
        int nSeconds = game.getGameClock().getSecondsRemaining();
        int nHours = nSeconds / (60 * 60);
        int nMinutes = nSeconds % (60 * 60);
        int nSecs = nMinutes;
        nMinutes /= 60;
        nSecs %= 60;

        return PropertyConfig.getMessage(
                nHours > 0 ? "msg.time.hour" : (nMinutes > 0) ? "msg.time.min" : "msg.time.sec",
                nHours,
                (nHours > 0 ? PokerConstants.fTimeNum2.form(nMinutes) :
                 PokerConstants.fTimeNum1.form(nMinutes)),
                PokerConstants.fTimeNum2.form(nSecs));
    }

    /**
     * display time in given label, return text string of time
     */
    public static String updateTime(PokerGame game, DDText label)
    {
        PokerTable table = game.getCurrentTable();
        if (table != null && table.isZipMode()) return null;

        int nSeconds = game.getGameClock().getSecondsRemaining();
        int nHours = nSeconds / (60 * 60);
        int nMinutes = nSeconds % (60 * 60);
        nMinutes /= 60;
        String sText = getTimeString(game);

        if (label != null)
        {
            if (game.getGameClock().isFlash())
            {
                boolean bNormal = true;
                if (nHours == 0 && nMinutes == 0 && nSeconds <= 10)
                {
                    bNormal = (nSeconds % 2) == 0;
                }

                Color fgNow = label.getForeground();
                Color bgNow = label.getBackground();

                if (bNormal)
                {
                    if (fgNow != fgNormal)
                    {
                        label.setForeground(fgNormal);
                    }
                    if (bgNow != bgNormal)
                    {
                        label.setBackground(bgNormal);
                    }
                }
                else
                {
                    if (fgNow != fgFlash)
                    {
                        label.setForeground(fgFlash);
                    }
                    if (bgNow != bgFlash)
                    {
                        label.setBackground(bgFlash);
                    }
                }
            }
            label.setText(sText);
        }

        return sText;
    }

    /**
     * Round chips to be a multiple of the min chip on the table
     */
    public static int roundAmountMinChip(PokerTable table, int chips)
    {
        int nNewAmount = chips;
        int nMinChip = table.getMinChip();
        int nOdd = chips % nMinChip;
        if (nOdd != 0)
        {
            nNewAmount = chips - nOdd;
            if ((float) nOdd >= (nMinChip / 2.0f))
            {
                nNewAmount += nMinChip;
            }
        }

        return nNewAmount;
    }

    /**
     * Is cheat option on?  If there is an online game
     * going on, always return false;
     */
    /**
     * Is cheat option on, specify default
     */
    public static boolean isCheatOn(GameContext context, String sName)
    {
        // check if online game, if so no cheat options
        if (context != null)
        {
            PokerGame game = (PokerGame) context.getGame();
            if (game != null && game.isOnlineGame() && !TESTING(PokerConstants.TESTING_ALLOW_CHEAT_ONLINE))
                return false;
        }

        return isOptionOn(sName);
    }

    /**
     * Is option on?, default in client.properties
     */
    public static boolean isOptionOn(String sName)
    {
        GameEngine engine = GameEngine.getGameEngine();
        return engine.getPrefsNode().getBooleanOption(sName);
    }

    /**
     * Get string option, default in client.properties
     */
    public static String getStringOption(String sName)
    {
        GameEngine engine = GameEngine.getGameEngine();
        return engine.getPrefsNode().getStringOption(sName);
    }

    /**
     * Get int option, default in client.properties
     */
    public static int getIntOption(String sName)
    {
        GameEngine engine = GameEngine.getGameEngine();
        return engine.getPrefsNode().getIntOption(sName);
    }

    /**
     * Get int preference, specify default
     */
    public static int getIntPref(String sName, int nDefault)
    {
        GameEngine engine = GameEngine.getGameEngine();
        return engine.getPrefsNode().getInt(sName, nDefault);
    }

    public static final int DEMO_LIMIT = 30;
    public static final int DEMO_LIMIT_ONLINE = 15;

    /**
     * is game done for this player in demo?
     */
    public static boolean isDemoOver(GameContext context, PokerPlayer player, boolean bDuringHand)
    {
        boolean bOnline = false;
        PokerGame game = (PokerGame) context.getGame();
        if (game != null) bOnline = game.isOnlineGame();
        int nLimit = TESTING(EngineConstants.TESTING_DEMO) ? 2 :
                     (bOnline ? DEMO_LIMIT_ONLINE : DEMO_LIMIT);
        if (bDuringHand) nLimit++;
        return (player.isHuman() && player.isDemo() && player.getHandsPlayed() >= nLimit);
    }

    /**
     * Format as important dealer message
     */
    public static String chatImportant(String s)
    {
        return PropertyConfig.getMessage("msg.chat.important", s);
    }

    /**
     * Format as important dealer message
     */
    public static String chatInformation(String s)
    {
        return PropertyConfig.getMessage("msg.chat.information", s);
    }

    /**
     * Get pauser
     */
    public static TournamentDirectorPauser TDPAUSER(GameContext context)
    {
        return new TournamentDirectorPauser(context);
    }

    /**
     * @param n Number of items to choose from.
     * @param k Number of choices.
     * @return Non-ordered combinations possible.
     */
    public static long nChooseK(int n, int k)
    {
        return factorial_[n].divide(factorial_[k]).divide(factorial_[n - k]).longValue();
    }


    /**
     * Do a screen shot
     */
    public static void doScreenShot(GameContext context)
    {
        String suggestedName = "ddpoker.jpg";
        PokerGame game = (PokerGame) context.getGame();
        if (game != null)
        {
            StringBuilder sb = new StringBuilder();
            if (game.getProfile() != null)
            {
                sb.append(game.getProfile().getName());
            }

            if (game.getCurrentTable() != null && game.getCurrentTable().getHandNum() > 0)
            {
                sb.append("-Table").append(game.getCurrentTable().getNumber());
                sb.append("-Hand").append(game.getCurrentTable().getHandNum());
            }

            if (sb.length() > 0)
            {
                sb.insert(0, "ddpoker-");
                suggestedName = sb.toString().replaceAll("[^\\w_-]*", "");
            }

        }

        // get scale to max from prefs
        int maxWidth = PokerUtils.getIntOption(PokerConstants.OPTION_SCREENSHOT_MAX_WIDTH);
        int maxHeight = PokerUtils.getIntOption(PokerConstants.OPTION_SCREENSHOT_MAX_HEIGHT);
        BufferedImage image = GuiUtils.printToImage(context.getRootComponent(), maxWidth, maxHeight);
        AudioConfig.playFX("camera");

        TypedHashMap params = new TypedHashMap();
        params.setString(FileChooserDialog.PARAM_SUGGESTED_NAME, suggestedName);
        params.setObject(FileChooserDialog.PARAM_TOP_LABEL_PARAMS, new Object[]{maxWidth, maxHeight});
        Phase choose = context.processPhaseNow("SaveScreenShot", params);
        Object oResult = choose.getResult();
        if (oResult != null && oResult instanceof File)
        {
            File file = (File) oResult;
            logger.info("Exporting screenshot to " + file.getAbsolutePath());
            GuiUtils.printImageToFile(image, file);
        }
    }
}
