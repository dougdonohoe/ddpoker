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
 * DDMailer.java
 *
 * Created on September 27, 2003, 5:26 PM
 */

package com.donohoedigital.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.mail.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class DDMailer extends BaseCommandLineApp
{
    // logging
    private Logger logger = LogManager.getLogger(DDMailer.class);

    // members
    private String sKey_;
    private File file_;
    private String sFrom_;
    private List<Object[]> params_ = new ArrayList<Object[]>();

    // debugging/testing - limit number of emails sent
    private int LIMIT = 0;
    private boolean DEBUG = false;
    private String TESTTO = null;
    private DDPostalService ddPostalService;

    /**
     * Run emailer
     */
    public static void main(String[] args)
    {
        try
        {
            new DDMailer("ddmailer", args);
        }
        catch (ApplicationError ae)
        {
            System.err.println("DDMailer ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
    }

    /**
     * Can be overridden for application specific options
     */
    @Override
    protected void setupApplicationCommandLineOptions()
    {
        CommandLine.addStringOption("key", null);
        CommandLine.setDescription("key", "cmdline.properties message key", "key");
        CommandLine.setRequired("key");

        CommandLine.addStringOption("file", null);
        CommandLine.setDescription("file", "email and params file", "file");
        CommandLine.setRequired("file");

        CommandLine.addStringOption("from", null);
        CommandLine.setDescription("from", "from email", "from");
        CommandLine.setRequired("from");

    }

    /**
     * Create War from config file
     */
    public DDMailer(String sConfigName, String[] args)
    {
        super(sConfigName, args);

        // debug
        DEBUG = PropertyConfig.getBooleanProperty("settings.debug.enabled", false);
        LIMIT = PropertyConfig.getIntegerProperty("settings.debug.limit", 2);
        TESTTO = PropertyConfig.getStringProperty("settings.debug.testto", null, false);
        ddPostalService = new DDPostalServiceImpl(true);

        // get key
        sKey_ = htOptions_.getString("key");
        sFrom_ = htOptions_.getString("from");
        logger.info("Message key: " + sKey_);
        logger.info("From: " + sFrom_);

        // debug?
        if (DEBUG && TESTTO != null) logger.debug("DEBUG:  all mail goes to " + TESTTO);

        // load params file
        String sFile = htOptions_.getString("file");
        file_ = new File(sFile);
        loadFile();

        // email
        doEmail();
        ddPostalService.destroy();

        logger.info("Done.");
    }

    /**
     * Load file
     */
    private void loadFile()
    {
        ConfigUtils.verifyFile(file_);
        Reader reader = ConfigUtils.getReader(file_);
        BufferedReader buf = new BufferedReader(reader);

        try
        {
            String sLine;
            StringTokenizer token;
            Object o[];
            int nCnt;
            while ((sLine = buf.readLine()) != null)
            {
                token = new StringTokenizer(sLine);
                o = new Object[token.countTokens()];
                nCnt = 0;
                while (token.hasMoreTokens())
                {
                    o[nCnt++] = token.nextToken();
                }
                params_.add(o);
            }
            ConfigUtils.close(reader);
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }

    /**
     * Process all
     */
    private void doEmail()
    {
        Object o[];
        String sEmail;
        String sProcess;
        boolean bProcess;

        for (int i = 0; i < params_.size(); i++)
        {
            o = params_.get(i);
            sEmail = (String) o[0];
            sProcess = (String) o[o.length - 1];
            bProcess = Utils.parseBoolean(sProcess, false);

            if (bProcess)
            {
                logger.debug("SENDING to " + sEmail);
                sendEmail(sEmail, PropertyConfig.getMessage("email." + sKey_ + ".sub", o),
                          PropertyConfig.getMessage("email." + sKey_, o));
            }
            else
            {
                logger.debug("SKIPPING " + sEmail);
            }

            if (DEBUG && (i + 1) == LIMIT) break;
        }
    }

    /**
     * Send email
     */
    private void sendEmail(String sEmailID, String sSubject, String sPlain)
    {
        // get results and send email
        String sTo = (DEBUG && TESTTO != null) ? TESTTO : sEmailID;

        // send email
        ddPostalService.sendMail(sTo, sFrom_, null, sSubject, sPlain, null, null, null);
    }
}
