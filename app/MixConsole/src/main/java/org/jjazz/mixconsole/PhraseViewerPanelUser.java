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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uiutilities.api.CornerLayout;
import org.jjazz.utilities.api.ResUtil;

/**
 * Add edit/close buttons for UserRhythmVoice, and slightly change border color.
 */
public class PhraseViewerPanelUser extends PhraseViewerPanel
{

    private static final ImageIcon ICON_EDIT = new ImageIcon(PhraseViewerPanelUser.class.getResource("resources/Edit-14x14.png"));
    private static final ImageIcon ICON_CLOSE = new ImageIcon(PhraseViewerPanelUser.class.getResource("resources/Close14x14.png"));
    private final FlatButton fbtn_edit;
    private final FlatButton fbtn_close;


    public PhraseViewerPanelUser(Song song, MidiMix mMix, MixChannelPanelController controller, RhythmVoice rv)
    {
        super(song, mMix, controller, rv);


        // Add buttons
        fbtn_edit = new FlatButton();
        fbtn_edit.setIcon(GeneralUISettings.adaptIconToLightThemeIfRequired(ICON_EDIT, -20));
        fbtn_edit.addActionListener(ae -> editButtonPressed());
        fbtn_edit.setToolTipText(ResUtil.getString(getClass(), "PhraseViewerPanel.BtnEditTooltip"));

        fbtn_close = new FlatButton();
        fbtn_close.setIcon(GeneralUISettings.adaptIconToLightThemeIfRequired(ICON_CLOSE, -20));
        fbtn_close.addActionListener(ae -> closeButtonPressed());
        fbtn_close.setToolTipText(ResUtil.getString(getClass(), "PhraseViewerPanel.BtnCloseTooltip"));

        add(fbtn_edit, CornerLayout.NORTH_WEST);
        add(fbtn_close, CornerLayout.NORTH_EAST);


        // Double-click in the panel = edit
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

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------
    private void editButtonPressed()
    {
        getController().editUserPhrase((UserRhythmVoice) getRhythmVoice());
    }

    private void closeButtonPressed()
    {
        getController().removeUserPhrase((UserRhythmVoice) getRhythmVoice());
    }

}
