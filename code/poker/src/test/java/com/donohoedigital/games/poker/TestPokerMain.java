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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.network.PokerConnectionServer;

/**
 * A PokerMain that never opens a socket.
 * <p/>
 * The constructor chain is cheap: BaseApp's headless constructor only assigns fields (the
 * Mac Desktop setup is guarded by !bHeadless), GameEngine's sets the engine_ singleton, and
 * all the heavy work - the main window, themes, prefs, license keys - lives in init(),
 * which is never called here.
 * <p/>
 * Only the three server accessors are overridden, so the routing in
 * getPokerConnectionServer(PokerConnection) remains the production code under test.
 * <p/>
 * Constructing this DOES set the GameEngine singleton for the life of the JVM, and it is
 * never cleared, which is why AbstractPokerTest builds exactly one and hands it out.
 */
public class TestPokerMain extends PokerMain
{
    private FakePokerConnectionServer udp_ = FakePokerConnectionServer.udp();
    private FakePokerConnectionServer tcp_ = FakePokerConnectionServer.tcp();
    private PokerConnectionServer game_ = tcp_;

    TestPokerMain()
    {
        super("poker", "poker", new String[0], true /* headless */, false /* loadNames */);
    }

    /**
     * Fresh servers, so one test cannot see what another sent.  Game transport defaults to
     * TCP, which is what a hosted game actually uses unless testing turns UDP on.
     */
    public void reset()
    {
        udp_ = FakePokerConnectionServer.udp();
        tcp_ = FakePokerConnectionServer.tcp();
        game_ = tcp_;
    }

    public FakePokerConnectionServer getFakeUDP()
    {
        return udp_;
    }

    public FakePokerConnectionServer getFakeTCP()
    {
        return tcp_;
    }

    /**
     * Which transport the game itself is on - what p2p_ would hold in production.
     */
    public void setGameTransport(PokerConnectionServer p2p)
    {
        game_ = p2p;
    }

    ////
    //// PokerMain overrides - hand out the fakes instead of binding anything
    ////

    @Override
    PokerConnectionServer udpServer()
    {
        return udp_;
    }

    @Override
    PokerConnectionServer tcpServer()
    {
        return tcp_;
    }

    @Override
    PokerConnectionServer gameServer()
    {
        return game_;
    }

    @Override
    public PokerConnectionServer getPokerConnectionServer(boolean bUDP)
    {
        // the production version creates and shuts down real servers
        return bUDP ? udp_ : tcp_;
    }

    @Override
    public void shutdownPokerConnectionServer(PokerConnectionServer p2p)
    {
        // nothing to shut down - keeps OnlineManager.finish() safe in a test
    }
}
