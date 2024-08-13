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
 * EngineUtils.java
 *
 * Created on December 28, 2002, 2:22 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author  Doug Donohoe
 */
public class EngineUtils 
{  
    //static Logger logger = Logger.getLogger(EngineUtils.class);
    
    private static JTextComponent msg_;
    protected static Gameboard gameboard_;
    protected static JComponent scroll_;
    
    private static javax.swing.border.Border standardMsgBorder_ = BorderFactory.createEmptyBorder(6,10,5,10);
    private static javax.swing.border.Border standardMenuTextBorder_ = BorderFactory.createEmptyBorder(15,20,15,20);
    private static javax.swing.border.Border standardMenuLowerTextBorder_ = BorderFactory.createEmptyBorder(2,10,2,10);
    
    /**
     * Get standard border around message areas
     */
    public static javax.swing.border.Border getStandardMessageBorder()
    {
        return standardMsgBorder_;
    }

    /**
     * Get standard border around text areas in menu panels
     */
    public static javax.swing.border.Border getStandardMenuTextBorder()
    {
        return standardMenuTextBorder_;
    }
    
    /**
     * Get standard border around text areas at bottom of menu panels
     */
    public static javax.swing.border.Border getStandardMenuLowerTextBorder()
    {
        return standardMenuLowerTextBorder_;
    }
    
    /**
     * Set gameboard
     */
    public static void setGameboard(Gameboard gameboard)
    {
        gameboard_ = gameboard;
    }
    
    /**
     * Get gameboard
     */
    public static Gameboard getGameboard()
    {
        return gameboard_;
    }
    
    /**
     * Set component responsible for displaying & scrolling
     * gameboard
     */
    public static void setScroll(JComponent scroll)
    {
        scroll_ = scroll;
    }
    
    /**
     * Set cursor used on gameboard
     */
    public static void setGameboardCursor(Cursor c)
    {
        if (scroll_ != null)
        {
            scroll_.setCursor(c);
        }
    }
    
    /**
     * Set cursor using invokeLater - used to do things like
     * scrolling where the painting happens before cursor is
     * set (looks more natural)
     */
    public static void setGameboardCursorLater(final Cursor c)
    {
        if (scroll_ == null) return;
        
        SwingUtilities.invokeLater(
                new Runnable() {
                    Cursor _c = c;
                    public void run() {     
                            setGameboardCursor(_c);
                    }
                }
            );
    }
    /**
     * Get cursor used on gameboard
     */
    public static Cursor getGameboardCursor()
    {
        if (scroll_ == null) return null;
        
        return scroll_.getCursor();
    }
    
    /**
     * Display a message
     */
    public static void setMessage(String sMsg)
    {
        if (msg_ != null)
        {
            msg_.setText(sMsg);
        }
    }
    
    /**
     * Set message area
     */
    public static void setMsgArea(JTextComponent msg)
    {
        msg_ = msg;
    }
    
    /**
     * Get message area
     */
    public static JTextComponent getMsgArea()
    {
        return msg_;
    }
    
    /**
     * Show message in a confirmation dialog. Return true if 'yes'
     * pressed, false otherwise.
     */
    public static boolean displayConfirmationDialog(GameContext context,
                                                 String sMsg)
    {
        return displayConfirmationDialog(context, sMsg, null);
    }
    
    /**
     * Same as above, except display "don't show" option if sNoShowKey is non-null
     */
    public static boolean displayConfirmationDialog(GameContext context,
                                                 String sMsg, String sNoShowKey)
    {
        return displayConfirmationDialog(context, sMsg, null, sNoShowKey);
    }
    
    /**
     * Same as above, except display "don't show" option if sNoShowKey is non-null.
     * Title is set to title 
     */
    public static boolean displayConfirmationDialog(GameContext context,
                                                 String sMsg, String sTitleKey, String sNoShowKey)
    {
        return displayConfirmationDialog(context, sMsg, sTitleKey, sNoShowKey, null);
    }
        
    /**
     * Same as above, except display "don't show" option if sNoShowKey is non-null.
     * Title is set to title 
     */
    public static boolean displayConfirmationDialog(GameContext context,
                                                 String sMsg,
                                                 String sTitleKey,
                                                 String sNoShowKey,
                                                 String sNoShowCheckBoxName)
    {
        GameButton buttonpressed = displayConfirmationDialogCustom(context, "DisplayConfirmation",
                        sMsg, sTitleKey, sNoShowKey, sNoShowCheckBoxName);
        
        if (buttonpressed != null && buttonpressed.getName().equals("yes"))
        {
            return true;
        }
        
        return false;
    }

