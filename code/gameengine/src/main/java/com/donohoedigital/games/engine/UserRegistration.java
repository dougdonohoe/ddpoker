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
 * UserRegistration.java
 *
 * Created on September 1, 2003, 9:35 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.beans.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;

/**
 *
 * @author  Doug Donohoe
 */
public class UserRegistration extends BasePhase implements PropertyChangeListener
{
    //static Logger logger = LogManager.getLogger(UserRegistration.class);
    
    private DDHtmlArea text_;
    private ButtonBox buttonbox_;
    private MenuBackground menu_;
    private DDButton registerButton_;
    private DDButton laterButton_;
    private DDButton returnButton_;
    private DDButton reregButton_;
    private JComponent focus_;
    private RegistrationMessage msg_ = new RegistrationMessage(EngineMessage.CAT_USER_REG);
    private List<DDOption> options_ = new ArrayList<DDOption>();
    private long nRegTime_ = 0;
    private Preferences prefs_;
    private static String REGTIME = "regtime";
    
    /** 
     * Creates a new instance of UserRegistration 
     */
    public UserRegistration() {
    }
    
    /**
     * return whether registered
     */
    public static boolean isRegistered()
    {
        String NODE = GameEngine.getGameEngine().getPrefsNodeName() + "/userreg";
        Preferences prefs = DDOption.getOptionPrefs(NODE);
        long nRegTime = prefs.getLong(REGTIME, 0);
        return nRegTime > 0;
    }
    
    /**
     * init
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        
        // node info
        String NODE = engine_.getPrefsNodeName() + "/userreg";
        prefs_ = DDOption.getOptionPrefs(NODE);
        nRegTime_ = prefs_.getLong(REGTIME, 0);
        
        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        String STYLE = gamephase_.getString("menubox-style", "default");
        String sTextBorderStyle_ = gamephase_.getString("text-border-style", "default");

        // buttons
        buttonbox_ = new ButtonBox(context_, gamephase, this, "empty", false, false);
        menu_.getMenuBox().add(buttonbox_, BorderLayout.SOUTH);
        registerButton_ = buttonbox_.getDefaultButton();
        laterButton_= buttonbox_.getButton("reglater");
        ApplicationError.assertNotNull(laterButton_, "No reglater button");
        returnButton_= buttonbox_.getButton("returnmain");
        ApplicationError.assertNotNull(returnButton_, "No returnmain button");
        reregButton_ = buttonbox_.getButton("reregister");
        ApplicationError.assertNotNull(reregButton_, "No reregister button");
        
        registerButton_.setEnabled(false);
        buttonbox_.removeButton(returnButton_);
        buttonbox_.removeButton(reregButton_);
        
        // base area for displaying information
        DDPanel base = new DDPanel();
        menu_.getMenuBox().add(base, BorderLayout.CENTER);
        
        // text
        text_ = new DDHtmlArea(GuiManager.DEFAULT, "Registration");
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuTextBorder());
        base.add(text_, BorderLayout.CENTER);
        
        // reg info
        DDPanel regbase = new DDPanel();
        base.add(GuiUtils.CENTER(regbase), BorderLayout.SOUTH);
        regbase.setBorder(BorderFactory.createEmptyBorder(10,10,55,10)); // bottom border used to move fields up
        regbase.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, VerticalFlowLayout.RIGHT));
        
        // options start
        int WIDTH = 450;

        // name
        OptionText otext = new OptionText(NODE, RegistrationMessage.PARAM_NAME, STYLE, sTextBorderStyle_, msg_, 100, "^.+$", WIDTH, true);
        regbase.add(otext);
        focus_ = otext.getTextField();
        otext.getTextField().addPropertyChangeListener("value", this);
        options_.add(otext);

        // email
        otext = new OptionText(NODE, RegistrationMessage.PARAM_REG_EMAIL, STYLE, sTextBorderStyle_, msg_, 256, PropertyConfig.getRequiredStringProperty("regexp.email"), WIDTH, true);
        regbase.add(otext);
        otext.getTextField().addPropertyChangeListener("value", this);
        options_.add(otext);
        
        // address
        OptionTextArea atext = new OptionTextArea(NODE, RegistrationMessage.PARAM_ADDRESS, STYLE, sTextBorderStyle_, msg_, 1024, ".(?:.|\\s)*", 2, WIDTH);
        regbase.add(atext);
        atext.getTextArea().addPropertyChangeListener("value", this);
        options_.add(atext);
        
        // city
        otext = new OptionText(NODE, RegistrationMessage.PARAM_CITY, STYLE, sTextBorderStyle_, msg_, 50, "^.+$", WIDTH, true);
        regbase.add(otext);
        otext.getTextField().addPropertyChangeListener("value", this);
        options_.add(otext);
        
        // state
        otext = new OptionText(NODE, RegistrationMessage.PARAM_STATE, STYLE, sTextBorderStyle_, msg_, 50, null, WIDTH, true);
        regbase.add(otext);
        otext.getTextField().addPropertyChangeListener("value", this);
        options_.add(otext);
        
        // postal
        otext = new OptionText(NODE, RegistrationMessage.PARAM_POSTAL, STYLE, sTextBorderStyle_, msg_, 50, "^.+$", WIDTH, true);
        regbase.add(otext);
        otext.getTextField().addPropertyChangeListener("value", this);
        options_.add(otext);
        
        // country
        otext = new OptionText(NODE, RegistrationMessage.PARAM_COUNTRY, STYLE, sTextBorderStyle_, msg_, 120, "^.+$", WIDTH, true);
        regbase.add(otext);
        otext.getTextField().addPropertyChangeListener("value", this);
        options_.add(otext);   
        
        // update if already reg'd
        regCheck();
    }
    
    public void start()
    {        
        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, focus_);
    }
 
    /**
     *  if registered, set fields display only
     */
    private void regCheck()
    {             
        if (nRegTime_ != 0)
        {
            text_.setText(PropertyConfig.getMessage("msg.userRegistered", 
                                PropertyConfig.getDateFormat(engine_.getLocale()).format(new Date(nRegTime_)),
                                engine_.getRealLicenseKey()));
            for (DDOption opt : options_)
            {
                opt.setDisplayOnly(true);
            }

            buttonbox_.removeButton(registerButton_);
            buttonbox_.removeButton(laterButton_);
            buttonbox_.addButton(reregButton_);
            buttonbox_.addButton(returnButton_);
            buttonbox_.revalidate();
        }
        else
        {
            text_.setText(PropertyConfig.getMessage("msg.userNotRegistered", engine_.getRealLicenseKey()));

            for (DDOption opt : options_)
            {
                opt.setDisplayOnly(false);
            }
            
            buttonbox_.removeButton(returnButton_);
            buttonbox_.removeButton(reregButton_);
            buttonbox_.addButton(registerButton_);
            buttonbox_.addButton(laterButton_);
            
            buttonbox_.revalidate();
            checkButtons();
        }
    }
    
