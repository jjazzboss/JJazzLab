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
package org.jjazz.ui.cl_editor.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordSymbolTextInput;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import static org.jjazz.ui.cl_editor.editors.Bundle.CTL_Bar;
import static org.jjazz.ui.cl_editor.editors.Bundle.ERR_DuplicateSectionName;
import static org.jjazz.ui.cl_editor.editors.Bundle.ERR_IncompleteSection;
import static org.jjazz.ui.cl_editor.editors.Bundle.ERR_InvalidTimeSignature;
import org.jjazz.ui.cl_editor.spi.CL_BarEditorDialog;
import org.jjazz.ui.cl_editor.spi.Preset;
import org.jjazz.ui.utilities.NoSelectOnFocusGainedJTF;
import org.jjazz.util.diff.api.DiffProvider;
import org.jjazz.util.diff.api.Difference;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle.Messages;

@Messages(
        {
            "CTL_Bar=Bar",
            "ERR_IncompleteSection=Both section name and time signature must be specified",
            "ERR_DuplicateSectionName=Section name is already used",
            "ERR_InvalidTimeSignature=Invalid time signature value"
        })
public class CL_BarEditorDialogImpl extends CL_BarEditorDialog
{

    static private CL_BarEditorDialogImpl INSTANCE;

    static public CL_BarEditorDialogImpl getInstance()
    {
        synchronized (CL_BarEditorDialogImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new CL_BarEditorDialogImpl();
            }
        }
        return INSTANCE;
    }
    /**
     * The ChordLeadSheet which is edited.
     */
    private ChordLeadSheet model;
    /**
     * The list of ChordSymbol in the model.
     */
    private List<? extends CLI_ChordSymbol> modelCsList;
    /**
     * The Section in the model.
     */
    private CLI_Section modelSection;
    /**
     * The barIndex for this dialog.
     */
    private int barIndex;
    private CLI_Section resultSection;
    private List<ChordLeadSheetItem<?>> resultAddedItems;
    private List<ChordLeadSheetItem<?>> resultRemovedItems;
    private HashMap<CLI_ChordSymbol, ExtChordSymbol> resultMapChangedChordSymbols;
    /**
     * Undo manager for the text edits
     */
    private UndoManager undoManager = new UndoManager();
    /**
     * The component who will receive the focus when dialog is shown.
     */
    private Component focusOnShow;
    /**
     * True if dialog was exited with the OK action.
     */
    private boolean exitedOk;
    /**
     * Save the text to check if it has been changed on exit.
     */
    private final Color saveSectionFieldsForeground;
    private String saveCsText;
    private String saveTsText;
    private String saveSectionText;
    private static final Logger LOGGER = Logger.getLogger(CL_BarEditorDialogImpl.class.getSimpleName());

    private CL_BarEditorDialogImpl()
    {
        initComponents();
        saveSectionFieldsForeground = jtfSectionName.getForeground();
        resultSection = null;
        resultAddedItems = new ArrayList<>();
        resultRemovedItems = new ArrayList<>();
        resultMapChangedChordSymbols = new HashMap<>();
    }

    @Override
    public void cleanup()
    {
        model = null;
        modelSection = null;
        if (modelCsList != null)
        {
            modelCsList.clear();
        }
        resultSection = null;
        resultAddedItems.clear();
        resultRemovedItems.clear();
        resultMapChangedChordSymbols.clear();
    }

    @Override
    public void preset(final Preset preset, ChordLeadSheet cls, int barIndx)
    {
        if (preset == null || cls == null || barIndx < 0 || barIndx >= cls.getSize())
        {
            throw new IllegalArgumentException("preset=" + preset + " cls=" + cls + " barIndx=" + barIndx);
        }

        cleanup();

        model = cls;
        barIndex = barIndx;
        modelCsList = model.getItems(barIndex, barIndex, CLI_ChordSymbol.class);
        modelSection = model.getSection(barIndex);
        boolean isSectionInBar = (modelSection.getPosition().getBar() == barIndx);

        // Update the section field
        jtfSectionName.setText(modelSection.getData().getName());
        saveSectionText = jtfSectionName.getText();

        // Update the Time Signature field
        jtfTimeSignature.setText(modelSection.getData().getTimeSignature().toString());
        saveTsText = jtfTimeSignature.getText();

        if (!isSectionInBar)
        {
            jtfSectionName.setForeground(Color.LIGHT_GRAY);
            jtfTimeSignature.setForeground(Color.LIGHT_GRAY);
        } else
        {
            jtfSectionName.setForeground(saveSectionFieldsForeground);
            jtfTimeSignature.setForeground(saveSectionFieldsForeground);
        }

        // Update the Chord Symbols field
        jtfChordSymbols.setText(ChordSymbolTextInput.toStringNoPosition(modelCsList));
        saveCsText = jtfChordSymbols.getText();

        setTitle(CTL_Bar() + " " + (barIndx + 1) + " - " + modelSection.getData().getName() + " " + modelSection.getData().getTimeSignature());
        undoManager.discardAllEdits();

        // Specific actions depending on presets
        switch (preset.getPresetType())
        {
            case BarEdit:
            {
                focusOnShow = jtfChordSymbols;
                if (preset.getKey() != (char) 0)
                {
                    // Append char at the end, with a leading space if required
                    final String text = jtfChordSymbols.getText().trim();
                    final String space = text.isEmpty() ? "" : " ";
                    jtfChordSymbols.setText(text + space + Character.toUpperCase(preset.getKey()));
                } else
                {
                    jtfChordSymbols.selectAll();
                }
            }
            break;

            case ChordSymbolEdit:
            {
                focusOnShow = jtfChordSymbols;
                CLI_ChordSymbol item = (CLI_ChordSymbol) preset.getItem();
                selectChordSymbol(item);
                if (preset.getKey() != (char) 0)
                {
                    jtfChordSymbols.replaceSelection("" + Character.toUpperCase(preset.getKey()));
                }
            }
            break;
            case SectionNameEdit:
            {
                focusOnShow = jtfSectionName;
                jtfSectionName.selectAll();
            }
            break;
            case TimeSignatureEdit:
            {
                focusOnShow = jtfTimeSignature;
                jtfTimeSignature.selectAll();
            }
            break;
        }
    }

    @Override
    public boolean isExitOk()
    {
        return exitedOk;
    }

    @Override
    public CLI_Section getSection()
    {
        return resultSection;
    }

    @Override
    public List<ChordLeadSheetItem<?>> getAddedItems()
    {
        return resultAddedItems;
    }

    @Override
    public List<ChordLeadSheetItem<?>> getRemovedItems()
    {
        return resultRemovedItems;
    }

    @Override
    public Map<CLI_ChordSymbol, ExtChordSymbol> getUpdatedChordSymbols()
    {
        return resultMapChangedChordSymbols;
    }

    // ------------------------------------------------------------------------------
    // Private functions
    // ------------------------------------------------------------------------------    
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    private void actionOK()
    {
        String strSection = jtfSectionName.getText().trim();
        String strSignature = jtfTimeSignature.getText().trim();
        String strChords = jtfChordSymbols.getText().trim();
        boolean isTimeSignatureChanged = !saveTsText.equals(strSignature);
        boolean isSectionChanged = !saveSectionText.equals(strSection);
        boolean isChordsChanged = !saveCsText.equals(strChords);
        boolean isSectionInBar = (modelSection.getPosition().getBar() == barIndex);

        if (isSectionChanged || isTimeSignatureChanged)
        {
            // Both fields must be filled
            if (strSection.length() == 0 || strSignature.length() == 0)
            {
                notifyError(ERR_IncompleteSection());
                if (strSection.length() == 0)
                {
                    jtfSectionName.requestFocusInWindow();
                } else
                {
                    jtfTimeSignature.requestFocusInWindow();
                }
                return;
            }

            // Convert the time signature
            TimeSignature ts = null;
            try
            {
                ts = TimeSignature.parse(strSignature);

            } catch (ParseException e)
            {
                notifyError(ERR_InvalidTimeSignature());
                jtfTimeSignature.selectAll();
                jtfTimeSignature.requestFocusInWindow();
                return;
            }

            // Check section name is valid
            if (model.getSection(strSection) != null && !(isSectionInBar && modelSection.getData().getName().equals(strSection)))
            {
                notifyError(ERR_DuplicateSectionName());
                jtfSectionName.selectAll();
                jtfSectionName.requestFocusInWindow();
                return;
            }

            // Finally update the result
            resultSection = CLI_Factory.getDefault().createSection(model, strSection, ts, barIndex);

        }

        if (isChordsChanged)
        {
            List<CLI_ChordSymbol> newItems;
            try
            {
                newItems = ChordSymbolTextInput.toCLI_ChordSymbolsNoPosition(strChords, barIndex, model);
            } catch (ParseException ex)
            {
                // Select the erroneous chord symbol
                String[] rawStrings = strChords.split("\\s+");
                int start = jtfChordSymbols.getText().indexOf(rawStrings[ex.getErrorOffset()]);
                int length = rawStrings[ex.getErrorOffset()].length();
                jtfChordSymbols.select(start, start + length);
                notifyError(ex.getMessage());
                jtfChordSymbols.requestFocusInWindow();
                return;
            }

            // Use diff to see what's added/deleted/changed
            DiffProvider dp = DiffProvider.Utilities.getDefault();
            List<Difference> diffResult = dp.diff(modelCsList, newItems, new Comparator<CLI_ChordSymbol>()
            {
                @Override
                public int compare(CLI_ChordSymbol i1, CLI_ChordSymbol i2)
                {
                    return (i1.getData().equals(i2.getData())) ? 0 : 1;
                }
            });
            LOGGER.log(Level.FINE, "Diff model=" + modelCsList + " newItems=" + newItems);
            for (Difference aDiff : diffResult)
            {
                if (aDiff.getType() == Difference.ResultType.ADDED)
                {
                    for (int i = aDiff.getAddedStart(); i <= aDiff.getAddedEnd(); i++)
                    {
                        resultAddedItems.add(newItems.get(i));
                        LOGGER.log(Level.FINE, "adding " + newItems.get(i));
                    }
                } else if (aDiff.getType() == Difference.ResultType.DELETED)
                {
                    for (int i = aDiff.getDeletedStart(); i <= aDiff.getDeletedEnd(); i++)
                    {
                        resultRemovedItems.add(modelCsList.get(i));
                        LOGGER.log(Level.FINE, "removing " + modelCsList.get(i));
                    }
                } else
                {
                    // Then it's changed
                    int d = aDiff.getDeletedStart();
                    int a = aDiff.getAddedStart();
                    do
                    {
                        resultMapChangedChordSymbols.put(modelCsList.get(d), newItems.get(a).getData());
                        LOGGER.log(Level.FINE, "changing " + modelCsList.get(d) + " to " + newItems.get(a));
                        d++;
                        a++;
                    } while (d <= aDiff.getDeletedEnd());
                }
            }
        }

        // To avoid one of a section fields accidentally get the focus on next dialog show
        // and have the text cleared by listener
        jtfChordSymbols.requestFocusInWindow();
        exitedOk = true;
        setVisible(false);
    }

    private void actionCancel()
    {
        // To avoid one of a section fields accidentally get the focus on next dialog show
        // and have the text cleared by listener        
        jtfChordSymbols.requestFocusInWindow();
        exitedOk = false;
        setVisible(false);
        cleanup();
    }

    /**
     * Undo a Text edit.
     */
    private void actionUndo()
    {
        if (!undoManager.canUndo())
        {
            return;
        }

        try
        {
            undoManager.undo();
        } catch (CannotUndoException ex)
        {
        }
    }

    /**
     * Redo a Text edit.
     */
    private void actionRedo()
    {
        if (!undoManager.canRedo())
        {
            return;
        }

        try
        {
            undoManager.redo();
        } catch (CannotUndoException ex)
        {
        }
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

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ctrl Z"), "actionUndo");
        contentPane.getActionMap().put("actionUndo", new AbstractAction("Undo")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionUndo();
            }
        });
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ctrl Y"), "actionRedo");
        contentPane.getActionMap().put("actionRedo", new AbstractAction("Redo")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionRedo();
            }
        });
        return contentPane;
    }

    private void selectChordSymbol(CLI_ChordSymbol item)
    {
        int index = modelCsList.indexOf(item);
        assert index >= 0 : " modelCsList=" + modelCsList + " item=" + item;
        String[] rawStrings = jtfChordSymbols.getText().split("\\s+");
        int start = 0;
        for (int i = 0; i < index; i++)
        {
            start += rawStrings[i].length() + 1;  // +1 for space separation
        }
        int end = start + rawStrings[index].length();
        jtfChordSymbols.select(start, end);
    }

    private void notifyError(String msg)
    {
        NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
        DialogDisplayer.getDefault().notify(d);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents()
   {

      jtfTimeSignature = new javax.swing.JTextField();
      jtfTimeSignature.getDocument().addUndoableEditListener(undoManager);

      jtfChordSymbols = new NoSelectOnFocusGainedJTF();
      jtfChordSymbols.getDocument().addUndoableEditListener(undoManager);
      jtfSectionName = new javax.swing.JTextField();
      jtfSectionName.getDocument().addUndoableEditListener(undoManager);
      jLabel1 = new javax.swing.JLabel();
      jLabel2 = new javax.swing.JLabel();

      setModal(true);
      setResizable(false);
      addComponentListener(new java.awt.event.ComponentAdapter()
      {
         public void componentShown(java.awt.event.ComponentEvent evt)
         {
            formComponentShown(evt);
         }
      });

      jtfTimeSignature.setToolTipText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.jtfTimeSignature.toolTipText")); // NOI18N
      jtfTimeSignature.addFocusListener(new java.awt.event.FocusAdapter()
      {
         public void focusGained(java.awt.event.FocusEvent evt)
         {
            jtfTimeSignatureFocusGained(evt);
         }
      });
      jtfTimeSignature.addKeyListener(new java.awt.event.KeyAdapter()
      {
         public void keyPressed(java.awt.event.KeyEvent evt)
         {
            jtfTimeSignatureKeyPressed(evt);
         }
      });

      jtfChordSymbols.setToolTipText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.jtfChordSymbols.toolTipText")); // NOI18N

      jtfSectionName.setToolTipText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.jtfSectionName.toolTipText")); // NOI18N
      jtfSectionName.addFocusListener(new java.awt.event.FocusAdapter()
      {
         public void focusGained(java.awt.event.FocusEvent evt)
         {
            jtfSectionNameFocusGained(evt);
         }
      });
      jtfSectionName.addKeyListener(new java.awt.event.KeyAdapter()
      {
         public void keyPressed(java.awt.event.KeyEvent evt)
         {
            jtfSectionNameKeyPressed(evt);
         }
      });

      jLabel1.setText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.jLabel1.text")); // NOI18N

      jLabel2.setText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.jLabel2.text")); // NOI18N

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
                  .addComponent(jtfSectionName, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                  .addComponent(jLabel2)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jtfTimeSignature, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addComponent(jtfChordSymbols))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jtfChordSymbols, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jtfSectionName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(jtfTimeSignature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(jLabel1)
               .addComponent(jLabel2))
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

    private void formComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_formComponentShown
    {//GEN-HEADEREND:event_formComponentShown
        focusOnShow.requestFocusInWindow();
    }//GEN-LAST:event_formComponentShown

    private void jtfSectionNameFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_jtfSectionNameFocusGained
    {//GEN-HEADEREND:event_jtfSectionNameFocusGained
        jtfSectionName.selectAll();
    }//GEN-LAST:event_jtfSectionNameFocusGained

    private void jtfTimeSignatureFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_jtfTimeSignatureFocusGained
    {//GEN-HEADEREND:event_jtfTimeSignatureFocusGained
        jtfTimeSignature.selectAll();
    }//GEN-LAST:event_jtfTimeSignatureFocusGained

    private void jtfSectionNameKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtfSectionNameKeyPressed
    {//GEN-HEADEREND:event_jtfSectionNameKeyPressed
        jtfSectionName.setForeground(saveSectionFieldsForeground);
    }//GEN-LAST:event_jtfSectionNameKeyPressed

    private void jtfTimeSignatureKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtfTimeSignatureKeyPressed
    {//GEN-HEADEREND:event_jtfTimeSignatureKeyPressed
        jtfTimeSignature.setForeground(saveSectionFieldsForeground);
    }//GEN-LAST:event_jtfTimeSignatureKeyPressed

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JTextField jtfSectionName;
   private javax.swing.JTextField jtfChordSymbols;
   private javax.swing.JTextField jtfTimeSignature;
   // End of variables declaration//GEN-END:variables

}
