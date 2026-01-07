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
 * Message.java
 *
 * Created on March 6, 2003, 8:43 AM
 */

package com.donohoedigital.comms;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
@DataCoder('M')
public class DDMessage extends TypedHashMap implements PostWriter, PostReader, DataMarshal
{
    private static Logger logger = LogManager.getLogger(DDMessage.class);

    public static final String CONTENT_TYPE = "application/x-donohoedigital-msg";

    // delim between params and data when writing
    // we write DELIM_CNT of these because we are
    // guessing UTF-8 doesn't have a char which
    // equals this many returns in a row
    public static final char DELIM = '\n';
    public static final int DELIM_CNT = 3;

    /**
     * param used to store num chunks, used in transport
     */
    private static final String PARAM_NUM_CHUNKS = "_#_";
    
    /**
     * RESERVED - param "cat"
     */
    public static final String PARAM_CATEGORY = "cat";
    
    /**
     * RESERVED - param "time"
     */
    public static final String PARAM_TIME = "time";
    
    /**
     * RESERVED - param "exception" 
     */
    public static final String PARAM_EXCEPTION_MESSAGE = "exception";
    
     /**
     * RESERVED - param "ddexception" 
     */
    public static final String PARAM_DDEXCEPTION_MESSAGE = "ddexception";
    
    /**
     * RESERVED - param "errormsg"
     */
    public static final String PARAM_ERROR_MESSAGE = "errormsg";

    /**
     * RESERVED - param "statusmsg"
     */
    public static final String PARAM_STATUS_MESSAGE = "statusmsg";

    /**
     * RESERVED - param "version"
     */
    public static final String PARAM_VERSION = "version";
    
    /**
     * RESERVED - param "key"
     */
    public static final String PARAM_KEY = "key";
    
    /**
     * RESERVED - param "fromip"
     */
    public static final String PARAM_FROM_IP = "fromip";
    
    /**
     * Returned if no category defined
     */
    public static final int CAT_NONE = -1;
    
    /**
     * Error category
     */
    public static final int CAT_ERROR = -2;
    
    /**
     * Error category
     */
    public static final int CAT_APPL_ERROR = -3;
    
    /**
     * Testing category
     */
    public static final int CAT_TESTING = -999;
    
    // data that comprises the message
    private List<MessageData> msgdata_ = null;

    // transient data only
    private int nStatus_ = DDMessageListener.STATUS_NONE;
    
    // class to represent data chunks
    private class MessageData
    {
        private byte[] bytedata_;
        private File filedata_;
        int nSize_;
        
        private MessageData(File file)
        {
            filedata_ = file;
            nSize_ = (int) file.length();
        }
        
        private MessageData(byte[] data)
        {
            bytedata_ = data;
            nSize_ = data.length;
        }
    }
    
    /**
     * Copy contents of this to the given message
     */
    public void copyTo(DDMessage msg)
    {
        // status
        msg.nStatus_ = nStatus_;
        
        // data
        if (msgdata_ != null)
        {
            msg.getDataList().addAll(msgdata_); // use method to call to ensure it exists
        }
        
        // params
        msg.putAll(this);
    }
    
    /**
     * Empty constructor for initializing from post data
     */
    public DDMessage()
    {
        setVersion(getDefaultVersion());
        setKey(getDefaultKey());
        setCreateTimeStamp();
    }
    
    /**
     * Message with no data
     */
    public DDMessage(int nCategory)
    {
        this();
        setCategory(nCategory);
    }
    
    /** 
     * Creates new message from string data
     */
    public DDMessage(int nCategory, String sData) 
    {
        this();
        setCategory(nCategory);
        addData(sData);
    }
    
    /**
     * Create new message from byte data
     */
    public DDMessage(int nCategory, byte[] baData)
    {
        this();
        setCategory(nCategory);
        addData(baData);
    }
    
