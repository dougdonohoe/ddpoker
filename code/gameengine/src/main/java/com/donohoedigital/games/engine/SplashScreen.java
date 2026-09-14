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
 * SplashScreen.java
 *
 * Created on October 8, 2002, 1:58 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.AudioConfig;
import com.donohoedigital.config.ImageConfig;
import com.donohoedigital.config.ImageDef;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Splash screen.  Displayed from Game Engine as soon as possible, then later updated
 * when config files have been loaded.  Best not to use logger / PropertyConfig or other
 * config related APIs in constructor.
 *
 * @author Doug Donohoe
 */
public class SplashScreen extends JFrame
{
    private final ImageComponent ic_;
    private final URL bgFile_;

    /**
     * initial splash - shown as soon as possible
     */
    public SplashScreen(URL bg, URL icon, String sTitle)
    {
        super();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);
        setTitle(sTitle);

        // load file directly (since config files not loaded)
        bgFile_ = bg;
        BufferedImage img = ImageDef.getBufferedImage(bg);
        ic_ = new ImageComponent(img, 1.0d);
        // Splash is a fixed-size 1x bitmap of hard-edged artwork, painted essentially once.
        // On a scaled display (Windows at 125%/150%, HiDPI) the default nearest-neighbor
        // interpolation stair-steps the logo badly, so pay for bicubic here.
        ic_.setInterpolation(RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        ic_.setLayout(new XYLayout());
        setContentPane(ic_);

        // icon
        setIconImage(ImageDef.getBufferedImage(icon));

        // frame final setup
        validate();
        pack();
        center();
    }

    /**
     * Called by GameEngine after config files loaded
     */
    public void changeUI(GameEngine engine, String sErrorMessage)
    {
        int BUTTONSIZE = 15;
        XYConstraints xy;
        setTitle(PropertyConfig.getMessage("msg.title.splash"));

        String sKey = "splash";
        if (sErrorMessage != null) sKey = "splash-empty";

        // localize
        sKey = PropertyConfig.localize(sKey, engine.getLocale());
        ImageDef img = ImageConfig.getImageDef(sKey);
        if (!img.getImageURL().toString().equals(bgFile_.toString()))
        {
            ic_.changeName(sKey);
        }

        // version label
        DDLabel version = new DDLabel("version", "Splash");
        GuiManager.setLabelAsMessage(version, engine.getVersion());
        version.setHorizontalAlignment(SwingConstants.CENTER);
        Dimension size = version.getPreferredSize();
        JComponent versionpanel = GuiUtils.NORTH(version);
        xy = new XYConstraints(ic_.getWidth() - size.width - BUTTONSIZE - 8, 5, size.width, size.height);
        ic_.add(versionpanel, xy);

        if (sErrorMessage != null)
        {
            DDLabel wrong = new DDLabel(GuiManager.DEFAULT, "SplashWrong");
            wrong.setText(sErrorMessage);
            xy = new XYConstraints(25, 105, ic_.getWidth() - 20, wrong.getPreferredSize().height);
            ic_.add(wrong, xy);

            MouseAdapter listener = new MouseAdapter()
            {
                // exit if clicked
                public void mouseReleased(MouseEvent e)
                {
                    engine.exit(0);
                }
            };
            GuiUtils.addMouseListenerChildren(this, listener);
            getContentPane().addMouseListener(listener);

            // notify user input required
            AudioConfig.playFX("bell");
        }

        // exit button
        DDButton exit = new GlassButton("deleteitem", "Glass");
        exit.setBorderGap(0, 2, 0, 0);
        exit.addActionListener(_ -> System.exit(0));
        exit.setFocusable(false);
        exit.setFocusPainted(false);
        xy = new XYConstraints(ic_.getWidth() - BUTTONSIZE - 5, 4, BUTTONSIZE, BUTTONSIZE);
        ic_.add(exit, xy);

        // frame final setup
        validate();
        repaint();
    }

    /**
     * center
     */
    private void center()
    {
        Dimension size = getSize();
        Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        int nX = center.x - (size.width / 2);
        int nY = center.y - (size.height / 2);
        setLocation(nX, nY);
    }
}
