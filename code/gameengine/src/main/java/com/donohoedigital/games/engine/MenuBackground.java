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
 * BasicBackground.java
 *
 * Created on November 15, 2002, 3:41 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author Doug Donohoe
 */
public class MenuBackground extends DDScrollPane
{
    //static Logger logger = LogManager.getLogger(MenuBackground.class);

    public static final String PARAM_MENUBOX_HELP_NAME = "menubox-help-name";

    // members
    private DDPanel menubox_;
    private String sHelpName_;

    /**
     * Creates a new instance of BasicBackground
     */
    public MenuBackground(GamePhase gamephase_)
    {
        super(null,
              gamephase_.getString("menubox-background-style", "default"),
              null,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setOpaque(false);
        setViewportBorder(BorderFactory.createEmptyBorder());

        // Create base panel which holds everything
        String sBackStyle = gamephase_.getString("menubox-background-style", "default");
        String sBoxStyle = gamephase_.getString("menubox-style", "default");
        String sBevelStyle = gamephase_.getString("menubox-bevel-style", "BrushedMetal");
        sHelpName_ = gamephase_.getString(PARAM_MENUBOX_HELP_NAME, "welcome");
        DDPanel base = new DDPanel(sHelpName_, sBackStyle);
        base.setLayout(new XYLayout());
        setViewportView(base);
        Dimension pref = new Dimension(800, 600);
        setPreferredSize(pref);
        base.setBorder(new DDBevelBorder(sBevelStyle, DDBevelBorder.RAISED));
        base.setPreferredSize(pref);
        int nBorderAdjust = 2;

        // version
        DDLabel version = new DDLabel("version", "version");
        GuiManager.setLabelAsMessage(version, GameEngine.getGameEngine().getVersion().toString());

        Dimension size = version.getPreferredSize();
        int nShift = Utils.ISMAC ? 15 : 3; // 15 to move over for mac grow box
        XYConstraints xy = new XYConstraints(pref.width - size.width - nShift - nBorderAdjust,
                                             pref.height - size.height - 3 - nBorderAdjust,
                                             size.width, size.height);
        base.add(version, xy);

        // first place image
        String sImageName = gamephase_.getString("menubox-background-image");
        if (sImageName != null && !"NONE".equalsIgnoreCase(sImageName))
        {
            if (GameEngine.getGameEngine().isDemo())
            {
                // clear cache of old image since we won't be using it
                // anymore
                ImageDef id = ImageConfig.getImageDef(sImageName);
                if (id != null)
                {
                    id.clearCache();
                }

                sImageName += "-demo";
            }
            ImageComponent ic = new ImageComponent(sImageName, 1.0);
            ic.setCentered(false);

            // background image
            xy = new XYConstraints(0 - nBorderAdjust, 0 - nBorderAdjust, ic.getWidth(), ic.getHeight());
            base.add(ic, xy);
        }

        // create menubox_, an overlay panel which holds the buttons and other stuff
        int x = gamephase_.getInteger("menubox-x", 30);
        int y = gamephase_.getInteger("menubox-y", 30);
        int width = gamephase_.getInteger("menubox-width", 400);
        int height = gamephase_.getInteger("menubox-height", 200);
        boolean bTitle = gamephase_.getBoolean("menubox-title", true);
        boolean bBorder = gamephase_.getBoolean("menubox-border", true);

        DDPanel menuboxbase = new DDPanel();

        menubox_ = new DDPanel(sHelpName_, sBoxStyle);
        menuboxbase.add(menubox_, BorderLayout.CENTER);
        menubox_.setOpaque(true);
        xy = new XYConstraints(x - nBorderAdjust, y - nBorderAdjust, width, height);

        if (bTitle)
        {
            DDPanel titlebase = new PillPanel(sBoxStyle);
            menuboxbase.setBorderLayoutGap(5, 0);
            JLabel titlelabel = new DDLabel();
            titlelabel.setBorder(BorderFactory.createEmptyBorder(-1, 20, 1, 20));
            titlelabel.setFont(StylesConfig.getFont(sBoxStyle + ".title", titlelabel.getFont()));
            titlelabel.setForeground(StylesConfig.getColor(sBoxStyle + ".title.fg", titlelabel.getForeground()));
            titlelabel.setText(PropertyConfig.getStringProperty(
                    gamephase_.getString("menubox-title-prop", "menubox-title-prop"),
                    "This Space For Rent")); // no title found so leave funny title)
            Dimension pref2 = titlelabel.getPreferredSize();
            pref2.width += 4; // on mac, doesn't layout right with some fonts
            titlelabel.setPreferredSize(pref2);
            titlebase.add(titlelabel, BorderLayout.CENTER);
            menuboxbase.add(GuiUtils.CENTER(titlebase), BorderLayout.NORTH);

            if (bBorder)
            {
                Border outside = new DDBevelBorder(sBevelStyle, BevelBorder.RAISED);
                Border inside = BorderFactory.createEmptyBorder(2, 2, 2, 2);

                menubox_.setBorder(BorderFactory.createCompoundBorder(outside, inside));
            }
        }
//        else
//        {
//            // testing
//            menubox_.setBorder(GuiUtils.BLACKBORDER);
//        }

        base.add(menuboxbase, xy, 0);
    }

    /**
     * Return base menubox panel
     */
    public DDPanel getMenuBox()
    {
        return menubox_;
    }

    /**
     * get name for DDPanels for help text
     */
    public String getHelpName()
    {
        return sHelpName_;
    }
}
