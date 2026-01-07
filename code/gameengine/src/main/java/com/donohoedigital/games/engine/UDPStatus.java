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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.udp.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.Timer;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 19, 2006
 * Time: 3:04:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UDPStatus extends BasePhase implements DDTable.TableMenuItems
{
    static Logger logger = LogManager.getLogger(UDPStatus.class);

    private DDPanel base_;

    private static UDPStatus STATUS;

    private UDPServer udp_;
    private UDPModel model_;
    private DDScrollTable statsScroll_;
    private DDTable table_;
    private Timer timer_;
    private DDHtmlArea text_;

    private boolean bRunning_ = false;


    /**
     * Set new UDP
     */
    public static void setUDPServer(UDPServer udp)
    {
        if (STATUS == null) return;

        if (STATUS.udp_ != null)
        {
            STATUS.udp_.manager().removeMonitor(STATUS.model_);
        }
        STATUS.udp_ = udp;

        if (STATUS.udp_ != null)
        {
            STATUS.udp_.manager().addMonitor(STATUS.model_);
            STATUS.model_.update();
        }
    }

    /**
     * init
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        STATUS = this;

        String STYLE = gamephase_.getString("style", "default");
        String BEVEL_STYLE = gamephase_.getString("bevel-style", STYLE);

        // base
        base_ = new DDPanel();
        base_.setBorderLayoutGap(0, 0);
        base_.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 2));

        // info
        text_ = new DDHtmlArea(GuiManager.DEFAULT, "UDPStatus");
        text_.setDisplayOnly(true);
        text_.setBorder(BorderFactory.createEmptyBorder(2,4,2,20));
        text_.setAlwaysAntiAlias(true);
        text_.setText("&nbsp;");

        // status table
        DDPanel left = new DDPanel();
        left.setBorderLayoutGap(3, 0);

        ///
        /// player list table
        ///
        DDPanel statsbase = new DDPanel();
        statsScroll_ = new DDScrollTable(GuiManager.DEFAULT, STYLE, BEVEL_STYLE, COLUMN_NAMES, COLUMN_WIDTHS);
        statsScroll_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        statsScroll_.setPreferredSize(new Dimension(statsScroll_.getPreferredWidth(), COLUMN_WIDTHS[0]));
        statsbase.add(statsScroll_, BorderLayout.CENTER);

        table_ = statsScroll_.getDDTable();
        model_ = new UDPModel();
        table_.setFocusable(false);
        table_.setTableMenuItems(this);
        table_.setExporter(new TableExporter(context_, "udpstatus"));
        table_.setSelectOnRightClick(true);
        table_.setModel(model_);
        table_.setShowHorizontalLines(true);
        table_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int i = 2; i < COLUMN_NAMES.length; i++)
        {
            table_.setAlign(i, SwingConstants.RIGHT);
        }
        table_.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // put it together
        base_.add(statsbase, BorderLayout.CENTER);
        base_.add(text_, BorderLayout.SOUTH);
    }

    /**
     * Start of phase
     */
    public void start()
    {
        // if users presses launch button again, this will be called.  don't run logic again in this case
        if (bRunning_) return;
        bRunning_ = true;

        // set help text
        //context_.getWindow().setHelpTextWidget(text_);

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, base_, true, table_);

        // timer
        timer_ = new Timer("UDPStatusTimer", false);
        timer_.scheduleAtFixedRate(new UpdateTask(), 1000, 1000);

        // update
        setUDPServer(engine_.getUDPServer());
    }

    /**
     * time task
     */
    private class UpdateTask extends TimerTask
    {
        public void run()
        {
            model_.update();
        }
    }

    /**
     * finish
     */
    public void finish()
    {
        bRunning_ = false;
        timer_.cancel();
        timer_ = null;

        super.finish();
    }

    // column names
    private static String COL_LINK = "udp.link";
    private static String COL_REMOTE = "udp.remote";
    private static String COL_MTU = "udp.mtu";
    private static String COL_AVG = "udp.avg";
    private static String COL_TIME = "udp.time";
    private static String COL_SEND = "udp.send";
    private static String COL_RESEND = "udp.resend";
    private static String COL_RECEIVE = "udp.receive";
    private static String COL_DUP = "udp.dup";
    private static String COL_PKTSNT = "udp.pktsnt"; // not used for now
    private static String COL_PKTERR = "udp.pkterr"; // not used for now
    private static String COL_PKTRCV = "udp.pktrcv"; // not used for now
    private static String COL_BYTESIN = "udp.bytesin";
    private static String COL_BYTESOUT = "udp.bytesout";

    // client table info
    private static final int[] COLUMN_WIDTHS = new int[] {
        110, 150, 40, 70, 35, 50, 45,
        50, 30, 70, 70,
    };
    private static final String[] COLUMN_NAMES = new String[] {
        COL_LINK, COL_REMOTE, COL_MTU, COL_TIME, COL_AVG, COL_SEND, COL_RESEND,
        COL_RECEIVE, COL_DUP, COL_BYTESIN, COL_BYTESOUT

    };

    /**
     * Used by table to display players in game
     */
    private class UDPModel extends DefaultTableModel implements UDPManagerMonitor
    {
        private ArrayList<UDPLink> list = new ArrayList<UDPLink>();

        public UDPModel()
        {
        }

        public void monitorEvent(UDPManagerEvent event)
        {
            update();
        }

        private void update()
        {
            // skip repaint if nothing changed
            if (udp_ == null && list.size() == 0) return;

            // get list
            list.clear();
            if (udp_ != null) udp_.manager().getLinks(list);

            // sort
            Collections.sort(list, LINK_COMPARATOR);

            // table changed
            GuiUtils.invoke(new Runnable() {
                public void run() {
                    updateSwing();
                }
            });
        }

        private void updateSwing()
        {
            UDPManager mgr = (udp_ != null) ? udp_.manager() : null;
            if (mgr != null)
            {
                MovingAverage in = mgr.getBytesInMovingAverage();
                MovingAverage out = mgr.getBytesOutMovingAverage();
                int cnt = mgr.getMessageOnOutGoingQueue();
                String sMsg = PropertyConfig.getMessage("msg.udp.stats",
                                                        Utils.formatSizeBytes(in.getAverageLong()),
                                                        Utils.formatSizeBytes(in.getHigh()),
                                                        Utils.formatSizeBytes(in.getPeak()),
                                                        Utils.formatSizeBytes(out.getAverageLong()),
                                                        Utils.formatSizeBytes(out.getHigh()),
                                                        Utils.formatSizeBytes(out.getPeak()),
                                                        Utils.formatSizeBytes(mgr.getBytesOnOutgoingQueue()),
                                                        cnt,
                                                        PropertyConfig.getMessage(cnt == 1 ? "msg.udp.msg.singular":"msg.udp.msg.plural"),
                                                        mgr.getPeakMessageOnOutGoingQueue()
                );
                text_.setText(sMsg);
            }
            else
            {
                text_.setText("");
            }

            fireTableDataChanged();
        }

        public String getColumnName(int c) {
            return COLUMN_NAMES[c];
        }

        public int getColumnCount() {
            return COLUMN_WIDTHS.length;
        }

        public boolean isCellEditable(int r, int c) {
            return false;
        }

        public int getRowCount() {
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        public Object getValueAt(int rowIndex, int colIndex)
        {
            UDPLink link = getLink(rowIndex);
            if (link == null) return "";

            UDPLink.UDPStats stats = link.getStats();

            if (COLUMN_NAMES[colIndex].equals(COL_LINK))
            {
                return link.getName();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_REMOTE))
            {
                return Utils.getAddressPort(link.getRemoteIP());
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_MTU))
            {
                return link.getMTU();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_TIME))
            {
                return Utils.getTimeString(link.getTimeConnected(), false);
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_AVG))
            {
                return stats.getAverage();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_SEND))
            {
                return stats.getDataOut();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_RESEND))
            {
                return stats.getDataResend();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_RECEIVE))
            {
                return stats.getDataIn();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_DUP))
            {
                return stats.getDataDups();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_PKTSNT))
            {
                return stats.getPacketSent();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_PKTERR))
            {
                return stats.getPacketSendErrors();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_PKTRCV))
            {
                return stats.getPacketReceived();
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_BYTESIN))
            {
                return Utils.formatSizeBytes(stats.getBytesIn());
            }
            else if (COLUMN_NAMES[colIndex].equals(COL_BYTESOUT))
            {
                return Utils.formatSizeBytes(stats.getBytesOut());
            }


            return "[bad column]";
        }

        private UDPLink getLink(int rowIndex)
        {
            if (rowIndex >= list.size()) return null; // check in case of paint after update
            return list.get(rowIndex);
        }
    }

    public static final UDPLinkComparator LINK_COMPARATOR = new UDPLinkComparator();

    /**
     * link comprator for displaying status
     */
    private static class UDPLinkComparator implements Comparator<UDPLink>
    {
        public int compare(UDPLink link1, UDPLink link2)
        {
            if (link1.getName().startsWith("Chat"))
            {
                return -1;
            }
            else
            {
                return (link1.getName().compareToIgnoreCase(link2.getName()));
            }
        }
    }

//    /**
//     * get selected player
//     */
//    private UDPLink getSelectedPlayer(DDTable table)
//    {
//        int n = table.getSelectedRow();
//        if (n < 0) return null;
//        return model_.getPlayer(n);
//    }

    ////
    //// Table menu interface
    ////

    public boolean isItemsToBeAdded(DDTable table)
    {
        return false; //getSelectedPlayer(table) != null;
    }

    //private static ImageIcon infoIcon_ = ImageConfig.getImageIcon("menuicon.info");

    public void addMenuItems(DDTable table, DDPopupMenu menu)
    {

    }
}
