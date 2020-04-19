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
package org.jjazz.ui.ss_editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.util.SmallMap;
import org.openide.windows.WindowManager;

public class ShowHideRpsDialog extends javax.swing.JDialog implements ItemListener
{

    private static ShowHideRpsDialog INSTANCE;
    private boolean exitOk;
    private String lastSelectedRhythmName;
    private SmallMap<Rhythm, List<RhythmParameter<?>>> map;

    public static ShowHideRpsDialog getInstance()
    {
        synchronized (ShowHideRpsDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ShowHideRpsDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    private ShowHideRpsDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
    }

    /**
     * Used to initialize the dialog.
     * <p>
     * Changes in the dialog are reflected on this model.
     *
     * @param m
     */
    public void setModel(SmallMap<Rhythm, List<RhythmParameter<?>>> m)
    {
        map = m;
        cbx_Rhythms.removeItemListener(this); // Don't want to trigger events while modifying the JComboBox
        cbx_Rhythms.removeAllItems();
        for (Rhythm r : map.getKeys())
        {
            cbx_Rhythms.addItem(r.getName());
        }
        if (lastSelectedRhythmName != null)
        {
            cbx_Rhythms.setSelectedItem(lastSelectedRhythmName);
        }
        cbx_Rhythms.addItemListener(this);
        rhythmChanged();
    }

    private void rhythmChanged()
    {
        lastSelectedRhythmName = cbx_Rhythms.getItemAt(cbx_Rhythms.getSelectedIndex());
        Rhythm rhythm = null;
        for (Rhythm r : map.getKeys())
        {
            if (r.getName().equals(lastSelectedRhythmName))
            {
                rhythm = r;
                break;
            }
        }
        assert rhythm != null;
        final List<RhythmParameter<?>> visibleRps = map.getValue(rhythm);
        pnl_Rps.removeAll();
        for (final RhythmParameter<?> rp : rhythm.getRhythmParameters())
        {
            final JCheckBox cb = new JCheckBox();
            cb.setSelected(visibleRps.contains(rp));
            cb.setAction(new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (cb.isSelected())
                    {
                        visibleRps.add(rp);
                    } else
                    {
                        visibleRps.remove(rp);
                    }
                }
            });
            cb.setText(rp.getDisplayName());
            cb.setToolTipText(rp.getDescription());
            pnl_Rps.add(cb);
        }
        pnl_Rps.revalidate();
        pnl_Rps.repaint();
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
        setVisible(false);
    }

    public boolean isExitOk()
    {
        return exitOk;
    }

    @Override
    public void itemStateChanged(ItemEvent ie)
    {
        rhythmChanged();
    }

    /**
     * This method is called from within the constructor to setModel the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        cbx_Rhythms = new javax.swing.JComboBox<>();
        pnl_Rps = new javax.swing.JPanel();
        btn_Ok = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();

        setTitle(org.openide.util.NbBundle.getMessage(ShowHideRpsDialog.class, "ShowHideRpsDialog.title")); // NOI18N

        pnl_Rps.setLayout(new javax.swing.BoxLayout(pnl_Rps, javax.swing.BoxLayout.Y_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(ShowHideRpsDialog.class, "ShowHideRpsDialog.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(ShowHideRpsDialog.class, "ShowHideRpsDialog.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(cbx_Rhythms, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(pnl_Rps, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbx_Rhythms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_Rps, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Ok)
                    .addComponent(btn_Cancel))
                .addContainerGap())
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
    private javax.swing.JComboBox<String> cbx_Rhythms;
    private javax.swing.JPanel pnl_Rps;
    // End of variables declaration//GEN-END:variables

}
