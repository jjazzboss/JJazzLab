/*
 * JFugue, an Application Programming Interface (API) for Music Programming
 * http://www.jfugue.org
 *
 * Copyright (C) 2003-2014 David Koelle
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * MODIFICATIONS @March 2021 by Jerome Lelasseux for JJazzLab
 */
package org.jjazz.importers.musicxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.jjazz.harmony.ChordType;
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.Degree;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import static org.jjazz.importers.musicxml.MusicXmlParser.XMLtoJJazzChordMap;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * Parses a MusicXML file, and fires events for <code>MusicXmlParserListener</code> interfaces when tokens are interpreted.
 * <p>
 * The <code>ParserListener</code> does intelligent things with the resulting events, such as create music, draw sheet music, or
 * transform the data.
 * <p>
 * MusicXmlParser.parse can be called with a file name, File, InputStream, or Reader
 *
 * @author E.Philip Sobolik
 * @author David Koelle (updates for JFugue 5)
 * @author Richard Lavoie (Major rewriting)
 *
 * March 2020: Modifications J. Lelasseux for JJazzLab
 *
 */
public final class MusicXmlParser
{

    private final CopyOnWriteArrayList<MusicXmlParserListener> parserListeners;
    private final Builder xomBuilder;
    private Document xomDoc;

    private byte divisionsPerBeat;
    public TimeSignature timeSignature;
    private int curBarIndex;
    private int curDivisionInBar;

    private static final Logger LOGGER = Logger.getLogger(MusicXmlParser.class.getSimpleName());

    public static Map<String, String> XMLtoJJazzChordMap;

    // CONSTRUCTOR
    public MusicXmlParser() // throws ParserConfigurationException
    {
        initChordMap();

        parserListeners = new CopyOnWriteArrayList<MusicXmlParserListener>();
        xomBuilder = new Builder();

        // Set up MusicXML default values
        divisionsPerBeat = 1;
        curBarIndex = 0;
        curDivisionInBar = 0;
        timeSignature = TimeSignature.FOUR_FOUR;
    }

    public void parse(String musicXmlString) throws ValidityException, ParsingException, IOException
    {
        // URI is null when parsing a String as it's coming from somewhere else
        parse(xomBuilder.build(musicXmlString, (String) null));
    }

    public void parse(File inputFile) throws ValidityException, ParsingException, IOException
    {
        parse(xomBuilder.build(inputFile));
    }

    public void parse(FileInputStream inputStream) throws ValidityException,
            ParsingException, IOException
    {
        parse(xomBuilder.build(inputStream));
    }

    public void parse(Reader reader) throws ValidityException,
            ParsingException, IOException
    {
        parse(xomBuilder.build(reader));
    }

    private void parse(Document document)
    {
        xomDoc = document;
        parse();
    }

    /**
     * Parses a MusicXML file and fires events to subscribed <code>ParserListener</code> interfaces.
     * <p>
     * As the file is parsed, events are sent to <code>ParserListener</code> interfaces, which are responsible for doing something
     * interesting with the music data.
     * <p>
     * the input is a XOM Document, which has been built previously
     *
     * @throws Exception if there is an error parsing the pattern
     */
    public void parse()
    {
        fireBeforeParsingStarts();
        Element root = xomDoc.getRootElement();
        if (root.getQualifiedName().equalsIgnoreCase("score-timewise"))
        {
            LOGGER.warning("parse() score-timewise musicXML is not currently supported.");
            return;
        } else if (root.getQualifiedName().equalsIgnoreCase("score-partwise"))
        {
            parseHarmonyPartWise(root);
        }
        fireAfterParsingFinished();
    }

    private void parseHarmonyPartWise(Element root)
    {
        Element part = findPartContainingHarmony(root);
        if (part == null)
        {
            LOGGER.warning("parseHarmonyPartWise() No part found with an harmony element.");
            return;
        }

        LOGGER.fine("parseHarmonyPartWise() Processing part id=" + part.getAttribute("id").getValue());

        for (Element elMeasure : part.getChildElements("measure"))
        {
            int barId = Integer.parseInt(elMeasure.getAttribute("number").getValue());
            LOGGER.log(Level.FINE, "parseHarmonyPartWise() processing measure numer={0}", barId);
            curDivisionInBar = 0;
            fireBarLineParsed(barId, curBarIndex);
            parseMeasure(elMeasure);
            curBarIndex++;
        }
    }

