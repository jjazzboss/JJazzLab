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
package org.jjazz.ui.musiccontrolactions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import org.jjazz.musiccontrol.api.MusicController;
import static org.jjazz.musiccontrol.api.MusicController.State.DISABLED;
import static org.jjazz.musiccontrol.api.MusicController.State.PAUSED;
import static org.jjazz.musiccontrol.api.MusicController.State.PLAYING;
import static org.jjazz.musiccontrol.api.MusicController.State.STOPPED;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.netbeans.api.annotations.common.StaticResource;


/**
 * The panel used to show the auto-preview button.
 */
public class AutoPreviewToolbarPanel extends javax.swing.JPanel
{

    private final AutoUpdateLabelModel autoUpdateLabelModel;
    private static final Logger LOGGER = Logger.getLogger(AutoPreviewToolbarPanel.class.getSimpleName());

    public AutoPreviewToolbarPanel()
    {
        initComponents();

        autoUpdateLabelModel = new AutoUpdateLabelModel(lbl_autoPreview);
    }

    // ======================================================================
    // Private methods
    // ======================================================================   

    // ======================================================================
    // Inner classes
    // ======================================================================   
    private static class AutoUpdateLabelModel implements PropertyChangeListener
    {

        private static final Icon ICON_OFF = new ImageIcon(AutoPreviewToolbarPanel.class.getResource("resources/AutoUpdate-OFF-24x24.png"));
        private static final Icon ICON_ON = new ImageIcon(AutoPreviewToolbarPanel.class.getResource("resources/AutoUpdate-ON-24x24.png"));
        //  private static final Icon ICON_DISABLED = new ImageIcon(AutoPreviewToolbarPanel.class.getResource("resources/AutoUpdate-disabled-24x24.png"));

        private UpdatableSongSession currentUpdatableSession;
        private final JLabel label;

        private AutoUpdateLabelModel(JLabel label)
        {
            this.label = label;
            this.label.setText(null);
            this.label.setIcon(ICON_OFF);
            MusicController.getInstance().addPropertyChangeListener(this);
        }

        // ======================================================================
        // PropertyChangeListener interface
        // ======================================================================    
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            var mc = MusicController.getInstance();

            if (evt.getSource() == currentUpdatableSession)
            {
                if (evt.getPropertyName().equals(UpdatableSongSession.PROP_ENABLED))
                {
                    if (!currentUpdatableSession.isEnabled())
                    {
                        setEnabledIcon(false);
                    }
                }
            } else if (evt.getSource() == MusicController.getInstance())
            {
                if (evt.getPropertyName().equals(MusicController.PROP_PLAYBACK_SESSION))
                {
                    // Unregister previous session
                    if (currentUpdatableSession != null)
                    {
                        currentUpdatableSession.removePropertyChangeListener(this);
                    }

                    currentUpdatableSession = null;       // By default


                    PlaybackSession newSession = mc.getPlaybackSession();
                    if (newSession instanceof UpdatableSongSession uss)
                    {
                        currentUpdatableSession = uss;
                        currentUpdatableSession.addPropertyChangeListener(this);
                        setEnabledIcon(currentUpdatableSession.isEnabled());
                    }
                }

                if (currentUpdatableSession == null)
                {
                    // Unknow session, autoupdate has no meaning                 
                    setEnabledIcon(false);
                }

            } else if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                MusicController.State newState = (MusicController.State) evt.getNewValue();
                MusicController.State oldState = (MusicController.State) evt.getOldValue();

                switch (newState)
                {
                    case DISABLED:
                        break;
                    case PAUSED:
                    case STOPPED:
                        if (currentUpdatableSession != null)
                        {
                            setEnabledIcon(true);
                        }
                        break;
                    case PLAYING:
                        break;
                    default:
                        throw new AssertionError(newState.name());

                }
            }
        }

        private void setEnabledIcon(boolean b)
        {
            label.setIcon(b ? ICON_ON : ICON_OFF);
        }

    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        lbl_autoPreview = new javax.swing.JLabel();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));

        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        add(filler4);

        lbl_autoPreview.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-OFF-24x24.png"))); // NOI18N
        lbl_autoPreview.setToolTipText(org.openide.util.NbBundle.getMessage(AutoPreviewToolbarPanel.class, "AutoPreviewToolbarPanel.lbl_autoPreview.toolTipText")); // NOI18N
        add(lbl_autoPreview);
        add(filler9);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler9;
    private javax.swing.JLabel lbl_autoPreview;
    // End of variables declaration//GEN-END:variables

}
