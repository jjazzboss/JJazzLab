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
package org.jjazz.test.walkingbass.explorer;

import java.awt.Component;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.WbpDatabase;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.IntRange;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * A dialog to query the WbpDatabase.
 */
public class WbpDatabaseExplorerDialog extends javax.swing.JDialog
{

    private static final Logger LOGGER = Logger.getLogger(WbpDatabaseExplorerDialog.class.getSimpleName());

    public WbpDatabaseExplorerDialog(boolean modal)
    {
        super(WindowManager.getDefault().getMainWindow(), modal);
        setAlwaysOnTop(modal);
        initComponents();
        tbl_wbpSources.setAutoCreateRowSorter(true);
        
                JPopupMenu pMenu = new JPopupMenu();
        pMenu.add(new JMenuItem(new FindDuplicatesAction(this)));
        pMenu.add(new JMenuItem(new PrintPhrasesAction(this)));
        tbl_wbpSources.setComponentPopupMenu(pMenu);


        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        UIUtilities.installEscapeKeyAction(this, () -> exit());
        UIUtilities.installEnterKeyAction(this, () -> doUpdate());

        // System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "[%1$s p=%2$.1f d=%3$.1f]");
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s p=%2$.1f d=%3$.1f");


        doUpdate();
    }

    public List<WbpSource> getSelectedWbpSources()
    {
        List<WbpSource> res = new ArrayList<>();
        for (int row : tbl_wbpSources.getSelectedRows())
        {
            int modelIndex = tbl_wbpSources.convertRowIndexToModel(row);
            res.add(getTableModel().getWbpSource(modelIndex));
        }
        return res;
    }


    // ========================================================================================================
    // Private methods
    // ========================================================================================================
    private MyModel getTableModel()
    {
        return (MyModel) tbl_wbpSources.getModel();
    }

    private void doUpdate()
    {
        var wbpSources = rb_rootProfile.isSelected() ? getRootProfileWbpSources() : getRpAndChordTypeWbpSources();
        tbl_wbpSources.setModel(new MyModel(wbpSources));
        adjustWidths();
        lbl_info.setText(wbpSources.size() + " WbpSource(s)");
    }

    private List<WbpSource> getRootProfileWbpSources()
    {

        // Compute the root profile
        SimpleChordSequence scs;
        try
        {
            scs = computeSimpleChordSequence();
        } catch (ParseException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return Collections.EMPTY_LIST;
        }

        String rootProfile = scs.getRootProfile();
        var wbpSources = WbpDatabase.getInstance().getWbpSources(rootProfile);

        return wbpSources;
    }

    private List<WbpSource> getRpAndChordTypeWbpSources()
    {
        // Compute the root profile
        SimpleChordSequence scs;
        try
        {
            scs = computeSimpleChordSequence();
        } catch (ParseException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return Collections.EMPTY_LIST;
        }

        String rootProfile = scs.getRootProfile();
        var wbpSources = new ArrayList<>(WbpDatabase.getInstance().getWbpSources(rootProfile));  // getWbpSources returns an immutable collection


        // Remove WbpSources for which chord types do not match enough
        var it = wbpSources.iterator();
        while (it.hasNext())
        {
            WbpSource wbps = it.next();

            var wbpScs = wbps.getSimpleChordSequence();
            float score = scs.getChordTypeSimilarityScore(wbpScs, WbpSourceAdaptation.MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE, true);
            if (score == 0)
            {
                it.remove();
            }
        }

        return wbpSources;

    }


    private SimpleChordSequence computeSimpleChordSequence() throws ParseException
    {
        int nbBars = (Integer) spn_barSize.getValue();

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
        res.removeRedundantChords();
        if (res.isEmpty())
        {
            throw new ParseException("No chords", 0);
        }

        return res;
    }


