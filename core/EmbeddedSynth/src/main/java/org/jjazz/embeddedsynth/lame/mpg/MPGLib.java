/**
 *      LAME MP3 encoding engine
 *
 *      Copyright (c) 1999-2000 Mark Taylor
 *      Copyright (c) 2003 Olcios
 *      Copyright (c) 2008 Robert Hegemann
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * @author Ken H�ndel
 */
package org.jjazz.embeddedsynth.lame.mpg;

import org.jjazz.embeddedsynth.lame.mp3.FrameSkip;
import org.jjazz.embeddedsynth.lame.mp3.MP3Data;
import org.jjazz.embeddedsynth.lame.mp3.PlottingData;

import java.util.List;

public class MPGLib {

  public final static int MP3_ERR = -1;
  public final static int MP3_OK = 0;
  final static int MP3_NEED_MORE = 1;

  ;
  private static final int smpls[][] = {
        /* Layer   I    II   III */
      {0, 384, 1152, 1152}, /* MPEG-1     */
      {0, 384, 1152, 576} /* MPEG-2(.5) */
  };
  private static final int OUTSIZE_CLIPPED = 4096;
  /* we forbid input with more than 1152 samples per channel for output in the unclipped mode */
  private final static int OUTSIZE_UNCLIPPED = 1152 * 2;
  Interface interf;

  public void setModules(Interface i) {
    interf = i;
  }

  /* copy mono samples */
  protected void COPY_MONO(float[] pcm_l, int pcm_lPos,
                           int processed_samples, float[] p) {
    int p_samples = 0;
    for (int i = 0; i < processed_samples; i++)
      pcm_l[pcm_lPos++] = p[p_samples++];
  }

  /* copy stereo samples */
  protected void COPY_STEREO(float[] pcm_l, int pcm_lPos, float[] pcm_r,
                             int pcm_rPos, int processed_samples, float[] p) {
    int p_samples = 0;
    for (int i = 0; i < processed_samples; i++) {
      pcm_l[pcm_lPos++] = p[p_samples++];
      pcm_r[pcm_rPos++] = p[p_samples++];
    }
  }

  private int decode1_headersB_clipchoice(mpstr_tag pmp, byte[] buffer,
                                          int bufferPos, int len, float[] pcm_l, int pcm_lPos, float[] pcm_r,
                                          int pcm_rPos, MP3Data mp3data, FrameSkip enc, float[] p, int psize,
                                          IDecoder decodeMP3_ptr) {

    mp3data.header_parsed = false;

    ProcessedBytes pb = new ProcessedBytes();
    int ret = decodeMP3_ptr.decode(pmp, buffer, bufferPos, len, p, psize, pb);
    int processed_samples = pb.pb; /* processed samples per channel */
        /* three cases:
         * 1. headers parsed, but data not complete
         *       pmp.header_parsed==1
         *       pmp.framesize=0
         *       pmp.fsizeold=size of last frame, or 0 if this is first frame
         *
         * 2. headers, data parsed, but ancillary data not complete
         *       pmp.header_parsed==1
         *       pmp.framesize=size of frame
         *       pmp.fsizeold=size of last frame, or 0 if this is first frame
         *
         * 3. frame fully decoded:
         *       pmp.header_parsed==0
         *       pmp.framesize=0
         *       pmp.fsizeold=size of frame (which is now the last frame)
         *
         */
    if (pmp.header_parsed || pmp.fsizeold > 0 || pmp.framesize > 0) {
      mp3data.header_parsed = true;
      mp3data.stereo = pmp.fr.stereo;
      mp3data.samplerate = Common.freqs[pmp.fr.sampling_frequency];
      mp3data.mode = pmp.fr.mode;
      mp3data.mode_ext = pmp.fr.mode_ext;
      mp3data.setFrameSize(smpls[pmp.fr.lsf][pmp.fr.lay]);

            /* free format, we need the entire frame before we can determine
             * the bitrate.  If we haven't gotten the entire frame, bitrate=0 */
      if (pmp.fsizeold > 0) /* works for free format and fixed, no overrun, temporal results are < 400.e6 */
        mp3data.bitrate = (int) (8 * (4 + pmp.fsizeold) * mp3data.samplerate /
            (1.e3 * mp3data.getFrameSize()) + 0.5);
      else if (pmp.framesize > 0)
        mp3data.bitrate = (int) (8 * (4 + pmp.framesize) * mp3data.samplerate /
            (1.e3 * mp3data.getFrameSize()) + 0.5);
      else
        mp3data.bitrate = Common.tabsel_123[pmp.fr.lsf][pmp.fr.lay - 1][pmp.fr.bitrate_index];


      if (pmp.num_frames > 0) {
                /* Xing VBR header found and num_frames was set */
        mp3data.setTotalFrames(pmp.num_frames);
        mp3data.setNumSamples(mp3data.getFrameSize() * pmp.num_frames);
        enc.setEncoderDelay(pmp.enc_delay);
        enc.setEncoderPadding(pmp.enc_padding);
      }
    }

    switch (ret) {
      case MP3_OK:
        switch (pmp.fr.stereo) {
          case 1:
            COPY_MONO(pcm_l, pcm_lPos, processed_samples, p);
            break;
          case 2:
            processed_samples = (processed_samples) >> 1;
            COPY_STEREO(pcm_l, pcm_lPos, pcm_r, pcm_rPos, processed_samples, p);
            break;
          default:
            processed_samples = -1;
            assert (false);
            break;
        }
        break;

      case MP3_NEED_MORE:
        processed_samples = 0;
        break;

      case MP3_ERR:
        processed_samples = -1;
        break;

      default:
        processed_samples = -1;
        assert (false);
        break;
    }

    return processed_samples;
  }

