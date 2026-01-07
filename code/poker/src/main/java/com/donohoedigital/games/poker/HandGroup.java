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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;

import java.io.*;
import java.util.*;

public class HandGroup extends BaseProfile {

    public static final String PROFILE_BEGIN = "handgroup";
    public static final String HAND_GROUP_DIR = "handgroups";

    private boolean[] pairs_;
    private boolean[][] suited_;
    private boolean[][] offsuit_;

    private String summary_ = null;
    private HandList expanded_ = null;

    private int classCount_;
    private int handCount_;
    private int strength_;

    private int hashcode_ = 0;

    private static HandGroup ALL_HANDS = null;

    public synchronized static HandGroup getAllHands()
    {
        if (ALL_HANDS == null)
        {
            ALL_HANDS = new HandGroup().all();
        }
        return ALL_HANDS;
    }

    private HandGroup all()
    {
        for (int i = Card.TWO; i <= Card.ACE; ++i)
        {
            setContainsPair(i, true);

            for (int j = Card.THREE; j <= Card.ACE; ++j)
            {
                setContainsSuited(i, j, true);
                setContainsOffsuit(i, j, true);
            }
        }

        return this;
    }

    public HandList expand()
    {
        if (expanded_ == null)
        {
            expanded_ = new HandList(sName_);
            expanded_.setDescription(sDescription_);

            // pairs
            for (int rank = Card.ACE; rank >= Card.TWO; --rank)
            {
                if (pairs_[rank])
                {
                    expanded_.addAllPairs(rank);
                }
            }

            // suited
            for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
            {
                for (int rank2 = rank1 - 1; rank2 >= Card.TWO; --rank2)
                {
                    if (suited_[rank1][rank2])
                    {
                        expanded_.addAllSuited(rank1, rank2);
                    }
                }
            }

            // offsuit
            for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
            {
                for (int rank2 = rank1 - 1; rank2 >= Card.TWO; --rank2)
                {
                    if (offsuit_[rank1][rank2])
                    {
                        expanded_.addAllUnsuited(rank1, rank2);
                    }
                }
            }

        }

        return expanded_;
    }

    public String getSummary()
    {
        if (summary_ == null)
        {
            StringBuilder buf = new StringBuilder();

            int consecutive = 0;

            // pairs
            for (int rank = Card.ACE; rank >= Card.TWO; --rank)
            {
                if (pairs_[rank])
                {
                    consecutive++;
                    switch (consecutive)
                    {
                        case 1:
                        case 2:
                            break;
                        case 3:
                            buf.setLength(buf.length() - 6);
                            buf.append("-");
                            break;
                        default:
                            buf.setLength(buf.length() - 5);
                            buf.append("-");
                            break;
                    }

                    buf.append(Card.getRankSingle(rank));
                    buf.append(Card.getRankSingle(rank));

                    buf.append(", ");
                }
                else
                {
                    consecutive = 0;
                }
            }

            // suited
            for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
            {
                consecutive = 0;

                for (int rank2 = rank1 - 1; rank2 >= Card.TWO; --rank2)
                {
                    if (suited_[rank1][rank2])
                    {
                        consecutive++;
                        switch (consecutive)
                        {
                            case 1:
                            case 2:
                                break;
                            case 3:
                                buf.setLength(buf.length() - 7);
                                buf.append("-");
                                break;
                            default:
                                buf.setLength(buf.length() - 6);
                                buf.append("-");
                                break;
                        }

                        buf.append(Card.getRankSingle(rank1));
                        buf.append(Card.getRankSingle(rank2));
                        buf.append("s, ");
                    }
                    else
                    {
                        consecutive = 0;
                    }
                }
            }

            // offsuit
            for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
            {
                consecutive = 0;

                for (int rank2 = rank1 - 1; rank2 >= Card.TWO; --rank2)
                {
                    if (offsuit_[rank1][rank2])
                    {
                        consecutive++;
                        switch (consecutive)
                        {
                            case 1:
                            case 2:
                                break;
                            case 3:
                                buf.setLength(buf.length() - 6);
                                buf.append("-");
                                break;
                            default:
                                buf.setLength(buf.length() - 5);
                                buf.append("-");
                                break;
                        }

                        buf.append(Card.getRankSingle(rank1));
                        buf.append(Card.getRankSingle(rank2));
                        buf.append(", ");
                    }
                    else
                    {
                        consecutive = 0;
                    }
                }
            }

            if (buf.length() > 0)
            {
                buf.setLength(buf.length()-2);
            }

            summary_ = buf.toString();
        }

        return summary_;
    }

    public void setStrength(int strength)
    {
        strength_ = strength;
        hashcode_ = 0;
    }

    public int getStrength()
    {
        return strength_;
    }

    private String sDescription_ = null;

    // saved members
    private DMTypedHashMap map_;

    public HandGroup() {
        this(null, null);
    }

    public HandGroup(String name) {
        this(null, name);
    }

    public HandGroup(File file, boolean bFull)
    {
        super(file, bFull);
    }

