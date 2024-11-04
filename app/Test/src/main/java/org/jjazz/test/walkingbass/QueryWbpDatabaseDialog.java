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
package org.jjazz.test.walkingbass;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * A dialog to query the WbpDatabase.
 */
public class QueryWbpDatabaseDialog extends javax.swing.JDialog
{
    
    public QueryWbpDatabaseDialog()
    {
        super(WindowManager.getDefault().getMainWindow(), false);
        initComponents();
        tbl_wbpSources.setAutoCreateRowSorter(true);
        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        UIUtilities.installEscapeKeyAction(this, () -> exit());

        // System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "[%1$s p=%2$.1f d=%3$.1f]");
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s p=%2$.1f d=%3$.1f");
        
        tbl_wbpSources.setModel(new MyModel(new ArrayList<>()));
    }

    // ========================================================================================================
    // Private methods
    // ========================================================================================================

    private void updateTableRootProfile()
    {
        int nbBars = (Integer) spn_barSize.getValue();

        // Compute the root profile
        SimpleChordSequence scs;
        try
        {
            scs = computeSimpleChordSequence(nbBars);
        } catch (ParseException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }
        
        String rootProfile = scs.getRootProfile();
        var wbpSources = WbpDatabase.getInstance().getWbpSources(rootProfile);
        tbl_wbpSources.setModel(new MyModel(wbpSources));
        updateTable(wbpSources, "rp only");
    }
    
    private void updateTableRootProfileAndChordTypes()
    {
        int nbBars = (Integer) spn_barSize.getValue();

        // Compute the root profile
        SimpleChordSequence scs;
        try
        {
            scs = computeSimpleChordSequence(nbBars);
        } catch (ParseException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }
        
        String rootProfile = scs.getRootProfile();
        var wbpSources = new ArrayList<>(WbpDatabase.getInstance().getWbpSources(rootProfile));  // getWbpSources returns an immutable collection


        // Remove WbpSources for which chord types do not match enough
        var it = wbpSources.iterator();
        while (it.hasNext())
        {
            WbpSource wbps = it.next();
            
            var wbpScs = wbps.getSimpleChordSequence();
            float score = scs.getChordTypeSimilarityScore(wbpScs, 56, true);
            if (score < 56) // 56 = 3rd + 5th + 6th/7th
            {
                it.remove();
            }
        }
        
        updateTable(wbpSources, "rp+ct");
        
    }
    
