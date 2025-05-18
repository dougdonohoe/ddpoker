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
 * PlayerProfileDialog.java
 *
 * Created on January 25, 2003, 10:11 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * @author Doug Donohoe
 */
public class PlayerProfileDialog extends DialogPhase implements PropertyChangeListener
{
    static Logger logger = LogManager.getLogger(PlayerProfileDialog.class);

    public static final int PLAYER_NAME_LIMIT = 15;
    public static final String PLAYER_NAME_REGEXP = "^.+$";

    private PlayerProfile profile_;

    private OnlinePanel onlinePanel_;
    private DDRadioButton noRadio_, newRadio_, existRadio_;
    private OnlinePanel.TextWidgets nameWidgets_;
    private OnlinePanel.TextWidgets emailWidgets_;
    private OnlinePanel.TextWidgets passwordWidgets_;
    private DDPanel buttonPanel_;
    private DDButton emailButton_;
    private DDButton passwordButton_;
    private DDButton sendButton_;
    private DDButton resetButton_;
    private DDButton syncButton_;
    private boolean bEmailMode_;
    private boolean bPasswordMode_;

    /**
     * create chat ui
     */
    @Override
    public JComponent createDialogContents()
    {
        // Use original profile information to determine how to update values.
        profile_ = (PlayerProfile) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");

        // see if called from start menu (very first profile creation)
        boolean bStartMenuMode = gamephase_.getBoolean("startmenu", false);

        // contents
        DDPanel base = new DDPanel();
        BorderLayout layout = (BorderLayout) base.getLayout();
        layout.setVgap(15);
        layout.setHgap(5);
        base.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (bStartMenuMode)
        {
            DDPanel welpanel = new DDPanel();
            welpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            layout = (BorderLayout) welpanel.getLayout();
            layout.setVgap(10);
            base.add(welpanel, BorderLayout.NORTH);

            DDLabel welcome = new DDLabel("newplayer.welcome", STYLE);
            welcome.setIcon(ImageConfig.getImageIcon("pokericon48"));
            welpanel.add(welcome, BorderLayout.NORTH);

            DDLabel msg = new DDLabel("newplayer.msg", STYLE);
            welpanel.add(msg, BorderLayout.CENTER);
        }

        // add standard fields
        DDPanel fieldPanel = new DDPanel();
        base.add(fieldPanel, BorderLayout.CENTER);

        DDPanel panel = new DDPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        fieldPanel.add(panel, BorderLayout.NORTH);

        DDLabel nameLabel = new DDLabel("profilename", STYLE);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        panel.add(nameLabel, BorderLayout.WEST);

        DDTextField nameText = new DDTextField(GuiManager.DEFAULT, STYLE);
        nameText.setTextLengthLimit(PLAYER_NAME_LIMIT);
        nameText.setText(profile_.getName());
        nameText.setRegExp(PLAYER_NAME_REGEXP);
        panel.add(nameText, BorderLayout.CENTER);

        nameWidgets_ = new TablePanel.TextWidgets();
        nameWidgets_.label_ = nameLabel;
        nameWidgets_.text_ = nameText;

        // add online fields
        DDLabelBorder onlineBorder = new DDLabelBorder(profile_.isOnline() ? "onlineprofile" : "onlineprofile2", STYLE);
        fieldPanel.add(onlineBorder, BorderLayout.CENTER);

        onlinePanel_ = new OnlinePanel();
        onlineBorder.add(onlinePanel_);

        // only email and password can be changed for an online profile
        if (profile_.isOnline())
        {
            nameLabel.setEnabled(false);
            nameText.setEnabled(false);

            if (profile_.isActivated())
            {
                addEmail();
                emailWidgets_.setEnabled(false);
            }
            else
            {
                addPassword();
            }

            onlinePanel_.addButtons();
        }
        else
        {
            noRadio_ = onlinePanel_.addRadio("profilenoonline");
            newRadio_ = onlinePanel_.addRadio("profilenewonline");
            existRadio_ = onlinePanel_.addRadio("profileexistonline");

            // default to local profile
            noRadio_.setSelected(true);
        }

        // add help text
        DDHtmlArea helpText = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        helpText.setDisplayOnly(true);
        helpText.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 0));
        fieldPanel.add(helpText, BorderLayout.SOUTH);

        Dimension curSize = helpText.getPreferredSize();
        Dimension newSize = new Dimension();
        newSize.width = curSize.width;
        newSize.height = (bStartMenuMode) ? 75 : 100;
        helpText.setPreferredSize(newSize);

        getDialog().setHelpTextWidget(helpText);

        // add listeners
        nameText.addPropertyChangeListener(this);

        if (profile_.isOnline())
        {
            emailButton_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    doButton(e);
                }
            });

            if (passwordButton_ != null)
            {
                passwordButton_.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        doButton(e);
                    }
                });
            }

            if (sendButton_ != null)
            {
                sendButton_.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        doButton(e);
                    }
                });
            }

            if (resetButton_ != null)
            {
                resetButton_.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        doButton(e);
                    }
                });
            }

            if (syncButton_ != null)
            {
                syncButton_.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        doButton(e);
                    }
                });
            }
        }

        if (noRadio_ != null)
        {
            noRadio_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    doRadio();
                }
            });

            newRadio_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    doRadio();
                }
            });

            existRadio_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    doRadio();
                }
            });
        }

        checkButtons();

        return base;
    }

    private void addEmail()
    {
        if (emailWidgets_ == null)
        {
            emailWidgets_ = onlinePanel_.addTextField("profileemail",
                                                      "profileemail",
                                                      STYLE,
                                                      256,
                                                      profile_.getEmail(),
                                                      PropertyConfig.getRequiredStringProperty("regexp.email"),
                                                      new Insets(5, 0, 5, 0));

            emailWidgets_.text_.addPropertyChangeListener(this);

            checkButtons();
        }
    }

    private void removeEmail()
    {
        if (emailWidgets_ != null)
        {
            onlinePanel_.remove(emailWidgets_.label_);
            onlinePanel_.remove(emailWidgets_.text_);
            emailWidgets_ = null;

            checkButtons();
        }
    }

    private void addPassword()
    {
        if (passwordWidgets_ == null)
        {
            passwordWidgets_ = onlinePanel_.addTextField("profilepassword",
                                                         "profilepassword",
                                                         STYLE,
                                                         16,
                                                         null,
                                                         PLAYER_NAME_REGEXP,
                                                         new Insets(5, 0, 5, 0));

            passwordWidgets_.text_.addPropertyChangeListener(this);

            checkButtons();
        }
    }

    private void removePassword()
    {
        if (passwordWidgets_ != null)
        {
            onlinePanel_.remove(passwordWidgets_.label_);
            onlinePanel_.remove(passwordWidgets_.text_);
            passwordWidgets_ = null;

            checkButtons();
        }
    }

    private boolean isOnline()
    {
        if (noRadio_ != null)
        {
            return newRadio_.isSelected() || existRadio_.isSelected();
        }
        else
        {
            return profile_.isOnline();
        }
    }

    /**
     * Focus to text field
     */
    @Override
    protected Component getFocusComponent()
    {
        // choose the most useful field
        if (nameWidgets_.isEnabled())
        {
            return nameWidgets_.text_;
        }
        else if (noRadio_ != null)
        {
            if (noRadio_.isSelected()) return noRadio_;
            else if (newRadio_.isSelected()) return newRadio_;
            else return existRadio_;
        }
        else if (passwordWidgets_ != null)
        {
            return passwordWidgets_.text_;
        }
        else if (emailWidgets_ != null)
        {
            return emailWidgets_.text_;
        }
        else
        {
            return null;
        }
    }

    /**
     * Closes the dialog unless an error occurs saving the profile information
     */
    @Override
    public boolean processButton(GameButton button)
    {
        boolean bResult = false;
        boolean bSuccess = true;
        boolean bOnline = false;
        boolean bNew = false;

        if (button.getName().equals(okayButton_.getName()))
        {
            // okay
            String sText = nameWidgets_.getText();
            String sCurrent = profile_.getName();
            if (!sText.equals(sCurrent))
            {
                // name changed
                profile_.setName(sText);
                bResult = true;
            }

            bOnline = isOnline();
            if (bOnline)
            {
                // new online profile if previously a local profile
                bNew = !profile_.isOnline();
                bResult = bNew | bEmailMode_ | bPasswordMode_ | (passwordWidgets_ != null);
            }
        }

        if (bResult && bOnline)
        {
            // if online profile, send it to the server
            OnlineProfile profile = profile_.toOnlineProfile();

            if (bNew)
            {
                if (emailWidgets_ != null)
                {
                    // submit new profile
                    profile.setActivated(false);
                    profile.setEmail(emailWidgets_.getText());
                    bSuccess = SendWanProfile.sendWanProfile(context_, OnlineMessage.CAT_WAN_PROFILE_ADD, profile, null);
                }
                else
                {
                    // retrieve profile
                    profile.setPassword(passwordWidgets_.getText());
                    SendMessageDialog dialog = SendWanProfile.sendWanProfileDialog(context_, OnlineMessage.CAT_WAN_PROFILE_LINK, profile, null);
                    bSuccess = dialog.getStatus() == DDMessageListener.STATUS_OK;

                    if (bSuccess)
                    {
                        // update local values using the server profile
                        EngineMessage resEngineMsg = dialog.getReturnMessage();
                        OnlineMessage resOnlineMsg = new OnlineMessage(resEngineMsg);
                        OnlineProfile resProfile = new OnlineProfile(resOnlineMsg.getOnlineProfileData());

                        profile.setActivated(true);
                        profile.setEmail(resProfile.getEmail());
                    }
                }
            }
            else if (bEmailMode_)
            {
                // submit email change
                profile.setActivated(false);
                profile.setEmail(emailWidgets_.getText());
                bSuccess = SendWanProfile.sendWanProfile(context_, OnlineMessage.CAT_WAN_PROFILE_RESET, profile, null);

                if (bSuccess)
                {
                    // new password was sent so reset the local value
                    profile_.setPassword(null);
                }
            }
            else if (bPasswordMode_)
            {
                // close dialog after password change
                bSuccess = true;
            }
            else
            {
                // submit profile activation
                profile.setActivated(true);
                profile.setPassword(passwordWidgets_.getText());
                bSuccess = SendWanProfile.sendWanProfile(context_, OnlineMessage.CAT_WAN_PROFILE_ACTIVATE, profile, null);
            }

            if (bSuccess)
            {
                // update local profile values
                profile_.setActivated(profile.isActivated());
                profile_.setEmail(profile.getEmail());

                if (passwordWidgets_ != null)
                {
                    profile_.setPassword(passwordWidgets_.getText());
                }
            }
            else
            {
                bResult = false;
            }
        }

        if (bSuccess)
        {
            removeDialog();
        }

        setResult(bResult);

        return bSuccess;
    }

    /**
     * msg text change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        checkButtons();
    }

    /**
     * Handle radio change
     */
    private void doRadio()
    {
        if (isOnline() && engine_.isDemo())
        {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.playerprofile.demo"));
            noRadio_.setSelected(true);
            return;
        }

        if (noRadio_.isSelected())
        {
            removeEmail();
            removePassword();

            context_.getWindow().clearMessage();
        }
        else if (newRadio_.isSelected())
        {
            removePassword();
            addEmail();

            getDialog().showHelp(emailWidgets_.text_);
            getDialog().ignoreNextHelp();
            emailWidgets_.text_.requestFocus();
        }
        else
        {
            removeEmail();
            addPassword();

            getDialog().showHelp(passwordWidgets_.text_);
            getDialog().ignoreNextHelp();
            passwordWidgets_.text_.requestFocus();
        }

        repaint();
    }

    /**
     * Handle online button click
     */
    private void doButton(ActionEvent e)
    {
        Object source = e.getSource();

        if (emailButton_ == source)
        {
            // change email
            if (profile_.isActivated())
            {
                String sMsg = PropertyConfig.getMessage("msg.profileemail");
                if (!EngineUtils.displayConfirmationDialog(context_, sMsg, "profileemail"))
                {
                    return;
                }
            }

            bEmailMode_ = true;

            if (passwordWidgets_ != null)
            {
                removePassword();
                addEmail();
            }

            emailWidgets_.setEnabled(true);
            onlinePanel_.remove(buttonPanel_);

            emailButton_ = null;
            passwordButton_ = null;
            sendButton_ = null;
            resetButton_ = null;
            syncButton_ = null;

            // resize and set focus to a useful field
            getDialog().showHelp(emailWidgets_.text_);
            getDialog().ignoreNextHelp();
            emailWidgets_.text_.requestFocus();

            repaint();
        }
        else if (passwordButton_ == source)
        {
            // change password
            TypedHashMap hmParams = new TypedHashMap();
            hmParams.setObject(ProfileList.PARAM_PROFILE, profile_);
            ChangePasswordDialog dialog = (ChangePasswordDialog) context_.processPhaseNow("ChangePasswordDialog", hmParams);

            // close the dialog if a new password was successfully submitted
            Boolean result = (Boolean) dialog.getResult();

            if ((result != null) && (result))
            {
                bPasswordMode_ = true;
                okayButton_.doClick();
            }
        }
        else if (syncButton_ == source)
        {
            // sync password
            TypedHashMap hmParams = new TypedHashMap();
            hmParams.setObject(ProfileList.PARAM_PROFILE, profile_);
            SyncPasswordDialog dialog = (SyncPasswordDialog) context_.processPhaseNow("SyncPasswordDialog", hmParams);

            // close the dialog if a new password was successfully submitted
            Boolean result = (Boolean) dialog.getResult();

            if ((result != null) && (result))
            {
                bPasswordMode_ = true;
                okayButton_.doClick();
            }
        }
        else if (sendButton_ == source)
        {
            // send password
            OnlineProfile profile = profile_.toOnlineProfile();
            SendWanProfile.sendWanProfile(context_, OnlineMessage.CAT_WAN_PROFILE_SEND_PASSWORD, profile, null);
        }
        else if (resetButton_ == source)
        {
            // reset password
            String sMsg = PropertyConfig.getMessage("msg.profilereset");
            if (!EngineUtils.displayConfirmationDialog(context_, sMsg, "profilereset"))
            {
                return;
            }

            // do an update since it automatically resets the password
            OnlineProfile profile = profile_.toOnlineProfile();
            if (!SendWanProfile.sendWanProfile(context_, OnlineMessage.CAT_WAN_PROFILE_RESET, profile, null))
            {
                return;
            }

            // reset the local profile
            profile_.setPassword(null);
            profile_.setActivated(false);

            // close the dialog
            bPasswordMode_ = true;
            okayButton_.doClick();
        }
    }

    /**
     * Enable buttons
     */
    private void checkButtons()
    {
        boolean bEnabled = (nameWidgets_.getText().length() > 0);
        boolean bOnline = isOnline();

        if (bEnabled && bOnline)
        {
            if (emailWidgets_ != null)
            {
                bEnabled = (emailWidgets_.getText().length() > 0) && (emailWidgets_.text_.isValidData());
            }
            else if (passwordWidgets_ != null)
            {
                bEnabled = (passwordWidgets_.getText().length() > 0);

                if (emailButton_ != null) emailButton_.setEnabled(!bEnabled);
                if (sendButton_ != null) sendButton_.setEnabled(!bEnabled);
            }
        }

        okayButton_.setEnabled(bEnabled);
    }

    /**
     * Online panel
     */
    private class OnlinePanel extends TablePanel
    {
        private ButtonGroup radioGroup_ = new ButtonGroup();

        public DDRadioButton addRadio(String name)
        {
            // spacer - probably more going on here than necessary but it works
            DDRadioButton radio = new DDRadioButton(name);
            constraints_.weightx = 1.0;
            constraints_.gridwidth = GridBagConstraints.REMAINDER;
            constraints_.anchor = GridBagConstraints.WEST;
            layout_.setConstraints(radio, constraints_);
            radioGroup_.add(radio);
            add(radio);

            return radio;
        }

        public void addButtons()
        {
            buttonPanel_ = new DDPanel();
            constraints_.weightx = 0.0;
            constraints_.insets = new Insets(5, 5, 5, 5);
            constraints_.gridwidth = GridBagConstraints.REMAINDER;
            constraints_.fill = GridBagConstraints.BOTH;
            layout_.setConstraints(buttonPanel_, constraints_);
            add(buttonPanel_);

            DDPanel buttonPanel = new DDPanel();
            buttonPanel.setLayout(new GridLayout(1, 0, 8, 0));

            emailButton_ = new GlassButton("profileemail", "Glass");
            buttonPanel.add(emailButton_);

            if (profile_.isActivated())
            {
                passwordButton_ = new GlassButton("profilepassword", "Glass");
                buttonPanel.add(passwordButton_);
                resetButton_ = new GlassButton("profilereset", "Glass");
                buttonPanel.add(resetButton_);
                syncButton_ = new GlassButton("profilesync", "Glass");
                buttonPanel.add(syncButton_);
            }
            else
            {
                sendButton_ = new GlassButton("profilesend", "Glass");
                buttonPanel.add(sendButton_);
            }

            DDPanel align = new DDPanel();
            align.setLayout(new CenterLayout());
            align.add(buttonPanel, BorderLayout.CENTER);
            buttonPanel_.add(align, BorderLayout.SOUTH);
        }

    }
}
