package org.jjazz.importers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.util.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * For debug purposes...
 */
@ActionID(category = "JJazz", id = "org.jjazz.test.batchimportsave")
@ActionRegistration(displayName = "Batch Import And Save")
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 87232),
        // @ActionReference(path = "Shortcuts", name = "C-T")
        })
public final class BatchImportSave implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(BatchImportSave.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "BatchImportSave() STARTED");
        final File dirIn = new File("c:\\in");
        final File dirOut = new File("c:\\out");
        LOGGER.info("BatchImportSave() dirIn=" + dirIn.getAbsolutePath());
        LOGGER.info("BatchImportSave() dirOut=" + dirOut.getAbsolutePath());

        final ImprovisorImporter importer = Lookup.getDefault().lookup(ImprovisorImporter.class);
        if (importer == null)
        {
            LOGGER.severe("BatchImportSave() importer=" + importer);
            return;
        }
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                convert(dirIn, importer, dirOut);
                LOGGER.log(Level.INFO, "BatchImportSave()  COMPLETED");
            }
        };
        new Thread(r).start();

    }

    private void convert(File dirIn, ImprovisorImporter importer, File dirOut)
    {
        for (File f : dirIn.listFiles())
        {
            LOGGER.info("BatchImportSave() Processing " + f.getAbsolutePath() + "...");
            Song song = null;
            try
            {
                song = importer.importFromFile(f);
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "BatchImportSave()   => ERROR ex=" + ex.getLocalizedMessage());
                continue;
            }
            if (song == null)
            {
                LOGGER.log(Level.WARNING, "BatchImportSave()   => ERROR song is null");
            } else
            {
                // Ok we got the new song
                String outFilename = Utilities.replaceExtension(f.getName(), ".sng");
                File fOut = new File(dirOut, outFilename);
                try
                {
                    song.saveToFile(fOut, false);
                } catch (IOException ex)
                {
                    LOGGER.log(Level.WARNING, "BatchImportSave()   => ERROR saving " + fOut.getAbsolutePath() + " ex=" + ex.getLocalizedMessage());
                }
            }
        }
    }
}
