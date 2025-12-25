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
package org.jjazz.yamjjazz.rhythm.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
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
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.yamjjazz.rhythm.YamJJazzAdaptedRhythmImpl;
import org.jjazz.yamjjazz.rhythm.YamJJazzRhythmImpl;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Lookup;

/**
 * A provider of standard Yamaha style rhythms.
 */
@ServiceProvider(service = RhythmProvider.class)
public class YamahaRhythmProvider implements RhythmProvider
{

    private static YamahaRhythmProvider INSTANCE;
    public static final String RP_ID = "YamahaRhythmProviderID";
    private static final String DEFAULT_FILES_SUBDIR = "Yamaha";
    @StaticResource(relative = true)
    private static final String DEFAULT_FILES_RESOURCE_ZIP = "resources/YamahaDefaultFiles.zip";

    public static final String[] FILE_EXTENSIONS = new String[]
    {
        "sty", "prs", "bcs", "sst"
    };
    private List<Rhythm> fileRhythms;
    private final Info info;
    private final ExtensionFileFilter fileFilter;
    private static final Logger LOGGER = Logger.getLogger(YamahaRhythmProvider.class.getSimpleName());

    static public YamahaRhythmProvider getInstance()
    {
        return Lookup.getDefault().lookup(YamahaRhythmProvider.class);
    }

    /**
     * Public because of @ServiceProvider
     */
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
        var defaultRhythmFiles = getDefaultRhythmFiles(forceRescan);
        for (File f : defaultRhythmFiles)
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
        File rDir = RhythmDirsLocator.getDefault().getUserRhythmsDirectory();
        if (!rDir.isDirectory())
        {
            LOGGER.log(Level.WARNING, "getFileRhythms() RhythmProvider={0} - Rhythm file directory does not exist : {1}", new Object[]
            {
                info.getName(),
                rDir.getAbsolutePath()
            });
            return fileRhythms;
        }


        // Collect all the user-provided rhythm files (including .yjz files to be able to exclude base styles)
        ExtensionFileFilter specialFilter = new ExtensionFileFilter(YamJJazzRhythmProvider.FILE_EXTENSION, getSupportedFileExtensions());
        Set<Path> userRhythmPaths = Utilities.listFiles(rDir, specialFilter, PREFIX_IGNORED_SUBDIR, SUBDIR_MAX_DEPTH);
        LOGGER.log(Level.FINE, "getFileRhythms()   userRhythmPaths={0}", userRhythmPaths);


        // Don't add it it's the style is just a base style of a .yjz file
        removeYjzAndTheirBaseStyles(userRhythmPaths);


        // Read the user rhythm files
        for (Path path : userRhythmPaths)
        {
            if (defaultRhythmFiles.contains(path.toFile()))
            {
                // It might happen that the user rhythm directory is the same than the default rhythm directory (e.g. JJazzLabToolkit) 
                continue;
            }
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

    static public boolean isMine(RhythmInfo ri)
    {
        return ri.rhythmProviderId().equals(RP_ID);
    }

    // -------------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------------
    /**
     * Get the list of rhythm files (matching getFilenameFile()) present in the directory for default rhythm files.
     * <p>
     * If files are not yet present, extract them.
     *
     * @param doCopy If true extract the files from zip resource file
     * @return
     */
    private List<File> getDefaultRhythmFiles(boolean doCopy)
    {
        List<File> res = new ArrayList<>();
        var ddir = RhythmDirsLocator.getDefault().getDefaultRhythmsDirectory();
        File rDir = new File(ddir, DEFAULT_FILES_SUBDIR);
        if (!rDir.isDirectory() && !rDir.mkdir())
        {
            LOGGER.log(Level.SEVERE, "getDefaultRhythmFiles() Can''t create directory {0}", rDir.getAbsolutePath());
            return res;
        }

        File[] files = rDir.listFiles(fileFilter);
        if (doCopy || files.length == 0)
        {
            for (File f : copyDefaultResourceFiles(rDir))
            {
                if (fileFilter.accept(rDir, f.getName()))
                {
                    res.add(f);
                }
            }
        } else
        {
            res.addAll(Arrays.asList(files));
        }
        return res;
    }

    /**
     * Copy the default rhythm files within the JAR to destPath.
     * <p>
     *
     * @param destDir
     * @return The list of copied files in destDir
     */
    private List<File> copyDefaultResourceFiles(File destDir)
    {
        List<File> res = Utilities.extractZipResource(getClass(), DEFAULT_FILES_RESOURCE_ZIP, destDir.toPath(), true);
        LOGGER.log(Level.INFO, "copyDefaultResourceFiles() Copied {0} default rhythm files to {1}", new Object[]
        {
            res.size(), destDir.getAbsolutePath()
        });
        return res;
    }

    private void removeYjzAndTheirBaseStyles(Set<Path> stylePaths)
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
