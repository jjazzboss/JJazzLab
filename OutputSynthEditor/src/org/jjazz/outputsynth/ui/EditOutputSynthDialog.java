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
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.outputsynth.OS_JJazzLabSoundFont_GM2;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
import org.jjazz.outputsynth.OS_JJazzLabSoundFont_GS;
import org.jjazz.outputsynth.OS_JJazzLabSoundFont_XG;
import org.jjazz.outputsynth.OS_YamahaRef;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
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

    private boolean askUserApplyPredefinedConfig(String presetName, String helpText)
    {
        PresetConfirmDialog dlg = new PresetConfirmDialog(WindowManager.getDefault().getMainWindow(), true, presetName, helpText);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        return dlg.isExitYes();
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

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        editor = new org.jjazz.outputsynth.ui.OutputSynthEditor();
        btn_Save = new javax.swing.JButton();
        tf_configName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        menu_file = new javax.swing.JMenu();
        mi_load = new javax.swing.JMenuItem();
        mi_saveAs = new javax.swing.JMenuItem();
        menu_predefined = new javax.swing.JMenu();
        mi_defaultGM = new javax.swing.JMenuItem();
        submnu_JJazzLabSoundFont = new javax.swing.JMenu();
        mi_jjazzlabSoundFontGS = new javax.swing.JMenuItem();
        mi_jjazzlabSoundFontXG = new javax.swing.JMenuItem();
        mi_jjazzlabSoundFontGM2 = new javax.swing.JMenuItem();
        mi_yamaha = new javax.swing.JMenuItem();

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

        tf_configName.setEditable(false);
        tf_configName.setText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.tf_configName.text")); // NOI18N
        tf_configName.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.tf_configName.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(menu_file, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.menu_file.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(mi_load, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_load.text")); // NOI18N
        mi_load.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_loadActionPerformed(evt);
            }
        });
        menu_file.add(mi_load);

        org.openide.awt.Mnemonics.setLocalizedText(mi_saveAs, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_saveAs.text")); // NOI18N
        mi_saveAs.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_saveAsActionPerformed(evt);
            }
        });
        menu_file.add(mi_saveAs);

        jMenuBar1.add(menu_file);

        org.openide.awt.Mnemonics.setLocalizedText(menu_predefined, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.menu_predefined.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(mi_defaultGM, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_defaultGM.text")); // NOI18N
        mi_defaultGM.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_defaultGMActionPerformed(evt);
            }
        });
        menu_predefined.add(mi_defaultGM);

        org.openide.awt.Mnemonics.setLocalizedText(submnu_JJazzLabSoundFont, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.submnu_JJazzLabSoundFont.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(mi_jjazzlabSoundFontGS, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_jjazzlabSoundFontGS.text")); // NOI18N
        mi_jjazzlabSoundFontGS.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_jjazzlabSoundFontGSActionPerformed(evt);
            }
        });
        submnu_JJazzLabSoundFont.add(mi_jjazzlabSoundFontGS);

        org.openide.awt.Mnemonics.setLocalizedText(mi_jjazzlabSoundFontXG, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_jjazzlabSoundFontXG.text")); // NOI18N
        mi_jjazzlabSoundFontXG.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_jjazzlabSoundFontXGActionPerformed(evt);
            }
        });
        submnu_JJazzLabSoundFont.add(mi_jjazzlabSoundFontXG);

        org.openide.awt.Mnemonics.setLocalizedText(mi_jjazzlabSoundFontGM2, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_jjazzlabSoundFontGM2.text")); // NOI18N
        mi_jjazzlabSoundFontGM2.setToolTipText(org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_jjazzlabSoundFontGM2.toolTipText")); // NOI18N
        mi_jjazzlabSoundFontGM2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_jjazzlabSoundFontGM2ActionPerformed(evt);
            }
        });
        submnu_JJazzLabSoundFont.add(mi_jjazzlabSoundFontGM2);

        menu_predefined.add(submnu_JJazzLabSoundFont);

        org.openide.awt.Mnemonics.setLocalizedText(mi_yamaha, org.openide.util.NbBundle.getMessage(EditOutputSynthDialog.class, "EditOutputSynthDialog.mi_yamaha.text")); // NOI18N
        mi_yamaha.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mi_yamahaActionPerformed(evt);
            }
        });
        menu_predefined.add(mi_yamaha);

        jMenuBar1.add(menu_predefined);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 804, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tf_configName, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_Save)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Save)
                    .addComponent(jLabel1)
                    .addComponent(tf_configName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    private void mi_loadActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_loadActionPerformed
    {//GEN-HEADEREND:event_mi_loadActionPerformed
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
    }//GEN-LAST:event_mi_loadActionPerformed

    private void mi_saveAsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_saveAsActionPerformed
    {//GEN-HEADEREND:event_mi_saveAsActionPerformed
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
                LOGGER.warning("mi_saveAsActionPerformed() " + msg);
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            }
            osm.setOutputSynth(newOutSynth);
            StatusDisplayer.getDefault().setStatusText("Saved " + f.getAbsolutePath());
        }
    }//GEN-LAST:event_mi_saveAsActionPerformed

    private void mi_defaultGMActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_defaultGMActionPerformed
    {//GEN-HEADEREND:event_mi_defaultGMActionPerformed
        JMenuItem mi = (JMenuItem) evt.getSource();
        if (askUserApplyPredefinedConfig("GM", null))
        {
            outputSynth.reset();
        }
    }//GEN-LAST:event_mi_defaultGMActionPerformed

    private void mi_jjazzlabSoundFontGSActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_jjazzlabSoundFontGSActionPerformed
    {//GEN-HEADEREND:event_mi_jjazzlabSoundFontGSActionPerformed
        String preset = "JJazzLab SoundFont/VirtualMidiSynth";
        if (askUserApplyPredefinedConfig(preset,
                "This preset requires that :\n\n  1/ VirualMIDISynth is the Midi output device of JJazzLab\n  2/ VirtualMIDISynth uses the JJazzLab SoundFont."))
        {
            File f = outputSynth.getFile();
            OutputSynthManager osm = OutputSynthManager.getInstance();
            OutputSynth outSynth = new OutputSynth(OS_JJazzLabSoundFont_GS.getInstance());
            outSynth.setFile(f);
            osm.setOutputSynth(outSynth);
            StatusDisplayer.getDefault().setStatusText("Applied Output Synth preset: " + preset);
        }
    }//GEN-LAST:event_mi_jjazzlabSoundFontGSActionPerformed

    private void mi_yamahaActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_yamahaActionPerformed
    {//GEN-HEADEREND:event_mi_yamahaActionPerformed
        String preset = "Yamaha Tyros/PSR";
        if (askUserApplyPredefinedConfig(preset, "This preset requires that :\n\n  - A Yamaha Tyros/PSR compatible synth is connected to JJazzLab."))
        {
            File f = outputSynth.getFile();
            OutputSynthManager osm = OutputSynthManager.getInstance();
            OutputSynth outSynth = new OutputSynth(OS_YamahaRef.getInstance());
            outSynth.setFile(f);
            osm.setOutputSynth(outSynth);
            StatusDisplayer.getDefault().setStatusText("Applied Output Synth preset: " + preset);
        }
    }//GEN-LAST:event_mi_yamahaActionPerformed

    private void mi_jjazzlabSoundFontGM2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_jjazzlabSoundFontGM2ActionPerformed
    {//GEN-HEADEREND:event_mi_jjazzlabSoundFontGM2ActionPerformed
        String preset = "JJazzLab SoundFont/Java Internal Synth";
        if (askUserApplyPredefinedConfig(preset,
                "This preset requires that :\n\n  1/ Java Internal Synth is the Midi output device of JJazzLab\n  2/ Java Internal Synth uses the JJazzLab SoundFont (see Midi Options/Preferences)."
                + "\n\nNOTE: if you experience dropped notes during playback, prefer external SoundFont synths like VirtualMIDISynth (Windows) or FluidSynth (Linux)."))
        {
            File f = outputSynth.getFile();
            OutputSynthManager osm = OutputSynthManager.getInstance();
            OutputSynth outSynth = new OutputSynth(OS_JJazzLabSoundFont_GM2.getInstance());
            outSynth.setFile(f);
            osm.setOutputSynth(outSynth);
            StatusDisplayer.getDefault().setStatusText("Applied Output Synth preset: " + preset);
        }
    }//GEN-LAST:event_mi_jjazzlabSoundFontGM2ActionPerformed

    private void mi_jjazzlabSoundFontXGActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mi_jjazzlabSoundFontXGActionPerformed
    {//GEN-HEADEREND:event_mi_jjazzlabSoundFontXGActionPerformed
        String preset = "JJazzLab SoundFont/FluidSynth";
        if (askUserApplyPredefinedConfig(preset,
                "This preset requires that :\n\n  1/ JJazzLab is connected to FluidSynth\n  2/ FluidSynth uses the JJazzLab SoundFont\n  3/ FluidSynth uses Midi Bank Select mode 'XG'"))
        {
            File f = outputSynth.getFile();
            OutputSynthManager osm = OutputSynthManager.getInstance();
            OutputSynth outSynth = new OutputSynth(OS_JJazzLabSoundFont_XG.getInstance());
            outSynth.setFile(f);
            osm.setOutputSynth(outSynth);
            StatusDisplayer.getDefault().setStatusText("Applied Output Synth preset: " + preset);
        }
    }//GEN-LAST:event_mi_jjazzlabSoundFontXGActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Save;
    private org.jjazz.outputsynth.ui.OutputSynthEditor editor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu menu_file;
    private javax.swing.JMenu menu_predefined;
    private javax.swing.JMenuItem mi_defaultGM;
    private javax.swing.JMenuItem mi_jjazzlabSoundFontGM2;
    private javax.swing.JMenuItem mi_jjazzlabSoundFontGS;
    private javax.swing.JMenuItem mi_jjazzlabSoundFontXG;
    private javax.swing.JMenuItem mi_load;
    private javax.swing.JMenuItem mi_saveAs;
    private javax.swing.JMenuItem mi_yamaha;
    private javax.swing.JMenu submnu_JJazzLabSoundFont;
    private javax.swing.JTextField tf_configName;
    // End of variables declaration//GEN-END:variables

}
