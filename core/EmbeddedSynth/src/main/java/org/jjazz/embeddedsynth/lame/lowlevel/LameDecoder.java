package org.jjazz.embeddedsynth.lame.lowlevel;

import org.jjazz.embeddedsynth.lame.mp3.*;

import java.nio.ByteBuffer;

public class LameDecoder {

  private Lame lame;

  public LameDecoder(final String mp3File) {
    // Create Decoder
    lame = new Lame();
    // Set parameters
    lame.getFlags().setWriteId3tagAutomatic(false);
    // Analyze parameters and set more internal options accordingly
    lame.initParams();

    lame.getParser().setInputFormat(GetAudio.SoundFileFormat.sf_mp3);

    final FrameSkip frameSkip = new FrameSkip();

    lame.getAudio().initInFile(lame.getFlags(), mp3File, frameSkip);

    int skipStart = 0;
    int skipEnd = 0;

    if (lame.getParser().silent < 10)
      System.out.printf("\rinput:  %s%s(%g kHz, %d channel%s, ", mp3File,
          mp3File.length() > 26 ? "\n\t" : "  ", lame.getFlags()
              .getInSampleRate() / 1.e3, lame.getFlags()
              .getInNumChannels(), lame.getFlags()
              .getInNumChannels() != 1 ? "s" : "");

    if (frameSkip.getEncoderDelay() > -1
        || frameSkip.getEncoderPadding() > -1) {
      if (frameSkip.getEncoderDelay() > -1)
        skipStart = frameSkip.getEncoderDelay() + 528 + 1;
      if (frameSkip.getEncoderPadding() > -1)
        skipEnd = frameSkip.getEncoderPadding() - (528 + 1);
    } else {
      skipStart = lame.getFlags().getEncoderDelay() + 528 + 1;
    }
    System.out.printf("MPEG-%d%s Layer %s", 2 - lame.getFlags()
            .getMpegVersion(),
        lame.getFlags().getOutSampleRate() < 16000 ? ".5" : "", "III");

    System.out.printf(")\noutput: (16 bit, Microsoft WAVE)\n");

    if (skipStart > 0)
      System.out.printf(
          "skipping initial %d samples (encoder+decoder delay)\n",
          skipStart);
    if (skipEnd > 0)
      System.out
          .printf("skipping final %d samples (encoder padding-decoder delay)\n",
              skipEnd);

    final int totalFrames = lame.getParser().getMp3InputData()
        .getNumSamples()
        / lame.getParser().getMp3InputData().getFrameSize();
    lame.getParser().getMp3InputData().setTotalFrames(totalFrames);

    assert (lame.getFlags().getInNumChannels() >= 1 && lame.getFlags()
        .getInNumChannels() <= 2);
  }

  public final boolean decode(final ByteBuffer sampleBuffer) {
    final float buffer[][] = new float[2][1152];
    final LameGlobalFlags flags = lame.getFlags();
    int iread = lame.getAudio().get_audio16(flags, buffer);
    if (iread >= 0) {
      final MP3Data mp3InputData = lame.getParser().getMp3InputData();
      final int framesDecodedCounter = mp3InputData
          .getFramesDecodedCounter()
          + iread
          / mp3InputData.getFrameSize();
      mp3InputData.setFramesDecodedCounter(framesDecodedCounter);

      for (int i = 0; i < iread; i++) {
        int sample = ((int) buffer[0][i] & 0xffff);
        sampleBuffer.array()[(i << flags.getInNumChannels()) + 0] = (byte) (sample & 0xff);
        sampleBuffer.array()[(i << flags.getInNumChannels()) + 1] = (byte) ((sample >> 8) & 0xff);
        if (flags.getInNumChannels() == 2) {
          sample = ((int) buffer[1][i] & 0xffff);
          sampleBuffer.array()[(i << flags.getInNumChannels()) + 2] = (byte) (sample & 0xff);
          sampleBuffer.array()[(i << flags.getInNumChannels()) + 3] = (byte) ((sample >> 8) & 0xff);
        }
      }
    }
    return iread > 0;
  }

  public final void close() {
    lame.close();
  }

  public final int getChannels() {
    return lame.getFlags().getInNumChannels();
  }

  public final int getSampleRate() {
    return lame.getFlags().getInSampleRate();
  }

  public final int getFrameSize() {
    return lame.getFlags().getFrameSize();
  }

  public final int getBufferSize() {
    return getChannels() * getFrameSize() * 2;
  }
}
