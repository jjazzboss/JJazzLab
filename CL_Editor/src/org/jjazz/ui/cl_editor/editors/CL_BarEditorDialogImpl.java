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
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.text.DefaultCaret;
import org.jjazz.chordsymboltextinput.api.ChordSymbolTextInput;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.ui.cl_editor.spi.CL_BarEditorDialog;
import org.jjazz.ui.cl_editor.spi.Preset;
import org.jjazz.ui.utilities.api.Utilities;
import static org.jjazz.ui.utilities.api.Utilities.getGenericControlKeyStroke;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.diff.api.DiffProvider;
import org.jjazz.util.diff.api.Difference;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

public class CL_BarEditorDialogImpl extends CL_BarEditorDialog
{

    static private final Icon ICON_COLLAPSED = new ImageIcon(CL_BarEditorDialogImpl.class.getResource("resources/arrow_collapsed.png"));
    static private final Icon ICON_EXPANDED = new ImageIcon(CL_BarEditorDialogImpl.class.getResource("resources/arrow_expanded.png"));

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
    private final JScrollPane sp_annotation;
    private final JTextArea ta_annotation;
    private static final Logger LOGGER = Logger.getLogger(CL_BarEditorDialogImpl.class.getSimpleName());

    private CL_BarEditorDialogImpl()
    {
        initComponents();

        // The annotatio component
        ta_annotation = new JTextArea();
        ta_annotation.setRows(2);
        ta_annotation.setLineWrap(true);
        ta_annotation.setToolTipText(pnl_annotations.getToolTipText());
        lbl_annotation.setToolTipText(pnl_annotations.getToolTipText());
        // Make ctrl+ENTER validate the dialog when ta_annotation is focused
        ta_annotation.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_ENTER), "validateDialog");
        ta_annotation.getActionMap().put("validateDialog", Utilities.getAction(ae -> actionOK()));
        sp_annotation = new JScrollPane(ta_annotation);


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
    public void preset(final Preset preset, ChordLeadSheet cls, int barIndx, boolean swng)
    {
        if (preset == null || cls == null || barIndx < 0 || barIndx >= cls.getSizeInBars())
        {
            throw new IllegalArgumentException("preset=" + preset + " cls=" + cls + " barIndx=" + barIndx + " swing=" + swing);
        }

        cleanup();
        model = cls;
        barIndex = barIndx;
        modelCsList = model.getItems(barIndex, barIndex, CLI_ChordSymbol.class);
        modelSection = model.getSection(barIndex);
        modelBarAnnotation = model.getBarFirstItem(barIndex, CLI_BarAnnotation.class, cli -> true);   // Can be null
        swing = swng;
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


        // Update the annotation field
        saveAnnotationText = modelBarAnnotation == null ? "" : modelBarAnnotation.getData();
        ta_annotation.setText(saveAnnotationText);
        ta_annotation.setCaretPosition(0);
        setAnnotationPanelExpanded(!saveAnnotationText.isBlank());


        setTitle(ResUtil.getString(getClass(),
                "CL_BarEditorDialogImpl.CTL_Bar") + " " + (barIndx + 1) + " - " + modelSection.getData().getName() + " " + modelSection.getData().getTimeSignature());
        undoManager.discardAllEdits();

        // Specific actions depending on presets
        switch (preset.getPresetType())
        {
            case BarEdit ->
            {
                focusOnShow = jtfChordSymbols;
                if (preset.getKey() != (char) 0)
                {
                    // Append char at the end, with a leading space if required
                    String text = jtfChordSymbols.getText().trim();
                    String space = text.isEmpty() ? "" : " ";
                    text = text + space + Character.toUpperCase(preset.getKey());
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
                CLI_ChordSymbol item = (CLI_ChordSymbol) preset.getItem();
                selectChordSymbol(item);
                if (preset.getKey() != (char) 0)
                {
                    jtfChordSymbols.replaceSelection("" + Character.toUpperCase(preset.getKey()));
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
            resultSection = CLI_Factory.getDefault().createSection(model, strSection, ts, barIndex);

        }

        if (isChordsChanged)
        {
            List<CLI_ChordSymbol> newItems;
            try
            {
                newItems = ChordSymbolTextInput.toCLI_ChordSymbolsNoPosition(strChords, barIndex, model, swing);
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
            LOGGER.log(Level.FINE, "Diff model={0} newItems={1}", new Object[]
            {
                modelCsList, newItems
            });
            for (Difference aDiff : diffResult)
            {
                if (aDiff.getType() == Difference.ResultType.ADDED)
                {
                    for (int i = aDiff.getAddedStart(); i <= aDiff.getAddedEnd(); i++)
                    {
                        resultAddedItems.add(newItems.get(i));
                        LOGGER.log(Level.FINE, "adding {0}", newItems.get(i));
                    }
                } else if (aDiff.getType() == Difference.ResultType.DELETED)
                {
                    for (int i = aDiff.getDeletedStart(); i <= aDiff.getDeletedEnd(); i++)
                    {
                        resultRemovedItems.add(modelCsList.get(i));
                        LOGGER.log(Level.FINE, "removing {0}", modelCsList.get(i));
                    }
                } else
                {
                    // Then it's changed
                    int d = aDiff.getDeletedStart();
                    int a = aDiff.getAddedStart();
                    do
                    {
                        resultMapChangedItems.put(modelCsList.get(d), newItems.get(a).getData());
                        LOGGER.log(Level.FINE, "changing {0} to {1}", new Object[]
                        {
                            modelCsList.get(d), newItems.get(a)
                        });
                        d++;
                        a++;
                    } while (d <= aDiff.getDeletedEnd());
                }
            }
        }
        
        if (isAnnotationChanged)
        {
            if (modelBarAnnotation==null && !strAnnotation.isBlank())
            {
                // Add an item
                var cliBa = CLI_Factory.getDefault().createBarAnnotation(model, strAnnotation, barIndex);
                resultAddedItems.add(cliBa);
            } else if (modelBarAnnotation!=null && strAnnotation.isBlank())
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

    private void setAnnotationPanelExpanded(boolean b)
    {
        if (b == isAnnotationPanelExpanded())
        {
            return;
        }
        fbtn_expand.setSelected(b);
        if (b)
        {
            pnl_annotations.add(sp_annotation);
            pnl_annotations.revalidate();
            ta_annotation.requestFocusInWindow();
        } else
        {
            jtfChordSymbols.requestFocusInWindow();
            pnl_annotations.remove(sp_annotation);
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
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
        fbtn_expand = new org.jjazz.ui.flatcomponents.api.FlatToggleButton();
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

        fbtn_expand.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/cl_editor/editors/resources/arrow_collapsed.png"))); // NOI18N
        fbtn_expand.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/ui/cl_editor/editors/resources/arrow_expanded.png"))); // NOI18N
        fbtn_expand.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_expandActionPerformed(evt);
            }
        });

        pnl_annotations.setToolTipText(org.openide.util.NbBundle.getMessage(CL_BarEditorDialogImpl.class, "CL_BarEditorDialogImpl.pnl_annotations.toolTipText")); // NOI18N
        pnl_annotations.setLayout(new javax.swing.BoxLayout(pnl_annotations, javax.swing.BoxLayout.LINE_AXIS));

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
    private org.jjazz.ui.flatcomponents.api.FlatToggleButton fbtn_expand;
    private javax.swing.JTextField jtfChordSymbols;
    private javax.swing.JTextField jtfSectionName;
    private javax.swing.JTextField jtfTimeSignature;
    private javax.swing.JLabel lbl_annotation;
    private javax.swing.JLabel lbl_section;
    private javax.swing.JLabel lbl_timeSig;
    private javax.swing.JPanel pnl_annotations;
    // End of variables declaration//GEN-END:variables

}
