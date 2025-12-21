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
package org.jjazz.ss_editorimpl.rhythmselectiondialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmselectiondialog.spi.RhythmPreviewer;
import org.jjazz.rhythmselectiondialog.api.RhythmSelectionDialog;

public class SimpleRhythmSelectionDialog extends RhythmSelectionDialog implements ListSelectionListener
{

    private RhythmInfo selectedRhythm;
    private TimeSignature timeSignature;
    private boolean exitOk;

    public SimpleRhythmSelectionDialog()
    {
        initComponents();
        list_Rhythms.setCellRenderer(new RhythmRenderer());
        list_Rhythms.addListSelectionListener(this);

        // Make first column fix
        tbl_props.getColumnModel().getColumn(0).setPreferredWidth(100);
        tbl_props.getColumnModel().getColumn(0).setMaxWidth(100);
    }

    @Override
    public void preset(RhythmInfo ri, RhythmPreviewer rpp, BooleanSupplier useRhythmTempoSettingSupplier)
    {
        Objects.requireNonNull(ri);
        exitOk = false;
        cleanup();

        selectedRhythm = ri;
        timeSignature = selectedRhythm.timeSignature();
        updateRhythmInfo(selectedRhythm);

        // Populate the list of rhythms for this TimeSignature
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        RhythmInfo[] rInfos = rdb.getRhythms(timeSignature).toArray(new RhythmInfo[0]);
        list_Rhythms.setListData(rInfos);
        list_Rhythms.setSelectedValue(selectedRhythm, true);
    }

    @Override
    public void setCustomComponent(JComponent comp)
    {
        // Do nothing
    }

    public String getLastSelectedVariation()
    {
        return null;
    }

    @Override
    public void cleanup()
    {
        selectedRhythm = null;
        timeSignature = null;
        list_Rhythms.setListData(new RhythmInfo[0]);
    }

    @Override
    public boolean isExitOk()
    {
        return exitOk;
    }

    private void updateRhythmInfo(RhythmInfo ri)
    {
        if (ri == null)
        {
            return;
        }
        StringBuilder sb = new StringBuilder();
        TableModel tm = this.tbl_props.getModel();
        TempoRange tr = ri.rhythmFeatures().tempoRange();
        tm.setValueAt("Description", 0, 0);
        tm.setValueAt(ri.description(), 0, 1);

        tm.setValueAt("Tags", 1, 0);
        for (String tag : ri.tags())
        {
            sb.append(tag).append(" ");
        }
        tm.setValueAt(sb.toString(), 1, 1);

        tm.setValueAt("Preferred tempo", 2, 0);
        tm.setValueAt(ri.preferredTempo(), 2, 1);

        tm.setValueAt("Preferred tempo", 3, 0);
        tm.setValueAt(tr.getName() + " [" + tr.getMin() + "~" + tr.getMax() + "]", 3, 1);

        tm.setValueAt("File", 4, 0);
        tm.setValueAt(ri.file().getAbsolutePath(), 4, 1);
//        {
//            sb.append(rv.getName()).append(" ");
//        }
//        tm.setValueAt(sb.toString(), 4, 1);
//
//        tm.setValueAt("Parameters", 5, 0);
//        sb.delete(0, sb.length());
//        for (RhythmParameter<?> rp : ri.getRhythmParameters())
//        {
//            sb.append(rp.getName()).append(" ");
//        }
//        tm.setValueAt(sb.toString(), 5, 1);

        tm.setValueAt("Version", 6, 0);
        tm.setValueAt(ri.version(), 6, 1);

        tm.setValueAt("Vendor", 7, 0);
        tm.setValueAt(ri.author(), 7, 1);

        tm.setValueAt("Feel", 8, 0);
        tm.setValueAt(ri.rhythmFeatures().division(), 8, 1);
    }


    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting())
        {
            return;
        }
        selectedRhythm = list_Rhythms.getSelectedValue();

        updateRhythmInfo(selectedRhythm);
    }

    @Override
    public RhythmInfo getSelectedRhythm()
    {
        return selectedRhythm;
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
        return contentPane;
    }

    private void actionOK()
    {
        exitOk = true;
        setVisible(false);
    }

    private void actionCancel()
    {
        exitOk = false;
        selectedRhythm = null;
        setVisible(false);
    }

    @Override
    public void setTitleText(String title)
    {
        lbl_Title.setText(title);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        lbl_Title = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_Rhythms = new javax.swing.JList<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbl_props = new javax.swing.JTable();
        btn_Ok = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        cb_applyRhythmToNextSpts = new javax.swing.JCheckBox();

        setTitle("Simple rhythm selection dialog"); // NOI18N
        setModal(true);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, "Select rhythm"); // NOI18N

        list_Rhythms.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_Rhythms.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                list_RhythmsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(list_Rhythms);

        tbl_props.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String []
            {
                "Title 1", "Title 2"
            }
        )
        {
            boolean[] canEdit = new boolean []
            {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        tbl_props.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane2.setViewportView(tbl_props);

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, "OK"); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, "Cancel"); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cb_applyRhythmToNextSpts, "Apply rhythm also to next song parts"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cb_applyRhythmToNextSpts))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lbl_Title, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_Cancel))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_Title)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cb_applyRhythmToNextSpts)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Cancel)
                    .addComponent(btn_Ok))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void list_RhythmsMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_list_RhythmsMouseClicked
    {//GEN-HEADEREND:event_list_RhythmsMouseClicked
        if (evt.getClickCount() == 2)
        {
            actionOK();
        }
    }//GEN-LAST:event_list_RhythmsMouseClicked

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        actionCancel();
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        actionOK();
    }//GEN-LAST:event_btn_OkActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JCheckBox cb_applyRhythmToNextSpts;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbl_Title;
    private javax.swing.JList<RhythmInfo> list_Rhythms;
    private javax.swing.JTable tbl_props;
    // End of variables declaration//GEN-END:variables

    private class RhythmRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Rhythm ri = (Rhythm) value;
            setText(ri.getName());
            return c;
        }
    }

}
