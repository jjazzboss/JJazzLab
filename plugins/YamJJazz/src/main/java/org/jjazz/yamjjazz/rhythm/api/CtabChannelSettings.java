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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.midi.api.MidiConst;

/**
 * Corresponds to data in the Ctab structure or ctb2 structure (first part only, 2nd ctb2 part is in the ct2Low/Main/High fields) in the style file, associated
 * to one channel.
 * <p>
 * For simplification fields are public and can be filled directly by the user, or via the helper methods.<p>
 */
public class CtabChannelSettings
{

    private int channel;
    public String name;
    public AccType accType;
    public boolean editable; // Not used
    public ArrayList<Note> mutedNotes = new ArrayList<>();
    public boolean autoStart;
    public ArrayList<YamChord> mutedChords = new ArrayList<>();
    public Note sourceChordNote;
    public YamChord sourceChordType;
    private int ctb2MiddleLowPitch = 0; // Not used if SFF1
    private int ctb2MiddleHighPitch = 127;// Not used if SFF1
    public Ctb2ChannelSettings ctb2Low = null;
    public final Ctb2ChannelSettings ctb2Main = new Ctb2ChannelSettings();      // Used for SFF1 or SFF2
    public Ctb2ChannelSettings ctb2High = null;

    private static final Logger LOGGER = Logger.getLogger(CtabChannelSettings.class.getSimpleName());

    public CtabChannelSettings(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        this.channel = channel;
    }

    public ExtChordSymbol getSourceChordSymbol()
    {
        ChordType ct = sourceChordType.getChordType();
        ExtChordSymbol ecs = new ExtChordSymbol(sourceChordNote, sourceChordNote, ct);
        return ecs;
    }

    /**
     *
     * @return 1-16
     */
    public int getChannel()
    {
        return channel;
    }

    public int getCtb2MiddeLowPitch()
    {
        return ctb2MiddleLowPitch;
    }

    /**
     * Set the middle low pitch.
     * <p>
     * This also creates ctb2Low if required.
     *
     * @param b1
     */
    public void setCtb2MiddleLowPitch(int b1)
    {
        ctb2MiddleLowPitch = b1;
        if (ctb2MiddleLowPitch > 0)
        {
            ctb2Low = new Ctb2ChannelSettings();
        }
    }

    /**
     * True if there is only one ctb2 for the main/middle note range (no ctb2 for low range or high range).
     * <p>
     * Will always return true for SFF1 files.
     *
     * @return
     */
    public boolean isSingleCtb2()
    {
        return ctb2Low == null && ctb2High == null;
    }

    public int getCtb2MiddeHighPitch()
    {
        return ctb2MiddleHighPitch;
    }

    /**
     * Set the middle high pitch.
     * <p>
     * This also creates ctb2High if required.
     *
     * @param b1
     */
    public void setCtb2MiddleHighPitch(int b1)
    {
        ctb2MiddleHighPitch = b1;
        if (ctb2MiddleHighPitch < 127)
        {
            ctb2High = new Ctb2ChannelSettings();
        }
    }

    /**
     *
     * @param b1 [0; 0x22]
     *
     */
    public void setSourceChordType(int b1)
    {
        if (b1 >= 0x23 || b1 < 0)
        {
            throw new IllegalArgumentException("b1=" + b1);   //NOI18N
        }
        if (b1 == 0x22)
        {
            LOGGER.warning("setSourceChordType() valid value (0X22: cancel all instruments) but not yet supported by JJazzLab. Using a '7M' chord instead");
        }
        // 0x22 = "cancel (stop all instruments)"
        sourceChordType = (b1 == 0x22) ? YamChord.ALL_CHORDS.get(2) : YamChord.ALL_CHORDS.get(0x21 - b1);
    }

