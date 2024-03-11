package org.jjazz.embeddedsynth.lame.mp3;

public class MP3Data {
  /**
   * true if header was parsed and following data was computed
   */
  public boolean header_parsed;
  /**
   * number of channels
   */
  public int stereo;
  /**
   * sample rate
   */
  public int samplerate;
  /**
   * bitrate
   */
  public int bitrate;
  /**
   * mp3 frame type
   */
  public int mode;
  /**
   * mp3 frame type
   */
  public int mode_ext;
  /**
   * Number of samples per mp3 frame.
   */
  private int frameSize;
  /**
   * Number of samples in mp3 file.
   */
  private int numSamples;
  /**
   * Total number of frames in mp3 file.
   */
  private int totalFrames;

	/* this data is only computed if mpglib detects a Xing VBR header */
  /**
   * Frames decoded counter
   */
  private int framesDecodedCounter;

  /**
   * Get number of samples per mp3 frame.
   *
   * @return number of samples per mp3 frame
   */
  public int getFrameSize() {
    return frameSize;
  }

  /**
   * Set number of samples per mp3 frame.
   *
   * @param frameSize number of samples per mp3 frame
   */
  public void setFrameSize(int frameSize) {
    this.frameSize = frameSize;
  }

  /**
   * Get number of samples in mp3 file.
   *
   * @return number of samples in mp3 file
   */
  public int getNumSamples() {
    return numSamples;
  }

  /**
   * Set number of samples in mp3 file.
   *
   * @param numSamples number of samples in mp3 file
   */
  public void setNumSamples(int numSamples) {
    this.numSamples = numSamples;
  }

  /**
   * Get total number of frames in mp3 file
   *
   * @return total number of frames in mp3 file
   */
  public int getTotalFrames() {
    return totalFrames;
  }

	/* this data is not currently computed by the mpglib routines */

  /**
   * Set Total number of frames in mp3 file.
   *
   * @param totalFrames total number of frames in mp3 file
   */
  public void setTotalFrames(int totalFrames) {
    this.totalFrames = totalFrames;
  }

  /**
   * Get frames decoded counter.
   *
   * @return frames decoded counter
   */
  public int getFramesDecodedCounter() {
    return framesDecodedCounter;
  }

  /**
   * Set frames decoded counter.
   *
   * @param framesDecodedCounter frames decoded counter
   */
  public void setFramesDecodedCounter(int framesDecodedCounter) {
    this.framesDecodedCounter = framesDecodedCounter;
  }

}
