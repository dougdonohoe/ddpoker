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
 * DMArrayList.java
 *
 * Created on March 8, 2003, 4:58 PM
 */

package com.donohoedigital.comms;

import org.apache.logging.log4j.*;

import java.util.*;

/**
 * This array list is marshallable and can marshal the following
 * contents:  String, Integer, Double, Boolean and anything that
 * implements DataMarshal
 *
 * @author  donohoe
 */
@DataCoder('a')
public class DMArrayList<E> extends ArrayList<E> implements DataMarshal
{    
    static Logger logger = LogManager.getLogger(DMArrayList.class);

    /** 
     * Creates a new instance of DMArrayList 
     */
    public DMArrayList()
    {
    }
    
    /**
     * Create a list of the given size
     */
    public DMArrayList(int initSize)
    {
        super(initSize);
    }
    
    /** 
     * Creates a new instance of DMArrayList 
     */
    public DMArrayList(DMArrayList<E> copy)
    {
        super(copy);
    }

    /**
     * Recreate list from marshalled data
     */
    @SuppressWarnings({"unchecked"})
    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        int nNum = list.removeIntToken();
        
        DataMarshal dmValue;
        for (int i = 0; i < nNum; i++)
        {
            dmValue = list.removeToken();
            if (dmValue instanceof DataMarshaller.DMWrapper)
            {
                add((E)((DataMarshaller.DMWrapper) dmValue).value());
            }
            else
            {
                add((E)dmValue);
            }
        }
    }

    /** 
     * Save list to string
     */
    public String marshal(MsgState state) {
        TokenizedList list = new TokenizedList();
        list.addToken(size());
        Object oValue;
        for (int i = 0; i < size(); i++)
        {
            oValue = get(i);
            if (oValue == null)
            {
                list.addToken(new DataMarshaller.DMNull());
            }
            else if (oValue instanceof Integer)
            {
                list.addToken(new DataMarshaller.DMInteger((Integer)oValue));
            }
            else if (oValue instanceof Long)
            {
                list.addToken(new DataMarshaller.DMLong((Long)oValue));
            }
            else if (oValue instanceof String)
            {
                list.addToken(new DataMarshaller.DMString((String)oValue));
            }
            else if (oValue instanceof Double)
            {
                list.addToken(new DataMarshaller.DMDouble((Double)oValue));
            }
            else if (oValue instanceof Boolean)
            {
                list.addToken(new DataMarshaller.DMBoolean((Boolean)oValue));
            }
            else if (oValue instanceof DataMarshal)
            {
                list.addToken((DataMarshal)oValue);
            }
            else
            {
                logger.debug("Warning: skipping unsupported array entry #" + i + 
                                " class is "+(oValue != null ? oValue.getClass().getName():"null"));
            }
        }
        return list.marshal(state);
    }  
    
    /**
     * Return list in format [a,b,c]
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < size(); i++)
        {
            if (i > 0) sb.append(',');
            sb.append(get(i).toString());
        }
        sb.append(']');
        return sb.toString();
    }
}
