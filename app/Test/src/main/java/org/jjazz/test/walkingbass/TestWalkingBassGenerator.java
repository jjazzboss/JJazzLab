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
package org.jjazz.test.walkingbass;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice.Type;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.test.walkingbass.generator.WalkingBassGenerator;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

/**
 * For debug purposes...
 */
@ActionID(category = "JJazz", id = "org.jjazz.test.TestWalkingBassGenerator")
@ActionRegistration(displayName = "Test WalkingBassGenerator")
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 50000)
        })
public final class TestWalkingBassGenerator implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(TestWalkingBassGenerator.class.getSimpleName());
 
    
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "TestWalkingBassGenerator.actionPerformed() called");

        testCompositeRhythm();
    }

    private void testCompositeRhythm()
    {
        var rdb = RhythmDatabase.getDefault();
        var ri = rdb.getRhythm("MediumJazz.S737.sst-ID");
        Rhythm r = null;
        try
        {
            r = rdb.getRhythmInstance(ri);
        } catch (UnavailableRhythmException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assert r instanceof YamJJazzRhythm : "r=" + r;
        YamJJazzRhythm yjr = (YamJJazzRhythm) r;
        var oldGen = yjr.getMusicGenerator();
        if (oldGen instanceof CompositeMusicGenerator)
        {
            return;
        }

        var newGen = new WalkingBassGenerator(r);

        Multimap<MusicGenerator, RhythmVoice> mmap = ArrayListMultimap.create();
        EnumSet<Type> types1 = EnumSet.of(Type.BASS);
        for (var rv : r.getRhythmVoices())
        {
            var mg = types1.contains(rv.getType()) ? newGen : oldGen;
            mmap.put(mg, rv);
        }

        var compositeGen = new CompositeMusicGenerator(yjr, mmap);
        yjr.setMusicGenerator(compositeGen);

        LOGGER.log(Level.INFO, "testCompositeRhythm() yjr={0} updated with new CompositeMusicGenerator", yjr);
    }


}
