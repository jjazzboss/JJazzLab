/**
 * Mpeg Layer-1,2,3 audio decoder
 *
 * Copyright (C) 1999-2010 The L.A.M.E. project
 *
 * Initially written by Michael Hipp, see also AUTHORS and README.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * @author Ken H�ndel
 */
package org.jjazz.embeddedsynth.lame.mpg;

import org.jjazz.embeddedsynth.lame.mpg.MPGLib.ProcessedBytes;
import org.jjazz.embeddedsynth.lame.mpg.MPGLib.mpstr_tag;

public class Decode {

  private TabInit tab = new TabInit();
  private DCT64 dct64 = new DCT64();

  private int writeSampleClipped(final float sum, int clip,
                                 final float[] out, final int outPos) {
    if (sum > 32767.0) {
      out[outPos] = 32767;
      clip++;
    } else if (sum < -32768.0) {
      out[outPos] = -32768;
      clip++;
    } else {
      out[outPos] = (int) (sum > 0 ? sum + 0.5 : sum - 0.5);
    }
    return clip;
  }

  private void writeSampleUnclipped(final float sum, final float[] out,
                                    final int outPos) {
    out[outPos] = sum;
  }

  int synth1to1mono(final mpstr_tag mp, final float[] bandPtr,
                    final int bandPos, final float[] out, final ProcessedBytes pnt) {
    float[] samples = new float[64];

    int clip = synth_1to1(mp, bandPtr, bandPos, 0, samples,
        new ProcessedBytes());

    for (int i = 0; i < samples.length; i += 2) {
      out[pnt.pb++] = samples[i];
    }
    return clip;
  }

  void synth1to1monoUnclipped(final mpstr_tag mp, final float[] bandPtr,
                              final int bandPos, final float[] out, final ProcessedBytes pnt) {
    float[] samples = new float[64];

    synth_1to1_unclipped(mp, bandPtr, bandPos, 0, samples,
        new ProcessedBytes());

    for (int i = 0; i < samples.length; i += 2) {
      out[pnt.pb++] = samples[i];
    }
  }

