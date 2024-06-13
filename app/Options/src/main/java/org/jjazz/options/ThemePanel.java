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
package org.jjazz.options;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uisettings.api.Theme;
import org.openide.awt.ColorComboBox;
import org.openide.util.Lookup;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider;
import org.jjazz.uiutilities.api.FontColorUserSettingsProvider.FCSetting;
import org.jjazz.uiutilities.api.JFontChooser;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

final class ThemePanel extends javax.swing.JPanel implements ActionListener
{

    /**
     * Store the FCSettings values by id.
     */
    private final HashMap<String, FCvalues> saveMapIdValues = new HashMap<>();
    private final List<FCSetting> saveListValues = new ArrayList<>();
    private Theme saveTheme;
    private final ThemeOptionsPanelController controller;
    private ColorComboBox colorComboBox;
    private static final Logger LOGGER = Logger.getLogger(ThemePanel.class.getSimpleName());

    ThemePanel(ThemeOptionsPanelController controller)
    {
        this.controller = controller;

        colorComboBox = new ColorComboBox();
        initComponents();

        FCSettingCellRenderer cdr = new FCSettingCellRenderer();
        list_fcSettings.setCellRenderer(cdr);

        // TODO listen to changes in form fields and call controller.changed()
    }

    void load()
    {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(EditorPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(EditorPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());

        saveMapIdValues.clear();
        saveListValues.clear();
        Collection<? extends FontColorUserSettingsProvider> result = Lookup.getDefault().lookupAll(FontColorUserSettingsProvider.class);
        for (FontColorUserSettingsProvider p : result)
        {
            List<FCSetting> pFcsSettings = p.getFCSettings();

            // Prepare data for JList
            saveListValues.addAll(pFcsSettings);

            // Save original values
            for (FCSetting fcs : pFcsSettings)
            {
                String id = fcs.getId();
                if (saveMapIdValues.get(id) != null)
                {
                    LOGGER.log(Level.SEVERE, "Duplicate FCSetting Id={0}", id);
                }
                saveMapIdValues.put(id, new FCvalues(fcs));
            }
        }

        // Sort the list by name
        saveListValues.sort((fcs1, fcs2) -> fcs1.getDisplayName().compareToIgnoreCase(fcs2.getDisplayName()));

        list_fcSettings.setListData(saveListValues.toArray(new FCSetting[0]));
        if (!saveListValues.isEmpty())
        {
            list_fcSettings.setSelectedIndex(0);
        }
        btn_resetAll.setEnabled(!saveListValues.isEmpty());

        saveTheme = GeneralUISettings.getInstance().getSessionTheme();
        cmb_themes.setSelectedItem(saveTheme);

    }

    void store()
    {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(EditorPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(EditorPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());


        Theme theme = (Theme) cmb_themes.getSelectedItem();
        var uis = GeneralUISettings.getInstance();
        if (uis.getSessionTheme() != theme)
        {
            uis.setThemeUponRestart((Theme) cmb_themes.getSelectedItem(), false);
        }
    }

    public void restoreOldValues()
    {
        for (FCSetting fcs : saveListValues)
        {
            FCvalues fcv = saveMapIdValues.get(fcs.getId());
            assert fcv != null : "fcs=" + fcs + " mapIdValues=" + saveMapIdValues;
            if (fcv.color != null)
            {
                fcs.setColor(fcv.color);
            }
            if (fcv.font != null)
            {
                fcs.setFont(fcv.font);
            }
        }

        cmb_themes.setSelectedItem(saveTheme);

    }

    boolean valid()
    {
        // TODO check whether form is consistent and complete
        return true;
    }

    // ===================================================================================
    // ActionListener implementation
    // ===================================================================================    
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getSource() == colorComboBox)
        {
            Color c = colorComboBox.getSelectedColor();
            if (c != null)
            {
                FCSetting fcs = list_fcSettings.getSelectedValue();
                assert fcs != null;
                fcs.setColor(c);
            }
        }
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================

    // =========================================================================================
    // Private classes
    // =========================================================================================
    /**
     * Store a FCSetting value pair.
     */
    private class FCvalues
    {

        public Font font;
        public Color color;

        public FCvalues(FCSetting fcs)
        {
            font = fcs.getFont();
            color = fcs.getColor();
        }

        @Override
        public String toString()
        {
            return font.getName() + "-" + color;
        }
    }

    private class FCSettingCellRenderer extends DefaultListCellRenderer
    {

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JComponent jc = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            FCSetting fcs = (FCSetting) value;
            setText(fcs.getDisplayName());
            return jc;
        }
    }
    // =========================================================================================
    // Private methods
    // =========================================================================================

