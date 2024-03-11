package org.jjazz.embeddedsynth.lame.lowlevel;

import org.jjazz.embeddedsynth.lame.mp3.Lame;
import org.jjazz.embeddedsynth.lame.mp3.MPEGMode;
import org.jjazz.embeddedsynth.lame.mp3.VbrMode;
import org.jjazz.embeddedsynth.lame.mp3.Version;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

/**
 * Wrapper for the jump3r encoder.
 *
 * @author Ken Händel
 */
public class LameEncoder {

  public static final AudioFormat.Encoding MPEG1L3 = new AudioFormat.Encoding(
      "MPEG1L3");
  // Lame converts automagically to MPEG2 or MPEG2.5, if necessary.
  public static final AudioFormat.Encoding MPEG2L3 = new AudioFormat.Encoding(
      "MPEG2L3");
  public static final AudioFormat.Encoding MPEG2DOT5L3 = new AudioFormat.Encoding(
      "MPEG2DOT5L3");

  // property constants
  /**
   * property key to read/set the VBR mode (Boolean, default: false)
   */
  public static final String P_VBR = "vbr";
  /**
   * property key to read/set the channel mode (MPEGMode).
   */
  public static final String P_CHMODE = "chmode";
  /**
   * property key to read/set the bitrate (Integer : 32...320 kbit/s). Set to
   * BITRATE_AUTO for default bitrate.
   */
  public static final String P_BITRATE = "bitrate";
  /**
   * property key to read/set the quality (Integer : 1 (highest) to 9
   * (lowest).
   */
  public static final String P_QUALITY = "quality";

  /**
   * MPEG-2
   */
  public static final int MPEG_VERSION_2 = 0;
  /**
   * MPEG-1
   */
  public static final int MPEG_VERSION_1 = 1;
  /**
   * MPEG-2.5
   */
  public static final int MPEG_VERSION_2DOT5 = 2;

  public static final int BITRATE_AUTO = -1;
  /**
   * Constant bit rate.
   * <p>
   * Note:
   * In VBR mode, bit rate is ignored.
   */
  public static int DEFAULT_BITRATE = BITRATE_AUTO;
  private int bitRate = DEFAULT_BITRATE;
  /**
   * suggested maximum buffer size for an mpeg frame
   */
  private static final int DEFAULT_PCM_BUFFER_SIZE = 2048 * 16;
  public static int DEFAULT_QUALITY = Lame.QUALITY_MIDDLE;
  private int quality = DEFAULT_QUALITY;
  public static boolean DEFAULT_VBR = false;
  private boolean vbrMode = DEFAULT_VBR;
  /**
   * MP3 encoder.
   */
  private Lame lame = new Lame();
  private Version version = new Version();
  // encoding values
  private int sampleSizeInBits;
  private ByteOrder byteOrder;
  private MPEGMode chMode;

  // these fields are set upon successful initialization to show effective
  // values.
  private int effQuality;
  private int effBitRate;
  private VbrMode effVbr;
  private MPEGMode effChMode;
  private int effSampleRate;
  private int effEncoding;

  /**
   * Initializes the encoder with the given source/PCM format. The default mp3
   * encoding parameters are used, see DEFAULT_BITRATE, DEFAULT_CHANNEL_MODE,
   * DEFAULT_QUALITY, and DEFAULT_VBR.
   *
   * @throws IllegalArgumentException when parameters are not supported by LAME.
   */
  public LameEncoder(AudioFormat sourceFormat) {
    readParams(sourceFormat, null);
    initParams(sourceFormat);
  }

  /**
   * Initializes the encoder with the given source/PCM format. The mp3
   * parameters are read from the targetFormat's properties. For any parameter
   * that is not set, global system properties are queried for backwards
   * tritonus compatibility. Last, parameters will use the default values
   * DEFAULT_BITRATE, DEFAULT_CHANNEL_MODE, DEFAULT_QUALITY, and DEFAULT_VBR.
   *
   * @throws IllegalArgumentException when parameters are not supported by LAME.
   */
  public LameEncoder(AudioFormat sourceFormat, AudioFormat targetFormat) {
    readParams(sourceFormat, targetFormat.properties());
    initParams(sourceFormat);
  }

  /**
   * Initializes the encoder, overriding any parameters set in the audio
   * format's properties or in the system properties.
   *
   * @throws IllegalArgumentException when parameters are not supported by LAME.
   */
  public LameEncoder(AudioFormat sourceFormat, int bitRate,
                     MPEGMode channelMode, int quality, boolean VBR) {
    this.bitRate = bitRate;
    this.chMode = channelMode;
    this.quality = quality;
    this.vbrMode = VBR;
    initParams(sourceFormat);
  }

  private void readParams(AudioFormat sourceFormat, Map<String, Object> props) {
    if (props != null) {
      readProps(props);
    }
  }

