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
package org.jjazz.spteditor;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RP_Integer;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.rhythm.api.RP_State;
import org.jjazz.rhythm.api.RP_StringSet;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.spi.DefaultRpEditorComponentFactory;
import org.jjazz.spteditor.spi.RpEditorComponent;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;

public class DefaultRpEditorComponentFactoryImpl implements DefaultRpEditorComponentFactory
{

    private static DefaultRpEditorComponentFactoryImpl INSTANCE;

    public static DefaultRpEditorComponentFactoryImpl getInstance()
    {
        synchronized (DefaultRpEditorComponentFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultRpEditorComponentFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private DefaultRpEditorComponentFactoryImpl()
    {
    }

    @Override
    public RpEditorComponent createComponent(Song song, SongPart spt, RhythmParameter<?> rp)
    {
        Type type;
        if (RpCustomEditorFactory.findFactory(rp) != null)
        {
            type = Type.CUSTOM_DIALOG;
        } else if (rp instanceof RP_Integer)
        {
            type = Type.SPINNER;
        } else if (rp instanceof RP_State)
        {
            type = Type.COMBO;
        } else if (rp instanceof RP_StringSet)
        {
            type = Type.LIST;
        } else
        {
            type = Type.STUB;
        }
        RpEditorComponent c = createComponent(song, type, spt, rp);
        return c;
    }

    @Override
    public RpEditorComponent createComponent(Song song, Type type, SongPart spt, RhythmParameter<?> rp)
    {
        if (type == null || spt == null || rp == null)
        {
            throw new NullPointerException("type=" + type + " spt=" + spt + " rp=" + rp);   
        }
        RpEditorComponent c = null;
        switch (type)
        {
            case LIST:
                if (rp instanceof RP_SYS_Mute)
                {
                    c = new RpEditorList(spt, (RP_SYS_Mute) rp, new RpMuteCellRenderer(spt));
                } else if (rp instanceof RP_StringSet)
                {
                    c = new RpEditorList(spt, (RP_StringSet) rp, null);
                }
                break;
            case SPINNER:
                if (rp instanceof RP_Integer)
                {
                    c = new RpEditorSpinner(spt, (RP_Integer) rp);
                }
                break;
            case COMBO:
                if (rp instanceof RP_State)
                {
                    c = new RpEditorCombo(spt, (RP_State) rp);
                }
                break;
            case CUSTOM_DIALOG:
                if (RpCustomEditorFactory.findFactory(rp) != null)
                {
                    c = new RpEditorCustom(song, spt, rp);
                }
                break;
            case STUB:
                c = new RpEditorStub(spt, rp);
                break;
            default:
                throw new AssertionError(type.name());

        }

        if (c == null)
        {
            throw new IllegalArgumentException("rp=" + rp + " has an unsupported class for RpEditorComponent type=" + type);
        }

        return c;
    }

    /**
     * A cell renderer to provide more information for RP_SYS_Mute
     */
    private class RpMuteCellRenderer extends DefaultListCellRenderer
    {

        HashMap<String, RhythmVoice> mapNameRv = new HashMap<>();

        RpMuteCellRenderer(SongPart spt)
        {
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
