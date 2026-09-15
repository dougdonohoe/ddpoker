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
package com.donohoedigital.udp;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wire format: a UDPMessage serialized with toBuffer() reads back identically, and
 * packets that aren't ours (bad CRC, too short) are rejected.
 */
public class UDPMessageTest
{
    // above 32767 so the port round trips through a signed short
    private static final String PORT = "40779";
    private static final UDPID REMOTE_ID = new UDPID("0A1B2C3D-4E5F-6A7B-8C9D-0E1F2A3B4C5D");
    private static final InetSocketAddress REMOTE = new InetSocketAddress("10.1.2.3", 65000);

    private TestPeer peer;
    private InetSocketAddress local;

    @BeforeEach
    public void setUp()
    {
        new ConfigManager("udptest", ApplicationType.COMMAND_LINE);
        peer = new TestPeer();
        UDPServer server = peer.bind(PORT);
        local = server.getIP(server.getDefaultChannel());
    }

    @AfterEach
    public void tearDown()
    {
        peer.shutdown();
    }

    @Test
    public void roundTrip()
    {
        byte[] padded = Utils.encode("##hello world##");
        UDPMessage out = new UDPMessage(peer.server, 1234567890123L, REMOTE_ID, local, REMOTE);
        out.addData(new UDPData(UDPData.Type.HELLO, 1, (short) 1, (short) 1, null, 0, 0, UDPData.USER_TYPE_UNSPECIFIED));
        out.addData(new UDPData(UDPData.Type.MESSAGE, 7, (short) 2, (short) 3, padded, 2, 11, (byte) 42));
        out.addData(new UDPData(UDPData.Type.PING_ACK, 99, (short) 1, (short) 1, new byte[] {1, 2, 3}, 0, 3, (byte) 0));

        ByteBuffer buffer = out.toBuffer();
        assertEquals(out.getBufferedLength(), buffer.remaining());

        InetSocketAddress from = new InetSocketAddress("192.168.1.50", 50123);
        UDPMessage in = new UDPMessage(peer.server, buffer, local, from);

        assertEquals(1234567890123L, in.getSessionID());
        assertEquals(peer.server.getID(local), in.getSourceID());
        assertEquals(REMOTE_ID, in.getDestinationID());
        assertEquals(local, in.getSourceIPActual(), "source address read from header");
        assertTrue(local.getPort() > Short.MAX_VALUE, "port above signed short range: " + local.getPort());
        assertEquals(from, in.getSourceIPApparent());
        assertEquals(local, in.getDestinationIP(), "destination is where it actually arrived");
        assertEquals(3, in.getNumData());

        assertData(in.getData(0), UDPData.Type.HELLO, 1, 1, 1, UDPData.USER_TYPE_UNSPECIFIED, "");
        assertData(in.getData(1), UDPData.Type.MESSAGE, 7, 2, 3, (byte) 42, "hello world");
        assertEquals(3, in.getData(2).getLength());
        assertEquals(99, in.getData(2).getID());
        assertEquals(1, in.getData(1).getSendCount(), "receiver counts the send in progress");
    }

    @Test
    public void unknownDestinationIsUs()
    {
        UDPMessage out = new UDPMessage(peer.server, 1, UDPID.UNKNOWN_ID, local, REMOTE);
        UDPMessage in = new UDPMessage(peer.server, out.toBuffer(), local, REMOTE);
        assertEquals(peer.server.getID(local), in.getDestinationID());
    }

    @Test
    public void corruptHeaderRejected()
    {
        ByteBuffer buffer = new UDPMessage(peer.server, 1, REMOTE_ID, local, REMOTE).toBuffer();
        buffer.put(10, (byte) (buffer.get(10) ^ 0xFF));
        assertInvalid(buffer);
    }

    @Test
    public void shortPacketRejected()
    {
        assertInvalid(ByteBuffer.wrap(new byte[UDPMessage.HEADER_SIZE - 1]));
    }

    @Test
    public void randomBytesRejected()
    {
        byte[] junk = new byte[200];
        new java.util.Random(42).nextBytes(junk);
        assertInvalid(ByteBuffer.wrap(junk));
    }

    private void assertInvalid(ByteBuffer buffer)
    {
        ApplicationError e = assertThrows(ApplicationError.class, () -> new UDPMessage(peer.server, buffer, local, REMOTE));
        assertEquals(ErrorCodes.ERROR_INVALID_MESSAGE, e.getErrorCode());
    }

    private static void assertData(UDPData data, UDPData.Type type, int id, int partID, int parts, byte userType, String text)
    {
        assertEquals(type, data.getType());
        assertEquals(id, data.getID());
        assertEquals(partID, data.getPartID());
        assertEquals(parts, data.getNumParts());
        assertEquals(userType, data.getUserType());
        assertEquals(text, data.getLength() == 0 ? "" : Utils.decode(data.getData(), data.getOffset(), data.getLength()));
    }
}