    private void updateTable(List<WbpSource> wbpSources, String infoText)
    {
        tbl_wbpSources.setModel(new MyModel(wbpSources));
        lbl_info.setText(infoText + " " + wbpSources.size());
    }
    
    
    private SimpleChordSequence computeSimpleChordSequence(int nbBars) throws ParseException
    {
        SimpleChordSequence res = new SimpleChordSequence(new IntRange(0, nbBars - 1), TimeSignature.FOUR_FOUR);
        switch (nbBars)
        {
            case 4:
                var str = tf_bar3.getText();
                var cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 3, false);
                res.addAll(cliList);
            case 3:
                str = tf_bar2.getText();
                cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 2, false);
                res.addAll(cliList);
            case 2:
                str = tf_bar1.getText();
                cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 1, false);
                res.addAll(cliList);
            case 1:
                str = tf_bar0.getText();
                cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 0, false);
                res.addAll(cliList);
                break;
            default:
                throw new IllegalStateException("nbBars=" + nbBars);
        }
        res.removeDuplicateChords();
        return res;
    }
    
    private void exit()
    {
        setVisible(false);
        dispose();
    }

    // ========================================================================================================
    // Inner classes
    // ========================================================================================================
    private class MyModel extends AbstractTableModel
    {
        
        public static final int COL_ID = 0;
        public static final int COL_SESSION_ID = 100;
        public static final int COL_SESSION_FROM_BAR = 200;
        public static final int COL_SIZE = 1;
        public static final int COL_CHORDS = 2;
        public static final int COL_PHRASE = 3;
        
        private final List<WbpSource> wbpSources;
        
        public MyModel(List<WbpSource> wbpss)
        {
            wbpSources = wbpss;
        }
        
        @Override
        public int getRowCount()
        {
            return wbpSources.size();
        }
        
        @Override
        public int getColumnCount()
        {
            return 4;
        }
        
        @Override
        public Class<?> getColumnClass(int col)
        {
            var res = switch (col)
            {
                case COL_ID, COL_SESSION_ID, COL_CHORDS, COL_PHRASE ->
                    String.class;
                case COL_SESSION_FROM_BAR, COL_SIZE ->
                    Integer.class;
                default -> throw new IllegalStateException("col=" + col);
            };
            return res;
        }
        
        @Override
        public String getColumnName(int col)
        {
            String s = switch (col)
            {
                case COL_ID ->
                    "Id";
                case COL_SESSION_ID ->
                    "Session";
                case COL_SESSION_FROM_BAR ->
                    "FromBar";
                case COL_SIZE ->
                    "NbBars";
                case COL_CHORDS ->
                    "Chords";
                case COL_PHRASE ->
                    "Phrase";
                default -> throw new IllegalStateException("columnIndex=" + col);
            };
            return s;
        }
        
        @Override
        public Object getValueAt(int row, int col)
        {
            WbpSource wbps = wbpSources.get(row);
            Object res = switch (col)
            {
                case COL_ID ->
                    wbps.getId();
                case COL_SESSION_ID ->
                    wbps.getSessionId();
                case COL_SESSION_FROM_BAR ->
                    wbps.getSessionBarOffset();
                case COL_SIZE ->
                    wbps.getBarRange().size();
                case COL_CHORDS ->
                    new ArrayList<>(wbps.getSimpleChordSequence()).toString();
                case COL_PHRASE ->
                    Utilities.truncateWithDots(new ArrayList<>(wbps.getSizedPhrase()).toString(), 100);
                default -> throw new IllegalStateException("columnIndex=" + col);
            };
            return res;
        }
    }

    // ========================================================================================================
    // UI
    // ========================================================================================================
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        spn_barSize = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        tf_bar0 = new javax.swing.JTextField();
        tf_bar1 = new javax.swing.JTextField();
        tf_bar2 = new javax.swing.JTextField();
        tf_bar3 = new javax.swing.JTextField();
        btn_clear = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_wbpSources = new javax.swing.JTable();
        btn_updateRootProfile = new javax.swing.JButton();
        btn_updateRpChordTypes = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        lbl_info = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.title")); // NOI18N

        spn_barSize.setModel(new javax.swing.SpinnerNumberModel(4, 1, 4, 1));
        spn_barSize.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_barSizeStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.jLabel1.text")); // NOI18N

        tf_bar0.setText(org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.tf_bar0.text")); // NOI18N

        tf_bar1.setText(org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.tf_bar1.text")); // NOI18N
        tf_bar1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar1ActionPerformed(evt);
            }
        });

        tf_bar2.setText(org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.tf_bar2.text")); // NOI18N

        tf_bar3.setText(org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.tf_bar3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.btn_clear.text")); // NOI18N
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.jLabel2.text")); // NOI18N

        tbl_wbpSources.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String []
            {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tbl_wbpSources.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        jScrollPane1.setViewportView(tbl_wbpSources);

        org.openide.awt.Mnemonics.setLocalizedText(btn_updateRootProfile, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.btn_updateRootProfile.text")); // NOI18N
        btn_updateRootProfile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_updateRootProfileActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_updateRpChordTypes, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.btn_updateRpChordTypes.text")); // NOI18N
        btn_updateRpChordTypes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_updateRpChordTypesActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_info, org.openide.util.NbBundle.getMessage(QueryWbpDatabaseDialog.class, "QueryWbpDatabaseDialog.lbl_info.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_barSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1))
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tf_bar0, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tf_bar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(20, 20, 20)
                                .addComponent(btn_updateRootProfile)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btn_updateRpChordTypes)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lbl_info))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tf_bar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tf_bar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 122, Short.MAX_VALUE)
                                .addComponent(btn_clear)))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {tf_bar0, tf_bar1, tf_bar2, tf_bar3});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_barSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_bar0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tf_bar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tf_bar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tf_bar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_clear))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_updateRootProfile)
                            .addComponent(btn_updateRpChordTypes)
                            .addComponent(jLabel3))
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(lbl_info)
                        .addGap(7, 7, 7)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 365, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void spn_barSizeStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_barSizeStateChanged
    {//GEN-HEADEREND:event_spn_barSizeStateChanged
        int value = (Integer) spn_barSize.getValue();
        tf_bar1.setEnabled(value >= 2);
        tf_bar2.setEnabled(value >= 3);
        tf_bar3.setEnabled(value >= 4);
    }//GEN-LAST:event_spn_barSizeStateChanged

    private void btn_clearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearActionPerformed
    {//GEN-HEADEREND:event_btn_clearActionPerformed
        tf_bar0.setText("");
        tf_bar1.setText("");
        tf_bar2.setText("");
        tf_bar3.setText("");
    }//GEN-LAST:event_btn_clearActionPerformed

    private void btn_updateRootProfileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_updateRootProfileActionPerformed
    {//GEN-HEADEREND:event_btn_updateRootProfileActionPerformed
        updateTableRootProfile();
    }//GEN-LAST:event_btn_updateRootProfileActionPerformed

    private void btn_updateRpChordTypesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_updateRpChordTypesActionPerformed
    {//GEN-HEADEREND:event_btn_updateRpChordTypesActionPerformed
        updateTableRootProfileAndChordTypes();
    }//GEN-LAST:event_btn_updateRpChordTypesActionPerformed

    private void tf_bar1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar1ActionPerformed
    {//GEN-HEADEREND:event_tf_bar1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tf_bar1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_clear;
    private javax.swing.JButton btn_updateRootProfile;
    private javax.swing.JButton btn_updateRpChordTypes;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_info;
    private javax.swing.JSpinner spn_barSize;
    private javax.swing.JTable tbl_wbpSources;
    private javax.swing.JTextField tf_bar0;
    private javax.swing.JTextField tf_bar1;
    private javax.swing.JTextField tf_bar2;
    private javax.swing.JTextField tf_bar3;
    // End of variables declaration//GEN-END:variables

    
}
