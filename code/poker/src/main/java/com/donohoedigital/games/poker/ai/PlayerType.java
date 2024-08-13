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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.config.*;

import java.io.*;
import java.util.*;

public class PlayerType extends BaseProfile
{
    public static final String PROFILE_BEGIN = "playertype";
    public static final String PROFILE_DIR = "playertypes";

    public static final String ADVISOR_BEGIN = "advisor";
    public static final String ADVISOR_DIR = "advisors";

    private DMTypedHashMap map_;

    private HandSelectionScheme handSelectionFull_;
    private HandSelectionScheme handSelectionShort_;
    private HandSelectionScheme handSelectionVeryShort_;
    private HandSelectionScheme handSelectionHup_;

    private static PlayerType defaultProfile_ = null;
    private static List<BaseProfile> cached_ = null;

    public PlayerType()
    {
        super("");
    }

    public PlayerType(File file, boolean bFull)
    {
        super(file, bFull);
    }

    public PlayerType(String sName)
    {
        super(sName);
        map_ = new DMTypedHashMap();
        setStratValue("discipline", 0);
        setAIClassName("com.donohoedigital.games.poker.ai.V2Player");
    }

    /**
     * New profile copied from given profile, using new name
     */
    public PlayerType(PlayerType tp, String sName)
    {
        super(sName);
        map_ = new DMTypedHashMap();
        map_.putAll(tp.map_);

        // remove stuff we don't want to copy
        if (!sName.equals(tp.getName()))
        {
            map_.removeBoolean("default");
            map_.removeInteger("order");
            map_.removeLong("id");
        }

        handSelectionFull_ = tp.handSelectionFull_;
        handSelectionShort_ = tp.handSelectionShort_;
        handSelectionVeryShort_ = tp.handSelectionVeryShort_;
        handSelectionHup_ = tp.handSelectionHup_;
    }

    public static PlayerType getByUniqueKey(String key)
    {
        return getByUniqueKey(key, null);
    }

    public static PlayerType getByUniqueKey(String key, PokerSaveDetails details)
    {
        PlayerType pt = null;
        if (key != null)
        {
            List<BaseProfile> types = getProfileListCached(details);

            for (BaseProfile type : types)
            {
                PlayerType playerType = (PlayerType) type;

                if (key.equals(playerType.getUniqueKey()))
                {
                    pt = playerType;
                    break;
                }
            }
        }

        if (pt == null)
        {
            pt = getDefaultProfile(details);
        }
        return pt;
    }

