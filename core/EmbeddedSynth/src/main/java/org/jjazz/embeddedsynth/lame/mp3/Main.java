/*
 *      Command line frontend program
 *
 *      Copyright (c) 1999 Mark Taylor
 *                    2000 Takehiro TOMINAGA
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
 */

/* $Id: Main.java,v 1.47 2012/03/23 10:02:29 kenchis Exp $ */

package org.jjazz.embeddedsynth.lame.mp3;

import org.jjazz.embeddedsynth.lame.mp3.GetAudio.SoundFileFormat;

import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

public class Main {

  private static final int MAX_NOGAP = 200;
  private static final int MAX_WIDTH = 79;
  private Lame lame;
  private Usage usage = new Usage();
  private Version version = new Version();
  private PropertyChangeSupport support = new PropertyChangeSupport(this);
  private double last_time = 0.0;
  private int oldPercent, curPercent, oldConsoleX;

  public static final void main(final String[] args) {
    try {
      new Main().run(args);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public PropertyChangeSupport getSupport() {
    return support;
  }

  /**
   * PURPOSE: MPEG-1,2 Layer III encoder with GPSYCHO psychoacoustic model.
   */
  private int parse_args_from_string(
      final String argv, final StringBuilder inPath,
      final StringBuilder outPath) {
    /* Quick & very Dirty */
    if (argv == null || argv.length() == 0)
      return 0;

    StringTokenizer tok = new StringTokenizer(argv, " ");
    ArrayList<String> args = new ArrayList<String>();
    while (tok.hasMoreTokens()) {
      args.add(tok.nextToken());
    }
    return lame.getParser().parse_args(lame.getFlags(), args, inPath, outPath, null, null);
  }

  private DataOutput init_files(
      final String inPath, final String outPath, final FrameSkip enc) {
		/*
		 * Mostly it is not useful to use the same input and output name. This
		 * test is very easy and buggy and don't recognize different names
		 * assigning the same file
		 */
    if (inPath.equals(outPath)) {
      System.err
          .println("Input file and Output file are the same. Abort.");
      return null;
    }

		/*
		 * open the wav/aiff/raw pcm or mp3 input file. This call will open the
		 * file, try to parse the headers and set gf.samplerate,
		 * gf.num_channels, gf.num_samples. if you want to do your own file
		 * input, skip this call and set samplerate, num_channels and
		 * num_samples yourself.
		 */
    lame.getAudio().initInFile(lame.getFlags(), inPath, enc);

    try {
      FileOutputStream fout = new FileOutputStream(outPath);
      return new DataOutputStream(new BufferedOutputStream(fout, 1 << 20));
    } catch (FileNotFoundException e) {
      System.err.printf("Can't init outfile '%s'\n", outPath);
      return null;
    }
  }

  /**
   * the simple lame decoder
   * <p>
   * After calling lame_init(), Lame.initParams() and init_infile(), call
   * this routine to read the input MP3 file and output .wav data to the
   * specified file pointer
   * <p>
   * lame_decoder will ignore the first 528 samples, since these samples
   * represent the mpglib delay (and are all 0). skip = number of additional
   * samples to skip, to (for example) compensate for the encoder delay
   */
  private void lame_decoder(
      DataOutput outf, int skip_start, final String inPath,
      final String outPath, final FrameSkip enc) throws IOException {
    float Buffer[][] = new float[2][1152];
    int iread;
    int skip_end = 0;
    int i;
    int tmp_num_channels = lame.getFlags().getInNumChannels();

    if (lame.getParser().silent < 10)
      System.out.printf("\rinput:  %s%s(%g kHz, %d channel%s, ", inPath,
          inPath.length() > 26 ? "\n\t" : "  ",
          lame.getFlags().getInSampleRate() / 1.e3, tmp_num_channels,
          tmp_num_channels != 1 ? "s" : "");

    switch (lame.getParser().getInputFormat()) {
      case sf_mp123: /* FIXME: !!! */
        throw new RuntimeException("Internal error.  Aborting.");

      case sf_mp3:
        if (skip_start == 0) {
          if (enc.getEncoderDelay() > -1 || enc.getEncoderPadding() > -1) {
            if (enc.getEncoderDelay() > -1)
              skip_start = enc.getEncoderDelay() + 528 + 1;
            if (enc.getEncoderPadding() > -1)
              skip_end = enc.getEncoderPadding() - (528 + 1);
          } else
            skip_start = lame.getFlags().getEncoderDelay() + 528 + 1;
        } else {
				/* user specified a value of skip. just add for decoder */
          skip_start += 528 + 1;
				/*
				 * mp3 decoder has a 528 sample delay, plus user supplied "skip"
				 */
        }

        if (lame.getParser().silent < 10)
          System.out.printf("MPEG-%d%s Layer %s", 2 - lame.getFlags().getMpegVersion(),
              lame.getFlags().getOutSampleRate() < 16000 ? ".5" : "", "III");
        break;
      case sf_mp2:
        skip_start += 240 + 1;
        if (lame.getParser().silent < 10)
          System.out.printf("MPEG-%d%s Layer %s", 2 - lame.getFlags().getMpegVersion(),
              lame.getFlags().getOutSampleRate() < 16000 ? ".5" : "", "II");
        break;
      case sf_mp1:
        skip_start += 240 + 1;
        if (lame.getParser().silent < 10)
          System.out.printf("MPEG-%d%s Layer %s", 2 - lame.getFlags().getMpegVersion(),
              lame.getFlags().getOutSampleRate() < 16000 ? ".5" : "", "I");
        break;
      case sf_raw:
        if (lame.getParser().silent < 10)
          System.out.printf("raw PCM data");
        lame.getParser().getMp3InputData().setNumSamples(lame.getFlags().num_samples);
        lame.getParser().getMp3InputData().setFrameSize(1152);
        skip_start = 0;
			/* other formats have no delay */
        break;
      case sf_wave:
        if (lame.getParser().silent < 10)
          System.out.printf("Microsoft WAVE");
        lame.getParser().getMp3InputData().setNumSamples(lame.getFlags().num_samples);
        lame.getParser().getMp3InputData().setFrameSize(1152);
        skip_start = 0;
			/* other formats have no delay */
        break;
      case sf_aiff:
        if (lame.getParser().silent < 10)
          System.out.printf("SGI/Apple AIFF");
        lame.getParser().getMp3InputData().setNumSamples(lame.getFlags().num_samples);
        lame.getParser().getMp3InputData().setFrameSize(1152);
        skip_start = 0;
			/* other formats have no delay */
        break;
      default:
        if (lame.getParser().silent < 10)
          System.out.printf("unknown");
        lame.getParser().getMp3InputData().setNumSamples(lame.getFlags().num_samples);
        lame.getParser().getMp3InputData().setFrameSize(1152);
        skip_start = 0;
			/* other formats have no delay */
        assert (false);
        break;
    }

    if (lame.getParser().silent < 10) {
      System.out.printf(")\noutput: %s%s(16 bit, Microsoft WAVE)\n",
          outPath, outPath.length() > 45 ? "\n\t" : "  ");

      if (skip_start > 0)
        System.out
            .printf("skipping initial %d samples (encoder+decoder delay)\n",
                skip_start);
      if (skip_end > 0)
        System.out
            .printf("skipping final %d samples (encoder padding-decoder delay)\n",
                skip_end);
    }

    if (!lame.getParser().embedded) {
      System.out.print("|");
      for (int j = 0; j < MAX_WIDTH - 2; j++) {
        System.out.print("=");
      }
      System.out.println("|");
    }
    oldPercent = curPercent = oldConsoleX = 0;

    if (!lame.getParser().disable_wav_header)
      lame.getAudio().WriteWaveHeader(outf, Integer.MAX_VALUE, lame.getFlags().getInSampleRate(),
          tmp_num_channels, 16);
		/* unknown size, so write maximum 32 bit signed value */

    double wavsize = -(skip_start + skip_end);
    lame.getParser().getMp3InputData().setTotalFrames(lame.getParser().getMp3InputData().getNumSamples()
        / lame.getParser().getMp3InputData().getFrameSize());

    assert (tmp_num_channels >= 1 && tmp_num_channels <= 2);

    do {
      iread = lame.getAudio().get_audio16(lame.getFlags(), Buffer);
			/* read in 'iread' samples */
      if (iread >= 0) {
        lame.getParser().getMp3InputData()
            .setFramesDecodedCounter(
                lame.getParser().getMp3InputData().getFramesDecodedCounter() + iread
                    / lame.getParser().getMp3InputData().getFrameSize());
        wavsize += iread;

        if (lame.getParser().silent <= 0 || lame.getParser().embedded) {
          timestatus(lame.getParser().getMp3InputData().getFramesDecodedCounter(),
              lame.getParser().getMp3InputData().getTotalFrames());
        }

        skip_start -= (i = skip_start < iread ? skip_start : iread);
				/*
				 * 'i' samples are to skip in this frame
				 */

        if (skip_end > 1152
            && lame.getParser().getMp3InputData().getFramesDecodedCounter() + 2 > lame.getParser().getMp3InputData().getTotalFrames()) {
          iread -= (skip_end - 1152);
          skip_end = 1152;
        } else if (lame.getParser().getMp3InputData().getFramesDecodedCounter() == lame.getParser().getMp3InputData().getTotalFrames()
            && iread != 0)
          iread -= skip_end;

        for (; i < iread; i++) {
          if (lame.getParser().disable_wav_header) {
            if (lame.getParser().swapbytes) {
              WriteBytesSwapped(outf, Buffer[0], i);
            } else {
              WriteBytes(outf, Buffer[0], i);
            }
            if (tmp_num_channels == 2) {
              if (lame.getParser().swapbytes) {
                WriteBytesSwapped(outf, Buffer[1], i);
              } else {
                WriteBytes(outf, Buffer[1], i);
              }
            }
          } else {
            lame.getAudio().write16BitsLowHigh(outf, (int) Buffer[0][i] & 0xffff);
            if (tmp_num_channels == 2)
              lame.getAudio().write16BitsLowHigh(outf, (int) Buffer[1][i] & 0xffff);
          }
        }
      }
    } while (iread > 0);

    if (lame.getParser().silent <= 0) {
      for (i = curPercent; i < 100; i++) {
        progressStep(i);
        curPercent++;
      }
      System.out.println();
    }

    i = (16 / 8) * tmp_num_channels;
    assert (i > 0);
    if (wavsize <= 0) {
      if (lame.getParser().silent < 10)
        System.err.println("WAVE file contains 0 PCM samples");
      wavsize = 0;
    } else if (wavsize > 0xFFFFFFD0L / i) {
      if (lame.getParser().silent < 10)
        System.err
            .println("Very huge WAVE file, can't set filesize accordingly");
      wavsize = 0xFFFFFFD0;
    } else {
      wavsize *= i;
    }

    ((Closeable) outf).close();
		/* if outf is seekable, rewind and adjust length */
    if (!lame.getParser().disable_wav_header) {
      RandomAccessFile rf = new RandomAccessFile(outPath, "rw");
      lame.getAudio().WriteWaveHeader(rf, (int) wavsize, lame.getFlags().getInSampleRate(),
          tmp_num_channels, 16);
      rf.close();
    }

    if (!lame.getParser().embedded) {
      System.out.print("|");
      for (int j = 0; j < MAX_WIDTH - 2; j++) {
        System.out.print("=");
      }
      System.out.println("|");
    }
  }

  private void print_lame_tag_leading_info() {
    if (lame.getFlags().bWriteVbrTag)
      System.out.println("Writing LAME Tag...");
  }

  private void print_trailing_info() {
    if (lame.getFlags().bWriteVbrTag)
      System.out.println("done\n");

    if (lame.getFlags().isFindReplayGain()) {
      int RadioGain = lame.getFlags().internal_flags.RadioGain;
      System.out.printf("ReplayGain: %s%.1fdB\n", RadioGain > 0 ? "+"
          : "", (RadioGain) / 10.0f);
      if (RadioGain > 0x1FE || RadioGain < -0x1FE)
        System.out
            .println("WARNING: ReplayGain exceeds the -51dB to +51dB range. Such a result is too\n"
                + "         high to be stored in the header.");
    }

		/*
		 * if (the user requested printing info about clipping) and (decoding on
		 * the fly has actually been performed)
		 */
    if (lame.getParser().print_clipping_info && lame.getFlags().decode_on_the_fly) {
      float noclipGainChange = (float) lame.getFlags().internal_flags.noclipGainChange / 10.0f;
      float noclipScale = lame.getFlags().internal_flags.noclipScale;

      if (noclipGainChange > 0.0) {
				/* clipping occurs */
        System.out
            .printf("WARNING: clipping occurs at the current gain. Set your decoder to decrease\n"
                    + "         the  gain  by  at least %.1fdB or encode again ",
                noclipGainChange);

				/* advice the user on the scale factor */
        if (noclipScale > 0) {
          System.out.printf(Locale.US, "using  --scale %.2f\n", noclipScale);
          System.out
              .print("         or less (the value under --scale is approximate).\n");
        } else {
					/*
					 * the user specified his own scale factor. We could suggest
					 * the scale factor of
					 * (32767.0/gfp->PeakSample)*(gfp->scale) but it's usually
					 * very inaccurate. So we'd rather advice him to disable
					 * scaling first and see our suggestion on the scale factor
					 * then.
					 */
          System.out
              .print("using --scale <arg>\n"
                  + "         (For   a   suggestion  on  the  optimal  value  of  <arg>  encode\n"
                  + "         with  --scale 1  first)\n");
        }

      } else { /* no clipping */
        if (noclipGainChange > -0.1)
          System.out
              .print("\nThe waveform does not clip and is less than 0.1dB away from full scale.\n");
        else
          System.out
              .printf("\nThe waveform does not clip and is at least %.1fdB away from full scale.\n",
                  -noclipGainChange);
      }
    }

  }

  private int write_xing_frame(final RandomAccessFile outf) {
    byte mp3buffer[] = new byte[Lame.LAME_MAXMP3BUFFER];

    int imp3 = lame.getVbr().getLameTagFrame(lame.getFlags(), mp3buffer);
    if (imp3 > mp3buffer.length) {
      System.err
          .printf("Error writing LAME-tag frame: buffer too small: buffer size=%d  frame size=%d\n",
              mp3buffer.length, imp3);
      return -1;
    }
    if (imp3 <= 0) {
      return 0;
    }
    try {
      outf.write(mp3buffer, 0, imp3);
    } catch (IOException e) {
      System.err.println("Error writing LAME-tag");
      return -1;
    }
    return imp3;
  }

  private int lame_encoder(
      final DataOutput outf, final boolean nogap,
      final String inPath, final String outPath) {
    byte mp3buffer[] = new byte[Lame.LAME_MAXMP3BUFFER];
    float Buffer[][] = new float[2][1152];
    int iread;

    encoder_progress_begin(inPath, outPath);

    int imp3 = lame.getId3().lame_get_id3v2_tag(lame.getFlags(), mp3buffer, mp3buffer.length);
    if (imp3 > mp3buffer.length) {
      encoder_progress_end();
      System.err
          .printf("Error writing ID3v2 tag: buffer too small: buffer size=%d  ID3v2 size=%d\n",
              mp3buffer.length, imp3);
      return 1;
    }
    try {
      outf.write(mp3buffer, 0, imp3);
    } catch (IOException e) {
      encoder_progress_end();
      System.err.printf("Error writing ID3v2 tag \n");
      return 1;
    }
    int id3v2_size = imp3;

		/* encode until we hit eof */
    do {
			/* read in 'iread' samples */
      iread = lame.getAudio().get_audio(lame.getFlags(), Buffer);

      if (iread >= 0) {
        encoder_progress();

				/* encode */
        imp3 = lame
            .encodeBuffer(Buffer[0], Buffer[1], iread, mp3buffer);

				/* was our output buffer big enough? */
        if (imp3 < 0) {
          if (imp3 == -1)
            System.err.printf("mp3 buffer is not big enough... \n");
          else
            System.err.printf(
                "mp3 internal error:  error code=%d\n", imp3);
          return 1;
        }

        try {
          outf.write(mp3buffer, 0, imp3);
        } catch (IOException e) {
          encoder_progress_end();
          System.err.printf("Error writing mp3 output \n");
          return 1;
        }
      }
    } while (iread > 0);

    if (nogap)
      imp3 = lame
          .lame_encode_flush_nogap(mp3buffer, mp3buffer.length);
		/*
		 * may return one more mp3 frame
		 */
    else
      imp3 = lame.encodeFlush(mp3buffer);
		/*
		 * may return one more mp3 frame
		 */

    if (imp3 < 0) {
      if (imp3 == -1)
        System.err.printf("mp3 buffer is not big enough... \n");
      else
        System.err.printf("mp3 internal error:  error code=%d\n", imp3);
      return 1;

    }

    encoder_progress_end();

    try {
      outf.write(mp3buffer, 0, imp3);
    } catch (IOException e) {
      encoder_progress_end();
      System.err.printf("Error writing mp3 output \n");
      return 1;
    }

    imp3 = lame.getId3().lame_get_id3v1_tag(lame.getFlags(), mp3buffer, mp3buffer.length);
    if (imp3 > mp3buffer.length) {
      System.err
          .printf("Error writing ID3v1 tag: buffer too small: buffer size=%d  ID3v1 size=%d\n",
              mp3buffer.length, imp3);
    } else {
      if (imp3 > 0) {
        try {
          outf.write(mp3buffer, 0, imp3);
        } catch (IOException e) {
          encoder_progress_end();
          System.err.printf("Error writing ID3v1 tag \n");
          return 1;
        }
      }
    }

    if (lame.getParser().silent <= 0) {
      print_lame_tag_leading_info();
    }
    try {
      ((Closeable) outf).close();
      RandomAccessFile rf = new RandomAccessFile(outPath, "rw");
      rf.seek(id3v2_size);
      write_xing_frame(rf);
      rf.close();
    } catch (IOException e) {
      System.err.printf("fatal error: can't update LAME-tag frame!\n");
    }

    print_trailing_info();
    return 0;
  }

  private void brhist_init_package() {
    if (lame.getParser().brhist) {
      if (lame.getHist().brhist_init(lame.getFlags(), lame.getFlags().VBR_min_bitrate_kbps,
          lame.getFlags().VBR_max_bitrate_kbps) != 0) {
				/* fail to initialize */
        lame.getParser().brhist = false;
      }
    } else {
      lame.getHist().brhist_init(lame.getFlags(), 128, 128);
			/* Dirty hack */
    }
  }

  private void parse_nogap_filenames(final int nogapout, final String inPath,
                                     final StringBuilder outPath, final StringBuilder outdir) {
    outPath.setLength(0);
    outPath.append(outdir);
    if (0 == nogapout) {
      outPath.setLength(0);
      outPath.append(inPath);
			/* nuke old extension, if one */
      if (outPath.toString().endsWith(".wav")) {
        outPath.setLength(0);
        outPath.append(outPath.substring(0, outPath.length() - 4)
            + ".mp3");
      } else {
        outPath.setLength(0);
        outPath.append(outPath + ".mp3");
      }
    } else {
      int slasher = inPath.lastIndexOf(System
          .getProperty("file.separator"));

			/* backseek to last dir delemiter */

			/* skip one foward if needed */
      if (slasher != 0
          && (outPath.toString().endsWith(
          System.getProperty("file.separator")) || outPath
          .toString().endsWith(":")))
        slasher++;
      else if (slasher == 0
          && (!outPath.toString().endsWith(
          System.getProperty("file.separator")) || outPath
          .toString().endsWith(":")))
        outPath.append(System.getProperty("file.separator"));

      outPath.append(inPath.substring(slasher));
			/* nuke old extension */
      if (outPath.toString().endsWith(".wav")) {
        String string = outPath.substring(0, outPath.length() - 4)
            + ".mp3";
        outPath.setLength(0);
        outPath.append(string);
      } else {
        String string = outPath + ".mp3";
        outPath.setLength(0);
        outPath.append(string);
      }
    }
  }

  public int run(String[] args) throws IOException {
    // Create Encoder
    lame = new Lame();
    // Set parameters
    StringBuilder outPath = new StringBuilder();
    StringBuilder nogapdir = new StringBuilder();
    StringBuilder inPath = new StringBuilder();
		/* add variables for encoder delay/padding */
    FrameSkip enc = new FrameSkip();
		/* support for "nogap" encoding of up to 200 .wav files */
    int nogapout = 0;
    int max_nogap = MAX_NOGAP;
    String[] nogap_inPath = new String[max_nogap];
    DataOutput outf;
		/* initialize libmp3lame */
    lame.getParser().setInputFormat(SoundFileFormat.sf_unknown);
    if (args.length < 1) {
      System.err.println(version.getVersion());
      System.err.println();
      usage.print(System.err);
			/*
			 * no command-line args, print usage, exit
			 */
      lame.close();
      return 1;
    }
		/*
		 * parse the command line arguments, setting various flags in the struct
		 * 'gf'. If you want to parse your own arguments, or call libmp3lame
		 * from a program which uses a GUI to set arguments, skip this call and
		 * set the values of interest in the gf struct. (see the file API and
		 * lame.h for documentation about these parameters)
		 */
    parse_args_from_string(System.getenv("LAMEOPT"), inPath, outPath);
    ArrayList<String> argsList = new ArrayList<String>();
    for (int i = 0; i < args.length; i++) {
      argsList.add(args[i]);
    }
    Parse.NoGap ng = new Parse.NoGap();
    int ret = lame.getParser().parse_args(lame.getFlags(), argsList, inPath, outPath, nogap_inPath, ng);
    max_nogap = ng.num_nogap;
    if (ret < 0) {
      lame.close();
      return ret == -2 ? 0 : 1;
    }
    if (lame.getParser().update_interval < 0.)
      lame.getParser().update_interval = 2.f;
    if (outPath.length() != 0 && max_nogap > 0) {
      nogapdir = outPath;
      nogapout = 1;
    }
		/*
		 * initialize input file. This also sets samplerate and as much other
		 * data on the input file as available in the headers
		 */
    if (max_nogap > 0) {
			/*
			 * for nogap encoding of multiple input files, it is not possible to
			 * specify the output file name, only an optional output directory.
			 */
      parse_nogap_filenames(nogapout, nogap_inPath[0], outPath, nogapdir);
      outf = init_files(nogap_inPath[0], outPath.toString(), enc);
    } else {
      outf = init_files(inPath.toString(), outPath.toString(), enc);
    }
    if (outf == null) {
      lame.close();
      return -1;
    }
    lame.getFlags().setWriteId3tagAutomatic(false);
    // Analyze parameters and set more internal options accordingly
    int rc = lame.initParams();

    if (rc < 0) {
      if (rc == -1) {
        usage.printBitrates(System.err);
      }
      System.err.println("fatal error during initialization");
      lame.close();
      return rc;
    }
    if (lame.getParser().silent > 0) {
      lame.getParser().brhist = false; /* turn off VBR histogram */
    }

    if (lame.getFlags().decode_only) {
			/* decode an mp3 file to a .wav */
      if (lame.getParser().mp3_delay_set)
        lame_decoder(outf, lame.getParser().mp3_delay, inPath.toString(),
            outPath.toString(), enc);
      else
        lame_decoder(outf, 0, inPath.toString(),
            outPath.toString(), enc);

    } else {
      if (max_nogap > 0) {
				/*
				 * encode multiple input files using nogap option
				 */
        for (rc = 0; rc < max_nogap; ++rc) {
          boolean use_flush_nogap = (rc != (max_nogap - 1));
          if (rc > 0) {
            parse_nogap_filenames(nogapout, nogap_inPath[rc],
                outPath, nogapdir);
						/*
						 * note: if init_files changes anything, like
						 * samplerate, num_channels, etc, we are screwed
						 */
            outf = init_files(nogap_inPath[rc],
                outPath.toString(), enc);
						/*
						 * reinitialize bitstream for next encoding. this is
						 * normally done by Lame.initParams(), but we cannot
						 * call that routine twice
						 */
            lame.lame_init_bitstream();
          }
          brhist_init_package();
          lame.getFlags().internal_flags.nogap_total = max_nogap;
          lame.getFlags().internal_flags.nogap_current = rc;

          ret = lame_encoder(outf, use_flush_nogap,
              nogap_inPath[rc], outPath.toString());

          ((Closeable) outf).close();
          lame.getAudio().close_infile(); /* close the input file */

        }
      } else {
				/*
				 * encode a single input file
				 */
        brhist_init_package();

        ret = lame_encoder(outf, false, inPath.toString(),
            outPath.toString());

        ((Closeable) outf).close();
        lame.getAudio().close_infile(); /* close the input file */
      }
    }
    lame.close();
    return ret;
  }

  private void encoder_progress_begin(
      final String inPath, final String outPath) {
    if (lame.getParser().silent < 10) {
      lame.lame_print_config();
			/* print useful information about options being used */

      System.out.printf("Encoding %s%s to %s\n", inPath, inPath.length()
          + outPath.length() < 66 ? "" : "\n     ", outPath);

      System.out.printf("Encoding as %g kHz ", 1.e-3 * lame.getFlags().getOutSampleRate());

      {
        String[][] mode_names = {
            {"stereo", "j-stereo", "dual-ch", "single-ch"},
            {"stereo", "force-ms", "dual-ch", "single-ch"}};
        switch (lame.getFlags().getVBR()) {
          case vbr_rh:
            System.out.printf(
                "%s MPEG-%d%s Layer III VBR(q=%g) qval=%d\n",
                mode_names[lame.getFlags().force_ms ? 1 : 0][lame.getFlags().getMode().getNumMode()],
                2 - lame.getFlags().getMpegVersion(), lame.getFlags().getOutSampleRate() < 16000 ? ".5"
                    : "", lame.getFlags().getVBRQuality() + lame.getFlags().VBR_q_frac, lame.getFlags().getQuality());
            break;
          case vbr_mt:
          case vbr_mtrh:
            System.out.printf("%s MPEG-%d%s Layer III VBR(q=%d)\n",
                mode_names[lame.getFlags().force_ms ? 1 : 0][lame.getFlags().getMode().getNumMode()],
                2 - lame.getFlags().getMpegVersion(), lame.getFlags().getOutSampleRate() < 16000 ? ".5"
                    : "", lame.getFlags().getQuality());
            break;
          case vbr_abr:
            System.out
                .printf("%s MPEG-%d%s Layer III (%gx) average %d kbps qval=%d\n",
                    mode_names[lame.getFlags().force_ms ? 1 : 0][lame.getFlags().getMode().getNumMode()],
                    2 - lame.getFlags().getMpegVersion(),
                    lame.getFlags().getOutSampleRate() < 16000 ? ".5" : "",
                    0.1 * (int) (10. * lame.getFlags().compression_ratio + 0.5),
                    lame.getFlags().VBR_mean_bitrate_kbps, lame.getFlags().getQuality());
            break;
          default:
            System.out.printf(
                "%s MPEG-%d%s Layer III (%gx) %3d kbps qval=%d\n",
                mode_names[lame.getFlags().force_ms ? 1 : 0][lame.getFlags().getMode().getNumMode()],
                2 - lame.getFlags().getMpegVersion(), lame.getFlags().getOutSampleRate() < 16000 ? ".5"
                    : "",
                0.1 * (int) (10. * lame.getFlags().compression_ratio + 0.5),
                lame.getFlags().getBitRate(), lame.getFlags().getQuality());
            break;
        }
      }

      if (lame.getParser().silent <= -10) {
        lame.lame_print_internals();
      }
      if (!lame.getParser().embedded) {
        System.out.print("|");
        for (int i = 0; i < MAX_WIDTH - 2; i++) {
          System.out.print("=");
        }
        System.out.println("|");
      }
      oldPercent = curPercent = oldConsoleX = 0;
    }
  }

  private void encoder_progress() {
    if (lame.getParser().silent <= 0 || lame.getParser().embedded) {
      int frames = lame.getFlags().frameNum;
      if (lame.getParser().update_interval <= 0) {
				/* most likely --disptime x not used */
        if ((frames % 100) != 0) {
					/* true, most of the time */
          return;
        }
      } else {
        if (frames != 0 && frames != 9) {
          double act = System.currentTimeMillis();
          double dif = act - last_time;
          if (dif >= 0 && dif < lame.getParser().update_interval) {
            return;
          }
        }
        last_time = System.currentTimeMillis();
				/* from now! disp_time seconds */
      }
      if (lame.getParser().brhist) {
        lame.getHist().brhist_jump_back();
      }
      timestatus(lame.getFlags().frameNum, lame_get_totalframes());
      if (lame.getParser().brhist) {
        lame.getHist().brhist_disp(lame.getFlags());
      }
    }
  }

  private void encoder_progress_end() {
    if (lame.getParser().silent <= 0 || lame.getParser().embedded) {
      if (lame.getParser().brhist) {
        lame.getHist().brhist_jump_back();
      }
      timestatus(lame.getFlags().frameNum, lame_get_totalframes());
      if (lame.getParser().brhist) {
        lame.getHist().brhist_disp(lame.getFlags());
      }
      if (!lame.getParser().embedded) {
        System.out.print("|");
        for (int i = 0; i < MAX_WIDTH - 2; i++) {
          System.out.print("=");
        }
        System.out.println("|");
      }
    }
  }

  private void timestatus(final int frameNum, final int totalframes) {
    int percent;

    if (frameNum < totalframes) {
      percent = (int) (100. * frameNum / totalframes + 0.5);
    } else {
      percent = 100;
    }
    if (oldPercent != percent) {
      progressStep(percent);
      curPercent++;
    }
    oldPercent = percent;
  }

  private void progressStep(int percent) {
    float consoleX = (float) percent * MAX_WIDTH / 100f;
    if ((int) consoleX != oldConsoleX && !lame.getParser().embedded)
      System.out.print(".");
    oldConsoleX = (int) consoleX;
    support.firePropertyChange("progress", oldPercent, percent);
  }

  /**
   * LAME's estimate of the total number of frames to be encoded. Only valid
   * if calling program set num_samples.
   */
  private int lame_get_totalframes() {
		/* estimate based on user set num_samples: */
    int totalframes = (int) (2 + ((double) lame.getFlags().num_samples * lame.getFlags().getOutSampleRate())
        / ((double) lame.getFlags().getInSampleRate() * lame.getFlags().getFrameSize()));

    return totalframes;
  }

  private void WriteBytesSwapped(final DataOutput fp, final float[] p,
                                 final int pPos) throws IOException {
    fp.writeShort((int) p[pPos]);
  }

  private void WriteBytes(final DataOutput fp, final float[] p,
                          final int pPos) throws IOException {
		/* No error condition checking */
    fp.write((int) p[pPos] & 0xff);
    fp.write((((int) p[pPos] & 0xffff) >> 8) & 0xff);
  }

}