  public mpstr_tag hip_decode_init() {
    return interf.InitMP3();
  }

    /*
     * For lame_decode:  return code
     * -1     error
     *  0     ok, but need more data before outputing any samples
     *  n     number of samples output.  either 576 or 1152 depending on MP3 file.
     */

  public int hip_decode_exit(mpstr_tag hip) {
    if (hip != null) {
      interf.ExitMP3(hip);
      hip = null;
    }
    return 0;
  }

  /*
   * same as hip_decode1 (look in lame.h), but returns unclipped raw
   * floating-point samples. It is declared here, not in lame.h, because it
   * returns LAME's internal type sample_t. No more than 1152 samples per
   * channel are allowed.
   */
  public int hip_decode1_unclipped(mpstr_tag hip, byte[] buffer, int bufferPos,
                                   int len, final float pcm_l[], final float pcm_r[]) {

    MP3Data mp3data = new MP3Data();
    FrameSkip enc = new FrameSkip();

    if (hip != null) {
      IDecoder dec = new IDecoder() {

        @Override
        public int decode(mpstr_tag mp, byte[] in, int bufferPos, int isize,
                          float[] out, int osize, ProcessedBytes done) {
          return interf.decodeMP3_unclipped(mp, in, bufferPos, isize, out, osize, done);
        }
      };
      float[] out = new float[OUTSIZE_UNCLIPPED];
      return decode1_headersB_clipchoice(hip, buffer, bufferPos, len,
          pcm_l, 0, pcm_r, 0, mp3data, enc, out, OUTSIZE_UNCLIPPED,
          dec);
    }
    return 0;
  }

  /*
   * For lame_decode:  return code
   *  -1     error
   *   0     ok, but need more data before outputing any samples
   *   n     number of samples output.  Will be at most one frame of
   *         MPEG data.
   */
  public int
  hip_decode1_headers(mpstr_tag hip, byte[] buffer,
                      int len,
                      final float[] pcm_l, final float[] pcm_r, MP3Data mp3data,
                      FrameSkip enc) {
    if (hip != null) {
      IDecoder dec = new IDecoder() {

        @Override
        public int decode(mpstr_tag mp, byte[] in, int bufferPos, int isize,
                          float[] out, int osize, ProcessedBytes done) {
          return interf.decodeMP3(mp, in, bufferPos, isize, out, osize, done);
        }
      };
      float[] out = new float[OUTSIZE_CLIPPED];
      return decode1_headersB_clipchoice(hip, buffer, 0, len, pcm_l, 0,
          pcm_r, 0, mp3data, enc, out, OUTSIZE_CLIPPED, dec);
    }
    return -1;
  }

  void hip_set_pinfo(mpstr_tag hip, PlottingData pinfo) {
    if (hip != null) {
      hip.pinfo = pinfo;
    }
  }

  interface IDecoder {
    int decode(mpstr_tag mp, byte[] in, int bufferPos, int isize, float[] out, int osize, ProcessedBytes done);
  }

  public static class buf {
    byte[] pnt;
    int size;
    int pos;
  }

  public static class mpstr_tag {
    /**
     * Buffer linked list, first list entry points to oldest buffer.
     */
    List<buf> list;
    /**
     * Valid Xing vbr header detected?
     */
    boolean vbr_header;
    /**
     * Set if vbr header present.
     */
    int num_frames;
    /**
     * Set if vbr header present.
     */
    int enc_delay;
    /**
     * Set if vbr header present.
     */
    int enc_padding;
    /**
     * Header of current frame has been parsed.
     * <p>
     * Note: Header_parsed, side_parsed and data_parsed must be all set
     * before the full frame has been parsed.
     */
    boolean header_parsed;
    /**
     * Header of sideinfo of current frame has been parsed.
     * <p>
     * Note: Header_parsed, side_parsed and data_parsed must be all set
     * before the full frame has been parsed.
     */
    boolean side_parsed;
    /**
     * Note: Header_parsed, side_parsed and data_parsed must be all set
     * before the full frame has been parsed.
     */
    boolean data_parsed;
    /**
     * Free format frame?
     */
    boolean free_format;
    /**
     * Last frame was free format?
     */
    boolean old_free_format;
    int bsize;
    int framesize;
    /**
     * Number of bytes used for side information, including 2 bytes for
     * CRC-16 if present.
     */
    int ssize;
    int dsize;
    /**
     * Size of previous frame, -1 for first.
     */
    int fsizeold;
    int fsizeold_nopadding;
    /**
     * Holds the parameters decoded from the header.
     */
    Frame fr = new Frame();
    /**
     * Bit stream space used.
     */
    byte bsspace[][] = new byte[2][MPG123.MAXFRAMESIZE + 1024];
    float hybrid_block[][][] = new float[2][2][MPG123.SBLIMIT
        * MPG123.SSLIMIT];
    int hybrid_blc[] = new int[2];
    long header;
    int bsnum;
    float synth_buffs[][][] = new float[2][2][0x110];
    int synth_bo;
    /**
     * Bit-stream is yet to be synchronized.
     */
    boolean sync_bitstream;

    int bitindex;
    byte[] wordpointer;
    int wordpointerPos;
    PlottingData pinfo;
  }

  static class ProcessedBytes {
    int pb;
  }

}
