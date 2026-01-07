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
 * ResultsPiece.java
 *
 * Created on January 9, 2004, 4:16 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  donohoe
 */
public class ResultsPiece extends PokerGamePiece 
{
    public static final int HIDDEN = 0;
    public static final int WIN = 1;
    public static final int LOSE = 2;
    public static final int OVERBET  = 3;
    public static final int FOLD  = 4;
    public static final int ALLIN  = 5;
    public static final int INFO = 6;


    public static int X_ADJUST = 14;
    public static int Y_ADJUST = 50;
    
    public static int getYADJUST(Territory t)
    {
        int nSeat = PokerUtils.getDisplaySeatForTerritory(t);
        if (nSeat == 4)
        {
            return Y_ADJUST + 10;
        }
        return Y_ADJUST;
    }

    public static int getXADJUST(Territory t)
    {
        int nSeat = PokerUtils.getDisplaySeatForTerritory(t);
        if (nSeat == 4)
        {
            return X_ADJUST + 1;
        }
        else if (nSeat == 5)
        {
            return X_ADJUST - 3;
        }
        return X_ADJUST;
    }

    public static final String WIN_IC =    "results-win";
    public static final String LOSE_IC   = "results-lose";
    public static final String OVERBET_IC   = "results-overbet";
    public static final String FOLD_IC   = "results-fold";
    public static final String ALLIN_IC   = "results-allin";
    public static final String INFO_IC = "results-info";

    public static final double SCALE = 1.6d;
    
    private int nResult_ = HIDDEN;
    private String sResultsText_ = "";
    
    
    /** 
     * Creates a new instance of ResultsPiece 
     */
    public ResultsPiece() 
    {
        super(PokerConstants.PIECE_RESULTS, null, CardPiece.POINT_HOLE1, "results");
        setResult(HIDDEN, "");
    }
    
    /**
     * Set result
     */
    public void setResult(int nResult, String sText)
    {
        nResult_ = nResult;
        sResultsText_ = sText;
        if (nResult == HIDDEN)
        {
            setNotDrawn(true);
        }
        else
        {
            setNotDrawn(false);
        }
    }

    /**
     * Get result
     */
    public int getResult()
    {
        return nResult_;
    }

    /**
     * No mouse over detection
     */
    public boolean allowMouseOver()
    {
        return false;
    }
    
    /**
     * Get image based on whether marker is up/down
     */
    public ImageComponent getImageComponent() 
    {
        switch (nResult_)
        {
            case WIN:
                setICName(WIN_IC);
                break;
                
            case LOSE:
                setICName(LOSE_IC);
                break;
                
            case OVERBET:
                setICName(OVERBET_IC);
                break;
                
            case FOLD:
                setICName(FOLD_IC);
                break;
                
            case ALLIN:
                setICName(ALLIN_IC);
                break;

            case INFO:
                setICName(INFO_IC);
                break;
        }
        return super.getImageComponent();
    }
    
    /**
     * fixed scale
     */
    public double getScale()
    {
        return SCALE;
    }
    
    /**
     * override to add x,y adjust to center over 2 cards (since we
     * use the same territory point as cards)
     */
    public void drawPiece(Gameboard board, Graphics2D g, Territory t,
                        GeneralPath path, Rectangle territoryBounds, int iPart,
                        int x_adjust, int y_adjust) 
    {
        updateLabel();

        super.drawPiece(board, g, t, path, territoryBounds, iPart, getXADJUST(t), getYADJUST(t));
    }
    
    /**
     * used to update cooresponding label which holds results text
     */
    private void updateLabel()
    {
        DDText label = PokerGameboard.getTerritoryInfo(getTerritory()).result;
        String sCurrent = label.getText();
        if (!sCurrent.equals(sResultsText_))
        {
            label.setText(sResultsText_);
        }
    }
    
    ////
    //// Save/Load logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state, boolean bAdd)
    {
        GameStateEntry entry = super.addGameStateEntry(state, bAdd);
        entry.addToken(nResult_);
        entry.addToken(sResultsText_);
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        super.loadFromGameStateEntry(state, entry);
        nResult_ = entry.removeIntToken();
        sResultsText_ = entry.removeStringToken();
    }
}
