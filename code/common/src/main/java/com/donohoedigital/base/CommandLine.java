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
package com.donohoedigital.base;

import java.util.*;

/**
 * This class is used to parse command line arguments
 * Features:  Auto usage message, auto -? and -help options
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class CommandLine
{
    // members
    private static String sUsage_ = null;
    private static Integer nMinRequiredParams_ = null;  // min number of non-option params
    private static Integer nMaxRequiredParams_ = null;  // max number of non-option params
    private static String sParamName_ = "[file]";
    private static String sParamUsage_ = "[file 1] ... [file N]";
    private static String sParamDesc_ = "a file";
    private static Map<String, Option> htOpts_ = new HashMap<String, Option>(); // options to gather

    private static TypedHashMap htValues_ = new TypedHashMap(); // options gathered
    private static String[] saRemainingArgs_ = null; // command line values not part of args
    private static String sMacFileArg_ = null; // filename passed into a mac

    // types
    private static final int OPT_UNSPECIFIED = 0;
    private static final int OPT_STRING = 1;
    private static final int OPT_INTEGER = 2;
    private static final int OPT_FLAG = 3;
    private static final int OPT_DOUBLE = 4;

    // inner class to store option information
    private static class Option
    {
        int nType = OPT_UNSPECIFIED;
        boolean bRequired = false;
        String sName = null;
        String sDesc = null;
        String sExample = null;
        Object oDefault = null;
    }

    // setup methods

    /**
     * Return Option for given name; null if non-existant
     */
    private static Option getOption(String sName)
    {
        return getOption(sName, false);
    }

    /**
     * Return Option for given name; create new if non-existant
     */
    private static Option getNewOption(String sName)
    {
        return getOption(sName, true);
    }

    /**
     * Return Option for given name; create new if non-existant and
     * bCreate passed
     */
    private static Option getOption(String sName, boolean bCreate)
    {
        Option o = htOpts_.get(sName);
        if (o == null)
        {
            if (bCreate)
            {
                o = new Option();
                o.sName = sName;
                htOpts_.put(sName, o);
            }
        }
        return o;
    }

    /**
     * Set usage for this command line app.  Typically this is a description
     * of the application w/out explaining each parameter (those descriptions
     * are set individually)
     */
    public static void setUsage(String sUsage)
    {
        sUsage_ = sUsage;
    }

    /**
     * Set description and example for an option
     */
    public static void setDescription(String sOpt, String sDesc, String sExample)
    {
        Option o = getNewOption(sOpt);

        o.sDesc = sDesc;
        o.sExample = sExample;
    }

    /**
     * Set description for an option
     */
    public static void setDescription(String sOpt, String sDesc)
    {
        setDescription(sOpt, sDesc, null);
    }

    /**
     * Specify whether the option is required to be provided
     */
    public static void setRequired(String sOpt)
    {
        Option o = getNewOption(sOpt);
        o.bRequired = true;
    }

    /**
     * Add a string option, with default value
     */
    public static void addStringOption(String sOpt, String sDefault)
    {
        Option o = getNewOption(sOpt);
        o.oDefault = sDefault;
        o.nType = OPT_STRING;
    }

    /**
     * Specify mininum number of non-optioned command line arguments are
     * required.  Typically used for commands which take
     * one-to-many items like file names (for example, foo <file1> ... <filen>)
     */
    public static void setMinParams(int nNum)
    {
        nMinRequiredParams_ = nNum;
    }

    /**
     * Specify maxinum number of non-optioned command line arguments are
     * required.  Typically used for commands which take
     * a fixed number of items like diff (for example, diff <file1> <file2>)
     */
    public static void setMaxParams(int nNum)
    {
        nMaxRequiredParams_ = nNum;
    }

    /**
     * Sets a description for parameters passed at end of command line.  Used
     * in usage and error message.   The sName is what appears in the
     * usage parameter list (e.g., "file").  The sUsage string is what appears
     * after the usage summary (e.g., "[file 1] ... [file N]").  The sDesc
     * string is the description of the parameter.
     */
    public static void setParamDescription(String sName,
                                           String sUsage,
                                           String sDesc)
    {
        sParamName_ = sName;
        sParamUsage_ = sUsage;
        sParamDesc_ = sDesc;
    }

    /**
     * Add an integer option, with default value
     */
    public static void addIntegerOption(String sOpt, int nDefault)
    {
        Option o = getNewOption(sOpt);
        o.oDefault = nDefault;
        o.nType = OPT_INTEGER;
    }

    /**
     * Add an double option, with default value
     */
    public static void addDoubleOption(String sOpt, double dDefault)
    {
        Option o = getNewOption(sOpt);
        o.oDefault = dDefault;
        o.nType = OPT_DOUBLE;
    }

    /**
     * Add a flag option
     */
    public static void addFlagOption(String sOpt)
    {
        Option o = getNewOption(sOpt);
        o.nType = OPT_FLAG;
    }

    // utility methods

    /**
     * Parse arguments from main().  Results can be obtained with
     * getOptions() and getRemainingArgs() methods
     */
    @SuppressWarnings({"AssignmentToForLoopParameter"})
    public static void parseArgs(String[] args)
    {
        String sArg;
        boolean bDone = false;
        htValues_ = new TypedHashMap();
        List<String> vArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++)
        {
            sArg = args[i];

            if (sArg.charAt(0) == '-' && !bDone)
            {
                // if only a -, that ends command line processing
                if (sArg.length() == 1)
                {
                    bDone = true;
                    continue;
                }

                sArg = sArg.substring(1); // lop off the -

                // look for ? or help
                if (sArg.equals("?") || sArg.equals("help"))
                {
                    printUsage();
                    System.exit(0);
                }

                // get the option
                Option o = getOption(sArg);
                if (o == null)
                {
                    exitWithError("Unknown option: " + sArg);
                }

                switch (o.nType)
                {
                    case OPT_FLAG:
                        htValues_.put(sArg, Boolean.TRUE);
                        break;

                    case OPT_INTEGER:
                        if ((i + 1) >= args.length)
                        {
                            exitWithError("Value not found for option: " + sArg);
                        }

                        String sInt = args[i + 1];
                        i++;
                        int n = 0;
                        try
                        {
                            n = Integer.parseInt(sInt);
                        }
                        catch (NumberFormatException nfe)
                        {
                            exitWithError("Value for option " + sArg + " is not an integer: " + sInt);
                        }

                        htValues_.put(sArg, n);
                        break;

                    case OPT_DOUBLE:
                        if ((i + 1) >= args.length)
                        {
                            exitWithError("Value not found for option: " + sArg);
                        }

                        String dInt = args[i + 1];
                        i++;
                        double d = 0;
                        try
                        {
                            d = Double.parseDouble(dInt);
                        }
                        catch (NumberFormatException nfe)
                        {
                            exitWithError("Value for option " + sArg + " is not a double: " + dInt);
                        }

                        htValues_.put(sArg, d);
                        break;

                    case OPT_STRING:
                        if ((i + 1) >= args.length)
                        {
                            exitWithError("Value not found for option: " + sArg);
                        }

                        String s = args[i + 1];
                        i++;
                        htValues_.put(sArg, s);
                        break;

                    case OPT_UNSPECIFIED:
                    default:
                        exitWithError("Option type not specified (application coding error): " + sArg);
                }
            }
            else
            {
                vArgs.add(sArg);
            }
        }

        // iterate through options and enter into the values those that
        // had default values, but were not specified by the user.  Also
        // check for required values
        Iterator<String> iter = htOpts_.keySet().iterator();
        Option o;
        Object value;
        String sRequired = null;

        while (iter.hasNext())
        {
            o = getOption(iter.next());
            value = htValues_.get(o.sName);
            if (value == null)
            {
                if (o.bRequired)
                {
                    if (sRequired == null)
                        sRequired = o.sName;
                    else
                        sRequired = sRequired + ' ' + o.sName;
                }

                if (o.oDefault != null)
                {
                    htValues_.put(o.sName, o.oDefault);
                }
            }
        }

        if (sRequired != null)
        {
            exitWithError("Required options missing: " + sRequired);
        }

        // add mac file arg to remaining args
        if (sMacFileArg_ != null) vArgs.add(sMacFileArg_);

        // check size of remaining args
        int nNum = vArgs.size();
        if (nMinRequiredParams_ != null && nNum < nMinRequiredParams_)
        {
            exitWithError("Requires at least " + nMinRequiredParams_ +
                          " (" + sParamName_ + ')');
        }
        else if (nMaxRequiredParams_ != null && nNum > nMaxRequiredParams_)
        {
            exitWithError("Requires no more than " + nMaxRequiredParams_ +
                          " (" + sParamName_ + ')');
        }

        // Create string array of remaining args
        saRemainingArgs_ = new String[nNum];
        for (int i = 0; i < nNum; i++)
        {
            saRemainingArgs_[i] = vArgs.get(i);
        }
    }

    /**
     * Return parsed options in TypedHashMap, mapping option name to
     * Object containing value (String, Integer or Boolean)
     */
    public static TypedHashMap getOptions()
    {
        return htValues_;
    }

    /**
     * Return commandline arguments not part of options
     */
    public static String[] getRemainingArgs()
    {
        return saRemainingArgs_;
    }

    /**
     * Set mac file argument
     */
    public static void setMacFileArg(String s)
    {
        sMacFileArg_ = s;
    }

    /**
     * Print error message, usage and then exit
     */
    public static void exitWithError(String sMsg)
    {
        System.out.println("\n***\n*** ERROR: " + sMsg + "\n***");
        printUsage();
        System.exit(1);
    }

    /**
     * Print usage message
     */
    private static void printUsage()
    {
        String sParamUsage = "";
        if (nMinRequiredParams_ != null && nMaxRequiredParams_ != null)
        {
            sParamUsage = sParamUsage_;
        }
        System.out.println("\nUsage: " + sUsage_ + ' ' + sParamUsage);
        Iterator<String> iter = htOpts_.keySet().iterator();
        Option o;
        boolean bRequired = false;

        // print param info
        if (nMinRequiredParams_ != null || nMaxRequiredParams_ != null)
        {
            System.out.println("\nParams:\n");
            System.out.print("   " + sParamName_);

            // min only
            if (nMaxRequiredParams_ == null)
            {
                System.out.print(" (" + nMinRequiredParams_ + " or more)");
            }
            // max only
            else if (nMinRequiredParams_ == null)
            {
                System.out.print(" (no more than " + nMaxRequiredParams_ + ')');
            }
            // min and max
            else
            {
                System.out.print(" (between " + nMinRequiredParams_ +
                                 " and " + nMaxRequiredParams_ + ')');
            }

            System.out.println(" - " + sParamDesc_);
        }

        System.out.println("\nOptions:\n");

        while (iter.hasNext())
        {
            o = getOption(iter.next());
            if (o.bRequired) bRequired = true;

            System.out.println((o.bRequired ? "*" : " ") +
                               "  -" + o.sName +
                               (o.sExample == null ? "" : " [" + o.sExample + ']') +
                               " - " +
                               (o.sDesc == null ? "" : o.sDesc));
        }
        if (bRequired) System.out.println("\n* denotes required option");
    }

}
