package org.jjazz.jjswing.bass.db;

import org.jjazz.jjswing.api.BassStyle;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
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
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.jjswing.drums.db.DpSourceDatabase;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.jjswing.bass.Score;
import org.jjazz.jjswing.bass.WbpSourceAdaptation;
import static org.jjazz.jjswing.bass.BassGenerator.NON_QUANTIZED_WINDOW;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Exceptions;
import org.jjazz.jjswing.bass.WbpsaScorer;

/**
 * The walking bass source phrase database.
 * <p>
 * Contains WbpSource phrases of 1, 2 3, or 4 bars. Note that the 3 * 2-bar subphrases that compose each 4-bar phrase are also found in the database. Same for
 * the 4 or 2 * 1-bar subphrases of a 4 or 2-bar phrase.
 * <p>
 * Example: If we have Cm7-F7-E7-Em7 in the database, then we also have Cm7-F7, F7-E7, E7-Em7, Cm7, F7, E7, Em7 phrases in the database.
 * <p>
 * The implementation is thread safe: this is required because sometimes 2 threads might call it with concurrent read/write access, for example when a
 * background music generation is performed and user wants to preview-hear a change from RP_SYS_OverrideTracks editor.
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
    private static WbpSourceDatabase INSTANCE;
    @StaticResource(relative = true)
    private static final String MIDI_FILE_WALKING_RESOURCE_PATH = "WalkingBassMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_A_RESOURCE_PATH = "WalkingBass2feelAMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_B_RESOURCE_PATH = "WalkingBass2feelBMidiDB.mid";
    private final Map<String, String> mapSessionIdResource;
    private final Database database;

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
        long time = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "WbpSourceDatabase() initializing...");


        database = new Database();
        mapSessionIdResource = new HashMap<>();

        // Extract the WbpSources from the WbpSessions
        List<WbpSession> wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_WALKING_RESOURCE_PATH, "", "walking", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_A_RESOURCE_PATH, "2FA", "2feel-a", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_B_RESOURCE_PATH, "2FB", "2feel-b", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));


        time = System.currentTimeMillis() - time;
        // {x,number,#} avoids auto number formatting which turns 1200 into "1,200"        
        LOGGER.log(Level.INFO, "WbpSourceDatabase() initialized in {0,number,#}ms  ", new Object[]
        {
            time
        });

//        LOGGER.log(Level.FINE, "WbpSourceDatabase() 1-bar:{0,number,#}  2-bar:{1,number,#}  3-bar:{2,number,#}  4-bar:{3,number,#}", new Object[]
//        {
//            getNbWbpSources(1), getNbWbpSources(2), getNbWbpSources(3), getNbWbpSources(4)
//        });

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
        if (!database.exists(wbps.getId())
                && getFirstCompatibleWbpSource(wbps.getBassStyle(), wbps.getSimpleChordSequence(), wbps.getSizedPhrase(), true) == null)
        {
            database.add(wbps);
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
        if (database.exists(wbps.getId()))
        {
            database.remove(wbps);
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
        ctdb.getChordTypes().forEach(ct -> allChordTypes.add(getSimplified(ct)));

        var clif = CLI_Factory.getDefault();
        final var C_NOTE = new Note();
        final var POS0 = new Position();
        final var POS2 = new Position(0, 2);
        final int NB_BARS = 1;
        final var scs = new SimpleChordSequence(new IntRange(0, NB_BARS - 1), 0, TimeSignature.FOUR_FOUR);
        // final WbpsaScorer scorer = new WbpsaScorer(null, -1, null, bStyle);
        final WbpsaScorer scorer = new WbpsaScorer(null, null);


        // Check for 1-chord-per-bar with 0 or only 1 one-bar WbpSource
        for (var ct : allChordTypes)
        {
            scs.clear();
            var ecs = new ExtChordSymbol(C_NOTE, C_NOTE, ct);
            scs.add(clif.createChordSymbol(ecs, POS0));
            var wbpsas = WbpSourceAdaptation.getWbpSourceAdaptations(scs, scorer, null, -1, List.of(bStyle));
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
                    var wbpsas = WbpSourceAdaptation.getWbpSourceAdaptations(scs, scorer, null, -1, List.of(bStyle));
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


        // Search suspicious ghost notes
        for (var wbpSource : getWbpSources(1, bStyle))
        {
            var sp = wbpSource.getSizedPhrase();
            float lastStartPos = -1;
            final float LIMIT = 0.06f;
            for (var ne : sp)
            {
                var pos = ne.getPositionInBeats();
                if (pos - lastStartPos < LIMIT)
                {
                    LOGGER.log(Level.SEVERE, "checkConsistency() suspicious near-same-position notes at pos={0} for {1}", new Object[]
                    {
                        pos, wbpSource
                    });
                }
                if (ne.getDurationInBeats() < LIMIT)
                {
                    LOGGER.log(Level.SEVERE, "checkConsistency() suspicious very short note at pos={0} for {1}", new Object[]
                    {
                        pos, wbpSource
                    });
                }
                lastStartPos = pos;
            }
        }
    }


    /**
     * Get the WbpSources matching the specified parameters.
     * <p>
     * Results are NOT cached, this can impact performance.
     *
     * @param nbBars WbpSource size in bars. [1;4] or -1. If -1 WbpSources of all sizes are returned.
     * @param styles If empty, WbpSources of all BassStyles are returned.
     * @return
     */
    public List<WbpSource> getWbpSources(int nbBars, BassStyle... styles)
    {
        Preconditions.checkArgument(nbBars == -1 || checkWbpSourceSize(nbBars), "nbBars=%s", nbBars);
        return database.getWbpSources(nbBars, styles);
    }

    /**
     * Get the WbpSources matching size and bass style.
     * <p>
     * Results are cached.
     *
     * @param style
     * @param rootProfile
     * @return Unmodifiable list
     */
    public List<WbpSource> getWbpSources(BassStyle style, RootProfile rootProfile)
    {
        Objects.requireNonNull(style);
        Objects.requireNonNull(rootProfile);
        var sap = new StyleAndProfile(style, rootProfile);
        var res = Collections.unmodifiableList(database.getWbpSources(sap));
        return res;
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
        return Collections.unmodifiableList(database.getSessionWbpSources(sessionId));
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


    /**
     * Check that nbBars is a valid WbpSource size.
     *
     * @param nbBars
     * @return
     */
    static public boolean checkWbpSourceSize(int nbBars)
    {
        return nbBars >= SIZE_MIN && nbBars <= SIZE_MAX;
    }

    public void dump()
    {
        LOGGER.info("WbpDatabase dump =========================================");
        for (var wbps : getWbpSources(-1))
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
        LOGGER.log(Level.FINE, "processWbpSession() -- wbpSession={0}", wbpSession);
        var wbpSources = wbpSession.extractWbpSources(false, false);
        for (var wbpSource : wbpSources)
        {
            if (getFirstCompatibleWbpSource(wbpSource.getBassStyle(), wbpSource.getSimpleChordSequence(), wbpSource.getSizedPhrase(), true) == null)
            {
                // Add only non redundant phrases                
                database.add(wbpSource);
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
     *
     * @param midiFileResourcePath
     * @param sessionIdPrefix      A prefix added to each sessionId
     * @param sessionTag           A tag added to the tags read from the resource file
     * @param ts
     * @return
     */
    private List<WbpSession> loadWbpSessionsFromMidiFile(String midiFileResourcePath, String sessionIdPrefix, String sessionTag, TimeSignature ts)
    {
        LOGGER.log(Level.FINE, "loadWbpSessionsFromMidiFile() -- prefix={0} sessionTag={1} file={2}", new Object[]
        {
            sessionIdPrefix, sessionTag, midiFileResourcePath
        });

        List<WbpSession> res = new ArrayList<>();

        Sequence sequence = DpSourceDatabase.loadSequenceFromResource(getClass(), midiFileResourcePath);
        int seqResolution = sequence.getResolution();
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new IllegalStateException("sequence.getDivisionType()=" + sequence.getDivisionType());
        }

        // Get all markers at the appropriate resolution
        var markerEvents = DpSourceDatabase.getMarkersSorted(sequence);


        // Verify the presence of the end session marker
        var lastMarker = markerEvents.getLast();
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


            // Get the phrase
            Phrase p = Phrases.getSlice(bigPhrase, beatRange, false, 2, 0);
            if (p.isEmpty())
            {
                LOGGER.log(Level.FINE, "Empty session found for barRange={0}", barRange);
                continue;
            }
            p.shiftAllEvents(-beatRange.from, false);
            beatRange = beatRange.getTransformed(-beatRange.from);
            SizedPhrase sp = new SizedPhrase(0, beatRange, ts, false);
            sp.add(p);


            // Get the SimpleChordSequence
            SimpleChordSequence cSeq = getChordSequence(sizeInBars, ts, sessionChords, startPosInTicks);


            // Build the WbpSession
            WbpSession session = new WbpSession(sessionId, sessionTags, cSeq, sp, targetNote);
            if (mapSessionIdResource.put(sessionId, midiFileResourcePath) != null)
            {
                LOGGER.severe(() -> "loadWbpSessionsFromMidiFile() ignoring session with duplicate sessionId=" + sessionId);
            } else
            {
                res.add(session);
            }

        }

        return res;

    }


    private SimpleChordSequence getChordSequence(int nbBars, TimeSignature ts, List<MidiEvent> sessionChords, long tickOffset)
    {
        SimpleChordSequence cSeq = new SimpleChordSequence(new IntRange(0, nbBars - 1), 0, ts);

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
     * Find the first WbpSource compatible with the specified parameters.
     * <p>
     * Compatible means chord sequences share the same root profile and phrases have the same note intervals and approximately the same note positions.
     *
     * @param style
     * @param scs
     * @param sp
     * @param checkNotesDuration If true, to be compatible sp notes must also have (approximatively) the same duration
     * @return Can be null
     * @see SizedPhrase#equalsAsIntervals(org.jjazz.phrase.api.Phrase, boolean, float)
     */
    private WbpSource getFirstCompatibleWbpSource(BassStyle style, SimpleChordSequence scs, SizedPhrase sp, boolean checkNotesDuration)
    {
        WbpSource res = null;
        WbpsaScorer scorer = new WbpsaScorer(null, null);

        var rp = RootProfile.of(scs);
        var wbpSources = getWbpSources(style, rp);

        for (WbpSource wbpSource : wbpSources)
        {
            if (!sp.equalsAsIntervals(wbpSource.getSizedPhrase(), checkNotesDuration, NON_QUANTIZED_WINDOW))
            {
                continue;
            }
            var wbpsa = WbpSourceAdaptation.of(wbpSource, scs);
            if (scorer.updateCompatibilityScore(wbpsa, null, -1).compareTo(Score.ZERO) > 0)
            {
                res = wbpSource;
                break;
            }
        }
        return res;
    }


    // =================================================================================================================================
    // Inner classes
    // =================================================================================================================================
    /**
     * Most of the database requests for WbpSources are based on a combination of these 2 parameters.
     */
    static private record StyleAndProfile(BassStyle bassStyle, RootProfile rootProfile)
            {

        public StyleAndProfile 
        {
            Objects.requireNonNull(bassStyle);
            Objects.requireNonNull(rootProfile);
        }
    }

    /**
     * Manage the real data in a synchronized way.
     * <p>
     * Just use synchronized methods: tried finer grain syncrhonization but it's not worth it.
     */
    static private class Database
    {

        private final ListMultimap<String, WbpSource> mmapSessionIdWbpSources;
        private final ListMultimap<StyleAndProfile, WbpSource> mmapSapWbpSources;
        private final Map<String, WbpSource> mapIdWbpSource;

        protected Database()
        {
            mmapSapWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
            mmapSessionIdWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
            mapIdWbpSource = new HashMap<>();
        }

        public synchronized boolean exists(String wbpSourceId)
        {
            return mapIdWbpSource.containsKey(wbpSourceId);
        }

        public synchronized List<WbpSource> getSessionWbpSources(String sessionId)
        {
            return mmapSessionIdWbpSources.get(sessionId);
        }

        public synchronized List<WbpSource> getWbpSources(int nbBars, BassStyle... styles)
        {
            List<BassStyle> bassStyles = List.of(styles);
            List<WbpSource> res;
            res = mapIdWbpSource.values().stream()
                    .filter(wbps -> (nbBars == -1 || wbps.getSimpleChordSequence().getBarRange().size() == nbBars)
                    && (bassStyles.isEmpty() || bassStyles.contains(wbps.getBassStyle())))
                    .toList();
            return res;
        }

        public synchronized List<WbpSource> getWbpSources(StyleAndProfile sap)
        {
            return mmapSapWbpSources.get(sap);
        }

        public synchronized void add(WbpSource wbpSource)
        {
            var old = mapIdWbpSource.put(wbpSource.getId(), wbpSource);
            if (old != null)
            {
                throw new IllegalStateException("Duplicate WbpSource.id found: wbpSource=" + wbpSource + " old=" + old);
            }
            if (!mmapSessionIdWbpSources.put(wbpSource.getSessionId(), wbpSource))
            {
                throw new IllegalStateException(
                        "Adding to mmapSessionIdWbpSources failed for " + wbpSource + " mmapSessionIdWbpSources=" + mmapSessionIdWbpSources);
            }

            StyleAndProfile sap = new StyleAndProfile(wbpSource.getBassStyle(), wbpSource.getRootProfile());
            if (!mmapSapWbpSources.put(sap, wbpSource))
            {
                throw new IllegalStateException("Adding to mmapBsrpWbpSources failed for " + wbpSource + " mmapBsrpWbpSources=" + mmapSapWbpSources);
            }
        }

        public synchronized void remove(WbpSource wbpSource)
        {
            var old = mapIdWbpSource.remove(wbpSource.getId());
            if (old == null)
            {
                throw new IllegalStateException("Removing non-existing WbpSourceId=" + wbpSource.getId());
            }
            if (!mmapSessionIdWbpSources.remove(wbpSource.getSessionId(), wbpSource))
            {
                throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource + " mmapSessionIdWbpSources=" + mmapSessionIdWbpSources);
            }

            StyleAndProfile bsRp = new StyleAndProfile(wbpSource.getBassStyle(), wbpSource.getRootProfile());
            if (!mmapSapWbpSources.remove(bsRp, wbpSource))
            {
                throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource + " mmapBsrpWbpSources=" + mmapSapWbpSources);
            }
        }

    }
}
