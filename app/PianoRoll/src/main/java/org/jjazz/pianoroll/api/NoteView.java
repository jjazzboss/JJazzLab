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
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.uisettings.api.NoteColorManager;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.utilities.api.ResUtil;

/**
 * A JComponent which represents a NoteEvent.
 * <p>
 */
public class NoteView extends JPanel implements PropertyChangeListener, Comparable<NoteEvent>
{

    public static final String PROP_SELECTED = "PropSelected";
    public static final String PROP_MODEL = "PropModel";
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Font FONT = GeneralUISettings.getInstance().getStdCondensedFont().deriveFont(10f);
    private static final int FONT_HEIGHT = (int) UIUtilities.getStringBounds("A", FONT).getHeight();
    private static final NoteColorManager noteColorManager = NoteColorManager.getDefault();

    /**
     * We use our own variable to avoid using setBackground() (which calls repaint() which can slow down UI a lot when selecting hundreds of notes).
     */
    private Color noteColor;
    /**
     * We use our own variable to avoid using setBorder() (which calls repaint() which can slow down UI a lot when selecting hundreds of notes).
     */
    private LineBorder noteBorder;
    private NoteEvent noteEvent;
    private String noteAsString;
    private boolean selected;
    private boolean muted;
    private boolean showNoteString;
    private String extraTooltip = ResUtil.getString(NoteView.class, "NoteViewToolTipHelp");


    private final PianoRollEditorSettings settings;
    private static final Logger LOGGER = Logger.getLogger(NoteView.class.getSimpleName());


    public NoteView(NoteEvent ne)
    {
        noteEvent = ne;
        settings = PianoRollEditorSettings.getDefault();
        showNoteString = true;
        updateGraphics(false);

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
        updateGraphics(true);
        firePropertyChange(PROP_MODEL, old, noteEvent);
    }

    public NoteEvent getModel()
    {
        return noteEvent;
    }

    public Color getNoteColor()
    {
        return noteColor;
    }

    public Color getNoteBorderColor()
    {
        return noteBorder.getLineColor();
    }

    /**
     * Select this NoteView.
     * <p>
     * Fire a PROP_SELECTED change event.
     *
     * @param b
     * @param repaint If true call repaint() after. Use false to greatly improve performance when updating many NoteViews at once (call repaint() on the
     *                container at the end)
     */
    public void setSelected(boolean b, boolean repaint)
    {
        if (selected != b)
        {
            selected = b;
            updateGraphics(repaint);
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
        updateGraphics(true);
    }

    public boolean isShowNoteString()
    {
        return showNoteString;
    }

    /**
     * Decide if we show the note string in the viewer (eg "Eb").
     *
     * @param b
     */
    public void setShowNoteString(boolean b)
    {
        this.showNoteString = b;
        repaint();
    }

    public String getExtraTooltip()
    {
        return extraTooltip;
    }

    /**
     * The extra help tooltip text added to note name and velocity.
     *
     * @param text
     */
    public void setExtraTooltip(String text)
    {
        Objects.requireNonNull(text);
        this.extraTooltip = text;
        updateGraphics(false);
    }

    @Override
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        // Paint background and line border ourselves with our own variables (avoids useless calls to repaint() by setBackground() and setBorder())
        g2.setColor(noteColor);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // g2.setStroke(new BasicStroke(0.7f));                
        // g2.setColor(noteBorderColor);
        noteBorder.paintBorder(this, g, 0, 0, getWidth(), getHeight());
        // g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);


        var r = UIUtilities.getUsableArea(this);

        if (showNoteString && r.height >= FONT_HEIGHT)
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
        List<NoteEvent> res = new ArrayList<>(noteViews.size());
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
            updateGraphics(true);
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

    /**
     * Update graphics state.
     *
     * @param repaint If true calls repaint() after updating the graphics variables.
     */
    private void updateGraphics(boolean repaint)
    {
        noteColor = selected ? noteColorManager.getSelectedNoteColor(noteEvent.getVelocity()) : noteColorManager.getNoteColor(noteEvent.getVelocity());
        var borderColor = !selected ? HSLColor.changeLuminance(noteColor, -12) : Color.BLACK;
        noteBorder = (LineBorder) BorderFactory.createLineBorder(borderColor, 1);
        noteAsString = new Note(noteEvent.getPitch()).toPianoOctaveString();
        String tt = String.format("%s (%d) vel=%d pos=%.2f dur=%.2f",
                noteAsString, noteEvent.getPitch(), noteEvent.getVelocity(), noteEvent.getPositionInBeats(), noteEvent.getDurationInBeats());
        if (!extraTooltip.isBlank())
        {
            tt += ". " + extraTooltip;
        }
        setToolTipText(tt);

        if (repaint)
        {
            repaint();
        }
    }


}
