package org.jjazz.test.rhythm;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.DrumKit.Type;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RP_STD_Variation;
import org.jjazz.rhythm.parameters.RP_SYS_Mute;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.jjazz.rhythmmusicgeneration.Phrase;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;

/**
 *
 * @author Administrateur
 */
public class TestRhythm implements Rhythm, MusicGenerator
{

    public List<RhythmVoice> rhythmVoices;
    public List<RhythmParameter<?>> rhythmParameters;

    public TestRhythm()
    {

    }

    @Override
    public RhythmFeatures getFeatures()
    {
        return RhythmFeatures.guessFeatures("Bossa Nova");
    }

    @Override
    public void loadResources() throws MusicGenerationException
    {
        // Nothing
    }

    @Override
    public void releaseResources()
    {
        // Nothing
    }

    @Override
    public boolean isResourcesLoaded()
    {
        return true;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        if (rhythmVoices == null)
        {
            // Build the RhythmVoices
            RhythmVoice rvDrums = new RhythmVoice(new DrumKit(Type.STANDARD, KeyMapGM.getInstance()),
                    this,
                    RhythmVoice.Type.DRUMS, "Drums",
                    StdSynth.getInstance().getVoidInstrument(),
                    9);
            RhythmVoice rvBass = new RhythmVoice(this,
                    RhythmVoice.Type.BASS,
                    "Bass",
                    RhythmVoice.Type.BASS.getDefaultInstrument(),
                    10);
            rhythmVoices = Arrays.asList(rvDrums, rvBass);
        }
        return rhythmVoices;
    }

    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        if (rhythmParameters == null)
        {
            rhythmParameters = new ArrayList<>();
            RP_STD_Variation rpVar = new RP_STD_Variation();
            RP_SYS_Mute rpMute = RP_SYS_Mute.createMuteRp(this);
            RP_Test rpTest1 = new RP_Test(getRhythmVoices().get(0));
            rhythmParameters = Arrays.asList(rpVar, rpMute, rpTest1);
        }
        return rhythmParameters;
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public String getUniqueId()
    {
        return "TestRhythmId";
    }

    @Override
    public String getDescription()
    {
        return "Test rhythm desc";
    }

    @Override
    public int getPreferredTempo()
    {
        return 120;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return TimeSignature.FOUR_FOUR;
    }

    @Override
    public String getName()
    {
        return "TestRhythm";
    }

    @Override
    public String getAuthor()
    {
        return "JL";
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        // Nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        // Nothing
    }

    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(MusicGenerationContext context) throws MusicGenerationException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
