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
package org.jjazz.ui.mixconsole;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.backgroundsongmusicbuilder.api.ActiveSongMusicBuilder;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContextCopy;
import org.jjazz.ui.flatcomponents.api.FlatButton;
import org.jjazz.util.api.ResUtil;

/**
 * A panel to represent the phrase corresponding to a RhythmVoice.
 * <p>
 * Get the phrase from the ActiveSongMusicBuilder. Add edit/close buttons for UserRhythmVoice.
 */
public class PhraseViewerPanel extends PhraseBirdsEyeViewComponent implements ChangeListener, PropertyChangeListener
{

    private static final Icon ICON_EDIT = new ImageIcon(PhraseViewerPanel.class.getResource("resources/Edit-14x14.png"));
    private static final Icon ICON_CLOSE = new ImageIcon(PhraseViewerPanel.class.getResource("resources/Close14x14.png"));
    private static final Color BORDER_COLOR = new Color(32, 36, 53);
    private RhythmVoice rhythmVoice;
    private final Song song;
    private FlatButton fbtn_edit, fbtn_close;
    private final MixChannelPanelController controller;
    private final MidiMix midiMix;


    public PhraseViewerPanel(Song song, MidiMix mMix, MixChannelPanelController controller, RhythmVoice rv)
    {
        this.rhythmVoice = rv;
        this.song = song;
        this.midiMix = mMix;
        this.controller = controller;

        midiMix.addPropertyChangeListener(this);

        var asmb = ActiveSongMusicBuilder.getInstance();
        asmb.addChangeListener(this);


        setPreferredSize(new Dimension(50, 50));        // width will be ignored by MixConsole layout manager        
        setMinimumSize(new Dimension(50, 8));           // width will be ignored by MixConsole layout manager        
        setOpaque(false);
        setShowVelocityMode(2);
        setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        setLayout(new MyLayoutManager());


        // Refresh content if ActiveSongMusicBuilder has already a result for us (happens when user switches between songs)
        var result = asmb.getLastResult();
        if (result != null && result.songContext() instanceof SongContextCopy scc && scc.getOriginalSong() == song)
        {
            musicGenerationResultReceived(result);
        }

        if (this.rhythmVoice instanceof UserRhythmVoice)
        {
            addUserButtons();
        }
    }


    public void cleanup()
    {
        ActiveSongMusicBuilder.getInstance().removeChangeListener(this);
        midiMix.removePropertyChangeListener(this);
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    //=============================================================================
    // ChangeListener interface
    //=============================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        musicGenerationResultReceived(ActiveSongMusicBuilder.getInstance().getLastResult());
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == midiMix)
        {
            if (e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
            {
                if (rhythmVoice == e.getOldValue())
                {
                    rhythmVoice = (RhythmVoice) e.getNewValue();
                }
            } else if (e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL))
            {
                // Nothing
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------

    private void musicGenerationResultReceived(MusicGenerationQueue.Result result)
    {
        if (result.userException() != null)
        {
            return;
        }

        Phrase p = result.mapRvPhrases().get(rhythmVoice);
        if (p != null)
        {
            setModel(p, null, song.getSongStructure().toBeatRange(null));
        }
    }

    private void addUserButtons()
    {
        fbtn_edit = new FlatButton();
        fbtn_edit.setIcon(ICON_EDIT);
        fbtn_edit.addActionListener(ae -> editButtonPressed());
        fbtn_edit.setToolTipText(ResUtil.getString(getClass(), "PhraseViewerPanel.BtnEditTooltip"));

        fbtn_close = new FlatButton();
        fbtn_close.setIcon(ICON_CLOSE);
        fbtn_close.addActionListener(ae -> closeButtonPressed());
        fbtn_close.setToolTipText(ResUtil.getString(getClass(), "PhraseViewerPanel.BtnCloseTooltip"));

        add(fbtn_edit);
        add(fbtn_close);


        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent me)
            {
                if (SwingUtilities.isLeftMouseButton(me) && me.getClickCount() == 2)
                {
                    editButtonPressed();
                }
            }
        });
    }

    private void editButtonPressed()
    {
        controller.editUserPhrase((UserRhythmVoice) rhythmVoice);
    }

    private void closeButtonPressed()
    {
        controller.removeUserPhrase((UserRhythmVoice) rhythmVoice);
    }

    // ----------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------
    /**
     * Our LayoutManager to arrange the buttons.
     */
    private class MyLayoutManager implements LayoutManager
    {

        private static final int PADDING = 2;

        @Override
        public void layoutContainer(Container container)
        {
            if (fbtn_edit == null)
            {
                return;
            }

            Insets in = container.getInsets();
            int y = in.top + PADDING;

            fbtn_edit.setSize(fbtn_edit.getPreferredSize());
            fbtn_close.setSize(fbtn_close.getPreferredSize());

            fbtn_edit.setLocation(in.left + PADDING, y);
            fbtn_close.setLocation(container.getWidth() - in.right - PADDING - fbtn_close.getWidth(), y);
        }

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

    }

}
