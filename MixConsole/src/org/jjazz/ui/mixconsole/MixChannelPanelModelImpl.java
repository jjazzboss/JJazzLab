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
package org.jjazz.ui.mixconsole;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midi.MidiConst;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.UserChannelRvKey;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;

/**
 * Model based on a channel/InstrumentMix data belonging to a MidiMix.
 * <p>
 * Listen to InstrumentMix model changes and notify listeners. UI updates are propagated on the InstrumentMix model and possibly to the
 * enclosing MidiMix.
 *
 */
public class MixChannelPanelModelImpl implements MixChannelPanelModel, PropertyChangeListener
{

    private MidiMix midiMix;
    private InstrumentMix insMix;
    private InstrumentSettings insSettings;
    private int channelId;
    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    /**
     * @param mMix    The MidiMix containing all data of our model.
     * @param channel Used to retrieve the InstrumentMix from mMix.
     */
    public MixChannelPanelModelImpl(MidiMix mMix, int channel)
    {
        if (mMix == null || !MidiConst.checkMidiChannel(channel) || mMix.getInstrumentMixFromChannel(channel) == null)
        {
            throw new IllegalArgumentException("mMix=" + mMix + " channel=" + channel);
        }
        channelId = channel;
        midiMix = mMix;
        midiMix.addPropertyListener(this);
        insMix = mMix.getInstrumentMixFromChannel(channel);
        insMix.addPropertyChangeListener(this);
        insSettings = insMix.getSettings();
        insSettings.addPropertyChangeListener(this);
    }

    /**
     * @return the channel
     */
    @Override
    public int getChannelId()
    {
        return channelId;
    }

    public InstrumentMix getInstrumentMix()
    {
        return insMix;
    }

    @Override
    public void cleanup()
    {
        insMix.removePropertyChangeListener(this);
        insSettings.removePropertyChangeListener(this);
    }

    @Override
    public boolean isUserChannel()
    {
        return midiMix.getRhythmVoice(channelId) instanceof UserChannelRvKey;
    }

    @Override
    public boolean isDrumsReroutingEnabled()
    {
        return midiMix.getDrumsReroutedChannels().contains(channelId);
    }

    @Override
    public void setChorus(int value)
    {
        insSettings.setChorus(value);
    }

    @Override
    public int getChorus()
    {
        return insSettings.getChorus();
    }

    @Override
    public void setReverb(int value)
    {
        insSettings.setReverb(value);
    }

    @Override
    public int getReverb()
    {
        return insSettings.getReverb();
    }

    @Override
    public void setPanoramic(int value)
    {
        insSettings.setPanoramic(value);
    }

    @Override
    public int getPanoramic()
    {
        return insSettings.getPanoramic();
    }

    /**
     * Set volume of the channel.
     * <p>
     * If volume was changed using mouse with SHIFT pressed, then we apply the volume delta change to other channels as well, unless one
     * channel reaches min or max volume.
     *
     * @param oldValue
     * @param newValue
     * @param e
     */
    @Override
    public void setVolume(int oldValue, int newValue, MouseEvent e)
    {
        insSettings.setVolume(newValue);
        if (e != null)
        {
            // It was a Mouse wheel or mouse Drag
            boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
            if (!shift || oldValue == newValue)
            {
                return;
            }
            // Shift was pressed
            // Let's modify the volume of the other visible MidiMix channels proportionnaly
            // until one volume is at the max or min
            MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
            Rhythm visibleRhythm = (mixConsole != null) ? mixConsole.getVisibleRhythm() : null;
            int delta = newValue - oldValue;
            int possibleDelta = delta;
            List<Integer> channelsToUpdate = new ArrayList<>();
            // Check if there is enough room to apply the volume delta to all other channels
            for (Integer channel : midiMix.getUsedChannels())
            {
                Rhythm rChannel = midiMix.getRhythmVoice(channel).getContainer();
                if (channel == channelId || (visibleRhythm != null && visibleRhythm != rChannel))
                {
                    continue;
                }
                channelsToUpdate.add(channel);
                int volume = midiMix.getInstrumentMixFromChannel(channel).getSettings().getVolume() + delta;
                if (volume > 127)
                {
                    // TargetDelta is > 0
                    int newDelta = delta - (volume - 127);
                    possibleDelta = Math.min(possibleDelta, newDelta);
                } else if (volume < 0)
                {
                    // TargetDelta is < 0
                    int newDelta = delta - volume;
                    possibleDelta = Math.max(possibleDelta, newDelta);
                }
            }
            // Now apply the realDelta to other channels
            for (Integer channel : channelsToUpdate)
            {
                InstrumentSettings is = midiMix.getInstrumentMixFromChannel(channel).getSettings();
                int newVolume = is.getVolume() + possibleDelta;
                is.setVolume(newVolume);
            }
        }
    }

