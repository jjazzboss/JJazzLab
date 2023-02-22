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

import org.jjazz.rhythmselectiondialog.api.RhythmProviderList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.FavoriteRhythms;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythmselectiondialog.api.RhythmTable;
import org.jjazz.ui.ss_editor.spi.RhythmSelectionDialog;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.MultipleErrorsReport;
import org.jjazz.util.api.MultipleErrorsReportDialog;
import org.jjazz.util.api.ResUtil;
import org.openide.*;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

@ServiceProvider(service = RhythmSelectionDialog.class)
public class RhythmSelectionDialogImpl extends RhythmSelectionDialog implements ListSelectionListener, ActionListener
{

    private static final String PREF_HIDE_ADD_RHYTHM_INFO_DIALOG = "HideAddRhythmInfoDialog";
    private TimeSignature timeSignature;
    private RhythmInfo presetRhythm;
    private RhythmProvider presetRhythmProvider;
    private RhythmProvider selectedRhythmProvider;
    private RhythmPreviewProvider rhythmPreviewProvider;
    private boolean previewDone;
    private boolean exitOk;
    private File lastRhythmDir = null;
    private final HashMap<RhythmProvider, RhythmInfo> mapRpSelectedrythm = new HashMap<>();
    private final RhythmTable rhythmTable = new RhythmTable();
    private static final Preferences prefs = NbPreferences.forModule(RhythmSelectionDialogImpl.class);

    private static final Logger LOGGER = Logger.getLogger(RhythmSelectionDialogImpl.class.getSimpleName());