    /**
     * register
     */
    public boolean processButton(GameButton button) 
    {
        if (button.getName().equals(registerButton_.getName()))
        {
            TypedHashMap params = new TypedHashMap();
            params.setObject(UserRegistrationSend.PARAM_USER_REG, msg_);
            UserRegistrationSend dialog = (UserRegistrationSend) context_.processPhaseNow("UserRegistrationSend", params);
            if (dialog.getStatus() == DDMessageListener.STATUS_OK)
            {
                // user registered!
                nRegTime_ = System.currentTimeMillis();
                prefs_.putLong(REGTIME, nRegTime_);
                regCheck();
            }
            return true;
        }
        else if (button.getName().equals("reregister"))
        {
            if (EngineUtils.displayConfirmationDialog(context_, PropertyConfig.getMessage("msg.reregister")))
            {
                prefs_.remove(REGTIME);
                nRegTime_ = 0;
                regCheck();
            }
            return true;
        }
        else
        {
            // if other button press, process TO-DO phase if that is defined
            // (from activation) and return false to prevent normal processing
            if (context_.hasTODO())
            {
                context_.processTODO();
                return false;
            }

        }
        
        return true;
    }
    
    /** 
     * When text field changes
     */
    public void propertyChange(PropertyChangeEvent evt) 
    {
        checkButtons();
    }
    
    /**
     * Enable buttons
     */
    private void checkButtons()
    {
        for (DDOption opt : options_)
        {
            if (nRegTime_ != 0 || !opt.isValidData())
            {
                registerButton_.setEnabled(false);
                return;
            }
        }
        registerButton_.setEnabled(true);
    }
}
