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
package org.jjazz.ui.spteditor;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RP_Integer;
import org.jjazz.rhythm.parameters.RP_SYS_Mute;
import org.jjazz.rhythm.parameters.RP_State;
import org.jjazz.rhythm.parameters.RP_StringSet;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rpcustomeditor.spi.RpCustomEditorProvider;
import org.jjazz.song.api.Song;
import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.spteditor.spi.DefaultRpEditorFactory;

public class DefaultRpEditorFactoryImpl implements DefaultRpEditorFactory
{

    private static DefaultRpEditorFactoryImpl INSTANCE;

    public static DefaultRpEditorFactoryImpl getInstance()
    {
        synchronized (DefaultRpEditorFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultRpEditorFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private DefaultRpEditorFactoryImpl()
    {
    }

    @Override
    public RpEditor createRpEditor(Song song, SongPart spt, RhythmParameter<?> rp)
    {
        Type type;
        if (rp instanceof RpCustomEditorProvider)
        {
            type = Type.CUSTOM_DIALOG;
        } else if (rp instanceof RP_Integer)
        {
            type = Type.SPINNER;
        } else if (rp instanceof RP_State)
        {
            type = Type.COMBO;
        } else if (rp instanceof RP_SYS_Mute || rp instanceof RP_StringSet)
        {
            type = Type.LIST;
        } else
        {
            type = Type.STUB;
        }
        RpEditor rpe = createRpEditor(type, song, spt, rp);
        return rpe;
    }

    @Override
    public RpEditor createRpEditor(Type type, Song song, SongPart spt, RhythmParameter<?> rp)
    {
        if (type == null || song == null || spt == null || rp == null)
        {
            throw new NullPointerException("type=" + type + " song=" + song + " spt=" + spt + " rp=" + rp);   //NOI18N
        }
        RpEditor rpe;
        switch (type)
        {
            case LIST:
                if (rp instanceof RP_SYS_Mute)
                {
                    rpe = new RpEditorList(spt, rp, new RpMuteCellRenderer(song, spt, (RP_SYS_Mute) rp));
                } else
                {
                    rpe = new RpEditorList(spt, rp, null);
                }
                break;
            case SPINNER:
                rpe = new RpEditorSpinner(spt, rp);
                break;
            case COMBO:
                rpe = new RpEditorCombo(spt, rp);
                break;
            case STUB:
                rpe = new RpEditorStub(spt, rp);
                break;
            case CUSTOM_DIALOG:
                rpe = new RpEditorCustomDialog(spt, rp);
                break;
            default:
                throw new AssertionError(type.name());

        }
        return rpe;
    }

    /**
     * A cell renderer to provide more information for RP_SYS_Mute
     */
    private class RpMuteCellRenderer extends DefaultListCellRenderer
    {

        Song song;
        SongPart spt;
        RP_SYS_Mute rp;
        HashMap<String, RhythmVoice> mapNameRv = new HashMap<>();

        RpMuteCellRenderer(Song song, SongPart spt, RP_SYS_Mute rp)
        {
            this.song = song;
            this.spt = spt;
            this.rp = rp;

            spt.getRhythm().getRhythmVoices().forEach(rv -> mapNameRv.put(rv.getName(), rv));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String muteValue = (String) value;      // Normally it's the RhythmVoice name            
            RhythmVoice rv = mapNameRv.get(muteValue);
            if (rv == null)
            {
                return label;
            }

            // Adjust text
            String strFamily = rv.isDrums() ? "" : " (" + rv.getPreferredInstrument().getSubstitute().getFamily().getShortName() + ")";
            label.setText(muteValue + strFamily);
            label.setToolTipText(label.getText());

            return label;
        }
    }
}
