/*
 *      Command line parsing related functions
 *
 *      Copyright (c) 1999 Mark Taylor
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

/* $Id: Parse.java,v 1.33 2012/03/23 10:02:29 kenchis Exp $ */

package org.jjazz.embeddedsynth.lame.mp3;

import org.jjazz.embeddedsynth.lame.mp3.GetAudio.SoundFileFormat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Scanner;

public class Parse {

  private static boolean INTERNAL_OPTS = false;
  /**
   * force byte swapping default=0
   */
  public boolean swapbytes = false;
  /**
   * Verbosity
   */
  public int silent;
  public boolean embedded;
  public boolean brhist;
  /**
   * to use Frank's time status display
   */
  public float update_interval;
  /**
   * to adjust the number of samples truncated during decode
   */
  public int mp3_delay;
  /**
   * user specified the value of the mp3 encoder delay to assume for decoding
   */
  public boolean mp3_delay_set;
  public boolean disable_wav_header;
  /**
   * print info whether waveform clips
   */
  public boolean print_clipping_info;
  /**
   * WAV signed
   */
  public boolean in_signed = true;
  public ByteOrder in_endian = ByteOrder.LITTLE_ENDIAN;
  public int in_bitwidth = 16;
  ID3Tag id3;
  Presets pre;
  private Usage usage = new Usage();
  private Version version = new Version();
  /**
   * Input: sound file format.
   */
  private GetAudio.SoundFileFormat inputFormat;
  /**
   * Ignore errors in values passed for tags
   */
  private boolean ignore_tag_errors;
  /**
   * Input: mp3InputData used by MP3
   */
  private MP3Data mp3InputData = new MP3Data();

  public final void setModules(ID3Tag id32, Presets pre2) {
    this.id3 = id32;
    this.pre = pre2;
  }

  /**
   * Input: Get sound file format.
   *
   * @return sound file format.
   */
  public GetAudio.SoundFileFormat getInputFormat() {
    return inputFormat;
  }

  /**
   * Input: Set sound file format.
   *
   * @param inputFormat sound file format.
   */
  public void setInputFormat(GetAudio.SoundFileFormat inputFormat) {
    this.inputFormat = inputFormat;
  }

  /**
   * Input: Get used by MP3.
   *
   * @return the mp3InputData used by MP3
   */
  public MP3Data getMp3InputData() {
    return mp3InputData;
  }

  /**
   * Input: Set mp3InputData used by MP3.
   *
   * @param mp3InputData the mp3InputData mp3InputData
   */
  public void setMp3InputData(MP3Data mp3InputData) {
    this.mp3InputData = mp3InputData;
  }

  private boolean set_id3tag(final LameGlobalFlags gfp, final int type,
                             final String str) {
    switch (type) {
      case 'a':
        id3.id3tag_set_artist(gfp, str);
        return false;
      case 't':
        id3.id3tag_set_title(gfp, str);
        return false;
      case 'l':
        id3.id3tag_set_album(gfp, str);
        return false;
      case 'g':
        id3.id3tag_set_genre(gfp, str);
        return false;
      case 'c':
        id3.id3tag_set_comment(gfp, str);
        return false;
      case 'n':
        id3.id3tag_set_track(gfp, str);
        return false;
      case 'y':
        id3.id3tag_set_year(gfp, str);
        return false;
      case 'v':
        id3.id3tag_set_fieldvalue(gfp, str);
        return false;
    }
    return false;
  }

  private boolean set_id3v2tag(final LameGlobalFlags gfp, final int type,
                               final String str) {
    switch (type) {
      case 'a':
        id3.id3tag_set_textinfo_ucs2(gfp, "TPE1", str);
        return false;
      case 't':
        id3.id3tag_set_textinfo_ucs2(gfp, "TIT2", str);
        return false;
      case 'l':
        id3.id3tag_set_textinfo_ucs2(gfp, "TALB", str);
        return false;
      case 'g':
        id3.id3tag_set_textinfo_ucs2(gfp, "TCON", str);
        return false;
      case 'c':
        id3.id3tag_set_comment(gfp, null, null, str, 0);
        return false;
      case 'n':
        id3.id3tag_set_textinfo_ucs2(gfp, "TRCK", str);
        return false;
    }
    return false;
  }

  private boolean id3_tag(final LameGlobalFlags gfp, final int type,
                          final TextEncoding enc, final String str) {
    String x = null;
    boolean result;
    switch (enc) {
      default:
      case TENC_RAW:
        x = str;
        break;
      case TENC_LATIN1:
        x = str/* toLatin1(str) */;
        break;
      case TENC_UCS2:
        x = str/* toUcs2(str) */;
        break;
    }
    switch (enc) {
      default:
      case TENC_RAW:
      case TENC_LATIN1:
        result = set_id3tag(gfp, type, x);
        break;
      case TENC_UCS2:
        result = set_id3v2tag(gfp, type, x);
        break;
    }
    return result;
  }

