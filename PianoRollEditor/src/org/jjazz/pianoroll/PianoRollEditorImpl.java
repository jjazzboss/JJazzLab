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
package org.jjazz.pianoroll;

import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.ZoomValue;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.KeyboardRange;
import org.jjazz.ui.utilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

/**
 * Implementation of a PianoRollEditor
 */
public class PianoRollEditorImpl extends PianoRollEditor
{
    
    private static final float MAX_WIDTH_FACTOR = 1.5f;
    
    private ToolbarPanel toolbarPanel;
    private JSplitPane splitPane;
    private VelocityPanel velocityPanel;
    private NotesPanel notesPanel;
    private KeyboardComponent keyboard;
    private RulerPanel rulerPanel;
    private JScrollPane scrollpane;
    private MouseDragLayerUI mouseDragLayerUI;
    private JLayer mouseDragLayer;
    
    private ZoomValue zoomValue;
    private final SizedPhrase spModel;
    private final DrumKit.KeyMap keymap;
    private final PianoRollEditorSettings settings;

    /**
     * Our global lookup.
     */
    private final Lookup lookup;

    /**
     * The lookup for non-selection stuff.
     */
    private final Lookup generalLookup;
    /**
     * Store non-selection stuff.
     */
    /**
     * Our UndoManager.
     */
    private JJazzUndoManager undoManager;
    private final InstanceContent generalLookupContent;
    private final int startBarIndex;
    
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorImpl.class.getSimpleName());

    /**
     * Creates new form PianoRollEditorImpl
     */
    public PianoRollEditorImpl(int startBarIndex, SizedPhrase sp, DrumKit.KeyMap kmap, PianoRollEditorSettings settings)
    {
        Preconditions.checkNotNull(sp);
        Preconditions.checkNotNull(settings);
        this.startBarIndex = startBarIndex;
        this.spModel = sp;
        this.keymap = kmap;
        this.settings = settings;
        
        
        createUI();


        // The lookup for other stuff
        generalLookupContent = new InstanceContent();
        generalLookupContent.add(new PianoRollZoomable());
        generalLookup = new AbstractLookup(generalLookupContent);

        // Our implementation is made "Zoomable" by controllers
        // Initialize with actionmap, our Zoomable object   
        // zoomable = new SS_EditorZoomable();
        // generalLookupContent.add(zoomable);
        generalLookupContent.add(getActionMap());


        // Global lookup = sum of both
        lookup = new ProxyLookup(notesPanel.getSelectionLookup(), generalLookup);


        // Normal zoom
        zoomValue = new ZoomValue(20, 20);
        notesPanel.setScaleFactorX(toScaleFactorX(zoomValue.hValue()));
        float yFactor = toScaleFactorY(zoomValue.vValue());
        keyboard.setScaleFactor(yFactor, Math.min(MAX_WIDTH_FACTOR, yFactor));
        
    }
    
    
    @Override
    public Lookup getLookup()
    {
        return lookup;
    }
    
    @Override
    public SizedPhrase getModel()
    {
        return spModel;
    }
    
    @Override
    public int getStartBarIndex()
    {
        return startBarIndex;
    }
    
    @Override
    public DrumKit.KeyMap getDrumKeyMap()
    {
        return keymap;
    }
    
    @Override
    public UndoRedo getUndoManager()
    {
        return undoManager;
    }
    
    @Override
    public PianoRollEditorSettings getSettings()
    {
        return settings;
    }
    
    @Override
    public void cleanup()
    {
        rulerPanel.cleanup();
        notesPanel.cleanup();
    }
    
    @Override
    public void setZoom(ZoomValue zoom)
    {
        Preconditions.checkNotNull(zoom);
        LOGGER.log(Level.FINE, "setZoom() -- zoom={0}", zoom);
        
        if (zoomValue == null || zoomValue.hValue() != zoom.hValue())
        {
            notesPanel.setScaleFactorX(toScaleFactorX(zoom.hValue()));
        }
        
        if (zoomValue == null || zoomValue.vValue() != zoom.vValue())
        {
            // Save pitch at center
            int saveCenterPitch = (int) getVisiblePitchRange().getCenter();


            // Scale the keyboard
            float factor = toScaleFactorY(zoom.vValue());
            // Because keyboard is in RIGHT orientation factorX impacts the keyboard height.
            // We limit factorY because we don't want the keyboard to get wide
            keyboard.setScaleFactor(factor, Math.min(MAX_WIDTH_FACTOR, factor));


            // restore pitch at center
            scrollToCenter(saveCenterPitch);
        }
        
        zoomValue = zoom;
    }
    
    @Override
    public ZoomValue getZoom()
    {
        return zoomValue;
    }
    
    @Override
    public void setQuantization(Quantization q)
    {
        notesPanel.getXMapper().setQuantization(q);
    }
    
    @Override
    public Quantization getQuantization()
    {
        return notesPanel.getXMapper().getQuantization();
    }
    
    @Override
    public void setSnapEnabled(boolean b)
    {
        // 
    }
    
    @Override
    public boolean isSnapEnabled()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    @Override
    public void setSelectedNote(NoteEvent ne, boolean b)
    {
        notesPanel.setSelectedNote(ne, b);
    }
    
    @Override
    public boolean isSelectedNote(NoteEvent ne)
    {
        return notesPanel.isSelectedNote(ne);
    }
    
    @Override
    public void setFocusedNote(NoteEvent ne)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    @Override
    public NoteEvent getFocusedNote()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    @Override
    public void setActiveTool(EditTool tool)
    {
        notesPanel.setActiveTool(tool);
    }
    
    @Override
    public EditTool getActiveTool()
    {
        return notesPanel.getActiveTool();
    }
    
    
    @Override
    public void showPlaybackPoint(boolean b, float pos)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    @Override
    public float getPositionFromPoint(Point editorPoint)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    @Override
    public int getPitchFromPoint(Point notesPanelPoint)
    {
        return notesPanel.getYMapper().getPitch(notesPanelPoint.y);
    }
    
    @Override
    public void showSelectionRectangle(Rectangle r)
    {
        mouseDragLayerUI.showSelectionRectangle(r);
        mouseDragLayer.repaint();        
    }
    
    @Override
    public List<NoteEvent> getNotes(Rectangle r)
    {
        return notesPanel.getNotes(r);
    }
    
    public KeyboardComponent getKeyboardComponent()
    {
        return keyboard;
    }
    
    public NotesPanel getNotesPanel()
    {
        return notesPanel;
    }
    
    public FloatRange getBeatRange()
    {
        return getModel().getBeatRange();
    }
    
    
    public IntRange getBarRange()
    {
        int nbBars = (int) (getBeatRange().size() / getModel().getTimeSignature().getNbNaturalBeats());
        return new IntRange(getStartBarIndex(), getStartBarIndex() + nbBars - 1);
    }

    /**
     * Scroll so that specified pitch is shown in the center of the editor, if possible.
     *
     * @param pitch
     */
    public void scrollToCenter(int pitch)
    {
        Preconditions.checkArgument(pitch >= 0 && pitch < 128);
        
        var vpRect = scrollpane.getViewport().getViewRect();
        float vpCenterY = vpRect.y + vpRect.height / 2f;
        IntRange pitchYRange = notesPanel.getYMapper().getKeyboardYRange(pitch);
        float pitchCenterY = (int) pitchYRange.getCenter();
        int dy = Math.round(vpCenterY - pitchCenterY);
        var r = new Rectangle(vpRect.x, dy > 0 ? vpRect.y - dy : vpRect.y + vpRect.height - 1 - dy, 1, 1);
        notesPanel.scrollRectToVisible(r);

        // LOGGER.severe("scrollToCenter() pitch=" + pitch + " vpRect=" + vpRect + " vpCenterY=" + vpCenterY + " pitchCenterY=" + pitchCenterY + " r=" + r);

    }

    /**
     * Scroll so that specified position is shown in the center of the editor, if possible.
     *
     * @param posInBeats
     */
    public void scrollToCenter(float posInBeats)
    {
        Preconditions.checkArgument(getBeatRange().contains(posInBeats, true));
    }


    /**
     * Get the min/max notes which are currently visible.
     *
     * @return
     */
    public IntRange getVisiblePitchRange()
    {
        IntRange vpYRange = getYRange(scrollpane.getViewport().getViewRect());
        IntRange keysYRange = getYRange(keyboard.getKeysBounds());
        IntRange ir = keysYRange.getIntersection(vpYRange);
        var pitchBottom = notesPanel.getYMapper().getPitch(ir.to);
        var pitchTop = notesPanel.getYMapper().getPitch(ir.from);
        return new IntRange(pitchBottom, pitchTop);
    }

    /**
     * Get the min/max beat positions which are visible.
     *
     * @return
     */
    public FloatRange getVisibleBeatRange()
    {
        var vRect = scrollpane.getViewport().getViewRect();
        var posLeft = notesPanel.getXMapper().getPositionInBeats(vRect.x);
        var posRight = notesPanel.getXMapper().getPositionInBeats(vRect.x + vRect.width - 1);
        return new FloatRange(posLeft, posRight);
    }

    /**
     * Get the min/max bar indexes which are visible.
     *
     * @return
     */
    public IntRange getVisibleBarRange()
    {
        float nbBeatsPerBar = getModel().getTimeSignature().getNbNaturalBeats();
        var beatRange = getVisibleBeatRange();
        int relBarFrom = (int) ((beatRange.from - spModel.getBeatRange().from) / nbBeatsPerBar);
        int relBarTo = (int) ((beatRange.to - spModel.getBeatRange().from) / nbBeatsPerBar);
        var res = new IntRange(startBarIndex + relBarFrom, startBarIndex + relBarTo);
        return res;
    }


    // =======================================================================================================================
    // Private methods
    // =======================================================================================================================
    private boolean isDrums()
    {
        return keymap != null;
    }
    
    private float toScaleFactorX(int zoomHValue)
    {
        float xFactor = 0.2f + 4 * zoomHValue / 100f;
        return xFactor;
    }
    
    private float toScaleFactorY(int zoomVValue)
    {
        float yFactor = 0.4f + 4 * zoomVValue / 100f;
        return yFactor;
    }
    
    private void createUI()
    {
        // The keyboard 
        // We need an enclosing panel for keyboard, so that keyboard size changes when its scaleFactor changes (zoom in/out). If we put the keyboard directly
        // in the JScrollpane, keyboard size might not change when JScrollpane is much bigger than the keys bounds.
        JPanel pnl_keyboard = new JPanel();
        pnl_keyboard.setLayout(new BorderLayout());
        keyboard = new KeyboardComponent(KeyboardRange._128_KEYS, KeyboardComponent.Orientation.RIGHT, false);
        keyboard.getWhiteKeys().stream()
                .filter(k -> k.getPitch() % 12 == 0)
                .forEach(k -> k.setText("C" + (k.getPitch() / 12 - 1)));
        pnl_keyboard.add(keyboard, BorderLayout.PAGE_START);


        // The scrollpane
        mouseDragLayerUI = new MouseDragLayerUI();
        notesPanel = new NotesPanel(this, keyboard);
        mouseDragLayer = new JLayer(notesPanel, mouseDragLayerUI);
        
        
        rulerPanel = new RulerPanel(this, notesPanel);
        scrollpane = new JScrollPane();
        scrollpane.setViewportView(mouseDragLayer);
        scrollpane.setRowHeaderView(pnl_keyboard);
        scrollpane.setColumnHeaderView(rulerPanel);


        // The splitpane
        velocityPanel = new VelocityPanel(this, notesPanel);
        splitPane = new javax.swing.JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0);
        splitPane.setLeftComponent(scrollpane);
        splitPane.setRightComponent(velocityPanel);


        // Final layout
        toolbarPanel = new ToolbarPanel(this);
        setLayout(new BorderLayout());
        add(toolbarPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        
        
    }
    
    private IntRange getYRange(Rectangle r)
    {
        assert r.height > 0;
        IntRange res = new IntRange(r.y, r.y + r.height - 1);
        return res;
    }
    
    private IntRange getXRange(Rectangle r)
    {
        assert r.width > 0;
        IntRange res = new IntRange(r.x, r.x + r.width - 1);
        return res;
    }


    // =======================================================================================================================
    // Inner classes
    // =======================================================================================================================
    /**
     * Implements the Zoomable functionalities.
     */
    private class PianoRollZoomable implements Zoomable
    {
        
        @Override
        public Zoomable.Capabilities getZoomCapabilities()
        {
            return Zoomable.Capabilities.X_Y;
        }
        
        @Override
        public int getZoomYFactor()
        {
            return getZoom().vValue();
        }
        
        @Override
        public void setZoomYFactor(int newFactor, boolean valueIsAdjusting)
        {
            if (valueIsAdjusting)
            {
                return;
            }
            int old = getZoomYFactor();
            setZoom(new ZoomValue(getZoomXFactor(), newFactor));
            firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, old, newFactor);
        }
        
        @Override
        public int getZoomXFactor()
        {
            return getZoom().hValue();
        }
        
        @Override
        public void setZoomXFactor(int newFactor, boolean valueIsAdjusting)
        {
            int old = getZoomXFactor();
            setZoom(new ZoomValue(newFactor, getZoomYFactor()));
            firePropertyChange(Zoomable.PROPERTY_ZOOM_X, old, newFactor);
            
        }
        
        @Override
        public void addPropertyListener(PropertyChangeListener l)
        {
            addPropertyChangeListener(Zoomable.PROPERTY_ZOOM_X, l);
            addPropertyChangeListener(Zoomable.PROPERTY_ZOOM_Y, l);
        }
        
        @Override
        public void removePropertyListener(PropertyChangeListener l)
        {
            removePropertyChangeListener(Zoomable.PROPERTY_ZOOM_X, l);
            removePropertyChangeListener(Zoomable.PROPERTY_ZOOM_Y, l);
        }
    };
    
}
