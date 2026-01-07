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
package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;
import com.donohoedigital.proto.tests.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.util.*;

public class UnicastTest implements Runnable
{
    private static Selector selector_;
    private static int nNum_;
    static final int BUFFER_LENGTH = 128;
    static final int PORT = 11889;

    private DatagramChannel dc;
    private static final String DEST_HOST = "127.0.0.1";

    static boolean bSend = true;

    public static void main( String[] argv )
	{
        if (argv.length == 0)
        {
            System.err.println("UnicastTest [true|false]");
            System.exit(-1);
        }
        bSend = Utils.parseBoolean(argv[0]).booleanValue();

        UnicastTest ur = new UnicastTest();
        Thread t = new Thread(ur);
        t.start();

        //Utils.sleepMillis(250);
        if (bSend) ur.send();
    }

    public UnicastTest()
    {
        try {
            dc = DatagramChannel.open();
            dc.configureBlocking(false);
            InetSocketAddress myisa;
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Localhost: "+ localHost);
            myisa = new InetSocketAddress(localHost, PORT);
            try {
                dc.socket().bind(myisa);
            } catch (SocketException se)
            {
                System.out.println("Error trying to bind, trying alt port ("+(PORT + 1)+")");
                myisa = new InetSocketAddress(InetAddress.getLocalHost(), PORT + 1);
                dc.socket().bind(myisa);
            }
        }
        catch (IOException e)
        {
            System.out.println("processing error: " + Utils.formatExceptionText(e));
        }
    }

    public void send()
    {
        try
		{
			InetAddress ia = InetAddress.getByName(DEST_HOST); // Who to send to
            InetSocketAddress isa = new InetSocketAddress(ia, PORT);
            ByteBuffer cb = ByteBuffer.allocate(UnicastReceiverNIO.BUFFER_LENGTH);

            // not needed dc.connect(isa);

            System.out.println("Socket open on port " + dc.socket().getLocalPort() + " ... for sending to " + isa);

            String sData;
            for (int i = 1; i <= 1000; i++)
            {
                sData = "Message " + i + " " + dc.socket().getLocalAddress().getHostAddress() + " port " + dc.socket().getLocalPort();
                //sData += " (this is extra stuff to pad the message for additional testing purposes that are important)";
                cb.clear();
                cb.put(Utils.encode(sData));
                cb.flip();
                dc.send(cb, isa);

                if (i % 1 == 0) System.out.println("Sent # " + i + " from " + dc.socket().getLocalAddress().getHostAddress() + ":"+ dc.socket().getLocalPort());
                //if (i % 1 == 0) Utils.sleepMillis(250);
            }

            Utils.sleepSeconds(1);
			dc.close();
            System.exit(0);
		}
		catch (IOException e)
        {
            System.out.println("processing error: " + Utils.formatExceptionText(e));
        }
    }

    public void run()
    {
        try
		{
            selector_ = Selector.open();
            dc.register(selector_, SelectionKey.OP_READ);

			System.out.println( "waiting for a packet ...");

            // MAIN: loop forever, processing requests
            int n;
            boolean bDone_ = false;
            while (!bDone_)
            {
                n = 0;

                // this may block for a long time, upon return the
                // selected set contains keys of the ready channels
                try {
                    n = selector_.select();
                }
                catch (Throwable t)
                {
                    // don't print to log if interrupted system call - happens on shutdown,
                    // in particular on Linux
                    if (!bDone_ && Utils.getExceptionMessage(t).indexOf("Interrupted system call") == -1)
                    {
                        System.out.println("selector.select() error: " + Utils.formatExceptionText(t));
                    }
                    continue;
                }

                try {
                    // process selection
                    if (n > 0)
                    {
                        processSelection();
                    }
                }
                catch (Throwable t)
                {
                    System.out.println("processing error: " + Utils.formatExceptionText(t));
                }
            }

			dc.close();
		}
		catch (IOException e) {
            System.out.println("processing error: " + Utils.formatExceptionText(e));
        }
    }

    /**
     * Logic to process selected keys
     */
    private void processSelection()
    {
        // get an iterator over the set of selected keys
        Iterator iter = selector_.selectedKeys().iterator();

        // look at each key in the selected set
        while (iter.hasNext())
        {
            nNum_++;

            SelectionKey key = (SelectionKey) iter.next();

            try {
                if (key.isReadable())
                {
                    processChannel(key, nNum_);
                }
                // write (testing)
                //else if (key.isWritable())
                //{
                //    logger.debug("[" + nNum_ + "] WRITEABLE " + ((SocketChannel)key.channel()).socket().getInetAddress().getHostAddress());
                //}
                else
                {
                    System.out.println("[" + nNum_ + "] NOTHING TO DO: key ready ops are " + key.readyOps());
                }
            }
            catch (IOException ioe)
            {
                System.out.println("processSelection error: " + Utils.formatExceptionText(ioe));
            }

            // remove key from selected set, as it has been handled
            iter.remove();


        }

	}

    	/**
     * Get worker from pool to handle socket connection
	 */
	private void processChannel(SelectionKey key, long nNum) throws IOException
	{
        // get channel
        DatagramChannel channel = (DatagramChannel) key.channel();

        // need to cancel key otherwise can't change blocking for replies
        //key.cancel();

        // create an empty datagram packet
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_LENGTH);
        InetSocketAddress from = (InetSocketAddress) channel.receive(bb);
        if (from == null) return;
        bb.flip();
        String rcvd = Utils.decode(bb.array(), bb.position(), bb.remaining());
        System.out.println( nNum + " " + rcvd +
                            " from " + from.toString());

        // respond
        if (!bSend)
        {
            ByteBuffer cb = ByteBuffer.allocate(UnicastReceiverNIO.BUFFER_LENGTH);
            String sData = "Response to " + rcvd;
            cb.put(Utils.encode(sData));
            cb.flip();
            dc.send(cb, from);
        }
    }
}