  private void initParams(AudioFormat sourceFormat) {
    sampleSizeInBits = sourceFormat.getSampleSizeInBits();
    byteOrder = sourceFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN
        : ByteOrder.LITTLE_ENDIAN;
    // simple check that bitrate is not too high for MPEG2 and MPEG2.5
    // todo: exception ?
    if (sourceFormat.getSampleRate() < 32000 && bitRate > 160) {
      bitRate = 160;
    }
    int result = initParams(sourceFormat.getChannels(),
        Math.round(sourceFormat.getSampleRate()), bitRate, chMode,
        quality, vbrMode, sourceFormat.isBigEndian());
    if (result < 0) {
      throw new IllegalArgumentException(
          "parameters not supported by LAME (returned " + result
              + ")");
    }
  }

  /**
   * Initializes the lame encoder. Throws IllegalArgumentException when
   * parameters are not supported by LAME.
   */
  private int initParams(int channels, int sampleRate, int bitrate,
                         MPEGMode mode, int quality, boolean VBR, boolean bigEndian) {
    // Set parameters
    lame.getFlags().setInNumChannels(channels);
    lame.getFlags().setInSampleRate(sampleRate);
    lame.getFlags().setMode(mode);
    if (VBR) {
      lame.getFlags().setVBR(VbrMode.vbr_default);
      lame.getFlags().setVBRQuality(quality);
    } else {
      if (bitrate != BITRATE_AUTO) {
        lame.getFlags().setBitRate(bitrate);
      }
    }
    lame.getFlags().setQuality(quality);
    lame.getId3().init(lame.getFlags());
    lame.getFlags().setWriteId3tagAutomatic(false);
    lame.getFlags().setFindReplayGain(true);
    // Analyze parameters and set more internal options accordingly
    int rc = lame.initParams();
    // return effective values
    effSampleRate = lame.getFlags().getOutSampleRate();
    effBitRate = lame.getFlags().getBitRate();
    effChMode = lame.getFlags().getMode();
    effVbr = lame.getFlags().getVBR();
    effQuality = (VBR) ? lame.getFlags().getVBRQuality() : lame.getFlags()
        .getQuality();
    return rc;
  }

  /**
   * Get encoder version string.
   *
   * @return encoder version string
   */
  public final String getEncoderVersion() {
    return version.getLameVersion();
  }

  /**
   * Returns the buffer needed pcm buffer size. The passed parameter is a
   * wished buffer size. The implementation of the encoder may return a lower
   * or higher buffer size. The encoder must be initalized (i.e. not closed)
   * at this point. A return value of <0 denotes an error.
   */
  public final int getPCMBufferSize() {
    return DEFAULT_PCM_BUFFER_SIZE;
  }

  public final int getMP3BufferSize() {
    // bad estimate :)
    return getPCMBufferSize() / 2 + 1024;
  }

