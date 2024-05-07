/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.synths.InstrumentFamily;

/**
 * The table can associate an Instrument from a MidiSynth to each of the GM1 instruments + the special DRUMS/PERCUSSION static
 * instances.
 * <p>
 * Association can also be done at the GM1Bank.Family level.
 */
public class GMRemapTable implements Serializable
{

    /**
     * Specific exception for this object.
     */
    public class InvalidMappingException extends Exception
    {

        public InvalidMappingException(String msg)
        {
            super(msg);
        }
    }

    /**
     * Special instances for the Drums/Percussion, since GM1 does not define them.
     */
    public static final Instrument DRUMS_INSTRUMENT = new Instrument(0, "Drums");
    public static final Instrument PERCUSSION_INSTRUMENT = new Instrument(1, "Percussion");

    /**
     * oldValue=Family, newValue=Instrument
     */
    public static final String PROP_FAMILY = "Family";    
    /**
     * oldValue=GM1Instrument or the special DRUMS/PERCUSSION static instances, newValue=Instrument
     */
    public static final String PROP_INSTRUMENT = "Instrument";    
    private HashMap<Instrument, Instrument> mapInstruments = new HashMap<>();
    private HashMap<InstrumentFamily, Instrument> mapFamilyInstruments = new HashMap<>();
    private final MidiSynth midiSynth;
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(GMRemapTable.class.getSimpleName());

    /**
     * Create an instance to remap GM instruments to instruments from the specified MidiSynth.
     * <p>
     * @param midiSynth
     */
    public GMRemapTable(MidiSynth midiSynth)
    {
        Preconditions.checkNotNull(midiSynth);
        this.midiSynth = midiSynth;
    }


    /**
     * Set the mappings from another GMRemapTable which must share the same MidiSynth.
     *
     * @param rt Must share the same MidiSynth that this instance.
     */
    public void set(GMRemapTable rt)
    {
        Preconditions.checkArgument(rt.midiSynth == midiSynth);
        mapInstruments = new HashMap<>(rt.mapInstruments);
        mapFamilyInstruments = new HashMap<>(rt.mapFamilyInstruments);
    }

    /**
     * The associated MidiSynth.
     *
     * @return
     */
    public MidiSynth getMidiSynth()
    {
        return midiSynth;
    }


    /**
     * The map which associates a GM instrument (+ the special drums instances) to an instrument from the associated MidiSynth.
     *
     * @return
     */
    public HashMap<Instrument, Instrument> getInstrumentMap()
    {
        return new HashMap<>(mapInstruments);
    }

    /**
     * The map which associates a GM family to an instrument from the associated MidiSynth.
     *
     * @return
     */
    public HashMap<InstrumentFamily, Instrument> getFamilyInstrumentMap()
    {
        return new HashMap<>(mapFamilyInstruments);
    }

    /**
     * Remove all mappings.
     */
    public void clear()
    {
        for (Instrument ins : getInstrumentMap().keySet())
        {
            setInstrumentNoException(ins, null, true);
        }
    }

    /**
     * Check that the specified remapped Instrument is valid.
     *
     * @param remappedIns
     * @throws IllegalArgumentException If invalid value
     */
    static public void checkRemappedInstrument(Instrument remappedIns)
    {
        if (remappedIns == null || remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT && !(remappedIns instanceof GM1Instrument))
        {
            throw new IllegalArgumentException("remappedIns");   
        }
    }

    /**
     * Get the mapped instrument for remappedIns.
     *
     * @param remappedIns Must be a GM1Instrument or the special DRUMS/PERCUSSION static instances.
     * @return Null if no mapping defined for remappedIns. Returned instrument belongs to the associated MidiSynth.
     */
    public Instrument getInstrument(Instrument remappedIns)
    {
        checkRemappedInstrument(remappedIns);
        return mapInstruments.get(remappedIns);
    }

    /**
     * Set the mapped instrument for remappedIns.
     *
     * @param remappedIns        Must be a GM1Instrument or the special DRUMS/PERCUSSION static instances.
     * @param ins                Can be null. If remappedIns is one of the special DRUMS/PERCUSSION instances, ins must be a
     *                           Drums/Perc instrument with a GM compatible DrumsKit.KeyMap. ins must belong to the associated
     *                           MidiSynth. If not null must be an instrument from the associated MidiSynth.
     * @param useAsFamilyDefault If true ins will be also the default instrument for the remappedIns's family. Not used if
     *                           remappedIns is one of the special DRUMS/PERCUSSION instances.
     * @throws InvalidMappingException If arguments are invalid. The exception error message can be used for user notification.
     */
    public void setInstrument(Instrument remappedIns, Instrument ins, boolean useAsFamilyDefault) throws InvalidMappingException
    {
        checkRemappedInstrument(remappedIns);
        if (ins == remappedIns)
        {
            throw new InvalidMappingException("Invalid instrument: " + ins.getFullName() + " is not different from the mapped instrument.");
        }
        if (remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT && !(remappedIns instanceof GM1Instrument))
        {
            throw new InvalidMappingException("Invalid remapped instrument: " + remappedIns.getFullName() + " is not GM1Instrument.");
        }
        if (ins != null && !ins.isDrumKit() && (remappedIns == DRUMS_INSTRUMENT || remappedIns == PERCUSSION_INSTRUMENT))
        {
            throw new InvalidMappingException("Invalid instrument: " + ins.getFullName() + " is not a Drums/Percussion instrument.");
        }
        if (ins != null && ins.isDrumKit() && !ins.getDrumKit().getKeyMap().isContaining(KeyMapGM.getInstance()))
        {
            throw new InvalidMappingException("Invalid instrument: " + ins.getFullName() + " drum kit keymap (" + ins.getDrumKit().getKeyMap().getName() + ") is not GM-compatible.");
        }
        if (ins != null && !midiSynth.contains(ins))
        {
            throw new IllegalArgumentException("Invalid instrument: " + ins.getFullName() + " does not belong to associated midiSynth=" + midiSynth);
        }
        setInstrumentNoException(remappedIns, ins, useAsFamilyDefault);
    }

