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
 * DataMarshaller.java
 *
 * Created on February 7, 2003, 2:20 PM
 */

package com.donohoedigital.comms;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
@SuppressWarnings({"PublicInnerClass"})
public class DataMarshaller
{
    //private static Logger logger = LogManager.getLogger(DataMarshaller.class);

    /**
     * Marshal given DataMarshal to string
     */
    public static String marshal(DataMarshal dm)
    {
        return marshal(null, dm);
    }

    /**
     * Marshal given DataMarshal to string
     */
    public static String marshal(MsgState state, DataMarshal dm)
    {
        ApplicationError.assertNotNull(dm, "DataMarshal must not be null");
        char TYPE = getTypeForCoder(dm);
        return TYPE + dm.marshal(state);
    }

    /**
     * Demarshal string to associated object
     */
    public static DataMarshal demarshal(String sData)
    {
        return demarshal(null, sData);
    }

    /**
     * Demarshal string to associated object
     */
    public static DataMarshal demarshal(MsgState state, String sData)
    {
        Class<?> dmType = getCoderForType(sData.charAt(0));

        try
        {
            DataMarshal dm = (DataMarshal) ConfigUtils.newInstance(dmType);
            dm.demarshal(state, sData.substring(1));
            return dm;
        }
        catch (ClassCastException cce)
        {
            throw new ApplicationError(cce);
        }
    }

    ////
    //// Registered demarshallers 
    ////
    private static final Map<Character, Class<? extends DataMarshal>> typeToCoder_ = new HashMap<>();
    private static final Map<Class<? extends DataMarshal>, Character> coderToType_ = new HashMap<>();

    /*
     * Find coders upon class initialization
     */
    static
    {
        scanForCoders();
    }

    /**
     * scan classpath for classes annotated with DataCoder
     */
    public static void scanForCoders()
    {
        MatchingResources resources = new MatchingResources("classpath*:com/donohoedigital/**/*.class");
        Set<Class<?>> codes = resources.getAnnotatedMatches(DataCoder.class);
        for (Class<?> c : codes)
        {
            registerCoder(c);
        }
    }

    /**
     * Register given class
     */
    @SuppressWarnings({"unchecked"})
    private static void registerCoder(Class<?> c)
    {
        if (!(DataMarshal.class.isAssignableFrom(c)))
        {
            throw new ApplicationError("Class should implement DataMarshal: " + c);
        }
        Class<? extends DataMarshal> dm = (Class<? extends DataMarshal>) c;
        DataCoder dc = dm.getAnnotation(DataCoder.class);

        //logger.info("Found DataCoder (" + dc.value() + ") for "+ dm.getName());
        registerCoderForType(dc.value(), dm);
    }

    /**
     * Register a character as a type and associated class
     */
    private static void registerCoderForType(char cType, Class<? extends DataMarshal> cClass)
    {
        ApplicationError.assertTrue(DataMarshal.class.isAssignableFrom(cClass), "cClass must implement DataMarshal", cClass);
        Class<? extends DataMarshal> cExist = typeToCoder_.get(cType);
        ApplicationError.assertTrue(cExist == null, "Duplicate definition for DataMarshal", cType);

        typeToCoder_.put(cType, cClass);
        coderToType_.put(cClass, cType);
    }

    /**
     * Get class associated with type
     */
    private static Class<? extends DataMarshal> getCoderForType(char cType)
    {
        Class<? extends DataMarshal> cExist = typeToCoder_.get(cType);
        ApplicationError.assertNotNull(cExist, "Missing definition for DataMarshal", cType);
        return cExist;
    }

    /**
     * Return char type for given data marshaller
     */
    private static char getTypeForCoder(DataMarshal cCoder)
    {
        Class<? extends DataMarshal> cClass = cCoder.getClass();
        Character cType = coderToType_.get(cClass);
        ApplicationError.assertNotNull(cType, "No type for DataMarshal", cCoder.getClass());
        return cType;
    }

    ////
    //// marshaller wrapper classes
    ////

    public interface DMWrapper extends DataMarshal
    {
        Object value();
    }

    /**
     * Marshallable integer
     */
    @DataCoder('i')
    public static class DMInteger implements DMWrapper
    {
        int n;

