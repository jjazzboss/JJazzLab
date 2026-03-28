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
 *  or (at your option) any later version. *
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
package org.jjazz.midimix;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midimix.api.MidiMix;
import static org.jjazz.midimix.api.MidiMix.PROP_RHYTHM_VOICE;
import static org.jjazz.midimix.api.MidiMix.PROP_RHYTHM_VOICE_CHANNEL;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import static org.jjazz.rhythm.api.RhythmVoice.Type.DRUMS;
import static org.jjazz.rhythm.api.RhythmVoice.Type.PERCUSSION;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.ExecutionManager;
import org.jjazz.song.SongImpl;
import org.jjazz.song.ThrowingWriteOperation;
import org.jjazz.song.WriteOperation;
import org.jjazz.song.WriteOperationResults;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ThrowingSupplier;
import org.jjazz.xstream.api.XStreamInstancesManager;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;
import static org.jjazz.midimix.api.MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES;

/**
 * MidiMix implementation.
 */
public class MidiMixImpl implements PropertyChangeListener, Serializable, MidiMix
{

    /**
     * Store the instrumentMixes, one per Midi Channel.
     */
    private final InstrumentMix[] instrumentMixes = new InstrumentMix[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    /**
     * Store the RhythmVoices associated to an instrumentMix, one per channel.
     */
    private final RhythmVoice[] rhythmVoices = new RhythmVoice[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    /**
     * The InstrumentMixes with Solo ON
     */
    private final transient HashSet<InstrumentMix> soloedInsMixes = new HashSet<>();
    /**
     * The channels which should be rerouted to the GM DRUMS channel, and the related saved config.
     */
    private final transient HashMap<Integer, InstrumentMix> drumsReroutedChannels = new HashMap<>();
    /**
     * Saved Mute configuration on first soloed channel
     */
    private final transient boolean[] saveMuteConfiguration = new boolean[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    private final transient CopyOnWriteArrayList<UndoableEditListener> undoListeners = new CopyOnWriteArrayList<>();
    /**
     * The file where MidiMix was saved.
     */
    private transient File file;
    private transient SongImpl song;
    private ExecutionManager executionManager;
    private final PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(MidiMixImpl.class.getSimpleName());

    /**
     * Create an empty MidiMix.
     * <p>
     */
    public MidiMixImpl()
    {
        executionManager = new ExecutionManager();
    }

    public ExecutionManager getExecutionManager()
    {
        return executionManager;
    }

    @Override
    public Song getSong()
    {
        return song;
    }

    @Override
    public void removeRhythm(Rhythm r)
    {
        performWriteAPImethod(removeRhythmOperation(r));
    }

    public WriteOperation removeRhythmOperation(Rhythm r)
    {
        Objects.requireNonNull(r);
        WriteOperation operation = () -> 
        {
            if (!getUniqueRhythms().contains(r))
            {
                return WriteOperationResults.of(null);
            }

            // Save data
            var saveRhythmVoices = rhythmVoices.clone();
            var saveInstrumentMixes = instrumentMixes.clone();


            // Update data
            List<InsMixChange> insMixChanges = new ArrayList<>();
            List<InsMixChange> insMixChangesUndo = new ArrayList<>();
            for (int channel = MidiConst.CHANNEL_MIN; channel <= MidiConst.CHANNEL_MAX; channel++)
            {
                if (rhythmVoices[channel] != null && rhythmVoices[channel].getContainer() == r)
                {
                    insMixChanges.add(new InsMixChange(channel, rhythmVoices[channel], instrumentMixes[channel], null));
                    insMixChangesUndo.add(new InsMixChange(channel, rhythmVoices[channel], null, instrumentMixes[channel]));
                    prepareRemove(channel);
                    rhythmVoices[channel] = null;
                    instrumentMixes[channel] = null;
                }
            }

            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("Remove rhythm from MidiMix")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
                        {
                            if (saveRhythmVoices[i] != null && saveRhythmVoices[i].getContainer() == r)
                            {
                                prepareAdd(saveInstrumentMixes[i]);
                                rhythmVoices[i] = saveRhythmVoices[i];
                                instrumentMixes[i] = saveInstrumentMixes[i];
                            }
                        }

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChangesUndo, null);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
                        {
                            if (rhythmVoices[i] != null && rhythmVoices[i].getContainer() == r)
                            {
                                prepareRemove(i);
                                rhythmVoices[i] = null;
                                instrumentMixes[i] = null;
                            }
                        }

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };


            fireUndoableEditHappened(edit);


            var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void addRhythm(Rhythm r) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(addRhythmOperation(r));
    }

    public ThrowingWriteOperation addRhythmOperation(Rhythm r)
    {
        Objects.requireNonNull(r);
        ThrowingWriteOperation operation = () -> 
        {
            if (getUniqueRhythms().contains(r))
            {
                return WriteOperationResults.of(null);
            }

            var rRvs = r.getRhythmVoices();
            if (getUnusedChannels().size() < rRvs.size())
            {
                throwNotEnoughMidiChannelException();       // throws UnsupportedEditException
            }


            // Prepare data
            MidiMixImpl mmRhythm = (MidiMixImpl) MidiMixManager.getDefault().findMix(r);
            if (!getUniqueRhythms().isEmpty())
            {
                // Adapt mmRhythm to sound like the InstrumentMixes of r0           
                Rhythm r0 = getUniqueRhythms().iterator().next();
                mmRhythm.adaptInstrumentMixes(this, r0);
            }


            // Update state
            List<InsMixChange> insMixChanges = new ArrayList<>();
            List<InsMixChange> insMixChangesUndo = new ArrayList<>();
            for (Integer channelRhythm : mmRhythm.getUsedChannels())
            {
                RhythmVoice rvRhythm = mmRhythm.getRhythmVoice(channelRhythm);
                assert !(rvRhythm instanceof UserRhythmVoice);
                int channel = getUsedChannels().contains(channelRhythm) ? findFreeChannel(rvRhythm.isDrums()) : channelRhythm;
                assert channel != -1;
                InstrumentMix insMixRhythm = mmRhythm.getInstrumentMix(channelRhythm);

                insMixChanges.add(new InsMixChange(channel, rvRhythm, null, insMixRhythm));
                insMixChangesUndo.add(new InsMixChange(channel, rvRhythm, insMixRhythm, null));

                prepareAdd(insMixRhythm);
                rhythmVoices[channel] = rvRhythm;
                instrumentMixes[channel] = insMixRhythm;
            }


            // Save new state
            var saveNewRhythmVoices = rhythmVoices.clone();
            var saveNewInstrumentMixes = instrumentMixes.clone();


            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("add rhythm from MidiMix")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
                        {
                            if (rhythmVoices[i] != null && rhythmVoices[i].getContainer() == r)
                            {
                                prepareRemove(i);
                                rhythmVoices[i] = null;
                                instrumentMixes[i] = null;
                            }
                        }

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChangesUndo, null);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
                        {
                            if (saveNewRhythmVoices[i] != null && saveNewRhythmVoices[i].getContainer() == r)
                            {
                                prepareAdd(saveNewInstrumentMixes[i]);
                                rhythmVoices[i] = saveNewRhythmVoices[i];
                                instrumentMixes[i] = saveNewInstrumentMixes[i];
                            }
                        }


                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };


            fireUndoableEditHappened(edit);


            var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void addUserChannel(String name, boolean isDrums) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(addUserChannelOperation(name, isDrums));
    }

    public ThrowingWriteOperation addUserChannelOperation(String name, boolean isDrums)
    {
        Objects.requireNonNull(name);

        ThrowingWriteOperation operation = () -> 
        {
            // Prepare data
            int channel = findFreeUserChannel(isDrums);         // throws UnsupportedEditException


            var userChannelConfig = getUserChannelConfig(name, isDrums);
            var ins = userChannelConfig.instrument();
            var urv = userChannelConfig.userRhythmVoice();
            var insMix = new InstrumentMix(userChannelConfig.instrument(), new InstrumentSettings());


            // Update state
            prepareAdd(insMix);
            instrumentMixes[channel] = insMix;
            rhythmVoices[channel] = urv;
            var insMixChanges = List.of(new InsMixChange(channel, urv, null, insMix));
            var insMixChangesUndo = List.of(new InsMixChange(channel, urv, insMix, null));


            if (isDrums && ins == GMSynth.getInstance().getVoidInstrument() && channel != MidiConst.CHANNEL_DRUMS)
            {
                // Special case, better to activate drums rerouting
                changeDrumsReroutedChannel(true, channel);
            }


            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("Add user channel")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        prepareRemove(channel);
                        instrumentMixes[channel] = null;
                        rhythmVoices[channel] = null;

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChangesUndo, null);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        prepareAdd(insMix);
                        instrumentMixes[channel] = insMix;
                        rhythmVoices[channel] = urv;

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };


            fireUndoableEditHappened(edit);


            var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
            return WriteOperationResults.of(event, null);
        };