    public HandGroup(HandGroup proto) {
        this(proto, null);
    }

    public HandGroup(HandGroup proto, String name) {
        clearContents();
        map_ = new DMTypedHashMap();
        if (proto == null) {
            sName_ = "New Group";
        } else {
            map_.putAll(proto.map_);
            sName_ = proto.getName();
            classCount_ = proto.classCount_;
            handCount_ = proto.handCount_;
            System.arraycopy(proto.pairs_,  0, pairs_, 0, Card.ACE+1);
            for (int rank = Card.TWO; rank <= Card.ACE; ++rank)
            {
                System.arraycopy(proto.suited_[rank], 0, suited_[rank], 0, Card.ACE + 1);
                System.arraycopy(proto.offsuit_[rank], 0, offsuit_[rank], 0, Card.ACE + 1);
            }

            // remove stuff we don't want to copy
            if ((sName_ != null) && !sName_.equals(name))
            {
                map_.removeLong("id");
            }
        }
        if (name != null)
        {
            sName_ = name;
        }
    }

    private void fireContentsChanged()
    {
        summary_ = null;
        expanded_ = null;
        hashcode_ = 0;
    }

    public void setContains(HandGroup group, boolean b)
    {
        boolean changed = false;

        for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
        {
            if (group.pairs_[rank1] && pairs_[rank1] != b)
            {
                pairs_[rank1] = b;
                classCount_ += (b ? 1 : -1);
                handCount_ += (b ? 6 : -6);
                changed = true;
            }

            for (int rank2 = rank1 - 1; rank2 >= Card.TWO; --rank2)
            {
                if (group.suited_[rank1][rank2] && suited_[rank1][rank2] != b)
                {
                    suited_[rank1][rank2] = b;
                    classCount_ += (b ? 1 : -1);
                    changed = true;
                }

                if (group.offsuit_[rank1][rank2] && offsuit_[rank1][rank2] != b)
                {
                    offsuit_[rank1][rank2] = b;
                    classCount_ += (b ? 1 : -1);
                    changed = true;
                }
            }
        }

        if (changed) fireContentsChanged();
    }

    public void setContains(int rank1, int rank2, boolean b, boolean bSuited)
    {
        if (bSuited)
        {
            setContainsSuited(rank1, rank2, b);
        }
        else
        {
            setContainsOffsuit(rank1, rank2, b);
        }
    }

    public void setContains(int rank1, int rank2, boolean b)
    {
        if (rank1 == rank2)
        {
            setContainsPair(rank1, b);
        }
        else if (rank1 < rank2)
        {
            setContains(rank2, rank1, b);
        }
        else
        {
            if (suited_[rank1][rank2] != b)
            {
                suited_[rank1][rank2] = b;
                classCount_ += (b ? 1 : -1);
                fireContentsChanged();
            }
            if (offsuit_[rank1][rank2] != b)
            {
                offsuit_[rank1][rank2] = b;
                classCount_ += (b ? 1 : -1);
                fireContentsChanged();
            }
        }
    }

    public void setContainsSuited(int rank1, int rank2, boolean b)
    {
        if (rank1 < rank2)
        {
            setContainsSuited(rank2, rank1, b);
        }
        else if (rank1 > rank2)
        {
            if (suited_[rank1][rank2] != b)
            {
                suited_[rank1][rank2] = b;
                classCount_ += (b ? 1 : -1);
                handCount_ += (b ? 4 : -4);
                fireContentsChanged();
            }
        }
    }

    public void setContainsOffsuit(int rank1, int rank2, boolean b)
    {

        if (rank1 == rank2)
        {
            setContainsPair(rank1, b);
        }
        else if (rank1 < rank2)
        {
            setContainsOffsuit(rank2, rank1, b);
        }
        else
        {
            if (offsuit_[rank1][rank2] != b)
            {
                offsuit_[rank1][rank2] = b;
                classCount_ += (b ? 1 : -1);
                handCount_ += (b ? 12 : -12);
                fireContentsChanged();
            }
        }
    }

    public void setContainsPair(int rank, boolean b)
    {
        if (pairs_[rank] != b)
        {
            pairs_[rank] = b;
            classCount_ += (b ? 1 : -1);
            handCount_ += (b ? 6 : -6);
            fireContentsChanged();
        }
    }

    public boolean contains(int rank1, int rank2, boolean suited)
    {
        if (suited)
        {
            return containsSuited(rank1, rank2);
        }
        else
        {
            if (rank1 == rank2)
            {
                return containsPair(rank1);
            }
            else
            {
                return containsOffsuit(rank1, rank2);
            }
        }
    }

    public boolean containsSuited(int rank1, int rank2)
    {
        if (rank1 == rank2)
        {
            return false;
        }
        else if (rank1 < rank2)
        {
            return suited_[rank2][rank1];
        }
        else
        {
            return suited_[rank1][rank2];
        }
    }

    public boolean containsOffsuit(int rank1, int rank2)
    {
        if (rank1 == rank2)
        {
            return pairs_[rank1];
        }
        else if (rank1 < rank2)
        {
            return offsuit_[rank2][rank1];
        }
        else
        {
            return offsuit_[rank1][rank2];
        }
    }

