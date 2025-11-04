/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.cl_editorimpl.actions;

import org.jjazz.uiutilities.api.FixedCompSizeGridLayout;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.spi.ChordTypeSelectorUIProvider;
import static org.jjazz.cl_editorimpl.actions.ChordAuditioning.createChordSamplePhrase;
import static org.jjazz.cl_editorimpl.actions.ChordAuditioning.findChannel;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.ChordType.Family;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * A JTable-based chord type selector UI component.
 */
@ServiceProvider(service = ChordTypeSelectorUIProvider.class)
public class SetChordTypeTableSelector extends JPanel implements ChordTypeSelectorUIProvider
{

    private static final String PREF_HIDE_COMPLEX_CHORDS = "HideComplexChords";
    private static final int SIMPLE_CHORD_MAX_NB_NOTES = 4;
    private Consumer<ChordType> chordTypeSetter;
    private ChordSymbol chordSymbol;
    private final FixedCompSizeGridLayout fixedLayout;
    private static final Preferences prefs = NbPreferences.forModule(SetChordTypeTableSelector.class);
    private static final Logger LOGGER = Logger.getLogger(SetChordTypeTableSelector.class.getSimpleName());

    public SetChordTypeTableSelector()
    {
        this.chordSymbol = new ChordSymbol();

        initComponents();
        ta_help.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);    // right alignment
        var d = new JButton("A#13b9#11").getPreferredSize();
        fixedLayout = new FixedCompSizeGridLayout(getCtColumnCount(), new Dimension(d.width, d.height));
        pnl_chordTypes.setLayout(fixedLayout);

