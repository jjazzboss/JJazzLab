/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.midimix.api;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.xstream.api.XStreamInstancesManager;

/**
 * A set of up to 16 InstrumentMixes, 1 per Midi channel with 1 RhythmVoice associated.
 * <p>
 * The object manages the solo functionality between the InstrumentMixes.<p>
 * If MidiMix is modified the corresponding PropertyChangeEvent is fired (e.g. PROP_INSTRUMENT_MUTE), then the PROP_MODIFIED_OR_SAVED_OR_RESET change event is
 * also fired, and possibly PROP_MUSIC_GENERATION.
 * <p>
 */
public interface MidiMix
{

    /**
     * Describe an InstrumentMix change added (oldInsMix is null), removed (newInsMix is null), or replaced for a given RhythmVoice-channel pair.
     *
     * @param channel   Midi channel
     * @param rv        Cannot be null
     * @param oldInsMix Cannot be null if newInsMix is null.
     * @param newInsMix Cannot be null if oldInsMix is null.
     */
    public record InsMixChange(int channel, RhythmVoice rv, InstrumentMix oldInsMix, InstrumentMix newInsMix)
            {

        public InsMixChange
        {
            Preconditions.checkArgument(MidiConst.checkMidiChannel(channel), "channel=%s", channel);
            Preconditions.checkArgument(oldInsMix != null || newInsMix != null, "oldInsMix=%s newInsMix=%s", oldInsMix, newInsMix);
        }
    }
    ;
    public static final String MIX_FILE_EXTENSION = "mix";
    public static final int NB_AVAILABLE_CHANNELS = MidiConst.CHANNEL_MAX - MidiConst.CHANNEL_MIN + 1;
    /**
     * oldValue=channel, newValue=true/false.
     */
    String PROP_CHANNEL_DRUMS_REROUTED = "ChannelDrumsRerouted";
    /**
     * One or more InstrumentMixes were added, replaced or removed.
     * <p>
     * OldValue=List&lt;InsMixChange&gt; (list contains only additions or only replacements or only removals)
     */
    String PROP_CHANNEL_INSTRUMENT_MIXES = "ChannelInstrumentMixes";
    /**
     * A drums instrument has changed with different keymap.
     * <p>
     * oldValue=channel, newValue=old keymap (may be null)
     */
    String PROP_DRUMS_INSTRUMENT_KEYMAP = "DrumsInstrumentKeyMap";
    /**
     * oldValue=InstumentMix, newValue=mute boolean state.
     */
    String PROP_INSTRUMENT_MUTE = "InstrumentMute";
    /**
     * oldValue=InstumentMix, newValue=transposition value.
     */
    String PROP_INSTRUMENT_TRANSPOSITION = "InstrumentTransposition";
    /**
     * oldValue=InstumentMix, newValue=velocity shift value.
     */
    String PROP_INSTRUMENT_VELOCITY_SHIFT = "InstrumentVelocityShift";
    /**
     * This property changes when the MidiMix is modified (false-&gt;true) or saved (true-&gt;false).
     */
    String PROP_MODIFIED_OR_SAVED = "PROP_MIDIMIX_MODIFIED_OR_SAVED";
    /**
     * Fired each time a MidiMix parameter which impacts music generation is modified, like instrument transposition.
     * <p>
     * OldValue=the source PropertyChangeEvent that triggers the musical change<p>
     * Use PROP_MODIFIED_OR_SAVED to get notified of any MidiMix change, including non-musical ones like track mute change, etc.
     */
    String PROP_MUSIC_GENERATION = "MidiMixMusicContent";
    /**
     * A RhythmVoice has replaced another one, e.g. this is used to change the name of a user track.
     * <p>
     * oldValue=old RhythmVoice, newValue=new RhythmVoice
     */
    String PROP_RHYTHM_VOICE = "RhythmVoice";
    /**
     * The channel of a RhythmVoice has been changed by user.
     * <p>
     * oldValue=old channel, newValue=new channel
     */
    String PROP_RHYTHM_VOICE_CHANNEL = "RhythmVoiceChannel";

