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
package com.donohoedigital.games.poker;

import com.donohoedigital.comms.*;

import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

@DataCoder('c')
public class GameClock extends Timer implements ActionListener, DataMarshal
{
    private static final int ACTION_TICK = 0;
    private static final int ACTION_START = 1;
    private static final int ACTION_STOP = 2;
    private static final int ACTION_SET = 3;

    // time remaining
    private long nMillisRemaining_;

    // transient
    private long nTickBegin_;
    private boolean bFlash_;
    private boolean bPaused_;

    public GameClock()
    {
        super(1000, null);
        addActionListener(this);
    }
    
    public void setFlash(boolean b)
    {
        bFlash_ = b;
    }
    
    public boolean isFlash()
    {
        return bFlash_;
    }

    public int getSecondsRemaining()
    {
        return (int)(getMillis() / 1000);
    }

    public boolean isExpired() {
        return (getMillis() == 0);
    }

    public synchronized void setSecondsRemaining(int n)
    {
        nTickBegin_ = System.currentTimeMillis();
        setMillis(n * 1000);
        this.fireActionPerformed(new ActionEvent(this, ACTION_SET, null, System.currentTimeMillis(), 0));
    }
    
    private synchronized void setMillis(long n)
    {
        nMillisRemaining_ = n;
    }
    
    private synchronized long getMillis()
    {
        return nMillisRemaining_;
    }

    public void pause()
    {
        bPaused_ = true;
        stop();
    }

    public void unpause()
    {
        bPaused_ = false;
        start();
    }

    public boolean isPaused()
    {
        return bPaused_;
    }

    public void start()
    {
        if (!isRunning())
        {
            nTickBegin_ = System.currentTimeMillis();
            super.start();
            this.fireActionPerformed(new ActionEvent(this, ACTION_START, null, System.currentTimeMillis(), 0));
        }
    }

    public void stop()
    {
        if (isRunning())
        {
            super.stop();
            this.fireActionPerformed(new ActionEvent(this, ACTION_STOP, null, System.currentTimeMillis(), 0));
        }
    }

    public void toggle()
    {
        if (isRunning())
        {
            stop();
        }
        else
        {
            start();
        }
    }

    public synchronized void actionPerformed(ActionEvent e)
    {
        if (e.getID() == ACTION_TICK)
        {
            long now = System.currentTimeMillis();
            long elapsed = now - nTickBegin_;
            if (elapsed >= getMillis())
            {
                setMillis(0);
                stop();
            }
            else
            {
                setMillis(getMillis() - elapsed);
            }
            nTickBegin_ = now;
        }

        Object[] listeners = listenerList.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == GameClockListener.class)
            {
                switch (e.getID())
                {
                    case ACTION_SET:
                        ((GameClockListener) listeners[i + 1]).gameClockSet(this);
                        break;
                    case ACTION_START:
                        ((GameClockListener) listeners[i + 1]).gameClockStarted(this);
                        break;
                    case ACTION_STOP:
                        ((GameClockListener) listeners[i + 1]).gameClockStopped(this);
                        break;
                    case ACTION_TICK:
                        ((GameClockListener) listeners[i + 1]).gameClockTicked(this);
                        break;
                }
            }
        }

    }

    public void addGameClockListener(GameClockListener listener)
    {
        listenerList.add(GameClockListener.class, listener);
    }

    public void removeGameClockListener(GameClockListener listener)
    {
        listenerList.remove(GameClockListener.class, listener);
    }

    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        nMillisRemaining_ = list.removeLongToken();
        if (list.removeBooleanToken()) // isRunning()
        {
            start();
        }
    }

    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(nMillisRemaining_);
        list.addToken(isRunning());
        return list.marshal(state);
    }

}