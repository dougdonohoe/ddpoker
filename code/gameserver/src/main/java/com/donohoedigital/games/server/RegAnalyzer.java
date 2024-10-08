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
 * RegAnalyzer.java
 *
 * Created on September 27, 2004, 3:47 PM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.server.model.*;
import com.donohoedigital.games.server.model.util.*;
import com.donohoedigital.games.server.service.*;
import com.donohoedigital.jsp.*;
import org.apache.logging.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "JDBCExecuteWithNonConstantString"})
public class RegAnalyzer
{
    // logging
    private static Logger logger = LogManager.getLogger(RegAnalyzer.class);

    // option names
    public static final String OPTION_KEY_START = "keystart";
    public static final String OPTION_GAME = "game";
    public static final String OPTION_NOCOUNTS = "nocounts";
    public static final String OPTION_NOSUSPECT = "nosuspect";
    public static final String OPTION_NOTIMES = "notimes";
    public static final String OPTION_OUT = "out";

    // options
    private String sGame_;
    private File output_;
    private boolean bDoCounts_;
    private boolean bDoTimes_;
    private boolean bDoSuspect_;
    private String keyStart = null;

    // stat members
    private int totalRegistrations;
    private int numActivations;
    private int numPatches;
    private int numRegistrations;
    private int numMac;
    private int numWindows;
    private int numLinux;
    private List<Counter> monthCnt_ = new ArrayList<Counter>();
    private List<Counter> weekCnt_ = new ArrayList<Counter>();
    private List<Counter> dayCnt_ = new ArrayList<Counter>();
    private int[] hourCnt_ = new int[24];

    // keys members
    private List<RegInfo> suspectKeys_ = new ArrayList<RegInfo>();
    private List<RegInfo> bannedKeys_ = new ArrayList<RegInfo>();

    // services
    private BannedKeyService bannedService;
    private RegistrationService regService;

    /**
     * Implements command line application interface.
     */
    private static class RegAnalyzerApp extends BaseCommandLineApp
    {
        private RegAnalyzerApp(String sConfigName, String[] args)
        {
            // init app (this parses the args)
            super(sConfigName, args);
        }

        /**
         * our specific options
         */
        protected void setupApplicationCommandLineOptions()
        {
            CommandLine.addStringOption(OPTION_GAME, null);
            CommandLine.setDescription(OPTION_GAME, "game to analyze (e.g., war-aoi or poker)", "name");
            CommandLine.setRequired(OPTION_GAME);

            CommandLine.addStringOption(OPTION_OUT, null);
            CommandLine.setDescription(OPTION_OUT, "output file for html", "file");
            CommandLine.setRequired(OPTION_OUT);

            CommandLine.addStringOption(OPTION_KEY_START, null);
            CommandLine.setDescription(OPTION_KEY_START, "start of activation key (e.g., 21 (v1) or 22 (v2)", "key");

            CommandLine.addFlagOption(OPTION_NOCOUNTS);
            CommandLine.setDescription(OPTION_NOCOUNTS, "do not query registration counts");

            CommandLine.addFlagOption(OPTION_NOTIMES);
            CommandLine.setDescription(OPTION_NOTIMES, "do not query registration times");

            CommandLine.addFlagOption(OPTION_NOSUSPECT);
            CommandLine.setDescription(OPTION_NOSUSPECT, "do not query suspect keys");
        }
    }

    /**
     * Run analyzer
     */
    public static void main(String[] args) 
    {
        try
        {
            // Create app to parse command line options
            RegAnalyzerApp info = new RegAnalyzerApp("servertools", args);

            logger.info("Analyzer initializing, params: " + Utils.toString(args, " "));

            // create application context
            ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-gameserver.xml");

            // get the analyzer bean and run it
            RegAnalyzer app = (RegAnalyzer) ctx.getBean("regAnalyzer");
            app.initAndRun(info.getCommandLineOptions());
        }
        catch (ApplicationError ae)
        {
            System.err.println("RegAnalyzer ending due to ApplicationError: " + ae.toString());
        }  
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
        }
        catch (Throwable t)
        {
            System.err.println(Utils.formatExceptionText(t));
        }
        
