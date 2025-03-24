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
package org.jjazz.proswing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.proswing.walkingbass.GenericTilingFactory;
import org.jjazz.proswing.walkingbass.GenericTilingFactory.CustomWbpSourcesProvider;
import org.jjazz.proswing.walkingbass.TilingFactory;
import org.jjazz.proswing.walkingbass.WbpSource;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;
import org.jjazz.rhythmmusicgeneration.api.DummyGenerator;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * The possible bass styles of a WbpSource.
 * <p>
 * ...CUSTOM styles are dynamically created WbpSources when database could not provide the relevant WbpSource.
 */
public enum BassStyle
{
    BASIC,
    TWO_FEEL,
    TWO_FEEL_CUSTOM,
    WALKING,
    WALKING_CUSTOM,
    WALKING_DOUBLE,
    WALKING_DOUBLE_CUSTOM;

    private static final ImmutableBiMap<BassStyle, BassStyle> BIMAP_STYLE_CUSTOM = ImmutableBiMap.of(
            TWO_FEEL, TWO_FEEL_CUSTOM,
            WALKING, WALKING_CUSTOM,
            WALKING_DOUBLE, WALKING_DOUBLE_CUSTOM
    );
    private static final Logger LOGGER = Logger.getLogger(BassStyle.class.getSimpleName());

    /**
     * Get the factory associated to this style.
     *
     * @return
     */
    public TilingFactory getTilingFactory()
    {
        TilingFactory res;
        res = switch (this)
        {
            case BASIC ->
                new BasicFactory();
            case TWO_FEEL ->
                new GenericTilingFactory(this, new TwoFeelCustomBuilder());
            case WALKING ->
                new GenericTilingFactory(this, new TwoFeelCustomBuilder());
            case WALKING_DOUBLE ->
                new WalkingDoubleTilingFactory();
            default -> throw new IllegalStateException("this=" + this);
        };
        return res;
    }

    public boolean is2feel()
    {
        return this == TWO_FEEL || this == TWO_FEEL_CUSTOM;
    }

    public boolean isWalking()
    {
        return this == WALKING || this == WALKING_CUSTOM || this == WALKING_DOUBLE || this == WALKING_DOUBLE_CUSTOM;
    }

    public boolean isCustom()
    {
        return BIMAP_STYLE_CUSTOM.containsValue(this);
    }

    public BassStyle getCustomStyle()
    {
        BassStyle res = BIMAP_STYLE_CUSTOM.get(this);
        if (res == null)
        {
            throw new IllegalArgumentException("this=" + this);
        }
        return res;
    }

    public BassStyle getStyleFromCustom(BassStyle style)
    {
        BassStyle res = BIMAP_STYLE_CUSTOM.inverse().get(style);
        if (res == null)
        {
            throw new IllegalArgumentException("style=" + style);
        }
        return res;
    }


    //===============================================================================================
    // Inner classes
    //===============================================================================================
    static private class TwoFeelCustomBuilder implements CustomWbpSourcesProvider
    {
        /**
         * Create one or more default WbpSources with BassType=CUSTOM for subSeq.
         *
         * @param scs Must start at bar 0. Contains 2 or more chords.
         * @return
         */
        @Override
        public List<WbpSource> build(SimpleChordSequence scs)
        {
            Preconditions.checkArgument(scs.getBarRange().from == 0 && scs.size() > 1, "subSeq=%s", scs);

            LOGGER.log(Level.SEVERE, "createCustomWbpSources() - scs={0}", scs);

            List<WbpSource> res = new ArrayList<>();
            if (scs.isTwoChordsPerBar(0.25f, false))
            {
                LOGGER.log(Level.SEVERE, "createCustomWbpSources() => 2-chord-per-bar phrase");
                SizedPhrase sp = create2ChordsPerBarPhrase(scs);
                String id = "Gen2Chords-" + (SESSION_COUNT++);
                WbpSource wbpSource = new WbpSource(id, 0, BassStyle.CUSTOM, scs, sp, 0, null);
                WbpSource wbpGrooveRef = findGrooveReference(sp);
                if (wbpGrooveRef != null)
                {
                    Phrases.applyGroove(wbpGrooveRef.getSizedPhrase(), sp, 0.15f);
                } else
                {
                    LOGGER.log(Level.WARNING, "createCustomWbpSources() no groove reference found for phrase {0}", sp);
                }

                res.add(wbpSource);

            } else
            {
                LOGGER.log(Level.SEVERE, "createCustomWbpSources() => random phrase");
                var p = DummyGenerator.getBasicBassPhrase(0, scs, new IntRange(50, 65), 0);
                SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(0), scs.getTimeSignature(), false);
                sp.add(p);
                String id = "GenDefault-" + (SESSION_COUNT++);
                WbpSource wbpSource = new WbpSource(id, 0, BassStyle.CUSTOM, scs, sp, 0, null);
                res.add(wbpSource);
            }

            return res;
        }

        /**
         * Create a bass phrase for a 2-chord per bar chord sequence.
         *
         * @param subSeq
         * @return
         */
        private SizedPhrase create2ChordsPerBarPhrase(SimpleChordSequence subSeq)
        {
            SizedPhrase sp = new SizedPhrase(0, subSeq.getBeatRange(0), subSeq.getTimeSignature(), false);
            for (var cliCs : subSeq)
            {
                var ecs = cliCs.getData();
                int cBase = 3 * 12;
                int bassPitch0 = cBase + ecs.getRootNote().getRelativePitch();
                int bassPitch1 = cBase + ecs.getRelativePitch(ChordType.DegreeIndex.THIRD_OR_FOURTH);
                float pos0 = subSeq.toPositionInBeats(cliCs.getPosition(), 0);
                float pos1 = pos0 + 1f;
                NoteEvent ne0 = new NoteEvent(bassPitch0, 1f, 80, pos0);
                NoteEvent ne1 = new NoteEvent(bassPitch1, 1f, 80, pos1);
                sp.add(ne0);
                sp.add(ne1);
            }
            return sp;
        }

        private boolean isGeneratedWbpSource(WbpSource wbpSource)
        {
            return wbpSource.getId().startsWith("Gen");
        }

        /**
         * Find a random WbpSource which can serve as groove reference for sp.
         *
         * @param sp
         * @return
         */
        private WbpSource findGrooveReference(SizedPhrase sp)
        {
            WbpSource res = null;

            var wbpSources = WbpSourceDatabase.getInstance().getWbpSources(sp.getSizeInBars()).stream()
                    .filter(w -> !isGeneratedWbpSource(w) && Phrases.isSamePositions(w.getSizedPhrase(), sp, 0.15f))
                    .toList();

            if (wbpSources.size() == 1)
            {
                res = wbpSources.get(0);
            } else if (wbpSources.size() > 1)
            {
                int index = (int) (Math.random() * wbpSources.size());
                res = wbpSources.get(index);
            }

            return res;
        }

    }

}
