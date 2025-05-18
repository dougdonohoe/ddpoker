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

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class Utils
{
    public static final boolean ISMAC;
    public static final boolean ISLINUX;
    public static final boolean IS141;
    public static final boolean IS142;
    public static final boolean IS14;
    public static final boolean IS15;
    public static final boolean IS16;
    public static final boolean ISWINNT;
    public static final boolean ISWIN9X;
    public static final boolean ISWINDOWS;
    public static final boolean ISMAC_10_4;
    public static final String OS;

    /**
     * Used to turn on/off tcp no-delay
     */
    public static final boolean TCPNODELAY = true;

    /**
     * Charset for things like http headers
     */
    public static final String CHARSET_BASIC_NAME = "ISO-8859-1";

    /**
     * Charset we use
     */
    public static final Charset CHARSET_BASIC = Charset.forName(CHARSET_BASIC_NAME);

    /**
     * Charset name we use
     */
    public static final String CHARSET_NAME = "UTF-8";

    /**
     * Charset we use
     */
    public static final Charset CHARSET = Charset.forName(CHARSET_NAME);

    /**
     * Figure out if this a mac/windows/linux
     * TODO:  GAH!  this code is crap
     */
    static
    {
        boolean ismac = false;
        boolean islinux = false;
        boolean is141 = false;
        boolean is142 = false;
        boolean is14 = false;
        boolean is15 = false;
        boolean is16 = false;
        boolean iswinnt = false;
        boolean iswin9x = false;
        boolean iswindows = false;
        boolean ismac_10_4 = false;

        Properties props = System.getProperties();
        props.setProperty("file.encoding", CHARSET_NAME);

        // java digest says this fixes multi-port binding issue on Mac,
        // but doesn't seem to help
        // props.setProperty("java.net.preferIPv4Stack", "true");

        // debug print keys
//        if (false)
//        {
//            String sKey;
//            Enumeration enu = props.keys();
//            while (enu.hasMoreElements())
//            {
//                sKey = (String) enu.nextElement();
//                System.out.println(sKey+"="+props.getProperty(sKey));
//            }
//        }

        // java/os version info
        String sVersion = (String) props.get("java.runtime.version");
        String osVersion = (String) props.get("os.version");
        String os = (String) props.get("os.name");
        if (os == null) os = "";
        OS = os + " " + osVersion;
        os = os.toLowerCase();

        //System.out.println("OS: " + os + " version: " + sVersion);

        if (sVersion.startsWith("1.4"))
        {
            is14 = true;
        }

        if (sVersion.startsWith("1.4.1"))
        {
            is141 = true;
        }

        if (sVersion.startsWith("1.4.2"))
        {
            is142 = true;
        }

        if (sVersion.startsWith("1.5"))
        {
            is15 = true;
        }

        if (sVersion.startsWith("1.6"))
        {
            is16 = true;
        }

        // mac
        if (isMacOS(os))
        {
            ismac = true;
            if (osVersion.startsWith("10.4")) ismac_10_4 = true;
        }
        // linux
        else if (isLinux(os))
        {
            islinux = true;
            // BUG 360 - use dns for host lookup
            props.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
        }
        // windows
        else if (isWindows(os))
        {
            iswindows = true;
            if (os.contains("9") || os.contains("Me"))
            {
                iswin9x = true;
            }
            else
            {
                iswinnt = true;
            }
        }

        ISMAC = ismac;
        ISLINUX = islinux;
        IS141 = is141;
        IS142 = is142;
        IS14 = is14;
        IS15 = is15;
        IS16 = is16;
        ISWINNT = iswinnt;
        ISWIN9X = iswin9x;
        ISWINDOWS = iswindows;
        ISMAC_10_4 = ismac_10_4;
    }

    public static boolean isWindows(String os)
    {
        return os.toLowerCase().startsWith("windows");
    }

    public static boolean isLinux(String os)
    {
        return os.toLowerCase().startsWith("linux");
    }

    public static boolean isMacOS(String os)
    {
        return os.toLowerCase().startsWith("mac os x");
    }

    // version number to use as part of userhome
    private static String VERSION = "";

    /**
     * Set version string for use in UserHome path
     */
    public static void setVersionString(String s)
    {
        VERSION = s;
    }

    /**
     * Get version string
     */
    public static String getVersionString()
    {
        return VERSION;
    }

    /**
     * Fixes slashes to be correct for file system.  Removes trailing
     * slash
     */
    public static String fixFilePath(String sPath)
    {
        if (sPath == null) return null;
        StringBuilder sb = new StringBuilder(sPath);
        int nSize = sb.length();
        char c;
        for (int i = 0; i < nSize; i++)
        {
            c = sb.charAt(i);
            // look for sep char
            if (c == '\\' || c == '/')
            {
                if (i == (nSize - 1))
                {
                    sb.setLength(nSize - 1); // if sep char at end, remove it
                }
                else
                {
                    sb.setCharAt(i, File.separatorChar); // replace with file system correct
                }
            }
        }
        return sb.toString();
    }

    /**
     * Creates a File object from the given directory and file
     */
    public static File getFile(String sDir, String sFile)
    {
        sDir = fixFilePath(sDir);
        File f = new File(sDir, sFile);
        try
        {
            return f.getCanonicalFile();
        }
        catch (Exception ignore)
        {
        }
        // if canonical fails for some reason, return f
        return f;
    }

    /**
     * Create a File object from given directory or file
     */
    public static File getFile(String sDir)
    {
        sDir = fixFilePath(sDir);
        File f = new File(sDir);
        try
        {
            return f.getCanonicalFile();
        }
        catch (Exception ignore)
        {
        }
        // if canonical fails for some reason, return f
        return f;
    }

    public static String getAllStacktraces()
    {
        StringBuilder sb = new StringBuilder();
        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();

        // sort map
        Map<Thread, StackTraceElement[]> smap = new TreeMap<Thread, StackTraceElement[]>(TC);
        smap.putAll(map);
        Iterator<Thread> iter = smap.keySet().iterator();
        Thread t;
        Object stackitems[];
        while (iter.hasNext())
        {
            t = iter.next();
            //if (!t.getName().startsWith("Thread")) continue; // (testing)
            stackitems = smap.get(t);
            sb.append("Thread: ").append(t.getName()).append(" [").append(t.getClass().getName()).append("] daemon: ").append(t.isDaemon()).append('\n');
            for (Object stackitem : stackitems)
            {
                sb.append("   at ").append(stackitem).append('\n');
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    private static ThreadComparator TC = new ThreadComparator();

    private static class ThreadComparator implements Comparator<Thread>
    {
        public int compare(Thread t1, Thread t2)
        {
            return t1.getName().compareTo(t2.getName());
        }
    }

    /**
     * Return string showing exception message and stack trace
     * for output to regular text
     */
    public static String formatExceptionText(Throwable e)
    {
        if (e == null) return "null";
        ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(ostr));
        //return "Exception: " + e.toString() + "\n" + ostr.toString();
        return ostr.toString(); // this includes the message
    }

    /**
     * Return exception message, or if that is null, exception class name
     */
    public static String getExceptionMessage(Throwable e)
    {
        String msg = e.getMessage();
        if (msg == null) msg = e.getClass().getName();

        return msg;
    }

    /**
     * Print given message and exception to system.err
     */
    public static void printException(String sMsg, Throwable e)
    {
        System.err.println(sMsg + ':' + formatExceptionText(e));
    }

    /**
     * Return string showing exception message and stack trace
     * for output in HTML
     */
    public static String formatExceptionHTML(Throwable e)
    {
        return "<PRE>" + formatExceptionText(e) + "</PRE>";
    }

    /**
     * Get #FF00FF style hex string representation of this color
     */
    public static String getHtmlColor(Color c)
    {
        StringBuilder sb = new StringBuilder(7);
        sb.append('#');
        appendColorPart(c.getRed(), sb);
        appendColorPart(c.getGreen(), sb);
        appendColorPart(c.getBlue(), sb);
        return sb.toString();
    }

    /**
     * get color from #FF00FF style hex string, with alpha value
     */
    public static Color getHtmlColorAlpha(String s)
    {
        if (s == null) return null;
        return new Color(Integer.parseInt(s.substring(1, 3), 16),
                         Integer.parseInt(s.substring(3, 5), 16),
                         Integer.parseInt(s.substring(5, 7), 16),
                         Integer.parseInt(s.substring(7, 9), 16));
    }

    /**
     * Get #FF00FF style hex string representation of this color,
     * but with alpha too
     */
    public static String getHtmlColorAlpha(Color c)
    {
        StringBuilder sb = new StringBuilder(7);
        sb.append('#');
        appendColorPart(c.getRed(), sb);
        appendColorPart(c.getGreen(), sb);
        appendColorPart(c.getBlue(), sb);
        appendColorPart(c.getAlpha(), sb);
        return sb.toString();
    }

    /**
     * get color from #FF00FF style hex string
     */
    public static Color getHtmlColor(String s)
    {
        if (s == null) return null;
        return new Color(Integer.parseInt(s.substring(1, 3), 16),
                         Integer.parseInt(s.substring(3, 5), 16),
                         Integer.parseInt(s.substring(5, 7), 16));
    }

    /**
     * used by getHtmlColor - appends a 2 digit hex value
     * representation of c to the given string buffer
     */
    private static void appendColorPart(int c, StringBuilder sb)
    {
        String s = Integer.toHexString(c);
        if (s.length() == 1)
        {
            sb.append('0');
        }
        sb.append(s);
    }

    /**
     * Encode string for csv output
     */
    public static String encodeCSV(String s)
    {
        if (isHTMLString(s))
        {
            s = replace(s, "<[^>]*>", "");
            s = replace(s, "&nbsp;", " ");
        }
        int nComma = s.indexOf(',');
        int nQuote = s.indexOf('\"');
        if (nComma == -1 &&
            nQuote == -1) return s;

        if (nQuote == -1) return '"' + s + '"';

        return replace(s, "\"", "\"\"");
    }

    /**
     * Encode string for output in HTML
     */
    public static String encodeHTML(String original)
    {
        return encodeHTML(original, true);
    }

    /**
     * Encode string for output in HTML, encoding white spaces and slashes
     */
    public static String encodeHTMLWhitespace(String original)
    {
        return encodeHTML(original, true, true);
    }

    /**
     * Encode string for output in HTML (if bSlash is set,
     * also encode / (so it is ignored when / escape sequence is decoded)
     */
    public static String encodeHTML(String original, boolean bSlash)
    {
        return encodeHTML(original, bSlash, false);
    }

    /**
     * Encode string for output in HTML (if bSlash is set,
     * also encode / (so it is ignored when / escape sequence is decoded).
     * If bWhiteSpace is set, also encode spaces and tabs
     */
    public static String encodeHTML(String original, boolean bSlash, boolean bWhiteSpace)
    {
        return encodeML(original, bSlash, bWhiteSpace, true, false, false);
    }

    /**
     * Encode string for output in XML attribute or plain-text
     */
    public static String encodeXML(String original)
    {
        return encodeML(original, false, false, false, true, false);
    }

    /**
     * Encode string for output in javascript attribute or plain-text
     * NOTE: does not deal with return
     */
    public static String encodeJavascript(String original)
    {
        return encodeML(original, false, false, false, true, true);
    }

    /**
     * Encode for XML or HTML
     */
    private static String encodeML(String original, boolean bSlash, boolean bWhiteSpace, boolean bReturn, boolean bDoubleQuote, boolean bSingleQuote)
    {
        if (original == null) return null;

        StringBuilder sb = new StringBuilder();
        char c;

        char ampersand = '&';
        String ampReplacement = "&amp;";

        char lessthan = '<';
        String lessReplacement = "&lt;";

        char greaterthan = '>';
        String greatReplacement = "&gt;";

        char slash = '/'; // so ignores slash in chat escape sequenece
        String slashReplacement = "&#47;";

        char ret = '\n';
        String retReplacement = "<BR>";

        char space = ' ';
        String spaceReplacement = "&nbsp;";

        char tab = '\t';
        String tabReplacement = "&nbsp;&nbsp;&nbsp;&nbsp;";

        char quote = '"';
        String quoteReplacement = "&quot;";

        char squote = '\'';
        String squoteReplacement = "&acute;";

        for (int i = 0; i < original.length(); i++)
        {
            c = original.charAt(i);

            if (c == ampersand)
            {
                sb.append(ampReplacement);
            }
            else if (c == lessthan)
            {
                sb.append(lessReplacement);
            }
            else if (c == greaterthan)
            {
                sb.append(greatReplacement);
            }
            else if (c == ret && bReturn)
            {
                sb.append(retReplacement);
            }
            else if (c == slash && bSlash)
            {
                sb.append(slashReplacement);
            }
            else if (c == space && bWhiteSpace)
            {
                sb.append(spaceReplacement);
            }
            else if (c == tab && bWhiteSpace)
            {
                sb.append(tabReplacement);
            }
            else if (c == quote && bDoubleQuote)
            {
                sb.append(quoteReplacement);
            }
            else if (c == squote && bSingleQuote)
            {
                sb.append(squoteReplacement);
            }
            else
            {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Convert String[] to a comma separated list (delim = ", ")
     */
    public static String toString(String[] list)
    {
        return toString(list, ", ");
    }

    /**
     * Convert String[] to a [given delim] separated list
     */
    public static String toString(String[] list, String sDelim)
    {
        if (list == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; i++)
        {
            if (i > 0) sb.append(sDelim);
            sb.append(list[i]);
        }
        return sb.toString();
    }

    /**
     * Convert ArrayList to a comma separated list
     */
    public static String toString(List<?> list)
    {
        return toString(list, 0);
    }

    /**
     * Convert ArrayList to a comma separated list, starting at given index
     */
    public static String toString(List<?> list, int nStartIndex)
    {
        if (list == null) return "";
        StringBuilder sb = new StringBuilder();
        int nNum = list.size();
        for (int i = nStartIndex; i < nNum; i++)
        {
            sb.append(list.get(i).toString());
            if (i < (nNum - 1)) sb.append(", ");
        }
        return sb.toString();
    }

    public static String joinWithDelimiter(String delimiter, String... strings) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String str : strings) {
            if (str != null && !str.isEmpty()) {
                if (!first) {
                    result.append(delimiter);
                } else {
                    first = false;
                }
                result.append(str);
            }
        }

        return result.toString();
    }
    /**
     * Convert byte to a hex string of the form 0xXX
     */
    public static String toHexString(byte b)
    {
        return "0x" + Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3);
    }

    /**
     * Is the string considered to have no data (null or zero length)
     */
    public static boolean isEmpty(final CharSequence string)
    {
        return string == null || string.length() == 0 || string.toString().trim().length() == 0;
    }

    /**
     * Convienence function for sleeping - any exceptions are caught/ignored
     */
    public static void sleepMillis(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException ignored)
        {
            Thread.interrupted();
        }
    }

    /**
     * Convienence function for sleeping - any exceptions are caught/ignored.
     * Argument 'seconds' is a double to allow passing of fractional seconds like ".5"
     */
    public static void sleepSeconds(double seconds)
    {
        sleepMillis((long) (seconds * 1000));
    }

    /**
     * Wait until notified or timeout.   While loop is used due to
     * comments in javadoc for Object.wait().
     */
    public static void wait(WaitBoolean wait, long timeoutMillis)
    {
        synchronized (wait.obj)
        {
            long start = System.currentTimeMillis();
            long elapsed = 0;
            while (!wait.isDone())
            {
                try
                {
                    wait.object().wait(timeoutMillis - elapsed);
                }
                catch (InterruptedException ignore)
                {

                }
                // if here but not done, see if we sleep long enough
                // if we did exit, otherwise loop back around and sleep
                // what's left.
                if (!wait.isDone())
                {
                    elapsed = System.currentTimeMillis() - start;
                    if (elapsed >= timeoutMillis)
                    {
                        wait.done();
                    }
                }
            }
        }
    }

    /**
     * Used with wait.  When wait condition is done, call done on this object.
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class WaitBoolean
    {
        final Object obj;
        boolean done = false;

        public WaitBoolean(Object obj)
        {
            this.obj = obj;
        }

        public Object object()
        {
            return obj;
        }

        public boolean isDone()
        {
            return done;
        }

        public void done()
        {
            synchronized (obj)
            {
                if (!done)
                {
                    done = true;
                    obj.notifyAll();
                }
            }
        }
    }

    /**
     * Return given byte buffer as a string, up to limit chars
     */
    public static String getBufferAsString(ByteBuffer buffer, int limit)
    {
        String input = decodeBasic(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.position());
        return getPrintableString(input, limit);
    }

    /**
     * return printable version of string - removing any non-ascii
     */
    public static String getPrintableString(String input, int limit)
    {
        // avoid printing non-ascii to log
        char c;
        int n;
        StringBuilder request = new StringBuilder();
        for (int i = 0; i < input.length() && i < limit; i++)
        {
            c = input.charAt(i);
            n = (int) c;
            // if non-ascii, change to a tilde
            if (n == 127 || n < 32 || n > 168) c = '~';
            // change carriage return/line feeds
            if (n == '\r') request.append("[Cr]");
            else if (n == '\n') request.append("[Lf]");
            else request.append(c);
        }
        if (input.length() > limit)
        {
            request.append(" ... [truncated]");
        }

        return request.toString();
    }

    /**
     * convert string to boolean.  returns null if not a valid value (-/0/false/no, +/1/true/yes)
     */
    public static Boolean parseBoolean(String sValue)
    {
        if (sValue == null) return null;

        Boolean value = null;
        if (sValue.length() == 1)
        {
            if (sValue.charAt(0) == '0') value = Boolean.FALSE;
            if (sValue.charAt(0) == '-') value = Boolean.FALSE;
            if (sValue.charAt(0) == '1') value = Boolean.TRUE;
            if (sValue.charAt(0) == '+') value = Boolean.TRUE;
        }
        else
        {
            if (sValue.equalsIgnoreCase("false")) value = Boolean.FALSE;
            if (sValue.equalsIgnoreCase("true")) value = Boolean.TRUE;
            if (sValue.equalsIgnoreCase("no")) value = Boolean.FALSE;
            if (sValue.equalsIgnoreCase("yes")) value = Boolean.TRUE;
        }

        return value;
    }

    /**
     * convert string to boolean, returns bDefault if not a valid value or sValue is null
     */
    public static boolean parseBoolean(String sValue, boolean bDefault)
    {
        Boolean b = parseBoolean(sValue);
        if (b == null) return bDefault;

        return b;
    }

    /**
     * Return sorted array of Files with given extension in the given dir
     * Pass in the "." if desired (this method doesn't add it)
     */
    public static File[] getFileList(File fDir, String sExt, String sBeginsWith)
    {
        UtilFileFilter filter = new UtilFileFilter(sExt, sBeginsWith);
        File list[] = fDir.listFiles(filter);
        Arrays.sort(list);
        return list;
    }

    public static FilenameFilter getFilenameFilter(String sExt, String sBeginsWith)
    {
        return new UtilFileFilter(sExt, sBeginsWith);
    }

    /**
     * parse a string to double, using the multiplier to get desired
     * decimal points (i.e., pass in 1000 to get 3 decimal points).
     */
    public static double parseStringToDouble(String s, int multiplier)
    {
        if (multiplier <= 0) multiplier = 1;
        long d;

        if (s == null || s.length() == 0) return 0;

        try
        {
            StringBuilder sb = new StringBuilder();
            char c;
            for (int j = 0; j < s.length(); j++)
            {
                c = s.charAt(j);
                if (c >= '0' && c <= '9' || c == '-' || c == '.') sb.append(c);
            }
            s = sb.toString();
            d = (long) (Double.parseDouble(s) * multiplier);
        }
        catch (NumberFormatException ignored)
        {
            d = 0;
        }

        return ((double) d) / (double) multiplier;
    }

    /**
     * JDK1.5 has different HTML spacing than 1.4, so correct it
     */
    public static String fixHtmlTextFor15(String s)
    {
        if (IS14) return s;
        if (!isHTMLString(s)) return s;

        s = replace(s, "<BR>\\s*<TABLE", "<BR><BR><TABLE");
        s = replace(s, "<BR>\\s*<CENTER", "<BR><BR><CENTER");
        s = replace(s, "<BR>\\s*<UL", "<BR><BR><UL");
        s = replace(s, "<BR>\\s<OL", "<BR><BR><OL");

        return s;
    }

    /**
     * is this an html string?
     */
    public static boolean isHTMLString(String s)
    {
        if (s != null)
        {
            if ((s.length() >= 6) && (s.charAt(0) == '<') && (s.charAt(5) == '>'))
            {
                if ((s.charAt(1) == 'h' || s.charAt(1) == 'H') &&
                    (s.charAt(2) == 't' || s.charAt(2) == 'T') &&
                    (s.charAt(3) == 'm' || s.charAt(3) == 'M') &&
                    (s.charAt(4) == 'l' || s.charAt(4) == 'L'))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Filter by extension
     */
    private static class UtilFileFilter implements FilenameFilter
    {
        String sExt;
        String sBeginsWith = null;

        private UtilFileFilter(String sExt, String sBeginsWith)
        {
            if (sExt != null) this.sExt = sExt.toLowerCase();
            if (sBeginsWith != null) this.sBeginsWith = sBeginsWith.toLowerCase();
        }

        public boolean accept(File dir, String name)
        {
            String sName = name.toLowerCase();

            boolean bExt = true;
            if (sExt != null)
            {
                bExt = sName.endsWith(sExt);
            }

            boolean bBegins = true;
            if (sBeginsWith != null)
            {
                bBegins = sName.startsWith(sBeginsWith);
            }
            return bExt && bBegins;
        }
    }

    ////
    //// Time
    ////

    private static long nSeq_ = 0;

    /**
     * Store current time in (millis * 1000) + a sequence number 000-999
     */
    public static long getCurrentTimeStamp()
    {
        return (System.currentTimeMillis() * 1000) + getSequenceNum();
    }

    /**
     * Used to return a looping num from 0-999 to prevent
     * the rare case that multiple messages created
     * during same millisecond and have the same id
     */
    private static synchronized long getSequenceNum()
    {
        nSeq_++;
        if (nSeq_ > 999) nSeq_ = 0;
        return nSeq_;
    }

    /**
     * Return time in millis from time stamp (basically / 1000)
     */
    public static long getMillisFromTimeStamp(long l)
    {
        return (l / 1000);
    }

    /**
     * Get Date object from time stamp
     */
    public static Date getDateFromTimeStamp(long l)
    {
        return new Date(getMillisFromTimeStamp(l));
    }

    // formatting
    private static Format fNum2_ = new Format("%02d");
    private static Format fNum3_ = new Format("%03d");

    /**
     * Get string representing time elapsed as Xd 00:00:00.000
     */
    public static String getTimeString(long millis, boolean bPrintMillis)
    {
        long SECOND = 1000;
        long MINUTE = SECOND * 60;
        long HOUR = MINUTE * 60;
        long DAY = HOUR * 24;

        long nDays = millis / DAY;
        millis %= DAY;
        long nHours = millis / HOUR;
        millis %= HOUR;
        long nMinutes = millis / MINUTE;
        millis %= MINUTE;
        long nSeconds = millis / SECOND;
        millis %= SECOND;

        StringBuilder sb = new StringBuilder();
        if (nDays > 0) sb.append(nDays).append("d ");
        sb.append(fNum2_.form(nHours)).append(':').append(fNum2_.form(nMinutes)).append(':')
                .append(fNum2_.form(nSeconds));
        if (bPrintMillis) sb.append('.').append(fNum3_.form(millis));
        return sb.toString();
    }

    /**
     * get bytes as a string
     */
    public static String formatSizeBytes(long bytes)
    {
        NumberFormat format = NumberFormat.getInstance();

        format.setMinimumIntegerDigits(1);
        format.setGroupingUsed(true);

        if (bytes < 1024L)
        {
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(0);
            return format.format(bytes) + " B";
        }
        else if (bytes < 1024L * 10L)
        {
            format.setMinimumFractionDigits(1);
            format.setMaximumFractionDigits(1);
            return format.format(bytes / 1024.0) + " KB";
        }
        else if (bytes < 1024L * 1024L)
        {
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(0);
            return format.format(bytes / 1024L) + " KB";
        }
        else if (bytes < 1024L * 1024L * 10L)
        {
            format.setMinimumFractionDigits(1);
            format.setMaximumFractionDigits(1);
            return format.format((bytes / 1024.0) / 1024.0) + " MB";
        }
        else if (bytes < 1024L * 1024L * 1024L)
        {
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(0);
            return format.format(bytes / (1024L * 1024L)) + " MB";
        }
        else if (bytes < 1024L * 1024L * 1024L * 10L)
        {
            format.setMinimumFractionDigits(1);
            format.setMaximumFractionDigits(1);
            return format.format(((bytes / 1024.0) / 1024.0) / 1024.0) + " GB";
        }
        else // GB if (bytes < 1024L * 1024L * 1024L * 1024L)
        {
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(0);
            return format.format(bytes / (1024L * 1024L * 1024L)) + " GB";
        }
    }

    /**
     * replace given string
     */
    public static String replace(String sSrc, String sPattern, String sReplace)
    {
        Pattern pattern = Pattern.compile(sPattern);
        Matcher matcher = pattern.matcher(sSrc);
        return matcher.replaceAll(sReplace);
    }

    /**
     * Return new encoder for our charset
     */
    public static CharsetEncoder newEncoder()
    {
        CharsetEncoder encoder = CHARSET.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);//REPLACE
        return encoder;
    }

    /**
     * Return new decoder for our charset
     */
    public static CharsetDecoder newDecoder()
    {
        CharsetDecoder decoder = CHARSET.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder;
    }

    /**
     * Encode string in our charset
     */
    public static byte[] encode(String str)
    {
        ByteBuffer bb = CHARSET.encode(str);
        byte[] bytes = new byte[bb.remaining()];
        System.arraycopy(bb.array(), bb.position(), bytes, 0, bb.remaining());
        return bytes;
    }

    /**
     * Decode bytes back to string
     */
    public static String decode(byte[] bytes)
    {
        return decode(bytes, 0, bytes.length);
    }

    /**
     * Decode bytes back to string
     */
    public static String decode(byte[] bytes, int offset, int length)
    {
        if (bytes == null || length == 0) return "";
        ByteBuffer bb = ByteBuffer.wrap(bytes, offset, length);
        CharBuffer cb = CHARSET.decode(bb);
        return cb.toString();
    }

    /**
     * Encode string in our charset
     */
    public static byte[] encodeBasic(String str)
    {
        ByteBuffer bb = CHARSET_BASIC.encode(str);
        byte[] bytes = new byte[bb.remaining()];
        System.arraycopy(bb.array(), bb.position(), bytes, 0, bb.remaining());
        return bytes;
    }

    /**
     * Decode bytes back to string
     */
    public static String decodeBasic(byte[] bytes)
    {
        return decode(bytes, 0, bytes.length);
    }

    /**
     * Decode bytes back to string
     */
    public static String decodeBasic(byte[] bytes, int offset, int length)
    {
        if (bytes == null || length == 0) return "";
        ByteBuffer bb = ByteBuffer.wrap(bytes, offset, length);
        CharBuffer cb = CHARSET_BASIC.decode(bb);
        return cb.toString();
    }

    /**
     * open a url in an external browser
     */
    @SuppressWarnings({"CallToRuntimeExecWithNonConstantString"})
    public static void openURL(String sURL)
    {
        try
        {
            if (ISMAC)
            {
                Runtime.getRuntime().exec("/usr/bin/open " + sURL);
            }
            else if (ISWIN9X || ISWINNT)
            {
                String sCmd = (ISWIN9X ? "command.com" : "cmd.exe");

                Runtime.getRuntime().exec(new String[]
                        {
                                sCmd,
                                "/c",
                                "start",
                                "\"\"",
                                '"' + sURL + '"'
                        });

            }
            else if (ISLINUX)
            {
                Runtime.getRuntime().exec("/usr/bin/gnome-open " + sURL);
            }
        }
        catch (Exception ie)
        {
            System.err.println("openURL error: " + formatExceptionText(ie));
        }
    }
//    
//    public static void main(String args[])
//    {
//        openURL(args[0]);
//    }

    /**
     * Do a reverse DNS lookup.  If unable to determine a host name, null is returned.
     */
    public static String getHostForIP(String IP)
    {
        String name = null;

        if (ip2host_ != null)
        {
            name = (String) ip2host_.get(IP);
        }
        else
        {
            ip2host_ = new Properties();
        }

        if (name == null)
        {
            try
            {
                InetAddress inet = InetAddress.getByName(IP);
                name = inet.getCanonicalHostName();
            }
            catch (UnknownHostException ignored)
            {
                name = IP;
            }
        }

        if (ip2host_ != null)
        {
            ip2host_.put(IP, name);
            bUpdated_ = true;
        }

        // if host is same as IP, return null indicating no name was found
        if (name.equals(IP)) return null;
        return name;
    }

    /**
     *
     */

    // cache
    private static Properties ip2host_ = null;
    private static boolean bUpdated_ = false;

    /**
     * Load cache
     */
    public static void loadIPCache(File file)
    {
        ip2host_ = new Properties();
        if (!file.exists()) return;
        try
        {
            FileInputStream fis = new FileInputStream(file);
            ip2host_.load(fis);
            fis.close();
            bUpdated_ = false;
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }
    }

    /**
     * save cache
     */
    public static void saveIPCache(File file)
    {
        if (ip2host_ == null || !bUpdated_) return;

        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            ip2host_.store(fos, " Cached IP to hostname from Utils");
            fos.close();
            bUpdated_ = false;
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }
    }

    /**
     * debug
     */
    public static String getIPAddress(SocketChannel channel)
    {
        return (channel == null || channel.socket() == null || channel.socket().getInetAddress() == null)
               ? "[unknown]" : channel.socket().getInetAddress().getHostAddress();
    }

    /**
     * debug
     */
    public static String getLocalAddress(DatagramChannel channel)
    {
        return (channel == null || channel.socket() == null || channel.socket().getLocalAddress() == null)
               ? "[unknown]" : channel.socket().getLocalAddress().getHostAddress();
    }

    /**
     * debug
     */
    public static String getLocalPort(DatagramChannel channel)
    {
        return (channel == null || channel.socket() == null)
               ? "[unknown]" : "" + channel.socket().getLocalPort();
    }

    /**
     * debug
     */
    public static String getLocalAddressPort(DatagramChannel channel)
    {
        return getLocalAddress(channel) + ':' + getLocalPort(channel);
    }

    /**
     * debug
     */
    public static String getLocalAddress(ServerSocketChannel channel)
    {
        return (channel == null || channel.socket() == null || channel.socket().getInetAddress() == null)
               ? "[unknown]" : channel.socket().getInetAddress().getHostAddress();
    }

    /**
     * debug
     */
    public static String getLocalPort(ServerSocketChannel channel)
    {
        return (channel == null || channel.socket() == null)
               ? "[unknown]" : "" + channel.socket().getLocalPort();
    }

    /**
     * debug
     */
    public static String getLocalAddressPort(ServerSocketChannel channel)
    {
        return getLocalAddress(channel) + ':' + getLocalPort(channel);
    }

    /**
     * debug
     */
    public static String getAddress(InetSocketAddress socket)
    {
        return (socket == null || socket.getAddress() == null)
               ? "[unknown]" : socket.getAddress().getHostAddress();
    }

    /**
     * debug
     */
    public static String getPort(InetSocketAddress socket)
    {
        return (socket == null)
               ? "[unknown]" : "" + socket.getPort();
    }

    /**
     * debug
     */
    public static String getAddressPort(InetSocketAddress socket)
    {
        return getAddress(socket) + ':' + getPort(socket);
    }

    /**
     * Get current year
     */
    public static String getDateYear()
    {
        Calendar now = Calendar.getInstance();
        return Integer.toString(now.get(Calendar.YEAR));
    }

    /**
     * Change hours/minutes/seconds to 23:59:59 so this date represents the end of the day
     */
    public static Date getDateEndOfDay(Date end)
    {
        if (end == null) return null;

        // adjust time to end of day
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(end);

        // get just date (ignoring any time value)
        c = new GregorianCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // add a day and subtrack one second
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.SECOND, -1);

        return c.getTime();
    }

    /**
     * Get Date with +/- delta from today (in days)
     */
    public static Date getDateDays(int nDays)
    {
        return getDateDays(new Date(), nDays);
    }

    /**
     * Get Date with +/- delta from given date (in days)
     */
    public static Date getDateDays(Date date, int nDays)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, nDays);
        return c.getTime();
    }

    /**
     * Zero out time to 0:00:00
     */
    public static Date getDateZeroTime(Date date)
    {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * return true if both Dates are the dame day (ignore hours/mins/secs)
     */
    public static boolean isSameDay(Date one, Date two)
    {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();

        c1.setTime(one);
        c2.setTime(two);

        return (
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
        );
    }

    public static SimpleDateFormat getRFC822()
    {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    }
}
