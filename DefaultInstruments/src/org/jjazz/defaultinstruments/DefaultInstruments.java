/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.defaultinstruments;

import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midiconverters.DrumKit;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.GM2Bank;
import org.jjazz.midi.StdSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.rhythm.api.RvType;
import org.openide.util.NbPreferences;

/**
 * Store the default instruments preferences, one per RhythmVoice type, and one for the user channel.
 */
public class DefaultInstruments
{

    /**
     * Fired when a default instrument is changed for a RhythmVoice.Type. oldValue=RhythmVoice.Type, newValue=Instrument
     */
    public static final String PROP_INSTRUMENT = "PropDefaultInstrument";
    /**
     * Fired when a transposition is modified. oldValue=RhythmVoice.Type, newValue=transpose.
     */
    public static final String PROP_TRANSPOSE = "PropTransposeInstrument";
    /**
     * Fired when change of the default instrument for user channels. oldValue=old instrument, newValue=new instrument
     */
    public static final String PROP_USER_INSTRUMENT = "PropUserChannelDefaultInstrument";
    /**
     * Fired when transpose of user instrument is changed.
     */
    public static final String PROP_USER_INSTRUMENT_TRANSPOSE = "PropUserChannelDefaultInstrumentTranspose";
    public static final String TRANSPOSE_EXT = "Transpose";
    private static DefaultInstruments INSTANCE;

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(DefaultInstruments.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(DefaultInstruments.class.getSimpleName());

    public static DefaultInstruments getInstance()
    {
        synchronized (DefaultInstruments.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultInstruments();
            }
        }
        return INSTANCE;
    }

    private DefaultInstruments()
    {
        // nothing
    }

    /**
     * Get the default (non drums) instrument for specified type.
     *
     * @param type Must not be Drums or Percussion
     * @return Can't be null. Use getBuiltinDefaultInstrument() if preference not set or invalid.
     */
    public Instrument getInstrument(RvType type)
    {
        if (type.equals(RvType.Drums) || type.equals(RvType.Percussion))
        {
            throw new IllegalArgumentException("type=" + type);
        }
        Instrument defaultIns = getBuiltinDefaultInstrument(type);
        String s = prefs.get(type.name(), defaultIns.saveAsString());
        Instrument ins = Instrument.loadFromString(s);
        if (ins == null)
        {
            ins = defaultIns;
        }
        return ins;
    }

    /**
     * Set the default (non-drums) instrument for the specified type.
     *
     * @param type Can't be Drums or Percussion
     * @param ins  Can't be null
     */
    public void setInstrument(RvType type, Instrument ins)
    {
        if (type == null || ins == null || type.equals(RvType.Drums) || type.equals(RvType.Percussion))
        {
            throw new IllegalArgumentException("type=" + type + " ins=" + ins);
        }
        Instrument old = getInstrument(type);
        if (old != ins)
        {
            prefs.put(type.name(), ins.saveAsString());
            pcs.firePropertyChange(PROP_INSTRUMENT, type, ins);
        }
    }
//
//    /**
//     * Get the default DrumsInstrument for drums or percussion.
//     *
//     * @param rvType Must be Drums or Percussion.
//     * @param dkType
//     * @param dMap
//     * @return Can't be null. By default return the VoidDrumsInstrument instance if no specific default DrumsInstrument could
//     *         be found.
//     */
//    public DrumsInstrument getDrumsInstrument(RvType rvType, DrumKitType dkType, DrumKitKeyMap dMap)
//    {
//        if (rvType == null || (!rvType.equals(RvType.Drums) && !rvType.equals(RvType.Percussion)) || dkType == null || dMap == null)
//        {
//            throw new IllegalArgumentException("rvType=" + rvType + " dkType=" + dkType + " dMap=" + dMap);
//        }
//        DrumsInstrument ins = null;
//        String s = prefs.get(getDrumsPreferenceKey(rvType, dkType, dMap), null);
//        if (s != null)
//        {
//            ins = (DrumsInstrument) Instrument.loadFromString(s);
//        }
//        if (ins == null)
//        {
//            ins = VoidDrumsInstrument.getInstance();
//        }
//        return ins;
//    }

