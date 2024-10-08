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
 * BaseDataFile.java
 *
 * Created on March 8, 2003, 8:58 AM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
public abstract class BaseDataFile
{
    static Logger logger = LogManager.getLogger(BaseDataFile.class);
    
    // constants
    public static final String DELIM = ".";     // delim between components in a file name
    public static final String END = "\n";      // end of a save line
    
    // global data
    protected static Format fNum_ = new Format("%05d"); // THREAD: this should be thread safe
    
    // instance data
    protected File dir_;        // where we are saved
    protected File file_;       // file we are saved to
    protected String sFileNum_; // number for our file names
    protected long lastMod_;    // last modification time at load
    
   /**
     * Copy file info
     **/
    protected void copyBaseFileInfo(BaseDataFile bdf)
    {
        dir_ = bdf.dir_;
        file_ = bdf.file_;
        sFileNum_ = bdf.sFileNum_;
        lastMod_ = bdf.lastMod_;
    }
    
    /** 
     * Creates a new instance of BaseDataFile 
     */
    public BaseDataFile()
    {
    }
    
    /** 
     * Get last file modification
     */
    public long getLastModified()
    {
        return lastMod_;
    }
    
    /**
     * Get the file data is saved in
     */
    public File getFile()
    {
        return file_;
    }
    
    /**
     * Get the dir we are saved in
     */
    public File getDir()
    {
        return dir_;
    }
    
    /**
     * Return string rep of file num
     */
    public String getFileNum()
    {
        return Integer.toString(getFileNumber(getFile()));
    }

    /**
     * Save only if file is defined
     */
    public void saveIfPossible()
    {
        if (file_ != null) save();
    }
    
    /**
     * Save this to disk
     */
    public void save()
    {
        ApplicationError.assertNotNull(file_, "File null", this.getClass().getName());
        
        // check last mod date
        long lastMod2 = file_.lastModified();
        if (lastMod_ != 0 && lastMod2 != lastMod_)
        {
            logger.warn("Saving file " + file_.getAbsolutePath() + ", but last modified changed from " + lastMod_ + " to " + lastMod2);
        }
        
        // this actually changes last mod date!
        Writer writer = ConfigUtils.getWriter(file_);
        
        try {
            write(writer);
            writer.close();
            lastMod_ = file_.lastModified();
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }
    
    /**
     * subclass implements to save its contents to the given writer
     */
    public abstract void write(Writer writer) throws IOException;
    
    /**
     * for subclass to terminate an entry 
     */
    protected void writeEndEntry(Writer writer) throws IOException
    {
        writer.write(END);
    }
    
    /**
     * Load from disk, bFull = true
     */
    public void load()
    {
        load(true);
    }
    
    /**
     * Load from disk, bFull can be used to allow partial loads
     */
    public void load(boolean bFull)
    {
        ApplicationError.assertNotNull(file_, "File null", this.getClass().getName());
        Reader reader = ConfigUtils.getReader(file_);
        lastMod_ = file_.lastModified();

        try {
            read(reader, bFull);
            reader.close();
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }

    /**
     * subclass implements to load its contents from the given reader
     */
    public abstract void read(Reader reader, boolean bFull) throws IOException;
    
    /**
     * Create a file object using sFileNum_
     */
    protected File createFile(File dir, String sBegin, String sExt)
    {
        return createFile(dir, sBegin, sFileNum_, sExt);
    }
    
    /**
     * Create a file object
     */
    protected static File createFile(File dir, String sBegin, String sFileNum, String sExt)
    {
        return new File(dir, sBegin + DELIM + sFileNum + DELIM + sExt);
    }
    
    /**
     * Conveinence function to return tokenized list from next line in reader
     */
    protected static TokenizedList readTokenizedList(BufferedReader buf) throws IOException
    {
        TokenizedList list;
        String sLine = buf.readLine();
        list = new TokenizedList();
        list.demarshal(null, sLine);
        return list;
    }
    
    ////
    //// Save Files
    ////
    
    /**
     * Get file number from the given file
     */
    public static int getFileNumber(File file)
    {
        String sName = file.getName();
        StringTokenizer st = new StringTokenizer(sName, DELIM);
        ApplicationError.assertTrue(st.countTokens() == 3, "Incorrect file name");

        st.nextToken(); // skip Beginning part of name
        String sValue = st.nextToken();
        try {
            return Integer.parseInt(sValue);
        }
        catch (NumberFormatException nfe)
        {
            throw new ApplicationError(nfe);
        }
    }
}
