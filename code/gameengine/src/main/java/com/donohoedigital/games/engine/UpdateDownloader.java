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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import org.apache.log4j.*;

import java.io.*;
import java.net.*;

/**
 * Performs auto update tasks.
 */
public class UpdateDownloader
{
    static Logger logger = Logger.getLogger(UpdateDownloader.class);

    private static final String UPDATE_DIR_NAME = "update";
    private static final String DOWNLOAD_DIR_NAME = "download";

    private static final String OS_LINUX = "linux";
    private static final String OS_MAC = "mac";
    private static final String OS_WINDOWS = "win";

    private static final int HTTP_STATUS_OK = 200;

    private static final File updateDir_;
    private static final File downloadDir_;

    static
    {
        // Initialize the top-level directories used for download/install.
        File userDir = ConfigManager.getUserHome();

        updateDir_ = new File(userDir, UPDATE_DIR_NAME);
        downloadDir_ = new File(updateDir_, DOWNLOAD_DIR_NAME);
    }

    private Listener listener_ = null;

    /**
     * Get the download file associated with the given release.
     *
     * @param release release
     * @return the download file
     */
    public static File getDownloadFile(PatchRelease release)
    {
        // Format the target file path.
        URL url = release.getURL();
        String path = url.getPath();
        File file = null;

        int index = path.lastIndexOf('/');

        if (index >= 0)
        {
            path = path.substring(index + 1);
        }

        file = new File(downloadDir_, path);

        return file;
    }

    /**
     * Create an instance that reports its progress via the given listener.
     *
     * @param listener optional request listener
     */
    public UpdateDownloader(Listener listener)
    {
        listener_ = listener;
    }

    /**
     * Check if there is an update available for the current version.
     *
     * @param context
     * @param display display server connection dialog
     * @return the engine message containing release information, or <code>null</code> if the request failed
     */
    public EngineMessage checkUpdate(GameContext context, boolean display)
    {
        // Check for a patch to the current version.
        TypedHashMap params = new TypedHashMap();
        params.setString(EngineMessage.PARAM_PATCH_OS, getOS());

        if (!display)
        {
            params.setBoolean(SendMessageDialog.PARAM_FACELESS, Boolean.TRUE);
            params.setBoolean(SendMessageDialog.PARAM_FACELESS_ERROR, Boolean.FALSE);
        }

        UpdateCheck dialog = (UpdateCheck) context.processPhaseNow("UpdateCheck", params);
        return (dialog.getStatus() == DDMessageListener.STATUS_OK) ? dialog.getReturnMessage() : null;
    }

    /**
     * Download the given update release.
     *
     * @param release update release
     */
    public void downloadUpdate(PatchRelease release)
    {
        // Download the file.
        logger.info("Downloading update: " + release.getVersion());
        URL url = release.getURL();
        File file = getDownloadFile(release);
        FileOutputStream stream = null;

        ConfigUtils.verifyNewDirectory(downloadDir_);

        try
        {
            stream = new FileOutputStream(file);
            Client client = doRequest(url, stream, null);
            int statusCode = client.getStatusCode();

            // Handle error status.
            if (statusCode != HTTP_STATUS_OK)
            {
                String message = PropertyConfig.getMessage("msg.update.requestError", statusCode);
                throw new ApplicationError(ErrorCodes.ERROR_BAD_RESPONSE, message, null);
            }
        }
        catch (IOException e)
        {
            // TODO: change to partial download - requires differentiating between server error and file error
            // Delete invalid file.
            if (file.exists())
            {
                // Stream must be closed first to avoid sharing problems.
                try
                {
                    if (stream != null) stream.close();
                }
                catch (IOException ignored)
                {
                }
                stream = null;

                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }

            throw new ApplicationError(e);
        }
        finally
        {
            try
            {
                if (stream != null) stream.close();
            }
            catch (IOException ignored)
            {
            }
        }
    }

    /**
     * Get the client OS.
     */
    private String getOS()
    {
        if (Utils.ISLINUX)
        {
            return OS_LINUX;
        }
        else if (Utils.ISMAC)
        {
            return OS_MAC;
        }
        else if (Utils.ISWINDOWS)
        {
            return OS_WINDOWS;
        }
        else
        {
            return null;
        }
    }