    static final Logger LOGGER = Logger.getLogger(MidiMix.class.getSimpleName());

    /**
     * Add a user channel.
     * <p>
     * The channel will be associated to a UserRhythmVoice created from name. Fires a PROP_CHANNEL_INSTRUMENT_MIXES change event and an undoable event.
     *
     * @param name
     * @param isDrums
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If no MIDI channel available or a user channel with that name already exists.
     */
    void addUserChannel(String name, boolean isDrums) throws UnsupportedEditException;

    /**
     * Remove a user channel.
     * <p>
     * Fires a PROP_CHANNEL_INSTRUMENT_MIXES change event and an undoable event.
     *
     * @param name The name of the UserRhythmVoice
     */
    void removeUserChannel(String name);

    /**
     * Add channels for all the RhythmVoices of r.
     * <p>
     * Fires a PROP_CHANNEL_INSTRUMENT_MIXES change event and an undoable event.
     *
     * @param r
     * @throws UnsupportedEditException If no enough MIDI channels available
     */
    void addRhythm(Rhythm r) throws UnsupportedEditException;

    /**
     * Remove all the channels of r.
     * <p>
     * Fires a PROP_CHANNEL_INSTRUMENT_MIXES change event and an undoable event.
     *
     * @param r
     */
    void removeRhythm(Rhythm r);

