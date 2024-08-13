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
package com.donohoedigital.base;

import java.util.zip.*;
import java.util.*;
import java.io.*;

public class ZipUtil extends ZipFile
{
    Enumeration eEntries;
    ZipEntry zeCurrent;

    public ZipUtil(File f) throws IOException, ZipException
    {
        super(f);
        init();
    }

    /**
     * After construction, call init to setup ZipUtil for use.  This method
     * determines all the entries, etc.
     */
    private void init()
    {
        eEntries = this.entries();
    }

    /*
    ** Returns next entry name, null when no more
    */
    public String getNextFileName()
    {
        if (eEntries == null) return null;

        if (eEntries.hasMoreElements())
        {
            zeCurrent = (ZipEntry) eEntries.nextElement();
            return zeCurrent.getName();
        }

        zeCurrent = null;
        return null;
    }

    /**
     * Returns byte array containing data of current
     * entry (the contents of file name returned by
     * most recent call to getNextFileName()
     */
    public byte[] getByteContents() throws IOException
    {
        if (zeCurrent == null) return null;

        // TODO: what to do about loss of precision?  We shouldn't have
        // files bigger than MAXINT
        byte[] bytes = new byte[(int)zeCurrent.getSize()];
        InputStream is = this.getInputStream(zeCurrent);
        is.read(bytes, 0, (int)zeCurrent.getSize());

        return bytes;
    }

    /**
     * Return StringBuilder containing file contents
     */
    public StringBuilder getStringBufferContents() throws IOException
    {
        if (zeCurrent == null) return null;

        // TODO: what to do about loss of precision?  We shouldn't have
        // files bigger than MAXINT
        StringBuilder sb = new StringBuilder((int)zeCurrent.getSize());

        BufferedReader buf = new BufferedReader(new InputStreamReader(
                                        getInputStream(zeCurrent)));
        String sLine;
        while ((sLine = buf.readLine()) != null)
        {
            sb.append(sLine);
            sb.append("\n");
        }
        return sb;
    }

    /**
     * Return vector of String[], a parsed CSV file
     */
    public Vector getParsedCSVContents() throws IOException
    {
        if (zeCurrent == null) return null;

        // TODO: what to do about loss of precision?  We shouldn't have
        // files bigger than MAXINT
        Vector vLines = new Vector();
        String[] sValues;

        BufferedReader buf = new BufferedReader(new InputStreamReader(
                                        getInputStream(zeCurrent)));
        String sLine;
        while ((sLine = buf.readLine()) != null)
        {
            sValues = CSVParser.parseLine(sLine);
            vLines.add(sValues);
        }
        return vLines;
    }

    /**
     * When done, close file
     */
    public void close()
    {
        try {
            super.close();
        }
        catch (IOException e) {
            // who cares
        }
        zeCurrent = null;
        eEntries = null;
    }
}