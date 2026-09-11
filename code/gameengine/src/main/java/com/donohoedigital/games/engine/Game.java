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
 * GameConfig.java
 *
 * Created on November 21, 2002, 3:51 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import org.apache.logging.log4j.*;

import javax.swing.event.*;
import java.beans.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Game extends TypedHashMap implements GameInfo, GamePlayerList, GameObserverList
{
    static Logger logger = LogManager.getLogger(Game.class);

    // List of players.  Copy-on-write because these are mutated from threads which do
    // not own the game: a message thread as players join, and the EDT when the host
    // switches someone between player and observer (OnlineManager.switchPlayer()), while
    // the game thread is reading them.  A plain ArrayList made every for-each over either
    // list a ConcurrentModificationException waiting to happen, and every ranking path
    // walks one of them.  The iterator of a CopyOnWriteArrayList is a snapshot, taken
    // without allocating or locking, which is exactly what those readers want.
    //
    // The cost is that each add or remove copies the backing array.  That is nothing on
    // the join, quit and switch paths; where a whole field is added at once, add it at
    // once - see PokerGame.addPlayers().
    protected final List<GamePlayer> players_ = new CopyOnWriteArrayList<>();
    private final List<GamePlayer> observers_ = new CopyOnWriteArrayList<>();

    // last time this game was saved, used this GameState
    private GameState lastSave_ = null;
    
    // transient values
    private final GameContext context_;
    private String sBeginPhase_ = null;
    private TypedHashMap params_ = null;
    private boolean bFinished_ = false;

    // properties for storage
    private static final String PROP_PASSWORD = "pass";
    private static final String PROP_ONLINE_GAMEID = "online";
    private static final String PROP_OPTIONS = "options";
    private static final String PROP_TURN_NUM = "turn";
    private static final String PROP_GAME_OVER_STORAGE = "gameover";
    private static final String PROP_OBSERVER_SEQ = "obs-seq";
    private static final String PROP_PLAYER_SEQ = "ply-seq";

    /**
     * Name used in PropertyChangeEvents when players
     * added/removed from this game.  On an add event, the
     * added player is passed as the "new" property.  On a
     * remove event, the removed player is passed as the
     * "old" property.
     */
    public static final String PROP_PLAYERS = "_players_";

    /**
     * Name used in PropertyChangeEvents when players
     * list is cleared during a game load.  The "old" property
     * is the old num of players and the "new" property is the new num players.
     */
    public static final String PROP_PLAYERS_LIST = "_playerslist_";

    /**
     * Name used in PropertyChangeEvents when observers
     * added/removed from this game. On an add event, the
     * added observer is passed as the "new" property.  On a
     * remove event, the removed observer is passed as the
     * "old" property.
     */
    public static final String PROP_OBSERVERS = "_observers_";

    /**
     * Name used in PropertyChangeEvents when observers
     * list is cleared during a game load.  The "old" property
     * is the old num of observers and the "new" property is the new num observers.
     */
    public static final String PROP_OBSERVERS_LIST = "_observerslist_";

    /**
     * Name used in PropertyChangeEvents when game loaded.  The GameState
     * used during the load is passed as the "old" property value and
     * the Game itself as the "new".
     */
    public static final String PROP_GAME_LOADED = "_load_";

    /**
     * Name used in PropertyChangeEvents when game over.  Both "old" and
     * "new" are null
     */
    public static final String PROP_GAME_OVER = "_gameover_";

    /**
     * Creates a new instance of Game
     */
    public Game(GameContext context)
    {
        context_ = context;
        setObject(PROP_OPTIONS, new DMTypedHashMap());
        setGameOver(false);
    }

    /**
     * Return context
     */
    public GameContext getGameContext()
    {
        return context_;
    }

    /**
     * cleanup
     */
    public synchronized void finish()
    {
        bFinished_ = true;
        players_.clear();
        observers_.clear();
        changeSupport = null;
    }

    /**
     * True if engine is closing down this game
     */
    public boolean isFinished()
    {
        return bFinished_;
    }

    /**
     * Is this game done?
     */
    public boolean isGameOver()
    {
        Boolean b = getBoolean(PROP_GAME_OVER_STORAGE);
        if (b == null) return false;
        return b;
    }
    
    /**
     * Set game over
     */
    public void setGameOver(boolean b)
    {
        setBoolean(PROP_GAME_OVER_STORAGE, b ? Boolean.TRUE : Boolean.FALSE);
        if (b) firePropertyChange(PROP_GAME_OVER, null, null);
    }
    
    /**
     * is this an online gam?
     */
    public boolean isOnlineGame()
    {
        return (getOnlineGameID() != null);
    }
    
    /**
     * Set id for online game
     */
    public void setOnlineGameID(String sID)
    {
        setString(PROP_ONLINE_GAMEID, sID);
    }
    
    /**
     * Set temp online id for use when creating new online game
     */
    public void setTempOnlineGameID()
    {
        // can be anything since check is for non-null in isOnlineGame()
        setString(PROP_ONLINE_GAMEID, "new-temp-id");
    }
    
    /**
     * Get password for 
     */
    public String getOnlineGameID()
    {
        return getString(PROP_ONLINE_GAMEID);
    }    
  
    /**
     * Set turn # of game
     */
    public void setTurn(int num)
    {
        Integer old = getInteger(PROP_TURN_NUM);
        setInteger(PROP_TURN_NUM, num);
        firePropertyChange(PROP_TURN_NUM, old, num);
    }
    
    /**
     * Get turn # for game
     */
    public int getTurn()
    {
        return getInteger(PROP_TURN_NUM, 1);
    }

    /**
     * Get player/observer id seq
     */
    private int getNextID(String sKey)
    {
        int next;
        Integer id = getInteger(sKey);
        if (id == null) next = GamePlayer.HOST_ID;
        else next = id + 1;
        setInteger(sKey, next);
        return next;
    }

    /**
     * Get next seq id for player ids
     */
    public int getNextPlayerID()
    {
        return getNextID(PROP_PLAYER_SEQ);
    }

    /**
     * Get next seq id for observer ids
     */
    public int getNextObserverID()
    {
        return getNextID(PROP_OBSERVER_SEQ);
    }

    /**
     * Set password for online game
     */
    public void setOnlinePassword(String sPass)
    {
        setString(PROP_PASSWORD, sPass);
    }
    
    /**
     * Get password for 
     */
    public String getOnlinePassword()
    {
        return getString(PROP_PASSWORD);
    }
        
    /**
     * Return hash map of game options
     */
    public DMTypedHashMap getGameOptions()
    {
        return (DMTypedHashMap) getObject(PROP_OPTIONS);
    }
    
    /**
     * store last save file for this game (either from initial load or a save)
     */
    private void setLastGameState(GameState state)
    {
        lastSave_ = state;
    }
    
    /**
     * associate this state with this game
     */
    public void setGameState(GameState state)
    {
        // different method name in case we ever need to do verification here
        setLastGameState(state);
    }
    
    /**
     * Get last save file
     */
    public GameState getLastGameState()
    {
        return lastSave_;
    }
    
    /**
     * Clear all players
     */
    public void clearPlayerList(int nNewCount)
    {
        int nOldCount = players_.size();
        players_.clear();
        firePropertyChange(PROP_PLAYERS_LIST, nOldCount, nNewCount);
    }
    
    /**
     * Return whether this player is in the game
     */
    public boolean containsPlayer(GamePlayer player)
    {
        return players_.contains(player);
    }
    
    /**
     * Add player to game.  Added player passed as "new" value in
     * PROP_PLAYERS event.
     */
    public void addPlayer(GamePlayer player)
    {
        players_.add(player);
        firePropertyChange(PROP_PLAYERS, null, player);
    }
    
    /**
     * Remove player from game.  Removed player passed as "old" value in
     * PROP_PLAYERS event.
     */
    public void removePlayer(GamePlayer player)
    {
        players_.remove(player);
        firePropertyChange(PROP_PLAYERS, player, null);
    }
    
    /**
     * Return number of players
     */
    public int getNumPlayers()
    {
        return players_.size();
    }

    /**
     * Return number of human players
     */
    public int getNumHumans()
    {
        int nCnt = 0;
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            if (!getPlayerAt(i).isComputer()) nCnt++;
        }
        return nCnt;
    }

    /**
     * Return player at given index
     */
    public GamePlayer getPlayerAt(int nIndex)
    {
        return players_.get(nIndex);
    }
    
    /**
     * Return ID of player at given index
     */
    public int getPlayerIdAt(int i)
    {
        return getPlayerAt(i).getID();
    }

    /**
     * Add observer to game.  Added observer passed as "new" value in
     * PROP_OBSERVERS event.  setObserver(true) called on the player.
     */
    public void addObserver(GamePlayer player)
    {
        if (!observers_.contains(player))
        {
            player.setObserver(true);
            observers_.add(player);
            firePropertyChange(PROP_OBSERVERS, null, player);
        }
    }

    /**
     * Remove observer from game.  Removed observer passed as "old" value in
     * PROP_OBSERVERS event.  setObserver(false) called on the player.
     */
    public void removeObserver(GamePlayer player)
    {
        if (observers_.contains(player))
        {
            player.setObserver(false);
            observers_.remove(player);
            firePropertyChange(PROP_OBSERVERS, player, null);
        }
    }

    /**
     * Return number of observers
     */
    public int getNumObservers()
    {
        return observers_.size();
    }

    /**
     * Return observer at given index
     */
    public GamePlayer getObserverAt(int nIndex)
    {
        return observers_.get(nIndex);
    }

    /**
     * Clear all players
     */
    public void clearObserverList(int nNewCount)
    {
        int nOldCount = observers_.size();
        observers_.clear();
        firePropertyChange(PROP_OBSERVERS_LIST, nOldCount, nNewCount);
    }

    /**
     * Return whether this player is in the game
     */
    public boolean containsObserver(GamePlayer player)
    {
        return observers_.contains(player);
    }


    /**
     * Set all players dirty
     */
    public void setAllPlayersDirty(boolean bDirty)
    {
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            getPlayerAt(i).setDirty(bDirty);
        }
    }

    /**
     * Set all observers dirty
     */
    public void setAllObserversDirty(boolean bDirty)
    {
        for (int i = getNumObservers() - 1; i >= 0; i--)
        {
            getObserverAt(i).setDirty(bDirty);
        }
    }

    /**
     * Return player with given id (not necessarily same as getPlayerAt,
     * since the order can change.  Returns null if no player exists.
     */
    public GamePlayer getPlayerFromID(int nID)
    {
        return getPlayerFromID(nID, false);
    }

    /**
     * Get player with given ID.
     *
     * @param nID               player id
     * @param bSearchObservers  search observer list if true
     * @return                  player with given id
     */
    public GamePlayer getPlayerFromID(int nID, boolean bSearchObservers)
    {
        GamePlayer player;
        int nNum = players_.size();
        for (int i = 0; i < nNum; i++)
        {
            player = getPlayerAt(i);
            if (player.getID() == nID) return player;
        }

        nNum = getNumObservers();
        for (int i = 0; bSearchObservers && i < nNum; i++)
        {
            player = getObserverAt(i);
            if (player.getID() == nID) return player;
        }
        return null;
    }
    
    /**
     * Return whether player with given id is eliminated
     */
    public boolean isEliminated(int id)
    {
        return getPlayerFromID(id).isEliminated();
    }

    /**
     * String representation of game for debug
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("Game players= ");
        
        for (int i = 0; i < getNumPlayers(); i++)
        {
            if (i > 0) sb.append(", ");
            sb.append(getPlayerAt(i).toString());
        }
        sb.append(" PARAMS= ");
        sb.append(super.toString());
        return sb.toString();
    }
    
    //
    // Save Game logic
    //
    
    /**
     * Record the state of this game for use in online play
     * (no ext required)
     */
    public GameState newGameState(String sName)
    {
        return newGameState(sName, null);
    }
    
    /** 
     * Record the state of this game into the given game state
     */
    public GameState newGameState(String sName, String sExt)
    {
        // create game state
        if (sExt == null)
        {
            return GameStateFactory.createGameState(sName, getDescription());
        }
        else
        {
            return GameStateFactory.createGameState(sName, getBegin(), sExt, getDescription());
        }
    }
    
    /**
     * Get begin name of save file
     */
    public String getBegin()
    {
        if (isOnlineGame()) return GameState.ONLINE_GAME_BEGIN;
        return GameState.GAME_BEGIN;
    }
    
    /**
     * Get description from game players
     */
    public String getDescription()
    {
        // created description from players names
        StringBuilder sb = new StringBuilder();
        int nNum = getNumPlayers();

        for (int i = 0; i < nNum; i++)
        {
            if (i > 0) sb.append(", ");
            sb.append(getPlayerAt(i).getName());
        }

        return sb.toString();
    }
    
    /**
     * Get default version of SaveDetails for use on
     * normal save.  Can be overriden
     */
    public SaveDetails getSaveDetails(int nInit)
    {
        return new SaveDetails(nInit);
    }
    
    /**
     * Save game to state.
     */
    public void saveGame(GameState state)
    {
        _saveGame(state, getSaveDetails(SaveDetails.SAVE_ALL));
    }
    
    /**
     * Save game, only saving information specified in SaveDetails
     */
    public void saveGame(GameState state, SaveDetails details)
    {
        _saveGame(state, details);
    }
    
    /**
     * Save game to state.
     */
    private synchronized void _saveGame(GameState state, SaveDetails details)
    {   
        // make sure not saving a game that is finished
        if (bFinished_)
        {
            logger.warn("Skipping save of finished game {} to {}", state.getGameName(), state.getFile());
            return;
        }
        
        // 1st, init for saving (stores hash values in this step if !bDirty)
        state.initForSave(this, details);
        
        // 2nd, allow game to store additional data
        if (details.getSaveGameSubclassData() != SaveDetails.SAVE_NONE)
        {
            saveSubclassData(state);
        }
        
        // 3rd, store players, observers
        state.savePlayers(this);
        state.saveObservers(this);

        // 4th, store current phase info
        if (details.getSaveCurrentPhase() == SaveDetails.SAVE_ALL)
        {
            context_.addGameStateEntry(state);
        }
        
        // 5th, store territories and their contents
        Territory[] territories = Territory.getTerritoryArrayCached();
        state.saveTerritories(territories);
        
        // 6th, store custom data
        state.saveCustomData();
    }
    
    /**
     * game (or subclass) can save other data (not stored in hash values)
     */
    protected void saveSubclassData(GameState state)
    {
    }
    
    /**
     * Write the given GameState out and store it as our last save.
     * All entries are removed after writing (state.resetAfterReadWrite())
     */
    public synchronized void writeGame(GameState state)
    {
        // make sure not writing a game that is finished
        if (bFinished_)
        {
            logger.warn("Skipping write of finished game {} to {}", state.getGameName(), state.getFile());
            return;
        }
        state.write();
        state.resetAfterWrite();
        setLastGameState(state);
    }
    
    /**
     * Save & Write last saved game (must have previously called writeGame
     * or setLastGameState() such that getLastGameState() isn't null)
     */
    public synchronized void saveWriteGame()
    {
        GameState state = getLastGameState();
        ApplicationError.assertNotNull(state, "No last game state");
        saveGame(state);
        writeGame(state);
    }
    
    /**
     * Can save?
     */
    public boolean canSave()
    {
        return getLastGameState() != null;
    }    
    
    /**
     * Save gave if auto save setting on (and not online game)
     */
    public void autoSave()
    {
        // don't auto save during online games
        if (isOnlineGame()) return;
        
        // get engine
        GameEngine engine = GameEngine.getGameEngine();
        if (engine == null) return;
        
        // get prefs and check them
        EnginePrefs prefs = engine.getPrefsNode();
        if (prefs.getBoolean(EngineConstants.PREF_AUTOSAVE, false) && getLastGameState() != null)
        {
            //logger.debug("Auto saving...");
            saveWriteGame();
        }
    }
    
    /**
     * Load the game
     */
    public void loadGame(GameState state, boolean bProcessPhase)
    {
        _loadGame(state, bProcessPhase);
    }
        
    /** 
     * load 
     */
    private synchronized void _loadGame(GameState state, boolean bProcessPhase)
    {
        // make sure not loading a game that is finished
        if (bFinished_)
        {
            logger.warn("Skipping load of finished game {} to {}", state.getGameName(), state.getFile());
            return;
        }
        
        // phase
        GameStateEntry nextPhase = null;
                
        // PHASE 0 - prepopulate ids
        Territory[] territories = Territory.getTerritoryArrayCached();
        state.prepopulateIds(this, territories, this, this);
        
        // PHASE 1 - create all objects
        state.read(true);
        
        // PHASE 2 - read remaining tokens
        state.finishParsing();
        
        // PHASE 2.5 - get SaveDetails
        SaveDetails details = state.getSaveDetails();
        
        // PHASE 3 - restore our hash value
        state.initForLoad(this);
        
        // PHASE 4 - repeat process above with players, gamestate, territories custom data, etc.
        
            // game data
            if (details.getSaveGameSubclassData() != SaveDetails.SAVE_NONE)
            {
                loadSubclassData(state);
            }
            
            // 1st players and observers
            state.loadPlayers(this);
            state.loadObservers(this);

            // 2nd current phase, save for later
            if (details.getSaveCurrentPhase() == SaveDetails.SAVE_ALL) nextPhase = state.removeEntry();

            // 3rd territories
            state.loadTerritories(); // 3rd, load territories and contents

            // 4th custom data
            state.loadCustomData(); // 4th load custom data;

        // PHASE 5 - cleanup
        state.resetAfterRead(true);
        
        // PHASE 6 - save this state (correct save dir if not same)
        if (state.getFile() != null)
        {
            if (!state.isFileInSaveDirectory())
            {
                state.resetFileSaveDir();
            }
            
            setLastGameState(state);
        }
        
        // PHASE 6.5 Notify subclass loaded so any final steps can take place
        gameLoaded(state);
        
        // PHASE 6.6 Fire event for any listeners.  We pass the GameState
        // as the "old" property value and the Game as the "new".
        firePropertyChange(PROP_GAME_LOADED, state, this);
        
        // PHASE 7 - show game board and set next phase
        // the begin phase is always the same - we pass in params which tell it
        // the next phase to process after showing the board
        // if params_ is null, that means no phase to process
        if (nextPhase != null)
        {
            params_ = BasePhase.getPhaseFromGameStateEntry(nextPhase);
            sBeginPhase_ = state.getBeginGamePhase(context_, this, params_);
            if (bProcessPhase)
            {
                processStartPhase();
            }
        }
    }
    
    /**
     * game (or subclass) can load other data (not stored in hash values)
     */
    protected void loadSubclassData(GameState state)
    {
    }
    
    /**
     * allow subclass to do final setup after load
     */
    protected void gameLoaded(GameState state)
    {
    }
    
    /**
     * Process the phase loaded from the last loadGame().
     * Can only be called once and only if loadGame is passed false.
     */
    public void processStartPhase()
    {
        if (sBeginPhase_ == null) return;
        
        if (params_ != null)
        {
            context_.processPhase(sBeginPhase_, params_);
        }
        
        params_ = null;
        sBeginPhase_ = null;
    }
    
    ////
    //// PropertyChangeListener support - modeled after JComponent
    ////
    
    private SwingPropertyChangeSupport changeSupport;
    
    /**
     * Supports reporting bound property changes.  If <code>oldValue</code>
     * and <code>newValue</code> are not equal and the
     * <code>PropertyChangeEvent</code> listener list isn't empty,
     * then fire a <code>PropertyChange</code> event to each listener.
     * This method has an overloaded method for each primitive type.  For
     * example, here's how to write a bound property set method whose
     * value is an integer:
     * <pre>
     * public void setFoo(int newValue) {
     *     int oldValue = foo;
     *     foo = newValue;
     *     firePropertyChange("foo", oldValue, newValue);
     * }
     * </pre>
     *
     * @param propertyName  the programmatic name of the property
     *		that was changed
     * @param oldValue  the old value of the property (as an Object)
     * @param newValue  the new value of the property (as an Object)
     * @see PropertyChangeSupport
     */
    protected synchronized void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list.
     * The listener is registered for all properties.
     * <p>
     * A <code>PropertyChangeEvent</code> will get fired in response
     * to setting a bound property, such as <code>setFont</code>,
     * <code>setBackground</code>, or <code>setForeground</code>.
     * <p>
     * Note that if the current component is inheriting its foreground,
     * background, or font from its container, then no event will be
     * fired in response to a change in the inherited property.
     *
     * @param listener  the <code>PropertyChangeListener</code> to be added
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }


    /**
     * Adds a <code>PropertyChangeListener</code> for a specific property.
     * The listener will be invoked only when a call on
     * <code>firePropertyChange</code> names that specific property.
     * <p>
     * If listener is <code>null</code>, no exception is thrown and no
     * action is performed.
     *
     * @param propertyName  the name of the property to listen on
     * @param listener  the <code>PropertyChangeListener</code> to be added
     */
    public synchronized void addPropertyChangeListener(
				String propertyName,
				PropertyChangeListener listener) {
	if (listener == null) {
	    return;
	}
	if (changeSupport == null) {
	    changeSupport = new SwingPropertyChangeSupport(this);
	}
	changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties.
     *
     * @param listener  the <code>PropertyChangeListener</code> to be removed
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }


    /**
     * Removes a <code>PropertyChangeListener</code> for a specific property.
     * If listener is <code>null</code>, no exception is thrown and no
     * action is performed.
     *
     * @param propertyName  the name of the property that was listened on
     * @param listener  the <code>PropertyChangeListener</code> to be removed
     */
    public synchronized void removePropertyChangeListener(
				String propertyName,
				PropertyChangeListener listener) {
	if (listener == null) {
	    return;
	}
	if (changeSupport == null) {
	    return;
	}
	changeSupport.removePropertyChangeListener(propertyName, listener);
    }
}
