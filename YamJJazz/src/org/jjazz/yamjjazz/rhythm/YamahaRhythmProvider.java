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
package org.jjazz.yamjjazz.rhythm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import static org.jjazz.rhythm.spi.RhythmProvider.PREFIX_IGNORED_SUBDIR;
import static org.jjazz.rhythm.spi.RhythmProvider.SUBDIR_MAX_DEPTH;
import org.jjazz.utilities.api.ExtensionFileFilter;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.yamjjazz.FormatNotSupportedException;
import org.openide.modules.InstalledFileLocator;

/**
 * A provider of standard Yamaha style rhythms.
 */
@ServiceProvider(service = RhythmProvider.class)
public class YamahaRhythmProvider implements RhythmProvider
{

    public static final String RP_ID = "YamahaRhythmProviderID";
    public static final String DEFAULT_FILES_DEST_DIRNAME = "modules/YamahaDefaultFiles";
    public static final String[] FILE_EXTENSIONS = new String[]
    {
        "sty", "prs", "bcs", "sst"
    };
    private List<Rhythm> fileRhythms;
    private final Info info;
    private final ExtensionFileFilter fileFilter;
    private static final Logger LOGGER = Logger.getLogger(YamahaRhythmProvider.class.getSimpleName());

    public YamahaRhythmProvider()
    {
        info = new Info(RP_ID, "YamJJazz standard styles", "YamJJazz rhythm provider (.prs, .sty, .sst, .bcs)", "JL", "1");

        // Add the .yjz to be able to spot the
        fileFilter = new ExtensionFileFilter(getSupportedFileExtensions());
    }

    @Override
    public final String[] getSupportedFileExtensions()
    {
        return FILE_EXTENSIONS;
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
        return Collections.emptyList();

    }

    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        // Use cached data if possible
        if (!forceRescan && fileRhythms != null)
        {
            return new ArrayList<>(fileRhythms);
        }


        // Reset data
        fileRhythms = new ArrayList<>();


        // Get the default rhythms
        for (File f : getDefaultRhythmFiles())
        {
            Rhythm r;
            try
            {
                r = readFast(f);
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "getFileRhythms() ex={0}", ex.getLocalizedMessage());
                errRpt.individualErrorMessages.add(ex.getLocalizedMessage());
                continue;
            }
            fileRhythms.add(r);
        }


        // Check user rhythm dir is available
        File rDir = FileDirectoryManager.getInstance().getUserRhythmDirectory();
        if (!rDir.isDirectory())
        {
            LOGGER.log(Level.WARNING, "getFileRhythms() RhythmProvider={0} - Rhythm file directory does not exist : {1}", new Object[]{info.getName(),
                rDir.getAbsolutePath()});
            return fileRhythms;
        }


        // Collect all the user-provided rhythm files (including .yjz files to be able to exclude base styles)
        ExtensionFileFilter specialFilter = new ExtensionFileFilter(YamJJazzRhythmProvider.FILE_EXTENSION, getSupportedFileExtensions());
        HashSet<Path> userRhythmPaths = Utilities.listFiles(rDir, specialFilter, PREFIX_IGNORED_SUBDIR, SUBDIR_MAX_DEPTH);
        LOGGER.log(Level.FINE, "getFileRhythms()   userRhythmPaths={0}", userRhythmPaths);


        // Don't add it it's the style is just a base style of a .yjz file
        removeYjzAndTheirBaseStyles(userRhythmPaths);


        // Read the user rhythm files
        for (Path path : userRhythmPaths)
        {
            Rhythm r;
            try
            {
                r = readFast(path.toFile());
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "getFileRhythms() ex={0}", ex.getLocalizedMessage());
                errRpt.individualErrorMessages.add(ex.getLocalizedMessage());
                continue;
            }
            fileRhythms.add(r);
        }

        if (!errRpt.individualErrorMessages.isEmpty())
        {
            errRpt.primaryErrorMessage = errRpt.individualErrorMessages.size() + " rhythm files could not be read.";
            errRpt.secondaryErrorMessage = "Rhythm Provider: YamJJazz";
        }

        return new ArrayList<>(fileRhythms);
    }

    /**
     * Quickly read a file only to get enough information to build a minimal Rhythm object.
     * <p>
     *
     * @param stdFile
     * @return
     */
    @Override
    public Rhythm readFast(File stdFile) throws IOException
    {
        LOGGER.log(Level.INFO, "readFast() Reading {0}", stdFile.getAbsolutePath());

        if (!stdFile.exists())
        {
            throw new IOException("File " + stdFile.getAbsolutePath() + " not found.");
        }
        String ext = Utilities.getExtension(stdFile.getName()).toLowerCase();
        if (!Arrays.asList(FILE_EXTENSIONS).contains(ext))
        {
            throw new IOException("Invalid file extension " + stdFile.getAbsolutePath());
        }

        Rhythm r = null;
        try
        {
            r = new YamJJazzRhythmImpl(stdFile);  // Don't call loadResources() to save memory and gain some time
        } catch (IOException | InvalidMidiDataException | FormatNotSupportedException ex)
        {
            throw new IOException("Problem reading file " + stdFile.getAbsolutePath() + ". Ex=" + ex.getLocalizedMessage());
        }
        return r;
    }

    @Override
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        if (r == null || ts == null || r.getTimeSignature().equals(ts))
        {
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);   //NOI18N
        }
        if (RhythmDatabase.getDefault().getRhythmProvider(r) == this)
        {
            return new YamJJazzAdaptedRhythmImpl(getInfo().getUniqueId(), (YamJJazzRhythm) r, ts);
        }
        return null;
    }

    /**
     *
     * @return The list of files present in the directory for default rhythm files.
     */
    private File[] getDefaultRhythmFiles()
    {
        File[] res;
        File dir = InstalledFileLocator.getDefault().locate(DEFAULT_FILES_DEST_DIRNAME, "org.jjazz.yamjjazz", false);


        if (dir == null || !dir.isDirectory())
        {
            LOGGER.severe("getDefaultRhythmFiles() Can't find " + DEFAULT_FILES_DEST_DIRNAME);
            res = new File[0];
        } else
        {
            res = dir.listFiles(fileFilter);
        }

        return res;
    }


    private void removeYjzAndTheirBaseStyles(HashSet<Path> stylePaths)
    {
        // Get all .yjz files
        var yjzPaths = stylePaths.stream()
                .filter(p -> p.toString().toLowerCase().endsWith(YamJJazzRhythmProvider.FILE_EXTENSION))
                .toList();


        // Remove all .yjz files and remove all possible corresponding base styles
        for (Path yjzPath : yjzPaths)
        {
            stylePaths.remove(yjzPath);

            Path parent = yjzPath.getParent();
            String yjzFilenameNoExt = Utilities.replaceExtension(yjzPath.getFileName().toString(), "");
            for (String ext : FILE_EXTENSIONS)
            {
                Path baseFilePath = parent.resolve(yjzFilenameNoExt + "." + ext);
                stylePaths.remove(baseFilePath);
            }
        }
    }

}
