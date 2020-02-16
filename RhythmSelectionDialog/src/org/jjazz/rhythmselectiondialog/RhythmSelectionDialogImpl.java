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
package org.jjazz.rhythmselectiondialog;

import org.jjazz.rhythm.database.api.FavoriteRhythmProvider;
import org.jjazz.rhythmselectiondialog.ui.RhythmProviderList;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.FavoriteRhythms;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythmselectiondialog.ui.RhythmTable;
import org.jjazz.ui.ss_editor.spi.RhythmSelectionDialog;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

@Messages(
        {
            "CTL_Rhythms=Rhythms",
            "CTL_Parameters=Parameters",
            "CTL_Version=Version",
            "CTL_Vendor=Vendor"
        })
@ServiceProvider(service = RhythmSelectionDialog.class)
public class RhythmSelectionDialogImpl extends RhythmSelectionDialog implements ListSelectionListener
{

    private TimeSignature timeSignature;
    private Rhythm presetRhythm;
    private RhythmProvider presetRhythmProvider;
    private RhythmProvider selectedRhythmProvider;
    private boolean exitOk;
    private final HashMap<RhythmProvider, Rhythm> mapRpSelectedrythm = new HashMap<>();
    private RhythmTable rhythmTable = new RhythmTable();

    private static final Logger LOGGER = Logger.getLogger(RhythmSelectionDialogImpl.class.getSimpleName());