    /**
     * Get the mapped instrument for the specified instrument family.
     *
     * @param family
     * @return Null if no mapping defined for this family.
     */
    public Instrument getInstrument(InstrumentFamily family)
    {
        if (family == null)
        {
            throw new NullPointerException("family");   
        }
        return mapFamilyInstruments.get(family);
    }

    public String saveAsString()
    {
        var joiner = new StringJoiner("&_&", "[", "]");

        for (var insKey : mapInstruments.keySet())
        {
            var ins = mapInstruments.get(insKey);
            if (ins == null)
            {
                continue;       // No need to save
            }
            
            String prefix;
            if (insKey == DRUMS_INSTRUMENT)
            {
                // Mapped instrument is our special Drums instrument
                prefix = "@DRUMS@";
            } else if (insKey == PERCUSSION_INSTRUMENT)
            {
                // Mapped instrument is our special Percussion instrument
                prefix = "@PERC@";
            } else
            {
                // Mapped instrument is a standard GM instrument
                prefix = insKey.saveAsString();
                if ((insKey instanceof GM1Instrument insKeyGM) && mapFamilyInstruments.get(insKeyGM.getFamily()) == ins)
                {
                    // This instrument is also the default for the family
                    prefix = "@F" + prefix;
                }
            }
            joiner.add(prefix + "!!!" + ins.saveAsString());
        }

        return joiner.toString();
    }

    static public GMRemapTable loadFromString(MidiSynth midiSynth, String s) throws IOException
    {
        GMRemapTable res = new GMRemapTable(midiSynth);
        String msg = null;


        s = s.trim();
        if (!s.startsWith("[") || !s.endsWith("]"))
        {
            msg = "Invalid string s=" + s;
        }


        if (msg == null)
        {
            s = s.substring(1, s.length() - 1);
            String[] strs = s.split("&_&");

            try
            {
                for (String str : strs)
                {
                    str = str.trim();
                    if (str.isBlank())
                    {                        
                        continue;  // Empty table "[]"
                    }
                    else if (str.startsWith("@DRUMS@!!!"))
                    {
                        // Mapped instrument is special drums instrument
                        String strIns = str.substring(10).trim();
                        var ins = Instrument.loadFromString(strIns);         // throws InvalidMappingException
                        if (ins == null)
                        {
                            msg = "Instrument string value not found=" + strIns;
                            break;
                        }
                        res.setInstrument(DRUMS_INSTRUMENT, ins, false);    // throws InvalidMappingException

                    } else if (str.startsWith("@PERC@!!!"))
                    {
                        // Mapped instrument is special percussion instrument                        
                        String strIns = str.substring(9).trim();
                        var ins = Instrument.loadFromString(strIns);
                        if (ins == null)
                        {
                            msg = "Instrument string value not found=" + strIns;
                            break;
                        }
                        res.setInstrument(PERCUSSION_INSTRUMENT, ins, false);        // throws InvalidMappingException

                    } else
                    {
                        // GM Program Change                        
                        boolean isFamilyDefault = false;
                        if (str.startsWith("@F"))
                        {
                            isFamilyDefault = true;
                            str = str.substring(2);
                        }

                        String[] strs2 = str.split("!!!");
                        if (strs2.length != 2)
                        {
                            msg = "Invalid map-instrument string value=" + str;
                            break;
                        }


                        String strMappedIns = strs2[0];
                        var mappedIns = Instrument.loadFromString(strMappedIns);
                        if (mappedIns == null)
                        {
                            msg = "Instrument string value not found=" + strMappedIns;
                            break;
                        }


                        String strIns = strs2[1];
                        var ins = Instrument.loadFromString(strIns);
                        if (ins == null)
                        {
                            msg = "Instrument string value not found=" + strIns;
                            break;
                        }


                        res.setInstrument(mappedIns, ins, isFamilyDefault);    // throws InvalidMappingException
                    }
                }
            } catch (InvalidMappingException ex)
            {
                msg = ex.getMessage();
            }
        }

        if (msg != null)
        {
            // LOGGER.warning("loadFromString() " + msg + ". s=" + s);
            throw new IOException(msg);
        }


        return res;
    }


    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }


    // --------------------------------------------------------------------- 
    // Private methods
    // --------------------------------------------------------------------- 
    private void setInstrumentNoException(Instrument remappedIns, Instrument ins, boolean useAsFamilyDefault)
    {
        Instrument oldIns = mapInstruments.put(remappedIns, ins);
        if (!Objects.equals(ins, oldIns))
        {
            pcs.firePropertyChange(PROP_INSTRUMENT, remappedIns, ins);
        }
        if (useAsFamilyDefault && (remappedIns != DRUMS_INSTRUMENT && remappedIns != PERCUSSION_INSTRUMENT))
        {
            InstrumentFamily family = ((GM1Instrument) remappedIns).getFamily();
            oldIns = mapFamilyInstruments.put(family, ins);
            if (!Objects.equals(ins, oldIns))
            {
                pcs.firePropertyChange(PROP_FAMILY, family, ins);
            }
        }
    }


}
