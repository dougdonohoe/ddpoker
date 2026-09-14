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
package com.donohoedigital.comms;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DDMessage} - accessors, data chunks, the static defaults
 * and, most importantly, the write/read and marshal/demarshal round trips used
 * to send messages between client, server and peers.
 */
public class DDMessageTest {

    private static final Version VERSION = Version.parse("3.1.9_fr");
    private static final String PUBLIC_KEY = "P-public";
    private static final String REAL_KEY = "1234-5678-9012-3456";

    @TempDir
    File tempDir;

    private Version savedVersion;
    private String savedKey;
    private String savedRealKey;

    @BeforeEach
    public void setUp() {
        // defaults are static - save and restore so tests don't leak into each other
        savedVersion = DDMessage.getDefaultVersion();
        savedKey = new DDMessage().getKey();
        savedRealKey = DDMessage.getDefaultRealKey();

        DDMessage.setDefaultVersion(VERSION);
        DDMessage.setDefaultKey(PUBLIC_KEY);
        DDMessage.setDefaultRealKey(REAL_KEY);
        DDMessage.setMsgState(null);
    }

    @AfterEach
    public void tearDown() {
        DDMessage.setDefaultVersion(savedVersion);
        DDMessage.setDefaultKey(savedKey);
        DDMessage.setDefaultRealKey(savedRealKey);
        DDMessage.setMsgState(null);
    }

    // -------------------------------------------------------------------------
    // constructors / static defaults
    // -------------------------------------------------------------------------

    @Test
    public void constructor_appliesDefaults() {
        long before = Utils.getCurrentTimeStamp();
        DDMessage msg = new DDMessage();

        assertSame(VERSION, msg.getVersion());
        assertEquals(PUBLIC_KEY, msg.getKey());
        assertEquals(DDMessage.CAT_NONE, msg.getCategory());
        assertEquals(DDMessageListener.STATUS_NONE, msg.getStatus());
        assertEquals(0, msg.getNumData());
        assertTrue(msg.getCreateTimeStamp() >= before / 1000 * 1000);
        assertEquals(msg.getCreateTimeStamp(), msg.getCreateTimeStampLong());
    }

    @Test
    public void constructor_publicKeyNotRealKey() {
        // real key is only swapped in by DDMessenger when talking to the server
        assertEquals(REAL_KEY, DDMessage.getDefaultRealKey());
        assertEquals(PUBLIC_KEY, new DDMessage(1).getKey());

        DDMessage.setDefaultKey("P-changed");
        assertEquals("P-changed", new DDMessage().getKey());
    }

    @Test
    public void constructor_category() {
        DDMessage msg = new DDMessage(42);
        assertEquals(42, msg.getCategory());
        assertEquals(0, msg.getNumData());
    }

    @Test
    public void constructor_string() {
        DDMessage msg = new DDMessage(7, "hello");
        assertEquals(7, msg.getCategory());
        assertEquals(1, msg.getNumData());
        assertEquals("hello", msg.getDataAsString());
        assertArrayEquals(Utils.encode("hello"), msg.getData());
    }

    @Test
    public void constructor_bytes() {
        byte[] bytes = {1, 2, 3};
        DDMessage msg = new DDMessage(8, bytes);
        assertEquals(8, msg.getCategory());
        assertSame(bytes, msg.getData());
    }

    @Test
    public void constructor_files() throws IOException {
        File a = writeFile("a.txt", "aaa");
        File b = writeFile("b.txt", "bbbb");

        DDMessage one = new DDMessage(9, a);
        assertEquals(9, one.getCategory());
        assertEquals(1, one.getNumData());
        assertNull(one.getData(), "file data not available until marshalled");
        assertNull(one.getDataAsString());

        DDMessage two = new DDMessage(10, new File[]{a, b});
        assertEquals(2, two.getNumData());
    }

    @Test
    public void msgState_usedForParams() throws IOException {
        DDMessage.setMsgState(new MsgState());
        DDMessage msg = new DDMessage(1, "with state");
        msg.setString("param", "value");

        DDMessage read = roundTrip(msg);
        assertEquals("value", read.getString("param"));
    }

    // -------------------------------------------------------------------------
    // accessors
    // -------------------------------------------------------------------------

