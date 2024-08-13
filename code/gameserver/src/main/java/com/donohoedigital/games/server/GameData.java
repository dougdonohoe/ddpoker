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
 * GameData.java
 *
 * Created on March 8, 2003, 8:58 AM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.server.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 *
 * @author  donohoe
 */
public class GameData extends ServerDataFile
{
    //static Logger logger = Logger.getLogger(GameData.class);
    
    // constants
    public static final String DATA_EXT = "dat";       // game board file
    
    // instance data
    private String data_;
    
    public static File getGameDataFile(File dir, String sFileNum, boolean bValidate)
    {
        File file = createFile(dir, BEGIN, sFileNum, DATA_EXT);
        if (bValidate)
        {
            ConfigUtils.verifyFile(file);
        }
        return file;
    }
        
    /** 
     * Creates a new instance of GameData 
     */
    public GameData(File dir, String sFileNum, String data)
    {
        super();
        
        dir_ = dir;
        sFileNum_ = sFileNum;
        data_ = data;
        
        file_ = getGameDataFile(dir_, sFileNum_, false);
        ConfigUtils.verifyNewFile(file_);
        
        // save our contents out
        save();
    }
    
    /** 
     * Write contents out
     */
    public void write(Writer writer) throws IOException
    {
        writer.write(data_);
    }
    
    /**
     * Read contents in
     */
    public void read(Reader reader, boolean bFull) throws IOException
    {
        char[] buf = new char[1000];
        StringBuilder sb = new StringBuilder();
        int nRead;
        while ((nRead = reader.read(buf)) != -1)
        {
            sb.append(buf, 0, nRead);
        }
        data_ = sb.toString();
    }
    
}
