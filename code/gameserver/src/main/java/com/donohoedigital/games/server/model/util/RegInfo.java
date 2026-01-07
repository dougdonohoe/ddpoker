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
 * RegInfo.java
 *
 * Created on September 28, 2004, 9:21 AM
 */

package com.donohoedigital.games.server.model.util;

import com.donohoedigital.games.server.model.*;
import org.apache.logging.log4j.*;

import java.text.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
public class RegInfo implements Comparable<RegInfo>
{
    static Logger logger = LogManager.getLogger(RegInfo.class);
    
    // info
    private String sKey_;
    private List<Registration> msgs_ = new ArrayList<Registration>();
    private List<Registration> reg_msgs_ = new ArrayList<Registration>();
    private List<Registration> act_msgs_ = new ArrayList<Registration>();
    private List<Registration> patch_msgs_ = new ArrayList<Registration>();
    private List<Registration> dup_msgs_ = new ArrayList<Registration>();
    private long mostRecent_;
    private static SimpleDateFormat date_ = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss", Locale.US);

    private boolean banned = false;
    private String bannedComment = null;

    /**
     * Constructor
     */
    public RegInfo(String sKey, boolean banned, String bannedComment)
    {
        sKey_ = sKey;
        this.banned = banned;
        this.bannedComment = bannedComment;
    }

    /**
     * Is this a download sale?
     */
    public boolean isDownload()
    {
        return (sKey_.compareTo("2100-4299-7560-5704") < 0 ||  
                sKey_.compareTo("2200-8599-8704-9538") < 0);
    }
    
    /**
     * Does this key have a linux registration
     */
    public boolean isLinux()
    {
        for (Registration msg : msgs_)
        {
            if (msg.isLinux()) return true;
        }
        for (Registration msg : dup_msgs_)
        {
            if (msg.isLinux()) return true;
        }
        
        return false;
    }
    
    /**
     * Does this key have a mac registration
     */
    public boolean isMac()
    {
        for (Registration msg : msgs_)
        {
            if (msg.isMac()) return true;
        }
        for (Registration msg : dup_msgs_)
        {
            if (msg.isMac()) return true;
        }
        
        return false;
    }
    
    /**
     * Does this key have a win registration
     */
    public boolean isWin()
    {
        for (Registration msg : msgs_)
        {
            if (msg.isWin()) return true;
        }
        for (Registration msg : dup_msgs_)
        {
            if (msg.isWin()) return true;
        }
        
        return false;
    }
    
    /**
     * Get total messages
     */
    public int getNumTotal()
    {
        return msgs_.size();
    }
    
    /**
     * Get total registrations
     */
    public int getNumRegistrations()
    {
        return reg_msgs_.size();
    }
    
    /**
     * Get total patches
     */
    public int getNumPatches()
    {
        return patch_msgs_.size();
    }
    
    /**
     * Get total activations
     */
    public int getNumActivations()
    {
        return act_msgs_.size();
    }
    
    /**
     * Get total dups
     */
    public int getNumDups()
    {
        return dup_msgs_.size();
    }
    
    /**
     * Get messages
     */
    public List<Registration> getAll()
    {
        return msgs_;
    }
    
    /**
     * Get registration messages
     */
    public List<Registration> getRegistrations()
    {
        return reg_msgs_;
    }
    
    /**
     * Get patch messages
     */
    public List<Registration> getPatches()
    {
        return patch_msgs_;
    }
    
    /**
     * Get activation messages
     */
    public List<Registration> getActivations()
    {
        return act_msgs_;
    }
    
    /**
     * Get dups
     */
    public List<Registration> getDups()
    {
        return dup_msgs_;
    }
    
    /**
     * get key
     */
    public String getKey()
    {
        return sKey_;
    }
    
    /**
     * is banned?
     */
    public boolean isBanned()
    {
        return banned;
    }

    /**
     * get banned comment
     */
    public String getBannedComment()
    {
        return bannedComment;
    }

    /**
     * most recent time stamp
     */
    public long getMostRecentTimeStamp()
    {
        return mostRecent_;
    }

    /**
     * Add message to list - return if added
     */
    public void addRegistration(Registration reg, boolean bFilterDD)
    {
        // get stuff
        String ip = reg.getIp();
        String email = reg.getEmail();

        // skip testing messages
        if (bFilterDD)
        {
            if (contains(email, "donohoe.info")) return;
            if (contains(email, "gregking")) return;
            if (contains(sKey_, "2100-0000-8239-2505")) return; // greg
        }

        if (reg.getServerTimeMillis() > mostRecent_)
        {
            mostRecent_ = reg.getServerTimeMillis();
        }

        // if this is a duplicate, handle now
        if (reg.isDuplicate())
        {
            dup_msgs_.add(0, reg);
            return;
        }

        // tally normal
        switch (reg.getType())
        {
            case PATCH:
            {
                patch_msgs_.add(0, reg);
                break;
            }
            case ACTIVATION:
            {
                act_msgs_.add(0, reg);
                break;
            }
            case REGISTRATION:
            {
                reg_msgs_.add(0, reg); // add at beginning
                break;
            }
        }

        // store message in all messages
        msgs_.add(reg);
    }

