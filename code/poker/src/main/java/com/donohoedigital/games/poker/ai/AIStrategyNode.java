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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.PropertyConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class AIStrategyNode
{
    protected static Logger logger = LogManager.getLogger(AIStrategyNode.class);

    private PlayerType playerType_;
    private String name_;
    private AIStrategyNode parent_ = null;
    private ArrayList children_ = new ArrayList();
    private boolean expand_ = false;
    private int indent_ = 0;
    private boolean bEnabled_;

    public AIStrategyNode(PlayerType playerType, String sName)
    {
        this(playerType, sName, false);    
    }

    public AIStrategyNode(PlayerType playerType, String sName, boolean bEnabled)
    {
        playerType_ = playerType;
        name_ = sName;
        bEnabled_ = bEnabled;
    }

    public void addChild(AIStrategyNode child)
    {
        child.parent_ = this;
        child.indent_ = indent_+1;
        children_.add(child);
    }

    public boolean isEnabled()
    {
        return bEnabled_;
    }

    public void setExpanded(boolean b)
    {
        expand_ = b;
    }

    public boolean isExpanded()
    {
        return expand_;
    }

    public int getIndent()
    {
        return indent_;
    }

    public String getLabel()
    {
        try
        {
            return PropertyConfig.getMessage("msg.strat." + name_);
        }
        catch (ApplicationError e)
        {
            return name_;
        }
    }

    public String getHelpText()
    {
        try
        {
            return PropertyConfig.getMessage("help.strat", getLabel(), PropertyConfig.getMessage("help.strat." + name_));
        }
        catch (ApplicationError e)
        {
            return "";
        }
    }

    public int getChildCount()
    {
        return children_.size();
    }

    public AIStrategyNode getChild(int i)
    {
        return (AIStrategyNode)children_.get(i);
    }

    public ArrayList getChildren()
    {
        return children_;
    }

    public AIStrategyNode getParent()
    {
        return parent_;
    }

    public int getValue()
    {
        return playerType_.getStratValue(name_);
    }

    public void setValue(int value)
    {
        //logger_.info("Setting value for " + name_ + " to " + value);
        playerType_.setStratValue(name_, value);
    }

    public void propagateValueChange()
    {
        propagateUpwards();
        propagateDownwards();
    }

    public void propagateUpwards()
    {
        if (parent_ != null)
        {
            int sum = 0;

            for (int i = parent_.getChildCount() - 1; i >= 0; --i)
            {
                sum += parent_.getChild(i).getValue();
            }

            int avg = sum / parent_.getChildCount();

            parent_.setValue(avg);

            parent_.propagateUpwards();
        }
    }

    public void setMissingValues(PlayerType profile, int defval)
    {
        int value = profile.getMap().getInteger("strat." + name_, -1, 0, 100);

        if (value < 0)
        {
            profile.getMap().setInteger("strat." + name_, defval);
            value = defval;
        }

        for (int i = getChildCount() - 1; i >= 0; --i)
        {
            AIStrategyNode child = getChild(i);

            child.setMissingValues(profile, value);
        }
    }

    public void propagateDownwards()
    {
        int value = getValue();

        for (int i = getChildCount() - 1; i >= 0; --i)
        {
            AIStrategyNode child = getChild(i);

            child.setValue(value);
            child.propagateDownwards();
        }
    }

    public boolean smartExpand()
    {
        int count = getChildCount();

        if (count > 0)
        {
            int value = getValue();

            AIStrategyNode child;

            for (int i = 0; i < count; ++i)
            {
                child = getChild(i);

                if (child.smartExpand() || (child.getValue() != value))
                {
                    setExpanded(true);
                    return true;
                }
            }
        }

        return false;
    }
}
