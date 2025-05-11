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
package org.jjazz.ss_editorimpl.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorClientProperties;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

public class CompactRpViewSettingsDialog extends javax.swing.JDialog
{

    private static CompactRpViewSettingsDialog INSTANCE;
    private MyModel tblModel;
    private boolean exitOk;
    private static final Logger LOGGER = Logger.getLogger(CompactRpViewSettingsDialog.class.getSimpleName());

    public static CompactRpViewSettingsDialog getInstance()
    {
        synchronized (CompactRpViewSettingsDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new CompactRpViewSettingsDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    private CompactRpViewSettingsDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
    }

    /**
     * Used to initialize the dialog.
     * <p>
     * @param editor
     */
    public void setModel(SS_Editor editor)
    {
        List<Rhythm> uniqueRhythms = editor.getSongModel().getSongStructure().getUniqueRhythms(false, true);
        tblModel = new MyModel(uniqueRhythms, editor);
        tbl_rhythmParameters.setModel(tblModel);
        for (int i = MyModel.COL_FIRST_RHYTHM; i < tbl_rhythmParameters.getColumnCount(); i++)
        {
            tbl_rhythmParameters.getColumnModel().getColumn(i).setCellRenderer(new MyCellRenderer());
        }
        pack();
    }


    /**
     * Get the list of the visible RhythmParameters for each song rhythm.
     *
     * @return Valid only if isExitOk() if true.
     */
    public HashMap<Rhythm, List<RhythmParameter<?>>> getResult()
    {
        HashMap<Rhythm, List<RhythmParameter<?>>> res = new HashMap<>();
        if (tblModel != null)
        {
            for (int i = 0; i < tblModel.rhythms.size(); i++)
            {
                Rhythm r = tblModel.rhythms.get(i);
                List<RhythmParameter<?>> rps = new ArrayList<>();
                res.put(r, rps);
                for (int j = 0; j < tblModel.uniqueRps.size(); j++)
                {
                    if (tblModel.data[i][j])
                    {
                        rps.add(getRpFromClass(r.getRhythmParameters(), tblModel.uniqueRps.get(j).getClass()));
                    }
                }
            }
        }

        return res;
    }


    /**
     * Overridden to add global key bindings
     *
     * @return
     */
    @Override
    protected JRootPane createRootPane()
    {
        JRootPane contentPane = new JRootPane();
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");   
        contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionOK();
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");   
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionCancel();
            }
        });
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("Control-A"), "actionSelectAll");   
        contentPane.getActionMap().put("actionSelectAll", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionSelectAll();
            }
        });
        return contentPane;
    }

    private RhythmParameter<?> getRpFromClass(List<RhythmParameter<?>> rps, Class<?> rpClass)
    {
        return rps.stream().filter(rp -> rpClass.isAssignableFrom(rp.getClass())).findAny().orElse(null);
    }

    private void actionSelectAll()
    {
        tbl_rhythmParameters.selectAll();
    }

    private void actionOK()
    {
        // Check that there is at least one RP selected for each rhythm
        var result = getResult();
        for (var rpList : result.values())
        {
            if (rpList.isEmpty())
            {
                String msg = ResUtil.getString(getClass(), "CompactRpViewSettingsDialogErrNoRp");
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }
        }

        exitOk = true;
        setVisible(false);
    }

    private void actionCancel()
    {
        exitOk = false;
        setVisible(false);
    }

    public boolean isExitOk()
    {
        return exitOk;
    }

    /**
     * This method is called from within the constructor to setModel the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_Ok = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_rhythmParameters = new javax.swing.JTable();

        setTitle(org.openide.util.NbBundle.getMessage(CompactRpViewSettingsDialog.class, "CompactRpViewSettingsDialog.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(CompactRpViewSettingsDialog.class, "CompactRpViewSettingsDialog.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(CompactRpViewSettingsDialog.class, "CompactRpViewSettingsDialog.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CompactRpViewSettingsDialog.class, "CompactRpViewSettingsDialog.jLabel1.text")); // NOI18N

        tbl_rhythmParameters.setAutoCreateRowSorter(true);
        tbl_rhythmParameters.setModel(new javax.swing.table.DefaultTableModel(
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
        tbl_rhythmParameters.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tbl_rhythmParameters.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane1.setViewportView(tbl_rhythmParameters);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 86, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(15, 15, 15)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_Ok)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btn_Cancel)
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        actionOK();
    }//GEN-LAST:event_btn_OkActionPerformed

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        actionCancel();
    }//GEN-LAST:event_btn_CancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tbl_rhythmParameters;
    // End of variables declaration//GEN-END:variables


    public class MyModel extends AbstractTableModel
    {

        static final int COL_FIRST_RHYTHM = 1;
        static final int COL_RP = 0;
        List<Rhythm> rhythms;
        boolean[][] data;
        List<RhythmParameter<?>> uniqueRps = new ArrayList<>();
        int colCount;
        int rowCount;

        public MyModel(List<Rhythm> rhythms, SS_Editor editor)
        {
            this.rhythms = rhythms;
            colCount = rhythms.size() + 1;
            
            
            // Update uniqueRps
            HashSet<Class<?>> rpUniqueClasses = new HashSet<>();
            for (var r : rhythms)
            {
                for (var rp : r.getRhythmParameters())
                {
                    if (!rpUniqueClasses.contains(rp.getClass()))
                    {
                        uniqueRps.add(rp);
                        rpUniqueClasses.add(rp.getClass());
                    }
                }
            }                        
            rowCount = uniqueRps.size();
            
            
            // Update data
            data = new boolean[rhythms.size()][uniqueRps.size()];
            for (int i = 0; i < rhythms.size(); i++)
            {
                Rhythm r = rhythms.get(i);
                //var compactViewRps = editor.getCompactViewRPs(r);
                var compactViewRps = SS_EditorClientProperties.getCompactViewModeVisibleRPs(editor.getSongModel(), r);
                for (int j = 0; j < uniqueRps.size(); j++)
                {
                    data[i][j] = getRpFromClass(compactViewRps, uniqueRps.get(j).getClass()) != null;
                }
            }
        }

        @Override
        public int getRowCount()
        {
            return rowCount;
        }

        @Override
        public int getColumnCount()
        {
            return colCount;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if (columnIndex == COL_RP)
            {
                return this.uniqueRps.get(rowIndex).getDisplayName();
            } else
            {
                return data[columnIndex - COL_FIRST_RHYTHM][rowIndex];
            }
        }

        @Override
        public String getColumnName(int colIndex)
        {
            String s;
            if (colIndex == COL_RP)
            {
                s = ResUtil.getString(getClass(), "PARAMETER");
            } else
            {
                s = rhythms.get(colIndex - COL_FIRST_RHYTHM).getName();
            }
            return s;
        }


        @Override
        public boolean isCellEditable(int row, int col)
        {
            RhythmParameter<?> rp = uniqueRps.get(row);
            return col >= COL_FIRST_RHYTHM && getRpFromClass(rhythms.get(col - COL_FIRST_RHYTHM).getRhythmParameters(), rp.getClass()) != null;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            return col >= COL_FIRST_RHYTHM ? Boolean.class : String.class;
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            data[col - COL_FIRST_RHYTHM][row] = (Boolean) value;
        }

    }

    private class MyCellRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            Component c;
            if (tblModel.isCellEditable(row, col))
            {
                // Use the default renderer for Boolean which is JCheckBox based
                c = table.getDefaultRenderer(table.getColumnClass(col)).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            } else
            {
                // Use the standard default renderer which is a JLabel for String
                c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (c instanceof JLabel)
                {
                    ((JLabel) c).setText(null);
                }
            }
            return c;
        }
    }

}
