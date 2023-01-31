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

import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SpinnerNumberModel;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.actions.HearSelection;
import org.jjazz.pianoroll.actions.PlaybackAutoScroll;
import org.jjazz.pianoroll.actions.SnapToGrid;
import org.jjazz.pianoroll.actions.Solo;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.NotesSelection;
import org.jjazz.pianoroll.api.NotesSelectionListener;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.util.Exceptions;


/**
 * Toolbar panel of the PianoRollEditor.
 */
public class ToolbarPanel extends javax.swing.JPanel implements PropertyChangeListener
{

    private final PianoRollEditor editor;
    private int lastSpinnerValue;
    private String title;
    private NotesSelection selection;
    private static final Logger LOGGER = Logger.getLogger(ToolbarPanel.class.getSimpleName());

    /**
     * Creates new form ToolbarPanel
     */
    public ToolbarPanel(PianoRollEditor editor, String title)
    {
        this.editor = editor;
        this.title = title;


        initComponents();


        lbl_title.setText(title);


        tbtn_hearNotes.setAction(new HearSelection(editor));
        tbtn_snap.setAction(new SnapToGrid(editor));
        tbtn_playbackAutoScroll.setAction(new PlaybackAutoScroll(editor));
        tbtn_solo.setAction(new Solo(editor));


        var qModel = new DefaultComboBoxModel(Quantization.values());
        qModel.removeElement(Quantization.HALF_BAR);
        qModel.removeElement(Quantization.OFF);
        cmb_quantization.setModel(qModel);
        cmb_quantization.setSelectedItem(editor.getQuantization());
        cmb_quantization.setRenderer(new QuantizationRenderer());


        tbtn_snap.setSelected(editor.isSnapEnabled());


        editor.addPropertyChangeListener(this);


        NotesSelectionListener nsl = NotesSelectionListener.getInstance(editor);
        nsl.addListener(l -> selectionChanged(nsl.getSelection()));


        lastSpinnerValue = (Integer) spn_velocity.getModel().getValue();

    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
        lbl_title.setText(title);
    }


    public void cleanup()
    {
        editor.removePropertyChangeListener(this);
    }

    // ====================================================================================
    // PropertyChangeListener interface
    // ====================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // LOGGER.severe("propertyChange() -- " + Utilities.toDebugString(evt));

