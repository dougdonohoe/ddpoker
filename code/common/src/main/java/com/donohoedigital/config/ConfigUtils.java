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
 * ConfigUtils.java
 *
 * Created on February 10, 2003, 9:16 AM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.StringTokenizer;

/**
 * @author Doug Donohoe
 */
@SuppressWarnings({"ChannelOpenedButNotSafelyClosed", "RawUseOfParameterizedType", "resource"})
public class ConfigUtils
{
    static Logger logger = LogManager.getLogger(ConfigUtils.class);

    // allow resetting logger (needed since this is created before LoggingConfig runs)
    static void resetLogger() {
        logger = LogManager.getLogger(ConfigUtils.class);
    }

    ///
    /// Convenience functions for loading classes
    ///

    /**
     * Get a class given a class name
     */
    public static Class<?> getClass(String sClass)
    {
        return getClass(sClass, true);
    }

    /**
     * Get a class given a class name.  Throw an exception if bThrowExceptionOnError true and
     * class not found.  Otherwise return null and log a warning message.
     */
    public static Class<?> getClass(String sClass, boolean throwExceptionOnError)
    {
        try
        {
            return Class.forName(sClass);
        }
        catch (ClassNotFoundException cne)
        {
            String sMsg = "Class " + sClass + " was not found";
            if (throwExceptionOnError)
            {
                throw new ApplicationError(sMsg, cne);
            }
            else
            {
                logger.warn(sMsg);
            }
        }

        return null;
    }

    /**
     * Create new instance of given class using default constructor
     */
    public static <T> T newInstance(Class<T> cClass)
    {
        try
        {
            return cClass.getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException ie)
        {
            throw new ApplicationError(ie);
        }
    }

    /**
     * Create new instance of given class using a particular constructor
     */
    public static Object newInstanceGeneric(Class<?> cClass, Class<?>[] signature, Object[] params)
    {
        try
        {
            return cClass.getConstructor(signature).newInstance(params);
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ie)
        {
            throw new ApplicationError(ie);
        }
    }

    /**
     * Create new instance of given class using a particular constructor
     */
    public static <T> T newInstance(Class<T> cClass, Class<T>[] signature, Object[] params)
    {
        try
        {
            return cClass.getConstructor(signature).newInstance(params);
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ie)
        {
            throw new ApplicationError(ie);
        }
    }

    /**
     * Create new instance of given class using a particular constructor
     */
    public static <T> T newInstance(Constructor<T> constructor)
    {
        return newInstance(constructor, null);
    }

    /**
     * Create new instance of given class using a particular constructor
     */
    public static <T> T newInstance(Constructor<T> constructor, Object[] params)
    {
        try
        {
            return constructor.newInstance(params);
        }
        catch (InvocationTargetException | IllegalAccessException | InstantiationException ie)
        {
            throw new ApplicationError(ie);
        }
    }

    /**
     * Verify directory exists, if not create it
     */
    public static void verifyNewDirectory(File dir)
    {
        if (!dir.exists())
        {
            if (isNotTestPath(dir.getAbsolutePath())) {
                logger.info("Creating directory: {}", dir.getAbsolutePath());
            }
            if (!dir.mkdirs())
            {
                //logger.error("Unable to create dir " + dir.getAbsolutePath());
                throw new ApplicationError(ErrorCodes.ERROR_CREATE, "Unable to create directory",
                                           dir.getAbsolutePath(), "Check permissions on parent directories");
            }
        }

        if (!dir.isDirectory())
        {
            //logger.error("Path is not a directory: " + dir.getAbsolutePath());
            throw new ApplicationError(ErrorCodes.ERROR_CREATE, "Path should be a directory",
                                       dir.getAbsolutePath(), "Rename existing item");
        }
    }

