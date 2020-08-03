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
package org.jjazz.songstructure;

import java.util.ArrayList;
import java.util.Arrays;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.openide.util.Exceptions;

@ServiceProvider(service = SongStructureFactory.class)
public class SongStructureFactoryImpl extends SongStructureFactory
{

    static private SongStructureFactoryImpl INSTANCE;

    static public SongStructureFactoryImpl getInstance()
    {
        synchronized (SongStructureFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SongStructureFactoryImpl();
            }
        }
        return INSTANCE;
    }

    @Override
    public SongStructure createSgs(ChordLeadSheet cls, boolean keepSgsUpdated) throws UnsupportedEditException
    {
        if (cls == null)
        {
            throw new IllegalArgumentException("cls=" + cls);
        }

        SongStructureImpl sgs = new SongStructureImpl(cls, keepSgsUpdated);

        var rdb = RhythmDatabase.getDefault();

        var newSpts = new ArrayList<SongPart>();
        for (CLI_Section section : cls.getItems(CLI_Section.class))
        {
            int sptBarIndex = section.getPosition().getBar();
            
            
            Rhythm r = null;
            try
            {
                RhythmInfo ri = rdb.getDefaultRhythm(section.getData().getTimeSignature());
                r = rdb.getRhythmInstance(ri);
            } catch (UnavailableRhythmException ex)
            {
                // Should never be there
                Exceptions.printStackTrace(ex);
            }

            SongPart spt = sgs.createSongPart(
                    r,
                    section.getData().getName(),
                    sptBarIndex,
                    cls.getSectionRange(section).size(),
                    section,
                    false);
            newSpts.add(spt);
        }

        // Add new song parts in one shot to avoid issue if an AdaptedRhythm is used      
        sgs.addSongParts(newSpts);      // Can raise exception

        return sgs;
    }

    @Override
    public SongStructure createSimpleSgs()
    {
        SongStructureImpl sgs = new SongStructureImpl();
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Rhythm r = null;
        try
        {
            r = rdb.getRhythmInstance(rdb.getDefaultRhythm(TimeSignature.FOUR_FOUR));
        } catch (UnavailableRhythmException ex)
        {
            // Should never be there
            Exceptions.printStackTrace(ex);
        }
        assert r != null;
        SongPart spt = sgs.createSongPart(r, "Name", 0, 8, null, false);
        try
        {
            sgs.addSongParts(Arrays.asList(spt));
        } catch (UnsupportedEditException ex)
        {
            // This should not happen for a simple SGS.
            throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);
        }
        return sgs;
    }

    @Override
    public SongStructure createEmptySgs()
    {
        SongStructureImpl sgs = new SongStructureImpl();
        return sgs;
    }
}
