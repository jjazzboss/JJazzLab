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
package org.jjazz.importers.api;

import org.jjazz.importers.biab.SongBuilder;
import org.jjazz.importers.biab.BiabChord;
import org.jjazz.song.api.SongCreationException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Division;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.Utilities;

/**
 * Band In A Box file reader.
 * <p>
 * The reader retrieves data from the BIAB file and creates a Song. The ChordLeadSheet is complete and the SongStructure reflects the structure of the BIAB
 * file, including the chorus repeats. SongPart names are also updated.<p>
 * What remains to be done is to customize the SongParts rhythm/style parameters.
 * <p>
 * Not parsed yet, although it would have an impact on the JJazzLab song :<br>
 * - Repeats/2nd Endings/DC Al copa, etc.<br>
 * - Start 2 bars early when generate 2 bars end is ON<br>
 * - Chord Pedal bass 2/4 etc.<br>
 * - Bar settings: style changes, tempo changes, nb of beats changes<br>
 */
public class BiabFileReader
{

    private static final boolean STRICT = true;  // If true don't allow possibly handable errors
    private static final int NB_BARS_MAX = 255;
    public File file;
    public TimeSignature timeSignature;
    public boolean swing;
    private int byteIndex;     // Keep track of where we are in the file
    private int styleFamilyId;
    public String styleFileName;
    public BiabStyleFeatures styleFeatures;     // Provide data that might help find the appropriate rhythm
    public int version;
    public String title;
    public boolean overallLoop;
    public int keyId;
    public int tempo;
    public int startBar;
    public int chorusStart;
    public int chorusEnd;
    public int chorusNbRepeats;
    public boolean varyStyleInMiddleChorus;
    public boolean useTagJump;
    public int tagBeginBar, tagAfterBar, tagEndBar;
    public int hasSoloTrack = 0;
    public int nbMelodyNotes = 0;
    public int gmPatchBass = -1;
    public int gmPatchDrums = -1;
    public int gmPatchPiano = -1;
    public int gmPatchGuitar = -1;
    public int gmPatchStrings = -1;
    public int gmPatchSoloist = -1;
    public int gmPatchMelodist = -1;
    public int gmPatchThru = -1;
    public boolean allowPushInMiddleChorus;
    public boolean allowRestInFirstChorus;
    public boolean allowRestInMiddleChorus;
    public boolean allowRestInLastChorus;
    public boolean generate2barsEnding = true;      // Inversed !
    public boolean forceSongToSimpleArrangement;
    /**
     * Key=bar index. ZeroBased 0.
     */
    public TreeMap<Integer, Integer> mapBiabBarMarker = new TreeMap<>(); // To get the natural ordering by key (barIndex).  
    public TreeMap<Integer, Integer> mapClsBarMarker = new TreeMap<>(); // The equivalent but with ChordLeadSheet barindexes keys.

    /**
     * Key=chord slot index (there is 4 slots per bar, whatever the time signature). ZeroBased 0.
     */
    public TreeMap<Integer, BiabChord> chords = new TreeMap<>();

    private static final Logger LOGGER = Logger.getLogger(BiabFileReader.class.getSimpleName());

    /**
     * Create a reader.
     * <p>
     * There is no time signature and Feel information in BIAB files, so if user knows this info it should pass it via the parameters.
     *
     * @param f
     * @param ts    Can be null, in this case, algorithm will try to guess the TimeSignature based on the file name.
     * @param swing True if swing style
     */
    public BiabFileReader(File f, TimeSignature ts, boolean swing)
    {
        if (f == null)
        {
            throw new NullPointerException("f");   //NOI18N
        }
        this.file = f;
        this.timeSignature = ts;
        this.swing = swing;
    }