    /**
     * Same as above, except uses DisplayConfirmationCancelable
     */
    public static boolean displayCancelableConfirmationDialog(GameContext context,
                                                              String sMsg,
                                                              String sTitleKey,
                                                              String sNoShowKey,
                                                              String sNoShowCheckBoxName, int nTimeout)
    {
        GameButton buttonpressed = displayConfirmationDialogCustom(context,
                        nTimeout == 0 ? "DisplayConfirmationCancelable" : "DisplayTimedMessage",
                        sMsg, sTitleKey, sNoShowKey, sNoShowCheckBoxName, nTimeout);

        if (buttonpressed != null && buttonpressed.getName().equals("yes"))
        {
            return true;
        }

        return false;
    }

    /**
     * confirmation dialog where button pressed is returned to allow for custom
     * buttons.  Should check for null
     */
    public static GameButton displayConfirmationDialogCustom(GameContext context,
                                                 String sPhase,
                                                 String sMsg, 
                                                 String sTitleKey,
                                                 String sNoShowKey,
                                                 String sNoShowCheckBoxName)
    {
        return displayConfirmationDialogCustom(context, sPhase, sMsg, sTitleKey, sNoShowKey, sNoShowCheckBoxName, 0);
    }
    
    /**
     * confirmation dialog where button pressed is returned to allow for custom
     * buttons.  Should check for null
     */
    private static GameButton displayConfirmationDialogCustom(GameContext context,
                                                 String sPhase,
                                                 String sMsg, 
                                                 String sTitleKey,
                                                 String sNoShowKey,
                                                 String sNoShowCheckBoxName,
                                                 int nTimeoutSeconds) // only used with cancelable timeout
    {

        TypedHashMap params = new TypedHashMap();
        if (sMsg != null) params.setString(DisplayMessage.PARAM_MESSAGE, sMsg);
        if (sTitleKey != null) params.setString(DisplayMessage.PARAM_WINDOW_TITLE_KEY, sTitleKey);
        if (sNoShowKey != null)
        {
            params.setBoolean(DialogPhase.PARAM_NO_SHOW_OPTION, Boolean.TRUE);
            params.setString(DialogPhase.PARAM_NO_SHOW_KEY, sNoShowKey);
        }
        if (sNoShowCheckBoxName != null)
        {
            params.setString(DialogPhase.PARAM_NO_SHOW_NAME, sNoShowCheckBoxName);
        }
        if (nTimeoutSeconds > 0)
        {
            params.setInteger(DisplayTimedMessage.PARAM_SECONDS, new Integer(nTimeoutSeconds));
        }
        Phase confirm = context.processPhaseNow(sPhase, params);

        return (GameButton) confirm.getResult();
    }

    /**
     * Show message in a information dialog
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg)
    {
        displayInformationDialog(context, sMsg, null);
    }

    /**
     * Show message in a information dialog
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg, boolean bModal)
    {
        displayInformationDialog(context, sMsg, null, null, null, bModal);
    }

    /**
     * Same as above, except display "don't show" option if sNoShowKey is non-null
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg, String sNoShowKey)
    {
        displayInformationDialog(context, sMsg, null, sNoShowKey);
    }
    
    /**
     * Show message in a information dialog for current player.  If sNoShowkey
     * is not null, then a "don't show this dialog" option is shown
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg,
                                                 String sTitleKey,
                                                 String sNoShowKey)
    {
        displayInformationDialog(context, sMsg, sTitleKey, sNoShowKey, null);
    }
    
    /**
     * Show message in a information dialog for current player.  If sNoShowkey
     * is not null, then a "don't show this dialog" option is shown
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg,
                                                 String sTitleKey,
                                                 String sNoShowKey,
                                                 String sNoShowCheckBoxName)
    {
        displayInformationDialog(context, sMsg, sTitleKey, sNoShowKey, sNoShowCheckBoxName, true);
    }
    
        /**
     * Show message in a information dialog for current player.  If sNoShowkey
     * is not null, then a "don't show this dialog" option is shown
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg,
                                                 String sTitleKey,
                                                 String sNoShowKey,
                                                 String sNoShowCheckBoxName,
                                                 boolean bModal)
    {
        TypedHashMap params = new TypedHashMap();
        params.setString(DisplayMessage.PARAM_MESSAGE, sMsg);
        if (sTitleKey != null) params.setString(DisplayMessage.PARAM_WINDOW_TITLE_KEY, sTitleKey);
        if (sNoShowKey != null)
        {
            params.setBoolean(DialogPhase.PARAM_NO_SHOW_OPTION, Boolean.TRUE);
            params.setString(DialogPhase.PARAM_NO_SHOW_KEY, sNoShowKey);
        }
        if (sNoShowCheckBoxName != null)
        {
            params.setString(DialogPhase.PARAM_NO_SHOW_NAME, sNoShowCheckBoxName);
        }
        params.setBoolean(DialogPhase.PARAM_MODAL, bModal ? Boolean.TRUE : Boolean.FALSE);
        context.processPhaseNow("DisplayInfoMessage", params);
    }
    
    /**
     * Return true if the given player has a piece of the given type
     * in this territory that is visible (i.e., not currently moving)
     */
    public static boolean hasVisibleOwnerPiece(Territory t, int nType, GamePlayer player)
    {   
        EngineGamePiece gp = (EngineGamePiece) t.getGamePiece(nType, player);
        
        if (gp == null) return false;
        
        if (gp.getDrawingQuantity() > 0) return true;
        
        return false;
    }
    