        if (evt.getSource() == editor)
        {
            switch (evt.getPropertyName())
            {
                case PianoRollEditor.PROP_SNAP_ENABLED ->
                    tbtn_snap.setSelected(editor.isSnapEnabled());
                case PianoRollEditor.PROP_QUANTIZATION ->
                    cmb_quantization.setSelectedItem(editor.getQuantization());
                default ->
                {
                }
            }

        } else if (evt.getSource() instanceof NoteView nv)
        {
            if (evt.getPropertyName().equals(NoteView.PROP_MODEL))
            {
                updateVelocityUI();
            }
        }
    }
    // ====================================================================================
    // Private methods
    // ====================================================================================

    private void selectionChanged(NotesSelection newSelection)
    {
        if (selection != null)
        {
            selection.getNoteViews().forEach(nv -> nv.removePropertyChangeListener(this));
        }
        selection = newSelection;
        selection.getNoteViews().forEach(nv -> nv.addPropertyChangeListener(this));
        updateVelocityUI();
    }


    /**
     * Update the JSpinner when change resulted from an action which is NOT a JSpinner direct change.
     * <p>
     * It can be a selection change or a selected note has changed by other means. We must make sure our ChangeListener method is
     * not called when updating the JSpinner.
     * <p>
     */
    private void updateVelocityUI()
    {
        var nvs = selection.getNoteViews();
        LOGGER.log(Level.FINE, "updateVelocitySpinner() -- nvs={0}", nvs);

        var b = !nvs.isEmpty();
        spn_velocity.setEnabled(b);
        lbl_velocity.setEnabled(b);

        if (b)
        {
            int v0 = nvs.get(0).getModel().getVelocity();

            spn_velocity.setValue(v0, true);          // Important: avoid our ChangeListener to be notified 
            lastSpinnerValue = v0;

            boolean multipleValues = nvs.stream()
                    .anyMatch(nv -> nv.getModel().getVelocity() != v0);
            Font f = spn_velocity.getFont().deriveFont(multipleValues ? Font.ITALIC : Font.PLAIN);
            spn_velocity.setFont(f);
        }
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_left = new javax.swing.JPanel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(20, 32767));
        pnl_editTools = new EditToolBar(editor);
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(30, 32767));
        tbtn_snap = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        cmb_quantization = new javax.swing.JComboBox<>();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(30, 32767));
        lbl_velocity = new javax.swing.JLabel();
        spn_velocity = new org.jjazz.ui.utilities.api.WheelSpinner();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(20, 32767));
        pnl_title = new javax.swing.JPanel();
        lbl_title = new javax.swing.JLabel();
        pnl_right = new javax.swing.JPanel();
        pnl_miscButtons = new javax.swing.JPanel();
        tbtn_hearNotes = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        tbtn_solo = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        tbtn_playbackAutoScroll = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();

        setLayout(new java.awt.BorderLayout());

        pnl_left.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2));
        pnl_left.add(filler2);
        pnl_left.add(pnl_editTools);
        pnl_left.add(filler1);

        tbtn_snap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/SnapOFF.png"))); // NOI18N
        tbtn_snap.setToolTipText(org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.tbtn_snap.toolTipText")); // NOI18N
        tbtn_snap.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/SnapON.png"))); // NOI18N
        pnl_left.add(tbtn_snap);

        cmb_quantization.setToolTipText(org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.cmb_quantization.toolTipText")); // NOI18N
        cmb_quantization.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cmb_quantizationActionPerformed(evt);
            }
        });
        pnl_left.add(cmb_quantization);
        pnl_left.add(filler3);

        lbl_velocity.setFont(lbl_velocity.getFont().deriveFont(lbl_velocity.getFont().getSize()-1f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_velocity, org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.lbl_velocity.text")); // NOI18N
        pnl_left.add(lbl_velocity);

        spn_velocity.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));
        spn_velocity.setToolTipText(org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.spn_velocity.toolTipText")); // NOI18N
        spn_velocity.setColumns(2);
        spn_velocity.setEnabled(false);
        spn_velocity.setLoopValues(false);
        spn_velocity.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_velocityStateChanged(evt);
            }
        });
        pnl_left.add(spn_velocity);
        pnl_left.add(filler4);

        add(pnl_left, java.awt.BorderLayout.WEST);

        pnl_title.setLayout(new java.awt.BorderLayout());

        lbl_title.setFont(lbl_title.getFont().deriveFont(lbl_title.getFont().getStyle() | java.awt.Font.BOLD));
        lbl_title.setForeground(new java.awt.Color(168, 171, 199));
        lbl_title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_title, org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.lbl_title.text")); // NOI18N
        pnl_title.add(lbl_title, java.awt.BorderLayout.CENTER);

        add(pnl_title, java.awt.BorderLayout.CENTER);

        pnl_right.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 2));

        pnl_miscButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 1, 2));

        tbtn_hearNotes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/HearNoteOFF.png"))); // NOI18N
        tbtn_hearNotes.setToolTipText(org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.tbtn_hearNotes.toolTipText")); // NOI18N
        pnl_miscButtons.add(tbtn_hearNotes);

        tbtn_solo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/SoloOFF.png"))); // NOI18N
        tbtn_solo.setToolTipText(org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.tbtn_solo.toolTipText")); // NOI18N
        tbtn_solo.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/SoloON.png"))); // NOI18N
        pnl_miscButtons.add(tbtn_solo);

        tbtn_playbackAutoScroll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/PlaybackAutoScrollOFF.png"))); // NOI18N
        tbtn_playbackAutoScroll.setToolTipText(org.openide.util.NbBundle.getMessage(ToolbarPanel.class, "ToolbarPanel.tbtn_playbackAutoScroll.toolTipText")); // NOI18N
        tbtn_playbackAutoScroll.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/pianoroll/actions/resources/PlaybackAutoScrollON.png"))); // NOI18N
        pnl_miscButtons.add(tbtn_playbackAutoScroll);

        pnl_right.add(pnl_miscButtons);

        add(pnl_right, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void cmb_quantizationActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmb_quantizationActionPerformed
    {//GEN-HEADEREND:event_cmb_quantizationActionPerformed
        editor.setQuantization((Quantization) cmb_quantization.getSelectedItem());
    }//GEN-LAST:event_cmb_quantizationActionPerformed


    private void spn_velocityStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_velocityStateChanged
    {//GEN-HEADEREND:event_spn_velocityStateChanged
        // As we generate an undoable event here, the state change must be triggered only by a direct user action
        // on the JSpinner, ie change field or increase/decrease value.
        // Use WheelSpinner.setValue(Object, true) to avoid triggering the ChangeEvent.

        int newSpinnerValue = (int) spn_velocity.getValue();
        boolean isManualEdit;

        if (GeneralUISettings.getInstance().isChangeValueWithMouseWheelEnabled())
        {
            isManualEdit = spn_velocity.isChangeFromManualEdit();
        } else
        {
            // Not perfect method to detect manual edit, but no choice
            int stepSize = (int) ((SpinnerNumberModel) spn_velocity.getModel()).getStepSize();
            isManualEdit = newSpinnerValue != lastSpinnerValue + stepSize && newSpinnerValue != lastSpinnerValue - stepSize;
        }

        LOGGER.log(Level.FINE, "spn_velocityStateChanged() -- isManualEdit={0}  newSpinnerValue={1}", new Object[]
        {
            isManualEdit, newSpinnerValue
        });


        // Create an undoable event since it was a change made by user to the JSpinner
        String undoText = ResUtil.getString(getClass(), "ChangeVelocity");
        editor.getUndoManager().startCEdit(editor, undoText);


        var selectedNvs = editor.getSelectedNoteViews();

        if (isManualEdit)
        {
            // User typed a new value, apply it to all notes
            selectedNvs.stream()
                    .map(nv -> nv.getModel())
                    .filter(ne -> ne.getVelocity() != newSpinnerValue)
                    .forEach(ne -> editor.getModel().replace(ne, ne.getCopyVel(newSpinnerValue)));
        } else
        {
            // User increased/decreased, apply the delta to all notes
            int delta = newSpinnerValue - lastSpinnerValue;
            selectedNvs.stream()
                    .map(nv -> nv.getModel())
                    .forEach(ne ->
                    {
                        int newVel = MidiUtilities.limit(ne.getVelocity() + delta);
                        editor.getModel().replace(ne, ne.getCopyVel(newVel));
                    });
        }

        editor.getUndoManager().endCEdit(undoText);

        lastSpinnerValue = newSpinnerValue;
    }//GEN-LAST:event_spn_velocityStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<Quantization> cmb_quantization;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.JLabel lbl_title;
    private javax.swing.JLabel lbl_velocity;
    private javax.swing.JPanel pnl_editTools;
    private javax.swing.JPanel pnl_left;
    private javax.swing.JPanel pnl_miscButtons;
    private javax.swing.JPanel pnl_right;
    private javax.swing.JPanel pnl_title;
    private org.jjazz.ui.utilities.api.WheelSpinner spn_velocity;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_hearNotes;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_playbackAutoScroll;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_snap;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton tbtn_solo;
    // End of variables declaration//GEN-END:variables


    /**
     * A renderer for Quantization values
     */
    public class QuantizationRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Quantization q = (Quantization) value;
            var sd = q.getSymbolicDuration();
            label.setText(sd.getReadableName());
            // label.setIcon(NoteIcons.get20x30(sd));
            // label.setPreferredSize(new Dimension(label.getIcon().getIconWidth(), label.getIcon().getIconHeight()));
            return label;
        }
    }

}