    /**
     * Read the file and construct a song.
     * <p>
     * Update only the chordleadsheet: songstructure details must be set by a rhythm provider.
     *
     * @return @throws IOException
     * @throws org.jjazz.song.api.SongCreationException
     */
    public Song readFile() throws IOException, SongCreationException
    {
        if (byteIndex > 0)
        {
            throw new IllegalStateException("readFile() can be only called once for a given reader instance.");   //NOI18N
        }
        try
        {
            readFileData(false);
        } catch (SongCreationException ex)
        {
            if (ex.getMessage().equals("RETRY"))
            {
                LOGGER.info("readFile() caught RETRY special exception, retrying...");
                readFileData(true);
            } else
            {
                throw ex;
            }
        }
        Song song = new SongBuilder(this).buildSong();
        return song;
    }

    private void readFileDataOldFormat(DataInputStream in) throws IOException, SongCreationException
    {
        // Song title
        title = readString(in, readUByte(in));
        LOGGER.log(Level.FINE, "readFileDataOldFormat() title={0} byteIndex={1}", new Object[]
        {
            title, hex(byteIndex - title.length())
        });

        // Skip to byte 0x3e
        skipBytes(in, 0x3e - byteIndex);

        // Style family 
        styleFamilyId = readUByte(in);
        styleFeatures = BiabStyleFeatures.getStyleFeatures(styleFamilyId);
        if (styleFeatures != null)
        {
            timeSignature = styleFeatures.timeSignature;
            swing = styleFeatures.division == Division.EIGHTH_SHUFFLE;
        }
        LOGGER.log(Level.FINE, "readFileDataOldFormat() styleFamilyId={0}, timeSignature={1}, swing={2} byteIndex={3}", new Object[]
        {
            styleFamilyId,
            timeSignature, swing, hex(byteIndex - 1)
        });

        // Key - Not used 
        keyId = readUByte(in);
        LOGGER.log(Level.FINE, "readFileDataOldFormat() keyId={0} byteIndex={1}", new Object[]
        {
            keyId, hex(byteIndex - 1)
        });


        // Tempo
        tempo = readUByte(in) + (readUByte(in) << 8);
        LOGGER.log(Level.FINE, "readFileDataOldFormat() tempo={0} byteIndex={1}", new Object[]
        {
            tempo, hex(byteIndex - 2)
        });

        // Skip to byte 0x42
        skipBytes(in, 0x42 - byteIndex);

        // Read bar markers (64 bars max)
        LOGGER.log(Level.FINE, "readFileDataOldFormat() reading bar markers >>>>>>>>>> byteIndex={0}", hex(byteIndex));
        for (int barIndex = 0; barIndex < 64; barIndex++)
        {
            int value = readUByte(in);
            if (value > 0)
            {
                // value=marker type: 1=a, 2=b, 3=c, 4=d
                mapBiabBarMarker.put(barIndex, value);
            }
        }
        LOGGER.log(Level.FINE, "readFileDataOldFormat()   mapBarMarker={0}", mapBiabBarMarker);


        // Skip to byte 0x82
        skipBytes(in, 0x82 - byteIndex);


        // Read chord types     
        LOGGER.log(Level.FINE, "readFileDataOldFormat() reading chord types >>>>>>>>>> byteIndex={0}", hex(byteIndex));
        for (int index = 0; index < 256; index++)
        {
            int value = readUByte(in);
            if (value != 0)
            {
                BiabChord chord = new BiabChord(index);
                chord.setChordType(value);
                chords.put(index, chord);
            }
        }
        // LOGGER.fine("readFileDataOldFormat()   chords=" + getChordsAsString());


        // Read chord root/bass    
        LOGGER.log(Level.FINE, "readFileDataOldFormat() reading chord root/bass >>>>>>>>>> byteIndex={0}", hex(byteIndex));
        for (int index = 0; index < 256; index++)
        {
            int value = readUByte(in);
            if (value != 0)
            {
                BiabChord chord = chords.get(index);
                if (chord == null)
                {
                    LOGGER.log(Level.SEVERE, "readFileDataOldFormat() (root/bass) No chord found for index={0} at byte offset={1}. chords={2}", new Object[]
                    {
                        index,
                        hex(byteIndex - 1), getChordsAsString()
                    });
                    genericError(byteIndex - 1);
                    return;
                }

                // Translate value in root/bass notes
                chord.setChordBase(value);
            }
        }
        // LOGGER.fine("readFileDataOldFormat()   chords=" + getChordsAsString());


        // Chorus end
        skipBytes(in, 2);
        chorusEnd = readUByte(in);


        // Use default values for other values
        chorusStart = 1;
        chorusNbRepeats = 3;
        startBar = 1;
        generate2barsEnding = false;
    }

