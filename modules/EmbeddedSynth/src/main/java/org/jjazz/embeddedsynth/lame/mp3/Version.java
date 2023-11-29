package org.jjazz.embeddedsynth.lame.mp3;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

public class Version {

  /**
   * URL for the LAME website.
   */
  private static final String LAME_URL = "http://www.mp3dev.org/";

  /**
   * Major version number.
   */
  private static final int LAME_MAJOR_VERSION = 3;
  /**
   * Minor version number.
   */
  private static final int LAME_MINOR_VERSION = 98;
  /**
   * Patch level.
   */
  private static final int LAME_PATCH_VERSION = 4;

  /**
   * Major version number.
   */
  private static final int PSY_MAJOR_VERSION = 0;
  /**
   * Minor version number.
   */
  private static final int PSY_MINOR_VERSION = 93;

  /**
   * A string which describes the version of LAME.
   *
   * @return string which describes the version of LAME
   */
  public final String getLameVersion() {
    // primary to write screen reports
    return (LAME_MAJOR_VERSION + "." + LAME_MINOR_VERSION + "." + LAME_PATCH_VERSION);
  }

  /**
   * The short version of the LAME version string.
   *
   * @return short version of the LAME version string
   */
  public final String getLameShortVersion() {
    // Adding date and time to version string makes it harder for output
    // validation
    return (LAME_MAJOR_VERSION + "." + LAME_MINOR_VERSION + "." + LAME_PATCH_VERSION);
  }

  /**
   * The shortest version of the LAME version string.
   *
   * @return shortest version of the LAME version string
   */
  public final String getLameVeryShortVersion() {
    // Adding date and time to version string makes it harder for output
    return ("LAME" + LAME_MAJOR_VERSION + "." + LAME_MINOR_VERSION + "r");
  }

  /**
   * String which describes the version of GPSYCHO
   *
   * @return string which describes the version of GPSYCHO
   */
  public final String getPsyVersion() {
    return (PSY_MAJOR_VERSION + "." + PSY_MINOR_VERSION);
  }

  /**
   * Get version.
   */
  public final String getVersion() {
    final CharArrayWriter version = new CharArrayWriter();
    final PrintWriter pw = new PrintWriter(version);
    pw.printf("LAME 32bit version %s (%s)", getLameVersion(), LAME_URL);
    return version.toString();
  }

}
