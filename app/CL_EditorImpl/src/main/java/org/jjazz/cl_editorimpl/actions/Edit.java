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
package org.jjazz.cl_editorimpl.actions;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.spi.CL_BarEditorDialog;
import org.jjazz.cl_editor.spi.Preset;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.cl_editor.spi.SectionEditorDialog;
import org.jjazz.cl_editor.spi.ChordSymbolEditorDialog;
import org.jjazz.cl_editorimpl.BR_Annotation;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/**
 * Edit the selected item or bar.
 * <p>
 * Use the following services if available in the global lookup: <br>
 * - ChordSymbolEditorDialog <br>
 * - SectionEditorDialog <br>
 * otherwise use default bar editor for edit operations.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.edit")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
    {
        @ActionReference(path = "Actions/Section", position = 100),
        @ActionReference(path = "Actions/ChordSymbol", position = 100),
        @ActionReference(path = "Actions/Bar", position = 100),
        @ActionReference(path = "Actions/BarAnnotation", position = 100)
    })
public class Edit extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("ENTER");

    private final String undoText = ResUtil.getString(getClass(), "CTL_Edit");
    static private final Logger LOGGER = Logger.getLogger(Edit.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_Edit"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES, ListeningTarget.BAR_SELECTION));
    }

    /**
     * Perform the action.
     * <p>
     * If action was triggered by a key press, ae.getActionCommand() provides the key pressed. Use ae.getSource() to get component source of the
     * action eg BarBox, BR_Chords, etc.
     *
     * @param ae
     */
    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        final CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        char key = (char) 0;
        LOGGER.log(Level.FINE, "ae={0}", ae);

        // Is it a chord note ?        
        if (ae != null && ae.getActionCommand().length() == 1)
        {
            char c = ae.getActionCommand().toUpperCase().charAt(0);
            if (c >= 'A' && c <= 'G')
            {
                key = c;
            }
        }

        if (selection.isItemSelected())
        {
            ChordLeadSheetItem<?> item = selection.getSelectedItems().get(0);
            int barIndex = item.getPosition().getBar();
            if (item instanceof CLI_ChordSymbol csItem)
            {
                ChordSymbolEditorDialog dialog = ChordSymbolEditorDialog.getDefault();
                if (dialog != null)
                {
                    // Use specific editor if service is provided
                    editCSWithDialog(dialog, csItem, key, cls, undoText);
                } else
                {
                    // Otherwise use the standard Bar dialog
                    editBarWithDialog(editor, barIndex, new Preset(Preset.Type.ChordSymbolEdit, csItem, key), cls, undoText);
                }
            } else if (item instanceof CLI_Section sectionItem)
            {
                SectionEditorDialog dialog = SectionEditorDialog.getDefault();
                if (dialog != null)
                {
                    // Use specific editor if service is provided               
                    editSectionWithDialog(dialog, sectionItem, key, cls, undoText);
                } else
                {
                    // Otherwise use the standard Bar dialog
                    editBarWithDialog(editor, barIndex, new Preset(Preset.Type.SectionNameEdit, sectionItem, (char) 0), cls, undoText);
                }
            } else if (item instanceof CLI_BarAnnotation cliBa)
            {
                editBarWithDialog(editor, barIndex, new Preset(Preset.Type.AnnotationEdit, cliBa, (char) 0), cls, undoText);
            }
        } else
        {
            // A BarRenderer
            assert selection.isBarSelectedWithinCls() == true : "selection=" + selection;
            int modelBarIndex = selection.getMinBarIndexWithinCls();

            if (ae != null && ae.getSource() instanceof BR_Annotation)
            {
                editBarWithDialog(editor, modelBarIndex, new Preset(Preset.Type.AnnotationEdit, null, key), cls, undoText);
            } else
            {
                editBarWithDialog(editor, modelBarIndex, new Preset(Preset.Type.BarEdit, null, key), cls, undoText);
            }
        }
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b;
        if (selection.isItemSelected())
        {
            b = selection.getSelectedItems().size() == 1;
        } else
        {
            b = selection.getSelectedBarIndexesWithinCls().size() == 1;
        }
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
    }

    static protected void editSectionWithDialog(final SectionEditorDialog dialog, final CLI_Section sectionItem, final char key, final ChordLeadSheet cls,
        String undoText)
    {
        // Use specific editor if service is provided
        Runnable run = () ->
        {
            dialog.preset(sectionItem, key);
            dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            dialog.setVisible(true);
            if (dialog.exitedOk())
            {
                Section newSection = dialog.getNewData();
                assert newSection != null;
                JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
                try
                {
                    cls.setSectionName(sectionItem, newSection.getName());
                    cls.setSectionTimeSignature(sectionItem, newSection.getTimeSignature());
                } catch (UnsupportedEditException ex)
                {

                    String msg = ResUtil.getString(Edit.class, "ERR_ChangeSection", sectionItem.getData());
                    msg += "\n" + ex.getLocalizedMessage();
                    um.abortCEdit(undoText, msg);
                }
            }
        };
        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of random double chars 
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code      
        SwingUtilities.invokeLater(run);
    }

    static protected void editCSWithDialog(final ChordSymbolEditorDialog dialog, final CLI_ChordSymbol csItem, final char key, final ChordLeadSheet cls,
        String undoText)
    {
        Objects.requireNonNull(csItem);
        Objects.requireNonNull(dialog);
        Objects.requireNonNull(cls);

        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of random double chars
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code      
        SwingUtilities.invokeLater(() -> editCSWithDialogImpl(dialog, csItem, key, cls, undoText));
    }

    static private void editCSWithDialogImpl(final ChordSymbolEditorDialog dialog, final CLI_ChordSymbol csItem, final char key, final ChordLeadSheet cls,
        String undoText1)
    {
        Position pos = csItem.getPosition();
        dialog.setDisplayTransposition(PlaybackSettings.getInstance().getChordSymbolsDisplayTransposition());
        dialog.preset("Edit Chord Symbol - " + csItem.getData() + " - bar:" + (pos.getBar() + 1) + " beat:" + pos.getBeatAsUserString(), csItem, key, true);
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.setVisible(true);
        if (dialog.exitedOk())
        {
            ExtChordSymbol oldEcs = csItem.getData();
            ExtChordSymbol newEcs = dialog.getData();
            assert newEcs != null;
            JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
            um.startCEdit(undoText1);
            if (cls.changeItem(csItem, newEcs))
            {
                // Update color if alternate chord was set/unset by user
                if (isAlternateChordSet(oldEcs, newEcs) && isDefaultColorUsed(csItem))
                {
                    CL_EditorClientProperties.setChordSymbolUserColor(csItem, IR_ChordSymbolSettings.getDefault().getSubstituteFontColor());
                } else if (isAlternateChordUnset(oldEcs, newEcs) && isAlternateColorUsed(csItem))
                {
                    CL_EditorClientProperties.setChordSymbolUserColor(csItem, IR_ChordSymbolSettings.getDefault().getColor());
                }
            }
            um.endCEdit(undoText1);
        }
        dialog.cleanup();
    }

    static protected void editBarWithDialog(final CL_Editor editor, final int barIndex, final Preset preset, final ChordLeadSheet cls, String undoText)
    {
        Objects.requireNonNull(editor);
        Objects.requireNonNull(preset);
        Objects.requireNonNull(cls);

        int preNbAnnotations = editor.getSongModel().getChordLeadSheet().getItems(CLI_BarAnnotation.class).size();
        Song song = editor.getSongModel();

        Runnable run = () ->
        {
            // Prepare dialog
            final CL_BarEditorDialog dialog = CL_BarEditorDialog.getDefault();
            boolean swing = isSwing(song, barIndex);
            dialog.setDisplayTransposition(PlaybackSettings.getInstance().getChordSymbolsDisplayTransposition());
            dialog.preset(preset, editor.getModel(), barIndex, swing);
            adjustDialogPosition(dialog, barIndex);
            dialog.setVisible(true);
            LOGGER.fine("editBarWithDialog() right after setVisible(true)");
            if (!dialog.isExitOk())
            {
                dialog.cleanup();
                return;
            }

            JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
            um.startCEdit(undoText);


            // Manage section change
            CLI_Section resultSection = dialog.getSection();
            if (resultSection != null)
            {
                CLI_Section currentSection = cls.getSection(barIndex);
                if (currentSection.getPosition().getBar() == barIndex)
                {
                    // Update existing section
                    cls.setSectionName(currentSection, resultSection.getData().getName());
                    try
                    {
                        // Manage the case where we change initial section, user prompt to apply to whole song
                        SetTimeSignatureActionMenu.changeTimeSignaturePossiblyForWholeSong(cls, resultSection.getData().getTimeSignature(), Arrays.asList(
                            currentSection));
                    } catch (UnsupportedEditException ex)
                    {
                        String msg = ResUtil.getString(Edit.class, "ERR_ChangeSection", resultSection.getData());
                        msg += "\n" + ex.getLocalizedMessage();
                        um.abortCEdit(undoText, msg);
                        // There are other things to do, restart edit
                        um.startCEdit(undoText);
                    }
                } else
                {
                    // Add section
                    try
                    {
                        cls.addSection(resultSection);
                    } catch (UnsupportedEditException ex)
                    {
                        String msg = ResUtil.getString(Edit.class, "ERR_ChangeSection", resultSection.getData());
                        msg += "\n" + ex.getLocalizedMessage();
                        um.abortCEdit(undoText, msg);
                        // There are other things to do, restart edit
                        um.startCEdit(undoText);
                    }
                }
            }


            // Manage added/removed/changed items
            var resultAddedItems = dialog.getAddedItems();
            resultAddedItems.forEach(item -> cls.addItem(item));

            var resultRemovedItems = dialog.getRemovedItems();
            resultRemovedItems.forEach(item -> cls.removeItem(item));

            var mapChanged = dialog.getChangedItems();
            mapChanged.keySet().forEach(cliCs -> cls.changeItem(cliCs, mapChanged.get(cliCs)));

            um.endCEdit(undoText);

            // Go to next bar if something has changed
            boolean change = !resultAddedItems.isEmpty() || !resultRemovedItems.isEmpty() || !mapChanged.isEmpty();
            if (barIndex < cls.getSizeInBars() - 1 && change)
            {
                editor.clearSelection();
                editor.setFocusOnBar(barIndex + 1);
                editor.selectBars(barIndex + 1, barIndex + 1, true);
            }

            dialog.cleanup();


            // Automatically make bar annotations visible if first annotation
            int postNbAnnotations = song.getChordLeadSheet().getItems(CLI_BarAnnotation.class).size();
            if (!CL_EditorClientProperties.isBarAnnotationVisible(song) && preNbAnnotations == 0 && postNbAnnotations == 1)
            {
                CL_EditorClientProperties.setBarAnnotationVisible(song, true);
            }
            if (preNbAnnotations == 0 && postNbAnnotations == 1)
            {
                Analytics.logEvent("Bar annotation first input");
            }


        };

        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of random double chars
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code
        SwingUtilities.invokeLater(run);
    }

    static public boolean isSwing(Song song, int clsBarIndex)
    {
        boolean b = false;
        var cliSection = song.getChordLeadSheet().getSection(clsBarIndex);
        var q = CL_EditorClientProperties.getSectionUserQuantization(cliSection);
        if (q == null)
        {
            // Rely on division of rhythm of first SongPart whose parent section is cliSection
            var d = song.getSongStructure().getSongParts().stream()
                .filter(spt -> spt.getParentSection() == cliSection)
                .map(spt -> spt.getRhythm().getFeatures().division())
                .findFirst()
                .orElse(null);
            if (d != null)
            {
                b = d.isTernary();
            }
        } else
        {
            b = q.isTernary();
        }
        return b;
    }

    static private void adjustDialogPosition(JDialog dialog, int barIndex)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Rectangle r = editor.getBarRectangle(barIndex);
        Point p = r.getLocation();
        SwingUtilities.convertPointToScreen(p, editor);
        int x = p.x - ((dialog.getWidth() - r.width) / 2);
        int y = p.y - dialog.getHeight();
        dialog.setLocation(Math.max(x, 0), Math.max(y, 0));
    }

    static private boolean isAlternateChordSet(ExtChordSymbol oldEcs, ExtChordSymbol newEcs)
    {
        return oldEcs.getAlternateChordSymbol() == null && newEcs.getAlternateChordSymbol() != null;
    }

    static private boolean isAlternateChordUnset(ExtChordSymbol oldEcs, ExtChordSymbol newEcs)
    {
        return oldEcs.getAlternateChordSymbol() != null && newEcs.getAlternateChordSymbol() == null;
    }

    static private boolean isDefaultColorUsed(CLI_ChordSymbol csItem)
    {
        var c = CL_EditorClientProperties.getChordSymbolUserColor(csItem);
        return c == null || IR_ChordSymbolSettings.getDefault().getColor().equals(c);
    }

    static private boolean isAlternateColorUsed(CLI_ChordSymbol csItem)
    {
        var c = CL_EditorClientProperties.getChordSymbolUserColor(csItem);
        return IR_ChordSymbolSettings.getDefault().getSubstituteFontColor().equals(c);
    }
}
