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

import java.net.*;
import java.io.*;

// java MulticastSnooper ALL-SYSTEMS.MCAST.NET 4000
public class MulticastReceiver
{
	public static void main( String[] argv ) 
	{
		try 
		{
			// get the InetAddress of the MCAST group 
			InetAddress ia = InetAddress.getByName("239.252.101.202" );

			// get the port that the MCAST group members will be listening on
			int port = 7755;

			// create a multicast socket on the specified local port number
			MulticastSocket ms = new MulticastSocket( port );

			// create an empty datagram packet
			DatagramPacket dp = new DatagramPacket(new byte[128], 128);

			//Join a multicast group and wait for some action
			ms.joinGroup(ia); 
			System.out.println( "waiting for a packet from "+ia+"...");
            boolean bDone = false;
            while (!bDone)
            {
                ms.receive(dp);

                // print out what we received and quit
                System.out.println( new String(dp.getData() ));
            }

			ms.leaveGroup(ia);
			ms.close();
		} 
		catch (IOException e) {}
	}
}