        setComplexChordsHidden(prefs.getBoolean(PREF_HIDE_COMPLEX_CHORDS, true));
    }


    @Override
    public JComponent getUI(ChordSymbol chordSymbol, Consumer<ChordType> ctSetter)
    {
        Objects.requireNonNull(ctSetter);
        if (chordSymbol != null)
        {
            this.chordSymbol = chordSymbol;
        }
        this.chordTypeSetter = ctSetter;
        updateGrid();


        return this;
    }


    // =====================================================================================================
    // Private methods
    // =====================================================================================================    
    private void ctSelected(ChordType ct)
    {
        LOGGER.log(Level.FINE, "ctSelected() -- {0}", ct);
        chordTypeSetter.accept(ct);
    }

    private void setComplexChordsHidden(boolean b)
    {
        cb_hideComplex.setSelected(b);
        prefs.putBoolean(PREF_HIDE_COMPLEX_CHORDS, b);
        updateGrid();
    }

    private boolean isComplexChordsHidden()
    {
        return cb_hideComplex.isSelected();
    }

    /**
     * Clear and fill the panel with header labels and buttons for each chord type.
     * <p>
     */
    private void updateGrid()
    {
        var maxNbChordNotes = isComplexChordsHidden() ? SIMPLE_CHORD_MAX_NB_NOTES : Integer.MAX_VALUE;


        ListMultimap<Family, ChordType> mmapFamilyChordTypes = MultimapBuilder.enumKeys(Family.class).arrayListValues().build();
        ChordTypeDatabase.getDefault().getChordTypes().stream()
                .filter(ct -> ct.getNbDegrees() <= maxNbChordNotes)
                .forEach(ct -> mmapFamilyChordTypes.put(ct.getFamily(), ct));


        pnl_chordTypes.removeAll();

        int nbRows = getCtRowCount(mmapFamilyChordTypes) + 1;   // +1 for header
        int nbCols = getCtColumnCount();

        for (int row = 0; row < nbRows; row++)
        {
            for (int col = 0; col < nbCols; col++)
            {
                var f = getFamily(col);
                var cts = mmapFamilyChordTypes.get(f);
                if (row == 0)
                {
                    var lbl = buildColumnHeaderLabel(f.toString());
                    pnl_chordTypes.add(lbl);
                } else if (row - 1 < cts.size())
                {
                    var ct = cts.get(row - 1);
                    JButton btn = buildChordButton(ct);
                    pnl_chordTypes.add(btn);
                } else
                {
                    pnl_chordTypes.add(new JLabel(""));
                }
            }
        }

        pnl_chordTypes.revalidate();
        pnl_chordTypes.repaint();

    }

    private int getCtRowCount(ListMultimap<Family, ChordType> mmapFamilyChordTypes)
    {
        int res = 0;
        for (var f : mmapFamilyChordTypes.keySet())
        {
            res = Math.max(res, mmapFamilyChordTypes.get(f).size());
        }
        return res;
    }

    private int getCtColumnCount()
    {
        return Family.values().length;
    }

    private Family getFamily(int ordinal)
    {
        return Family.values()[ordinal];
    }

    private JButton buildChordButton(ChordType ct)
    {
        ChordSymbol cs = null;
        try
        {
            String s = chordSymbol.getRootNote().toRelativeNoteString() + ct.getName();
            cs = new ChordSymbol(s);        // Can not be NC, so no need to use ExtChordSymbol.get()
        } catch (ParseException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
        JButton btn = new JButton(cs.getName());
        btn.setToolTipText(cs.toNoteString());
        final var cs2 = cs;
        btn.addActionListener(e -> 
        {
            boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
            boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
            if (shift && ctrl)
            {
                hearChord(cs2);
            } else
            {
                ctSelected(ct);
            }
        });
        return btn;
    }

    private void hearChord(ChordSymbol cs)
    {
        TestPlayer tp = TestPlayer.getDefault();

        var song = getSong();
        int channel = song == null ? 0 : findChannel(song);
        Phrase p = createChordSamplePhrase(List.of(cs), channel);
        try
        {
            tp.playTestNotes(p, null);
        } catch (MusicGenerationException ex)
        {
            LOGGER.log(Level.WARNING, "hearChord() Unexpected error playing chord notes for cs={0}. ex={1}", new Object[]
            {
                cs, ex.getMessage()
            });
        }
    }

    private Song getSong()
    {
        var editor = CL_EditorTopComponent.getActive();
        Song res = editor != null ? editor.getSongModel() : null;
        return res;
    }

    private JLabel buildColumnHeaderLabel(String s)
    {
        JLabel lbl = new JLabel(s);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_cancel = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        ta_help = new org.jjazz.flatcomponents.api.HelpTextArea();
        cb_hideComplex = new javax.swing.JCheckBox();
        scollPane = new javax.swing.JScrollPane();
        pnl_chordTypes = new javax.swing.JPanel();

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(SetChordTypeTableSelector.class, "SetChordTypeTableSelector.btn_cancel.text")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        jScrollPane2.setBorder(null);

        ta_help.setColumns(20);
        ta_help.setRows(1);
        ta_help.setText(org.openide.util.NbBundle.getMessage(SetChordTypeTableSelector.class, "SetChordTypeTableSelector.ta_help.text")); // NOI18N
        jScrollPane2.setViewportView(ta_help);

        org.openide.awt.Mnemonics.setLocalizedText(cb_hideComplex, org.openide.util.NbBundle.getMessage(SetChordTypeTableSelector.class, "SetChordTypeTableSelector.cb_hideComplex.text")); // NOI18N
        cb_hideComplex.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_hideComplexActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_chordTypesLayout = new javax.swing.GroupLayout(pnl_chordTypes);
        pnl_chordTypes.setLayout(pnl_chordTypesLayout);
        pnl_chordTypesLayout.setHorizontalGroup(
            pnl_chordTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 545, Short.MAX_VALUE)
        );
        pnl_chordTypesLayout.setVerticalGroup(
            pnl_chordTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 341, Short.MAX_VALUE)
        );

        scollPane.setViewportView(pnl_chordTypes);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scollPane, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(cb_hideComplex)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_cancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scollPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cancel)
                    .addComponent(cb_hideComplex))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        ctSelected(null);
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void cb_hideComplexActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_hideComplexActionPerformed
    {//GEN-HEADEREND:event_cb_hideComplexActionPerformed
        setComplexChordsHidden(cb_hideComplex.isSelected());
    }//GEN-LAST:event_cb_hideComplexActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JCheckBox cb_hideComplex;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel pnl_chordTypes;
    private javax.swing.JScrollPane scollPane;
    private org.jjazz.flatcomponents.api.HelpTextArea ta_help;
    // End of variables declaration//GEN-END:variables


}
