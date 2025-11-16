/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.arranger.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Action;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.arranger.ArrangerPanel;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * Arranger Top component
 */
@ConvertAsProperties(
        dtd = "-//org.jjazz.arranger.api//Arranger//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "ArrangerTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "jlnavigator", openAtStartup = false, position = 40)
@ActionID(category = "Window", id = "org.jjazz.arranger.api.ArrangerTopComponent")
@ActionReference(path = "Menu/Tools", position = 30)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ArrangerAction",
        preferredID = "ArrangerTopComponent"
)
public final class ArrangerTopComponent extends TopComponent implements PropertyChangeListener
{

    private final ArrangerPanel arrangerPanel;

    public ArrangerTopComponent()
    {
        setName(ResUtil.getString(getClass(), "CTL_ArrangerTopComponent"));
        setToolTipText(ResUtil.getString(getClass(), "HINT_ArrangerTopComponent"));
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);


        initComponents();

        arrangerPanel = new ArrangerPanel();
        add(arrangerPanel);

    }

    /**
     * @return The actions to be shown in the TopComponent popup menu.
     */
    @Override
    public Action[] getActions()
    {
        var res = UIUtilities.getNetbeansTopComponentTabActions(super.getActions());
        return res.toArray(Action[]::new);
    }

    // ================================================================================    
    // PropertyChangeListener interface
    // ================================================================================   

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                arrangerPanel.setModel((Song) evt.getNewValue(), (MidiMix) evt.getOldValue());    // Both values can be null in the same time
            }
        }
    }

    @Override
    public void componentOpened()
    {
        arrangerPanel.opened();

        // Listen to active song changes
        var asm = ActiveSongManager.getDefault();
        asm.addPropertyListener(this);
        arrangerPanel.setModel(asm.getActiveSong(), asm.getActiveMidiMix());
    }

    @Override
    public void componentClosed()
    {
        arrangerPanel.closing();
        ActiveSongManager.getDefault().removePropertyListener(this);
    }


    /**
     *
     * @return Can be null
     */
    static public ArrangerTopComponent getInstance()
    {
        return (ArrangerTopComponent) WindowManager.getDefault().findTopComponent("ArrangerTopComponent");
    }

    void writeProperties(java.util.Properties p)
    {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p)
    {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
