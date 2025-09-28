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
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ImageIcon;
import org.jjazz.midi.api.Instrument;

/**
 * Model of a MixChannelPanel + controller.
 * <p>
 * - provide all variables that need to be listened to<br>
 * - provide all methods to handle actions not directly managed by the MixChannelPanel itself.<br>
 */
public interface MixChannelPanelModel
{

    static final String PROP_PANORAMIC = "PropPanoramic";    
    static final String PROP_REVERB = "PropReverb";    
    static final String PROP_CHORUS = "PropChorus";    
    static final String PROP_PANORAMIC_ENABLED = "PropPanoramicEnabled";    
    static final String PROP_REVERB_ENABLED = "PropReverbEnabled";    
    static final String PROP_CHORUS_ENABLED = "PropChorusEnabled";    
    static final String PROP_VOLUME_ENABLED = "PropVolumeEnabled";    
    static final String PROP_INSTRUMENT_ENABLED = "PropInstrumentEnabled";    
    static final String PROP_DRUMS_CHANNEL_REROUTED = "PropDrumsChannelRerouted";    
    static final String PROP_MUTE = "PropMute";    
    static final String PROP_SOLO = "PropSolo";    
    static final String PROP_VOLUME = "PropVolume";    
    static final String PROP_INSTRUMENT = "PropInstrument";    
    static final String PROP_CHANNEL_COLOR = "PropChannelColor";    
    static final String PROP_CHANNEL_ID = "PropChannelId";    
    static final String PROP_CHANNEL_NAME = "PropChannelName";    
    static final String PROP_ICON = "PropIcon";    
    static final String PROP_CHANNEL_NAME_TOOLTIP = "PropChannelNameTooltip";    
    static final String PROP_ICON_TOOLTIP = "PropIconTooltip";       

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    void setPanoramicEnabled(boolean b);

    boolean isPanoramicEnabled();

    void setChorusEnabled(boolean b);

    boolean isChorusEnabled();

    void setVolumeEnabled(boolean b);

    boolean isVolumeEnabled();

    void setInstrumentEnabled(boolean b);

    boolean isInstrumentEnabled();

    void setReverbEnabled(boolean b);

    boolean isReverbEnabled();

    void setReverb(int value);

    int getReverb();

    void setChorus(int value);

    int getChorus();

    void setPanoramic(int value);

    int getPanoramic();

    boolean isDrumsReroutingEnabled();
   
    /**
     * Get the channel name.
     * <p>
     *
     * @return
     */
    String getChannelName();    

    String getChannelNameTooltip();
    
    String getCategory();
    
    boolean isUserChannel();

    String getIconTooltip();

    ImageIcon getIcon();

    Color getChannelColor();
    
    void setChannelColor(Color c);

    /**
     * @param oldValue
     * @param newValue
     * @param e        The MouseEvent representing the user action that changed the volume. e will be null if change did not
     *                 result from a mouse drag or wheel event.
     */
    void setVolume(int oldValue, int newValue, MouseEvent e);

    int getVolume();

    void setMute(boolean b);

    boolean isMute();

    void setSolo(boolean b);

    boolean isSolo();

    int getChannelId();

    Instrument getInstrument();

    void cleanup();
}
