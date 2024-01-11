package org.jjazz.embeddedsynth.lame.mp3;

/**
 * Control Parameters set by User. These parameters are here for backwards
 * compatibility with the old, non-shared lib API. Please use the
 * lame_set_variablename() functions below
 *
 * @author Ken
 */
public class LameGlobalFlags {

  public long class_id;

	/* input description */

  /**
   * number of samples. default=-1
   */
  public int num_samples;
  /**
   * scale input by this amount before encoding at least not used for MP3
   * decoding
   */
  public float scale;
  /**
   * scale input of channel 0 (left) by this amount before encoding
   */
  public float scale_left;
  /**
   * scale input of channel 1 (right) by this amount before encoding
   */
  public float scale_right;
  /**
   * collect data for a MP3 frame analyzer?
   */
  public boolean analysis;
  /**
   * add Xing VBR tag?
   */
  public boolean bWriteVbrTag;
  /**
   * use lame/mpglib to convert mp3 to wav
   */
  public boolean decode_only;
  /**
   * force M/S mode. requires mode=1
   */
  public boolean force_ms;
  /**
   * use free format? default=0
   */
  public boolean free_format;
  /**
   * decode on the fly? default=0
   */
  public boolean decode_on_the_fly;
  /**
   * sizeof(wav file)/sizeof(mp3 file)
   */
  public float compression_ratio;
  /**
   * mark as copyright. default=0
   */
  public int copyright;
  /**
   * mark as original. default=1
   */
  public int original;

	/* general control params */
  /**
   * the MP3 'private extension' bit. Meaningless
   */
  public int extension;
  /**
   * Input PCM is emphased PCM (for instance from one of the rarely emphased
   * CDs), it is STRONGLY not recommended to use this, because psycho does not
   * take it into account, and last but not least many decoders don't care
   * about these bits
   */
  public int emphasis;
  /**
   * use 2 bytes per frame for a CRC checksum. default=0
   */
  public boolean error_protection;
  /**
   * enforce ISO spec as much as possible
   */
  public boolean strict_ISO;
  /**
   * use bit reservoir?
   */
  public boolean disable_reservoir;
  /* quantization/noise shaping */
  public int quant_comp;
  public int quant_comp_short;
  public boolean experimentalY;
  public int experimentalZ;
  public int exp_nspsytune;
  public int preset;
  /**
   * Range [0,...,1[
   */
  public float VBR_q_frac;
  public int VBR_mean_bitrate_kbps;
  public int VBR_min_bitrate_kbps;
  public int VBR_max_bitrate_kbps;
  /**
   * strictly enforce VBR_min_bitrate normaly, it will be violated for analog
   * silence
   */
  public int VBR_hard_min;
  /**
   * freq in Hz. 0=lame choses. -1=no filter
   */
  public int lowpassfreq;
  /**
   * freq in Hz. 0=lame choses. -1=no filter
   */
  public int highpassfreq;
  /**
   * freq width of filter, in Hz (default=15%)
   */
  public int lowpasswidth;
  /**
   * freq width of filter, in Hz (default=15%)
   */
  public int highpasswidth;
  public float maskingadjust;
  public float maskingadjust_short;

