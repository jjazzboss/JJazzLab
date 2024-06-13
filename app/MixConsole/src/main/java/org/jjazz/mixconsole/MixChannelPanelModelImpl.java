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
package org.jjazz.mixconsole;

import java.awt.Color;
import org.jjazz.mixconsole.api.MixConsole;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import static org.jjazz.midi.api.synths.InstrumentFamily.Bass;
import static org.jjazz.midi.api.synths.InstrumentFamily.Brass;
import static org.jjazz.midi.api.synths.InstrumentFamily.Ensemble;
import static org.jjazz.midi.api.synths.InstrumentFamily.Guitar;
import static org.jjazz.midi.api.synths.InstrumentFamily.Organ;
import static org.jjazz.midi.api.synths.InstrumentFamily.Percussive;
import static org.jjazz.midi.api.synths.InstrumentFamily.Piano;
import static org.jjazz.midi.api.synths.InstrumentFamily.Reed;
import static org.jjazz.midi.api.synths.InstrumentFamily.Strings;
import static org.jjazz.midi.api.synths.InstrumentFamily.Synth_Lead;
import static org.jjazz.midi.api.synths.InstrumentFamily.Synth_Pad;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import static org.jjazz.rhythm.api.RhythmVoice.Type.DRUMS;
import static org.jjazz.rhythm.api.RhythmVoice.Type.PERCUSSION;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uiutilities.api.HSLColor;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Model based on a channel/InstrumentMix data belonging to a MidiMix.
 * <p>
 * Listen to InstrumentMix model changes and notify listeners. UI updates are propagated on the InstrumentMix model and possibly to the enclosing MidiMix.
 * <p>
 */
public class MixChannelPanelModelImpl implements MixChannelPanelModel, PropertyChangeListener
{

    @StaticResource(relative = true)
    private final String USER_ICON_PATH = "resources/User-48x48.png";

    private MidiMix midiMix;
    private InstrumentMix insMix;
    private InstrumentSettings insSettings;
    private int channelId;
    private String channelName;
    private String channelNameTooltip;
    private String iconTooltip;
    private ImageIcon icon;
    private Color channelColor = Color.LIGHT_GRAY;
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private RhythmVoice rhythmVoice;

