package org.jjazz.embeddedsynth.lame.mp3;

public class FrameSkip {
  /**
   * Skip initial samples.
   */
  private int encoderDelay = -1;
  /**
   * Skip final samples.
   */
  private int encoderPadding = -1;

  /**
   * Get initial samples to skip.
   *
   * @return initial samples to skip
   */
  public final int getEncoderDelay() {
    return encoderDelay;
  }

  /**
   * Set initial samples to skip.
   *
   * @param encoderDelay initial samples to skip
   */
  public final void setEncoderDelay(final int encoderDelay) {
    this.encoderDelay = encoderDelay;
  }

  /**
   * Get final samples to skip.
   *
   * @return final samples to skip
   */
  public final int getEncoderPadding() {
    return encoderPadding;
  }

  /**
   * Set final samples to skip.
   *
   * @param enccoderPadding final samples to skip
   */
  public final void setEncoderPadding(int enccoderPadding) {
    this.encoderPadding = enccoderPadding;
  }

}
