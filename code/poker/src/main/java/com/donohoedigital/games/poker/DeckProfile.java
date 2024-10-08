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
 * DeckProfile.java
 *
 * Created on May 18, 2004, 1:33 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
public class DeckProfile extends BaseProfile
{
    static Logger logger = LogManager.getLogger(DeckProfile.class);
    
    // defines
    public static final String PROFILE_BEGIN = "deck";
    public static final String DECK_DIR = "decks";
    static final String NAME_BEGIN = "card-";

    /**
     * empty
     */
    public DeckProfile()
    {
    }

    /**
     * Load profile from file
     */
    public DeckProfile(File file, boolean bFull) 
    {
        setFile(file);
    }

    /**
     * set file
     */
    public void setFile(File file)
    {
        file_ = file;
        String sName = file.getName();
        sFileName_ = sName;
        int nDot = sName.lastIndexOf('.');
        sName = sName.substring(0, nDot);
        if (sName.startsWith(NAME_BEGIN))
        {
            sName = sName.substring(5);
        }
        sName_ = sName;
    }

    
    /**
     * New profile copied from given profile
     */
    public DeckProfile(DeckProfile dp)
    {
		file_ = dp.file_;
        sFileName_ = dp.sFileName_;
        sName_ = dp.sName_;
    } 

    /**
     * override - no saving
     */
    public void save()
    {
    }
    
    /**
     * override - no delete if custom
     */
    public boolean canDelete()
    {
       return (!sFileName_.startsWith(NAME_BEGIN));
    }
    
    /** 
     * Get begin part of profile name
     */
    protected String getBegin()
    {
        return PROFILE_BEGIN;
    }
    
    /** 
     * Get name of directory to store profiles in
     */
    protected String getProfileDirName() 
    {
        return DECK_DIR;
    }
    
    /**
     *  Get profile list
     */
    protected List<BaseProfile> getProfileFileList() {
        return getProfileList();
    }

    /**
     * Get list of save files in save directory
     */
    public static List<BaseProfile> getProfileList()
    {
        return BaseProfile.getProfileList(DECK_DIR, new DeckFileFilter(), DeckProfile.class, false);
    }
    
    /**
     * Filter by extension
     */
    private static class DeckFileFilter implements FilenameFilter
    {
        public DeckFileFilter()
        {
        }
        
        public boolean accept(File dir, String name)
        {
            return DeckProfile.accept(dir, name);
            
        }
    }
    
    /**
     * common accept
     */
    private static boolean accept(File dir, String name)
    {
        String sName = name.toLowerCase();
            
        if (sName.endsWith(".gif") || sName.endsWith(".jpg") || sName.endsWith(".png"))
        {
            File file = new File(dir, name);
            if (file.length() > 35000) return false;
            return true;
        }

        return false;
    }
    
    /**
     * filter for file chooser
     */
    static class DeckFilter extends javax.swing.filechooser.FileFilter
    {
        /** 
         * Whether the given file is accepted by this filter.
         */
        public boolean accept(File f) 
        {
            if (f.isFile())
            {
                return DeckProfile.accept(f.getParentFile(), f.getName());
            }
            
            return true;
        }
        
        /** The description of this filter. For example: "JPG and GIF Images"
         */
        public String getDescription() {
            return PropertyConfig.getMessage("msg.deck.files");
        }
        
    }
}
