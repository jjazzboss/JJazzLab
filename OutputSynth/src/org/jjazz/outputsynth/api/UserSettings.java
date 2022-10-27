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
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiUtilities;

/**
 * The user-dependent settings associated to an OutputSynth: GM remap table, latency, user track default voice, etc.
 */
public class UserSettings
{

    public static final String PROP_USERINSTRUMENT = "userInstrument";
    public static final String PROP_AUDIOLATENCY = "AudioLatency";
    public static final String PROP_SENDMODEONUPONSTARTUP = "sendModeOnUponStartup";

    public enum SendModeOnUponStartup
    {
        OFF, GM, GM2, XG, GS;
    }

    private final OutputSynth outputSynth;
    private SendModeOnUponStartup sendModeOnUponPlay;
    protected GMRemapTable remapTable;
    private int audioLatency;
    private Instrument userInstrument;

    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);


    /**
     * Create an instance with sendModeOnUponPlay=OFF, audioLatency=50, userInstrument = 1st instrument of the outputSynth, empty GMremapTable
     *
     * @param outputSynth
     */
    public UserSettings(OutputSynth outputSynth)
    {
        Preconditions.checkNotNull(outputSynth);
        this.remapTable = new GMRemapTable();
        this.outputSynth = outputSynth;
        this.sendModeOnUponPlay = SendModeOnUponStartup.OFF;
        this.userInstrument = outputSynth.getMidiSynths().get(0).getInstruments().get(0);
        this.audioLatency = 50;
    }


    /**
     * Get the value of AudioLatency
     *
     * @return the value of AudioLatency
     */
    public int getAudioLatency()
    {
        return audioLatency;
    }

    /**
     * Set the value of AudioLatency
     *
     * @param audioLatency new value of AudioLatency
     */
    public void setAudioLatency(int audioLatency)
    {
        int oldAudioLatency = this.audioLatency;
        this.audioLatency = audioLatency;
        propertyChangeSupport.firePropertyChange(PROP_AUDIOLATENCY, oldAudioLatency, audioLatency);
    }


    /**
     * Get the value of userInstrument
     *
     * @return Can't be null
     */
    public Instrument getUserInstrument()
    {
        return userInstrument;
    }

    /**
     * Set the value of userInstrument.
     *
     * @param ins Must be an instrument contained in the related OutputSynth.
     */
    public void setUserInstrument(Instrument ins)
    {
        if (ins == null || ins.getBank() == null || ins.getBank().getMidiSynth() == null)
        {
            throw new IllegalArgumentException("ins=" + ins.toLongString());   //NOI18N
        }
        if (!outputSynth.getMidiSynths().contains(ins.getBank().getMidiSynth()))
        {
            throw new IllegalArgumentException("ins=" + ins.toLongString());   //NOI18N
        }

        Instrument oldUserInstrument = this.userInstrument;
        this.userInstrument = ins;
        propertyChangeSupport.firePropertyChange(PROP_USERINSTRUMENT, oldUserInstrument, ins);

    }

    public GMRemapTable getRemapTable()
    {
        return remapTable;
    }

    /**
     * Get the value of getOutputSynth.
     *
     * @return the value of getOutputSynth
     */
    public OutputSynth getOutputSynth()
    {
        return outputSynth;
    }

    /**
     * Get the value of sendModeOnUponStartup
     *
     * @return the value of sendModeOnUponStartup
     */
    public SendModeOnUponStartup getSendModeOnUponPlay()
    {
        return sendModeOnUponPlay;
    }

    /**
     * Set the value of sendModeOnUponStartup
     *
     * @param sendModeOnUponPlay new value of sendModeOnUponStartup
     */
    public void setSendModeOnUponPlay(SendModeOnUponStartup sendModeOnUponPlay)
    {
        SendModeOnUponStartup oldSendModeOnUponStartup = this.sendModeOnUponPlay;
        this.sendModeOnUponPlay = sendModeOnUponPlay;
        propertyChangeSupport.firePropertyChange(PROP_SENDMODEONUPONSTARTUP, oldSendModeOnUponStartup, sendModeOnUponPlay);
    }


    /**
     * Send the Sysex messages corresponding to getSendModeOnUponPlay().
     */
    public void sendModeOnUponPlaySysexMessages()
    {
        switch (sendModeOnUponPlay)
        {
            case GM:
                MidiUtilities.sendSysExMessage(MidiUtilities.getGmModeOnSysExMessage());
                break;
            case GM2:
                MidiUtilities.sendSysExMessage(MidiUtilities.getGm2ModeOnSysExMessage());
                break;
            case XG:
                MidiUtilities.sendSysExMessage(MidiUtilities.getXgModeOnSysExMessage());
                break;
            case GS:
                MidiUtilities.sendSysExMessage(MidiUtilities.getGsModeOnSysExMessage());
                break;
            case OFF:
                break;
            default:
                throw new IllegalStateException("sendModeOnUponPlay=" + sendModeOnUponPlay);   //NOI18N
        }
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }


}