  int synth_1to1(mpstr_tag mp, float[] bandPtr, int bandPos, int ch,
                 float[] out, ProcessedBytes pnt) {
    float[] b0;
    int clip = 0;
    int bo1;

    if (0 == ch) {
      mp.synth_bo--;
      mp.synth_bo &= 0xf;
    } else {
      pnt.pb++;
    }

    if ((mp.synth_bo & 0x1) != 0) {
      b0 = mp.synth_buffs[ch][0];
      bo1 = mp.synth_bo;
      float bufs[] = new float[0x40];
      dct64.dct64_1(mp.synth_buffs[ch][1], ((mp.synth_bo + 1) & 0xf),
          mp.synth_buffs[ch][0], mp.synth_bo, bufs, 0x20, bandPtr,
          bandPos, tab.pnts);
    } else {
      b0 = mp.synth_buffs[ch][1];
      bo1 = mp.synth_bo + 1;
      float bufs[] = new float[0x40];
      dct64.dct64_1(mp.synth_buffs[ch][0], mp.synth_bo,
          mp.synth_buffs[ch][1], mp.synth_bo + 1, bufs, 0x20,
          bandPtr, bandPos, tab.pnts);
    }

    {
      int window = 16 - bo1;

      int b0Pos = 0;
      for (int j = 16; j != 0; j--, b0Pos += 0x10, window += 0x20, pnt.pb += 2) {
        float sum;
        sum = tab.decwin[window + 0x0] * b0[b0Pos + 0x0];
        sum -= tab.decwin[window + 0x1] * b0[b0Pos + 0x1];
        sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2];
        sum -= tab.decwin[window + 0x3] * b0[b0Pos + 0x3];
        sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4];
        sum -= tab.decwin[window + 0x5] * b0[b0Pos + 0x5];
        sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6];
        sum -= tab.decwin[window + 0x7] * b0[b0Pos + 0x7];
        sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8];
        sum -= tab.decwin[window + 0x9] * b0[b0Pos + 0x9];
        sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA];
        sum -= tab.decwin[window + 0xB] * b0[b0Pos + 0xB];
        sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC];
        sum -= tab.decwin[window + 0xD] * b0[b0Pos + 0xD];
        sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE];
        sum -= tab.decwin[window + 0xF] * b0[b0Pos + 0xF];
        clip = writeSampleClipped(sum, clip, out, pnt.pb);
      }

      {
        float sum;
        sum = tab.decwin[window + 0x0] * b0[b0Pos + 0x0];
        sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2];
        sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4];
        sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6];
        sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8];
        sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA];
        sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC];
        sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE];
        clip = writeSampleClipped(sum, clip, out, pnt.pb);
        b0Pos -= 0x10;
        window -= 0x20;
        pnt.pb += 2;
      }
      window += bo1 << 1;

      for (int j = 15; j != 0; j--, b0Pos -= 0x10, window -= 0x20, pnt.pb += 2) {
        float sum;
        sum = -tab.decwin[window + -0x1] * b0[b0Pos + 0x0];
        sum -= tab.decwin[window + -0x2] * b0[b0Pos + 0x1];
        sum -= tab.decwin[window + -0x3] * b0[b0Pos + 0x2];
        sum -= tab.decwin[window + -0x4] * b0[b0Pos + 0x3];
        sum -= tab.decwin[window + -0x5] * b0[b0Pos + 0x4];
        sum -= tab.decwin[window + -0x6] * b0[b0Pos + 0x5];
        sum -= tab.decwin[window + -0x7] * b0[b0Pos + 0x6];
        sum -= tab.decwin[window + -0x8] * b0[b0Pos + 0x7];
        sum -= tab.decwin[window + -0x9] * b0[b0Pos + 0x8];
        sum -= tab.decwin[window + -0xA] * b0[b0Pos + 0x9];
        sum -= tab.decwin[window + -0xB] * b0[b0Pos + 0xA];
        sum -= tab.decwin[window + -0xC] * b0[b0Pos + 0xB];
        sum -= tab.decwin[window + -0xD] * b0[b0Pos + 0xC];
        sum -= tab.decwin[window + -0xE] * b0[b0Pos + 0xD];
        sum -= tab.decwin[window + -0xF] * b0[b0Pos + 0xE];
        sum -= tab.decwin[window + -0x0] * b0[b0Pos + 0xF];

        clip = writeSampleClipped(sum, clip, out, pnt.pb);
      }
    }
    if (ch == 1) {
      pnt.pb--;
    }
    return clip;
  }

  void synth_1to1_unclipped(mpstr_tag mp, float[] bandPtr, int bandPos,
                            int ch, float[] out, ProcessedBytes pnt) {
    float[] b0;
    int bo1;

    if (0 == ch) {
      mp.synth_bo--;
      mp.synth_bo &= 0xf;
    } else {
      pnt.pb++;
    }

    if ((mp.synth_bo & 0x1) != 0) {
      b0 = mp.synth_buffs[ch][0];
      bo1 = mp.synth_bo;
      float bufs[] = new float[0x40];
      dct64.dct64_1(mp.synth_buffs[ch][1], ((mp.synth_bo + 1) & 0xf),
          mp.synth_buffs[ch][0], mp.synth_bo, bufs, 0x20, bandPtr,
          bandPos, tab.pnts);
    } else {
      b0 = mp.synth_buffs[ch][1];
      bo1 = mp.synth_bo + 1;
      float bufs[] = new float[0x40];
      dct64.dct64_1(mp.synth_buffs[ch][0], mp.synth_bo,
          mp.synth_buffs[ch][1], mp.synth_bo + 1, bufs, 0x20,
          bandPtr, bandPos, tab.pnts);
    }

    {
      int window = 16 - bo1;

      int b0Pos = 0;
      for (int j = 16; j != 0; j--, b0Pos += 0x10, window += 0x20, pnt.pb += 2) {
        float sum;
        sum = tab.decwin[window + 0x0] * b0[b0Pos + 0x0];
        sum -= tab.decwin[window + 0x1] * b0[b0Pos + 0x1];
        sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2];
        sum -= tab.decwin[window + 0x3] * b0[b0Pos + 0x3];
        sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4];
        sum -= tab.decwin[window + 0x5] * b0[b0Pos + 0x5];
        sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6];
        sum -= tab.decwin[window + 0x7] * b0[b0Pos + 0x7];
        sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8];
        sum -= tab.decwin[window + 0x9] * b0[b0Pos + 0x9];
        sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA];
        sum -= tab.decwin[window + 0xB] * b0[b0Pos + 0xB];
        sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC];
        sum -= tab.decwin[window + 0xD] * b0[b0Pos + 0xD];
        sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE];
        sum -= tab.decwin[window + 0xF] * b0[b0Pos + 0xF];
        writeSampleUnclipped(sum, out, pnt.pb);
      }

      {
        float sum;
        sum = tab.decwin[window + 0x0] * b0[b0Pos + 0x0];
        sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2];
        sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4];
        sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6];
        sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8];
        sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA];
        sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC];
        sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE];
        writeSampleUnclipped(sum, out, pnt.pb);
        b0Pos -= 0x10;
        window -= 0x20;
        pnt.pb += 2;
      }
      window += bo1 << 1;

      for (int j = 15; j != 0; j--, b0Pos -= 0x10, window -= 0x20, pnt.pb += 2) {
        float sum;
        sum = -tab.decwin[window + -0x1] * b0[b0Pos + 0x0];
        sum -= tab.decwin[window + -0x2] * b0[b0Pos + 0x1];
        sum -= tab.decwin[window + -0x3] * b0[b0Pos + 0x2];
        sum -= tab.decwin[window + -0x4] * b0[b0Pos + 0x3];
        sum -= tab.decwin[window + -0x5] * b0[b0Pos + 0x4];
        sum -= tab.decwin[window + -0x6] * b0[b0Pos + 0x5];
        sum -= tab.decwin[window + -0x7] * b0[b0Pos + 0x6];
        sum -= tab.decwin[window + -0x8] * b0[b0Pos + 0x7];
        sum -= tab.decwin[window + -0x9] * b0[b0Pos + 0x8];
        sum -= tab.decwin[window + -0xA] * b0[b0Pos + 0x9];
        sum -= tab.decwin[window + -0xB] * b0[b0Pos + 0xA];
        sum -= tab.decwin[window + -0xC] * b0[b0Pos + 0xB];
        sum -= tab.decwin[window + -0xD] * b0[b0Pos + 0xC];
        sum -= tab.decwin[window + -0xE] * b0[b0Pos + 0xD];
        sum -= tab.decwin[window + -0xF] * b0[b0Pos + 0xE];
        sum -= tab.decwin[window + -0x0] * b0[b0Pos + 0xF];

        writeSampleUnclipped(sum, out, pnt.pb);
      }
    }
    if (ch == 1) {
      pnt.pb--;
    }
  }
}
