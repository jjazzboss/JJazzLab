package org.jjazz.proswing.walkingbass;

import org.jjazz.proswing.BassStyle;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Exceptions;

/**
 * The walking bass source phrase database.
 * <p>
 * Contains WbpSource phrases of 1, 2 3, or 4 bars. Note that the 3 * 2-bar subphrases that compose each 4-bar phrase are also found in the database. Same for
 * the 4 or 2 * 1-bar subphrases of a 4 or 2-bar phrase.
 * <p>
 * Example: If we have Cm7-F7-E7-Em7 in the database, then we also have Cm7-F7, F7-E7, E7-Em7, Cm7, F7, E7, Em7 phrases in the database.
 * <p>
 */
public class WbpSourceDatabase
{

    /**
     * Min and max size of a WbpSource phrase.
     */
    public final static int SIZE_MIN = 1, SIZE_MAX = 4;
    /**
     * Beat window for the first note of a WbpSource.
     */
    public static float FIRST_NOTE_BEAT_WINDOW = 0.15f;
    private static WbpSourceDatabase INSTANCE;
    @StaticResource(relative = true)
    private static final String MIDI_FILE_WALKING_RESOURCE_PATH = "WalkingBassMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_A_RESOURCE_PATH = "WalkingBass2feelAMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_B_RESOURCE_PATH = "WalkingBass2feelBMidiDB.mid";

    private final Map<String, String> mapSessionIdResource;
    private final ListMultimap<String, WbpSource> mmapSessionIdWbpSources;
    private final ListMultimap<Integer, WbpSource> mmapSizeWbpSources;
    private final ListMultimap<BassStyle, WbpSource>[] mmapBassStyleWbpSources;
    private final Map<String, WbpSource> mapIdWbpSource;
    private static final Logger LOGGER = Logger.getLogger(WbpSourceDatabase.class.getSimpleName());

