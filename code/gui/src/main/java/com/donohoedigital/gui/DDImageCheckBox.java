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
package com.donohoedigital.gui;

import org.apache.log4j.*;
import com.donohoedigital.config.*;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 17, 2005
 * Time: 4:43:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDImageCheckBox extends DDCheckBox
{
   static Logger logger = Logger.getLogger(DDImageCheckBox.class);

    public DDImageCheckBox() {
        super();
    }

    /**
     * Creates a new instance of DDImageButton
     */
    public DDImageCheckBox(String sName, String sStyle) {
        super(sName, sStyle);
    }

    /**
     * init - get image icons and adjust other items
     */
    protected void init(String sName, String sStyleName)
    {
        super.init(sName, sStyleName);
        setIcon(ImageConfig.getImageIcon("checkbox." + sName + ".off"));
        setSelectedIcon(ImageConfig.getImageIcon("checkbox." + sName + ".on"));
        setSize(getIcon().getIconWidth(), getIcon().getIconHeight());
        setText(null);
        setOpaque(false);
        setBorderPainted(false);
        setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        setFocusable(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.CENTER);
    }
}
