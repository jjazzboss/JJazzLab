package org.jjazz.jjswing.drums.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.jjswing.api.DrumsStyle;
import org.jjazz.jjswing.drums.db.DpSource.Type;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Exceptions;

/**
 * The drums source phrase database for a given time signature.
 * <p>
 * The database ensures that at least one STD and one FILL DpSource exist for each DrumsStyle.
 */
public class DpSourceDatabase
{

    private static DpSourceDatabase INSTANCE;
    @StaticResource(relative = true)
    private static final String MIDI_FILE_DRUMS_RESOURCE_PATH = "drums44DB.mid";

    private final TimeSignature timeSignature;
    private final Map<DrumsStyle, DpSourceSet> mapStyleDpSources;

    private static final Logger LOGGER = Logger.getLogger(DpSourceDatabase.class.getSimpleName());

    public static DpSourceDatabase getInstance(TimeSignature ts)
    {
        synchronized (DpSourceDatabase.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DpSourceDatabase(ts);
            }
        }
        return INSTANCE;
    }

    private DpSourceDatabase(TimeSignature ts)
    {
        this.timeSignature = ts;
        long time = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "DpSourceDatabase() {0} initializing...", ts);

        mapStyleDpSources = new HashMap<>();


        // Extract the DpSourceSet from the DrumsStyleSessions
        loadDpSourcesFromMidiFile(MIDI_FILE_DRUMS_RESOURCE_PATH, "", TimeSignature.FOUR_FOUR);


        time = System.currentTimeMillis() - time;
        // {x,number,#} avoids auto number formatting which turns 1200 into "1,200"        
        LOGGER.log(Level.INFO, "DpSourceDatabase() {0} initialized in {1,number,#}ms  ", new Object[]
        {
            timeSignature, time
        });

        // dump();

    }

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }


    /**
     * Get a DpSourceSet for a DrumsStyle.
     *
     * @param dStyle
     * @return Can not be null
     */
    public DpSourceSet getDpSourceSet(DrumsStyle dStyle) throws IllegalArgumentException
    {
        return mapStyleDpSources.get(dStyle);
    }


    /**
     * Perform various checks on the database.
     * <p>
     */
    public void checkConsistency()
    {
        LOGGER.log(Level.INFO, "checkConsistency() --");


        // Check that each possible DrumsStyle has a value
        for (var dStyle : DrumsStyle.values())
        {
            if (!mapStyleDpSources.containsKey(dStyle))
            {
                LOGGER.log(Level.SEVERE, "checkConsistency() Missing data for dStyle={0}", dStyle);
            }
        }
    }

    static public Sequence loadSequenceFromResource(Class clazz, String midiFileResourcePath)
    {
        Sequence sequence = null;
        try (InputStream is = clazz.getResourceAsStream(midiFileResourcePath))
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

    /**
     * Retrieve all markers and sort them : first session name "_xxx", then tags "#xxx", then chord symbols.
     *
     * @param sequence
     * @return
     */
    static public List<MidiEvent> getMarkersSorted(Sequence sequence)
    {
        int seqResolution = sequence.getResolution();

        // Get all markers at the appropriate resolution
        List<MidiEvent> res = new ArrayList<>();
        for (Track track : sequence.getTracks())
        {
            var events = MidiUtilities.getMidiEvents(track,
                    MetaMessage.class,
                    me -> me.getType() == MidiConst.META_MARKER,
                    null);
            events = MidiUtilities.getMidiEventsAtPPQ(events, seqResolution, MidiConst.PPQ_RESOLUTION);
            res.addAll(events);
        }
        assert !res.isEmpty();


        res.sort((left, right) -> 
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

        return res;
    }

    public void dump()
    {
        LOGGER.info("DpSourceDatabase dump =========================================");
        for (var dStyle : DrumsStyle.values())
        {
            getDpSourceSet(dStyle).dump();
        }

        LOGGER.info("==========================================================");
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================

    /**
     * Retrieve DpSources from a Midi resource file.
     * <p>
     * Midi file requirements:
     * <p>
     * - 1 marker _&lt;DRUMS_STYLE&gt; (eg "_RIDE_1") at the beginning of each section, possibly with some #tag markers<br>
     * - 1 marker "#fill" for the fill phrases<br>
     * - 1 marker "#alt" for each new alternate phrase<br>
     * - a final _END marker
     * <p>
     * Midi notes must use only 1 channel<br>
     * <p>
     *
     * @param midiFileResourcePath
     * @param sessionTag
     * @param ts
     */
    private void loadDpSourcesFromMidiFile(String midiFileResourcePath, String sessionTag, TimeSignature ts)
    {
        LOGGER.log(Level.FINE, "loadDpSourcesFromMidiFile() -- file={0}", new Object[]
        {
            midiFileResourcePath
        });

        Sequence sequence = loadSequenceFromResource(getClass(), midiFileResourcePath);
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new IllegalStateException("sequence.getDivisionType()=" + sequence.getDivisionType());
        }


        // Read the big phrase
        var phrases = Phrases.getPhrases(sequence.getResolution(), sequence.getTracks());
        assert phrases.size() == 1 : "nb phrases=" + phrases.size();
        var bigPhrase = phrases.get(0);


        // Get all markers sorted and verify the presence of first DrumsStyle and end session marker
        List<MidiEvent> sortedMarkerEvents = getMarkersSorted(sequence);
        var firstMarkerText = MidiUtilities.getText(sortedMarkerEvents.getFirst());
        var lastMarkerText = MidiUtilities.getText(sortedMarkerEvents.getLast());
        if (firstMarkerText.charAt(0) != '_' || !lastMarkerText.equalsIgnoreCase("_END"))
        {
            throw new IllegalStateException("firstMarkerText=" + firstMarkerText + " lastMarkerText=" + lastMarkerText);
        }


        for (int i = 0; i < sortedMarkerEvents.size() - 1; i++)
        {
            // New DrumsStyle
            var markerEvent = sortedMarkerEvents.get(i);
            var markerText = MidiUtilities.getText(markerEvent);
            DrumsStyle drumsStyle = getDrumsStyle(markerText);
            List<DpSource> dpSources = new ArrayList<>();
            List<DpSource> dpFillSources = new ArrayList<>();
            int altId = 0;
            DpSource.Type type = Type.STD;


            // Extract all tag strings, convert to lowercase
//            var sessionTags = new ArrayList<>(List.of(sessionTag.toLowerCase()));
//            sortedMarkerEvents.stream()
//                    .filter(me -> me.getTick() == markerEvent.getTick() && MidiUtilities.getText(me).startsWith("#"))
//                    .map(me -> MidiUtilities.getText(me).substring(1).trim().toLowerCase())
//                    .forEach(tag -> sessionTags.add(tag));
            // Loop on the DrumsStyle STD and FILL phrases
            boolean firstLoop = true;
            while (firstLoop || markerText.charAt(0) != '_')
            {
                firstLoop = false;


                // Get the phrase at marker and save it
                var nextMarkerEvent = sortedMarkerEvents.get(i + 1);
                var nextMarkerText = MidiUtilities.getText(nextMarkerEvent).toLowerCase();
                var drumsPhrase = getPhrase(bigPhrase, markerEvent.getTick(), nextMarkerEvent.getTick() - 1, ts);
                var percPhrase = new SizedPhrase(0, drumsPhrase.getNotesBeatRange(), ts, true);
                var dps = new DpSource(drumsStyle, type, altId, drumsPhrase, percPhrase);
                (type == Type.STD ? dpSources : dpFillSources).add(dps);


                if (nextMarkerText.equals("#alt"))
                {
                    altId++;
                } else if (nextMarkerText.equals("#fill"))
                {
                    altId = 0;
                    type = Type.FILL;
                }

                // Next 
                markerEvent = nextMarkerEvent;
                markerText = nextMarkerText;
                i++;
                // LOGGER.log(Level.SEVERE, "        looping  markerText={0}", markerText);
            }
            i--;

            // We can create the DpSourceSet and save it in the database
            var dpss = new DpSourceSet(dpSources, dpFillSources);
            LOGGER.log(Level.FINE, "loadDpSourcesFromMidiFile() adding dpss={0}", dpss);
            if (mapStyleDpSources.put(drumsStyle, dpss) != null)
            {
                LOGGER.log(Level.WARNING, "loadDpSourcesFromMidiFile() found duplicate drumsStyle={0}", drumsStyle);
            }

        }

    }

    private SizedPhrase getPhrase(Phrase bigPhrase, long startPosInTicks, long endPosInTicks, TimeSignature ts)
    {
        FloatRange beatRange = new FloatRange(startPosInTicks / (float) MidiConst.PPQ_RESOLUTION, endPosInTicks / (float) MidiConst.PPQ_RESOLUTION);

        Phrase p = Phrases.getSlice(bigPhrase, beatRange, false, 1, 0);
        if (p.isEmpty())
        {
            throw new IllegalArgumentException("p is empty ! startPosInTicks=" + startPosInTicks + " endPosInTicks=" + endPosInTicks);
        }
        p.shiftAllEvents(-beatRange.from, false);
        beatRange = beatRange.getTransformed(-beatRange.from);
        SizedPhrase res = new SizedPhrase(0, beatRange, ts, false);
        res.add(p);
        return res;
    }

    /**
     *
     * @param markerText A string like "_HI_HAT_2"
     * @return
     */
    private DrumsStyle getDrumsStyle(String markerText)
    {
        DrumsStyle res = null;
        try
        {
            res = DrumsStyle.valueOf(markerText.substring(1).toUpperCase());
        } catch (NullPointerException | IllegalArgumentException ex)
        {
            throw new IllegalStateException("Invalid DrumsStyle markerText=" + markerText);
        }
        return res;
    }

    // =================================================================================================================================
    // Inner classes
    // =================================================================================================================================

}