    /**
     * Create new message from file data.
     * Note: getData() will return null when this is used
     * as the file data is not read in until marshalled
     */
    public DDMessage(int nCategory, File file)
    {
        this();
        setCategory(nCategory);
        addData(file);
    }
    
    /**
     * Create new message from file data.
     * Note: getData() will return null when this is used
     * as the file data is not read in until marshalled
     */
    public DDMessage(int nCategory, File[] files)
    {
        this();
        setCategory(nCategory);
        addData(files);
    }
    
    /**
     * Get version of software
     */
    public Version getVersion()
    {
        return (Version) getObject(PARAM_VERSION);
    }
    
    /**
     * Set version of software
     */
    public void setVersion(Version v)
    {
        setObject(PARAM_VERSION, v);
    }
    
    /** 
     * Get locale from version or null if not there
     */
    public String getLocale()
    {
        Version v = getVersion();
        if (v == null) return null;
        else return v.getLocale();
    }
    
    
    /**
     * Get license key of software
     */
    public String getKey()
    {
        return getString(PARAM_KEY);
    }
    
    /**
     * Set key of software
     */
    public void setKey(String s)
    {
        setString(PARAM_KEY, s);
    }
    
    /**
     * Store current time in millis* 1000 + a sequence number
     */
    public void setCreateTimeStamp()
    {
        setLong(PARAM_TIME, Utils.getCurrentTimeStamp());
    }
        
    /**
     * Get create time (millis since 1/1/1970 * 1000 + arbitrary sequence number)
     */
    public long getCreateTimeStamp()
    {
        Long cat = getLong(PARAM_TIME);
        if (cat == null) return 0;
        return cat;
    }
    
    /**
     * Get create time as a Long
     */
    public Long getCreateTimeStampLong()
    {
        return getLong(PARAM_TIME);
    }
    
    /**
     * Set category
     */
    public void setCategory(int nCategory)
    {
        setInteger(PARAM_CATEGORY, nCategory);
    }
    
    /**
     * Get message category
     */
    public int getCategory()
    {
        Integer cat = getInteger(PARAM_CATEGORY);
        if (cat == null) return CAT_NONE;
        return cat;
    }
    
    /**
     * Get from IP (set in BaseServlet)
     */
    public String getFromIP()
    {
        return getString(PARAM_FROM_IP);
    }
    
    /**
     * Set from IP
     */
    public void setFromIP(String s)
    {
        setString(PARAM_FROM_IP, s);
    }

    /**
     * Set exception string
     */
    public void setException(String sException)
    {
        setString(PARAM_EXCEPTION_MESSAGE, sException);
    }
    
    /**
     * Get exception string
     */
    public String getException()
    {
        return getString(PARAM_EXCEPTION_MESSAGE);
    }

    /**
     * Set donohoe digital exception string
     */
    public void setDDException(String sException)
    {
        setString(PARAM_DDEXCEPTION_MESSAGE, sException);
    }
    
    /**
     * Get donohoe digital exception string
     */
    public String getDDException()
    {
        return getString(PARAM_DDEXCEPTION_MESSAGE);
    }

    /**
     * Set application error string
     */
    public void setApplicationErrorMessage(String sMsg)
    {
        setString(PARAM_ERROR_MESSAGE, sMsg);
    }

    /**
     * Get application error string
     */
    public String getApplicationErrorMessage()
    {
        return getString(PARAM_ERROR_MESSAGE);
    }

    /**
     * Set application status string
     */
    public void setApplicationStatusMessage(String sMsg)
    {
        setString(PARAM_STATUS_MESSAGE, sMsg);
    }

    /**
     * Get appliation status string
     */
    public String getApplicationStatusMessage()
    {
        return getString(PARAM_STATUS_MESSAGE);
    }

