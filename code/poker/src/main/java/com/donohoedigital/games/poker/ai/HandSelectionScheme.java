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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;

import java.io.*;
import java.util.*;

public class HandSelectionScheme extends BaseProfile
{
    List<HandGroup> handgroups_;

    public static final String PROFILE_BEGIN = "handselection";
    public static final String HAND_SELECTION_DIR = "handselection";

    private DMTypedHashMap map_;

    public HandSelectionScheme()
    {
        super("");
    }

    public HandSelectionScheme(File file, boolean bFull)
    {
        super(file, bFull);
    }

    public HandSelectionScheme(String sName)
    {
        super(sName);
        map_ = new DMTypedHashMap();
        handgroups_ = new ArrayList<HandGroup>();
    }

    /**
     * New profile copied from given profile, using new name
     */
    public HandSelectionScheme(HandSelectionScheme tp, String sName)
    {
        super(sName);
        map_ = new DMTypedHashMap();
        map_.putAll(tp.map_);

        // remove stuff we don't want to copy
        if (!sName.equals(tp.getName()))
        {
            map_.removeLong("id");
        }

        parseHandGroups();
    }

    /**
     * Get map
     */
    public DMTypedHashMap getMap()
    {
        return map_;
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
        return HAND_SELECTION_DIR;
    }

    /**
     *  Get profile list
     */
    protected List<BaseProfile> getProfileFileList()
    {
        return getProfileList();
    }

    /**
     * Get list of save files in save directory
     */
    public static List<BaseProfile> getProfileList()
    {
        return BaseProfile.getProfileList
                (HAND_SELECTION_DIR, Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), HandSelectionScheme.class, false);
    }

    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        map_ = new DMTypedHashMap();
        map_.demarshal(null, buf.readLine());

        parseHandGroups();
    }

    private void parseHandGroups()
    {
        if (handgroups_ == null)
        {
            handgroups_ = new ArrayList<HandGroup>();
        }
        else
        {
            handgroups_.clear();
        }

        int i = 0;
        String s;

        while ((s = map_.getString("hands" + i)) != null)
        {
            i++;
            String v[] = s.split("\\|");
            handgroups_.add(HandGroup.parse(v[0], Integer.parseInt(v[1])));
        }
    }

    public void write(Writer writer) throws IOException
    {
        super.write(writer);

        if (!map_.containsKey("id")) {
            map_.setLong("id", System.currentTimeMillis());
        }

        for (int i = 0; i < 169; ++i) {
            map_.removeString("hands" + i);
        }

        for (int i = 0; i < handgroups_.size(); ++i) {
            HandGroup group = handgroups_.get(i);

            if (group.getClassCount() > 0) {
                map_.setString("hands" + i, group.getSummary().replaceAll(" ", "") + "|" + Integer.toString(group.getStrength()));
            }
        }

        writer.write(map_.marshal(null));
        writeEndEntry(writer);
    }

    public void removeEmptyGroups()
    {
        for (int i = handgroups_.size()-1; i >= 0; --i)
        {
            if (handgroups_.get(i).getClassCount() == 0) handgroups_.remove(i);
        }
    }

    public void ensureEmptyGroup()
    {
        removeEmptyGroups();

        handgroups_.add(new HandGroup());
    }

    public float getHandStrength(Hand holeCards)
    {
        return getHandStrength(holeCards.getCard(0), holeCards.getCard(1));
    }

    public float getHandStrength (com.ddpoker.Card card1, com.ddpoker.Card card2)
    {
        int rank1 = card1.getRank();
        int rank2 = card2.getRank();

        boolean suited = card1.getSuit() == card2.getSuit();

        for (int i = handgroups_.size() - 1; i >= 0; --i)
        {
            HandGroup group = handgroups_.get(i);

            if (group.contains(rank1, rank2, suited))
            {
                return ((float)group.getStrength()) / 10.0f;
            }
        }

        return 0.0f;
    }

    public List<HandGroup> getHandGroups()
    {
        return handgroups_;
    }

    /**
     * Get description
     */
    public String getDescription() {
        return map_.getString("desc", "");
    }

    /**
     * Set description
     */
    public void setDescription(String s) {
        map_.setString("desc", s);
    }

    public String toHTML()
    {
        StringBuilder buf = new StringBuilder();

        String sDesc = getDescription();
        if (sDesc != null)
        {
            buf.append(Utils.encodeHTML(sDesc).replaceAll("\n", "<BR>\n"));
            buf.append("<BR>");
        }

        String sBreak = "<BR>";
        buf.append(sBreak);
        buf.append("<TABLE>");
        String yadj = "0";

        for (HandGroup group : handgroups_)
        {
            if (group.getClassCount() > 0)
            {
                buf.append("<TR VALIGN=\"TOP\"><TD WIDTH=\"90\">");

                int strength = group.getStrength();

                for (int j = 2; j <= 10; j += 2)
                {

                    buf.append("<DDIMG YADJ=\"").append(yadj).append("\" WIDTH=\"16\" SRC=\"rating16_");
                    if (strength >= j)
                    {
                        buf.append("full");
                    }
                    else if (strength + 1 == j)
                    {
                        buf.append("half");

                    }
                    else
                    {
                        buf.append("empty");
                    }
                    buf.append("\"> ");
                }
                buf.append("</TD><TD>");
                //buf.append("<FONT SIZE=\"-2\">");
                buf.append(group.getSummary());
                //buf.append("</FONT>");
                buf.append("</TD></TR>");
            }

        }

        buf.append("</TABLE>");

        return buf.toString();
    }

    public static HandSelectionScheme getByName(String sName) {

        if (sName == null) {
            return null;
        }

        List<BaseProfile> schemes = getProfileList();

        for (BaseProfile scheme1 : schemes)
        {
            HandSelectionScheme scheme = (HandSelectionScheme) scheme1;

            if (sName.equals(scheme.getName()))
            {
                return scheme;
            }
        }

        return null;
    }

    public Long getID()
    {
        return map_.getLong("id");
    }

    public static HandSelectionScheme getByID(Long id) {

        if (id == null)
        {
            return null;
        }

        List<BaseProfile> schemes = getProfileList();

        for (BaseProfile scheme1 : schemes)
        {
            HandSelectionScheme scheme = (HandSelectionScheme) scheme1;

            if (id.equals(scheme.getID()))
            {
                return scheme;
            }
        }

        return null;
    }
}