        /**
         * Empty constructor needed for demarshalling
         */
        @SuppressWarnings("unused")
        public DMInteger()
        {
        }

        public DMInteger(int n)
        {
            this.n = n;
        }

        public DMInteger(Integer n)
        {
            this.n = n;
        }

        public Object value()
        {
            return n;
        }

        public void demarshal(MsgState state, String sData)
        {
            try
            {
                n = Integer.parseInt(sData);
            }
            catch (NumberFormatException nfe)
            {
                throw new ApplicationError(nfe);
            }
        }

        public String marshal(MsgState state)
        {
            return toString();
        }

        @Override
        public String toString()
        {
            return Integer.toString(n);
        }
    }

    /**
     * Marshallable null
     */
    @DataCoder('~')
    public static class DMNull implements DMWrapper
    {
        public Object value()
        {
            return null;
        }

        public void demarshal(MsgState state, String sData)
        {
        }

        public String marshal(MsgState state)
        {
            return "";
        }
    }

    /**
     * Marshallable string
     */
    @DataCoder('s')
    public static class DMString implements DMWrapper
    {
        String s;

        /**
         * Empty constructor needed for demarshalling
         */
        @SuppressWarnings("unused")
        public DMString()
        {
        }

        public DMString(String s)
        {
            this.s = s;
        }

        public Object value()
        {
            return s;
        }

        public void demarshal(MsgState state, String sData)
        {
            s = sData;
        }

        public String marshal(MsgState state)
        {
            return s;
        }

        @Override
        public String toString()
        {
            return s;
        }
    }

    /**
     * Marshallable boolean
     */
    @DataCoder('b')
    public static class DMBoolean implements DMWrapper
    {
        boolean b;

        /**
         * Empty constructor needed for demarshalling
         */
        @SuppressWarnings("unused")
        public DMBoolean()
        {
        }

        public DMBoolean(boolean b)
        {
            this.b = b;
        }

        public DMBoolean(Boolean b)
        {
            this.b = b;
        }

        public Object value()
        {
            return b;
        }

        public void demarshal(MsgState state, String sData)
        {
            Boolean bp = Utils.parseBoolean(sData);
            ApplicationError.assertNotNull(bp, "Unable to demarshal to boolean", sData);
            b = bp;
        }

        public String marshal(MsgState state)
        {
            return toString();
        }

        @Override
        public String toString()
        {
            if (b) return "+";
            else return "-";
        }
    }

    /**
     * Marshallable double
     */
    @DataCoder('d')
    public static class DMDouble implements DMWrapper
    {
        double d;

        /**
         * Empty constructor needed for demarshalling
         */
        @SuppressWarnings("unused")
        public DMDouble()
        {
        }

        public DMDouble(double d)
        {
            this.d = d;
        }

        public DMDouble(Double d)
        {
            this.d = d;
        }

        public Object value()
        {
            return d;
        }

        public void demarshal(MsgState state, String sData)
        {
            try
            {
                d = Double.parseDouble(sData);
            }
            catch (NumberFormatException nfe)
            {
                throw new ApplicationError(nfe);
            }
        }

        public String marshal(MsgState state)
        {
            return toString();
        }

        @Override
        public String toString()
        {
            return Double.toString(d);
        }
    }

    /**
     * Marshallable long
     */
    @DataCoder('l')
    public static class DMLong implements DMWrapper
    {
        long n;

        /**
         * Empty constructor needed for demarshalling
         */
        @SuppressWarnings("unused")
        public DMLong()
        {
        }

        public DMLong(long n)
        {
            this.n = n;
        }

        public DMLong(Long n)
        {
            this.n = n;
        }

        public Object value()
        {
            return n;
        }

        public void demarshal(MsgState state, String sData)
        {
            try
            {
                n = Long.parseLong(sData);
            }
            catch (NumberFormatException nfe)
            {
                throw new ApplicationError(nfe);
            }
        }

        public String marshal(MsgState state)
        {
            return toString();
        }

        @Override
        public String toString()
        {
            return Long.toString(n);
        }
    }

    ////
    //// For use during installer build time
    ////

    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public static void main(String[] args)
    {
        for (Class<?> clazz : coderToType_.keySet())
        {
            String name = clazz.getName();
            System.out.println(name);
        }
    }

}
