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
 * MenuPhase.java
 *
 * Created on March 24, 2005, 5:12 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class MenuPhase extends BasePhase 
{
    //static Logger logger = LogManager.getLogger(MenuPhase.class);
    
    protected DDHtmlArea helptext_;
    protected ButtonBox buttonbox_;
    protected MenuBackground menu_;
    protected DDPanel centerPanel_;
    protected String STYLE;

    /** 
     * Creates a new instance of MenuPhase 
     */
    public MenuPhase() {
    }
    
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        STYLE = gamephase_.getString("menubox-style", "default");

        // center panel is where help text and any sublcass
        // customization goes
        centerPanel_ = new DDPanel();
        menu_.getMenuBox().add(centerPanel_, BorderLayout.CENTER);

        // Text area for displaying information
        helptext_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        helptext_.setDisplayOnly(true);
        helptext_.setBorder(getHelpTextBorder());

        // add buttons to main area
        addButtons(menu_.getMenuBox());

        // layout the menu
        layoutMenu(centerPanel_, helptext_);
    }

    /**
     * By default, returns EngineUtils.getStandardMenuTextBorder()
     */
    protected javax.swing.border.Border getHelpTextBorder()
    {
        return EngineUtils.getStandardMenuTextBorder();
    }

    /**
     * Layout the container - place text where desired and add other
     * components
     */
    protected void layoutMenu(DDPanel base, JComponent helptext)
    {
        base.add(helptext, BorderLayout.CENTER);
    }

    /**
     * add buttons to panel.  By default uses values in gamedef to
     * determine where to add it (menubox-orientation) and creates
     * a ButtonBox in that orientation.
     */
    protected void addButtons(DDPanel parent)
    {
        String sOrientation = gamephase_.getString("menubox-orientation", "WEST");
        Object oLayout;
        boolean bVertical;

        if (sOrientation.equalsIgnoreCase("WEST"))
        {
            oLayout = BorderLayout.WEST;
            bVertical = true;
        }
        else if (sOrientation.equalsIgnoreCase("EAST"))
        {
            oLayout = BorderLayout.EAST;
            bVertical = true;
        }
        else if (sOrientation.equalsIgnoreCase("SOUTH"))
        {
            oLayout = BorderLayout.SOUTH;
            bVertical = false;
        }
        else if (sOrientation.equalsIgnoreCase("NORTH"))
        {
            oLayout = BorderLayout.NORTH;
            bVertical = false;
        }
        else
        {
            throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Bad orientation: " + sOrientation, null);
        }
        buttonbox_ = new ButtonBox(context_, gamephase_, this, "empty", bVertical, false);
        parent.add(buttonbox_, oLayout);
    }

    /**
     * start - set this as the main ui component
     */
    public void start()
    {
        context_.getWindow().setHelpTextWidget(helptext_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help
        context_.getWindow().ignoreNextHelp(); // ignore enter so main help message doesn't go away immediately

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, buttonbox_ != null ? (JComponent) buttonbox_.getDefaultButton() : menu_);
    }
    
}
