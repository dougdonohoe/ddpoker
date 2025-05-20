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
 * GameListPanel.java
 *
 * Created on July 13, 2003, 9:03 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GameListPanel extends DDPanel implements ListSelectionListener,
                                            PropertyChangeListener,
                                            ActionListener
{
    static Logger logger = LogManager.getLogger(GameListPanel.class);
    
    // file extensions
    public static final String SAVE_EXT = PropertyConfig.getRequiredStringProperty("settings.save.ext");

    // member stuff
    protected DialogPhase dialog_;
    protected DDButton okayButton_;
    protected GameEngine engine_;
    protected GameContext context_;
    protected GamePhase gamephase_;
    private String STYLE;
    private int[] COLUMN_WIDTHS;
    private String[] COLUMN_NAMES;
    private DDTextField name_;
    private com.donohoedigital.gui.DDButton delete_;
    private DDTable saveTable_;
    private DDScrollTable saveScroll_;
    private SaveTableModel model_;
    private GameState selected_;
    boolean bSaveMode_ = true;
    private GameState lastSave_;
    private boolean bOnlineSave_ = false;
    public boolean bOnlineLoad_ = false;
    private String sBegin_;
    private boolean bDemo_;
    
    // save game info
    private static final int[] COLUMN_WIDTHS_LOAD = new int[] {
        240, 293, 166
    };
    private static final int[] COLUMN_WIDTHS_SAVE = new int[] {
        240, 280, 166
    };
    private static final String[] COLUMN_NAMES_SAVE = new String[] {
        "gamename", "players", "date"
    };
    
    
    /**
     * Create new panel for saving games
     */
    public static GameListPanel newSaveDialogPanel(GameEngine engine, GameContext context, GamePhase gamephase, String sStyle, DialogPhase dialog, DDButton okay)
    {
        return new GameListPanel(engine, context, gamephase, COLUMN_WIDTHS_SAVE, COLUMN_NAMES_SAVE,
                                 sStyle, dialog, okay, "DisplayMessage", sStyle, null);
    }
    
    /**
     * Create new panel for saving games
     */
    public static GameListPanel newLoadMenuPanel(GameEngine engine, GameContext context, GamePhase gamephase, String sStyle, String sBevelStyle, DialogPhase dialog, DDButton okay)
    {
        return new GameListPanel(engine, context, gamephase, COLUMN_WIDTHS_LOAD, COLUMN_NAMES_SAVE,
                                 sStyle, dialog, okay, "DisplayMessageMenu", "DisplayMessageMenu2", sBevelStyle);
    }
    
    /** 
     * Save UI
     */
    private GameListPanel(GameEngine engine, GameContext context, GamePhase gamephase, int[] widths, String[] names, String sStyle,
                          DialogPhase dialog, DDButton okay, String labelStyle, String sNameStyle, String sBevelStyle)
    {    
        engine_ = engine;
        context_ = context;
        gamephase_ = gamephase;
        STYLE = sStyle;
        COLUMN_WIDTHS = widths;
        COLUMN_NAMES = names;
        dialog_ = dialog;
        okayButton_ = okay;
        bDemo_ = engine_.isDemo();
        
        Game game = context_.getGame();
        bSaveMode_ = gamephase_.getBoolean("savemode", true);
        
        // base
        DDPanel base = this;
        
        if (bDemo_)
        {
            DDLabel demo = new DDLabel("demosave", STYLE);
            demo.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            base.setLayout(new CenterLayout());
            base.add(demo);
            okayButton_.setEnabled(false);
            return;
        }

        if (bSaveMode_)
        {
            sBegin_ = game.getBegin();
            lastSave_ = game.getLastGameState();
            bOnlineSave_ = game.isOnlineGame();
            if (bOnlineSave_)
            {
                ApplicationError.assertNotNull(lastSave_, "Last save missing in online save");
            }
        }
        else
        {
            sBegin_ = gamephase_.getString("loadbegin", GameState.GAME_BEGIN);
        }
        
        
        
        // name of game
        DDPanel namebase = new DDPanel();
        namebase.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        namebase.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        base.add(namebase, BorderLayout.NORTH);
        
        DDLabel namelabel = new DDLabel("gamename", sNameStyle);
        namelabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        namebase.add(namelabel);
        
        name_ = new DDTextField(GuiManager.DEFAULT, STYLE, sBevelStyle);
        
        if (bOnlineSave_)
        {
            name_.setDisplayOnly(true);
            name_.setColumns(18);
        }
        else
        {
            name_.setColumns(30);
            name_.setTextLengthLimit(40);
            name_.setRegExp("^.+$");
            name_.addPropertyChangeListener("value", this);
        }
        namebase.add(name_);
        
        if (!bOnlineSave_)
        {
            // list of previous savedgames
            DDPanel savebase = new DDPanel();
            BorderLayout layout = (BorderLayout) savebase.getLayout();
            layout.setVgap(5);
            savebase.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            base.add(savebase, BorderLayout.CENTER);

            ////
            //// Title Area
            ////
            DDPanel titlearea = new DDPanel();
            savebase.add(titlearea, BorderLayout.NORTH);

            // savedgames title
            DDLabel prev = new DDLabel("savedgames."+sBegin_, labelStyle);
            titlearea.add(prev, BorderLayout.WEST);

            // delete button
            delete_ = new GlassButton("deletegame", "Glass");
            delete_.addActionListener(this);
            titlearea.add(delete_, BorderLayout.EAST);

            ////
            //// Table
            ////
            saveScroll_ = new DDScrollTable(GuiManager.DEFAULT, STYLE, "BrushedMetal", COLUMN_NAMES, COLUMN_WIDTHS);
            saveScroll_.setPreferredSize(new Dimension(saveScroll_.getPreferredWidth(), 200));
            savebase.add(saveScroll_, BorderLayout.CENTER);

            saveTable_ = saveScroll_.getDDTable();
            model_ = getSavedFileModel();
            saveTable_.setModel(model_);
            saveTable_.setShowHorizontalLines(true);
            saveTable_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            saveTable_.setColumnSelectionAllowed(false);
            saveTable_.getSelectionModel().addListSelectionListener(this);
            saveTable_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            saveTable_.addMouseListener(new MouseAdapter(){
                public void mouseClicked(MouseEvent e){
                    if (e.getClickCount() == 2)
                    {
                        handleDoubleClick();
                    }
                }});
        }
        else
        {
            DDHtmlArea online = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
            online.setText(PropertyConfig.getMessage("msg.onlinesave"));
            online.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            online.setPreferredSize(new Dimension(300, 75));
            base.add(online, BorderLayout.CENTER);
        }
    
        if (bSaveMode_ && lastSave_ != null)
        {
            name_.setText(lastSave_.getGameName());
            if (!bOnlineSave_) propertyChange(null); // force change
        }
    }
    
    /**
     * Handle dbl-click
     */
    private void handleDoubleClick()
    {
        if (bSaveMode_)
        {
            // nothing to do in save mode for dbl click
        }
        else
        {
            // load mode - load selected file
            processButton(okayButton_.getName());
        }
    }
    
    /**
     * Set focus to the name widget
     */
    public Component getFocusComponent()
    {
        return name_;
    }
    
    /**
     * Handle button press
     */
    public boolean processButton(GameButton button) 
    {
        return processButton(button.getName());
    }

    /**
     * actual logic
     */
    private boolean processButton(String sButtonName)
    {
        if (sButtonName.equals(okayButton_.getName()))
        {  
            if (bSaveMode_)
            {
                Game game = context_.getGame();
                String sName = getName(true);
                GameState newSave = null;
                // see if we are saving same name as last time
                if (lastSave_ != null)
                {
                    if (sName.equals(lastSave_.getGameName()))
                    {
                        newSave = lastSave_;
                    }
                }
                
                // next check for duplicate to existing game
                if (newSave == null)
                {
                    int nNum = model_.getRowCount();
                    for (int i = 0; i < nNum; i++)
                    {
                        if (sName.equals(model_.getGameState(i).getGameName()))
                        {
                            if (EngineUtils.displayConfirmationDialog(context_,
                                PropertyConfig.getMessage("msg.confirm.overwrite", sName)))
                            {
                                newSave = model_.getGameState(i);
                                // BUG 188 - update names when overwriting
                                newSave.setDescription(game.getDescription());
                            }
                            else
                            {
                                return false;
                            }
                        }
                    }
                }
                
                // if not using previous save, create a new one
                if (newSave == null)
                {
                    newSave = game.newGameState(sName, SAVE_EXT);
                }
                
                // get sync object
                Object sync = SAVE_EXT; // dummy for regular saves
                if (bOnlineSave_)
                {
                    sync = context_.getGameManager().getSaveLockObject();
                }
                
                // sync with online game object to prevent issues
                // in case user saves during an update from another user
                synchronized (sync)
                {
                    //logger.debug("Saving...");
                    game.saveGame(newSave);
                    game.writeGame(newSave);
                }
                
                if (dialog_ != null) dialog_.removeDialog();
            }
            else
            {
                if (dialog_ != null) dialog_.removeDialog();
                logger.info("Loading saved game: " + selected_.getFile().getAbsolutePath());
                LoadSavedGame.loadGame(context_, selected_);
            }
        }
        else // cancel button
        {
            if (dialog_ != null) dialog_.removeDialog();
        }
        
        return true;
    }
    
    boolean bIgnoreTextChange_ = false;
    /**
     * Table row selected
     */
    public void valueChanged(ListSelectionEvent e) 
    {
        if (e.getValueIsAdjusting()) return;
        
        ListSelectionModel lsm = saveTable_.getSelectionModel();
        int index = lsm.getMinSelectionIndex();
        if (index >= 0 ) 
        {
            selected_ = model_.getGameState(index);
            bIgnoreTextChange_ = true;
            name_.setText(selected_.getGameName());
            bIgnoreTextChange_ = false;        }
        else
        {
            selected_ = null;
        }
        
        checkButtons();
    }
    
    /** 
     * Called when value changes on the text fields
     */
    public void propertyChange(PropertyChangeEvent evt) {
        
        if (bIgnoreTextChange_) return;
        checkButtons();
        
        // see if we match something in the table, if so, highlight it
        String sName = getName(false);
        //logger.debug("Text change: <"+sName+">");
        
        ListSelectionModel selmodel = saveTable_.getSelectionModel();
        int nNum = model_.getRowCount();
        for (int i = 0; sName != null && i < nNum; i++)
        {
            if (sName.equals(model_.getGameState(i).getGameName()))
            {
                selmodel.setSelectionInterval(i, i);
                saveTable_.scrollRectToVisible(saveTable_.getCellRect(i, 0, true));
                return;
            }
        }
        
        if (!selmodel.isSelectionEmpty())
        {
            selmodel.clearSelection();
        }
    }
    
    /**
     * Turn buttons on / off
     */
    private void checkButtons()
    {
        delete_.setEnabled(selected_ != null);
        
        if (bSaveMode_)
        {
            okayButton_.setEnabled(name_.isValidData());
        }
        else
        {
            okayButton_.setEnabled(selected_ != null);
        }
    }
    
    private String getName(boolean bTrim)
    {
        String sName = name_.getText();
        if (bTrim && sName != null) sName = sName.trim();
        return sName;
    }
    
    /**
     * Get saved files
     */
    private SaveTableModel getSavedFileModel()
    {
        GameState saved[] = GameState.getSaveFileList(sBegin_, SAVE_EXT);
        return new SaveTableModel(saved);
    }
    
    /**
     * Delete button
     */
    public void actionPerformed(ActionEvent e) 
    {
        ApplicationError.assertNotNull(selected_, "Nothing selected to delete");
        
        String sName = selected_.getGameName();
        String sKey = (selected_.isOnlineGame() ? "msg.confirm.delete.online" : "msg.confirm.delete");
        if (EngineUtils.displayConfirmationDialog(context_, Utils.fixHtmlTextFor15(PropertyConfig.getMessage(sKey, sName))))
        {
            selected_.delete();
            model_.removeRow(selected_);
            saveTable_.getSelectionModel().clearSelection();
            name_.setText("");
        }
    }
    
    /**
     * Used by table to display save files
     */
    private class SaveTableModel extends DefaultTableModel 
    {
        private ArrayList files;

        public SaveTableModel(GameState[] filesin) 
        {
            Arrays.sort(filesin, LISTSORTER);
            files = new ArrayList(filesin.length);
            for (int i = 0; i < filesin.length; i++)
            {
                files.add(filesin[i]);
            }
        }
        
        public void removeRow(GameState f)
        {
            int row = files.indexOf(f);
            files.remove(row);
            fireTableRowsDeleted(row,row);            
        }

        public GameState getGameState(int r) {
            return (GameState) files.get(r);
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
            if (files == null) {
                return 0;
            }
            return files.size();
        }

        public Object getValueAt(int rowIndex, int colIndex) {
            GameState game = getGameState(rowIndex);
            switch (colIndex) {
                case 0:
                    return game.getGameName();
                    
                case 1:
                    return game.getDescription();
                    
                case 2:
                    Date date = new Date(game.lastModified());
                    return PropertyConfig.getDateFormat("msg.format.shortdatetime", engine_.getLocale()).format(date);
                    
                
            }
            throw new ArrayIndexOutOfBoundsException("Invalid column value");
        }
    }

    private static CompareFile LISTSORTER = new CompareFile();
    
    /**
     * Sort in descending order (most recent at top)
     */
    private static class CompareFile implements Comparator
    {
        public int compare(Object o1, Object o2) {
            GameState gs1 = (GameState) o1;
            GameState gs2 = (GameState) o2;
            long dif = (gs2.lastModified() - gs1.lastModified());
            if (dif < 0) return -1;
            else if (dif > 0) return 1;
            
            return gs2.getFile().getName().compareTo(gs1.getFile().getName());
            
        }
    }
}
