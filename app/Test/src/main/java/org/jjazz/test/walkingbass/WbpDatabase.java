package org.jjazz.test.walkingbass;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
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
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Exceptions;

/**
 * The walking bass phrases database.
 * <p>
 * Contains WbpSource phrases of 1, 2 or 4 bars. Note that the 3 * 2-bar subphrases that compose each 4-bar phrase are also found in the database. Same for the
 * 4 or 2 * 1-bar subphrases of a 4 or 2-bar phrase.
 * <p>
 * Example: If we have Cm7-F7-E7-Em7 in the database, then we also have Cm7-F7, F7-E7, E7-Em7, Cm7, F7, E7, Em7 phrases in the database.
 */
public class WbpDatabase
{

    
    private static WbpDatabase INSTANCE;
    @StaticResource(relative = true)
    private static final String MIDI_FILE_RESOURCE_PATH = "WalkingBassMidiDB.mid";

    private final List<WbpSession> wbpSessions;
    private final List<WbpSource> wbpSources;
    private final Map<String, WbpSource> mapIdWbpSource;
    private final Multimap<ChordType, WbpSource> mmapSimplifiedChordTypeOneBarWbpSource;
    private final Map<KeyablePredicate<?>, List<WbpSource>> mapFilterSources = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(WbpDatabase.class.getSimpleName());

