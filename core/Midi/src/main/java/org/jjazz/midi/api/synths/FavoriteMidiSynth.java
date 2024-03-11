/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midi.api.synths;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.utilities.api.ResUtil;

/**
 * A special synth that just mirrors the favorite instruments in a bank.
 * <p>
 * Fire a stateChanged event upon added/removed favorite instrument.
 */
public class FavoriteMidiSynth extends MidiSynth implements PropertyChangeListener
{

    private static FavoriteMidiSynth INSTANCE;
    private final FavoriteBank bank;
    private final ArrayList<ChangeListener> listeners = new ArrayList<>();

    public static FavoriteMidiSynth getInstance()
    {
        synchronized (FavoriteMidiSynth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FavoriteMidiSynth();
            }
        }
        return INSTANCE;
    }

    private FavoriteMidiSynth()
    {
        super(ResUtil.getString(FavoriteMidiSynth.class, "CTL_Favorites"), "JJazz");
        bank = new FavoriteBank(this);
        addBank(bank);
        FavoriteInstruments fi = FavoriteInstruments.getInstance();
        for (Instrument ins : fi.getInstruments())
        {
            bank.addInstrument(ins);
        }
        fi.addPropertyListener(this);
    }

    public void addChangeListener(ChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l");   
        }
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l)
    {
        listeners.remove(l);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        FavoriteInstruments fi = FavoriteInstruments.getInstance();
        if (evt.getSource() == fi)
        {
            if (evt.getPropertyName().equals(FavoriteInstruments.PROP_FAVORITE_INSTRUMENT))
            {
                if (evt.getNewValue() != null && evt.getOldValue() == null)
                {
                    // Favorite added
                    bank.addInstrument((Instrument) evt.getNewValue());
                    fireChanged();
                } else if (evt.getNewValue() == null && evt.getOldValue() != null)
                {
                    // Favorite removed
                    bank.removeInstrument((Instrument) evt.getOldValue());
                    fireChanged();
                }
            }
        }
    }

    private void fireChanged()
    {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners)
        {
            l.stateChanged(e);
        }
    }

    private static class FavoriteBank extends InstrumentBank<Instrument>
    {

        public FavoriteBank(MidiSynth synth)
        {
            super("Favorites", 0, 0);
        }

        /**
         * Overridden to not set the Bank !
         *
         * @param ins
         */
        @Override
        public void addInstrument(Instrument instrument)
        {
            if (instrument == null)
            {
                throw new IllegalArgumentException("instrument=" + instrument);   
            }
            if (!instruments.contains(instrument))
            {
                instruments.add(instrument);
            }
        }

        /**
         * Make the protected method accessible from enclosing class.
         *
         * @param ins
         */
        @Override
        public void removeInstrument(Instrument ins)
        {
            super.removeInstrument(ins);
        }

    }
}