        System.exit(0);
    }

    /**
     * Create an instance called from other applications (e.g., the website).
     */
    public RegAnalyzer()
    {
    }

    /**
     * Initialize options and then run the analyzer
     */
    public void initAndRun(TypedHashMap htOptions)
    {
        // get params
        setOptions(htOptions);

        // do the work
        long time = System.currentTimeMillis();
        doAnalyze();
        logger.debug("Elapsed time: " + (System.currentTimeMillis() - time));
    }

    private void setOptions(TypedHashMap htOptions)
    {
        sGame_ = htOptions.getString(OPTION_GAME);

        String value = htOptions.getString(OPTION_OUT);
        output_ = (value != null) ? new File(value) : null;

        bDoCounts_ = !(htOptions.getBoolean(OPTION_NOCOUNTS, false));
        bDoTimes_ = !(htOptions.getBoolean(OPTION_NOTIMES, false));
        bDoSuspect_ = !(htOptions.getBoolean(OPTION_NOSUSPECT, false));
        keyStart = htOptions.getString(OPTION_KEY_START, null);
    }

    public BannedKeyService getBannedService()
    {
        return bannedService;
    }

    @Autowired
    public void setBannedService(BannedKeyService bannedService)
    {
        this.bannedService = bannedService;
    }

    public RegistrationService getRegService()
    {
        return regService;
    }

    @Autowired
    public void setRegService(RegistrationService regService)
    {
        this.regService = regService;
    }

    /**
     * Get key start
     */
    public String getKeyStart()
    {
        return keyStart;
    }

    /**
     * get list of banned RegInfo
     */
    public List<RegInfo> getBannedKeys()
    {
        return bannedKeys_;
    }

    /**
     * get list of suspect RegInfo
     */
    public List<RegInfo> getSuspectKeys()
    {
        return suspectKeys_;
    }

    public int getTotalRegistrations()
    {
        return totalRegistrations;
    }

    public int getNumActivations()
    {
        return numActivations;
    }

    public int getNumPatches()
    {
        return numPatches;
    }

    public int getNumRegistrations()
    {
        return numRegistrations;
    }

    public int getNumMac()
    {
        return numMac;
    }

    public int getNumWindows()
    {
        return numWindows;
    }

    public int getNumLinux()
    {
        return numLinux;
    }

    /**
     * Get array of DayCnt
     */
    public List<Counter> getDayCount()
    {
        return dayCnt_;
    }

    /**
     * Get month count
     */
    public List<Counter> getMonthCount()
    {
        return monthCnt_;
    }

    /**
     * Get week count
     */
    public List<Counter> getWeekCount()
    {
        return weekCnt_;
    }

    /**
     * Get hour count
     */
    public int[] getHourCount()
    {
        return hourCnt_;
    }

    /**
     * Get game name as passed on command line
     */
    public String getGame()
    {
        return sGame_;
    }

    /**
     * Process all years in save directory
     */
	private void doAnalyze()
	{
        // load each file's registrations
        logger.debug("RUNNING: scanning registration records in " + sGame_);

        // query values - banned keys are required; all other values are options
        doBannedKeys();
        if (bDoSuspect_) doSuspectKeys();
        if (bDoCounts_) doCounts();
        if (bDoTimes_) doTimes();

        // generate HTML if output file specified
        if (output_ != null)
        {
            JspFile jsp = new JspFile("regstats", null, this);
            jsp.executeJSP(output_);
        }
    }

    /**
     * retrieve the banned keys
     */
    private void doBannedKeys()
    {
        bannedKeys_ = regService.getBannedKeys(keyStart);
    }

    /**
     * retrieve the suspect keys
     */
    private void doSuspectKeys()
    {
        suspectKeys_ = regService.getSuspectKeys(keyStart, 5);
    }

    /**
     * retrieve the registration counts
     */
    private void doCounts()
    {
        numRegistrations = regService.countRegistrationType(keyStart, Registration.Type.REGISTRATION);
        numActivations = regService.countRegistrationType(keyStart, Registration.Type.ACTIVATION);
        numPatches = regService.countRegistrationType(keyStart, Registration.Type.PATCH);
        totalRegistrations = regService.countKeysInUse(keyStart);
        numLinux = regService.countOperatingSystem(keyStart, "Linux");
        numMac = regService.countOperatingSystem(keyStart, "Mac OS");
        numWindows = regService.countOperatingSystem(keyStart, "Windows");
    }

    /**
     * retrieve the time values
     */
    private void doTimes()
    {
        Calendar c = Calendar.getInstance();
        int dayofyear = 0;
        int month = 0;
        int year = 0;
        int count = 0;
        String dayname = null;

        // query the day, month, and week
        List<RegDayOfYearCount> results = regService.countByDayOfYear(keyStart);

        // loop through results and query tally
        for (RegDayOfYearCount row : results)
        {
            count = row.getCount();
            dayofyear = row.getDay();
            year = row.getYear();

            c.set(Calendar.YEAR, year);
            c.set(Calendar.DAY_OF_YEAR, dayofyear);

            month = c.get(Calendar.MONTH);

            switch(c.get(Calendar.DAY_OF_WEEK))
            {
                case Calendar.MONDAY: dayname = "M"; break;
                case Calendar.TUESDAY: dayname = "T"; break;
                case Calendar.WEDNESDAY: dayname = "W"; break;
                case Calendar.THURSDAY: dayname = "T"; break;
                case Calendar.FRIDAY: dayname = "F"; break;
                case Calendar.SATURDAY: dayname = "S"; break;
                case Calendar.SUNDAY: dayname = "S"; break;
            }

            record(dayCnt_, year, dayofyear, count, dayname);
            record(monthCnt_, year, month, count, null);
            recordWeek(weekCnt_, c, count);
        }

        List<RegHourCount> results2 = regService.countByHour(keyStart);
        for (RegHourCount row : results2)
        {
            hourCnt_[row.getHour()] = row.getCount();
        }
    }

    @SuppressWarnings({"PublicInnerClass"})
    public static class Counter
    {
        private int year;
        private int sub;
        private int count;
        private String comment;
        
        public Counter(int nYear, int nSub, String sComment)
        {
            this.year = nYear;
            this.sub = nSub;
            this.comment = sComment;
        }
        
        public boolean equals(Object o)
        {
            if (!(o instanceof Counter)) return false;
            Counter c = (Counter) o;
            return year == c.year && sub == c.sub;
        }

        public int hashCode()
        {
            return 31 * year + sub;
        }

        public int getYear()
        {
            return year;
        }

        public int getSub()
        {
            return sub;
        }

        public int getCount()
        {
            return count;
        }

        public String getComment()
        {
            return comment;
        }
    }
    
    private Counter cLookup = new Counter(0,0,null);
    private void record(List<Counter> a, int nYear, int nSub, int nCnt, String sComment)
    {
        Counter c;
        cLookup.year = nYear;
        cLookup.sub = nSub;
        
        int nIndex = a.indexOf(cLookup);
        if (nIndex == -1)
        {
            c = new Counter(nYear, nSub, sComment);
            c.count = nCnt;
            a.add(c);
        }
        else
        {
            c = a.get(nIndex);
            c.count += nCnt;
        }
    }
    
    private void recordWeek(List<Counter> a, Calendar c, int nCnt)
    {
        int nDay = c.get(Calendar.DAY_OF_WEEK);
        if (nDay < Calendar.SATURDAY)
        {
            c.add(Calendar.DATE, Calendar.SATURDAY - nDay);
        }
        
        String sComment = "" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DAY_OF_MONTH);
        
        record(a, c.get(Calendar.YEAR), c.get(Calendar.DAY_OF_YEAR), nCnt, sComment);
    }
}
