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
 * CardPiece.java
 *
 * Created on January 1, 2004, 6:11 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.prefs.*;

/**
 *
 * @author  Doug Donohoe
 */
public class CardPiece extends PokerGamePiece
{    
    static Logger cLogger = Logger.getLogger(CardPiece.class);
    
    // territory point names
    public static final String POINT_HOLE1 = "hole1";
    public static final String POINT_FLOP1 = "flop1";
    public static final String POINT_FLOP2 = "flop2";
    public static final String POINT_FLOP3 = "flop3";
    public static final String POINT_FLOP4 = "flop4";
    public static final String POINT_FLOP5 = "flop5";
    
    // font/color
    protected static Font cardFont_ = StylesConfig.getFont("card.value");
    protected static Color cardRed_ =  Color.red;
    protected static Color cardBlack_ = Color.black;
    protected static Color cardBlue_ = Color.blue;
    protected static Color cardGreen_ = new Color(0, 153, 0);

    // transitory
    GameContext context_;
    private boolean bTempVisible_ = false;
    protected CardImageComponent ic_;

    // stored info
    protected boolean bUp_ = true;
    protected int nSeq_ = 0;
    protected boolean bDrawnNormal_ = true;
    protected boolean bThumbnailMode_ = false;

    // transient
    private Card card_ = null; // used to hard code card instead of linking to a player
    private boolean bLarge_ = false; // used to hard code large cards

    /**
     * Empty constructor needed for demarshalling
     */
    public CardPiece() {}
    
    /** 
     * Creates a new instance of CardPiece 
     */
    public CardPiece(GameContext context, PokerPlayer player, String sTerritoryPoint, boolean bUp, int nSeq) {
        super(PokerConstants.PIECE_CARD, player, sTerritoryPoint, "card");
        context_ = context;
        bUp_ = bUp;
        nSeq_ = nSeq;
        ic_  = new CardImageComponent(player, nSeq_);
    }
    
    /**
     * debug
     */
    @Override
    public String toString()
    {
        return getCard().toString();
    }

    /**
     * index of card in hand
     */
    public int getCardIndex()
    {
        return nSeq_;
    }

    /**
     * set up
     */
    public void setUp(boolean b)
    {
        bUp_ = b;
    }
    
    /**
     * set whether drawn in normal game
     */
    public void setDrawnNormal(boolean b)
    {
        bDrawnNormal_ = b;
    }
    
    
    /**
     * Return whether this card is up
     */
    public boolean isUp()
    {
        return bUp_;
    }
    
    /**
     * Return whether this card is drawn in a normal game
     */
    public boolean isDrawnNormal()
    {
        return bDrawnNormal_;
    }
    
    /**
     * Set chip race mode
     */
    public void setThumbnailMode(boolean b)
    {
        bThumbnailMode_ = b;
    }
    
    /**
     * Get card
     */
    public Card getCard()
    {
        if (card_ != null) return card_;
        Hand hand = pokerplayer_.getHand();
        if (hand == null) return null;
        return hand.getCard(nSeq_);
    }

    /**
     * Set card, override any associated player
     */
    public void setCard(Card card)
    {
        card_ = card;
    }

    /**
     *  Get number of cards in hand (used to formatting layout)
     */
    public int getNumCards()
    {
        Hand hand = pokerplayer_.getHand();
        if (hand == null) return 0;
        return hand.size();
    }

    /**
     * is this hand folded?
     */
    public boolean isFolded()
    {
        return (pokerplayer_ != null) && pokerplayer_.isFolded() && !pokerplayer_.showFoldedHand();
    }

    /**
     * Set visible flag
     */
    public void setTemporarilyVisible(boolean b)
    {
        bTempVisible_ = b;
    }
    
    /**
     * Is piece visible (used during exploration/native battles)
     */
    public boolean isTemporarilyVisible()
    {
        return bTempVisible_;
    }
    
    ////
    //// Overridden drawing methods
    ////

    /**
     * Get image based on whether card is up/down
     */
    @Override
    public ImageComponent getImageComponent()
    {
        return ic_;            
    }
    
    /**
     * Should the card be drawn up?
     */
    boolean drawCardUp()
    {
        if (bUp_) return true;
        
        if (isTemporarilyVisible()) return true;
        
        boolean bMouse = PokerUtils.isCheatOn(context_, PokerConstants.OPTION_CHEAT_MOUSEOVER);
        return bMouse && isUnderMouse();
    }
    
    /**
     * return card value (used for debugging)
     */
    @Override
    protected String getQuantityString(int nNum, int nHiddenNum, int nMovingNum)
    {
        return getCard().getDisplay();
    }