    public boolean containsPair(int rank)
    {
        return pairs_[rank];
    }

/*
public String getFileName() {
        return sFileName_ == null ? sName_ : sFileName_;
    }
*/
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
        return HAND_GROUP_DIR;
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
                (HAND_GROUP_DIR, Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), HandGroup.class, false);
    }

    public int getClassCount()
    {
        return classCount_;
    }

    public int getHandCount()
    {
        return handCount_;
    }

    public double getPercent()
    {
        return handCount_ / 13.26;
    }

    // TODO: changed from Object ... see if this still sorts properly
    public int compareTo(BaseProfile o) {
        return 0;
    }

    /**
     * Get description
     */
    public String getDescription()
    {
        return map_.getString("desc", "");
    }

    /**
     * Set description
     */
    public void setDescription(String s)
    {
        map_.setString("desc", s);
    }


    /**
     * Get map
     */
    public DMTypedHashMap getMap()
    {
        return map_;
    }

    public String toHTML()
    {
        String sBreak = "<BR><BR>";
        String sDesc = Utils.encodeHTML(getDescription());
        return (sDesc != null ? sDesc.replaceAll("\n", "<BR>\n") + sBreak : "") +
                "<DIV>" +
                "<DDHANDGROUP CARDS=\"" + getSummary().replaceAll(" ", "") + "\">" +
                "</DIV>";
    }

    public String toString()
    {
        return getName();
    }

    public void clearContents()
    {
        pairs_ = new boolean[Card.ACE + 1];
        suited_ = new boolean[Card.ACE + 1][Card.ACE + 1];
        offsuit_ = new boolean[Card.ACE + 1][Card.ACE + 1];

        classCount_ = 0;
        handCount_ = 0;

        fireContentsChanged();
    }

    public static HandGroup parse(String hands, int strength)
    {
        HandGroup group = new HandGroup();

        parse(group, hands, strength);

        return group;
    }

    private static void parse(HandGroup group, String hands, int strength)
    {
        group.clearContents();
        group.setStrength(strength);
        String items[] = hands.split(",");
        for (String item : items)
        {
            int rank1 = Card.getRank(item.charAt(0));
            int rank2 = Card.getRank(item.charAt(1));
            boolean isSuited = (item.length() > 2) && ("Ss".indexOf(item.charAt(2)) >= 0);
            boolean isRange = (item.length() > 3) && (item.charAt(isSuited ? 3 : 2) == '-');
            boolean isPair = (rank1 == rank2);
            if (isRange)
            {
                for (; rank2 >= Card.getRank(item.charAt(isSuited ? 5 : 4)); --rank2)
                {
                    if (isPair)
                    {
                        rank1 = rank2;
                    }
                    group.setContains(rank1, rank2, true, isSuited);
                }
            }
            else
            {
                group.setContains(rank1, rank2, true, isSuited);
            }
        }
    }

    /**
     * Equals based on hashcode
     */
    public boolean equals(Object o)
    {
        if (o instanceof HandGroup)
        {
            HandGroup other = (HandGroup) o;
            return hashCode() == other.hashCode();
        }
        return false;
    }

    /**
     * hashcode is unique and provides a repeatable ordinal number for sorting
     */
    public int hashCode()
    {
        if (hashcode_ > 0)
        {
            return hashcode_;
        }

        for (int rank = Card.ACE; rank >= Card.TWO; --rank)
        {
            if (pairs_[rank])
            {
                hashcode_ = (strength_ << 10) + ((rank + 32) << 5);
                return hashcode_;
            }
        }

        for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
        {
            for (int rank2 = Card.ACE; rank2 >= Card.TWO; --rank2)
            {
                if (suited_[rank1][rank2])
                {
                    hashcode_ = (strength_ << 10) + ((rank1 + 16) << 5) + rank2;
                    return hashcode_;
                }
            }
        }

        for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
        {
            for (int rank2 = Card.ACE; rank2 >= Card.TWO; --rank2)
            {
                if (offsuit_[rank1][rank2])
                {
                    hashcode_ = (strength_ << 10) + (rank1 << 5) + rank2;
                    return hashcode_;
                }
            }
        }

        return hashcode_;
    }

    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        map_ = new DMTypedHashMap();
        map_.demarshal(null, buf.readLine());

        String s = map_.getString("hands");

        if (s != null)
        {
            String v[] = s.split("\\|");
            parse(this, v[0], Integer.parseInt(v[1]));
        }
    }

    public void write(Writer writer) throws IOException
    {
        super.write(writer);

        if (!map_.containsKey("id"))
        {
            map_.setLong("id", System.currentTimeMillis());
        }

        if (getClassCount() > 0)
        {
            map_.setString("hands", getSummary().replaceAll(" ", "") + "|" + Integer.toString(getStrength()));
        }
        else
        {
            map_.removeString("hands");
        }

        writer.write(map_.marshal(null));
        writeEndEntry(writer);
    }
}
