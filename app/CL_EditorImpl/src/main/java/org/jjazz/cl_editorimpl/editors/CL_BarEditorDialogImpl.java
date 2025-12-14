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
package org.jjazz.cl_editorimpl.editors;

import com.google.common.base.Preconditions;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.text.DefaultCaret;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.spi.CL_BarEditorDialog;
import org.jjazz.cl_editor.spi.Preset;
import org.jjazz.harmony.api.Note;
import org.jjazz.uiutilities.api.UIUtilities;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Diff;
import org.jjazz.utilities.api.Diff.Difference;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = CL_BarEditorDialog.class)
public class CL_BarEditorDialogImpl extends CL_BarEditorDialog
{

    /**
     * The ChordLeadSheet which is edited.
     */
    private ChordLeadSheet model;
    /**
     * The list of ChordSymbol in the model.
     */
    private List<CLI_ChordSymbol> modelCsList;
    /**
     * The Section in the model.
     */
    private CLI_Section modelSection;
    private CLI_BarAnnotation modelBarAnnotation;
    /**
     * The barIndex for this dialog.
     */
    private int barIndex;
    private CLI_Section resultSection;
    private final List<ChordLeadSheetItem> resultAddedItems;
    private final List<ChordLeadSheetItem> resultRemovedItems;
    private final HashMap<ChordLeadSheetItem, Object> resultMapChangedItems;
    /**
     * Undo manager for the text edits
     */
    private final UndoManager undoManager = new UndoManager();
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
    private String saveAnnotationText;
    private boolean swing;
    private int displayTransposition;
    private final JScrollPane sp_annotation;
    private final JTextArea ta_annotation;
    private final JLabel lbl_helpAnnotation;
    private static final Logger LOGGER = Logger.getLogger(CL_BarEditorDialogImpl.class.getSimpleName());