    private List<MessageData> getDataList()
    {
        if (msgdata_ == null)
        {
            msgdata_ = new ArrayList<MessageData>();
        }
        return msgdata_;
    }
    /**
     * get string data chunk
     */
    public void addData(byte[] data)
    {
        if (data == null) return;
        getDataList().add(new MessageData(data));
    }
    
    /**
     * add string data chunk
     */
    public void addData(String sData)
    {
        if (sData == null) return;
        getDataList().add(new MessageData(Utils.encode(sData)));
    }
    
    /**
     * add string data chunk
     */
    public void addData(File fData)
    {
        if (fData == null) return;
        getDataList().add(new MessageData(fData));
    }
    
    /**
     * add string data chunk
     */
    public void addData(File fDatas[])
    {
        if (fDatas == null || fDatas.length == 0) return;
        List<MessageData> msgdata = getDataList();

        for (File fData : fDatas)
        {
            msgdata.add(new MessageData(fData));
        }
    }
    
    /**
     * Clear data chunks
     */
    public void clearData()
    {
        if (msgdata_ != null)
        {
            msgdata_.clear();
        }
    }
    
    /**
     * Get number of data chunks
     */
    public int getNumData()
    {
        if (msgdata_ == null) return 0;
        return msgdata_.size();
    }
    
    /**
     * Get data chunk of given index.  If data was added as
     * a file, null is returned (it isn't expected you would
     * want to fetch the file once added -- on the other size
     * when the message is received, it is byte data)
     */
    public byte[] getDataAt(int i)
    {
        if (msgdata_ == null) return null;
        return msgdata_.get(i).bytedata_;
    }
    
    /**
     * Return data chunk of given index as a string
     */
    public String getDataAtAsString(int i)
    {
        if (msgdata_ == null) return null;
        byte[] data = getDataAt(i);
        if (data == null) return null;
        
        return Utils.decode(data);
    }
    
    /**
     * Get 1st message data chunk
     */
    public byte[] getData()
    {
        return getDataAt(0);
    }
        
    /**
     * Get 1st message data chunk as a String
     */
    public String getDataAsString()
    {
        return getDataAtAsString(0);
    }
    
    /**
     * Set message status - used on client side to indicate
     * status of a communication with server.  This value
     * is not sent when the message is transmitted.
     *
     * @see DDMessageListener
     */
    public void setStatus(int nStatus)
    {
        nStatus_ = nStatus;
    }
    
    /**
     * Get status.
     *
     * @see DDMessageListener
     */
    public int getStatus()
    {
        return nStatus_;
    }
    
    /**
     * Get params marshalled from tokenized list
     */
    private String marshalParams()
    {
        TokenizedList list = new TokenizedList();
        NameValueToken.loadNameValueTokensIntoList(list, this);
        return list.marshal(state_);
    }
    
