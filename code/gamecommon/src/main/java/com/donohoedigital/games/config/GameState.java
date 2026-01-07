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
 * GameState.java
 *
 * Created on February 5, 2003, 1:03 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GameState extends MsgState implements SaveFile
{
    static Logger logger = LogManager.getLogger(GameState.class);
       
    public static final String GAME_BEGIN = "save";
    public static final String ONLINE_GAME_BEGIN = "online";

    // end of an entry signified by endline
    public static final char ENTRY_ENDLINE = '\n';
    
    // delegate
    private static GameStateDelegate delegate_ = null; // FIX: eliminate use of static here...
    
    // game state info
    private File file_;
    private byte[] savedata_;
    private String sName_;
    private String sDesc_;
    private List<GameStateEntry> entries_ = new ArrayList<GameStateEntry>();
    private TypedHashMap gamedata_;
    private SaveDetails details_;
    
    /**
     * Set the delegate used by all game state instances
     */
    public static void setDelegate(GameStateDelegate delegate)  // FIX: ick ... static
    {
        delegate_ = delegate;
    }
    
    /**
     * get delegate
     */
    public static GameStateDelegate getDelegate()
    {
        return delegate_;
    }
    
    /** 
     * Creates a new instance of GameState 
     * from an existing file
     */
    public GameState(File f, boolean bLoadHeader)
    {
        file_ = f;
        if (bLoadHeader) read(false);
    }
    
    /** 
     * Creates a new instance of GameState 
     * from a byte array
     */
    public GameState(byte[] data)
    {
        savedata_ = data;
    }
    
    /**
     * Create a new instance of GameState, given a name (stored in the file)
     * and an extension for the actual file.  File saved as
     * <sBegin>.NNNNNN.sExt where NNNNNN is the next available file number.
     * sDesc is a description of the game
     */
    public GameState(String sName, String sBegin, String sExt, String sDesc)
    {
        this(sName, GameConfigUtils.getSaveDir(), sBegin, sExt, sDesc);
    }

    /**
     * Create a new instance of GameState, given a name (stored in the file)
     * and an extension for the actual file.  File saved as
     * <sBegin>.NNNNNN.sExt where NNNNNN is the next available file number.
     * sDesc is a description of the game
     */
    public GameState(String sName, File fDir, String sBegin, String sExt, String sDesc)
    {
        sName_ = sName;
        sDesc_ = sDesc;
        // we refetch save list just to be sure we get correct last number
        int nNum = GameConfigUtils.getNextSaveNumber(getSaveFileList(fDir, sBegin, sExt));
        String sNum = GameConfigUtils.formatFileNumber(nNum);
        file_ = new File(fDir, sBegin + SaveFile.DELIM + sNum + SaveFile.DELIM + sExt);
    }

    /**
     * Create game with just name and description - for use for temporary
     * game states like in online play.
     */
    public GameState(String sName, String sDesc)
    {
        sName_ = sName;
        sDesc_ = sDesc;
    }
    
    /**
     * Change name
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }
    
    /**
     * Indicate whether the file associated with this game state is in the
     * save directory (might not be if loaded from email/web)
     */
    public boolean isFileInSaveDirectory()
    {
        if (file_ == null) return false;
        File dir = file_.getParentFile();
        File save = GameConfigUtils.getSaveDir();
        
        return dir.equals(save);
    }
    
    /**
     * Change file's location to save dir (used when file loaded
     * from some other location, like an email temp dir)
     */
    public void resetFileSaveDir()
    {
        if (file_ == null) return;
        file_ = new File(GameConfigUtils.getSaveDir(), file_.getName());
    }
    
    /**
     * Get SaveDetails
     */
    public SaveDetails getSaveDetails()
    {
        return details_;
    }
    
    /**
     * Set SaveDetails
     */
    public void setSaveDetails(SaveDetails details)
    {
        details_ = details;
    }

    /**
     * Store initial entry with game name
     */
    public void initForSave(TypedHashMap gamedata, SaveDetails details)
    {
        // make sure we are clear of past activity
        reset();
        
        // save details
        details_ = details;
        
        // first entry controlled by game state contains the name/description
        GameStateEntry entry = new GameStateEntry(this, null, ConfigConstants.SAVE_GAMENAME);
        entry.addToken(sName_);
        entry.addToken(sDesc_);
        entry.addToken(details_);
        if (details.getSaveGameHashData() != SaveDetails.SAVE_NONE)
        {
            NameValueToken.loadNameValueTokensIntoList(entry, gamedata);
        }
        addEntry(entry);
        
        // second entry contains the list of classes marshalled out
        GameStateEntry classNames = new GameStateEntry(this, null, ConfigConstants.SAVE_CLASSNAMES);
        setClassNames(classNames);
        addEntry(classNames);
    }
    
    /**
     * load hash data
     */
    public void initForLoad(TypedHashMap gamedata)
    {
        if (details_.getSaveGameHashData() != SaveDetails.SAVE_NONE)
        {
            gamedata.putAll(getGameData());
        }
    }
    
    /**
     * init this game state from the given GameStateEntry (get name/desc)
     */
    private void initFromGameStateEntry(GameStateEntry entry)
    {
        // match order of addition to entry, above
        sName_ = entry.removeStringToken();
        sDesc_ = entry.removeStringToken();
        details_ = (SaveDetails) entry.removeToken();
        gamedata_ = new TypedHashMap();
        NameValueToken.loadNameValueTokensIntoMap(entry, gamedata_);
    }
    
    /**
     * get hash values associated with this game state (after loading)
     */
    public TypedHashMap getGameData()
    {
        return gamedata_;
    }
    
    /**
     * Called when done read to free entries
     */
    public void resetAfterRead(boolean bCheckEmpty)
    {
        if (bCheckEmpty && entries_.size() > 0)
        {
            logger.warn("GameState resetAfterRead: " + sName_ + ": has " + entries_.size() + " entries left.");
            for (int i = 0; i < entries_.size(); i++)
            {
                GameStateEntry entry = entries_.get(i);
                logger.debug("Entry["+i+"]: " + entry.marshal(null));
            }
        }
        reset();
    }
    
    /**
     * Called after write to free entries
     */
    public void resetAfterWrite()
    {
        reset();
    }
    
    /**
     * Reset game state for use again
     */
    private void reset()
    {
        super.resetIds();

        // GameStateEntries
        entries_.clear();
    }
    
    /**
     * Return file associated with this game state
     */
    public File getFile()
    {
        return file_;
    }
    
    /**
     * Return number that is part of file name, or -1 if no file specified.
     */
    public int getFileNumber()
    {
        if (file_ == null) return -1;
        return GameConfigUtils.getFileNumber(file_);
    }
    
    /**
     * Set the name of the file and extension - this method creates
     * the appropriate file object in the correct directory.  Used
     * in online play, which uses the name of the online game instead
     * of the normal save names
     */
    public void setOnlineGameID(String sID, String sExt)
    {
        file_ = new File(GameConfigUtils.getSaveDir(), ONLINE_GAME_BEGIN + SaveFile.DELIM + sID + SaveFile.DELIM + sExt);
    }
        
    /**
     * Does this save file represent an online game?  Returns false
     * if no file defined
     */
    public boolean isOnlineGame()
    {
        if (file_ == null) return false;
        String sFileName = file_.getName();
        return sFileName.startsWith(GameState.ONLINE_GAME_BEGIN);
    }
    
    /**
     * write out game state to given file
     */
    public void write()
    {
        //logger.info("Saving " + file_.getName());
        
        // backup/temp file
        File bak = new File(file_.getAbsolutePath() + ".bak");
        File tmp = new File(file_.getAbsolutePath() + ".tmp");
        
        // remove old temp if it exists
        if (tmp.exists()) tmp.delete();
        
        // write to temp file
        Writer writer = ConfigUtils.getWriter(tmp);
        write(writer);
        ConfigUtils.close(writer);
        
        // backup existing file
        if (file_.exists())
        {
            if (bak.exists() && !bak.delete())
            {
                logger.warn("Unable to delete " + bak.getName());
            }
            
            if (!file_.renameTo(bak))
            {
                logger.warn("Unable to rename " + file_.getName() + " to " + bak.getName());             
            }
        }
        
        // move temp file to existing file
        if (!tmp.renameTo(file_))
        {
            logger.warn("Unable to rename " + tmp.getName() + " to " + file_.getName());
        }
        
        // cleanup backup file
        if (bak.exists() && !bak.delete())
        {
            logger.warn("Unable to delete " + bak.getName());
        }
        
    }
    
    /**
     * Write to given writer
     */
    public void write(Writer writer)
    {
        try {
            GameStateEntry entry;
            StringWriter tempWriter = new StringWriter();
            for (int i = 0; i < entries_.size(); i++)
            {
                entry = entries_.get(i);
                entry.write(this, tempWriter);
                Hide.obfuscate(tempWriter.getBuffer(), i);
                writer.write(tempWriter.toString());
                
                writer.write(ENTRY_ENDLINE);
                tempWriter.getBuffer().setLength(0);
            }
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    } 

    
    /**
     * Read data in from file associated with this game state
     */
    public void read(boolean bFull)
    {
        Reader reader = null;
        if (file_ != null)
        {
            reader = ConfigUtils.getReader(file_);
        }
        else if (savedata_ != null)
        {
            //logger.debug("Reading data: ");
            //Unhide.unhide(new StringReader(Utils.decode(savedata_)));
            reader = new StringReader(Utils.decode(savedata_));
        }
            
        read(bFull, reader);
    }

    /**
     * read data in.  If bFull is false, only the first line
     * is read to get name/description information
     */
    private void read(boolean bFull, Reader reader)
    {
        BufferedReader sreader = new BufferedReader(reader);

        try {
            String sLine;
            GameStateEntry entry = null;
            StringBuffer sbLine = new StringBuffer(80);
            int nEntry = -1;
            while ((sLine = sreader.readLine()) != null)
            {
                nEntry++;
                //logger.debug("Read: " + sLine);
                sbLine.setLength(0);
                sbLine.append(sLine);
                Hide.deobfuscate(sbLine, nEntry);
                entry = new GameStateEntry(this, sbLine.toString());

                // take 1st entry and fill in game name and description
                if (nEntry == 0)
                {
                    entry.finishParsing(this);
                    initFromGameStateEntry(entry);
                }
                else if (nEntry == 1)
                {
                    entry.finishParsing(this);
                    initClassIdsFromTokenizedList(entry);
                }
                else
                {
                    addEntry(entry);
                }

                if (!bFull)
                {
                    break;
                }
            }

            // TODO: error handling bogus GameState files?

            ConfigUtils.close(sreader);
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }
    
    /**
     * Ask each game state entry to finish parsing.  Called
     * after read is done (and all Object ids are known)
     */
    public void finishParsing()
    {
        for (GameStateEntry entry : entries_)
        {
            entry.finishParsing(this);
        }
    }
    
    /**
     * Delete the game state's associated file
     */
    public void delete()
    {
        if (file_ == null) return;
        file_.delete();
    }
    
    /**
     * Get game name
     */
    public String getGameName()
    {
        return sName_;
    }
    
    /**
     * Get game descriptoin
     */
    public String getDescription()
    {
        return sDesc_;
    }
    
    /**
     * Set game descriptoin
     */
    public void setDescription(String s)
    {
        sDesc_ = s;
    }
    
    /**
     * Get last modified
     */
    public long lastModified()
    {
        if (file_ == null) return 0;
        return file_.lastModified();
    }
    
    ///
    /// game information
    ///
    
    /**
     * Add an entry
     */
    public void addEntry(GameStateEntry entry)
    {
        if (entry == null) return;
        entries_.add(entry);
    }
    
    /**
     * Remove an entry from the beginning of the list
     */
    public GameStateEntry removeEntry()
    {
        ApplicationError.assertTrue(entries_.size() > 0, "No more entries");
        return entries_.remove(0);
    }
    
    /**
     * Get entry at top of list w/out removing it
     */
    public GameStateEntry peekEntry()
    {
        if (entries_.size() == 0) return null;
        return entries_.get(0);
    }
    
    /**
     * Store players
     */
    public void savePlayers(GamePlayerList players)
    {
        if (details_.getSavePlayers() == SaveDetails.SAVE_NONE) return;
        boolean bDirty = details_.getSavePlayers() == SaveDetails.SAVE_DIRTY;
        
        GameStateEntry entry = new GameStateEntry(this, null, ConfigConstants.SAVE_NUMPLAYERS);
        
        GamePlayer player;
        int nNum = 0;
        
        // count number we are saving
        int nNumPlayers = players.getNumPlayers();
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = players.getPlayerAt(i);
            if (bDirty)
            {
                if (player.isDirty()) nNum++;
            }
            else
            {
                nNum++;
            }
        }
        
        entry.addToken(nNum);
        addEntry(entry);

        for (int i = 0; i < nNumPlayers; i++)
        {
            player = players.getPlayerAt(i);
            if (bDirty && !player.isDirty()) continue;

            player.addGameStateEntry(this);
        }
    }
    
    /**
     * Get players from entries.  Return true if any player data loaded
     */
    public void loadPlayers(GamePlayerList players)
    {
        if (details_.getSavePlayers() == SaveDetails.SAVE_NONE) return;

        GameStateEntry entry = removeEntry();
        int nNum = entry.removeIntToken();

        // if loading all, clear existing players, in case
        // we are loading fewer players than we currently have
        if (details_.getSavePlayers() == SaveDetails.SAVE_ALL) players.clearPlayerList(nNum);

        GamePlayer player;
        for (int i = 0; i < nNum; i++)
        {
            entry = removeEntry();
            player = (GamePlayer) entry.getObject(); // on dirty load, this returns existing player
            player.loadFromGameStateEntry(this, entry);

            // if doing a dirty load, player may already be there, in
            // which case we just updated them in previous step
            if (!players.containsPlayer(player))
            {
                players.addPlayer(player);
            }
        }
    }

    /**
     * Store observers
     */
    public void saveObservers(GameObserverList observers)
    {
        if (details_.getSaveObservers() == SaveDetails.SAVE_NONE) return;
        boolean bDirty = details_.getSaveObservers() == SaveDetails.SAVE_DIRTY;

        GameStateEntry entry = new GameStateEntry(this, null, ConfigConstants.SAVE_NUMPLAYERS);

        GamePlayer observer;
        int nNum = 0;

        // count number we are saving
        int nNumObservers = observers.getNumObservers();
        for (int i = 0; i < nNumObservers; i++)
        {
            observer = observers.getObserverAt(i);
            if (bDirty)
            {
                if (observer.isDirty()) nNum++;
            }
            else
            {
                nNum++;
            }
        }

        entry.addToken(nNum);
        addEntry(entry);

        for (int i = 0; i < nNumObservers; i++)
        {
            observer = observers.getObserverAt(i);
            if (bDirty && !observer.isDirty()) continue;

            // TODO: shell entry if already added observer in save file as a player
            observer.addGameStateEntry(this);
        }
    }

    /**
     * Get observers from entries.  Return true if any observer data loaded
     */
    public void loadObservers(GameObserverList observers)
    {
        if (details_.getSaveObservers() == SaveDetails.SAVE_NONE) return;

        GameStateEntry entry = removeEntry();
        int nNum = entry.removeIntToken();

        // if loading all, clear existing observers, in case
        // we are loading fewer observers than we currently have
        if (details_.getSaveObservers() == SaveDetails.SAVE_ALL) observers.clearObserverList(nNum);

        GamePlayer observer;
        for (int i = 0; i < nNum; i++)
        {
            entry = removeEntry();
            observer = (GamePlayer) entry.getObject(); // on dirty load, this returns existing observer
            observer.loadFromGameStateEntry(this, entry);

            // if doing a dirty load, observer may already be there, in
            // which case we just updated them in previous step
            if (!observers.containsObserver(observer))
            {
                observers.addObserver(observer);
            }
        }
    }

    /**
     * Store territories
     */
    public void saveTerritories(Territory[] territories)
    {
        if (details_.getSaveTerritories() == SaveDetails.SAVE_NONE || territories == null) return;
        
        boolean bDirty = details_.getSaveTerritories() == SaveDetails.SAVE_DIRTY;
        boolean bSave;
        for (Territory territory : territories)
        {
            bSave = false;

            // if dirty save, save only if territory is dirty
            if (bDirty)
            {
                if (territory.isDirty())
                {
                    bSave = true;
                }
            }
            // otherwise, only save if delegate says okay
            else if (delegate_ != null)
            {
                if (delegate_.saveTerritory(territory))
                {
                    bSave = true;
                }
            }
            // otherwise if no delegate, always save
            else
            {
                bSave = true;
            }

            if (bSave)
            {
                territory.addGameStateEntry(this);
            }
        }
    }
    
    /**
     * Load territory data
     */
    public void loadTerritories()
    {
        if (details_.getSaveTerritories() == SaveDetails.SAVE_NONE) return;
        
        GameStateEntry entry = peekEntry();
        Territory t;
        while (entry != null && entry.getType() == ConfigConstants.SAVE_TERRITORY)
        {
            entry = removeEntry();
            
            t = (Territory) entry.getObject();
            t.loadFromGameStateEntry(this, entry);
            entry = peekEntry();
        }
    }
    
    /**
     * Ask for any custom data from the delegate
     */
    public void saveCustomData()
    {
        if (details_.getSaveCustom() == SaveDetails.SAVE_NONE) return;
        if (delegate_ != null)
        {
            delegate_.saveCustomData(this);
        }
    }
    
    /**
     * allow delegate to load any data
     */
    public void loadCustomData()
    {
        if (details_.getSaveCustom() == SaveDetails.SAVE_NONE) return;
        if (delegate_ != null)
        {
            delegate_.loadCustomData(this);
        }
    }
    
    /**
     * Prepopulate set ids
     */
    public void prepopulateIds(Object game, Territory[] territories, GamePlayerList players, GameObserverList observers)
    {
        // territories
        for (int i = 0; territories != null && i < territories.length; i++)
        {
            setId(territories[i]);
        }

        // players
        for (int i = 0; players != null && i < players.getNumPlayers(); i++)
        {
            setId(players.getPlayerAt(i));
        }

        // observers - need to make sure id isn't in use because observer
        // could be a player
        ObjectID o;
        for (int i = 0; observers != null && i < observers.getNumObservers(); i++)
        {
            o = observers.getObserverAt(i);
            if (!isIdUsed(o))
            {
                setId(o);
            }
        }

        // application specific
        prepopulateCustomIds(game);
    }
    
    /**
     * Allow delegate to specify any ids before load
     */
    public void prepopulateCustomIds(Object game)
    {
        if (delegate_ != null)
        {
            delegate_.prepopulateCustomIds(game, this);
        }
    }
    
    /**
     * Get the phase to run (from the delegate) when restoring a game.
     * This phase is typically the one that creates the main screen
     * for the game and subclasses ChainPhase.  The next-phase and
     * params are passed in from the data stored in the GameState.
     */
    public String getBeginGamePhase(Object context, Object game, TypedHashMap params)
    {
        if (delegate_ != null)
        {
            return delegate_.getBeginGamePhase(context, game, this, params);
        }
        
        return null;
    }
    
    ////
    //// Save Files
    ////
    
    /**
     * Get list of save files in save directory
     */
    public static GameState[] getSaveFileList(String sBegin, String sExt)
    {
        return getSaveFileList(GameConfigUtils.getSaveDir(), sBegin, sExt);
    }

    /**
     * Get list of save files in save directory
     */
    public static GameState[] getSaveFileList(File fDir, String sBegin, String sExt)
    {
        File files[] = Utils.getFileList(fDir, SaveFile.DELIM + sExt, sBegin);
        List<GameState> newst = new ArrayList<GameState>();

        for (File file : files)
        {
            try
            {
                newst.add(GameStateFactory.createGameState(file, true));
            }
            catch (Throwable e)
            {
                logger.error("Error loading " + file.getAbsolutePath() + ": " +
                             Utils.formatExceptionText(e));
            }
        }

        return newst.toArray(new GameState[newst.size()]);
    }
}