    /**
     * Return true if the given territory has a piece of the given type
     * (owned by the territory owner), with either visible or hidden
     * quantities (meaning it may have just been purchased)
     */
    public static boolean hasPiece(Territory t, int nType)
    {
        if (t == null) return false;
        
        return t.hasOwnerPiece(nType);
    }
    
    /**
     * Is there a territory owned by the given playerOwner adjacent
     * to this territory which contains a piece of the given type
     * (also owner by the playerOwner) that wasn't just placed
     */
    public static boolean isPreExistingAdjacent(Territory t, GamePlayer playerOwner, int nType)
    {
        Territory[] adjacentTerritories = t.getAdjacentTerritories();
        for (int i = 0; i < adjacentTerritories.length; i++)
        {
            if (adjacentTerritories[i].getGamePlayer() == playerOwner &&
                adjacentTerritories[i].hasOwnerPiece(nType, playerOwner) &&
                adjacentTerritories[i].getGamePiece(nType, playerOwner).getQuantity() > 0) return true;
        }
        return false;
    }
    
    /**
     * Return true if the given territory has a piece of the given type
     * (owned by the territory owner) where the visible quantity is
     * greater than 0 (meaning it existed before purchasing took place)
     */
    public static boolean hasPreExistingPiece(Territory t, int nType)
    {
        if (t == null) return false;
        
        GamePiece piece = t.getGamePiece(nType, t.getGamePlayer());
        if (piece == null) return false;
        
        if (piece.getQuantity() > 0) return true;
        
        return false;
    }
    