    public static WbpDatabase getInstance()
    {
        synchronized (WbpDatabase.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new WbpDatabase();
            }
        }
        return INSTANCE;
    }

    private WbpDatabase()
    {
        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_RESOURCE_PATH, TimeSignature.FOUR_FOUR);
        wbpSources = new ArrayList<>();
        mapIdWbpSource = new HashMap<>();
        mmapSimplifiedChordTypeOneBarWbpSource = MultimapBuilder.hashKeys().arrayListValues().build();


        for (var wbpSession : wbpSessions)
        {
            var wbps = wbpSession.extractWbpSources(true, true);
            for (var wbpSource : wbps)
            {
                var old = mapIdWbpSource.put(wbpSource.getId(), wbpSource);
                if (old != null)
                {
                    throw new IllegalStateException("Duplicate WbpSource.id found: wbpSource=" + wbpSource + " old=" + old);
                }
                wbpSources.add(wbpSource);
                if (wbpSource.getBarRange().size() == 1)
                {
                    var ct = simplifyChordType(wbpSource.getSimpleChordSequence().first().getData().getChordType());
                    mmapSimplifiedChordTypeOneBarWbpSource.put(ct, wbpSource);
                }
            }
        }
    }

    public List<WbpSession> getWbpSessions()
    {
        return wbpSessions;
    }


    public List<WbpSource> getWbpSources()
    {
        return wbpSources;
    }

    /**
     * Perform various checks on the database.
     * <p>
     */
    public void checkConsistency()
    {
        // All main ChordTypes should have at least a 1-bar WbpSource
        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();
        Set<ChordType> simplifiedCts = new HashSet<>();
        for (var ct : ctdb.getChordTypes())
        {
            simplifiedCts.add(simplifyChordType(ct));
        }


        for (var ct : simplifiedCts)
        {
            var wbps = getWbpSourcesOneBar(ct);
            if (wbps.isEmpty())
            {
                LOGGER.log(Level.SEVERE, "checkConsistency() No one-bar WbpSource found for ct={0}", ct);
            }
        }

        for (var ct : simplifiedCts)
        {
            var wbps = getWbpSourcesOneBar(ct);
            if (wbps.size() == 1)
            {
                LOGGER.log(Level.WARNING, "checkConsistency() Only 1 WbpSource found for ct={0}", ct);
            }
        }


    }

    /**
     * Get the WbpSources which are nbBars long.
     * <p>
     * Results are cached.
     *
     * @param nbBars
     * @return
     */
    public List<WbpSource> getWbpSources(int nbBars)
    {
        Predicate<WbpSource> tester = wbps -> wbps.getBarRange().size() == nbBars;
        var dbTester = new KeyablePredicate(String.valueOf(nbBars), tester);
        return WbpDatabase.this.getWbpSources(dbTester);
    }


    /**
     * Get the 1-bar WbpSources which uses the specified basic ChordType.
     * <p>
     *
     * @param basicChordType Can not have more than 4 degrees
     * @return
     */
    public List<WbpSource> getWbpSourcesOneBar(ChordType basicChordType)
    {
        Preconditions.checkArgument(basicChordType.getNbDegrees() <= 4, "basicChordType=%s", basicChordType);
        return (List) mmapSimplifiedChordTypeOneBarWbpSource.get(basicChordType);
    }

    /**
     * Get WbpSources which match the predicate.
     * <p>
     * If tester is an instance of KeyablePredicate, then results are cached.
     *
     * @param tester
     * @return
     */
    public List<WbpSource> getWbpSources(Predicate<WbpSource> tester)
    {
        if (tester instanceof KeyablePredicate)
        {
            var dbTester = (KeyablePredicate) tester;
            var res = mapFilterSources.get(dbTester);
            if (res != null)
            {
                return res;
            }
            res = wbpSources.stream()
                    .filter(tester)
                    .toList();
            mapFilterSources.put(dbTester, res);
            return res;
        } else
        {
            return wbpSources.stream()
                    .filter(tester)
                    .toList();
        }
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
        var dbTester = new KeyablePredicate(rootProfile, tester);
        return WbpDatabase.this.getWbpSources(dbTester);
    }

    /**
     * Get the WbpSources which match the specified chord sequence.
     * <p>
     *
     * @param scs
     * @param minScsSimilarityScore
     * @param minIndividualChordTypeSimilarityScore
     * @return
     * @see SimpleChordSequence#getChordTypeSimilarityScore(org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence, float, boolean)
     * @see ChordType#getSimilarityScore(org.jjazz.harmony.api.ChordType, boolean) 
     */
    public List<WbpSource> getWbpSources(SimpleChordSequence scs, float minScsSimilarityScore, int minIndividualChordTypeSimilarityScore)
    {
        List<WbpSource> res = new ArrayList<>();
        
        
        var rpSources = getWbpSources(scs.getRootProfile());
        for (var rpSource:rpSources)
        {
            
        }

        
        {
            WbpSource wbps = it.next();

            var wbpScs = wbps.getSimpleChordSequence();
            float score = scs.getChordTypeSimilarityScore(wbpScs, WbpSourceAdaptation.DEFAULT_MIN_INDIVIDUAL_CHORDTYPE_COMPATIBILITY_SCORE, true);
            if (score == 0)
            {
                it.remove();
            }
        }
    }

    public void dump()
    {
        LOGGER.info("WbpDatabase dump =========================================");
        for (var session : getWbpSessions())
        {
            LOGGER.log(Level.INFO, " {0}", session.toString());
        }
        for (var wbps : WbpDatabase.this.getWbpSources())
        {
            LOGGER.log(Level.INFO, " {0}", wbps.toLongString());
        }
        LOGGER.info("==========================================================");
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
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
     * @param ts
     * @return
     */
    private List<WbpSession> loadWbpSessionsFromMidiFile(String midiFileResourcePath, TimeSignature ts)
    {
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
            String sessionId = MidiUtilities.getText(sessionMarker);
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
                targetNote = new Note(Integer.valueOf(strPitch.substring(3)));
            }


            // The chord symbols
            var sessionChords = markerEvents.stream()
                    .filter(me -> 
                    {
                        boolean b = false;
                        if (sessionRange.contains(me.getTick()))
                        {
                            char c = MidiUtilities.getText(me).charAt(0);
                            b = c >= 'A' && c <= 'G';
                        }
                        return b;
                    })
                    .toList();


            // Get the size of the session
            FloatRange beatRange = new FloatRange(startPosInTicks / (float) MidiConst.PPQ_RESOLUTION, (endPosInTicks + 1) / (float) MidiConst.PPQ_RESOLUTION);
            int sizeInBars = (int) Math.round(beatRange.size() / ts.getNbNaturalBeats());


            // Get the notes
            Phrase p = Phrases.getSlice(bigPhrase, beatRange, false, 2, 0);
            assert !p.isEmpty() : "p=" + p + " beatRange=" + beatRange + " bigPhrase=" + bigPhrase;
            p.shiftAllEvents(-beatRange.from);
            beatRange = beatRange.getTransformed(-beatRange.from);
            // SizedPhrase sp = new SizedPhrase(0, beatRange, ts);
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

    private ChordType simplifyChordType(ChordType ct)
    {
        return ct.getSimplified(4);
    }


    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    
    /**
     * A special Predicate which can be used as key of a map, enabling results caching.
     *
     * @param <T>
     * @see #getWbpSources(java.util.function.Predicate)
     */
    public static class KeyablePredicate<T> implements Predicate<T>
    {

        private final Predicate<T> tester;
        private final String hashCode;

        public KeyablePredicate(String hashCode, Predicate<T> tester)
        {
            checkNotNull(hashCode);
            checkNotNull(tester);
            this.tester = tester;
            this.hashCode = hashCode;
        }

        @Override
        public boolean test(T t)
        {
            return tester.test(t);
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.hashCode);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final KeyablePredicate<?> other = (KeyablePredicate<?>) obj;
            return Objects.equals(this.hashCode, other.hashCode);
        }


    }

}