    /** 
     * Recreate params from marshalled tokenized list
     */
    private void demarshalParams(String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state_, sData);
        NameValueToken.loadNameValueTokensIntoMap(list, this);
    }
    
    /**
     * Print this message out
     */
    public void debugPrint()
    {
        if (nStatus_ != DDMessageListener.STATUS_NONE) logger.debug("MSG-Status: " + nStatus_);
        logger.debug("MSG-Params: " + super.toString());
        
        
        if (msgdata_ == null || msgdata_.isEmpty())
        {
            logger.debug("MSG-Data: NONE");
        }
        else
        {
            String sData;
            for (int i = 0; i < msgdata_.size(); i++)
            {
                sData = getDataAtAsString(i);
                logger.debug("MSG-Data[" + i +"]: " + sData.length() + " bytes of data, displayed below:");
                StringTokenizer tok = new StringTokenizer(sData,"\n");
                while (tok.hasMoreTokens())
                {
                    logger.debug(tok.nextToken());
                }
            }
        }
    }
    
    /**
     * Params as string for debugging
     */
    public String toStringParams()
    {
        return super.toString();
    }
    
    @Override
    public String toString()
    {
        return toString(true);
    }
    
    /**
     * String for debugging
     */
    public String toString(boolean bOutputData)
    {
        StringBuilder sb = new StringBuilder();
        if (nStatus_ != DDMessageListener.STATUS_NONE)
        {
            sb.append("STATUS: ");
            sb.append(nStatus_);
            sb.append(' ');
        }
        sb.append("PARAMS: ");
        sb.append(super.toString());
        
        int nNum = getNumData();
        if (nNum == 0)
        {
            sb.append(" DATA: (none)");
        }
        else
        {
            String sData;
            for (int i = 0; i < msgdata_.size(); i++)
            {
                sData = getDataAtAsString(i);
                sb.append(" DATA[").append(i).append("]: ");
                
                if (bOutputData)
                {
                    sb.append(sData);
                }
                else
                {
                    sb.append('(').append(sData == null ? 0 : sData.length()).append(" bytes)");
                }
            }
        }
        return sb.toString();
    }

    /**
     * size of data for debugging
     */
    public String toStringSize()
    {
        if (msgdata_ != null)
        {
            StringBuilder sb = new StringBuilder();
            String sData;
            for (int i = 0; i < msgdata_.size(); i++)
            {
                sData = getDataAtAsString(i);
                if (i > 0) sb.append(", ");
                sb.append("DATA[").append(i).append("]: ");
                sb.append('(').append(sData == null ? 0 : sData.length()).append(" bytes)");
            }
            return sb.toString();
        }

        return "[NO DATA]";
    }

    /**
     * Write this message out to the given writer.  Sync added
     * due to possible simulaneous write attempts in DD Poker.
     */
    public synchronized void write(OutputStream output) throws IOException
    {    
        // data
        if (msgdata_ != null && !msgdata_.isEmpty())
        {
            int nNumData = msgdata_.size();
            DMArrayList<Integer> sizes = new DMArrayList<Integer>(nNumData);
            MessageData data;
            
            for (int i = 0; i < nNumData; i++)
            {
                data = msgdata_.get(i);
                sizes.add(data.nSize_);
            }
            setList(PARAM_NUM_CHUNKS, sizes);
        }
        
        // params
        String sParams = marshalParams();
        byte[] baparam = Utils.encode(sParams);
        output.write(baparam);
        
        // remove list data
        removeList(PARAM_NUM_CHUNKS);
 
        // delim
        for (int i = 0; i < DELIM_CNT; i++)
        {
            output.write(DELIM);
        }
        
        // data chunks
        if (msgdata_ != null && !msgdata_.isEmpty())
        {
            int nNumData = msgdata_.size();
            FileChannel in = null;
            MessageData data;
            WritableByteChannel out = Channels.newChannel(output);
            long nSize;
            
            for (int i = 0; i < nNumData; i++)
            {
                data = msgdata_.get(i);
                nSize = data.nSize_;
                
                if (nSize == 0) continue;
                
                if (data.filedata_ != null)
                {
                    try
                    {
                        in = new FileInputStream(data.filedata_).getChannel();
                        long nNum = in.transferTo(0, nSize, out);
                        if (nNum != nSize)
                        {
                            throw new ApplicationError(ErrorCodes.ERROR_CREATE,
                                    "Failed to write all data", data.filedata_.getAbsolutePath(), null);
                        }
                    }
                    finally
                    {
                        in.close();
                    }
                }
                else
                {
                    output.write(data.bytedata_);
                }
            }
        }
    }
    
    /**
     * parse byte data into params and data.  Length is ignored.
     */
    public void read(InputStream input, int nLength) throws IOException
    {
        int nRead = 0;
        boolean bDoneParams = false;
        boolean bEmpty = true;
        
        int n;
        DDByteArrayOutputStream out = new DDByteArrayOutputStream(500);
         
        try {
            // load params
            char c;
            int nDelimCnt = 0;
                       
            while (!bDoneParams)
            {
                n = input.read();
                if (n == -1)
                {
                    throw new EOFException("End of file decoding header portion of DDMessage");
                }
                c = (char) n;
                bEmpty = false;
                nRead += n;

                //logger.debug("Read: " + c + " #"+nRead);
                if (c == DELIM)
                {
                    nDelimCnt++;
                    // done when all delims read in a row
                    if (nDelimCnt == DELIM_CNT)
                    {
                        bDoneParams = true;
                    }
                }
                else
                {
                    // if we previously got a delim char, but now we have
                    // a non-delim char, then write the previous delim chars
                    // received and reset count
                    if (nDelimCnt > 0)
                    {
                        for (int i = 0; i < nDelimCnt; i++)
                        {
                            out.write(DELIM);
                        }
                        nDelimCnt = 0;
                    }
                    out.write(n);
                }
            }

            String sParams = Utils.decode(out.getBuffer(), 0, out.size());
            //logger.debug("PARAMS: " + sParams);
            demarshalParams(sParams);

            // get rest of data
            DMArrayList<Integer> sizes = (DMArrayList<Integer>) removeList(PARAM_NUM_CHUNKS);
            if (sizes == null) return;
            int length;
            int nNum = sizes.size();
            byte[] bytedata;

            for (int i = 0; i < nNum; i++)
            {
                nRead = 0;
                length = sizes.get(i);
                bytedata = new byte[length];
                while (nRead != length)
                {
                    n = input.read(bytedata, nRead, length-nRead);
                    if (n == -1)
                    {
                        throw new EOFException("End of file decoding message data portion of DDMessage");
                    }   
                    nRead += n;
                }
                //logger.debug("TOTAL READ: " + nRead);
                addData(bytedata);
            }
        }
        catch (EOFException eof)
        {
            // can be caught if no data returned (empty message, which is okay)
            // otherwise, it is an error if happens after started processing
            if (!bEmpty) throw new ApplicationError(eof);
        }
    }
    
    public void demarshal(MsgState state, String sData)
    {
        InputStream stream = new ByteArrayInputStream(Utils.encode(sData));
        
        try {
            read(stream, sData.length());
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }
    
    public String marshal(MsgState state)
    {
        DDByteArrayOutputStream out = new DDByteArrayOutputStream();
        
        try {
            write(out);
            return Utils.decode(out.getBuffer(), 0, out.size());
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
            
    }
  
    /////
    ///// Version 
    /////
    
    /**
     * software version
     */
    protected static Version version_ = null;
    
    /**
     * Store version for sending in message
     */
    public static void setDefaultVersion(Version version)
    {
        version_ = version;
    }
    
    /**
     * Get version
     */
    public static Version getDefaultVersion()
    {
        return version_;
    }
    
    /////
    ///// License 
    /////
    
    /**
     * software key for public viewing
     */
    private static String key_ = null;
    
    /**
     * Store key for sending in public message
     */
    public static void setDefaultKey(String key)
    {
        key_ = key;
    }
    
    /**
     * Get key
     */
    private static String getDefaultKey()
    {
        return key_;
    }

    /**
     * software key
     */
    private static String realkey_ = null;

    /**
     * Store key for sending in server message
     */
    public static void setDefaultRealKey(String key)
    {
        realkey_ = key;
    }

    /**
     * Get key
     */
    static String getDefaultRealKey()
    {
        return realkey_;
    }

    /////
    ///// MsgState
    /////

    private static MsgState state_ = null;

    /**
     * when a DDMessage is marshaled/demarshaled it may contain
     * objects that need a MsgState to properly marshal/demarshal too (items
     * that are normally contained in a save game file).
     * Games can set a GameState to be used during DDMessage marshaling.
     */
    public static void setMsgState(MsgState state)
    {
        state_ = state;
    }
    
    /**
     * Get state currently used
     */
    public static MsgState getMsgState()
    {
        return state_;
    }
}
