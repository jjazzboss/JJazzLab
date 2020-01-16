/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
import org.jjazz.util.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 */
public class EditOutputSynthDialog extends javax.swing.JDialog implements PropertyChangeListener
{

    private static EditOutputSynthDialog INSTANCE;
    private OutputSynth outputSynth;
    private static final Logger LOGGER = Logger.getLogger(EditOutputSynthDialog.class.getSimpleName());

    public static EditOutputSynthDialog getInstance()
    {
        synchronized (EditOutputSynthDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new EditOutputSynthDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    private EditOutputSynthDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
        OutputSynthManager.getInstance().addPropertyChangeListener(this);
    }

    public void preset(OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new NullPointerException("outSynth");
        }
        if (outputSynth != null)
        {
            outputSynth.removePropertyChangeListener(this);
        }
        outputSynth = outSynth;
        outputSynth.addPropertyChangeListener(this);
        editor.preset(outputSynth);
        updateUI();
    }

    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == outputSynth)
        {
            if (evt.getPropertyName() == OutputSynth.PROP_FILE)
            {
                updateUI();
            }
        } else if (evt.getSource() == OutputSynthManager.getInstance())
        {
            if (evt.getPropertyName() == OutputSynthManager.PROP_DEFAULT_OUTPUTSYNTH)
            {
                preset((OutputSynth) evt.getNewValue());
            }
        }
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================
    private void updateUI()
    {
        String title = "Output Synth Configuration Editor";
        String cfgName = outputSynth.getFile() == null ? "-" : outputSynth.getFile().getName();
        if (outputSynth.getFile() != null)
        {
            title += ": " + cfgName;
        }
        this.setTitle(title);
        this.tf_configName.setText(cfgName);
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
                btn_SaveActionPerformed(null);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_SaveActionPerformed(null);
            }
        });
        return contentPane;
    }

    /** This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        editor = new org.jjazz.outputsynth.ui.OutputSynthEditor();
        btn_Save = new javax.swing.JButton();
        btn_SaveAsConfig = new javax.swing.JButton();
        btn_Load = new javax.swing.JButton();
        btn_Reset = new javax.swing.JButton();
        tf_configName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        setTitle(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.title")); // NOI18N

        btn_Save.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(btn_Save, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_Save.text")); // NOI18N
        btn_Save.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_Save.toolTipText")); // NOI18N
        btn_Save.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_SaveActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_SaveAsConfig, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_SaveAsConfig.text")); // NOI18N
        btn_SaveAsConfig.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_SaveAsConfig.toolTipText")); // NOI18N
        btn_SaveAsConfig.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_SaveAsConfigActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Load, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_Load.text")); // NOI18N
        btn_Load.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_Load.toolTipText")); // NOI18N
        btn_Load.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_LoadActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Reset, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_Reset.text")); // NOI18N
        btn_Reset.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.btn_Reset.toolTipText")); // NOI18N
        btn_Reset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_ResetActionPerformed(evt);
            }
        });

        tf_configName.setEditable(false);
        tf_configName.setText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.tf_configName.text")); // NOI18N
        tf_configName.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.tf_configName.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_configName, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Load)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_SaveAsConfig)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Reset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Save, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Save)
                    .addComponent(btn_SaveAsConfig)
                    .addComponent(btn_Load)
                    .addComponent(btn_Reset)
                    .addComponent(tf_configName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_SaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_SaveActionPerformed
    {//GEN-HEADEREND:event_btn_SaveActionPerformed
        File f = outputSynth.getFile();
        if (f != null)
        {
            try
            {
                outputSynth.saveToFile(f);
                StatusDisplayer.getDefault().setStatusText("Saved " + f.getAbsolutePath());
            } catch (IOException ex)
            {
                String msg = "Problem saving output synth file " + f.getName() + " : " + ex.getLocalizedMessage();
                LOGGER.warning("btn_SaveActionPerformed() " + msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            }
        }
        setVisible(false);
    }//GEN-LAST:event_btn_SaveActionPerformed

    private void btn_ResetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_ResetActionPerformed
    {//GEN-HEADEREND:event_btn_ResetActionPerformed
        String msg = "This will reset this dialog settings to their default values. Do you confirm ?";
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
        Object result = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.YES_OPTION == result)
        {
            outputSynth.reset();
        }
    }//GEN-LAST:event_btn_ResetActionPerformed

    private void btn_LoadActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_LoadActionPerformed
    {//GEN-HEADEREND:event_btn_LoadActionPerformed
        OutputSynthManager osm = OutputSynthManager.getInstance();
        File f = osm.showSelectOutputSynthFileDialog(false);
        if (f != null)
        {
            OutputSynth outSynth = osm.loadOutputSynth(f, true);
            if (outSynth != null)
            {
                osm.setOutputSynth(outSynth);
                StatusDisplayer.getDefault().setStatusText("Loaded " + f.getAbsolutePath());
            }
        }
    }//GEN-LAST:event_btn_LoadActionPerformed

    private void btn_SaveAsConfigActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_SaveAsConfigActionPerformed
    {//GEN-HEADEREND:event_btn_SaveAsConfigActionPerformed
        OutputSynthManager osm = OutputSynthManager.getInstance();
        File f = osm.showSelectOutputSynthFileDialog(true);
        if (f != null)
        {
            if (f.exists())
            {
                String msg = "Overwrite " + f.getName() + " ?";
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                Object result = DialogDisplayer.getDefault().notify(d);
                if (NotifyDescriptor.YES_OPTION != result)
                {
                    return;
                }
            }
            OutputSynth newOutSynth = new OutputSynth(outputSynth);
            try
            {
                newOutSynth.saveToFile(f);
            } catch (IOException ex)
            {
                String msg = "Problem saving file " + f.getName() + ". Ex=" + ex.getLocalizedMessage();
                LOGGER.warning("btn_SaveAsConfigActionPerformed() " + msg);
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }
            osm.setOutputSynth(newOutSynth);
            StatusDisplayer.getDefault().setStatusText("Saved " + f.getAbsolutePath());
        }

    }//GEN-LAST:event_btn_SaveAsConfigActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Load;
    private javax.swing.JButton btn_Reset;
    private javax.swing.JButton btn_Save;
    private javax.swing.JButton btn_SaveAsConfig;
    private org.jjazz.outputsynth.ui.OutputSynthEditor editor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField tf_configName;
    // End of variables declaration//GEN-END:variables

}
