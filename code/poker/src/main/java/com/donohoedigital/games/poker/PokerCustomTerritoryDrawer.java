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
 * PokerCustomTerritoryDrawer.java
 *
 * Created on January 1, 2004, 8:04 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import org.apache.log4j.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * @author Doug Donohoe
 */
public class PokerCustomTerritoryDrawer implements CustomTerritoryDrawer
{

    static Logger logger = Logger.getLogger(PokerCustomTerritoryDrawer.class);

    private PokerGame game_;
    private MersenneTwisterFast random_ = new MersenneTwisterFast();

    /**
     * Creates a new instance of PokerCustomTerritoryDrawer
     */
    public PokerCustomTerritoryDrawer(GameContext context)
    {
        game_ = (PokerGame) context.getGame();
    }

    public void drawTerritoryPart(Gameboard board, Graphics2D g, Territory t, GeneralPath path, Rectangle territoryBounds, int iPart)
    {
        PokerGamePiece gp;

        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT_DETAILS))
        {
            //logger.debug("DRAWING PIECES for " + t.getName());
        }

        PokerGamePiece last = null;
        int x_adjust = 0;
        int y_adjust = 0;

        synchronized (t.getMap())
        {
            Iterator iter = t.getGamePieces();
            while (iter.hasNext())
            {
                gp = (PokerGamePiece) iter.next();

                if (last != null)
                {
                    if (!PokerUtils.isFlop(t) && last.getType().intValue() == gp.getType().intValue())
                    {
                        x_adjust += (gp.getXAdjust() * gp.getScale());
                        y_adjust += (gp.getYAdjust() * gp.getScale());
                    }
                    else
                    {
                        x_adjust = 0;
                        y_adjust = 0;
                    }
                }
                last = gp;
                gp.drawPiece(board, g, t, path, territoryBounds, iPart, x_adjust, y_adjust);
            }
        }

        if (PokerUtils.isPot(t) && !game_.isClockMode())
        {
            drawPot(board, g, territoryBounds);
        }
    }

    /**
     * Draw the pot with chips
     */
    private void drawPot(Gameboard board, Graphics2D g, Rectangle territoryBounds)
    {
        // get hand
        PokerTable table = game_.getCurrentTable();
        HoldemHand hhand = table.getHoldemHand();
        if (hhand == null) return;

        // seed random so same for each hand
        random_.setSeed((table.getHandNum() + 1) * 499);

        // loop through history
        List<HandAction> hist = hhand.getHistoryCopy();
        int nAmount;

        for (HandAction action : hist)
        {
            nAmount = action.getAmount();
            if (nAmount == 0) continue;
            if (action.getAction() > HandAction.ACTION_ANTE) continue;

            while (nAmount > 0)
            {
                nAmount = drawAmount(board, nAmount, g, territoryBounds);
            }
        }
    }

    /**
     * Draw biggest chip the amount can handle and return amount
     * less that chip
     */
    private int drawAmount(Gameboard board, int nAmount, Graphics2D g, Rectangle territoryBounds)
    {
        int nValue;
        if (nAmount >= 100000)
        {
            nValue = 100000;
        }
        else if (nAmount >= 50000)
        {
            nValue = 50000;
        }
        else if (nAmount >= 10000)
        {
            nValue = 10000;
        }
        else if (nAmount >= 5000)
        {
            nValue = 5000;
        }
        else if (nAmount >= 1000)
        {
            nValue = 1000;
        }
        else if (nAmount >= 500)
        {
            nValue = 500;
        }
        else if (nAmount >= 100)
        {
            nValue = 100;
        }
        else if (nAmount >= 25)
        {
            nValue = 25;
        }
        else if (nAmount >= 5)
        {
            nValue = 5;
        }
        else
        {
            nValue = 1;
        }

        nAmount -= nValue;

        int gap = (int) board.scaleToCurrentSpace(s * WIDTH);
        int bottomgap = (int) board.scaleToCurrentSpace(27 + (s * WIDTH));
        int width = territoryBounds.width - (gap);
        int height = territoryBounds.height - (bottomgap);
        int x = territoryBounds.x;
        int y = territoryBounds.y;
        Ellipse2D ellipse = new Ellipse2D.Float(x, y, width, height); // fit chips in ellipse

        //border for debugging
//        g.setColor(Color.red);
//        g.drawRect(x, y, width, height);
//        g.setColor(Color.blue);
//        g.draw(ellipse);

        // find next point (loop if outside of ellipse)
        int x1 = 0, y1 = 0;
        boolean bOkay = false;
        while (!bOkay)
        {
            int X = random_.nextInt(RANGE);
            int Y = random_.nextInt(RANGE);
            x1 = x + (width * X) / RANGE;
            y1 = y + (height * Y) / RANGE;
            if (ellipse.contains(x1, y1)) bOkay = true;
        }

        // draw chip
        chip_.setValue(nValue);
        chip_.paintCustom(g, x1, y1,
                          (int) board.scaleToCurrentSpace(s * WIDTH),
                          (int) board.scaleToCurrentSpace(s * WIDTH));

        return nAmount;
    }

    static int WIDTH = 45;
    static PokerChip chip_ = new PokerChip(false);
    static int RANGE = 997;
    static double s = .95d;

}