    /**
     * Set the selected color in the combo WITHOUT notifying the ActionListeners.
     *
     * @param c
     */
    private void setComboSelectedColorSilently(Color c)
    {
        colorComboBox.removeActionListener(this);
        colorComboBox.setSelectedColor(c);
        colorComboBox.addActionListener(this);
    }

    /**
     * Update editors to match the selected fcSetting.
     */
    private void updateFontAndColorEditors()
    {

        // Reset everything by default
        lbl_font.setEnabled(false);
        btn_font.setEnabled(false);
        tf_font.setEnabled(false);
        tf_font.setText("");
        btn_resetFont.setEnabled(false);
        lbl_color.setEnabled(false);
        colorComboBox.setEnabled(false);
        btn_resetColor.setEnabled(false);

        FCSetting fcs = list_fcSettings.getSelectedValue();
        if (fcs != null)
        {
            Font f = fcs.getFont();
            if (f != null)
            {
                btn_font.setEnabled(true);
                tf_font.setEnabled(true);
                lbl_font.setEnabled(true);
                btn_resetFont.setEnabled(true);
                tf_font.setText(Utilities.fontAsString(f));
                tf_font.setFont(f);
            }
            Color c = fcs.getColor();
            if (c != null)
            {
                colorComboBox.setEnabled(true);
                setComboSelectedColorSilently(c);   // Do not trigger ActionListener to avoid creating a customization
                lbl_color.setEnabled(true);
                btn_resetColor.setEnabled(true);
            }
        }
    }

