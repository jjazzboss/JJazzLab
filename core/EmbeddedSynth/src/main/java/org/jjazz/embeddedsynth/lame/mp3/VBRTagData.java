package org.jjazz.embeddedsynth.lame.mp3;

/**
 * Structure to receive extracted header (toc may be null).
 *
 * @author Ken
 */
public class VBRTagData {
  /**
   * Total bit stream frames from Vbr header data.
   */
  public int frames;
  /**
   * Size of VBR header, in bytes.
   */
  public int headersize;
  /**
   * Encoder delay.
   */
  public int encDelay;
  /**
   * Encoder padding added at end of stream.
   */
  public int encPadding;
  /**
   * From MPEG header 0=MPEG2, 1=MPEG1.
   */
  protected int hId;
  /**
   * Sample rate determined from MPEG header.
   */
  protected int samprate;
  /**
   * From Vbr header data.
   */
  protected int flags;
  /**
   * Total bit stream bytes from Vbr header data.
   */
  protected int bytes;
  /**
   * Encoded vbr scale from Vbr header data.
   */
  protected int vbrScale;
  /**
   * May be null if toc not desired.
   */
  protected byte[] toc = new byte[VBRTag.NUMTOCENTRIES];
}