    /**
     * Pre-adjust the columns size parameters to have a correct display.
     */
    private void adjustWidths()
    {
        final TableColumnModel colModel = tbl_wbpSources.getColumnModel();
        final int EXTRA = 5;
        for (int colIndex = 0; colIndex < tbl_wbpSources.getColumnCount(); colIndex++)
        {
            // Handle header
            TableCellRenderer renderer = tbl_wbpSources.getTableHeader().getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(tbl_wbpSources, tbl_wbpSources.getColumnName(colIndex), true, true, 0, colIndex);
            int headerWidth = comp.getPreferredSize().width;

            int width = 20; // Min width

            // Handle data
            for (int row = 0; row < tbl_wbpSources.getRowCount(); row++)
            {
                renderer = tbl_wbpSources.getCellRenderer(row, colIndex);
                comp = tbl_wbpSources.prepareRenderer(renderer, row, colIndex);
                width = Math.max(comp.getPreferredSize().width, width);
            }
            width = Math.max(width, headerWidth);
            width = Math.min(width, 400);
            width += EXTRA;

            // We have our preferred width
            colModel.getColumn(colIndex).setPreferredWidth(width);

            // Also set max size
            switch (colIndex)
            {
                case MyModel.COL_ID -> colModel.getColumn(colIndex).setMaxWidth(width);
            }
        }
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
        public static final int COL_CHORDS = 1;
        public static final int COL_PHRASE = 2;

        private final List<WbpSource> wbpSources;

        public MyModel(List<WbpSource> wbpss)
        {
            wbpSources = wbpss;
        }

        WbpSource getWbpSource(int modelRow)
        {
            return wbpSources.get(modelRow);
        }

        @Override
        public int getRowCount()
        {
            return wbpSources.size();
        }

        @Override
        public int getColumnCount()
        {
            return 3;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            var res = switch (col)
            {
                case COL_ID, COL_SESSION_ID, COL_CHORDS, COL_PHRASE ->
                    String.class;
                case COL_SESSION_FROM_BAR ->
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
            WbpSource wbps = getWbpSource(row);
            Object res = switch (col)
            {
                case COL_ID ->
                    wbps.getId();
                case COL_SESSION_ID ->
                    wbps.getSessionId();
                case COL_SESSION_FROM_BAR ->
                    wbps.getSessionBarOffset();
                case COL_CHORDS ->
                    new ArrayList<>(wbps.getSimpleChordSequence()).toString();
                case COL_PHRASE ->
                    new ArrayList<>(wbps.getSizedPhrase()).toString();
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

        buttonGroup1 = new javax.swing.ButtonGroup();
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
        jLabel3 = new javax.swing.JLabel();
        lbl_info = new javax.swing.JLabel();
        btn_update = new javax.swing.JButton();
        rb_rootProfile = new javax.swing.JRadioButton();
        rb_rpChordTypes = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.title")); // NOI18N

        spn_barSize.setModel(new javax.swing.SpinnerNumberModel(4, 1, 4, 1));
        spn_barSize.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_barSizeStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.jLabel1.text")); // NOI18N

        tf_bar0.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar0.text")); // NOI18N
        tf_bar0.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar0ActionPerformed(evt);
            }
        });

        tf_bar1.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar1.text")); // NOI18N
        tf_bar1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar1ActionPerformed(evt);
            }
        });

        tf_bar2.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar2.text")); // NOI18N
        tf_bar2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar2ActionPerformed(evt);
            }
        });

        tf_bar3.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar3.text")); // NOI18N
        tf_bar3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar3ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.btn_clear.text")); // NOI18N
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.jLabel2.text")); // NOI18N

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

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_info, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.lbl_info.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_update, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.btn_update.text")); // NOI18N
        btn_update.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_updateActionPerformed(evt);
            }
        });

        buttonGroup1.add(rb_rootProfile);
        org.openide.awt.Mnemonics.setLocalizedText(rb_rootProfile, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.rb_rootProfile.text")); // NOI18N
        rb_rootProfile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rb_rootProfileActionPerformed(evt);
            }
        });

        buttonGroup1.add(rb_rpChordTypes);
        rb_rpChordTypes.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rb_rpChordTypes, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.rb_rpChordTypes.text")); // NOI18N
        rb_rpChordTypes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rb_rpChordTypesActionPerformed(evt);
            }
        });

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
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(rb_rootProfile)
                                .addGap(2, 2, 2))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tf_bar0, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tf_bar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(rb_rpChordTypes)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btn_update))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tf_bar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tf_bar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 130, Short.MAX_VALUE)
                                .addComponent(btn_clear))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_barSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1))
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(lbl_info, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(btn_update)
                    .addComponent(rb_rootProfile)
                    .addComponent(rb_rpChordTypes))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbl_info)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
        lbl_info.setText("-");
        tbl_wbpSources.setModel(new MyModel(Collections.EMPTY_LIST));
    }//GEN-LAST:event_btn_clearActionPerformed

    private void tf_bar1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar1ActionPerformed
    {//GEN-HEADEREND:event_tf_bar1ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar1ActionPerformed

    private void btn_updateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_updateActionPerformed
    {//GEN-HEADEREND:event_btn_updateActionPerformed
        doUpdate();
    }//GEN-LAST:event_btn_updateActionPerformed

    private void tf_bar0ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar0ActionPerformed
    {//GEN-HEADEREND:event_tf_bar0ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar0ActionPerformed

    private void tf_bar2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar2ActionPerformed
    {//GEN-HEADEREND:event_tf_bar2ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar2ActionPerformed

    private void tf_bar3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar3ActionPerformed
    {//GEN-HEADEREND:event_tf_bar3ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar3ActionPerformed

    private void rb_rootProfileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rb_rootProfileActionPerformed
    {//GEN-HEADEREND:event_rb_rootProfileActionPerformed
        doUpdate();
    }//GEN-LAST:event_rb_rootProfileActionPerformed

    private void rb_rpChordTypesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rb_rpChordTypesActionPerformed
    {//GEN-HEADEREND:event_rb_rpChordTypesActionPerformed
        doUpdate();
    }//GEN-LAST:event_rb_rpChordTypesActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_clear;
    private javax.swing.JButton btn_update;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_info;
    private javax.swing.JRadioButton rb_rootProfile;
    private javax.swing.JRadioButton rb_rpChordTypes;
    private javax.swing.JSpinner spn_barSize;
    private javax.swing.JTable tbl_wbpSources;
    private javax.swing.JTextField tf_bar0;
    private javax.swing.JTextField tf_bar1;
    private javax.swing.JTextField tf_bar2;
    private javax.swing.JTextField tf_bar3;
    // End of variables declaration//GEN-END:variables


}
