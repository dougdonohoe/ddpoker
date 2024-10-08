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
 * BorderChooser.java
 *
 * Created on October 31, 2002, 3:27 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
    
/**
 *
 * @author  Doug Donohoe
 */
public class BorderChooser extends InternalDialog implements ActionListener
{
    //static Logger logger = LogManager.getLogger(BorderChooser.class);
    
    Territories territories_;
    TerritoryTableModel model_;
    Territory[] choices = new Territory[2];
    Border border = null;
    
    /** 
     * Creates a new BorderChooser 
     */
    public BorderChooser(BaseFrame frame, String sTitle, Territories territories) 
    {
        super(frame, "BorderChooser", sTitle, true);
        
        territories_ = territories;
        
        setContentPane(createUI());
        validate();
        pack();
    }
       
    JButton button_ok;
    JButton button_cancel;
    JCheckBox checkbox_enclosed;
    JTextField text_num;
    
    /**
     * Create components of this chooser
     */
    private JComponent createUI()
    {
        // tables
        model_ = new TerritoryTableModel(territories_);
        JComponent t1 = createTable(model_, "Pick 1st Territory", 0);
        JComponent t2 = createTable(model_, "Pick 2nd Territory", 1);
        
        JPanel tables = new JPanel();
        GridLayout lay = new GridLayout(1,2);
        tables.setLayout(lay);
        tables.add(t1);
        tables.add(t2);
        
        // buttons
        JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new FlowLayout());
        
        button_ok = new JButton("OK");
        button_cancel = new JButton("Cancel");
        buttonpanel.add(button_ok);
        buttonpanel.add(button_cancel);
        button_ok.addActionListener(this);
        button_cancel.addActionListener(this);
        
        // checkbox for enclosed
        checkbox_enclosed = new JCheckBox("Enclosed border");
        
        // text field for num
        text_num = new JTextField("");
        text_num.setColumns(2);
        
        // panel for num/label
        JLabel label_num = new JLabel("Num:");
        JPanel panel_num = new JPanel();
        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEFT);
        panel_num.setLayout(layout);
        panel_num.add(label_num);
        panel_num.add(text_num);
        
        // panel for checkbox / num
        JPanel panel_options = new JPanel();
        panel_options.setLayout(new GridLayout(1,2));
        panel_options.add(checkbox_enclosed);
        panel_options.add(panel_num, BorderLayout.EAST);
        
        // panel for chooser/buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new BorderLayout());
        buttons.add(panel_options, BorderLayout.NORTH);
        buttons.add(buttonpanel, BorderLayout.SOUTH);
        
        // basepanel
        JPanel basepanel = new JPanel();
        basepanel.setLayout(new BorderLayout());
        basepanel.add(tables, BorderLayout.CENTER);
        basepanel.add(buttons, BorderLayout.SOUTH);
        
        // this frame
        setPreferredSize(new Dimension(400,500));
        setMinimumSize(new Dimension(250, 150));
        
        // set buttons
        enableButtons();
        
        return basepanel;
    }
    
    /**
     * Create a set of widgets which contains a table for picking a territory
     */
    private JComponent createTable(TerritoryTableModel model, String sLabel, int nIndex)
    {
        // panel
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        // label
        JLabel label = new JLabel(sLabel);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        
        // choosen value
        JLabel choice = new JLabel(" ");
        choice.setHorizontalAlignment(SwingConstants.CENTER);

        //choice.setBackground(Color.blue);
        choice.setForeground(Color.blue);
        choice.setOpaque(true);        
        
        // table
        BorderTable table = new BorderTable(model, choice, nIndex);
        //Dimension size = new Dimension(200,200);
        
        // scrollpane
        JScrollPane scroll = new JScrollPane(table);
        scroll.setMinimumSize(new Dimension(140,500));
        
        int swidth = scroll.getVerticalScrollBar().getPreferredSize().width;
        choice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4,4,4,swidth),
                BorderFactory.createEtchedBorder()));        
       
        
        // put it all together
        panel.add(label, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(choice, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Private table which knows about its related label and the index
     * of the territory it is picking
     */
    private class BorderTable extends JTable implements ListSelectionListener
    {
        private JLabel choice_;
        private int nIndex_; // where we store the chosen territory
        
        public BorderTable(TerritoryTableModel model, JLabel choice, int nIndex)
        {
            super(model);
            choice_ = choice;
            nIndex_ = nIndex;
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            getSelectionModel().addListSelectionListener(
                    new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) { myValueChanged(e); }
                    }
            );
        }
        
        public TerritoryTableModel getTerritoryTableModel()
        {
            return (TerritoryTableModel) this.getModel();
        }
        
        public void myValueChanged(ListSelectionEvent e)
        {
            super.valueChanged(e);
            if (e.getValueIsAdjusting()) return;
            int index = getSelectionModel().getAnchorSelectionIndex();
            if (!getSelectionModel().isSelectionEmpty() && choice_ != null && index >= 0 ) {
                Territory t = getTerritoryTableModel().getTerritory(index);
                choice_.setText(t.getName());
                setChoice(nIndex_, t);
                enableButtons();
            }
            else
            {
                setChoice(nIndex_, null);
                enableButtons();
            }
        }
    }
    
    /**
     * Store selection
     */
    private void setChoice(int nIndex, Territory t)
    {
        choices[nIndex] = t;
    }
    
    /**
     * Set buttons enabled/disabled based on data state
     */
    private void enableButtons()
    {
        if (choices[0] != choices[1] &&
            choices[0] != null &&
            choices[1] != null &&
            getNumber() != null)
        {
            button_ok.setEnabled(true);
        }
        else
        {
            button_ok.setEnabled(false);
        }
    }
    
    /**
     * Get current text value and return true if okay
     */
    private Integer getNumber()
    {
        String sValue = text_num.getText();
        
        // if empty, okay
        if (sValue == null || sValue.length() == 0)
        {
            return Border.DEFAULT_NUM;
        }
        
        // convert to integer
        try {
            return Integer.valueOf(sValue);
        }
        catch (NumberFormatException ne)
        {
            return null;
        }
    }
    
    /**
     * Handle button press
     */
    public void actionPerformed(ActionEvent e) 
    {
        if (e.getSource() == button_cancel)
        {
            border = null;
            removeDialog();
        }
        else if (e.getSource() == button_ok)
        {
            border = new Border(choices[0], choices[1], checkbox_enclosed.isSelected(), getNumber());
            removeDialog();
        }
    }
    
     
    /**
     * Use border chooser to get a border modally
     */
    public static Border getBorder(BorderChooser b, int x, int y)
    {
        // figure out best location so not to cover x,y point with buffer of 10 pixels
        b.setNotObscurredLocation(x,y,10);

        // clear number (but keep current territory selections)
        b.text_num.setText("");
        
        // set visible / modal
        b.showDialog(null, InternalDialog.POSITION_NOT_OBSCURRED);
        return b.border;
    }
}
