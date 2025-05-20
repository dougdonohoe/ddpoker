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
package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.util.*;

public class UnicastReceiverNIO
{
    private static Selector selector_;
    private static int nNum_;
    static final int BUFFER_LENGTH = 256;
    static final int PORT = 7755;

    public static void main( String[] argv )
	{
		try 
		{
			// create a multicast socket on the specified local port number
			//DatagramSocket ms = new DatagramSocket( port );
            DatagramChannel dc = DatagramChannel.open();
            dc.configureBlocking(false);
            InetSocketAddress isa = new InetSocketAddress(PORT);
            dc.socket().bind(isa);

            selector_ = Selector.open();
            dc.register(selector_, SelectionKey.OP_READ);


			System.out.println( "waiting for a packet from ...");


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
    private static void processSelection()
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
	private static void processChannel(SelectionKey key, long nNum) throws IOException
	{
        // get channel
        DatagramChannel channel = (DatagramChannel) key.channel();

//        // get worker
//		SocketThread worker = pool_.getWorker();
//
//		if (worker == null)
//        {
//            nRunningNoWorkerCnt_ ++;
//
//            // No threads available, do nothing, the selection
//			// loop will keep calling this method until a
//			// thread becomes available
//
//            // warn if worker is null and sleep
//            // to allow time for another thread to finish
//            if (nElapsedNoWorkerTime_ >= LOG_UNAVAIL)
//            {
//                logger.warn("*** NO worker thread available for " + nElapsedNoWorkerTime_ + " millis " +
//                            "(sleep is " + SLEEP_UNAVAIL + "), current ip=" + Utils.getIPAddress(channel));
//                nElapsedNoWorkerTime_ = 0;
//            }
//
//            Utils.sleepMillis(SLEEP_UNAVAIL);
//
//            nElapsedNoWorkerTime_ += SLEEP_UNAVAIL;
//
//			return;
//		}
//        else
//        {
//            nElapsedNoWorkerTime_ = 0;
//        }
//
//        // hit count
//        nHits_++;
//
//        // we have a worker, so process it
//        if (DEBUG_ONLINE) logger.debug("[" + nNum +"] READING " + Utils.getIPAddress(channel));

        // need to cancel key otherwise can't change blocking for replies
        //key.cancel();

		// invoking this wakes up the worker thread then returns
//		worker.processChannel(channel);

        // create an empty datagram packet
//        DatagramPacket dp = new DatagramPacket(new byte[128], 128);
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_LENGTH);
        InetSocketAddress from = (InetSocketAddress) channel.receive(bb);
        bb.flip();
        System.out.println( nNum + " " + Utils.decode(bb.array(), bb.position(), bb.remaining()) +
                            " from " + from.toString());
    }
}