    /**
     * Search the first Part which has at least one Harmony element defined.
     *
     * @param elRoot
     * @return Can be null
     */
    private Element findPartContainingHarmony(Element elRoot)
    {
        Element res = null;
        for (Element elPart : elRoot.getChildElements("part"))
        {
            for (Element elMeasure : elPart.getChildElements("measure"))
            {
                if (elMeasure.getFirstChildElement("harmony") != null)
                {
                    res = elPart;
                    break;
                }
            }
        }
        return res;
    }

    private void parseMeasure(Element musicDataRoot)
    {
        Element attributes = musicDataRoot.getFirstChildElement("attributes");
        if (attributes != null)
        {
            this.divisionsPerBeat = getByteValueOrDefault(attributes.getFirstChildElement("divisions"), this.divisionsPerBeat);

            // Time signature
            Element elTime = attributes.getFirstChildElement("time");
            if (elTime != null)
            {
                int upper = Integer.parseInt(elTime.getFirstChildElement("beats").getValue());      // Mandatory
                int lower = Integer.parseInt(elTime.getFirstChildElement("beat-type").getValue());  // Mandatory
                timeSignature = TimeSignature.get(upper, lower);
                if (timeSignature == null)
                {
                    LOGGER.warning("parseMusicData() Invalid time signature=" + upper + "/" + lower + ". Using 4/4 instead.");
                    timeSignature = TimeSignature.FOUR_FOUR;
                }
                fireTimeSignatureParsed(timeSignature, curBarIndex);
            }
        }

        for (Element el : musicDataRoot.getChildElements())
        {
            LOGGER.log(Level.FINE, "parseMeasure() el={0}", el.getLocalName());
            switch (el.getLocalName())
            {
                case "harmony":
                    parseHarmony(el, curBarIndex, curDivisionInBar);
                    break;
                case "note":
                {
                    // Grace notes don't have a duration
                    Element dur = el.getFirstChildElement("duration");
                    if (dur != null)
                    {
                        int duration = Integer.parseInt(dur.getValue());
                        int attack = getIntAttributeOrDefault(el, "attack", 0);
                        int release = getIntAttributeOrDefault(el, "release", 0);
                        curDivisionInBar += duration + attack + release;
                    }
                    break;
                }
                case "backup":
                {
                    int duration = Integer.parseInt(el.getFirstChildElement("duration").getValue());
                    curDivisionInBar -= duration;
                    break;
                }
                case "forward":
                {
                    int duration = Integer.parseInt(el.getFirstChildElement("duration").getValue());
                    curDivisionInBar += duration;
                    break;
                }
                // sound can be embedded in direction
                case "direction":
                    Element sound = el.getFirstChildElement("sound");
                    if (sound != null)
                    {
                        String value = sound.getAttributeValue("tempo");
                        if (value != null)
                        {
                            fireTempoChanged(Integer.parseInt(value), curBarIndex);
                        }
                    }
                    break;
                // sound can be directly in the measure as well
                case "sound":
                    String value = el.getAttributeValue("tempo");
                    if (value != null)
                    {
                        fireTempoChanged(Integer.parseInt(value), curBarIndex);
                    }
                    break;
                default:
                // Nothing
            }
            if (curDivisionInBar < 0)
            {
                LOGGER.severe("parseMusicData() invalid value for curDivisionInBar=" + curDivisionInBar + ", el=" + el + ". Resetting value to 0");
                curDivisionInBar = 0;
            }
        }
    }

    /**
     * Search the first child element corresponding to elementHierarchy.
     * <p>
     * Example: if elementHierarchy=["time", "beat"], search the first element "beat" within the first element "time".
     *
     * @param element
     * @param elementHierarchy
     * @return Null if could not find the last element of elementHierarchy.
     */
    private Element getFirstChildElementInHierarchy(Element element, String... elementHierarchy)
    {
        Element el = element;
        for (String c : elementHierarchy)
        {
            if (el == null)
            {
                return null;
            }
            el = el.getFirstChildElement(c);
        }
        return el;
    }

    private int getIntAttributeOrDefault(Element el, String attr, int defaultValue)
    {
        int res = defaultValue;
        String s = el.getAttributeValue(attr);
        if (s == null || s.isBlank())
        {
            return defaultValue;
        }
        try
        {
            res = Integer.valueOf(s);
        } catch (NumberFormatException ex)
        {
            // Do nothing
        }
        return res;
    }