    /**
     * set shadow
     */
    public void setShadow(boolean b)
    {
        ic_.setShadow(b);
    }

    /**
     * card fill
     */
    public void setFill(boolean b)
    {
        ic_.setFill(b);
    }

    /**
     * card fill color
     */
    public void setFillColor(Color c)
    {
        ic_.setFillColor(c);
    }

    /**
     * Card stroke
     */
    public void setStroke(boolean b)
    {
        ic_.setStroke(b);
    }

    /**
     * Set gradient
     */
    public void setGradient(boolean b)
    {
        ic_.setGradient(b);
    }
    /**
     * large cards
     */
    public void setLarge(boolean b)
    {
        bLarge_ = b;
    }

    /**
     * large cards?
     */
    private boolean isLarge()
    {
        return bLarge_ || isLargePref();
    }

    /**
     * large perf set?
     */
    protected boolean isLargePref()
    {
        return GameEngine.getGameEngine().getPrefsNode().getBoolean(PokerConstants.OPTION_LARGE_CARDS, true);
    }

    // image back
    private static String sPrefName_ = ProfileList.PROFILE_PREFIX + DeckProfilePanel.DECK_PROFILE;
    private static ImageComponent icDefault_ = null;
    private static String sLastBack_ = null;
    private static ImageComponent icBack_ = null;

    private synchronized static ImageComponent getDefault()
    {
        if (icDefault_ == null)
        {
            icDefault_ = ImageComponent.getImage("card-back", 1.0f);
        }
        return icDefault_;
    }

    /**
     * deck image
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    protected ImageComponent getDeckImage()
    {
        String sName = getDeckPref();

        if (sLastBack_ != null && sName.equals(sLastBack_))
        {
            return icBack_;
        }
        else
        {
            File file = new File(DeckProfile.getProfileDir(DeckProfile.DECK_DIR), sName);
            if (!file.exists() || !file.isFile())
            {
                icBack_ = getDefault();
                sLastBack_ = sName;
                return icDefault_;
            }
            BufferedImage img = ImageDef.getBufferedImage(file);
            if (img != null)
            {
                icBack_ = new ImageComponent(img, 1.0d);
                sLastBack_ = sName;
                return icBack_;
            }
            else
            {
                icBack_ = getDefault();
                sLastBack_ = sName;
                return icBack_;
            }
        }
    }

    /**
     * get name of preferred deck file name
     */
    protected String getDeckPref()
    {
        Preferences prefs = GameEngine.getGameEngine().getPrefsNode();
        String sName = prefs.get(sPrefName_, null);
        if (sName == null)
        {
            sName = PropertyConfig.getStringProperty("msg.back.default", "card-Water.jpg");
            prefs.put(sPrefName_, sName);
        }
        return sName;
    }

    /**
     * decide order of drawing
     */
    @Override
    public void drawPiece(Gameboard board, Graphics2D g, Territory t,
                            GeneralPath path, Rectangle territoryBounds, 
                            int iPart, int x_adjust, int y_adjust) 
    {
        if (isFolded()) return; // don't draw at all

        // adjust if drawing a hand
        if (pokerplayer_ != null)
        {
            int nSeat = PokerUtils.getDisplaySeatForTerritory(t);

            // chip race mode - draw 1/2 size, in a grid
            if (bThumbnailMode_)
            {
                int nOrder = nSeq_;
                boolean bShiftRight = true;
                
                // adjust position of cards based on seat - 
                // so they are drawn close to label
                switch (nSeat)
                {
                    case 0:
                    case 1:
                    case 7:
                    case 8:
                    case 9:
                        nOrder += 2;
                        if (nOrder > 3) nOrder -= 4;
                        break;
                            
                }
                
                int G = 4;
                
                switch (nOrder)
                {
                    case 0:                    
                        x_adjust = (int) -(G + CARD_W * getScale() * .5d);
                        y_adjust = (int) -(G + CARD_H * getScale() * .5d);
                        break;
                        
                    case 1:
                        x_adjust = (int) +(G + CARD_W * getScale() * .5d);
                        y_adjust = (int) -(G + CARD_H * getScale() * .5d);
                        break;
                        
                    case 2:
                        x_adjust = (int) -(G + CARD_W * getScale() * .5d);
                        y_adjust = (int) +(G + CARD_H * getScale() * .5d);
                        break;
                        
                    case 3:
                        x_adjust = (int) +(G + CARD_W * getScale() * .5d);
                        y_adjust = (int) +(G + CARD_H * getScale() * .5d);
                        break;
                        
                    default:
                        throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "too many color up cards: " +nSeq_, null);
                }
                
               if (bShiftRight) x_adjust += (int) (CARD_W * getScale() * .5d);
            }
            else if (getNumCards() > 1)
            {
                // if large mode, shift cards left a bit
                // so line up under placard
                if (isLarge())
                {
                    if (nSeat == 5)
                    {
                        x_adjust -= 10;
                    }
                    else if (nSeat >= 3 && nSeat <= 7)
                    {
                        x_adjust -= 5;
                    }
                    else
                    {
                        x_adjust -= 4;
                    }
                }
            }
            // high card - shift over to right to center
            else
            {
                x_adjust += 10;
            }
        }
        
