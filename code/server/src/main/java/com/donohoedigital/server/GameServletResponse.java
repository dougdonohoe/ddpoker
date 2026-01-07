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
 * GameServletResponse.java
 *
 * Created on August 14, 2003, 3:55 PM 
 */

package com.donohoedigital.server;

import com.donohoedigital.base.Utils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 * @author  donohoe
 */
public class GameServletResponse implements HttpServletResponse
{
    static Logger logger = LogManager.getLogger(GameServletResponse.class);
    
    String sContentType_;
    SocketChannel channel_;
    OutputStream out_;
    
    /** 
     * Creates a new instance of StringServletResponse 
     */
    public GameServletResponse(SocketChannel channel)
    {
        channel_ = channel;
        out_ = Channels.newOutputStream(channel);
    }
    
    // HttpServletResponse implementation
    
    public void flushBuffer() throws IOException
    {
        throw new IOException("Unsupported");
    }
    
    public int getBufferSize()
    {
        logger.warn("GetBufferSize called");
        return 0;
    }
    
    public String getCharacterEncoding()
    {
        logger.warn("GetCharacterEncoding called");
        return null;
    }
    
    public java.util.Locale getLocale()
    {
        logger.warn("GetLocale called");
        return Locale.US;
    }
    
    public ServletOutputStream getOutputStream() throws IOException
    {
        logger.warn("GetOutputStream called");
        throw new IOException("Unsupported");
    }
    
    /**
     * calling this begins the response and then returns output stream for writing data
     */
    public OutputStream getOutputStream2() throws IOException
    {
        beginResponse(200, "OK");
        return out_;
    }
    
    /**
     * calling returns output stream for writing data (it doesn't call beginReponse(),
     * but it does configure proper blocking)
     */
    public OutputStream getOutputStream3() throws IOException
    {
        channel_.configureBlocking(true);
        return out_;
    }
    
    public PrintWriter getWriter() throws IOException
    {
        logger.warn("getWriter called");
        return null;
    }
    
    public boolean isCommitted()
    {
        logger.warn("isCommitted called");
        return false;
    }
    
    public void reset()
    {
        logger.warn("reset called");
    }
    
    public void resetBuffer()
    {
        logger.warn("resetBuffer called");
    }
    
    public void setBufferSize(int param)
    {
        logger.warn("setBufferSize called " + param);
    }
    
    public void setContentLength(int param)
    {
        logger.warn("setContentLength called " + param);
    }
    
    public void setContentType(String str)
    {
        sContentType_ = str;
    }
    
    public void setLocale(Locale locale)
    {
        logger.warn("setLocale called");
    }
    
    public void addCookie(Cookie cookie)
    {
        logger.warn("addCookie called");
    }
    
    public void addDateHeader(String str, long param)
    {
        logger.warn("addDateHeader called " + str + '=' + param);
    }
    
    public void addHeader(String str, String str1)
    {
        logger.warn("addHeader called " + str + '=' + str1);
    }
    
    public void addIntHeader(String str, int param)
    {
        logger.warn("addIntHeader called " + str + '=' + param);
    }
    
    public boolean containsHeader(String str)
    {
        logger.warn("containsHeader called " + str);
        return false;
    }
    
    public String encodeRedirectURL(String str)
    {
        logger.warn("encodeRedirectURL called " + str);
        return str;
    }
    
    public String encodeRedirectUrl(String str)
    {
        logger.warn("encodeRedirectUrl called " + str);
        return str;
    }
    
    public String encodeURL(String str)
    {
        logger.warn("encodeURL called " + str);
        return str;
    }
    
    public String encodeUrl(String str)
    {
        logger.warn("encodeUrl called " + str);
        return str;
    }
    
    public void sendError(int param) throws IOException
    {
        sendError(param, "Unspecified error");
    }
    
    /**
     * Send an error response back
     */
    public void sendError(int param, String str) throws IOException
    {
        beginResponse(param, "ERROR");
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><BODY><H1>Error ");
        sb.append(param);
        sb.append("</H1><PRE>");
        sb.append(str);
        sb.append("</PRE></BODY></HTML>");
        out_.write(Utils.encodeBasic(sb.toString()));
        finishResponse();
    }
    
    /**
     * Finish responding by closing the socket
     */
    public void finishResponse() throws IOException
    {
        channel_.socket().shutdownOutput();
    }
    
    /**
     * Begin respone by sending header back with content type
     */
    private void beginResponse(int responseCode, String sInfo) throws IOException
    {
        // need to configure blocking for writes to the output stream to work
        channel_.configureBlocking(true);
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(responseCode).append(' ').append(Utils.encodeHTML(sInfo)).append('\n');
        sb.append("Server: Donohoe Digital/1.0\n");
        sb.append("Pragma: no-cache\n"); // don't cache our responses
        if (sContentType_ != null) sb.append("Content-Type: ").append(sContentType_).append('\n');
        sb.append('\n');
        out_.write(Utils.encodeBasic(sb.toString()));
    }
    
    public void sendRedirect(String str) throws IOException
    {
        logger.warn("sendRedirect called " + str);
    }
    
    public void setDateHeader(String str, long param)
    {
        logger.warn("setDateHeader called " + str);
    }
    
    public void setHeader(String str, String str1)
    {
        logger.warn("setHeader called " + str+ '=' +str1);
    }
    
    public void setIntHeader(String str, int param)
    {
        logger.warn("setIntHeader called " + str+ '=' +param);
    }
    
    public void setStatus(int param)
    {
        logger.warn("setStatus called " + param);
    }
    
    public void setStatus(int param, String str)
    {
        logger.warn("setStatus called " + param+ '=' +str);
    }
    
    public String getContentType() 
    {
        logger.warn("getContentType called");
        return null;
    }
    
    public void setCharacterEncoding(String str) 
    {
        logger.warn("setCharacterEncoding called "+str);
    }

    // Servlet API 3.1 additions

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return "";
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }

    @Override
    public void setContentLengthLong(long len) {

    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {

    }

    @Override
    public void sendRedirect(String location, boolean clearBuffer) throws IOException {
        HttpServletResponse.super.sendRedirect(location, clearBuffer);
    }

    @Override
    public void sendRedirect(String location, int sc) throws IOException {
        HttpServletResponse.super.sendRedirect(location, sc);
    }

    @Override
    public void setTrailerFields(Supplier<Map<String, String>> supplier) {
        HttpServletResponse.super.setTrailerFields(supplier);
    }

    @Override
    public Supplier<Map<String, String>> getTrailerFields() {
        return HttpServletResponse.super.getTrailerFields();
    }
}
