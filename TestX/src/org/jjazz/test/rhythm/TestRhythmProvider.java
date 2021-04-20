package org.jjazz.test.rhythm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.util.MultipleErrorsReport;
import org.openide.util.lookup.ServiceProvider;

/**
 * For testing purposes only.
 */
@ServiceProvider(service = RhythmProvider.class)
public class TestRhythmProvider implements RhythmProvider
{

    public static final String RP_ID = "TestRhythmProviderId";
    private final Info info;
    private static final Logger LOGGER = Logger.getLogger(TestRhythmProvider.class.getSimpleName());

    public TestRhythmProvider()
    {
        info = new Info(RP_ID, "Test rhythm provider", "test", "JL", "1");
    }

    @Override
    public final String[] getSupportedFileExtensions()
    {
        return new String[0] ;
    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    @Override
    public void showUserSettingsDialog()
    {
        // nothing
    }

    @Override
    public boolean hasUserSettings()
    {
        return false;
    }

    @Override
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        return Arrays.asList(new TestRhythm());

    }

    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        return Collections.emptyList();
    }

    @Override
    public Rhythm readFast(File stdFile) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        return null;
    }
    

}
