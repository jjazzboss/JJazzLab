package org.jjazz.embeddedsynth.lame.mp3;

import java.io.PrintStream;

public class Usage {
  /**
   * Print command line syntax.
   */
  public final void print(final PrintStream ps) {
    ps.println("usage: lame [options] <infile> [outfile]");
    ps.println();
    ps.println();
    ps.println("    <infile> and/or <outfile> can be \"-\", which means stdin/stdout.");
    ps.println();
    ps.println("Try:");
    ps.println("     \"lame --help\"           for general usage information");
    ps.println(" or:");
    ps.println("     \"lame --preset help\"    for information on suggested predefined settings");
    ps.println(" or:");
    ps.println("     \"lame --longhelp\"");
    ps.println("  or \"lame -?\"              for a complete options list");
    ps.println();
  }

  /**
   * Print license.
   */
  public final void printLicense(final PrintStream ps) {
    ps.println("Can I use LAME in my commercial program?");
    ps.println();
    ps.println("Yes, you can, under the restrictions of the LGPL.  In particular, you");
    ps.println("can include a compiled version of the LAME library (for example,");
    ps.println("lame.dll) with a commercial program.  Some notable requirements of");
    ps.println("the LGPL:");
    ps.println();
    ps.println("1. In your program, you cannot include any source code from LAME, with");
    ps.println("   the exception of files whose only purpose is to describe the library");
    ps.println("   interface (such as lame.h).");
    ps.println();
    ps.println("2. Any modifications of LAME must be released under the LGPL.");
    ps.println("   The LAME project (www.mp3dev.org) would appreciate being");
    ps.println("   notified of any modifications.");
    ps.println();
    ps.println("3. You must give prominent notice that your program is:");
    ps.println("      A. using LAME (including version number)");
    ps.println("      B. LAME is under the LGPL");
    ps.println("      C. Provide a copy of the LGPL.  (the file COPYING contains the LGPL)");
    ps.println("      D. Provide a copy of LAME source, or a pointer where the LAME");
    ps.println("         source can be obtained (such as http://sourceforge.net/projects/jsidplay2/)");
    ps.println("   An example of prominent notice would be an \"About the LAME encoding engine\"");
    ps.println("   button in some pull down menu within the executable of your program.");
    ps.println();
    ps.println("4. If you determine that distribution of LAME requires a patent license,");
    ps.println("   you must obtain such license.");
    ps.println();
    ps.println();
    ps.println("*** IMPORTANT NOTE ***");
    ps.println();
    ps.println("The decoding functions provided in LAME use the mpglib decoding engine which");
    ps.println("is under the GPL.  They may not be used by any program not released under the");
    ps.println("GPL unless you obtain such permission from the MPG123 project (www.mpg123.de).");
    ps.println();
  }

  /**
   * Print command line syntax, but only the most important ones.
   */
  public final void printShort(final LameGlobalFlags flags,
                               final PrintStream ps) {
    ps.println("usage: lame [options] <infile> [outfile]");
    ps.println();
    ps.println("    <infile> and/or <outfile> can be \"-\", which means stdin/stdout.");
    ps.println();
    ps.println("RECOMMENDED:");
    ps.println("    lame -V 2 input.wav output.mp3");
    ps.println();
    ps.println("OPTIONS:");
    ps.println("    -b bitrate      set the bitrate, default 128 kbps");
    ps.println("    -h              higher quality, but a little slower.  Recommended.");
    ps.println("    -f              fast mode (lower quality)");
    ps.println("    -V n            quality setting for VBR.  default n="
        + flags.getVBRQuality());
    ps.println("                    0=high quality,bigger files. 9=smaller files");
    ps.println("    --preset type   type must be \"medium\", \"standard\", \"extreme\", \"insane\",");
    ps.println("                    or a value for an average desired bitrate and depending");
    ps.println("                    on the value specified, appropriate quality settings will");
    ps.println("                    be used.");
    ps.println("                    \"--preset help\" gives more info on these");
    ps.println();
    ps.println("    --longhelp      full list of options");
    ps.println();
    ps.println("    --license       print License information");
    ps.println();
    ps.println();
  }

