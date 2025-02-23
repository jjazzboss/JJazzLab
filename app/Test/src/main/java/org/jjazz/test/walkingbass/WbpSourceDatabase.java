package org.jjazz.test.walkingbass;

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
import java.util.Set;
import java.util.function.Predicate;
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
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.test.walkingbass.generator.DefaultWbpsaScorer;
import org.jjazz.test.walkingbass.generator.WalkingBassGenerator.BassStyle;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;
import org.jjazz.test.walkingbass.generator.WbpsaScorer;
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

    private static WbpSourceDatabase INSTANCE;
    @StaticResource(relative = true)
    private static final String MIDI_FILE_WALKING_RESOURCE_PATH = "WalkingBassMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_A_RESOURCE_PATH = "WalkingBass2feelAMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_B_RESOURCE_PATH = "WalkingBass2feelBMidiDB.mid";

    private final Map<String, WbpSession> mapIdSessions;
    private final ListMultimap<Integer, WbpSource> mmapSizeWbpSources;
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
        mapIdWbpSource = new HashMap<>();
        mapIdSessions = new HashMap<>();


        // Extract the WbpSources from the WbpSessions
        var wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_WALKING_RESOURCE_PATH, "", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> mapIdSessions.put(s.getId(), s));
        wbpSessions.forEach(s -> processWbpSession(s, null));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_A_RESOURCE_PATH, "2FA", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> mapIdSessions.put(s.getId(), s));
        wbpSessions.forEach(s -> processWbpSession(s, "2feelA"));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_B_RESOURCE_PATH, "2FB", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> mapIdSessions.put(s.getId(), s));
        wbpSessions.forEach(s -> processWbpSession(s, "2feelB"));


        LOGGER.log(Level.SEVERE, "WbpDatabase() 1-bar:{0}  2-bar:{1}  3-bar:{2}  4-bar:{3}", new Object[]
        {
            mmapSizeWbpSources.get(1).size(), mmapSizeWbpSources.get(2).size(), mmapSizeWbpSources.get(3).size(), mmapSizeWbpSources.get(4).size()
        });
    }


    public WbpSession getWbpSession(String id)
    {
        return mapIdSessions.get(id);
    }

    public List<WbpSession> getWbpSessions()
    {
        return new ArrayList<>(mapIdSessions.values());
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
        if (mapIdWbpSource.get(wbps.getId()) == null
                && !mmapSizeWbpSources.get(wbps.getBarRange().size()).contains(wbps)
                && getWbpSources(wbps.getSimpleChordSequence(), wbps.getSizedPhrase()).isEmpty())
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
        if (mapIdWbpSource.get(wbps.getId()) != null && mmapSizeWbpSources.get(wbps.getBarRange().size()).contains(wbps))
        {
            removeWbpSourceImpl(wbps);
            b = true;
        }
        return b;
    }

    /**
     * Get all the WbpSources from the database (in no particular order).
     *
     * @return
     */
    public List<WbpSource> getWbpSources()
    {
        return new ArrayList<>(mmapSizeWbpSources.values());
    }

    /**
     * Perform various checks on the database.
     * <p>
     */
    public void checkConsistency()
    {
        LOGGER.severe("checkConsistency() starting");

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

        // Check for chord types with 0 or only 1 one-bar WbpSource
        WbpsaScorer scorer = new DefaultWbpsaScorer(null, BassStyle.ALL, -1);
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

        // Check for 2-chord bars with 0 or only 1 one-bar WbpSource
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
     * Get the WbpSources which are nbBars long.
     * <p>
     *
     * @param nbBars
     * @return An unmodifiable list
     */
    public List<WbpSource> getWbpSources(int nbBars)
    {
        Preconditions.checkArgument(nbBars >= SIZE_MIN && nbBars <= SIZE_MAX, "nbBars=%s", nbBars);
        return Collections.unmodifiableList(mmapSizeWbpSources.get(nbBars));
    }


    /**
     * Get WbpSources of specified size which match the predicate.
     * <p>
     *
     * @param nbBars
     * @param tester
     * @return
     */
    public List<WbpSource> getWbpSources(int nbBars, Predicate<WbpSource> tester)
    {
        Preconditions.checkArgument(nbBars >= SIZE_MIN && nbBars <= SIZE_MAX, "nbBars=%s", nbBars);
        return mmapSizeWbpSources.get(nbBars).stream()
                .filter(tester)
                .toList();
    }

    /**
     * Get WbpSources which match the predicate.
     * <p>
     *
     * @param tester
     * @return
     */
    public List<WbpSource> getWbpSources(Predicate<WbpSource> tester)
    {
        return mmapSizeWbpSources.values().stream()
                .filter(tester)
                .toList();
    }

    /**
     * Get the WbpSources which match the specified profile.
     * <p>
     * Results are cached.
     *
     * @param rootProfile
     * @return
     */
    public List<WbpSource> getWbpSources(String rootProfile)
    {
        Predicate<WbpSource> tester = wbps -> wbps.getRootProfile().equals(rootProfile);
        return getWbpSources(tester);
    }

    /**
     * Get all the WbpSources from same session than wbpSource and which share at least one bar with wbpSource.
     *
     * @param wbpSource
     * @return The returned list does not contain wbpSource
     */
    public List<WbpSource> getRelatedWbpSources(WbpSource wbpSource)
    {
        IntRange br = wbpSource.getBarRangeInSession();
        String sId = wbpSource.getSessionId();
        var res = getWbpSources(wbps -> wbps != wbpSource
                && wbps.getSessionId().equals(sId)
                && br.isIntersecting(wbps.getBarRangeInSession()));
        return res;
    }


    public void dump()
    {
        LOGGER.info("WbpDatabase dump =========================================");
        for (var session : getWbpSessions())
        {
            LOGGER.log(Level.INFO, " {0}", session.toString());
        }
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
     * @param extraTag   If not null, add this tag to WbpSources (in addition to the WbpSession tags).
     */
    private void processWbpSession(WbpSession wbpSession, String extraTag)
    {
        var wbps = wbpSession.extractWbpSources(true, true);
        for (var wbpSource : wbps)
        {
            wbpSource.simplifyChordSymbols();
            if (extraTag != null)
            {
                wbpSource.addTag(extraTag);
            }
            if (getWbpSources(wbpSource.getSimpleChordSequence(), wbpSource.getSizedPhrase()).isEmpty())
            {
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
     * @param ts
     * @return
     */
    private List<WbpSession> loadWbpSessionsFromMidiFile(String midiFileResourcePath, String sessionIdPrefix, TimeSignature ts)
    {
        LOGGER.log(Level.INFO, "loadWbpSessionsFromMidiFile() -- prefix={0} file={1}", new Object[]
        {
            sessionIdPrefix, midiFileResourcePath
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
        assert phrases.size() == 1 : "phrases=" + phrases;
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
            var sessionTags = markerEvents.stream()
                    .filter(me -> me.getTick() == startPosInTicks && MidiUtilities.getText(me).startsWith("#"))
                    .map(me -> MidiUtilities.getText(me).substring(1).trim().toLowerCase())
                    .toList();


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
     * Find the WbpSources in the database which are compatible with the specified SimpleChordSequence and SizedPhrase.
     * <p>
     * <p>
     * Compatible means chord sequences share the same root profile and phrases have the same note intervals and approximately the same note positions.
     *
     * @param scs
     * @param sp
     * @return Can be empty
     * @see SizedPhrase#equalsAsIntervals(org.jjazz.phrase.api.Phrase, float)
     */
    private List<WbpSource> getWbpSources(SimpleChordSequence scs, SizedPhrase sp)
    {
        List<WbpSource> res = new ArrayList<>();
        WbpsaScorer scorer = new DefaultWbpsaScorer(null, BassStyle.ALL, -1);

        for (var rpWbpSource : getWbpSources(scs.getRootProfile()))
        {
            var wbpsa = new WbpSourceAdaptation(rpWbpSource, scs);
            var score = scorer.computeCompatibilityScore(wbpsa, null);
            var spRp = rpWbpSource.getSizedPhrase();
            if (score.compareTo(WbpsaScorer.SCORE_ZERO) > 0 && sp.equalsAsIntervals(spRp, 0.15f))
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
            throw new IllegalStateException("Adding failed for " + wbpSource);
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
            throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource);
        }
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    

}