    /**
     * Set the default drums instrument for the specified type.
     *
     * @param rvType Must be Drums or Percussion.
     * @param dkType
     * @param dMap
     * @param ins    Can't be null.
     */
//    public void setDrumsInstrument(RvType rvType, DrumKitType dkType, DrumKitKeyMap dMap, DrumsInstrument ins)
//    {
//        if (rvType == null || ins == null || (!rvType.equals(RvType.Drums) && !rvType.equals(RvType.Percussion)) || dkType == null || dMap == null)
//        {
//            throw new IllegalArgumentException("rvType=" + rvType + " dkType=" + dkType + " dMap=" + dMap + " ins=" + ins);
//        }
//        DrumsInstrument old = getDrumsInstrument(rvType, dkType, dMap);
//        if (old != ins)
//        {
//            prefs.put(getDrumsPreferenceKey(rvType, dkType, dMap), ins.saveAsString());
//            pcs.firePropertyChange(PROP_INSTRUMENT, rvType, ins);
//        }
//    }

    /**
     * The transpose for the instrument associated to the specified rhythmvoice type.
     *
     * @param type
     * @return
     */
    public int getTranspose(RvType type)
    {
        String prop = type.name() + TRANSPOSE_EXT;
        int t = prefs.getInt(prop, 0);
        return t;
    }

    public void setTranspose(RvType type, int t)
    {
        if (t < -48 || t > 48)
        {
            throw new IllegalArgumentException("type=" + type + " t=" + t);
        }
        int old = getTranspose(type);
        if (old != t)
        {
            String prop = type.name() + TRANSPOSE_EXT;
            prefs.putInt(prop, t);
            pcs.firePropertyChange(PROP_TRANSPOSE, type, t);
        }
    }

    public int getUserInstrumentTranspose()
    {
        int t = prefs.getInt(PROP_USER_INSTRUMENT_TRANSPOSE, 0);
        return t;
    }

    public void setUserInstrumentTranspose(int t)
    {
        if (t < -48 || t > 48)
        {
            throw new IllegalArgumentException("t=" + t);
        }
        int old = getUserInstrumentTranspose();
        if (old != t)
        {
            prefs.putInt(PROP_USER_INSTRUMENT_TRANSPOSE, t);
            pcs.firePropertyChange(PROP_USER_INSTRUMENT_TRANSPOSE, old, t);
        }
    }

    /**
     * Get the default instrument for User Channels.
     *
     * @return Can't be null.
     */
    public Instrument getUserInstrument()
    {
        Instrument defaultIns = getBuiltinDefaultInstrument(RvType.Keyboard);
        String s = prefs.get(PROP_USER_INSTRUMENT, defaultIns.saveAsString());
        Instrument ins = Instrument.loadFromString(s);
        if (ins == null)
        {
            ins = defaultIns;
        }
        return ins;
    }

    /**
     * Set the default instrument for User Channels.
     *
     * @param ins Can't be null.
     */
    public void setUserInstrument(Instrument ins)
    {
        if (ins == null)
        {
            throw new NullPointerException("ins");
        }
        Instrument old = getUserInstrument();
        if (old != ins)
        {
            prefs.put(PROP_USER_INSTRUMENT, ins.saveAsString());
            pcs.firePropertyChange(PROP_USER_INSTRUMENT, old, ins);
        }
    }

    /**
     * The application builtin default instruments.
     * <p>
     * Delegates to the GM1 default instruments, except for Drums where the VoidInstrument instance is returned.
     *
     * @param t
     * @return A non-null Instrument.
     */
    static public Instrument getBuiltinDefaultInstrument(RvType t)
    {
        GM1Bank gm1bBank = StdSynth.getInstance().getGM1Bank();
        GM2Bank gm2Bank = StdSynth.getInstance().getGM2Bank();
        Instrument ins;
        switch (t)
        {
            case Drums:
            case Percussion:
                // GM1 does not define a drums instrument
                // If applications detects a GM2 compatible synth, the app should define a new default Drums instrument 
                ins = JJazzSynth.getVoidInstrument();
                break;
            case Guitar:
                ins = gm1bBank.getDefaultInstrument(GM1Bank.Family.Guitar);
                break;
            case Keyboard:
                ins = gm1bBank.getDefaultInstrument(GM1Bank.Family.Piano);
                break;
            case Bass:
                ins = gm1bBank.getDefaultInstrument(GM1Bank.Family.Bass);
                break;
            case Horn_Section:
                ins = gm1bBank.getDefaultInstrument(GM1Bank.Family.Brass);
                break;
            case Pad:
                ins = gm1bBank.getDefaultInstrument(GM1Bank.Family.Synth_Pad);
                break;
            default: // Accompaniment
                ins = gm1bBank.getDefaultInstrument(GM1Bank.Family.Piano);
        }
        return ins;
    }

    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------
    private String getDrumsPreferenceKey(RvType rvType, DrumKit kit)
    {
        return rvType.name() + "-" + kit.getType().name() + "-" + kit.getKeyMap().getName();
    }
}
