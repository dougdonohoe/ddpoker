/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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

package com.donohoedigital.comms;

import com.donohoedigital.base.*;

import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class MsgState 
{
    /**
     * Assigned IDs start at this number.  Everything below is reserved
     */
    private static final int START_ID = 1000;
    private static final int NEXT_ID_NOTSET = -1;
    
    // ids
    private Map<Object, Integer> ids_ = new HashMap<Object, Integer>();
    private Map<Integer, Object> reverseids_ = new HashMap<Integer, Object>();
    private int nextid_ = NEXT_ID_NOTSET;
    
    // class info
    private TokenizedList classNames_;
    private Map<String, Integer> classids_ = new HashMap<String, Integer>();
    private Map<Integer, String> reverseclassids_ = new HashMap<Integer, String>();
    private int nextclassid_ = 0;
    
    /**
     * Default constructor
     */
    public MsgState()
    {
    }

    /**
     * Return starting id
     */
    protected int getStartId()
    {
        return START_ID;
    }

    /**
     * Get next id
     */
    private int getNextId()
    {
        if (nextid_ == NEXT_ID_NOTSET)
        {
            nextid_ = getStartId();
        }
        int next = nextid_;
        nextid_++;
        return next;
    }
    
    /**
     * set list to be used for classnames
     */
    protected void setClassNames(TokenizedList classNames)
    {
        classNames_ = classNames;
    }
    
    ////
    //// object id methods
    ////
    
    /**
     * Reset game state for use again
     */
    protected void resetIds()
    {
        // object ids
        nextid_ = NEXT_ID_NOTSET;
        ids_.clear();
        reverseids_.clear();
        
        // class ids
        nextclassid_ = 0;
        classids_.clear();
        reverseclassids_.clear();
        classNames_ = null;
    }
    
    /**
     * Get unique id for given object (saving phase)
     */
    public Integer getId(Object o)
    {
        if (o == null) return null;
       
        Integer id = ids_.get(o);
        if (id == null)
        {
            if (o instanceof ObjectID)
            {
                id = ((ObjectID) o).getObjectID();
            }
            else
            {
                id = getNextId();
            }
            ids_.put(o, id);
            reverseids_.put(id, o);
        }
        return id;
    }
    
    /**
     * Add id for this to list
     */
    public void setId(ObjectID oid)
    {
        setId(oid, oid.getObjectID());
    }
    
    /**
     * Store an id for an object (loading phase)
     */
    public void setId(Object o, Integer id)
    {
        ApplicationError.assertTrue(ids_.get(o) == null, "Object already has id", o);
        ApplicationError.assertTrue(reverseids_.get(id) == null, "Id already in use", id);
        ids_.put(o, id);
        reverseids_.put(id, o);
    }

    /**
     *  return true if an id exists for this object
     */
    public boolean isIdUsed(Object o)
    {
        return ids_.get(o) != null;
    }

    /**
     * Get object based on an id.  Throws exception if no object for id
     */
    public Object getObject(Integer id)
    {
        if (id == null) return null;
        Object ret = reverseids_.get(id);
        ApplicationError.assertNotNull(ret, "No object for id", id);
        return ret;
    }
    
    /**
     * Get object based on id, okay if not there
     */
    public Object getObjectNullOkay(Integer id)
    {
        if (id == null) return null;
        return reverseids_.get(id);
    }

    /**
     * Get unique id for given class name
     */
    public Integer getClassId(String sClass)
    {
        if (sClass == null) return null;
        Integer id = classids_.get(sClass);
        if (id == null)
        {
            id = nextclassid_;
            nextclassid_++;
            classids_.put(sClass, id);
            reverseclassids_.put(id, sClass);
            classNames_.addToken(sClass);
        }
        return id;
    }
    
    /**
     * init class-id mapping from game state entry
     */
    protected void initClassIdsFromTokenizedList(TokenizedList entry)
    {
        classNames_ = entry;
        int nId = 0;
        
        while (entry.hasMoreTokens())
        {
            setClassId(entry.removeStringToken(), nId);
            nId++;
        }
    }
    
    /**
     * Store an id for a classname (loading phase)
     */
    private void setClassId(String sClass, Integer id)
    {
        ApplicationError.assertTrue(classids_.get(sClass) == null, "Class already has id", sClass);
        ApplicationError.assertTrue(reverseclassids_.get(id) == null, "Class id already in use", id);
        classids_.put(sClass, id);
        reverseclassids_.put(id, sClass);
    }
    
    /**
     * Get class based on an id
     */
    public String getClassName(Integer id)
    {
        if (id == null) return null;
        return reverseclassids_.get(id);
    }
}
