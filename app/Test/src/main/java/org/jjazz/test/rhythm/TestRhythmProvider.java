/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.test.rhythm;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.utilities.api.MultipleErrorsReport;

/**
 *
 * @author Jerome
 */
// @ServiceProvider(service = RhythmProvider.class)
public class TestRhythmProvider implements RhythmProvider
{

    private final CompositeRhythm rhythm;

    public TestRhythmProvider()
    {
        Rhythm r1, r2;
        try
        {
            r1 = RhythmDatabase.getDefault().getRhythmInstance("FastJazz.S741.sst-ID");
            r2 = RhythmDatabase.getDefault().getRhythmInstance("16BeatBallad2.S014.prs-ID");
        } catch (UnavailableRhythmException ex)
        {
            throw new IllegalStateException(ex);
        }
        Predicate<RhythmVoice> p = rv -> rv.getType() == RhythmVoice.Type.BASS || rv.getType() == RhythmVoice.Type.DRUMS;
        var rvs1 = r1.getRhythmVoices().stream()
                .filter(p)
                .toList();
        var rvs2 = r2.getRhythmVoices().stream()
                .filter(p.negate())
                .toList();
        rhythm = new CompositeRhythm("FirstCompRhythm", r1, rvs1, r2, rvs2);

    }

    public Rhythm getTestRhythm()
    {
        return rhythm;
    }

    @Override
    public Info getInfo()
    {
        return new RhythmProvider.Info("TestRhythmProviderId", "TestRhythmProvider", "desctiption", "Jerome", "0.1");
    }

    @Override
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        return List.of(rhythm);
    }

    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        return List.of();
    }

    @Override
    public String[] getSupportedFileExtensions()
    {
        return new String[0];
    }

    @Override
    public Rhythm readFast(File f) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void showUserSettingsDialog()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean hasUserSettings()
    {
        return false;
    }

}
