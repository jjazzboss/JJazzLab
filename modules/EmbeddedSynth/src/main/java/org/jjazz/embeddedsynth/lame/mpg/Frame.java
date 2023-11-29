package org.jjazz.embeddedsynth.lame.mpg;

import org.jjazz.embeddedsynth.lame.mpg.L2Tables.al_table2;

public class Frame {

  int stereo;
  int jsbound;
  /**
   * single channel (monophonic).
   */
  int single;
  /**
   * 0 = MPEG-1, 1 = MPEG-2/2.5
   */
  int lsf;
  /**
   * 1 = MPEG-2.5, 0 = MPEG-1/2
   */
  boolean mpeg25;
  /**
   * Layer
   */
  int lay;
  /**
   * 1 = CRC-16 code following header
   */
  boolean error_protection;
  int bitrate_index;
  /**
   * sample rate of decompressed audio in Hz
   */
  int sampling_frequency;
  int padding;
  int extension;
  int mode;
  int mode_ext;
  int copyright;
  int original;
  int emphasis;
  /**
   * computed framesize
   */
  int framesize;

  /**
   * AF: ADDED FOR LAYER1/LAYER2
   */
  int II_sblimit;
  al_table2[] alloc;
  int down_sample_sblimit;
  int down_sample;

}
