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
package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;
import com.donohoedigital.proto.tests.*;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class UnicastSenderNIO 
{
	public static void main( String[] argv ) 
	{
		try 
		{
			// get the InetAddress of the MCAST group
			InetAddress ia = InetAddress.getByName("127.0.0.1" );
            InetSocketAddress isa = new InetSocketAddress(ia, UnicastReceiverNIO.PORT);

            ByteBuffer cb = ByteBuffer.allocate(UnicastReceiverNIO.BUFFER_LENGTH);
            DatagramChannel dc = DatagramChannel.open();
            InetSocketAddress myisa;
            myisa = new InetSocketAddress(InetAddress.getLocalHost(), UnicastReceiverNIO.PORT);
            try {

                dc.socket().bind(myisa);
            } catch (SocketException se)
            {
                System.out.println("Error trying to bind, trying alt port ("+(UnicastReceiverNIO.PORT + 1)+")");
                myisa = new InetSocketAddress(InetAddress.getLocalHost(), UnicastReceiverNIO.PORT + 1);
                dc.socket().bind(myisa);
            }

            // not needed dc.connect(isa);

            System.out.println("Socket open on port " + dc.socket().getLocalPort() + " ... for sending to " + isa);

            String sData;
            for (int i = 1; i <= 1000; i++)
            {
                sData = "Message " + i + " " + dc.socket().getLocalAddress().getHostAddress() + " port " + dc.socket().getLocalPort();
                cb.clear();
                cb.put(Utils.encode(sData));
                cb.flip();
                dc.send(cb, isa);
                if (i % 3 == 0) Utils.sleepMillis(1);
                if (i % 100 == 0) System.out.println("Sent # " + i + " from " + dc.socket().getLocalAddress().getHostAddress() + ":"+ dc.socket().getLocalPort());
            }

            Utils.sleepSeconds(2);
			dc.close();
		}
		catch (IOException e)
        {
            System.out.println("processing error: " + Utils.formatExceptionText(e));
        }
	}
}
