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

import java.net.*;
import java.io.*;

public class MulticastSender 
{
	public static void main( String[] argv ) 
	{
		try 
		{
			// get the InetAddress of the MCAST group 
			InetAddress ia = InetAddress.getByName("239.252.101.202" );

			// get the port that the MCAST group members will be listening on
			int recvPort = 7755;

			// create a datagram with a suitable message
			String str = "Hello from: "+InetAddress.getLocalHost();
			byte[] data = str.getBytes();
			DatagramPacket dp = new DatagramPacket(data, data.length, ia, recvPort);

			// create a multicast socket bound to any local port
			MulticastSocket ms = new MulticastSocket();

			//Join the multicast group
			ms.joinGroup(ia); 

			// send the message with a Time-To-Live (TTL)=1
            int ttl = ms.getTimeToLive();
            ms.setTimeToLive(1);
            String sData;
            for (int i = 0; i < 1000; i++)
            {
                sData = "Message " + i + "  stuff stuff asd sdfa asdf asdf as asasdf asdfsaf sadflj asfasasjflj";
                dp.setData(sData.getBytes());
                ms.send(dp);
            }
            ms.setTimeToLive(ttl);

			// tidy up - leave the group and close the socket
			ms.leaveGroup(ia);
			ms.close();
		} 
		catch (IOException e) {}
	}
}