    public CL_BarEditorDialogImpl()
    {
        initComponents();

        displayTransposition = 0;

        // Prepare the annotation components
        ta_annotation = new JTextArea();
        ta_annotation.setRows(3);
        ta_annotation.setLineWrap(true);
        ta_annotation.setToolTipText(pnl_annotations.getToolTipText());
        lbl_annotation.setToolTipText(pnl_annotations.getToolTipText());
        ta_annotation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_ENTER),
                "validateDialog");
        ta_annotation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("alt ENTER"), "validateDialog");
        ta_annotation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift ENTER"), "validateDialog");
        ta_annotation.getActionMap().put("validateDialog", UIUtilities.getAction(ae -> actionOK()));
        sp_annotation = new JScrollPane(ta_annotation);
        lbl_helpAnnotation = new JLabel();
        UIUtilities.changeFontSize(lbl_helpAnnotation, -2f);
        lbl_helpAnnotation.setText(pnl_annotations.getToolTipText());


        // Mac OSX automatically does a select all upon focus gain: this generates problem see Issue #97
        // This is hack to make sure the default behavior is used, even on Mac OSX
        jtfChordSymbols.setCaret(new DefaultCaret());


        saveSectionFieldsForeground = jtfSectionName.getForeground();
        resultSection = null;
        resultAddedItems = new ArrayList<>();
        resultRemovedItems = new ArrayList<>();
        resultMapChangedItems = new HashMap<>();
    }

    @Override
    public void cleanup()
    {
        model = null;
        modelSection = null;
        modelCsList = null;
        resultSection = null;
        resultAddedItems.clear();
        resultRemovedItems.clear();
        resultMapChangedItems.clear();
    }

    @Override
    public void preset(final Preset preset, ChordLeadSheet cls, int bar, boolean swng)
    {
        Objects.requireNonNull(preset);
        Objects.requireNonNull(cls);
        Preconditions.checkElementIndex(bar, cls.getSizeInBars(), "bar");

        cleanup();
        model = cls;
        barIndex = bar;
        modelCsList = getModelChordSymbols(model, barIndex, displayTransposition);
        modelSection = model.getSection(barIndex);
        modelBarAnnotation = model.getBarFirstItem(barIndex, CLI_BarAnnotation.class, cli -> true);   // Can be null
        swing = swng;
        boolean isSectionInBar = (modelSection.getPosition().getBar() == barIndex);

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
        var f = jtfChordSymbols.getFont();
        f = f.deriveFont(displayTransposition==0 ? Font.PLAIN : Font.ITALIC);
        jtfChordSymbols.setFont(f);
        jtfChordSymbols.setText(CLI_ChordSymbol.toStringNoPosition(modelCsList));
        saveCsText = jtfChordSymbols.getText();


        // Update the annotation field
        saveAnnotationText = modelBarAnnotation == null ? "" : modelBarAnnotation.getData();
        ta_annotation.setText(saveAnnotationText);
        ta_annotation.setCaretPosition(0);
        setAnnotationPanelExpanded(!saveAnnotationText.isBlank());

        String title = ResUtil.getString(getClass(), "CL_BarEditorDialogImpl.CTL_Bar")
                + " " + (barIndex + 1)
                + " - " + modelSection.getData().getName()
                + " " + modelSection.getData().getTimeSignature();
        setTitle(title);
        undoManager.discardAllEdits();


        // Specific actions depending on presets
        switch (preset.type())
        {
            case BarEdit ->
            {
                focusOnShow = jtfChordSymbols;
                if (preset.key() != (char) 0)
                {
                    // Append char at the end, with a leading space if required
                    String text = jtfChordSymbols.getText().trim();
                    String space = text.isEmpty() ? "" : " ";
                    text = text + space + Character.toUpperCase(preset.key());
                    jtfChordSymbols.setText(text);


                    // Only on MacOSX, the inserted char ends up being selected! 
                    // This make sure there is no selection
                    jtfChordSymbols.setCaretPosition(text.length());
                    jtfChordSymbols.moveCaretPosition(text.length());
                } else
                {
                    jtfChordSymbols.selectAll();
                }
            }
            case ChordSymbolEdit ->
            {
                focusOnShow = jtfChordSymbols;
                int indexInBar = model.getItems(barIndex, barIndex, CLI_ChordSymbol.class).indexOf(preset.item());  // do not use modelCsList, it might have been transposed
                assert indexInBar >= 0;
                selectChordSymbol(indexInBar);
                if (preset.key() != (char) 0)
                {
                    jtfChordSymbols.replaceSelection("" + Character.toUpperCase(preset.key()));
                }
            }
            case SectionNameEdit ->
            {
                focusOnShow = jtfSectionName;
                jtfSectionName.selectAll();
            }
            case TimeSignatureEdit ->
            {
                focusOnShow = jtfTimeSignature;
                jtfTimeSignature.selectAll();
            }
            case AnnotationEdit ->
            {
                focusOnShow = ta_annotation;
                setAnnotationPanelExpanded(true);
                ta_annotation.selectAll();
            }
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
    public List<ChordLeadSheetItem> getAddedItems()
    {
        return resultAddedItems;
    }

    @Override
    public List<ChordLeadSheetItem> getRemovedItems()
    {
        return resultRemovedItems;
    }

    @Override
    public Map<ChordLeadSheetItem, Object> getChangedItems()
    {
        return resultMapChangedItems;
    }
    // ------------------------------------------------------------------------------
    // DisplayTransposableRenderer interface
    // ------------------------------------------------------------------------------    

    @Override
    public void setDisplayTransposition(int dt)
    {
        displayTransposition = dt;
    }

    @Override
    public int getDisplayTransposition()
    {
        return displayTransposition;
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
        String strAnnotation = ta_annotation.getText().trim();
        boolean isTimeSignatureChanged = !saveTsText.equals(strSignature);
        boolean isSectionChanged = !saveSectionText.equals(strSection);
        boolean isChordsChanged = !saveCsText.equals(strChords);
        boolean isSectionInBar = (modelSection.getPosition().getBar() == barIndex);
        boolean isAnnotationChanged = !saveAnnotationText.equals(strAnnotation);

        if (isSectionChanged || isTimeSignatureChanged)
        {
            // Both fields must be filled
            if (strSection.length() == 0 || strSignature.length() == 0)
            {
                notifyError(ResUtil.getString(getClass(), "CL_BarEditorDialogImpl.ERR_IncompleteSection"));
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
                notifyError(ResUtil.getString(getClass(), "CL_BarEditorDialogImpl.ERR_InvalidTimeSignature"));
                jtfTimeSignature.selectAll();
                jtfTimeSignature.requestFocusInWindow();
                return;
            }

            // Check section name is valid
            if (model.getSection(strSection) != null && !(isSectionInBar && modelSection.getData().getName().equals(strSection)))
            {
                notifyError(ResUtil.getString(getClass(), "CL_BarEditorDialogImpl.ERR_DuplicateSectionName"));
                jtfSectionName.selectAll();
                jtfSectionName.requestFocusInWindow();
                return;
            }

            // Finally update the result
            resultSection = CLI_Factory.getDefault().createSection(strSection, ts, barIndex, null);

        }

        if (isChordsChanged)
        {
            List<CLI_ChordSymbol> newItems;
            try
            {
                newItems = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(strChords, null, model, barIndex, swing);
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

            // Analyze changes to update resultMapChangedItems, resultAddedItems, resultRemovedItems
            diffChords(modelCsList, newItems);

            if (displayTransposition != 0)
            {
                // Update resultMapChangedItems, resultAddedItems, resultRemovedItems
                untranspose();
            }
        }

        if (isAnnotationChanged)
        {
            if (modelBarAnnotation == null && !strAnnotation.isBlank())
            {
                // Add an item
                var cliBa = CLI_Factory.getDefault().createBarAnnotation(strAnnotation, barIndex);
                resultAddedItems.add(cliBa);
            } else if (modelBarAnnotation != null && strAnnotation.isBlank())
            {
                // Remove the item
                resultRemovedItems.add(modelBarAnnotation);
            } else
            {
                // Annotation was changed
                resultMapChangedItems.put(modelBarAnnotation, strAnnotation);
            }
        }


        // To avoid one of a section fields accidentally get the focus on next dialog show
        // and have the text cleared by listener
        jtfChordSymbols.requestFocusInWindow();
        exitedOk = true;
        setVisible(false);
    }

    /**
     * Analyze which chords are changed/added/removed and update global variables accordingly: resultMapChangedItems, resultAddedItems, resultRemovedItems.
     *
     * @param csListOld
     * @param csListNew
     */
    private void diffChords(List<CLI_ChordSymbol> csListOld, List<CLI_ChordSymbol> csListNew)
    {
        LOGGER.log(Level.FINE, "diffChords() csListOld={0} csListNew={1}", new Object[]
        {
            csListOld, csListNew
        });

        List<Difference> diffResult = Diff.diff(csListOld, csListNew,
                (cliCs1, cliCs2) -> cliCs1.getData().equals(cliCs2.getData()) ? 0 : 1);


        for (Difference aDiff : diffResult)
        {
            if (null == aDiff.getType())
            {
                // Then it's changed
                int d = aDiff.getDeletedStart();
                int a = aDiff.getAddedStart();
                do
                {
                    resultMapChangedItems.put(csListOld.get(d), csListNew.get(a).getData());
                    LOGGER.log(Level.FINE, "changing {0} to {1}", new Object[]
                    {
                        csListOld.get(d), csListNew.get(a)
                    });
                    d++;
                    a++;
                } while (d <= aDiff.getDeletedEnd());
            } else
            {
                switch (aDiff.getType())
                {
                    case ADDED ->
                    {
                        for (int i = aDiff.getAddedStart(); i <= aDiff.getAddedEnd(); i++)
                        {
                            resultAddedItems.add(csListNew.get(i));
                            LOGGER.log(Level.FINE, "adding {0}", csListNew.get(i));
                        }
                    }
                    case DELETED ->
                    {
                        for (int i = aDiff.getDeletedStart(); i <= aDiff.getDeletedEnd(); i++)
                        {
                            resultRemovedItems.add(csListOld.get(i));
                            LOGGER.log(Level.FINE, "removing {0}", csListOld.get(i));
                        }
                    }
                    default ->
                    {
                        // Then it's changed
                        int d = aDiff.getDeletedStart();
                        int a = aDiff.getAddedStart();
                        do
                        {
                            resultMapChangedItems.put(csListOld.get(d), csListNew.get(a).getData());
                            LOGGER.log(Level.FINE, "changing {0} to {1}", new Object[]
                            {
                                csListOld.get(d), csListNew.get(a)
                            });
                            d++;
                            a++;
                        } while (d <= aDiff.getDeletedEnd());
                    }
                }
            }
        }
    }

    /**
     * Update results to cancel the display transposition.
     */
    private void untranspose()
    {
        // Added chords
        for (var it = resultAddedItems.listIterator(); it.hasNext();)
        {
            if (it.next() instanceof CLI_ChordSymbol cliCs)
            {
                var ecs = cliCs.getData();
                it.set(cliCs.getCopy(ecs.getTransposedChordSymbol(-displayTransposition, null), null));
            }
        }


        // Removed chords
        var cliCsList = model.getItems(barIndex, barIndex, CLI_ChordSymbol.class);      // original model chord, not transposed
        for (var it = resultRemovedItems.listIterator(); it.hasNext();)
        {
            if (it.next() instanceof CLI_ChordSymbol cliCs)
            {
                int index = modelCsList.indexOf(cliCs);             // transposed
                assert index >= 0 : "cliCs=" + cliCs + " modelCsList=" + modelCsList;
                it.set(cliCsList.get(index));
            }
        }


        // Changed chords
        for (var key : resultMapChangedItems.keySet().toArray(ChordLeadSheetItem[]::new))
        {
            if (key instanceof CLI_ChordSymbol cliCs)
            {
                ExtChordSymbol ecsMapped = (ExtChordSymbol) resultMapChangedItems.get(cliCs);

                int index = modelCsList.indexOf(cliCs);             // transposed
                assert index >= 0 : "cliCs=" + cliCs + " modelCsList=" + modelCsList;
                var cliCsOrig = cliCsList.get(index);

                resultMapChangedItems.remove(cliCs);
                resultMapChangedItems.put(cliCsOrig, ecsMapped.getTransposedChordSymbol(-displayTransposition, null));
            }
        }
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

    /**
     *
     * @param indexInBar Index of the chord in the bar
     */
    private void selectChordSymbol(int indexInBar)
    {
        String[] rawStrings = jtfChordSymbols.getText().split("\\s+");
        int start = 0;
        for (int i = 0; i < indexInBar; i++)
        {
            start += rawStrings[i].length() + 1;  // +1 for space separation
        }
        int end = start + rawStrings[indexInBar].length();
        jtfChordSymbols.select(start, end);
    }

    private void setAnnotationPanelExpanded(boolean b)
    {
        if (b == isAnnotationPanelExpanded())
        {
            return;
        }
        fbtn_expand.setSelected(b);
        if (b)
        {
            pnl_annotations.add(sp_annotation, BorderLayout.CENTER);
            pnl_annotations.add(lbl_helpAnnotation, BorderLayout.SOUTH);
            pnl_annotations.revalidate();
            ta_annotation.requestFocusInWindow();
        } else
        {
            jtfChordSymbols.requestFocusInWindow();
            pnl_annotations.remove(sp_annotation);
            pnl_annotations.remove(lbl_helpAnnotation);
            pnl_annotations.revalidate();
        }
        pack();
    }

    private boolean isAnnotationPanelExpanded()
    {
        return pnl_annotations.getComponentCount() > 0;
    }

    private void notifyError(String msg)
    {
        NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
        DialogDisplayer.getDefault().notify(d);
    }

    /**
     * Get the chord symbols possibly transposed.
     *
     * @param cls
     * @param bar
     * @param t   transposition
     * @return
     */
    private List<CLI_ChordSymbol> getModelChordSymbols(ChordLeadSheet cls, int bar, int t)
    {
        List<CLI_ChordSymbol> res = cls.getItems(bar, bar, CLI_ChordSymbol.class);
        if (!res.isEmpty() && t != 0)
        {
            res = res.stream()
                    .map(cliCs -> (CLI_ChordSymbol) cliCs.getCopy(cliCs.getData().getTransposedChordSymbol(t, null), null))
                    .toList();
        }
        return res;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jtfTimeSignature = new javax.swing.JTextField();
        jtfTimeSignature.getDocument().addUndoableEditListener(undoManager);
        jtfChordSymbols = new javax.swing.JTextField();
        jtfChordSymbols.getDocument().addUndoableEditListener(undoManager);
        jtfSectionName = new javax.swing.JTextField();
        jtfSectionName.getDocument().addUndoableEditListener(undoManager);
        lbl_section = new javax.swing.JLabel();
        lbl_timeSig = new javax.swing.JLabel();
        lbl_annotation = new javax.swing.JLabel();
        fbtn_expand = new org.jjazz.flatcomponents.api.FlatToggleButton();
        pnl_annotations = new javax.swing.JPanel();

        setModal(true);
        setResizable(false);
        addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentShown(java.awt.event.ComponentEvent evt)
            {
                formComponentShown(evt);
            }
        });

        jtfTimeSignature.setColumns(3);
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

        lbl_section.setText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.lbl_section.text")); // NOI18N

        lbl_timeSig.setText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.lbl_timeSig.text")); // NOI18N

        lbl_annotation.setText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.lbl_annotation.text")); // NOI18N

        fbtn_expand.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/cl_editorimpl/editors/resources/arrow_collapsed.png"))); // NOI18N
        fbtn_expand.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/cl_editorimpl/editors/resources/arrow_expanded.png"))); // NOI18N
        fbtn_expand.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_expandActionPerformed(evt);
            }
        });

        pnl_annotations.setToolTipText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.pnl_annotations.toolTipText")); // NOI18N
        pnl_annotations.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnl_annotations, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jtfChordSymbols, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(lbl_section)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtfSectionName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lbl_timeSig)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtfTimeSignature))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(lbl_annotation)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbtn_expand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jtfChordSymbols, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_section)
                    .addComponent(jtfSectionName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_timeSig)
                    .addComponent(jtfTimeSignature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_annotation)
                    .addComponent(fbtn_expand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_annotations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private void fbtn_expandActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_expandActionPerformed
    {//GEN-HEADEREND:event_fbtn_expandActionPerformed
        setAnnotationPanelExpanded(fbtn_expand.isSelected());
    }//GEN-LAST:event_fbtn_expandActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.flatcomponents.api.FlatToggleButton fbtn_expand;
    private javax.swing.JTextField jtfChordSymbols;
    private javax.swing.JTextField jtfSectionName;
    private javax.swing.JTextField jtfTimeSignature;
    private javax.swing.JLabel lbl_annotation;
    private javax.swing.JLabel lbl_section;
    private javax.swing.JLabel lbl_timeSig;
    private javax.swing.JPanel pnl_annotations;
    // End of variables declaration//GEN-END:variables


}
