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
package org.jjazz.notesviewer.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.notesviewer.NotesViewerPanel;
import org.jjazz.song.api.Song;
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
        dtd = "-//org.jjazz.notesviewer//NotesViewer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "NotesViewerTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "jlnavigator", openAtStartup = false, position = 20)
@ActionID(category = "Window", id = "org.jjazz.notesviewer.api.NotesViewerTopComponent")
@ActionReference(path = "Menu/Window", position = 20)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_NotesViewerAction",
        preferredID = "NotesViewerTopComponent"
)
public final class NotesViewerTopComponent extends TopComponent implements PropertyChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(NotesViewerTopComponent.class.getSimpleName());
    private final NotesViewerPanel viewer;

    public NotesViewerTopComponent()
    {

        setToolTipText(ResUtil.getString(getClass(), "CTL_NotesViewerTopComponentDesc"));
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        // putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);


        initComponents();


        viewer = new NotesViewerPanel();
        add(viewer);


    }

    @Override
    public void componentOpened()
    {
        viewer.opened();

        var asm = ActiveSongManager.getInstance();        
        asm.addPropertyListener(this);
        Song song = asm.getActiveSong();        
        updateTabName(song);        
        viewer.setModel(song, asm.getActiveMidiMix());
    }

    @Override
    public void componentClosed()
    {
        viewer.closing();
        
        var asm = ActiveSongManager.getInstance();        
        asm.removePropertyListener(this);                
    }

    /**
     *
     * @return Can be null
     */
    static public NotesViewerTopComponent getInstance()
    {
        return (NotesViewerTopComponent) WindowManager.getDefault().findTopComponent("NotesViewerTopComponent");
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
                updateTabName((Song) evt.getNewValue());
                viewer.setModel((Song) evt.getNewValue(), (MidiMix) evt.getOldValue());
            }
        }
    }


    // ===========================================================================
    // Private methods
    // ===========================================================================
    /**
     *
     * @param song Can be null
     */
    private void updateTabName(Song song)
    {
        String tabNameBase = ResUtil.getString(getClass(), "CTL_NotesViewerTopComponent");
        setName(tabNameBase + (song == null ? "" : " - " + song.getName()));
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

        setMinimumSize(new java.awt.Dimension(50, 50));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
