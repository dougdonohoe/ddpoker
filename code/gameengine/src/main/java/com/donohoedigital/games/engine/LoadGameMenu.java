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
/*
 * LoadGameMenu.java
 *
 * Created on July 13, 2003, 9:49 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class LoadGameMenu extends BasePhase
{
    //static Logger logger = LogManager.getLogger(LoadGameMenu.class);
    
    private ButtonBox buttonbox_;
    private MenuBackground menu_;
    private DDPanel menubox_;
    GameListPanel panel_;
        
    /** 
     * Creates a new instance of LoadGameMenu 
     */
    public LoadGameMenu() 
    {
    }
    
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        
        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        menubox_ = menu_.getMenuBox();
        
        // put buttons in the menubox_
        buttonbox_ = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox_.add(buttonbox_, BorderLayout.SOUTH);
        String sTextBorderStyle_ = gamephase_.getString("text-border-style", "default");

        // holds data we are gathering
        DDPanel data = new DDPanel();
        BorderLayout layout = (BorderLayout) data.getLayout();
        layout.setVgap(10);
        data.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        menubox_.add(data, BorderLayout.CENTER);

        // name of style used for all widgets in data area
        String STYLE = gamephase_.getString("style", "default");
        panel_ = GameListPanel.newLoadMenuPanel(engine, context_, gamephase, STYLE, sTextBorderStyle_, null, buttonbox_.getDefaultButton());
        data.add(panel_, BorderLayout.CENTER);
    }
    
    /**
     * Start of phase
     */
    public void start()
    {
        // set help text
        //GuiManager.setHelpTextWidget(text_);
        //GuiManager.showHelp(menu_.getMenuBox()); // init help
               
        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, (JComponent)panel_.getFocusComponent());

    }

    /**
     * Returns true
     */
    public boolean processButton(GameButton button) 
    {
        return panel_.processButton(button);
    }
}
