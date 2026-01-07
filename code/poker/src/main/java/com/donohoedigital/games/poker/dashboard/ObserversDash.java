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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 14, 2005
 * Time: 6:43:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObserversDash extends DashboardItem
{
    DDPanel obsPanel_;

    public ObserversDash(GameContext context)
    {
        super(context, "observers");
        trackTableEvents(PokerTableEvent.TYPE_OBSERVER_ADDED|PokerTableEvent.TYPE_OBSERVER_REMOVED);
        game_.addPropertyChangeListener(PokerGame.PROP_OBSERVERS, this);
    }

    protected JComponent createBody()
    {
        obsPanel_ = new DDPanel();
        obsPanel_.setLayout(new GridLayout(0, 1, 0, 2));
        return obsPanel_;
    }

    /**
     * track when game loaded, might need to update if sitting out
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        if (name.equals(PokerGame.PROP_OBSERVERS))
        {
            if (isDisplayed()) GuiUtils.invoke(updateRunner_);
        }
        super.propertyChange(evt);
    }

    // runnable for setting label text in swing thread
    private Runnable updateRunner_ = new Runnable()
                        {
                            public void run()
                            {
                                updateAll();
                            }
                        };

    /**
     * update observer list
     */
    protected void updateInfo()
    {
        PokerTable table = game_.getCurrentTable();
        obsPanel_.removeAll();

        // inline count - it can change during update
        if (table.getNumObservers() == 0)
        {
            obsPanel_.add(new DashObs(0, null));
        }
        else
        {
            PokerPlayer player;
            int cnt = 1;
            for (int i = 0; i < table.getNumObservers(); i++)
            {
                player = table.getObserver(i);
                if (player.getName() == null) continue; // could happen during load, b4 player object loaded
                obsPanel_.add(new DashObs(cnt++, player));
            }
        }

        obsPanel_.revalidate();
    }

    /**
     * class to represent an observer
     */
    private class DashObs extends DDLabel implements MouseListener
    {
        PokerPlayer p;

        DashObs(int num, PokerPlayer p)
        {
            super(GuiManager.DEFAULT, STYLE);
            this.p = p;

            if (p != null)
            {
                setText(PropertyConfig.getMessage(p.isWaiting() ? "msg.observers.name.wait":"msg.observers.name", 
                                                  num, Utils.encodeHTML(p.getName())));
            }
            else
            {
                setText(PropertyConfig.getMessage("msg.observers.none"));
            }

            addMouseListener(this);
        }


        public void mouseReleased(MouseEvent e)
        {
            if (!GuiUtils.isPopupTrigger(e, false) || p == null) return;

            DDPopupMenu menu = new DDPopupMenu();

            DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, "PopupMenu");
            title.setText(PropertyConfig.getMessage("menuitem.observers.title", Utils.encodeHTML(p.getName())));
            title.setDisplayMode(DDMenuItem.MODE_TITLE);
            menu.add(title);

            if (!p.isLocallyControlled())
            {
                PokerPrefsPlayerList muted = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_MUTE);
                PokerPrefsPlayerList banned = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_BANNED);
                OnlineManager mgr = game_.getOnlineManager();
                menu.add(new ShowTournamentTable.MutePlayer("PokerTable", p, muted.containsPlayer(p.getName(), p.getKey()), mgr, false));
                menu.add(new ShowTournamentTable.BanPlayer(context_, "PokerTable", p, banned.containsPlayer(p.getName(), p.getKey()), mgr, mgr, false, mgr.isHost()));
            }

            menu.show(this, e.getX(), e.getY());
        }

        public void mouseClicked(MouseEvent e) { }
        public void mousePressed(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e)  { }
    }
}
