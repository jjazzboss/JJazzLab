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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabaseimpl.api.FavoriteRhythms;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.jjazz.rhythmdatabaseimpl.api.FavoriteRhythmProvider;
import org.jjazz.rhythmselectiondialog.spi.RhythmPreviewer;
import org.jjazz.rhythmdatabaseimpl.api.AddRhythmsAction;
import org.jjazz.rhythmdatabaseimpl.api.DeleteRhythmFile;
import org.jjazz.rhythmselectiondialog.api.RhythmSelectionDialog;
import org.jjazz.rhythmselectiondialog.api.ui.RhythmJTable;
import org.jjazz.rhythmselectiondialog.api.ui.RhythmProviderJList;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.*;

public class RhythmSelectionDialogImpl extends RhythmSelectionDialog implements ListSelectionListener, ActionListener
{

    private TimeSignature timeSignature;
    private RhythmInfo presetRhythm;
    private RhythmProvider presetRhythmProvider;
    private RhythmProvider selectedRhythmProvider;
    private RhythmPreviewer rhythmPreviewProvider;
    private boolean previewDone;
    private boolean exitOk;
    private BooleanSupplier useRhythmTempoSettingSupplier;
    private final AddRhythmsAction addRhythmsAction = new AddRhythmsAction();
    private final DeleteRhythmFile deleteRhythmFileAction = new DeleteRhythmFile();
    private final HashMap<RhythmProvider, RhythmInfo> mapRpSelectedrythm = new HashMap<>();
    private final RhythmJTable rhythmTable = new RhythmJTable();


    private static final Logger LOGGER = Logger.getLogger(RhythmSelectionDialogImpl.class.getSimpleName());

