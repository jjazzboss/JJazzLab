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

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.cl_editorimpl.ItemsTransferable;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editorimpl.BarsTransferable;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.importers.api.TextReader;
import org.jjazz.song.api.Song;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.PasteAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.actions.SystemAction;

/**
 * Paste items chordsymbols or sections, possibly across songs, also manage the
 * case of pasting a copied string representing a song.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.paste")
@ActionRegistration(displayName = "#CTL_Paste", lazy = false)
@ActionReferences(
    {
        @ActionReference(path = "Actions/Section", position = 1200),
        @ActionReference(path = "Actions/ChordSymbol", position = 1200),
        @ActionReference(path = "Actions/Bar", position = 1200),
        @ActionReference(path = "Actions/BarAnnotation", position = 1020)
    })
public class Paste extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, FlavorListener
{

    private static final List<DataFlavor> SUPPORTED_FLAVORS = Arrays.asList(ItemsTransferable.DATA_FLAVOR, BarsTransferable.DATA_FLAVOR, DataFlavor.stringFlavor);
    private Lookup context;
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getString(getClass(), "CTL_Paste");
    protected static final Logger LOGGER = Logger.getLogger(Paste.class.getName());

    public Paste()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Paste(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        Icon icon = SystemAction.get(PasteAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_V));

        // Listen to clipboard contents changes        
        // Use WeakListener because no simple way to know when to remove listener
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        FlavorListener weakListener = WeakListeners.create(FlavorListener.class, this, clipboard);
        clipboard.addFlavorListener(weakListener);


        CL_SelectionUtilities selection = cap.getSelection();
        selectionChange(selection);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Paste(context);
    }

    @SuppressWarnings(
        {
            "rawtypes",
            "unchecked"
        })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet targetCls = selection.getChordLeadSheet();
        int targetBarIndex = selection.getMinBarIndex();
        assert targetBarIndex >= 0;


        DataFlavor df = getCurrentSupportedFlavor();
        if (df == null)
        {
            return;   // For robustness: should not be there normally if enabled state was updated correctly
        }

        Object data;
        try
        {
            data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(df);
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() getSystemClipboard().getData() exception={0}", ex.getMessage());
            return;
        }


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(targetCls);
        um.startCEdit(undoText);


        List<ChordLeadSheetItem> items = new ArrayList<>();  // Default do nothing
        int nbInsertBars = 0;   // Default do nothing


        if (df == BarsTransferable.DATA_FLAVOR)
        {
            BarsTransferable.Data dataBars = (BarsTransferable.Data) data;
            items = dataBars.getItemsCopy(targetBarIndex);
            nbInsertBars = dataBars.getBarRange().size();


        }
        else if (df == ItemsTransferable.DATA_FLAVOR)
        {
            ItemsTransferable.Data dataItems = (ItemsTransferable.Data) data;
            items = dataItems.getItemsCopy(targetBarIndex);

        }
        else if (df == DataFlavor.stringFlavor)
        {
            String text = (String) data;
            TextReader tr = new TextReader(text);
            Song song = tr.readSong();
            if (song != null)
            {
                var cls = song.getChordLeadSheet();
                for (var item : cls.getItems())
                {
                    items.add(item.getCopy(item.getPosition().getMoved(targetBarIndex, 0)));
                }
                nbInsertBars = cls.getSizeInBars();
            }
        }
        else
        {
            throw new IllegalStateException("df=" + df);
        }


        // Insert new bars if required
        if (nbInsertBars > 0)
        {
            if (targetBarIndex >= targetCls.getSizeInBars())
            {
                // Resize
                try
                {
                    targetCls.setSizeInBars(targetBarIndex + nbInsertBars);
                }
                catch (UnsupportedEditException ex)
                {
                    // Should never happen when resizing bigger
                    String msg = "Impossible to resize.\n" + ex.getLocalizedMessage();
                    msg += "\n" + ex.getLocalizedMessage();
                    um.handleUnsupportedEditException(undoText, msg);
                    return;
                }
            }
            else
            {
                // Insert bars
                targetCls.insertBars(targetBarIndex, nbInsertBars);
            }
        }


        // Add the items
        for (ChordLeadSheetItem<?> item : items)
        {
            int barIndex = item.getPosition().getBar();

            // Items which arrive after end of leadsheet are skipped.
            if (barIndex < targetCls.getSizeInBars())
            {
                if (item instanceof CLI_Section itemSection)
                {
                    // We need a new copy to make sure the new section name is generated with
                    // the possible previous sections added to the chordleadsheet.
                    // Otherwise possible name clash if e.g. bridge1 and bridge2 in the buffer,
                    // bridge1->bridge3, bridge2->bridge3.
                    CLI_Section newSection = (CLI_Section) itemSection.getCopy(null, targetCls);
                    CLI_Section curSection = targetCls.getSection(barIndex);
                    try
                    {
                        if (curSection.getPosition().getBar() != barIndex)
                        {
                            // There is no section on target bar
                            targetCls.addSection(newSection);
                        }
                        else
                        {
                            // There is a section on target bar, directly update existing section
                            targetCls.setSectionName(curSection, newSection.getData().getName());
                            targetCls.setSectionTimeSignature(curSection, newSection.getData().getTimeSignature());
                        }
                    }
                    catch (UnsupportedEditException ex)
                    {
                        String msg = ResUtil.getString(getClass(), "Err_Paste", newSection);
                        msg += "\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(undoText, msg);
                        return;
                    }
                }
                else
                {
                    // Simple
                    targetCls.addItem(item);
                }
            }
        }


        um.endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;

        if (!selection.isEmpty())
        {
            DataFlavor df = getCurrentSupportedFlavor();
            if (df == BarsTransferable.DATA_FLAVOR || df == DataFlavor.stringFlavor)
            {
                b = selection.isBarSelected();
            }
            else if (df == ItemsTransferable.DATA_FLAVOR)
            {
                b = selection.isBarSelectedWithinCls();
            }
            else
            {
                // Nothing interesting for us
                // Do nothing
            }
        }

        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize
    )
    {
        selectionChange(cap.getSelection());
    }

    // =================================================================================================
    // FlavorListener
    // =================================================================================================    
    @Override
    public void flavorsChanged(FlavorEvent e)
    {
        selectionChange(cap.getSelection());
    }
    // =================================================================================================
    // Private
    // =================================================================================================    

    /**
     * Get the first supported DataFlavor available in the system clipboard.
     *
     * @return Can be null
     */
    private DataFlavor getCurrentSupportedFlavor()
    {
        List<DataFlavor> dataFlavors = new ArrayList<>();
        try
        {
            dataFlavors = Arrays.asList(Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors());
        }
        catch (IllegalStateException e)
        {
            // If clipboard is unavailable
            // Do nothing
        }
        for (DataFlavor df : SUPPORTED_FLAVORS)
        {
            if (dataFlavors.contains(df))
            {
                return df;
            }
        }
        return null;
    }


}
