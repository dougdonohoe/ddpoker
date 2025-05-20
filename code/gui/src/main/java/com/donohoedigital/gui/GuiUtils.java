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
package com.donohoedigital.gui;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.StylesConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class GuiUtils
{
    static Logger logger = LogManager.getLogger(GuiUtils.class);

    public static final Color COLOR_SCROLL = StylesConfig.getColor("gui.scroll", null);
    public static final Color COLOR_DISABLED_TEXT = StylesConfig.getColor("gui.text.disabled.fg");
    public static final Color COLOR_FOCUS = new Color(255, 255, 255, 150);
    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);
    static final JTextComponent.KeyBinding[] MAC_CUT_COPY_PASTE = {
            new JTextComponent.KeyBinding(
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK),
                    DefaultEditorKit.copyAction),
            new JTextComponent.KeyBinding(
                    KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK),
                    DefaultEditorKit.pasteAction),
            new JTextComponent.KeyBinding(
                    KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK),
                    DefaultEditorKit.cutAction),
    };

    public static void setTheme(MetalTheme theme)
    {
        MetalLookAndFeel.setCurrentTheme(theme);
        UIManager.put("Label.disabledForeground", COLOR_DISABLED_TEXT);
        UIManager.put("RadioButton.disabledText", COLOR_DISABLED_TEXT);
        UIManager.put("ToggleButton.disabledText", COLOR_DISABLED_TEXT);
        UIManager.put("CheckBox.disabledText", COLOR_DISABLED_TEXT);
        UIManager.put("Button.disabledText", COLOR_DISABLED_TEXT);

        UIManager.put("RadioButton.focus", COLOR_FOCUS);
        UIManager.put("ToggleButton.focus", COLOR_FOCUS);
        UIManager.put("CheckBox.focus", COLOR_FOCUS);

        if (COLOR_SCROLL != null)
        {
            UIManager.put("ScrollBar.thumb", COLOR_SCROLL);
            UIManager.put("ScrollBar.thumbShadow", COLOR_SCROLL.darker());
            UIManager.put("ScrollBar.thumbHighlight", COLOR_SCROLL.brighter());
        }

        Color modalborder = StylesConfig.getColor("modal.border");
        UIManager.put("OptionPane.questionDialog.border.background", modalborder);
        UIManager.put("InternalFrame.border",
                      new UIDefaults.ProxyLazyValue("com.donohoedigital.gui.GuiUtils$InternalFrameBorder"));
    }

    /**
     * Copy to clipboard
     */
    public static void copyToClipboard(String sText)
    {
        StringSelection value = new StringSelection(sText);
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(value, null);
    }

    /**
     * Is this mouse event a popup trigger?
     */
    public static boolean isPopupTrigger(MouseEvent e, boolean bAllowLeftClick)
    {
        if (bAllowLeftClick) return true;

        if (Utils.ISMAC && e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())
        {
            return true;
        }

        return e.getButton() != MouseEvent.BUTTON1;

    }

    /**
     * Return DDComponent from this object.
     */
    public static DDComponent getDDComponent(Object o)
    {
        if (o instanceof DDComponent) return (DDComponent) o;

        if (o instanceof Component)
        {
            Component c = (Component) o;
            c = c.getParent();
            while (c != null)
            {
                if (c instanceof DDComponent) return (DDComponent) c;
                c = c.getParent();
            }
        }

        return null;
    }

    /**
     * Return component (or its nearest ancestor) that is opaque and
     * has a completely opaque background color
     */
    public static Component getSolidRepaintComponent(Component c)
    {
        Color color;
        while (c != null)
        {
            if (c.isOpaque())
            {
                color = c.getBackground();
                // if color is null, most likely repainting before everything 
                // is initialized, so just return this component
                if (color == null || color.getTransparency() == Transparency.OPAQUE)
                {
                    return c;
                }
            }

            c = c.getParent();
        }

        return null;
    }

    /**
     * Set preferred height of a component, keeping preferred width
     */
    public static void setPreferredHeight(JComponent c, int height)
    {
        c.setPreferredSize(new Dimension((int) c.getPreferredSize().getWidth(), height));
    }

    /**
     * Set preferred width of a component, keeping preferred height
     */
    public static void setPreferredWidth(JComponent c, int width)
    {
        c.setPreferredSize(new Dimension(width, (int) c.getPreferredSize().getHeight()));
    }

    /**
     * Print container and all the children of the container
     */
    public static void printChildren(Container container, int nIndent)
    {
        if (nIndent == 0) {
            logger.debug("===============================================================================================");
        }
        logger.debug(indent(nIndent) + "Container: " + container);
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            if (aChildren instanceof Container)
            {
                printChildren((Container) aChildren, nIndent + 1);
            }
            else
            {
                logger.debug(indent(nIndent) + "  child: " + aChildren);
            }
        }
    }

    private static String indent(int nIndent)
    {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < nIndent; i++)
        {
            indent.append("    ");
        }
        return indent.toString();
    }

    /**
     * Sets font of all children of container to font (container itself is
     * not set to prevent infinite loops if called from setFont itself)
     */
    public static void setFontChildren(Container container, Font font)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            aChildren.setFont(font);
            if (aChildren instanceof Container)
            {
                setFontChildren((Container) aChildren, font);
            }
        }
    }

    /**
     * Sets background of all children of container to color (container itself is
     * not set to prevent infinite loops if called from setBackground itself)
     */
    public static void setBackgroundChildren(Container container, Color color)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            aChildren.setBackground(color);
            if (aChildren instanceof Container)
            {
                setBackgroundChildren((Container) aChildren, color);
            }
        }
    }

    /**
     * Sets foreground of all children of container to color (container itself is
     * not set to prevent infinite loops if called from setForeground itself).
     * If the child is a JTextComponent (or subclass), the caret color is also set
     */
    public static void setForegroundChildren(Container container, Color color)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            aChildren.setForeground(color);
            if (aChildren instanceof JTextComponent)
            {
                ((JTextComponent) aChildren).setCaretColor(color);
            }
            if (aChildren instanceof Container)
            {
                setForegroundChildren((Container) aChildren, color);
            }
        }
    }

    /**
     * add mouse listener to all children components
     */
    public static void addMouseListenerChildren(Container container, MouseListener mouse)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            aChildren.addMouseListener(mouse);
            if (aChildren instanceof Container)
            {
                addMouseListenerChildren((Container) aChildren, mouse);
            }
        }
    }

    /**
     * remove mouse listener from all children components
     */
    public static void removeMouseListenerChildren(Container container, MouseListener mouse)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            aChildren.removeMouseListener(mouse);
            if (aChildren instanceof Container)
            {
                removeMouseListenerChildren((Container) aChildren, mouse);
            }
        }
    }

    /**
     * set border on all children
     */
    public static void setBorderChildren(Container container, Border border)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            if (aChildren instanceof JComponent)
            {
                ((JComponent) aChildren).setBorder(border);
            }
            if (aChildren instanceof Container)
            {
                setBorderChildren((Container) aChildren, border);
            }
        }
    }

    /**
     * Set focusable on all children
     */
    public static void setFocusableChildren(Container container, boolean b)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            if (aChildren instanceof JComponent)
            {
                aChildren.setFocusable(b);
                aChildren.setFocusTraversalKeysEnabled(b);
            }
            if (aChildren instanceof Container)
            {
                setFocusableChildren((Container) aChildren, b);
            }
        }
    }

    /**
     * Set opaque on all appropriate children
     */
    public static void setOpaqueChildren(Container container, boolean b)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            if (aChildren instanceof JPanel ||
                aChildren instanceof JLabel ||
                aChildren instanceof Box ||
                aChildren instanceof Box.Filler)
            {
                ((JComponent) aChildren).setOpaque(b);
            }
            if (aChildren instanceof Container)
            {
                setOpaqueChildren((Container) aChildren, b);
            }
        }
    }

    /**
     * Sets DD Panel debug flag true on all children
     */
    public static Container setDDPanelDebug(Container panel, boolean debug)
    {
        if (panel instanceof DDPanel){
            ((DDPanel)panel).setDebug(debug);
        }
        Component[] children = panel.getComponents();
        for (Component aChildren : children)
        {
            if (aChildren instanceof Container)
            {
                setDDPanelDebug((Container) aChildren, debug);
            }
        }
        return panel;
    }

    /**
     * add mouse listener to all children components
     */
    public static void addPropertyChangeListenerChildren(Container container, PropertyChangeListener mouse)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            aChildren.addPropertyChangeListener(mouse);
            if (aChildren instanceof Container)
            {
                addPropertyChangeListenerChildren((Container) aChildren, mouse);
            }
        }
    }

    /**
     * fill array with all DDOptions in the hierarchy
     */
    public static void getDDOptions(Container container, List<DDOption> options)
    {
        Component[] children = container.getComponents();
        for (Component aChildren : children)
        {
            if (aChildren instanceof DDOption)
            {
                DDOption dd = (DDOption) aChildren;
                if (!dd.isIgnored())
                {
                    options.add(dd);
                }
            }
            if (aChildren instanceof Container)
            {
                getDDOptions((Container) aChildren, options);
            }
        }
    }

    /**
     * Set the labels of all DDOptions under this container to the same width
     */
    public static void setDDOptionLabelWidths(Container container)
    {
        List<DDOption> options = new ArrayList<DDOption>();
        getDDOptions(container, options);

        int nNum = options.size();
        int nMaxWidth = 0;
        DDOption dd;
        JComponent label;
        for (int i = 0; i < nNum; i++)
        {
            dd = options.get(i);
            label = dd.getLabelComponent();
            if (label == null) continue;
            nMaxWidth = Math.max(nMaxWidth, label.getPreferredSize().width);
        }

        Dimension pref;
        for (int i = 0; i < nNum; i++)
        {
            dd = options.get(i);
            label = dd.getLabelComponent();
            if (label == null) continue;
            pref = label.getPreferredSize();
            pref.width = nMaxWidth;
            label.setPreferredSize(pref);
        }
    }

    /**
     * Simple routine to take the SVG path string and draw it in the given path.
     */
    public static GeneralPath drawSVGpath(String sPath, boolean bMoveTo0x0)
    {
        GeneralPath path = new GeneralPath();
        StringTokenizer st = new StringTokenizer(sPath, "McCsSzZhHvVlL,-", true);
        String token;
        float nums[] = new float[6];
        int idx = 0;
        int needed = 0;
        boolean bRelative = false;
        boolean bNegative = false;
        boolean bRelativeLast = false;
        char type = 'M';
        char typelast = 'M';
        Point2D currentPointLast = new Point2D.Double(0, 0);
        char first;
        while (st.hasMoreTokens())
        {
            token = st.nextToken();
            first = token.charAt(0);
            if (token.length() == 1 && (first < '0' || first > '9'))
            {
                switch (first)
                {
                    // TODO: implement m, qQ, tT, aA
                    case 'M':
                        needed = 2;
                        idx = 0;
                        bRelative = false;
                        type = 'M';
                        break;

                    case 'c':
                    case 'C':
                        bRelative = first == 'c';
                        needed = 6;
                        idx = 0;
                        type = 'C';
                        break;

                    case 's':
                    case 'S':
                        bRelative = first == 's';
                        needed = 4;
                        idx = 2;

                        // see http://www.w3.org/TR/SVG/paths.html#PathDataCubicBezierCommands
                        Point2D p = path.getCurrentPoint();
                        Point2D last2ndControl = new Point2D.Double();
                        Point2D new1stControl;

                        // for previous command of type S or C
                        if (typelast == 'S' || typelast == 'C')
                        {
                            // figure out absolute coordinates of previous control point
                            if (bRelativeLast)
                            {
                                last2ndControl.setLocation(currentPointLast.getX() + nums[2],
                                                           currentPointLast.getY() + nums[3]);
                            }
                            else
                            {
                                last2ndControl.setLocation(nums[2], nums[3]);
                            }

                            // 1st control point is reflection of 2nd control point from previous
                            // command relative to current point
                            new1stControl = new Point2D.Double(p.getX() + (p.getX() - last2ndControl.getX()),
                                                               p.getY() + (p.getY() - last2ndControl.getY()));
                        }
                        else
                        {
                            // per SVG spec, if last command wasn't s,S,c,C, assume
                            // first control point is co-incident with current point
                            new1stControl = p;
                        }

                        // set first control point, adjusting for whether this is
                        // a relative command or not
                        if (bRelative)
                        {
                            nums[0] = (float) (new1stControl.getX() - p.getX());
                            nums[1] = (float) (new1stControl.getY() - p.getY());
                        }
                        else
                        {
                            nums[0] = (int) new1stControl.getX();
                            nums[1] = (int) new1stControl.getY();
                        }

                        type = 'C';
                        break;

                    case 'l':
                    case 'L':
                        bRelative = first == 'l';
                        needed = 2;
                        idx = 0;
                        type = 'L';
                        break;

                    case 'h':
                    case 'H':
                        bRelative = first == 'h';
                        needed = 1;
                        idx = 0;
                        type = 'H';
                        break;

                    case 'v':
                    case 'V':
                        bRelative = first == 'v';
                        needed = 1;
                        idx = 0;
                        type = 'V';
                        break;

                    case 'z':
                    case 'Z':
                        path.closePath();
                        break;

                    case ',':
                        break;

                    case '-':
                        bNegative = true;
                        break;
                }
            }
            else
            {
                float num = Float.parseFloat(token);
                if (bNegative)
                {
                    num *= -1;
                    bNegative = false;
                }
                nums[idx++] = num;
                needed--;

                if (needed == 0)
                {
                    currentPointLast = path.getCurrentPoint();
                    if (type == 'M')
                    {
                        path.moveTo(nums[0], nums[1]);
                    }
                    else if (type == 'C')
                    {
                        float xstart = 0;
                        float ystart = 0;
                        if (bRelative)
                        {
                            xstart = (float) currentPointLast.getX();
                            ystart = (float) currentPointLast.getY();
                        }
                        path.curveTo(xstart + nums[0], ystart + nums[1],
                                     xstart + nums[2], ystart + nums[3],
                                     xstart + nums[4], ystart + nums[5]);
                    }
                    else if (type == 'L')
                    {
                        float xstart = 0;
                        float ystart = 0;
                        if (bRelative)
                        {
                            xstart = (float) currentPointLast.getX();
                            ystart = (float) currentPointLast.getY();
                        }
                        path.lineTo(xstart + nums[0], ystart + nums[1]);
                    }
                    else if (type == 'H')
                    {
                        float xstart = 0;
                        float ystart = (float) currentPointLast.getY();
                        if (bRelative)
                        {
                            xstart = (float) currentPointLast.getX();
                        }
                        path.lineTo(xstart + nums[0], ystart);
                    }
                    else if (type == 'V')
                    {
                        float xstart = (float) currentPointLast.getX();
                        float ystart = 0;
                        if (bRelative)
                        {
                            ystart = (float) currentPointLast.getY();
                        }
                        path.lineTo(xstart, ystart + nums[0]);
                    }

                    bRelativeLast = bRelative;
                    typelast = type;
                }
            }
        }

        // shift path to eliminate "whitespace" on top/left
        if (bMoveTo0x0)
        {
            Rectangle2D bounds = path.getBounds2D();
            AffineTransform tx = AffineTransform.getTranslateInstance(-bounds.getMinX(), -bounds.getMinY());
            path.transform(tx);
        }
        return path;
    }

    /**
     * invoke SwingIt logic if in event dispatch thread, otherwise invokeLater
     */
    public static void invoke(Runnable swingit)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            swingit.run();
        }
        else
        {
            SwingUtilities.invokeLater(swingit);
        }
    }

    /**
     * invoke SwingIt logic if in event dispatch thread, otherwise invokeAndWait
     */
    public static void invokeAndWait(Runnable swingit)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            swingit.run();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(swingit);
            }
            catch (InvocationTargetException e)
            {
                logger.error("invokeAndWait failed", e);
            }
            catch (InterruptedException e)
            {
                Thread.interrupted();
            }
        }
    }

    /**
     * invoke SwingIt logic later (regardless if current thread is swing thread)
     */
    public static void invokeLater(Runnable swingit)
    {
        SwingUtilities.invokeLater(swingit);
    }

    /**
     * Add standard escape key actions to the component - action is
     * invoked when one of DELETE / BACKSPACE / SPACE are pressed.
     */
    public static void addEscapeKeyActions(JComponent comp, AbstractAction action)
    {
        addKeyAction(comp, JComponent.WHEN_IN_FOCUSED_WINDOW,
                     "endmodaldelete", action,
                     KeyEvent.VK_DELETE, 0);
        addKeyAction(comp, JComponent.WHEN_IN_FOCUSED_WINDOW,
                     "endmodalbackspace", action,
                     KeyEvent.VK_BACK_SPACE, 0);
        addKeyAction(comp, JComponent.WHEN_IN_FOCUSED_WINDOW,
                     "endmodalspace", action,
                     KeyEvent.VK_SPACE, 0);
    }

    /**
     * Remove standard escape key actions
     */
    public static void removeEscapeKeyActions(JComponent comp)
    {
        removeKeyAction(comp, JComponent.WHEN_IN_FOCUSED_WINDOW,
                        "endmodaldelete",
                        KeyEvent.VK_DELETE, 0);
        removeKeyAction(comp, JComponent.WHEN_IN_FOCUSED_WINDOW,
                        "endmodalbackspace",
                        KeyEvent.VK_BACK_SPACE, 0);
        removeKeyAction(comp, JComponent.WHEN_IN_FOCUSED_WINDOW,
                        "endmodalspace",
                        KeyEvent.VK_SPACE, 0);
    }

    /**
     * Method to add a key action to a component using the ActionMap/InputMap
     * way.
     */
    public static void addKeyAction(JComponent comp, int nWhen,
                                    String sActionName, Action action,
                                    int key, int key_mods)
    {
        comp.getActionMap().put(sActionName, action);
        comp.getInputMap(nWhen).put(KeyStroke.getKeyStroke(key, key_mods), sActionName);
    }

    /**
     * Remove a key action
     */
    public static void removeKeyAction(JComponent comp, int nWhen,
                                       String sActionName,
                                       int key, int key_mods)
    {
        comp.getActionMap().remove(sActionName);
        comp.getInputMap(nWhen).remove(KeyStroke.getKeyStroke(key, key_mods));
    }

    /**
     * Hyperlink handler
     */
    public static final HyperlinkListener HYPERLINK_HANDLER = new HyperLinkHandler();

    /**
     * hyperlink implementation
     */
    private static class HyperLinkHandler implements HyperlinkListener
    {
        public void hyperlinkUpdate(HyperlinkEvent e)
        {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
            String sURL = e.getDescription();
            Utils.openURL(sURL);
        }
    }

    /**
     * Action for invoking a button
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class InvokeButton extends AbstractAction
    {
        DDButton button_;

        public InvokeButton(DDButton button)
        {
            button_ = button;
        }

        public void actionPerformed(ActionEvent e)
        {
            if (button_ != null && button_.isEnabled())
            {
                button_.doClick(120);
            }
        }
    }

    ////
    //// Layout Helpers
    ////

    // debugging borders
    public static final Border REDBORDER = BorderFactory.createLineBorder(Color.red);
    public static final Border GREENBORDER = BorderFactory.createLineBorder(Color.green);
    public static final Border CYANBORDER = BorderFactory.createLineBorder(Color.cyan);
    public static final Border BLUEBORDER = BorderFactory.createLineBorder(Color.blue);
    public static final Border BLACKBORDER = BorderFactory.createLineBorder(Color.black);
    public static final Border GRAYBORDER = BorderFactory.createLineBorder(Color.darkGray);

    public static JComponent CENTER(JComponent c)
    {
        DDPanel center = DDPanel.CENTER();
        center.add(c);
        return center;
    }

    public static JComponent NORTH(JComponent c)
    {
        DDPanel north = new DDPanel();
        north.add(c, BorderLayout.NORTH);
        return north;
    }

    public static JComponent SOUTH(JComponent c)
    {
        DDPanel south = new DDPanel();
        south.add(c, BorderLayout.SOUTH);
        return south;
    }

    public static JComponent WEST(JComponent c)
    {
        DDPanel west = new DDPanel();
        west.add(c, BorderLayout.WEST);
        return west;
    }

    public static JComponent WEST_SOUTH(JComponent west, JComponent south, int vgap)
    {
        DDPanel swest = new DDPanel();
        swest.add(west, BorderLayout.WEST);
        swest.add(south, BorderLayout.SOUTH);
        BorderLayout layout = (BorderLayout) swest.getLayout();
        layout.setVgap(vgap);
        return swest;
    }

    public static JComponent EAST(JComponent c)
    {
        DDPanel east = new DDPanel();
        east.add(c, BorderLayout.EAST);
        return east;
    }

    ////
    //// Things copied from swing since they aren't public
    ////

    /**
     * This draws the "Flush 3D Border" which is used throughout the Metal L&F
     */
    public static void drawFlush3DBorder(Graphics g, Component c, int x, int y, int w, int h)
    {
        Color back = c.getBackground();
        g.translate(x, y);
        g.setColor(back.darker());// MetalLookAndFeel.getControlDarkShadow() );
        g.drawRect(0, 0, w - 2, h - 2);
        g.setColor(back.brighter());//;//MetalLookAndFeel.getControlHighlight() );
        g.drawRect(1, 1, w - 2, h - 2);
        g.setColor(back);// MetalLookAndFeel.getControl() );
        g.drawLine(0, h - 1, 1, h - 2);
        g.drawLine(w - 1, 0, w - 2, 1);
        g.translate(-x, -y);
    }

    /**
     * This draws a variant "Flush 3D Border"
     * It is used for things like pressed buttons.
     */
    public static void drawPressed3DBorder(Graphics g, Component c, int x, int y, int w, int h)
    {
        g.translate(x, y);

        drawFlush3DBorder(g, c, 0, 0, w, h);

        g.setColor(c.getBackground());//MetalLookAndFeel.getControlShadow() );
        g.drawLine(1, 1, 1, h - 2);
        g.drawLine(1, 1, w - 2, 1);
        g.translate(-x, -y);
    }

    /**
     * Class to help debug focus issues
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class FocusDebugger implements FocusListener
    {
        String sName;

        public FocusDebugger(String sName)
        {
            this.sName = sName;
        }

        public void focusGained(FocusEvent e)
        {
            logger.debug(sName + " focus gained from " + e.getOppositeComponent());
        }

        public void focusLost(FocusEvent e)
        {
            logger.debug(sName + " focus lost to " + e.getOppositeComponent());
        }
    }

    @SuppressWarnings({"PublicField"})
    public static boolean TESTING_BUTTON = false;
    private static int DSEQ = 0;
    private static final Object SEQOBJ = new Object();

    /**
     * Return next seq id
     */
    protected static int nextSEQ()
    {
        if (!GuiUtils.TESTING_BUTTON) return 0;
        synchronized (SEQOBJ)
        {
            DSEQ++;
            return DSEQ;
        }
    }

    /**
     * log a message
     */
    protected static void log(String sMethod, String sMsg)
    {
        logger.debug(sMethod + " [" + nextSEQ() + "] " + (sMsg == null ? "" : sMsg));
    }

    /**
     * return true if swing thread
     */
    public static boolean isSwingThread()
    {
        return (SwingUtilities.isEventDispatchThread() || Thread.currentThread() == BaseApp.mainThread_);
    }

    /**
     * Throw exception if not swing thread
     */
    public static void requireSwingThread()
    {
        if (!isSwingThread())
            throw new ApplicationError("Updating from non-swing thread: " + Thread.currentThread().getName());
    }

    /**
     * get scroll parent
     */
    public static JScrollPane getScrollParent(JComponent c)
    {
        if (c == null) return null;

        Container p = c.getParent();
        while (p != null && !(p instanceof JScrollPane))
        {
            p = p.getParent();
        }
        return (JScrollPane) p;
    }

    /**
     * get first DDWindow parent of this component
     * (will find InternalWindow before the parent Frame)
     */
    public static DDWindow getHelpManager(Component c)
    {
        if (c == null) return null;

        Container p = c.getParent();
        while (p != null && !(p instanceof DDWindow))
        {
            p = p.getParent();
        }
        return (DDWindow) p;
    }

    /**
     * get BaseFrame this component is in
     */
    public static BaseFrame getBaseFrame(Component c)
    {
        if (c == null) return null;

        Container p = c.getParent();
        while (p != null && !(p instanceof BaseFrame))
        {
            p = p.getParent();
        }
        return (BaseFrame) p;
    }

    /**
     * get InternalDialog this component is in
     */
    public static InternalDialog getInternalDialog(Component c)
    {
        if (c == null) return null;

        Container p = c.getParent();
        while (p != null && !(p instanceof InternalDialog))
        {
            p = p.getParent();
        }
        return (InternalDialog) p;
    }

    /**
     * do we draw this component anti-aliased?
     * (looks bad when antialiased in smaller fonts)
     */
    public static boolean drawAntiAlias(JComponent c)
    {
        return drawAntiAlias(c.getFont(), 1.0d);
    }

    /**
     * do we draw this font anti-aliased at given scale?
     * (looks bad when antialiased in smaller fonts)
     */
    public static boolean drawAntiAlias(Font f, double scale)
    {
        //if (Utils.ISMAC)  return true;
        return (f.getSize() * scale > 12.0d);
    }

    ////
    //// DDOption Helper interface
    ////
    @SuppressWarnings({"PublicInnerClass"})
    public static interface CheckListener
    {
        void addListeners(List<DDOption> options);
    }

    /**
     * Internal frame border - copy to adjust how border is drawn to avoid
     * white dots at corner
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static class InternalFrameBorder extends AbstractBorder implements UIResource
    {

        private static final Insets insets = new Insets(5, 5, 5, 5);

        private static final int corner = 14;

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int w, int h)
        {

            Color background;
            Color highlight;
            Color shadow;

            if (c instanceof JInternalFrame && ((JInternalFrame) c).isSelected())
            {
                background = MetalLookAndFeel.getPrimaryControlDarkShadow();
                highlight = MetalLookAndFeel.getPrimaryControlShadow();
                shadow = MetalLookAndFeel.getPrimaryControlInfo();
            }
            else
            {
                background = MetalLookAndFeel.getControlDarkShadow();
                highlight = MetalLookAndFeel.getControlShadow();
                shadow = MetalLookAndFeel.getControlInfo();
            }

            g.setColor(background);
            g.fillRect(0, 0, w - 1, h - 1); // JDD - simpler, no white dots at corners
            g.setColor(background.darker()); // JDD
            g.drawRect(0, 0, w - 1, h - 1); // JDD
            // Draw outermost lines
//             g.setColor(Color.red);
//              g.drawLine( 1, 0, w-2, 0);
//            g.setColor(Color.blue);
//              g.drawLine( 0, 1, 0, h-2);
//            g.setColor(Color.cyan);
//              g.drawLine( w-1, 1, w-1, h-2);
//            g.setColor(Color.yellow);
//              g.drawLine( 1, h-1, w-2, h-1);

            // Draw the bulk of the border
//              for (int i = 1; i < 5; i++) {
//	          g.drawRect(x+i,y+i,w-(i*2)-1, h-(i*2)-1);
//              }

            if (c instanceof JInternalFrame &&
                ((JInternalFrame) c).isResizable())
            {
                g.setColor(highlight);
                // Draw the Long highlight lines
                g.drawLine(corner + 1, 3, w - corner, 3);
                g.drawLine(3, corner + 1, 3, h - corner);
                g.drawLine(w - 2, corner + 1, w - 2, h - corner);
                g.drawLine(corner + 1, h - 2, w - corner, h - 2);

                g.setColor(shadow);
                // Draw the Long shadow lines
                g.drawLine(corner, 2, w - corner - 1, 2);
                g.drawLine(2, corner, 2, h - corner - 1);
                g.drawLine(w - 3, corner, w - 3, h - corner - 1);
                g.drawLine(corner, h - 3, w - corner - 1, h - 3);
            }
        }

        @Override
        public Insets getBorderInsets(Component c)
        {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets newInsets)
        {
            newInsets.top = insets.top;
            newInsets.left = insets.left;
            newInsets.bottom = insets.bottom;
            newInsets.right = insets.right;
            return newInsets;
        }
    }

    /**
     * print to jpg image, scaling image into width x height, return BufferedImage for use by printImageToFile
     */
    public static BufferedImage printToImage(JComponent c, int width, int height)
    {
        double w = (double) width / (Math.max((double) c.getWidth(), width));
        double h = (double) height / (Math.max((double) c.getHeight(), height));

        double dScale = Math.min(w, h);

        BufferedImage image = new BufferedImage((int) (c.getWidth() * dScale), (int) (c.getHeight() * dScale),
                                                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        g.scale(dScale, dScale);
        c.print(g);
        return image;
    }

    /**
     * Write BufferedImage to a file
     */
    public static void printImageToFile(BufferedImage image, File file)
    {
        try
        {
            FileOutputStream out = new FileOutputStream(file);
            ImageIO.write(image, "jpg", out);
            out.close();
        }
        catch (IOException ioe)
        {
            logger.error(Utils.formatExceptionText(ioe));
        }
    }
}
