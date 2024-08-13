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
/*
 * FileChooserDialog.java
 *
 * Created on May 23, 2004, 7:43 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;

/**
 * TODO: currently this is designed for saving as... doing 'pick file' needs some work (see DeckProfile)
 *
 * @author Doug Donohoe
 */
public class FileChooserDialog extends DialogPhase implements PropertyChangeListener, KeyListener
{
    public static final String PARAM_SUGGESTED_NAME = "suggested";
    public static final String PARAM_TOP_LABEL_PARAMS = "toplabelparams";

    protected JFileChooser choose_;
    private JTextField name_;
    private File selected_;
    private String EXT;

    public JComponent createTopPanel()
    {
        return null;
    }

    public JComponent createBottomPanel()
    {
        return null;
    }

    @Override
    public JComponent createDialogContents()
    {
        String sName = gamephase_.getString("label", GuiManager.DEFAULT);
        Object[] params = (Object[]) gamephase_.getObject(PARAM_TOP_LABEL_PARAMS);

        EXT = gamephase_.getString("ext", "csv");

        // contents
        DDPanel base = new DDPanel();
        BorderLayout layout = (BorderLayout) base.getLayout();
        layout.setVgap(5);

        // top label
        DDLabel name = new DDLabel(sName, STYLE);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        GuiManager.setLabelAsMessage(name, params);
        base.add(name, BorderLayout.NORTH);

        choose_ = new DDFileChooser(sName, STYLE, engine_.getPrefsNode());
        choose_.addChoosableFileFilter(new ChooserFilter(EXT));
        //choose_.setAcceptAllFileFilterUsed(true);
        choose_.addPropertyChangeListener(this);
        name_ = getTextField(choose_);
        String sSuggested = gamephase_.getString(PARAM_SUGGESTED_NAME, null);
        if (sSuggested != null)
        {
            name_.setText(fixName(sSuggested));
        }
        name_.addKeyListener(this);
        base.add(choose_, BorderLayout.CENTER);

        // fix default swing label
        JLabel type = getLabel(choose_, "Files of Type:");
        if (type != null)
        {
            type.setText(PropertyConfig.getMessage("msg.saveastype"));
        }

        getSelected();

        JComponent top = createTopPanel();
        JComponent bottom = createBottomPanel();

        if ((top != null) || (bottom != null))
        {
            DDPanel outer = new DDPanel();
            layout = (BorderLayout) outer.getLayout();
            layout.setVgap(5);

            if (top != null)
            {
                outer.add(top, BorderLayout.NORTH);
            }

            if (bottom != null)
            {
                outer.add(bottom, BorderLayout.SOUTH);
            }

            outer.add(base, BorderLayout.CENTER);

            outer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return outer;
        }
        else
        {
            base.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return base;
        }
    }

    /**
     * Focus to text field
     */
    @Override
    protected Component getFocusComponent()
    {
        return choose_;
    }

    /**
     * Default processButton calls closes dialog on any button press
     */
    @Override
    public boolean processButton(GameButton button)
    {
        return processButton(button, true);
    }

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button, boolean closeDialog)
    {
        File fResult = null;

        setResult(fResult);

        if (button.getName().equals(okayButton_.getName()))
        {
            // if exists, confirm over-write
            if (selected_.exists() && !EngineUtils.displayConfirmationDialog(context_,
                                                                             PropertyConfig.getMessage("msg.confirm.replace", selected_.getName())))
            {
                return false;
            }

            // if exists and over-writing, see if we can get a writer
            if (selected_.exists())
            {
                try
                {
                    FileOutputStream fos = new FileOutputStream(selected_, true);
                    fos.close();
                }
                catch (Throwable t)
                {
                    EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.error.replace", selected_.getName()));
                    return false;
                }
            }
            fResult = selected_;
        }

        if (closeDialog) removeDialog();

        setResult(fResult);

        return true;
    }

    /**
     * msg text change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String sName = evt.getPropertyName();
        if (sName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) ||
            sName.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY))
        {
            getSelected();
        }
    }

    /**
     * fetch selected file
     */
    private void getSelected()
    {
        String sName = name_.getText().trim();
        if (sName.length() == 0)
        {
            selected_ = null;
            if (choose_.getSelectedFile() != null) choose_.setSelectedFile(null);
        }
        else
        {
            selected_ = new File(choose_.getCurrentDirectory(), fixName(sName));
        }

//        if (selected_ != null)
//        {
//            logger.debug("Selected: "+ selected_.getAbsolutePath() + " exists: " + selected_.exists());
//        }
        checkButtons();
    }

    /**
     * fix name to have right extension
     */
    protected String fixName(String sName)
    {
        if (sName.endsWith("."))
        {
            sName += EXT;
        }

        if (!sName.endsWith("." + EXT))
        {
            sName += "." + EXT;
        }
        return sName;
    }

    /**
     * Enable buttons
     */
    protected void checkButtons()
    {
        okayButton_.setEnabled(selected_ != null);
    }

    /**
     * find jtextfield in file chooser
     */
    private JTextField getTextField(Container container)
    {
        Component[] children = container.getComponents();
        for (Component child : children)
        {
            if (child instanceof JTextField)
            {
                return (JTextField) child;
            }
            if (child instanceof Container)
            {
                JTextField f = getTextField((Container) child);
                if (f != null) return f;
            }
        }
        return null;
    }

    /**
     * find jlabel in file chooser with given text
     */
    private JLabel getLabel(Container container, String sText)
    {
        Component[] children = container.getComponents();
        for (Component child : children)
        {
            if (child instanceof JLabel &&
                ((JLabel) child).getText().equalsIgnoreCase(sText))
            {
                return (JLabel) child;
            }
            if (child instanceof Container)
            {
                JLabel f = getLabel((Container) child, sText);
                if (f != null) return f;
            }
        }
        return null;
    }

    ///
    /// Key listener for changes to file name field
    ///

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
        getSelected();
    }

    /**
     * filter for file chooser
     */
    static class ChooserFilter extends javax.swing.filechooser.FileFilter
    {
        String EXT;

        ChooserFilter(String sExt)
        {
            EXT = sExt;
        }

        /**
         * Whether the given file is accepted by this filter.
         */
        @Override
        public boolean accept(File f)
        {
            if (f.isFile())
            {
                return f.getName().endsWith("." + EXT);
            }

            return true;
        }

        /**
         * description
         */
        @Override
        public String getDescription()
        {
            return PropertyConfig.getMessage("msg.files." + EXT);
        }

    }
}
