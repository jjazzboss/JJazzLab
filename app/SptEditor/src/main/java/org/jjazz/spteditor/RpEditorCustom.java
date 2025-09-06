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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.api.RpEditor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.spteditor.spi.RpEditorComponent;
import org.openide.util.Exceptions;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;

/**
 * A RP value editor for RhythmParameters.
 * <p>
 */
public class RpEditorCustom extends RpEditorComponent
{

    JButton btn_edit;
    Object value;
    Song songModel;
    private static final Logger LOGGER = Logger.getLogger(RpEditorCustom.class.getSimpleName());

    public RpEditorCustom(Song song, SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);
        var factory = RpCustomEditorFactory.findFactory(rp);
        if (factory == null || song == null)
        {
            throw new IllegalArgumentException("song=" + song + " spt=" + spt + " rp=" + rp + " factory=" + factory);
        }

        songModel = song;

        btn_edit = new JButton(ResUtil.getString(getClass(), "CTL_Edit"));
        btn_edit.addActionListener(ae -> showCustomEditDialog());
        updateEditorValue(spt.getRPValue(rp));

        setLayout(new BorderLayout());
        add(btn_edit, BorderLayout.CENTER);
    }

    @Override
    public void showMultiValueMode(boolean b)
    {
        RpEditor.showMultiModeUsingFont(b, btn_edit);
    }

    @Override
    public Object getEditorValue()
    {
        return value;
    }

    @Override
    public void updateEditorValue(Object value)
    {
        this.value = value;
        String strValue = rp.getDisplayValue(value);
        strValue = Utilities.truncateWithDots(strValue, 20);
        if (strValue.isBlank())
        {
            strValue = "edit";
        }
        btn_edit.setText(strValue);
        String strDesc = rp.getValueDescription(value);
        strDesc = strDesc == null ? "" : strDesc + " - ";
        btn_edit.setToolTipText(strDesc + "Click to edit");
    }

    public Song getSongModel()
    {
        return songModel;
    }

    // ===========================================================================
    // Private methods
    // ===========================================================================
    private void showCustomEditDialog()
    {

        // Prepare our dialog
        MidiMix mm = null;
        try
        {
            mm = MidiMixManager.getDefault().findMix(songModel);
        } catch (MidiUnavailableException ex)
        {
            // Should never happen 
            Exceptions.printStackTrace(ex);
            return;
        }
        SongPartContext sptContext = new SongPartContext(songModel, mm, songPart);
        var dlgEditor = RpCustomEditorFactory.findFactory(rp).getEditor(rp);
        assert dlgEditor != null : "rp=" + rp;
        dlgEditor.preset(value, sptContext);


        // Set location
        Rectangle r = btn_edit.getBounds();
        Point p = r.getLocation();
        SwingUtilities.convertPointToScreen(p, btn_edit.getParent());
        int x = p.x + r.width + 10;
        int y = Math.max(2, p.y + r.height / 2 - dlgEditor.getHeight() / 2);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        x = Math.min(x, screen.width - dlgEditor.getWidth());
        y = Math.min(y, screen.height - dlgEditor.getHeight());
        Point pd = new Point(x, y);
        SwingUtilities.convertPointFromScreen(pd, dlgEditor.getParent());
        dlgEditor.setLocation(x, y);
        dlgEditor.setVisible(true);


        // Process result
        Object newValue = dlgEditor.getRpValue();
        if (dlgEditor.isExitOk() && !Objects.equals(value, newValue))
        {
            Object old = value;
            value = newValue;
            firePropertyChange(RpEditor.PROP_RP_VALUE, old, value);
        }
    }

}
