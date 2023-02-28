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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.ui.colorsetmanager.api.NoteColorManager;
import org.jjazz.ui.utilities.api.HSLColor;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.ResUtil;

/**
 * A JComponent which represents a NoteEvent.
 * 
 */
public class NoteView extends JPanel implements PropertyChangeListener, Comparable<NoteEvent>
{

    public static final String PROP_SELECTED = "PropSelected";
    public static final String PROP_MODEL = "PropModel";
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Font FONT=GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(10f);;
    private static final int FONT_HEIGHT = (int)Utilities.getStringBounds("A", FONT).getHeight();
    private static String TOOLTIP_HELP = ResUtil.getString(NoteView.class, "NoteViewToolTipHelp");
        private static final NoteColorManager noteColorManager = NoteColorManager.getDefault();

    private NoteEvent noteEvent;
    private String noteAsString;
    private boolean selected;
    private boolean muted;


    private PianoRollEditorSettings settings;
    private static final Logger LOGGER = Logger.getLogger(NoteView.class.getSimpleName());


    public NoteView(NoteEvent ne)
    {
        noteEvent = ne;
        settings = PianoRollEditorSettings.getDefault();
        updateGraphics();

        settings.addPropertyChangeListener(this);

    }

    public void setModel(NoteEvent ne)
    {
        Preconditions.checkNotNull(ne);
        if (noteEvent == ne)
        {
            return;
        }
        var old = noteEvent;
        noteEvent = ne;
        updateGraphics();
        repaint();
        firePropertyChange(PROP_MODEL, old, noteEvent);
    }

    public NoteEvent getModel()
    {
        return noteEvent;
    }

    /**
     * Select this NoteView.
     * <p>
     * Fire a PROP_SELECTED change event.
     *
     * @param b
     */
    public void setSelected(boolean b)
    {
        if (selected != b)
        {
            selected = b;
            updateGraphics();
            repaint();
            firePropertyChange(PROP_SELECTED, !b, b);
        }
    }

    public boolean isSelected()
    {
        return selected;
    }


    public boolean isMuted()
    {
        return muted;
    }

    public void setMuted(boolean muted)
    {
        this.muted = muted;
        updateGraphics();
        repaint();
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);        // Paint background
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        var r = Utilities.getUsableArea(this);

        if (r.height >= FONT_HEIGHT)
        {
            // Draw note            
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float xStr = r.x + 1;
            float yStr = r.y + r.height - 2;           // text baseline position
            Color c = selected ? Color.BLACK : COLOR_TEXT;
            g2.setColor(c);
            g2.setFont(FONT);
            g2.drawString(noteAsString, xStr, yStr);
        }

        g2.dispose();
    }

    /**
     * Convenience method to get the LineBorder used by this NoteView.
     *
     * @return
     */
    public LineBorder getLineBorder()
    {
        return (LineBorder) getBorder();
    }

    public void cleanup()
    {
        settings.removePropertyChangeListener(this);
    }

    @Override
    public String toString()
    {
        return "NoteView[" + noteEvent + "]";
    }

    /**
     * Get a list of notes from a collection of NoteViews.
     *
     * @param noteViews
     * @return A mutable list
     */
    static public List<NoteEvent> getNotes(Collection<NoteView> noteViews)
    {
        Preconditions.checkNotNull(noteViews);
        List<NoteEvent> res = new ArrayList<>();
        noteViews.forEach(nv -> res.add(nv.getModel()));
        return res;
    }
 
    

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() instanceof PianoRollEditorSettings)
        {
            updateGraphics();
            repaint();
        }
    }

    // ==============================================================================================================
    // Comparable<NoteEvent> interface
    // ==============================================================================================================
    /**
     * Rely on NoteEvent.compareTo() but returns 0 only if noteEvent==ne.
     *
     * @param ne
     * @return
     */
    @Override
    public int compareTo(NoteEvent ne)
    {
        int res = noteEvent.compareTo(ne);
        if (res == 0)
        {
            int h = System.identityHashCode(noteEvent);
            int hNe = System.identityHashCode(ne);
            res = Integer.compare(h, hNe);
        }
        // LOGGER.severe("compareTo() -- ne=" + ne + " this=" + this + " res=" + res);
        return res;
    }

    // ==============================================================================================================
    // Private methods
    // ==============================================================================================================

    private void updateGraphics()
    {
        Color bgColor = selected ? noteColorManager.getSelectedNoteColor(noteEvent.getVelocity()) : noteColorManager.getNoteColor(noteEvent.getVelocity());
        setBackground(bgColor);
        setBorder(BorderFactory.createLineBorder(getBorderColor(bgColor, selected), 1));
        noteAsString = new Note(noteEvent.getPitch()).toPianoOctaveString();
        String tt = noteAsString + " (" + noteEvent.getPitch() + ") v=" + noteEvent.getVelocity() + ". " + TOOLTIP_HELP;
        setToolTipText(tt);
    }

    /**
     * Compute the border color.
     *
     * @param bgColor
     * @param selected
     * @return
     */
    private Color getBorderColor(Color bgColor, boolean selected)
    {
        return !selected ? HSLColor.changeLuminance(bgColor, -12) : Color.BLACK;
    }


}