    /**
     * to string
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(sKey_);
        sb.append(":");
        
        if (isBanned())
        {
            sb.append(" banned");
            String sComment = getBannedComment();
            if (sComment.length() > 0) sComment = " - "  + sComment;
            sb.append(sComment);
        }
        else
        {
            sb.append(" total=").append(getNumTotal());
            sb.append(" act=").append(getNumActivations());
            sb.append(" reg=").append(getNumRegistrations());
            sb.append(" patch=").append(getNumPatches());
        }
        
        return sb.toString();
    }
    
    /**
     * HTML output of list
     */
    public String toHTML(List<Registration> msgs, int nMaxLines)
    {
        long nowtime = System.currentTimeMillis();
        long regtime;
        StringBuilder sb = new StringBuilder();
        Registration msg;
        if (nMaxLines <= 0 || nMaxLines > msgs.size()) nMaxLines = msgs.size();
        int j = 0;
        boolean bRecent;
        String TD = "<TD valign=top>";
        String TDFN = "<TD valign=top><font size=-1>";
        String FNTD = "&nbsp;&nbsp;</font></TD>";
        sb.append("<TABLE>");
        for (; j < nMaxLines; j++)
        {
            msg = msgs.get(j);
            regtime = msg.getServerTimeMillis();
            bRecent = (nowtime - regtime) < (2l * 24l * 60l * 60l * 1000l);
            sb.append("<TR>");
                
                String color = "#990033";
                sb.append(TD);   
                    if (msg.isBanAttempt()) {
                        sb.append("<B><font color=\"red\">***&nbsp;</font></B>");
                    }
                    if (msg.isActivation() || msg.isPatch()) {
                        sb.append("<font color=\"").append(color).append("\">");
                        sb.append(msg.getIp());
                        sb.append("</font>");
                    } else {
                        sb.append("<a href=\"mailto:").append(msg.getEmail()).append("\">");
                        sb.append("<font color=\"").append(color).append("\">");
                        sb.append(msg.getEmail());
                        sb.append("</font>");
                        sb.append("</a>");
                    }
                sb.append("&nbsp;&nbsp;</TD>");

                sb.append(TDFN);
                    if (msg.isActivation() || msg.isPatch()) {
                        sb.append(getHostDisplay(msg));
                    } else {
                        sb.append(msg.getIp()).append(" - ").append(getHostDisplay(msg));
                        sb.append("<BR>");
                        sb.append(msg.getName()).append(" at ").append(msg.getAddress()).append("<BR>").append(msg.getCity()).append(", ").append(msg.getState()).append("  ").append(msg.getPostal()).append(" ").append(msg.getCountry());
                    }
                sb.append(FNTD);
            sb.append(TDFN).append(msg.getOperatingSystem()).append(FNTD);
            sb.append(TDFN).append("v").append(msg.getVersion()).append(FNTD);
                
                sb.append(TDFN);
                if (bRecent) sb.append("<B><font color=\"red\">");
                sb.append(date_.format(msg.getServerTime()));
                if (bRecent) sb.append("</font></B>");
                sb.append(FNTD);
                
            sb.append("</TR>");
        }
        if (msgs.size() > j)
        {
            sb.append("<TR>");
            sb.append(TDFN);
            int notdisplayed = msgs.size() - j;
            String x = notdisplayed == 1 ? " record " : " records ";
            sb.append("<I>(").append(notdisplayed).append(x).append("not displayed)").append("</I>");
            sb.append(FNTD);
            sb.append("</TR>");
        }
        sb.append("</TABLE>");
        return sb.toString();
    }

    public String getHostDisplay(Registration reg)
    {
        String host = reg.getHostNameModified();
        if (host == null) host = "[unknown]";
        return host;
    }
    
    /**
     * Compare (sort so biggest is first)
     */
    public int compareTo(RegInfo r)
    {
        long c1 = 0;
        long c2 = 0;

        if (isBanned() && !r.isBanned())
        {
            return 1;
        }
        if (!isBanned() && r.isBanned())
        {
            return -1;
        }
        if (r.getNumTotal() == getNumTotal())
        {
            c1 = r.getMostRecentTimeStamp();
            c2 = getMostRecentTimeStamp();
        }
        else
        {
            c1 = r.getNumTotal();
            c2 = getNumTotal();
        }

        if (c1 == c2)
        {
            return 0;
        }
        else if (c1 > c2)
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Simple compare
     */
    private boolean contains(String str, String contains)
    {
        if (str == null) return false;
        return str.contains(contains);
    }
}
