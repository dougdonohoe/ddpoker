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
 * NameValueToken.java
 *
 * Created on February 7, 2003, 8:57 AM
 */

package com.donohoedigital.comms;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 * Class used by TokenizedList
 *
 * @author  Doug Donohoe
 */
@DataCoder('$')
public class NameValueToken implements DataMarshal
{
    static Logger logger = LogManager.getLogger(NameValueToken.class);
    
    // token types
    public static final char TOKEN_NVT_SEP = '=';
    
    private String sName_;
    private DataMarshal dmValue_;
 
    /**
     * Empty constructor needed for demarshalling
     */
    public NameValueToken() {}
    
    NameValueToken(String sName, String sValue)
    {
        init(sName, new DataMarshaller.DMString(sValue));
    }
        
    NameValueToken(String sName, Integer nValue)
    {
        init(sName, new DataMarshaller.DMInteger(nValue));
    }
    
    NameValueToken(String sName, Long nValue)
    {
        init(sName, new DataMarshaller.DMLong(nValue));
    }

    NameValueToken(String sName, Boolean bValue)
    {
        init(sName, new DataMarshaller.DMBoolean(bValue));
    }
    
    NameValueToken(String sName, Double dValue)
    {
        init(sName, new DataMarshaller.DMDouble(dValue));
    }

    NameValueToken(String sName, DataMarshal dmValue)
    {
        init(sName, dmValue);
    }
    
    private void init(String sName, DataMarshal dmValue)
    {
        sName_ = sName;
        dmValue_ = dmValue;
    }
    
    /**
     * Get the name
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Get the value
     */
    public DataMarshal getValue()
    {
        return dmValue_;
    }
    
    public void demarshal(MsgState state, String sData) 
    {
        EscapeStringTokenizer st = new EscapeStringTokenizer(sData, TOKEN_NVT_SEP);
        sName_ = st.nextToken();
        dmValue_ = DataMarshaller.demarshal(state, st.nextToken());
    }
    
    public String marshal(MsgState state) {
                
        StringBuilder sb = new StringBuilder();
        sb.append(TokenizedList.escape(sName_));
        sb.append(TOKEN_NVT_SEP);
        sb.append(TokenizedList.escape(DataMarshaller.marshal(state, dmValue_)));

        return sb.toString();
    }
    
    ////
    //// Convience method for loading
    ////
    
    /**
     * create a NameValueToken from each entry in the map and insert into the 
     * TokenizedList
     */
    public static void loadNameValueTokensIntoList(TokenizedList entry, AbstractMap<String, Object> info)
    {
        Iterator<String> iter = info.keySet().iterator();
        String sName;
        Object oValue;
        
        while (iter.hasNext())
        {
            sName = iter.next();
            oValue = info.get(sName);
            
            if (oValue == null)
            {
                entry.addNameValueToken(sName,  new DataMarshaller.DMNull());
            }
            else if (oValue instanceof Integer)
            {
                entry.addNameValueToken(sName, (Integer)oValue);
            }
            else if (oValue instanceof Long)
            {
                entry.addNameValueToken(sName, (Long)oValue);
            }
            else if (oValue instanceof String)
            {
                entry.addNameValueToken(sName, (String)oValue);
            }
            else if (oValue instanceof Double)
            {
                entry.addNameValueToken(sName, (Double)oValue);
            }
            else if (oValue instanceof Boolean)
            {
                entry.addNameValueToken(sName, (Boolean)oValue);
            }
            else if (oValue instanceof DataMarshal)
            {
                entry.addNameValueToken(sName, (DataMarshal)oValue);
            }
            else
            {
                logger.debug("Warning: skipping unsupported map entry '" + sName + 
                                "' class is "+(oValue != null ? oValue.getClass().getName():"null"));
            }
        }
    }
    
    /**
     * load remaining entries in a TokenizedList into
     * the given hashmap - all remaining entries are assumed to be 
     * NameValueTokens
     */
    public static void loadNameValueTokensIntoMap(TokenizedList entry, AbstractMap<String, Object> info)
    {
        NameValueToken nvt;
        DataMarshal dmValue;
        String sName;
        while (entry.hasMoreTokens())
        {
            nvt = entry.removeNameValueToken();
            sName = nvt.getName();
            dmValue = nvt.getValue();

            if (dmValue instanceof DataMarshaller.DMWrapper)
            {
                info.put(sName, ((DataMarshaller.DMWrapper) dmValue).value());
            }
            else
            {
                info.put(sName, dmValue);
            }
        }
    }   
}