  /**
   * Print command line syntax completely.
   */
  public final void printLong(final LameGlobalFlags flags,
                              final PrintStream ps) {
    ps.println("usage: lame [options] <infile> [outfile]");
    ps.println();
    ps.println("    <infile> and/or <outfile> can be \"-\", which means stdin/stdout.");
    ps.println();
    ps.println("RECOMMENDED:");
    ps.println("    lame -V2 input.wav output.mp3");
    ps.println();
    ps.println("OPTIONS:");
    ps.println("  Input options:");
    ps.println("    --scale <arg>   scale input (multiply PCM data) by <arg>");
    ps.println("    --scale-l <arg> scale channel 0 (left) input (multiply PCM data) by <arg>");
    ps.println("    --scale-r <arg> scale channel 1 (right) input (multiply PCM data) by <arg>");
    ps.println("    --mp1input      input file is a MPEG Layer I   file");
    ps.println("    --mp2input      input file is a MPEG Layer II  file");
    ps.println("    --mp3input      input file is a MPEG Layer III file");
    ps.println("    --nogap <file1> <file2> <...>");
    ps.println("                    gapless encoding for a set of contiguous files");
    ps.println("    --nogapout <dir>");
    ps.println("                    output dir for gapless encoding (must precede --nogap)");
    ps.println("    --nogaptags     allow the use of VBR tags in gapless encoding");
    ps.println();
    ps.println("  Input options for RAW PCM:");
    ps.println("    -r              input is raw pcm");
    ps.println("    -x              force byte-swapping of input");
    ps.println("    -s sfreq        sampling frequency of input file (kHz) - default 44.1 kHz");
    ps.println("    --bitwidth w    input bit width is w (default 16)");
    ps.println("    --signed        input is signed (default)");
    ps.println("    --unsigned      input is unsigned");
    ps.println("    --little-endian input is little-endian (default)");
    ps.println("    --big-endian    input is big-endian");

    ps.println("  Operational options:");
    ps.println("    -a              downmix from stereo to mono file for mono encoding");
    ps.println("    -m <mode>       (j)oint, (s)imple, (f)orce, (d)dual-mono, (m)ono");
    ps.println("                    default is (j) or (s) depending on bitrate");
    ps.println("                    joint  = joins the best possible of MS and LR stereo");
    ps.println("                    simple = force LR stereo on all frames");
    ps.println("                    force  = force MS stereo on all frames.");
    ps.println("    --preset type   type must be \"medium\", \"standard\", \"extreme\", \"insane\",");
    ps.println("                    or a value for an average desired bitrate and depending");
    ps.println("                    on the value specified, appropriate quality settings will");
    ps.println("                    be used.");
    ps.println("                    \"--preset help\" gives more info on these");
    ps.println("    --comp  <arg>   choose bitrate to achive a compression ratio of <arg>");
    ps.println("    --replaygain-fast   compute RG fast but slightly inaccurately (default)");
    ps.println("    --replaygain-accurate   compute RG more accurately and find the peak sample");
    ps.println("    --noreplaygain  disable ReplayGain analysis");
    ps.println("    --clipdetect    enable --replaygain-accurate and print a message whether");
    ps.println("                    clipping occurs and how far the waveform is from full scale");
    ps.println("    --freeformat    produce a free format bitstream");
    ps.println("    --decode        input=mp3 file, output=wav");
    ps.println("    -t              disable writing wav header when using --decode");

    ps.println("  Verbosity:");
    ps.println("    --disptime <arg>print progress report every arg seconds");
    ps.println("    -S              don't print progress report, VBR histograms");
    ps.println("    --nohist        disable VBR histogram display");
    ps.println("    --silent        don't print anything on screen");
    ps.println("    --quiet         don't print anything on screen");
    ps.println("    --brief         print more useful information");
    ps.println("    --verbose       print a lot of useful information");
    ps.println();
    ps.println("  Noise shaping & psycho acoustic algorithms:");
    ps.println("    -q <arg>        <arg> = 0...9.  Default  -q 5 ");
    ps.println("                    -q 0:  Highest quality, very slow ");
    ps.println("                    -q 9:  Poor quality, but fast ");
    ps.println("    -h              Same as -q 2.   Recommended.");
    ps.println("    -f              Same as -q 7.   Fast, ok quality");

    ps.println("  CBR (constant bitrate, the default) options:");
    ps.println("    -b <bitrate>    set the bitrate in kbps, default 128 kbps");
    ps.println("    --cbr           enforce use of constant bitrate");
    ps.println();
    ps.println("  ABR options:");
    ps.println("    --abr <bitrate> specify average bitrate desired (instead of quality)");
    ps.println();
    ps.println("  VBR options:");
    ps.println("    -V n            quality setting for VBR.  default n="
        + flags.getVBRQuality());
    ps.println("                    0=high quality,bigger files. 9=smaller files");
    ps.println("    -v              the same as -V 4");
    ps.println("    --vbr-old       use old variable bitrate (VBR) routine");
    ps.println("    --vbr-new       use new variable bitrate (VBR) routine (default)");
    ps.println("    -b <bitrate>    specify minimum allowed bitrate, default  32 kbps");
    ps.println("    -B <bitrate>    specify maximum allowed bitrate, default 320 kbps");
    ps.println("    -F              strictly enforce the -b option, for use with players that");
    ps.println("                    do not support low bitrate mp3");
    ps.println("    -t              disable writing LAME Tag");
    ps.println("    -T              enable and force writing LAME Tag");

    ps.println("  ATH related:");
    ps.println("    --noath         turns ATH down to a flat noise floor");
    ps.println("    --athshort      ignore GPSYCHO for short blocks, use ATH only");
    ps.println("    --athonly       ignore GPSYCHO completely, use ATH only");
    ps.println("    --athtype n     selects between different ATH types [0-4]");
    ps.println("    --athlower x    lowers ATH by x dB");
    ps.println("    --athaa-type n  ATH auto adjust: 0 'no' else 'loudness based'");
    ps.println("    --athaa-sensitivity x  activation offset in -/ps.println(dB for ATH auto-adjustment");
    ps.println();

    ps.println("  PSY related:");
    ps.println("    --short         use short blocks when appropriate");
    ps.println("    --noshort       do not use short blocks");
    ps.println("    --allshort      use only short blocks");
    ps.println("    --temporal-masking x   x=0 disables, x=1 enables temporal masking effect");
    ps.println("    --nssafejoint   M/S switching criterion");
    ps.println("    --nsmsfix <arg> M/S switching tuning [effective 0-3.5]");
    ps.println("    --interch x     adjust inter-channel masking ratio");
    ps.println("    --ns-bass x     adjust masking for sfbs  0 -  6 (long)  0 -  5 (short)");
    ps.println("    --ns-alto x     adjust masking for sfbs  7 - 13 (long)  6 - 10 (short)");
    ps.println("    --ns-treble x   adjust masking for sfbs 14 - 21 (long) 11 - 12 (short)");
    ps.println("    --ns-sfb21 x    change ns-treble by x dB for sfb21");
    ps.println("    --shortthreshold x,y  short block switching threshold,");
    ps.println("                          x for L/R/M channel, y for S channel");
    ps.println("  Noise Shaping related:");
    ps.println("    --substep n     use pseudo substep noise shaping method types 0-2");

    ps.println("  experimental switches:");
    ps.println("    -X n[,m]        selects between different noise measurements");
    ps.println("                    n for long block, m for short. if m is omitted, m = n");
    ps.println("    -Y              lets LAME ignore noise in sfb21, like in CBR");
    ps.println("    -Z [n]          currently no effects");

    ps.println("  MP3 header/stream options:");
    ps.println("    -e <emp>        de-emphasis n/5/c  (obsolete)");
    ps.println("    -c              mark as copyright");
    ps.println("    -o              mark as non-original");
    ps.println("    -p              error protection.  adds 16 bit checksum to every frame");
    ps.println("                    (the checksum is computed correctly)");
    ps.println("    --nores         disable the bit reservoir");
    ps.println("    --strictly-enforce-ISO   comply as much as possible to ISO MPEG spec");
    ps.println();
    ps.println("  Filter options:");
    ps.println("  --lowpass <freq>        frequency(kHz), lowpass filter cutoff above freq");
    ps.println("  --lowpass-width <freq>  frequency(kHz) - default 15%% of lowpass freq");
    ps.println("  --highpass <freq>       frequency(kHz), highpass filter cutoff below freq");
    ps.println("  --highpass-width <freq> frequency(kHz) - default 15%% of highpass freq");
    ps.println("  --resample <sfreq>  sampling frequency of output file(kHz)- default=automatic");

    ps.println("  ID3 tag options:");
    ps.println("    --tt <title>    audio/song title (max 30 chars for version 1 tag)");
    ps.println("    --ta <artist>   audio/song artist (max 30 chars for version 1 tag)");
    ps.println("    --tl <album>    audio/song album (max 30 chars for version 1 tag)");
    ps.println("    --ty <year>     audio/song year of issue (1 to 9999)");
    ps.println("    --tc <comment>  user-defined text (max 30 chars for v1 tag, 28 for v1.1)");
    ps.println("    --tn <track[/total]>   audio/song track number and (optionally) the total");
    ps.println("                           number of tracks on the original recording. (track");
    ps.println("                           and total each 1 to 255. just the track number");
    ps.println("                           creates v1.1 tag, providing a total forces v2.0).");
    ps.println("    --tg <genre>    audio/song genre (name or number in list)");
    ps.println("    --ti <file>     audio/song albumArt (jpeg/png/gif file, 128KB max, v2.3)");
    ps.println("    --tv <id=value> user-defined frame specified by id and value (v2.3 tag)");
    ps.println("    --add-id3v2     force addition of version 2 tag");
    ps.println("    --id3v1-only    add only a version 1 tag");
    ps.println("    --id3v2-only    add only a version 2 tag");
    ps.println("    --space-id3v1   pad version 1 tag with spaces instead of nulls");
    ps.println("    --pad-id3v2     same as '--pad-id3v2-size 128'");
    ps.println("    --pad-id3v2-size <value> adds version 2 tag, pad with extra <value> bytes");
    ps.println("    --genre-list    print alphabetically sorted ID3 genre list and exit");
    ps.println("    --ignore-tag-errors  ignore errors in values passed for tags");
    ps.println();
    ps.println("    Note: A version 2 tag will NOT be added unless one of the input fields");
    ps.println("    won't fit in a version 1 tag (e.g. the title string is longer than 30");
    ps.println("    characters), or the '--add-id3v2' or '--id3v2-only' options are used,");
    ps.println("    or output is redirected to stdout.");
    ps.println();
    ps.println("Misc:");
    ps.println();
    ps.println("    --license       print License information");
  }

