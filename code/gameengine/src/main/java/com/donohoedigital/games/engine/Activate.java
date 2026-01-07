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
 * Activate.java
 *
 * Created on August 1, 2003, 2:47 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.net.*;

/**
 * @author Doug Donohoe
 */
public class Activate extends BasePhase implements PropertyChangeListener
{
    //static Logger logger = LogManager.getLogger(Activate.class);

    private DDTextField reg_;
    private MenuBackground menu_;
    private DDButton registerButton_;
    private boolean bPatch_;

    /**
     * Creates a new instance of Register
     */
    public Activate()
    {
    }

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // get last key
        String sLast = engine_.getLastLicenseKey();
        bPatch_ = (sLast != null);

        // in case we came here from banned key
        AudioConfig.stopBackgroundMusic();

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        String STYLE = gamephase_.getString("menubox-style", "default");
        String sTextBorderStyle_ = gamephase_.getString("text-border-style", "default");

        // buttons
        ButtonBox buttonbox = new ButtonBox(context_, gamephase, this, "empty", false, false);
        menu_.getMenuBox().add(buttonbox, BorderLayout.SOUTH);

        // base area for displaying information
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        base.setBorderLayoutGap(5, 0);
        menu_.getMenuBox().add(base, BorderLayout.CENTER);

        ////
        //// activate
        ////
        DDLabelBorder activate = new DDLabelBorder("activate", STYLE);
        base.add(activate, BorderLayout.CENTER);

        // text
        DDHtmlArea activateText = new DDHtmlArea(GuiManager.DEFAULT, "Activation");
        activateText.setDisplayOnly(true);
        activateText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        activateText.setText(PropertyConfig.getMessage(bPatch_ ? "msg.activate.patch" : "msg.activate.welcome"));
        activate.add(activateText, BorderLayout.CENTER);

        // text field for reg
        DDPanel regbase = new DDPanel();
        regbase.setBorderLayoutGap(0, 10);
        regbase.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        activate.add(GuiUtils.CENTER(regbase), BorderLayout.SOUTH);

        DDLabel label = new DDLabel("register", STYLE);
        regbase.add(GuiUtils.NORTH(label), BorderLayout.WEST);

        // create button before reg so property change listener works
        registerButton_ = new GlassButton("activate", "GlassBig");
        registerButton_.setEnabled(false);

        DDPanel regbase2 = new DDPanel();
        DDLabel sample = new DDLabel("regsample", STYLE);
        regbase2.add(sample, BorderLayout.SOUTH);
        reg_ = new RegText(GuiManager.DEFAULT, STYLE, sTextBorderStyle_);
        reg_.setColumns(14);
        reg_.addPropertyChangeListener("value", this);
        reg_.setTextLengthLimit(22); // key is 19, but allow 22 for extra space at beginning/end (copy/paste space padding support)
        reg_.setRegExp("^\\s*[0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]\\s*$");

        // BUG 294 - if a previous key has been set, we are doing a
        // patch re-activation.  Pre-populate key field
        if (bPatch_)
        {
            reg_.setText(sLast);
            reg_.setCaretPosition(sLast.length());
        }
        regbase2.add(reg_, BorderLayout.CENTER);
        regbase.add(GuiUtils.NORTH(regbase2), BorderLayout.CENTER);

