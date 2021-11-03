/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.improvisionsupport.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.improvisionsupport.BR_ImproSupport;
import org.jjazz.improvisionsupport.ImproSupport;
import org.jjazz.improvisionsupport.ImproSupportPanel;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.util.api.ResUtil;
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
@ActionReference(path = "Menu/Window", position = 30)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ImproSupportAction",
        preferredID = "ImproSupportTopComponent"
)
public final class ImproSupportTopComponent extends TopComponent implements PropertyChangeListener
{

    private final Map<CL_Editor, ImproSupport> mapEditorImproSupport = new HashMap<>();
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
    // ================================================================================    
    // PropertyChangeListener interface
    // ================================================================================   

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == ActiveSongManager.getInstance())
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
        var asm = ActiveSongManager.getInstance();
        asm.addPropertyListener(this);
        activeSongChanged(asm.getActiveSong());
    }

    @Override
    public void componentClosed()
    {
        improSupportPanel.closing();

        var asm = ActiveSongManager.getInstance();
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
        var clEditor = BR_ImproSupport.removeBR_ImproSupportInstances(song);
        if (clEditor != null)
        {
            mapEditorImproSupport.remove(clEditor);
        }
    }

    private void activeSongChanged(Song sg)
    {
        ImproSupport improSupport = null;

        if (sg != null)
        {
            CL_EditorTopComponent clTc = CL_EditorTopComponent.get(sg.getChordLeadSheet());
            var clEditor = clTc.getCL_Editor();
            improSupport = mapEditorImproSupport.get(clEditor);
            if (improSupport == null)
            {
                improSupport = new ImproSupport(clEditor);
                mapEditorImproSupport.put(clEditor, improSupport);
                sg.addPropertyChangeListener(this);
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */


    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
