package org.jjazz.embeddedsynth.lame.mp3;

public class HuffCodeTab {
  /**
   * max. x-index+
   */
  final int xlen;
  /**
   * max number to be stored in linbits
   */
  final int linmax;
  /**
   * pointer to array[xlen][ylen]
   */
  final int[] table;
  /**
   * pointer to array[xlen][ylen]
   */
  final int[] hlen;
  public HuffCodeTab(final int len, final int max, final int[] tab, final int[] hl) {
    xlen = len;
    linmax = max;
    table = tab;
    hlen = hl;
  }
}