    /**
     *
     * @param useByteShift Use the optional byte offset after reading .STY see comments below for the "Melody and Style patch byte suite"
     * @throws IOException
     * @throws SongCreationException
     */
    private void readFileData(boolean useByteShift) throws IOException, SongCreationException
    {
        LOGGER.fine("\n\n------------------------------------------------------");
        LOGGER.log(Level.FINE, "readFileData() -- useByteShift={0} reading file={1}", new Object[]
        {
            useByteShift, file.getAbsolutePath()
        });


        // Buffer must be big enough to store the whole file.
        // With small buffer, in.skip(x) may stop at the end of buffer.
        // Set at 0x100000 = 1MB
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 0X100000)))
        {
            // BIAB file version
            version = readUByte(in);
            switch (version)
            {
                case 0x43:  // 0x43 Probably no ChordPush series at all or completly different (rainrain.sgu)
                case 0x44:  // 0x44 Impact on pre-ChordPush Series  (git_test.mgu, R&HCOLL)
                case 0x45:
                case 0x46:
                case 0x47:  // Impact on ChordPush data: unknown data ? (SOLODEMO.SGU)
                case 0x48:
                case 0x49:  // Works OK
                case 0xbc:  // old format!                    
                case 0xbb: // old format!                   
                    break;
                default:
                    throw new SongCreationException(file.getName() + ": BIAB file version not supported: " + hex(version));
            }
            LOGGER.log(Level.FINE, "readFileData() version={0} byteIndex={1}", new Object[]
            {
                hex(version), hex(byteIndex - 1)
            });


            if (version == 0xbb || version == 0xbc)
            {
                // Special case
                LOGGER.log(Level.WARNING, "readFileData() old file format detected ({0}), import will be limited to chord symbols", hex(version));
                readFileDataOldFormat(in);
                return;
            }


            // Song title 
            title = readString(in, readUByte(in));
            LOGGER.log(Level.FINE, "readFileData() title={0} byteIndex={1}", new Object[]
            {
                title, hex(byteIndex - title.length())
            });


            // Skip to byte 0x3E
            processSuite(in, byteIndex, 0x3D, (index, value) -> 
            {
                // Do nothing
                LOGGER.log(Level.FINE, "readFileData() Unusual index/value index={0}, value={1}, byteIndex={2}", new Object[]
                {
                    index,
                    hex(value), hex(byteIndex - 1)
                });
            });


            // Byte index=0x3E
            // Style family : seems to be no more used in tested files, styleFeatures is always 1!
            // If it worked (see the perl script), could also give the time signature
            styleFamilyId = readUByte(in);
            LOGGER.log(Level.FINE, "readFileData() styleFamilyId={0}", styleFamilyId);


            // Key - Not used 
            keyId = readUByte(in);
            LOGGER.log(Level.FINE, "readFileData() keyId={0} byteIndex={1}", new Object[]
            {
                keyId, hex(byteIndex - 1)
            });


            // Tempo
            tempo = readUByte(in) + (readUByte(in) << 8);
            LOGGER.log(Level.FINE, "readFileData() tempo={0} byteIndex={1}", new Object[]
            {
                tempo, hex(byteIndex - 2)
            });
            if (tempo > 500)
            {
                LOGGER.log(Level.SEVERE, "readFileData() Invalid tempo value={0} at byte offset={1}", new Object[]
                {
                    tempo, hex(byteIndex - 1)
                });
                genericError(byteIndex - 1);
                return;
            }


            // Bar markers suite
            LOGGER.log(Level.FINE, "readFileData() reading bar markers >>>>>>>>>> byteIndex={0}", hex(byteIndex));
            startBar = readUByte(in);           // always 1 ?
            processSuite(in, startBar, NB_BARS_MAX - 1, (barIndex, value) -> 
            {
                // Save the barIndexes for which there is a marker defined
                // value=marker type: 1=a, 2=b, 3=c, 4=d
                mapBiabBarMarker.put(barIndex - 1, value);
            });
            LOGGER.log(Level.FINE, "readFileData()   mapBarMarker={0}", mapBiabBarMarker);


            // Chord types suite
            // The chords are listed as they appear in the song, without taking into account any structure info (repeats, coda, tags, etc.)
            // BIAB has 4 chords slots per bar (don't care the time signature)
            LOGGER.log(Level.FINE, "readFileData() reading chord types >>>>>>>>>>>>>>>>>>> byteIndex={0}", hex(byteIndex));
            processSuite(in, 0, (NB_BARS_MAX * 4) - 1, (index, value) -> 
            {
                // Save the chordIndex for which there is a chord type defined
                // index=chordIndex
                // value=chord type                             
                BiabChord chord = new BiabChord(index);
                chord.setChordType(value);
                chords.put(index, chord);
            });
            // LOGGER.fine("readFileData()   chords=" + getChordsAsString());


            // Chord roots suite : coded to contain the optional bass note too
            LOGGER.log(Level.FINE, "readFileData() reading chord root/bass notes >>>>>>>>>>>>>>>>>> byteIndex={0}", hex(byteIndex));
            int nextIndex = processSuite(in, 0, (NB_BARS_MAX * 4) - 1, (index, value) -> 
            {
                // Save the chord root and bass notes for each chord
                // idx=chordIndex
                // value=chord root id     

                BiabChord chord = chords.get(index);
                if (chord == null)
                {
                    LOGGER.log(Level.SEVERE, "readFileData() (root/bass) No chord found for index={0} at byte offset={1}. chords={2}", new Object[]
                    {
                        index,
                        hex(byteIndex - 1), getChordsAsString()
                    });
                    genericError(byteIndex - 1);
                    return;
                }

                // Translate value in root/bass notes
                chord.setChordBase(value);
                // LOGGER.severe(index+": value=" + value + " => root=" + chord.rootNote + " bass=" + chord.bassNote);
            });
            // LOGGER.fine("readFileData()  chords with root/bass=" + getChordsAsString());


            // Song structure data suite
            int startIndex = nextIndex - (NB_BARS_MAX * 4);     // Should be 0 or 1
            LOGGER.log(Level.FINE, "readFileData() reading overaAll, chorus and tags data >>>>>>>>>>>>>>>>>>> byteIndex={0}", hex(byteIndex));
            LOGGER.log(Level.FINE, "readFileData()   (startIndex={0})", startIndex);
            nextIndex = processSuite(in, startIndex, 8, (index, value) -> 
            {
                switch (index)
                {
                    case 0:
                        overallLoop = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   overallLoop={0}, byteIndex={1}", new Object[]
                        {
                            overallLoop,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 1:
                        chorusStart = value;
                        LOGGER.log(Level.FINE, "readFileData()   chorusStart={0}, byteIndex={1}", new Object[]
                        {
                            chorusStart,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 2:
                        chorusEnd = value;
                        LOGGER.log(Level.FINE, "readFileData()   chorusEnd={0}, byteIndex={1}", new Object[]
                        {
                            chorusEnd, hex(byteIndex - 1)
                        });
                        break;
                    case 3:
                        chorusNbRepeats = value;
                        LOGGER.log(Level.FINE, "readFileData()   chorusNbRepeats={0}, byteIndex={1}", new Object[]
                        {
                            chorusNbRepeats,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 4:
                        varyStyleInMiddleChorus = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   varyStyleInMiddleChorus={0}, byteIndex={1}", new Object[]
                        {
                            varyStyleInMiddleChorus,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 5:
                        useTagJump = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   useTagJump={0}, byteIndex={1}", new Object[]
                        {
                            useTagJump, hex(byteIndex - 1)
                        });
                        break;
                    case 6:
                        tagAfterBar = value;
                        LOGGER.log(Level.FINE, "readFileData()   tagAfterBar={0}, byteIndex={1}", new Object[]
                        {
                            tagAfterBar,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 7:
                        tagBeginBar = value;
                        LOGGER.log(Level.FINE, "readFileData()   tagBeginBar={0}, byteIndex={1}", new Object[]
                        {
                            tagBeginBar,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 8:
                        tagEndBar = value;
                        LOGGER.log(Level.FINE, "readFileData()   tagEndBar={0}, byteIndex={1}", new Object[]
                        {
                            tagEndBar, hex(byteIndex - 1)
                        });
                        break;
                    default:
                        throw new IllegalStateException("index=" + index + " value=" + value);   //NOI18N
                }
            });


            // With older versions the data after ".STY" is different, don't try to read, too many problems
            if (version <= 0x44)
            {
                LOGGER.log(Level.INFO, "readFileData() old file format detected version ({0}), import is limited to chord symbols and song structure.", hex(
                        version));
                return;
            }

            // 
            // Skip data : probably Bar settings stuff : patch changes, tempo changes, nb beats per bar, etc. 
            //
            LOGGER.log(Level.FINE, "readFileData() skipping bar related data >>>>>>>>>>>>>>>>>>> byteIndex={0}", hex(byteIndex));


            // Search style filename using ".STY"
            // Use a buffer to retrieve the complete style filename once we got the .sty
            Fifo<Character> fifo = new Fifo<>(13);    // .sty filename size=12 + 1 for string size
            boolean found = false;
            try
            {
                while (!found)
                {
                    // Might generate EOFException if not found
                    while (fifo.myPush((char) readUByte(in)) != '.');

                    if (fifo.myPush((char) readUByte(in)) == 'S' && fifo.myPush((char) readUByte(in)) == 'T' && fifo.myPush((char) readUByte(in)) == 'Y')
                    {
                        found = true;
                    }
                }
            } catch (EOFException ex)
            {
                LOGGER.info("readFileData() tag missing, import is limited to chord symbols and song structure");
                return;
            }

            // Retrieve file name, fifo content is "YTS.otot8" if file name is "toto.STY"
            int fifoIndex = fifo.size() - 1;
            while (fifo.get(fifoIndex) > 12)        // Find the string size
            {
                fifoIndex--;
            }
            StringBuilder sb = new StringBuilder();
            fifoIndex++;
            while (fifoIndex < fifo.size())
            {
                sb.append(fifo.get(fifoIndex));
                fifoIndex++;
            }
            styleFileName = sb.toString();

            LOGGER.log(Level.FINE, "readFileData() found .STY styleFileName={0} >>>>>>>>>>>>>>>>>>>>>>>> byteIndex={1}", new Object[]
            {
                styleFileName,
                hex(byteIndex - 4)
            });


            // Usually 00-FF, but sometimes xx 00 FF, xx yy 00 FF, xx yy zz 00 FF with xx and yy and zz > 0. 
            // Also case with 00-FF 00-FF !!! SOLODEMO.SGU 
            // Meaning is unknown: it seems to change if Generate 2 bar ending or Force simple arrangement change, but not clear.
            // Handle this as a byte suite so that the next suite processing works whatever the possible data here
            processSuite(in, 0, 0xFE, (index, value) -> 
            {
                // Do nothing
                LOGGER.log(Level.FINE, "readFileData() Unusual index/value index={0}, value={1}, byteIndex={2}", new Object[]
                {
                    index,
                    hex(value), hex(byteIndex - 1)
                });
            });


            // Melody and Style patch byte suite
            // 
            // Normally starts at 0 (whatever happened in the previous byte suite)
            //
            // BUT for Matt's (BIAB 2019?) Madalena In MGU or testStyleWithReadlDrumsBis.sgu, it must to be -1 to work. There are NO other 
            // byte difference BEFORE the byte shift except the style name which starts with a "-" (means Style with RealDrums track).
            // There are other differences but AFTER the byte shift.
            // 
            // When file is saved from my BIAB 2012 (with no real tracks I think), the problem does not occur, even when using 
            // a style which starts with a "-". If I load Matt's problematic file in BIAB 2012 then save it, problem is solved.
            startIndex = useByteShift ? -1 : 0;

            LOGGER.log(Level.FINE, "readFileData() reading melody and syle patches data >>>>>>>>>>>>>>>>>>> byteIndex={0}", hex(byteIndex));
            nextIndex = processSuite(in, startIndex, 0x1C, (index, value) -> 
            {
                switch (index)
                {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 0x0a:
                    case 0x0b:
                    case 0x0c:
                        LOGGER.log(Level.FINE, "readFileData()   Unusual data index={0}, value={1}, byteIndex={2}", new Object[]
                        {
                            hex(index),
                            hex(value), hex(byteIndex - 1)
                        });
                        break;
                    case 0x0d:
                        nbMelodyNotes = value;
                        LOGGER.log(Level.FINE, "readFileData()   nbMelodyNotes (LSB)={0}, byteIndex={1}", new Object[]
                        {
                            nbMelodyNotes,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x0e:
                        nbMelodyNotes += 256 * value;
                        LOGGER.log(Level.FINE, "readFileData()   nbMelodyNotes (MSB1)={0}, byteIndex={1}", new Object[]
                        {
                            nbMelodyNotes,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x0f:
                        LOGGER.log(Level.FINE, "readFileData()   Unusual data index={0}, value={1}, byteIndex={2}", new Object[]
                        {
                            hex(index),
                            hex(value), hex(byteIndex - 1)
                        });
                        break;
                    case 0x10:
                        gmPatchBass = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchBass={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchBass,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x11:
                        gmPatchPiano = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchPiano={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchPiano,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x12:
                        gmPatchDrums = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchDrums={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchDrums,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x13:
                        gmPatchGuitar = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchGuitar={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchGuitar,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x14:
                        gmPatchSoloist = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchSoloist={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchSoloist,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x15:
                        gmPatchStrings = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchStrings={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchStrings,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x16:
                        gmPatchMelodist = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchMelodist={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchMelodist,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x17:
                        gmPatchThru = value;
                        LOGGER.log(Level.FINE, "readFileData()   gmPatchThru={0}, byteIndex={1}", new Object[]
                        {
                            gmPatchThru,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 0x18:
                    case 0x19:
                    case 0x1A:
                    case 0x1B:
                    case 0x1C:
                        LOGGER.log(Level.INFO, "readFileData()   Unusual data index={0}, value={1}, byteIndex={2}", new Object[]
                        {
                            index,
                            hex(value), hex(byteIndex - 1)
                        });
                        break;
                    default:
                        throw new IllegalStateException("index=" + index + " value=" + value);   //NOI18N
                }
            });


            // Chord push suite
            // Start at index=0x1D=29 
            startIndex = nextIndex - 0x1D;
            LOGGER.log(Level.FINE, "readFileData() nextIndex={0}, startIndex={1}  reading chord pushes >>>>>>>>>>>>>>>>>>> byteIndex={2}", new Object[]
            {
                nextIndex,
                startIndex, hex(byteIndex)
            });
            final int CHORD_PUSH_SUITE_SIZE = 1024;
            int[] nbPushedChords =
            {
                0
            };
            nextIndex = processSuite(in, startIndex, CHORD_PUSH_SUITE_SIZE - 1, (index, value) -> 
            {
                // Save the chordIndex for which there is a chord push defined
                // index=chordIndex
                // value=chord push, 1=1/8, 2=1/16                             

                BiabChord chord = chords.get(index);
                if (chord == null)
                {
                    // It can be a real error of the algorithm, like due to changes linked to file version (see CHORD_PUSH_SUITE_SIZE example above)
                    // Or it can be a small problem in the file, eg git_text.mgu which has a Push1/16 chord settings on a bar far after the end
                    // and with no chord set !
                    String m = "readFileData() (Push) No chord found for index=" + index + " at byte offset=" + hex(byteIndex - 1);
                    if (STRICT)
                    {
                        LOGGER.fine(m);
                        LOGGER.log(Level.FINE, "  chords={0}", getChordsAsString());
                        if (!useByteShift)
                        {
                            // Raise a specific Exception which will be catched so we can retry with useByteShift ON
                            throw new SongCreationException("RETRY");
                        }
                        genericError(byteIndex - 1);
                    } else
                    {
                        LOGGER.warning(m);
                        LOGGER.log(Level.FINE, "  chords={0}", getChordsAsString());
                    }
                } else
                {
                    // Save push value
                    chord.setPush(value);
                    nbPushedChords[0]++;
                }
            });
            // LOGGER.fine("readFileData() nbPushedChords=" + nbPushedChords[0] + " nextIndex=" + nextIndex + ",   chords with pushes=" + getChordsAsString());


            // Chord hold/rest/shot suite
            // Start at index=29+1024=1053 for the usual 1020 beats
            startIndex = nextIndex - CHORD_PUSH_SUITE_SIZE;
            LOGGER.log(Level.FINE, "readFileData() startIndex={0} reading chord hold/rest/shot >>>>>>>>>>>>>>>>>>> byteIndex={1}", new Object[]
            {
                startIndex,
                hex(byteIndex)
            });
            final int CHORD_REST_SUITE_SIZE = 1020;
            int[] nbRestChords =
            {
                0
            };
            nextIndex = processSuite(in, startIndex, CHORD_REST_SUITE_SIZE - 1, (index, value) -> 
            {
                // Save the chordIndex for which there is a chord hold/rest/shot defined
                // index=chordIndex
                // value=chord hold/rest/shot code                            

                // Compute beat position
                BiabChord chord = chords.get(index);
                if (chord == null)
                {
                    String m = "readFileData() (Rest) No chord found for index=" + index + " at byte offset=" + hex(byteIndex - 1) + ". chords=" + getChordsAsString();
                    if (STRICT)
                    {
                        LOGGER.fine(m);
                        LOGGER.log(Level.FINE, "  chords={0}", getChordsAsString());
                        if (!useByteShift)
                        {
                            // Raise a specific Exception which will be catched so we can retry with useByteShift ON
                            throw new SongCreationException("RETRY");
                        }
                        genericError(byteIndex - 1);
                    } else
                    {
                        LOGGER.warning(m);
                        LOGGER.log(Level.FINE, "  chords={0}", getChordsAsString());
                    }
                } else
                {
                    // Save rest data: type, excluded instruments, excludedInstrumentsShouldRest
                    chord.setRest(value);
                    nbRestChords[0]++;
                }
            });
            // LOGGER.fine("readFileData() nbRestChords=" + nbRestChords[0] + " nextIndex=" + nextIndex + ",  chords with hold/rest/shot=" + getChordsAsString());


            // Song settings data suite
            startIndex = nextIndex - CHORD_REST_SUITE_SIZE;
            LOGGER.log(Level.FINE, "readFileData() startIndex={0} reading song settings >>>>>>>>>>>>>>>>>>> byteIndex={1}", new Object[]
            {
                startIndex,
                hex(byteIndex)
            });
            processSuite(in, startIndex, 7, (index, value) -> 
            {
                switch (index)
                {
                    case 0:
                        allowPushInMiddleChorus = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   allowPushInMiddleChorus={0}, byteIndex={1}", new Object[]
                        {
                            allowPushInMiddleChorus,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 1:
                        allowRestInMiddleChorus = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   allowRestInMiddleChorus={0}, byteIndex={1}", new Object[]
                        {
                            allowRestInMiddleChorus,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 2:
                        allowRestInLastChorus = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   allowRestInLastChorus={0}, byteIndex={1}", new Object[]
                        {
                            allowRestInLastChorus,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 3:
                        // Inverse setting : by default it's true, if "1" in the file, it's false
                        generate2barsEnding = !(value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   generate2barsEnding={0}, byteIndex={1}", new Object[]
                        {
                            generate2barsEnding,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 4:
                        forceSongToSimpleArrangement = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   forceSongToSimpleArrangement={0}, byteIndex={1}", new Object[]
                        {
                            forceSongToSimpleArrangement,
                            hex(byteIndex - 1)
                        });
                        break;
                    case 5:
                        // Unknown byte
                        break;
                    case 6:
                        // Unknown byte
                        break;
                    case 7:
                        allowRestInFirstChorus = (value != 0);
                        LOGGER.log(Level.FINE, "readFileData()   allowRestInFirstChorus={0}, byteIndex={1}", new Object[]
                        {
                            allowRestInFirstChorus,
                            hex(byteIndex - 1)
                        });
                        break;
                    default:
                        throw new IllegalStateException("index=" + index + " value=" + value);   //NOI18N
                }
            });

            LOGGER.log(Level.FINE, "readFileData() done reading BIAB data byteIndex={0}", hex(byteIndex));

        }   // End try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 0X100000)))   // End try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 0X100000)))   // End try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 0X100000)))   // End try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 0X100000)))

    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================

    /**
     * Read a string of max nbChars.
     * <p>
     * Increase byteIndex of nb of read bytes.
     *
     * @param in
     * @param n
     * @throws IOException
     */
    private String readString(InputStream in, int nbChars) throws IOException
    {
        byte[] buf = new byte[nbChars];
        byteIndex += in.read(buf);
        return Utilities.toString(buf);
    }

    /**
     * Skip unsigned bytes.
     * <p>
     * Increase byteIndex +n.
     *
     * @param in
     * @param n
     * @throws IOException
     */
    private void skipBytes(DataInputStream in, int n) throws IOException
    {
        in.skipBytes(n);
        byteIndex += n;
    }

    /**
     * Read an unsigned byte.
     * <p>
     * Increase byteIndex +1.
     *
     * @param in
     * @return
     * @throws IOException
     */
    private int readUByte(DataInputStream in) throws IOException
    {
        byteIndex++;
        return in.readUnsignedByte();
    }

    private String hex(long l)
    {
        return "0x" + Long.toHexString(l);
    }


    /**
     * Process a BIAB byte suite.
     * <p>
     * A BIAB suite works as follow, starting at index=startIndex.<br>
     * - If byte is &gt; 0 then value[index] is defined and equals to byte, index++.<br>
     * - If byte is 0, then value[index] is not defined and next byte gives the index offset, i.e. index += byte[index+1].
     * <p>
     * If value[index] is defined then call consumer(index, value[index]).
     *
     * @param in
     * @param startIndex
     * @param maxIndex   Process the suite while index &lt;= indexMax
     * @param p
     * @return The final value of index
     */
    private int processSuite(DataInputStream in, int startIndex, int maxIndex, ByteSuiteProcessor p) throws IOException, SongCreationException
    {
        int index = startIndex;
        while (index <= maxIndex)
        {
            int value = readUByte(in);
            if (value == 0)
            {
                int indexOffset = readUByte(in);
                if (indexOffset == 0)
                {
                    LOGGER.log(Level.SEVERE, "processSuite() Unexpected indexOffset=0, index={0} byteIndex={1}", new Object[]
                    {
                        index,
                        hex(byteIndex - 1)
                    });
                    genericError(byteIndex - 1);
                }
                index += indexOffset;
            } else
            {
                p.accept(index, value);
                index++;
            }
        }
        return index;
    }

    private void genericError(int offset) throws SongCreationException
    {
        throw new SongCreationException(file.getName() + ": Unexpected data at byte offset=" + hex(offset) + ". Consult log for details.");
    }

    /**
     * Return a multiline string showing the chords contents.
     *
     * @return
     */
    private String getChordsAsString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int index : chords.navigableKeySet())
        {
            sb.append(String.valueOf(index)).append("->").append(chords.get(index)).append("\n");
        }
        return sb.toString();
    }


    // ====================================================================================
    // Private classes
    // ====================================================================================
    private interface ByteSuiteProcessor
    {

        void accept(int index, int value) throws IOException, SongCreationException;  // Can't use BiConsumer because of the exceptions
    }

    private class Fifo<E> extends LinkedList<E>
    {

        private int limit;

        public Fifo(int limit)
        {
            this.limit = limit;
        }

        public E myPush(E o)
        {
            add(o);
            while (size() > limit)
            {
                remove();
            }
            return o;
        }
    }
}
