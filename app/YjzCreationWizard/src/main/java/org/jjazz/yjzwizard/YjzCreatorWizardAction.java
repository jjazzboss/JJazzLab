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
package org.jjazz.yjzwizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

/**
 * YamJJazz extended style file creation wizard.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.yjzwizard.YjzCreatorWizardAction")
@ActionRegistration(displayName = "#CTL_ExtStyleCreationDialogAction")
@ActionReference(path = "Menu/Tools", position = 1400, separatorAfter = 1500)
public final class YjzCreatorWizardAction implements ActionListener
{

    public static final String PROP_BASE_RHYTHM = "BaseRhythmInfo";   //NOI18N 
    public static final String PROP_NB_MAIN_A = "NbMainA";   //NOI18N 
    public static final String PROP_NB_MAIN_B = "NbMainB";   //NOI18N 
    public static final String PROP_NB_MAIN_C = "NbMainC";   //NOI18N 
    public static final String PROP_NB_MAIN_D = "NbMainD";   //NOI18N 
    public static final String PROP_INCLUDE_INTRO_ENDINGS = "IncludeIntroEndings";   //NOI18N 
    public static final String PROP_INCLUDE_FILLS = "IncludeFills";   //NOI18N 
    public static final String PROP_NB_SRC_PHRASES = "NbSrcPhrases";   //NOI18N 

    private static final Logger LOGGER = Logger.getLogger(YjzCreatorWizardAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        YjzCreatorWizardIterator iterator = new YjzCreatorWizardIterator();
        WizardDescriptor wiz = new WizardDescriptor(iterator);

        // Default values
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_A, 2);
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_B, 2);
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_C, 2);
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_MAIN_D, 2);
        wiz.putProperty(YjzCreatorWizardAction.PROP_NB_SRC_PHRASES, 2);
        wiz.putProperty(YjzCreatorWizardAction.PROP_INCLUDE_INTRO_ENDINGS, false);
        wiz.putProperty(YjzCreatorWizardAction.PROP_INCLUDE_FILLS, false);


        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
        wiz.setTitle(ResUtil.getString(getClass(), "CTL_ExtStyleCreationDialogTitle"));
        if (DialogDisplayer.getDefault().notify(wiz) != WizardDescriptor.FINISH_OPTION)
        {
            return;
        }

        // Log event
        Analytics.logEvent("Wizard Yjz Creation");


        // Retrieve the Wizard results
        int nbMainA = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_A);
        int nbMainB = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_B);
        int nbMainC = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_C);
        int nbMainD = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_MAIN_D);
        int nbSrcPhrases = (Integer) wiz.getProperty(YjzCreatorWizardAction.PROP_NB_SRC_PHRASES);
        boolean includeIntroEndings = (Boolean) wiz.getProperty(YjzCreatorWizardAction.PROP_INCLUDE_INTRO_ENDINGS);
        boolean includeFills = (Boolean) wiz.getProperty(YjzCreatorWizardAction.PROP_INCLUDE_FILLS);
        RhythmInfo baseRhythmInfo = (RhythmInfo) wiz.getProperty(YjzCreatorWizardAction.PROP_BASE_RHYTHM);


        LOGGER.log(Level.FINE, "baseRhythmInfo={0} nbMainA={1} nbMainB={2} nbMainC={3} nbMainD={4} includeIntroEndings={5} nbSrcPhrases={6}", new Object[]
        {
            baseRhythmInfo,
            nbMainA, nbMainB, nbMainC, nbMainD, includeIntroEndings, nbSrcPhrases
        });


        // Build the sequence
        YjzFileBuilder builder = new YjzFileBuilder(baseRhythmInfo, nbMainA, nbMainB, nbMainC, nbMainD, includeIntroEndings, includeFills, nbSrcPhrases);
        Sequence sequence;
        try
        {
            sequence = builder.buildSequence();
        } catch (UnavailableRhythmException | InvalidMidiDataException | MusicGenerationException ex)
        {
            String msg = ResUtil.getString(getClass(), "ERR_UnableToCreateYjzFile");
            msg += ": " + ex.getLocalizedMessage();
            LOGGER.warning(msg);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        // LOGGER.severe("actionPerformed() sequence=" + MidiUtilities.toString(sequence));

        // Save the files
        createFiles(sequence, baseRhythmInfo);

    }

    /**
     * Get the file for the copy of the original base style file.
     * <p>
     * File is based on getExtendedFile() with extension changed to match the one from the original base style file.
     *
     * @param baseRhythm
     * @return
     */
    public static File getBaseFileCopy(RhythmInfo baseRhythm)
    {
        String ext = Utilities.getExtension(baseRhythm.file().getName());
        File yjzFile = getExtendedFile(baseRhythm);
        var res = Utilities.replaceExtension(yjzFile, ext);
        return res;
    }

    /**
     * Get the file of the new .yjz file.
     * <p>
     * Make sure there is no filename clash in the targer directory.
     *
     * @param baseRhythm
     * @return
     */
    public static File getExtendedFile(RhythmInfo baseRhythm)
    {
        File res;
        var rdl = RhythmDirsLocator.getDefault();
        String baseFilename = baseRhythm.file().getName();
        String nameNoExt = Utilities.replaceExtension(baseFilename, "");
        String filename = nameNoExt + "-ext." + YamJJazzRhythmProvider.FILE_EXTENSION;
        int index = 1;
        do
        {
            res = new File(rdl.getUserRhythmsDirectory(), filename);
            filename = nameNoExt + "-ext" + (index++) + "." + YamJJazzRhythmProvider.FILE_EXTENSION;
        } while (res.exists());
        return res;
    }

    // ======================================================================================================
    // Private methods
    // ======================================================================================================    
    /**
     * Create the .yjz file from the sequence and copy the renamed base style file.
     *
     * @param sequence
     * @param baseRhythmInfo
     */
    private void createFiles(Sequence sequence, RhythmInfo baseRhythmInfo)
    {

        // Target files
        File yjzFile = getExtendedFile(baseRhythmInfo);
        File baseCopyFile = getBaseFileCopy(baseRhythmInfo);


        // getExtendedFile() should ensure there is no .yjz file name clash 
        assert !yjzFile.exists() : "yjzFile=" + yjzFile.getAbsolutePath();   //NOI18N


        // Write the .yjz file from the sequence
        LOGGER.log(Level.INFO, "actionPerformed() creating .yjz file: {0}", yjzFile.getAbsolutePath());
        try
        {
            MidiSystem.write(sequence, 1, yjzFile);
        } catch (IOException ex)
        {
            String msg = "Unable to create .yjz file";
            msg += ": " + ex.getLocalizedMessage();
            LOGGER.warning(msg);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Copy the renamed base style
        try
        {
            Files.copy(baseRhythmInfo.file().toPath(), baseCopyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex)
        {
            String msg = ResUtil.getString(getClass(), "ERR_UnableToCopy", new Object[]
            {
                baseRhythmInfo.file().toPath(), baseCopyFile.toPath()
            });
            msg += " : " + ex.getLocalizedMessage();
            LOGGER.warning(msg);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_CreatedYjzFile", yjzFile.getAbsolutePath()));


        // Add the new style in the rhythm database
        var rdb = RhythmDatabase.getDefault();
        RhythmProvider rp = RhythmProvider.getRhythmProvider(YamJJazzRhythmProvider.RP_ID);
        assert rp != null;   //NOI18N
        try
        {
            Rhythm r = rp.readFast(yjzFile);
            rdb.addRhythmInstance(rp, r);

        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "createFiles() ex={0}", ex);
            String msg = ResUtil.getString(getClass(), "ERR_FileCreatedButCantRead", yjzFile.getAbsolutePath());
            msg += ": " + ex.getLocalizedMessage();
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Success, notify user
        String msg = ResUtil.getString(getClass(), "CTL_CreatedYjzSuccess", yjzFile.getAbsolutePath());
        NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
        DialogDisplayer.getDefault().notify(d);

    }
}
