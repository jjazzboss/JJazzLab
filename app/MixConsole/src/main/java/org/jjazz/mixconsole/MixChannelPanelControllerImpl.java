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

import com.google.common.base.Preconditions;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.musiccontrol.spi.ActiveSongBackgroundMusicBuilder;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.instrumentchooser.spi.InstrumentChooserDialog;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.keymap.StandardKeyMapConverter;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.mixconsole.actions.AddUserTrack;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.song.api.Song;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.rhythm.api.MusicGenerationException;

/**
 * Controller of a MixChannelPanel + associated PhraseViewerPanel.
 */
public class MixChannelPanelControllerImpl implements MixChannelPanelController
{

    private final Song song;
    private final MidiMix midiMix;
    private static final Logger LOGGER = Logger.getLogger(MixChannelPanelControllerImpl.class.getSimpleName());


    public MixChannelPanelControllerImpl(Song song, MidiMix midiMix)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        this.song = song;
        this.midiMix = midiMix;
    }

    @Override
    public void editChannelId(int channel, String strNewChannel)
    {
        int newChannel;
        try
        {
            newChannel = Integer.parseInt(strNewChannel) - 1;
        } catch (NumberFormatException e)
        {
            return;
        }
        if (!MidiConst.checkMidiChannel(newChannel) || newChannel == channel)
        {
            return;
        }


        // The current channel data
        InstrumentMix insMixSrc = midiMix.getInstrumentMix(channel);
        assert insMixSrc != null : "midiMix=" + midiMix + " channel=" + channel;
        RhythmVoice rvSrc = midiMix.getRhythmVoice(channel);


        // Check if we use drums channel for a non drums instrument
        if (newChannel == MidiConst.CHANNEL_DRUMS && !rvSrc.isDrums() && !InstrumentFamily.couldBeDrums(insMixSrc.getInstrument().getPatchName()))
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.Channel10reserved");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Can't override the click channel
        int clickChannel = PlaybackSettings.getInstance().getClickChannel(midiMix);
        if (newChannel == clickChannel && !rvSrc.isDrums())
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.Channel10ClickReserved", clickChannel + 1);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Make sure new channel is free
        if (midiMix.getRhythmVoice(newChannel) != null)
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.ChannelInUse", newChannel + 1);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Undoable event
        String undoText = "Change channel";
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(undoText);

        midiMix.setRhythmVoiceChannel(rvSrc, newChannel);

        um.endCEdit(undoText);
    }

    @Override
    public void editChannelName(int channel, String newName)
    {
        RhythmVoice rv = midiMix.getRhythmVoice(channel);
        assert rv instanceof UserRhythmVoice : "rv=" + rv + " channel=" + channel + " midiMix=" + midiMix;


        String undoText = "Rename user track";
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);

        um.startCEdit(undoText);

        String oldName = rv.getName();
        song.renameUserPhrase(oldName, newName);

        um.endCEdit(undoText);
    }

    @Override
    public void cloneRhythmTrackAsUserTrack(RhythmVoice rv)
    {

        // Find a name not already used
        var usedNames = song.getUserPhraseNames();
        String basename = rv.getName() + "-";
        int index = 1;
        while (usedNames.contains(basename + index))
        {
            index++;
        }
        String name = basename + index;


        // Retrieve the phrase
        var asmb = ActiveSongBackgroundMusicBuilder.getDefault();


        // Check for update status
        if (!asmb.isLastResultUpToDate())
        {
            String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.ErrorCloneRhythmTrack");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        var lastResult = asmb.getLastResult();

        // Check for exception during last music generation
        Throwable throwable = lastResult.throwable();
        if (throwable != null)
        {
            Level logLevel;
            String msg;
            if (throwable instanceof MusicGenerationException)
            {
                // MusicGenerationException can be a missing chord at section start                
                msg = throwable.getMessage();
                logLevel = Level.WARNING;
            } else
            {
                // It's more serious
                throwable.printStackTrace();
                msg = "Unexpected error, please check the log file and report the bug.\nex=" + throwable.getMessage();
                logLevel = Level.SEVERE;
            }
            LOGGER.log(logLevel, "cloneRhythmTrackAsUserTrack() {0} catched. msg={1}", new Object[]
            {
                throwable.getClass().getSimpleName(),
                throwable.getMessage()
            });
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        Phrase p = lastResult.mapRvPhrases().get(rv);
        if (p == null)
        {
            String msg = "Unexpected error: no phrase found for " + rv.getName();
            LOGGER.log(Level.SEVERE, "cloneRhythmTrackAsUserTrack() {0}", msg);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        boolean isDrums = rv.isDrums();
        if (p.isDrums() != isDrums)
        {
            LOGGER.log(Level.WARNING, "cloneRhythmTrackAsUserTrack() rv={0}: rv.isDrums()={1} but p.isDrums()={2}", new Object[]
            {
                rv, isDrums, !isDrums
            });
        }
        Phrase p2 = new Phrase(p.getChannel(), isDrums);
        p2.add(p);


        // Add the user track
        if (AddUserTrack.performAddUserPhrase(song, name, p2))
        {
            // Copy InstrumentMix from the original rhythm track and mute original track
            var userRv = midiMix.getUserRhythmVoice(name);
            var userRvChannel = midiMix.getChannel(userRv);
            var rvInsMix = midiMix.getInstrumentMix(rv);
            rvInsMix.setMute(true);
            var userInsMix = new InstrumentMix(rvInsMix);
            userInsMix.setMute(false);
            midiMix.setInstrumentMix(userRvChannel, userRv, userInsMix);
            Analytics.logEvent("Clone as user track");
        }

    }

    @Override
    public void editUserPhrase(UserRhythmVoice userRhythmVoice)
    {
        SongEditorManager.getDefault().showPianoRollEditorForUserTrack(song, midiMix, userRhythmVoice);
    }


    @Override
    public void removeUserPhrase(UserRhythmVoice urv)
    {
        String undoText = "Remove user track";

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(undoText);

        song.removeUserPhrase(urv.getName());

        um.endCEdit(undoText);
    }

    @Override
    public void editSettings(int channel)
    {
        MixChannelPanelSettingsDialog dlg = MixChannelPanelSettingsDialog.getInstance();
        String title = buildSettingsTitle(channel);
        dlg.preset(midiMix, channel, title);
        dlg.setVisible(true);
    }

    @Override
    public void editInstrument(int channel)
    {
        InstrumentMix insMix = midiMix.getInstrumentMix(channel);
        RhythmVoice rv = midiMix.getRhythmVoice(channel);
        InstrumentChooserDialog dlg = InstrumentChooserDialog.getDefault();
        dlg.preset(OutputSynthManager.getDefault().getDefaultOutputSynth(), rv, insMix.getInstrument(),
                insMix.getSettings().getTransposition(), channel);
        dlg.setVisible(true);


        Instrument ins = dlg.getSelectedInstrument();
        if (ins != null)
        {
            // Warning if drums keymap is not compatible even via a converter
            if (rv.isDrums() && ins.isDrumKit())
            {
                DrumKit.KeyMap srcKeyMap = rv.getDrumKit().getKeyMap();
                DrumKit.KeyMap destKeyMap = ins.getDrumKit().getKeyMap();
                if (destKeyMap.isContaining(srcKeyMap))
                {
                    // No problem, do nothing
                } else if (!StandardKeyMapConverter.accept(srcKeyMap, destKeyMap))
                {
                    // No conversion possible

                    String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.DrumKeyMapMismatch", ins.getPatchName(),
                            destKeyMap.getName(), srcKeyMap.getName());
                    NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                    Object result = DialogDisplayer.getDefault().notify(d);
                    if (NotifyDescriptor.YES_OPTION != result)
                    {
                        return;
                    }
                } else
                {
                    // Managed via conversion
                    LOGGER.log(Level.INFO, "editInstrument() channel={0} ins={1}: drum keymap conversion will be used {2}>{3}", new Object[]
                    {
                        channel, ins.getPatchName(), srcKeyMap, destKeyMap
                    });
                    String msg = ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.DrumKeyMapConversion",
                            srcKeyMap.getName(), destKeyMap.getName(), ins.getPatchName(), (channel + 1));
                    StatusDisplayer.getDefault().setStatusText(msg);
                }
            }
            insMix.setInstrument(ins);
            insMix.getSettings().setTransposition(dlg.getTransposition());
        }
    }

    @Override
    public void editNextInstrument(int channel)
    {
        InstrumentMix insMix = midiMix.getInstrumentMix(channel);
        InstrumentBank<?> bank = insMix.getInstrument().getBank();
        Instrument ins = bank.getNextInstrument(insMix.getInstrument());
        insMix.setInstrument(ins);
    }

    @Override
    public void editPreviousInstrument(int channel)
    {
        InstrumentMix insMix = midiMix.getInstrumentMix(channel);
        InstrumentBank<?> bank = insMix.getInstrument().getBank();
        Instrument ins = bank.getPreviousInstrument(insMix.getInstrument());
        insMix.setInstrument(ins);
    }


    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------
    private String buildSettingsTitle(int channel)
    {
        StringBuilder title = new StringBuilder(ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.DialogTitle", channel + 1));
        RhythmVoice rv = midiMix.getRhythmVoice(channel);
        if (rv instanceof UserRhythmVoice)
        {
            title.append(" - ").append(ResUtil.getString(getClass(), "MixChannelPanelControllerImpl.User"));
        } else
        {
            title.append(" - ").append(rv.getContainer().getName()).append(" - ").append(rv.getName());
        }
        return title.toString();
    }


}
