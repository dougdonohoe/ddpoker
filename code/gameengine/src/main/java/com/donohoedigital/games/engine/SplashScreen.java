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

import com.donohoedigital.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;

/**
 * Splash screen.  Displayed from Game Engine as soon as possible, then later updated
 * when config files have been loaded.  Best not to use logger / PropertyConfig or other
 * config related APIs in constructor.
 *
 * @author Doug Donohoe
 */
public class SplashScreen extends JFrame implements ActionListener, MouseListener
{
    private DDButton full_;
    private DDButton win_;
    private DDButton del_;
    private GameEngine engine_;
    private JLabel bpfull_, bpwin_;
    private ImageComponent ic_;
    private URL bgFile_;

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
        ic_.setLayout(new XYLayout());
        setContentPane(ic_);

        // iocn
        setIconImage(ImageDef.getBufferedImage(icon));

        // frame final setup
        validate();
        pack();
        center();
    }

    /**
     * Called by GameEngine after config files loaded
     */
    public void changeUI(GameEngine engine, boolean bSkipSplashChoice, String sErrorMessage)
    {
        engine_ = engine;

        int BUTTONSIZE = 15;
        XYConstraints xy;
        setTitle(PropertyConfig.getMessage("msg.title.splash"));

        String sKey = bSkipSplashChoice ? "splash-nochoice" : "splash";
        if (sErrorMessage != null) sKey = "splash-empty";

        // localize
        sKey = PropertyConfig.localize(sKey, engine_.getLocale());
        ImageDef img = ImageConfig.getImageDef(sKey);
        if (!img.getImageURL().equals(bgFile_))
        {
            ic_.changeName(sKey);
        }

        // version label
        DDLabel version = new DDLabel("version", "Splash");
        GuiManager.setLabelAsMessage(version, engine_.getVersion());
        version.setHorizontalAlignment(SwingConstants.CENTER);
        Dimension size = version.getPreferredSize();
        JComponent versionpanel = GuiUtils.NORTH(version);
        xy = new XYConstraints(ic_.getWidth() - size.width - BUTTONSIZE - 8, 5, size.width, size.height);
        ic_.add(versionpanel, xy);

        // screen mode buttons
        if (!bSkipSplashChoice)
        {
            // fullscreen button
            full_ = new GlassButton("fullscreen", "Glass");
            full_.addActionListener(this);
            JComponent fullpanel = GuiUtils.NORTH(full_);
            xy = new XYConstraints(70, 210, 80, 40);
            ic_.add(fullpanel, xy);

            bpfull_ = new JLabel();
            xy = new XYConstraints(48, 103, 120, 90);
            ic_.add(bpfull_, xy);
            bpfull_.addMouseListener(this);
            bpfull_.setCursor(Cursors.HAND);

            // windows button
            win_ = new GlassButton("window", "Glass");
            win_.addActionListener(this);
            JComponent winpanel = GuiUtils.NORTH(win_);
            xy = new XYConstraints(250, 210, 80, 40);
            ic_.add(winpanel, xy);

            bpwin_ = new JLabel();
            xy = new XYConstraints(228, 103, 120, 90);
            ic_.add(bpwin_, xy);
            bpwin_.addMouseListener(this);
            bpwin_.setCursor(Cursors.HAND);

            // notify user input required
            AudioConfig.playFX("bell");
        }

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
                    engine_.exit(0);
                }
            };
            GuiUtils.addMouseListenerChildren(this, listener);
            getContentPane().addMouseListener(listener);

            // notify user input required
            AudioConfig.playFX("bell");
        }

        // delete button
        del_ = new GlassButton("deleteitem", "Glass");
        del_.setBorderGap(0, 2, 0, 0);
        del_.addActionListener(this);
        del_.setFocusable(false);
        del_.setFocusPainted(false);
        xy = new XYConstraints(380, 4, BUTTONSIZE, BUTTONSIZE);
        ic_.add(del_, xy);

        // frame final setup
        validate();
        repaint();
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e)
    {
        action(e.getSource());
    }

    /**
     * button press
     */
    private void action(Object source)
    {
        if (source == del_)
        {
            System.exit(0);
        }
        else if (source == full_ || source == bpfull_)
        {
            engine_.setFull(true);
        }
        else if (source == win_ || source == bpwin_)
        {
            engine_.setFull(false);
        }

        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        engine_.showMainWindow();
                    }
                }
        );
    }

    /**
     * mouse click
     */
    public void mouseClicked(MouseEvent e)
    {
        action(e.getSource());
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {

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