    public RhythmSelectionDialogImpl()
    {
        initComponents();

        useRhythmTempoSettingSupplier = () -> true;

        // Listen to ComboBox changes
        cmb_variation.addActionListener(this);


        // Update UI
        UIUtilities.installSelectAllWhenFocused(tf_filter);
        fbtn_autoPreviewMode.addActionListener(e -> toggleRhythmPreview());
        rhythmTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "PreviewRhythm");
        rhythmTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "ToggleFavorite");
        rhythmTable.getActionMap().put("PreviewRhythm", new PreviewRhythmAction());
        rhythmTable.getActionMap().put("ToggleFavorite", new ToggleFavoriteAction());


        // Register for rhythmdatabase changes
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        rdb.addChangeListener(ce -> UIUtilities.invokeLaterIfNeeded(() -> rhythmDatabaseChanged()));


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
    public void preset(RhythmInfo ri, RhythmPreviewer rpp, BooleanSupplier useRhythmTempoSettingSupplier)
    {
        Objects.requireNonNull(ri);
        Objects.requireNonNull(useRhythmTempoSettingSupplier);

        LOGGER.log(Level.FINE, "preset() -- ri={0}", ri);
        exitOk = false;
        previewDone = false;
        this.useRhythmTempoSettingSupplier = useRhythmTempoSettingSupplier;

        cleanup();


        presetRhythm = ri;
        timeSignature = ri.timeSignature();
        rhythmPreviewProvider = rpp;
        fbtn_autoPreviewMode.setSelected(false);
        fbtn_autoPreviewMode.setEnabled(rhythmPreviewProvider != null);
        ((RhythmProviderJList) list_RhythmProviders).setTimeSignatureFilter(timeSignature);
        lbl_timeSignature.setText(timeSignature.toString());


        // Select the preset rhythm provider (this will populate the rhythm table)
        presetRhythmProvider = getProviderFavoriteFirst(presetRhythm);
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


        tf_userRhythmDir.setText(RhythmDirsLocator.getDefault().getUserRhythmsDirectory().getAbsolutePath());
        rhythmTable.requestFocusInWindow();
    }

    @Override
    public RhythmInfo getSelectedRhythm()
    {
        RhythmInfo ri = mapRpSelectedrythm.get(selectedRhythmProvider);
        return (ri == null) ? presetRhythm : ri;
    }

    @Override
    public String getLastSelectedVariation()
    {
        String res = previewDone ? (String) cmb_variation.getSelectedItem() : null;
        return res;
    }

    @Override
    public void setTitleText(String title)
    {
        lbl_Title.setText(title);
    }

    @Override
    public void setCustomComponent(JComponent comp)
    {
        Objects.requireNonNull(comp);
        pnl_customComponent.removeAll();
        pnl_customComponent.add(comp);
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
    // ActionListener interfacce
    // ===================================================================================

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == cmb_variation)
        {
            // Used has changed the variation using the JComboBox
            assert rhythmPreviewProvider != null;
            Rhythm r = rhythmPreviewProvider.getPreviewedRhythm();
            if (r == null)
            {
                // Should not happen since cmb_variation should be disabled when not previewing...
                return;
            }
            RhythmInfo ri = RhythmDatabase.getDefault().getRhythm(r.getUniqueId());
            previewRhythm(ri);

            // previewRhythm will first stop => endAction=>previewComplete() is executed and cmb_variation is disabled
            // so we lose focus, need to regain it, user may want to change other variation using the keyboard
            cmb_variation.requestFocusInWindow();
        }
    }

    // ===================================================================================
    // ListSelectionListener interfacce
    // ===================================================================================
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting())
        {
            return;
        }

        LOGGER.log(Level.FINE, "valueChanged() e.getFirstIndex()={0} e.getLastIndex()={1}", new Object[]
        {
            e.getFirstIndex(), e.getLastIndex()
        });

        if (e.getSource() == this.list_RhythmProviders)
        {
            RhythmProvider rp = list_RhythmProviders.getSelectedValue();

            if (rp != null && selectedRhythmProvider != rp)
            {
                if (rhythmPreviewProvider != null && previewDone)
                {
                    rhythmPreviewProvider.stop();
                }
                selectedRhythmProvider = rp;
                updateRhythmTable(selectedRhythmProvider);
            }

        } else if (e.getSource() == this.rhythmTable.getSelectionModel())
        {
            RhythmInfo ri = rhythmTable.getSelectedRhythm();                 // ri may be null
            mapRpSelectedrythm.put(selectedRhythmProvider, ri);

            LOGGER.log(Level.FINE, "valueChanged() selected rhythm ri={0}", ri);

            // Manage rhythm preview
            if (rhythmPreviewProvider != null)
            {
                Rhythm pr = rhythmPreviewProvider.getPreviewedRhythm();
                LOGGER.log(Level.FINE, "valueChanged() pr={0}", pr);
                if (pr != null)
                {
                    // RhythmPreview is ON
                    RhythmInfo pri = RhythmDatabase.getDefault().getRhythm(pr.getUniqueId());
                    LOGGER.log(Level.FINE, "valueChanged() pri={0}", pri);
                    if (fbtn_autoPreviewMode.isSelected())
                    {
                        // Change previewed rhythm only if auto preview button is ON and 
                        if (ri != null && ri != pri)
                        {
                            previewRhythm(ri);
                        }
                    } else
                    {
                        // It's a one time preview
                        // Other rhythms may have different variations that currently played rhythm, so disable for other rhythms
                        cmb_variation.setEnabled(ri == pri);
                    }
                } else if (fbtn_autoPreviewMode.isEnabled() && fbtn_autoPreviewMode.isSelected())
                {
                    // RhythmPreview is OFF, but auto preview is ON. This typically happens after a new RhythmProvider is selected
                    if (ri != null)
                    {
                        // Preview rhythm if not
                        previewRhythm(ri);
                    } else
                    {
                        cmb_variation.setEnabled(false);
                    }
                } else
                {
                    // Rhythm preview is OFF and autoPreview is OFF. 
                    // Just update enabled button status              
                    fbtn_autoPreviewMode.setEnabled(ri != null);
                    cmb_variation.setEnabled(false);
                }
            } else
            {
                // No RhythmPreview capability
                cmb_variation.setEnabled(false);
            }
        }

        var ri = mapRpSelectedrythm.get(selectedRhythmProvider);
        btn_deleteRhythm.setEnabled(ri != null && !ri.file().getName().equals(""));
        btn_openFolder.setEnabled(ri != null && !ri.file().getName().equals(""));
        btn_rpSettings.setEnabled(selectedRhythmProvider != null && selectedRhythmProvider.hasUserSettings());

    }

    // ===================================================================================
    // Private mehods
    // ===================================================================================
    /**
     *
     * Update the UI
     */
    private void rhythmDatabaseChanged()
    {
        LOGGER.fine("rhythmDatabaseChanged() --");

        // Save selection
        RhythmProvider saveSelRp = selectedRhythmProvider;

        List<RhythmProvider> rps = buildRhythmProviderList();
        list_RhythmProviders.setListData(rps.toArray(RhythmProvider[]::new));

        // Restore selection when possible
        selectedRhythmProvider = null;        // To make sure table is rebuilt
        RhythmProvider selRp = saveSelRp != null ? saveSelRp : presetRhythmProvider;
        list_RhythmProviders.setSelectedValue(selRp, true);  // This will update the rhythm table and restore selection if possible
    }

    private List<RhythmProvider> buildRhythmProviderList()
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        List<RhythmProvider> rps = new ArrayList<>();
        rps.addAll(rdb.getRhythmProviders());
        rps.sort((rp1, rp2) -> rp1.getInfo().getName().compareTo(rp2.getInfo().getName()));     // alphabetical sort
        // Put StubRhythmProviders at the end
        var stubRps = rps.stream()
                .filter(rp -> rp instanceof StubRhythmProvider)
                .toList();
        rps.removeAll(stubRps);
        rps.addAll(stubRps);
        rps.add(0, FavoriteRhythmProvider.getInstance());          // Always first
        return rps;
    }

    /**
     * Reset the rhythm table with rp's rhythms.
     * <p>
     * The method disables listening to rhythmTable during operation. So valueChanged() is not called during this method.
     *
     * @param rp
     */
    private void updateRhythmTable(RhythmProvider rp)
    {
        LOGGER.log(Level.FINE, "updateRhythmTable() -- rp={0}", rp.getInfo().getName());


        // We don't want to react on table change events here, this would mess up our data
        rhythmTable.getSelectionModel().removeListSelectionListener(this);


        // Reset the filter
        btn_clearFilterActionPerformed(null);


        // Refresh the list of rhythms
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        var frp = FavoriteRhythmProvider.getInstance();
        List<RhythmInfo> rhythms = (rp == frp) ? frp.getBuiltinRhythmInfos() : rdb.getRhythms(rp);
        rhythms = rhythms
                .stream()
                .filter(r -> r.timeSignature().equals(timeSignature))
                .toList();


        // Update the table
        rhythmTable.getModel().setRhythms(rhythms);


        // Restore table selection listening
        rhythmTable.getSelectionModel().addListSelectionListener(this);


        // Try to restore rhythm selection
        RhythmInfo ri = mapRpSelectedrythm.get(rp);
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
        if (rhythmPreviewProvider != null)
        {
            rhythmPreviewProvider.stop();
        }
        setVisible(false);
    }

    private void actionCancel()
    {
        mapRpSelectedrythm.put(selectedRhythmProvider, null);
        if (rhythmPreviewProvider != null)
        {
            rhythmPreviewProvider.stop();
        }
        setVisible(false);
    }

    private void handleTableMouseClicked(MouseEvent evt)
    {
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        boolean shift = (evt.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        LOGGER.log(Level.FINE, "handleTableMouseClicked()  left={0} ctrl={1} shift={2} clickCount={3}", new Object[]
        {
            SwingUtilities.isLeftMouseButton(evt),
            ctrl, shift, evt.getClickCount()
        });

        if (SwingUtilities.isLeftMouseButton(evt))
        {
            if (shift)
            {
                new ToggleFavoriteAction().actionPerformed(null);
            } else if (evt.getClickCount() == 2)
            {
                actionOK();
            }
        } else if (SwingUtilities.isRightMouseButton(evt))
        {
            // First select the clicked rhythm then preview 
            RhythmInfo ri = rhythmTable.getRhythm(evt.getPoint());
            if (ri == null)
            {
                return;
            }
            rhythmTable.setSelectedRhythm(ri);   // In preview mode this will trigger rhythm playback
            new PreviewRhythmAction().actionPerformed(null);
        }
    }

    private void toggleRhythmPreview()
    {
        RhythmInfo ri = rhythmTable.getSelectedRhythm();
        if (ri == null)
        {
            return;
        }
        if (fbtn_autoPreviewMode.isSelected())
        {
            previewRhythm(ri);
        } else
        {
            rhythmPreviewProvider.stop();
        }
    }

    /**
     * Preview the specified rhythm.
     *
     * @param ri
     * @return True if preview was successfully launched.
     */
    private boolean previewRhythm(RhythmInfo ri)
    {
        Objects.requireNonNull(ri);

        if (rhythmPreviewProvider == null)
        {
            return false;
        }

        previewDone = true;

        LOGGER.log(Level.FINE, "previewRhythm() ri={0}", ri);


        // Get the Rhythm instance
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Rhythm r;
        try
        {
            r = rdb.getRhythmInstance(ri);
        } catch (UnavailableRhythmException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }


        // Update cmb_variation
        String rpStdVariationValue = updateCmbVariation(r);


        // Prepare the RpValue map
        Map<RhythmParameter<?>, Object> mapRpValues = new HashMap<>();      // No specific value by default
        if (rpStdVariationValue != null)
        {
            mapRpValues.put(RP_SYS_Variation.getVariationRp(r), rpStdVariationValue);
        }


        // Preview
        try
        {
            LOGGER.fine("previewRhythm() calling rhythmPreviewProvider().previewRhythm()");
            rhythmPreviewProvider.previewRhythm(r, mapRpValues, useRhythmTempoSettingSupplier.getAsBoolean(), fbtn_autoPreviewMode.isSelected(), e
                    -> rhythmPreviewComplete(r));
            // previewRhythm will first stop => endAction => previewComplete() => cmb_variation is disabled/lose focus + highlight is removed
            // So need to restore state
            cmb_variation.setEnabled(true);
            rhythmTable.getModel().setHighlighted(rhythmTable.getSelectedRhythm(), true);
        } catch (MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }

        return true;
    }

    /**
     * Update cmbVariation to fit the specified rhythm.
     * <p>
     * Adjust to the rhythm RP_STD_Variation possible values. Select a default value, reuse previous one if possible. The changes done by this method do not
     * trigger an JComboBox.actionPerformed(). If r does not use RP_STD_Variation disable cmbVariation.
     *
     * @param r
     * @return Null if r does not use RP_STD_Variation, otherwise return the RP_STD_Variation value to use (we try to reuse the previous value)
     */
    private String updateCmbVariation(Rhythm r)
    {
        String newSelectedValue = null;
        RP_SYS_Variation rpVariation = RP_SYS_Variation.getVariationRp(r);

        if (rhythmPreviewProvider.getPreviewedRhythm() == r)
        {
            newSelectedValue = (String) cmb_variation.getSelectedItem();

        } else if (rpVariation != null)
        {
            cmb_variation.removeActionListener(this);

            String oldSelectedValue = (String) cmb_variation.getSelectedItem();
            var possibleValues = rpVariation.getPossibleValues();
            cmb_variation.setModel(new DefaultComboBoxModel<>(possibleValues.toArray(String[]::new)));
            newSelectedValue = oldSelectedValue; // Try to reuse previous value by default
            if (oldSelectedValue == null || !possibleValues.contains(oldSelectedValue))
            {
                // If new rhythm does not support the old value, use default
                newSelectedValue = rpVariation.getDefaultValue();
            }
            cmb_variation.setSelectedItem(newSelectedValue);

            cmb_variation.addActionListener(this);
        }

        cmb_variation.setEnabled(rpVariation != null);
        return newSelectedValue;
    }

    /**
     * Get the RhythmProvider of ri, or the Favorite RhythmProvider if it's a favorite rhythm.
     *
     * @param ri
     * @return Can be null
     */
    private RhythmProvider getProviderFavoriteFirst(RhythmInfo ri)
    {
        RhythmProvider rp;
        FavoriteRhythmProvider frp = FavoriteRhythmProvider.getInstance();
        if (frp.getBuiltinRhythmInfos().contains(ri))
        {
            rp = frp;
        } else
        {
            rp = RhythmDatabase.getDefault().getRhythmProvider(ri);
        }
        return rp;
    }

    private void rhythmPreviewComplete(Rhythm r)
    {
        rhythmTable.getModel().setHighlighted(RhythmDatabase.getDefault().getRhythm(r.getUniqueId()), false);
        cmb_variation.setEnabled(false);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jPanel1 = new javax.swing.JPanel();
        tf_filter = new javax.swing.JTextField();
        lbl_rhythmProviders = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_RhythmProviders = new RhythmProviderJList();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbl_rhythms = rhythmTable;
        lbl_rhythms = new javax.swing.JLabel();
        btn_Filter = new javax.swing.JButton();
        btn_clearFilter = new javax.swing.JButton();
        lbl_timeSignature = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        btn_openFolder = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        fbtn_autoPreviewMode = new org.jjazz.flatcomponents.api.FlatToggleButton();
        cmb_variation = new javax.swing.JComboBox<>();
        jScrollPane3 = new javax.swing.JScrollPane();
        helpTextArea1 = new org.jjazz.flatcomponents.api.HelpTextArea();
        btn_addRhythms = new javax.swing.JButton();
        btn_deleteRhythm = new javax.swing.JButton();
        btn_rpSettings = new javax.swing.JButton();
        lbl_Title = new javax.swing.JLabel();
        tf_userRhythmDir = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        pnl_customComponent = new javax.swing.JPanel();
        btn_Cancel = new javax.swing.JButton();
        btn_Ok = new javax.swing.JButton();

        setTitle(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.title")); // NOI18N
        setModal(true);

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

        tbl_rhythms.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
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

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timeSignature, "3/4"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_openFolder, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_openFolder.text")); // NOI18N
        btn_openFolder.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_openFolder.toolTipText")); // NOI18N
        btn_openFolder.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_openFolderActionPerformed(evt);
            }
        });

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 0));

        fbtn_autoPreviewMode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ss_editorimpl/rhythmselectiondialog/resources/SpeakerOff-24x24.png"))); // NOI18N
        fbtn_autoPreviewMode.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.fbtn_autoPreviewMode.toolTipText")); // NOI18N
        fbtn_autoPreviewMode.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ss_editorimpl/rhythmselectiondialog/resources/SpeakerOnRed-24x24.png"))); // NOI18N
        jPanel2.add(fbtn_autoPreviewMode);

        cmb_variation.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cmb_variation.toolTipText")); // NOI18N
        cmb_variation.setEnabled(false);
        jPanel2.add(cmb_variation);

        jScrollPane3.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(2);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.helpTextArea1.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(btn_addRhythms, addRhythmsAction.getValue(Action.NAME).toString());
        btn_addRhythms.setToolTipText(addRhythmsAction.getValue(Action.SHORT_DESCRIPTION).toString());
        btn_addRhythms.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_addRhythmsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_deleteRhythm, deleteRhythmFileAction.getValue(Action.NAME).toString()
        );
        btn_deleteRhythm.setToolTipText(deleteRhythmFileAction.getValue(Action.SHORT_DESCRIPTION).toString());
        btn_deleteRhythm.setEnabled(false);
        btn_deleteRhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_deleteRhythmActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_rpSettings, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_rpSettings.text")); // NOI18N
        btn_rpSettings.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_rpSettings.toolTipText")); // NOI18N
        btn_rpSettings.setEnabled(false);
        btn_rpSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_rpSettingsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btn_openFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_addRhythms, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_deleteRhythm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_rpSettings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_addRhythms, btn_deleteRhythm, btn_openFolder});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_deleteRhythm)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_addRhythms)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_openFolder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_rpSettings)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_rhythmProviders, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lbl_timeSignature)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_rhythms)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_clearFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Filter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_filter, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8))
        );

        lbl_Title.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, "Select a 3/4 rhythm for rhythm part \"X\""); // NOI18N

        tf_userRhythmDir.setEditable(false);
        tf_userRhythmDir.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.tf_userRhythmDir.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.jLabel1.text")); // NOI18N

        pnl_customComponent.setLayout(new java.awt.BorderLayout());

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Ok, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_Ok.text")); // NOI18N
        btn_Ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_OkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(pnl_customComponent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(lbl_Title)
                        .addGap(99, 99, 99)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_userRhythmDir))))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_customComponent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_Cancel)
                        .addComponent(btn_Ok)))
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
        RowFilter<TableModel, Object> rf;
        try
        {
            rf = RowFilter.regexFilter("(?i)" + s);
        } catch (java.util.regex.PatternSyntaxException e)
        {
            LOGGER.log(Level.WARNING, "btn_FilterActionPerformed() invalid filter regex string e={0}", e.getMessage());
            return;
        }
        TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) rhythmTable.getRowSorter();
        sorter.setRowFilter(rf);
        btn_Filter.setEnabled(false);
        btn_clearFilter.setEnabled(true);
        tf_filter.setEnabled(false);
        s = lbl_rhythms.getText();
        String msg = ResUtil.getString(getClass(), "RhythmSelectionDialogImpl.filtered");
        lbl_rhythms.setText(s + "* (" + msg + ")");
    }//GEN-LAST:event_btn_FilterActionPerformed

    private void tf_filterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_filterActionPerformed
    {//GEN-HEADEREND:event_tf_filterActionPerformed
        btn_FilterActionPerformed(null);
    }//GEN-LAST:event_tf_filterActionPerformed

    private void btn_clearFilterActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearFilterActionPerformed
    {//GEN-HEADEREND:event_btn_clearFilterActionPerformed
        LOGGER.fine("btn_clearFilterActionPerformed() --");
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

    private void btn_addRhythmsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_addRhythmsActionPerformed
    {//GEN-HEADEREND:event_btn_addRhythmsActionPerformed
        addRhythmsAction.actionPerformed(null);
        var ri = addRhythmsAction.getLastRhythmAdded();
        if (ri != null)
        {
            var rp = RhythmDatabase.getDefault().getRhythmProvider(ri);
            // rhythmTable will be updated later on the EDT, so we also need a task on the EDT
            SwingUtilities.invokeLater(() -> 
            {
                // Set selection to the first new rhythm
                list_RhythmProviders.setSelectedValue(rp, true);
                rhythmTable.setSelectedRhythm(ri);
            });
        }
    }//GEN-LAST:event_btn_addRhythmsActionPerformed

    private void btn_deleteRhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_deleteRhythmActionPerformed
    {//GEN-HEADEREND:event_btn_deleteRhythmActionPerformed
        var ri = getSelectedRhythm();
        if (ri != null)
        {
            deleteRhythmFileAction.deleteRhythmFile(ri);
        }
    }//GEN-LAST:event_btn_deleteRhythmActionPerformed

    private void btn_openFolderActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_openFolderActionPerformed
    {//GEN-HEADEREND:event_btn_openFolderActionPerformed
        var ri = getSelectedRhythm();
        if (ri == null || ri.file().getName().equals(""))
        {
            return;
        }
        org.jjazz.utilities.api.Utilities.systemBrowseFileDirectory(ri.file(), false);

    }//GEN-LAST:event_btn_openFolderActionPerformed

    private void btn_rpSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_rpSettingsActionPerformed
    {//GEN-HEADEREND:event_btn_rpSettingsActionPerformed
        RhythmProvider rp = list_RhythmProviders.getSelectedValue();
        if (rp != null)
        {
            rp.showUserSettingsDialog();
        }
    }//GEN-LAST:event_btn_rpSettingsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Filter;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_addRhythms;
    private javax.swing.JButton btn_clearFilter;
    private javax.swing.JButton btn_deleteRhythm;
    private javax.swing.JButton btn_openFolder;
    private javax.swing.JButton btn_rpSettings;
    private javax.swing.JComboBox<String> cmb_variation;
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_autoPreviewMode;
    private org.jjazz.flatcomponents.api.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_Title;
    private javax.swing.JLabel lbl_rhythmProviders;
    private javax.swing.JLabel lbl_rhythms;
    private javax.swing.JLabel lbl_timeSignature;
    private javax.swing.JList<RhythmProvider> list_RhythmProviders;
    private javax.swing.JPanel pnl_customComponent;
    private javax.swing.JTable tbl_rhythms;
    private javax.swing.JTextField tf_filter;
    private javax.swing.JTextField tf_userRhythmDir;
    // End of variables declaration//GEN-END:variables

// ===================================================================================
// Private classes
// ===================================================================================
    private class PreviewRhythmAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            RhythmInfo ri = rhythmTable.getSelectedRhythm();
            if (rhythmPreviewProvider != null && ri != null && !fbtn_autoPreviewMode.isSelected())
            {
                if (rhythmPreviewProvider.getPreviewedRhythm() != null)
                {
                    rhythmPreviewProvider.stop();
                } else
                {
                    previewRhythm(ri);
                }
            }
        }
    }

    private class ToggleFavoriteAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            RhythmInfo ri = rhythmTable.getSelectedRhythm();
            LOGGER.log(Level.FINE, "actionPerformed() selectedRhythm={0}", ri);
            if (ri != null)
            {
                FavoriteRhythms fr = FavoriteRhythms.getInstance();
                if (selectedRhythmProvider == FavoriteRhythmProvider.getInstance())
                {
                    fr.removeRhythm(ri);
                    updateRhythmTable(selectedRhythmProvider);
                } else
                {
                    if (fr.contains(ri))
                    {
                        LOGGER.log(Level.FINE, "actionPerformed()    removing from favorites: {0}", ri);
                        fr.removeRhythm(ri);
                    } else
                    {
                        LOGGER.log(Level.FINE, "actionPerformed()    adding to favorites: {0}", ri);
                        fr.addRhythm(ri);
                    }
                }
            }
        }
    }

}
