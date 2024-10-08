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
 * DDPostalServiceImpl.java
 *
 * Created on March 12, 2003, 1:55 PM
 */

package com.donohoedigital.mail;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 * @author donohoe
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DDPostalServiceImpl implements Runnable, DDPostalService
{
    private static final Logger logger = LogManager.getLogger(DDPostalServiceImpl.class);

    private static final boolean DEBUG = false;

    // one mail queue needed
    private Thread threadQ_ = null;

    // instance info
    private final boolean bInitMailQ_;
    private List<Message> mailQ_ = new ArrayList<>();
    private boolean bDone_ = false;
    private Properties props_;
    private DDAuthenticator auth_;
    private int nWait_;
    private boolean bLoopAtEnd_;
    private final Map<String, DDMailErrorHandler> errorHandlers_ = new HashMap<>();
    private boolean bSleeping_ = false;
    private final Object SLEEPCHECK = new Object();
    public static final String HEADER_ERRORINFO = "X-DDMail-ErrorInfo";

    /**
     * Creates a new instance of DDMailQueue
     */
    public DDPostalServiceImpl(boolean bLoopAtEnd)
    {
        bInitMailQ_ = PropertyConfig.getBooleanProperty("settings.mailqueue.init", false);

        if (!bInitMailQ_)
        {
            logger.info("NOT initializing DDPostalServiceImpl");
            return;
        }

        logger.info("Initializing DD Postal Service");
        String sHost = PropertyConfig.getRequiredStringProperty("settings.smtp.host");
        String sUser = PropertyConfig.getRequiredStringProperty("settings.smtp.user");
        String sPass = PropertyConfig.getRequiredStringProperty("settings.smtp.pass");
        int nWait = PropertyConfig.getRequiredIntegerProperty("settings.mailqueue.wait");
        boolean bAuth = PropertyConfig.getRequiredBooleanProperty("settings.smtp.auth");
        int nTimeout = PropertyConfig.getRequiredIntegerProperty("settings.smtp.timeout.millis");
        int nConnectionTimeout = PropertyConfig.getRequiredIntegerProperty("settings.smtp.connectiontimeout.millis");

        threadQ_ = new Thread(this, "DD Postal Service");
        threadQ_.start();

        nWait_ = nWait;
        bLoopAtEnd_ = bLoopAtEnd;

        // properties
        props_ = new Properties();
        props_.put("mail.smtp.host", sHost);
        props_.put("mail.smtp.timeout", Integer.toString(nTimeout)); // milli
        props_.put("mail.smtp.connectiontimeout", Integer.toString(nConnectionTimeout)); // milli
        //props_.put("mail.smtp.from", "bouncer@example.com");
        //props_.put("mail.smtp.localhost", "games.example.com");

        // auth
        if (bAuth)
        {
            props_.put("mail.smtp.auth", "true");
            auth_ = new DDAuthenticator(sUser, sPass);
        }

        logger.info("DD Postal Service props: " + props_);

        // debug
        if (DEBUG) props_.put("mail.debug", "1");
    }

    /**
     * Cleanup
     */
    public void destroy()
    {
        if (!bInitMailQ_) return;

        logger.info("Starting DD Postal Service Shutdown...");
        bDone_ = true;
        wakeup();
        try
        {
            threadQ_.join();
        }
        catch (InterruptedException ie)
        {
            Thread.interrupted();
        }
        threadQ_ = null;
    }

    /**
     * Start queue
     */
    public void run()
    {
        logger.info("Mail Queue started, wait seconds = " + nWait_);
        while (!bDone_)
        {
            // sleep first, so if we are woken up by interrupt,
            // we process any email that came through
            bSleeping_ = true;
            Utils.sleepSeconds(nWait_);
            bSleeping_ = false;

            // use while loop in case messages come in while we are processing
            // skip if last send attempt failed (so we can sleep then try again
            // in a bit)
            synchronized (SLEEPCHECK)
            {
                List<Message> list;
                boolean bSuccess = true;
                while (bSuccess && (list = getMessageQueue()) != null)
                {
                    bSuccess = sendMessages(list);
                }
            }
        }

        logger.info("Shutting down DD Postal Service...");
        List<Message> list;
        while ((list = getMessageQueue()) != null)
        {
            boolean bSuccess = sendMessages(list);
            if (!bSuccess || !bLoopAtEnd_) break;
        }
        list = getMessageQueue();
        if (list != null && !list.isEmpty())
        {
            logger.error("Unable to send " + list.size() + " emails");
        }
        logger.info("DD Postal Service shut down.");
    }

    /**
     * Wake up thread if sleeping
     */
    private void wakeup()
    {
        synchronized (SLEEPCHECK)
        {
            if (bSleeping_)
            {
                logger.info("Waking up sleeping DD Postal Service...");
                threadQ_.interrupt();
            }
        }
    }

    /**
     * Add an error handler
     */
    public void addErrorHandler(String sAppName, DDMailErrorHandler handler)
    {
        errorHandlers_.put(sAppName, handler);
    }

    /**
     * Parse the error info and call the appropriate handler
     */
    private void handleError(String[] sErrorHeaders) throws IOException
    {
        if (sErrorHeaders == null || sErrorHeaders.length == 0) return;

        handleError(sErrorHeaders[0]);
    }

    /**
     * Parse the error info and call the appropriate handler
     */
    private void handleError(String sErrorHeader) throws IOException
    {
        if (sErrorHeader == null) return;

        int nDelim = sErrorHeader.indexOf('.');
        if (nDelim == -1)
        {
            logger.warn("Unable to parse " + HEADER_ERRORINFO + "=" + sErrorHeader);
            return;
        }

        String sAppName = sErrorHeader.substring(0, nDelim);
        String sErrorDetails = sErrorHeader.substring(nDelim + 1);

        DDMailErrorHandler handler = errorHandlers_.get(sAppName);
        if (handler == null)
        {
            logger.warn("No error handler found for '" + sAppName + "'");
            return;
        }

        handler.handleError(sAppName, sErrorDetails);
    }

    /**
     * Return value used for error header (DDMail.HEADER_ERRORINFO)
     */
    private String getErrorHeader(DDMailErrorInfo info)
    {
        return info.getErrorAppName() + "." + info.getErrorDetails();
    }

    /**
     * Add a message to the Queue
     */
    private synchronized void addMessage(Message msg)
    {
        mailQ_.add(msg);
    }

    /**
     * Used to add unprocessed messages back into queue (at start)
     */
    private synchronized void addMessages(List<Message> list)
    {
        mailQ_.addAll(0, list);
    }

    /**
     * Get current contents of queue and start new list
     */
    private synchronized List<Message> getMessageQueue()
    {
        if (mailQ_.isEmpty()) return null; // avoid new object if empty

        List<Message> list = mailQ_;
        mailQ_ = new ArrayList<>();
        return list;
    }

    /**
     * Process current messages in the queue.  Return true if successful
     * and returns false if an exception was received.
     */
    private boolean sendMessages(List<Message> list)
    {
        Message msg;

        int nNum = (list == null) ? 0 : list.size();

        logger.debug("Mail queue: " + nNum + " messages to process");

        if (nNum == 0) return true;

        // get session
        Session session = Session.getDefaultInstance(props_, auth_);
        session.setDebug(DEBUG);

        try
        {
            Transport transport = session.getTransport("smtp");
            transport.connect();
            boolean sent;
            // loop from last message on queue, so we can remove entries
            for (int i = nNum - 1; i >= 0; i--)
            {
                msg = list.get(0);
                sent = sendMessage(transport, msg);
                if (!sent)
                {
                    handleError(msg.getHeader(HEADER_ERRORINFO));
                }
                list.remove(0);
                logger.debug((sent ? "SUCCESS" : "FAILED") + " SENDING MAIL TO: " + msg.getRecipients(Message.RecipientType.TO)[0] + "  Subject: " + msg.getSubject());
            }

            transport.close();
            return true;
        }
        catch (NoSuchProviderException nspe)
        {
            // this should not happen at all, but we need to catch
            // the exception - smtp is a known provider
            throw new ApplicationError(ErrorCodes.ERROR_BAD_PROVIDER,
                                       "SMTP provider not found", nspe, null);
        }
        catch (Throwable me)
        {
            logger.error("Failed sending message: " + Utils.formatExceptionText(me));

            // put unprocessed messages back on queue
            addMessages(list);
            return false;
        }
    }

    /**
     * Send a message using the given transport. Return true if sent
     * successfully
     */
    private boolean sendMessage(Transport transport, Message msg)
            throws MessagingException
    {
        try
        {
            transport.sendMessage(msg, msg.getAllRecipients());
            return true;
        }
        catch (SendFailedException sfex)
        {
            String sMsg = Utils.getExceptionMessage(sfex);
            sMsg = sMsg.replace('\n', ' ');
            logger.error("Error sending mail: " + sMsg);

            if (DEBUG)
            {
                Address[] invalid = sfex.getInvalidAddresses();
                if (invalid != null && invalid.length > 0)
                {
                    logger.error("Invalid Addresses: ");
                    for (Address anInvalid : invalid)
                        logger.error("  --> " + anInvalid);
                }
                Address[] validUnsent = sfex.getValidUnsentAddresses();
                if (validUnsent != null && validUnsent.length > 0)
                {
                    logger.error("Valid Unsent Addresses: ");
                    for (Address aValidUnsent : validUnsent)
                        logger.error("  --> " + aValidUnsent);
                }
                Address[] validSent = sfex.getValidSentAddresses();
                if (validSent != null && validSent.length > 0)
                {
                    logger.error("Valid Sent Addresses: ");
                    for (Address aValidSent : validSent)
                        logger.error("  --> " + aValidSent);
                }
            }

            return false;
        }
    }

    /**
     * Send mail to single recipient
     */
    public void sendMail(String sTo,
                         String sFrom, String sReplyTo,
                         String sSubject,
                         String sPlainText, String sHtmlText,
                         DDAttachment attachment,
                         DDMailErrorInfo info)
    {
        sendMail(createInternetAddressArray(sTo),
                 null, null,
                 sFrom, sReplyTo,
                 sSubject,
                 sPlainText, sHtmlText,
                 attachment, info);
    }

    /**
     * Basic api for sending email
     */
    @SuppressWarnings("SameParameterValue")
    private void sendMail(InternetAddress[] to,
                          InternetAddress[] cc,
                          InternetAddress[] bcc,
                          String sFrom, String sReplyTo,
                          String sSubject,
                          String sPlainText, String sHtmlText,
                          DDAttachment attachment,
                          DDMailErrorInfo info)
    {

        ApplicationError.assertNotNull(to, "To address(es) must be specified");
        ApplicationError.assertTrue(to.length > 0, "To address(es) must be specified");
        ApplicationError.assertNotNull(sSubject, "Subject must be specified");
        ApplicationError.assertNotNull(sFrom, "From must be specified");


        try
        {
            // create a message
            Message msg = new MimeMessage((Session) null);
            msg.setHeader("X-Mailer", "Donohoe Digital LLC Java Architecture");
            if (info != null) msg.setHeader(HEADER_ERRORINFO, getErrorHeader(info));
            msg.setFrom(createInternetAddress(sFrom));
            msg.setSubject(sSubject);
            msg.setSentDate(new Date());
            if (sReplyTo != null) msg.setReplyTo(createInternetAddressArray(sReplyTo));

            // recipients
            msg.setRecipients(Message.RecipientType.TO, to);
            if (cc != null) msg.setRecipients(Message.RecipientType.CC, cc);
            if (bcc != null) msg.setRecipients(Message.RecipientType.BCC, bcc);

            // determine if a multipart message
            if ((attachment == null) &&
                (((sPlainText != null) && (sHtmlText == null)) || ((sHtmlText != null) && (sPlainText == null))))
            {
                if (sPlainText != null)
                {
                    msg.setContent(sPlainText, "text/plain; charset=UTF-8");
                }
                else
                {
                    msg.setContent(sHtmlText, "text/html; charset=UTF-8");
                }
            }
            else
            {
                // create the plain part
                MimeBodyPart plain = null;
                if (sPlainText != null)
                {
                    plain = new MimeBodyPart();
                    plain.setDataHandler(new DataHandler(new PlainSource(sPlainText)));
                }

                // create the html part
                MimeBodyPart html = null;
                if (sHtmlText != null)
                {
                    html = new MimeBodyPart();
                    html.setDataHandler(new DataHandler(new HtmlSource(sHtmlText)));
                }

                // create the second message part for attachment
                MimeBodyPart attach = null;
                if (attachment != null)
                {
                    attach = new MimeBodyPart();
                    attach.setDataHandler(new DataHandler(attachment));
                    attach.setFileName(attachment.getFileName());
                }

                // put everything together
                Multipart alt = new MimeMultipart("alternative");
                if (plain != null) alt.addBodyPart(plain);
                if (html != null) alt.addBodyPart(html);

                if (attach == null)
                {
                    msg.setContent(alt);
                }
                else
                {
                    Multipart mixed = new MimeMultipart("mixed");
                    MimeBodyPart wrap = new MimeBodyPart();
                    wrap.setContent(alt);    // HERE'S THE KEY
                    mixed.addBodyPart(wrap);
                    mixed.addBodyPart(attach);
                    msg.setContent(mixed);
                }

                msg.saveChanges();
            }

            addMessage(msg);
        }
        catch (MessagingException mex)
        {
            throw new ApplicationError(ErrorCodes.ERROR_BAD_EMAIL_ADDRESS,
                                       "Error composing message to " + to[0], mex, null);
        }
    }

    /**
     * Create InternetAddress array from a single address
     */
    private static InternetAddress[] createInternetAddressArray(String sEmail)
    {
        return new InternetAddress[]{createInternetAddress(sEmail)};
    }

    /**
     * Create an InternetAddress from the given string
     */
    private static InternetAddress createInternetAddress(String sEmail)
    {
        try
        {
            return new InternetAddress(sEmail);
        }
        catch (AddressException ae)
        {
            throw new ApplicationError(ErrorCodes.ERROR_BAD_EMAIL_ADDRESS,
                                       "Invalid email: " + sEmail, ae, null);
        }
    }

    /**
     * Authenticator
     */
    private static class DDAuthenticator extends Authenticator
    {
        PasswordAuthentication auth;

        public DDAuthenticator(String sUser, String sPass)
        {
            auth = new PasswordAuthentication(sUser, sPass);

        }

        @Override
        public PasswordAuthentication getPasswordAuthentication()
        {
            return auth;
        }
    }

    /**
     * DataSource for html part of an email
     */
    private static class HtmlSource implements DataSource
    {
        String sHtml;

        public HtmlSource(String sHtml)
        {
            this.sHtml = sHtml;
        }

        public String getContentType()
        {
            return "text/html; charset=" + Utils.CHARSET_NAME;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(Utils.encode(sHtml));
        }

        public String getName()
        {
            return "HtmlSource";
        }

        public OutputStream getOutputStream() throws IOException
        {
            throw new IOException("Unsupported");
        }
    }

    /**
     * DataSource for plaintext part of an email
     */
    private static class PlainSource implements DataSource
    {
        String sPlain;

        public PlainSource(String sPlain)
        {
            this.sPlain = sPlain;
        }

        public String getContentType()
        {
            return "text/plain; charset=" + Utils.CHARSET_NAME;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(Utils.encode(sPlain));
        }

        public String getName()
        {
            return "PlainSource";
        }

        public OutputStream getOutputStream() throws IOException
        {
            throw new IOException("Unsupported");
        }
    }
}