    /**
     * Get cached list of profiles from details (used when loading)
     */
    private static List<BaseProfile> getProfileListCached(PokerSaveDetails details)
    {
        List<BaseProfile> types = null;
        if (details != null) types = details.getPlayerTypeProfiles();
        if (types == null)
        {
            types = getProfileList();
            if (details != null) details.setPlayerTypeProfiles(types);
        }
        return types;
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
        return PROFILE_DIR;
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
                (PROFILE_DIR, Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), PlayerType.class, false);
    }

    /**
     * Get cached list of save files for tournament display
     */
    public static List<BaseProfile> getProfileListCached()
    {
        if (cached_ == null)
        {
            cached_ = getProfileList();
            Collections.sort(cached_);
        }
        return cached_;
    }

    // because using instance as hash key doesn't work
    // TODO: change value returned, for durable cross-references
    public String getUniqueKey()
    {
        return getFileName();
    }

    public void setAIClassName(String aiclass)
    {
        map_.setString("aiclass", aiclass);
    }

    public String getAIClassName()
    {
        return map_.getString("aiclass");
    }

    public boolean canCopy()
    {
        String aiClass = getAIClassName();
        return (aiClass == null) || !aiClass.endsWith("V1Player");
    }

    public void read(Reader reader, boolean bFull) throws IOException
    {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        map_ = new DMTypedHashMap();
        map_.demarshal(null, buf.readLine());

        setHandSelectionFull(HandSelectionScheme.getByID(map_.getLong("hsfull")));
        setHandSelectionShort(HandSelectionScheme.getByID(map_.getLong("hsshort")));
        setHandSelectionVeryShort(HandSelectionScheme.getByID(map_.getLong("hsvshort")));
        setHandSelectionHup(HandSelectionScheme.getByID(map_.getLong("hshup")));

        if (getHandSelectionFull() == null)
        {
            setHandSelectionFull((HandSelectionScheme)HandSelectionScheme.getProfileList().get(0));
        }
        if (getHandSelectionShort() == null)
        {
            setHandSelectionShort(getHandSelectionFull());
        }
        if (getHandSelectionVeryShort() == null)
        {
            setHandSelectionVeryShort(getHandSelectionShort());
        }
        if (getHandSelectionHup() == null)
        {
            setHandSelectionHup(getHandSelectionVeryShort());
        }
    }

    public void write(Writer writer) throws IOException
    {
        super.write(writer);

        map_.remove("hsfull");
        map_.remove("hsshort");
        map_.remove("hsvshort");
        map_.remove("hshup");
        map_.remove("pfloose");
        map_.remove("pfavg");
        map_.remove("pftight");

        if (handSelectionFull_ != null)
        {
            map_.setLong("hsfull", handSelectionFull_.getID());
        }
        if (handSelectionShort_ != null)
        {
            map_.setLong("hsshort", handSelectionShort_.getID());
        }
        if (handSelectionVeryShort_ != null)
        {
            map_.setLong("hsvshort", handSelectionVeryShort_.getID());
        }
        if (handSelectionHup_ != null)
        {
            map_.setLong("hshup", handSelectionHup_.getID());
        }

        writer.write(map_.marshal(null));
        writeEndEntry(writer);

        defaultProfile_ = null;
        cached_ = null;
    }

    public String toHTML()
    {
        StringBuilder buf = new StringBuilder();
        String sDesc = getDescription();

        if (sDesc != null && sDesc.length() > 0)
        {
            buf.append(Utils.encodeHTML(sDesc).replaceAll("\n", "<BR>\n"));
            buf.append("<BR><BR>");
        }

        String aiClass = getAIClassName();

        if ((aiClass == null) || !aiClass.endsWith("V1Player"))
        {
            buf.append("<TABLE CELLPADDING=0 CELLSPACING=0>");

            buf.append(PropertyConfig.getMessage("msg.playertype.handselection",
                    getHandSelectionFull().getName(),
                    getHandSelectionShort().getName(),
                    getHandSelectionVeryShort().getName(),
                    getHandSelectionHup().getName()
                    ));

            ArrayList nodes = getSummaryNodes(false);
            int count = nodes.size();
            AIStrategyNode node;
            boolean bBold;
            for (int i = 0; i < count; ++i)
            {
                node = (AIStrategyNode)nodes.get(i);

                buf.append("<TR><TD>");

                for (int j = node.getIndent(); j >= 0; --j)
                {
                    buf.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                }


                bBold = (node.isExpanded() || (node.getParent() == null));

                if (bBold)
                {
                    buf.append("<B>");
                }

                buf.append(node.getLabel());
                buf.append("&nbsp;&nbsp;");
                if (bBold)
                {
                    buf.append("</B>");
                }

                buf.append("</TD><TD>");

                if (node.isExpanded())
                {
                    buf.append("&nbsp;");
                }
                else
                {
                    buf.append(node.getValue());
                }

                buf.append("</TD></TR>");
            }
            buf.append("</TABLE>");
        }
        return buf.toString();
    }

    public void setHandSelectionFull(HandSelectionScheme scheme) {
        handSelectionFull_ = scheme;
    }

    public void setHandSelectionShort(HandSelectionScheme scheme) {
        handSelectionShort_ = scheme;
    }

    public void setHandSelectionVeryShort(HandSelectionScheme scheme) {
        handSelectionVeryShort_ = scheme;
    }

    public void setHandSelectionHup(HandSelectionScheme scheme) {
        handSelectionHup_ = scheme;
    }

    public HandSelectionScheme getHandSelectionFull() {
        return handSelectionFull_;
    }

    public HandSelectionScheme getHandSelectionShort() {
        return handSelectionShort_;
    }

    public HandSelectionScheme getHandSelectionVeryShort() {
        return handSelectionVeryShort_;
    }

    public HandSelectionScheme getHandSelectionHup() {
        return handSelectionHup_;
    }

    public static PlayerType setAdvisor(PlayerType profile)
    {
        GameEngine.getGameEngine().getPrefsNode().put
                (PokerConstants.OPTION_DEFAULT_ADVISOR, profile.getUniqueKey());

        File advisorFile = getAdvisorFile();

        if (advisorFile.exists())
        {
            advisorFile.delete();
        }

        return getAdvisor();
    }

    public static File getAdvisorFile()
    {
        return getAdvisorFile(PlayerProfileOptions.getDefaultProfile());
    }

    public static File getAdvisorFile(PlayerProfile playerProfile)
    {
        return createFile(getProfileDir(ADVISOR_DIR), ADVISOR_BEGIN, playerProfile.getFileNum(), PROFILE_EXT);
    }

    public static String getAdvisorKey()
    {
        return GameEngine.getGameEngine().getPrefsNode().get
                (PokerConstants.OPTION_DEFAULT_ADVISOR, null);
    }

    public static PlayerType getAdvisor()
    {
        File advisorFile = getAdvisorFile();

        if (advisorFile.exists())
        {
            return new PlayerType(advisorFile, true);
        }

        String prefAdvisor = getAdvisorKey();

        PlayerType profile = getByUniqueKey(prefAdvisor);

        PlayerType advisor = new PlayerType(profile, profile.getName());

        advisor.file_ = advisorFile;

        return advisor;
    }

    public static PlayerType getDefaultProfile()
    {
        return getDefaultProfile(null);
    }

    public static PlayerType getDefaultProfile(PokerSaveDetails details)
    {
        if (defaultProfile_ == null) synchronized (PlayerType.class)
        {
            List<BaseProfile> profiles = getProfileListCached(details);

            if (profiles.size() == 0)
            {
                throw new ApplicationError("No computer player types defined!");
            }

            PlayerType profile;

            PlayerType defaultProfile = (PlayerType)profiles.get(0);

            for (int i = 0; i < profiles.size(); ++i)
            {
                profile = (PlayerType)profiles.get(i);

                if (profile.isDefault())
                {
                    defaultProfile = profile;
                    break;
                }
            }

            defaultProfile_ = defaultProfile;
        }

        return defaultProfile_;
    }

    public boolean isDefault()
    {
        return map_.getBoolean("default", false);
    }

    public float getStratFactor(String name, Hand hand, float min, float max, int mod)
    {
        return getStratFactor(name, hand, min, max, 50, mod);
    }

    public float getStratFactor(String name, Hand hand, float min, float max, int defval, int mod)
    {
        float value = min + (max - min)  / 100.0f * Math.min(Math.max((float)getStratValue(name, hand, defval) + mod, 0f), 100f);

        //System.out.println("getStratFactor(" + name + "," + hand + "," + min + "," + max + "," + defval + "," + mod + " = " + value);

        return value;
    }

    public float getStratFactor(String name, float min, float max)
    {
        return getStratFactor(name, min, max, 50);
    }

    public float getStratFactor(String name, float min, float max, int defval)
    {
        return min + (max - min)  / 100.0f * (float)getStratValue(name, defval);
    }

    public int getStratValue(String name, Hand hand)
    {
        return getStratValue(name, hand, 50);
    }

    public int getStratValue(String name, Hand hand, int defval)
    {
        if (hand == null) return getStratValue(name, defval);

        String key;

        if (hand.isPair())
        {
            int rank = hand.getHighestRank();

            if (rank < 7) key = "small_pair";
            else if (rank > 10) key = "big_pair";
            else key = "medium_pair";
        }
        else
        {
            if (hand.getLowestRank() > 9)
            {
                if (hand.isSuited())
                {
                    key = "suited_high_cards";
                }
                else
                {
                    key = "unsuited_high_cards";
                }
            }
            else
            {
                if (hand.getHighestRank() == Card.ACE)
                {
                    if (hand.isSuited())
                    {
                        key = "suited_ace";
                    }
                    else
                    {
                        key = "unsuited_ace";
                    }
                }
                else
                {
                    if (hand.isConnectors(Card.TWO, Card.TEN))
                    {
                        if (hand.isSuited())
                        {
                            key = "suited_connectors";
                        }
                        else
                        {
                            key = "unsuited_connectors";
                        }
                    }
                    else
                    {
                        key = "other";
                    }
                }
            }
        }

        //System.out.println("getStratValue(" + name + "." + key + ", " + defval + ") returns " +
        //        getStratValue(name + "." + key, defval));

        return getStratValue(name + "." + key, defval);
    }

    public int getStratValue(String name)
    {
        return getStratValue(name, 50);
    }

    public int getStratValue(String name, int defval)
    {
        int value = -1;

        value = getMap().getInteger("strat." + name, -1, 0, 100);

        if (value < 0)
        {
            int chop = name.lastIndexOf(".");

            if (chop > 0)
            {
                name = name.substring(0, chop);

                return getStratValue(name);
            }
            else
            {
                return defval;
            }
        }
        else
        {
            return value;
        }
    }

    public void setStratValue(String name, int value)
    {
        getMap().setInteger("strat." + name, new Integer(value));
    }

    public ArrayList getSummaryNodes(boolean bIncludeDisabled)
    {
        ArrayList rootNodes = getRootNodes();

        int count = rootNodes.size();

        for (int i = 0; i < count; ++i)
        {
            ((AIStrategyNode)rootNodes.get(i)).smartExpand();
        }

        ArrayList nodes = new ArrayList();

        addExpanded(nodes, rootNodes, bIncludeDisabled);

        return nodes;
    }

    private void addExpanded(ArrayList addTo, ArrayList nodes, boolean bIncludeDisabled)
    {
        AIStrategyNode parent;

        AIStrategyNode node;

        int count = nodes.size();

        for (int i = 0; i < count; ++i)
        {
            node = (AIStrategyNode)nodes.get(i);

            parent = node.getParent();

            if ((parent == null) || parent.isExpanded())
            {
                if (bIncludeDisabled || node.isEnabled())
                {
                    addTo.add(node);
                    addExpanded(addTo, node.getChildren(), bIncludeDisabled);
                }
            }
        }
    }

    public ArrayList getRootNodes()
    {
        ArrayList rootNodes = new ArrayList();

        AIStrategyNode level_1;
        AIStrategyNode level_2;
        AIStrategyNode level_3;
        AIStrategyNode level_4;

        rootNodes.add(level_1 = new AIStrategyNode(this, "handselection", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.big_pair", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.medium_pair", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.small_pair", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.suited_high_cards", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.unsuited_high_cards", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.suited_ace", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.unsuited_ace", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.suited_connectors", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.unsuited_connectors", true));
        level_1.addChild(new AIStrategyNode(this, "handselection.other", true));

        rootNodes.add(level_1 = new AIStrategyNode(this, "basics", true));
        level_1.addChild(new AIStrategyNode(this, "basics.aggression", true));
        level_1.addChild(new AIStrategyNode(this, "basics.position", true));
        level_1.addChild(new AIStrategyNode(this, "basics.pot_odds_call", true));
        level_1.addChild(new AIStrategyNode(this, "basics.observation", true));
        //level_1.addChild(new AIStrategyNode(this, "basics.number_of_opponents", true));

        //level_1.addChild(new SliderListItem(this, "basics.pot_odds_raise"));
        //level_1.addChild(new SliderListItem(this, "basics.relative_stack_size"));
        //level_1.addChild(new SliderListItem(this, "basics.relative_blind_size"));

        rootNodes.add(level_1 = new AIStrategyNode(this, "tournament", true));
        level_1.addChild(new AIStrategyNode(this, "tournament.stack_size", true));
        level_1.addChild(new AIStrategyNode(this, "tournament.opponent_stack_size", true));

        /*
        rootNodes.add(level_1 = new AIStrategyNode(this, "perception"));
        level_1.addChild(level_2 = new AIStrategyNode(this, "perception.opponent"));
        level_2.addChild(new AIStrategyNode(this, "perception.opponent.table_tight"));
        level_2.addChild(new AIStrategyNode(this, "perception.opponent.table_aggressive"));
        level_2.addChild(new AIStrategyNode(this, "perception.opponent.defend_blinds"));
        level_2.addChild(new AIStrategyNode(this, "perception.opponent.individual_style"));
        level_1.addChild(level_2 = new AIStrategyNode(this, "perception.blindspot"));
        //level_2.addChild(new SliderListItem(this, "perception.blindspot.low_end_straight"));
        //level_2.addChild(new SliderListItem(this, "perception.blindspot.non_nut_flush"));
        level_2.addChild(new AIStrategyNode(this, "perception.blindspot.flush_possible"));
        level_2.addChild(new AIStrategyNode(this, "perception.blindspot.straight_possible"));
        level_2.addChild(new AIStrategyNode(this, "perception.blindspot.pair_on_board"));
        */

        /*
        rootNodes.add(level_1 = new AIStrategyNode(this, "deception", true));
        level_1.addChild(level_2 = new AIStrategyNode(this, "deception.strong_hands"));
        level_2.addChild(new AIStrategyNode(this, "deception.strong_hands.check_raise"));
        level_2.addChild(new AIStrategyNode(this, "deception.strong_hands.slow_play"));
        level_2.addChild(new AIStrategyNode(this, "deception.strong_hands.trap"));

        level_1.addChild(level_2 = new AIStrategyNode(this, "deception.bluff", true));
        level_2.addChild(new AIStrategyNode(this, "deception.bluff.semi_bluff"));
        level_2.addChild(new AIStrategyNode(this, "deception.bluff.steal_blinds", true));
        level_2.addChild(new AIStrategyNode(this, "deception.bluff.buy_pot"));
        level_2.addChild(new AIStrategyNode(this, "deception.bluff.bully"));
        */

        rootNodes.add(level_1 = new AIStrategyNode(this, "draws", true));

        level_1.addChild(level_2 = new AIStrategyNode(this, "draws.straight", true));
        level_2.addChild(new AIStrategyNode(this, "draws.straight.nut", true));
        level_2.addChild(new AIStrategyNode(this, "draws.straight.non_nut", true));
        level_1.addChild(level_2 = new AIStrategyNode(this, "draws.flush", true));
        level_2.addChild(new AIStrategyNode(this, "draws.flush.nut", true));
        level_2.addChild(new AIStrategyNode(this, "draws.flush.non_nut", true));

        rootNodes.add(level_1 = new AIStrategyNode(this, "discipline", true));

        level_1.addChild(new AIStrategyNode(this, "discipline.tilt", true));
        level_1.addChild(new AIStrategyNode(this, "discipline.boredom", true));

        for (int i = 0; i < rootNodes.size(); ++i)
        {
            ((AIStrategyNode) rootNodes.get(i)).setMissingValues(this, 50);
        }

        return rootNodes;
    }

    /**
     * Override to allow forced positions and grouping.
     */
    public int compareTo(BaseProfile o)
    {
        PlayerType p = (PlayerType) o;

        int pPos = p.getMap().getInteger("order", 0);
        int myPos = getMap().getInteger("order", 0);

        if (pPos == myPos)
        {
            return super.compareTo(o);
        }
        else
        {
            return (myPos - pPos);
        }
    }
}
