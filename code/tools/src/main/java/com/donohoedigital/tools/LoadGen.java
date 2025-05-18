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
package com.donohoedigital.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.server.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * * Class to test performance of a website.
 * * <P>
 * <PRE>
 * Usage: LoadGen [options] [url file 1] ... [url file N]
 * <p/>
 * -log             <filename>     String  Specify name of file to log summary information
 * -threads         <#>            Integer # of threads for each session to create
 * -loops           <#>            Integer # of times to loop through each sessions's urls
 * -sleep                          Flag    Sleep between URL requests (as specified by 'sleepBefore' in urls file)
 * -sleepsecs       <#>            Integer # of seconds to sleep between requests
 * -sleepfirst                     Flag    Sleep before the 1st request
 * -nopagestats                    Flag    Skip printing of stats for each page as they are requested
 * -outputpage                     Flag    Output page as it comes back (used to verify test)
 * -forcenewsession                Flag    Force a new session to be created on each request (for special testing)
 * -calcstats                      Flag    Calculate min/max/avg time for each individual url
 * </PRE>
 * * <P>
 * * -threads specifies the number of separate threads to start up for each
 * * session.  A Session is a collecion of URLs in a single url file
 * * Each thread establishes a new Weblogic session and requests each URL
 * * in the collection. The collection of URLs is requested -loop times.
 * * A new session is established at the beginning of the loop.
 * * <P>
 * * As each page is requested, you will see output like the following:<BR>
 * * <PRE>
 * *  filename Tn [xxx.yy] N.NNN http://machine:7001/main/home.jsp (zzzzz) [n]
 * * </PRE>
 * * Where Tn is the thread number for the session, filename is the name
 * * of the file that contained the URLs, [xxx.yy] is the loop that thread is on
 * * (xxx is the loop, yy is the index of the url:  e.g., 050.02 means the
 * * 50th loop, 2nd URL in the url list),
 * * N.NNN is the number of seconds it took to fetch the page.  After the URL
 * * that is fetched, (zzzzz) is the number of bytes returned in the page and
 * * [n] is number of zeros received at the end of the page (this is used to
 * * look for a bug where extra bytes were being sent down -- a valid
 * * value is 0 or 1).  You will see a summary at the end like below (this
 * * is what is put in the local log file):
 * * <PRE>
 * * GRAND TOTAL errors:                2500 (20%)
 * * GRAND TOTAL requests:             12500
 * * GRAND TOTAL kbytes:              213145
 * * GRAND TOTAL elapsed time:            97.938 seconds
 * * AVERAGE KBYTES/SECOND:             2176.326
 * * AVERAGE PAGES/SECOND:               127.632
 * * AVERAGE RESPONSE TIME:                0.074 seconds
 * * </PRE>
 * *
 * * Note that the averages are across all threads and all loops.
 * * <P>
 * * You can log summary information and errors to a log file with the -log option.
 * *
 * * @author Doug Donohoe
 * *
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public class LoadGen
{

    // Debugging
    static boolean bPageStats = true;  // if false, turns off stats for each page
    static boolean bCalcStats = false;  // if true, turns on response time stats for each URL
    static boolean bPageVerbose = false;  // if true, prints page to console
    static boolean bDoSessions = true;   // if false, each request generates a new session; if true, each thread=1 session
    static boolean bLogLocally = false;
    static boolean bSleep = false;
    static boolean bSleepFirst = false;
    static int nSleepSecs = 0;

    // params
    static int nThreads = 0;
    static int nLoops = 0;

    // formatters
    static Format fFloating = new Format("%17.3f");
    static Format fsum4Floating = new Format("%4.3f");
    static Format fsum5Floating = new Format("%5.3f");
    static Format fsum6Floating = new Format("%6.3f");
    static Format fsumCount = new Format("%3d");
    static Format fDecimal = new Format("%13d");
    static Format fTime = new Format("%2.3f");
    static Format fString = new Format("%-13s");
    static Format fLoop = new Format("%03d");
    static Format fBytes = new Format("%5d");
    static Format fURL = new Format("%-40s"); // must match TRIM
    static int TRIM = 40;

    /**
     * Sets up application-specific command line options.
     */
    private static void setupCommandLineOptions()
    {

        CommandLine.setUsage("LoadGen [options]");

        CommandLine.setMinParams(1);
        CommandLine.setParamDescription("[url list]",
                                        "[url list 1] ... [url list N]",
                                        "A file containing a list of URLs (one per line)");

        CommandLine.addStringOption("log", null);
        CommandLine.setDescription("log", "Specify name of file to log summary information", "filename");

        CommandLine.addIntegerOption("threads", 1);
        CommandLine.setDescription("threads", "# of threads for each session to create", "#");

        CommandLine.addIntegerOption("loops", 1);
        CommandLine.setDescription("loops", "# of times to loop through each sessions's urls", "#");

        CommandLine.addIntegerOption("sleepsecs", 0);
        CommandLine.setDescription("sleepsecs", "# secs to sleep between each request", "#");

        CommandLine.addFlagOption("sleep");
        CommandLine.setDescription("sleep", "Sleep between URL requests (as specified by 'sleepsecs')");

        CommandLine.addFlagOption("sleepfirst");
        CommandLine.setDescription("sleepfirst", "Sleep before the 1st request");

        CommandLine.addFlagOption("nopagestats");
        CommandLine.setDescription("nopagestats", "Skip printing of stats for each page as they are requested");

        CommandLine.addFlagOption("outputpage");
        CommandLine.setDescription("outputpage", "Output page as it comes back (used to verify test)");

        CommandLine.addFlagOption("forcenewsession");
        CommandLine.setDescription("forcenewsession", "Force a new session to be created every request (special testing)");

        CommandLine.addFlagOption("calcstats");
        CommandLine.setDescription("calcstats", "Calculate min/max/avg response time for each URL");
    }

    /**
     * * It all happens here
     */
    public static void main(String[] args)
    {
        //String urlFile;
        String outFile;
        List<URLSpec[]> vURLSpecs = new ArrayList<URLSpec[]>();

        setupCommandLineOptions();
        CommandLine.parseArgs(args);
        String[] saArgs = CommandLine.getRemainingArgs();
        TypedHashMap htOptions = CommandLine.getOptions();

        try
        {
            //urlFile = htOptions.getString("urls");
            outFile = htOptions.getString("log");
            if (outFile != null)
            {
                bLogLocally = true;
                setupLog(outFile);
            }
            bPageStats = !(htOptions.getBoolean("nopagestats", !bPageStats));
            bCalcStats = htOptions.getBoolean("calcstats", bCalcStats);
            bDoSessions = !(htOptions.getBoolean("forcenewsession", !bDoSessions));
            bPageVerbose = htOptions.getBoolean("outputpage", bPageVerbose);
            bSleep = htOptions.getBoolean("sleep", bSleep);
            bSleepFirst = htOptions.getBoolean("sleepfirst", bSleepFirst);
            nSleepSecs = htOptions.getInteger("sleepsecs", nSleepSecs);
            if (nSleepSecs > 0) bSleep = true;
            nThreads = htOptions.getInteger("threads", 1);
            nLoops = htOptions.getInteger("loops", 1);

            // print out what arguments this program was started with
            String startup = "\nRunning LoadGen";
            for (String arg : args)
            {
                startup = startup + " " + arg;
            }
            log(startup + "\n");

            // Read URL files
            for (int i = 0; i < saArgs.length; i++)
            {
                vURLSpecs.add(readFile(saArgs, i));
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
            System.exit(-1);
        }


        // overall test start time
        long time1 = System.currentTimeMillis();

        // Get ready to start threads
        URLSpec urlspecs[];
        String sSessionName;
        int nSessions = vURLSpecs.size();
        UrlRunner[] runners = new UrlRunner[nThreads * nSessions];

        // info message
        println("");
        log("STARTING Files: " + nSessions + ", Threads: " + nThreads + ", Loops: " + nLoops);

        // create the threads and start them
        int nRunnerCnt = 0;
        for (int j = 0; j < nSessions; j++)
        {
            urlspecs = vURLSpecs.get(j);
            sSessionName = saArgs[j];
            for (int i = 0; i < nThreads; i++)
            {
                runners[nRunnerCnt] = new UrlRunner(sSessionName, i, urlspecs, nLoops);
                runners[nRunnerCnt].start();
                nRunnerCnt++;

                // Stagger slightly
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException ie)
                {
                    Thread.interrupted();
                }
            }
        }

        long totalRequests = 0;
        long totalResponseTime = 0;
        long totalErrors = 0;
        long totalBytes = 0;

        // wait for threads to finish and gather stats
        for (int i = 0; i < nRunnerCnt; i++)
        {
            try
            {
                runners[i].join();
                totalRequests += runners[i].getTotalCount();
                totalResponseTime += runners[i].getTotalTime();
                totalBytes += runners[i].getTotalBytes();
                totalErrors += runners[i].getErrors();
            }
            catch (InterruptedException ie)
            {
                Thread.interrupted();
            }
        }

        // overall test stop time
        long time2 = System.currentTimeMillis();

        // total test execution time
        long total = (time2 - time1);

        totalBytes /= 1000; // convert to Kbytes
        double time = (double) total / 1000.0; // elapsed time in seconds
        double avg = 1.0 / (time / totalRequests); // pages/second
        double response = (double) totalResponseTime / 1000.0 / (double) totalRequests; // average response time in seconds
        double avgbytes = 1.0 / (time / totalBytes);
        long totalErrorPercent = (totalErrors * 100) / (totalRequests > 0 ? totalRequests : 1);

        log("\nFINISHED Files: " + nSessions + ", Threads: " + nThreads + ", Loops: " + nLoops);

        if (bCalcStats)
        {
            log("");
            log("Individual URL Statistics:");
            URLSpec urlspec;
            int MAXLEN = 35;
            for (int j = 0; j < nSessions; j++)
            {
                urlspecs = vURLSpecs.get(j);
                log("\n  " + saArgs[j]);
                log("              AVG    MIN    MAX   VAR    TIME    #  URL");
                log("              ---    ---    ---   ---    ----    -  ------------------");
                double urlavg;
                double variance;
                double count;
                String sURL;
                for (int i = 0; i < urlspecs.length; i++)
                {
                    urlspec = urlspecs[i];
                    urlavg = (urlspec.dRunningTotal / (double) urlspec.nCount);
                    count = (double) urlspec.nCount;

                    // (1/(N-1) * Sum((Xi)2)) - (N/(N-1) * (Xavg)2)
                    variance = (1 / (count - 1) *
                                urlspec.dRunningTotalSquares) -
                               (count / (count - 1) * urlavg * urlavg);

                    sURL = urlspec.sURL;
                    if (sURL.length() > MAXLEN) sURL = sURL.substring(0, MAXLEN);
                    log("    URL #" + (i + 1) + " - " +
                        fsum5Floating.form(urlavg) + "  " +
                        fsum4Floating.form(urlspec.dMin) + "  " +
                        fsum5Floating.form(urlspec.dMax) + "  " +
                        fsum5Floating.form(variance) + "  " +
                        fsum6Floating.form(urlspec.dRunningTotal) + " " +
                        fsumCount.form(urlspec.nCount) + "  " +
                        sURL);


                }
            }
        }

        println("");
        log("GRAND TOTAL errors:       " + fDecimal.form(totalErrors) + " (" + totalErrorPercent + "%)");
        log("GRAND TOTAL requests:     " + fDecimal.form(totalRequests));
        log("GRAND TOTAL kbytes:       " + fDecimal.form(totalBytes));
        log("GRAND TOTAL elapsed time: " + fFloating.form(time) + " seconds");
        log("AVERAGE KBYTES/SECOND:    " + fFloating.form(avgbytes));
        log("AVERAGE PAGES/SECOND:     " + fFloating.form(avg));
        log("AVERAGE RESPONSE TIME:    " + fFloating.form(response) + " seconds");

        println("\nShutting down...");
        println("Done.");

        closeLog();
        System.exit(0);
    }

    /**
     * Read the given file, it assumes one URL per line.  Return
     * URL Specs:
     * <p/>
     * TODO: allow specification of sleep time (not implemented yet)
     */
    private static URLSpec[] readFile(String saArgs[], int index)
    {
        String sFile = saArgs[index];
        File file = new File(sFile);
        if (!file.exists() || !file.isFile())
        {
            System.out.println("File does not exist: " + sFile);
            System.exit(-1);
        }

        List<String> vURLs = new ArrayList<String>();

        try
        {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader buf = new BufferedReader(new InputStreamReader(fis));
            String sLine;
            while ((sLine = buf.readLine()) != null)
            {
                //System.out.println("Line: " + sLine);

                // skip lines that start with '#' -- for commenting
                if (sLine.charAt(0) != '#')
                {
                    vURLs.add(sLine);
                }
            }

            fis.close();
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.println("File not found: " + sFile + " :" + fnfe);
            System.exit(-1);
        }
        catch (IOException ioe)
        {
            System.out.println("Error reading " + sFile + " :" + ioe);
            System.exit(-1);
        }

        int nSize = vURLs.size();
        URLSpec specs[] = new URLSpec[nSize];
        for (int i = 0; i < nSize; i++)
        {
            specs[i] = new URLSpec(vURLs.get(i), 0);
        }

        // change argument to just file name (eliminate any path info)
        saArgs[index] = file.getName();
        return specs;
    }

    /**
     * * This method logs a message to the logfile if logging is on as
     * * well as printing out the message to the console
     */
    private static void log(String sMsg)
    {
        println(sMsg);
    }

    /**
     * * This class is the guts of the test
     */
    private static class UrlRunner extends Thread
    {
        String sThreadName;
        URLSpec urlspecs[];
        int loop;
        int loopstart;
        int nCount = 0;
        String cookie = null;
        long total = 0;
        long totalbytes = 0;
        int errors = 0;
        int bytesRead = 0;
        int zeroBytesRead = 0;
        String name = null;
        byte[] bytes = new byte[10000];

        /**
         * * Returns total pages requested by this runner
         */
        public int getTotalCount()
        {
            return nCount;
        }

        /**
         * * Returns total bytes read by this runner
         */
        public long getTotalBytes()
        {
            return totalbytes;
        }

        /**
         * * Returns total cumulative request time
         */
        public long getTotalTime()
        {
            return total;
        }

        /**
         * * Returns # error pages
         */
        public long getErrors()
        {
            return errors;
        }

        public UrlRunner(String sName, int n, URLSpec urls[], int l)
        {
            name = sName;
            sThreadName = fString.form(sName + " T" + (n + 1));
            this.setName(sThreadName);
            urlspecs = urls;
            loop = l;
            loopstart = loop;
        }

        public void run()
        {
            String data = null;
            boolean bError;
            long sleep;
            String url;
            URLSpec urlspec;

            // Loop until loop count decremented to zero
            while (loop > 0)
            {
                // each loop starts new session
                cookie = null;

                // Loop through each page
                for (int i = 0; i < urlspecs.length; i++)
                {
                    bError = false;
                    urlspec = urlspecs[i];
                    url = urlspec.sURL;
                    sleep = nSleepSecs;//urlspec.nSleepBefore;

                    // sleep unless sleeping turned off
                    if (bSleep)
                    {
                        // sleep unless first request and not sleeping on first request
                        if (!(loop == loopstart && !bSleepFirst))
                        {
                            System.out.println("Sleeping " + sleep);
                            Utils.sleepSeconds(sleep);
                        }
                    }

                    long time1 = System.currentTimeMillis();
                    try
                    {

                        data = getURL(url);
                        if (data != null)
                        {
                            bError = true;
                            errors++;
                        }
                        nCount++;
                    }
                    catch (Exception e)
                    {
                        bError = true;
                        errors++;
                    }
                    long time2 = System.currentTimeMillis();
                    total += (time2 - time1);
                    totalbytes += bytesRead;
                    double time = (double) (time2 - time1) / 1000.0;

                    // log URL:  fileName + i is unique id
                    if (bCalcStats)
                    {
                        synchronized (LoadGen.class)
                        {
                            urlspec.nCount++;
                            if (time > urlspec.dMax) urlspec.dMax = time;
                            if (time < urlspec.dMin) urlspec.dMin = time;
                            urlspec.dRunningTotal += time;
                            urlspec.dRunningTotalSquares += (time * time);
                        }
                    }

                    // Output time to console
                    if (bPageStats)
                    {
                        // sync to avoid thread safety problems in formatters
                        synchronized (LoadGen.class)
                        {
                            String urlout = fURL.form(url);
                            if (bError)
                            {
                                urlout = fURL.form("*" + data + "* " + url);
                            }
                            if (urlout.length() > TRIM)
                            {
                                urlout = urlout.substring(0, TRIM);
                            }

                            System.out.println(" " + sThreadName +
                                               " [" + fLoop.form((long) loop) + "." + (i + 1) + "]" +
                                               " " + fTime.form(time) +
                                               " " + urlout +
                                               " (" + fBytes.form(bytesRead) + ")" +
                                               " [" + zeroBytesRead + "]");
                        }
                    }
                }

                --loop;
            }

            double time = (double) total / 1000.0;
            double avg = time / nCount;

            // At end of thread, log times
            if (bPageStats)
            {
                synchronized (LoadGen.class)
                {
                    log("Thread " + sThreadName + " TOTAL errors:         " + fDecimal.form(errors));
                    log("Thread " + sThreadName + " TOTAL request time:   " + fFloating.form(time));
                    log("Thread " + sThreadName + " AVERAGE request time: " + fFloating.form(avg));
                }
            }
        }

        /**
         * * Connect to site and get content of the given URL
         */
        public String getURL(String sURL) throws Exception
        {

            URL url = null;
            bytesRead = 0;
            zeroBytesRead = 0;
            String sPost = null;
            String sPostContentType = null;
            int nPostIndex;

            // look for post data
            if ((nPostIndex = sURL.indexOf(ServletDebug.POST_DELIMITER)) >= 0)
            {
                sPost = sURL.substring(nPostIndex + ServletDebug.POST_DELIMITER.length());
                if (sPost.length() == 0) sPost = null;
                sURL = sURL.substring(0, nPostIndex);

                int nContentTypeIndex = sPost.indexOf(ServletDebug.CONTENT_TYPE_DELIMITER);
                sPostContentType = sPost.substring(nContentTypeIndex + ServletDebug.CONTENT_TYPE_DELIMITER.length());
                sPost = sPost.substring(0, nContentTypeIndex);

                //System.out.println("WRITING url: " + sURL);
                //System.out.println("WRITING post: " + sPost);
                //System.out.println("WRITING content-type: " + sPostContentType);
            }

            try
            {
                url = new URL(sURL);
            }
            catch (MalformedURLException me)
            {
                throw new Exception("MALFORMED_URL: " + me.toString());
            }

            int nRead = 0;
            try
            {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1");//DDMessenger.USERAGENT);
                // Add cookies
                if (cookie != null && bDoSessions)
                {
                    //System.out.println("\nSENDING Cookie: " + cookie);
                    conn.setRequestProperty("Cookie", cookie);
                }

                // If a post, write it out
                if (sPost != null)
                {
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("content-type", sPostContentType);
                    OutputStream o = conn.getOutputStream();
                    o.write(sPost.getBytes());
                    o.write("\n".getBytes()); // for War!
                    o.close();
                }

                //String sContentType = conn.getContentType();
                int nResponseCode = conn.getResponseCode();

                // Check for error pages
                if (nResponseCode != 200)
                {
                    //TODO: use getErrorStream() to get returned data
                    conn.disconnect();
                    //println("FAILED (" + nResponseCode + "): " + sURL);
                    return "" + nResponseCode;
                }
                int nHeader = 0;
                String key, value;
                int nIndex;
                while (true)
                {
                    key = conn.getHeaderFieldKey(nHeader);
                    value = conn.getHeaderField(nHeader);

                    if (key == null && value == null) break;

                    // Capture cookies sent down
                    if (key != null && key.equalsIgnoreCase("Set-Cookie"))
                    {
                        // strip off expires, path, etc
                        nIndex = value.indexOf(";");
                        value = value.substring(0, nIndex);

                        //System.out.println("RECEIVED COOKIE: " + key + ":" + value);
                        if (cookie == null)
                        {
                            cookie = value;
                        }
                        else
                        {
                            cookie = value + "; " + cookie;
                        }
                    }

                    nHeader++;
                }

                // Read data off stream
                InputStream is = conn.getInputStream();
                int nExtra = 0;
                while ((nRead = is.read(bytes)) != -1)
                {
                    nExtra = 0;
                    // Loop through each byte read -- we count the
                    // ascii zero bytes that are read -- to make sure
                    // no issues with extra data at the end of a page
                    for (int j = 0; j < nRead; j++)
                    {
                        if (bytes[j] == 0)
                        {
                            nExtra++;

                        }
                        else if (bPageVerbose)
                        {
                            System.out.print((char) bytes[j]);
                        }
                    }

                    //if (bPageVerbose)
                    //{
                    //System.out.println();
                    //}

                    bytesRead += nRead;
                    zeroBytesRead += nExtra;

                    // we don't actually keep the data for performance reasons
                    //bmbytes.write(bytes, 0, nRead);
                }
                is.close();
                conn.disconnect();
            }
            catch (IOException io)
            {
                System.out.println("FAILED (IOException): " + io.getMessage());
                return "IO";
            }

            //return bmbytes.toByteArray();
            return null;
        }

    }

    private static final String PROPERTY_ENCODING = "file.encoding";
    static Writer writer = null;

    private static void setupLog(String file)
    {
        if (!bLogLocally) return;
        System.out.println("\n***** Logging to " + file);
        try
        {
            OutputStream stream = new FileOutputStream(file);
            writer = new OutputStreamWriter(stream, System.getProperty(PROPERTY_ENCODING));
        }
        catch (Exception e)
        {
            System.out.println("Error opening log: " + e);
            System.exit(-1);
        }
    }

    private static void closeLog()
    {
        if (!bLogLocally) return;
        try
        {
            writer.close();
        }
        catch (Exception e)
        {
            System.out.println("Error opening log: " + e);
            System.exit(-1);
        }

    }

    private static void println(String sMsg)
    {
        print(sMsg + "\n");
    }

    private static void print(String sMsg)
    {
        try
        {
            if (bLogLocally) writer.write(sMsg);
            System.out.print(sMsg);
        }
        catch (IOException e)
        {
            System.out.println("Error writing log: " + e);
            System.exit(-1);
        }
    }

    /**
     * This private class lists an URL and the amount of time to
     * sleep before requesting the URL (if the -sleep option is passed)
     */
    private static class URLSpec
    {
        URLSpec(String sURL, int nSleepBefore)
        {
            this.sURL = sURL;
            this.nSleepBefore = nSleepBefore;
        }

        String sURL = null;
        int nSleepBefore = 0;

        int nCount;
        double dMax = 0.0;
        double dMin = Double.MAX_VALUE;
        double dRunningTotal = 0.0;
        double dRunningTotalSquares = 0.0;
        // average = nRunningTotal/nCount
    }

}