    /**
     * Return all pieces of a given type in container.  Should synchronize on container.getMap() around
     * call to this and use of iterator to avoid concurrent modification exceptions and to make
     * sure all pieces you get are still there while you iterate
     */
    public static List<GamePiece> getMatchingPieces(GamePieceContainer container, int nType)
    {
        List<GamePiece> list = new ArrayList<GamePiece>();
        if (container == null) return list;
        GamePiece piece;
        synchronized (container.getMap())
        {
            Iterator iter = container.getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                if (piece.getType().intValue() == nType)
                {
                    list.add(piece);
                }
            }
        }
        return list;
    }
    
    /**
     * Return number of pieces of a given type in container
     */
    public static int getMatchingPiecesCount(GamePieceContainer container, int nType)
    {
        int nCnt = 0;
        GamePiece piece;
        synchronized (container.getMap())
        {
            Iterator iter = container.getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                if (piece.getType().intValue() == nType)
                {
                    nCnt ++;
                }
            }
        }
        return nCnt;
    }
    
    /**
     * Return number of tokens of a given type in container owned by given player
     */
    public static int getNumTokens(GamePieceContainer container, int nType, GamePlayer player)
    {
        GamePiece piece = container.getGamePiece(nType, player);
        if (piece == null) return 0;
        
        return piece.getNumTokens();
    }
    
    // used for online components
    private static DDLabel ledonline_, ledconnect_;
    private static ImageIcon ledgreen_;
    private static ImageIcon ledred_;
    private static ImageIcon ledyellow_;
    private static ImageIcon ledyellowoff_;
    private static String sConnected_;
    private static String sDisconnected_;
    
    /**
     * Load images/labels for status used in games with online components
     */
    public static void initOnline()
    {
        ledgreen_ = ImageConfig.getImageIcon("led-green");
        ledred_ = ImageConfig.getImageIcon("led-red");
        ledyellow_ = ImageConfig.getImageIcon("led-yellow");
        ledyellowoff_ = ImageConfig.getImageIcon("led-yellow-off");
        sConnected_ = PropertyConfig.getMessage("msg.online.ok");
        sDisconnected_ = PropertyConfig.getMessage("msg.online.dis");
    }
    
    /**
     * Set leds
     */
    public static void setLeds(DDLabel online, DDLabel connect)
    {
        ledonline_ = online;
        ledconnect_ = connect;
        
        if (ledonline_ != null) ledonline_.setIcon(ledgreen_);
        if (ledconnect_!= null) ledconnect_.setIcon(ledyellowoff_);
    }
    
    /**
     * Set connect status
     */
    public static void setOnlineStatusLed(boolean bConnected)
    {
        if (ledonline_ == null) return;
        ledonline_.setIcon(bConnected ? ledgreen_ : ledred_);
        ledonline_.setToolTipText(bConnected ? sConnected_ : sDisconnected_);
    }
    
    /**
     * Set connection activity
     */
    public static void setConnectActivity()
    {
        if (ledconnect_ == null) return;
        
        ledconnect_.setIcon(ledyellow_);
        
        // sleep in sep thread before turning led off
        Thread tWait = new Thread(
            new Runnable() {
                public void run() {
                        Utils.sleepMillis(100);
                        // need to invoke later so happens from swing thread
                        SwingUtilities.invokeLater(
                            new Runnable() {
                                public void run() {
                                    if (ledconnect_ != null) ledconnect_.setIcon(ledyellowoff_);
                                }
                            }
                        );
                }
            }
        );
        tWait.start();
    }
    
    private static DDButton chat_;
    
    /**
     * Store chat button
     */
    public static void setChatButton(DDButton chat)
    {
        chat_ = chat;
    }
    
    /**
     * Get chat button
     */
    public static DDButton getChatButton()
    {
        return chat_;
    }
    
    /**
     * play background music start, first then loop
     */
    public static void startBackgroundMusic(GamePhase gamephase)
    {
        startBackgroundMusic(gamephase, true);
    }
    
    /**
     * background audio from gamephase data - loops for audio-start and
     * audio-loop params.
     */
    public static void startBackgroundMusic(GamePhase gamephase, boolean bPlayStart)
    {
        String sStart = gamephase.getString("audio-start");
        String sLoop = gamephase.getString("audio-loop");
        if (sStart != null && !sStart.equals("NONE") && sLoop != null)
        {
            AudioConfig.startBackgroundMusic(sStart, sLoop, bPlayStart);
        }
    }
    
   /**
     * music loop from gamephase data - loops for audio-start and
     * audio-loop params.
     */
    public static AudioPlayer playMusicLoop(GamePhase gamephase)
    {
        String sStart = gamephase.getString("audio-start");
        String sLoop = gamephase.getString("audio-loop");
        if (sStart != null && !sStart.equals("NONE") && sLoop != null)
        {
            return AudioConfig.playMusicLoop(sStart, sLoop);
        }
        return null;
    }

    ///
    /// cancelable phases
    ///

    private static ArrayList cancelables_;

    /**
     * add cancelable phase
     */
    public synchronized static void addCancelable(CancelablePhase phase)
    {
        if (cancelables_ == null) cancelables_ = new ArrayList();

        if (!cancelables_.contains(phase)) cancelables_.add(phase);
    }

    /**
     * remove cancelable phase
     */
    public synchronized static void removeCancelable(CancelablePhase phase)
    {
        if (cancelables_ == null) return;

        cancelables_.remove(phase);
    }

    /**
     * cancel cancelable phases and clear list
     */
    public synchronized static void cancelCancelables()
    {
        if (cancelables_ == null || cancelables_.size() == 0) return;

        // run in swing loop since possible closing dialogs
        GuiUtils.invoke(new Runnable() {
            public void run() {
                EngineUtils.cancel();
            }
        });
    }

    /**
     * cancel each item in the list and clear the list
     */
    private synchronized static void cancel()
    {
        CancelablePhase c;
        ArrayList dup = new ArrayList(cancelables_);
        int nNum = dup.size();
        for (int i = 0; i < nNum; i++)
        {
            c = (CancelablePhase) dup.get(i);
            c.cancelPhase();
        }

        cancelables_.clear();
    }
}
