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
package com.donohoedigital.base;

import java.util.*;

/**
 * This class is used to make hashtable access easier (less casting by
 * caller).  You will get class cast exceptions if object you are retrieving
 * doesn't match what is expected.
 */
public class TypedHashMap extends TreeMap<String, Object>
{
    /**
     * return a string param, sDefault if not found
     */
    public String getString(String sName, String sDefault)
    {
        String s = (String) get(sName);
        if (s == null) s = sDefault;

        return s;
    }
    
    /**
     * return a string param, null if not found
     */    
    public String getString(String sName)
    {
        return getString(sName, null);
    }
    
    /**
     * insert a string param
     */
    public void setString(String sName, String sValue)
    {
        put(sName, sValue);
    }

    /**
     * remove a string param
     */
    public String removeString(String sName)
    {
        return (String) remove(sName);
    }
    
    /**
     * return an integer param, nDefault if not found
     */
    public int getInteger(String sName, int nDefault)
    {
        Integer n = (Integer) get(sName);
        if (n == null) return nDefault;
        return n;
    }
    
    /**
     * return an integer param, nDefault if not found,
     * bounds are checked (min/max)
     */
    public int getInteger(String sName, int nDefault, int nMin, int nMax)
    {
        Integer n = (Integer) get(sName);
        if (n == null) return nDefault;
        int nValue = n;
        if (nValue < nMin) nValue = nMin;
        if (nValue > nMax) nValue = nMax;
        return nValue;
    }
    
    /**
     * return an integer param, null if not found
     */
    public Integer getInteger(String sName)
    {
        return (Integer) get(sName);
    }

    /**
     * insert an integer param
     */
    public void setInteger(String sName, Integer iValue)
    {
        put(sName, iValue);
    }
    
    /**
     * remove an integer param
     */
    public Integer removeInteger(String sName)
    {
        return (Integer) remove(sName);
    }
    
    /**
     * return an long param, nDefault if not found
     */
    public long getLong(String sName, long nDefault)
    {
        Long n = (Long) get(sName);
        if (n == null) return nDefault;
        return n;
    }
    
    /**
     * return an long param, null if not found
     */
    public Long getLong(String sName)
    {
        return (Long) get(sName);
    }

    /**
     * Return a long as a date, null if not found
     */
    public Date getLongAsDate(String sName)
    {
        Long value = getLong(sName);
        if (value == null) return null;
        return new Date(value);
    }

    /**
     * insert an long param
     */
    public void setLong(String sName, Long iValue)
    {
        put(sName, iValue);
    }

    /**
     * insert a date as a long
     */
    public void setLongFromDate(String sName, Date date)
    {
        if (date == null)
        {
            remove(sName);
            return;
        }
        setLong(sName, date.getTime());
    }
    
    /**
     * remove a long param
     */
    public Long removeLong(String sName)
    {
        return (Long) remove(sName);
    }
    
    /**
     * return a double param, dDefault if not found
     */
    public double getDouble(String sName, double dDefault)
    {
        Double n = (Double) get(sName);
        if (n == null) return dDefault;
        return n;
    }
    
    /**
     * return an double param, null if not found
     */
    public Double getDouble(String sName)
    {
        return (Double) get(sName);
    }

    /**
     * insert a double param
     */
    public void setDouble(String sName, Double dValue)
    {
        put(sName, dValue);
    }
    
    /**
     * remove a double param
     */
    public Double removeDouble(String sName)
    {
        return (Double) remove(sName);
    }
    
    /**
     * return a boolean param, bDefault if not found
     */
    public boolean getBoolean(String sName, boolean bDefault)
    {
        Boolean b = (Boolean) get(sName);
        if (b == null) return bDefault;
        return b;
    }
    
    /**
     * return an boolean param, null if not found
     */
    public Boolean getBoolean(String sName)
    {
        return (Boolean) get(sName);
    }
    
    /**
     * insert a boolean param
     */
    public void setBoolean(String sName, Boolean bValue)
    {
        put(sName, bValue);
    }
        
    /**
     * remove a boolean param
     */
    public Boolean removeBoolean(String sName)
    {
        return (Boolean) remove(sName);
    }
    
    /**
     * return an ArrayList param
     */
    public List<?> getList(String sName)
    {
        return (List<?>) get(sName);
    }
    
    /**
     * insert a list param
     */
    public void setList(String sName, List<?> aValue)
    {
        put(sName, aValue);
    }
    
    /**
     * remove a list param
     */
    public List<?> removeList(String sName)
    {
        return (List<?>) remove(sName);
    }
    
    /**
     * Return a generic object
     */
    public Object getObject(String sName)
    {
        return get(sName);
    }
    
    /**
     * insert an object param
     */
    public void setObject(String sName, Object oValue)
    {
        put(sName, oValue);
    }
    
    /**
     * remove a object param
     */
    public Object removeObject(String sName)
    {
        return remove(sName);
    }
    
    /**
     * String representation of contents of this map
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = keySet().iterator();
        String sName;
        List<?> list;
        Object oValue;
        while (iter.hasNext())
        {
            sName = iter.next();
            oValue = get(sName);
            if (sb.length() > 0) sb.append(", ");
            sb.append(sName);
            sb.append('=');
            if (oValue instanceof List)
            {
                list = (List<?>) oValue;
                sb.append('[');
                for (int i = 0; i < list.size(); i++)
                {
                    if (i > 0) sb.append(", ");
                    
                    sb.append(list.get(i));
                }
                sb.append(']');
            }
            else
            {
                sb.append(oValue);
            }
        }
        
        return sb.toString();
    }
}
