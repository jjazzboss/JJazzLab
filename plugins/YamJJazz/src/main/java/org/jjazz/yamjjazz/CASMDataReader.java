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
package org.jjazz.yamjjazz;

import org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.CtabChannelSettings;
import org.jjazz.yamjjazz.rhythm.api.StylePartType;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.NoteTranspositionRule;
import org.jjazz.yamjjazz.rhythm.api.Ctb2ChannelSettings.NoteTranspositionTable;

/**
 * Read CASM data of a Yamaha style file.
 * <p>
 * This will mainly create the StyleParts (with no music data) to the style.
 */
public class CASMDataReader
{

    private static final Logger LOGGER = Logger.getLogger(CASMDataReader.class.getSimpleName());
    private Style style;
    private String logName = "";

    public CASMDataReader(Style s, String logName)
    {
        if (s == null)
        {
            throw new NullPointerException("s");   //NOI18N
        }
        style = s;
        if (logName != null)
        {
            this.logName = logName;
        }

    }

    /**
     * Read CASM data from specified file.
     *
     * @param f
     * @throws java.io.IOException
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     * @todo manage cases with no CASM
     */
    public void read(File f) throws IOException, FormatNotSupportedException
    {
        // Buffer must be big enough to store the whole file.
        // With small buffer, in.skip(x) may stop at the end of buffer.
        // Set at 0x100000 = 1MB
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 0X100000)))
        {
            // Start of file
            readStringAndAssert(in, "MThd");
            readUIntAndAssert(in, 6);   // The header size
            in.skip(4);                 // File format + Nb of tracks => always Midi file 0 and Nb of tracks=1
            style.ticksPerQuarter = in.readUnsignedShort();

            // Skip the Midi track
            readStringAndAssert(in, "MTrk");
            long trackSize = readUInt(in);
            LOGGER.log(Level.FINE, "read() f=" + f.getName() + " MTrk tracksize={0}", hex(trackSize));
            in.skip(trackSize - 1);      // -1 to accomodate possible corrupted file, see below

            // We need a CASM here. 
            // Some files don't have a CASM, then is the end of file here. @todo Manage this no-CASM use case
            // There might be some errors in Midi files where CASM expected position differs from +1/-1 byte. 
            // Try to recover. 
            byte[] buf = new byte[4];
            in.read(buf);
            String s = Utilities.toString(buf);
            if (s.equals("CASM"))
            {
                // CASM was 1 byte too early, corrupted file ! we're good now
                LOGGER.log(Level.INFO, "{0} - read() wrong CASM position but we could recover", logName);
            } else if (s.substring(1).equals("CAS"))
            {
                // normal case
                readStringAndAssert(in, "M");
            } else if (s.substring(2).equals("CA"))
            {
                // CAS was 1 byte too late, corrupted file ! we're good now
                readStringAndAssert(in, "SM");
                LOGGER.log(Level.INFO, "{0} - read() wrong CASM position but we could recover", logName);
            } else
            {
                // Can't recover
                throw new FormatNotSupportedException(logName + " - read() can't find the 'CASM' string at expected position");
            }

            long sectionSize = readUInt(in);
            LOGGER.log(Level.FINE, "read() CASM sectionSize={0}", hex(sectionSize));
            buf = new byte[(int) sectionSize]; // cast to int is ok, CASM section should never be > 0X7FFF (32KBytes)
            in.readFully(buf);
            parseCASMdata(buf);
        }
    }

    private void parseCASMdata(byte[] casmData) throws IOException, FormatNotSupportedException
    {
        LOGGER.fine("parseCASMdata()");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(casmData));
        // CASM = one or more CSEG sections
        byte[] buf = new byte[4];
        while (in.read(buf) != -1)
        {
            String sectionName = Utilities.toString(buf);
            if (!sectionName.equals("CSEG"))
            {
                throw new FormatNotSupportedException(logName + " - CSEG section expected, found section name=" + sectionName);
            }
            long sectionSize = readUInt(in);
            buf = new byte[(int) sectionSize]; // cast to int is ok, CASM section should never be > 0X7FFF (32KBytes)
            in.readFully(buf);
            parseCSEGdata(buf);
            buf = new byte[4];
        }
    }

    private void parseCSEGdata(byte[] csegData) throws IOException, FormatNotSupportedException
    {
        LOGGER.fine("parseCSEGdata()");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(csegData));
        // CSEG = One Sdec section + Ctab sections + optional Cntt sections

        // Sdec section
        readStringAndAssert(in, "Sdec");
        long sectionSize = readUInt(in);
        LOGGER.log(Level.FINE, "parseCSEGdata() sSdec sectionSize={0}", sectionSize);
        byte[] buf = new byte[(int) sectionSize]; // cast to int is ok, small section
        in.read(buf);
        String s = Utilities.toString(buf); // E.g. "Main A,Main B,Intro A,Fill In AA,Ending A"
        LOGGER.log(Level.FINE, "parseCSEGdata() s={0}", s);
        String[] strs = s.split(" *, *");
        if (strs.length == 0)
        {
            throw new FormatNotSupportedException(logName + " - Empty string in Sdec section: s=" + s);
        }
        ArrayList<StylePart> impactedStyleParts = new ArrayList<>();
        for (String str : strs)
        {
            StylePartType t = StylePartType.getType(str);
            if (t == null)
            {
                throw new FormatNotSupportedException(logName + " - Unrecognised StylePart type in sdec section: s=" + s + " str=" + str);
            }
            // Create the StyleParts impacted by this CSEG section
            StylePart sp = style.addStylePart(t);
            impactedStyleParts.add(sp);
        }

        // Ctab sections
        buf = new byte[4];

        while (in.read(buf) != -1)
        {
            String sectionName = Utilities.toString(buf);
            switch (sectionName)
            {
                case "Ctab":
                case "Ctb2":
                case "Cntt":
                    break;
                default:
                    // Need to check this before allocating the buf[sectionSize] which can trigger an outOfMemoryException
                    throw new FormatNotSupportedException(logName + " - Unknown section name found in CSEG section: sectionName=" + sectionName);
            }
            sectionSize = readUInt(in);
            LOGGER.log(Level.FINE, "parseCSEGdata() Ctab sectionName=" + sectionName + " sectionSize={0}", sectionSize);
            buf = new byte[(int) sectionSize]; // cast to int is ok, small section
            in.readFully(buf);
            switch (sectionName)
            {
                case "Ctab":
                {
                    style.sffType = Style.SFFtype.SFF1;
                    CtabChannelSettings ctab = parseCtabData(buf);
                    for (StylePart sp : impactedStyleParts)
                    {
                        sp.setCtabChannelSettings(ctab.getChannel(), ctab);
                    }
                    break;
                }
                case "Ctb2":
                {
                    style.sffType = Style.SFFtype.SFF2;
                    CtabChannelSettings ctab = parseCtb2Data(buf);
                    for (StylePart sp : impactedStyleParts)
                    {
                        sp.setCtabChannelSettings(ctab.getChannel(), ctab);
                    }
                    break;
                }
                case "Cntt":
                {
                    // Cntt sections appear AFTER ctab sections
                    LOGGER.log(Level.FINE, "{0} - parseCSEGData() Cntt section parsed, overriding Ctab data", logName);
                    CnttData cnttData = parseCnttData(buf);
                    for (StylePart sp : impactedStyleParts)
                    {
                        // CNTT values override the ctab values
                        CtabChannelSettings ctab = sp.getCtabChannelSettings(cnttData.channel);
                        if (ctab != null)
                        {
                            ctab.ctb2Main.bassOn = cnttData.bassOn;
                            ctab.ctb2Main.ntt = cnttData.ntt;
                        } else
                        {
                            LOGGER.log(Level.INFO, "{0} - parseCSEGData() cntt processing, unexpected ctab=null for channel={1}", new Object[]{logName,
                                cnttData.channel});
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("Serious bug : we should never be here");   //NOI18N
            }
            buf = new byte[4];
        }
    }

    private CtabChannelSettings parseCtabData(byte[] ctabData) throws IOException, FormatNotSupportedException
    {
        // LOGGER.fine("parseCtabData()");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(ctabData));

        // Read the first generic part
        CtabChannelSettings cTab = parseCtabDataFirstPart(in);

        // Ctab has only only ctb2 subpart
        parseCtb2Subpart(in, cTab.ctb2Main);

        // Special feature last bytes
        int specialFeature = readUByte(in);
        if (specialFeature != 0)
        {
            in.skip(4);             // Extra bytes for the extra break drum voice (not processed, rare) 
        }
        return cTab;
    }

    private CtabChannelSettings parseCtb2Data(byte[] ctbData) throws IOException, FormatNotSupportedException
    {
        LOGGER.fine("parseCtb2Data()");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(ctbData));
        CtabChannelSettings cTab = this.parseCtabDataFirstPart(in);
        // Middle section lowest and highest notes
        int middleLowPitch = readUByte(in);
        if (middleLowPitch > 127)
        {
            throw new FormatNotSupportedException(logName + " - Invalid middle-lowest note value in Ctb2: " + middleLowPitch);
        }
        cTab.setCtb2MiddleLowPitch(middleLowPitch);         // This will also create cTab.ctb2Low if required
        int middleHighPitch = readUByte(in);
        if (middleHighPitch > 127)
        {
            throw new FormatNotSupportedException(logName + " - Invalid middle-highest note value in Ctb2: " + middleHighPitch);
        }
        cTab.setCtb2MiddleHighPitch(middleHighPitch);       // This will also create cTab.ctb2High if required
        

        // Parse each ctb2 subpart
        parseCtb2Subpart(in, cTab.ctb2Low);     
        parseCtb2Subpart(in, cTab.ctb2Main);
        parseCtb2Subpart(in, cTab.ctb2High);
        

        // Skip the final 7 "unknown bytes" at the end of the ctb2 section
        in.skip(7);

        return cTab;
    }

    /**
     * Read a Ctb2ChannelSettings section.
     *
     * @param in
     * @param ctb2 If null, read data is ignored.
     */
    private void parseCtb2Subpart(DataInputStream in, Ctb2ChannelSettings ctb2) throws IOException, FormatNotSupportedException
    {
        if (ctb2 == null)
        {
            in.skip(6);
            return;
        }

        // Note transposition rule
        int ntr = readUByte(in);
        if (ntr > 2)
        {
            throw new FormatNotSupportedException(logName + " - Invalid note transposition rule value in Ctab: " + ntr);
        } 
        ctb2.setNtr(ntr);

        // Note transposition table
        byte ntt = in.readByte();
        ctb2.bassOn = ((ntt & 0x80) == 0x80);
        int index = ntt & 0x7F;
        if (ctb2.ntr.equals(NoteTranspositionRule.GUITAR))
        {
            if (index >= 3)
            {
                throw new FormatNotSupportedException(logName + " - Invalid NTT/BassOn value for : " + ntt + ", ntr=" + ctb2.ntr);
            }
            index += 11; // Actually we put the GUITAR NTT value at the end of the non-guitar NTT values
        } else
        {
            if (index >= 11)
            {
                throw new FormatNotSupportedException(logName + " - Invalid NTT/BassOn value : " + ntt + ", ntr=" + ctb2.ntr);
            }
            if (style.sffType.equals(Style.SFFtype.SFF1))
            {
                // Need to adapt SFF1 NTT to SFF2 NTT
                if (index == 3)
                {
                    // 3 = SFF1 Bass
                    ctb2.bassOn = true;
                    index = 1; // Melody
                } else if (index == 4)
                {
                    index = 3; // Melodic minor
                } else
                {
                    // Do nothing, SFF1 index matches the SFF2 index
                }
            }
        }
        ctb2.ntt = NoteTranspositionTable.values()[index];

        // Chord root note upper limit
        int rootUpper = readUByte(in);
        if (rootUpper > 127)
        {
            throw new FormatNotSupportedException(logName + " - Invalid root note upper value in Ctab: " + rootUpper);
        }
        ctb2.chordRootUpperLimit = new Note(rootUpper);
        // Note limits
        int noteLow = readUByte(in);
        if (noteLow > 127)
        {
            throw new FormatNotSupportedException(logName + " - Invalid low limit note value in Ctab: " + noteLow);
        }
        ctb2.noteLowLimit = new Note(noteLow);
        int noteHigh = readUByte(in);
        if (noteHigh > 127)
        {
            throw new FormatNotSupportedException(logName + " - Invalid high limit note value in Ctab: " + noteHigh);
        }
        ctb2.noteHighLimit = new Note(noteHigh);
        // Retrigger rule
        int rtr = readUByte(in);
        if (rtr > 5)
        {
            throw new FormatNotSupportedException(logName + " - Invalid retrigger rule value in Ctab: " + rtr);
        }
        ctb2.setRetriggerRule(rtr);
    }

    /**
     * Ctab and ctb2 structures have a common first part.
     *
     * @param in
     * @return
     */
    private CtabChannelSettings parseCtabDataFirstPart(DataInputStream in) throws IOException, FormatNotSupportedException
    {
        int srcChannel = readUByte(in);
        if (!MidiConst.checkMidiChannel(srcChannel))
        {
            throw new FormatNotSupportedException(logName + " - Invalid source channel value in Ctab: " + srcChannel);
        }
        CtabChannelSettings cTab = new CtabChannelSettings(srcChannel);
        cTab.name = readString(in, 8).trim();
        int destChannel = readUByte(in);
        if (destChannel < 8 || destChannel > 15)
        {
            throw new FormatNotSupportedException(logName + " - Invalid dest channel value in Ctab: " + destChannel);
        }
        cTab.accType = AccType.getAccType(destChannel);
        LOGGER.log(Level.FINE, "parseCtabData()  channel={0} name={1} accType={2}", new Object[]
        {
            srcChannel, cTab.name, cTab.accType
        });
        cTab.editable = (in.readByte() == 0);
        // Bitfields for muted notes
        byte b1 = in.readByte();
        byte b2 = in.readByte();
        cTab.setMutedNotes(b1, b2);
        // Bitfields for muted chords
        b1 = in.readByte();
        b2 = in.readByte();
        byte b3 = in.readByte();
        byte b4 = in.readByte();
        byte b5 = in.readByte();
        cTab.setMutedChords(b1, b2, b3, b4, b5);
        // Source chord key
        int pitch = readUByte(in);
        if (pitch > 11)
        {
            throw new FormatNotSupportedException(logName + " - Invalid source chord note value in Ctab: " + pitch);
        }
        cTab.sourceChordNote = new Note(pitch);
        // Source chord type
        int srcChordType = readUByte(in);
        if (srcChordType > 0x22)
        {
            throw new FormatNotSupportedException(logName + " - Invalid source chord type value in Ctab: " + srcChordType);
        }
        cTab.setSourceChordType(srcChordType);
        return cTab;
    }

    /**
     * Parse one Cntt section and return
     *
     * @param cnttData
     * @return
     * @throws IOException
     * @throws FormatNotSupportedException
     */
    private CnttData parseCnttData(byte[] cnttData) throws IOException, FormatNotSupportedException
    {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(cnttData));
        int channel = in.readByte();
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new FormatNotSupportedException(logName + " - Invalid Midi channel: " + channel);
        }
        byte nttBass = in.readByte();
        boolean bassOn = ((nttBass & 0x80) == 0x80);
        int index = nttBass & 0x7F;
        if (index >= NoteTranspositionTable.values().length)
        {
            throw new FormatNotSupportedException(logName + " - Invalid NTT/BassOn value : " + nttBass);
        }
        NoteTranspositionTable ntt = NoteTranspositionTable.values()[index];
        return new CnttData(channel, ntt, bassOn);
    }

    private long readUInt(DataInputStream in) throws IOException
    {
        return (long) (in.readInt() & 0xFFFFFFFF);
    }

    private int readUByte(DataInputStream in) throws IOException
    {
        return in.readByte() & 0xFF;
    }

    /**
     * Read s.length() bytes and check the resulting string is equals to s.
     *
     * @param s
     * @return The number of bytes actually read, -1 if end of file reached.
     */
    private int readStringAndAssert(InputStream in, String s) throws IOException, FormatNotSupportedException
    {
        byte[] buf = new byte[s.length()];
        int rb = in.read(buf);
        String s2 = Utilities.toString(buf);
        if (!s.equals(s2))
        {
            throw new FormatNotSupportedException(logName + " - Expected string=" + s + ", found string=" + s2 + " (" + hex(buf) + ")");
        }
        return rb;
    }

    private String readString(InputStream in, int nbChars) throws IOException
    {
        byte[] buf = new byte[nbChars];
        in.read(buf);
        return Utilities.toString(buf);
    }

    /**
     * Read an unsigned int value and check it is equal to ui
     *
     * @param ui
     */
    private void readUIntAndAssert(DataInputStream in, long ui) throws IOException, FormatNotSupportedException
    {
        long ui2;
        ui2 = readUInt(in);
        if (ui != ui2)
        {
            throw new FormatNotSupportedException(logName + " - Expected UInt=" + ui + ", found UInt=" + ui2);
        }
    }

    private String hex(long l)
    {
        return "0x" + Long.toHexString(l);
    }

    private String hex(byte[] buf)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf)
        {
            if (sb.length() != 0)
            {
                sb.append(",");
            }
            int uByte = b & 0xFF;
            sb.append("0x" + Integer.toHexString(uByte));
        }
        return sb.toString();
    }

    private class CnttData
    {

        int channel;
        Ctb2ChannelSettings.NoteTranspositionTable ntt;
        boolean bassOn;

        public CnttData(int c, Ctb2ChannelSettings.NoteTranspositionTable ntt, boolean bass)
        {
            this.channel = c;
            this.ntt = ntt;
            this.bassOn = bass;
        }
    }
}