    /**
     * Create empty file, if one exists, that is an error
     */
    public static void verifyNewFile(File file)
    {
        if (file.exists())
        {
            //logger.error("File already exists: " + file.getAbsolutePath());
            throw new ApplicationError(ErrorCodes.ERROR_CREATE, "File should not exist",
                                       file.getAbsolutePath(), "Synchronization problem?");
        }

        try
        {
            if (!file.createNewFile())
            {
                //logger.error("Unable to create file: " + file.getAbsolutePath());
                throw new ApplicationError(ErrorCodes.ERROR_CREATE, "Unable to create file",
                                           file.getAbsolutePath(), "Check permissions");
            }
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ErrorCodes.ERROR_CREATE, ioe);
        }
    }

    /**
     * Verify dir exists
     */
    public static void verifyDirectory(File dir)
    {
        if (!dir.exists() || !dir.canRead() || !dir.isDirectory())
        {
            //logger.error("Directory doesn't exist or can't be read: " + dir.getAbsolutePath());
            throw new ApplicationError(ErrorCodes.ERROR_FILE_NOT_FOUND, "Directory should exist and be readable",
                                       dir.getAbsolutePath(), "Check permissions");
        }
    }

    /**
     * Verify file exists
     */
    public static void verifyFile(File file)
    {
        if (!file.exists() || !file.canRead() && !file.isFile())
        {
            //logger.error("File doesn't exist or can't be read: " + file.getAbsolutePath());
            throw new ApplicationError(ErrorCodes.ERROR_FILE_NOT_FOUND, "File should exist and be readable",
                                       file.getAbsolutePath(), "Check permissions");
        }
    }

    /**
     * Get a Writer for the given file, throws ApplicationError if problem
     */
    public static Writer getWriter(File file)
    {
        FileOutputStream fos;
        try
        {
            fos = new FileOutputStream(file);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ApplicationError(fnfe);
        }

        FileChannel out = fos.getChannel();
        CharsetEncoder encoder = Utils.newEncoder();

        return Channels.newWriter(out, encoder, -1);
    }

    /**
     * Get reader for given file
     */
    public static Reader getReader(File file)
    {
        FileInputStream fis = getFileInputStream(file);

        FileChannel in = fis.getChannel();
        CharsetDecoder decoder = Utils.newDecoder();

        return Channels.newReader(in, decoder, -1);
    }

    /**
     * Get reader for given url
     */
    public static Reader getReader(URL url)
    {
        InputStream fis;
        try
        {
            fis = url.openStream();
        }
        catch (IOException fnfe)
        {
            throw new ApplicationError(fnfe);
        }

        return new InputStreamReader(fis);
    }

    /**
     * Get an OutputStream for the given file, throws ApplicationError if problem
     */
    public static FileOutputStream getFileOutputStream(File file, boolean bAppend)
    {
        FileOutputStream fos;
        try
        {
            fos = new FileOutputStream(file, bAppend);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ApplicationError(fnfe);
        }

        return fos;
    }

    /**
     * Get an InputStream for the given file, throws ApplicationError if problem
     */
    public static FileInputStream getFileInputStream(File file)
    {
        FileInputStream fis;
        try
        {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ApplicationError(fnfe);
        }

        return fis;
    }

    /**
     * close stream, throws ApplicationError if IOException occurs
     */
    public static void close(Closeable out)
    {
        if (out == null) return;

        try
        {
            out.close();
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
    }

    /**
     * Read given file and return a String.  File is read by
     * lines and each line is appended with a newline (including
     * the last line).
     */
    public static String readFile(File file)
    {
        verifyFile(file);
        Reader reader = getReader(file);
        BufferedReader sreader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String sLine;
        try
        {
            while ((sLine = sreader.readLine()) != null)
            {
                sb.append(sLine);
                sb.append('\n');
            }
            close(reader);
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
        return sb.toString();
    }

    /**
     * Read given url and return a String.  File is read by
     * lines and each line is appended with a newline (including
     * the last line).
     */
    public static String readURL(URL url)
    {
        Reader reader = getReader(url);
        BufferedReader sreader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String sLine;
        try
        {
            while ((sLine = sreader.readLine()) != null)
            {
                sb.append(sLine);
                sb.append('\n');
            }
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
        finally
        {
            close(reader);
        }
        return sb.toString();
    }

    /**
     * Write given string to file - appends new line at end if bAppendNewLine true
     */
    public static void writeFile(File file, String sData, boolean bAppendNewLine)
    {
        Writer writer = ConfigUtils.getWriter(file);
        try
        {
            writer.write(sData);
            if (bAppendNewLine) writer.write("\n");
        }
        catch (IOException ioe)
        {
            throw new ApplicationError(ioe);
        }
        ConfigUtils.close(writer);
    }

    /**
     * Get a file from a URL
     */
    public static File getFile(URL url)
    {
        try
        {
            return new File(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new ApplicationError(e);
        }
    }

    /**
     * Copy a file
     */
    public static void copyFile(File from, File to)
    {
        try
        {
            FileChannel src = new FileInputStream(from).getChannel();
            FileChannel dst = new FileOutputStream(to).getChannel();
            src.transferTo(0, src.size(), dst);
            src.close();
            dst.close();
        }
        catch (Throwable t)
        {
            logger.error("Unable to copy {} to {}{}", from.getAbsolutePath(), to.getAbsolutePath(), Utils.formatExceptionText(t));
        }
    }

    /**
     * Copy contents of URL to a file
     */
    public static void copyUrlToFile(URL from, File to)
    {
        InputStream is = null;
        FileChannel dst = null;
        try
        {
            // open stream and dest file
            is = from.openStream();
            dst = new FileOutputStream(to).getChannel();

            // use byte buffer and read directly into it
            ByteBuffer buffer = ByteBuffer.allocate(10000);
            byte[] bytes = buffer.array();
            int num;
            while ((num = is.read(bytes)) != -1)
            {
                // if no data, just wait a bit
                if (num == 0)
                {
                    Utils.sleepMillis(5);
                    continue;
                }
                buffer.limit(num); // tell buffer where we wrote to
                dst.write(buffer); // write data out
                buffer.clear();    // reset it
            }
        }
        catch (IOException e)
        {
            throw new ApplicationError(e);
        }
        finally
        {
            ConfigUtils.close(is);
            ConfigUtils.close(dst);
        }
    }

    /**
     * Copy contents of directory if dest files are missing or older
     * than source files
     */
    public static void copyDir(File dFrom, File dTo, FilenameFilter filter)
    {
        // validate args
        ApplicationError.assertTrue(dFrom.isDirectory(), "From file is not directory", dFrom);
        ApplicationError.assertTrue(!dTo.exists() || dTo.isDirectory(), "To file is not directory", dTo);
        ApplicationError.assertTrue(!dFrom.equals(dTo), "To and From are same", dFrom);

        // create dest directory if needed
        verifyNewDirectory(dTo);

        // loop through source
        File[] files = dFrom.listFiles(filter);
        File src;
        File dest;
        for (int i = 0; files != null && i < files.length; i++)
        {
            src = files[i];
            if (src.getName().toLowerCase().contains("cvs")) continue;

            // recurse if dir, copy if dest doesn't exist
            dest = new File(dTo, src.getName());
            if (src.isDirectory())
            {
                copyDir(src, dest, filter);
            }
            else if (!dest.exists())
            {
                logger.info("Copy {} to {}", src.getAbsolutePath(), dest.getAbsolutePath());
                copyFile(src, dest);
            }
            else if (dest.exists() && src.lastModified() > dest.lastModified())
            {
                logger.info("Update {} to {}", src.getAbsolutePath(), dest.getAbsolutePath());
                copyFile(src, dest);
            }
        }
    }

    /**
     * Copy contents of directory if dest files are missing or older
     * than source files
     */
    public static void copyURLs(String sResourceBase, String pattern, File dTo)
    {
        URL[] matches = new MatchingResources("classpath*:" + sResourceBase + '/' + pattern).getAllMatchesURL();
        if (matches.length == 0) return;

        // validate args
        ApplicationError.assertTrue(!dTo.exists() || dTo.isDirectory(), "To file is not directory", dTo);

        // create dest directory if needed
        verifyNewDirectory(dTo);

        // loop through source
        File dest;
        for (URL src : matches)
        {
            String srcString = src.toString();
            if (srcString.toLowerCase().contains("cvs")) continue;

            // figure out file name and ensure directories exist
            String subpath = srcString.replaceFirst(".*" + sResourceBase, "");
            dest = new File(dTo, subpath);
            verifyNewDirectory(dest.getParentFile());

            // copy over
            if (!dest.exists())
            {
                logger.info("Copy URL {} to {}", src, dest.getAbsolutePath());
                copyUrlToFile(src, dest);
            }
        }
    }

    /**
     * Delete a directory and all of its contents
     */
    public static boolean deleteDir(File dir)
    {
        File[] files = dir.listFiles();
        File file;

        for (int i = 0; files != null && i < files.length; i++)
        {
            file = files[i];

            if (file.isDirectory())
            {
                // recurse through subdirectory
                deleteDir(file);
            }
            else
            {
                // delete a file
                if (isNotTestPath(file.getAbsolutePath())) {
                    logger.info("Delete file {}", file.getAbsolutePath());
                }
                if (!file.delete())
                {
                    return false;
                }
            }
        }

        // delete the directory
        if (isNotTestPath(dir.getAbsolutePath())) {
            logger.info("Delete directory {}", dir.getAbsolutePath());
        }
        return dir.delete();
    }

    /**
     * Get local host name, return null if unknown
     */
    public static String getLocalHost(boolean bLogError)
    {
        String sLocalHost;

        try
        {
            InetAddress local = InetAddress.getLocalHost();
            sLocalHost = local.getHostName();

            // strip ".local" at end of mac host name
            if (Utils.ISMAC)
            {
                sLocalHost = sLocalHost.replaceAll("\\.local$", "");
            }
        }
        catch (UnknownHostException uhe)
        {
            StringTokenizer st = new StringTokenizer(uhe.getMessage(), ":");
            if (st.hasMoreTokens())
            {
                sLocalHost = st.nextToken();
                if (bLogError) logger.warn("Unable to determine local host name, guessing it is: {}", sLocalHost);
            }
            else
            {
                if (bLogError) logger.warn("Unable to determine local host name: {}", uhe.getMessage());
                sLocalHost = null;
            }
        }

        return sLocalHost;
    }

    /**
     * Get username
     */
    public static String getUserName()
    {
        return System.getProperties().getProperty("user.name");
    }

    // hack to skip certain log messages that hamper LoggingConfigTest
    public static final String SKIP_LOGGING_PATH = "skip-logging";
    private static boolean isNotTestPath(String path) {
       return !path.contains(SKIP_LOGGING_PATH);
    }
}