    public static WbpSourceDatabase getInstance()
    {
        synchronized (WbpSourceDatabase.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new WbpSourceDatabase();
            }
        }
        return INSTANCE;
    }

    private WbpSourceDatabase()
    {


        mmapSizeWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
        mmapSessionIdWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
        mmapBassStyleWbpSources = new ListMultimap[SIZE_MAX - SIZE_MIN + 1];
        for (int size = SIZE_MIN; size <= SIZE_MAX; size++)
        {
            mmapBassStyleWbpSources[size - SIZE_MIN] = MultimapBuilder.enumKeys(BassStyle.class).arrayListValues().build();
        }
        mapIdWbpSource = new HashMap<>();
        mapSessionIdResource = new HashMap<>();

        // Extract the WbpSources from the WbpSessions
        var wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_WALKING_RESOURCE_PATH, "", "walking", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_A_RESOURCE_PATH, "2FA", "2feel-a", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_B_RESOURCE_PATH, "2FB", "2feel-b", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        LOGGER.log(Level.SEVERE, "WbpDatabase() 1-bar:{0}  2-bar:{1}  3-bar:{2}  4-bar:{3}", new Object[]
        {
            mmapSizeWbpSources.get(1).size(), mmapSizeWbpSources.get(2).size(), mmapSizeWbpSources.get(3).size(), mmapSizeWbpSources.get(4).size()
        });
    }

    /**
     * The number of WbpSources in the database.
     *
     * @param nbBars WbpSource size in bars. [1;4] or -1. If -1 return the number of all WbpSources whatever the size
     * @return
     */
    public int getNbWbpSources(int nbBars)
    {
        Preconditions.checkArgument(nbBars == -1 || check(nbBars), "nbBars=%s", nbBars);
        return nbBars == -1 ? mapIdWbpSource.size() : mmapSizeWbpSources.get(nbBars).size();
    }

    /**
     * Add a WbpSource to the database, unless an identical WbpSource is already there.
     *
     * @param wbps
     * @return True if WbpSource was added.
     */
    public boolean addWbpSource(WbpSource wbps)
    {
        boolean b = false;
        if (mapIdWbpSource.get(wbps.getId()) == null && getWbpSources(wbps.getBassStyle(), wbps.getSimpleChordSequence(), wbps.getSizedPhrase()).isEmpty())
        {
            addWbpSourceImpl(wbps);
            b = true;
        }
        return b;
    }

    /**
     * Remove a WbpSource to the database.
     *
     * @param wbps
     * @return True if WbpSource was removed.
     */
    public boolean removeWbpSource(WbpSource wbps)
    {
        boolean b = false;
        if (mapIdWbpSource.get(wbps.getId()) != null)
        {
            removeWbpSourceImpl(wbps);
            b = true;
        }
        return b;
    }


    /**
     * Perform various checks on the database.
     * <p>
     * @param bStyle
     */
    public void checkConsistency(BassStyle bStyle)
    {
        LOGGER.log(Level.INFO, "checkConsistency() -- bStyle={0}", bStyle);


        // All the base ChordTypes should have at least a 1-bar WbpSource
        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();
        Set<ChordType> allChordTypes = new HashSet<>();
        Stream.of(ctdb.getChordTypes()).forEach(ct -> allChordTypes.add(getSimplified(ct)));

        var clif = CLI_Factory.getDefault();
        final var C_NOTE = new Note();
        final var POS0 = new Position();
        final var POS2 = new Position(0, 2);
        final int NB_BARS = 1;
        final var scs = new SimpleChordSequence(new IntRange(0, NB_BARS - 1), TimeSignature.FOUR_FOUR);
        final WbpsaScorer scorer = new WbpsaScorerDefault(null, -1, null, bStyle);


        // Check for 1-chord-per-bar with 0 or only 1 one-bar WbpSource
        for (var ct : allChordTypes)
        {
            scs.clear();
            var ecs = new ExtChordSymbol(C_NOTE, C_NOTE, ct);
            scs.add(clif.createChordSymbol(ecs, POS0));
            var wbpsas = scorer.getWbpSourceAdaptations(scs, null);
            if (wbpsas.size() <= 1)
            {
                LOGGER.log(Level.SEVERE, "checkConsistency() {0} x {1}-bar WbpSource for {2}", new Object[]
                {
                    wbpsas.size(), NB_BARS, ecs
                });
            }
        }


        // Check for 2-chord-per-bar with 0 or only 1 one-bar WbpSource
        List<CLI_ChordSymbol> baseChords;
        try
        {
            baseChords = List.of(clif.createChordSymbol("C", POS0),
                    clif.createChordSymbol("C+", POS0),
                    clif.createChordSymbol("Cm", POS0),
                    clif.createChordSymbol("Csus", POS0));
        } catch (ParseException ex)
        {
            Exceptions.printStackTrace(ex);
            return;
        }

        var zeroMatchList = new ArrayList<SimpleChordSequence>();
        var oneMatchList = new ArrayList<SimpleChordSequence>();

        // Check all baseChords combinations 
        for (int i = 0; i < baseChords.size(); i++)
        {
            var cliCs0 = baseChords.get(i);

            for (int j = 0; j < baseChords.size(); j++)
            {
                var ct2 = baseChords.get(j).getData().getChordType();

                for (int pitch2 = 0; pitch2 < 12; pitch2++)
                {
                    if (i == j && pitch2 == 0)
                    {
                        continue;
                    }

                    var rootNote2 = new Note(pitch2);
                    var ecs2 = new ExtChordSymbol(rootNote2, rootNote2, ct2);
                    var cliCs2 = clif.createChordSymbol(ecs2, 0, 2);
                    scs.clear();
                    scs.add(cliCs0);
                    scs.add(cliCs2);
                    var wbpsas = scorer.getWbpSourceAdaptations(scs, null);
                    if (wbpsas.isEmpty())
                    {
                        zeroMatchList.add(scs.clone());
                    } else if (wbpsas.size() == 1)
                    {
                        oneMatchList.add(scs.clone());
                    }
                }
            }
        }

        for (var cSeq : zeroMatchList)
        {
            LOGGER.log(Level.SEVERE, "checkConsistency() 0 x {0}-bar WbpSource for {1}", new Object[]
            {
                NB_BARS, cSeq.toString()
            });
        }
        for (var cSeq : oneMatchList)
        {
            LOGGER.log(Level.SEVERE, "checkConsistency() 1 x {0}-bar WbpSource for {1}", new Object[]
            {
                NB_BARS, cSeq.toString()
            });
        }
    }

    /**
     * Get all the WbpSources from the database (in no particular order).
     *
     * @return An unmodifiable list
     */
    public List<WbpSource> getWbpSources()
    {
        return new ArrayList<>(mapIdWbpSource.values());
    }

    /**
     * Get the WbpSources which are nbBars long.
     * <p>
     *
     * @param nbBars
     * @return An unmodifiable list
     */
    public List<WbpSource> getWbpSources(int nbBars)
    {
        return Collections.unmodifiableList(mmapSizeWbpSources.get(nbBars));
    }

    /**
     * Get the WbpSources matching size and bass style.
     *
     * @param nbBars If -1 retrieve all WbpSources whatever the size
     * @param style
     * @return Unmodifiable list
     */
    public List<WbpSource> getWbpSources(int nbBars, BassStyle style)
    {
        Objects.requireNonNull(style);
        Preconditions.checkArgument(nbBars == -1 || check(nbBars), "nbBars=%s style=%s", nbBars, style);
        List<WbpSource> res;
        if (nbBars == -1)
        {
            res = new ArrayList<>();
            for (int size = SIZE_MAX; size >= SIZE_MIN; size--)
            {
                res.addAll(getBassStyleMultimap(size).get(style));
            }
        } else
        {
            res = getBassStyleMultimap(nbBars).get(style);
        }
        return Collections.unmodifiableList(res);
    }


    /**
     * The original resource file from which a session was read.
     *
     * @param sessionId
     * @return
     */
    public String getSessionResource(String sessionId)
    {
        return mapSessionIdResource.get(sessionId);
    }

    /**
     * Get the WbpSources from a specific session.
     *
     * @param sessionId
     * @return
     */
    public List<WbpSource> getSessionWbpSources(String sessionId)
    {
        return mmapSessionIdWbpSources.get(sessionId);
    }

    /**
     * Get all the WbpSources which have one or more bars in common with wbpSource.
     * <p>
     * For example a 2-bar WbpSource can have maximum 11 related WbpSources : 5 x 4-bar + 4 x 3-bar + 2 x 1-bar. Note that it might be less than 11 if for some
     * reason some related WbpSources were discarded by the database.<p>
     *
     * @param wbpSource
     * @return The returned list does not contain wbpSource. All elements belong to the same WbpSession.
     */
    public List<WbpSource> getRelatedWbpSources(WbpSource wbpSource)
    {
        var sessionSources = getSessionWbpSources(wbpSource.getSessionId());
        IntRange br = wbpSource.getBarRangeInSession();
        var res = sessionSources.stream()
                .filter(wbps -> wbps != wbpSource
                && br.isIntersecting(wbps.getBarRangeInSession()))
                .toList();
        return res;
    }

    public void dump()
    {
        LOGGER.info("WbpDatabase dump =========================================");
        for (var wbps : getWbpSources())
        {
            LOGGER.log(Level.INFO, " {0}", wbps.toLongString());
        }
        LOGGER.info("==========================================================");
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
    /**
     * Extract the WbpSources from a WbpSession and add them to the database.
     *
     * @param wbpSession
     */
    private void processWbpSession(WbpSession wbpSession)
    {
        var wbpSources = wbpSession.extractWbpSources(true, true);
        for (var wbpSource : wbpSources)
        {
            wbpSource.simplifyChordSymbols();
            if (getWbpSources(wbpSource.getBassStyle(), wbpSource.getSimpleChordSequence(), wbpSource.getSizedPhrase()).isEmpty())
            {
                // Add only non redundant phrases                
                addWbpSourceImpl(wbpSource);
            }
        }
    }

    /**
     * Retrieve WbpSession objects from a Midi resource file for the specified TimeSignature.
     * <p>
     * Midi file requirements:
     * <p>
     * - 1 marker "_SectionName" at the beginning of each section<br>
     * - 1 marker "_END" at the end of the last section<br>
     * - 0 or more markers #tag at the beginning of each section. <br>
     * - Markers "C7", "Ebm7", etc. <br>
     * - Midi notes must use only 1 channel<br>
     * <p>
     * The target note of a session is by default the root of the 1st chord symbol of the section (the closest root of the last session note), unless it is
     * specified via a "tn" session tag with this form: #tn=pitch, e.g. "#tn=36" for Midi note C1.
     *
     * @param midiFileResourcePath
     * @param sessionIdPrefix      A prefix added to each sessionId
     * @param sessionTag           A tag added to the tags read from the resource file
     * @param ts
     * @return
     */
    private List<WbpSession> loadWbpSessionsFromMidiFile(String midiFileResourcePath, String sessionIdPrefix, String sessionTag, TimeSignature ts)
    {
        LOGGER.log(Level.INFO, "loadWbpSessionsFromMidiFile() -- prefix={0} sessionTag={1} file={2}", new Object[]
        {
            sessionIdPrefix, sessionTag, midiFileResourcePath
        });

        List<WbpSession> res = new ArrayList<>();

        Sequence sequence = loadSequenceFromResource(midiFileResourcePath);
        int seqResolution = sequence.getResolution();
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new IllegalStateException("sequence.getDivisionType()=" + sequence.getDivisionType());
        }

        // Get all markers at the appropriate resolution
        List<MidiEvent> markerEvents = new ArrayList<>();
        for (Track track : sequence.getTracks())
        {
            var events = MidiUtilities.getMidiEvents(track,
                    MetaMessage.class,
                    me -> me.getType() == MidiConst.META_MARKER,
                    null);
            events = MidiUtilities.getMidiEventsAtPPQ(events, seqResolution, MidiConst.PPQ_RESOLUTION);
            markerEvents.addAll(events);
        }
        assert !markerEvents.isEmpty() : "midiFileResourcePath=" + midiFileResourcePath;

        // Sort markers: first session name "_xxx", then tags "#xxx", then chord symbols
        markerEvents.sort((left, right) -> 
        {
            int c = Long.compare(left.getTick(), right.getTick());
            if (c == 0)
            {
                String strLeft = MidiUtilities.getText(left);
                String strRight = MidiUtilities.getText(right);
                if (strLeft.charAt(0) == '_')
                {
                    c = -1;
                    assert strRight.charAt(0) != '_' : "tick=" + left.getTick();
                } else if (strRight.charAt(0) == '_')
                {
                    c = 1;
                    assert strLeft.charAt(0) != '_' : "tick=" + left.getTick();
                } else if (strLeft.charAt(0) == '#')
                {
                    c = -1;
                } else if (strRight.charAt(0) == '#')
                {
                    c = 1;
                } else
                {
                    // Nothing
                }
            }
            return c;
        });

        // Verify the presence of the end session marker
        var lastMarker = markerEvents.get(markerEvents.size() - 1);
        assert "_END".equalsIgnoreCase(MidiUtilities.getText(lastMarker)) : "MidiUtilities.getText(lastMarker)=" + MidiUtilities.getText(lastMarker);

        // Read the big phrase
        var phrases = Phrases.getPhrases(seqResolution, sequence.getTracks());
        assert phrases.size() == 1 : "nb phrases=" + phrases.size();
        var bigPhrase = phrases.get(0);

        // Loop on each session markers
        var sessionMarkers = markerEvents.stream().filter(me -> MidiUtilities.getText(me).startsWith("_")).toList();
        for (int i = 0; i < sessionMarkers.size() - 1; i++)
        {

            // Session data
            var sessionMarker = sessionMarkers.get(i);
            String sessionId = sessionIdPrefix + MidiUtilities.getText(sessionMarker);
            long startPosInTicks = sessionMarker.getTick();
            long endPosInTicks = sessionMarkers.get(i + 1).getTick() - 1;
            LongRange sessionRange = new LongRange(startPosInTicks, endPosInTicks);

            // Extract all tag strings, convert to lowercase
            var sessionTags = new ArrayList<>(List.of(sessionTag.toLowerCase()));
            markerEvents.stream()
                    .filter(me -> me.getTick() == startPosInTicks && MidiUtilities.getText(me).startsWith("#"))
                    .map(me -> MidiUtilities.getText(me).substring(1).trim().toLowerCase())
                    .forEach(tag -> sessionTags.add(tag));

            // Is there a target note defined ?
            Note targetNote = null;
            String strPitch = sessionTags.stream().filter(str -> str.startsWith("tn=")).findFirst().orElse(null);
            if (strPitch != null)
            {
                targetNote = new Note(Integer.parseInt(strPitch.substring(3)));
            }

            // The chord symbols
            var sessionChords = markerEvents.stream()
                    .filter(me -> 
                    {
                        boolean b = false;
                        if (sessionRange.contains(me.getTick()))
                        {
                            String s = MidiUtilities.getText(me);
                            if (s.isBlank())
                            {
                                LOGGER.log(Level.SEVERE, "Empty marker found at tick={0}", me.getTick());
                            } else
                            {
                                char c = MidiUtilities.getText(me).charAt(0);
                                b = c >= 'A' && c <= 'G';
                            }
                        }
                        return b;
                    })
                    .toList();

            // Get the size of the session
            FloatRange beatRange = new FloatRange(startPosInTicks / (float) MidiConst.PPQ_RESOLUTION, (endPosInTicks + 1) / (float) MidiConst.PPQ_RESOLUTION);
            int sizeInBars = (int) Math.round(beatRange.size() / ts.getNbNaturalBeats());
            int barFrom = (int) (beatRange.from / ts.getNbNaturalBeats());
            IntRange barRange = new IntRange(barFrom, barFrom + sizeInBars - 1);

            // Get the notes
            Phrase p = Phrases.getSlice(bigPhrase, beatRange, false, 2, 0);
            if (p.isEmpty())
            {
                LOGGER.log(Level.WARNING, "Empty session found for barRange={0}", barRange);
                continue;
            }
            p.shiftAllEvents(-beatRange.from);
            beatRange = beatRange.getTransformed(-beatRange.from);
            SizedPhrase sp = new SizedPhrase(0, beatRange, ts, false);
            sp.add(p);

            // Get the SimpleChordSequence
            SimpleChordSequence cSeq = getChordSequence(sizeInBars, ts, sessionChords, startPosInTicks);

            // Compute target note if not defined via a #tn tag
            if (targetNote == null)
            {
                Note lastNote = sp.last();
                ExtChordSymbol firstChord = cSeq.first().getData();
                targetNote = new Note(lastNote.getClosestPitch(firstChord.getRootNote().getRelativePitch()));
            }

            // Build the WbpSession
            WbpSession session = new WbpSession(sessionId, sessionTags, cSeq, sp, targetNote);
            mapSessionIdResource.put(sessionId, midiFileResourcePath);
            res.add(session);
        }

        return res;

    }

    private Sequence loadSequenceFromResource(String midiFileResourcePath)
    {
        Sequence sequence = null;
        try (InputStream is = getClass().getResourceAsStream(midiFileResourcePath))
        {
            // Load file into a sequence
            sequence = MidiSystem.getSequence(is);       // Throws IOException, InvalidMidiDataException
            if (sequence.getDivisionType() != Sequence.PPQ)
            {
                throw new IOException("Midi stream does not use PPQ division");
            }
        } catch (IOException | InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        return sequence;
    }

    private SimpleChordSequence getChordSequence(int nbBars, TimeSignature ts, List<MidiEvent> sessionChords, long tickOffset)
    {
        SimpleChordSequence cSeq = new SimpleChordSequence(new IntRange(0, nbBars - 1), ts);

        for (var me : sessionChords)
        {
            String txt = MidiUtilities.getText(me);
            ExtChordSymbol ecs = null;
            try
            {
                ecs = ExtChordSymbol.get(txt);
            } catch (ParseException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            float beatPos = (me.getTick() - tickOffset) / (float) MidiConst.PPQ_RESOLUTION;
            int bar = (int) Math.floor(beatPos / ts.getNbNaturalBeats());
            float beat = Math.round(beatPos - bar * ts.getNbNaturalBeats());
            Position pos = new Position(bar, beat);

            var cli = CLI_Factory.getDefault().createChordSymbol(ecs, pos);
            cSeq.add(cli);
        }

        return cSeq;
    }

    private ChordType getSimplified(ChordType ct)
    {
        return ct.getSimplified(4);
    }

    /**
     * Find the WbpSources in the database which are compatible with the specified parameters.
     * <p>
     * <p>
     * Compatible means chord sequences share the same root profile and phrases have the same note intervals and approximately the same note positions.
     *
     * @param style
     * @param scs
     * @param sp
     * @return Can be empty
     * @see SizedPhrase#equalsAsIntervals(org.jjazz.phrase.api.Phrase, float)
     */
    private List<WbpSource> getWbpSources(BassStyle style, SimpleChordSequence scs, SizedPhrase sp)
    {
        List<WbpSource> res = new ArrayList<>();
        WbpsaScorer scorer = new WbpsaScorerDefault(null, -1, null, style);

        int nbBars = scs.getBarRange().size();
        var wbpSources = getWbpSources(nbBars, style).stream()
                .filter(s -> scs.getRootProfile().equals(s.getRootProfile()))
                .toList();

        for (WbpSource rpWbpSource : wbpSources)
        {
            var wbpsa = new WbpSourceAdaptation(rpWbpSource, scs);
            var score = scorer.computeCompatibilityScore(wbpsa, null);
            var spRp = rpWbpSource.getSizedPhrase();
            if (score.compareTo(Score.ZERO) > 0 && sp.equalsAsIntervals(spRp, 0.15f))
            {
                res.add(rpWbpSource);
            }
        }
        return res;
    }

    private void addWbpSourceImpl(WbpSource wbpSource)
    {
        var old = mapIdWbpSource.put(wbpSource.getId(), wbpSource);
        if (old != null)
        {
            throw new IllegalStateException("Duplicate WbpSource.id found: wbpSource=" + wbpSource + " old=" + old);
        }
        int nbBars = wbpSource.getBarRange().size();
        if (!mmapSizeWbpSources.put(nbBars, wbpSource))
        {
            throw new IllegalStateException("Adding to mmapSizeWbpSources failed for " + wbpSource + " mmapSizeWbpSources=" + mmapSizeWbpSources);
        }
        if (!mmapSessionIdWbpSources.put(wbpSource.getSessionId(), wbpSource))
        {
            throw new IllegalStateException("Adding to mmapSessionIdWbpSources failed for " + wbpSource + " mmapSessionIdWbpSources=" + mmapSessionIdWbpSources);
        }
        if (!getBassStyleMultimap(nbBars).put(wbpSource.getBassStyle(), wbpSource))
        {
            throw new IllegalStateException(
                    "Adding to mmapBassStyleWbpSources failed for " + wbpSource + " getBassStyleMultimap(nbBars)=" + getBassStyleMultimap(nbBars));
        }
    }

    private void removeWbpSourceImpl(WbpSource wbpSource)
    {
        var old = mapIdWbpSource.remove(wbpSource.getId());
        if (old == null)
        {
            throw new IllegalStateException("Removing non-existing WbpSourceId=" + wbpSource.getId());
        }
        int nbBars = wbpSource.getBarRange().size();
        if (!mmapSizeWbpSources.remove(nbBars, wbpSource))
        {
            throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource + " mmapSizeWbpSources=" + mmapSizeWbpSources);
        }
        if (!mmapSessionIdWbpSources.remove(wbpSource.getSessionId(), wbpSource))
        {
            throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource + " mmapSessionIdWbpSources=" + mmapSessionIdWbpSources);
        }
        if (!getBassStyleMultimap(nbBars).remove(wbpSource.getBassStyle(), wbpSource))
        {
            throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource + " getBassStyleMultimap(nbBars)=" + getBassStyleMultimap(nbBars));
        }
    }

    private ListMultimap<BassStyle, WbpSource> getBassStyleMultimap(int nbBars)
    {
        return mmapBassStyleWbpSources[nbBars - SIZE_MIN];
    }

    private boolean check(int nbBars)
    {
        return nbBars >= SIZE_MIN && nbBars <= SIZE_MAX;
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    
    /**
     * A session is one consistent recording of a possibly long WalkingBassPhrase, which will be sliced in smaller WbpSource units.
     * <p>
     * It is recommended to use the shortest chord symbol corresponding to the phrase notes, typically 3-degrees or 4-degrees chord symbols (C, Cm, C+, Cdim,
     * Cm6, C6, C7M, C7, Csus, C7sus, ...), though more complex chord symbols are allowed. If a chord symbol is too complex for the actual phrase notes (eg C7b9
     * but phrase only uses C E G), chord symbol will be simplified in the WbpSource (C7b9 &gt; C).
     * <p>
     */
    static private class WbpSession extends Wbp
    {

        private final String id;
        private final List<String> tags;
        private final BassStyle bassStyle;
        private final Set<NoteEvent> loggedCrossingNotes;
        private static final Logger LOGGER = Logger.getLogger(WbpSession.class.getSimpleName());

        public WbpSession(String id, List<String> tags, SimpleChordSequence cSeq, SizedPhrase phrase, Note targetNote)
        {
            super(cSeq, phrase, 0, targetNote);
            this.id = id;
            this.tags = tags;
            this.bassStyle = computeBassStyle(tags);
            this.loggedCrossingNotes = new HashSet<>();
        }

        public String getId()
        {
            return id;
        }

        /**
         * The tags associated to this session.
         *
         * @return
         */
        public List<String> getTags()
        {
            return tags;
        }

        /**
         * Extract all the possible WbpSources from this session.
         * <p>
         * We extract all the possible 1/2/3/4-bar WbpSources. So for one 4-bar session phrase, the method can generate 10 WbpSource objects: 1 * 4-bar + 2 *
         * 3-bar + 3 * 2-bar + 4 * 1-bar.
         * <p>
         * Returned WbpSources get the tags of the session.
         *
         * @param disallowNonRootStartNote     If true a WbpSource is not extracted if its first note is different from the chord root note.
         * @param disallowNonChordToneLastNote If true a WbpSource is not extracted if its last note is note a chord note (ie no transition note).
         * @return
         */
        public List<WbpSource> extractWbpSources(boolean disallowNonRootStartNote, boolean disallowNonChordToneLastNote)
        {
            List<WbpSource> res = new ArrayList<>();

            SizedPhrase sessionPhrase = getSizedPhrase();
            int sessionSizeInBars = sessionPhrase.getSizeInBars();

            for (int srcSize = 1; srcSize <= 4; srcSize++)
            {
                for (int bar = 0; bar < sessionSizeInBars - srcSize + 1; bar++)
                {
                    WbpSource wbpSource = extractWbpSource(bar, srcSize);
                    if (wbpSource != null)
                    {
                        boolean bFirst = !disallowNonRootStartNote || wbpSource.startsOnChordRoot();
                        boolean bLast = !disallowNonChordToneLastNote || wbpSource.endsOnChordTone();
                        if (bFirst && bLast)
                        {
                            res.add(wbpSource);
                        }
                    }
                }
            }

            return res;
        }

        @Override
        public String toString()
        {
            return "WbpSession id=" + id + " tags=" + tags + " | " + super.toString();
        }

        // ==============================================================================================
        // Private methods
        // ==============================================================================================
        /**
         * Extract a WbpSource from a SizedPhrase.
         *
         * @param barOffset
         * @param nbBars
         * @return Can be null if no valid WbpSource could be extracted
         */
        private WbpSource extractWbpSource(int barOffset, int nbBars)
        {
            SizedPhrase sessionPhrase = getSizedPhrase();
            TimeSignature ts = sessionPhrase.getTimeSignature();
            IntRange barRange = new IntRange(barOffset, barOffset + nbBars - 1);

            LOGGER.log(Level.FINE, "extractWbpSource() barRange={0} WbpSession={1}", new Object[]
            {
                barRange, this
            });


            // Compute phrase beat range
            FloatRange beatRange = new FloatRange(barRange.from * ts.getNbNaturalBeats(), (barRange.to + 1) * ts.getNbNaturalBeats());


            // Do not extract if there is a crossing note at start (but ignore crossing notes due to non quantized notes)
            var crossingNotes = Phrases.getCrossingNotes(sessionPhrase, beatRange.from, true);
            if (crossingNotes.stream()
                    .anyMatch(ne
                            -> ne.getPositionInBeats() < beatRange.from - FIRST_NOTE_BEAT_WINDOW && (ne.getPositionInBeats() + ne.getDurationInBeats()) > (beatRange.from + FIRST_NOTE_BEAT_WINDOW)))
            {
                if (!loggedCrossingNotes.contains(crossingNotes.get(0)))
                {
                    loggedCrossingNotes.addAll(crossingNotes);
                    LOGGER.log(Level.WARNING, "extractWbpSource() start crossing note(s) detected={0} at bar {1}, WbpSession={2}",
                            new Object[]
                            {
                                crossingNotes, barRange.from, getId()
                            }
                    );
                    loggedCrossingNotes.addAll(crossingNotes);
                    return null;
                }
            }


            // Do not extract if there is a crossing note at end (but ignore crossing notes due to non quantized start notes from next bar)
            crossingNotes = Phrases.getCrossingNotes(sessionPhrase, beatRange.to, true);
            if (crossingNotes.stream()
                    .anyMatch(ne
                            -> ne.getPositionInBeats() < beatRange.to - FIRST_NOTE_BEAT_WINDOW
                    && (ne.getPositionInBeats() + ne.getDurationInBeats()) > (beatRange.to + FIRST_NOTE_BEAT_WINDOW)))
            {
                if (!loggedCrossingNotes.contains(crossingNotes.get(0)))
                {
                    loggedCrossingNotes.addAll(crossingNotes);
                    LOGGER.log(Level.WARNING, "extractWbpSource() end crossing note(s) detected={0} at bar {1}, WbpSession={2}",
                            new Object[]
                            {
                                crossingNotes, barRange.to, getId()
                            }
                    );
                }
                return null;
            }


            // For slice to work, remove FIRST_NOTE_BEAT_WINDOW from range.to in order to avoid possible (though rare) same-pitch notes overlaps when the generator
            // will assemble WbpSources
            FloatRange sliceBeatRange = beatRange.getTransformed(0, -FIRST_NOTE_BEAT_WINDOW);
            Phrase p = Phrases.getSlice(sessionPhrase, sliceBeatRange, true, 1, FIRST_NOTE_BEAT_WINDOW);

            p.shiftAllEvents(-beatRange.from);
            SizedPhrase sp = new SizedPhrase(sessionPhrase.getChannel(), beatRange.getTransformed(-beatRange.from), sessionPhrase.getTimeSignature(), false);

            sp.addAll(p);

            // Get possible firstNoteBeatShift
            float firstNoteBeatShift = 0;       // By default
            if (beatRange.from - FIRST_NOTE_BEAT_WINDOW >= 0)
            {
                p = Phrases.getSlice(sessionPhrase, new FloatRange(beatRange.from - FIRST_NOTE_BEAT_WINDOW, beatRange.from), false, 1, 0);
                if (!p.isEmpty())
                {
                    var neLast = p.last();
                    if (neLast.getBeatRange().to >= beatRange.from)
                    {
                        firstNoteBeatShift = -(beatRange.from - neLast.getPositionInBeats());
                    }
                }
            }

            // Get the progression starting at bar 0
            var br = new IntRange(barOffset, barOffset + nbBars - 1);
            var cSeq = getSimpleChordSequence().subSequence(br, true).getShifted(-barOffset);

            // Target note
            Note targetNote = getTargetNote();      // By default
            int nextBar = barOffset + nbBars;
            if (nextBar < sessionPhrase.getSizeInBars())
            {
                // If one bar after our phrase, find the next note
                FloatRange fr = new FloatRange(nextBar * ts.getNbNaturalBeats(), (nextBar + 1) * ts.getNbNaturalBeats());
                var nextBarNotes = sessionPhrase.getNotes(n -> true, fr, true);
                assert !nextBarNotes.isEmpty();
                targetNote = nextBarNotes.get(0);
            }

            var wbpSource = new WbpSource(getId(), barOffset, bassStyle, cSeq, sp, firstNoteBeatShift, targetNote);

            tags.forEach(t -> wbpSource.addTag(t));

            return wbpSource;
        }


        private BassStyle computeBassStyle(List<String> tags)
        {
            BassStyle res = BassStyle.WALKING;

            boolean twoFeel = tags.stream().anyMatch(t -> t.startsWith("2feel"));
            boolean walking = tags.stream().anyMatch(t -> t.equalsIgnoreCase("walking"));
            if ((twoFeel && walking) || (!twoFeel && !walking))
            {
                LOGGER.log(Level.SEVERE, "computeBassStyle() Inconsistent tags found in WbpSession={0}", this);
            } else if (twoFeel)
            {
                res = BassStyle.TWO_FEEL;
            }
            return res;
        }

    }

}