  private int doEncodeBuffer(final byte[] pcm, final int pcmOffset,
                             final int length, final byte[] encoded) {
    int bytesPerSample = sampleSizeInBits >> 3;
    int samplesRead = length / bytesPerSample;
    int[] sampleBuffer = new int[samplesRead];

    int sampleBufferPos = samplesRead;
    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
      if (bytesPerSample == 1)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24;
      if (bytesPerSample == 2)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 16
              | (pcm[pcmOffset + i + 1] & 0xff) << 24;
      if (bytesPerSample == 3)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 8
              | (pcm[pcmOffset + i + 1] & 0xff) << 16
              | (pcm[pcmOffset + i + 2] & 0xff) << 24;
      if (bytesPerSample == 4)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff)
              | (pcm[pcmOffset + i + 1] & 0xff) << 8
              | (pcm[pcmOffset + i + 2] & 0xff) << 16
              | (pcm[pcmOffset + i + 3] & 0xff) << 24;
    } else {
      if (bytesPerSample == 1)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = ((pcm[pcmOffset + i] & 0xff) ^ 0x80) << 24
              | 0x7f << 16;
      if (bytesPerSample == 2)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24
              | (pcm[pcmOffset + i + 1] & 0xff) << 16;
      if (bytesPerSample == 3)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24
              | (pcm[pcmOffset + i + 1] & 0xff) << 16
              | (pcm[pcmOffset + i + 2] & 0xff) << 8;
      if (bytesPerSample == 4)
        for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0; )
          sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24
              | (pcm[pcmOffset + i + 1] & 0xff) << 16
              | (pcm[pcmOffset + i + 2] & 0xff) << 8
              | (pcm[pcmOffset + i + 3] & 0xff);
    }

    sampleBufferPos = samplesRead;
    samplesRead /= lame.getFlags().getInNumChannels();

    float buffer[][] = new float[2][samplesRead];
    if (lame.getFlags().getInNumChannels() == 2) {
      for (int i = samplesRead; --i >= 0; ) {
        buffer[1][i] = sampleBuffer[--sampleBufferPos];
        buffer[0][i] = sampleBuffer[--sampleBufferPos];
      }
    } else if (lame.getFlags().getInNumChannels() == 1) {
      Arrays.fill(buffer[1], 0, samplesRead, 0);
      for (int i = samplesRead; --i >= 0; ) {
        buffer[0][i] = buffer[1][i] = sampleBuffer[--sampleBufferPos];
      }
    }
    return lame.encodeBuffer(buffer[0], buffer[1], samplesRead, encoded);
  }

  /**
   * Encode a block of data. Throws IllegalArgumentException when parameters
   * are wrong. When the <code>encoded</code> array is too small, an
   * ArrayIndexOutOfBoundsException is thrown. <code>length</code> should be
   * the value returned by getPCMBufferSize.
   *
   * @return the number of bytes written to <code>encoded</code>. May be 0.
   */
  public final int encodeBuffer(final byte[] pcm, final int pcmOffset,
                                final int pcmLength, final byte[] encoded)
      throws ArrayIndexOutOfBoundsException {
    if (pcmLength < 0 || (pcmOffset + pcmLength) > pcm.length) {
      throw new IllegalArgumentException("inconsistent parameters");
    }
    int result = doEncodeBuffer(pcm, pcmOffset, pcmLength, encoded);
    if (result < 0) {
      if (result == -1) {
        throw new ArrayIndexOutOfBoundsException(
            "Encode buffer too small");
      }
      throw new RuntimeException("crucial error in encodeBuffer.");
    }
    return result;
  }

  public final int encodeFinish(final byte[] encoded) {
    return lame.encodeFlush(encoded);
  }

  public final void close() {
    lame.close();
  }

  /**
   * Read properties.
   * <ul>
   * <li>P_QUALITY - Integer, 1 (highest) to 9 (lowest)<BR>
   * <li>P_BITRATE - Integer, 32...320 kbit/s<BR>
   * <li>P_CHMODE - MPEGMode<BR>
   * <li>P_VBR - Boolean
   * </ul>
   */
  private void readProps(final Map<String, Object> props) {
    quality = (Integer) props.get(P_QUALITY);
    bitRate = (Integer) props.get(P_BITRATE);
    chMode = (MPEGMode) props.get(P_CHMODE);
    vbrMode = (Boolean) props.get(P_VBR);
  }

  /**
   * Return the audioformat representing the encoded mp3 stream. The format
   * object will have the following properties:
   * <ul>
   * <li>P_QUALITY - Integer, 1 (highest) to 9 (lowest)<BR>
   * <li>P_BITRATE - Integer, 32...320 kbit/s<BR>
   * <li>P_CHMODE - MPEGMode<BR>
   * <li>P_VBR - Boolean
   * <li>encoder.name: a string with the name of the encoder
   * <li>encoder.version: a string with the version of the encoder
   * </ul>
   */
  public final AudioFormat getEffectiveFormat() {
    // first gather properties
    final HashMap<String, Object> map = new HashMap<String, Object>();
    map.put(P_QUALITY, getEffectiveQuality());
    map.put(P_BITRATE, getEffectiveBitRate());
    map.put(P_CHMODE, getEffectiveChannelMode());
    map.put(P_VBR, getEffectiveVBR());
    // map.put(P_SAMPLERATE, getEffectiveSampleRate());
    // map.put(P_ENCODING,getEffectiveEncoding());
    map.put("encoder.name", "LAME");
    map.put("encoder.version", getEncoderVersion());
    int channels = (chMode == MPEGMode.MONO) ? 1 : 2;

    return new AudioFormat(getEffectiveEncoding(),
        getEffectiveSampleRate(), NOT_SPECIFIED, channels,
        NOT_SPECIFIED, NOT_SPECIFIED, false, map);
  }

  public final int getEffectiveQuality() {
    if (effQuality >= Lame.QUALITY_LOWEST) {
      return Lame.QUALITY_LOWEST;
    } else if (effQuality >= Lame.QUALITY_LOW) {
      return Lame.QUALITY_LOW;
    } else if (effQuality >= Lame.QUALITY_MIDDLE) {
      return Lame.QUALITY_MIDDLE;
    } else if (effQuality >= Lame.QUALITY_HIGH) {
      return Lame.QUALITY_HIGH;
    }
    return Lame.QUALITY_HIGHEST;
  }

  public final int getEffectiveBitRate() {
    return effBitRate;
  }

  public final MPEGMode getEffectiveChannelMode() {
    return effChMode;
  }

  public final boolean getEffectiveVBR() {
    return effVbr != VbrMode.vbr_off;
  }

  public final int getEffectiveSampleRate() {
    return effSampleRate;
  }

  public final AudioFormat.Encoding getEffectiveEncoding() {
    if (effEncoding == MPEG_VERSION_2) {
      if (getEffectiveSampleRate() < 16000) {
        return MPEG2DOT5L3;
      }
      return MPEG2L3;
    } else if (effEncoding == MPEG_VERSION_2DOT5) {
      return MPEG2DOT5L3;
    }
    // default
    return MPEG1L3;
  }

  /** * Lame.java ** */

}
