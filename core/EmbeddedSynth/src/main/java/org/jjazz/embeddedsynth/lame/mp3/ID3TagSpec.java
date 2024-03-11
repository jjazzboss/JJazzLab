package org.jjazz.embeddedsynth.lame.mp3;

import org.jjazz.embeddedsynth.lame.mp3.ID3Tag.MimeType;

import java.util.ArrayList;

public final class ID3TagSpec {
  int flags;
  int year;
  String title;
  String artist;
  String album;
  String comment;
  int track_id3v1;
  int genre_id3v1;
  byte[] albumart;
  int albumart_size;
  int padding_size;
  MimeType albumart_mimetype;
  ArrayList<String> values = new ArrayList<String>();
  int num_values;
  FrameDataNode v2_head, v2_tail;
}
