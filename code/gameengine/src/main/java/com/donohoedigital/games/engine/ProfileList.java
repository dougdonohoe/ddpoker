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
 * ProfileList.java
 *
 * Created on January 27, 2004, 1:48 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;

/**
 *
 * @author  Doug Donohoe
 */
public abstract class ProfileList extends DDPanel implements AWTEventListener, FocusListener,
                                                             AncestorListener
{
    static Logger logger = Logger.getLogger(ProfileList.class);
    
    public static final String PARAM_PROFILE = "profile";
    public static final String PROFILE_PREFIX = "profilelist-";
    
    protected GameEngine engine_;
    private GameContext context_;
    private String STYLE;
    private List<BaseProfile> profiles_;
    private List<ProfilePanel> profilePanels_ = new ArrayList<ProfilePanel>();
    private DDPanel profilesParent_;
    private DDScrollPane scroll_;
    private BaseProfile selected_ = null;
    private ProfilePanel selectedPanel_ = null;
    private ImageIcon selectedIcon_ = null;
    private ImageIcon emptyIcon_ = ImageConfig.getImageIcon("blank16");
    public DDButton buttonEdit_, buttonNew_, buttonDelete_, buttonCopy_;
    private String sPanelName_;
    private String sMsgName_; // used as suffix for looking up phases/properties
    private boolean bFileMode_ = true;
    private DDPanel buttons_;
    private DDPanel top_;
    private EnginePrefs prefs_;
    private String sPrefName_;
    private boolean bUIMode_ = true;

    /**
     * Creates a new instance of ProfileList
     */
    public ProfileList(GameEngine engine, GameContext context, List<BaseProfile> profiles,
                       String sStyle,
                       String sMsgName,
                       String sPanelName,
                       String sIconName,
                       boolean bUseCopyButton)
    {
        init(engine, context, profiles, sStyle, sMsgName, sPanelName, sIconName, bUseCopyButton);
        
    }
    
    /**
     * Create ProfileList for non-display uses
     */
    public ProfileList(GameEngine engine, GameContext context, String sMsgName)
    {
        bUIMode_ = false;
        init(engine, context, null, null, sMsgName, null, null, false);
    }
    
    /** 
     * get selected profile for given profile type
     */
    public static String getStoredProfile(String sMsgName)
    {
        EnginePrefs prefs = GameEngine.getGameEngine().getPrefsNode();
        return prefs.get(getPrefsName(sMsgName), null);        
    }
    
    /**
     * remember profile
     */
    public static void setStoredProfile(BaseProfile profile, String sMsgName)
    {
        EnginePrefs prefs = GameEngine.getGameEngine().getPrefsNode();
        rememberProfile(profile, prefs, getPrefsName(sMsgName));
        
    }
    
    /**
     * remember profile in prefs
     */
    public void rememberProfile(BaseProfile profile)
    {
        rememberProfile(profile, prefs_, sPrefName_);
    }
    
    /**
     * remember profile
     */
    private static void rememberProfile(BaseProfile profile,
                                        EnginePrefs prefs, String sPrefName)
    {
        if (profile != null)
            prefs.put(sPrefName, profile.getFileName());
        else
            prefs.remove(sPrefName);
    }
    
    /**
     * Return name of prefs entry key
     */
    private static String getPrefsName(String sMsgName)
    {
        return PROFILE_PREFIX+sMsgName;
    }
    
    /**
     * init
     */
    private void init(GameEngine engine, GameContext context, List<BaseProfile> profiles,
                      String sStyle,
                      String sMsgName,
                      String sPanelName,
                      String sIconName,
                      boolean bUseCopyButton)
    {
        
        // store
        context_ = context;
        engine_ = engine;
        prefs_ = engine_.getPrefsNode();
        sPrefName_ = getPrefsName(sMsgName);
        sMsgName_ = "." + sMsgName;
        
        // ui mode only
        if (bUIMode_)
        {
            profiles_ = profiles;
            STYLE = sStyle;
            selectedIcon_ = ImageConfig.getImageIcon(sIconName);
            sPanelName_ = sPanelName;
        
            // focus
            setFocusable(true);
            addFocusListener(this);
            addAncestorListener(this);

            // Profile list
            add(createProfileList(bUseCopyButton), BorderLayout.CENTER);
        }
    }
    
    /**
     * Create list of Profiles
     */
    private JComponent createProfileList(boolean bUseCopy) {
        top_ = new DDPanel(sPanelName_);
        BorderLayout layout = (BorderLayout) top_.getLayout();
        layout.setVgap(13);
        top_.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        // panel with profile info
        profilesParent_ = new DDPanel(sPanelName_, STYLE);
        profilesParent_.setLayout(new GridLayout(0, 1, 0, 1));
        DDPanel formatProfiles = new DDPanel(sPanelName_);
        formatProfiles.add(profilesParent_,BorderLayout.NORTH);

        // scroll
        scroll_ = new DDScrollPane(formatProfiles, STYLE, null,
                                   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setOpaque(false);
        GuiManager.addListeners(scroll_.getViewport());
        GuiManager.addListeners(scroll_);
        scroll_.getVerticalScrollBar().setUnitIncrement(27);
        scroll_.getVerticalScrollBar().setBlockIncrement(27*4);
        top_.add(scroll_, BorderLayout.CENTER);

        // get previous selected
        String sSelected = prefs_.get(sPrefName_, null);

        ProfilePanel pp;
        for (BaseProfile aProfiles_ : profiles_)
        {
            pp = new ProfilePanel(aProfiles_, STYLE);
            profilesParent_.add(pp);
            profilePanels_.add(pp);

            // if selected name matches, remember that
            if (sSelected != null && pp.profile_.getFileName().equals(sSelected))
            {
                selectedPanel_ = pp;
                selected_ = pp.profile_;
            }
        }

        // buttons
        buttons_ = new DDPanel();
        buttons_.setLayout(new GridLayout(1,3,10,5));
        top_.add(GuiUtils.CENTER(buttons_),BorderLayout.SOUTH);

        buttonDelete_ = new ProfileButton("deleteprofile"+sMsgName_);
        buttonEdit_ = new ProfileButton("editprofile"+sMsgName_);
        buttonNew_ = new ProfileButton("newprofile"+sMsgName_);
        if (bUseCopy) buttonCopy_ = new ProfileButton("copyprofile"+sMsgName_);

        buttons_.add(buttonNew_);
        if (bUseCopy) buttons_.add(buttonCopy_);
        buttons_.add(buttonEdit_);
        buttons_.add(buttonDelete_);

        // if no previous selection, select 1st row
        if (selected_ == null)
        {
            select(1);
        }
        // else select that remembered in prefs
        else
        {
            profileSelected(selectedPanel_);
        }
        return top_;
    }
    
    /**
     * get profiles
     */
    public List<BaseProfile> getProfiles() {
        return profiles_;
    }
    
    /**
     * Remove button
     */
    public void removeButton(DDButton button) {
        buttons_.remove(button);
    }
    
    /**
     * Remove all buttons
     */
    public void removeAllButtons()
    {
        top_.remove(buttons_);
    }
    
    /**
     * Get profile panel for given profile
     */
    private ProfilePanel getProfilePanel(BaseProfile profile) {
        ProfilePanel pp;
        for (ProfilePanel aProfilePanels_ : profilePanels_)
        {
            pp = aProfilePanels_;
            if (pp.getProfile() == profile)
            {
                return pp;
            }
        }
        return null;
    }
    
    /**
     * class for profile
     */
    private class ProfilePanel extends ButtonPanel implements ActionListener {
        BaseProfile profile_;
        DDLabel name_;
        
        public ProfilePanel(BaseProfile profile, String sStyle) {
            super(sPanelName_, sStyle, true);
            profile_ = profile;
            
            // use protected border
            bNormal_  = borderProtected_;
            bMouseDown_ = borderProtected_;
            setBorder(bNormal_);
            
            // name
            name_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            name_.addMouseListener(this);
            name_.setIcon(emptyIcon_);
            add(name_, BorderLayout.WEST);
            
            // misc
            addActionListener(this);
            
            // set text
            update();
        }
        
        /**
         * Update text
         */
        public void update() {
            name_.setText(profile_.getName());
        }
        
        /**
         * Get profile associated with this panel
         */
        public BaseProfile getProfile() {
            return profile_;
        }
        
        /**
         * button pressed
         */
        public void actionPerformed(ActionEvent e) 
        {          
            profileSelected(this);
        }
    }
    
    /**
     * inner class for convienence
     */
    private class ProfileButton extends GlassButton implements ActionListener {
        public ProfileButton(String sName) {
            super(sName, "Glass");
            setBorderGap(3,5,3,4);
            addActionListener(this);
        }
        
        /**
         * button press
         */
        public void actionPerformed(ActionEvent e) {
            if (this == buttonNew_) {
                newProfile(null);
            }
            else if (this == buttonDelete_) {
                deleteProfile();
            }
            else if (this == buttonEdit_) {
                editProfile();
            }
            else if (this == buttonCopy_) {
                copyProfile();
            }
        }
        
    }
    
    /**
     * Start of phase
     */
    private void start()
    {
        // listener
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        
        // check button states and focus
        checkButtons();
        
        // if no profiles, show msg
        if (bFileMode_ && profilePanels_.size() == 0) {
            SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    String sMsg = PropertyConfig.getMessage("msg.needprofile"+sMsgName_);
                    EngineUtils.displayInformationDialog(context_, sMsg, "msg.needprofile.title"+sMsgName_, null);
                }
            });
        }
    }
    
    /**
     * Finish
     */
    private void finish()
    {
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }
    
    /**
     * Set file mode - if true, associated files are modified when edit/new/delete/copy occurs.
     * Otherwise, just the UI and profile ArrayList is updated
     */
    public void setFileMode(boolean b) {
        bFileMode_ = b;
    }
    
    /**
     * update based on focus
     */
    private void focus()
    {
        if (selectedPanel_ != null) selectedPanel_.name_.setIcon(hasFocus() ? selectedIcon_ : emptyIcon_);
    }
    
    /**
     * profile selected logic
     */
    private void profileSelected(ProfilePanel pp) {
        // remove icon from old selection
        if (selectedPanel_ != null) {
            selectedPanel_.name_.setIcon(emptyIcon_);
            selectedPanel_.setSelected(false);
        }
        
        // set new selected panel and set icon
        selectedPanel_ = pp;
        
        if (selectedPanel_ != null) {
            focus();
            selectedPanel_.setSelected(true);
            
            // set current selected profile and update stats label
            selected_ = selectedPanel_.profile_;
        }
        else 
        {
            selected_ = null;
        }
        
        // remember selected name in prefs
        rememberProfile(selected_);
        
        // set buttons
        checkButtons();
        
        // request focus
        SwingUtilities.invokeLater(
        new Runnable() {
            public void run() {
                // request focus
                requestFocus();
                
                // scroll to visible (after display so it adjusts for new)
                if (selectedPanel_ != null) {
                    Point loc = selectedPanel_.getLocation();
                    loc = SwingUtilities.convertPoint(selectedPanel_.getParent(), loc, scroll_.getViewport());
                    scroll_.getViewport().scrollRectToVisible(new Rectangle(loc,selectedPanel_.getSize()));
                }
            }
        });
        
        // notify
        fireStateChanged();
    }
    
    /**
     * Return selected profile
     */
    public BaseProfile getSelectedProfile() {
        return selected_;
    }
    
    /**
     * Create empty profile
     */
    protected abstract BaseProfile createEmptyProfile();
    
    /**
     * Return copy of given profile
     */
    protected abstract BaseProfile copyProfile(BaseProfile profile, boolean bForEdit);
    
    /**
     * add a new profile, return profile created.  Usable in non-ui mode.
     * Runs phase named NewProfile.<msgname from constructor> - if
     * sPhaseSuffix is non-null, then .<suffix> is appended to the
     * phase name.
     */
    public BaseProfile newProfile(String sPhaseSuffix) {
        BaseProfile profile = createEmptyProfile();
        
        boolean bAdded = showProfileDialog("NewProfile"+sMsgName_+
                                            (sPhaseSuffix == null ? "" : ("."+sPhaseSuffix)), profile);
        if (bAdded) {
            addProfile(profile);
            return profile;
        }
        return null;
    }
    
    /**
     * Add profile programatically
     */
    public void addProfile(BaseProfile profile) {
        // save
        if (bFileMode_) {
            profile.initFile();
            profile.setCreateDate();
            profile.save();
        }
        
        if (bUIMode_)
        {
            // list
            profiles_.add(profile);
            Collections.sort(profiles_);

            // display panel

            ProfilePanel newpanel = new ProfilePanel(profile, STYLE);
            profilePanels_.add(newpanel);

            reorder(profile);
        }
    }
    
    /**
     * Reorder (sorted) profile list, selecting given profile
     */
    private void reorder(BaseProfile profile) {
        ProfilePanel pp;
        
        // manage parent (remove all and re-add so sort order is good)
        List<ProfilePanel> newPP = new ArrayList<ProfilePanel>();
        profilesParent_.removeAll();
        for (BaseProfile aProfiles_ : profiles_)
        {
            pp = getProfilePanel(aProfiles_);
            profilesParent_.add(pp);
            newPP.add(pp);
        }
        profilesParent_.validate();
        profilesParent_.repaint();
        profilePanels_ = newPP;
        
        profileSelected(getProfilePanel(profile));
    }
    
    /**
     * replace profile in array list
     */
    private void updateProfile(BaseProfile from, BaseProfile to) {
        ProfilePanel pp;
        BaseProfile p;
        for (int i = 0; i < profiles_.size(); i++) {
            p = profiles_.get(i);
            if (p == from) {
                profiles_.set(i, to);
                pp = getProfilePanel(from);
                pp.profile_ = to;
                pp.update();
                break;
            }
        }
        
        Collections.sort(profiles_);
        reorder(to);
    }
    
    /**
     * Edit a Profile
     */
    private void editProfile() {
        BaseProfile old = selected_;
        BaseProfile copy = copyProfile(selected_, true);
        boolean bChanged = showProfileDialog("EditProfile"+sMsgName_, copy);
        if (bChanged) {
            // save file
            if (bFileMode_) {
                copy.setCreateDate(old);
                copy.copyFileInfo(old);
                copy.save();
            }
            
            // update to new version (copy)
            updateProfile(selected_, copy);
        }
    }
    
    /**
     * Copy ad Profile
     */
    private void copyProfile() {
        BaseProfile profile = copyProfile(selected_, false);
        
        boolean bAdded = showProfileDialog("CopyProfile"+sMsgName_, profile);
        if (bAdded) {
            addProfile(profile);
        }
    }

    protected boolean deleteProfile(BaseProfile profile)
    {
        return true;
    }
    
    /**
     * delete a Profile
     */
    private void deleteProfile() {
        if (EngineUtils.displayConfirmationDialog(context_,
        PropertyConfig.getMessage("msg.confirm.deleteprofile"+sMsgName_, Utils.encodeHTML(selected_.getName())))) {
            // Allow the subclass to attempt deletion first.
            if (!deleteProfile(selected_)) {
                return;
            }

            // delete file
            if (bFileMode_) selected_.getFile().delete();
            
            // remove from list
            profiles_.remove(selected_);

            // cleanup UI
            ProfilePanel pp = getProfilePanel(selected_);
            profilePanels_.remove(pp);
            profilesParent_.remove(pp);
            profilesParent_.repaint();
            
            if (profilesParent_.getComponentCount() > 0) {
                profileSelected((ProfilePanel) profilesParent_.getComponent(0));
            }
            else {
                profileSelected(null);
            }
        }
    }
    
    /**
     * profile dialog
     */
    protected boolean showProfileDialog(String sPhase, BaseProfile profile) {
        TypedHashMap params = new TypedHashMap();
        params.setObject(PARAM_PROFILE, profile);
        Phase phase = context_.processPhaseNow(sPhase, params);
        return (Boolean) phase.getResult();
    }
    
    /**
     * set buttons enabled/disabled based on selection
     */
    private void checkButtons() {
        boolean bSelected = (selected_ != null);
        buttonDelete_.setEnabled(bSelected && (selected_.canDelete() || TESTING(EngineConstants.TESTING_PROFILE_EDITABLE)));
        buttonEdit_.setEnabled(bSelected && (selected_.canEdit() || TESTING(EngineConstants.TESTING_PROFILE_EDITABLE)));
        if (buttonCopy_ != null) buttonCopy_.setEnabled(bSelected && (selected_.canCopy() || TESTING(EngineConstants.TESTING_PROFILE_EDITABLE)));
    }
    
    /**
     * Invoked when an event is dispatched in the AWT.
     */
    public void eventDispatched(AWTEvent event) {
        // fast action keys
        if (event instanceof KeyEvent) {
            KeyEvent k = (KeyEvent) event;
            
            // if not pressed or source is a text component, ignore
            if (!hasFocus() || k.getID() != KeyEvent.KEY_PRESSED || k.getSource() instanceof javax.swing.text.JTextComponent
            || k.getSource() instanceof javax.swing.JTabbedPane) return;
            
            switch (k.getKeyCode()) {
                case KeyEvent.VK_UP:
                    select(-1);
                    break;
                    
                case KeyEvent.VK_DOWN:
                    select(1);
                    break;
                    
            }
        }
    }
    
    /**
     * Select next/previous ProfilePanel
     */
    public void select(int nDirection) {
        int nNum = profilePanels_.size();
        if (nNum == 0) return;
        
        int nCurrent = -1;
        for (int i = 0; i < nNum; i++) {
            if (selectedPanel_ == profilePanels_.get(i)) {
                nCurrent = i;
                break;
            }
        }
        
        // nothing currently selected
        if (nCurrent == -1) {
            if (nDirection > 0)
                nCurrent = 0;
            else
                nCurrent = nNum -1;
        }
        else {
            nCurrent += nDirection;
        }
        
        // fix wraparound
        if (nCurrent < 0) nCurrent = nNum - 1;
        if (nCurrent >= nNum) nCurrent = 0;
        
        // do your magic
        profileSelected(profilePanels_.get(nCurrent));
    }
    
    /**
     * init selection when first displayed - triggers profile selection listeners
     */
    public void selectInit()
    {
        profileSelected(selectedPanel_);
    }
    
    private transient ChangeEvent changeEvent;
    
    /**
     * Adds a listener to the list that is notified each time a change
     * to the model occurs.  The source of <code>ChangeEvents</code>
     * delivered to <code>ChangeListeners</code> will be this
     * <code>DDNumberSpinner</code>.
     *
     * @param listener the <code>ChangeListener</code> to add
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
    
    
    /**
     * Removes a <code>ChangeListener</code> from this spinner.
     *
     * @param listener the <code>ChangeListener</code> to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }
    
    
    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this DDNumberSpinner with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(
        ChangeListener.class);
    }
    
    
    /**
     * Sends a <code>ChangeEvent</code>, whose source is this
     * <code>DDNumberSpinner</code>, to each <code>ChangeListener</code>.
     * When a <code>ChangeListener</code> has been added
     * to the spinner, this method method is called each time
     * a <code>ChangeEvent</code> is received from the model.
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }
    
    boolean bHasFocus_ = false;
    
    /** Invoked when a component gains the keyboard focus.
     *
     */
    public void focusGained(FocusEvent e) {
        focus();
        profilesParent_.repaint();
    }
    
    /** Invoked when a component loses the keyboard focus.
     *
     */
    public void focusLost(FocusEvent e) {
        focus();
        profilesParent_.repaint();
    }

    ////
    //// Ancestor listener

    /*
    ** added - call start
    */
    public void ancestorAdded(AncestorEvent event)
    {
        start();
    }

    /**
     * removed - call finsish
     */
    public void ancestorRemoved(AncestorEvent event)
    {
        finish();
    }

    /**
     * Not used
     */
    public void ancestorMoved(AncestorEvent event)
    {
    }
}
