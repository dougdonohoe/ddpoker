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
 * ShowPokerTable.java
 *
 * Created on December 26, 2003, 10:17 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.dashboard.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Doug Donohoe
 */
public abstract class ShowPokerTable extends ChainPhase implements
                                                        Gameboard.AIDebug, AWTEventListener,
                                                        ChangeListener, InternalDialog.ModalBlockerListener,
                                                        MouseListener, PokerTableInput,
                                                        ActionListener, PokerGameboardDelegate
{
    static Logger logger = LogManager.getLogger(ShowPokerTable.class);

    // UI Components
    protected ImageComponent base_;
    protected BaseFrame frame_;

    protected PokerGame game_;
    protected PokerGameboard board_;
    private Resize resize_;
    private Camera camera_;
    private DDPanel panelbase_;
    private DashboardPanel dashboardPanel_;

    protected JComponent rightbase_;
    protected JComponent bottombase_;

    protected PokerImageButton buttonQuit_;
    protected PokerImageButton buttonInfo_;
    protected PokerImageButton buttonOptions_;
    protected PokerImageButton buttonHelp_;
    protected PokerImageButton buttonSave_;

    protected DDLabel labelName_;

    // Dimensions
    private static int SMALLEST_WIDTH = 25;
    private static int SMALLEST_HEIGHT = 18;
    private int nInputMode_ = MODE_INIT;
    public static final int LEFT_PANEL_WIDTH = 200;

    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // init territory nums
        PokerUtils.initTerritories();

        // get game and set input handler
        game_ = (PokerGame) context.getGame();
        game_.setInput(this);

        // if full screen, change smallest width/height to the screen size
        frame_ = context.getFrame();
        if (frame_.isFullScreen())
        {
            DisplayMode mode = frame_.getDisplayMode();
            SMALLEST_WIDTH = mode.getWidth();
            SMALLEST_HEIGHT = mode.getHeight();
        }

        // base for gameboard
        base_ = new ImageComponent("engine.basepanel", 1.0d);
        base_.setTile(true);
        base_.setOpaque(true);
        base_.setName("pokerbase");

        // panels
        createPanels();

        // allow subclass to do stuff (including create
        // its instance of common widgets like time, blinds labels
        subclassInit(engine, gamephase);

        /////
        ///// Game init (need to repaint all to ensure everything is displayed)
        /////
        updateName();
        board_.repaintAll();

        // mouse listener for right click and modal dialog listener
        GuiUtils.addMouseListenerChildren(base_, this);
        InternalDialog.setModalBlockerListener(this);
    }

    protected void createPanels()
    {
        // panels
        DDPanel center = createCenterPanel();
        rightbase_ = createLeftPanel();
        bottombase_ = createBottomPanel();

        // layout
        base_.setLayout(new BorderLayout(0, 0));
        DDPanel right = new DDPanel();
        base_.add(right, BorderLayout.CENTER);
        base_.add(rightbase_, BorderLayout.WEST);
        right.add(center, BorderLayout.CENTER);
        right.add(bottombase_, BorderLayout.SOUTH);

        // name (in title area above table)
        labelName_ = new DisplayLabel(600 / 2.6f, 35 / 2.2f, 300f / 1200f, 10f / 900f, 600f / 1200f,
                                      SwingConstants.CENTER, SwingConstants.TOP, "tname");

    }

    protected DDPanel createCenterPanel()
    {
        // another panel to hold gameboard and message area
        panelbase_ = new DDPanel();
        panelbase_.setName("panelbase");
        panelbase_.setLayout(new GameboardCenterLayout());
        panelbase_.setMinimumSize(new Dimension(SMALLEST_WIDTH, SMALLEST_HEIGHT));

        // resize control
        if (createResize())
        {
            resize_ = new Resize();
            panelbase_.add(resize_, new ScaleConstraintsFixed(SwingConstants.BOTTOM, SwingConstants.LEFT));
        }

        // camera control
        if (createCamera())
        {
            camera_ = new Camera();
            panelbase_.add(camera_, new ScaleConstraintsFixed(SwingConstants.TOP, SwingConstants.RIGHT));
        }

        // create gameboard
        board_ = createGameboard();
        panelbase_.add(board_);

        // buttons and other controls that
        // go directly on the game board
        board_.setLayout(new ScaleLayout());

        return panelbase_;
    }

    /**
     * Should resize control be created on gameboard?  Default is false.
     */
    protected boolean createResize()
    {
        return false;
    }

    /**
     * Should camera control be created on gameboard?  Default is false.
     */
    protected boolean createCamera()
    {
        return false;
    }

    /**
     * resize control UI and logic
     */
    private class Resize extends DDLabel implements MouseMotionListener, MouseListener
    {
        boolean bMoving;
        DDLabel resizeDisplay;
        int xStart;
        int yStart;

        public Resize()
        {
            setPreferredSize(new Dimension(16, 20));

            ImageIcon icon = ImageConfig.getImageIcon("resize");
            clearText();
            setIcon(icon);
            setCursor(Cursors.MOVE);
            setToolTipText(PropertyConfig.getMessage("msg.tooltip.resize"));
            addMouseMotionListener(this);
            addMouseListener(this);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

            resizeDisplay = new DDLabel();
            resizeDisplay.setSize(new Dimension(25, 25));
            resizeDisplay.clearText();
            resizeDisplay.setIcon(ImageConfig.getImageIcon("resize-move"));
            resizeDisplay.setCursor(Cursors.MOVE);
        }

        public void mouseDragged(MouseEvent e)
        {
            Point p = SwingUtilities.convertPoint(this, e.getX() - 8, e.getY() - 4, frame_.getContentPane());
            resizeDisplay.setLocation(p);
        }

        public void mouseMoved(MouseEvent e)
        {
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mousePressed(MouseEvent e)
        {
            bMoving = true;
            xStart = e.getX();
            yStart = e.getY();
            Point p = SwingUtilities.convertPoint(this, xStart - 8, yStart - 4, frame_.getContentPane());
            resizeDisplay.setLocation(p);
            frame_.getLayeredPane().add(resizeDisplay, JLayeredPane.DRAG_LAYER);
            if (e.getClickCount() == 2)
            {
                bMoving = false;
                changeSize(-Integer.MAX_VALUE, -Integer.MAX_VALUE);
            }
        }

        public void mouseReleased(MouseEvent e)
        {
            if (bMoving) changeSize((e.getX() - xStart), -(e.getY() - yStart));
        }

        private void changeSize(int wDelta, int hDelta)
        {
            frame_.getLayeredPane().remove(resizeDisplay);
            if (wDelta == 0 && hDelta == 0) return;
            if (rightbase_ != null && bottombase_ != null)
            {
                Dimension size;
                size = rightbase_.getPreferredSize();
                size.width = Math.max(size.width + wDelta, rightbase_.getMinimumSize().width);
                rightbase_.setPreferredSize(size);

                size = bottombase_.getPreferredSize();
                size.height = Math.max(size.height + hDelta, bottombase_.getMinimumSize().height);
                bottombase_.setPreferredSize(size);

                //logger.debug("revalidate/repaint, bottom size: " + size);
                base_.revalidate();
            }
        }

        public void mouseEntered(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
        }
    }

    /**
     * Camera button
     */
    private class Camera extends DDImageButton implements ActionListener
    {
        private Camera()
        {
            super("camera");
            addActionListener(this);
            setToolTipText(PropertyConfig.getMessage("msg.tooltip.camera"));
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        }

        public void actionPerformed(ActionEvent e)
        {
            PokerUtils.doScreenShot(context_);
        }
    }

    /**
     * logic to create the poker gameboard
     */
    protected PokerGameboard createGameboard()
    {
        PokerGameboard board = new PokerGameboard(engine_, context_, engine_.getGameboardConfig(),
                                                  SMALLEST_WIDTH,
                                                  SMALLEST_HEIGHT,
                                                  frame_.getContentPane().getWidth(),
                                                  frame_.getContentPane().getHeight(),
                                                  this, base_);
        board.setAIDebug(this);
        PokerUtils.setPokerGameboard(board);
        return board;
    }

    /**
     * logic to create the bottom panel, meant to be overriden
     */
    protected DDPanel createBottomPanel()
    {
        return new DDPanel();
    }

    /**
     * create left panel (dashboard)
     */
    protected DDPanel createLeftPanel()
    {
        // right base
        DDPanel rightbase = new DDPanel();
        rightbase.setBorderLayoutGap(5, 0);
        rightbase.setMinimumSize(new Dimension(LEFT_PANEL_WIDTH, 0));

        // button panel
        DDPanel buttonpanel = createButtonPanel(true);
        rightbase.add(GuiUtils.WEST(buttonpanel), BorderLayout.NORTH);

        // dashboard
        DashboardManager manager = new DashboardManager(game_);
        addDashboardItems(manager);
        dashboardPanel_ = new DashboardPanel(manager);
        rightbase.add(dashboardPanel_, BorderLayout.CENTER);

        return rightbase;
    }

    /**
     * Create button panel (quit,save,etc.)
     */
    protected DDPanel createButtonPanel(boolean bHorizontal)
    {
        // create buttons
        buttonQuit_ = new PokerImageButton(this, getGameButton("quit"));
        buttonSave_ = new PokerImageButton(this, getGameButton("save"));
        buttonInfo_ = new PokerImageButton(this, getGameButton("info"));
        buttonOptions_ = new PokerImageButton(this, getGameButton("options"));
        buttonHelp_ = new PokerImageButton(this, getGameButton("help"));

        // button layout - in north west corner of right panel
        DDPanel buttonpanel = new DDPanel();
        if (bHorizontal)
        {
            buttonpanel.setLayout(new GridLayout(1, 5, 0, 0));
            buttonpanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, buttonQuit_.getHeight()));
        }
        else
        {
            buttonpanel.setLayout(new GridLayout(5, 1, 0, 0));
        }

        // add buttons
        buttonpanel.add(buttonQuit_);
        buttonpanel.add(buttonSave_);
        buttonpanel.add(buttonInfo_);
        buttonpanel.add(buttonOptions_);
        buttonpanel.add(buttonHelp_);

        return buttonpanel;
    }

    /**
     * Add common items.  Subclass should call super.addDashboardItems()
     */
    protected void addDashboardItems(DashboardManager manager)
    {
    }

    /**
     * subclass customizations
     */
    protected void subclassInit(GameEngine engine, GamePhase gamephase)
    {
        return;
    }

    /**
     * EMPTY (used by subclasses) *
     */
    public void stateChanged(ChangeEvent e)
    {
    }

    /**
     * Get button name in form of name:phase, if phase exists in phase params
     */
    protected GameButton getGameButton(String sName)
    {
        return new GameButton(gamephase_.getButtonNameFromParam(sName));
    }

    /**
     * label for display info
     */
    protected class DisplayLabel extends DDLabel
    {
        public DisplayLabel(float nPrefW, float nPrefH,
                            double x, double y, double scale, int nHAlign, int nVAlign)
        {
            this(nPrefW, nPrefH, x, y, scale, nHAlign, nVAlign, GuiManager.DEFAULT);
        }

        public DisplayLabel(float nPrefW, float nPrefH,
                            double x, double y, double scale, int nHAlign, int nVAlign, String sName)
        {
            this(nPrefW, nPrefH, x, y, scale, nHAlign, nVAlign, sName, "PokerTable");
        }

        public DisplayLabel(float nPrefW, float nPrefH,
                            double x, double y, double scale, int nHAlign, int nVAlign, String sName, String sStyle)
        {
            super(sName, sStyle);
            setName("DisplayLabel-" + sName);
            setPreferredSize(new Dimension((int) nPrefW, (int) nPrefH));
            setHorizontalAlignment(nHAlign);
            setVerticalAlignment(nVAlign);
            ScaleConstraints sc = new ScaleConstraints(x, y, scale, getFont());
            board_.add(this, sc);
            if (sName.equals(GuiManager.DEFAULT)) setText("");
            else setForeground(StylesConfig.getColor("poker.label", Color.black));
            //setBorder(GuiUtils.REDBORDER); // TESTING
        }
    }

    /**
     * label for display info
     */
    protected class DisplayPill extends PillLabel
    {
        public DisplayPill(float nPrefW, float nPrefH,
                           double x, double y, double scale, int nHAlign, int nVAlign, String sName)
        {
            this(nPrefW, nPrefH, x, y, scale, nHAlign, nVAlign, sName, "PokerTablePill");
        }

        public DisplayPill(float nPrefW, float nPrefH,
                           double x, double y, double scale, int nHAlign, int nVAlign, String sName, String sStyle)
        {
            super(sName, sStyle);
            setName("PillLabel-" + sName);
            setPreferredSize(new Dimension((int) nPrefW, (int) nPrefH));
            setHorizontalAlignment(nHAlign);
            setVerticalAlignment(nVAlign);
            ScaleConstraints sc = new ScaleConstraints(x, y, scale, getFont());
            board_.add(this, sc);
            //setBorder(GuiUtils.REDBORDER); // TESTING
        }
    }

    /**
     * button for use on pokergameboard
     */
    protected class PokerButton extends GlassButton
    {
        public PokerButton(GameButton gb, float nPrefW, float nPrefH,
                           double x, double y, double scale, boolean bEnabled)
        {
            super(gb.getName(), "Glass");

            setPreferredSize(new Dimension((int) nPrefW, (int) nPrefH));
            ScaleConstraints sc = new ScaleConstraints(x, y, scale, getFont());
            board_.add(this, sc);
            setEnabled(bEnabled);
            setFocusable(false);
            setFocusTraversalKeysEnabled(false);
            //setBorder(GuiUtils.GREENBORDER); // TESTING
            EngineButtonListener listener = new EngineButtonListener(context_, ShowPokerTable.this, gb);
            addActionListener(listener);
            addActionListener(ShowPokerTable.this);
        }
    }

    /**
     * used for quit/save/etc.
     */
    public class PokerImageButton extends DDImageButton
    {
        /**
         * Creates a new instance of GameImageButton
         */
        public PokerImageButton(Phase phase, GameButton button) throws ApplicationError
        {
            super(button.getName());
            addActionListener(new EngineButtonListener(context_, phase, button));
        }

    }

    /**
     * Called by superclass from start method
     */
    public void process()
    {
        // clear any help widgets from previous phases to release assoc memory
        context_.getWindow().setHelpTextWidget(null);

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, base_, true, board_);
    }

    /**
     * start
     */
    public void start()
    {
        // super class
        super.start();

        // add our own listeners
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);

        // start bg music
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        EngineUtils.startBackgroundMusic(gamephase_);
                    }
                }
        );
    }

    /**
     * cleanup
     */
    public void finish()
    {
        // super
        super.finish();

        // clean dashboard items
        if (dashboardPanel_ != null)
        {
            dashboardPanel_.finish();
        }

        // stop music
        AudioConfig.stopBackgroundMusic();
        AudioConfig.stopLastMusic();

        // remove listeners
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);

        // cleanup board and other stuff
        board_.cleanup();
        PokerUtils.setPokerGameboard(null);
        PokerUtils.setScroll(null);
        InternalDialog.setModalBlockerListener(null);
    }

    ///
    /// AWTListener/Focus methods for mouse scrolling
    ///

    /**
     * Invoked when an event is dispatched in the AWT.
     */
    public void eventDispatched(AWTEvent event)
    {
        // fast action keys
        if (event instanceof KeyEvent)
        {
            KeyEvent k = (KeyEvent) event;

            // ignore keys when modal
            if (nModal_ > 0)
            {
                return;
            }

            // ignore if not a key pressed event
            if (k.getID() != KeyEvent.KEY_PRESSED)
            {
                return;
            }

            // see if we should ignore this keystroke
            if (filterKeystroke(k))
            {
                return;
            }

            // see if we should ignore keystroke based on
            // if it was repeated recently (within XXX millis)
            if (filterKeyStrokeDuplicates())
            {
                // look for duplicate
                int key = k.getKeyCode();
                if (key == lastkey_ && (k.getWhen() - lastwhen_) < 250)
                {
                    //logger.debug("Skipping dup: " + k.getKeyChar());
                    lastwhen_ = k.getWhen();
                    return;
                }

                // remember
                lastwhen_ = k.getWhen();
                lastkey_ = key;
            }

            handleKeyPressed((KeyEvent) event);
        }
    }

    // used to store last keystroke
    long lastwhen_ = 0;
    long lastkey_ = 0;

    /**
     * Ignored duplicate keystrokes (those within 250 millis of each other).
     * Default is false;
     */
    protected boolean filterKeyStrokeDuplicates()
    {
        return false;
    }

    /**
     * Default filtering.  Ignore tabbed pane source, text components
     */
    protected boolean filterKeystroke(KeyEvent k)
    {
        // ignore if source is a tabbed pane, because info dialog tabs can be changed with arrow keys
        if (k.getSource() instanceof javax.swing.JTabbedPane)
        {
            return true;
        }

        // ignore if source is a text component other than spinner
        if (k.getSource() instanceof javax.swing.text.JTextComponent)
        {
            return true;
        }

        return false;
    }

    /**
     * logic to handle a key press event, can be overriden but subclasses should
     * call this if they don't handle it.
     */
    protected boolean handleKeyPressed(KeyEvent event)
    {
        int key = event.getKeyCode();

        switch (key)
        {
            case KeyEvent.VK_I:
                if (buttonInfo_ != null && buttonInfo_.isEnabled())
                {
                    buttonInfo_.doClick(10);
                    return true;
                }
                break;
        }
        return false;
    }

    ////
    //// modal handling
    ////

    // count modals
    int nModal_ = 0;

    /**
     * Modal window started - allow us to process the panel
     */
    public void blockerCreated(JPanel panel)
    {
        nModal_++;
        //logger.debug("Create: " + nModal_);
    }

    /**
     * modal window done - allow us to clean up
     */
    public void blockerFinished(JPanel panel)
    {
        nModal_--;
        //logger.debug("Finish: " + nModal_);
    }

    ////
    //// Debugging AI
    ////

    public String getDebugDisplay(Territory t)
    {
        return null;
    }

    public double getScale()
    {
        return 1.0d;
    }

    public double getYAdjust(Territory t, TextUtil tuName, TextUtil tuDebug)
    {
        int nSeat = PokerUtils.getDisplaySeatForTerritory(t);
        if (nSeat == -1) return 0;

        if (nSeat <= 0 || nSeat >= 9) return -(2.5 * tuDebug.lineHeight);
        else if (nSeat <= 1 || nSeat >= 8) return -(4.0 * tuDebug.lineHeight);
        else return tuName.totalHeight + tuDebug.lineHeight / 2 + 2;
    }

    public Color getTextColor()
    {
        return Color.orange;
    }

    ////
    //// Mouse listener
    ////

    /**
     * EMPTY *
     */
    public void mouseClicked(MouseEvent e)
    {
    }

    /**
     * EMPTY *
     */
    public void mouseEntered(MouseEvent e)
    {
    }

    /**
     * EMPTY *
     */
    public void mouseExited(MouseEvent e)
    {
    }

    /**
     * EMPTY *
     */
    public void mousePressed(MouseEvent e)
    {
    }

    /**
     * EMPTY *
     */
    public void mouseReleased(MouseEvent e)
    {
    }

    /**
     * Set name from profile
     */
    private void updateName()
    {
        if (labelName_ != null)
        {
            labelName_.setText(PropertyConfig.getMessage("msg.name", game_.getProfile().getName()));
        }
    }

    /**
     * Set input mode
     */
    public void setInputMode(int nMode, HoldemHand hhand, PokerPlayer player)
    {
        nInputMode_ = nMode;
    }

    /**
     * Get current input mode
     */
    public int getInputMode()
    {
        return nInputMode_;
    }

    /**
     * Passes clicks on buttons with action codes to Game object for broadcast to listeners.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof DDButton)
        {
            DDButton button = (DDButton) e.getSource();

            int action = button.getActionID();

            if (action != 0)
            {
                game_.playerActionPerformed(action, 0);
            }
        }
    }


    ////
    //// PokerGameboardDelegate interface
    ////

    /**
     * Default doesn't do anything with request focus, let
     */
    public boolean processRequestFocus()
    {
        return false;
    }

    // for fast bounds
    private Rectangle bounds_ = new Rectangle();
    private Rectangle resizeBounds_ = new Rectangle();

    /**
     * Notify delegate that board is repainting, so resize control
     * can repaint if overlapped
     */
    public void repainting(Graphics2D g)
    {
        if (resize_ != null)
        {
            //resize_.paintImmediately(0,0,resize_.getWidth(),resize_.getHeight());
            //resize_.paintComponent(g);
            g.getClipBounds(bounds_);
            Rectangle parent = SwingUtilities.convertRectangle(board_, bounds_, panelbase_);
            resize_.getBounds(resizeBounds_);
            //logger.debug("Repaint request: " + bounds_ + " in parent: " + parent);

            if (parent.intersects(resizeBounds_))
            {
                //logger.debug("**** SHOULD REPAINT ***");
                Rectangle inboard = SwingUtilities.convertRectangle(panelbase_, resizeBounds_, board_);
                g.translate(inboard.x, inboard.y);
                resize_.paintComponent(g);
                g.translate(-inboard.x, -inboard.y);
            }
        }

        if (camera_ != null)
        {
            //resize_.paintImmediately(0,0,resize_.getWidth(),resize_.getHeight());
            //resize_.paintComponent(g);
            g.getClipBounds(bounds_);
            Rectangle parent = SwingUtilities.convertRectangle(board_, bounds_, panelbase_);
            camera_.getBounds(resizeBounds_);
            //logger.debug("Repaint request: " + bounds_ + " in parent: " + parent);

            if (parent.intersects(resizeBounds_))
            {
                //logger.debug("**** SHOULD REPAINT ***");
                Rectangle inboard = SwingUtilities.convertRectangle(panelbase_, resizeBounds_, board_);
                g.translate(inboard.x, inboard.y);
                camera_.paintComponent(g);
                g.translate(-inboard.x, -inboard.y);
            }
        }
    }
}
