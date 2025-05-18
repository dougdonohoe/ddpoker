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
package com.donohoedigital.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 3, 2005
 * Time: 4:04:34 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DDTabPanel extends DDPanel implements AncestorListener
{
    private DDTabbedPane tabPane_;
    private int nTabNum_;
    private Icon icon_;
    private Icon error_;
    private List<DDOption> options_ = new ArrayList<DDOption>();
    private String sHelp_;

    public DDTabPanel()
    {
        super();
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addAncestorListener(this);
    }

    void setTabPane(DDTabbedPane tab)
    {
        tabPane_ = tab;
    }

    void setHelpText(String sText)
    {
        sHelp_ = sText;
    }

    public String getHelpText()
    {
        return sHelp_;
    }

    public DDTabbedPane getTabPane()
    {
        return tabPane_;
    }

    public boolean isSelectedTab()
    {
        return tabPane_.getSelectedIndex() == nTabNum_;
    }

    void setTabNum(int n)
    {
        nTabNum_ = n;
    }

    public int getTabNum()
    {
        return nTabNum_;
    }

    public void setIcon(Icon icon)
    {
        icon_ = icon;
    }

    public Icon getIcon()
    {
        return icon_;
    }

    public void setErrorIcon(Icon icon)
    {
        error_ = icon;
    }

    public Icon getErrorIcon()
    {
        return error_;
    }

    private void setValid(boolean b)
    {
        tabPane_.setIconAt(nTabNum_, b ? icon_ : error_);
    }

    /**
     * Do valid check (which sets error icon appropriately),
     * and return whether tab contents is valid
     */
    protected boolean doValidCheck()
    {
        options_.clear();
        GuiUtils.getDDOptions(this, options_);
        DDOption dd;
        boolean bValid = true;
        for (int i = 0; bValid && i < options_.size(); i++)
        {
            dd = options_.get(i);
            if (!dd.isEnabled()) continue;
            bValid &= dd.isValidData();
        }

        // also do isValidCheck
        bValid &= isValidCheck();

        // set icon
        setValid(bValid);

        return bValid;
    }

    /**
     * subclass can chime in on validity
     */
    protected boolean isValidCheck()
    {
        return true;
    }

    /**
     * Create the UI components (called lazily from ancestorAdded())
     */
    protected abstract void createUI();

    /**
     * initialize the UI
     */
    public void initUI()
    {
        if (getComponentCount() > 0) return;

        createUI();
        repaint();
    }

    public void ancestorAdded(AncestorEvent event)
    {
        initUI();
    }

    public void ancestorMoved(AncestorEvent event)
    {
    }

    public void ancestorRemoved(AncestorEvent event)
    {
    }

}