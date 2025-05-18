/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
/*
 * DescriptionDialog.java
 *
 * Created on April 29, 2004, 02:18 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.db.BindArray;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.engine.FileChooserDialog;
import com.donohoedigital.games.poker.impexp.ImpExp;
import com.donohoedigital.games.poker.impexp.ImpExpHand;
import com.donohoedigital.games.poker.impexp.ImpExpParadise;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class HistoryExportDialog extends FileChooserDialog
{
    public static String PARAM_HAND_ID = "handID";
    
    private PokerDatabase.ExportSummary exp_;

    private JComponent base_;

    private DDCheckBox cbxHands_;
    private DDCheckBox cbxTournaments_;

    private DDProgressBar progressBar_;

    private DDTextField txtSize_;

    private boolean cancel_ = false;

    private static final javax.swing.filechooser.FileFilter filterParadise_ =
        new javax.swing.filechooser.FileFilter()
        {
            public boolean accept(File f) { return !f.isFile() || f.getName().endsWith(".paradise.txt"); }
            public String getDescription() { return "Paradise Poker (*.paradise.txt)"; }
        };

    public JComponent createTopPanel()
    {
        String sStyle = gamephase_.getString("style");

        DDLabel lblExport = new DDLabel("export", sStyle);
        DDLabel lblSize = new DDLabel("estfilesize", sStyle);

        cbxHands_ = new DDCheckBox();
        cbxTournaments_ = new DDCheckBox();

        txtSize_ = new DDTextField("estfilesize", sStyle);

        lblSize.setHorizontalAlignment(DDTextField.RIGHT);

        txtSize_.setHorizontalAlignment(DDTextField.RIGHT);
        txtSize_.setEditable(false);
        txtSize_.setOpaque(false);

        DDPanel left = new DDPanel();

        ((BorderLayout)left.getLayout()).setHgap(8);

        left.add(lblExport, BorderLayout.WEST);
        left.add(cbxHands_, BorderLayout.CENTER);

        DDPanel right = new DDPanel();

        ((BorderLayout)right.getLayout()).setHgap(8);

        right.add(lblSize, BorderLayout.WEST);
        right.add(txtSize_, BorderLayout.CENTER);

        DDPanel top = new DDPanel();

        ((BorderLayout)top.getLayout()).setHgap(32);
        top.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.CENTER);

        return top;
    }

    public JComponent createBottomPanel()
    {
        String sStyle = gamephase_.getString("style");

        progressBar_ = new DDProgressBar(GuiManager.DEFAULT, sStyle, false);
        progressBar_.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        progressBar_.setPreferredWidth(500);
        progressBar_.setPreferredHeight(30);
        progressBar_.setVisible(false);

        return GuiUtils.CENTER(progressBar_);
    }

    public JComponent createDialogContents()
    {
        String sStyle = gamephase_.getString("style");

        base_ = super.createDialogContents();

        choose_.resetChoosableFileFilters();
        choose_.addChoosableFileFilter(filterParadise_);

        final DDLabel wait = new DDLabel("waitexport", sStyle);

        wait.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        wait.setPreferredSize(choose_.getPreferredSize());

        base_.remove(choose_);
        base_.add(wait, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                Integer handID = (Integer)gamephase_.getObject(PARAM_HAND_ID);

                if (handID != null)
                {
                    exp_ = new PokerDatabase.ExportSummary();
                    exp_.handIDs = new ArrayList();
                    exp_.handIDs.add(handID);
                    exp_.tournamentCount = 1;
                }
                else
                {
                    String where = (String)gamephase_.getObject("where");
                    BindArray bindArray = (BindArray)gamephase_.getObject("bindArray");

                    exp_ = PokerDatabase.getExportSummary(where, bindArray);
                }

                ActionListener actionListener = new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            checkButtons();
                        }
                    };

                cbxHands_.setText(PropertyConfig.getMessage(
                                    "msg.handtranscriptcount" + (exp_.handIDs.size() != 1 ? ".plural" : ".singular"),
                                    exp_.handIDs.size()));
                cbxHands_.setSelected(true);

                cbxHands_.addActionListener(actionListener);

                cbxTournaments_.setText(PropertyConfig.getMessage(
                                    "msg.tourneysummarycount" + (exp_.tournamentCount != 1 ? ".plural" : ".singular"),
                                    exp_.tournamentCount));

                cbxTournaments_.addActionListener(actionListener);

                base_.remove(wait);
                base_.add(choose_, BorderLayout.CENTER);

                checkButtons();
            }
        });

        return base_;
    }

    protected void checkButtons()
    {
        super.checkButtons();

        if (txtSize_ != null) txtSize_.setText(String.valueOf(Utils.formatSizeBytes((int)(
                (cbxHands_.isSelected() ? (1500L * exp_.handIDs.size()) : 0)
                ))));

        if ((okayButton_ != null) && (cbxHands_ != null) && (cbxTournaments_ != null)) okayButton_.setEnabled(
                okayButton_.isEnabled() &&
                (cbxHands_.isSelected() || cbxTournaments_.isSelected()));

//       cancelButton_.setEnabled(true);
    }

    /**
     * Focus to text field
     *
    protected Component getFocusComponent()
    {
        return desc_;
    }
    */

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) 
    {
        if (super.processButton(button, false) && (getResult() instanceof File))
        {
            getMatchingButton("export").setEnabled(false);

            progressBar_.setVisible(true);

            new Thread("HistoryExportDialog")
            {
                public void run()
                {
                    ArrayList summariesExported = new ArrayList();

                    ImpExpHand ieHand;

                    ImpExp ie = new ImpExpParadise();

                    ie.setPlayerName(PlayerProfileOptions.getDefaultProfile().getName());

                    try
                    {
                        File file = (File)getResult();
                        String path = file.getAbsolutePath();

                        Object filter = choose_.getFileFilter();

                        if (filter == filterParadise_)
                        {
                            if (!path.endsWith(".paradise.txt"))
                            {
                                path = path + ".paradise.txt";
                            }
                        }

                        Writer w = new FileWriter(path);

                        for (int i = 0; i < exp_.handIDs.size() && !cancel_; ++i)
                        {
                            ieHand = PokerDatabase.getHandForExport((Integer) exp_.handIDs.get(i));

                            if (cbxTournaments_.isSelected() &&
                                (summariesExported.size() < exp_.tournamentCount) &&
                                !summariesExported.contains(ieHand.tournamentID))
                            {
                                w.write(ie.exportTournament(ieHand));
                                w.write("\r\n\r\n");
                                summariesExported.add(ieHand.tournamentID);
                            }

                            if (cbxHands_.isSelected())
                            {
                                w.write(ie.exportHand(ieHand));
                                w.write("\r\n\r\n");
                            }

                            progressBar_.setPercentDone((i * 100) / exp_.handIDs.size());
                        }

                        w.close();
                    }
                    catch (IOException e)
                    {
                        throw new ApplicationError(e);
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                removeDialog();
                            }
                        });
                    }
                }
            }.start();
        }
        else
        {
            removeDialog();
            cancel_ = true;
        }

        return true;
    }

    protected String fixName(String sName)
    {
        return sName;
    }
}
