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
 * HelpConfig.java
 *
 * Created on August 05, 2003, 6:26 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import org.jdom2.*;

import java.net.*;
import java.util.*;

/**
 * Loads help.xml files in the module directories defined by
 * the appconfig.xml file.
 *
 * @author  donohoe
 */
public class HelpConfig extends XMLConfigFileLoader
{
    private static Logger hLogger = LogManager.getLogger(HelpConfig.class);
    
    private String HELP_CONFIG = "help.xml";

    private static HelpConfig helpConfig = null;
    
    private Map<String, HelpTopic> helps_ = new HashMap<String, HelpTopic>();
    private List<HelpTopic> helparray_ = new ArrayList<HelpTopic>();
    
    /** 
     * Creates a new instance of HelpConfig from the Appconfig file 
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    public HelpConfig(String[] modules, String sLocale) throws ApplicationError
    {
        ApplicationError.warnNotNull(helpConfig, "HelpConfig is already initialized");
        helpConfig = this;

        if (sLocale != null)
        {
            HELP_CONFIG = "help-"+sLocale+".xml";
        }
        init(modules);
    }
    
    /**
     * Get help topic list
     */
    public static List<HelpTopic> getHelpTopics()
    {
        ApplicationError.assertNotNull(helpConfig, "HelpConfig not initialized");
        return helpConfig.helparray_;
    }
    
    /**
     * Get help topic by name
     */
    public static HelpTopic getHelpTopic(String sName)
    {
        ApplicationError.assertNotNull(helpConfig, "HelpConfig not initialized");
        return helpConfig.helps_.get(sName);
    }
    
    /**
     * Load helps from modules
     */
    private void init(String[] modules) throws ApplicationError
    {
        ApplicationError.assertNotNull(modules, "Modules list is null");
        
        Document doc;
        for (String module : modules)
        {
            // if help file is missing, no big deal
            URL url = new MatchingResources("classpath*:config/" + module + "/" + HELP_CONFIG).getSingleResourceURL();
            if (url != null)
            {
                doc = this.loadXMLUrl(url, "help.xsd");
                init(doc, module);
            }
        }
    }
    
    /**
     * Initialize from JDOM doc
     */
    private void init(Document doc, String module) throws ApplicationError
    {
        Element root = doc.getRootElement();
        
        // helpdir name
        String helpDir = getChildStringValueTrimmed(root, "helpdir", ns_, true, HELP_CONFIG);
        
        // get list of helps
        List<Element> helps = getChildren(root, "help", ns_, false, HELP_CONFIG);
        if (helps == null) return;
        
        // create helpdef for each one
        for (Element help : helps)
        {
            initHelp(help, module, helpDir);
        }
    }
    
    /**
     * Read help info
     */
    private void initHelp(Element help, String module, String helpdir) throws ApplicationError
    {
        String sName = getStringAttributeValue(help, "name", true, HELP_CONFIG);
        String sLocation = getStringAttributeValue(help, "location", true, HELP_CONFIG);
        String sDisplay = getStringAttributeValue(help, "display", true, HELP_CONFIG);
        Integer nIndent = getIntegerAttributeValue(help, "indent", true, HELP_CONFIG);

        String location = module + "/" + helpdir + "/" + sLocation;
        URL url = new MatchingResources("classpath*:config/" + location).getSingleResourceURL();
        if (url == null)
        {
            hLogger.warn("Help " + sName + " not found at " + location + ".  Skipping");
            return;
        }
        
        HelpTopic topic = new HelpTopic(sName, sDisplay, url, nIndent);
        helps_.put(sName, topic);
        helparray_.add(topic);
    }
}
