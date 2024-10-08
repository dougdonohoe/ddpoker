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
package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ListPanel extends DDScrollPane implements ComponentListener
{
    //static Logger logger = LogManager.getLogger(ListPanel.class);

    // icons
    private ImageIcon emptyIcon_ = ImageConfig.getImageIcon("blank16");
    private ImageIcon selectedIcon_ = emptyIcon_;

    // list of items and panels which display the items
    private List items_;
    private InternalPanel panels_;

    // class which displays the item
    private Class itemPanelClass_;
    private String sStyle_;

    // selected info
    private int selectedIndex_;
    private Object selectedItem_ = null;
    private ListItemPanel selectedPanel_ = null;

    // viewport base
    private DDPanel listParent_;

    /**
     * Create new basic panel
     */
    public ListPanel(Class itemPanelClass, String sStyle)
    {
        this(itemPanelClass, sStyle, null,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * Create new panel specifying styles, scrollbar policies
     */
    public ListPanel(Class itemPanelClass, String sStyle, String bevelStyle, int nVerticalPolicy, int nHorizPolicy)
    {
        super(null, sStyle, bevelStyle, nVerticalPolicy, nHorizPolicy);

        sStyle_ = sStyle;
        itemPanelClass_ = itemPanelClass;

        items_ = new ArrayList();
        panels_ = new InternalPanel();

        listParent_ = new DDPanel(GuiManager.DEFAULT, sStyle);
        listParent_.add(panels_, BorderLayout.NORTH);
        setViewportView(listParent_);
        setOpaque(false); // need to reset after changing viewport

        getVerticalScrollBar().setUnitIncrement(36);
        getVerticalScrollBar().setBlockIncrement(36 * 3);

        addComponentListener(this);
    }

    /**
     * Get base panel
     */
    public DDPanel getListParent()
    {
        return listParent_;
    }

    /**
     * selected icon
     */
    public void setSelectedIcon(ImageIcon selectedIcon)
    {
        selectedIcon_ = selectedIcon;
    }

    /**
     * set items shown by this list
     */
    public void setItems(List items)
    {
        items_ = items;
        panels_.clear();

        int nSize = items.size();
        for (int i = 0; i < nSize; ++i)
        {
            addItemPanel(i, items.get(i), i == (nSize - 1));
        }
    }

    /**
     * update item
     */
    public void updateItem(int index, Object o)
    {
        items_.set(index, o);
        getItemPanel(index).setItem(o);
        updateItemPanel(index);
    }

    /**
     * Select next/previous ListItemPanel
     */
    public void select(int nDirection)
    {
        int nNum = panels_.getNumPanels();
        if (nNum == 0) return;

        int nCurrent = -1;
        for (int i = 0; i < nNum; i++)
        {
            if (selectedPanel_ == panels_.get(i))
            {
                nCurrent = i;
                break;
            }
        }

        // nothing currently selected
        if (nCurrent == -1)
        {
            if (nDirection > 0)
                nCurrent = 0;
            else
                nCurrent = nNum - 1;
        }
        else
        {
            nCurrent += nDirection;
        }

        // fix wraparound
        if (nCurrent < 0) nCurrent = nNum - 1;
        if (nCurrent >= nNum) nCurrent = 0;

        // do your magic
        itemSelected(nCurrent);
    }

    /**
     * Get selected index
     */
    public int getSelectedIndex()
    {
        return selectedIndex_;
    }

    /**
     * Get selected item
     */
    public Object getSelectedItem()
    {
        return selectedItem_;
    }

    /**
     * Get selected item's panel
     */
    public ListItemPanel getSelectedPanel()
    {
        return selectedPanel_;
    }

    /**
     * get panel at given index
     */
    public ListItemPanel getItemPanel(int index)
    {
        return panels_.get(index);
    }

    /**
     * get item at given index
     */
    public Object getItem(int index)
    {
        return items_.get(index);
    }

    /**
     * get items
     */
    public List getItems()
    {
        return items_;
    }

    /**
     * set selected item by index
     */
    public void setSelectedIndex(int index)
    {
        itemSelected(index);
    }

    /**
     * Set selected item by item
     */
    public void setSelectedItem(Object item)
    {
        itemSelected(getItemIndex(item));
    }

    /**
     * record item selected, change icon, notify listeners
     */
    private void itemSelected(int index)
    {
        if (index < 0)
        {
            selectedPanel_ = null;
            selectedItem_ = null;
            selectedIndex_ = -1;
            return;
        }

        ListItemPanel panel = panels_.get(index);
        if (selectedPanel_ != null)
        {
            selectedPanel_.setIcon(emptyIcon_);
            selectedPanel_.setSelected(false);
        }
        selectedPanel_ = panel;
        if (selectedPanel_ != null)
        {
            selectedPanel_.setIcon(selectedIcon_);
            selectedPanel_.setSelected(true);
            scrollComponentToVisible(selectedPanel_);
        }
        selectedIndex_ = index;
        selectedItem_ = items_.get(selectedIndex_);

        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent selectionEvent = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == ListSelectionListener.class)
            {
                if (selectionEvent == null)
                {
                    selectionEvent = new ListSelectionEvent(this, index, index, false);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(selectionEvent);
            }
        }
        
        updateHelpText(selectedPanel_);
    }

    public void updateHelpText(ListItemPanel itemPanel)
    {
        if (helpPanel_ != null)
        {
            String helpText = (itemPanel != null) ? itemPanel.getHelpText() :
                (selectedPanel_ == null) ? "" : selectedPanel_.getHelpText();

            if (helpPanel_ instanceof DDHtmlArea)
            {
                ((JTextComponent) helpPanel_).setText(helpText);
            }
        }
    }

    /**
     * scroll item at index to visible
     */
    public void scrollItemToVisible(int index)
    {
        scrollComponentToVisible(getItemPanel(index));
    }

    /**
     * scroll given component to visible
     */
    public void scrollComponentToVisible(JComponent component)
    {
        if (component == null) return;

        if (isUserScrolling())
        {
            //logger.debug("User is scrolling...");
            return;
        }

        Point loc = component.getLocation();
        JViewport vp = getViewport();
        Border border = getViewportBorder();
        Insets insets = border.getBorderInsets(vp);
        loc = SwingUtilities.convertPoint(component.getParent(), loc, vp);
        vp.scrollRectToVisible(new Rectangle(
                (int)loc.getX(),
                (int)loc.getY(),
                Math.min(component.getWidth(), vp.getWidth() - insets.left - insets.right),
                Math.min(component.getHeight(), vp.getHeight() - insets.top - insets.bottom)));
    }

    /**
     * move given item to last in the list
     */
    public void moveItemToLast(int index)
    {
        panels_.moveItemToLast(index);
        items_.add(items_.remove(index));
    }

    /**
     * swap items
     */
    public void swapItems(int i, int j)
    {
        if (i > j)
        {
            swapItems(j, i);
            return;
        }

        Object iItem = items_.get(i);
        Object jItem = items_.get(j);

        items_.set(i, jItem);
        items_.set(j, iItem);

        ListItemPanel iPanel = panels_.get(i);
        ListItemPanel jPanel = panels_.get(j);

        iPanel.setItem(jItem);
        jPanel.setItem(iItem);

        iPanel.update();
        jPanel.update();

        panels_.update();
    }

    /**
     * add item to list and select
     */
    public void addItem(Object item)
    {
        insertItem(items_.size(), item);
    }

    /**
     * add item and specify whether should be selected
     */
    public void addItem(Object item, boolean selectNew)
    {
        insertItem(items_.size(), item, selectNew);
    }

    /**
     * insert item at selected index and select it
     */
    public void insertItem(Object item)
    {
        insertItem(selectedIndex_, item);
    }

    /**
     * insert item at given index and selected it
     */
    public void insertItem(int index, Object item)
    {
        insertItem(index, item, true);
    }

    /**
     * insert item at given index and specify whether should be selected
     */
    public void insertItem(int index, Object item, boolean selectNew)
    {
        items_.add(index, item);
        addItemPanel(index, item, true);
        if (selectNew) itemSelected(index);
    }

    /**
     * remove selected item
     */
    public void removeSelectedItem()
    {
        removeItem(selectedIndex_);
    }

    /**
     * remove all
     */
    public void removeAllItems()
    {
        panels_.clear();
        items_.clear();
    }

    /**
     * remove item at given index
     */
    public void removeItem(int index)
    {
        items_.remove(index);
        panels_.removePanel(index);

        updateItemPanels();

        if (index == selectedIndex_)
        {
            if (selectedIndex_ >= items_.size())
            {
                itemSelected(items_.size() - 1);
            }
            else
            {
                itemSelected(selectedIndex_);
            }
        }
    }

    /**
     * sort items
     */
    public void sort()
    {
        if (panels_.getNumPanels() == 0) return;

        ListItemPanel iPanel;
        ListItemPanel jPanel;

        int insert;

        // standard insertion sort

        for (int i = items_.size() - 1; i > 0; --i)
        {
            insert = 0;
            iPanel = panels_.get(0);

            for (int j = 1; j <= i; ++j)
            {
                jPanel = panels_.get(j);

                if (jPanel.compareTo(iPanel) < 0)
                {
                    iPanel = jPanel;
                    insert = j;
                }
            }

            if (insert != i)
            {
                swapItems(insert, i);
            }
        }

        setSelectedItem(selectedItem_);

        updateItemPanels();
    }


    /**
     * Update given item's panel
     */
    public void updateItemPanel(int index)
    {
        panels_.get(index).update();
    }

    /**
     * update all item panels
     */
    public void updateItemPanels()
    {
        for (int i = panels_.getNumPanels() - 1; i >= 0; --i)
        {
            updateItemPanel(i);
        }
    }

    /**
     * add listener
     */
    public void addListSelectionListener(ListSelectionListener listener)
    {
        listenerList.add(ListSelectionListener.class, listener);
    }

    /**
     * remove listener
     */
    public void removeListSelectionListener(ListSelectionListener listener)
    {
        listenerList.remove(ListSelectionListener.class, listener);
    }

    /**
     * Get index of item in list
     */
    public int getItemIndex(Object item)
    {
        for (int i = items_.size()-1; i >= 0; --i)
        {
            if (items_.get(i) == item) return i;
        }

        return -1;
    }

    /**
     * add panel for item
     */
    protected void addItemPanel(int index, Object item, boolean bUpdate)
    {
        ListItemPanel itemPanel;

        itemPanel = (ListItemPanel) ConfigUtils.newInstance(
                itemPanelClass_,
                new Class[]{ListPanel.class, java.lang.Object.class, java.lang.String.class},
                new Object[]{this, item, sStyle_});
        itemPanel.setIcon(emptyIcon_);

        panels_.addPanel(itemPanel, index);

        // update this panel, and subsequent panels in case they care about index
        while (index < panels_.getNumPanels())
        {
            panels_.get(index).update();
            index++;
        }

        // if directed, update so layout is correct
        if (bUpdate) panels_.update();
    }

    /**
     * get width of viewport
     */
    public int getItemWidth()
    {
        return getViewport().getWidth();
    }

    /**
     * when resized, need to update panels
     */
    public void componentResized(ComponentEvent e)
    {
        panels_.update();
    }

    // not used
    public void componentMoved(ComponentEvent e) { }

    // not used
    public void componentShown(ComponentEvent e) { }

    // not used
    public void componentHidden(ComponentEvent e) { }

    /**
     * customized panel which uses layout class below
     * as well as itemPanels_ for components and keeps component
     * list in panel in sync with panels list.  We think this is
     * necessary because swing's Container class has lots of
     * stuff which relies on the fact that it knows about all
     * its components
     */
    private class InternalPanel extends DDPanel
    {
        List panels = new ArrayList();

        InternalPanel()
        {
            super(GuiManager.DEFAULT, sStyle_);
            setLayout(new ListLayout(this, 0));
        }

        public void update()
        {
            revalidate();
            doLayout();
            repaint();
        }

        public void clear()
        {
            panels.clear();
            removeAll();
            update();
        }

        public int getNumPanels()
        {
            return panels.size();
        }

        public ListItemPanel get(int i)
        {
            return (ListItemPanel) panels.get(i);
        }

        public void moveItemToLast(int index)
        {
            addPanel(_removePanel(index));
            update();
        }

        /**
         * called should call update() after to ensure
         * layout is proper
         */
        public void addPanel(ListItemPanel panel, int nIndex)
        {
            panels.add(nIndex, panel);
            add(panel, nIndex);

            verify(nIndex);
        }

        public void addPanel(ListItemPanel panel)
        {
            panels.add(panel);
            add(panel);

            verify(panels.size() - 1);
            update();
        }

        public ListItemPanel removePanel(int i)
        {
            verify(i);
            ListItemPanel ret = _removePanel(i);
            update();

            return ret;
        }

        public ListItemPanel _removePanel(int i)
        {
            ListItemPanel ret = (ListItemPanel) panels.remove(i);
            remove(i);
            return ret;
        }

        private void verify(int i)
        {
            Object one = get(i);
            Object two = getComponent(i);
            ApplicationError.assertTrue(one == two, "Mismatch during remove");
        }

    }

    /**
     * customized layout class
     */
    private class ListLayout implements LayoutManager
    {
        InternalPanel panel;
        int vgap;

        public ListLayout(InternalPanel panel, int vgap)
        {
            this.panel = panel;
            this.vgap = vgap;
        }

        public int getVgap()
        {
            return vgap;
        }

        public void setVgap(int vgap)
        {
            this.vgap = vgap;
        }

        public void addLayoutComponent(String name, Component comp)
        {
        }

        public void removeLayoutComponent(Component comp)
        {
        }

        public Dimension preferredLayoutSize(Container parent)
        {
            synchronized (parent.getTreeLock())
            {
                Insets insets = parent.getInsets();
                List itemPanels = panel.panels;
                int nrows = itemPanels.size();

                int w = 0;
                int h = 0;
                for (int i = 0; i < nrows; i++)
                {
                    Component comp = (Component)itemPanels.get(i);
                    Dimension d = comp.getPreferredSize();
                    if (w < d.width)
                    {
                        w = d.width;
                    }
                    h += d.height;
                }
                return new Dimension(insets.left + insets.right + w,
                        insets.top + insets.bottom + h + (nrows - 1) * vgap);
            }
        }

        public Dimension minimumLayoutSize(Container parent)
        {
            synchronized (parent.getTreeLock())
            {
                Insets insets = parent.getInsets();
                List itemPanels = panel.panels;
                int nrows = itemPanels.size();

                int w = 0;
                int h = 0;
                for (int i = 0; i < nrows; i++)
                {
                    Component comp = (Component) itemPanels.get(i);
                    Dimension d = comp.getMinimumSize();
                    if (w < d.width)
                    {
                        w = d.width;
                    }
                    h += d.height;
                }
                return new Dimension(insets.left + insets.right + w,
                        insets.top + insets.bottom + h + (nrows - 1) * vgap);
            }
        }

        public void layoutContainer(Container parent)
        {
            synchronized (parent.getTreeLock())
            {
                Insets insets = parent.getInsets();
                List itemPanels = panel.panels;
                int nrows = itemPanels.size();

                if (nrows == 0)
                {
                    return;
                }

                int w = parent.getWidth() - (insets.left + insets.right);
                int x = insets.left;
                int h;

                Component c;

                for (int r = 0, y = insets.top; r < nrows; r++)
                {
                    c = (Component) itemPanels.get(r);
                    h = c.getPreferredSize().height;
                    c.setBounds(x, y, w, h);
                    y += h + vgap;
                }
            }
        }

        public String toString()
        {
            return getClass().getName() + "[vgap=" + vgap + "]";
        }
    }

    private Component helpPanel_ = null;

    public void setHelpPanel(Component helpPanel)
    {
        helpPanel_ = helpPanel;
    }
}
