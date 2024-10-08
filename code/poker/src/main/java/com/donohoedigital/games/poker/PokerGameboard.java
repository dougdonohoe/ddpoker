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
 * PokerGameboard.java
 *
 * Created on January 4, 2004, 8:41 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerGameboard extends Gameboard
{
    static Logger logger = LogManager.getLogger(PokerGameboard.class);

    private static final GeneralPath FELT = GuiUtils.drawSVGpath("M180,0c90,1,760,1,850,0c90,1,180,184,180,443.5c0,260.5-90,442.5-180,442.5s-760,0-850,0S0,704,0,443.5C0,184,90,1,180,0z", false);
    private static Rectangle FBOUNDS = FELT.getBounds();

    private PokerGame game_;
    protected int nSmallWidth_, nSmallHeight_;
    protected int nStartingWidth_, nStartingHeight_;

    private PokerGameboardDelegate delegate_;

    // default - green felt
    private Color top_ = new Color(38,175,23);
    private Color bottom_ = new Color(20,82,1);

    /**
     * Get delegate
     */
    public PokerGameboardDelegate getDelegate()
    {
        return delegate_;
    }

    /**
     * Create new scrollgameboard
     */
    public PokerGameboard(GameEngine engine, GameContext context, GameboardConfig gameconfig,
                          int nSmallWidth, int nSmallHeight,
                          int nStartingWidth, int nStartingHeight,
                          PokerGameboardDelegate delegate, ImageComponent tile)
    {
        super(engine, context, gameconfig, true, null, false);
        setBuffer(true);
        setParentTile(tile);
        game_ = (PokerGame) context.getGame();
        nSmallWidth_ = nSmallWidth;
        nSmallHeight_ = nSmallHeight;
        nStartingHeight_ = nStartingHeight;
        nStartingWidth_ = nStartingWidth;
        delegate_ = delegate;
        initGameboard();
        updateFelt(false);
    }

    private void initGameboard()
    {                
        // set initial resize sizes
        setupSize(nStartingWidth_, nStartingHeight_);
    }
    
    @Override
    public void cleanup()
    {
        super.cleanup();
    }
    
    /**
     * Request focus - send to bet widget if it is enabled
     */
    @Override
    public void requestFocus()
    {
        if (delegate_ != null && delegate_.processRequestFocus())
        {
            return;
        }

        super.requestFocus();
    }

    /**
     * Request focus directly here, ignoring delegate
     */
    public void requestFocusDirect()
    {
        super.requestFocus();
    }

    //////
    ////// Customizations
    //////

    /**
     * update felt colors
     */
    public void updateFelt(boolean bRepaint)
    {
        if (!isUseImage()) return;

        TableDesign td = TableDesignManager.getDefaultProfile();
        if (td != null)
        {
            boolean bChanged = !(td.getColorTop().equals(top_) && td.getColorBottom().equals(bottom_));
            if (bChanged)
            {
                top_ = td.getColorTop();
                bottom_ = td.getColorBottom();
                if (bRepaint)
                {
                    setRefreshBuffer(true);
                    repaint();
                }
            }
        }
    }

    /**
     * After tiling, but before image is overlaid, draw felt
     */
    @Override
    protected void paintParentTile(Graphics2D g)
    {
        super.paintParentTile(g);
        if (isUseImage())
        {
            drawFelt(g, getWidth(), getHeight(), top_, bottom_);
        }
    }

    /**
     * draw felt
     */
    public static void drawFelt(Graphics2D g, int width, int height, Color from, Color to)
    {
        // current settings
        Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform txOld = g.getTransform();

        // 1230x955 taken from photoshop image
        g.scale(width/1230.0d, height/955.0d);

        // starting coordinates 10,60 is taken from photoshop
        // image, need to do this since illustrator adjusts
        // path to 0,0
        g.translate(10,60);

        // gradient - needs to be based on size of original path
        GradientPaint gp = new GradientPaint(0, 0, from, 0, FBOUNDS.height, to);
        g.setPaint(gp);
        g.fill(FELT);

        // reset
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
        g.setTransform(txOld);
    }

    /**
     * Get whether labels are antialiased
     */
    @Override
    protected boolean getTerritoryLabelAntiAliased(Territory t)
    {
        return true;
    }
    
    /**
     * Get line spacing for territory labels
     */
    @Override
    protected float getTerritoryDisplayLineSpacing()
    {
        return 0.18f;
    }

    /**
     * Terrtory names - pot/player
     */
    @Override
    protected String getTerritoryDisplay(Territory t)
    {
        if (game_.isClockMode()) return "";
        
        if (PokerUtils.isPot(t))
        {
            String sMsg = "";

            PokerTable table = game_.getCurrentTable();
            HoldemHand hhand = table.getHoldemHand();

            if (hhand != null)
            {
                sMsg = PropertyConfig.getMessage("msg.pot", hhand.getTotalPotChipCount());
            }

            return sMsg;
        }
        
        if (!PokerUtils.isSeat(t)) return super.getTerritoryDisplay(t);
        
        PokerPlayer player = PokerUtils.getPokerPlayer(context_, t);
        if (player == null)
        {
            return "";
        }
        else
        {
            return PropertyConfig.getMessage("msg.playerinfo", player.getDisplayName(game_.isOnlineGame(), false),
                                             player.getChipCount());
        }
    }

    /**
     * repaint bet label too
     */
    @Override
    public void repaintTerritory(Territory t, boolean bImmediate)
    {
        PokerTable table = game_.getCurrentTable();
        if (table != null && table.isZipMode()) return;

        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) logger.debug("************** PokerGameboard REPAINT " + t.getName() + ' ' + bImmediate
                   // + " " + Utils.formatExceptionText(new Throwable())
                    );

        TerritoryInfo info = getTerritoryInfo(t);
        if (info != null && !game_.isClockMode() && PokerUtils.isSeat(t) && info.bet != null)
        {
            boolean bFold = false;
            String sIcon = "icon-blank";
            String sText = "";
            PokerPlayer player = table.getPlayer(PokerUtils.getTableSeatForTerritory(table, t));
            Color color = fireGetTerritoryLabelColor(t);
            int nChipAmount = 0;
            if (player != null)
            {
                HoldemHand hhand = table.getHoldemHand();
                
                // coloring up mode
                if (table.isColoringUpDisplay())
                {
                    color = PokerDisplayAdapter.cCurrent_;
                    int nAmount = 0;
                    if (player.isWonChipRace())
                    {
                        sText = PropertyConfig.getMessage(player.isBrokeChipRace() ? "msg.oddchips.broke":"msg.oddchips.won");
                        nAmount = table.getNextMinChip();
                    }
                    else
                    {
                        int nOdd = player.getOddChips();                    
                        sText = PropertyConfig.getMessage(nOdd == 0 ? "msg.oddchips.none" : 
                                (nOdd == 1 ? "msg.oddchips.sing":"msg.oddchips.plur"),
                                                          nOdd);
                        if (nOdd > 0) nAmount = table.getMinChip();
                    }
                    
                    nChipAmount = nAmount;
                }                                
                else if (hhand != null)
                {
                    int nBet = hhand.getBet(player);
                    int nLast = hhand.getLastAction(player);

                    if (TournamentDirector.DEBUG_EVENT_DISPLAY || TESTING(EngineConstants.TESTING_DEBUG_REPAINT))
                    {
                        logger.debug("repaintTerritory (with hhand) for " + player.getName() +
                                 " round: " + HoldemHand.getRoundName(hhand.getRound()) +
                                 " current: " + player.isCurrentGamePlayer());
                    }

                    if (nBet > 0 || player.isCurrentGamePlayer())
                    {
                        String NBET = PropertyConfig.getAmount(nBet);

                        if (player.isFolded())
                        {
                            sText = PropertyConfig.getMessage("msg.fold");
                            bFold = true;
                        }
                        else if (player.isAllIn())
                        {
                            sText = PropertyConfig.getMessage("msg.bet.allin", NBET);
                            sIcon = "icon-allin";
                        }
                        else if (player.isCurrentGamePlayer())
                        {
                            int nCall = hhand.getCall(player);
                            // something to call
                            if (nCall > 0)
                            {
                                sText = PropertyConfig.getMessage(nBet == 0 ? "msg.bet.call" : "msg.bet.call2", NBET, PropertyConfig.getAmount(nCall));
                            }
                            // nothing to call, but already bet means option on big blind (or small if small==big)
                            else if (nLast == HandAction.ACTION_BLIND_BIG || nLast == HandAction.ACTION_BLIND_SM)
                            {
                                sText = PropertyConfig.getMessage("msg.bet.option", NBET);
                            }
                            sIcon = "icon-act";
                        }
                        else
                        {
                            sText = PropertyConfig.getMessage("msg.bet", NBET);
                            switch (nLast)
                            {
                                case HandAction.ACTION_CALL:
                                    sIcon = "icon-call";
                                    break;

                                case HandAction.ACTION_RAISE:
                                    int nPrior = hhand.getNumPriorRaises(player);
                                    if (nPrior >= 2)
                                    {
                                        sIcon = "icon-rereraise";
                                    }
                                    else if (nPrior == 1)
                                    {
                                        sIcon = "icon-reraise";
                                    }
                                    else
                                    {
                                        sIcon = "icon-raise";
                                    }
                                    break;

                                case HandAction.ACTION_CHECK:
                                case HandAction.ACTION_CHECK_RAISE:
                                    sText = PropertyConfig.getMessage("msg.check");
                                    sIcon = "icon-check";
                                    break;

                                case HandAction.ACTION_BET:
                                    sIcon = "icon-bet";
                                    break;

                                case HandAction.ACTION_BLIND_BIG:
                                    sIcon = "icon-bet";
                                    sText = PropertyConfig.getMessage("msg.bet.big", NBET);
                                    break;

                                case HandAction.ACTION_BLIND_SM:
                                    sIcon = "icon-small";
                                    sText = PropertyConfig.getMessage("msg.bet.small", NBET);
                                    break;

                                default:
                                    ApplicationError.assertTrue(false, "Unhandled last action: " + nLast);
                            }


                        }
                    }
                    // no bet, not current player, but player acted
                    else if (hhand.hasPlayerActed(player))
                    {
                        if (hhand.isFolded(player))
                        {
                            sText = PropertyConfig.getMessage("msg.fold");
                            bFold = true;
                        }
                        else
                        {
                            sText = PropertyConfig.getMessage("msg.check");
                            sIcon = "icon-check";
                        }
                    }
                    // no bet, not current player, player not acted but all in
                    else if (player.isAllIn() && hhand.getRound() < HoldemHand.ROUND_SHOWDOWN)
                    {
                        if (player.getAllInPerc() == null)
                        {
                            sText = PropertyConfig.getMessage("msg.allin");
                            sIcon = "icon-allin";
                        }
                    }
                }
            }
            info.bet.setText(sText);
            info.bet.setForeground(color);
            
            if (TournamentDirector.DEBUG_EVENT_DISPLAY || TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) {
                logger.debug(player == null ? "null":player.getName() + " text: " + sText + ", icon: " + sIcon);
            }
            
            if (bFold)
            {
                info.icon.setHidden(true);
                info.iconfold.setHidden(false);
            }
            else
            {
                info.iconfold.setHidden(true);
                PokerChip chip = (PokerChip) info.icon.getCustomImage();
                if (nChipAmount > 0)
                {
                    chip.setValue(nChipAmount);
                    info.icon.setHidden(false);
                    info.icon.setUseCustom(true);
                }
                else if (sIcon.equals("icon-blank"))
                {
                    info.icon.setHidden(true);
                }
                else
                {
                    info.icon.changeName(sIcon);
                    info.icon.setHidden(false);
                    info.icon.setUseCustom(false);
                }
            }
            info.icon.repaint();
            info.iconfold.repaint();
            
            //ImageComponent ic = ImageComponent.getImage("icon-bet", 1.0d);
            //info.bet.setIcon(ic);
            //ImageConfig.getImageIcon("icon-bet"));
            // no need to repaint here since territory redoes it
            // info.bet.repaint();
        }
        
        super.repaintTerritory(t, bImmediate);
    }

    /**
     * Repaint all
     */
    public void repaintAll()
    {
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) logger.debug("************** REPAINT ALL ********************************************");
        for (Territory territory : territories_)
        {
            if (territory.isEdge()) continue;
            repaintTerritory(territory);
        }
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) logger.debug("************** REPAINT ALL END ****************************************");
    }
    

    @Override
    protected void paintChildren(Graphics g)
    {
        //if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) logger.debug("************** REPAINT CHILDREN ++++++++++++++++++++++++++++++++++++++++");
        super.paintChildren(g);
        //if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) logger.debug("************** REPAINT CHILDREN END +++++++++++++++++++++++++++++++++++++");
    }

    /**
     * override to repaint resize correctly
     * @param g1
     */
    @Override
    protected void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;
        super.paintComponent(g);

        if (delegate_ != null) delegate_.repainting(g);
    }

    //////
    ////// SIZE CHANGE logic
    //////
    
    /**
     * Setup resize sizes given starting nWidth/nHeight (smallest size)
     */
    private void setupSize(int nWidth, int nHeight)
    {
        // display size of board is size of scroll minus border
        Dimension newsize = new Dimension(nWidth, nHeight);

        // size first fits board in initial window, second uses default size of image
        Dimension dPref = getPreferredSize(newsize);
        
        // create resize dimensions based on starting size - also sets board to this initialize size
        setResizeDimensions(dPref);
    }
    
    ////
    //// territory data
    ////
    
    public static class TerritoryInfo
    {
        public DDText bet;
        public ImageComponent icon;
        public ImageComponent iconfold;
        public DDText result;
        public ResultsPiece resultpiece;
    }
    
    public static TerritoryInfo getTerritoryInfo(Territory t)
    {
        if (t == null) return null;
        return (TerritoryInfo) t.getUserData();
    }
    
    public static void setTerritoryInfo(Territory t, TerritoryInfo info)
    {
        t.setUserData(info);
    }


    ////
    //// sample gameboard for table designer
    ////
    public static class FauxPokerGameboard extends ImageComponent
    {
        Color top_, bottom_;
        private Faux2 faux2_;

        public FauxPokerGameboard(Color top, Color bottom)
        {
            super("engine.basepanel", 1.0d);
            setLayout(new BorderLayout());
            setTile(true);
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(Color.black));


            faux2_ = new Faux2(this);
            add(faux2_, BorderLayout.CENTER);

            setPreferredSize(new Dimension(faux2_.getImageWidth() + 2, faux2_.getImageHeight() + 2));

            top_ = top;
            bottom_ = bottom;
        }

        public void updateColors(Color top, Color bottom)
        {
            if (top_.equals(top) && bottom_.equals(bottom)) return;
            top_ = top;
            bottom_ = bottom;
            refresh();
        }

        public void updateTop(Color top)
        {
            if (top_.equals(top)) return;
            top_ = top;
            refresh();
        }

        public void updateBottom(Color bottom)
        {
            if (bottom_.equals(bottom)) return;
            bottom_ = bottom;
            refresh();
        }

        private void refresh()
        {
            faux2_.setRefreshBuffer(true);
            repaint();
        }

    }

    /**
     * for use above
     */
    private static class Faux2 extends ImageComponent
    {
        FauxPokerGameboard parent;

        private Faux2(FauxPokerGameboard parent)
        {
            super("pokertable2preview", 1.0);
            this.parent = parent;
            setParentTile(parent);
            setBuffer(true);
        }
        /**
         * After tiling, but before image is overlaid, draw felt
         */
        @Override
        protected void paintParentTile(Graphics2D g)
        {
            super.paintParentTile(g);
            drawFelt(g, getWidth(), getHeight(), parent.top_, parent.bottom_);
        }
    }
}