    private byte getByteValueOrDefault(Element element, byte defaultValue)
    {
        if (element != null)
        {
            int value = Integer.parseInt(element.getValue());
            return (byte) value;
            //return (byte) (value - 1);
        }
        return defaultValue;
    }

    /**
     * Parse the Harmony element.
     *
     * @param elHarmony
     */
    private void parseHarmony(Element elHarmony, int barIndex, int divisionPosInBar)
    {
        if (barIndex < 0 || divisionPosInBar < 0)
        {
            throw new IllegalArgumentException("harmony=" + elHarmony + " barIndex=" + barIndex + " divisionPosInBar=" + divisionPosInBar);
        }
        ChordTypeDatabase ctdb = ChordTypeDatabase.getInstance();

        // Mandatory : the root note
        String strRoot = getNoteFromRootElement(elHarmony);

        // Optional bass note
        String strBass = getNoteFromBassElement(elHarmony);
        if (!strBass.isBlank())
        {
            strBass = "/" + strBass;
        }

        // Optional offset
        Element elOffset = elHarmony.getFirstChildElement("offset");
        if (elOffset != null)
        {
            int offset = Integer.parseInt(elOffset.getValue());
            divisionPosInBar += offset;
            if (divisionPosInBar < 0)
            {
                LOGGER.severe("parseHarmony() invalid value for divisionPosInBar=" + divisionPosInBar + ", barIndex=" + barIndex + ", elOffset=" + elOffset + ", elHarmony=" + elHarmony + ". Resetting value to 0");
                divisionPosInBar = 0;
            }
        }

        // The chord symbol position
        float beat = (float) divisionPosInBar / divisionsPerBeat;
        beat = Math.round(beat);
        if (!timeSignature.checkBeat(beat))
        {
            beat = timeSignature.getUpper() - 1;
        }
        Position pos = new Position(barIndex, beat);

        // Mandatory : the kind of chord, "dominant-seven", "major", etc.
        Element chord_kind = elHarmony.getFirstChildElement("kind");
        String strKindValue = chord_kind.getValue();
        String strKindText = chord_kind.getAttributeValue("text");   // Optional

        // Get the standard degrees corresponding to chord kind
        List<Degree> degrees = new ArrayList<>();
        if (strKindValue.equals("none"))
        {
            // Special case: no chord 
            // Not supported in JJazzLab
            return;
        } else if (strKindValue.equals("other"))
        {
            // Special case, will use only the degrees specified in the "degree" XML elements
        } else
        {
            // Get the corresponding chordtype string 
            String strChordType = XMLtoJJazzChordMap.get(strKindValue);
            if (strChordType == null)
            {
                // kind value is not supported
                if (strKindText != null)
                {
                    // Try to directly use text if present
                    ChordType ct = ctdb.getChordType(strKindText);
                    if (ct != null)
                    {
                        fireChordSymbolParsed(strRoot + strKindText + strBass, pos);
                        return;
                    } else
                    {
                        LOGGER.warning("parseHarmony() No chord type found for kind_value=" + strKindValue + " in element harmony=" + elHarmony.toString() + ". Using major chord instead.");
                        strChordType = "";
                    }
                }
            }

            // Get the corresponding chordtype which will give us the Degrees
            ChordType ct = ctdb.getChordType(strChordType);
            assert ct != null : "strChordType=" + strChordType;
            degrees.addAll(ct.getDegrees());
        }

        // Optional degrees       
        parseDegrees(elHarmony, degrees);

        // Now find the chordtype using the resulting degrees
        ChordType ct = degrees.isEmpty() ? null : ctdb.getChordType(degrees);
        if (ct == null)
        {
            // Try to directly use kind text if present
            ct = ctdb.getChordType(strKindText);
            if (ct == null)
            {
                LOGGER.warning("parseHarmony() Can't parse chord symbol for " + strKindText + ". Using major chord instead.");
                ct = ctdb.getChordType(0);
            } else
            {
                LOGGER.warning("parseHarmony() Can't parse chord symbol for " + strKindText + ". Using chord kind value=" + ct.getName() + " instead.");
            }
        }

        // Finally assemble the chord and fire event
        String strChord = strRoot + ct.getName() + strBass;
        fireChordSymbolParsed(strChord, pos);
    }

