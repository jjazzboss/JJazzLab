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

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import org.jjazz.midi.api.Instrument;

/**
 * Model of a MixChannelPanel + controller.
 * <p>
 * - provide all variables that need to be listened to<br>
 * - provide all methods to handle actions not directly managed by the MixChannelPanel itself.<br>
 */
public interface MixChannelPanelModel
{

    public static final String PROP_PANORAMIC = "PropPanoramic";   //NOI18N 
    public static final String PROP_REVERB = "PropReverb";   //NOI18N 
    public static final String PROP_CHORUS = "PropChorus";   //NOI18N 
    public static final String PROP_PANORAMIC_ENABLED = "PropPanoramicEnabled";   //NOI18N 
    public static final String PROP_REVERB_ENABLED = "PropReverbEnabled";   //NOI18N 
    public static final String PROP_CHORUS_ENABLED = "PropChorusEnabled";   //NOI18N 
    public static final String PROP_VOLUME_ENABLED = "PropVolumeEnabled";   //NOI18N 
    public static final String PROP_INSTRUMENT_ENABLED = "PropInstrumentEnabled";   //NOI18N 
    public static final String PROP_DRUMS_CHANNEL_REROUTED = "PropDrumsChannelRerouted";   //NOI18N 
    public static final String PROP_MUTE = "PropMute";   //NOI18N 
    public static final String PROP_SOLO = "PropSolo";   //NOI18N 
    public static final String PROP_VOLUME = "PropVolume";   //NOI18N 
    public static final String PROP_INSTRUMENT = "PropInstrument";   //NOI18N 
    public static final String PROP_CHANNEL_ID = "PropChannelId";   //NOI18N 

    public void addPropertyChangeListener(PropertyChangeListener l);

    public void removePropertyChangeListener(PropertyChangeListener l);

    public void setPanoramicEnabled(boolean b);

    public boolean isPanoramicEnabled();

    public void setChorusEnabled(boolean b);

    public boolean isChorusEnabled();

    public void setVolumeEnabled(boolean b);

    public boolean isVolumeEnabled();

    public void setInstrumentEnabled(boolean b);

    public boolean isInstrumentEnabled();

    public void setReverbEnabled(boolean b);

    public boolean isReverbEnabled();

    public void setReverb(int value);

    public int getReverb();

    public void setChorus(int value);

    public int getChorus();

    public void setPanoramic(int value);

    public int getPanoramic();

    public boolean isDrumsReroutingEnabled();
    
    public boolean isUserChannel();            

    /**
     * @param oldValue
     * @param newValue
     * @param e The MouseEvent representing the user action that changed the volume. e will be null if change did not result from
     * a mouse drag or wheel event.
     */
    public void setVolume(int oldValue, int newValue, MouseEvent e);

    public int getVolume();

    public void setMute(boolean b);

    public boolean isMute();

    public void setSolo(boolean b);

    public boolean isSolo();

    public int getChannelId();

    public Instrument getInstrument();

    public void cleanup();

}