    @Test
    public void accessors() {
        DDMessage msg = new DDMessage();

        msg.setKey("K");
        assertEquals("K", msg.getKey());

        Version v = Version.parse("2.0b6_en");
        msg.setVersion(v);
        assertSame(v, msg.getVersion());
        assertEquals("en", msg.getLocale());

        msg.setCategory(3);
        assertEquals(3, msg.getCategory());

        msg.setFromIP("10.0.0.1");
        assertEquals("10.0.0.1", msg.getFromIP());

        msg.setException("exc");
        assertEquals("exc", msg.getException());

        msg.setDDException("ddexc");
        assertEquals("ddexc", msg.getDDException());

        msg.setApplicationErrorMessage("err");
        assertEquals("err", msg.getApplicationErrorMessage());

        msg.setApplicationStatusMessage("status");
        assertEquals("status", msg.getApplicationStatusMessage());

        msg.setStatus(DDMessageListener.STATUS_TIMEOUT);
        assertEquals(DDMessageListener.STATUS_TIMEOUT, msg.getStatus());

        long ts = msg.getCreateTimeStamp();
        msg.setCreateTimeStamp();
        assertTrue(msg.getCreateTimeStamp() > ts, "timestamp includes sequence so always increases");

        // reserved param names
        assertEquals("K", msg.getString(DDMessage.PARAM_KEY));
        assertEquals(3, msg.getInteger(DDMessage.PARAM_CATEGORY));
        assertEquals("10.0.0.1", msg.getString(DDMessage.PARAM_FROM_IP));
    }

    @Test
    public void locale_nullWithoutVersion() {
        DDMessage.setDefaultVersion(null);
        DDMessage msg = new DDMessage();
        assertNull(msg.getVersion());
        assertNull(msg.getLocale());
    }

    @Test
    public void unsetAccessorsReturnNull() {
        DDMessage msg = new DDMessage();
        assertNull(msg.getFromIP());
        assertNull(msg.getException());
        assertNull(msg.getDDException());
        assertNull(msg.getApplicationErrorMessage());
        assertNull(msg.getApplicationStatusMessage());
    }

    @Test
    public void createTimeStamp_zeroWhenMissing() {
        DDMessage msg = new DDMessage();
        msg.remove(DDMessage.PARAM_TIME);
        assertEquals(0, msg.getCreateTimeStamp());
        assertNull(msg.getCreateTimeStampLong());
    }

    // -------------------------------------------------------------------------
    // data chunks
    // -------------------------------------------------------------------------

    @Test
    public void addData_ignoresNulls() {
        DDMessage msg = new DDMessage();
        msg.addData((byte[]) null);
        msg.addData((String) null);
        msg.addData((File) null);
        msg.addData((File[]) null);
        msg.addData(new File[0]);
        assertEquals(0, msg.getNumData());
        assertNull(msg.getData());
        assertNull(msg.getDataAsString());
    }

    @Test
    public void addData_multipleChunks() {
        DDMessage msg = new DDMessage();
        msg.addData("first");
        msg.addData(new byte[]{9, 8});
        msg.addData("third é中");

        assertEquals(3, msg.getNumData());
        assertEquals("first", msg.getDataAtAsString(0));
        assertArrayEquals(new byte[]{9, 8}, msg.getDataAt(1));
        assertEquals("third é中", msg.getDataAtAsString(2));
    }

    @Test
    public void clearData() {
        DDMessage msg = new DDMessage(1, "data");
        msg.clearData();
        assertEquals(0, msg.getNumData());

        new DDMessage().clearData(); // no data list yet - no-op
    }

    @Test
    public void copyTo() {
        DDMessage src = new DDMessage(5, "chunk");
        src.setString("extra", "value");
        src.setStatus(DDMessageListener.STATUS_APPL_ERROR);

        DDMessage dest = new DDMessage();
        dest.setKey("overwritten");
        src.copyTo(dest);

        assertEquals(5, dest.getCategory());
        assertEquals("value", dest.getString("extra"));
        assertEquals(PUBLIC_KEY, dest.getKey());
        assertEquals(src.getCreateTimeStamp(), dest.getCreateTimeStamp());
        assertEquals(DDMessageListener.STATUS_APPL_ERROR, dest.getStatus());
        assertEquals(1, dest.getNumData());
        assertEquals("chunk", dest.getDataAsString());

        // no data - nothing added
        DDMessage empty = new DDMessage();
        new DDMessage().copyTo(empty);
        assertEquals(0, empty.getNumData());
    }