  private int presets_set(final LameGlobalFlags gfp, final int fast,
                          final int cbr, String preset_name, final String ProgramName) {
    int mono = 0;

    if ((preset_name.equals("help")) && (fast < 1) && (cbr < 1)) {
      System.out.println(version.getVersion());
      System.out.println();
      usage.printPresets(System.out);
      return -1;
    }

		/* aliases for compatibility with old presets */

    if (preset_name.equals("phone")) {
      preset_name = "16";
      mono = 1;
    }
    if ((preset_name.equals("phon+")) || (preset_name.equals("lw"))
        || (preset_name.equals("mw-eu")) || (preset_name.equals("sw"))) {
      preset_name = "24";
      mono = 1;
    }
    if (preset_name.equals("mw-us")) {
      preset_name = "40";
      mono = 1;
    }
    if (preset_name.equals("voice")) {
      preset_name = "56";
      mono = 1;
    }
    if (preset_name.equals("fm")) {
      preset_name = "112";
    }
    if ((preset_name.equals("radio")) || (preset_name.equals("tape"))) {
      preset_name = "112";
    }
    if (preset_name.equals("hifi")) {
      preset_name = "160";
    }
    if (preset_name.equals("cd")) {
      preset_name = "192";
    }
    if (preset_name.equals("studio")) {
      preset_name = "256";
    }

    if (preset_name.equals("medium")) {
      pre.lame_set_VBR_q(gfp, 4);
      if (fast > 0) {
        gfp.setVBR(VbrMode.vbr_mtrh);
      } else {
        gfp.setVBR(VbrMode.vbr_rh);
      }
      return 0;
    }

    if (preset_name.equals("standard")) {
      pre.lame_set_VBR_q(gfp, 2);
      if (fast > 0) {
        gfp.setVBR(VbrMode.vbr_mtrh);
      } else {
        gfp.setVBR(VbrMode.vbr_rh);
      }
      return 0;
    } else if (preset_name.equals("extreme")) {
      pre.lame_set_VBR_q(gfp, 0);
      if (fast > 0) {
        gfp.setVBR(VbrMode.vbr_mtrh);
      } else {
        gfp.setVBR(VbrMode.vbr_rh);
      }
      return 0;
    } else if ((preset_name.equals("insane")) && (fast < 1)) {

      gfp.preset = Lame.INSANE;
      pre.apply_preset(gfp, Lame.INSANE, 1);

      return 0;
    }

		/* Generic ABR Preset */
    if (((Integer.valueOf(preset_name)) > 0) && (fast < 1)) {
      if ((Integer.valueOf(preset_name)) >= 8
          && (Integer.valueOf(preset_name)) <= 320) {
        gfp.preset = Integer.valueOf(preset_name);
        pre.apply_preset(gfp, Integer.valueOf(preset_name), 1);

        if (cbr == 1)
          gfp.setVBR(VbrMode.vbr_off);

        if (mono == 1) {
          gfp.setMode(MPEGMode.MONO);
        }

        return 0;

      } else {
        System.err.println(version.getVersion());
        System.err.println();
        System.err
            .printf("Error: The bitrate specified is out of the valid range for this preset\n"
                    + "\n"
                    + "When using this mode you must enter a value between \"32\" and \"320\"\n"
                    + "\n"
                    + "For further information try: \"%s --preset help\"\n",
                ProgramName);
        return -1;
      }
    }

    System.err.println(version.getVersion());
    System.err.println();
    System.err
        .printf("Error: You did not enter a valid profile and/or options with --preset\n"
            + "\n"
            + "Available profiles are:\n"
            + "\n"
            + "   <fast>        medium\n"
            + "   <fast>        standard\n"
            + "   <fast>        extreme\n"
            + "                 insane\n"
            + "          <cbr> (ABR Mode) - The ABR Mode is implied. To use it,\n"
            + "                             simply specify a bitrate. For example:\n"
            + "                             \"--preset 185\" activates this\n"
            + "                             preset and uses 185 as an average kbps.\n"
            + "\n");
    System.err
        .printf("    Some examples:\n"
                + "\n"
                + " or \"%s --preset fast standard <input file> <output file>\"\n"
                + " or \"%s --preset cbr 192 <input file> <output file>\"\n"
                + " or \"%s --preset 172 <input file> <output file>\"\n"
                + " or \"%s --preset extreme <input file> <output file>\"\n"
                + "\n"
                + "For further information try: \"%s --preset help\"\n",
            ProgramName, ProgramName, ProgramName, ProgramName,
            ProgramName);
    return -1;
  }

  /**
   * LAME is a simple frontend which just uses the file extension to determine
   * the file type. Trying to analyze the file contents is well beyond the
   * scope of LAME and should not be added.
   */
  private SoundFileFormat filename_to_type(String FileName) {
    int len = FileName.length();

    if (len < 4)
      return SoundFileFormat.sf_unknown;

    FileName = FileName.substring(len - 4);
    if (FileName.equalsIgnoreCase(".mpg"))
      return SoundFileFormat.sf_mp123;
    if (FileName.equalsIgnoreCase(".mp1"))
      return SoundFileFormat.sf_mp123;
    if (FileName.equalsIgnoreCase(".mp2"))
      return SoundFileFormat.sf_mp123;
    if (FileName.equalsIgnoreCase(".mp3"))
      return SoundFileFormat.sf_mp123;
    if (FileName.equalsIgnoreCase(".wav"))
      return SoundFileFormat.sf_wave;
    if (FileName.equalsIgnoreCase(".aif"))
      return SoundFileFormat.sf_aiff;
    if (FileName.equalsIgnoreCase(".raw"))
      return SoundFileFormat.sf_raw;
    if (FileName.equalsIgnoreCase(".ogg"))
      return SoundFileFormat.sf_ogg;
    return GetAudio.SoundFileFormat.sf_unknown;
  }

