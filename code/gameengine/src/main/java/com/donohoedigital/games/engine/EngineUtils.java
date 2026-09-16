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
 * EngineUtils.java
 *
 * Created on December 28, 2002, 2:22 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.config.AudioConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.config.GamePhase;
import com.donohoedigital.games.config.GamePiece;
import com.donohoedigital.games.config.GamePieceContainer;
import com.donohoedigital.gui.GuiUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author  Doug Donohoe
 */
public class EngineUtils 
{
    protected static Gameboard gameboard_;
    protected static JComponent scroll_;
    
    private static final javax.swing.border.Border standardMsgBorder_ = BorderFactory.createEmptyBorder(6,10,5,10);
    private static final javax.swing.border.Border standardMenuTextBorder_ = BorderFactory.createEmptyBorder(15,20,15,20);
    private static final javax.swing.border.Border standardMenuLowerTextBorder_ = BorderFactory.createEmptyBorder(2,10,2,10);
    
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

        return buttonpressed != null && buttonpressed.getName().equals("yes");
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

        return buttonpressed != null && buttonpressed.getName().equals("yes");
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
            params.setInteger(DisplayTimedMessage.PARAM_SECONDS, nTimeoutSeconds);
        }
        Phase confirm = context.processPhaseNow(sPhase, params);

        return (GameButton) confirm.getResult();
    }

    /**
     * Message for the unexpected-error dialog.  Uses the exception's message, or its
     * class name if it has none (e.g., an NPE).  Full details are in the log.
     */
    public static String getUnexpectedErrorMessage(Throwable e)
    {
        String sDetail = e.getMessage();
        if (sDetail == null || sDetail.isBlank()) sDetail = e.getClass().getSimpleName();
        return PropertyConfig.getMessage("msg.error.unexpected", sDetail);
    }

    /**
     * Show message in an information dialog
     */
    public static void displayInformationDialog(GameContext context,
                                                 String sMsg)
    {
        displayInformationDialog(context, sMsg, null);
    }

    /**
     * Show message in an information dialog
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
     * Show message in an information dialog for current player.  If sNoShowkey
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
     * Show message in an information dialog for current player.  If sNoShowkey
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
     * Show message in an information dialog for current player.  If sNoShowkey
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
     * Return all pieces of a given type in container.  Should synchronize on container.getMap() around
     * call to this and use of iterator to avoid concurrent modification exceptions and to make
     * sure all pieces you get are still there while you iterate
     */
    public static List<GamePiece> getMatchingPieces(GamePieceContainer container, int nType)
    {
        List<GamePiece> list = new ArrayList<>();
        if (container == null) return list;
        GamePiece piece;
        synchronized (container.getMap())
        {
            Iterator iter = container.getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                if (piece.getType() == nType)
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
                if (piece.getType() == nType)
                {
                    nCnt ++;
                }
            }
        }
        return nCnt;
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

    //
    // cancelable phases
    //

    private static ArrayList<CancelablePhase> cancelables_;

    /**
     * add cancelable phase
     */
    public static synchronized void addCancelable(CancelablePhase phase)
    {
        if (cancelables_ == null) cancelables_ = new ArrayList<>();

        if (!cancelables_.contains(phase)) cancelables_.add(phase);
    }

    /**
     * remove cancelable phase
     */
    public static synchronized void removeCancelable(CancelablePhase phase)
    {
        if (cancelables_ == null) return;

        cancelables_.remove(phase);
    }

    /**
     * cancel cancelable phases and clear list
     */
    public static synchronized void cancelCancelables()
    {
        if (cancelables_ == null || cancelables_.isEmpty()) return;

        // run in swing loop since possible closing dialogs
        GuiUtils.invoke(EngineUtils::cancel);
    }

    /**
     * cancel each item in the list and clear the list
     */
    private static synchronized void cancel()
    {
        CancelablePhase c;
        ArrayList<CancelablePhase> dup = new ArrayList<>(cancelables_);
        for (CancelablePhase cancelablePhase : dup) {
            c = cancelablePhase;
            c.cancelPhase();
        }

        cancelables_.clear();
    }
}