	/* frame params */
  /**
   * only use ATH
   */
  public boolean ATHonly;
  /**
   * only use ATH for short blocks
   */
  public boolean ATHshort;
  /**
   * disable ATH
   */
  public boolean noATH;
  /**
   * select ATH formula
   */
  public int ATHtype;
  /**
   * change ATH formula 4 shape
   */
  public float ATHcurve;
  /**
   * lower ATH by this many db
   */
  public float ATHlower;
  /**
   * select ATH auto-adjust scheme
   */
  public int athaa_type;
  /**
   * select ATH auto-adjust loudness calc
   */
  public int athaa_loudapprox;
  /**
   * dB, tune active region of auto-level
   */
  public float athaa_sensitivity;
  public ShortBlock short_blocks;
  /**
   * use temporal masking effect
   */
  public Boolean useTemporal;
  public float interChRatio;
  /**
   * Naoki's adjustment of Mid/Side maskings
   */
  public float msfix;
  /**
   * 0 off, 1 on
   */
  public boolean tune;
  /**
   * used to pass values for debugging and stuff
   */
  public float tune_value_a;
  /**
   * number of samples of padding appended to input
   */
  public int encoder_padding;
  /**
   * number of frames encoded
   */
  public int frameNum;
  /**
   * is this struct owned by calling program or lame?
   */
  public int lame_allocated_gfp;
  /**
   * **********************************************************************
   */
  public LameInternalFlags internal_flags;
  /**
   * input number of channels. default=2
   */
  private int inNumChannels;
  /**
   * Input: Sampling rate in Hz (default=44100 - 44.1 kHz)
   */
  private int inSampleRate;
  /**
   * Output: Sampling rate in Hz (default=44100 - 44.1 kHz)
   * <p>
   * Default: LAME picks best value at least not used for MP3 decoding:
   * Remember 44.1 kHz MP3s and AC97
   */
  private int outSampleRate;
  /**
   * quality setting 0=best, 9=worst default=5
   */
  private int quality;
  /**
   * Mono/Stereo mode.
   */
  private MPEGMode mode = MPEGMode.STEREO;

	/* resampling and filtering */
  /**
   * find the RG value? default=0
   */
  private boolean findReplayGain;
  /**
   * true (default) writes ID3 tags, false not.
   */
  private boolean writeId3tagAutomatic;
  /**
   * bit rate (constant bit rate).
   * <p>
   * Note: set either bit rate>0 or compression ratio>0, LAME will compute the
   * value of the variable not set. Default is compression_ratio = 11.025
   */
  private int bitRate;
  /**
   * VBR control (variable bit rate)
   */
  private VbrMode VBR;

	/*
   * psycho acoustics and other arguments which you should not change unless
	 * you know what you are doing
	 */
  /**
   * Range [0,...,9]
   */
  private int VBRQuality;
  /**
   * MPEG version (0=MPEG-2/2.5 1=MPEG-1).
   */
  private int mpegVersion;
  /**
   * encoder delay.
   */
  private int encoderDelay;
  /**
   * Get frame size.
   */
  private int frameSize;

  /**
   * Input: Get number of channels (default=2)
   *
   * @return input number of channels (default=2)
   */
  public final int getInNumChannels() {
    return inNumChannels;
  }

  /**
   * Input: Set number of channels (default=2)
   *
   * @param inNumChannels input number of channels (default=2)
   */
  public final void setInNumChannels(final int inNumChannels) {
    this.inNumChannels = inNumChannels;
  }

  /**
   * Input: Get sampling rate.
   *
   * @return sampling rate in Hz (default=44100 - 44.1 kHz)
   */
  public final int getInSampleRate() {
    return inSampleRate;
  }

  /**
   * Input: Set sampling rate.
   *
   * @param inSampleRate sampling rate in Hz (default=44100 - 44.1 kHz)
   */
  public final void setInSampleRate(final int inSampleRate) {
    this.inSampleRate = inSampleRate;
  }

  /**
   * Output: Get sampling rate.
   *
   * @return sampling rate in Hz (default=44100 - 44.1 kHz)
   */
  public int getOutSampleRate() {
    return outSampleRate;
  }

  /**
   * Output: Set sampling rate.
   *
   * @param inSampleRate sampling rate in Hz (default=44100 - 44.1 kHz)
   */
  public void setOutSampleRate(int outSampleRate) {
    this.outSampleRate = outSampleRate;
  }

  /**
   * Get quality setting 0=best, 9=worst default=5.
   *
   * @return quality
   */
  public int getQuality() {
    return quality;
  }

  /**
   * Set quality setting 0=best, 9=worst default=5.
   *
   * @param quality quality setting 0=best, 9=worst default=5
   */
  public void setQuality(int quality) {
    this.quality = quality;
  }