  /**
   * Print bit rates of MPEG versions.
   */
  public final void printBitrates(final PrintStream ps) {
    printBitrate(ps, "1", 1, 1);
    printBitrate(ps, "2", 2, 0);
    printBitrate(ps, "2.5", 4, 0);
    ps.println();
  }

  /**
   * Print bit rates of MPEG version.
   */
  private void printBitrate(final PrintStream ps, final String version,
                            final int d, final int indx) {
    int nBitrates = 14;
    if (d == 4) {
      nBitrates = 8;
    }
    ps.printf(
        "MPEG-%-3s layer III sample frequencies (kHz):  %2d  %2d  %g",
        version, 32 / d, 48 / d, 44.1 / d);
    ps.println();
    ps.println("bitrates (kbps):");
    for (int i = 1; i <= nBitrates; i++) {
      ps.printf(" %2d", Tables.bitrate_table[indx][i]);
    }
    ps.println();
  }

  /**
   * Print pre-setting command line syntax.
   */
  public final void printPresets(final PrintStream ps) {
    ps.println();
    ps.println("The --preset switches are aliases over LAME settings.");
    ps.println();
    ps.println();
    ps.println("To activate these presets:");
    ps.println();
    ps.println("   For VBR modes (generally highest quality):");
    ps.println();
    ps.println("     \"--preset medium\" This preset should provide near transparency");
    ps.println("                             to most people on most music.");
    ps.println();
    ps.println("     \"--preset standard\" This preset should generally be transparent");
    ps.println("                             to most people on most music and is already");
    ps.println("                             quite high in quality.");
    ps.println();
    ps.println("     \"--preset extreme\" If you have extremely good hearing and similar");
    ps.println("                             equipment, this preset will generally provide");
    ps.println("                             slightly higher quality than the \"standard\"");
    ps.println("                             mode.");
    ps.println();
    ps.println("   For CBR 320kbps (highest quality possible from the --preset switches):");
    ps.println();
    ps.println("     \"--preset insane\"  This preset will usually be overkill for most");
    ps.println("                             people and most situations, but if you must");
    ps.println("                             have the absolute highest quality with no");
    ps.println("                             regard to filesize, this is the way to go.");
    ps.println();
    ps.println("   For ABR modes (high quality per given bitrate but not as high as VBR):");
    ps.println();
    ps.println("     \"--preset <kbps>\"  Using this preset will usually give you good");
    ps.println("                             quality at a specified bitrate. Depending on the");
    ps.println("                             bitrate entered, this preset will determine the");
    ps.println("                             optimal settings for that particular situation.");
    ps.println("                             While this approach works, it is not nearly as");
    ps.println("                             flexible as VBR, and usually will not attain the");
    ps.println("                             same level of quality as VBR at higher bitrates.");
    ps.println();
    ps.printf("The following options are also available for the corresponding profiles:");
    ps.println();
    ps.println("   <fast>        standard");
    ps.println("   <fast>        extreme");
    ps.println("                 insane");
    ps.println("   <cbr> (ABR Mode) - The ABR Mode is implied. To use it,");
    ps.println("                      simply specify a bitrate. For example:");
    ps.println("                      \"--preset 185\" activates this");
    ps.println("                      preset and uses 185 as an average kbps.");
    ps.println();
    ps.println("   \"fast\" - Enables the fast VBR mode for a particular profile.");
    ps.println();
    ps.println("   \"cbr\"  - If you use the ABR mode (read above) with a significant");
    ps.println("            bitrate such as 80, 96, 112, 128, 160, 192, 224, 256, 320,");
    ps.println("            you can use the \"cbr\" option to force CBR mode encoding");
    ps.println("            instead of the standard abr mode. ABR does provide higher");
    ps.println("            quality but CBR may be useful in situations such as when");
    ps.println("            streaming an mp3 over the internet may be important.");
    ps.println();
    ps.println("    For example:");
    ps.println();
    ps.println("    \"--preset fast standard <input file> <output file>\"");
    ps.println(" or \"--preset cbr 192 <input file> <output file>\"");
    ps.println(" or \"--preset 172 <input file> <output file>\"");
    ps.println(" or \"--preset extreme <input file> <output file>\"");
    ps.println();
    ps.println();
    ps.println("A few aliases are also available for ABR mode:");
    ps.println("phone => 16kbps/mono        phon+/lw/mw-eu/sw => 24kbps/mono");
    ps.println("mw-us => 40kbps/mono        voice => 56kbps/mono");
    ps.println("fm/radio/tape => 112kbps    hifi => 160kbps");
    ps.println("cd => 192kbps               studio => 256kbps");
  }

}