    public RhythmSelectionDialogImpl()
    {
        initComponents();

        // Listen to ComboBox changes
        cmb_variation.addActionListener(this);


        // Update UI
        Utilities.installSelectAllWhenFocused(tf_filter);
        fbtn_autoPreviewMode.addActionListener(e -> toggleRhythmPreview());
        rhythmTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "PreviewRhythm");   
        rhythmTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "ToggleFavorite");   
        rhythmTable.getActionMap().put("PreviewRhythm", new PreviewRhythmAction());
        rhythmTable.getActionMap().put("ToggleFavorite", new ToggleFavoriteAction());


        // Register for rhythmdatabase changes
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        rdb.addChangeListener(new ChangeListener()              // RhythmDatabase events might not be sent on the EDT
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
                Utilities.invokeLaterIfNeeded(run);
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
    public void preset(RhythmInfo ri, RhythmPreviewProvider rpp)
    {
        if (ri == null)
        {
            throw new IllegalArgumentException("ri=" + ri);   
        }
        LOGGER.log(Level.FINE, "preset() -- ri={0}", ri);   
        exitOk = false;
        previewDone = false;


        cleanup();


        presetRhythm = ri;
        timeSignature = ri.getTimeSignature();
        rhythmPreviewProvider = rpp;
        fbtn_autoPreviewMode.setSelected(false);
        fbtn_autoPreviewMode.setEnabled(rhythmPreviewProvider != null);
        ((RhythmProviderList) list_RhythmProviders).setTimeSignatureFilter(timeSignature);
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


        tf_userRhythmDir.setText(FileDirectoryManager.getInstance().getUserRhythmDirectory().getAbsolutePath());
        rhythmTable.requestFocusInWindow();
    }

    @Override
    public RhythmInfo getSelectedRhythm()
    {
        RhythmInfo ri = mapRpSelectedrythm.get(selectedRhythmProvider);
        return (ri == null) ? presetRhythm : ri;
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

        LOGGER.log(Level.FINE, "valueChanged() e.getFirstIndex()=" + e.getFirstIndex() + " e.getLastIndex()=" + e.getLastIndex());   

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

            LOGGER.fine("valueChanged() selected rhythm ri=" + ri);   

            btn_deleteRhythm.setEnabled(ri != null && !ri.getFile().getName().equals(""));
            btn_openFolder.setEnabled(ri != null && !ri.getFile().getName().equals(""));

            // Manage rhythm preview
            if (rhythmPreviewProvider != null)
            {
                Rhythm pr = rhythmPreviewProvider.getPreviewedRhythm();
                LOGGER.fine("valueChanged() pr=" + pr);   
                if (pr != null)
                {
                    // RhythmPreview is ON
                    RhythmInfo pri = RhythmDatabase.getDefault().getRhythm(pr.getUniqueId());
                    LOGGER.fine("valueChanged() pri=" + pri);   
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

        // Rebuild the RhythmProvider list
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        List<RhythmProvider> rps = new ArrayList<>();
        rps.add(FavoriteRhythmProvider.getInstance());          // Always first
        rps.addAll(rdb.getRhythmProviders());
        list_RhythmProviders.setListData(rps.toArray(new RhythmProvider[0]));

        // Restore selection when possible
        selectedRhythmProvider = null;        // To make sure table is rebuilt
        RhythmProvider selRp = saveSelRp != null ? saveSelRp : presetRhythmProvider;
        list_RhythmProviders.setSelectedValue(selRp, true);  // This will update the rhythm table and restore selection if possible
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
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        var frp = FavoriteRhythmProvider.getInstance();
        List<RhythmInfo> rhythms = (rp == frp) ? frp.getBuiltinRhythmInfos() : rdb.getRhythms(rp);
        rhythms = rhythms
                .stream()
                .filter(r -> r.getTimeSignature().equals(timeSignature))
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

    private void addRhythms()
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();

        // Show notification first time
        if (!prefs.getBoolean(PREF_HIDE_ADD_RHYTHM_INFO_DIALOG, false))
        {
            AddRhythmInfoDialog dlg = new AddRhythmInfoDialog(WindowManager.getDefault().getMainWindow(), true);
            dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            dlg.setVisible(true);
            prefs.putBoolean(PREF_HIDE_ADD_RHYTHM_INFO_DIALOG, dlg.isDoNotShowAnymmore());
        }


        // Prepare FileChooser
        JFileChooser chooser = Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(lastRhythmDir);
        chooser.setDialogTitle(ResUtil.getString(RhythmSelectionDialogImpl.class, "CTL_AddRhythms"));
        // Prepare the FileNameFilter
        StringBuilder sb = new StringBuilder();
        List<String> allExts = new ArrayList<>();
        List<RhythmProvider> rps = rdb.getRhythmProviders();
        for (RhythmProvider rp : rps)
        {
            for (String ext : rp.getSupportedFileExtensions())
            {
                allExts.add(ext);
                if (sb.length() != 0)
                {
                    sb.append(",");
                }
                sb.append(".").append(ext);
            }
        }
        if (allExts.isEmpty())
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ResUtil.getString(getClass(), "ERR_NoRhythmProviderFound."), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter(
                        ResUtil.getString(getClass(), "RhythmFiles", sb.toString()),
                        allExts.toArray(new String[0])));


        // Show filechooser
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
        {
            // User cancelled
            return;
        }


        List<String> rhythmFiles = Arrays.asList(chooser.getSelectedFiles()).stream().limit(10).map(f -> f.getName()).collect(Collectors.toList());
        Analytics.logEvent("Add Rhythms", Analytics.buildMap("Files", rhythmFiles));


        // Process files
        MultipleErrorsReport errRpt = new MultipleErrorsReport();
        final List<RhythmDatabase.RpRhythmPair> pairs = new ArrayList<>();
        HashSet<TimeSignature> timeSigs = new HashSet<>();
        for (File f : chooser.getSelectedFiles())
        {
            lastRhythmDir = f.getParentFile();
            String ext = org.jjazz.util.api.Utilities.getExtension(f.getName()).toLowerCase();
            Rhythm r = null;
            for (RhythmProvider rp : rps)
            {
                if (Arrays.asList(rp.getSupportedFileExtensions()).contains(ext))
                {
                    try
                    {
                        r = rp.readFast(f);
                    } catch (IOException ex)
                    {
                        LOGGER.warning("btn_addRhythmsActionPerformed() ex=" + ex);   
                        errRpt.individualErrorMessages.add(ex.getLocalizedMessage());
                        continue;
                    }
                    pairs.add(new RhythmDatabase.RpRhythmPair(rp, r));
                    timeSigs.add(r.getTimeSignature());
                }
            }
        }


        // Notify end-user of errors
        if (!errRpt.individualErrorMessages.isEmpty())
        {
            errRpt.primaryErrorMessage = ResUtil.getString(getClass(), "ERR_RhythmFilesCouldNotBeRead", errRpt.individualErrorMessages.size());
            errRpt.secondaryErrorMessage = "";
            MultipleErrorsReportDialog dlg = new MultipleErrorsReportDialog(WindowManager.getDefault().getMainWindow(), ResUtil.getString(getClass(), "CTL_RhythmCreationErrors"), errRpt);
            dlg.setVisible(true);
        }


        if (!pairs.isEmpty())
        {
            // Add to the rhythmdatabase
            int nbActuallyAdded = rdb.addExtraRhythms(pairs);  // This will update the rhythmTable on a task put on the EDT
            int nbAlreadyAdded = pairs.size() - nbActuallyAdded;


            // Notify user 
            String msg = ResUtil.getString(getClass(), "ProcessedFiles", pairs.size(), timeSigs);
            msg += ResUtil.getString(getClass(), "NewRhythmsAdded", nbActuallyAdded);
            msg += ResUtil.getString(getClass(), "PreExistingRhythmsSkipped", nbAlreadyAdded);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);


            // rhythmTable will be updated later on the EDT, so we also need a task on the EDT
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    // Set selection to the first new rhythm
                    list_RhythmProviders.setSelectedValue(pairs.get(0).rp, true);
                    rhythmTable.setSelectedRhythm(rdb.getRhythm(pairs.get(0).r.getUniqueId()));
                }
            };
            SwingUtilities.invokeLater(r);
        }
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
        LOGGER.fine("handleTableMouseClicked()  left=" + SwingUtilities.isLeftMouseButton(evt) + " ctrl=" + ctrl + " shift=" + shift + " clickCount=" + evt.getClickCount());   

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
     * @param rpVariationValue If not null
     * @return True if preview was successfully launched.
     */
    private boolean previewRhythm(RhythmInfo ri)
    {
        if (ri == null)
        {
            throw new IllegalArgumentException("ri=" + ri);   
        }

        if (rhythmPreviewProvider == null)
        {
            return false;
        }

        previewDone = true;

        LOGGER.fine("previewRhythm() ri=" + ri);   


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
            mapRpValues.put(RP_STD_Variation.getVariationRp(r), rpStdVariationValue);
        }


        // Preview
        try
        {
            LOGGER.fine("previewRhythm() calling rhythmPreviewProvider().previewRhythm()");   
            rhythmPreviewProvider.previewRhythm(r, mapRpValues, cb_useRhythmTempo.isSelected(), fbtn_autoPreviewMode.isSelected(), e -> rhythmPreviewComplete(r));
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
     * Adjust to the rhythm RP_STD_Variation possible values. Select a default value, reuse previous one if possible. The changes
     * done by this method do not trigger an JComboBox.actionPerformed(). If r does not use RP_STD_Variation disable cmbVariation.
     *
     * @param r
     * @return Null if r does not use RP_STD_Variation, otherwise return the RP_STD_Variation value to use (we try to reuse the
     * previous value)
     */
    private String updateCmbVariation(Rhythm r)
    {
        String newSelectedValue = null;
        RP_STD_Variation rpVariation = RP_STD_Variation.getVariationRp(r);

        if (rhythmPreviewProvider.getPreviewedRhythm() == r)
        {
            newSelectedValue = (String) cmb_variation.getSelectedItem();

        } else if (rpVariation != null)
        {
            cmb_variation.removeActionListener(this);

            String oldSelectedValue = (String) cmb_variation.getSelectedItem();
            var possibleValues = rpVariation.getPossibleValues();
            cmb_variation.setModel(new DefaultComboBoxModel<>(possibleValues.toArray(new String[0])));
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
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
        helpTextArea1 = new org.jjazz.ui.utilities.api.HelpTextArea();
        btn_addRhythms = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        fbtn_autoPreviewMode = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
        cmb_variation = new javax.swing.JComboBox<>();
        btn_deleteRhythm = new javax.swing.JButton();
        btn_openFolder = new javax.swing.JButton();
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

        org.openide.awt.Mnemonics.setLocalizedText(lbl_timeSignature, "3/4"); // NOI18N

        jScrollPane3.setBorder(null);

        helpTextArea1.setColumns(20);
        helpTextArea1.setRows(2);
        helpTextArea1.setText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.helpTextArea1.text")); // NOI18N
        jScrollPane3.setViewportView(helpTextArea1);

        org.openide.awt.Mnemonics.setLocalizedText(btn_addRhythms, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_addRhythms.text")); // NOI18N
        btn_addRhythms.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_addRhythms.toolTipText")); // NOI18N
        btn_addRhythms.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_addRhythmsActionPerformed(evt);
            }
        });

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 0));

        fbtn_autoPreviewMode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rhythmselectiondialog/resources/SpeakerOff-24x24.png"))); // NOI18N
        fbtn_autoPreviewMode.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.fbtn_autoPreviewMode.toolTipText")); // NOI18N
        fbtn_autoPreviewMode.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/rhythmselectiondialog/resources/SpeakerOnRed-24x24.png"))); // NOI18N
        jPanel2.add(fbtn_autoPreviewMode);

        cmb_variation.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cmb_variation.toolTipText")); // NOI18N
        cmb_variation.setEnabled(false);
        jPanel2.add(cmb_variation);

        org.openide.awt.Mnemonics.setLocalizedText(btn_deleteRhythm, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_deleteRhythm.text")); // NOI18N
        btn_deleteRhythm.setEnabled(false);
        btn_deleteRhythm.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_deleteRhythmActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_openFolder, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_openFolder.text")); // NOI18N
        btn_openFolder.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.btn_openFolder.toolTipText")); // NOI18N
        btn_openFolder.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_openFolderActionPerformed(evt);
            }
        });

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 67, Short.MAX_VALUE)
                        .addComponent(btn_clearFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Filter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_filter, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(btn_deleteRhythm, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btn_addRhythms, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btn_openFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(0, 66, Short.MAX_VALUE)))
                        .addContainerGap())))
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
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 433, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(85, 85, 85)
                        .addComponent(btn_addRhythms)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_deleteRhythm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_openFolder)))
                .addContainerGap())
        );

        lbl_Title.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_Title, "Select a 3/4 rhythm for rhythm part \"X\""); // NOI18N

        cb_applyRhythmToNextSpts.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cb_applyRhythmToNextSpts, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_applyRhythmToNextSpts.text")); // NOI18N
        cb_applyRhythmToNextSpts.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_applyRhythmToNextSpts.toolTipText")); // NOI18N
        cb_applyRhythmToNextSpts.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        cb_useRhythmTempo.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cb_useRhythmTempo, org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_useRhythmTempo.text")); // NOI18N
        cb_useRhythmTempo.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_useRhythmTempo.toolTipText")); // NOI18N

        tf_userRhythmDir.setEditable(false);
        tf_userRhythmDir.setToolTipText(org.openide.util.NbBundle.getMessage(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.tf_userRhythmDir.toolTipText")); // NOI18N

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
                        .addGap(12, 12, 12)
                        .addComponent(lbl_Title)
                        .addGap(99, 99, 99)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_userRhythmDir)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cb_useRhythmTempo)
                            .addComponent(cb_applyRhythmToNextSpts))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Ok, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel)
                        .addGap(8, 8, 8))))
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
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cb_useRhythmTempo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cb_applyRhythmToNextSpts))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Ok)
                            .addComponent(btn_Cancel))
                        .addContainerGap())))
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
            LOGGER.warning("btn_FilterActionPerformed() invalid filter regex string e=" + e.getMessage());   
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
        addRhythms();
    }//GEN-LAST:event_btn_addRhythmsActionPerformed

    private void btn_deleteRhythmActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_deleteRhythmActionPerformed
    {//GEN-HEADEREND:event_btn_deleteRhythmActionPerformed
        var ri = getSelectedRhythm();
        if (ri == null || ri.getFile().getName().equals(""))
        {
            return;
        }

        String msg = ResUtil.getString(getClass(), "CTL_ConfirmRhythmFileDelete", ri.getFile().getAbsolutePath());
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
        Object result = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.YES_OPTION == result)
        {
            ri.getFile().deleteOnExit();
            RhythmDatabase.getDefault().forceRescan(false);
        }

    }//GEN-LAST:event_btn_deleteRhythmActionPerformed

    private void btn_openFolderActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_openFolderActionPerformed
    {//GEN-HEADEREND:event_btn_openFolderActionPerformed
        var ri = getSelectedRhythm();
        if (ri == null || ri.getFile().getName().equals(""))
        {
            return;
        }
        org.jjazz.util.api.Utilities.browseFileDirectory(ri.getFile(), false);

    }//GEN-LAST:event_btn_openFolderActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Filter;
    private javax.swing.JButton btn_Ok;
    private javax.swing.JButton btn_addRhythms;
    private javax.swing.JButton btn_clearFilter;
    private javax.swing.JButton btn_deleteRhythm;
    private javax.swing.JButton btn_openFolder;
    private javax.swing.JCheckBox cb_applyRhythmToNextSpts;
    private javax.swing.JCheckBox cb_useRhythmTempo;
    private javax.swing.JComboBox<String> cmb_variation;
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton fbtn_autoPreviewMode;
    private org.jjazz.ui.utilities.api.HelpTextArea helpTextArea1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
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
            LOGGER.fine("actionPerformed() selectedRhythm=" + ri);   
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
                        LOGGER.fine("actionPerformed()    removing from favorites: " + ri);   
                        fr.removeRhythm(ri);
                    } else
                    {
                        LOGGER.fine("actionPerformed()    adding to favorites: " + ri);   
                        fr.addRhythm(ri);
                    }
                }
            }
        }
    }

}