    // -------------------------------------------------------------------------
    // write / read round trip
    // -------------------------------------------------------------------------

    @Test
    public void writeRead_paramsRoundTrip() throws IOException {
        DDMessage msg = new DDMessage(99);
        msg.setFromIP("192.168.1.1");
        msg.setApplicationErrorMessage("error: with = special : chars ~ and \\ backslash");
        msg.setString("unicode", "café 中文 🂡");
        msg.setString("newlines", "one\ntwo\n\nthree\n");
        msg.setString("empty", "");
        msg.setString("null", null);
        msg.setInteger("int", Integer.MIN_VALUE);
        msg.setLong("long", Long.MAX_VALUE);
        msg.setDouble("double", 3.14159);
        msg.setBoolean("true", true);
        msg.setBoolean("false", false);

        DMArrayList<Object> list = new DMArrayList<>();
        list.add(1);
        list.add("two");
        list.add(3L);
        msg.setList("list", list);

        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("nested", "map");
        map.setInteger("n", 5);
        msg.setObject("map", map);

        DDMessage read = roundTrip(msg);

        assertEquals(msg.size(), read.size(), "same params: " + read.toStringParams());
        assertEquals(99, read.getCategory());
        assertEquals(VERSION, read.getVersion());
        assertEquals("fr", read.getLocale());
        assertEquals(PUBLIC_KEY, read.getKey());
        assertEquals(msg.getCreateTimeStamp(), read.getCreateTimeStamp());
        assertEquals("192.168.1.1", read.getFromIP());
        assertEquals(msg.getApplicationErrorMessage(), read.getApplicationErrorMessage());
        assertEquals(msg.getString("unicode"), read.getString("unicode"));
        assertEquals(msg.getString("newlines"), read.getString("newlines"));
        assertEquals("", read.getString("empty"));
        assertTrue(read.containsKey("null"));
        assertNull(read.getString("null"));
        assertEquals(Integer.MIN_VALUE, read.getInteger("int"));
        assertEquals(Long.MAX_VALUE, read.getLong("long"));
        assertEquals(3.14159, read.getDouble("double"));
        assertEquals(Boolean.TRUE, read.getBoolean("true"));
        assertEquals(Boolean.FALSE, read.getBoolean("false"));
        assertEquals(list, read.getList("list"));

        DMTypedHashMap readMap = assertInstanceOf(DMTypedHashMap.class, read.getObject("map"));
        assertEquals("map", readMap.getString("nested"));
        assertEquals(5, readMap.getInteger("n"));

        assertEquals(0, read.getNumData());
        assertEquals(DDMessageListener.STATUS_NONE, read.getStatus());
    }

    @Test
    public void writeRead_statusNotSent() throws IOException {
        DDMessage msg = new DDMessage(1);
        msg.setStatus(DDMessageListener.STATUS_SERVER_ERROR);
        assertEquals(DDMessageListener.STATUS_NONE, roundTrip(msg).getStatus());
    }

    @Test
    public void writeRead_dataChunksRoundTrip() throws IOException {
        // every byte value, including the '\n' delimiter
        byte[] allBytes = new byte[256 * 3];
        for (int i = 0; i < allBytes.length; i++) allBytes[i] = (byte) i;
        byte[] newlines = {'\n', '\n', '\n', '\n', '\n'};

        DDMessage msg = new DDMessage(2, "text é");
        msg.addData(allBytes);
        msg.addData(new byte[0]);
        msg.addData(newlines);
        msg.addData("last");

        DDMessage read = roundTrip(msg);

        assertEquals(5, read.getNumData());
        assertEquals("text é", read.getDataAsString());
        assertArrayEquals(allBytes, read.getDataAt(1));
        assertArrayEquals(new byte[0], read.getDataAt(2));
        assertArrayEquals(newlines, read.getDataAt(3));
        assertEquals("last", read.getDataAtAsString(4));

        // internal chunk count param is not left behind on either side
        assertFalse(msg.containsKey("_#_"));
        assertFalse(read.containsKey("_#_"));
    }

