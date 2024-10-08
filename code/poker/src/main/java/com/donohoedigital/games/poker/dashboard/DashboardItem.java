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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 17, 2005
 * Time: 9:01:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class DashboardItem implements Comparable<DashboardItem>, DataMarshal,
                                      AncestorListener, PokerTableListener,
                                      PropertyChangeListener
{
    static Logger logger = LogManager.getLogger(DashboardItem.class);

    // statics
    public static final int NOT_SET = -1;

    // members
    private String sName_;
    private DashboardHeader header_;
    private JComponent body_;
    private boolean bInDashboard_ = true;
    private boolean bUiCreated_ = false;
    private boolean bOpen_ = false;
    private DashboardPanel dashboard_;
    private JComponent editor_;
    private int nPosition_ = NOT_SET;
    //private boolean bTableEventsInSwing_ = true;
    private boolean bDynamicTitle_ = false;

    // protected
    protected String STYLE = "DashboardItem";
    protected String STYLE_BIGGER = "DashboardItemBigger";
    protected PokerGame game_;
    protected GameContext context_;

    // table we are tracking and events we are interested in
    private PokerTable table_;
    private int nTableEvents_ = 0;

    /**
     * Create new dashboard item
     */
    public DashboardItem(GameContext context, String sName)
    {
        sName_ = sName;
        context_ = context;
        game_ = (PokerGame) context_.getGame();
    }

    /**
     * finish - does nothing, for subclasses
     */
    public void finish()
    {
    }

    /**
     * Get name
     */
    public String getName()
    {
        return sName_;
    }

    /**
     * debug
     */
    @Override
    public String toString()
    {
        return sName_ + " at position " + nPosition_;
    }

    /**
     * Get position
     */
    public int getPosition()
    {
        return nPosition_;
    }

    /**
     * Set position
     */
    public void setPosition(int n)
    {
        nPosition_ = n;
    }

    /**
     * Is this displayed in the dashboard and the ui created.
     */
    public boolean isDisplayed()
    {
        return isInDashboard() && bUiCreated_;
    }

    /**
     * Is this displayed in the dashboard?
     */
    public boolean isInDashboard()
    {
        return bInDashboard_;
    }

    /**
     * Set whether this is displayed in dashboard
     */
    public void setInDashboard(boolean b)
    {
        bInDashboard_ = b;
        if (!b)
        {
            if (dashboard_ != null) dashboard_.itemRemoveRequested(DashboardItem.this);
            bOpen_ = false; // removed, set closed for re-open (just set flag for perf)
        }
    }

    /**
     * Set displayed flag in editor mode w/out changing display
     */
    void setInDashboardEditor(boolean b)
    {
        bInDashboard_ = b;
    }

    /**
     * Is this panel open?
     */
    public boolean isOpen()
    {
        return bOpen_;
    }

    /**
     * Set this panel open
     */
    public void setOpen(boolean b)
    {
        bOpen_ = b;
        if (body_ != null && body_.isVisible() != b) body_.setVisible(b);
        if (header_ != null && header_.check_.isSelected() != b) header_.check_.setSelected(b);
        if (dashboard_ != null) dashboard_.itemOpenClose(DashboardItem.this);
    }

    /**
     * create ui
     *
     * @param dashboard
     */
    void createUI(DashboardPanel dashboard)
    {
        dashboard_ = dashboard;
        if (header_ == null)
        {
            header_ = new DashboardHeader("DashboardHeader", false);
            header_.setText(getTitle());
            header_.addAncestorListener(this);
            header_.check_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    setOpen(header_.check_.isSelected());
                }
            });

            header_.delete_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    setInDashboard(header_.delete_.isSelected());
                }
            });
        }

        if (body_ == null)
        {
            body_ = new DDPanel();
            body_.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            body_.add(createBody(), BorderLayout.CENTER);
            body_.addAncestorListener(this);
        }

        // ui is all nice and created
        bUiCreated_ = true;

        // make sure open setting matches what we think we have
        setOpen(bOpen_);
    }

    /**
     * Get title, taking into account dynamic title
     */
    protected String getTitle()
    {
        Object param = null;
        String sExtra = "";
        if (bDynamicTitle_ && !isOpen())
        {
            param = getDynamicTitleParam();
            if (param != null) sExtra = ".2";
        }
        return PropertyConfig.getMessage("dashboard." + sName_ + sExtra, param);
    }

    /**
     * get object to be passed to dynamic title message
     */
    protected Object getDynamicTitleParam()
    {
        return null;
    }


    /**
     * call to manually set title
     */
    public void setTitle(String s)
    {
        header_.setText(s);
    }

    /**
     * Subclasses should implement this
     */
    @SuppressWarnings({"UnsecureRandomNumberGeneration"})
    protected JComponent createBody()
    {
        // create a basic panel for testing
        DDPanel panel = new DDPanel();
        panel.setPreferredSize(new Dimension(100, (40 + (int) (30 * Math.random()))));
        return panel;
    }

    /**
     * Update title and header
     */
    protected void updateAll()
    {
        // things get removed when restarting, so ignore updates
        if (!game_.isFinished())
        {
            updateInfo();
            updateTitle();
        }
    }

    /**
     * subclass should override this to update the UI components.
     * When dynamic titles in use, will be called even when
     * body is hidden
     */
    protected void updateInfo()
    {
    }

    /**
     * updates title by calling setTitle(geTitle()).  Called
     * when dynamic titles in use, after updateInfo
     */
    protected void updateTitle()
    {
        if (bDynamicTitle_) setTitle(getTitle());
    }

    /**
     * called when displayed in dashboard, by default
     * calls updateInfo()
     */
    protected void bodyDisplayed()
    {
        updateAll();
    }

    /**
     * called when body hidden (not removed), by default
     * calls updateInfo() if dynamic titles in use
     */
    protected void bodyHidden()
    {
        if (bDynamicTitle_) updateAll();
    }

    /**
     * called when header displayed, by default
     * calls updateInfo() if dynamic titles in use and
     * body not open
     */
    protected void headerDisplayed()
    {
        if (!isOpen() && bDynamicTitle_) updateAll();
    }

    /**
     * called when header hidden
     */
    protected void headerHidden()
    {
    }

    /**
     * called when poker table changed (if listening to table events
     * turned on with trackTableEvents()).  By default, calls updateInfo()
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void tableChanged(PokerTable newTable)
    {
        if (isDisplayed()) updateAll();
    }

    /**
     * subclass should implement (if listening to table events
     * turned on with trackTableEvents()).  By default, calls updateInfo()
     */
    public void tableEventOccurred(PokerTableEvent event)
    {
        if (isDisplayed()) updateAll();
    }

    /**
     * get header
     */
    public JComponent getHeader()
    {
        return header_;
    }

    /**
     * get body
     */
    public JComponent getBody()
    {
        return body_;
    }

    /**
     * for use by DashboardEditorDialog
     */
    void setEditor(JComponent e)
    {
        editor_ = e;
    }

    /**
     * Get editor
     */
    JComponent getEditor()
    {
        return editor_;
    }

    /**
     * sort based on position
     */
    public int compareTo(DashboardItem i)
    {
        return nPosition_ - i.nPosition_;
    }

    /**
     * marshal to string for prefs
     */
    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(bOpen_);
        list.addToken(bInDashboard_);
        list.addToken(nPosition_);
        return list.marshal(state);
    }

    /**
     * demarshal from string prefs
     */
    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        bOpen_ = list.removeBooleanToken();
        bInDashboard_ = list.removeBooleanToken();
        nPosition_ = list.removeIntToken();
    }

    public void ancestorAdded(AncestorEvent event)
    {
        if (event.getSource() == body_) bodyDisplayed();
        if (event.getSource() == header_) headerDisplayed();
    }

    public void ancestorMoved(AncestorEvent event)
    {
    }

    public void ancestorRemoved(AncestorEvent event)
    {
        if (event.getSource() == body_) bodyHidden();
        if (event.getSource() == header_) headerHidden();
    }

    // runnable for invoking table changed event in swing thread
    private Runnable tableChangedRunner_ = new Runnable()
    {
        public void run()
        {
            tableChanged(game_.getCurrentTable());
        }
    };

    /**
     * Game property changed - we track current table
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        // table changed
        if (name.equals(PokerGame.PROP_CURRENT_TABLE))
        {
            trackTable(game_.getCurrentTable());

            // make sure tableChanged() is called from swing thread
            GuiUtils.invoke(tableChangedRunner_);
        }
    }

    // only one needed for all instances  TODO: move to ulility
    private static final java.util.Timer timer = new java.util.Timer("Dashboard-Timer", true);

    /**
     * inner class for listener to poker table events.  When received,
     * invoke in timer thread (to get out of any locks held by
     * callers like TournamentDirector).
     */
    private PokerTableListener listener_ = new PokerTableListener()
    {
        public void tableEventOccurred(final PokerTableEvent event)
        {
            // run immediately
            timer.schedule(new TableEventTask(event), 0);
        }
    };

    /**
     * Task for particular poker table event.  When invoked, runs immediately
     * in swing thread using invokeAndWait.
     */
    private class TableEventTask extends TimerTask
    {
        private PokerTableEvent event;

        private TableEventTask(PokerTableEvent event)
        {
            this.event = event;
        }

        @Override
        public void run()
        {
            GuiUtils.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    DashboardItem.this.tableEventOccurred(event);
                }
            });
        }
    }

    /**
     * subclass should call during constructor to
     * indicate it is interested in tracking table
     * events
     */
    protected void trackTableEvents(int nEvents)
    {
        ApplicationError.assertTrue(nTableEvents_ == 0, "trackTableEvents called previously");
//        bSetTableEventsImmediateNotAllowed_ = true;
        nTableEvents_ = nEvents;
        game_.addPropertyChangeListener(PokerGame.PROP_CURRENT_TABLE, this);
        trackTable(game_.getCurrentTable());
    }

    /**
     * track table
     */
    private void trackTable(PokerTable table)
    {
//        PokerTableListener listener = bTableEventsInSwing_ ? listener_ : this;
        PokerTableListener listener = listener_;

        // cleanup old
        if (table_ != null) table_.removePokerTableListener(listener, PokerTableEvent.TYPES_ALL);

        // store new and add listener if different
        if (table_ != table)
        {
            table_ = table;
            if (table_ != null)
            {
                table_.addPokerTableListener(listener, nTableEvents_);
            }
        }
    }

    // DESIGN NOTE:   Removed this when changed listener_ above to use invokeAndWait so all listeners notified
    // immediately (3.0p1)
//    private boolean bSetTableEventsImmediateNotAllowed_ = false;
//
//    /**
//     * Indicate that table events should be delivered immediately.
//     * The default is to use invokeLater and put them in the Swing thread,
//     * but in some cases that is undesirable because the table
//     * can change state during the time between when a poker table
//     * event is generated and when the swing runnable runs.
//     * This should only be called from a constructor and
//     * before trackTableEvents() is called.
//     */
//    protected void setTableEventsImmediate()
//    {
//        ApplicationError.assertTrue(!bSetTableEventsImmediateNotAllowed_,
//                                    "Can't call after trackTableEvents called");
//
//        // TODO:  testing to see if invokeNow removes need for this method
//        // bTableEventsInSwing_ = false;
//    }

    /**
     * Indicate whether the title is dynamically updated
     */
    protected void setDynamicTitle(boolean b)
    {
        bDynamicTitle_ = b;
    }

    /**
     * Is title dynamic?
     */
    protected boolean isDynamicTitle()
    {
        return bDynamicTitle_;
    }

    /**
     * Get preferred body height.
     */
    public int getPreferredBodyHeight()
    {
        return 0;
    }
}
