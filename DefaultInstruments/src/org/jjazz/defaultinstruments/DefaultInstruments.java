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
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.GM2Bank;
import org.jjazz.midi.GMSynth;
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
    private static final String NOT_SET = "NotSet";
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
     * Get the default instrument for specified type.
     *
     * @param type
     * @return Can't be null. Use getBuiltinDefaultInstrument() if preference not set or invalid.
     * @see getBuiltinDefaultInstrument()
     */
    public Instrument getInstrument(RvType type)
    {
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
     * Set the default instrument for a specified type.
     *
     * @param type
     * @param ins Can be null.
     */
    public void setInstrument(RvType type, Instrument ins)
    {
        Instrument old = getInstrument(type);
        if (old != ins)
        {
            prefs.put(type.name(), ins == null ? NOT_SET : ins.saveAsString());
            pcs.firePropertyChange(PROP_INSTRUMENT, type, ins);
        }
    }

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
     * @return
     */
    public Instrument getUserInstrument()
    {
        GM1Bank gmb = GMSynth.getInstance().getGM1Bank();
        Instrument defaultIns = gmb.getDefaultInstrument(GM1Bank.Family.Piano);
        String s = prefs.get(PROP_USER_INSTRUMENT, defaultIns.saveAsString());
        return s.equals(NOT_SET) ? null : Instrument.loadFromString(s);
    }

    /**
     * Set the default instrument for User Channels.
     */
    public void setUserInstrument(Instrument ins)
    {
        Instrument old = getUserInstrument();
        if (old != ins)
        {
            prefs.put(PROP_USER_INSTRUMENT, ins == null ? NOT_SET : ins.saveAsString());
            pcs.firePropertyChange(PROP_USER_INSTRUMENT, old, ins);
        }
    }

    /**
     * The application builtin default instruments. Delegates to the GM1 default instruments except for Drums type where we use
     * GM2 standard kit since GM1 does not define a drums instrument.
     *
     * @param t
     * @return A non-null Instrument.
     */
    static public Instrument getBuiltinDefaultInstrument(RvType t)
    {
        GM1Bank gm1bBank = GMSynth.getInstance().getGM1Bank();
        GM2Bank gm2Bank = GMSynth.getInstance().getGM2Bank();
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
}
