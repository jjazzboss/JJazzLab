package org.jjazz.proswing.walkingbass.db;

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
import java.util.Random;
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
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.proswing.walkingbass.Score;
import org.jjazz.proswing.walkingbass.WbpSourceAdaptation;
import org.jjazz.proswing.walkingbass.WbpsaScorer;
import org.jjazz.proswing.walkingbass.WbpsaScorerDefault;
import static org.jjazz.proswing.walkingbass.WalkingBassMusicGenerator.NON_QUANTIZED_WINDOW;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;
import org.jjazz.utilities.api.Utilities;
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
    private static WbpSourceDatabase INSTANCE;
    @StaticResource(relative = true)
    private static final String MIDI_FILE_WALKING_RESOURCE_PATH = "WalkingBassMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_A_RESOURCE_PATH = "WalkingBass2feelAMidiDB.mid";
    @StaticResource(relative = true)
    private static final String MIDI_FILE_2FEEL_B_RESOURCE_PATH = "WalkingBass2feelBMidiDB.mid";

    private final Map<String, String> mapSessionIdResource;
    private final ListMultimap<String, WbpSource> mmapSessionIdWbpSources;
    private final ListMultimap<BsRp, WbpSource> mmapBsrpWbpSources;
    private final Map<String, WbpSource> mapIdWbpSource;
    private final Random random;
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
        mmapBsrpWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
        mmapSessionIdWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
        mapIdWbpSource = new HashMap<>();
        mapSessionIdResource = new HashMap<>();
        random = new Random();

        // Extract the WbpSources from the WbpSessions
        List<WbpSession> wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_WALKING_RESOURCE_PATH, "", "walking", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_A_RESOURCE_PATH, "2FA", "2feel-a", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        wbpSessions = loadWbpSessionsFromMidiFile(MIDI_FILE_2FEEL_B_RESOURCE_PATH, "2FB", "2feel-b", TimeSignature.FOUR_FOUR);
        wbpSessions.forEach(s -> processWbpSession(s));

        // {x,number,#} avoids auto number formatting which turns 1200 into "1,200"
        LOGGER.log(Level.SEVERE, "WbpDatabase() 1-bar:{0,number,#}  2-bar:{1,number,#}  3-bar:{2,number,#}  4-bar:{3,number,#}", new Object[]
        {
            getNbWbpSources(1), getNbWbpSources(2), getNbWbpSources(3), getNbWbpSources(4)
        });
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
                && getFirstCompatibleWbpSource(wbps.getBassStyle(), wbps.getSimpleChordSequence(), wbps.getSizedPhrase(), true) == null)
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
     * Get all the WbpSources from the database (in no particular order).
     *
     * @return An unmodifiable list
     */
    public List<WbpSource> getWbpSources()
    {
        return new ArrayList<>(mapIdWbpSource.values());
    }

    /**
     * The number of WbpSources in the database matching the specified parameters.
     *
     * @param nbBars WbpSource size in bars. [1;4] or -1. If -1 all sizes are counted.
     * @param styles If empty, all BassStyles are counted.
     * @return
     */
    public int getNbWbpSources(int nbBars, BassStyle... styles)
    {
        Preconditions.checkArgument(nbBars == -1 || checkWbpSourceSize(nbBars), "nbBars=%s", nbBars);
        List<BassStyle> bassStyles = List.of(styles);
        int res;
        if (nbBars == -1 && bassStyles.isEmpty())
        {
            res = mapIdWbpSource.size();
        } else
        {
            res = (int) mapIdWbpSource.values().stream()
                    .filter(wbps -> (nbBars == -1 || wbps.getSimpleChordSequence().getBarRange().size() == nbBars)
                    && (bassStyles.isEmpty() || bassStyles.contains(wbps.getBassStyle())))
                    .count();
        }
        return res;
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
        List<BassStyle> bassStyles = List.of(styles);
        List<WbpSource> res;
        if (nbBars == -1 && bassStyles.isEmpty())
        {
            res = new ArrayList<>(mapIdWbpSource.values());
        } else
        {
            res = mapIdWbpSource.values().stream()
                    .filter(wbps -> (nbBars == -1 || wbps.getSimpleChordSequence().getBarRange().size() == nbBars)
                    && (bassStyles.isEmpty() || bassStyles.contains(wbps.getBassStyle())))
                    .toList();
        }
        return res;
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
        List<WbpSource> res = mmapBsrpWbpSources.get(new BsRp(style, rootProfile));
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

    /**
     * Get the most probable velocity range.
     * <p>
     * TODO: calculate it from the WbpSources.
     *
     * @return
     *
     */
    public IntRange getMostProbableVelocityRange()
    {
        return new IntRange(50, 65);
    }

    /**
     * Get a random velocity using a gaussian distribution in the range returned by getMostProbableVelocityRange().
     *
     * @return
     * @see #getMostProbableVelocityRange()
     */
    public int getRandomVelocity()
    {
        float r = Utilities.getNextGaussianRandomValue(random); // [-1;1]
        var rg = getMostProbableVelocityRange();
        float f = rg.getCenter() + r * rg.size() / 2;
        int vel = rg.clamp(Math.round(f));
        return vel;
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
        var wbpSources = wbpSession.extractWbpSources(false, false);
        for (var wbpSource : wbpSources)
        {
            if (getFirstCompatibleWbpSource(wbpSource.getBassStyle(), wbpSource.getSimpleChordSequence(), wbpSource.getSizedPhrase(), true) == null)
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


            // Get the phrase
            Phrase p = Phrases.getSlice(bigPhrase, beatRange, false, 2, 0);
            if (p.isEmpty())
            {
                LOGGER.log(Level.FINE, "Empty session found for barRange={0}", barRange);
                continue;
            }
            p.shiftAllEvents(-beatRange.from);
            beatRange = beatRange.getTransformed(-beatRange.from);
            SizedPhrase sp = new SizedPhrase(0, beatRange, ts, false);
            sp.add(p);


            // Get the SimpleChordSequence
            SimpleChordSequence cSeq = getChordSequence(sizeInBars, ts, sessionChords, startPosInTicks);


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
        WbpsaScorer scorer = new WbpsaScorerDefault(null, -1, null, style);

        var rp = RootProfile.of(scs);
        var wbpSources = getWbpSources(style, rp);

        for (WbpSource wbpSource : wbpSources)
        {
            if (!sp.equalsAsIntervals(wbpSource.getSizedPhrase(), checkNotesDuration, NON_QUANTIZED_WINDOW))
            {
                continue;
            }
            var wbpsa = new WbpSourceAdaptation(wbpSource, scs);
            var score = scorer.computeCompatibilityScore(wbpsa, null);
            if (score.compareTo(Score.ZERO) > 0)
            {
                res = wbpSource;
                break;
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
        if (!mmapSessionIdWbpSources.put(wbpSource.getSessionId(), wbpSource))
        {
            throw new IllegalStateException("Adding to mmapSessionIdWbpSources failed for " + wbpSource + " mmapSessionIdWbpSources=" + mmapSessionIdWbpSources);
        }

        BsRp bsRp = new BsRp(wbpSource.getBassStyle(), wbpSource.getRootProfile());
        if (!mmapBsrpWbpSources.put(bsRp, wbpSource))
        {
            throw new IllegalStateException("Adding to mmapBsrpWbpSources failed for " + wbpSource + " mmapBsrpWbpSources=" + mmapBsrpWbpSources);
        }
    }

    private void removeWbpSourceImpl(WbpSource wbpSource)
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

        BsRp bsRp = new BsRp(wbpSource.getBassStyle(), wbpSource.getRootProfile());
        if (!mmapBsrpWbpSources.remove(bsRp, wbpSource))
        {
            throw new IllegalStateException("Removing non-existing WbpSource=" + wbpSource + " mmapBsrpWbpSources=" + mmapBsrpWbpSources);
        }
    }


    // =================================================================================================================================
    // Inner classes
    // =================================================================================================================================
    /**
     * Most of the database requests for WbpSources are based on a combination of these 2 parameters.
     */
    static private record BsRp(BassStyle bassStyle, RootProfile rootProfile)
            {

        public BsRp 
        {
            Objects.requireNonNull(bassStyle);
            Objects.requireNonNull(rootProfile);
        }


    }
}