    @Override
    public int getVolume()
    {
        return insSettings.getVolume();
    }

    @Override
    public void setVolumeEnabled(boolean value
    )
    {
        insSettings.setVolumeEnabled(value);
    }

    @Override
    public boolean isVolumeEnabled()
    {
        return insSettings.isVolumeEnabled();
    }

    @Override
    public void setInstrumentEnabled(boolean value
    )
    {
        insMix.setInstrumentEnabled(value);
    }

    @Override
    public boolean isInstrumentEnabled()
    {
        return insMix.isInstrumentEnabled();
    }

    @Override
    public void setPanoramicEnabled(boolean value
    )
    {
        insSettings.setPanoramicEnabled(value);
    }

    @Override
    public boolean isPanoramicEnabled()
    {
        return insSettings.isPanoramicEnabled();
    }

    @Override
    public void setChorusEnabled(boolean value
    )
    {
        insSettings.setChorusEnabled(value);
    }

    @Override
    public boolean isChorusEnabled()
    {
        return insSettings.isChorusEnabled();
    }

    @Override
    public void setReverbEnabled(boolean value
    )
    {
        insSettings.setReverbEnabled(value);
    }

    @Override
    public boolean isReverbEnabled()
    {
        return insSettings.isReverbEnabled();
    }

    @Override
    public void setMute(boolean value
    )
    {
        insMix.setMute(value);
    }

    @Override
    public boolean isMute()
    {
        return insMix.isMute();
    }

    @Override
    public void setSolo(boolean value)
    {
        insMix.setSolo(value);
    }

    @Override
    public boolean isSolo()
    {
        return insMix.isSolo();
    }

    @Override
    public Instrument getInstrument()
    {
        return insMix.getInstrument();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l
    )
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l
    )
    {
        pcs.removePropertyChangeListener(l);
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == insMix)
        {
            if (null != e.getPropertyName())
            {
                switch (e.getPropertyName())
                {
                    case InstrumentMix.PROP_INSTRUMENT:
                        this.firePropertyChange(PROP_INSTRUMENT, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentMix.PROP_MUTE:
                        this.firePropertyChange(PROP_MUTE, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentMix.PROP_SOLO:
                        this.firePropertyChange(PROP_SOLO, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentMix.PROP_INSTRUMENT_ENABLED:
                        this.firePropertyChange(PROP_INSTRUMENT_ENABLED, e.getOldValue(), e.getNewValue());
                        break;
                    default:
                        break;
                }
            }
        } else if (e.getSource() == insSettings)
        {
            if (null != e.getPropertyName())
            {
                switch (e.getPropertyName())
                {
                    case InstrumentSettings.PROPERTY_CHORUS:
                        this.firePropertyChange(PROP_CHORUS, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_REVERB:
                        this.firePropertyChange(PROP_REVERB, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_VOLUME:
                        this.firePropertyChange(PROP_VOLUME, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_PANORAMIC:
                        this.firePropertyChange(PROP_PANORAMIC, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_PANORAMIC_ENABLED:
                        this.firePropertyChange(PROP_PANORAMIC_ENABLED, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_REVERB_ENABLED:
                        this.firePropertyChange(PROP_REVERB_ENABLED, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_CHORUS_ENABLED:
                        this.firePropertyChange(PROP_CHORUS_ENABLED, e.getOldValue(), e.getNewValue());
                        break;
                    case InstrumentSettings.PROPERTY_VOLUME_ENABLED:
                        this.firePropertyChange(PROP_VOLUME_ENABLED, e.getOldValue(), e.getNewValue());
                        break;
                    default:
                        break;
                }
            }
        } else if (e.getSource() == midiMix)
        {
            if (e.getPropertyName() == MidiMix.PROP_CHANNEL_DRUMS_REROUTED)
            {
                int channel = (int) e.getOldValue();
                if (channel == this.channelId)
                {
                    this.firePropertyChange(PROP_DRUMS_CHANNEL_REROUTED, e.getOldValue(), e.getNewValue());
                }
            }
        }
    }
    //-----------------------------------------------------------------------
    // Private functions
    //-----------------------------------------------------------------------

    protected void firePropertyChange(String prop, Object oldValue, Object newValue)
    {
        pcs.firePropertyChange(prop, oldValue, newValue);
    }

}
