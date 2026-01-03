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
package org.jjazz.songeditormanager;

import static com.google.common.base.Preconditions.checkArgument;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songeditormanager.CreatePracticeSongDialog.Config;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.api.SelectedBar;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
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
@ActionID(category = "JJazz", id = "org.jjazz.actions.createpracticesong")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 832, separatorAfter = 850),
            @ActionReference(path = "Menu/Tools", position = 189)
        })
public final class CreatePracticeSong extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    public static String SONG_NAME_SUFFIX = "-Practice-";
    private static int SUFFIX_ID = 0;
    private static CreatePracticeSongDialog.Config LAST_CONFIG;
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
        cap.addWeakSelectionListener(this);
        putValue(NAME, ResUtil.getString(getClass(), "CTL_CreatePracticeSong"));
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Song song = CL_EditorTopComponent.getActive().getSongModel();
        assert song != null;
        CL_Selection selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        var selectedBars = selection.getSelectedBarsWithinCls();
        assert !selectedBars.isEmpty() : "selection=" + selection;
        IntRange selRange = new IntRange(selectedBars.get(0).getModelBarIndex(),
                selectedBars.get(selectedBars.size() - 1).getModelBarIndex());


        // Create a new ChordLeadSheet from selected bars only
        var newCls = createCls(cls, selectedBars);


        // Create the song with newCls and create the corresponding SongStructure
        Song newSong;
        try
        {
            newSong = createSong(song, selRange, newCls);
        } catch (UnsupportedEditException ex)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() ex={0}", ex.getMessage());
            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        // Prepare and show the config dialog
        int tempoEnd = song.getTempo();
        int tempoStart = Math.max(TempoRange.TEMPO_MIN, tempoEnd - 30);
        Config config = new Config(tempoStart, tempoEnd, 6);
        if (LAST_CONFIG != null)
        {
            if (tempoEnd == LAST_CONFIG.tempoEnd)
            {
                config = LAST_CONFIG;
            } else
            {
                config.nbSteps = LAST_CONFIG.nbSteps;
                float r = (float) LAST_CONFIG.tempoStart / LAST_CONFIG.tempoEnd;
                config.tempoStart = Math.round(r * tempoEnd);
            }
        }
        var dlg = new CreatePracticeSongDialog(config);
        dlg.setVisible(true);
        CreatePracticeSongDialog.Config res = dlg.getResult();
        if (res == null)
        {
            return;
        }

        LAST_CONFIG = res;


        // Update and display newSong      
        updateSongForPractice(newSong, LAST_CONFIG.tempoStart, LAST_CONFIG.tempoEnd, LAST_CONFIG.nbSteps);
        SongEditorManager.getDefault().showSong(newSong, true, true);      // Make song appear as modified/savable

        Analytics.logEvent("Create practice song");

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
    public void selectionChange(CL_Selection selection)
    {
        boolean b = selection.isContiguousBarboxSelectionWithinCls();
        setEnabled(b);
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

    /**
     * Update a song so that tempo gradually increases from tempoStart to tempoEnd in nbSteps.
     *
     * @param song
     * @param tempoStart BPM tempo
     * @param tempoEnd   BPM tempo
     * @param nbSteps    Must be &gt; 1
     * @throws IllegalArgumentException If tempoStart/tempoEnd ratio is not in the range ]0.5;2[
     */
    private void updateSongForPractice(Song song, int tempoStart, int tempoEnd, int nbSteps)
    {
        checkArgument(nbSteps > 1, "nbSteps=%s", nbSteps);
        float r = (float) tempoStart / tempoEnd;
        float tempoStep = ((float) tempoEnd - tempoStart) / (nbSteps - 1);
        checkArgument(r >= 0.25 && r <= 4, "tempoStart=%s tempoEnd=%s", tempoStart, tempoEnd);


        var sgs = song.getSongStructure();
        var spts = sgs.getSongParts();
        int nbSptsOriginal = spts.size();

        int songTempo = tempoEnd;       // By default
        if (r < 0.5 || r > 2)
        {
            // tempoEnd can not be the song's tempo
            songTempo = Math.round(Math.max(tempoEnd, tempoStart) / 2f);
        }
        song.setTempo(songTempo);


        // First duplicate the song parts
        for (int i = 0; i < nbSteps - 1; i++)
        {
            for (int j = spts.size() - 1; j >= 0; j--)
            {
                var spt = spts.get(j);
                var newSpt = spt.getCopy(null, 0, spt.getNbBars(), spt.getParentSection());
                try
                {
                    sgs.addSongParts(Arrays.asList(newSpt));
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen we dont change rhythm
                    Exceptions.printStackTrace(ex);
                }
            }
        }


        // Update the tempo factor of song parts
        spts = sgs.getSongParts();
        int sptIndex = 0;
        for (int i = 0; i < nbSteps; i++)
        {

            // Calculate the RP_SYS_TempoFactor value
            int rpTempoFactorValue;
            float tempo = tempoStart + tempoStep * i;
            rpTempoFactorValue = Math.round(100 * tempo / songTempo);


            for (int j = 0; j < nbSptsOriginal; j++)
            {
                var spt = spts.get(sptIndex);
                var rpTempo = RP_SYS_TempoFactor.getTempoFactorRp(spt.getRhythm());
                sgs.setRhythmParameterValue(spt, rpTempo, rpTempoFactorValue);
                sptIndex++;
            }

        }


    }

    /**
     * Create a song with newCls and the corresponding SongStructure.
     *
     * @param song
     * @param selRange
     * @param newCls
     * @return
     * @throws UnsupportedEditException
     */
    private Song createSong(Song song, IntRange selRange, ChordLeadSheet newCls) throws UnsupportedEditException
    {
        ChordLeadSheet cls = song.getChordLeadSheet();
        SongStructure sgs = song.getSongStructure();


        Song newSong = SongFactory.getInstance().createSong(getPracticeSongName(song.getName()), newCls);


        // Clear the new SongStructure
        var newSgs = newSong.getSongStructure();
        newSgs.removeSongParts(newSgs.getSongParts());


        // Get all sections
        List<CLI_Section> cliSections = new ArrayList<>();
        cliSections.add(cls.getSection(selRange.from));  // Section might be before selection range
        if (selRange.size() > 1)
        {
            cliSections.addAll(cls.getItems(selRange.from + 1, selRange.to, CLI_Section.class));
        }


        // Duplicate the SongParts
        List<SongPart> addSpts = new ArrayList<>();
        for (var cliSection : cliSections)
        {
            int sectionBar = Math.max(cliSection.getPosition().getBar(), selRange.from);     // cliSection might start before selection range if no section defined at bar selRange.from
            SongPart spt = sgs.getSongParts().stream()
                    .filter(s -> s.getParentSection() == cliSection)
                    .findFirst()
                    .orElse(null);
            assert spt != null : "cliSection=" + cliSection;

            // Get the equivalent section in the new song
            var newCliSection = newCls.getSection(sectionBar - selRange.from);
            int newSptSize = newCls.getBarRange(newCliSection).size();      // Might be < sptSize if 1st selected bar had no section defined
            SongPart newSpt = spt.getCopy(null, newCliSection.getPosition().getBar(), newSptSize, newCliSection);
            if (RP_SYS_TempoFactor.getTempoFactorRp(newSpt.getRhythm()) == null)
            {
                throw new UnsupportedEditException(
                        "Rhythm " + newSpt.getRhythm().getName() + " does not use the Tempo Factor rhythm parameter");
            }
            addSpts.add(newSpt);
        }

        // Add the song parts
        newSgs.addSongParts(addSpts);

        return newSong;

    }

    private ChordLeadSheet createCls(ChordLeadSheet cls, List<SelectedBar> selectedBars)
    {
        int startBar = selectedBars.get(0).getModelBarIndex();
        CLI_Section oldInitSection = cls.getSection(startBar);
        ChordLeadSheet newCls = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet(oldInitSection.getData().getName(),
                oldInitSection.getData().getTimeSignature(), selectedBars.size(), null);
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
                var newCliSection = (CLI_Section) cliSection.getCopy(new Position(barIndex - startBar), newCls);
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
            Position newPos = cliCs.getPosition().getMoved(-startBar, 0);
            var cliCsNew = cliCs.getCopy(null, newPos);
            newCls.addItem(cliCsNew);
        }


        // Add an initial chord symbol if not present
        cliChordSymbols = newCls.getItems(0, 0, CLI_ChordSymbol.class);
        if (cliChordSymbols.isEmpty() || !cliChordSymbols.get(0).getPosition().isFirstBarBeat())
        {
            assert startBar > 0 : "startBar=" + startBar + " cliChordSymbols=" + cliChordSymbols;
            // var cliCs = cls.getLastItem(0, startBar - 1, CLI_ChordSymbol.class);
            var cliCs = cls.getLastItemBefore(new Position(startBar), false, CLI_ChordSymbol.class, cli -> true);
            assert cliCs != null : "startBar=" + startBar + " cls=" + cls;
            var cliCsNew = cliCs.getCopy(null, new Position(0));
            newCls.addItem(cliCsNew);
        }

        return newCls;
    }
}
