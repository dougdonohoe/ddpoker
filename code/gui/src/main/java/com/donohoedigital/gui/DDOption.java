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
 * DDOption.java
 *
 * Created on April 16, 2003, 1:42 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.*;

/**
 *
 * @author  donohoe
 */
public abstract class DDOption extends DDPanel implements MouseListener
{
    //static Logger logger = LogManager.getLogger(DDOption.class);
    
    protected String sName_;
    protected String sOrigName_;
    protected String STYLE;
    protected Preferences prefs_;
    protected TypedHashMap map_;
    protected boolean bIgnored_ = false;

    /**
     * Creates a new instance of DDOption.  If sPrefNode is null,
     * then a DummyPref is used, which is useful if one wants to
     * use the option widgets without saving to prefs (i.e. for layout)
     */
    public DDOption(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map)
    {
        sName_ = sName;
        sOrigName_ = sName;
        STYLE = sStyle;
        prefs_ = getOptionPrefs(sPrefNode);
        map_ = map;
        addMouseListener(this);
    }
    
    /**
     * Set the map used
     */
    public void setMap(TypedHashMap map)
    {
        map_ = map;
    }
    
    /**
     * Set the name (special cases only like in TournamentProfileDialog (poker))
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }
    
    /**
     * Get name
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Return Preferences node as used by DDOption components
     */
    public static Preferences getOptionPrefs(String sPrefNode)
    {
        if (sPrefNode == null) return new DummyPref();
        return Prefs.getUserPrefs(Prefs.NODE_OPTIONS +sPrefNode);
    }
    
    /**
     * Return label for this component
     */
    public String getLabel()
    {
        return PropertyConfig.getMessage("option." + sOrigName_ + ".label");
    }
    
    /**
     * Return left label for this component
     */
    public String getLeftLabel()
    {
        return PropertyConfig.getStringProperty("option."+sOrigName_+".left", null, false);
    }
    
    /**
     * Return left gap for this component
     */
    public int getLeftGap()
    {
        return PropertyConfig.getIntegerProperty("option."+sOrigName_+".leftgap", 5);
    }
    
    /**
     * Return key used for default value
     */
    public String getDefaultKey()
    {
        return GetDefaultKey(sOrigName_);
    }
    
    /**
     * Return key used for default value
     */
    public static String GetDefaultKey(String sName)
    {
        return "option." + sName + ".default";
    }

    /**
     * Get the swing component used for the label
     */
    public JComponent getLabelComponent()
    {
        return null;
    }

    /**
     * Set the component enabled
     */
    public abstract void setEnabled(boolean b);