    public RhythmSelectionDialogImpl()
    {
        initComponents();

        // Register for rhythmdatabase changes
        RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
        rdb.addChangeListener(new ChangeListener()              // RhythmDatabase events are not sent on the EDT
        {
            @Override
            public void stateChanged(ChangeEvent ce)
            {
                Runnable run = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        rhythmDatabaseChanged();
                    }
                };
                SwingUtilities.invokeLater(run);
            }
        });

        // Prepare rhythm providers JList
        list_RhythmProviders.addListSelectionListener(this);

        // Prepare table
        rhythmTable.getSelectionModel().addListSelectionListener(this);
        rhythmTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleTableMouseClicked(e);
            }
        });

    }

    @Override
    public void cleanup()
    {
        presetRhythm = null;
        timeSignature = null;
        mapRpSelectedrythm.clear();
        selectedRhythmProvider = null;
        presetRhythmProvider = null;
    }

    @Override
    public void preset(Rhythm r)
    {
        if (r == null)
        {
            throw new IllegalArgumentException("r=" + r);
        }
        LOGGER.log(Level.FINE, "preset() -- r={0}", r);
        exitOk = false;

        cleanup();

        presetRhythm = r;
        timeSignature = r.getTimeSignature();
        ((RhythmProviderList) list_RhythmProviders).setTimeSignatureFilter(timeSignature);
        lbl_timeSignature.setText(timeSignature.toString());

        // Select the preset rhythm provider (this will populate the rhythm table)
        RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
        presetRhythmProvider = rdb.getRhythmProvider(presetRhythm);
        if (!rhythmProvidersListContains(presetRhythmProvider))
        {
            // It's the first time, need to populate the UI
            rhythmDatabaseChanged();
            assert rhythmProvidersListContains(presetRhythmProvider) : "presetRhythmProvider=" + presetRhythmProvider;
        }
        list_RhythmProviders.clearSelection();      // Make sure the rhythm table will be updated by next line
        list_RhythmProviders.setSelectedValue(presetRhythmProvider, true);  // This will update the rhythm table        

        // Select the preset rhythm and make it visible
        rhythmTable.setSelectedRhythm(presetRhythm);
        int row = rhythmTable.getSelectedRow();
        assert row != -1;
        rhythmTable.scrollRectToVisible(rhythmTable.getCellRect(row, 0, true));

        tf_userRhythmDir.setText(FileDirectoryManager.getInstance().getUserRhythmDirectory().getAbsolutePath());
        rhythmTable.requestFocusInWindow();
    }

    @Override
    public Rhythm getSelectedRhythm()
    {
        Rhythm r = mapRpSelectedrythm.get(selectedRhythmProvider);
        return (r == null) ? presetRhythm : r;
    }

    @Override
    public void setTitleLabel(String title)
    {
        lbl_Title.setText(title);
    }

    @Override
    public boolean isApplyRhythmToNextSongParts()
    {
        return cb_applyRhythmToNextSpts.isSelected();
    }

    @Override
    public boolean isUseRhythmTempo()
    {
        return cb_useRhythmTempo.isSelected();
    }

    @Override
    public boolean isExitOk()
    {
        return exitOk;
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

    // ===================================================================================
    // ListSelectionListener interfacce
    // ===================================================================================
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        LOGGER.log(Level.FINE, "valueChanged() e={0}", e);
        if (e.getValueIsAdjusting())
        {
            return;
        }
        if (e.getSource() == this.list_RhythmProviders)
        {
            RhythmProvider rp = list_RhythmProviders.getSelectedValue();
            if (rp != null && selectedRhythmProvider != rp)
            {
                selectedRhythmProvider = rp;
                updateRhythmTable(selectedRhythmProvider);
            }
        } else if (e.getSource() == this.rhythmTable.getSelectionModel())
        {
            Rhythm ri = rhythmTable.getSelectedRhythm();                 // ri may be null
            mapRpSelectedrythm.put(selectedRhythmProvider, ri);
        }
    }

    // ===================================================================================
    // Private mehods
    // ===================================================================================
    /**
     * Update the UI
     */
    private void rhythmDatabaseChanged()
    {
        LOGGER.fine("rhythmDatabaseChanged()");
        RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
        List<RhythmProvider> rps = new ArrayList<>();
        rps.add(FavoriteRhythmProvider.getInstance());          // Always first
        rps.addAll(rdb.getRhythmProviders());
        list_RhythmProviders.setListData(rps.toArray(new RhythmProvider[0]));

        if (presetRhythmProvider != null)
        {
            list_RhythmProviders.setSelectedValue(presetRhythmProvider, true);  // This will update the rhythm table 
        }
        if (presetRhythm != null)
        {
            rhythmTable.setSelectedRhythm(presetRhythm);
            int row = rhythmTable.getSelectedRow();
            if (row != -1)
            {
                rhythmTable.scrollRectToVisible(rhythmTable.getCellRect(row, 0, true));
            }
        }
    }

    /**
     * Reset the rhythm table with rp's rhythms.
     * <p>
     * The method disables listening to rhythmTable during operation. So valueChanged() is not called during this method.
     *
     * @param rp
     * @param sri
     */
    private void updateRhythmTable(RhythmProvider rp)
    {
        LOGGER.fine("updateRhythmTable() -- rp=" + rp.getInfo().getName());
        // We don't want to react on table change events here, this would mess up our data
        rhythmTable.getSelectionModel().removeListSelectionListener(this);

        // Reset the filter
        btn_clearFilterActionPerformed(null);

        // Refresh the list of rhythms
        RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
        List<Rhythm> rhythms = (rp == FavoriteRhythmProvider.getInstance()) ? FavoriteRhythmProvider.getInstance().getBuiltinRhythms() : rdb.getRhythms(rp);
        rhythms = rdb.getRhythms(timeSignature, rhythms);

        // Update the table
        rhythmTable.getModel().setRhythms(rhythms);

        // Restore table selection listening
        rhythmTable.getSelectionModel().addListSelectionListener(this);

        // Try to restore rhythm selection
        Rhythm ri = mapRpSelectedrythm.get(rp);
        if (ri != null)
        {
            rhythmTable.setSelectedRhythm(ri);
        }
    }

    private boolean rhythmProvidersListContains(RhythmProvider presetRhythmProvider)
    {
        boolean b = false;
        for (int i = 0; i < list_RhythmProviders.getModel().getSize(); i++)
        {
            if (list_RhythmProviders.getModel().getElementAt(i) == presetRhythmProvider)
            {
                b = true;
                break;
            }
        }
        return b;
    }

    private void actionOK()
    {
        exitOk = true;
        setVisible(false);
    }

    private void actionCancel()
    {
        mapRpSelectedrythm.put(selectedRhythmProvider, null);
        setVisible(false);
    }

    private void handleTableMouseClicked(MouseEvent evt)
    {
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if (SwingUtilities.isLeftMouseButton(evt))
        {
            if (evt.getClickCount() == 1 && !ctrl && shift)
            {
                toggleFavoriteRhythm();
            } else if (evt.getClickCount() == 2 && !ctrl && !shift)
            {
                actionOK();
            }
        }
    }

    private void toggleFavoriteRhythm()
    {
        Rhythm r = rhythmTable.getSelectedRhythm();
        LOGGER.fine("toggleFavoriteRhythm() selectedRhythm=" + r);
        if (r != null)
        {
            FavoriteRhythms fr = FavoriteRhythms.getInstance();
            if (selectedRhythmProvider == FavoriteRhythmProvider.getInstance())
            {
                fr.removeRhythm(r);
                updateRhythmTable(selectedRhythmProvider);
            } else
            {
                if (fr.contains(r))
                {
                    fr.removeRhythm(r);
                } else
                {
                    fr.addRhythm(r);
                }
            }
        }
    }

    // ===================================================================================
    // Private classes
    // ===================================================================================
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_Ok = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        tf_filter = new javax.swing.JTextField();
        lbl_rhythmProviders = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_RhythmProviders = new RhythmProviderList();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbl_rhythms = rhythmTable;
        lbl_rhythms = new javax.swing.JLabel();
        btn_Filter = new javax.swing.JButton();
        btn_clearFilter = new javax.swing.JButton();
        lbl_timeSignature = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.ui.utilities.HelpTextArea();
        lbl_Title = new javax.swing.JLabel();
        cb_applyRhythmToNextSpts = new javax.swing.JCheckBox();
        cb_useRhythmTempo = new javax.swing.JCheckBox();
        tf_userRhythmDir = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        setTitle(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.title")); // NOI18N
        setModal(true);

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        tf_filter.setText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.tf_filter.text")); // NOI18N
        tf_filter.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.tf_filter.toolTipText")); // NOI18N
        tf_filter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_filterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythmProviders, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.lbl_rhythmProviders.text")); // NOI18N

        list_RhythmProviders.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(list_RhythmProviders);

        jScrollPane2.setAutoscrolls(true);
        jScrollPane2.setViewportView(tbl_rhythms);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_rhythms, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.lbl_rhythms.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Filter, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_Filter.text")); // NOI18N
        btn_Filter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_FilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_clearFilter, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_clearFilter.text")); // NOI18N
        btn_clearFilter.setEnabled(false);
        btn_clearFilter.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearFilterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timeSignature, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.lbl_timeSignature.text")); // NOI18N

        jScrollPane3.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(2);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.helpTextArea1.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lbl_rhythmProviders, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 690, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(lbl_timeSignature)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_rhythms)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_clearFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Filter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_filter, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jScrollPane3)))
                .addGap(0, 0, 0))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_rhythmProviders)
                    .addComponent(lbl_rhythms)
                    .addComponent(tf_filter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_Filter)
                    .addComponent(btn_clearFilter)
                    .addComponent(lbl_timeSignature))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        lbl_Title.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.lbl_Title.text")); // NOI18N

        cb_applyRhythmToNextSpts.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cb_applyRhythmToNextSpts, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_applyRhythmToNextSpts.text")); // NOI18N
        cb_applyRhythmToNextSpts.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_applyRhythmToNextSpts.toolTipText")); // NOI18N
        cb_applyRhythmToNextSpts.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        cb_useRhythmTempo.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cb_useRhythmTempo, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_useRhythmTempo.text")); // NOI18N
        cb_useRhythmTempo.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_useRhythmTempo.toolTipText")); // NOI18N

        tf_userRhythmDir.setEditable(false);
        tf_userRhythmDir.setText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.tf_userRhythmDir.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cb_useRhythmTempo)
                            .addComponent(cb_applyRhythmToNextSpts))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Cancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(lbl_Title)
                        .addGap(99, 99, 99)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_userRhythmDir)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_Title)
                    .addComponent(jLabel1)
                    .addComponent(tf_userRhythmDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(9, 9, 9)
                .addComponent(cb_applyRhythmToNextSpts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cb_useRhythmTempo)
                    .addComponent(btn_Cancel)
                    .addComponent(btn_Ok))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        actionCancel();
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_OkActionPerformed
    {//GEN-HEADEREND:event_btn_OkActionPerformed
        actionOK();
    }//GEN-LAST:event_btn_OkActionPerformed

    private void btn_FilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_FilterActionPerformed
    {//GEN-HEADEREND:event_btn_FilterActionPerformed
        String s = tf_filter.getText().trim();
        if (s.isEmpty())
        {
            return;
        }
        RowFilter<TableModel, Object> rf = null;
        try
        {
            rf = RowFilter.regexFilter("(?i)" + s);
        } catch (java.util.regex.PatternSyntaxException e)
        {
            LOGGER.warning("btn_FilterActionPerformed() invalid filter regex string e=" + e.getLocalizedMessage());
            return;
        }
        TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) rhythmTable.getRowSorter();
        sorter.setRowFilter(rf);
        btn_Filter.setEnabled(false);
        btn_clearFilter.setEnabled(true);
        tf_filter.setEnabled(false);
        s = lbl_rhythms.getText();
        lbl_rhythms.setText(s + "* (FILTERED)");
    }//GEN-LAST:event_btn_FilterActionPerformed

    private void tf_filterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_filterActionPerformed
    {//GEN-HEADEREND:event_tf_filterActionPerformed
        btn_FilterActionPerformed(null);
    }//GEN-LAST:event_tf_filterActionPerformed

    private void btn_clearFilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearFilterActionPerformed
    {//GEN-HEADEREND:event_btn_clearFilterActionPerformed
        TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) rhythmTable.getRowSorter();
        sorter.setRowFilter(null);
        btn_Filter.setEnabled(true);
        btn_clearFilter.setEnabled(false);
        tf_filter.setEnabled(true);
        String s = lbl_rhythms.getText();
        int i = s.indexOf("*");
        if (i != -1)
        {
            lbl_rhythms.setText(s.substring(0, i));
        }
    }//GEN-LAST:event_btn_clearFilterActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Filter;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_clearFilter;
    private javax.swing.JCheckBox cb_applyRhythmToNextSpts;
    private javax.swing.JCheckBox cb_useRhythmTempo;
    private org.jjazz.ui.utilities.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_Title;
    private javax.swing.JLabel lbl_rhythmProviders;
    private javax.swing.JLabel lbl_rhythms;
    private javax.swing.JLabel lbl_timeSignature;
    private javax.swing.JList<RhythmProvider> list_RhythmProviders;
    private javax.swing.JTable tbl_rhythms;
    private javax.swing.JTextField tf_filter;
    private javax.swing.JTextField tf_userRhythmDir;
    // End of variables declaration//GEN-END:variables

}
