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
package org.jjazz.yamjjazz;

import org.jjazz.yamjjazz.rhythm.api.AccType;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.GM2Synth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.XGSynth;
import org.jjazz.midi.spi.MidiSynthManager;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Find a Yamaha specific instrument from the Midi address and AccType found in Yamaha styles.
 * <p>
 */
public class YamahaInstrumentFinder
{

    private static YamahaInstrumentFinder INSTANCE;
    public static final String YAMAHA_REF_SYNTH_NAME = "Tyros5 Synth";    
    @StaticResource(relative = true)    
    private static final String YAMAHA_REF_SYNTH_PATH = "resources/YamahaRefSynth.ins";

    private final MidiSynth yamahaRefSynth;

    private static final Logger LOGGER = Logger.getLogger(YamahaInstrumentFinder.class.getSimpleName());

    public static YamahaInstrumentFinder getInstance()
    {
        synchronized (YamahaInstrumentFinder.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new YamahaInstrumentFinder();
            }
        }
        return INSTANCE;
    }

    private YamahaInstrumentFinder()
    {
        // Add our MidiSynth
        yamahaRefSynth = MidiSynthManager.loadFromResource(getClass(), YAMAHA_REF_SYNTH_PATH);
        MidiSynthManager.getDefault().addMidiSynth(yamahaRefSynth);        
    }

    /**
     * Try to find a Yamaha specific instrument from the specified Midi address and AccType.
     * <p>
     * Search matching instrument in the following order:<br>
     * - in the YamahaRefSynth<br>
     * - in the standard Midi banks<br>
     * - if at.isDrums(), use default standard drums<br>
     * - if !at.isDrums(), create an orphan Instrument with the MidiAddress and set its GM1Substitute to at.defaultGM1Instrument.
     *
     * @param address
     * @param at
     * @param logPrefix A prefix used in the log messages.
     * @return Can't be null. If not a drums/percussion, instrument will have a GM1 substitute instrument defined. Instrument has always a Bank/Synth defined.
     */
    public Instrument findInstrument(MidiAddress address, AccType at, String logPrefix)
    {
        if (address == null || at == null || logPrefix == null)
        {
            throw new IllegalArgumentException("address=" + address + " at=" + at + " logPrefix=" + logPrefix);   //NOI18N
        }

        Instrument ins = null;
        int bankMSB = address.getBankMSB();
        int pc = address.getProgramChange();

        LOGGER.log(Level.FINE, "findInstrument() {0} -- at={1} address={2}", new Object[]
        {
            logPrefix, at, address
        });

        // Get an instrument from our reference synth
        ins = yamahaRefSynth.getInstrument(address);

        LOGGER.log(Level.FINE, "findInstrument() {0}    refMidiSynth.getInstrument(address)={1}", new Object[]
        {
            logPrefix, ins != null ? ins : "null"
        });

        if (ins == null)
        {
            LOGGER.log(Level.FINE, "{0} - instrument not found in Yamaha ref. synth for at={1} address={2}", new Object[]
            {
                logPrefix, at, address
            });
        }

        if (ins != null)
        {
            // Found a YamahaRefSynth instrument : do some additional processing
            if (bankMSB == 0 || bankMSB == 104 || bankMSB == 109)
            {
                // Special case: these specific voice banks are compatible with the GM1 program change
                // Because of this the YamahaRefSynth .ins does not define {{ GM1sub }} extra arguments for instruments of these banks.
                // So we can reset the GM1 substitute instrument.
                GM1Instrument insGM1 = GMSynth.getInstance().getGM1Bank().getInstrument(pc);
                ins.setSubstitute(insGM1);
                LOGGER.log(Level.FINE, "findInstrument() {0}    changing substitute to insGM1={1}", new Object[]
                {
                    logPrefix, insGM1
                });
            } else if (!ins.isDrumKit() && ins.getSubstitute() == null)
            {
                // We found a Yamaha patch but with no substitute, take the default GM1 instrument for AccType
                GM1Instrument insGM1 = at.defaultGM1Instrument;
                LOGGER.log(Level.INFO, "{0} - No substitute GM1 instrument for ins={1}. Using {2} instead", new Object[]
                {
                    logPrefix, ins, insGM1
                });
                ins.setSubstitute(insGM1);
            }
        }

        // No instrument found in YamahaRefSynth, try the GM/GM2/XG banks (not GS, we don't expect Roland GS sounds in a Yamaha style).
        if (ins == null)
        {
            var stdSynths = Arrays.asList(GMSynth.getInstance(), XGSynth.getInstance(), GM2Synth.getInstance());
            for (var synth : stdSynths)
            {
                ins = synth.getInstrument(address);
                if (ins != null)
                {
                    break;
                }
            }

            if (ins != null)
            {
                LOGGER.log(Level.FINE, "{0} - instrument found in standard synth for at={1} address={2}", new Object[]
                {
                    logPrefix, at, address
                });
            }
        }

        // Still no instrument found, try other ways
        if (ins == null)
        {
            if (at.isDrums() || bankMSB == 126 || bankMSB == 127 || bankMSB == 120)
            {
                // It's supposed to be a drums sound, use the default one
                ins = getDefaultDrumsInstrument();
                LOGGER.log(Level.FINE, "findInstrument() {0}    No Yamaha instrument found for address={1} (at={2}). Using {3} instead.", new Object[]
                {
                    logPrefix, address, at, ins.toLongString()
                });
            } else
            {
                // Use a GM1 instrument
                GM1Instrument insGM1 = at.defaultGM1Instrument;     // Can not be null because no drums here
                assert insGM1 != null : "at=" + at;   //NOI18N
                if (bankMSB == 0 || bankMSB == 104 || bankMSB == 109)
                {
                    // We consider those banks as "GM1 compatible", we could get a better substitute
                    insGM1 = GMSynth.getInstance().getGM1Bank().getInstrument(pc);
                }
                ins = insGM1;
                LOGGER.log(Level.WARNING, "findInstrument() {0} - No Yamaha instrument found for address={1} (at={2}). Using {3} instead.", new Object[]
                {
                    logPrefix, address, at, ins.toLongString()
                });
            }
        }

        assert ins != null : "ins" + ins + " address=" + address + " at=" + at + " logPrefix=" + logPrefix;   //NOI18N

        // Robustness: some styles may have voice instruments assigned to drums/perc channels and vice-versa
        // This happens when styles completly change the channel/AccType assignments between StyleParts (not common)
        if (at.isDrums() && !ins.isDrumKit())
        {
            Instrument newIns = getDefaultDrumsInstrument();
            assert newIns != null : "newIns=" + newIns + " ins=" + ins + ", at=" + at;   //NOI18N
            LOGGER.log(Level.WARNING, "{0} - Invalid instrument {1} for at={2} (address={3}). Using {4} instead.", new Object[]
            {
                logPrefix, ins.toLongString(), at, address, newIns.toLongString()
            });
            ins = newIns;
        } else if (!at.isDrums() && ins.isDrumKit())
        {
            Instrument newIns = at.defaultGM1Instrument;
            assert newIns != null : "newIns=" + newIns + " ins=" + ins + ", at=" + at;   //NOI18N
            LOGGER.log(Level.WARNING, "{0} - Invalid instrument {1} for at={2} (address={3}). Using {4} instead.", new Object[]
            {
                logPrefix, ins.toLongString(), at, address, newIns.toLongString()
            });
            ins = newIns;
        }

        assert ins != null && (ins.isDrumKit() || ins.getSubstitute() != null) : "ins" + ins + " address=" + address + " at=" + at + " logPrefix=" + logPrefix;   //NOI18N

        return ins;
    }


    private Instrument getDefaultDrumsInstrument()
    {
        return yamahaRefSynth.getInstrument(new MidiAddress(0, 127, 0, MidiAddress.BankSelectMethod.MSB_LSB));
    }


}