        return operation;

    }

    @Override
    public void removeUserChannel(String name)
    {
        performWriteAPImethod(removeUserChannelOperation(name));
    }

    public WriteOperation removeUserChannelOperation(String name)
    {
        Objects.requireNonNull(name);

        WriteOperation operation = () -> 
        {
            var urv = getUserRhythmVoice(name);
            if (urv == null)
            {
                return WriteOperationResults.of(null);
            }
            int channel = getChannel(urv);


            // Update state
            final InstrumentMix oldInsMix = prepareRemove(channel);
            instrumentMixes[channel] = null;
            rhythmVoices[channel] = null;
            var insMixChanges = List.of(new InsMixChange(channel, urv, oldInsMix, null));
            var insMixChangesUndo = List.of(new InsMixChange(channel, urv, null, oldInsMix));


            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("Remove user channel")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        prepareAdd(oldInsMix);

                        instrumentMixes[channel] = oldInsMix;
                        rhythmVoices[channel] = urv;

                        SongPropertyChangeEvent event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChangesUndo, null);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        prepareRemove(channel);
                        instrumentMixes[channel] = null;
                        rhythmVoices[channel] = null;

                        SongPropertyChangeEvent event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };


            fireUndoableEditHappened(edit);


            SongPropertyChangeEvent event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setInstrumentMix(int channel, RhythmVoice rvKey, InstrumentMix insMix)
    {
        performWriteAPImethod(setInstrumentMixOperation(channel, rvKey, insMix));
    }

    public WriteOperation setInstrumentMixOperation(int channel, RhythmVoice rvKey, InstrumentMix insMix)
    {
        Preconditions.checkArgument(MidiConst.checkMidiChannel(channel), "channel=%s", channel);
        Preconditions.checkArgument(!(rvKey instanceof RhythmVoiceDelegate), "rvKey=%s", rvKey);
        Preconditions.checkArgument((rvKey == null && insMix == null) || (rvKey != null && insMix != null), "rvKey=%s insMix=%s", rvKey, insMix);


        WriteOperation operation = () -> 
        {
            // Consistency checks
            if (rvKey != null && song != null)
            {
                // Check that rvKey belongs to song
                if (!(rvKey instanceof UserRhythmVoice) && !song.getSongStructure().getUniqueRhythmVoices(true, false).contains(rvKey))
                {
                    throw new IllegalArgumentException(
                            "setInstrumentMixOperation() channel=" + channel + " rvKey=" + rvKey + " insMix=" + insMix + ". rvKey does not belong to any of the song's rhythms.");
                }
                if ((rvKey instanceof UserRhythmVoice) && !song.getUserPhraseNames().contains(rvKey.getName()))
                {
                    throw new IllegalArgumentException("setInstrumentMixOperation() channel=" + channel + " rvKey=" + rvKey
                            + " insMix=" + insMix + " rvKey.getName()=" + rvKey.getName()
                            + " song=" + song.getName() + ". Song does not have a user phrase with the specified name");  // NOI18N
                }
            }
            if (insMix != null)
            {
                // Check the InstrumentMix is not already used for a different channel
                int ch = getInstrumentMixesPerChannel().indexOf(insMix);
                if (ch != -1 && ch != channel)
                {
                    throw new IllegalArgumentException(
                            "setInstrumentMixOperation() channel=" + channel + " rvKey=" + rvKey + " im=" + insMix + ". im is already present in MidiMix at channel " + ch);
                }
            }


            LOGGER.log(Level.FINE, "setInstrumentMix() channel={0} rvKey={1} insMix={2}", new Object[]
            {
                channel, rvKey, insMix
            });


            final RhythmVoice oldRvKey = rhythmVoices[channel];
            final InstrumentMix oldInsMix = prepareRemove(channel);
            if (Objects.equals(oldInsMix, insMix))
            {
                return WriteOperationResults.of(null);
            }
            prepareAdd(insMix);


            // Update state
            instrumentMixes[channel] = insMix;
            rhythmVoices[channel] = rvKey;
            var insMixChanges = List.of(new InsMixChange(channel, rvKey, oldInsMix, insMix));
            var insMixChangesUndo = List.of(new InsMixChange(channel, rvKey, insMix, oldInsMix));


            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("Change instrumemt mix")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        var tmpInsMix = prepareRemove(channel);
                        assert tmpInsMix == insMix;
                        prepareAdd(oldInsMix);

                        instrumentMixes[channel] = oldInsMix;       // Can be null
                        rhythmVoices[channel] = oldRvKey;       // Can be null

                        SongPropertyChangeEvent event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChangesUndo, null);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        var tmpInsMix = prepareRemove(channel);
                        assert tmpInsMix == oldInsMix;
                        prepareAdd(insMix);

                        instrumentMixes[channel] = insMix;          // Can be null
                        rhythmVoices[channel] = rvKey;              // Can be null

                        SongPropertyChangeEvent event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };


            fireUndoableEditHappened(edit);

            SongPropertyChangeEvent event = new SongPropertyChangeEvent(this, PROP_CHANNEL_INSTRUMENT_MIXES, insMixChanges, null);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setRhythmVoice(RhythmVoice oldRv, RhythmVoice newRv)
    {
        performWriteAPImethod(replaceRhythmVoiceOperation(oldRv, newRv));
    }

    public WriteOperation replaceRhythmVoiceOperation(final RhythmVoice oldRv, final RhythmVoice newRv)
    {
        Objects.requireNonNull(oldRv);
        Objects.requireNonNull(newRv);
        Preconditions.checkArgument(oldRv.getType() == newRv.getType(), "oldRv=%s, newRv=%s", oldRv, newRv);


        WriteOperation operation = () -> 
        {
            int channel = getChannel(oldRv);
            Preconditions.checkArgument(channel != -1, "replaceRhythmVoiceOperation() oldRv=%s", oldRv);
            Preconditions.checkArgument(getChannel(newRv) == -1, "replaceRhythmVoiceOperation() newRv=%s", newRv);

            LOGGER.log(Level.FINE, "replaceRhythmVoiceOperation() oldRv={0} newRv={1}", new Object[]
            {
                oldRv, newRv
            });


            // Update state
            rhythmVoices[channel] = newRv;


            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("Replace RhythmVoice")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        LOGGER.log(Level.FINER, "replaceRhythmVoice().undoBody oldRv={0} newRv={1}", new Object[]
                        {
                            oldRv, newRv
                        });

                        rhythmVoices[channel] = oldRv;

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_RHYTHM_VOICE, newRv, oldRv);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        LOGGER.log(Level.FINER, "replaceRhythmVoice().redoBody oldRv={0} newRv={1}", new Object[]
                        {
                            oldRv, newRv
                        });

                        rhythmVoices[channel] = newRv;

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_RHYTHM_VOICE, oldRv, newRv);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);

            var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_RHYTHM_VOICE, oldRv, newRv);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setRhythmVoiceChannel(RhythmVoice rv, int newChannel)
    {
        performWriteAPImethod(setRhythmVoiceChannelOperation(rv, newChannel));
    }

    public WriteOperation setRhythmVoiceChannelOperation(RhythmVoice rv, int newChannel)
    {
        WriteOperation operation = () -> 
        {
            int oldChannel = getChannel(rv);
            Preconditions.checkArgument(oldChannel != -1, "setRhythmVoiceChannelOperation() rv=%s", rv);
            Preconditions.checkArgument(getRhythmVoice(newChannel) == null, "setRhythmVoiceChannelOperation() newChannel=%s this=", newChannel, this);


            LOGGER.log(Level.FINE, "setRhythmVoiceChannelOperation() rv={0} newChannel={1}", new Object[]
            {
                rv, newChannel
            });


            // Change state
            swapChannels(oldChannel, newChannel);


            // Prepare the undoable edit
            UndoableEdit edit = new SimpleEdit("Set RhythmVoice channel")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        LOGGER.log(Level.FINER, "setRhythmVoiceChannel().undoBody oldChannel={0} newChannel={1}", new Object[]
                        {
                            oldChannel, newChannel
                        });

                        swapChannels(newChannel, oldChannel);

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_RHYTHM_VOICE_CHANNEL, newChannel, oldChannel);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        LOGGER.log(Level.FINER, "setRhythmVoiceChannel().redoBody oldChannel={0} newChannel={1}", new Object[]
                        {
                            oldChannel, newChannel
                        });

                        swapChannels(oldChannel, newChannel);

                        var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_RHYTHM_VOICE_CHANNEL, oldChannel, newChannel);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };


            fireUndoableEditHappened(edit);

            var event = new SongPropertyChangeEvent(MidiMixImpl.this, PROP_RHYTHM_VOICE_CHANNEL, oldChannel, newChannel);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }


    @Override
    public InstrumentMix getInstrumentMix(int channel)
    {
        Preconditions.checkArgument(MidiConst.checkMidiChannel(channel), "channel=%s", channel);
        return performReadAPImethod(() -> instrumentMixes[channel]);
    }

    @Override
    public InstrumentMix getInstrumentMix(RhythmVoice rv)
    {
        if (rv == null)
        {
            return null;
        }
        var rvKey = rv instanceof RhythmVoiceDelegate rvd ? rvd.getSource() : rv;
        return performReadAPImethod(() -> 
        {
            int index = Utilities.indexOfInstance(rvKey, rhythmVoices);
            return index == -1 ? null : instrumentMixes[index];
        });
    }


    @Override
    public int getChannel(InstrumentMix im)
    {
        return performReadAPImethod(() -> Utilities.indexOfInstance(im, instrumentMixes));
    }

    @Override
    public RhythmVoice getRhythmVoice(InstrumentMix im)
    {
        return performReadAPImethod(() -> 
        {
            int index = getChannel(im);
            return index == -1 ? null : rhythmVoices[index];
        });
    }

    @Override
    public RhythmVoice getRhythmVoice(int channel)
    {
        Preconditions.checkArgument(MidiConst.checkMidiChannel(channel), "channel=%s", channel);
        return performReadAPImethod(() -> rhythmVoices[channel]);
    }


    @Override
    public int getChannel(RhythmVoice rvKey)
    {
        Objects.requireNonNull(rvKey);
        var rv = rvKey instanceof RhythmVoiceDelegate rvd ? rvd.getSource() : rvKey;
        return performReadAPImethod(() -> Utilities.indexOfInstance(rv, rhythmVoices));
    }

    @Override
    public List<Integer> getUsedChannels()
    {
        List<Integer> channels = new ArrayList<>();
        return performReadAPImethod(() -> 
        {
            for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
            {
                if (instrumentMixes[i] != null)
                {
                    channels.add(i);
                }
            }
            return channels;
        });
    }

    @Override
    public List<Integer> getUsedChannels(Rhythm r)
    {
        if (r == null)
        {
            return getUsedChannels();
        }
        var r2 = (r instanceof AdaptedRhythm ar) ? ar.getSourceRhythm() : r;

        List<Integer> channels = new ArrayList<>();
        performReadAPImethod(() -> 
        {
            for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
            {
                if (instrumentMixes[i] != null && rhythmVoices[i].getContainer() == r2)
                {
                    channels.add(i);
                }
            }
            return null;
        });
        return channels;
    }

    @Override
    public List<Integer> getUnusedChannels()
    {
        List<Integer> channels = new ArrayList<>();
        performReadAPImethod(() -> 
        {
            for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
            {
                if (instrumentMixes[i] == null)
                {
                    channels.add(i);
                }
            }
            return null;
        });
        return channels;
    }


    @Override
    public List<Integer> getUserChannels()
    {
        return performReadAPImethod(() -> 
        {
            List<Integer> res = new ArrayList<>();
            for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
            {
                if (rhythmVoices[i] instanceof UserRhythmVoice)
                {
                    res.add(i);
                }
            }
            return res;
        });
    }

    @Override
    public List<UserRhythmVoice> getUserRhythmVoices()
    {
        return performReadAPImethod(() -> 
        {
            List<UserRhythmVoice> res = new ArrayList<>();
            for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
            {
                if (rhythmVoices[i] instanceof UserRhythmVoice urv)
                {
                    res.add(urv);
                }
            }
            return res;
        });
    }

    @Override
    public UserRhythmVoice getUserRhythmVoice(String name)
    {
        return getUserRhythmVoices().stream()
                .filter(urv -> urv.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    @Override
    public Set<Integer> getDrumsReroutedChannels()
    {
        return performReadAPImethod(() -> new HashSet<>(drumsReroutedChannels.keySet()));
    }

    @Override
    public void setDrumsReroutedChannel(boolean b, int channel)
    {
        performWriteAPImethod(setDrumsReroutedChannelOperation(b, channel));
    }

    public WriteOperation setDrumsReroutedChannelOperation(boolean b, int channel)
    {
        WriteOperation operation = () -> 
        {
            boolean changed = changeDrumsReroutedChannel(b, channel);

            SongPropertyChangeEvent event = null;
            if (changed)
            {
                event = new SongPropertyChangeEvent(this, PROP_CHANNEL_DRUMS_REROUTED, channel, b);
            }
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    /**
     * The file where this object is stored.
     *
     * @return Null if not set.
     */
    @Override
    public File getFile()
    {
        return file;
    }

    @Override
    public void setFile(File f)
    {
        file = f;
    }


    @Override
    public boolean saveToFileNotify(File f, boolean isCopy)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }

        boolean b = true;
        if (f.exists() && !f.canWrite())
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantOverwrite", f.getAbsolutePath());
            LOGGER.log(Level.WARNING, "saveToFileNotify() {0}", msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            b = false;
        }
        if (b)
        {
            try
            {
                saveToFile(f, isCopy);
            } catch (IOException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_ProblemSavingMixFile", f.getAbsolutePath()) + " : " + ex.getLocalizedMessage();
                if (ex.getCause() != null)
                {
                    msg += "\n" + ex.getCause().getLocalizedMessage();
                }
                LOGGER.log(Level.WARNING, "saveToFileNotify() {0}", msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                b = false;
            }
        }
        return b;
    }

    @Override
    public void saveToFile(File f, boolean isCopy) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f + " isCopy=" + isCopy);
        }
        LOGGER.log(Level.FINE, "saveToFile() f={0} isCopy={1}", new Object[]
        {
            f.getAbsolutePath(), isCopy
        });

        if (!isCopy)
        {
            file = f;
        }

        try (FileOutputStream fos = new FileOutputStream(f); Writer w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")))
        {
            XStream xstream = XStreamInstancesManager.getInstance().getSaveMidiMixInstance();
            xstream.toXML(this, w);
            if (!isCopy)
            {
                pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, true, false);
            }
        } catch (IOException e)
        {
            if (!isCopy)
            {
                file = null;
            }
            throw new IOException(e);
        } catch (XStreamException e)
        {
            if (!isCopy)
            {
                file = null;
            }
            LOGGER.log(Level.WARNING, "saveToFile() exception={0}", e.getMessage());
            // Translate into an IOException to be handled by the Netbeans framework 
            throw new IOException("XStream XML marshalling error", e);
        }
    }

    @Override
    public List<InstrumentMix> getInstrumentMixesPerChannel()
    {
        return performReadAPImethod(() -> Stream.of(instrumentMixes)
                .toList());
    }

    @Override
    public List<InstrumentMix> getInstrumentMixes()
    {
        return performReadAPImethod(() -> Stream.of(instrumentMixes)
                .filter(insMix -> insMix != null)
                .toList());
    }


    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return performReadAPImethod(() -> Stream.of(rhythmVoices).filter(rv -> rv != null).toList());
    }

    @Override
    public Set<Rhythm> getUniqueRhythms()
    {
        return performReadAPImethod(() -> 
        {
            Set<Rhythm> result = new HashSet<>();
            for (RhythmVoice rv : rhythmVoices)
            {
                if (rv != null && !(rv instanceof UserRhythmVoice))
                {
                    result.add(rv.getContainer());
                }
            }
            return result;
        });
    }

    @Override
    public int findFreeChannel(boolean isDrums)
    {
        List<Integer> usedChannels = getUsedChannels();
        if (isDrums && !usedChannels.contains(MidiConst.CHANNEL_DRUMS))
        {
            return MidiConst.CHANNEL_DRUMS;
        }

        // First search channels above Drums channel
        for (int channel = MidiConst.CHANNEL_DRUMS + 1; channel <= MidiConst.CHANNEL_MAX; channel++)
        {
            if (!usedChannels.contains(channel))
            {
                return channel;
            }
        }
        for (int channel = MidiConst.CHANNEL_DRUMS - 1; channel >= MidiConst.CHANNEL_MIN; channel--)
        {
            if (!usedChannels.contains(channel))
            {
                return channel;
            }
        }
        return -1;
    }


    @Override
    public void sendAllMidiMixMessages()
    {
        LOGGER.fine("sendAllMidiMixMessages()");
        for (Integer channel : getUsedChannels())
        {
            InstrumentMix insMix = getInstrumentMix(channel);
            JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
            jms.sendMidiMessagesOnJJazzMidiOut(insMix.getAllMidiMessages(channel));
        }
    }

    @Override
    public void sendAllMidiVolumeMessages()
    {
        LOGGER.fine("sendAllMidiVolumeMessages()");
        for (Integer channel : getUsedChannels())
        {
            InstrumentMix insMix = getInstrumentMix(channel);
            InstrumentSettings insSet = insMix.getSettings();
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(insSet.getVolumeMidiMessages(channel));
        }
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propertyName, l);
    }

    @Override
    public String toString()
    {
        return "MidiMix[sg=" + (song == null ? "null" : song.getName()) + ", ch=" + getUsedChannels() + "]";
    }


    /**
     * Associate a song to this MidiMix.
     * <p>
     * Song's ExecutionManager will be reused by this instance. Song will be also used to perform some consistency checks when modifying this instance.
     *
     * @param song Cannot be null
     */
    protected void setSong(SongImpl song)
    {
        Objects.requireNonNull(song);
        LOGGER.log(Level.FINE, "setSong() song={0}", song);
        this.song = song;
        executionManager = song.getExecutionManager();
    }

    /**
     * Get a deep copy of this MidiMix.
     * <p>
     *
     * @param song The song to be set on the returned instance. If not null, caller is responsible to provide a song consistent with this MidiMix.
     * @return
     * @see #checkConsistency(org.jjazz.song.api.Song, boolean)
     */
    protected MidiMix getDeepCopy(Song song)
    {
        MidiMixImpl mm = new MidiMixImpl();

        if (song instanceof SongImpl sgImpl)
        {
            mm.setSong(sgImpl);
        }

        performReadAPImethod(() -> 
        {
            System.arraycopy(rhythmVoices, 0, mm.rhythmVoices, 0, rhythmVoices.length);
            System.arraycopy(saveMuteConfiguration, 0, mm.saveMuteConfiguration, 0, saveMuteConfiguration.length);

            for (int i = 0; i < instrumentMixes.length; i++)
            {
                var insMix = instrumentMixes[i];
                mm.instrumentMixes[i] = insMix == null ? null : new InstrumentMix(insMix);
                if (soloedInsMixes.contains(insMix))
                {
                    mm.soloedInsMixes.add(mm.instrumentMixes[i]);
                }
            }

            for (int channel : drumsReroutedChannels.keySet())
            {
                assert mm.instrumentMixes[channel] != null : "this=" + this;
                mm.drumsReroutedChannels.put(channel, mm.instrumentMixes[channel]);
            }
            return null;
        });

        return mm;
    }

    /**
     * Fire the specified event, then a PROP_MODIFIED_OR_SAVED event (false-&gt;true), then possibly a PROP_MUSIC_GENERATION event.
     *
     * @param event
     */
    public void firePropertyChangeEvent(PropertyChangeEvent event)
    {
        pcs.firePropertyChange(event);


        fireIsModified();


        // Fire a PROP_MUSIC_GENERATION for specific events
        boolean musicChanged = switch (event.getPropertyName())
        {
            case PROP_CHANNEL_INSTRUMENT_MIXES ->
            {
                List<MidiMix.InsMixChange> insMixChanges = (List<MidiMix.InsMixChange>) event.getOldValue();
                for (var imc : insMixChanges)
                {
                    InstrumentMix oldInsMix = imc.oldInsMix();
                    InstrumentMix newInsMix = imc.newInsMix();
                    if (oldInsMix != null && newInsMix != null && InstrumentSettings.isMusicGenerationImpacted(oldInsMix.getSettings(), newInsMix.getSettings()))
                    {
                        yield true;
                    }
                }
                yield false;
            }
            // Need also PROP_RHYTHM_VOICE to fix issue #707 Exception when dragging from MixConsole PhraseViewer after a user channel name was changed
            case PROP_DRUMS_INSTRUMENT_KEYMAP, PROP_INSTRUMENT_TRANSPOSITION, PROP_INSTRUMENT_VELOCITY_SHIFT, PROP_CHANNEL_DRUMS_REROUTED, PROP_RHYTHM_VOICE ->
                true;
            default ->
                false;
        };
        if (musicChanged)
        {
            var mEvent = new PropertyChangeEvent(this, PROP_MUSIC_GENERATION, event, null);
            pcs.firePropertyChange(mEvent);
        }

    }

    static public void throwNotEnoughMidiChannelException() throws UnsupportedEditException
    {
        throw new UnsupportedEditException(ResUtil.getString(MidiMixImpl.class, "ERR_NotEnoughChannels"));
    }

    static public void throwSameNameUserChannelException(String name) throws UnsupportedEditException
    {
        throw new UnsupportedEditException(ResUtil.getString(MidiMixImpl.class, "ERR_SameNameUserChannel", name));
    }

    private <R> R performWriteAPImethod(WriteOperation<R> operation)
    {
        R res = executionManager.executeWriteOperation(operation);
        return res;
    }

    private <R> R performWriteAPImethodThrowing(ThrowingWriteOperation<R> operation) throws UnsupportedEditException
    {
        R res = executionManager.executeWriteOperationThrowing(operation);
        return res;
    }

    public <R> R performReadAPImethod(Supplier<R> operation)
    {
        R res = executionManager.executeReadOperation(operation);
        return res;
    }

    public <R, E extends Exception> R performReadAPImethodThrowing(ThrowingSupplier<R, E> operation) throws E
    {
        R res = executionManager.executeReadOperationThrowing(operation);
        return res;
    }

    //-----------------------------------------------------------------------
    // PropertyChangeListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);


        if (e.getSource() instanceof InstrumentMix insMix)
        {
            int channel = getChannel(insMix);

            switch (e.getPropertyName())
            {
                case InstrumentMix.PROP_SOLO ->
                {
                    boolean b = (boolean) e.getNewValue();
                    LOGGER.log(Level.FINE, "propertyChange() channel={0} solo={1}", new Object[]
                    {
                        channel, b
                    });

                    if (b)
                    {
                        // Solo switched to ON
                        if (soloedInsMixes.isEmpty())
                        {
                            // Fist solo !
                            soloedInsMixes.add(insMix);
                            // Save config
                            saveMuteConfig();
                            // Switch other channels to ON (ezxcept soloed one)
                            for (InstrumentMix im : instrumentMixes)
                            {
                                if (im != null && im != insMix)
                                {
                                    im.setMute(true);
                                }
                            }
                        } else
                        {
                            // It's another solo
                            soloedInsMixes.add(insMix);
                        }
                    } else // Solo switched to OFF
                    {
                        soloedInsMixes.remove(insMix);
                        if (soloedInsMixes.isEmpty())
                        {
                            // This was the last SOLO OFF, need to restore Mute config
                            restoreMuteConfig();
                        } else
                        {
                            // There are still other Solo ON channels, put it in mute again
                            insMix.setMute(true);
                        }
                    }
                    fireIsModified();
                }

                case InstrumentMix.PROP_MUTE ->
                {
                    boolean b = (boolean) e.getNewValue();
                    // If in solo mode, pressing unmute of a muted channel turns it in solo mode
                    if (b == false && !soloedInsMixes.isEmpty())
                    {
                        insMix.setSolo(true);
                    }
                    // Forward the MUTE change event
                    var evt = new PropertyChangeEvent(this, MidiMix.PROP_INSTRUMENT_MUTE, insMix, b);
                    firePropertyChangeEvent(evt);
                }

                case InstrumentMix.PROP_INSTRUMENT ->
                {
                    boolean fireModified = true;

                    // If drums instrument change with different KeyMap
                    Instrument oldIns = (Instrument) e.getOldValue();
                    Instrument newIns = (Instrument) e.getNewValue();
                    RhythmVoice rv = getRhythmVoice(channel);
                    if (rv.isDrums())
                    {
                        DrumKit oldKit = oldIns.getDrumKit();
                        DrumKit newKit = newIns.getDrumKit();
                        if ((oldKit != null && newKit != null && oldKit.getKeyMap() != newKit.getKeyMap())
                                || (oldKit == null && newKit != null)
                                || (oldKit != null && newKit == null))
                        {
                            var evt = new PropertyChangeEvent(this, MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, channel, oldKit != null
                                    ? oldKit.getKeyMap()
                                    : null);
                            firePropertyChangeEvent(evt);
                            fireModified = false;       // already done by call above
                        }
                    }

                    if (fireModified)
                    {
                        fireIsModified();
                    }
                }
                default ->
                {
                }
            }

        } else if (e.getSource() instanceof InstrumentSettings insSet)
        {
            // Forward some change events
            InstrumentMix insMix = insSet.getContainer();
            if (e.getPropertyName().equals(InstrumentSettings.PROPERTY_TRANSPOSITION))
            {
                int value = (Integer) e.getNewValue();
                var evt = new PropertyChangeEvent(this, MidiMix.PROP_INSTRUMENT_TRANSPOSITION, insMix, value);
                firePropertyChangeEvent(evt);

            } else if (e.getPropertyName().equals(InstrumentSettings.PROPERTY_VELOCITY_SHIFT))
            {
                int value = (Integer) e.getNewValue();
                var evt = new PropertyChangeEvent(this, MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT, insMix, value);
                firePropertyChangeEvent(evt);
            }

        }
    }

    //-----------------------------------------------------------------------
    // Private methods
    //-----------------------------------------------------------------------

    /**
     * Perform the drums-rerouting state change for channel.
     *
     * @param b
     * @param channel
     * @return True if rerouting was changed
     */
    private boolean changeDrumsReroutedChannel(boolean b, int channel)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "write lock required");
        Preconditions.checkArgument(instrumentMixes[channel] != null, "channel=%s this=%s", channel, this);

        LOGGER.log(Level.FINE, "changeDrumsReroutedChannel() -- b={0} channel={1}", new Object[]
        {
            b, channel
        });

        if (b == drumsReroutedChannels.containsKey(channel) || channel == MidiConst.CHANNEL_DRUMS)
        {
            return false;
        }

        InstrumentMix insMix = instrumentMixes[channel];
        if (b)
        {
            // Save state
            InstrumentMix saveMixData = new InstrumentMix(insMix);
            drumsReroutedChannels.put(channel, saveMixData);


            // Disable all parameters since it's rerouted
            insMix.setInstrumentEnabled(false);
            insMix.getSettings().setChorusEnabled(false);
            insMix.getSettings().setReverbEnabled(false);
            insMix.getSettings().setPanoramicEnabled(false);
            insMix.getSettings().setVolumeEnabled(false);
        } else
        {
            InstrumentMix saveMixData = drumsReroutedChannels.get(channel);
            assert saveMixData != null : "b=" + b + " channel=" + channel + " this=" + this;
            drumsReroutedChannels.remove(channel);


            // Restore parameters enabled state
            insMix.setInstrumentEnabled(saveMixData.isInstrumentEnabled());
            insMix.getSettings().setChorusEnabled(saveMixData.getSettings().isChorusEnabled());
            insMix.getSettings().setReverbEnabled(saveMixData.getSettings().isReverbEnabled());
            insMix.getSettings().setPanoramicEnabled(saveMixData.getSettings().isPanoramicEnabled());
            insMix.getSettings().setVolumeEnabled(saveMixData.getSettings().isVolumeEnabled());
        }

        return true;
    }

    /**
     * Prepare an InstrumentMix to be removed from this MidiMix.
     *
     * @param channel
     * @return The InstrumentMix prepared for remove. Can be null.
     */
    private InstrumentMix prepareRemove(int channel)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "prepareRemove() write lock required");
        var oldInsMix = instrumentMixes[channel];
        if (oldInsMix != null)
        {
            oldInsMix.removePropertyChangeListener(this);   // We don't want to be notified for setSolo(false)
            oldInsMix.setSolo(false);                       // So it does not mess if we were in solo mode
            soloedInsMixes.remove(oldInsMix);               // Directly update model data since we did not receive the solo=false property change event
            oldInsMix.getSettings().removePropertyChangeListener(this);
            changeDrumsReroutedChannel(false, channel);
        }
        return oldInsMix;
    }

    /**
     * Prepare insMix to be added.
     *
     * @param insMix Can be null
     */
    private void prepareAdd(InstrumentMix insMix)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "prepareAdd() write lock required");
        if (insMix != null)
        {
            // insMix.setMute(false);       // Don't change
            insMix.setSolo(false);
            insMix.addPropertyChangeListener(this);
            insMix.getSettings().addPropertyChangeListener(this);
        }
    }


    private void fireIsModified()
    {
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, false, true);
    }

    private void saveMuteConfig()
    {
        int i = 0;
        for (InstrumentMix im : instrumentMixes)
        {
            saveMuteConfiguration[i++] = (im == null) ? false : im.isMute();
        }
    }

    private void restoreMuteConfig()
    {
        int i = 0;
        for (InstrumentMix im : instrumentMixes)
        {
            if (im != null)
            {
                im.setMute(saveMuteConfiguration[i]);
            }
            i++;
        }
    }


    /**
     * Change the internal state so that oldChannel becomes newChannel.
     *
     * @param oldChannel
     * @param newChannel
     */
    private void swapChannels(int oldChannel, int newChannel)
    {
        rhythmVoices[newChannel] = rhythmVoices[oldChannel];
        rhythmVoices[oldChannel] = null;
        var insMix = instrumentMixes[oldChannel];
        instrumentMixes[newChannel] = insMix;
        instrumentMixes[oldChannel] = null;
        var reroutingSaveInsMix = drumsReroutedChannels.get(oldChannel);
        if (reroutingSaveInsMix != null)
        {
            drumsReroutedChannels.remove(oldChannel);
            drumsReroutedChannels.put(newChannel, reroutingSaveInsMix);
        }
        saveMuteConfiguration[newChannel] = saveMuteConfiguration[oldChannel];
        saveMuteConfiguration[oldChannel] = false;
    }


    /**
     * Get a new MidiMix channel for a user track.
     *
     * @param drums
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If nore more channel available
     */
    private int findFreeUserChannel(boolean drums) throws UnsupportedEditException
    {
        var res = getUsedChannels().contains(UserRhythmVoice.DEFAULT_USER_PHRASE_CHANNEL) ? findFreeChannel(drums)
                : UserRhythmVoice.DEFAULT_USER_PHRASE_CHANNEL;
        if (res == -1)
        {
            throwNotEnoughMidiChannelException();
        }
        return res;
    }


    /**
     * Adapt our InstrumentMixes to "sound" like the InstrumentMixes of rSrc in midiMixSrc.
     *
     * @param midiMixSrc
     * @param rSrc
     */
    private void adaptInstrumentMixes(MidiMix midiMixSrc, Rhythm rSrc)
    {
        LOGGER.log(Level.FINE, "adaptInstrumentMixes() midiMixSrc={0} rSrc={1}", new Object[]
        {
            midiMixSrc, rSrc
        });

        Map<String, InstrumentMix> mapKeyMix = new HashMap<>();
        Map<InstrumentFamily, InstrumentMix> mapFamilyMix = new HashMap<>();
        InstrumentMix rSrcInsMixDrums = null;
        InstrumentMix rSrcInsMixPerc = null;

        // First try to match InstrumentMixes using "key" = "3 first char of Rv.getName() + GM1 family"
        for (int channelSrc : midiMixSrc.getUsedChannels(rSrc))
        {
            // Build the keys from rSrc
            RhythmVoice rvSrc = midiMixSrc.getRhythmVoice(channelSrc);
            InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(channelSrc);
            if (rvSrc.isDrums())
            {
                // Special case, use the 2 special variables for Drums or Percussion                
                if (midiMixSrc.getDrumsReroutedChannels().contains(channelSrc))
                {
                    // If channel is rerouted, re-enable the disabled parameters
                    insMixSrc = new InstrumentMix(insMixSrc);
                    insMixSrc.setInstrumentEnabled(true);
                    insMixSrc.getSettings().setChorusEnabled(true);
                    insMixSrc.getSettings().setReverbEnabled(true);
                    insMixSrc.getSettings().setPanoramicEnabled(true);
                    insMixSrc.getSettings().setVolumeEnabled(true);
                }
                if (rvSrc.getType().equals(RhythmVoice.Type.DRUMS))
                {
                    rSrcInsMixDrums = insMixSrc;
                } else
                {
                    rSrcInsMixPerc = insMixSrc;
                }

            } else
            {
                GM1Instrument insGM1 = insMixSrc.getInstrument().getSubstitute();  // Might be null            
                InstrumentFamily family = insGM1 != null ? insGM1.getFamily() : null;
                String mapKey = Utilities.truncate(rvSrc.getName().toLowerCase(), 3) + "-" + ((family != null) ? family.name() : "");
                if (mapKeyMix.get(mapKey) == null)
                {
                    mapKeyMix.put(mapKey, insMixSrc);  // If several instruments have the same Type, save only the first one
                }
                if (family != null && mapFamilyMix.get(family) == null)
                {
                    mapFamilyMix.put(family, insMixSrc);       // If several instruments have the same family, save only the first one
                }
            }
        }

        // Try to convert using the keys
        HashSet<Integer> doneChannels = new HashSet<>();
        for (int channel : getUsedChannels())
        {
            RhythmVoice rv = getRhythmVoice(channel);
            InstrumentMix insMix = getInstrumentMix(channel);
            InstrumentMix insMixSrc;

            switch (rv.getType())
            {
                case DRUMS ->
                    insMixSrc = rSrcInsMixDrums;
                case PERCUSSION ->
                    insMixSrc = rSrcInsMixPerc;
                default ->
                {
                    GM1Instrument mmInsGM1 = insMix.getInstrument().getSubstitute();  // Can be null            
                    InstrumentFamily mmFamily = mmInsGM1 != null ? mmInsGM1.getFamily() : null;
                    String mapKey = Utilities.truncate(rv.getName().toLowerCase(), 3) + "-" + ((mmFamily != null)
                            ? mmFamily.name() : "");
                    insMixSrc = mapKeyMix.get(mapKey);
                }

            }

            if (insMixSrc != null)
            {
                // Copy InstrumentMix data
                insMix.setInstrument(insMixSrc.getInstrument());
                insMix.getSettings().set(insMixSrc.getSettings());
                doneChannels.add(channel);
                LOGGER.log(Level.FINER, "adaptInstrumentMixes() set (1) channel {0} instrument setting to : {1}", new Object[]
                {
                    channel,
                    insMixSrc.getSettings()
                });
            }
        }

        // Try to convert also the other channels by matching only the instrument family
        for (int channel : getUsedChannels())
        {
            if (doneChannels.contains(channel))
            {
                continue;
            }
            InstrumentMix insMix = getInstrumentMix(channel);
            GM1Instrument insGM1 = insMix.getInstrument().getSubstitute();  // Can be null          
            if (insGM1 == null || insGM1 == GMSynth.getInstance().getVoidInstrument())
            {
                continue;
            }
            InstrumentFamily mmFamily = insGM1.getFamily();
            InstrumentMix insMixFamily = mapFamilyMix.get(mmFamily);
            if (insMixFamily != null)
            {
                // Copy InstrumentMix data
                insMix.setInstrument(insMixFamily.getInstrument());
                insMix.getSettings().set(insMixFamily.getSettings());
                LOGGER.log(Level.FINER, "adaptInstrumentMixes() set (2) channel {0} instrument setting to : {1}", new Object[]
                {
                    channel,
                    insMixFamily.getSettings()
                });
            }
        }
    }

    /**
     * Get a dummy event which won't be fired.
     *
     * @return
     */
    private SongPropertyChangeEvent buildDummySongEvent()
    {
        return new SongPropertyChangeEvent(this, "DUMMY", Boolean.TRUE, Boolean.TRUE);
    }

    /**
     * Get the most appropriate UserRhythmVoice/Instrument for a new user channel.
     *
     * @param name
     * @param isDrums
     * @return
     */
    private UserChannelConfig getUserChannelConfig(String name, boolean isDrums)
    {
        UserRhythmVoice urv;
        Instrument ins;
        var insProvider = RhythmVoiceInstrumentProvider.getProvider();


        if (!isDrums)
        {
            // Directly use a RhythmVoiceInstrumentProvider to get the melodic instrument
            urv = new UserRhythmVoice(name);
            ins = insProvider.findInstrument(urv);

        } else
        {
            // Try to reuse the same drums instrument than in the current song
            var rvDrums = getRhythmVoice(MidiConst.CHANNEL_DRUMS);
            if (rvDrums == null)
            {
                // Unusual, but there might be another drums channel
                rvDrums = getRhythmVoices().stream()
                        .filter(rv -> rv.isDrums())
                        .findAny()
                        .orElse(null);
            }
            if (rvDrums != null)
            {
                ins = getInstrumentMix(rvDrums).getInstrument();
                DrumKit kit = ins.getDrumKit();     // Might be null if ins is the VoidInstrument from the GM bank
                urv = new UserRhythmVoice(name, kit != null ? kit : new DrumKit());
            } else
            {
                urv = new UserRhythmVoice(name, new DrumKit());
                ins = insProvider.findInstrument(urv);
            }
        }

        return new UserChannelConfig(urv, ins);
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        Objects.requireNonNull(edit);
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        undoListeners.forEach(l -> l.undoableEditHappened(event));
    }

    // ===============================================================================================================
    // Inner classes
    // ===============================================================================================================

    private record UserChannelConfig(UserRhythmVoice userRhythmVoice, Instrument instrument)
            {

    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // Nothing
                }
                case MIDIMIX_LOAD, MIDIMIX_SAVE ->
                {
                    if (instanceId.equals(MIDIMIX_LOAD))
                    {
                        // From 5.1.1 : introduced MidiMix interface API + MidiMixImpl 
                        // From 4.1.0: <MidiMix resolves-to="MidiMixSP">          
                        // From 3.0: <MidiMix resolves-to="org.jjazz.midimix.api.MidiMix$SerializationProxy">                         
                        // Before 3.0 : <MidiMix resolves-to="org.jjazz.midimix.MidiMix$SerializationProxy">
                        xstream.alias("org.jjazz.midimix.api.MidiMix$SerializationProxy", MidiMixImpl.SerializationProxy.class);
                        xstream.alias("org.jjazz.midimix.api.MidiMix$SerializationProxy$RvStorage", MidiMixImpl.SerializationProxy.RvStorage.class);
                        xstream.alias("org.jjazz.midimix.MidiMix$SerializationProxy", MidiMixImpl.SerializationProxy.class);
                        xstream.alias("org.jjazz.midimix.MidiMix$SerializationProxy$RvStorage", MidiMixImpl.SerializationProxy.RvStorage.class);


                        // From 3.0 all public packages were renamed with api or spi somewhere in the path
                        // Need package aliasing to be able to load old sng/mix files            
                        // xstream.aliasPackage("org.jjazz.midimix.api", "org.jjazz.midimix.api");     // Make sure new package name is not replaced by next alias
                        // xstream.aliasPackage("org.jjazz.midimix", "org.jjazz.midimix.api");
                    }

                    // From 5.1.1 we use MidiMixImpl
                    xstream.alias("MidiMix", MidiMixImpl.class);
                    xstream.alias("MidiMixSP", MidiMixImpl.SerializationProxy.class);
                    xstream.alias("RvStorage", MidiMixImpl.SerializationProxy.RvStorage.class);

                    // From 4.1.3 XStream with Java23 can not read anymore the 2 private fields from the RvStorage class. 
                    // Making RvStorage class static + its 2 fields public solves the issue for the future, but this does not let us read old .mix files
                    // which contain the "outer-class reference=..." tag. So we just ignore this element.
                    xstream.ignoreUnknownElements("outer-class");

                }
                default ->
                    throw new AssertionError(instanceId.name());
            }
        }
    }


    // ---------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------- 
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");

    }


    /**
     * A RhythmVoice depends on system dependent rhythm, therefore it must be stored in a special way: just save rhythm serial id + RhythmVoice name, and it
     * will be reconstructed at deserialization.
     * <p>
     * MidiMix is saved with Drums rerouting disabled and all solo status OFF, but all Mute status are saved.
     * <p>
     * spVERSION 2 changes saved fields see below<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction) <br>
     * spVERSION 4 (JJazzLab 4.1.3 with Java23) makes RvStorage class static + its 2 fields public + xStream.ignoreElement("outer-cast").<br>
     * spVERSION 5 (JJazzLab 5.0.2) add SP_RHYTHM_USER_CHANNEL_RHYTHM_ID to differentiate the melodic/drums type of a user voice.<br>
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -344448971122L;
        private static final String SP_MELODIC_USER_CHANNEL_RHYTHM_ID = "SpUserChannelRhythmID";
        private static final String SP_DRUMS_USER_CHANNEL_RHYTHM_ID = "SpDrumsUserChannelRhythmID";     // Since spVERSION 5
        private int spVERSION = 5;      // Do not make final!
        private InstrumentMix[] spInsMixes;
        private RvStorage[] spKeys;
        // spDelegates introduced with JJazzLab 2.1 => not used anymore with spVERSION=2        
        private List<RvStorage> spDelegates;   // Not used anymore, but keep it for backward compatibility

        private SerializationProxy(MidiMixImpl mm)
        {
            // Make a copy because we want to disable drums rerouting in the saved instance
            MidiMixImpl mmCopy = new MidiMixImpl();         // Drums rerouting disabled by default
            for (Integer channel : mm.getUsedChannels())
            {
                RhythmVoice rv = mm.getRhythmVoice(channel);
                InstrumentMix insMix = mm.getInstrumentMix(channel);
                mmCopy.setInstrumentMix(channel, rv, new InstrumentMix(insMix));
            }


            // If mm had some rerouted channels, apply the saved settings in the copy
            for (Integer channel : mm.getDrumsReroutedChannels())
            {
                InstrumentMix saveInsMix = mm.drumsReroutedChannels.get(channel);
                mmCopy.setInstrumentMix(channel, mm.getRhythmVoice(channel), saveInsMix);           // This also sets solo/mute off
            }


            // Save the RhythmVoice keys and the InstrumentMixes
            spInsMixes = mmCopy.instrumentMixes;
            spKeys = new RvStorage[mmCopy.rhythmVoices.length];
            for (int i = 0; i < mmCopy.rhythmVoices.length; i++)
            {
                RhythmVoice rv = mmCopy.rhythmVoices[i];
                if (rv != null)
                {
                    // Restore the mute status so it can be saved
                    InstrumentMix originalInsMix = mm.getInstrumentMix(i);
                    if (!(rv instanceof UserRhythmVoice))
                    {
                        spInsMixes[i].setMute(originalInsMix.isMute());
                    }

                    // Store the RhythmVoice using a RvStorage object
                    spKeys[i] = new RvStorage(rv);
                }
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            assert spKeys.length == this.spInsMixes.length :
                    "spKeys=" + Arrays.asList(spKeys) + " spInsMixes=" + Arrays.asList(spInsMixes);
            MidiMixImpl mm = new MidiMixImpl();


            for (int channel = 0; channel < spKeys.length; channel++)
            {
                RvStorage rvs = spKeys[channel];
                InstrumentMix insMix = spInsMixes[channel];


                if (insMix == null && rvs == null)
                {
                    // No instrument, skip
                    continue;
                }


                if (insMix == null)
                {
                    String msg = "Unexpected null value for insMix for channel=" + channel;
                    throw new XStreamException(msg.toString());
                } else if (rvs == null)
                {
                    String msg = "Unexpected null value for rvs for channel=" + channel;
                    throw new XStreamException(msg.toString());
                }


                // Retrieve the RhythmVoice
                RhythmVoice rv;
                try
                {
                    rv = rvs.rebuildRhythmVoice(insMix.getInstrument());
                } catch (UnavailableRhythmException ex)
                {
                    throw new XStreamException(ex.getMessage());
                }


                // Need a copy of insMix because setInstrumentMix() will make modifications on the object (e.g. setMute(false))
                // and we can not modify spInsMixes during the deserialization process.
                InstrumentMix insMixNew = new InstrumentMix(insMix);


                // Make sure we don't have a melodic instrument on a rhythm channel (can happen if we could not retrieve the MidiSynth for example)
                Instrument ins = insMixNew.getInstrument();
                if (rv.isDrums() && ins != GMSynth.getInstance().getVoidInstrument() && !ins.isDrumKit())
                {
                    insMixNew.setInstrument(GMSynth.getInstance().getVoidInstrument());
                }

                // Update the created MidiMix with the deserialized data
                mm.setInstrumentMix(channel, rv, insMixNew);
            }

            // spDelegates are no longer used from spVersion=2, IGNORED

            return mm;
        }

        /**
         * Stores the 2 strings used to identifiy a RhythmVoice.
         */
        static private class RvStorage
        {

            public String rhythmId;         // Had to switch to public for XStream with Java23
            public String rvName;           // Had to switch to public for XStream with Java23


            public RvStorage(RhythmVoice rv)
            {
                if (rv instanceof UserRhythmVoice)
                {
                    rhythmId = rv.isDrums() ? SP_DRUMS_USER_CHANNEL_RHYTHM_ID : SP_MELODIC_USER_CHANNEL_RHYTHM_ID;
                } else
                {
                    rhythmId = rv.getContainer().getUniqueId();
                }
                this.rvName = rv.getName();
            }

            /**
             * Rebuild a RhythmVoice or a UserRhythmVoice
             *
             * @param ins
             * @return Cannot be null
             * @throws org.jjazz.rhythmdatabase.api.UnavailableRhythmException RhythmVoice could not be retrieved
             */
            public RhythmVoice rebuildRhythmVoice(Instrument ins) throws UnavailableRhythmException
            {
                Objects.requireNonNull(ins);
                RhythmVoice rv = null;

                if (rhythmId.equals(SP_MELODIC_USER_CHANNEL_RHYTHM_ID))
                {
                    rv = new UserRhythmVoice(rvName);
                } else if (rhythmId.equals(SP_DRUMS_USER_CHANNEL_RHYTHM_ID))
                {
                    DrumKit kit = ins.getDrumKit();     // Might be null if ins is the VoidInstrument from the GM bank
                    rv = new UserRhythmVoice(rvName, kit == null ? new DrumKit() : kit);
                } else
                {
                    // Normal RhythmVoice
                    RhythmDatabase rdb = RhythmDatabase.getSharedInstance();
                    Rhythm r;
                    try
                    {
                        r = rdb.getRhythmInstance(rhythmId);    // Possible exception here
                    } catch (UnavailableRhythmException ex)
                    {
                        throw new UnavailableRhythmException("No rhythm found in RhythmDatabase for rhythmId=" + rhythmId);
                    }
                    rv = r.getRhythmVoices().stream()
                            .filter(rhv -> rhv.getName().equals(rvName))
                            .findAny()
                            .orElseThrow(() -> new UnavailableRhythmException("No " + r.getName() + " RhythmVoice found for rvName=" + rvName));
                }
                return rv;
            }
        }


    }

}
