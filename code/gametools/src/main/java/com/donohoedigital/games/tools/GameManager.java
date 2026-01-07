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
 * GameManager.java
 *
 * Created on November 11, 2002, 4:06 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public abstract class GameManager extends BaseApp implements KeyListener, StatusDisplay
{
    private Logger logger = LogManager.getLogger(GameManager.class);
    
    // debugging settings
    private boolean bDoSave = true;
    private boolean bExitEarly = false;
    private String sTitle;
    
    // config stuff
    protected GameboardConfig gameconfig_;
    
    // UI Components
    protected JPanel scrollBasePanel_ = new JPanel();
    protected JDesktopPane desktop_ = new JDesktopPane();
    protected JScrollPane scroll_;
    protected JComponent scrollThis_;
    protected JTextArea status_; 
    protected JTextField pointer_;
    protected Crosshair xhair_;
    
    /**
     * Create GameManager from config file
     */
    public GameManager(String sConfigName, String sTitle, String[] args)
                throws ApplicationError
    {
        super(sConfigName, null, args);
        
        if (bExitEarly) System.exit(0);

        this.sTitle = sTitle;
    }

    @Override
    public void init()
    {
        super.init();
           
        // load config
        gameconfig_ = new GameboardConfig(ConfigManager.getConfigManager().getExtraModule());
        
        // set title
        frame_.setTitle(sTitle + " - " + gameconfig_.getGamename());
    }
    
    /**
     * Set extra module as required
     */
    protected void setupApplicationCommandLineOptions()
    {
        CommandLine.setRequired("module");
    }
    
    /**
     * Show the main frame
     */
    public void displayGameManager()
    {
        // frame final setup     
        frame_.validate();
        frame_.pack();
        frame_.center();
        frame_.addWindowListener(new GameManagerWindowAdapter());
        
        displayMainWindow();
    }

    /**
     * Implement this to return item that gets focus upon display
     */
    public abstract JComponent getFocusComponent();
    
    /**
     * Implemetn this to return item that goes in scrollwindow
     */
    public abstract JComponent createScrollComponent();
    
    /** 
     * Called from window closing - return true by default
     */
    public boolean okayToClose() {
        
        return true;
    }
    
    /**
     * Save current points out to the XML file
     */
    protected void save()
    {
        try {
            if (bDoSave) 
            {
                gameconfig_.save(true);
                setStatus("Saved " + gameconfig_.getTerritories().size() + " territories, " +
                                           gameconfig_.getBorders().size() + " borders, and " +
                                           gameconfig_.getMapPoints().size() + " points: " +
                                           gameconfig_.getFile().getAbsolutePath());
            }
        }
        catch (ApplicationError e)
        {
            logger.warn("Error trying to save: " + e.toString());
        }
    }
    
    /**
     * Create window components
     */
    protected void createUI()
    {       
        scrollThis_ = createScrollComponent();
        
        // Panel which holds image and on which the XPoints/Lines are placed
        scrollBasePanel_.setLayout(new XYLayout()); 
        scrollBasePanel_.setCursor(Cursors.CROSSHAIR);
        
        // cross hair control
        scrollThis_.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isConsumed()) return;
                if (e.isControlDown())
                {
                    moveCrosshair(e);
                    if (!xhair_.isDrawing()) xhair_.setDrawing(true);
                }
            }
        });
        
        // pointer location update
        scrollThis_.addMouseMotionListener(new MouseMotionAdapter() {
            Format fString = new Format("%5d");
            
            public void mouseMoved(MouseEvent e) {
                
                String sMsg = fString.form(e.getPoint().x) + "," + fString.form(e.getPoint().y);
                pointer_.setText(sMsg);
                pointer_.repaint();
            }
        });
        
        // xhair cursor
        xhair_ = new Crosshair(100,100);
        xhair_.setDrawing(false);
        
        // add stuff to panel
        XYConstraints xyc = new XYConstraints(0,0,scrollThis_.getWidth(), scrollThis_.getHeight());
        scrollBasePanel_.add(scrollThis_, xyc, 0);
        scrollBasePanel_.add(xhair_, xhair_.getXYConstraints(), 0);
        
        // scrollpane for panel
        scroll_ = new JScrollPane(scrollBasePanel_);
        scroll_.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll_.getViewport().setPreferredSize(new Dimension(800,600));
        scroll_.getHorizontalScrollBar().setUnitIncrement(25);
        scroll_.getVerticalScrollBar().setUnitIncrement(25);
        
        // status
        status_ = new JTextArea(" ");
        status_.setRows(2);
        status_.setWrapStyleWord(true);
        status_.setLineWrap(true);
        status_.setEditable(false);
        status_.setOpaque(false);
        status_.setFocusable(false);
        
        // mouse pointer
        pointer_ = new JTextField(" ");
        pointer_.setColumns(7);
        pointer_.setEditable(false);
        pointer_.setOpaque(false);
        pointer_.setFocusable(false);
        pointer_.setHorizontalAlignment(JTextField.RIGHT);
        pointer_.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(3, 0, 3, 0),
                            BorderFactory.createEtchedBorder()));
        
        // status panel
        JPanel statuspanel = new JPanel();
        BorderLayout b = new BorderLayout();
        b.setVgap(2);
        statuspanel.setLayout(b);
        statuspanel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 5));
        statuspanel.add(status_, BorderLayout.CENTER);
        statuspanel.add(pointer_, BorderLayout.EAST);
        
        // basepanel (scroll panel and status)
        JPanel basepanel = new JPanel();
        basepanel.setLayout(new BorderLayout());
        basepanel.add(scroll_, BorderLayout.CENTER);
        basepanel.add(statuspanel, BorderLayout.SOUTH);
        
        // change content pane on frame to scrollpane
        frame_.setContentPane(basepanel);
        
        // key listeners
        basepanel.addKeyListener(this);
        basepanel.setFocusTraversalKeysEnabled(false);
        scrollBasePanel_.addKeyListener(this); 
        scrollBasePanel_.setFocusTraversalKeysEnabled(false); // prevent focus from leaving panel
        frame_.addKeyListener(this);
        frame_.setFocusTraversalKeysEnabled(false);
        scrollThis_.addKeyListener(this);
    }
    
    
    /**
     * Set status message
     */
    public void setStatus(String sMsg)
    {
        status_.setText(sMsg);
        status_.repaint();
    }
    
    /**
     * Move cross hair to location of mouse
     */
    protected void moveCrosshair(MouseEvent e)
    {
        // move that crosshair
        xhair_.moveTo(e.getPoint().x, e.getPoint().y);
        // TODO: repaint old/new location
        scrollThis_.repaint();
    }
    
    ////
    //// Key listeners
    ////
    
    /**
     * Empty
     */
    public void keyReleased(KeyEvent e) {
    }
    
    /**
     * Handles K (keyboard cursor), arrows for keyboard cursor, Save, Quit
     */
    public void keyPressed(KeyEvent e) 
    {
        if (e.isConsumed()) return;
        
        switch (e.getKeyCode())
        {
            // keyboard cursor
            case KeyEvent.VK_K:
                    xhair_.setDrawing(!xhair_.isDrawing());
                    break;
                    
            // shift-arrows move keyboard cursor
            case KeyEvent.VK_DOWN:
                    if (xhair_.isDrawing()) {
                        int nMove = 1;
                        if (e.isAltDown()) nMove = 10;
                        xhair_.moveRelative(0,nMove);
                        e.consume();
                    }
                    break;
            case KeyEvent.VK_UP:
                    if (xhair_.isDrawing()) {
                        int nMove = -1;
                        if (e.isAltDown()) nMove = -10;
                        xhair_.moveRelative(0,nMove);
                        e.consume();
                    }
                    break;
            case KeyEvent.VK_LEFT:
                    if (xhair_.isDrawing()) {
                        int nMove = -1;
                        if (e.isAltDown()) nMove = -10;
                        xhair_.moveRelative(nMove,0);
                        e.consume();
                    }
                    break;
            case KeyEvent.VK_RIGHT:
                    if (xhair_.isDrawing()) {
                        int nMove = 1;
                        if (e.isAltDown()) nMove = 10;
                        xhair_.moveRelative(nMove,0);
                       e.consume();
                    }
                    break;
                    
            // save CTRL-S, ALT-S
            case KeyEvent.VK_S:
                    if (e.isControlDown() || e.isAltDown())
                    {
                        save();
                    }
                    break;
            // exit on CTRL-Q, ALT-Q
            case KeyEvent.VK_Q:

                    if (e.isControlDown() || e.isAltDown())
                    {
                        if (okayToClose())
                        {
                            exit(0);
                        }
                    }
                    break;
        }
    }
    
    /**
     * Empty
     */
    public void keyTyped(KeyEvent e) {
    }
    
    ///
    /// WindowListener
    ///
    
    /**
     * make sure board has focus on reactivate
     */
    private class GameManagerWindowAdapter extends WindowAdapter
    {
        public void windowActivated(WindowEvent e) 
        {
           getFocusComponent().requestFocus();
        }
    }
}
