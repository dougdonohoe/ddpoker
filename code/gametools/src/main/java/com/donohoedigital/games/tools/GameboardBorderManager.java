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
 * GameBoardManager.java
 *
 * Created on October 27, 2002, 2:46 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.LoggingConfig;
import com.donohoedigital.games.config.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GameboardBorderManager extends GameManager
{
    static Logger logger = LogManager.getLogger(GameboardBorderManager.class);
    
    // run options
    double dScale_;
    boolean bNoImage_;
    int nPointSize_;
    
    // default values
    private static final double NO_SCALE = -1.0;
    private static final boolean DEFAULT_NO_IMAGE = false;
    private static final int DEFAULT_POINT_SIZE = 6;
    
    // UI components
    private XPoints xpoints_;
    
    /**
     * Run the Gameboard Manager
     */
    public static void main(String[] args) {
        try {
            LoggingConfig loggingConfig = new LoggingConfig("gametools", ApplicationType.CLIENT);
            loggingConfig.init();

            GameboardBorderManager gamemgr = new GameboardBorderManager("gametools", args);
            gamemgr.init();
            
            // create the UI
            gamemgr.createUI();
                    
            // display the window
            gamemgr.displayGameManager();
            
        }
        catch (ApplicationError ae)
        {
            logger.fatal("GameboardBorderManager ending due to ApplicationError: " + ae.toStringNoStackTrace());
            System.exit(1);
        }  
    }
    
    /**
     * Can be overridden for application specific options
     */
    protected void setupApplicationCommandLineOptions()
    {
        super.setupApplicationCommandLineOptions();
        CommandLine.addFlagOption("noimage");
        CommandLine.setDescription("noimage", "Don't display image (lines only)");
        CommandLine.addDoubleOption("scale", NO_SCALE);
        CommandLine.setDescription("scale", "Scale to display image", "double");
        CommandLine.addIntegerOption("pointsize", DEFAULT_POINT_SIZE);
        CommandLine.setDescription("pointsize", "Width of points in pixels", "integer");
    }
    
    /**
     * Create GameboardBorderManager from config file
     */
    public GameboardBorderManager(String sConfigName, String[] args)
                throws ApplicationError
    {
        super(sConfigName, "Border Manager", args);
    }

    /**
     * init
     */
    public void init()
    {
        super.init();
           
        // get options
        dScale_ = gameconfig_.getScale();
        bNoImage_ = htOptions_.getBoolean("noimage", DEFAULT_NO_IMAGE);
        nPointSize_ = htOptions_.getInteger("pointsize", DEFAULT_POINT_SIZE);
        
        double dNewScale = htOptions_.getDouble("scale", NO_SCALE);
        
        if (dScale_ != dNewScale && dNewScale != NO_SCALE)
        {
            logger.info("Setting new scale: " + dNewScale);
            gameconfig_.setScale(dNewScale);
            dScale_ = dNewScale;
        }
    }
    
    /**
     * Create the UI
     */
    protected void createUI()
    {
        super.createUI();
        
        // init status bar
        borderPointSelected(null, null);
    }
    
    /**
     * Create component that gets scrolled
     */
    public JComponent createScrollComponent() {

        xpoints_ = new XPoints(this, gameconfig_, dScale_, bNoImage_);
        xpoints_.setPointSize(nPointSize_);
        
        // update width/height with current width we are using
        gameconfig_.setWidth(xpoints_.getWidth());
        gameconfig_.setHeight(xpoints_.getHeight());
        
        return xpoints_;
    }
    
    /**
     * Return component that should get focus upon display
     */
    public JComponent getFocusComponent() {
        return xpoints_;
    }

    // used within chooseBorder logic
    BorderChooser chooser_;
    private boolean bIgnoreBorderUpdates_ = false;
    
    /**
     * Choose a border from the chooser
     */
    Border chooseBorder(String sTitle, int x, int y, boolean bIgnoreBorderUpdates)
    {
        bIgnoreBorderUpdates_ = bIgnoreBorderUpdates;
        // get current point of map in view so we can calc
        // correct x,y for screen
        Point viewPoint = scroll_.getViewport().getViewPosition();
        
        if (chooser_ == null)
        {
            chooser_ = new BorderChooser(frame_, sTitle, gameconfig_.getTerritories());
        }
        // show chooser
        Border b = BorderChooser.getBorder(chooser_, x-viewPoint.x, y-viewPoint.y);
        
        // add to list of borders (if already there, just updates)
        if (b != null) {
            b = gameconfig_.getBorders().addBorder(b);
        }
        bIgnoreBorderUpdates_ = false;
        return b;
    }
    
    /**
     * called when border point selected - used to update status area
     */
    public void borderPointSelected(Border b, BorderPoint p) 
    {
        if (bIgnoreBorderUpdates_) return;
        if (b == null)
        {
            setStatus("No border selected");
        }
        else
        {
            String sMessage = "Current border:  " + b.shortDesc();
            if (p == null)
            {
                sMessage += "   (no point selected)";
            }
            else
            {
                sMessage += "   Point " + p.longDesc(b);
            }
            setStatus(sMessage);
        }
    }
    
    ////
    //// Key listeners
    ////
    
    public void keyPressed(KeyEvent e) 
    {
        if (e.isConsumed()) return;
        
        switch (e.getKeyCode())
        {
            // add new point at current keyboard cursor (if visible) other
            case KeyEvent.VK_N:
                    if (xhair_.isDrawing())
                    {
                        MouseEvent fake = new MouseEvent(xhair_, MouseEvent.MOUSE_RELEASED, 
                                                0, 0,
                                                xhair_.getX() + xhair_.getWidth()/2, 
                                                xhair_.getY() + xhair_.getHeight()/2,
                                                1, false, MouseEvent.BUTTON1);
                        xpoints_.processMouseClick(fake);
                        e.consume();
                    }
                    break;

            default:
                super.keyPressed(e);
        }
    }
}
