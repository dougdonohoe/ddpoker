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
 * GameConfigUtils.java
 *
 * Created on March 8, 2003, 9:26 AM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
public class GameConfigUtils
{
    //private static Logger logger = LogManager.getLogger(GameConfigUtils.class);

    public static final String SAVE_DIR = "save";

    private static File saveDir = null;
    private static final Map<String, ObjectLock> lockMap = new HashMap<String, ObjectLock>();

    /**
     * Get the location for save files, creating the directory if not there.
     * the returned value is cached in the ConfigManager, so there is only
     * one per application (useful for locking)
     */
    public synchronized static File getSaveDir()
    {
        if (saveDir == null)
        {
            saveDir = getSaveDirLocation();
        }
        return saveDir;
    }
    
    /**
     * Get the location for save files, create if it doesn't exist
     */
    private static File getSaveDirLocation()
    {
        File parentDir = ConfigManager.getUserHome();
        File saveDir = new File(parentDir, SAVE_DIR);
        ConfigUtils.verifyNewDirectory(saveDir);
        return saveDir;
    }
    
    /**
     * Get next save file number given the list of existing files.
     * Assumes the files are in sorted order, with last one being
     * highest number.
     * BUG 32 - loops from last till first to handle misnamed files
     */
    public static int getNextSaveNumber(Object[] existing)
    {
        int nNum = 1;
        if (existing != null && existing.length > 0)
        { 
            for (int i = existing.length - 1; i >= 0; i--)
            {
                SaveFile last = (SaveFile)existing[i];
                File file = last.getFile();
                // handles lists where initial items are not file-based
                if (file != null)
                {
                    int nValue = getFileNumber(file);
                    if (nValue != -1) return nValue + 1;
                }
            }
        }
        
        return nNum;
    }
    
    /**
     * Get file number from the given file, returns -1 if no valid number.
     * File name expected to be <leading>.<num>.<ext>
     */
    public static int getFileNumber(File file)
    {
        String sName = file.getName();
        StringTokenizer st = new StringTokenizer(sName, SaveFile.DELIM);
        if (st.countTokens() != 3) return -1;

        st.nextToken(); // skip <leading>
        String sValue = st.nextToken();
        try {
            return Integer.parseInt(sValue);
        }
        catch (NumberFormatException nfe)
        {
            // BUG 32
            return -1;
        }
    }
    
    // used in formating file number (6 digits with leading 0s)
    private static final Format fNum = new Format("%06d");
    
    /**
     * Format file number into a string for use in file name
     */
    public static String formatFileNumber(int n)
    {
        return fNum.form(n);
    }

    /**
     * Get an object to lock on for synchronizing game submissions
     */
    public static ObjectLock getGameLockingObject(String sGameID)
    {
        synchronized(lockMap)
        {
            ObjectLock lock = lockMap.get(sGameID);
            if (lock == null)
            {
                lock = new ObjectLock(sGameID);
                //logger.debug("Lock Created: " + lock.getID());
                lockMap.put(sGameID, lock);
            }
            else
            {
                lock.increment();
            }
            return lock;
        }
    }
    
    /**
     * Remove locking object
     */
    public static void removeGameLockingObject(ObjectLock lock)
    {
        synchronized(lockMap)
        {
            if (lock.decrement() == 0)
            {
                lockMap.remove(lock.getID());
                //logger.debug("Lock Removed: " + lock.getID());
            }
        }
    }
}
