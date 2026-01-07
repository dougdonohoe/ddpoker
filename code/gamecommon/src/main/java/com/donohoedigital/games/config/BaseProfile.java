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
 * BaseProfile.java
 *
 * Created on January 27, 2004, 10:19 AM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 * @author donohoe
 */
public abstract class BaseProfile extends BaseDataFile implements SaveFile, Comparable<BaseProfile>, NamedObject
{
    private static final Logger logger = LogManager.getLogger(BaseProfile.class);

    // defines
    public static final String PROFILE_EXT = "dat";
    public static final String PROFILE_DIR = "profiles";
    protected static Format fNum = new Format("%02d");

    // saved members
    protected String sName_;
    protected long nCreate_;
    protected boolean bCanDelete_ = true;
    protected boolean bCanCopy_ = true;
    protected String sFileName_;

    /**
     * Empty for subclasses
     */
    protected BaseProfile()
    {
    }

    /**
     * New profile with given name
     */
    public BaseProfile(String sName)
    {
        sName_ = sName;
    }

    /**
     * Load profile from string file
     */
    public BaseProfile(String sFile, boolean bFull)
    {
        File file = new File(sFile);
        ConfigUtils.verifyFile(file);
        init(file, bFull);
    }

    /**
     * Load profile from file
     */
    public BaseProfile(File file, boolean bFull)
    {
        init(file, bFull);
    }

    /**
     * init from file
     */
    private void init(File file, boolean bFull)
    {
        file_ = file;
        load(bFull);

        // set to file loaded from after load
        sFileName_ = file.getName();
    }

    /**
     * init file
     */
    public void initFile()
    {
        if (file_ == null)
        {
            // we refetch save list just to be sure we get correct last number
            int nNum = GameConfigUtils.getNextSaveNumber(getProfileFileList().toArray());
            sFileNum_ = fNum.form(nNum);
            file_ = createFile(getProfileDir(getProfileDirName()), getBegin(), PROFILE_EXT);
            sFileName_ = file_.getName();
        }
    }

    /**
     * Copy file info
     */
    public void copyFileInfo(BaseProfile profile)
    {
        super.copyBaseFileInfo(profile);
        sFileName_ = profile.sFileName_;
        bCanDelete_ = profile.bCanDelete_;
    }

    /**
     * Get file name - used primarily for equality checking
     */
    public String getFileName()
    {
        return sFileName_;
    }

    /**
     * Get begin part of profile name
     */
    protected abstract String getBegin();

    public String toHTML()
    {
        return toString();
    }

    /**
     * Get name of directory to store profiles in
     */
    protected abstract String getProfileDirName();

    /**
     * Get profile list
     */
    protected abstract List<BaseProfile> getProfileFileList();

    /**
     * Get name of player
     */
    public String getName()
    {
        return sName_;
    }

    /**
     * Set name
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }

    /**
     * Set create date as now
     */
    public void setCreateDate()
    {
        nCreate_ = System.currentTimeMillis();
    }

    /**
     * Set create date from given profile
     */
    public void setCreateDate(BaseProfile profile)
    {
        nCreate_ = profile.nCreate_;
    }

    /**
     * Get create date
     */
    public long getCreateDate()
    {
        return nCreate_;
    }

    /**
     * Can this profile be deleted?
     */
    public boolean canDelete()
    {
        return bCanDelete_;
    }

    /**
     * Can this profile be edited? By default, returns canDelete()
     */
    public boolean canEdit()
    {
        return canDelete();
    }

    /**
     * Can this profile be copied?
     */
    public boolean canCopy()
    {
        return bCanCopy_;
    }

    /**
     * Set can delete flag
     */
    public void setDelete(boolean b)
    {
        bCanDelete_ = b;
    }

    /**
     * Set can copy flag
     */
    public void setCopy(boolean b)
    {
        bCanCopy_ = b;
    }

    /**
     * Comparable: for sorting by name
     */
    public int compareTo(BaseProfile p)
    {
        int c = sName_.compareToIgnoreCase(p.sName_);
        if (c != 0) return c;

        // if names equal, sort most recent higher
        return (int) (p.nCreate_ - nCreate_);
    }

    /**
     * Equals - compare file name
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof BaseProfile)) return false;

        BaseProfile bp = (BaseProfile) o;

        if (sFileName_ == null && bp.sFileName_ == null) return true;
        if (sFileName_ == null || bp.sFileName_ == null) return false;

        return sFileName_.equals(bp.sFileName_);
    }

    /**
     * hashcode
     */
    public int hashCode()
    {
        if (sFileName_ == null) return super.hashCode();
        return sFileName_.hashCode();
    }

    ////
    //// Saved profiles
    ////

    /**
     * subclass implements to load its contents from the given reader
     */
    public void read(Reader reader, boolean bFull) throws IOException
    {
        // allow buffered reader to be passed in from subclass
        BufferedReader buf;
        if (reader instanceof BufferedReader) buf = (BufferedReader) reader;
        else buf = new BufferedReader(reader);

        TokenizedList list = readTokenizedList(buf);
        sName_ = list.removeStringToken();
        nCreate_ = list.removeLongToken();
        bCanDelete_ = list.removeBooleanToken();
        sFileName_ = list.removeStringToken();
    }

    /**
     * subclass implements to save its contents to the given writer
     */
    public void write(Writer writer) throws IOException
    {
        // scalar values
        TokenizedList list = new TokenizedList();
        list.addToken(sName_);
        list.addToken(nCreate_);
        list.addToken(bCanDelete_);
        list.addToken(sFileName_);
        writer.write(list.marshal(null));
        writeEndEntry(writer);
    }

    /**
     * Get the location for save profiles, creating the directory if not there.
     */
    public static File getProfileDir(String sDir)
    {
        File saveDir = GameConfigUtils.getSaveDir();
        File profileDir = new File(saveDir, sDir);
        ConfigUtils.verifyNewDirectory(profileDir);
        return profileDir;
    }

    /**
     * Get list of saved profiles in save directory.
     */
    protected static List<BaseProfile> getProfileList(String sDirPath, FilenameFilter filter,
                                                      Class<?> c, boolean bFull)
    {
        Class<?>[] signature = new Class[]{File.class, Boolean.TYPE};

        File[] files = getProfileDir(sDirPath).listFiles(filter);

        List<BaseProfile> profiles = new ArrayList<>();
        if (files == null) return profiles;

        Arrays.sort(files);
        BaseProfile profile;

        Object[] params = new Object[2];

        params[1] = bFull ? Boolean.TRUE : Boolean.FALSE;

        for (File file : files)
        {
            try
            {
                params[0] = file;
                profile = (BaseProfile) ConfigUtils.newInstanceGeneric(c, signature, params);
                profiles.add(profile);
            }
            catch (Throwable e)
            {
                logger.error("Error loading {}: {}", file, Utils.formatExceptionText(e));
            }
        }

        return profiles;
    }
}