    @Test
    public void writeRead_fileChunksRoundTrip() throws IOException {
        byte[] binary = new byte[10_000];
        for (int i = 0; i < binary.length; i++) binary[i] = (byte) (i * 31);
        File text = writeFile("text.txt", "file one\n\n\ncontents");
        File bin = new File(tempDir, "binary.dat");
        Files.write(bin.toPath(), binary);
        File empty = writeFile("empty.txt", "");

        DDMessage msg = new DDMessage(3, new File[]{text, bin, empty});
        msg.addData("after files");

        DDMessage read = roundTrip(msg);

        assertEquals(4, read.getNumData());
        assertEquals("file one\n\n\ncontents", read.getDataAtAsString(0));
        assertArrayEquals(binary, read.getDataAt(1));
        assertArrayEquals(new byte[0], read.getDataAt(2));
        assertEquals("after files", read.getDataAtAsString(3));
    }

    @Test
    public void writeRead_missingFileSendsEmptyChunk() throws IOException {
        // size is taken when the file is added (0 if missing) and zero-size chunks are skipped on write
        DDMessage msg = new DDMessage(3, new File(tempDir, "does-not-exist.txt"));
        DDMessage read = roundTrip(msg);
        assertEquals(1, read.getNumData());
        assertArrayEquals(new byte[0], read.getData());
    }

    @Test
    public void write_isRepeatable() throws IOException {
        DDMessage msg = new DDMessage(4, "data");
        msg.addData(new byte[]{1, 2, 3});

        assertArrayEquals(writeBytes(msg), writeBytes(msg));
    }

    @Test
    public void writeRead_nestedMessage() throws IOException {
        DDMessage inner = new DDMessage(11, "inner data");
        inner.setString("inner", "param");

        DDMessage outer = new DDMessage(12, "outer data");
        outer.setObject("msg", inner);

        DDMessage read = roundTrip(outer);
        DDMessage readInner = assertInstanceOf(DDMessage.class, read.getObject("msg"));

        assertEquals("outer data", read.getDataAsString());
        assertEquals(11, readInner.getCategory());
        assertEquals("param", readInner.getString("inner"));
        assertEquals("inner data", readInner.getDataAsString());
    }

    @Test
    public void read_overwritesExistingParams() throws IOException {
        DDMessage msg = new DDMessage(13);
        msg.setKey("sent-key");

        // receiving message starts with local defaults, which are replaced by what was sent
        DDMessage.setDefaultKey("receiver-key");
        DDMessage received = new DDMessage();
        received.read(new ByteArrayInputStream(writeBytes(msg)), 0);

        assertEquals("sent-key", received.getKey());
        assertEquals(13, received.getCategory());
    }

    @Test
    public void read_emptyStreamIsOkay() throws IOException {
        DDMessage msg = new DDMessage();
        msg.clear();
        msg.read(new ByteArrayInputStream(new byte[0]), 0);
        assertTrue(msg.isEmpty());
        assertEquals(0, msg.getNumData());
    }

    @Test
    public void read_truncatedHeaderFails() throws IOException {
        byte[] bytes = writeBytes(new DDMessage(14));
        byte[] truncated = Arrays.copyOf(bytes, bytes.length - 1); // lose last delimiter

        assertThrows(ApplicationError.class, () -> read(truncated));
    }

    @Test
    public void read_truncatedDataFails() throws IOException {
        byte[] bytes = writeBytes(new DDMessage(15, "some data here"));
        byte[] truncated = Arrays.copyOf(bytes, bytes.length - 3);

        assertThrows(ApplicationError.class, () -> read(truncated));
    }

    @Test
    public void read_onlyReadsOneMessage() throws IOException {
        // data sizes are sent, so read stops at end of message even if the stream has more
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DDMessage(16, "first").write(out);
        new DDMessage(17, "second").write(out);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        DDMessage first = new DDMessage();
        first.read(in, 0);
        DDMessage second = new DDMessage();
        second.read(in, 0);

        assertEquals(16, first.getCategory());
        assertEquals("first", first.getDataAsString());
        assertEquals(17, second.getCategory());
        assertEquals("second", second.getDataAsString());
    }

    // -------------------------------------------------------------------------
    // marshal / demarshal round trip
    // -------------------------------------------------------------------------