    /**
     * Set enabled when embedded in another option (e.g., radio)
     */
    public void setEnabledEmbedded(boolean b)
    {
        setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public abstract boolean isEnabled();
    
    /**
     * reset to default value
     */
    public abstract void resetToDefault();

    /**
     * reset to value stored in preferences
     */
    public abstract void resetToPrefs();

    /**
     * reset to value in map
     */
    public abstract void resetToMap();
    
    /**
     * save widget value to map
     */
    public abstract void saveToMap();
    
    /**
     * Are the contents of the option currently valid?  Returns true
     * by default
     */
    public boolean isValidData()
    {
        return true;
    }
    
    /**
     * set display only
     */
    public void setDisplayOnly(boolean b)
    {
        // nothing to do here, optional to implement
    }

    /**
     * Set ignored when getting options list in components
     */
    public void setIgnored(boolean b)
    {
        bIgnored_ = b;
    }

    /**
     * Should this be ignored when using GuiUtils.getDDOptions()?
     */
    public boolean isIgnored()
    {
        return bIgnored_;
    }

    // small perf improvement
    private static StringBuilder sbHelp = new StringBuilder();
    /**
     * when get mouse entered, set help text
     */
    public void mouseEntered(MouseEvent e) 
    {
        sbHelp.setLength(0);
        sbHelp.append("option.");
        sbHelp.append(sOrigName_);
        sbHelp.append(".help");
        String sHelp = PropertyConfig.getStringProperty(sbHelp.toString(), 
                                                            null, false);
        GuiUtils.getHelpManager(this).setHelpMessage(sHelp);
    }

    /**
     * Empty
     */
    public void mouseExited(MouseEvent e) {}    
    
    /**
     * Empty 
     */
    public void mouseClicked(MouseEvent e) {}
    
    /**
     * Empty 
     */
    public void mousePressed(MouseEvent e) {}
    
    /**
     * Empty 
     */
    public void mouseReleased(MouseEvent e) {}
    
    // events
    protected EventListenerList eventList = new EventListenerList();
    private transient ChangeEvent changeEvent;
    
   /**
     * Adds a listener to the list that is notified each time a change
     * to the model occurs.  The source of <code>ChangeEvents</code> 
     * delivered to <code>ChangeListeners</code> will be this 
     * <code>DDNumberSpinner</code>.
     * 
     * @param listener the <code>ChangeListener</code> to add
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener listener) {
        eventList.remove(ChangeListener.class, listener); // make sure not there already
        eventList.add(ChangeListener.class, listener);
    }
    
    /**
     * Removes a <code>ChangeListener</code> from this spinner.
     *
     * @param listener the <code>ChangeListener</code> to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener listener) {
        eventList.remove(ChangeListener.class, listener);
    }


    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this DDNumberSpinner with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return eventList.getListeners(ChangeListener.class);
    }


    /**
     * Sends a <code>ChangeEvent</code>, whose source is this 
     * <code>DDNumberSpinner</code>, to each <code>ChangeListener</code>.  
     * When a <code>ChangeListener</code> has been added 
     * to the spinner, this method method is called each time 
     * a <code>ChangeEvent</code> is received from the model.
     * 
     * @see #addChangeListener
     * @see #removeChangeListener
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        Object[] listeners = eventList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }
    
    /**
     * Used when null node passed in
     */
    private static class DummyPref extends Preferences
    {
        public DummyPref()
        {
        }
        
        public String toString() {
            return "Dummy";
        }
        
        public String absolutePath() {
            return "Dummy";
        }
        
        public void addNodeChangeListener(NodeChangeListener ncl) {
        }
        
        public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        }
        
        public String[] childrenNames() throws BackingStoreException {
            return null;
        }
        
        public void clear() throws BackingStoreException {
        }
        
        public void exportNode(OutputStream os) throws IOException, BackingStoreException {
        }
        
        public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
        }
        
        public void flush() throws BackingStoreException {
        }
        
        public String get(String key, String def) {
            return def;
        }
        
        public boolean getBoolean(String key, boolean def) {
            return def;
        }
        
        public byte[] getByteArray(String key, byte[] def) {
            return def;
        }
        
        public double getDouble(String key, double def) {
            return def;
        }
        
        public float getFloat(String key, float def) {
            return def;
        }
        
        public int getInt(String key, int def) {
            return def;
        }
        
        public long getLong(String key, long def) {
            return def;
        }
        
        public boolean isUserNode() {
            return true;
        }

        public String[] keys() throws BackingStoreException {
            return null;
        }

        public String name() {
            return "Dummy";
        }
        
        public Preferences node(String pathName) {
            return null;
        }
        
        public boolean nodeExists(String pathName) throws BackingStoreException {
            return false;
        }
        
        public Preferences parent() {
            return null;
        }
        
        public void put(String key, String value) {
        }
        
        public void putBoolean(String key, boolean value) {
        }
        
        public void putByteArray(String key, byte[] value) {
        }
        
        public void putDouble(String key, double value) {
        }
        
        public void putFloat(String key, float value) {
        }
        
        public void putInt(String key, int value) {
        }
        
        public void putLong(String key, long value) {
        }
        
        public void remove(String key) {
        }
        
        public void removeNode() throws BackingStoreException {
        }
        
        public void removeNodeChangeListener(NodeChangeListener ncl) {
        }
        
        public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        }
        
        public void sync() throws BackingStoreException {
        }
        
    }
    
}