  private int resample_rate(double freq) {
    if (freq >= 1.e3)
      freq *= 1.e-3;

    switch ((int) freq) {
      case 8:
        return 8000;
      case 11:
        return 11025;
      case 12:
        return 12000;
      case 16:
        return 16000;
      case 22:
        return 22050;
      case 24:
        return 24000;
      case 32:
        return 32000;
      case 44:
        return 44100;
      case 48:
        return 48000;
      default:
        System.err.printf("Illegal resample frequency: %.3f kHz\n", freq);
        return 0;
    }
  }

  private int set_id3_albumart(final LameGlobalFlags gfp,
                               final String file_name) {
    int ret = -1;
    RandomAccessFile fpi = null;

    if (file_name == null) {
      return 0;
    }
    try {
      fpi = new RandomAccessFile(file_name, "r");
      try {
        int size = (int) (fpi.length() & Integer.MAX_VALUE);
        byte[] albumart = new byte[size];
        fpi.readFully(albumart);
        ret = id3.id3tag_set_albumart(gfp, albumart, size) ? 0 : 4;
      } catch (IOException e) {
        ret = 3;
      } finally {
        try {
          fpi.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (FileNotFoundException e1) {
      ret = 1;
    }
    switch (ret) {
      case 1:
        System.err.printf("Could not find: '%s'.\n", file_name);
        break;
      case 2:
        System.err
            .printf("Insufficient memory for reading the albumart.\n");
        break;
      case 3:
        System.err.printf("Read error: '%s'.\n", file_name);
        break;
      case 4:
        System.err
            .printf("Unsupported image: '%s'.\nSpecify JPEG/PNG/GIF image (128KB maximum)\n",
                file_name);
        break;
      default:
        break;
    }
    return ret;
  }

  public int parse_args(final LameGlobalFlags gfp,
                        final ArrayList<String> argv, final StringBuilder inPath,
                        final StringBuilder outPath, final String[] nogap_inPath,
                        final NoGap ng) {
    /* set to 1 if we parse an input file name */
    int input_file = 0;
    int autoconvert = 0;
    double val;
    int nogap = 0;
		/* set to 1 to use VBR tags in NOGAP mode */
    int nogap_tags = 0;
    final String ProgramName = "lame";
    int count_nogap = 0;
		/* is RG explicitly disabled by the user */
    int noreplaygain = 0;
    ID3TAG_MODE id3tag_mode = ID3TAG_MODE.ID3TAG_MODE_DEFAULT;

    inPath.setLength(0);
    outPath.setLength(0);
		/* turn on display options. user settings may turn them off below */
    silent = 0;
    embedded = false;
    ignore_tag_errors = false;
    brhist = true;
    mp3_delay = 0;
    mp3_delay_set = false;
    print_clipping_info = false;
    disable_wav_header = false;
    id3.init(gfp);

		/* process args */
    for (int i = 0; i < argv.size(); i++) {
      char c;
      String token;
      int tokenPos = 0;
      String arg;
      String nextArg;
      int argUsed;

      token = argv.get(i);
      if (token.charAt(tokenPos++) == '-') {
        argUsed = 0;
        nextArg = i + 1 < argv.size() ? argv.get(i + 1) : "";

        if (token.length() - tokenPos == 0) {
					/* The user wants to use stdin and/or stdout. */
          input_file = 1;
          if (inPath.length() == 0) {
            inPath.setLength(0);
            inPath.append(argv.get(i));
          } else if (outPath.length() == 0) {
            outPath.setLength(0);
            outPath.append(argv.get(i));
          }
        }
        if (token.charAt(tokenPos) == '-') { /* GNU style */
          tokenPos++;

          if (token.substring(tokenPos).equalsIgnoreCase("resample")) {
            argUsed = 1;
            gfp.setOutSampleRate(resample_rate(Double
                .parseDouble(nextArg)));

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "vbr-old")) {
            gfp.setVBR(VbrMode.vbr_rh);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "vbr-new")) {
            gfp.setVBR(VbrMode.vbr_mtrh);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "vbr-mtrh")) {
            gfp.setVBR(VbrMode.vbr_mtrh);

          } else if (token.substring(tokenPos)
              .equalsIgnoreCase("cbr")) {
            gfp.setVBR(VbrMode.vbr_off);

          } else if (token.substring(tokenPos)
              .equalsIgnoreCase("abr")) {
            argUsed = 1;
            gfp.setVBR(VbrMode.vbr_abr);
            gfp.VBR_mean_bitrate_kbps = Integer.valueOf(nextArg);
						/*
						 * values larger than 8000 are bps (like Fraunhofer), so
						 * it's strange to get 320000 bps MP3 when specifying
						 * 8000 bps MP3
						 */
            if (gfp.VBR_mean_bitrate_kbps >= 8000)
              gfp.VBR_mean_bitrate_kbps = (gfp.VBR_mean_bitrate_kbps + 500) / 1000;

            gfp.VBR_mean_bitrate_kbps = Math.min(
                gfp.VBR_mean_bitrate_kbps, 320);
            gfp.VBR_mean_bitrate_kbps = Math.max(
                gfp.VBR_mean_bitrate_kbps, 8);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "r3mix")) {
            gfp.preset = Lame.R3MIX;
            pre.apply_preset(gfp, Lame.R3MIX, 1);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "bitwidth")) {
            argUsed = 1;
            in_bitwidth = Integer.valueOf(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "signed")) {
            in_signed = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "unsigned")) {
            in_signed = false;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "little-endian")) {
            in_endian = ByteOrder.LITTLE_ENDIAN;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "big-endian")) {
            in_endian = ByteOrder.BIG_ENDIAN;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "mp1input")) {
            setInputFormat(GetAudio.SoundFileFormat.sf_mp1);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "mp2input")) {
            setInputFormat(GetAudio.SoundFileFormat.sf_mp2);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "mp3input")) {
            setInputFormat(GetAudio.SoundFileFormat.sf_mp3);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "ogginput")) {
            System.err
                .printf("sorry, vorbis support in LAME is deprecated.\n");
            return -1;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "phone")) {
            if (presets_set(gfp, 0, 0, token, ProgramName) < 0)
              return -1;
            System.err
                .printf("Warning: --phone is deprecated, use --preset phone instead!");

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "voice")) {
            if (presets_set(gfp, 0, 0, token, ProgramName) < 0)
              return -1;
            System.err
                .printf("Warning: --voice is deprecated, use --preset voice instead!");

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "noshort")) {
            gfp.short_blocks = ShortBlock.short_block_dispensed;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "short")) {
            gfp.short_blocks = ShortBlock.short_block_allowed;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "allshort")) {
            gfp.short_blocks = ShortBlock.short_block_forced;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "decode")) {
            gfp.decode_only = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "decode-mp3delay")) {
            mp3_delay = Integer.valueOf(nextArg);
            mp3_delay_set = true;
            argUsed = 1;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nores")) {
            gfp.disable_reservoir = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "strictly-enforce-ISO")) {
            gfp.strict_ISO = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "scale")) {
            argUsed = 1;
            gfp.scale = (float) Double.parseDouble(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "scale-l")) {
            argUsed = 1;
            gfp.scale_left = (float) Double.parseDouble(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "scale-r")) {
            argUsed = 1;
            gfp.scale_right = (float) Double.parseDouble(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "freeformat")) {
            gfp.free_format = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "replaygain-fast")) {
            gfp.setFindReplayGain(true);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "replaygain-accurate")) {
            gfp.decode_on_the_fly = true;
            gfp.setFindReplayGain(true);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "noreplaygain")) {
            noreplaygain = 1;
            gfp.setFindReplayGain(false);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "clipdetect")) {
            print_clipping_info = true;
            gfp.decode_on_the_fly = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nohist")) {
            brhist = false;

						/* options for ID3 tag */
          } else if (token.substring(tokenPos).equalsIgnoreCase("tt")) {
            argUsed = 1;
            id3_tag(gfp, 't', TextEncoding.TENC_RAW, nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase("ta")) {
            argUsed = 1;
            id3_tag(gfp, 'a', TextEncoding.TENC_RAW, nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase("tl")) {
            argUsed = 1;
            id3_tag(gfp, 'l', TextEncoding.TENC_RAW, nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase("ty")) {
            argUsed = 1;
            id3_tag(gfp, 'y', TextEncoding.TENC_RAW, nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase("tc")) {
            argUsed = 1;
            id3_tag(gfp, 'c', TextEncoding.TENC_RAW, nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase("tn")) {
            boolean ret = id3_tag(gfp, 'n', TextEncoding.TENC_RAW,
                nextArg);
            argUsed = 1;
            if (ret) {
              if (!ignore_tag_errors) {
                if (id3tag_mode == ID3TAG_MODE.ID3TAG_MODE_V1_ONLY) {
                  System.err
                      .printf("The track number has to be between 1 and 255 for ID3v1.\n");
                  return -1;
                } else if (id3tag_mode == ID3TAG_MODE.ID3TAG_MODE_V2_ONLY) {
									/*
									 * track will be stored as-is in ID3v2 case,
									 * so no problem here
									 */
                } else {
                  if (silent < 10) {
                    System.err
                        .printf("The track number has to be between 1 and 255 for ID3v1, ignored for ID3v1.\n");
                  }
                }
              }
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase("tg")) {
            id3_tag(gfp, 'g', TextEncoding.TENC_RAW, nextArg);
            argUsed = 1;

          } else if (token.substring(tokenPos).equalsIgnoreCase("tv")) {
            argUsed = 1;
            if (id3_tag(gfp, 'v', TextEncoding.TENC_RAW, nextArg)) {
              if (silent < 10) {
                System.err.printf(
                    "Invalid field value: '%s'. Ignored\n",
                    nextArg);
              }
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase("ti")) {
            argUsed = 1;
            if (set_id3_albumart(gfp, nextArg) != 0) {
              if (!ignore_tag_errors) {
                return -1;
              }
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "ignore-tag-errors")) {
            ignore_tag_errors = true;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "add-id3v2")) {
            id3.id3tag_add_v2(gfp);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "id3v1-only")) {
            id3.id3tag_v1_only(gfp);
            id3tag_mode = ID3TAG_MODE.ID3TAG_MODE_V1_ONLY;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "id3v2-only")) {
            id3.id3tag_v2_only(gfp);
            id3tag_mode = ID3TAG_MODE.ID3TAG_MODE_V2_ONLY;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "space-id3v1")) {
            id3.id3tag_space_v1(gfp);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "pad-id3v2")) {
            id3.id3tag_pad_v2(gfp);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "pad-id3v2-size")) {
            int n = Integer.valueOf(nextArg);
            n = n <= 128000 ? n : 128000;
            n = n >= 0 ? n : 0;
            id3.id3tag_set_pad(gfp, n);
            argUsed = 1;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "genre-list")) {
            id3.id3tag_genre_list(new GenreListHandler() {

              public void genre_list_handler(int num, String name) {
                System.out.printf("%3d %s\n", num, name);
              }

            });
            return -2;

            // XXX Unsupported: some experimental switches for
            // setting ID3 tags

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "lowpass")) {
            val = Double.parseDouble(nextArg);
            argUsed = 1;
            if (val < 0) {
              gfp.lowpassfreq = -1;
            } else {
							/* useful are 0.001 kHz...50 kHz, 50 Hz...50000 Hz */
              if (val < 0.001 || val > 50000.) {
                System.err
                    .printf("Must specify lowpass with --lowpass freq, freq >= 0.001 kHz\n");
                return -1;
              }
              gfp.lowpassfreq = (int) (val
                  * (val < 50. ? 1.e3 : 1.e0) + 0.5);
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "lowpass-width")) {
            val = Double.parseDouble(nextArg);
            argUsed = 1;
						/* useful are 0.001 kHz...16 kHz, 16 Hz...50000 Hz */
            if (val < 0.001 || val > 50000.) {
              System.err
                  .printf("Must specify lowpass width with --lowpass-width freq, freq >= 0.001 kHz\n");
              return -1;
            }
            gfp.lowpassfreq = (int) (val
                * (val < 16. ? 1.e3 : 1.e0) + 0.5);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "highpass")) {
            val = Double.parseDouble(nextArg);
            argUsed = 1;
            if (val < 0.0) {
              gfp.highpassfreq = -1;
            } else {
							/* useful are 0.001 kHz...16 kHz, 16 Hz...50000 Hz */
              if (val < 0.001 || val > 50000.) {
                System.err
                    .printf("Must specify highpass with --highpass freq, freq >= 0.001 kHz\n");
                return -1;
              }
              gfp.highpassfreq = (int) (val
                  * (val < 16. ? 1.e3 : 1.e0) + 0.5);
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "highpass-width")) {
            val = Double.parseDouble(nextArg);
            argUsed = 1;
						/* useful are 0.001 kHz...16 kHz, 16 Hz...50000 Hz */
            if (val < 0.001 || val > 50000.) {
              System.err
                  .printf("Must specify highpass width with --highpass-width freq, freq >= 0.001 kHz\n");
              return -1;
            }
            gfp.highpasswidth = (int) val;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "comp")) {
            argUsed = 1;
            val = Double.parseDouble(nextArg);
            if (val < 1.0) {
              System.err
                  .printf("Must specify compression ratio >= 1.0\n");
              return -1;
            }
            gfp.compression_ratio = (float) val;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "notemp")) {
            gfp.useTemporal = false;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "interch")) {
            argUsed = 1;
            gfp.interChRatio = (float) Double.parseDouble(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "temporal-masking")) {
            argUsed = 1;
            gfp.useTemporal = Integer.valueOf(nextArg) != 0;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nssafejoint")) {
            gfp.exp_nspsytune = gfp.exp_nspsytune | 2;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nsmsfix")) {
            argUsed = 1;
            gfp.msfix = (float) Double.parseDouble(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "ns-bass")) {
            argUsed = 1;
            {
              double d;
              int k;
              d = Double.parseDouble(nextArg);
              k = (int) (d * 4);
              if (k < -32)
                k = -32;
              if (k > 31)
                k = 31;
              if (k < 0)
                k += 64;
              gfp.exp_nspsytune = gfp.exp_nspsytune | (k << 2);
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "ns-alto")) {
            argUsed = 1;
            {
              double d;
              int k;
              d = Double.parseDouble(nextArg);
              k = (int) (d * 4);
              if (k < -32)
                k = -32;
              if (k > 31)
                k = 31;
              if (k < 0)
                k += 64;
              gfp.exp_nspsytune = gfp.exp_nspsytune | (k << 8);
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "ns-treble")) {
            argUsed = 1;
            {
              double d;
              int k;
              d = Double.parseDouble(nextArg);
              k = (int) (d * 4);
              if (k < -32)
                k = -32;
              if (k > 31)
                k = 31;
              if (k < 0)
                k += 64;
              gfp.exp_nspsytune = gfp.exp_nspsytune | (k << 14);
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "ns-sfb21")) {
						/*
						 * to be compatible with Naoki's original code, ns-sfb21
						 * specifies how to change ns-treble for sfb21
						 */
            argUsed = 1;
            {
              double d;
              int k;
              d = Double.parseDouble(nextArg);
              k = (int) (d * 4);
              if (k < -32)
                k = -32;
              if (k > 31)
                k = 31;
              if (k < 0)
                k += 64;
              gfp.exp_nspsytune = gfp.exp_nspsytune | (k << 20);
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nspsytune2")) {

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "quiet")
              || token.substring(tokenPos).equalsIgnoreCase(
              "silent")) {
            silent = 10; /* on a scale from 1 to 10 be very silent */

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "brief")) {
            silent = -5; /* print few info on screen */

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "embedded")) {
            embedded = true;
          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "verbose")) {
            silent = -10; /* print a lot on screen */

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "version")
              || token.substring(tokenPos).equalsIgnoreCase(
              "license")) {
            System.out.println(version.getVersion());
            System.out.println();
            usage.printLicense(System.out);
            return -2;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "help")
              || token.substring(tokenPos).equalsIgnoreCase(
              "usage")) {
            System.out.println(version.getVersion());
            System.out.println();
            usage.printShort(gfp, System.out);
            return -2;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "longhelp")) {
            System.out.println(version.getVersion());
            System.out.println();
            usage.printLong(gfp, System.out);
            usage.printBitrates(System.out);

            return -2;

          } else if (token.substring(tokenPos).equalsIgnoreCase("?")) {
            System.out.println(version.getVersion());
            System.out.println();
            usage.printLong(gfp, System.out);
            usage.printBitrates(System.out);
            return -2;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "preset")
              || token.substring(tokenPos).equalsIgnoreCase(
              "alt-preset")) {
            argUsed = 1;
            {
              int fast = 0, cbr = 0;

              while ((nextArg.equals("fast"))
                  || (nextArg.equals("cbr"))) {

                if ((nextArg.equals("fast")) && (fast < 1))
                  fast = 1;
                if ((nextArg.equals("cbr")) && (cbr < 1))
                  cbr = 1;

                argUsed++;
                nextArg = i + argUsed < argv.size() ? argv
                    .get(i + argUsed) : "";
              }

              if (presets_set(gfp, fast, cbr, nextArg,
                  ProgramName) < 0)
                return -1;
            }

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "disptime")) {
            argUsed = 1;
            update_interval = (float) Double.parseDouble(nextArg);

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nogaptags")) {
            nogap_tags = 1;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nogapout")) {
            outPath.setLength(0);
            outPath.append(nextArg);
            argUsed = 1;

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "nogap")) {
            nogap = 1;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "tune")) { /* without helptext */
            argUsed = 1;
            {
              gfp.tune_value_a = (float) Double
                  .parseDouble(nextArg);
              gfp.tune = true;
            }

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "shortthreshold")) {
            {
              float x, y;
              try (Scanner sc = new Scanner(nextArg)) {
                x = sc.nextFloat();
                if (!sc.hasNext()) {
                  y = x;
                } else {
                  sc.nextByte();
                  y = sc.nextFloat();
                }
              }
              argUsed = 1;
              gfp.internal_flags.nsPsy.attackthre = x;
              gfp.internal_flags.nsPsy.attackthre_s = y;
            }

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "maskingadjust")) { /* without helptext */
            argUsed = 1;
            gfp.maskingadjust = (float) Double.parseDouble(nextArg);

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "maskingadjustshort")) { /* without helptext */
            argUsed = 1;
            gfp.maskingadjust_short = (float) Double
                .parseDouble(nextArg);

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "athcurve")) { /* without helptext */
            argUsed = 1;
            gfp.ATHcurve = (float) Double.parseDouble(nextArg);

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "no-preset-tune")) { /* without helptext */

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "substep")) {
            argUsed = 1;
            gfp.internal_flags.substep_shaping = Integer
                .valueOf(nextArg);

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "sbgain")) { /* without helptext */
            argUsed = 1;
            gfp.internal_flags.subblock_gain = Integer
                .valueOf(nextArg);

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "sfscale")) { /* without helptext */
            gfp.internal_flags.noise_shaping = 2;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "noath")) {
            gfp.noATH = true;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "athonly")) {
            gfp.ATHonly = true;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "athshort")) {
            gfp.ATHshort = true;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "athlower")) {
            argUsed = 1;
            gfp.ATHlower = -(float) Double.parseDouble(nextArg) / 10.0f;

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "athtype")) {
            argUsed = 1;
            gfp.ATHtype = Integer.valueOf(nextArg);

          } else if (INTERNAL_OPTS
              && token.substring(tokenPos).equalsIgnoreCase(
              "athaa-type")) {
						/*
						 * switch for developing, no DOCU
						 */
            argUsed = 1;
						/*
						 * once was 1:Gaby, 2:Robert, 3:Jon, else:off
						 */
            gfp.athaa_type = Integer.valueOf(nextArg);
						/*
						 * now: 0:off else:Jon
						 */

          } else if (token.substring(tokenPos).equalsIgnoreCase(
              "athaa-sensitivity")) {
            argUsed = 1;
            gfp.athaa_sensitivity = (float) Double
                .parseDouble(nextArg);

          } else {
            {
              System.err.printf("%s: unrecognized option --%s\n",
                  ProgramName, token);
              return -1;
            }
          }
          i += argUsed;

        } else {
          while (tokenPos < token.length()) {
            c = token.charAt(tokenPos++);

            arg = tokenPos < token.length() ? token : nextArg;
            switch (c) {
              case 'm':
                argUsed = 1;

                switch (arg.charAt(0)) {
                  case 's':
                    gfp.setMode(MPEGMode.STEREO);
                    break;
                  case 'd':
                    gfp.setMode(MPEGMode.DUAL_CHANNEL);
                    break;
                  case 'f':
                    gfp.force_ms = true;
								/* FALLTHROUGH */
                  case 'j':
                    gfp.setMode(MPEGMode.JOINT_STEREO);
                    break;
                  case 'm':
                    gfp.setMode(MPEGMode.MONO);
                    break;
                  case 'a':
                    gfp.setMode(MPEGMode.JOINT_STEREO);
                    break;
                  default:
                    System.err
                        .printf("%s: -m mode must be s/d/j/f/m not %s\n",
                            ProgramName, arg);
                    return -1;
                }
                break;

              case 'V':
                argUsed = 1;
							/* to change VBR default look in lame.h */
                if (gfp.getVBR() == VbrMode.vbr_off) {
                  gfp.setVBRQuality(VbrMode.vbr_default.ordinal());
                  gfp.VBR_q_frac = 0;
                }
                gfp.setVBRQuality((int) (float) Double.parseDouble(arg));
                gfp.VBR_q_frac = (float) Double.parseDouble(arg)
                    - gfp.getVBRQuality();
                break;
              case 'v':
							/* to change VBR default look in lame.h */
                if (gfp.getVBR() == VbrMode.vbr_off)
                  gfp.setVBR(VbrMode.vbr_mtrh);
                break;

              case 'q':
                argUsed = 1;
              {
                int tmp_quality = Integer.valueOf(arg);

								/*
								 * XXX should we move this into
								 * lame_set_quality()?
								 */
                if (tmp_quality < 0)
                  tmp_quality = 0;
                if (tmp_quality > 9)
                  tmp_quality = 9;

                gfp.setQuality(tmp_quality);
              }
              break;
              case 'f':
                gfp.setQuality(7);
                break;
              case 'h':
                gfp.setQuality(2);
                break;

              case 's':
                argUsed = 1;
                val = Double.parseDouble(arg);
                gfp.setInSampleRate((int) (val
                    * (val <= 192 ? 1.e3 : 1.e0) + 0.5));
                break;
              case 'b':
                argUsed = 1;
                gfp.setBitRate(Integer.valueOf(arg));

                if (gfp.getBitRate() > 320) {
                  gfp.disable_reservoir = true;
                }
                gfp.VBR_min_bitrate_kbps = gfp.getBitRate();
                break;
              case 'B':
                argUsed = 1;
                gfp.VBR_max_bitrate_kbps = Integer.valueOf(arg);
                break;
              case 'F':
                gfp.VBR_hard_min = 1;
                break;
              case 't': /* dont write VBR tag */
                gfp.bWriteVbrTag = false;
                disable_wav_header = true;
                break;
              case 'T': /* do write VBR tag */
                gfp.bWriteVbrTag = true;
                nogap_tags = 1;
                disable_wav_header = false;
                break;
              case 'r': /* force raw pcm input file */
                setInputFormat(SoundFileFormat.sf_raw);
                break;
              case 'x': /* force byte swapping */
                swapbytes = true;
                break;
              case 'p':
							/*
							 * (jo) error_protection: add crc16 information to
							 * stream
							 */
                gfp.error_protection = true;
                break;
              case 'a':
							/*
							 * autoconvert input file from stereo to mono - for
							 * mono mp3 encoding
							 */
                autoconvert = 1;
                gfp.setMode(MPEGMode.MONO);
                break;
              case 'S':
                silent = 10;
                break;
              case 'X':
							/*
							 * experimental switch -X: the differnt types of
							 * quant compare are tough to communicate to
							 * endusers, so they shouldn't bother to toy around
							 * with them
							 */
              {
                int x, y;
                try (Scanner sc = new Scanner(arg)) {
                  x = sc.nextInt();
                  if (!sc.hasNext()) {
                    y = x;
                  } else {
                    sc.nextByte();
                    y = sc.nextInt();
                  }
                }
                argUsed = 1;
                if (INTERNAL_OPTS) {
                  gfp.quant_comp = x;
                  gfp.quant_comp_short = y;
                }
              }
              break;
              case 'Y':
                gfp.experimentalY = true;
                break;
              case 'Z':
							/*
							 * experimental switch -Z: this switch is obsolete
							 */
              {
                int n = 1;
                try (Scanner sc = new Scanner(arg)) {
                  n = sc.nextInt();
                }
                if (INTERNAL_OPTS) {
                  gfp.experimentalZ = n;
                }
              }
              break;
              case 'e':
                argUsed = 1;

                switch (arg.charAt(0)) {
                  case 'n':
                    gfp.emphasis = 0;
                    break;
                  case '5':
                    gfp.emphasis = 1;
                    break;
                  case 'c':
                    gfp.emphasis = 3;
                    break;
                  default:
                    System.err.printf(
                        "%s: -e emp must be n/5/c not %s\n",
                        ProgramName, arg);
                    return -1;
                }
                break;
              case 'c':
                gfp.copyright = 1;
                break;
              case 'o':
                gfp.original = 0;
                break;

              case '?':
                System.out.println(version.getVersion());
                System.out.println();
                usage.printLong(gfp, System.out);
                usage.printBitrates(System.out);
                return -1;

              default:
                System.err.printf("%s: unrecognized option -%c\n",
                    ProgramName, c);
                return -1;
            }
            if (argUsed != 0) {
              if (arg == token)
                token = ""; /* no more from token */
              else
                ++i; /* skip arg we used */
              arg = "";
              argUsed = 0;
            }
          }
        }
      } else {
        if (nogap != 0) {
          if ((ng != null) && (count_nogap < ng.num_nogap)) {
            nogap_inPath[count_nogap++] = argv.get(i);
            input_file = 1;
          } else {
						/* sorry, calling program did not allocate enough space */
            System.err
                .printf("Error: 'nogap option'.  Calling program does not allow nogap option, or\n"
                    + "you have exceeded maximum number of input files for the nogap option\n");
            ng.num_nogap = -1;
            return -1;
          }
        } else {
					/* normal options: inputfile [outputfile] */
          if (inPath.length() == 0) {
            inPath.setLength(0);
            inPath.append(argv.get(i));
            input_file = 1;
          } else {
            if (outPath.length() == 0) {
              outPath.setLength(0);
              outPath.append(argv.get(i));
            } else {
              System.err.printf("%s: excess arg %s\n",
                  ProgramName, argv.get(i));
              return -1;
            }
          }
        }
      }
    } /* loop over args */

    if (0 == input_file) {
      System.out.println(version.getVersion());
      System.out.println();
      usage.print(System.out);
      return -1;
    }

    if (inPath.toString().charAt(0) == '-')
      silent = (silent <= 1 ? 1 : silent);

    if (outPath.length() == 0 && count_nogap == 0) {
      outPath.setLength(0);
      outPath.append(inPath.substring(0, inPath.length() - 4));
      if (gfp.decode_only) {
        outPath.append(".mp3.wav");
      } else {
        outPath.append(".mp3");
      }
    }

		/* RG is enabled by default */
    if (0 == noreplaygain)
      gfp.setFindReplayGain(true);

		/* disable VBR tags with nogap unless the VBR tags are forced */
    if (nogap != 0 && gfp.bWriteVbrTag && nogap_tags == 0) {
      System.out
          .println("Note: Disabling VBR Xing/Info tag since it interferes with --nogap\n");
      gfp.bWriteVbrTag = false;
    }

		/* some file options not allowed with stdout */
    if (outPath.toString().charAt(0) == '-') {
      gfp.bWriteVbrTag = false; /* turn off VBR tag */
    }

		/* if user did not explicitly specify input is mp3, check file name */
    if (getInputFormat() == SoundFileFormat.sf_unknown)
      setInputFormat(filename_to_type(inPath.toString()));

    if (getInputFormat() == SoundFileFormat.sf_ogg) {
      System.err.printf("sorry, vorbis support in LAME is deprecated.\n");
      return -1;
    }
		/* default guess for number of channels */
    if (autoconvert != 0)
      gfp.setInNumChannels(2);
    else if (MPEGMode.MONO == gfp.getMode())
      gfp.setInNumChannels(1);
    else
      gfp.setInNumChannels(2);

    if (gfp.free_format) {
      if (gfp.getBitRate() < 8 || gfp.getBitRate() > 640) {
        System.err
            .printf("For free format, specify a bitrate between 8 and 640 kbps\n");
        System.err.printf("with the -b <bitrate> option\n");
        return -1;
      }
    }
    if (ng != null)
      ng.num_nogap = count_nogap;
    return 0;
  }

  /**
   * possible text encodings
   */
  private enum TextEncoding {
    /**
     * bytes will be stored as-is into ID3 tags, which are Latin1/UCS2 per
     * definition
     */
    TENC_RAW, /**
     * text will be converted from local encoding to Latin1, as
     * ID3 needs it
     */
    TENC_LATIN1, /**
     * text will be converted from local encoding to UCS-2, as
     * ID3v2 wants it
     */
    TENC_UCS2
  }

  ;

  private enum ID3TAG_MODE {
    ID3TAG_MODE_DEFAULT, ID3TAG_MODE_V1_ONLY, ID3TAG_MODE_V2_ONLY
  }

  public static class NoGap {
    int num_nogap;
  }

}