    @Test
    public void marshalDemarshal_roundTrip() {
        DDMessage msg = new DDMessage(20, "chunk one");
        msg.addData("chunk é two\n\n\n\nwith newlines");
        msg.setString("param", "a:b=c~d\\e");
        msg.setInteger("int", 123);

        String marshalled = msg.marshal(null);

        DDMessage read = new DDMessage();
        read.demarshal(null, marshalled);

        assertMessagesEqual(msg, read);
        assertEquals("chunk one", read.getDataAtAsString(0));
        assertEquals("chunk é two\n\n\n\nwith newlines", read.getDataAtAsString(1));
        assertEquals("a:b=c~d\\e", read.getString("param"));
        assertEquals(123, read.getInteger("int"));

        // marshal is stable
        assertEquals(marshalled, read.marshal(null));
    }

    @Test
    public void marshalDemarshal_viaDataMarshaller() {
        DDMessage msg = new DDMessage(21, "via marshaller");
        msg.setBoolean("flag", true);

        String marshalled = DataMarshaller.marshal(msg);
        assertEquals('M', marshalled.charAt(0));

        DDMessage read = assertInstanceOf(DDMessage.class, DataMarshaller.demarshal(marshalled));
        assertMessagesEqual(msg, read);
        assertEquals("via marshaller", read.getDataAsString());
        assertEquals(Boolean.TRUE, read.getBoolean("flag"));
    }

    @Test
    public void marshal_matchesWrite() throws IOException {
        DDMessage msg = new DDMessage(22, "same");
        assertEquals(Utils.decode(writeBytes(msg)), msg.marshal(null));
    }

    // -------------------------------------------------------------------------
    // debugging output
    // -------------------------------------------------------------------------

    @Test
    public void debugOutput() throws IOException {
        DDMessage empty = new DDMessage(30);
        empty.debugPrint();
        assertTrue(empty.toString().contains("DATA: (none)"));
        assertEquals("[NO DATA]", empty.toStringSize());
        assertFalse(empty.toString().contains("STATUS"));

        DDMessage msg = new DDMessage(31, "line one\nline two");
        msg.setStatus(DDMessageListener.STATUS_TIMEOUT);
        msg.addData(writeFile("debug.txt", "file"));
        msg.debugPrint(); // file chunk has no data until marshalled

        String full = msg.toString();
        assertTrue(full.startsWith("STATUS: " + DDMessageListener.STATUS_TIMEOUT));
        assertTrue(full.contains("line one\nline two"), full);
        assertTrue(msg.toStringParams().contains(PUBLIC_KEY));

        String brief = msg.toString(false);
        assertFalse(brief.contains("line one"), brief);
        assertTrue(brief.contains("DATA[0]: (17 bytes)"), brief);
        assertTrue(brief.contains("DATA[1]: (0 bytes)"), brief); // file data not loaded

        assertEquals("DATA[0]: (17 bytes), DATA[1]: (0 bytes)", msg.toStringSize());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private File writeFile(String name, String contents) throws IOException {
        File file = new File(tempDir, name);
        Files.write(file.toPath(), Utils.encode(contents));
        return file;
    }

    private static byte[] writeBytes(DDMessage msg) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.write(out);
        return out.toByteArray();
    }

    private static DDMessage read(byte[] bytes) throws IOException {
        DDMessage msg = new DDMessage();
        msg.clear(); // start empty so we only see what was sent
        msg.read(new ByteArrayInputStream(bytes), bytes.length);
        return msg;
    }

    private static DDMessage roundTrip(DDMessage msg) throws IOException {
        DDMessage read = read(writeBytes(msg));
        assertMessagesEqual(msg, read);
        return read;
    }

    private static void assertMessagesEqual(DDMessage expected, DDMessage actual) {
        assertEquals(expected.keySet(), actual.keySet());
        assertEquals(expected.getCategory(), actual.getCategory());
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getLocale(), actual.getLocale());
        assertEquals(expected.getCreateTimeStamp(), actual.getCreateTimeStamp());
        assertEquals(expected.getNumData(), actual.getNumData());

        // file chunks can't be compared directly (null until marshalled)
        for (int i = 0; i < expected.getNumData(); i++) {
            byte[] data = expected.getDataAt(i);
            if (data != null) assertArrayEquals(data, actual.getDataAt(i), "chunk " + i);
            else assertNotNull(actual.getDataAt(i), "chunk " + i);
        }
    }
}