        super.drawPiece(board, g, t, path, territoryBounds, iPart, x_adjust, y_adjust);
    }
    
    /**
     * Get scale, adjust for color up mode
     */
    @Override
    public double getScale()
    {
        double s = super.getScale();
        if (bThumbnailMode_) s *= .45d;
        return s;
    }
    
    /**
     * Return xadjust when drawing multiple pieces in a region
     */
    @Override
    public int getXAdjust()
    {
        if (isLarge()) 
        {
//            int nSeat = PokerUtils.getSeatForTerritory(getTerritory());
//            if (nSeat >= 3 && nSeat <= 6)
//            {
//                return 82;
//            }
//            else
//            {
//                return 77;
//            }
            return 82;
        }
        return 55;
    }
    
    /**
     * Return yadjust when drawing multiple pieces in a region
     */
    @Override
    public int getYAdjust()
    {
        return 26;
    }
    
    /**
     * Set last drawn bounds - override to add fudge factor for mouse selection
     */
    @Override
    protected void setLastDrawnBounds(int x, int y, int width, int height)
    {    
        super.setLastDrawnBounds(x-5, y-5,width+10,height+10);
    }

    /**
     * Overridden to draw card
     */
    @Override
    public void drawImageAt(Graphics2D g, ImageComponent ic,
                                    int nNum, int nHiddenNum, int nMovingNum,
                                    double x, double y, 
                                    double width, double height,
                                    double dScale)
    {
        // defensive: if no card to draw, skip out
        Card card = getCard(); if (card == null) return;

        super.drawImageAt(g, ic, nNum, nHiddenNum, nMovingNum, x, y,
                                width, height, dScale);

        if (ic instanceof CardImageComponent)
        {
            ic.setHighlighted(isDrawHighlighted());
        }

        double dADJ = 1.0d;
        boolean bLarge = isLarge();

        if (bThumbnailMode_)
        {
            dADJ = 3.75d;
        }
        else if (bLarge)
        {
            dADJ = 2.0d;
        }

        // card back if not face up
        if (!drawCardUp())
        {
            ImageComponent back = getDeckImage();
            double fw = width * .85d;
            double fh = (fw * back.getHeight()) / (double)back.getWidth();
            double heightcheck = .85d;
            // if too tall, scale based on height (for custom images)
            if (fh > (height * heightcheck))
            {
                fh = height * heightcheck;
                fw = (fh * back.getWidth()) / (double)back.getHeight();
            }
            double fx = x + (width - fw)/2.0d;
            double fy = y + (height - fh)/2.0d;
            back.drawImageAt(g, (int) Math.round(fx), (int) Math.round(fy), (int) Math.round(fw), (int) Math.round(fh));
            return;
        }

        // Color
        Color c = getSuitColor(card);

        // draw pips/face image if not chip race mode (really large)
        if (!bThumbnailMode_)
        {
            FaceCards FC = getFaceImage(card);

            double x1 = x;
            double y1 = y;
            double width1 = width;
            double height1 = height;

            double boxxadj;
            double boxyadj;
            double boxx = 0;
            double boxy = 0;
            double boxwidth = 0;
            double boxheight = 0;

            // adjust position, size for large mode
            if (bLarge)
            {
                x1 = x + (width * .23d);
                y1 = y + (width * .30d);
                width1 = width * .82d;
                height1 = height * .80d;
            }

            // enclosing box and size of face cards
            if (bLarge)
            {
                boxxadj = width1 * .13d;
                boxyadj = height1 * .07d;
                boxx = x1 + boxxadj;
                boxy = y1 + boxyadj;
                boxwidth = width1 - (2 * boxxadj);
                boxheight = height1 - (2 * boxyadj);
            }
            else if (card.isFaceCard())
            {
                // scale based on size of card we are drawing
                Rectangle2D bounds = FC.getBounds();
                boxwidth = (width * .60d);
                boxheight = ((boxwidth * bounds.getHeight()) / bounds.getWidth());
                boxx = (x + (width - boxwidth)/2);
                boxy = (y + (height - boxheight)/2);
            }


            // draw pips
            switch(card.getRank())
            {
                case Card.ACE:
                    drawPip(card, g, R4C2, x1, y1, width1, height1, c, 2.0);
                    break;

                case 3:
                    drawPip(card, g, R4C2, x1, y1, width1, height1, c, 1.0);
                    // fall through
                case 2:
                    drawPip(card, g, R1C2, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R7C2, x1, y1, width1, height1, c, 1.0);
                    break;

                case 5:
                    drawPip(card, g, R4C2, x1, y1, width1, height1, c, 1.0);
                    // fall through

                case 4:
                    drawPip(card, g, R1C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R1C3, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R5C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R5C3, x1, y1, width1, height1, c, 1.0);
                    break;

                case 8:
                    drawPip(card, g, R5C2, x1, y1, width1, height1, c, 1.0);
                    // fall through

                case 7:
                    drawPip(card, g, R3C2, x1, y1, width1, height1, c, 1.0);
                    // fall through

                case 6:
                    drawPip(card, g, R1C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R3C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R5C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R1C3, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R3C3, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R5C3, x1, y1, width1, height1, c, 1.0);
                    break;

                case 9:
                case 10:
                    drawPip(card, g, R1C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R2C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R4C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R5C1, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R1C3, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R2C3, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R4C3, x1, y1, width1, height1, c, 1.0);
                    drawPip(card, g, R5C3, x1, y1, width1, height1, c, 1.0);

                    if (card.getRank() == 9) {
                        drawPip(card, g, R4C2, x1, y1, width1, height1, c, 1.0);
                    } else {
                        drawPip(card, g, R2C2, x1, y1, width1, height1, c, 1.0);
                        drawPip(card, g, R6C2, x1, y1, width1, height1, c, 1.0);
                    }
                    break;

                case Card.JACK:
                case Card.QUEEN:
                case Card.KING:
                    FC.draw(g, boxx, boxy, boxwidth, boxheight);
                    break;
            }

            // draw outline box in large mode (all cards) and face cards in normal mode
            if (bLarge || card.isFaceCard())
            {
                g.setColor(c);
                g.drawRect((int)boxx, (int)boxy,
                           (int)Math.round(boxwidth), (int)Math.round(boxheight));
                //logger.debug("Size: "+ boxwidth +" x "+boxheight);
            }
        }

        // setup size
        String sRank = card.getRankDisplay();
        double xadjust = 0.0d;
        if (card.getRank() == 10)
        {
            xadjust = TEN_ADJUST;
            sRank = "1";
        }

        double textscale = height/CARD_H;
        if (bLarge || bThumbnailMode_) textscale *= dADJ;
        if (bThumbnailMode_) textscale *= .90;

        // draw suit
        double fw = RATIO_SIDE_WIDTH * dADJ;
        double fh = RATIO_SIDE_HEIGHT * dADJ;
        double fx = x + width*SUIT_HORZSUIT_INDENT;
        double fy = y + height*SUIT_VERTSUIT_INDENT;
        if (bThumbnailMode_)
        {
            fw *= 1.5;
            fh *= 1.6;
            fx += width * .56d;
            fy += height * .50d;
        }
        else if (bLarge)
        {
            fw *= 1.3;
            fh *= 1.4;
            fx += width * .075d;
            fy += height * .20d;
        }
        drawPip(card, g, width, height, fx, fy, fw, fh, false, c, 1.0);

        // draw rank
        fx = (x + width*(SUIT_HORZFONT_INDENT + xadjust));
        fy = (y + height*SUIT_VERTFONT_INDENT);
        if (bThumbnailMode_)
        {
            fx += width * .20d;
            fy += height * .15d;
        }
        else if (bLarge)
        {
            fx += width * .06d;
            fy += height * .04d;
        }
        TextUtil tuNum = new TextUtil(g, cardFont_, sRank);
        tuNum.setAlwaysAntiAlias(true);
        tuNum.prepareDraw(fx, fy, null, textscale, true);
        tuNum.drawString(c, null);
        tuNum.finishDraw();

        double font10_zero_scale = .080d;
        if (card.getRank() == 10)
        {
            fx += (width * font10_zero_scale * dADJ);
            tuNum = new TextUtil(g, cardFont_, "0");
            tuNum.setAlwaysAntiAlias(true);
            tuNum.prepareDraw(fx, fy, null, textscale, true);
            tuNum.drawString(c, null);
            tuNum.finishDraw();
        }

        // do upside down version
        if (!bLarge && !bThumbnailMode_)
        {
            // suit
            fx = ((x + width) - (width*(SUIT_HORZSUIT_INDENT)) - fw);
            fy = ((y + height) - (height*SUIT_VERTSUIT_INDENT) - fh);
            fx -= width * .005d;
            drawPip(card, g, width, height, fx, fy, fw, fh, true, c, 1.0);

            // rank
            fx = ((x + width) - width*(SUIT_HORZFONT_INDENT+xadjust));
            fy = ((y + height) - height*SUIT_VERTFONT_INDENT);
            tuNum = new TextUtil(g, cardFont_, sRank);
            tuNum.setAlwaysAntiAlias(true);
            tuNum.prepareDraw(fx, fy, UPSIDE_DOWN, textscale, true);
            tuNum.drawString(c, null);
            tuNum.finishDraw();

            if (card.getRank() == 10)
            {
                fx -= (width * font10_zero_scale);
                tuNum = new TextUtil(g, cardFont_, "0");
                tuNum.setAlwaysAntiAlias(true);
                tuNum.prepareDraw(fx, fy, UPSIDE_DOWN, textscale, true);
                tuNum.drawString(c, null);
                tuNum.finishDraw();
            }
        }
    }

    /**
     * Get color used for suit
     */
    private Color getSuitColor(Card card)
    {
        if (!isEnabled()) return FG_GRAY;

        Color c = cardBlack_;
        if (card.isHearts()) {
            c = cardRed_;
        } else if (card.isSpades()) {
                c = cardBlack_;
        } else if (card.isClubs()) {
            if (is4ColorPref()) {
                c = cardGreen_;
            } else {
                c = cardBlack_;
            }
        } else if (card.isDiamonds()) {
            if (is4ColorPref()) {
                c = cardBlue_;
            } else {
                c = cardRed_;
            }
        }
        return c;
    }

    /**
     * is 4 color pref on?
     */
    protected boolean is4ColorPref()
    {
        return GameEngine.getGameEngine().getPrefsNode().getBoolean(PokerConstants.OPTION_FOUR_COLOR_DECK, false);
    }

    /** 
     * draw card pip
     */
    private void drawPip(Card card, Graphics2D g, Pos pos, double x, double y,
                         double width, double height, Color suitColor, double altsize)
    {
        double fx = (x + ((width)*pos.x));
        double fy = (y + ((height)*pos.y));

        drawPip(card, g,
                width, height,
                fx, fy,
                RATIO_WIDTH, RATIO_HEIGHT,
                pos.bUpsideDown,
                suitColor,
                altsize);
    }


    private void drawPip(Card card, Graphics2D g,
                         double boundsWidth, double boundsHeight,
                         double centerX, double centerY,
                         double maxWidthRatio, double maxHeightRatio,
                         boolean bUpsideDown,
                         Color suitColor,
                         double altsize)
    {
        GeneralPath pp = null;

        if (card.getCardSuit() == CardSuit.DIAMONDS)
        {
            pp = diamond_;
        }
        else if (card.getCardSuit() == CardSuit.SPADES)
        {
            pp = spade_; altsize *= .95; // make a bit smaller
        }
        else if (card.getCardSuit() == CardSuit.HEARTS)
        {
            pp = heart_; altsize *= 1.08; // heart is a bit small, so compensate here
        }
        else if (card.getCardSuit() == CardSuit.CLUBS)
        {
            pp = club_; altsize *= 1.05; // make a bit bigger
        }
        else
        {
            pp = unknown_; altsize *= .85; // ? is a bit big
        }

        // figure out max width and height, and scale from that
        Rectangle2D bounds = pp.getBounds2D();
        double maxWidth = boundsWidth * maxWidthRatio;
        double maxHeight = boundsHeight * maxHeightRatio;
        double scale = Math.min(maxWidth / bounds.getWidth(),
                                maxHeight / bounds.getHeight());
        scale *= altsize;

        // create transformation from path to this instance
        AffineTransform tx = new AffineTransform();
        tx.translate(centerX-(scale*bounds.getWidth()/2),centerY-(scale*bounds.getHeight()/2));
        if (bUpsideDown)
        {
            tx.rotate(Math.PI);
            tx.translate(-scale*bounds.getWidth(), -scale*bounds.getHeight());
        }
        tx.scale(scale, scale);

        // paint it

        Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

        Shape s = pp.createTransformedShape(tx);
        g.setColor(suitColor);
        g.fill(s);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);

        // debug - 2x2 square at center
        //g.setColor(Color.white);
        //g.drawRect((int)(centerX-1),(int)(centerY-1),2,2);
    }

    /**
     * Get a FaceCards path for card
     */
    private FaceCards getFaceImage(Card card)
    {
        if (card.isFaceCard())
        {
            Preferences prefsNode = GameEngine.getGameEngine().getPrefsNode();
            boolean fourColorDeck = prefsNode.getBoolean(PokerConstants.OPTION_FOUR_COLOR_DECK, false);
            boolean stylized = prefsNode.getBoolean(PokerConstants.OPTION_STYLIZED_FACE_CARDS, true);

            if (card.getCardSuit() == CardSuit.SPADES)
            {
                switch (card.getRank())
                {
                    case com.ddpoker.Card.JACK:
                        if (stylized)
                        {
                            return FaceCards.Style_Jack_Spades;
                        }
                        return FaceCards.J_Spades;
                    case Card.QUEEN:
                        if (stylized)
                        {
                            return FaceCards.Style_Queen_Spades;
                        }
                        return FaceCards.Q_Spades;
                    case Card.KING:
                        if (stylized)
                        {
                            return FaceCards.Style_King_Spades;
                        }
                        return FaceCards.K_Spades;
                }
            }
            if (card.getCardSuit() == CardSuit.CLUBS)
            {
                switch (card.getRank())
                {
                    case Card.JACK:
                        if (stylized)
                        {
                            if (fourColorDeck) return FaceCards.Style_Jack_Clubs_Grn;
                            else return FaceCards.Style_Jack_Clubs_Blk;
                        }
                        return FaceCards.J_Clubs;
                    case Card.QUEEN:
                        if (stylized)
                        {
                            if (fourColorDeck) return FaceCards.Style_Queen_Clubs_Grn;
                            else return FaceCards.Style_Queen_Clubs_Blk;
                        }
                        return FaceCards.Q_Clubs;
                    case Card.KING:
                        if (stylized)
                        {
                            if (fourColorDeck) return FaceCards.Style_King_Clubs_Grn;
                            else return FaceCards.Style_King_Clubs_Blk;
                        }
                        return FaceCards.K_Clubs;
                }
            }
            if (card.getCardSuit() == CardSuit.DIAMONDS)
            {
                switch (card.getRank())
                {
                    case Card.JACK:
                        if (stylized)
                        {
                            if (fourColorDeck) return FaceCards.Style_Jack_Diamonds_Blue;
                            else return FaceCards.Style_Jack_Diamonds_Red;
                        }
                        return FaceCards.J_Diamonds;
                    case Card.QUEEN:
                        if (stylized)
                        {
                            if (fourColorDeck) return FaceCards.Style_Queen_Diamonds_Blue;
                            else return FaceCards.Style_Queen_Diamonds_Red;
                        }
                        return FaceCards.Q_Diamonds;
                    case Card.KING:
                        if (stylized)
                        {
                            if (fourColorDeck) return FaceCards.Style_King_Diamonds_Blue;
                            else return FaceCards.Style_King_Diamonds_Red;
                        }
                        return FaceCards.K_Diamonds;
                }
            }
            if (card.getCardSuit() == CardSuit.HEARTS)
            {
                switch (card.getRank())
                {
                    case Card.JACK:
                        if (stylized)
                        {
                            return FaceCards.Style_Jack_Hearts;
                        }
                        return FaceCards.J_Hearts;
                    case Card.QUEEN:
                        if (stylized)
                        {
                            return FaceCards.Style_Queen_Hearts;
                        }
                        return FaceCards.Q_Hearts;
                    case Card.KING:
                        if (stylized)
                        {
                            return FaceCards.Style_King_Hearts;
                        }
                        return FaceCards.K_Hearts;
                }
            }

        }
        return null;
    }

    ////
    //// suits
    ////
    private static GeneralPath club_ = GuiUtils.drawSVGpath(CardSuitPaths.CLUB, true);
    private static GeneralPath spade_ = GuiUtils.drawSVGpath(CardSuitPaths.SPADE, true);
    private static GeneralPath diamond_ = GuiUtils.drawSVGpath(CardSuitPaths.DIAMOND, true);
    private static GeneralPath heart_ = GuiUtils.drawSVGpath(CardSuitPaths.HEART, true);
    private static GeneralPath unknown_ = GuiUtils.drawSVGpath(CardSuitPaths.UNKNOWN, true);

    // 180 degree flip for bottom right part of card
    private static Integer UPSIDE_DOWN = 180;

    // basis of measurements/positions below.  Width/height of
    // a KEM card measured using the 50 scale (times 10) on my architects ruler
    private static final double W = 112;
    private static final double H = 174.5;

    // pip sizes
    private static final double RATIO_HEIGHT = 29/H; // max height of a pip relative to total height
    private static final double RATIO_WIDTH = 21/W;  // max width of a pip relative to total width
    private static final double RATIO_SIDE_HEIGHT = 20/H; // max height of side suit display relative to total height
    private static final double RATIO_SIDE_WIDTH = 17/H;  // max width of side suit display relative to total width

    // these are used to gauge indent for rank/suit
    private static final double SUIT_HORZSUIT_INDENT = 10.20/W;
    private static final double SUIT_VERTSUIT_INDENT = 37/H;
    private static final double SUIT_VERTFONT_INDENT = 14.14d/H;
    private static final double SUIT_HORZFONT_INDENT = 12.44d/W;

    // adjust for drawing 10, since it is wider
    private static final double TEN_ADJUST = -(6.22d/W);

    // location of pips
    private static final double X1 = 29.5d/W;
    private static final double X2 = .5;
    private static final double X3 = 82.5/W;

    private static final double Y1 = 31/H;
    private static final double Y2 = 50/H;
    private static final double Y3 = 60/H;
    private static final double Y4 = 69/H;
    private static final double Y5 = .5;
    private static final double Y6 = (H-69)/H;
    private static final double Y7 = (H-60)/H;
    private static final double Y8 = (H-50)/H;
    private static final double Y9 = (H-31)/H;

    private static final Pos R1C1 = new Pos(X1, Y1, false);
    private static final Pos R2C1 = new Pos(X1, Y4, false);
    private static final Pos R3C1 = new Pos(X1, Y5, false);
    private static final Pos R4C1 = new Pos(X1, Y6, true);
    private static final Pos R5C1 = new Pos(X1, Y9, true);
    
    private static final Pos R1C2 = new Pos(X2, Y1, false);
    private static final Pos R2C2 = new Pos(X2, Y2, false);
    private static final Pos R3C2 = new Pos(X2, Y3, false);
    private static final Pos R4C2 = new Pos(X2, Y5, false);
    private static final Pos R5C2 = new Pos(X2, Y7, true);
    private static final Pos R6C2 = new Pos(X2, Y8, true);
    private static final Pos R7C2 = new Pos(X2, Y9, true);

    private static final Pos R1C3 = new Pos(X3, Y1, false);
    private static final Pos R2C3 = new Pos(X3, Y4, false);
    private static final Pos R3C3 = new Pos(X3, Y5, false);
    private static final Pos R4C3 = new Pos(X3, Y6, true);
    private static final Pos R5C3 = new Pos(X3, Y9, true);

    /**
     * class to represent pip location and whether it is drawn
     * upside down
     */
    private static class Pos
    {
        double x, y;
        boolean bUpsideDown;
        Pos(double x, double y, boolean bUpsideDown)
        {
            this.x=x;
            this.y=y;
            this.bUpsideDown = bUpsideDown;
        }
    }
    
    /**
     * Override to compare based on card
     */
    @Override
    public int compareTo(Object o)
    {
        if (o instanceof CardPiece)
        {
            CardPiece c = (CardPiece) o;
            // use sequence so cards stay in order they were dealt
            return nSeq_ - c.nSeq_;
        }
        else
        {
            return super.compareTo(o);
        }
    }

    ////
    //// cards
    ////
    private static final Color BORDER = StylesConfig.getColor("card.border");
    private static final Color SELECTED = StylesConfig.getColor("card.selected");
    private static final Color STROKE = StylesConfig.getColor("card.stroke");
    private static final Color SHADOW = PokerChip.shadow_;//StylesConfig.getColor("card.shadow");
    private static final Color BG = StylesConfig.getColor("card.bg");
    private static final Color BG_GRAY = StylesConfig.getColor("card.bg.gray");
    private static final Color FG_GRAY = StylesConfig.getColor("card.fg.gray");
    public static final float CARD_W = 250;
    public static final float CARD_H = 350;

    /**
     * Class to draw card bg
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class CardImageComponent extends EmptyImageComponent
    {
        PokerPlayer player;
        int nSeq;
        private boolean bShadow_ = true;
        private boolean bFill_ = true;
        private boolean bStroke_ = false;
        private boolean bGradient_ = false;
        private Color bg_ = BG;

        public CardImageComponent()
        {
            this(null, 0);
        }
        
        public CardImageComponent(PokerPlayer player, int nSeq)
        {
            super((int)CARD_W, (int)CARD_H);
            this.player = player;
            this.nSeq = nSeq;
        }

        private void setShadow(boolean b)
        {
            bShadow_ = b;
        }

        private void setFill(boolean b)
        {
            bFill_ = b;
        }

        private void setFillColor(Color c)
        {
            bg_ = c;
        }

        private void setStroke(boolean b)
        {
            bStroke_ = b;
        }

        private void setGradient(boolean b)
        {
            bGradient_ = b;
        }

        @Override
        public void drawImageAt(Graphics2D g, int x, int y, int width, int height)
        {
            Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

            // calc arc and shadow adjust (proportion of drawing width/height)
            float arc = ((35.0f/CARD_H) * height);

            // create rects to draw
            RoundRectangle2D.Float rect = new RoundRectangle2D.Float(x, y, width, height, arc, arc);
            RoundRectangle2D.Float shad = new RoundRectangle2D.Float(x+2, y+2, width, height, arc, arc);
            
            // shadow
            if (bFill_ && bShadow_)
            {
                g.setColor(SHADOW);
                g.fill(shad);
            }

            // border (top/left)
            if (bFill_)
            {
                rect.x -= 1.0f;
                rect.y -= 1.0f;
                if (!bStroke_ && bShadow_)
                {
                    g.setColor(BORDER);
                    g.fill(rect);
                }

                // card
                rect.x += 1.0f;
                rect.y += 1.0f;
                g.setColor(isEnabled() ? bg_ : BG_GRAY);
                g.fill(rect);
            }

            // gradient to draw more 3D
            if (bGradient_ && isEnabled() && bg_.equals(BG))
            {
                RoundRectangle2D.Float grect = new RoundRectangle2D.Float(x, y, width, height, arc, arc);
                float mod = rect.height / 3;
                grect.y += mod;
                grect.height -= mod;
                GradientPaint gp = new GradientPaint(grect.x+(grect.width*(2.0f/3)), grect.y, gradFrom_,
                                                     grect.x+(grect.width*(1.0f/3)), grect.y+grect.height, gradTo_);
                g.setPaint(gp);
                g.fill(grect);
            }

            // border if selected
            boolean bHighlite = isHighlighted();
            boolean bDouble = false;
            if (!bHighlite && (player != null))
            {
                if (player.isCurrentGamePlayer()) bHighlite = true;
                
                if (player.isWonChipRace() && !player.isBrokeChipRace())
                {
                    Hand uhand = player.getHand();
                    HandSorted hand = player.getHandSorted();
                    if (hand.getCard(hand.size() - 1).equals(uhand.getCard(nSeq)))
                    {
                        bHighlite = true;
                        bDouble = true;
                    }
                    
                }
            }

            if (bStroke_ && isEnabled())
            {
                float w = rect.width;
                float h = rect.height;
                rect.width -= .8f;
                rect.height -= 1.0f;
                float strokewidth = width*.04f;
                BasicStroke borderStroke = new BasicStroke(strokewidth, BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_ROUND);
                Stroke olds = g.getStroke();
                g.setStroke(borderStroke);
                g.setColor(STROKE);
                g.draw(rect);

                g.setStroke(olds);
                rect.width = w;
                rect.height = h;
            }

            if (bHighlite)
            {
                float strokewidth = width*.04f;
                if (bDouble) strokewidth *= 2;
                BasicStroke borderStroke = new BasicStroke(strokewidth, BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_ROUND);
                Stroke olds = g.getStroke();
                g.setStroke(borderStroke);
                g.setColor(SELECTED);
                g.draw(rect);
                g.setStroke(olds);
            }

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
        }
    }

    private static Color gradFrom_ = new Color(255,255,255,255);
    private static Color gradTo_ = new Color(222,222,222,255);

    ////
    //// Save/Load logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    @Override
    public GameStateEntry addGameStateEntry(GameState state, boolean bAdd)
    {
        GameStateEntry entry = super.addGameStateEntry(state, bAdd);
        entry.addToken(bUp_);
        entry.addToken(bDrawnNormal_);
        entry.addToken(nSeq_);
        entry.addToken(bThumbnailMode_);
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    @Override
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        super.loadFromGameStateEntry(state, entry);
        bUp_ = entry.removeBooleanToken();
        bDrawnNormal_ = entry.removeBooleanToken();
        nSeq_ = entry.removeIntToken();
        bThumbnailMode_ = entry.removeBooleanToken();
        
        ic_  = new CardImageComponent(pokerplayer_, nSeq_);
    }
}
