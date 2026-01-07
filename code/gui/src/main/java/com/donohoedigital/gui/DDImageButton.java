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
 * DDImageButton.java
 *
 * Created on January 4, 2003, 8:06 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;

/**
 * @author Doug Donohoe
 */
public class DDImageButton extends DDButton
{
    static Logger logger = LogManager.getLogger(DDImageButton.class);

    private boolean bTransparencyIgnored_ = true;

    public DDImageButton()
    {
        super();
    }

    /**
     * Creates a new instance of DDImageButton
     */
    public DDImageButton(String sName)
    {
        super(sName);
    }

    /**
     * init - get image icons and adjust other items
     */
    @Override
    protected void init(String sName, String sStyleName)
    {
        super.init(sName, GuiManager.DEFAULT);
        ImageIcon icon = ImageConfig.getImageIcon("button." + sName + ".up", null);
        ImageIcon roll = ImageConfig.getImageIcon("button." + sName + ".over", icon);
        ImageIcon press = ImageConfig.getImageIcon("button." + sName + ".down", icon);
        ImageIcon disable = ImageConfig.getImageIcon("button." + sName + ".disable", icon);
        setIcon(icon);
        setRolloverIcon(roll);
        setPressedIcon(press);
        setDisabledIcon(disable);
        setSize(getIcon().getIconWidth(), getIcon().getIconHeight());
        setText(null);
        setOpaque(false);
        setBorderPainted(false);
        setFocusable(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setBorderGap(0, 0, 0, 0);
        setDisableMode(DDButton.DISABLED_NONE); // we use disabled image
    }

    /**
     * Need to repaint after rename, due to changed images.
     */
    @Override
    public void rename(String sName)
    {
        super.rename(sName);
        init(sName, (String) null);
    }


    /**
     * Is transparent portion of image ignored?
     */
    public boolean isTransparentIgnored()
    {
        return bTransparencyIgnored_;
    }

    /**
     * Set transparent ignored (default is true)
     */
    public void setTransparentIgnored(boolean b)
    {
        bTransparencyIgnored_ = b;
    }

    /**
     * Override to make public
     */
    @Override
    public void paintComponent(Graphics g1)
    {
        super.paintComponent(g1);
    }
}