        // finish registerButton setup
        reg_.setDefaultOverride(registerButton_);
        registerButton_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                activate();
            }
        });
        DDPanel actButtonBase = new DDPanel();
        actButtonBase.add(registerButton_, BorderLayout.NORTH);
        actButtonBase.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        regbase.add(actButtonBase, BorderLayout.EAST);

        ////
        //// demo
        ////
        DDLabelBorder demo = new DDLabelBorder("demo", STYLE);
        base.add(demo, BorderLayout.SOUTH);

        // text
        DDHtmlArea demoText = new DDHtmlArea(GuiManager.DEFAULT, "Activation");
        demoText.setDisplayOnly(true);
        demoText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        demoText.setText(PropertyConfig.getMessage("msg.activate.demo"));
        demo.add(demoText, BorderLayout.CENTER);

        // button
        DDPanel demoButtonBase = new DDPanel();
        demoButtonBase.setLayout(new GridLayout(2, 1, 0, 5));
        DDButton demoButton = new GlassButton("demo", "Glass");
        demoButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                engine_.setDemoMode();
            }
        });
        DDButton orderButton = new GlassButton("order", "Glass");
        orderButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                engine_.setActivationNeeded(false); // temporary so can show order dialog
                context_.processPhaseNow("Order", null);
                engine_.setActivationNeeded(true);
            }
        });
        demoButtonBase.add(demoButton);
        demoButtonBase.add(orderButton);
        demo.add(GuiUtils.CENTER(demoButtonBase), BorderLayout.EAST);

    }

    // override to avoid selectall-on-focus behavoir
    private class RegText extends DDTextField
    {
        public RegText(String a, String b, String c)
        {
            super(a, b, c);
        }

        @Override
        public void focusGained(FocusEvent e)
        {
            // do nothing
        }
    }

    @Override
    public void start()
    {
        context_.getWindow().setHelpTextWidget(null); // no more help after this

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, reg_);
    }

    /**
     * Don't allow default button processing - to avoid hack
     */
    @Override
    public boolean processButton(GameButton button)
    {
        if (button.getName().equals("exit"))
        {
            System.exit(0);
        }

        return false;
    }

    private void activate()
    {
        // BUG 198 - ask user to confirm license
        engine_.setActivationNeeded(false); // temporary so can show license
        License lic = (License) context_.processPhaseNow("License", null);
        engine_.setActivationNeeded(true);

        GameButton result = (GameButton) lic.getResult();
        if (result.getName().startsWith("yes"))
        {
            engine_.setLicenseKey(reg_.getText().trim());

            // BUG 275 - verify key
            boolean bVerify = engine_.getVersion().isVerify();
            VerifyActivationKey dialog = null;
            if (bVerify)
            {
                // BUG 294 - fail silently if no internet connection present
                // BUG 294 - or firewall prevents connection
                try
                {
                    // get local host
                    InetAddress localaddr = InetAddress.getLocalHost();
                    String local = localaddr.getHostAddress();
                    if (local == null || local.equals("127.0.0.1") || local.equals("0.0.0.0"))
                    {
                        logger.warn("Skipping verification: no local address: " + local);
                        bVerify = false;
                    }
                    else
                    {
                        logger.info("Verifying from local addr: " + localaddr.getHostAddress());
                    }
                }
                catch (UnknownHostException uhe)
                {
                    bVerify = false;
                    logger.warn("Unknown host - can't determine local address");
                }

                // check again
                if (bVerify)
                {
                    TypedHashMap params = new TypedHashMap();
                    params.setBoolean(MessageErrorDialog.PARAM_IGNORE_INET_ERRORS, Boolean.TRUE);
                    dialog = (VerifyActivationKey) context_.processPhaseNow("VerifyActivationKey", params);
                    ApplicationError.assertNotNull(dialog, "No dialog for VerifyActivationKey");
                }
            }

            // if no verifying done, or if done, status is OK, proceed
            if (!bVerify || dialog.getStatus() == DDMessageListener.STATUS_OK)
            {
                // license okay
                engine_.keyValidated(bPatch_);
                context_.seedHistory("StartMenu");
                context_.processPhase("UserRegistration");
                // BUG 198 - engine_.processTODO moved to UserRegistration
            }
            else
            {
                reg_.setText("");
                engine_.resetLicenseKey(); // ensure reset in case failed due to unavailable connection
            }
        }
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        registerButton_.setEnabled(
                Activation.validate(engine_.getKeyStart(), reg_.getText().trim(), engine_.getLocale()) &&
                !engine_.isBannedLicenseKey(reg_.getText().trim())
        );
    }

}