    public void setMutedChords(int b1, int b2, int b3, int b4, int b5)
    {
        autoStart = ((b1 & 4) == 4);
        if ((b1 & 2) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(0));
        }
        if ((b1 & 1) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(1));
        }
        if ((b2 & 128) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(2));
        }
        if ((b2 & 64) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(3));
        }
        if ((b2 & 32) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(4));
        }
        if ((b2 & 16) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(5));
        }
        if ((b2 & 8) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(6));
        }
        if ((b2 & 4) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(7));
        }
        if ((b2 & 2) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(8));
        }
        if ((b2 & 1) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(9));
        }
        if ((b3 & 128) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(10));
        }
        if ((b3 & 64) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(11));
        }
        if ((b3 & 32) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(12));
        }
        if ((b3 & 16) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(13));
        }
        if ((b3 & 8) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(14));
        }
        if ((b3 & 4) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(15));
        }
        if ((b3 & 2) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(16));
        }
        if ((b3 & 1) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(17));
        }
        if ((b4 & 128) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(18));
        }
        if ((b4 & 64) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(19));
        }
        if ((b4 & 32) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(20));
        }
        if ((b4 & 16) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(21));
        }
        if ((b4 & 8) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(22));
        }
        if ((b4 & 4) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(23));
        }
        if ((b4 & 2) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(24));
        }
        if ((b4 & 1) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(25));
        }
        if ((b5 & 128) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(26));
        }
        if ((b5 & 64) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(27));
        }
        if ((b5 & 32) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(28));
        }
        if ((b5 & 16) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(29));
        }
        if ((b5 & 8) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(30));
        }
        if ((b5 & 4) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(31));
        }
        if ((b5 & 2) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(32));
        }
        if ((b5 & 1) == 0)
        {
            mutedChords.add(YamChord.ALL_CHORDS.get(33));
        }
    }

    /**
     * @param b1 bit field of the CTAB struct (byte 11)
     * @param b2 bit field of the CTAB struct (byte 12)
     */
    public void setMutedNotes(int b1, int b2)
    {
        mutedNotes.clear();
        if ((b1 & 8) == 0)
        {
            mutedNotes.add(new Note(11));
        }
        if ((b1 & 4) == 0)
        {
            mutedNotes.add(new Note(10));
        }
        if ((b1 & 2) == 0)
        {
            mutedNotes.add(new Note(9));
        }
        if ((b1 & 1) == 0)
        {
            mutedNotes.add(new Note(8));
        }
        if ((b2 & 128) == 0)
        {
            mutedNotes.add(new Note(7));
        }
        if ((b2 & 64) == 0)
        {
            mutedNotes.add(new Note(6));
        }
        if ((b2 & 32) == 0)
        {
            mutedNotes.add(new Note(5));
        }
        if ((b2 & 16) == 0)
        {
            mutedNotes.add(new Note(4));
        }
        if ((b2 & 8) == 0)
        {
            mutedNotes.add(new Note(3));
        }
        if ((b2 & 4) == 0)
        {
            mutedNotes.add(new Note(2));
        }
        if ((b2 & 2) == 0)
        {
            mutedNotes.add(new Note(1));
        }
        if ((b2 & 1) == 0)
        {
            mutedNotes.add(new Note(0));
        }
    }

    /**
     * True if specified note is one of the muted notes.
     *
     * @param n
     * @return
     */
    public boolean isMuted(Note n)
    {
        boolean res = false;
        for (Note mn : mutedNotes)
        {
            if (mn.equalsRelativePitch(n))
            {
                res = true;
                break;
            }
        }
        return res;
    }

    /**
     * True if specified YamChord is one of the muted chords.
     *
     * @param yc
     * @return
     */
    public boolean isMuted(YamChord yc)
    {
        boolean res = false;
        for (YamChord myc : mutedChords)
        {
            if (myc.equals(yc))
            {
                res = true;
                break;
            }
        }
        return res;
    }

    public void dump()
    {
        LOGGER.log(Level.INFO, "----CHANNEL SETTINGS (CTAB) channel={0} accType={1} name={2}", new Object[]
        {
            channel, accType, name
        });
        LOGGER.log(Level.INFO, "  mutedNotes={0}", mutedNotes);
        LOGGER.log(Level.INFO, "  autoStart={0}", autoStart);
        LOGGER.log(Level.INFO, "  mutedChords={0}", mutedChords);
        LOGGER.log(Level.INFO, "  sourceChordNote={0} sourceChordType={1}", new Object[]
        {
            sourceChordNote, sourceChordType
        });
        LOGGER.log(Level.INFO, "  ctb2MiddleLowPitch={0} ctb2MiddleHighPitch={1}", new Object[]
        {
            ctb2MiddleLowPitch, ctb2MiddleHighPitch
        });
        if (ctb2Low != null)
        {
            LOGGER.info("   -- CTB2 LOW");
            ctb2Low.dump();
        }
        LOGGER.info("   -- CTB2 MIDDLE/MAIN (CTAB)");
        ctb2Main.dump();
        if (ctb2High != null)
        {
            LOGGER.info("   -- CTB2 HIGH");
            ctb2High.dump();
        }
    }

}
