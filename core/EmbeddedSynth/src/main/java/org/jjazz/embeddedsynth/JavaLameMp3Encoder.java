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
package org.jjazz.embeddedsynth;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jjazz.embeddedsynth.lame.lowlevel.LameEncoder;
import org.jjazz.embeddedsynth.lame.mp3.Lame;
import org.jjazz.embeddedsynth.lame.mp3.MPEGMode;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.api.Mp3Encoder;
import org.jjazz.utilities.api.Utilities;

/**
 * A mp3 encoder which relies on org.jjazz.embeddedsynth.lame-3.98.4.jar found here https://github.com/nwaldispuehl/java-lame
 */
public class JavaLameMp3Encoder implements Mp3Encoder
{

    private static final int MP3_GOOD_QUALITY_BITRATE = 320;
    private static final int MP3_LOW_QUALITY_BITRATE = 128;
    private static final Logger LOGGER = Logger.getLogger(JavaLameMp3Encoder.class.getSimpleName());

    @Override
    public void encode(File inFile, File mp3File, boolean lowQuality, boolean useVariableEncoding) throws EmbeddedSynthException
    {
        if (!Utilities.getExtension(inFile.getName()).equalsIgnoreCase("wav"))
        {
            throw new EmbeddedSynthException("File format not supported: " + inFile.getName());
        }

        try
        {
            wavToMp3(inFile, mp3File, lowQuality ? MP3_LOW_QUALITY_BITRATE : MP3_GOOD_QUALITY_BITRATE, useVariableEncoding);

        } catch (IOException | UnsupportedAudioFileException ex)
        {
            LOGGER.log(Level.WARNING, "encode() inFile={0} ex={1}", new Object[]
            {
                inFile, ex
            });
            String msg = ex.getMessage();
            if (msg == null)
            {
                msg = "Unknown exception";
            }
            throw new EmbeddedSynthException(msg);
        }
    }
    // ============================================================================================================
    // Private methods
    // ============================================================================================================

    /**
     * Convert a WAV file to a MP3 file.
     *
     * @param wavFile
     * @param mp3File
     * @param bitRate             E.g 128 for medium quality, 320 for high quality
     * @param useVariableEncoding
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    private void wavToMp3(File wavFile, File mp3File, int bitRate, boolean useVariableEncoding) throws IOException, UnsupportedAudioFileException
    {
        var is = new FileInputStream(wavFile);
        AudioInputStream audioIs = AudioSystem.getAudioInputStream(new BufferedInputStream(is));        // BufferedInputStream needed to add mark/reset support
        byte[] mp3Bytes = encodeToMp3(audioIs, bitRate, useVariableEncoding);
        new FileOutputStream(mp3File).write(mp3Bytes);
    }

    /**
     * Do the stream encoding using lame.
     */
    private byte[] encodeToMp3(AudioInputStream audioInputStream, int bitRate, boolean useVariableEncoding) throws IOException
    {
        LameEncoder encoder = new LameEncoder(audioInputStream.getFormat(), bitRate, MPEGMode.STEREO, Lame.QUALITY_HIGHEST,
                useVariableEncoding);

        ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
        byte[] inputBuffer = new byte[encoder.getPCMBufferSize()];
        byte[] outputBuffer = new byte[encoder.getPCMBufferSize()];

        int bytesRead;
        int bytesWritten;

        while (0 < (bytesRead = audioInputStream.read(inputBuffer)))
        {
            bytesWritten = encoder.encodeBuffer(inputBuffer, 0, bytesRead, outputBuffer);
            mp3.write(outputBuffer, 0, bytesWritten);
        }

        encoder.close();
        return mp3.toByteArray();
    }
}
