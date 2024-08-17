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
/*
 * Demo.java
 *
 * Created on August 11, 2003, 4:47 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import java.awt.*;
import java.util.prefs.*;

/**
 * @author Doug Donohoe
 */
public class Demo extends BasePhase
{
    //static Logger logger = Logger.getLogger(Demo.class);

    private DDHtmlArea text_;
    private MenuBackground menu_;
    private DDButton demoButton_;
    public static final String PREF_DEMO_LICENSE_DISPLAYED = "demo-license-shown";

    /**
     * Creates a new instance of Demo
     */
    public Demo()
    {
    }

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        String sBoxStyle = gamephase_.getString("menubox-style", "default");

        // buttons
        ButtonBox buttonbox = new ButtonBox(context_, gamephase, this, "empty", false, false);
        menu_.getMenuBox().add(buttonbox, BorderLayout.SOUTH);
        demoButton_ = buttonbox.getDefaultButton();

        // base area for displaying information
        DDPanel base = new DDPanel();
        menu_.getMenuBox().add(base, BorderLayout.CENTER);

        // text
        text_ = new DDHtmlArea(GuiManager.DEFAULT, sBoxStyle);
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuTextBorder());
        base.add(text_, BorderLayout.CENTER);
    }

    @Override
    public void start()
    {
        context_.getWindow().setHelpTextWidget(text_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help
        context_.getWindow().setHelpTextWidget(null); // no more help after this

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, demoButton_);
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

        if (button.getName().equals("playgame"))
        {
            boolean bOkay = false;

            EnginePrefs prefs = engine_.getPrefsNode();
            if (!prefs.getBoolean(PREF_DEMO_LICENSE_DISPLAYED, false))
            {
                engine_.setBDemo(false); // temp
                License lic = (License) context_.processPhaseNow("License", null);
                engine_.setBDemo(true);

                GameButton result = (GameButton) lic.getResult();
                if (result.getName().startsWith("yes"))
                {
                    prefs.putBoolean(PREF_DEMO_LICENSE_DISPLAYED, true);
                    bOkay = true;
                }
            }
            else
            {
                bOkay = true;
            }


            if (bOkay)
            {
                engine_.setDemoMsgDisplayed();
                context_.processTODO();
            }
        }

        if (button.getName().startsWith("order"))
        {
            engine_.setBDemo(false);
            context_.processPhaseNow("Order", null);
            engine_.setBDemo(true);
        }

        return false;
    }

}