    /**
     * Make a request for the given URL.  Response data is written to the given stream.
     *
     * @param url     url
     * @param options options
     * @return the client used to make the request
     */
    private Client doRequest(URL url, OutputStream stream, DDHttpClient.HttpOptions options)
    {
        // Use a separate thread if the requests are being tracked by a listener.
        Client client = null;

        if (listener_ != null)
        {
            Thread thread = new Thread(client = new Client(url, stream, options), "UpdateDownloader");
            thread.start();

            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                throw new ApplicationError(e);
            }
        }
        else
        {
            client = new Client(url, stream, options);
            client.run();
        }

        // Throw an error if one occurred.
        Throwable t = client.getError();

        if (t != null)
        {
            throw new ApplicationError(t);
        }

        return client;
    }

    /**
     * Reports download progress.
     */
    static interface Listener
    {
        /**
         * Called to report the amount of resposne data read.
         *
         * @param percent percent read
         */
        public void setPercentDone(int percent);
    }

    /**
     * Makes requests to the server.
     */
    private class Client implements Runnable
    {
        private DDHttpClient client_ = null;
        private OutputStream stream_ = null;
        private Throwable error_ = null;

        /**
         * Create a client with the given options.  Response data is written to the given stream.
         *
         * @param url     url
         * @param stream  stream
         * @param options options
         */
        public Client(URL url, OutputStream stream, DDHttpClient.HttpOptions options)
        {
            try
            {
                logger.info("Requesting update URL: " + url);
                client_ = new DDHttpClient(url, null, options);
                stream_ = stream;
            }
            catch (Exception e)
            {
                throw new ApplicationError(e);
            }
        }

        /**
         * Connect to the server and receive the response.
         */
        public void run()
        {
            try
            {
                // Connect and write the request.
                client_.connect();
                client_.write(null, null, DDMessenger.getUSERAGENT());

                // Read the headers and check the status.
                client_.startRead();

                if (client_.getResponseCode() != HTTP_STATUS_OK)
                {
                    // Force full read.
                    setPercentRead(1, 1);
                    return;
                }

                // Read the response data and write to the given output stream.
                InputStream is = client_.getInputStream();
                byte[] bytes = new byte[1024];
                int contentLength = client_.getContentLength();
                int bytesRemaining = contentLength;
                int bytesRead = 0;
                int bytesTotal = 0;

                if (bytesRemaining > 0)
                {
                    // Content length header was received, so use it to control the amount
                    // of data read.
                    while (bytesRemaining > 0)
                    {
                        if ((bytesRead = is.read(bytes)) != -1)
                        {
                            stream_.write(bytes, 0, bytesRead);
                            bytesRemaining -= bytesRead;
                            bytesTotal += bytesRead;
                            setPercentRead(bytesTotal, contentLength);
                        }
                    }
                }
                else
                {
                    // No content length header, so read until the stream is done.
                    while ((bytesRead = is.read(bytes)) != -1)
                    {
                        stream_.write(bytes, 0, bytesRead);
                        bytesTotal += bytesRead;
                        setPercentRead(bytesTotal, contentLength);
                    }
                }
            }
            catch (Throwable t)
            {
                error_ = t;
            }
            finally
            {
                try
                {
                    client_.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }

        /**
         * Set percent read.
         *
         * @param read  bytes read
         * @param total total number of bytes
         */
        public void setPercentRead(int read, int total)
        {
            if (listener_ == null)
            {
                return;
            }

            int percent = (int) ((((double) read) / ((double) total)) * 100);
            listener_.setPercentDone(percent);
        }

        /**
         * Get any error that occurred during the request.
         *
         * @return the error, or <code>null</code> if the request was successful
         */
        public Throwable getError()
        {
            return error_;
        }

        /**
         * Get the status code.
         *
         * @return status code
         */
        public int getStatusCode()
        {
            return client_.getResponseCode();
        }

        /**
         * Get the content type.
         *
         * @return content type
         */
        public String getContentType()
        {
            return client_.getContentType();
        }

        /**
         * Get the content length.
         *
         * @return content length
         */
        public int getContentLength()
        {
            return client_.getContentLength();
        }
    }
}
