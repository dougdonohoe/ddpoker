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
 * PlayerQueue.java
 *
 * Created on March 8, 2003, 8:58 AM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.config.ConfigUtils;
import com.donohoedigital.games.comms.EngineMessage;

import java.io.*;

/**
 *
 * @author  donohoe
 */
public class PlayerQueue extends ServerDataFile
{
    //static Logger logger = LogManager.getLogger(PlayerQueue.class);
    
    // constants
    public static final String PLAYER_Q_EXT = "pq";    // player queue - pq0, pq1, pq2, etc.
    
    // instance data
    private Integer nPlayerID_;
    private DMArrayList msgList_;  // list of actions to send to player
    
    /**
     * Create a PlayerQueue from an existing file
     */
    public static PlayerQueue loadPlayerQueue(File dir, String sFileNum, int nPlayerID)
    {
        return new PlayerQueue(dir, sFileNum, nPlayerID, false);
    }
    
    public static PlayerQueue newPlayerQueue(File dir, String sFileNum, int nPlayerID)
    {
        return new PlayerQueue(dir, sFileNum, nPlayerID, true);
    }
    
    public static File getPlayerQueueFile(File dir, String sFileNum, int nPlayerID, boolean bValidate)
    {
        File file = createFile(dir, BEGIN, sFileNum, PLAYER_Q_EXT + nPlayerID);
        if (bValidate)
        {
            ConfigUtils.verifyFile(file);
        }
        return file;
    }
    
    /**
     * Empty constructor
     */
    private PlayerQueue()
    {
        super();
    }
    
    /** 
     * Creates a new instance of PlayerQueue 
     */
    private PlayerQueue(File dir, String sFileNum, int nPlayerID, boolean bNew)
    {
        super();
        
        dir_ = dir;
        sFileNum_ = sFileNum;
        nPlayerID_ = nPlayerID;
        file_ = getPlayerQueueFile(dir_, sFileNum_, nPlayerID_, false);
        
        if (bNew)
        {
            msgList_ = new DMArrayList();
            ConfigUtils.verifyNewFile(file_);
            save();
        }
        else
        {
            ConfigUtils.verifyFile(file_);
            load();
        }
    }
    
    /**
     * Get player id
     */
    public int getPlayerID()
    {
        return nPlayerID_;
    }
    
    /**
     * Add new message to the queue
     */
    public void addMessage(EngineMessage msg)
    {
        msgList_.add(msg);
    }
    
    /** 
     * Remove all messages in queue up to and including the
     * given timestamp
     */
    public void removeMessagesUpTo(long timestamp)
    {
        // shortcut
        if (timestamp == 0) return;
        
        EngineMessage msg;
        boolean bDone = false;
        
        // keep getting head of list (earlier messages are first)
        // remove from head of list as long as timestamp is less
        // than or equal to the given timestamp
        while (!bDone && msgList_.size() > 0)
        {
            msg = (EngineMessage) msgList_.get(0);
            if (msg.getCreateTimeStamp() <= timestamp)
            {
                msgList_.remove(0);
            }
            else
            {
                bDone = true;
            }
        }
    }
    
    /** 
     * Write contents out
     */
    public void write(Writer writer) throws IOException
    {
        // actions
        String sMarsh = msgList_.marshal(null);        
        writer.write(sMarsh);
        writeEndEntry(writer); // OnlineGameManager on client removes this extra return
    }
    
    /** 
     * Read contents in
     */
    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        
        // actions
        msgList_ = new DMArrayList();
        msgList_.demarshal(null, buf.readLine());
    }
    
}
