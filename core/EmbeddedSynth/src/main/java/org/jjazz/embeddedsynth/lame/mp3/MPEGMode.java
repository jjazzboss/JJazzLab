package org.jjazz.embeddedsynth.lame.mp3;


/* MPEG modes */
public enum MPEGMode {
  STEREO(0), JOINT_STEREO(1),
  /**
   * LAME doesn't supports this!
   */
  DUAL_CHANNEL(2), MONO(3), NOT_SET(-1);

  private int mode;

  /**
   * Set Mono/Stereo mode.
   *
   * @param md Mono/Stereo mode
   */
  private MPEGMode(final int md) {
    mode = md;
  }

  /**
   * Get numerical Mono/Stereo mode.
   *
   * @return numerical Mono/Stereo mode
   */
  public final int getNumMode() {
    return mode;
  }

}
