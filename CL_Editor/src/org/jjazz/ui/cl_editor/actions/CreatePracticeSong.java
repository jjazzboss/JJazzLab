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
package org.jjazz.ui.cl_editor.actions;

import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.cl_editor.api.SelectedBar;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;


/**
 * Create a practice song from the selected bars.
 * <p>
 */
@ActionRegistration(displayName = "#CTL_CreatePracticeSong", lazy = false)
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.createpracticesong")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 205),
        })
public final class CreatePracticeSong extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    public static String SONG_NAME_SUFFIX = "-Practice-";
    private static int SUFFIX_ID = 0;
    private Lookup context;
    private CL_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(CreatePracticeSong.class.getSimpleName());

    public CreatePracticeSong()
    {
        this(Utilities.actionsGlobalContext());
    }

    private CreatePracticeSong(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, ResUtil.getString(getClass(), "CTL_CreatePracticeSong"));
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Song song = CL_EditorTopComponent.getActive().getSongModel();
        assert song != null;
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        SongStructure sgs = song.getSongStructure();
        var selectedBars = selection.getSelectedBarsWithinCls();
        assert !selectedBars.isEmpty() : "selection=" + selection;
        int startBar = selectedBars.get(0).getModelBarIndex();
        int lastBar = startBar + selectedBars.size() - 1;


        // Create a new ChordLeadSheet from selected bars only
        var newCls = createCls(cls, selectedBars);


        // Create the song and update the song structure
        Song newSong = null;
        try
        {
            newSong = SongFactory.getInstance().createSong(getPracticeSongName(song.getName()), newCls);


            // Clear the new SongStructure
            var newSgs = newSong.getSongStructure();            
            newSgs.removeSongParts(newSgs.getSongParts());


            // Get all sections
            List<CLI_Section> cliSections = new ArrayList<>();
            cliSections.add(cls.getSection(startBar));  // Section might be before startBar
            if (startBar < lastBar)
            {
                cliSections.addAll(cls.getItems(startBar + 1, lastBar, CLI_Section.class));
            }


            // Duplicate the SongParts
            List<SongPart> addSpts = new ArrayList<>();
            for (var cliSection : cliSections)
            {
                int sectionBar = Math.max(cliSection.getPosition().getBar(), startBar);     // cliSection might start before startBar if no section defined at startBar
                SongPart spt = sgs.getSongPart(sectionBar);
                // Get equivalent section in the new song
                var newCliSection = newCls.getSection(sectionBar - startBar);
                int newSptSize = newCls.getBarRange(newCliSection).size();      // Might be < sptSize if 1st selected bar had no section defined
                SongPart newSpt = spt.clone(null, newCliSection.getPosition().getBar(), newSptSize, newCliSection);
                addSpts.add(newSpt);
            }
            newSgs.addSongParts(addSpts);

        } catch (UnsupportedEditException ex)
        {
            LOGGER.warning("actionPerformed() ex=" + ex.getMessage());
            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        // Display the new song
        
        
    }


    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new CreatePracticeSong(context);
    }

    // ========================================================================================
    // CL_ContextActionListener interface
    // ========================================================================================

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = selection.isContiguousBarboxSelectionWithinCls();
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================

    private String getPracticeSongName(String sgName)
    {
        SUFFIX_ID++;
        String res = sgName + SONG_NAME_SUFFIX + SUFFIX_ID;
        return res;
    }

    private ChordLeadSheet createCls(ChordLeadSheet cls, List<SelectedBar> selectedBars)
    {
        int startBar = selectedBars.get(0).getModelBarIndex();
        CLI_Section oldInitSection = cls.getSection(startBar);
        ChordLeadSheet newCls = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet(oldInitSection.getData().getName(), oldInitSection.getData().getTimeSignature(), selectedBars.size(), false);
        CLI_Section newInitSection = newCls.getSection(0);
        newCls.setSectionName(newInitSection, oldInitSection.getData().getName());


        // Add possible other sections
        selectedBars.stream().skip(1).forEach(sb ->
        {
            int barIndex = sb.getModelBarIndex();
            CLI_Section cliSection = cls.getSection(barIndex);

            if (cliSection.getPosition().getBar() == barIndex)
            {
                // There is a section on barIndex, copy it
                var newCliSection = (CLI_Section) cliSection.getCopy(newCls, new Position(barIndex - startBar, 0));
                try
                {
                    newCls.addSection(newCliSection);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen, ChordLeadSheet is not a song yet
                    Exceptions.printStackTrace(ex);
                }
            }
        });


        // Add the chord symbols
        var cliChordSymbols = cls.getItems(startBar, startBar + selectedBars.size() - 1, CLI_ChordSymbol.class);
        for (var cliCs : cliChordSymbols)
        {
            Position newPos = cliCs.getPosition().getMovedPosition(-startBar, 0);
            var cliCsNew = cliCs.getCopy(newCls, newPos);
            newCls.addItem(cliCsNew);
        }


        // Add an initial chord symbol if not present
        cliChordSymbols = newCls.getItems(0, 0, CLI_ChordSymbol.class);
        if (cliChordSymbols.isEmpty() || !cliChordSymbols.get(0).getPosition().isFirstBarBeat())
        {
            assert startBar > 0 : "startBar=" + startBar + " cliChordSymbols=" + cliChordSymbols;
            var cliCs = cls.getLastItem(0, startBar - 1, CLI_ChordSymbol.class);
            assert cliCs != null : "startBar=" + startBar + " cls=" + cls;
            var cliCsNew = cliCs.getCopy(newCls, new Position(0, 0));
            newCls.addItem(cliCsNew);
        }

        return newCls;
    }
}
