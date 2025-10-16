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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.cl_editorimpl.ItemsTransferable;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editorimpl.BarsTransferable;
import org.jjazz.cl_editor.api.CL_Selection;
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
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;

/**
 * Paste items chordsymbols or sections, possibly across songs, also manage the case of pasting a copied string representing a song.
 */
public class Paste extends CL_ContextAction implements FlavorListener
{

    private static Paste INSTANCE;
    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_V);
    private static final List<DataFlavor> SUPPORTED_FLAVORS = Arrays.asList(ItemsTransferable.DATA_FLAVOR, BarsTransferable.DATA_FLAVOR, DataFlavor.stringFlavor);
    protected static final Logger LOGGER = Logger.getLogger(Paste.class.getName());

    /**
     * We want a singleton because we need to listen to the system clipboard (and not obvious to find an event to unregister the listener).
     *
     * @return
     */
    @ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.paste")
    @ActionRegistration(displayName = "not_used", lazy = false)
    @ActionReferences(
            {
                @ActionReference(path = "Actions/Section", position = 1200),
                @ActionReference(path = "Actions/ChordSymbol", position = 1200),
                @ActionReference(path = "Actions/Bar", position = 1200),
                @ActionReference(path = "Actions/BarAnnotation", position = 1020)
            })
    public static Paste getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new Paste();
        }
        return INSTANCE;
    }

    /**
     * Enforce singleton
     */
    private Paste()
    {
    }

    /**
     * Enforce singleton.
     *
     * @param lkp
     * @return
     */
    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return this;
    }

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getCommonString("CTL_Paste"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        Icon icon = SystemAction.get(PasteAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES, ListeningTarget.BAR_SELECTION));


        // Listen to clipboard contents changes        
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.addFlavorListener(this);      // Never unregister, but it's ok we're singleton
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
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
        } catch (Exception ex)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() getSystemClipboard().getData() exception={0}", ex.getMessage());
            return;
        }


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());


        List<ChordLeadSheetItem> items = new ArrayList<>();  // Default do nothing
        int nbInsertBars = 0;   // Default do nothing


        if (df == BarsTransferable.DATA_FLAVOR)
        {
            BarsTransferable.Data dataBars = (BarsTransferable.Data) data;
            items = dataBars.getItemsCopy(targetBarIndex);
            nbInsertBars = dataBars.getBarRange().size();


        } else if (df == ItemsTransferable.DATA_FLAVOR)
        {
            ItemsTransferable.Data dataItems = (ItemsTransferable.Data) data;
            items = dataItems.getItemsCopy(targetBarIndex);

        } else if (df == DataFlavor.stringFlavor)
        {
            String text = (String) data;
            TextReader tr = new TextReader(text);
            Song pastedSong = tr.readSong();
            if (pastedSong != null)
            {
                var pastedCls = pastedSong.getChordLeadSheet();
                for (var item : pastedCls.getItems())
                {
                    items.add(item.getCopy(null, item.getPosition().getMoved(targetBarIndex, 0)));
                }
                nbInsertBars = pastedCls.getSizeInBars();
            }
        } else
        {
            throw new IllegalStateException("df=" + df);
        }


        // Insert new bars if required
        if (nbInsertBars > 0)
        {
            if (targetBarIndex >= cls.getSizeInBars())
            {
                // Resize
                try
                {
                    cls.setSizeInBars(targetBarIndex + nbInsertBars);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen when resizing bigger
                    String msg = "Impossible to resize.\n" + ex.getLocalizedMessage();
                    msg += "\n" + ex.getLocalizedMessage();
                    um.abortCEdit(getActionName(), msg);
                    return;
                }
            } else
            {
                // Insert bars
                cls.insertBars(targetBarIndex, nbInsertBars);
            }
        }


        // Add the items
        for (ChordLeadSheetItem<?> item : items)
        {
            int barIndex = item.getPosition().getBar();

            // Items which arrive after end of leadsheet are skipped.
            if (barIndex < cls.getSizeInBars())
            {
                if (item instanceof CLI_Section itemSection)
                {
                    // We need a new copy to make sure the new section name is generated with
                    // the possible previous sections added to the chordleadsheet.
                    // Otherwise possible name clash if e.g. bridge1 and bridge2 in the buffer,
                    // bridge1->bridge3, bridge2->bridge3.
                    CLI_Section newSection = (CLI_Section) itemSection.getCopy(null, cls);
                    try
                    {
                        newSection = cls.addSection(newSection);
                        LOGGER.log(Level.FINE, "newSection={0}", newSection);   // Just to make sure newSection is used
                    } catch (UnsupportedEditException ex)
                    {
                        String msg = ResUtil.getString(getClass(), "ERR_Paste", newSection);
                        msg += "\n" + ex.getLocalizedMessage();
                        um.abortCEdit(getActionName(), msg);
                        return;
                    }
                } else
                {
                    // Simple
                    cls.addItem(item);
                }
            }
        }


        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;

        if (!selection.isEmpty())
        {
            DataFlavor df = getCurrentSupportedFlavor();
            if (df == BarsTransferable.DATA_FLAVOR || df == DataFlavor.stringFlavor)
            {
                b = selection.isBarSelected();
            } else if (df == ItemsTransferable.DATA_FLAVOR)
            {
                b = selection.isBarSelectedWithinCls();
            } else
            {
                // Nothing interesting for us
                // Do nothing
            }
        }

        setEnabled(b);
    }


    // =================================================================================================
    // FlavorListener
    // =================================================================================================    
    @Override
    public void flavorsChanged(FlavorEvent e)
    {
        selectionChange(getSelection());
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
        } catch (IllegalStateException e)
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