    private void resetAllSettings()
    {
        for (FCSetting fcs : saveListValues)
        {
            fcs.setColor(null);
            fcs.setFont(null);
        }
        list_fcSettingsValueChanged(null);

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jPanel2 = new javax.swing.JPanel();
        lbl_font = new javax.swing.JLabel();
        btn_resetAll = new javax.swing.JButton();
        btn_resetColor = new javax.swing.JButton();
        btn_font = new javax.swing.JButton();
        tf_font = new javax.swing.JTextField();
        cmb_color = colorComboBox;
        btn_resetFont = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        list_fcSettings = new javax.swing.JList<>();
        lbl_color = new javax.swing.JLabel();
        cmb_themes = new JComboBox<>(GeneralUISettings.getThemes().toArray(new Theme[0]));
        lblTheme = new javax.swing.JLabel();

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.jPanel2.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_font, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.lbl_font.text")); // NOI18N
        lbl_font.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(btn_resetAll, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.btn_resetAll.text")); // NOI18N
        btn_resetAll.setEnabled(false);
        btn_resetAll.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetAllActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_resetColor, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.btn_resetColor.text")); // NOI18N
        btn_resetColor.setEnabled(false);
        btn_resetColor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetColorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_font, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.btn_font.text")); // NOI18N
        btn_font.setEnabled(false);
        btn_font.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_fontActionPerformed(evt);
            }
        });

        tf_font.setEditable(false);
        tf_font.setEnabled(false);
        tf_font.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                tf_fontMouseClicked(evt);
            }
        });
        tf_font.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_fontActionPerformed(evt);
            }
        });

        cmb_color.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(btn_resetFont, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.btn_resetFont.text")); // NOI18N
        btn_resetFont.setEnabled(false);
        btn_resetFont.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_resetFontActionPerformed(evt);
            }
        });

        list_fcSettings.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_fcSettings.addListSelectionListener(new javax.swing.event.ListSelectionListener()
        {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt)
            {
                list_fcSettingsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list_fcSettings);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_color, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.lbl_color.text")); // NOI18N
        lbl_color.setEnabled(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lbl_color, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_font, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tf_font)
                    .addComponent(cmb_color, 0, 126, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_font)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_resetFont)
                    .addComponent(btn_resetColor)
                    .addComponent(btn_resetAll))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_resetColor, btn_resetFont});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lbl_color, lbl_font});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btn_font)
                                .addComponent(btn_resetFont))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lbl_font)
                                .addComponent(tf_font, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbl_color)
                            .addComponent(btn_resetColor)
                            .addComponent(cmb_color, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(btn_resetAll)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        cmb_themes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cmb_themesActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lblTheme, org.openide.util.NbBundle.getMessage(ThemePanel.class, "ThemePanel.lblTheme.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cmb_themes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTheme)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmb_themes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTheme))
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

   private void btn_fontActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_fontActionPerformed
   {//GEN-HEADEREND:event_btn_fontActionPerformed
       FCSetting fcs = list_fcSettings.getSelectedValue();
       assert fcs != null;

       JFontChooser jfc = new JFontChooser();
       jfc.setSelectedFont(fcs.getFont());
       int res = jfc.showDialog(this);

       if (res == JFontChooser.OK_OPTION)
       {
           Font newFont = jfc.getSelectedFont();
           fcs.setFont(newFont);
           tf_font.setText(Utilities.fontAsString(newFont));
           tf_font.setFont(newFont);
       }

   }//GEN-LAST:event_btn_fontActionPerformed

   private void list_fcSettingsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_list_fcSettingsValueChanged
   {//GEN-HEADEREND:event_list_fcSettingsValueChanged
       // Selected fcSettings has changed, update the font/color editors
       if (evt != null && evt.getValueIsAdjusting())
       {
           return;
       }
       updateFontAndColorEditors();

   }//GEN-LAST:event_list_fcSettingsValueChanged

   private void btn_resetAllActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetAllActionPerformed
   {//GEN-HEADEREND:event_btn_resetAllActionPerformed

       NotifyDescriptor nd = new NotifyDescriptor.Confirmation(ResUtil.getString(getClass(), "CTL_ConfirmResetAllThemeCategories"),
               NotifyDescriptor.OK_CANCEL_OPTION);
       Object result = DialogDisplayer.getDefault().notify(nd);
       if (result != NotifyDescriptor.OK_OPTION)
       {
           return;
       }
       resetAllSettings();
   }//GEN-LAST:event_btn_resetAllActionPerformed

   private void btn_resetFontActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetFontActionPerformed
   {//GEN-HEADEREND:event_btn_resetFontActionPerformed
       FCSetting fcs = list_fcSettings.getSelectedValue();
       assert fcs != null;
       fcs.setFont(null);
       list_fcSettingsValueChanged(null);
   }//GEN-LAST:event_btn_resetFontActionPerformed

   private void btn_resetColorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_resetColorActionPerformed
   {//GEN-HEADEREND:event_btn_resetColorActionPerformed
       FCSetting fcs = list_fcSettings.getSelectedValue();
       assert fcs != null;
       fcs.setColor(null);
       list_fcSettingsValueChanged(null);
   }//GEN-LAST:event_btn_resetColorActionPerformed

    private void tf_fontActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_fontActionPerformed
    {//GEN-HEADEREND:event_tf_fontActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tf_fontActionPerformed

    private void tf_fontMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_tf_fontMouseClicked
    {//GEN-HEADEREND:event_tf_fontMouseClicked
        // Double click is same as pressing Change button
        if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && btn_font.isEnabled())
        {
            btn_fontActionPerformed(null);
        }
    }//GEN-LAST:event_tf_fontMouseClicked

    private void cmb_themesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmb_themesActionPerformed
    {//GEN-HEADEREND:event_cmb_themesActionPerformed
        controller.changed();
    }//GEN-LAST:event_cmb_themesActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_font;
    private javax.swing.JButton btn_resetAll;
    private javax.swing.JButton btn_resetColor;
    private javax.swing.JButton btn_resetFont;
    private javax.swing.JComboBox cmb_color;
    private javax.swing.JComboBox<Theme> cmb_themes;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTheme;
    private javax.swing.JLabel lbl_color;
    private javax.swing.JLabel lbl_font;
    private javax.swing.JList<FCSetting> list_fcSettings;
    private javax.swing.JTextField tf_font;
    // End of variables declaration//GEN-END:variables

}