    void addPropertyChangeListener(PropertyChangeListener l);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener l);

    void addUndoableEditListener(UndoableEditListener l);


    /**
     * Get the RhythmVoice corresponding to im.
     *
     * @param im
     * @return null if InstrumentMix not found.
     */
    RhythmVoice getRhythmVoice(InstrumentMix im);

    /**
     * Find the channel corresponding to the specified InstrumentMix.
     *
     * @param im
     * @return -1 if InstrumentMix not found.
     */
    int getChannel(InstrumentMix im);

    /**
     * Get the Midi channel associated to the specified RhythmVoice.
     *
     * @param rvKey If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return -1 if key not found.
     */
    int getChannel(RhythmVoice rvKey);

    /**
     * Get a deep copy of this MidiMix.
     * <p>
     * Mutable internal objects are deeply copied, e.g. InstrumentMixes. Not copied: listeners, isSaveNeeded.
     *
     * @param song The song to be set on the returned instance. If not null, caller is responsible to provide a song consistent with this MidiMix.
     * @return
     * @see #checkConsistency(org.jjazz.song.api.Song, boolean)
     */
    MidiMix getDeepCopy(Song song);

    /**
     * The channels which are rerouted to the GM Drums channel.
     *
     * @return Can be empty.
     * @see #setDrumsReroutedChannel(boolean, int)
     */
    Set<Integer> getDrumsReroutedChannels();

    /**
     * The file where this object is stored.
     *
     * @return Null if not set.
     */
    File getFile();

    /**
     * Get the instrumet mix for the specified channel.
     *
     * @param channel A valid midi channel number
     * @return The InstrumentMix assigned to the specified Midi channel, or null if no InstrumentMix for this channel.
     */
    InstrumentMix getInstrumentMix(int channel);

    /**
     * Get the instrumet mix for the specified RhythmVoice.
     *
     * @param rv If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return The InstrumentMix associated to rv. Null if no InstrumentMix found.
     */
    InstrumentMix getInstrumentMix(RhythmVoice rv);

    /**
     * The non-null instrument mixes ordered by channel.
     *
     * @return Can be an empty list.
     */
    List<InstrumentMix> getInstrumentMixes();

    /**
     * All InstrumentMixes ordered by channel.
     *
     * @return A 16 items list, one instrumentMix per channel (some items can be null)
     */
    List<InstrumentMix> getInstrumentMixesPerChannel();

    /**
     * Get the RhythmVoice for the specified Midi channel.
     * <p>
     *
     * @param channel
     * @return The RhythmVoice key corresponding to specified channel. Can be null.
     */
    RhythmVoice getRhythmVoice(int channel);

    /**
     * Get all the RhythmVoices corresponding to the non-null InstrumentMixes.
     * <p>
     * Returned list includes UserRhythmVoice instances as well. The list does not contain RhythmVoiceDelegate instances.
     *
     * @return
     */
    List<RhythmVoice> getRhythmVoices();

    Song getSong();

    /**
     * The unique rhythm set used in this MidiMix.
     * <p>
     *
     * @return
     */
    Set<Rhythm> getUniqueRhythms();

    /**
     * @return The list of Midi channel numbers for which no InstrumentMix is assigned.
     */
    List<Integer> getUnusedChannels();

    /**
     * Get the list of used channels in this MidiMix.
     *
     * @return The list of Midi channel numbers for which a non-null InstrumentMix is assigned.
     */
    List<Integer> getUsedChannels();

    /**
     * Get the list of used channels for specified rhythm in this MidiMix.
     *
     * @param r If null return all used channels. If r is an AdaptedRhythm, returns the channels from it source rhythm.
     * @return The list of Midi channel numbers for rhythm r and for which a non-null InstrumentMix is assigned.
     */
    List<Integer> getUsedChannels(Rhythm r);

    /**
     * Get the Midi channels used by user tracks.
     *
     * @return
     */
    List<Integer> getUserChannels();

    /**
     * Get the user track RhythmVoice key for the specified name.
     *
     * @param name
     * @return Null if not found
     */
    UserRhythmVoice getUserRhythmVoice(String name);

    /**
     * Return the subset of RhythmVoices which are UserRhythmVoices.
     *
     * @return
     */
    List<UserRhythmVoice> getUserRhythmVoices();

    void removePropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener l);

    void removeUndoableEditListener(UndoableEditListener l);


    /**
     * Find an available channel.
     * <p>
     * Try to keep channels in one section above the drums channel reserved to Drums. If not enough channels extend to channel below the drums channel.
     *
     * @param isDrums If true try to use MidiConst.CHANNEL_DRUMS if it is available.
     * @return -1 if no available channel.
     */
    int findFreeChannel(boolean isDrums);

    /**
     * Save this MidiMix to a file.
     * <p>
     * This will fire a PROP_MODIFIED_OR_SAVED change event (true=&gt;false).
     *
     * @param f
     * @param isCopy Indicate that we save a copy, ie perform the file save but nothing else (eg no PROP_MODIFIED_OR_SAVED state change)
     * @throws java.io.IOException
     */
    void saveToFile(File f, boolean isCopy) throws IOException;

    /**
     * Same as saveToFile() but notify user if problems.
     *
     * @param f
     * @param isCopy
     * @return False if a problem occured
     */
    boolean saveToFileNotify(File f, boolean isCopy);

    /**
     * Send the midi messages to initialize all the instrument mixes.
     * <p>
     * Midi messages are sent to the default JJazzLab Midi OUT device.
     */
    void sendAllMidiMixMessages();

    /**
     * Send the midi messages to set the volume of all instrument mixes.
     * <p>
     * Midi messages are sent to the default JJazzLab Midi OUT device.
     */
    void sendAllMidiVolumeMessages();

    /**
     * Enable or disable the rerouting of channel to the GM Drums channel.
     * <p>
     * If enabled, the related InstrumentMix/Settings will be disabled, and vice versa.
     *
     * @param b
     * @param channel
     * @see #getDrumsReroutedChannels()
     */
    void setDrumsReroutedChannel(boolean b, int channel);

    void setFile(File f);

    /**
     * Assign an InstrumentMix to a midi channel and to a key.
     * <p>
     * Replaces any existing InstrumentMix associated to the midi channel. The solo and "drums rerouted channel" status are reset to off for the channel. <br>
     * Fires a PROP_CHANNEL_INSTRUMENT_MIXES change event and an undoable event.
     *
     * @param channel A valid midi channel number.
     * @param rvKey   Can be null if insMix is also null. If a song is set, must be consistent with its rhythms and user phrases. Cannot be a
     *                RhythmVoiceDelegate.
     * @param insMix  Can be null if rvKey is also null.
     * @throws IllegalArgumentException If insMix is already part of this MidiMix for a different channel, or if rvKey is a RhythmVoiceDelegate.
     */
    void setInstrumentMix(int channel, RhythmVoice rvKey, InstrumentMix insMix);

    /**
     * Change the channel of a RhythmVoice.
     * <p>
     * Fires a PROP_RHYTHM_VOICE_CHANNEL and an undoable event.
     *
     * @param rv         Must be a RhythmVoice used by this MidiMix.
     * @param newChannel Must be a free channel
     */
    void setRhythmVoiceChannel(RhythmVoice rv, int newChannel);

    /**
     * Replace an existing RhythmVoice by a new one.
     * <p>
     * Fires a PROP_RHYTHM_VOICE and an undoable event.
     *
     * @param oldRv Must be a RhythmVoice used by this MidiMix.
     * @param newRv Must not be already used by this MidiMix. Must be the same RhythmVoice.Type than oldRv.
     */
    void setRhythmVoice(RhythmVoice oldRv, RhythmVoice newRv);


    /**
     * Get the song mix File object for a specified song file.
     * <p>
     * SongMix file will be located in the same directory than songFile.
     *
     * @param songFile
     * @return Return a new file identical to songFile except the extension. If songFile is null returns null.
     */
    static public File getSongMixFile(File songFile)
    {
        if (songFile == null)
        {
            return null;
        }
        var res = Utilities.replaceExtension(songFile, MIX_FILE_EXTENSION);
        return res;
    }

    /**
     *
     * @param f
     * @return Null if MidiMix could not be created for some reason.
     * @throws java.io.IOException If problem occured while reading file
     */
    public static MidiMix loadFromFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }
        MidiMix mm = null;

        try (var fis = new FileInputStream(f); Reader r = new BufferedReader(new InputStreamReader(fis, "UTF-8")))
        {
            // UTF8 required to support special/accented chars
            XStream xstream = XStreamInstancesManager.getInstance().getLoadMidiMixInstance();
            
            mm = (MidiMix) xstream.fromXML(r);
            mm.setFile(f);
        } catch (XStreamException e)
        {
            LOGGER.log(Level.WARNING, "loadFromFile() XStreamException e={0}", e.getMessage());   // Important in order to get the details of the XStream error   
            throw new IOException("XStream loading error", e);         // Translate into an IOException to be handled by the Netbeans framework 
        }
        return mm;
    }

    /**
     * Get the expected location of the rhythm mix file.
     * <p>
     * If rhythmFile is defined then .mix file is located in the same directory. Otherwise .mix file is located in the default rhythms directory<br>
     *
     * @param rhythmName
     * @param rhythmFile Can not be null but can be the empty path ("")
     * @return
     * @see Rhythm#getFile()
     * @see RhythmDirsLocator#getDefaultRhythmsDirectory()
     */
    static public File getRhythmMixFile(String rhythmName, File rhythmFile)
    {
        Preconditions.checkNotNull(rhythmName);
        Preconditions.checkNotNull(rhythmFile);
        String mixFileName;
        File dir = rhythmFile.getParentFile();
        if (dir != null)
        {
            // File-based rhythm
            mixFileName = Utilities.replaceExtension(rhythmFile.getName(), MIX_FILE_EXTENSION);
        } else
        {
            dir = RhythmDirsLocator.getDefault().getDefaultRhythmsDirectory();
            var rhythmNameNoSpace = rhythmName.replace(" ", "");
            mixFileName = rhythmNameNoSpace + "." + MIX_FILE_EXTENSION;
        }
        File f = new File(dir, mixFileName);
        return f;
    }
}
