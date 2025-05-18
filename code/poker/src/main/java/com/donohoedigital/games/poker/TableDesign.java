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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;

import java.awt.*;
import java.io.*;
import java.util.List;

public class TableDesign extends BaseProfile 
{

    public static final String PROFILE_BEGIN = "table";
    public static final String TABLE_DESIGN_DIR = "tables";

    private static final Color DEFAULT_TOP = new Color(38,175,23);
    private static final Color DEFAULT_BOTTOM = new Color(20,82,1);

    private Color colorTop_;
    private Color colorBottom_;

    public void setColorTop(Color c)
    {
        colorTop_ = c;
    }

    public Color getColorTop()
    {
        return colorTop_;
    }

    public void setColorBottom(Color c)
    {
        colorBottom_ = c;
    }

    public Color getColorBottom()
    {
        return colorBottom_;
    }

    public TableDesign() {
        this(null, null);
    }

    public TableDesign(String name) {
        this(null, name);
    }

    public TableDesign(File file, boolean bFull)
    {
        super(file, bFull);
    }

    public TableDesign(TableDesign proto) {
        this(proto, null);
    }

    public TableDesign(TableDesign proto, String sName)
    {
        super(sName);

        if (proto == null) {
            colorTop_ = DEFAULT_TOP;
            colorBottom_ = DEFAULT_BOTTOM;
        } else {
            colorTop_ = proto.colorTop_;
            colorBottom_ = proto.colorBottom_;
        }
    }

    /**
     * Get begin part of profile name
     */
    protected String getBegin()
    {
        return PROFILE_BEGIN;
    }

    /**
     * Get name of directory to store profiles in
     */
    protected String getProfileDirName()
    {
        return TABLE_DESIGN_DIR;
    }

    /**
     *  Get profile list
     */
    protected List<BaseProfile> getProfileFileList() {
        return getProfileList();
    }

    /**
     * Get list of save files in save directory
     */
    public static List<BaseProfile> getProfileList()
    {
        return BaseProfile.getProfileList
                (TABLE_DESIGN_DIR, Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), TableDesign.class, false);
    }

    /**
     * return File for given profile name
     */
    public static File getProfileFile(String sFileName)
    {
        return new File(TableDesign.getProfileDir(TableDesign.TABLE_DESIGN_DIR), sFileName);

    }

    public String toHTML()
    {
        return
                "<DIV>" +
               // "<DDHANDGROUP CARDS=\"" + getSummary().replaceAll(" ", "") + "\">" +
                "TODO: html summary" +
                "</DIV>";
    }

    public String toString()
    {
        return getName();
    }


    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        TokenizedList list = new TokenizedList();
        list.demarshal(null, buf.readLine());

        colorTop_ = Utils.getHtmlColorAlpha(list.removeStringToken());
        colorBottom_ = Utils.getHtmlColorAlpha(list.removeStringToken());
    }

     public void write(Writer writer) throws IOException
    {
        super.write(writer);
        TokenizedList list = new TokenizedList();

        list.addToken(Utils.getHtmlColorAlpha(colorTop_));
        list.addToken(Utils.getHtmlColorAlpha(colorBottom_));

        writer.write(list.marshal(null));
        writeEndEntry(writer);
    }
}
