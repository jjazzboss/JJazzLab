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

import org.jjazz.yamjjazz.rhythm.api.SInt;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.Style;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.parser.MidiParserListenerAdapter;
import org.jjazz.midi.api.synths.GMSynth;

/**
 * MidiParserListener for the "SInt" Midi section to retrieve InstrumentMixes in a standard Yamaha style file.
 * <p>
 * Retrieve from the SInt section, for each AccType's channel, a standard (GM, GM2, etc.) or a YamahaRefSynth instrument with the
 * volumne/chorus/reverb settings.
 * <p>
 * Note that there can be several source channels for one AccType in a style file. And in theory they can be mapped to any channel
 * using CASM. But here we only take the instrument of the "official" AccType channels (e.g. 10 for drums, 11 for bass..).
 * <p>
 * SInt voices can be GM, XG, Panel (Yamaha keyboard specific voices) and others (see Table 34 of
 * StyleFileDescription_v21.pdf).<br>. We only retrieve: <br>
 * - Control events for volume, chorus send and reverb send. The rest is ignored, e.g. SYSEX commands.
 */
public class MPL_SInt extends MidiParserListenerAdapter
{

    private boolean insideSIntSection = false;
    private AccType currentAccType = null;
    private InstrumentMix currentInsMix = null;
    private int curBankMSB = -1;
    private int curBankLSB = -1; // Not used
    private Style style;
    private List<AccType> styleAccTypes;
    private boolean warningRaised = false;
    private String logName = "";
    private static final Logger LOGGER = Logger.getLogger(MPL_SInt.class.getSimpleName());

    /**
     * The parser will directly update the specified style's SInt data object.
     *
     * @param style
     * @param logName If non null used to provide additional information when logging, typically the filename
     */
    public MPL_SInt(Style style, String logName)
    {
        this.style = style;
        this.styleAccTypes = style.getAllAccTypes();
        if (logName != null)
        {
            this.logName = logName;
        }
    }

    @Override
    public void onMarkerParsed(String marker, float positionInBeats)
    {        
        insideSIntSection = marker.equals("SInt");
    }

    @Override
    public void onChannelChanged(byte b)
    {
        if (!insideSIntSection)
        {
            return;
        }

        AccType t = style.getAccType(b); // t can be null if it's a "secondary" source channel

        if (currentAccType == t)
        {
            // Nothing changed...
            return;
        }

        if (t == null || !styleAccTypes.contains(t))
        {
            // If new AccType is already null, or it is not supported by this rhythm (should not occur, but if broken style...)
            currentAccType = null;
            currentInsMix = null;
        } else
        {
            // There is a new non-null AccType
            currentAccType = t;
            currentInsMix = getInsMix(currentAccType);
        }
    }

    @Override
    public void onControllerEventParsed(byte controller, byte value, float posInBeats)
    {
        if (!insideSIntSection || currentAccType == null)
        {
            return;
        }
        if (value < 0 || value > 127)
        {
            LOGGER.log(Level.FINE, "{0} - onControllerEventParsed() invalid controller value={1} (controller={2})", new Object[]
            {
                logName, value, controller
            });
            return ;
        }
        switch (controller)
        {
            case MidiConst.CTRL_CHG_BANK_SELECT_MSB:
                curBankMSB = value;
                break;
            case MidiConst.CTRL_CHG_BANK_SELECT_LSB:
                curBankLSB = value;
                break;
            case MidiConst.CTRL_CHG_VOLUME_MSB:
                currentInsMix.getSettings().setVolume(value);
                break;
            case MidiConst.CTRL_CHG_PAN_MSB:
                currentInsMix.getSettings().setPanoramic(value);
                break;
            case MidiConst.CTRL_CHG_CHORUS_DEPTH:
                currentInsMix.getSettings().setChorus(value);
                break;
            case MidiConst.CTRL_CHG_REVERB_DEPTH:
                currentInsMix.getSettings().setReverb(value);
                break;
            default:
                break;
        }
    }

    @Override
    public void onInstrumentParsed(byte programChange, float posInBeats)
    {
        if (!insideSIntSection || currentAccType == null)
        {
            return;
        }
        LOGGER.log(Level.FINE, "{0} - onInstrumentParsed() programChange={1} curBankMSB={2}", new Object[]
        {
            logName, programChange, curBankMSB
        });

        Instrument ins;
        SInt sInt = style.getSInt();
        MidiAddress address = new MidiAddress(programChange, curBankMSB, curBankLSB, MidiAddress.BankSelectMethod.MSB_LSB);

        if (sInt.getOriginalMidiAddress(currentAccType) != null)
        {
            // We already have an instrument defined for AccType, keep the first one
            return;
        }

        sInt.setOriginalMidiAddress(currentAccType, address);

        if (curBankMSB == 8)
        {
            // This is mega voices, save the info
            sInt.setExpectingMegaVoice(currentAccType, true);
        }

        // Get Yamaha-compatible instrument corresponding to MidiAddress
        ins = YamahaInstrumentFinder.getInstance().findInstrument(address, currentAccType, logName); // Can't be null

        currentInsMix.setInstrument(ins);
        if (ins.getBank() == null)
        {
             LOGGER.log(Level.WARNING, "{0} - onInstrumentParsed() orphan instrument found ins={1} currentAccType={2}", new Object[]
        {
            logName, ins.toLongString(), currentAccType
        });
        }
        LOGGER.log(Level.FINE, "   ins={0}", ins);
    }

    /**
     * Retrieve the existing InstrumentMix associated to specified AccType, or create a new default one and store it in the style's SInt
     * data.
     * <p>
     * InstrumenMix is already stored in the result map, so the parser just has to update it.
     *
     * @param at
     * @return
     */
    private InstrumentMix getInsMix(AccType at)
    {
        SInt sInt = style.getSInt();
        InstrumentMix insMix = sInt.get(at);
        if (insMix == null)
        {
            insMix = new InstrumentMix(GMSynth.getInstance().getVoidInstrument(), new InstrumentSettings());  
            sInt.set(at, insMix);
        }
        return insMix;
    }
}
