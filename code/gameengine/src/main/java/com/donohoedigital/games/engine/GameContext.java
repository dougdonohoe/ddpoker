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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 18, 2006
 * Time: 12:23:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameContext
{
    protected static Logger logger = Logger.getLogger(GameContext.class);

    // engine
    private GameEngine engine_;
    private GameContext parent_;

    // context-specific items
    private GameManager gameMgr_;
    private Game game_;
    private DDWindow window_;
    private EngineWindow frame_;
    private EngineDialog dialog_;
    private JComponent main_;

    // Holds last phase to be set as main panel in this context
    private Phase currentMainUIPhase_ = null;

    // Current phase being executed - this is set whenever a phase is
    // run unless the definition of the phase says it is transient (typically
    // set for info dialog type phases
    private Phase currentPhase_ = null;

    // Last loop phase (subclass of GamePlayerLoopPhase) to be run.  This is
    // important because it is used to determine which phase to return to when
    // doing a loop.  Assumes one loop runs at a time!!
    private GamePhase lastLoopPhase_ = null;

    // Stack of GamePhase's that have history=true
    private Stack<GamePhase> pastPhases_ = new Stack<GamePhase>();

    // Cached phases are Phase instances that are saved
    // for reuse because they typically retain state (e.g., loop phases and
    // menu phases which has user input
    private Map<String, Phase> cachedPhases_ = new HashMap<String, Phase>();


    /**
     * Constructor - new internal window in given context
     */
    public GameContext(GameContext context, String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        engine_ = context.engine_;
        game_ = context.game_;

        frame_ = context.frame_;
        dialog_ = new EngineDialog(this, sName, nDesiredMinWidth, nDesiredMinHeight);
        window_ = dialog_;

        // close handler created when displayed
    }

    /**
     * Constructor - new window
     */
    public GameContext(GameEngine engine, Game game, String sName,
                       int nDesiredMinWidth, int nDesiredMinHeight,
                       boolean bQuitOnClose)
    {
        engine_ = engine;
        game_ = game;

        frame_ = createEngineWindow(engine, sName, nDesiredMinWidth, nDesiredMinHeight);
        dialog_ = null;
        window_ = frame_;

        // close handler
        frame_.addWindowListener(new GameContextWindowAdapter(bQuitOnClose));
    }

    /**
     * create EngineWindow (allows for overrides)
     */
    protected EngineWindow createEngineWindow(GameEngine engine, String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        return new EngineWindow(engine, this, sName, nDesiredMinWidth, nDesiredMinHeight);
    }

    /**
     * Set parent game context
     */
    private void setParent(GameContext context)
    {
        parent_ = context;
    }

    /**
     * Set the online game manager
     */
    public void setGameManager(GameManager mgr)
    {
        // cleanup if null
        if (mgr == null)
        {
            gameMgr_ = null;
            return;
        }

        // set otherwise
        ApplicationError.assertTrue(gameMgr_ == null, "Online manager already set");
        gameMgr_ = mgr;
    }

    /**
     * Get the GameEngine
     */
    public GameEngine getGameEngine()
    {
        return engine_;
    }

    /**
     * Get the online game manager
     */
    public GameManager getGameManager()
    {
        return gameMgr_;
    }

    /**
     * Get the current game (use a PropertyChangeListener to
     * catch changes to the game)
     */
    public Game getGame()
    {
        return game_;
    }

    /**
     * Set the current game
     */
    public void setGame(Game game)
    {
        // if we have an existing game, clean it up if we are the top context for this game
        if (game_ != null && parent_ == null)
        {
            game_.finish();
        }

        game_ = game;
        if (game != null) ApplicationError.assertTrue(game.getGameContext() == this, "Context mismatch");
    }

    /**
     * Create a new game from the given save file
     */
    public Game createGame(GameState state)
    {
        return createGame(state, true);
    }

    /**
     * Create a new game from the given save file, but don't process
     * the stored phase.  It can be processed later by calling
     * game.processStartPhase()
     */
    public Game createGame(GameState state, boolean bProcessPhase)
    {
        Game game = createNewGame();
        setGame(game);
        game.loadGame(state, bProcessPhase);
        return game;
    }

    /**
     * Create an empty Game (or subclass thereof).
     * This is here for subclasses to optionally override
     * so they can use a subclass of Game
     */
    public Game createNewGame()
    {
        return new Game(this);
    }

    /**
     * Get dd window this game is in
     */
    public DDWindow getWindow()
    {
        return window_;
    }

    /**
     * return whether this is an internal window
     */
    public boolean isInternal()
    {
        return dialog_ != null;
    }

    /**
     * get dialog (null if isInternal is false - use getFrame instead)
     */
    public EngineDialog getDialog()
    {
        return dialog_;
    }

    /**
     * Get BaseFrame this context uses
     */
    public EngineWindow getFrame()
    {
        return frame_;
    }

    /**
     * process button in given phase.  calls phase.processButton(),
     * which can return false to prevent processing phase associated
     * with this button.  Returns result of processButton, which
     * indicates whether next phase processed.
     */
    public boolean buttonPressed(GameButton button, Phase phase)
    {
        if (phase.processButton(button))
        {
            String sPhase = button.getGotoPhase();
            if (sPhase != null)
            {
                TypedHashMap params = null;
                String sParam = button.getGenericParam();
                if (sParam != null)
                {
                    params = new TypedHashMap();
                    params.setString(GameButton.PARAM_GENERIC, sParam);
                }
                processPhase(sPhase, params);
            }
            return true;
        }
        return false;
    }

    /**
     * Display a phase in BasePanel.  If a phase is already set, it is
     * removed and finish() is called on it.
     */
    public void setMainUIComponent(Phase phase, JComponent comp, boolean bBorderLayout, JComponent cFocus)
    {
        if (currentMainUIPhase_ != null)
        {
            currentMainUIPhase_.finish();
        }
        currentMainUIPhase_ = phase;
        main_ = comp;

        if (!isInternal())
        {
            frame_.getBasePanel().setCenterComponent(comp, bBorderLayout, cFocus);
        }
        else
        {
            dialog_.setCenterComponent(comp, cFocus);
        }

        // TODO: option to resize window to component (timing might not work due to fact window displayed b4 this is called)
    }

    /**
     * get Main ui component
     */
    public JComponent getMainUIComponent()
    {
        return main_;
    }

    /**
     * Get highest component
     */
    public JComponent getRootComponent()
    {
        if (!isInternal())
        {
            return (JComponent) frame_.getContentPane();
        }
        else
        {
            return (JComponent) dialog_.getContentPane();
        }
    }

    /**
     * calls processPhase(sPhaseName, null)
     */
    public void processPhase(String sPhaseName)
    {
        processPhase(sPhaseName, null);
    }

    /**
     * Process next phase (uses invokeLater to avoid any
     * possible swing locking issues.  Any params passed
     * in are added to the GamePhase's list of parameters,
     * possibly overridding default values permanently.
     */
    public void processPhase(String sPhaseName, TypedHashMap params)
    {
        processPhase(sPhaseName, params, true);
    }

    /**
     * Same as above, but specify history flag (overrides history setting in gamedef.xml)
     */
    public void processPhase(String sPhaseName, TypedHashMap params, boolean bHistory)
    {
        SwingUtilities.invokeLater(new ProcessPhaseRunnable(sPhaseName, params, bHistory));
    }

    /**
     * Runnable for processing phase later in swing loop
     */
    private class ProcessPhaseRunnable implements Runnable
    {
        String _sPhaseName;
        TypedHashMap _params;
        boolean _bHistory;

        private ProcessPhaseRunnable(String sPhaseName, TypedHashMap params, boolean bHistory)
        {
            _sPhaseName = sPhaseName;
            _params = params;
            _bHistory = bHistory;
        }

        public void run()
        {
            _processPhase(_sPhaseName, _params, _bHistory);
        }
    }

    /**
     * Process given phase now forcing history to false.
     * Returns phase after start() done.  Typically used to display modal dialogs
     * based on DialogPhase
     */
    public Phase processPhaseNow(String sPhaseName, TypedHashMap params)
    {
        return _processPhase(sPhaseName, params, false);
    }

    // stores phase that should be done after registation is complete
    private String TODOphase_;
    private TypedHashMap TODOparams_;
    private boolean TODOhistory_;

    /**
     * Process the TO-DO stored phase
     */
    public void processTODO()
    {
        if (TODOphase_ != null)
        {
            String ph = TODOphase_;
            TODOphase_ = null;
            TypedHashMap pa = TODOparams_;
            TODOparams_ = null;
            boolean b = TODOhistory_;
            TODOhistory_ = false;

            // process phase that we were going to do before registering
            // and notify engine we are doing so
            engine_.processingTODO(this);
            processPhase(ph, pa, b);
        }
    }

    /**
     * Return true if has TO-DO to process
     */
    public boolean hasTODO()
    {
        return TODOphase_ != null;
    }

    /**
     * process phase actual logic
     */
    private Phase _processPhase(String sPhaseName, TypedHashMap params, boolean bHistory)
    {
        if ((engine_.isBDemo() || engine_.isActivationNeeded()) && TODOphase_ != null)
        {
            logger.warn("Skipping " + sPhaseName + " because TODO phase is not null: " + TODOphase_);
            return null;
        }

        try
        {
            // force startmenu params to load (class is hardcoded below to prevent
            // tampering with gamedef.xml file)
            if (engine_.bExpired_)
            {
                sPhaseName = "StartMenu";
                params = new TypedHashMap();
                params.setBoolean(StartMenu.PARAM_EXPIRED, Boolean.TRUE);
            }
            else if (engine_.isActivationNeeded())
            {
                // if registration was void, to-do phase should be the start menu
                if (engine_.isActivationVoided())
                {
                    TODOphase_ = "StartMenu";
                    TODOparams_ = null;
                    TODOhistory_ = true;
                }
                else
                {
                    TODOphase_ = sPhaseName;
                    TODOparams_ = params;
                    TODOhistory_ = bHistory;
                }
                sPhaseName = "Activate";
                params = null;
                bHistory = false;
            }
            else if (engine_.isBDemo())
            {
                TODOphase_ = sPhaseName;
                TODOparams_ = params;
                TODOhistory_ = bHistory;
                sPhaseName = "Demo";
                params = null;
                bHistory = false;
            }

            GamePhase phase = engine_.getGamedefconfig().getGamePhases().get(sPhaseName);
            ApplicationError.assertNotNull(phase, "GamePhase not found", sPhaseName);

            return createAndStartPhase(phase, params, bHistory, false);
        }
        catch (ApplicationError ae)
        {
            logger.warn("GameContext - ApplicationError caught processing phase " + sPhaseName);
            switch (ae.getErrorCode())
            {
                case ErrorCodes.ERROR_NULL:
                case ErrorCodes.ERROR_ASSERTION_FAILED:
                case ErrorCodes.ERROR_UNEXPECTED_EXCEPTION:
                case ErrorCodes.ERROR_CODE_ERROR:
                    logger.warn(ae.toString());
                    logger.warn(Utils.formatExceptionText(ae));
                    break;

                default:
                    logger.warn(ae.toStringNoStackTrace());
                    break;
            }
            _handleProcessPhaseException(ae);
        }
        catch (Throwable e)
        {
            logger.warn("GameContext - Exception caught processing phase " + sPhaseName);
            logger.warn(Utils.formatExceptionText(e));
            _handleProcessPhaseException(e);
        }
        return null;
    }

    /**
     * subclass logging catch
     */
    private void _handleProcessPhaseException(Throwable e)
    {
        try
        {
            handleProcessPhaseException(e);
        }
        catch (Throwable t)
        {
            logger.warn("GameContext - Exception caught handling above error");
            logger.warn(Utils.formatExceptionText(t));
        }
    }

    /**
     * override for subclass logging
     */
    protected void handleProcessPhaseException(Throwable e)
    {
    }

    /**
     * Create phase and start it.
     */
    private Phase createAndStartPhase(GamePhase gamephase, TypedHashMap params, boolean bHistory, boolean bAvoidRecursion)
            throws ApplicationError
    {
        // window phase - run in new (or existing) window [bNewWindow used to prevent infinite loops]
        if (gamephase.isWindow() && !bAvoidRecursion)
        {
            // name, get title and starting width/height
            String sWindowName = gamephase.getWindowName();
            boolean bMulti = gamephase.getBoolean("window-multi", false);

            // look for existing context if not multi-instance window
            GameContext context = null;
            if (!bMulti)
            {
                context = engine_.getContext(sWindowName);

                // if existing, bring it to front
                if (context != null) context.getWindow().toFront();
            }

            // no existing (or multi-instance), create new window
            if (context == null)
            {
                String sTitle = PropertyConfig.getStringProperty(gamephase.getString("window-title"), "msg.application.name");
                int nHeight = gamephase.getInteger("window-height", 400);
                int nWidth = gamephase.getInteger("window-width", 400);
                int nMinHeight = gamephase.getInteger("window-height-min", 25);
                int nMinWidth = gamephase.getInteger("window-width-min", 100);
                boolean bResizable = gamephase.getBoolean("window-resize", true);

                if (!TESTING(EngineConstants.TESTING_NO_EXTERNAL) && !frame_.isFullScreen())
                {
                    context = engine_.createGameContext(game_, sWindowName, nMinWidth, nMinHeight, false);
                    context.setParent(this);
                    EngineWindow window = context.getFrame();
                    window.init(gamephase, false, new Dimension(nWidth, nHeight), false, sTitle, bResizable);
                    engine_.contextInited(context);
                    window.display();
                }
                else
                {
                    context = engine_.createInternalGameContext(this, sWindowName, nWidth, nHeight);
                    context.setParent(this);
                    EngineDialog dialog = (EngineDialog) context.getWindow();
                    dialog.init(gamephase, sTitle, bResizable);
                    engine_.contextInited(context);
                    dialog.display(new GameContextInternalAdapter(context));
                }
            }

            if (context != this)
            {
                return context.createAndStartPhase(gamephase, params, bHistory, true);
            }
        }

        // if we have override params, clone phase
        if (params != null)
        {

            gamephase = (GamePhase) gamephase.clone();
            gamephase.putAll(params);
        }

        // get instance of phase
        Phase phase = getInstance(gamephase);

        // bug 212 - demo mode - if asking for a phase that is in demo,
        // which generally should not happen, then show start menu
        if (engine_.isDemo() && !phase.isUsedInDemo())
        {
            return _processPhase("StartMenu", null, true);
        }

        // store in history if the phase says too, and the
        // calling function wants it stored.  Note:  bHistory
        // is always true when a phase is normally run
        if (gamephase.isHistory() && bHistory)
        {
            pastPhases_.push(gamephase);
        }

        // notify phase of the current phase (non-transient) that
        // invoked them
        phase.setFromPhase(currentPhase_);

        // if this phase isn't transient, store it as the current phase
        // dialog phases tend to be driven by other phases.  Current
        // phase should be the last guiphase (that which changed the
        // base component) or the last phase invoked that is driving
        // responses to user interaction
        if (!gamephase.isTransient())
        {
            currentPhase_ = phase;
        }

        // store last loop phase
        if (phase instanceof GamePlayerLoopPhase)
        {
            lastLoopPhase_ = gamephase;
        }

        // start the phase
        //logger.debug("STARTING phase: " + gamephase.getName());
        phase.start();
        return phase;
    }

    /**
     * Return current phase (last non DialogPhase set)
     */
    public Phase getCurrentPhase()
    {
        return currentPhase_;
    }

    /**
     * Return current UI phase
     */
    public Phase getCurrentUIPhase()
    {
        return currentMainUIPhase_;
    }

    /**
     * removed given phase from cache and history (if on top)
     */
    public void removeCachedPhase(GamePhase gamephase)
    {
        if (!pastPhases_.empty() && pastPhases_.peek() == gamephase)
        {
            pastPhases_.pop();
        }

        cachedPhases_.remove(gamephase.getName());
    }

    /**
     * Use to simulate this phase being created so it is on history list
     */
    public void seedHistory(String sPhaseName)
    {
        GamePhase gamephase = engine_.getGamedefconfig().getGamePhases().get(sPhaseName);
        ApplicationError.assertNotNull(gamephase, "GamePhase not found", sPhaseName);
        if (gamephase.isHistory())
        {
            pastPhases_.push(gamephase);
        }
    }

    /**
     * Get number on history
     */
    public int getNumHistory()
    {
        return pastPhases_.size();
    }

    /**
     * Goto last phase stored in history (prior to current phase) - called
     * from PreviousPhase
     */
    void gotoPreviousPhase(int nStepsBack)
    {
        GamePhase phase = null;
        nStepsBack += 1; // need to get past top of stack which is current phase
        for (int i = 0; i < nStepsBack && !pastPhases_.empty(); i++)
        {

            phase = pastPhases_.pop();
            //logger.debug("pop: " + phase.getName());
        }

        if (phase != null)
        {
            //logger.debug("goto prev: " + phase.getName());
            processPhase(phase.getName(), null);
        }
        else
        {
            logger.warn("Not able to step back " + nStepsBack);
        }
    }

    /**
     * Goto last loop phase - called from PreviousLoopPhase
     */
    void gotoPreviousLoopPhase()
    {
        ApplicationError.assertNotNull(lastLoopPhase_, "No loop phase to go to");
        processPhase(lastLoopPhase_.getName(), null);
    }

    /**
     * finish whatever the curent phase is (unless the current phase is the
     * main ui phase - used in rare cases like BUG 268)
     */
    public void finishCurrentNonUIPhase()
    {
        // cleanup any dialogs lying around (if non-internal context)
        if (!isInternal())
        {
            frame_.removeAllDialogs();
            frame_.endModalLogged();
        }

        if (currentPhase_ != null && currentPhase_ != currentMainUIPhase_
            && !(currentPhase_ instanceof DialogPhase))
        {
            currentPhase_.finish();
            // don't null it (needed in places like OnlineActionConfirmation)
        }
    }

    // restart phase info
    String sRestartPhase_ = null;
    TypedHashMap restartParams_ = null;

    /**
     * restart
     */
    public void restart()
    {
        restart(engine_.getGamedefconfig().getStartPhaseName(), null);
    }

    /**
     * Restart.  Instead of restarting with normal default start phase,
     * use given phase.
     */
    public void restart(String sRestartPhase, TypedHashMap restartParams)
    {
        sRestartPhase_ = sRestartPhase;
        restartParams_ = restartParams;

        if (getGameManager() != null)
        {
            restartOnline();
        }
        else
        {
            restartNormal();
        }
    }

    /**
     * Clear everything to initial state and start with initial phase
     */
    private void restartOnline()
    {
        // stop online game manager (which calls restartNormal)
        GameManager mgr = getGameManager();
        if (mgr != null)
        {
            mgr.cleanup();
        }
    }

    /**
     * Clear everything to initial state and start with initial phase
     */
    public void restartNormal()
    {
        // cleanup
        clean();

        // reinit territories (clearing owners, pieces, etc.)
        if (engine_.gameconfig_ != null) engine_.gameconfig_.initTerritories(); // TODO: multi-game this needs to change

        // start at beginning phase
        processPhase(sRestartPhase_, restartParams_);
        sRestartPhase_ = null;
        restartParams_ = null;
    }

    /**
     * close this game context (also closes associated window)
     */
    public void close()
    {
        // close window
        if (!isInternal())
        {
            frame_.setVisible(false);
            frame_.cleanup();
        }
        else
        {
            dialog_.removeDialog();
        }

        // cleanup any dialogs, etc.
        clean();

        // remove from context list
        engine_.contextDestroyed(this);

        // allow gc to do work
        parent_ = null;
        frame_ = null;
        dialog_ = null;
        window_ = null;
        game_ = null;
        gameMgr_ = null;
        engine_ = null;
    }

    /**
     * cleanup phase (for restart or close)
     */
    void clean()
    {
        // cleanup any dialogs lying around
        if (!isInternal())
        {
            frame_.removeAllDialogs();
            frame_.endModalLogged();
        }

        // cleanup any modal loops


        // cleanup current phase
        if (currentPhase_ != null)
        {
            if (currentPhase_ instanceof DialogPhase)
            {
                // do nothing - removeAllDialogs() should have called finish()
            }
            else
            {
                currentPhase_.finish();
            }
        }

        // cleanup current ui phase
        if (currentMainUIPhase_ != null && currentMainUIPhase_ != currentPhase_)
        {
            currentMainUIPhase_.finish();
        }

        // cleanup any remaining cached phases
        Iterator<String> iter = cachedPhases_.keySet().iterator();
        String sName;
        Phase phase;
        while (iter.hasNext())
        {
            sName = iter.next();
            phase = cachedPhases_.get(sName);

            // skip any handled above
            if (phase == currentPhase_) continue;
            if (phase == currentMainUIPhase_) continue;
            if (phase instanceof DialogPhase) continue;

            phase.finish();
        }

        // refresh lists and phase stuff
        sSpecialSave_ = null;
        currentMainUIPhase_ = null;
        main_ = null;
        currentPhase_ = null;
        lastLoopPhase_ = null;
        cachedPhases_.clear();
        pastPhases_.clear();

        // cleanup current game
        setGame(null);
    }

    /**
     * Get instance of class associated with this phase
     */
    @SuppressWarnings({"unchecked"})
    private Phase getInstance(GamePhase gamephase) throws ApplicationError
    {
        String sName = gamephase.getName();
        Phase phase = null;

        // first see if it is in the cache

        phase = cachedPhases_.get(sName);
        // if exists, reset it
        if (phase != null)
        {
            //logger.debug("REUSING: " + phase.getGamePhase().getName());
            phase.reinit(gamephase);
        }
        // if new, create it
        else if (phase == null)
        {
            String sClass = gamephase.getClassName();
            try
            {
                Class<? extends Phase> cClass = gamephase.getClassObject();

                // force startmenu to load if expired (matches above)
                if (engine_.bExpired_)
                {
                    sClass = "com.donohoedigital.games.engine.StartMenu";
                    cClass = StartMenu.class;
                }
                else if (engine_.isActivationNeeded())
                {
                    sClass = "com.donohoedigital.games.engine.Activate";
                    cClass = Activate.class;
                }
                else if (engine_.isBDemo())
                {
                    sClass = "com.donohoedigital.games.engine.Demo";
                    cClass = Demo.class;
                }
                else if (sName.equals("License")) // BUG 198 - ensure license class used
                {
                    sClass = "com.donohoedigital.games.engine.License";
                    cClass = License.class;
                }

                if (cClass == null)
                {
                    throw new ApplicationError(ErrorCodes.ERROR_CLASS_NOT_FOUND,
                                               "Phase " + sName +
                                               " class not found: " + sClass,
                                               "Make sure class exists");
                }

                phase = cClass.newInstance();

                // otherwise init
                phase.init(engine_, this, gamephase);

                //logger.debug("CREATING: " + phase.getGamePhase().getName());
                if (gamephase.isCached())
                {
                    cachedPhases_.put(sName, phase);
                }
            }
            catch (ClassCastException ce)
            {
                throw new ApplicationError(ErrorCodes.ERROR_CLASS_NOT_FOUND,
                                           "The class (" + sClass + ") for phase " + sName +
                                           " does not implement Phase.",
                                           ce,
                                           "Make sure this class implements com.donohoedigital.games.engine.Phase " +
                                           Utils.formatExceptionText(ce));
            }
            catch (Exception e)
            {
                throw new ApplicationError(ErrorCodes.ERROR_UNEXPECTED_EXCEPTION,
                                           "The class (" + sClass + ") for phase " + sName +
                                           " could not be created",
                                           e,
                                           "Resolve the condition indicated by the exception");
            }
        }

        return phase;
    }

    ////
    //// Game save logic
    ////

    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        GameManager mgr = getGameManager();
        Game game = getGame();

        // game over - this phase overrides GameManager
        if (game.isGameOver())
        {
            return BasePhase.addNamedGameStateEntry(state, "GameOver");
        }
        else if (mgr != null)
        {
            return BasePhase.addNamedGameStateEntry(state, mgr.getPhaseName());
        }

        // not needed since we specifically set name of online game entry
//        if (lastLoopPhase_ == null)
//        {
//            return BasePhase.addEmptyGameStateEntry(state);
//        }

        Phase cached = null;

        // store last loop phase since that is all we support right now
        // except for special save case
        if (lastLoopPhase_ != null)
        {
            cached = cachedPhases_.get(lastLoopPhase_.getName());
        }

        // BUG 99/166 - allow save during War's DisplayPurchaseSummary
        if (cached == null && sSpecialSave_ != null)
        {
            return BasePhase.addNamedGameStateEntry(state, sSpecialSave_);
        }

        // else make sure we have a cached loop phase
        ApplicationError.assertNotNull(cached, "Cached phase not found", lastLoopPhase_);
        return cached.addGameStateEntry(state);
    }

    /**
     * used in cases where save done from other than a loop phase
     */
    private String sSpecialSave_;

    /**
     * special case saves
     */
    public void setSpecialSavePhase(String sName)
    {
        sSpecialSave_ = sName;
    }

    ///
    /// WindowListener
    ///

    /**
     * Class to handle window closing events plus state changes issues
     */
    private class GameContextWindowAdapter extends WindowAdapter
    {
        boolean bQuitOnClose;

        GameContextWindowAdapter(boolean bQuitOnClose)
        {
            this.bQuitOnClose = bQuitOnClose;
        }

        /**
         * Calls okayToClose(), which if returns true, then exit is called
         */
        @Override
        public void windowClosing(WindowEvent e)
        {
            if (bQuitOnClose)
            {
                engine_.quit();
            }
            else
            {
                close();
            }
        }
    }

    /**
     * ditto for internal frames
     */
    private static class GameContextInternalAdapter extends InternalFrameAdapter
    {
        GameContext context;

        GameContextInternalAdapter(GameContext context)
        {
            this.context = context;
        }

        /**
         * Detect when window close icon is pressed - activate associated button
         */
        @Override
        public void internalFrameClosing(InternalFrameEvent e)
        {
            context.close();
        }
    }
}
