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
 * GameBoardManager.java
 *
 * Created on October 27, 2002, 2:46 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.LoggingConfig;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GameboardTerritoryManager extends GameManager implements CustomTerritoryDrawer
{
    static Logger logger = LogManager.getLogger(GameboardTerritoryManager.class);
    
    // UI Components
    private TerritoryBoard board_;
    
    /**
     * Run the Gameboard Manager
     */
    public static void main(String[] args) {
        try {
            LoggingConfig loggingConfig = new LoggingConfig("gametools", ApplicationType.CLIENT);
            loggingConfig.init();

            GameboardTerritoryManager gamemgr = new GameboardTerritoryManager("gametools", args);
            gamemgr.init();
            
            // create the UI
            gamemgr.createUI();
                    
            // display the window
            gamemgr.displayGameManager();
        }
        catch (ApplicationError ae)
        {
            logger.fatal("GameboardTerritoryManager ending due to ApplicationError: " + Utils.formatExceptionText(ae));
            System.exit(1);
        }  
    }

    /**
     * Create GameboardTerritoryManager from config file
     */
    public GameboardTerritoryManager(String sConfigName, String[] args)
                throws ApplicationError
    {
        super(sConfigName, "Territory Manager", args);
    }
    
    private int nDefaultSize = 800;
    
    /**
     * Create UI
     */
    protected void createUI()
    {
        super.createUI();
        scroll_.getViewport().setPreferredSize(board_.getPreferredSize(new Dimension(nDefaultSize,nDefaultSize)));
    }

    /**
     * Create component that gets scrolled
     */
    public JComponent createScrollComponent() 
    {
        board_ = new TerritoryBoard(frame_, this, gameconfig_);
        board_.setTerritoryDisplayListener(new XAdapter());
        board_.setCustomTerritoryDrawer(this);
        
        // set to default size
        //board_.setNewSize(board_.getPreferredSize(new Dimension(nDefaultSize,nDefaultSize)), false);
        
        // set to actual size of image
        board_.setNewSize(board_.getPreferredSize(new Dimension(board_.getWidth(),board_.getHeight())), false);
        
        // add actions for [ and ]
        GuiUtils.addKeyAction(board_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "decrease", new DecreaseSize(), 
                            KeyEvent.VK_OPEN_BRACKET, 0);
        GuiUtils.addKeyAction(board_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "increase", new IncreaseSize(), 
                            KeyEvent.VK_CLOSE_BRACKET, 0);
        
        return board_;
    }
    
    private class XAdapter extends TerritoryDisplayApapter
    {    
        /**
        * If t.isSelected(), return "territory.border.selected" color
        */
        public Color getTerritoryBorderColor(Territory t) 
        {
            return Color.black;
        }

        /**
         * Get stroke for drawing border
         */
        public BasicStroke getTerritoryBorderStroke(Territory t) 
        {
            return borderStroke_;
        }
    }
    
    /**
     * Return component that should get focus upon display
     */
    public JComponent getFocusComponent() {
        return board_;
    }    
    
    /**
     * increate/decease size of game board
     */
    private void changeGameboardSize(int nChange)
    {
        board_.changeSize(nChange);
        Dimension size = board_.getPreferredSize();
        XYConstraints xyc = new XYConstraints(0,0,size.width, size.height);
        scrollBasePanel_.remove(board_);
        scrollBasePanel_.setSize(board_.getPreferredSize());
        scrollBasePanel_.add(board_, xyc, 1);
        scroll_.validate();
        scroll_.repaint();
        board_.requestFocus();
    }

//    // war-aoi specific (TODO: config)
//    private String INFANTRY = PropertyConfig.getStringProperty("define.territoryPointType.infantry", "notdefined", false);
//    private String ARTILLERY = PropertyConfig.getStringProperty("define.territoryPointType.artillery", "notdefined", false);
//    private String CAVALRY = PropertyConfig.getStringProperty("define.territoryPointType.cavalry", "notdefined", false);
//    private String EXPLORER = PropertyConfig.getStringProperty("define.territoryPointType.explorer", "notdefined", false);
//    private String ENGINEER = PropertyConfig.getStringProperty("define.territoryPointType.engineer", "notdefined", false);
//    private String REDSHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-red", "notdefined", false);
//    private String PURPLESHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-purple", "notdefined", false);
//    private String GREENSHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-green", "notdefined", false);
//    private String BLUESHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-blue", "notdefined", false);
//    private String YELLOWSHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-yellow", "notdefined", false);
//    private String GRAYSHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-gray", "notdefined", false);
//    private String WHITESHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-white", "notdefined", false);
//    private String BLACKSHIP = PropertyConfig.getStringProperty("define.territoryPointType.ship-black", "notdefined", false);
//    private String LEADER = PropertyConfig.getStringProperty("define.territoryPointType.leader", "notdefined", false);
//    private String FORT = PropertyConfig.getStringProperty("define.territoryPointType.fort", "notdefined", false);
//    private String SCHOOL = PropertyConfig.getStringProperty("define.territoryPointType.school", "notdefined", false);
//    private String BUILDING = PropertyConfig.getStringProperty("define.territoryPointType.building", "notdefined", false);
//    private String RESOURCE = PropertyConfig.getStringProperty("define.territoryPointType.resource", "notdefined", false);
//    private String NATIVE = PropertyConfig.getStringProperty("define.territoryPointType.native", "notdefined", false);
//    private Double SCALE_MARKER = .9d; // must match resource piece getScale() override
    private Double SCALE_BUTTON = .4d; // must match buttonpiece getScale() override
    private Double SCALE_ICON = .75d; // approximation
    
    public void drawTerritoryPart(Gameboard board, Graphics2D g, Territory t, GeneralPath path, Rectangle territoryBounds, int iPart) 
    {
        
//        int nHash = t.hashCode();
//        int nColor = nHash % 8;
//
//        String sColor = null;
//        switch (nColor)
//        {
//            case 0: sColor = "red"; break;
//            case 1: sColor = "yellow"; break;
//            case 2: sColor = "blue"; break;
//            case 3: sColor = "green"; break;
//            case 4: sColor = "purple"; break;
//            case 5: sColor = "gray"; break;
//            case 6: sColor = "white"; break;
//            case 7: sColor = "black"; break;
//        }
//
//
//        //war-aoi (TODO: config-driven)
//        drawItem(board, g, t, REDSHIP, "ship.small.red"); 
//        drawItem(board, g, t, BLUESHIP, "ship.small.blue");
//        drawItem(board, g, t, GREENSHIP, "ship.small.green");
//        drawItem(board, g, t, YELLOWSHIP, "ship.small.yellow");
//        drawItem(board, g, t, PURPLESHIP, "ship.small.purple");
//        drawItem(board, g, t, GRAYSHIP, "ship.small.gray");
//        drawItem(board, g, t, WHITESHIP, "ship.small.white");
//        drawItem(board, g, t, BLACKSHIP, "ship.small.black");
//        drawItem(board, g, t, NATIVE,   "marker.native.small", SCALE_MARKER);  // match
//        drawItem(board, g, t, RESOURCE, "marker.resource.small", SCALE_MARKER);
//        drawItem(board, g, t, FORT, "fort.small");
//        drawItem(board, g, t, SCHOOL, "school.small");
//        drawItem(board, g, t, BUILDING, "factory.small");
//        drawItem(board, g, t, LEADER, "leader.small."+ sColor);
//        drawItem(board, g, t, CAVALRY, "cavalry.small."+ sColor);
//        drawItem(board, g, t, ARTILLERY, "artillery.small."+ sColor);
//        drawItem(board, g, t, ENGINEER, "engineer.small."+ sColor);
//        drawItem(board, g, t, EXPLORER, "explorer.small."+ sColor);
//        drawItem(board, g, t, INFANTRY, "infantry.small."+ sColor);
        
        // poker (TODO: config-driven)
        drawItem(board, g, t, "hole1", "card", null, false, 0, 0);
        int xadjust;
        int nSeat = PokerUtils.getDisplaySeatForTerritory(t);
        if (nSeat >= 3 && nSeat <= 6)
        {
            xadjust = 85;
        }
        else
        {
            xadjust = 77;
        }
        if (nSeat >= 3 && nSeat <= 6)
        {
            xadjust -= 7;
        }
        else
        {
            xadjust -= 2;
        }
        drawItem(board, g, t, "hole1", "card", null, false, xadjust, 26); // match PokerGamePiece
        drawItem(board, g, t, "flop1", "card", null, false, 0, 0);
        drawItem(board, g, t, "flop2", "card", null, false, 0, 0);
        drawItem(board, g, t, "flop3", "card", null, false, 0, 0);
        drawItem(board, g, t, "flop4", "card", null, false, 0, 0);
        drawItem(board, g, t, "flop5", "card", null, false, 0, 0);
        drawItem(board, g, t, "button", "button", SCALE_BUTTON, false, 0, 0);
        drawItem(board, g, t, "icon", "icon-call", SCALE_ICON, false, 0, 0);
        drawItem(board, g, t, "icon-check", "icon-check", SCALE_ICON, false, 0, 0);
    }
    
    private void drawItem(Gameboard board, Graphics2D g, Territory t, String sNAME, String sImageName)
    {
        drawItem(board, g, t, sNAME, sImageName, null);
    }
    
    private void drawItem(Gameboard board, Graphics2D g, Territory t, String sNAME, String sImageName, Double dScale)
    {
        drawItem(board, g, t, sNAME, sImageName, dScale, true, 0, 0);
    }
    
    private void drawItem(Gameboard board, Graphics2D g, Territory t, String sNAME, String sImageName, 
                        Double dScale, boolean bCenterAtBottom, int xadjust, int yadjust)
    {
        TerritoryPoint tp = t.getTerritoryPoint(sNAME);
        if (tp == null) {
            return;
        }
        
        double scale = tp.getTerritory().getScaleImagesAsDouble();
        if (dScale != null)
        {
            scale = dScale;
        }
        ImageComponent ic = null;
        
        if (sImageName.equals("card"))
        {
            ic = new CardPiece.CardImageComponent();
            ic.setScale(scale);
        }
        else
        {
            ic = ImageComponent.getImage(sImageName, scale);
        }
        
        if (ic == null) return;
        
        Dimension size = ic.getSize();
        xadjust *= scale;
        yadjust *= scale;
        int width = board.scaleToCurrentSpace(size.width);
        int height = board.scaleToCurrentSpace(size.height);
        int x = board.scaleToCurrentSpace(tp.x_+xadjust) - (width/2);// center left-right
        int y;
        
        if (bCenterAtBottom)
            y = board.scaleToCurrentSpace(tp.y_+yadjust) - (height-2); // position bottom of image at point
        else
            y = board.scaleToCurrentSpace(tp.y_+yadjust) - (height/2); // center top-bottom
        
        ic.drawImageAt(g, x, y, width, height);
    }
    
    /**
     * Increase size of this by x%
     */
    private class IncreaseSize extends AbstractAction
    {
        public void actionPerformed(ActionEvent e) 
        {
            changeGameboardSize(1);
        }
    }
    
    /**
     * Decrease size of this by x%
     */
    private class DecreaseSize extends AbstractAction
    {
        public void actionPerformed(ActionEvent e) 
        {
            changeGameboardSize(-1);
        }
    }    
}
