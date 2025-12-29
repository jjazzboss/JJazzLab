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
package org.jjazz.mixconsole;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLayer;
import javax.swing.JPanel;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uiutilities.api.CornerLayout;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;

/**
 * Add "clone as user track" button which appears only when hovering component.
 */
public class PhraseViewerPanelRhythm extends PhraseViewerPanel
{

    private static final ImageIcon ICON_CLONE_AS_USER_TRACK = new ImageIcon(PhraseViewerPanelRhythm.class.getResource("resources/CloneTrack-10x10.png"));
    private final JPanel supportPanel;        // Needed to use JLayer
    private final JLayer layer;
    private final FlatButton fbtn_clone;
    private boolean buttonShown;
    private static final Logger LOGGER = Logger.getLogger(PhraseViewerPanelRhythm.class.getSimpleName());

    public PhraseViewerPanelRhythm(Song song, MidiMix mMix, MixChannelPanelController controller, RhythmVoice rv)
    {
        super(song, mMix, controller, rv);

        // Prepare the button
        fbtn_clone = new FlatButton();
        fbtn_clone.setIcon(GeneralUISettings.adaptIconToLightThemeIfRequired(ICON_CLONE_AS_USER_TRACK, -30));
        fbtn_clone.addActionListener(ae -> getController().cloneRhythmTrackAsUserTrack(getRhythmVoice()));
        fbtn_clone.setToolTipText(ResUtil.getString(getClass(), "PhraseViewerPanelRhythm.CloneAsUserTrackTooltip"));


        // Prepare for the JLayer to show the button when hovering over the component
        // We can't directly associate a JLayer to "this" JPanel object, so we add a support panel on which JLayer will be used
        supportPanel = new JPanel();
        supportPanel.setLayout(new CornerLayout(BUTTONS_PADDING, CornerLayout.NORTH_WEST));  // Reuse layout of parent class
        supportPanel.setOpaque(false);        // So we can see the PhraseViewerPanel background
        var enterExitConsumer = new Consumer<Boolean>()
        {
            @Override
            public void accept(Boolean b)
            {
//                LOGGER.log(Level.SEVERE, "this={0} accept() b={1} buttonShown={2}", new Object[]
//                {
//                    PhraseViewerPanelRhythm.this, b, buttonShown
//                });
                if (b && !buttonShown)
                {
                    buttonShown = true;
                    supportPanel.add(fbtn_clone, CornerLayout.NORTH_EAST);
                    supportPanel.revalidate();
                    supportPanel.repaint();

                } else if (!b && buttonShown)
                {
                    buttonShown = false;
                    supportPanel.remove(fbtn_clone);
                    supportPanel.revalidate();
                    supportPanel.repaint();
                }
            }
        };
        layer = UIUtilities.createEnterExitComponentLayer(supportPanel, enterExitConsumer);


        // Now add the JLayer so that it takes all the place
        setLayout(new BorderLayout());
        add(layer, BorderLayout.CENTER);


        // Because of the JLayer we need to redo stuff done in PhraseViewerPanel here
        supportPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                handleMouseDrag(evt);
            }
        });
        supportPanel.setToolTipText(getToolTipText());

    }


// ----------------------------------------------------------------------------
// Private methods
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Inner classes
// ----------------------------------------------------------------------------
}