    /**
     * @param mMix    The MidiMix containing all data of our model.
     * @param channel Used to retrieve the InstrumentMix from mMix.
     */
    public MixChannelPanelModelImpl(MidiMix mMix, int channel)
    {
        if (mMix == null || !MidiConst.checkMidiChannel(channel) || mMix.getInstrumentMix(channel) == null)
        {
            throw new IllegalArgumentException("mMix=" + mMix + " channel=" + channel);
        }
        channelId = channel;
        midiMix = mMix;
        midiMix.addPropertyChangeListener(this);
        insMix = mMix.getInstrumentMix(channel);
        insMix.addPropertyChangeListener(this);
        insSettings = insMix.getSettings();
        insSettings.addPropertyChangeListener(this);
        rhythmVoiceChanged(midiMix.getRhythmVoice(channel));
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
        midiMix.removePropertyChangeListener(this);
        insMix.removePropertyChangeListener(this);
        insSettings.removePropertyChangeListener(this);
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
     * If volume was changed using mouse with SHIFT pressed, then we apply the volume delta change to other channels as well, unless one channel reaches min or
     * max volume.
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
                int volume = midiMix.getInstrumentMix(channel).getSettings().getVolume() + delta;
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
                InstrumentSettings is = midiMix.getInstrumentMix(channel).getSettings();
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
    public String getChannelName()
    {
        return channelName;
    }


    @Override
    public String getChannelNameTooltip()
    {
        return channelNameTooltip;
    }

    @Override
    public String getIconTooltip()
    {
        return iconTooltip;
    }

    @Override
    public ImageIcon getIcon()
    {
        return icon;
    }

    @Override
    public void setChannelColor(Color c)
    {
        var old = channelColor;
        channelColor = c;
        pcs.firePropertyChange(PROP_CHANNEL_COLOR, old, channelColor);
    }

    @Override
    public Color getChannelColor()
    {
        return channelColor;
    }

    @Override
    public String getCategory()
    {
        return isUserChannel() ? ResUtil.getString(getClass(), "USER") : rhythmVoice.getContainer().getName();
    }

    @Override
    public boolean isUserChannel()
    {
        return rhythmVoice instanceof UserRhythmVoice;
    }


    @Override
    public void addPropertyChangeListener(PropertyChangeListener l
    )
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
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
            switch (e.getPropertyName())
            {
                case InstrumentMix.PROP_INSTRUMENT ->
                    pcs.firePropertyChange(PROP_INSTRUMENT, e.getOldValue(), e.getNewValue());
                case InstrumentMix.PROP_MUTE ->
                    pcs.firePropertyChange(PROP_MUTE, e.getOldValue(), e.getNewValue());
                case InstrumentMix.PROP_SOLO ->
                    pcs.firePropertyChange(PROP_SOLO, e.getOldValue(), e.getNewValue());
                case InstrumentMix.PROP_INSTRUMENT_ENABLED ->
                    pcs.firePropertyChange(PROP_INSTRUMENT_ENABLED, e.getOldValue(), e.getNewValue());
                default ->
                {
                }
            }
        } else if (e.getSource() == insSettings)
        {
            switch (e.getPropertyName())
            {
                case InstrumentSettings.PROPERTY_CHORUS ->
                    pcs.firePropertyChange(PROP_CHORUS, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_REVERB ->
                    pcs.firePropertyChange(PROP_REVERB, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_VOLUME ->
                    pcs.firePropertyChange(PROP_VOLUME, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_PANORAMIC ->
                    pcs.firePropertyChange(PROP_PANORAMIC, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_PANORAMIC_ENABLED ->
                    pcs.firePropertyChange(PROP_PANORAMIC_ENABLED, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_REVERB_ENABLED ->
                    pcs.firePropertyChange(PROP_REVERB_ENABLED, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_CHORUS_ENABLED ->
                    pcs.firePropertyChange(PROP_CHORUS_ENABLED, e.getOldValue(), e.getNewValue());
                case InstrumentSettings.PROPERTY_VOLUME_ENABLED ->
                    pcs.firePropertyChange(PROP_VOLUME_ENABLED, e.getOldValue(), e.getNewValue());
                default ->
                {
                }
            }
        } else if (e.getSource() == midiMix)
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED ->
                {
                    int channel = (int) e.getOldValue();
                    if (channel == this.channelId)
                    {
                        pcs.firePropertyChange(PROP_DRUMS_CHANNEL_REROUTED, e.getOldValue(), e.getNewValue());
                    }
                }
                case MidiMix.PROP_RHYTHM_VOICE ->
                {
                    if (rhythmVoice == e.getOldValue())
                    {
                        rhythmVoiceChanged((RhythmVoice) e.getNewValue());
                    }
                }
                case MidiMix.PROP_RHYTHM_VOICE_CHANNEL ->
                {
                    if (e.getOldValue().equals(channelId))
                    {
                        int old = channelId;
                        channelId = (int) e.getNewValue();
                        pcs.firePropertyChange(PROP_CHANNEL_ID, old, channelId);
                    }
                }
                default ->
                {
                }
            }
        }
    }
    //-----------------------------------------------------------------------
    // Private functions
    //-----------------------------------------------------------------------

    /**
     * Update values depending on the RhythmVoice.
     *
     * @param rv
     */
    private void rhythmVoiceChanged(RhythmVoice rv)
    {
        rhythmVoice = rv;
        channelName = rhythmVoice.getName();
        icon = getIcon(rhythmVoice);
        Rhythm r = rhythmVoice.getContainer();
        channelNameTooltip = isUserChannel() ? rhythmVoice.getName() : r.getName() + " - " + rhythmVoice.getName();
        iconTooltip = getIconTooltip(rhythmVoice);

        pcs.firePropertyChange(PROP_CHANNEL_NAME, null, channelName);
        pcs.firePropertyChange(PROP_CHANNEL_NAME_TOOLTIP, null, channelNameTooltip);
        pcs.firePropertyChange(PROP_ICON, null, icon);
        pcs.firePropertyChange(PROP_ICON_TOOLTIP, null, iconTooltip);
    }

    private String getIconTooltip(RhythmVoice rv)
    {
        StringBuilder sb = new StringBuilder("<html>");

        sb.append(ResUtil.getString(getClass(), "DragToExportTrack"));

        if (!(rv instanceof UserRhythmVoice))
        {
            Instrument prefIns = rv.getPreferredInstrument();
            sb.append(".<br>");
            sb.append(ResUtil.getString(getClass(), "OriginalStyleInstrument", prefIns.getFullName()));

            if (!(prefIns instanceof GM1Instrument))
            {
                DrumKit kit = prefIns.getDrumKit();
                sb.append(" - ");
                sb.append(rv.isDrums() ? "DrumKit type=" + kit.getType().toString() + " keymap= " + kit.getKeyMap().getName()
                        : "GM substitute: " + prefIns.getSubstitute().getPatchName());
            }
        }

        sb.append("</html>");
        return sb.toString();
    }

    private ImageIcon getIcon(RhythmVoice rv)
    {
        assert rv != null;
        ImageIcon res = null;
        
        if (rv instanceof UserRhythmVoice)
        {
            res = new ImageIcon(getClass().getResource(USER_ICON_PATH));
        } else
        {
            res = switch (rv.getType())
            {
                case DRUMS ->
                    new ImageIcon(getClass().getResource("resources/Drums-48x48.png"));
                case PERCUSSION ->
                    new ImageIcon(getClass().getResource("resources/Percu-48x48.png"));
                default ->
                {
                    // VOICE
                    yield switch (rv.getPreferredInstrument().getSubstitute().getFamily())
                    {
                        case Guitar ->
                            new ImageIcon(getClass().getResource("resources/Guitar-48x48.png"));
                        case Piano, Organ, Synth_Lead ->
                            new ImageIcon(getClass().getResource("resources/Keyboard-48x48.png"));
                        case Bass ->
                            new ImageIcon(getClass().getResource("resources/Bass-48x48.png"));
                        case Brass, Reed ->
                            new ImageIcon(getClass().getResource("resources/HornSection-48x48.png"));
                        case Strings, Synth_Pad, Ensemble ->
                            new ImageIcon(getClass().getResource("resources/Strings-48x48.png"));
                        case Percussive ->
                            new ImageIcon(getClass().getResource("resources/Percu-48x48.png"));
                        default ->
                            new ImageIcon(getClass().getResource("resources/Notes-48x48.png")); // Ethnic, Sound_Effects, Synth_Effects, Pipe, Chromatic_Percussion:
                    };
                }
            };
        }

        return res;
    }


}
