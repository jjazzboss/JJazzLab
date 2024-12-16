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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.Degree;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import static org.jjazz.importers.musicxml.MusicXmlParser.XMLtoJJazzChordMap;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.BaseUtilities;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Parses a MusicXML file, and fires events for <code>MusicXmlParserListener</code> interfaces when tokens are interpreted.
 * <p>
 * The <code>ParserListener</code> does intelligent things with the resulting events, such as create music, draw sheet music, or transform the data.
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

    @StaticResource(relative = true)
    public static final String ZIP_RESOURCE_PATH = "resources/partwise-dtd.zip";

    private static Builder XOM_BUILDER;
    private static File DTD_FILE;
    private Document xomDoc;
    private final CopyOnWriteArrayList<MusicXmlParserListener> parserListeners;
    private int divisionsPerBeat;
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

        // Set up MusicXML default values
        divisionsPerBeat = 1;
        curBarIndex = 0;
        curDivisionInBar = 0;
        timeSignature = TimeSignature.FOUR_FOUR;
    }

    public void parse(String musicXmlString) throws ValidityException, ParsingException, IOException, SAXException, ParserConfigurationException
    {
        // URI is null when parsing a String as it's coming from somewhere else
        parse(getBuilder().build(musicXmlString, (String) null));
    }

    public void parse(File inputFile) throws ValidityException, ParsingException, IOException, ParserConfigurationException, SAXException
    {
        parse(getBuilder().build(inputFile));
    }

    public void parse(FileInputStream inputStream) throws ValidityException,
            ParsingException, IOException, ParserConfigurationException, SAXException
    {
        parse(getBuilder().build(inputStream));
    }

    public void parse(Reader reader) throws ValidityException, ParsingException, IOException, ParserConfigurationException, SAXException
    {
        parse(getBuilder().build(reader));
    }

    private void parse(Document document)
    {
        xomDoc = document;
        parse();
    }

    /**
     * Parses a MusicXML file and fires events to subscribed <code>ParserListener</code> interfaces.
     * <p>
     * As the file is parsed, events are sent to <code>ParserListener</code> interfaces, which are responsible for doing something interesting with the music
     * data.
     * <p>
     * the input is a XOM Document, which has been built previously
     * <p>
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

        Attribute partId = part.getAttribute("id");     // Some files don't have an id !?
        if (partId == null)
        {
            LOGGER.log(Level.WARNING, "parseHarmonyPartWise() No id found for part={0}", part.getLocalName());
        } else
        {
            LOGGER.log(Level.FINE, "parseHarmonyPartWise() Processing part id={0}", partId.getValue());
        }


        for (Element elMeasure : part.getChildElements("measure"))
        {
            String numberId = elMeasure.getAttribute("number").getValue();
            LOGGER.log(Level.FINE, "parseHarmonyPartWise() processing measure numberId={0} curBarIndex={1}", new Object[]
            {
                numberId, curBarIndex
            });
            curDivisionInBar = 0;
            fireBarLineParsed(numberId, curBarIndex);
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
            this.divisionsPerBeat = getIntValueOrDefault(attributes.getFirstChildElement("divisions"), this.divisionsPerBeat);

            // Time signature
            Element elTime = attributes.getFirstChildElement("time");
            if (elTime != null)
            {
                int upper = Integer.parseInt(elTime.getFirstChildElement("beats").getValue());      // Mandatory
                int lower = Integer.parseInt(elTime.getFirstChildElement("beat-type").getValue());  // Mandatory
                timeSignature = TimeSignature.get(upper, lower);
                if (timeSignature == null)
                {
                    LOGGER.log(Level.WARNING, "parseMusicData() Invalid time signature={0}/{1}. Using 4/4 instead.", new Object[]
                    {
                        upper,
                        lower
                    });
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
                case "harmony" -> parseHarmony(el, curBarIndex, curDivisionInBar);
                case "note" ->
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
                }
                case "backup" ->
                {
                    int duration = Integer.parseInt(el.getFirstChildElement("duration").getValue());
                    curDivisionInBar -= duration;
                }
                case "forward" ->
                {
                    int duration = Integer.parseInt(el.getFirstChildElement("duration").getValue());
                    curDivisionInBar += duration;
                }
                case "direction" ->
                {
                    Element sound = el.getFirstChildElement("sound");
                    if (sound != null)
                    {
                        // sound/dacapo or sound/dalsegno does not directly specify if we should go al coda or al fine (or nothing). However, at least in the iRealPro export files, this 
                        // can be found in direction/direction-type/words ("D.S. al Fine", "D.S. al Coda", "D.C. al Coda", "D.C. al Fine"...). 
                        // So we try to reuse this info when available.
                        String alCodaOrAlFine = "";       // By default no information specified
                        Element words = getFirstGrandChild(el, "direction-type", "words");
                        if (words != null)
                        {
                            String wordsValue = words.getValue().toLowerCase();
                            if (wordsValue.contains("fine"))
                            {
                                alCodaOrAlFine = "alfine";
                            } else if (wordsValue.contains("coda"))
                            {
                                alCodaOrAlFine = "alcoda";
                            }
                        }

                        parseDirectionSound(sound, alCodaOrAlFine);
                    }

                    Element rehearsal = getFirstGrandChild(el, "direction-type", "rehearsal");
                    if (rehearsal != null)
                    {
                        String value = rehearsal.getValue();
                        fireRehearsalParsed(curBarIndex, value);
                    }
                }
                case "barline" ->
                {
                    Element ending = el.getFirstChildElement("ending");
                    if (ending != null)
                    {
                        String strNumbers = ending.getAttributeValue("number");       // examples: "1", "2", "1,2", "1,3,4"
                        var numbers = toList(strNumbers);
                        int type = switch (ending.getAttributeValue("type"))
                        {
                            case "start" ->
                                0;
                            case "stop" ->
                                1;
                            default ->
                                2;  // "discontinue"
                        };
                        fireEndingParsed(curBarIndex, numbers, type);
                    }


                    Element repeat = el.getFirstChildElement("repeat");
                    if (repeat != null)
                    {
                        boolean repeatStart = repeat.getAttributeValue("direction").equals("forward");
                        int times = getIntAttributeOrDefault(repeat, "times", -1);
                        fireRepeatParsed(curBarIndex, repeatStart, times);
                    }
                }
                case "sound" ->
                {
                    String value = el.getAttributeValue("tempo");
                    if (value != null)
                    {
                        fireTempoChanged(Math.round(Float.parseFloat(value)), curBarIndex);
                    }
                }
                default ->
                {
                }
            }
            // sound can be embedded in direction
            // sound can be directly in the measure as well
            // Nothing
            if (curDivisionInBar < 0)
            {
                LOGGER.log(Level.SEVERE, "parseMusicData() invalid value for curDivisionInBar={0}, el={1}. Resetting value to 0", new Object[]
                {
                    curDivisionInBar,
                    el
                });
                curDivisionInBar = 0;
            }
        }
    }


    /**
     * Process a sound element
     *
     * @param sound
     * @param alCodaOrAlFine "" or "alcoda" or "alfine" (only used for dalsegno or dacapo)
     * @throws NumberFormatException
     */
    private void parseDirectionSound(Element sound, String alCodaOrAlFine) throws NumberFormatException
    {
        String value = sound.getAttributeValue("tempo");
        if (value != null)
        {
            fireTempoChanged(Math.round(Float.parseFloat(value)), curBarIndex);
        }

        Element otherPlay = getFirstGrandChild(sound, "play", "other-play");
        if (otherPlay != null)
        {
            value = otherPlay.getValue();
            String type = otherPlay.getAttributeValue("type");
            fireOtherPlayParsed(curBarIndex, value, type);
        }

        value = sound.getAttributeValue("time-only");   // only used by tocoda, dacapo, dalsegno
        List<Integer> timeOnly = value == null ? new ArrayList<>() : toList(value);

        value = sound.getAttributeValue("coda");
        if (value != null)
        {
            fireStructureMarkerParsed(curBarIndex, NavigationMark.CODA, value, timeOnly);
        }

        value = sound.getAttributeValue("dacapo");      // If not null it will be "yes"
        if (value != null)
        {
            if (timeOnly.isEmpty())
            {
                timeOnly = List.of(1);      // default value for dacapo
            }
            NavigationMark nm = switch (alCodaOrAlFine)
            {
                case "alcoda" ->
                    NavigationMark.DACAPO_ALCODA;
                case "alfine" ->
                    NavigationMark.DACAPO_ALFINE;
                default ->
                    NavigationMark.DACAPO;
            };
            fireStructureMarkerParsed(curBarIndex, nm, "coda", timeOnly);
        }

        value = sound.getAttributeValue("tocoda");
        if (value != null)
        {
            if (timeOnly.isEmpty())
            {
                timeOnly = List.of(2);      // default value for tocoda
            }
            fireStructureMarkerParsed(curBarIndex, NavigationMark.TOCODA, value, timeOnly);
        }

        value = sound.getAttributeValue("segno");
        if (value != null)
        {
            fireStructureMarkerParsed(curBarIndex, NavigationMark.SEGNO, value, timeOnly);
        }

        value = sound.getAttributeValue("dalsegno");
        if (value != null)
        {
            if (timeOnly.isEmpty())
            {
                timeOnly = List.of(1);      // default value for dalsegno
            }
            if (value.equals("yes"))
            {
                value = "segno";
            }
            NavigationMark nm = switch (alCodaOrAlFine)
            {
                case "alcoda" ->
                    NavigationMark.DALSEGNO_ALCODA;
                case "alfine" ->
                    NavigationMark.DALSEGNO_ALFINE;
                default ->
                    NavigationMark.DALSEGNO;
            };
            fireStructureMarkerParsed(curBarIndex, nm, value, timeOnly);
        }

        value = sound.getAttributeValue("fine");
        if (value != null)
        {
            fireStructureMarkerParsed(curBarIndex, NavigationMark.FINE, value, timeOnly);
        }
    }


    /**
     * Search the first child element corresponding to elementHierarchy.
     * <p>
     * Example: if elementHierarchy=["direction-type", "rehearsal"], search the first grandchild element "rehearsal" (child of a direction-type)
     *
     * @param element
     * @param elementHierarchy
     * @return Null if could not find the last element of elementHierarchy.
     */
    private Element getFirstGrandChild(Element element, String... elementHierarchy)
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

    private int getIntValueOrDefault(Element element, int defaultValue)
    {
        int res = defaultValue;
        if (element != null)
        {
            try
            {
                res = Integer.parseInt(element.getValue());
            } catch (NumberFormatException ex)
            {
            }
        }
        return res;
    }

    /**
     * Convert an integer list string ("2,4,5") to a list of Integer.
     * <p>
     * Invalid numbers are ignored.
     *
     * @param intList
     * @return
     */
    private List<Integer> toList(String intList)
    {
        List<Integer> res = new ArrayList<>();
        for (var str : intList.split("\\s*,\\s*"))
        {
            try
            {
                res.add(Integer.parseInt(str));
            } catch (NumberFormatException ex)
            {
                LOGGER.log(Level.WARNING, "toList() Invalid integer value ex={0} bar={1}", new Object[]
                {
                    ex.getMessage(),
                    curBarIndex
                });
            }
        }
        return res;
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


    /**
     * Parse the Harmony element.
     *
     * @param elHarmony
     * @param barIndex
     * @param divisionPosInBar
     */
    private void parseHarmony(Element elHarmony, int barIndex, int divisionPosInBar)
    {
        if (barIndex < 0 || divisionPosInBar < 0)
        {
            throw new IllegalArgumentException("harmony=" + elHarmony + " barIndex=" + barIndex + " divisionPosInBar=" + divisionPosInBar);
        }
        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();

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
            int offset = Math.round(Float.parseFloat(elOffset.getValue()));
            divisionPosInBar += offset;
            if (divisionPosInBar < 0)
            {
                LOGGER.log(Level.SEVERE,
                        "parseHarmony() invalid value for divisionPosInBar={0}, barIndex={1}, elOffset={2}, elHarmony={3}. Resetting value to 0", new Object[]
                        {
                            divisionPosInBar,
                            barIndex, elOffset, elHarmony
                        });
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
        Element chord_kind = elHarmony.getFirstChildElement("kind");    // In rare cases was null!
        if (chord_kind == null)
        {
            LOGGER.log(Level.WARNING, "parseHarmony() No kind value for element harmony={1}. Using major chord instead.", new Object[]
            {
                elHarmony.toString()
            });
        }
        String strKindValue = chord_kind == null ? "" : chord_kind.getValue();
        String strKindText = chord_kind == null ? "" : chord_kind.getAttributeValue("text");   // Optional

        // Get the standard degrees corresponding to chord kind
        List<Degree> degrees = new ArrayList<>();
        if (strKindValue == null || strKindValue.isBlank())
        {
            // Robustness cases - it should never happen but some .xml are malformed
            LOGGER.log(Level.WARNING, "parseHarmony() Invalid empty kind value={0} in element harmony={1}. Using major chord instead.", new Object[]
            {
                strKindValue, elHarmony.toString()
            });
            ChordType ct = ctdb.getChordType("");
            degrees.addAll(ct.getDegrees());
        } else if (strKindValue.equals("none"))
        {
            // Special case: no chord 
            fireChordSymbolParsed("NC", pos);
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
                    }
                }

                // Default
                LOGGER.log(Level.WARNING, "parseHarmony() No chord type found for kind_value={0} in element harmony={1}. Using major chord instead.",
                        new Object[]
                        {
                            strKindValue,
                            elHarmony.toString()
                        });
                strChordType = "";

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
            if (strKindText != null)
            {
                ct = ctdb.getChordType(strKindText);    // Might return null
            }

            if (ct == null)
            {
                ct = ctdb.getChordType(0); // Default if problem
                LOGGER.log(Level.WARNING, "parseHarmony() Can''t parse chord symbol for {0}. Using chord kind value={1} instead.", new Object[]
                {
                    strKindText,
                    ct.getName()
                });
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
                int intValue = getIntValueOrDefault(element_alter, 0);
                if (intValue == -1)
                {
                    sb.append("b");
                } else if (intValue == 1)
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
                int intValue = getIntValueOrDefault(element_alter, 0);
                if (intValue == -1)
                {
                    sb.append("b");
                } else if (intValue == 1)
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
            int intAlter = elAlter == null ? 0 : Integer.parseInt(elAlter.getValue());    // -1, 0, +1
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
                    LOGGER.log(Level.WARNING, "parseDegrees() degree-value={0} not supported. Skipping degree element...", intValue);
                    continue;
            }

            if (degree == null)
            {
                // Example found: 13 degree with alter=+1 !
                LOGGER.log(Level.WARNING, "parseDegrees() degree-value={0}/degree-alter={1} not supported. Skipping degree element...",
                        new Object[]
                        {
                            intValue, intAlter
                        });
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
     * converts beats per minute (BPM) to pulses per minute (PPM) assuming 240 pulses per second In MusicXML, BPM can be fractional, so <code>BPMtoPPM</code>
     * takes a float argument
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

    /**
     * Get the builder configured to use our local dtd file.
     *
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private Builder getBuilder() throws ParserConfigurationException, SAXException
    {
        if (XOM_BUILDER != null)
        {
            return XOM_BUILDER;
        }

        // Some dtd uri referenced in .musicxml files are not available anymore, like "http://www.musicxml.org/dtds/partwise.dtd", which caused Issue #461
        // Plus it's better to have files locally for performance reasons (especially when importing in batch mode).
        // From 4.1.1 we now embed our own musicxml 4.0 dtd local copy. This means we need a custom EntityResolver().
        // (also tried https://xerces.apache.org/xerces2-j/faq-xcatalogs.html but never managed to make it work, too complicated).

        final File dtdFile;
        try
        {
            dtdFile = getDtdFile(); // extract the files in a tmp directory if not already done
        } catch (IOException ex)
        {
            throw new SAXException("Impossible to create the local dtd file. ex=" + ex.getMessage());
        }


        var spf = SAXParserFactory.newDefaultInstance();
        XMLReader reader = spf.newSAXParser().getXMLReader();
        reader.setEntityResolver(new EntityResolver()
        {

            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
            {
                LOGGER.log(Level.FINE, "resolveEntity() publicId={0} systemId={1}", new Object[]
                {
                    publicId, systemId
                });

                InputSource res = null;  // By default, no substitution is proposed, so parser (the caller) will set up an URI connection to the system idenfifier

                if (publicId.matches("-//Recordare//DTD MusicXML [1-9]\\.[0-9] Partwise//EN"))
                {
                    LOGGER.log(Level.FINE, "                ==> handling {0}", publicId);
                    FileInputStream fis = new FileInputStream(dtdFile);        // throws FileNotFoundException (subclass of IOException)
                    res = new InputSource(fis);
                    res.setSystemId(BaseUtilities.toURI(dtdFile).toString());
                }
                return res;
            }
        });


        XOM_BUILDER = new Builder(reader);
        return XOM_BUILDER;
    }

    /**
     * Get the local copy of the partwise.dtd file (+ related files in same directory).
     *
     * @return The existing partwise.dtd file
     * @throws java.io.IOException A problem occured
     */
    private File getDtdFile() throws IOException
    {
        if (DTD_FILE != null && DTD_FILE.exists())
        {
            return DTD_FILE;
        }

        DTD_FILE = null;

        // Create a temporary directory -always the same so it can be reused
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir")).resolve("jl-musicxml");
        if (!Files.isDirectory(tmpDir))
        {
            Files.createDirectory(tmpDir);
        }

        // Extract the dtd files
        List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, tmpDir, true);
        for (var f : res)
        {
            if (f.getName().equals("partwise.dtd"))
            {
                DTD_FILE = f;
                break;
            }
        }

        LOGGER.log(Level.INFO, "getDtdFile() DTD_FILE={0}", DTD_FILE);

        if (DTD_FILE == null)
        {
            throw new IOException("Could not create dtd file");
        }

        return DTD_FILE;
    }

    //
    // Event firing methods
    //
    private void fireBeforeParsingStarts()
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.beforeParsingStarts();
        }
    }

    private void fireAfterParsingFinished()
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.afterParsingFinished();
        }
    }

    private void fireTempoChanged(int tempoBPM, int barIndex)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onTempoChanged(tempoBPM, barIndex);
        }
    }

    private void fireOtherPlayParsed(int barIndex, String value, String type)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onOtherPlayParsed(barIndex, value, type);
        }
    }

    private void fireEndingParsed(int barIndex, List<Integer> numbers, int type)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onEndingParsed(barIndex, numbers, type);
        }
    }

    private void fireRehearsalParsed(int barIndex, String value)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onRehearsalParsed(barIndex, value);
        }
    }

    private void fireStructureMarkerParsed(int barIndex, NavigationMark marker, String value, List<Integer> timesOnly)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onNavigationMarkParsed(barIndex, marker, value, timesOnly);
        }
    }

    private void fireRepeatParsed(int barIndex, boolean repeatStart, int times)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onRepeatParsed(barIndex, repeatStart, times);
        }
    }

    private void fireTimeSignatureParsed(TimeSignature ts, int barIndex)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onTimeSignatureParsed(ts, barIndex);
        }
    }

    private void fireBarLineParsed(String id, int barIndex)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onBarLineParsed(id, barIndex);
        }
    }

    private void fireLyricParsed(String lyric, Position pos)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onLyricParsed(lyric, pos);
        }
    }

    private void fireNoteParsed(Note note, Position pos)
    {
        for (MusicXmlParserListener listener : parserListeners)
        {
            listener.onNoteParsed(note, pos);
        }
    }

    private void fireChordSymbolParsed(String strChord, Position pos)
    {
        for (MusicXmlParserListener listener : parserListeners)
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
        XMLtoJJazzChordMap.put("major-minor", "m7M");       // Not a  mistake!

        // Sixths
        XMLtoJJazzChordMap.put("major-sixth", "6");
        XMLtoJJazzChordMap.put("minor-sixth", "m6");

        // Ninths
        XMLtoJJazzChordMap.put("dominant-ninth", "9");
        XMLtoJJazzChordMap.put("major-ninth", "M9");
        XMLtoJJazzChordMap.put("minor-ninth", "m9");

        // 11ths (usually as the basis for accidental):        
        XMLtoJJazzChordMap.put("dominant-11th", "9sus");
        XMLtoJJazzChordMap.put("major-11th", "M9");
        XMLtoJJazzChordMap.put("minor-11th", "m11");

        // 13ths (usually as the basis for accidental):        
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