    private String getNoteFromRootElement(Element harmony)
    {
        StringBuilder sb = new StringBuilder();
        Element element = harmony.getFirstChildElement("root");
        if (element != null)
        {
            Element element_step = element.getFirstChildElement("root-step");   // Mandatory
            sb.append(element_step.getValue());
            Element element_alter = element.getFirstChildElement("root-alter"); // Optional
            if (element_alter != null)
            {
                if (element_alter.getValue().equals("-1"))
                {
                    sb.append("b");
                } else if (element_alter.getValue().equals("+1"))
                {
                    sb.append("#");
                }
            }
        }
        return sb.toString();
    }

    /**
     * @param harmony
     * @return An empty string no bass element specified
     */
    private String getNoteFromBassElement(Element harmony)
    {
        StringBuilder sb = new StringBuilder();
        Element element = harmony.getFirstChildElement("bass");
        if (element != null)
        {
            Element element_step = element.getFirstChildElement("bass-step");   // Mandatory
            sb.append(element_step.getValue());
            Element element_alter = element.getFirstChildElement("bass-alter"); // Optional
            if (element_alter != null)
            {
                if (element_alter.getValue().equals("-1"))
                {
                    sb.append("b");
                } else if (element_alter.getValue().equals("+1"))
                {
                    sb.append("#");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Read the "degree" Harmony elements and update the specified degrees list accordingly.
     *
     * @param harmony
     * @param degrees Degree list to be adjusted. Can already contain some Degrees.
     * @return
     */
    private void parseDegrees(Element harmony, List<Degree> degrees)
    {
        for (Element element : harmony.getChildElements("degree"))
        {
            Element elValue = element.getFirstChildElement("degree-value");     // mandatory
            Element elAlter = element.getFirstChildElement("degree-alter");     // mandatory
            Element elType = element.getFirstChildElement("degree-type");       // mandatory

            int intValue = Integer.parseInt(elValue.getValue());    // 1 for root, 5 for fifth, 13 for thirteenth etc.
            int intAlter = Integer.parseInt(elAlter.getValue());    // -1, 0, +1
            String strType = elType.getValue();                //  add, alter, substract

            final Degree degree;
            switch (intValue)
            {
                case 1:
                    degree = Degree.ROOT;
                    break;
                case 2:
                    degree = Degree.getDegree(Degree.Natural.NINTH, intAlter);
                    break;
                case 3:
                    degree = Degree.getDegree(Degree.Natural.THIRD, intAlter);
                    break;
                case 4:
                    degree = Degree.getDegree(Degree.Natural.ELEVENTH, intAlter);
                    break;
                case 5:
                    degree = Degree.getDegree(Degree.Natural.FIFTH, intAlter);
                    break;
                case 6:
                    degree = Degree.getDegree(Degree.Natural.SIXTH, intAlter);
                    break;
                case 7:
                    degree = Degree.getDegree(Degree.Natural.SEVENTH, intAlter);
                    break;
                case 9:
                    degree = Degree.getDegree(Degree.Natural.NINTH, intAlter);
                    break;
                case 11:
                    degree = Degree.getDegree(Degree.Natural.ELEVENTH, intAlter);
                    break;
                case 13:
                    degree = Degree.getDegree(Degree.Natural.SIXTH, intAlter);
                    break;
                default:
                    LOGGER.warning("parseDegrees() degree-value=" + intValue + " not supported. Skipping degree element...");
                    continue;
            }
            switch (strType)
            {
                case "add":
                case "alter":
                    degrees.removeIf(d -> d.getNatural().equals(degree.getNatural()));
                    degrees.add(degree);
                    break;
                case "subtract":
                    LOGGER.warning("parseDegrees() degree-type=substract not handled. Skipping degree element...");
                    continue;
                default:
                    throw new IllegalStateException("strType=" + strType);
            }
        }
    }

    /**
     * converts beats per minute (BPM) to pulses per minute (PPM) assuming 240 pulses per second In MusicXML, BPM can be
     * fractional, so <code>BPMtoPPM</code> takes a float argument
     *
     * @param bpm
     * @return ppm
     */
    public static int BPMtoPPM(float bpm)
    {
        return (Float.valueOf((60.f * 240.f) / bpm).intValue());
    }

    public void addParserListener(MusicXmlParserListener listener)
    {
        parserListeners.add(listener);
    }

    public void removeParserListener(MusicXmlParserListener listener)
    {
        parserListeners.remove(listener);
    }

    public List<MusicXmlParserListener> getParserListeners()
    {
        return parserListeners;
    }

    public void clearParserListeners()
    {
        this.parserListeners.clear();
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
    //
    // Event firing methods
    //
    private void fireBeforeParsingStarts()
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.beforeParsingStarts();
        }
    }

    private void fireAfterParsingFinished()
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.afterParsingFinished();
        }
    }

    private void fireTempoChanged(int tempoBPM, int barIndex)
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.onTempoChanged(tempoBPM, barIndex);
        }
    }

    private void fireTimeSignatureParsed(TimeSignature ts, int barIndex)
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.onTimeSignatureParsed(ts, barIndex);
        }
    }

    private void fireBarLineParsed(int id, int barIndex)
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.onBarLineParsed(id, barIndex);
        }
    }

    private void fireLyricParsed(String lyric, Position pos)
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.onLyricParsed(lyric, pos);
        }
    }

    private void fireNoteParsed(Note note, Position pos)
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.onNoteParsed(note, pos);
        }
    }

    private void fireChordSymbolParsed(String strChord, Position pos)
    {
        List<MusicXmlParserListener> listeners = getParserListeners();
        for (MusicXmlParserListener listener : listeners)
        {
            listener.onChordSymbolParsed(strChord, pos);
        }
    }

    private void initChordMap()
    {
        XMLtoJJazzChordMap = new TreeMap<String, String>();

        // Triads
        XMLtoJJazzChordMap.put("major", "");
        XMLtoJJazzChordMap.put("minor", "m");
        XMLtoJJazzChordMap.put("augmented", "+");
        XMLtoJJazzChordMap.put("diminished", "dim");

        // Sevenths
        XMLtoJJazzChordMap.put("dominant", "7");
        XMLtoJJazzChordMap.put("major-seventh", "M7");
        XMLtoJJazzChordMap.put("minor-seventh", "m7");
        XMLtoJJazzChordMap.put("diminished-seventh", "dim7");
        XMLtoJJazzChordMap.put("augmented-seventh", "7#5");
        XMLtoJJazzChordMap.put("half-diminished", "m7b5");
        XMLtoJJazzChordMap.put("major-minor", "m7M");

        // Sixths
        XMLtoJJazzChordMap.put("major-sixth", "6");
        XMLtoJJazzChordMap.put("minor-sixth", "m6");

        // Ninths
        XMLtoJJazzChordMap.put("dominant-ninth", "9");
        XMLtoJJazzChordMap.put("major-ninth", "M9");
        XMLtoJJazzChordMap.put("minor-ninth", "m9");

        // 11ths (usually as the basis for alteration):        
        XMLtoJJazzChordMap.put("dominant-11th", "9sus");
        XMLtoJJazzChordMap.put("major-11th", "M9");
        XMLtoJJazzChordMap.put("minor-11th", "m11");

        // 13ths (usually as the basis for alteration):        
        XMLtoJJazzChordMap.put("dominant-13th", "13");
        XMLtoJJazzChordMap.put("major-13th", "M13");
        XMLtoJJazzChordMap.put("minor-13th", "m13");

        // Suspended Chords
        XMLtoJJazzChordMap.put("suspended-fourth", "sus");
        XMLtoJJazzChordMap.put("suspended-second", "sus");

        // Functional sixths
        XMLtoJJazzChordMap.put("Neapolitan", null);      // Not supported
        XMLtoJJazzChordMap.put("Italian", null); // Not supported
        XMLtoJJazzChordMap.put("French", null); // Not supported
        XMLtoJJazzChordMap.put("German", null); // Not supported

        // Other
        XMLtoJJazzChordMap.put("pedal", null);         // Not supported
        XMLtoJJazzChordMap.put("power", "");           // 1+5
        XMLtoJJazzChordMap.put("Tristan", null);       // Not supported
        XMLtoJJazzChordMap.put("other", null);         // Degrees are all added specifically
        XMLtoJJazzChordMap.put("none", null);          // absence of chord

    }

    // ======================================================================
    // Private classes
    // ======================================================================    
}
