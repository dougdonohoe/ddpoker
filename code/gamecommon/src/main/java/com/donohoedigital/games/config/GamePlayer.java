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
 * GamePlayer.java
 *
 * Created on November 21, 2002, 3:56 PM
 */

package com.donohoedigital.games.config;


import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.beans.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GamePlayer implements ObjectID
{
    protected static Logger logger = LogManager.getLogger(GamePlayer.class);
    
    /** by convention, host is 1st player added (id 0) */
    public static final int HOST_ID = 0;
    private  static boolean DEMO = false;

    /**
     * Set in demo mode
     */
    public static void setDemo()
    {
        DEMO = true;
    }

    // members
    protected int id_;
    protected String sName_;
    protected TypedHashMap info_ = new TypedHashMap(); // use this so order of elements remains same during marshalling
    private boolean bEliminated_ = false;
    private GameAI ai_;
    private boolean bCurrent_ = false; // used to indicate whether player is currently active
    boolean bObserver_;
    boolean bDemo_;

    // transient - not saved
    private long lastOnline_ = 0; // used to indicate last time player polled server in online game

    /**
     * Empty constructor needed for demarshalling
     */
    public GamePlayer() {
        bDemo_ = DEMO;
    }
    
    /** 
     * Creates a new instance of GamePlayer 
     */
    public GamePlayer(int id, String sName)
    {
        this();
        id_ = id;
        sName_ = sName;
    }

    /**
     * Return unique id for this player
     */
    public int getID()
    {
        return id_;
    }
    
    /**
     * Is this player the host of an online game?
     * True is id == HOST_ID
     */
    public boolean isHost()
    {
        return (id_ == HOST_ID);
    }
    
    /**
     * set last poll time
     */
    public void setLastPoll(long l)
    {
        lastOnline_ = l;
    }
    
    /**
     * Get last time online
     */
    public long getLastPoll()
    {
        return lastOnline_;
    }
    
    /**
     * Is this a computer controlled player?
     */
    public boolean isComputer()
    {
        return ai_ != null;
    }

    /**
     * Set GameAI
     */
    public void setGameAI(GameAI ai)
    {
        ai_ = ai;
    }

    /**
     * Get GameAI
     */
    public GameAI getGameAI()
    {
        return ai_;
    }
    
    /**
     * Get player's name
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Set player's name
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }
    
    /**
     * Is player eliminated from game?
     */
    public boolean isEliminated()
    {
        return bEliminated_;
    }
    
    /**
     * Set player as eliminated
     */
    public void setEliminated(boolean b)
    {
        Boolean old = (bEliminated_ ? Boolean.TRUE : Boolean.FALSE);
        Boolean nu = (b ? Boolean.TRUE : Boolean.FALSE);
        bEliminated_ = b;
        
        firePropertyChange("eliminated", old, nu); // values changed so let listeners know
    }
    
    /**
     * put the given name/object into the list
     */
    public void putInfo(String sName, Object oValue)
    {
        Object oOld = info_.get(sName);
        info_.put(sName, oValue);
        firePropertyChange(sName, oOld, oValue);
    }
    
    /**
     * Get the item with the given name
     */
    public Object getInfo(String sName)
    {
        return info_.get(sName);
    }
    
    /**
     * Remove the item with the given name
     */
    public void removeInfo(String sName)
    {
        Object oOld = info_.remove(sName);
        firePropertyChange(sName, oOld, null);
    }
    
    /**
     * Return player's name
     */
    public String toString()
    {
        return sName_;
    }
    
    /**
     * Long description of player for debugging
     */
    public String toStringLong()
    {
        return sName_ + " id="+id_+" ai is " + isComputer() + " info: " + info_;
    }
    
    /**
     * Compare two players
     */
    public int compareTo(Object o)
    {
        if (!(o instanceof GamePlayer)) return 0;
        
        GamePlayer gp = (GamePlayer) o;
        
        return id_ - gp.id_;
    }
    
    /**
     * Return whether we are the current player
     */
    public boolean isCurrentGamePlayer()
    {
        return bCurrent_;
    }
    
    /**
     * Set us as current player
     */
    public void setCurrentGamePlayer(boolean b)
    {
        bCurrent_ = b;
    }

    /**
     * @return Is this an observer?
     */
    public boolean isObserver()
    {
        return bObserver_;
    }

    /**
     *  Set as observer
     *
     * @param bObserver
     */
    public void setObserver(boolean bObserver)
    {
        bObserver_ = bObserver;
    }

    /**
     * Is player from a demo copy?
     */
    public boolean isDemo()
    {
        return bDemo_;
    }

    /**
     * Set whether player is demo copy.  Only
     * really used when creating player for online,
     * where the remote player may or may not be
     * a demo user.
     */
    public void setDemo(boolean b)
    {
        bDemo_ = b;
    }

    ////
    //// Game save logic
    ////
    
    /**
     * Return this player encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        SaveDetails details = state.getSaveDetails();

        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_PLAYER);
        state.addEntry(entry);
        entry.addToken(id_);
        entry.addToken(sName_);
        if (details.getSaveAI() == SaveDetails.SAVE_ALL)
        {
            entry.addToken(ai_);
        }
        else
        {
            entry.addToken((DataMarshal) null);
        }
        entry.addToken(bEliminated_);
        entry.addToken(bCurrent_);
        entry.addToken(bObserver_);
        entry.addToken(bDemo_);
        entry.addToken(bDirty_);
        
        addExtraToGameStateEntry(state, entry);
        
        NameValueToken.loadNameValueTokensIntoList(entry, info_);   
        return entry;
    }
    
    /**
     * Allow subclass to add extra items to entry
     */
    protected void addExtraToGameStateEntry(GameState state, GameStateEntry entry)
    {
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        SaveDetails details = state.getSaveDetails();
        boolean bDirtyLoad = details.getSavePlayers() == SaveDetails.SAVE_DIRTY;
        
        id_ = entry.removeIntToken();
        sName_ = entry.removeStringToken();
        if (details.getSaveAI() == SaveDetails.SAVE_ALL)
        {
            ai_ = (GameAI) entry.removeToken();
        }
        else
        {
            entry.removeToken();
        }
        bEliminated_ = entry.removeBooleanToken();
        bCurrent_ = entry.removeBooleanToken();
        bObserver_ = entry.removeBooleanToken();
        bDemo_ = entry.removeBooleanToken();
        boolean bDirty = entry.removeBooleanToken();
        
        // ignore bDirty flag on dirty load
        if (!bDirtyLoad)
        {
            bDirty_ = bDirty;
        }
        
        loadExtraFromGameStateEntry(state, entry);
        
        NameValueToken.loadNameValueTokensIntoMap(entry, info_);
        firePropertyChange(null, null, null); // values changed so let listeners know
    }
    
    /**
     * Allow subclass to get extra items from entry
     */
    protected void loadExtraFromGameStateEntry(GameState state, GameStateEntry entry)
    {
    }
    
    /**
     * Return getID()
     */
    public int getObjectID()
    {
        return getID();
    }
    
    
    ////
    //// Online game methods
    ////
    
    private boolean bDirty_ = false;
    
    public void setDirty(boolean b)
    {
        bDirty_ = b;
    }
    
    public boolean isDirty()
    {
        return bDirty_;
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
     * @see java.beans.PropertyChangeSupport
     */
    protected synchronized void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        if (changeSupport != null) {
            // BUG 251 - need to make sure this runs from swing loop because it
            // tends to trigger UI updates.  Also, need to sync since we use
            // final varibles
            if (SwingUtilities.isEventDispatchThread())
            {
                changeSupport.firePropertyChange(propertyName, oldValue, newValue);
            }
            else
            {
                SwingUtilities.invokeLater(
                    new Runnable() {
                        String prop = propertyName;
                        Object old = oldValue;
                        Object nu = newValue;
                        public void run() {
                            changeSupport.firePropertyChange(prop, old, nu);
                        }
                    }
                ); 
            }
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

    /**
     * Returns an array of all the <code>PropertyChangeListener</code>s
     * added to this Component with addPropertyChangeListener().
     *
     * @return all of the <code>PropertyChangeListener</code>s added or
     *         an empty array if no listeners have been added
     *
     * @see      #addPropertyChangeListener
     * @see      #removePropertyChangeListener
     * @see      #getPropertyChangeListeners(java.lang.String)
     * @see      java.beans.PropertyChangeSupport#getPropertyChangeListeners
     * @since    1.4
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
	if (changeSupport == null) {
	    return new PropertyChangeListener[0];
	}
	return changeSupport.getPropertyChangeListeners();
    }    

    /**
     * Returns an array of all the listeners which have been associated 
     * with the named property.
     *
     * @return all of the <code>PropertyChangeListeners</code> associated with
     *         the named property or an empty array if no listeners have 
     *         been added
     * @see #getPropertyChangeListeners
     * @since 1.4
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
	if (changeSupport == null) {
	    return new PropertyChangeListener[0];
	}
	return changeSupport.getPropertyChangeListeners(propertyName);
    }
}
