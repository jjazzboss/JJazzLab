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

import java.util.Arrays;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

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
        for (CLI_Section section : cls.getItems(CLI_Section.class))
        {
            Rhythm r = sgs.getLastUsedRhythm(section.getData().getTimeSignature());
            if (r == null)
            {
                r = RhythmDatabase.getDefault().getDefaultRhythm(section.getData().getTimeSignature());
            }
            SongPart spt = sgs.createSongPart(
                    r,
                    section.getPosition().getBar(),
                    cls.getSectionRange(section).size(),
                    section);
            sgs.addSongParts(Arrays.asList(spt));
        }
        return sgs;
    }

    @Override
    public SongStructure createSimpleSgs()
    {
        SongStructureImpl sgs = new SongStructureImpl();
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Rhythm r = rdb.getDefaultRhythm(TimeSignature.FOUR_FOUR);
        assert r != null;
        SongPart spt = sgs.createSongPart(r, 0, 8, null);
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
