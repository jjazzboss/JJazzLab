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
package org.jjazz.improvisionsupport.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.improvisionsupport.BR_ImproSupport;
import org.jjazz.improvisionsupport.ImproSupport;
import org.jjazz.improvisionsupport.ImproSupportPanel;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.jjazz.improvisationsupport.api//ImproSupport//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "ImproSupportTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "jlnavigator", openAtStartup = false, position = 30)
@ActionID(category = "Window", id = "org.jjazz.improvisionsupport.api.ImproSupportTopComponent")
@ActionReference(path = "Menu/Tools", position = 25)
@TopComponent.OpenActionRegistration(displayName = "#CTL_ImproSupportAction", preferredID = "ImproSupportTopComponent")
public final class ImproSupportTopComponent extends TopComponent implements PropertyChangeListener
{

    private final Map<Integer, ImproSupport> mapSongImproSupport = new HashMap<>();
    private final ImproSupportPanel improSupportPanel;
    private static final Logger LOGGER = Logger.getLogger(ImproSupportTopComponent.class.getName());

    public ImproSupportTopComponent()
    {
        setName(ResUtil.getString(getClass(), "CTL_ImproSupportTopComponent"));
        setToolTipText(ResUtil.getString(getClass(), "HINT_ImproSupportTopComponent"));
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);


        initComponents();


        improSupportPanel = new ImproSupportPanel();
        add(improSupportPanel);

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
                activeSongChanged((Song) evt.getNewValue());
            }
        } else if (evt.getSource() instanceof Song)
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                songClosed((Song) evt.getSource());
            }
        }
    }

    @Override
    public void componentOpened()
    {
        improSupportPanel.opened();

        // Listen to active song changes
        var asm = ActiveSongManager.getDefault();
        asm.addPropertyListener(this);
        activeSongChanged(asm.getActiveSong());
    }

    @Override
    public void componentClosed()
    {
        improSupportPanel.closing();

        var asm = ActiveSongManager.getDefault();
        asm.removePropertyListener(this);
    }

    public ImproSupportPanel getImproSupportPanel()
    {
        return improSupportPanel;
    }

    /**
     *
     * @return Can be null
     */
    static public ImproSupportTopComponent getInstance()
    {
        return (ImproSupportTopComponent) WindowManager.getDefault().findTopComponent("ImproSupportTopComponent");
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
    private void songClosed(Song song)
    {
        song.removePropertyChangeListener(this);
        BR_ImproSupport.removeBR_ImproSupportInstances(song);
        var improSupport = mapSongImproSupport.get(System.identityHashCode(song));
        if (improSupport != null)
        {
            improSupport.cleanup();
            mapSongImproSupport.remove(System.identityHashCode(song));
        }
    }

    private void activeSongChanged(Song sg)
    {
        ImproSupport improSupport = null;

        if (sg != null)
        {
            CL_EditorTopComponent clTc = CL_EditorTopComponent.get(sg.getChordLeadSheet()); // Can be null in some cases eg when rhythm previewer is used
            if (clTc != null)
            {
                var clEditor = clTc.getEditor();
                improSupport = mapSongImproSupport.get(System.identityHashCode(sg));
                if (improSupport == null)
                {
                    improSupport = new ImproSupport(clEditor);
                    mapSongImproSupport.put(System.identityHashCode(sg), improSupport);
                    sg.addPropertyChangeListener(this);
                }
            }
        }

        improSupportPanel.setModel(improSupport);
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