  /**
   * Get Mono/Stereo mode.
   *
   * @return mono/Stereo mode.
   */
  public final MPEGMode getMode() {
    return mode;
  }

  /**
   * Set Mono/Stereo mode.
   *
   * @param mode Mono/Stereo mode.
   */
  public final void setMode(final MPEGMode mode) {
    this.mode = mode;
  }

  /**
   * Find the RG value? default=0
   *
   * @return find RG value
   */
  public boolean isFindReplayGain() {
    return findReplayGain;
  }

  /**
   * Set to find the RG value? default=0
   *
   * @param findReplayGain find the RG value
   */
  public void setFindReplayGain(boolean findReplayGain) {
    this.findReplayGain = findReplayGain;
  }

  /**
   * Turn off automatic writing of ID3 tag data into mp3 stream.
   *
   * @return true (default) writes ID3 tags, false not.
   */
  public final boolean isWriteId3tagAutomatic() {
    return writeId3tagAutomatic;
  }

  /************************************************************************/
	/* internal variables, do not set... */
	/* provided because they may be of use to calling application */
  /************************************************************************/

  /**
   * true (default) writes ID3 tags, false not.
   * <p>
   * Turn off automatic writing of ID3 tag data into mp3 stream we have to
   * call it before 'Lame.initParams', because that function would spit out
   * ID3v2 tag data.
   */
  public final void setWriteId3tagAutomatic(final boolean writeId3tagAutomatic) {
    this.writeId3tagAutomatic = writeId3tagAutomatic;
  }

  /**
   * Get bit rate (8-640 kbps constant bit rate).
   *
   * @return bitrate
   */
  public final int getBitRate() {
    return bitRate;
  }

  /**
   * Set bit rate (constant bit rate)
   *
   * @param bitRate
   */
  public final void setBitRate(final int bitRate) {
    this.bitRate = bitRate;
  }

  /**
   * Get VBR control (variable bit rate).
   *
   * @return VBR control
   */
  public final VbrMode getVBR() {
    return VBR;
  }

  /**
   * Set VBR control (variable bit rate).
   *
   * @param VBR control
   */
  public final void setVBR(final VbrMode vBR) {
    VBR = vBR;
  }

  /**
   * Get VBR quality.
   *
   * @return VBR quality
   */
  public final int getVBRQuality() {
    return VBRQuality;
  }

  /**
   * Set VBR quality.
   *
   * @param vBRQuality VBR quality
   */
  public final void setVBRQuality(final int vBRQuality) {
    VBRQuality = vBRQuality;
  }

  /**
   * Get MPEG version (0=MPEG-2/2.5 1=MPEG-1)
   *
   * @return MPEG version (0=MPEG-2/2.5 1=MPEG-1)
   */
  public int getMpegVersion() {
    return mpegVersion;
  }

  /**
   * Set MPEG version (0=MPEG-2/2.5 1=MPEG-1).
   *
   * @param mpegVersion MPEG version (0=MPEG-2/2.5 1=MPEG-1)
   */
  public void setMpegVersion(int mpegVersion) {
    this.mpegVersion = mpegVersion;
  }

  /**
   * Get encoder delay.
   *
   * @return encoder delay
   */
  public int getEncoderDelay() {
    return encoderDelay;
  }

  /**
   * Set encoder delay.
   *
   * @param encoderDelay encoder delay
   */
  public void setEncoderDelay(int encoderDelay) {
    this.encoderDelay = encoderDelay;
  }

  /**
   * Set frame size.
   *
   * @return frame size
   */
  public int getFrameSize() {
    return frameSize;
  }
  /**************************************************************************/
	/* more internal variables are stored in this structure: */

  /**
   * Set frame size.
   *
   * @param frameSize frame size
   */
  public void setFrameSize(int frameSize) {
    this.frameSize = frameSize;
  }

}
