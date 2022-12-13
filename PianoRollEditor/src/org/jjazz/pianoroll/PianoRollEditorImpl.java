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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import javax.swing.JSplitPane;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.ZoomValue;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.utilities.api.Zoomable;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

/**
 * Implementation of a PianoRollEditor
 */
public class PianoRollEditorImpl extends PianoRollEditor implements PropertyChangeListener
{

    private ToolbarPanel toolbarPanel;
    private JSplitPane splitPane;
    private KeysAndNotesPanel keysAndNotesPanel;
    private VelocityPanel velocityPanel;

    private ZoomValue zoomValue;
    private final SizedPhrase spModel;
    private final DrumKit.KeyMap keymap;
    private final PianoRollEditorSettings settings;

    /**
     * Our global lookup.
     */
    private final Lookup lookup;
    /**
     * The lookup for the selection.
     */
    private final Lookup selectionLookup;
    /**
     * Instance content behind the selection lookup.
     */
    private final InstanceContent selectionLookupContent;
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


        // Listen to settings changes
        this.settings.addPropertyChangeListener(this);


        // The lookup for selection
        selectionLookupContent = new InstanceContent();
        selectionLookup = new AbstractLookup(selectionLookupContent);

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
        lookup = new ProxyLookup(selectionLookup, generalLookup);

        // Normal zoom
        zoomValue = new ZoomValue();


        createUI();
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
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setZoom(ZoomValue zoom)
    {
        if (!zoomValue.equals(zoom))
        {
            // X factor : between 0.2 and 4.2
            float xFactor = 0.2f + 4 * zoom.hFactor() / 100f;
            keysAndNotesPanel.getNotesPanel().setZoomX(xFactor);

            // Y factor : between 0.4 and 4.2
            float yFactor = 0.4f + 4 * zoom.vFactor() / 100f;
            keysAndNotesPanel.setZoomY(yFactor);
            zoomValue = zoom;
        }
    }

    @Override
    public ZoomValue getZoom()
    {
        return zoomValue;
    }

    @Override
    public void setQuantization(Quantization q)
    {
        keysAndNotesPanel.getNotesPanel().getXMapper().setQuantization(q);
    }

    @Override
    public Quantization getQuantization()
    {
        return keysAndNotesPanel.getNotesPanel().getXMapper().getQuantization();
    }

    @Override
    public void setSnapEnabled(boolean b)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isSnapEnabled()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void selectNote(NoteEvent ne, boolean b)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setFocusOnNote(NoteEvent ne)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setNoteVisible(NoteEvent ne)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setActiveTool(EditTool tool)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public EditTool getActiveTool()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public NoteEvent getFocusedNote()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
    public int getPitchFromPoint(Point editorPoint)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    public ToolbarPanel getToolbarPanel()
    {
        return toolbarPanel;
    }

    public KeysAndNotesPanel getKeysAndNotesPanel()
    {
        return keysAndNotesPanel;
    }

    public NotesPanel getNotesPanel()
    {
        return keysAndNotesPanel.getNotesPanel();
    }

    public KeyboardComponent getKeyboard()
    {
        return keysAndNotesPanel.getKeyboard();
    }

    public VelocityPanel getVelocityPanel()
    {
        return velocityPanel;
    }
    //------------------------------------------------------------------------------
    // PropertyChangeListener interface
    //------------------------------------------------------------------------------

    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {

        if (evt.getSource() == settings)
        {
            // 
        } else if (evt.getSource() == spModel)
        {
            if (evt.getPropertyName().equals(Phrase.PROP_NOTE_ADDED))
            {
                NoteEvent ne = (NoteEvent) evt.getNewValue();
            }
        }

    }
    // =======================================================================================================================
    // Private methods
    // =======================================================================================================================

    private boolean isDrums()
    {
        return keymap != null;
    }

    private void createUI()
    {
        keysAndNotesPanel = new KeysAndNotesPanel(this);
        toolbarPanel = new ToolbarPanel(this);      // Must be after keysAndNotesPanel creation        
        velocityPanel = new VelocityPanel(this);

        splitPane = new javax.swing.JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0);
        splitPane.setLeftComponent(keysAndNotesPanel);
        splitPane.setRightComponent(velocityPanel);

        setLayout(new BorderLayout());
        add(toolbarPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

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
            return getZoom().vFactor();
        }

        @Override
        public void setZoomYFactor(int newFactor)
        {
            int old = getZoomYFactor();
            setZoom(new ZoomValue(getZoomXFactor(), newFactor));
            firePropertyChange(Zoomable.PROPERTY_ZOOM_Y, old, newFactor);
        }

        @Override
        public int getZoomXFactor()
        {
            return getZoom().hFactor();
        }

        @Override
        public void setZoomXFactor(int newFactor)
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
