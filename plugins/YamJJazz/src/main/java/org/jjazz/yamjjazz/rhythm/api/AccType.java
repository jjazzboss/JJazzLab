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
package org.jjazz.yamjjazz.rhythm.api;

import org.jjazz.harmony.api.Degree;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * Standard Yamaha Accompaniment Part channels.
 * <p>
 * SUB_RHYTHM channel = 8, PHRASE2 channel=15.
 */
public enum AccType
{
    SUBRHYTHM(null, null),
    RHYTHM(null, null),
    BASS(GMSynth.getInstance().getGM1Bank().getInstrument(35), new Degree[]
    {
        Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH
    }),
    CHORD1(GMSynth.getInstance().getGM1Bank().getInstrument(26), new Degree[]
    {
        Degree.ROOT, Degree.THIRD, Degree.FIFTH, Degree.SEVENTH
    }),
    CHORD2(GMSynth.getInstance().getGM1Bank().getInstrument(0), new Degree[]
    {
        Degree.ROOT, Degree.THIRD, Degree.FIFTH, Degree.SEVENTH
    }),
    PAD(GMSynth.getInstance().getGM1Bank().getInstrument(50), new Degree[]
    {
        Degree.ROOT, Degree.THIRD, Degree.FIFTH, Degree.SEVENTH
    }),
    PHRASE1(GMSynth.getInstance().getGM1Bank().getInstrument(62), new Degree[]
    {
        Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH
    }),
    PHRASE2(GMSynth.getInstance().getGM1Bank().getInstrument(2), new Degree[]
    {
        Degree.ROOT, Degree.NINTH, Degree.THIRD, Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH, Degree.SEVENTH
    });

    /**
     * The authorized source notes. Can be null if all notes authorized.
     */
    public final Degree[] authorizedDegrees;

    /**
     * The default GM1 instrument to use by default for this AccType.
     * <p>
     * Null only for SubRhythm and Rhythm.
     */
    public final GM1Instrument defaultGM1Instrument;

    private AccType(GM1Instrument defaultGM1Ins, Degree[] degrees)
    {
        authorizedDegrees = degrees;
        defaultGM1Instrument = defaultGM1Ins;
    }

    @Override
    public String toString()
    {
        if (this.equals(SUBRHYTHM))
        {
            return "SubRhythm";
        } else
        {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    /**
     * True if d is an authorized note for this AccType.
     *
     * @param relPitch A relative pitch.
     * @return
     */
    public boolean isAuthorizedNote(int relPitch)
    {
        if (relPitch < 0 || relPitch > 11)
        {
            throw new IllegalArgumentException("relPitch=" + relPitch);   //NOI18N
        }
        boolean r = false;
        if (authorizedDegrees != null)
        {
            for (Degree d : authorizedDegrees)
            {
                if (d.getPitch() == relPitch)
                {
                    r = true;
                    break;
                }
            }
        } else
        {
            r = true;
        }
        return r;
    }

    /**
     * The authorized degree for this AccType corresponding to relPitch.
     *
     * @param relPitch A relative pitch (0-11)
     * @return A Degree, or null if relPitch does not correspond to an authorized degree
     * @see #isAuthorizedNote(int) 
     */
    public Degree getAuthorizedDegree(int relPitch)
    {
        if (relPitch < 0 || relPitch > 11)
        {
            throw new IllegalArgumentException("relPitch=" + relPitch);   //NOI18N
        }
        Degree d = null;
        if (authorizedDegrees != null)
        {
            for (Degree di : authorizedDegrees)
            {
                if (di.getPitch() == relPitch)
                {
                    d = di;
                    break;
                }
            }
        } else
        {
            d = Degree.getDegrees(relPitch).get(0);
        }
        return d;
    }

    /**
     * True for Rhythm or SubRhythm.
     *
     * @return
     */
    public boolean isDrums()
    {
        return this.equals(RHYTHM) || this.equals(SUBRHYTHM);
    }

    public RhythmVoice.Type getRvType()
    {
        switch (this)
        {
            case RHYTHM:
                return RhythmVoice.Type.DRUMS;
            case SUBRHYTHM:
                return RhythmVoice.Type.PERCUSSION;
            default:
                return RhythmVoice.Type.valueOf(name());
        }
    }


    /**
     *
     * @return 8-15
     */
    public int getChannel()
    {
        return 8 + ordinal();
    }

    /**
     *
     * @param channel
     * @return Null if channel is not in the [8-15] interval
     */
    static public AccType getAccType(int channel)
    {
        if (channel < 8 || channel > 15)
        {
            return null;
        } else
        {
            return values()[channel - 8];
        }
    }

    /**
     * Get the AccType corresponding to a RhythmVoice.
     *
     * @param rv
     * @return Can be null if rv's type is Other.
     */
    static public AccType getAccType(RhythmVoice rv)
    {
        for (AccType at : AccType.values())
        {
            if (at.getRvType().equals(rv.getType()))
            {
                return at;
            }
        }
        return null;
    }
}
