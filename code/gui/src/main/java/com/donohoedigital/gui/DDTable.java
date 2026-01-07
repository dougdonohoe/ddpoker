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
/*
 * DDTable.java
 *
 * Created on February 4, 2003, 1:13 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDTable extends JTable implements DDTextVisibleComponent, MouseListener
{
    private Exporter exporter_ = null;
    private TableMenuItems menuitems_ = null;
    private boolean bSelectOnRightClick_ = false;
    private Color headerFG_, headerBG_;

    /**
     * Creates a new instance of DDTable 
     */
    public DDTable(String sName, String sStyle, String[] columnnames, int[] columnwidths) 
    {
        GuiManager.init(this, sName, sStyle);
        setAutoCreateColumnsFromModel(false);
        addMouseListener(this);
        getTableHeader().addMouseListener(this);

        for (int i = 0; i < columnwidths.length; i++) {
            TableColumn col = new TableColumn(i, columnwidths[i]);
            col.setIdentifier(columnnames[i]);
            col.setHeaderValue(PropertyConfig.getRequiredStringProperty("table.column." + columnnames[i]));
            if (headerFG_ != null && headerBG_ != null) col.setHeaderRenderer(new DDHeaderRenderer());
            this.addColumn(col);
        }
    }

    /**
     * set this table to also select row on popup-trigger
     */
    public void setSelectOnRightClick(boolean b)
    {
        bSelectOnRightClick_ = b;
    }

    /**
     * Is select on right click?
     */
    public boolean isSelectOnRightClick()
    {
        return bSelectOnRightClick_;
    }

    /**
     * Override to set row height when font is set
     */
    public void setFont(Font f)
    {
        super.setFont(f);
        getTableHeader().setFont(f);
        if (f == null) return;

        TextUtil util = new TextUtil((Graphics2D)BaseApp.getGraphicsDefault(), f, "X");
        setRowHeight((int)util.metrics.getHeight() + (int) util.metrics.getLeading() + 4);
    }

    /**
     * set header fg
     */
    public void setHeaderForeground(Color c)
    {
        headerFG_ = c;
    }

    /**
     * set header bg
     */
    public void setHeaderBackground(Color c)
    {
        headerBG_ = c;
    }


    public String getType() {
        return "table";
    }

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    /**
     * set alignment for column
     */
    public void setAlign(int col, int align)
    {
        DDRenderer r = new DDRenderer(align);
        getColumnModel().getColumn(col).setCellRenderer(r);
    }

    ////
    //// Mouse listener for popup-menu
    ////

    public void mouseClicked(MouseEvent e)
    {
    }

    private static ImageIcon exportIcon_ = ImageConfig.getImageIcon("menuicon.export");

    public void mouseReleased(MouseEvent e)
    {
        if (exporter_ == null && menuitems_ == null) return;
        if (!GuiUtils.isPopupTrigger(e, false)) return;
        if (getRowCount() == 0) return;

        int nMenuCnt = 0;
        if (exporter_ != null) nMenuCnt++;
        if (menuitems_ != null && menuitems_.isItemsToBeAdded(this)) nMenuCnt++;

        if (nMenuCnt == 0) return;

        DDPopupMenu menu = new DDPopupMenu();

        DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, "PopupMenu");
        title.setText(PropertyConfig.getMessage("menuitem.table.title"));
        title.setDisplayMode(DDMenuItem.MODE_TITLE);
        menu.add(title);

        if (exporter_ != null)
        {
            DDMenuItem item = new DDMenuItem(GuiManager.DEFAULT, "PopupMenu");
            item.setText(PropertyConfig.getMessage("menuitem.table.export"));
            item.setIcon(exportIcon_);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    exporter_.exportRequested(DDTable.this);
                }
            });
            menu.add(item);
        }

        if (menuitems_ != null)
        {
            menuitems_.addMenuItems(this, menu);
        }

        Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), this);
        menu.show(this, p.x, p.y);
    }

    public void mousePressed(MouseEvent e)
    {
        if (bSelectOnRightClick_ && GuiUtils.isPopupTrigger(e, false))
        {
            int row = rowAtPoint(e.getPoint());
            if (row != -1)
            {
                getSelectionModel().setSelectionInterval(row, row);
            }
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    /**
     * Get contents of table as a csv string, using
     * platforms' default line end character
     */
    public String toCSV(boolean bIncludeHeader)
    {
        StringBuilder sb = new StringBuilder();
        int nRowCnt = getRowCount();
        int nColCnt = getColumnCount();
        String sSep = System.getProperty("line.separator");
        Object oValue;
        if (bIncludeHeader && getTableHeader() != null)
        {
            TableColumnModel cm = getColumnModel();
            for (int i = 0; i < nColCnt; i++)
            {
                if (i > 0) sb.append(",");
                oValue = cm.getColumn(i).getHeaderValue();
                if (oValue != null) sb.append(Utils.encodeCSV(oValue.toString()));
            }
            sb.append(sSep);
        }
        
        TableModel tm = getModel();
        for (int i = 0; i < nRowCnt; i++)
        {
            for (int j = 0; j < nColCnt; j++)
            {
                if (j > 0) sb.append(",");
                oValue = tm.getValueAt(i, j);
                if (oValue != null) sb.append(Utils.encodeCSV(oValue.toString()));
            }
            sb.append(sSep);
        }
        return sb.toString();
    }

    /**
     * set exporter, which handles UI to export table
     */
    public void setExporter(Exporter exporter)
    {
        exporter_ = exporter;
    }

    /**
     * set exporter, which handles UI to export table
     */
    public void setTableMenuItems(TableMenuItems menuitems)
    {
        menuitems_ = menuitems;
    }

    /**
     * interface for exporter handler
     */
    public static interface Exporter
    {
        void exportRequested(DDTable table);
    }

    /**
     * interface for other table options
     */
    public static interface TableMenuItems
    {
        boolean isItemsToBeAdded(DDTable table);

        void addMenuItems(DDTable table, DDPopupMenu menu);
    }

    /**
     * cell renderer
     */
    private class DDRenderer extends DefaultTableCellRenderer
    {
        public DDRenderer(int align) {
            setHorizontalAlignment(align);
        }
    }

    /**
     * header renderer
     */
    private class DDHeaderRenderer extends DefaultTableCellRenderer
    {
        public DDHeaderRenderer()
        {
            super();
            if (headerFG_ != null) setForeground(headerFG_);
            if (headerBG_ != null) setBackground(headerBG_);
        }
//
//        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//        {
//            JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//            if (headerFG_ != null && headerBG_ != null)
//            {
//                c.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, headerFG_.darker(), headerBG_.brighter()));
//            }
//            return c;
//        }
    }
}
