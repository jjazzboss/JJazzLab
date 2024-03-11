package org.jjazz.embeddedsynth.lame.lowlevel;

import java.io.*;

/**
 * @author Ken Händel
 */
public class ID3V2Decoder {

  private String imageMimeType;
  private byte[] imageBytes;
  private String commentLanguage;
  private String comment;
  private String album;
  private String interpret;
  private String albumInterpret;
  private String componist;
  private String cdNumber;
  private String genre;
  private String title;
  private String track;
  private String year;

  public static void main(String[] args) {
    try {
      ID3V2Decoder id3v2Decoder = new ID3V2Decoder();
      id3v2Decoder.read(new RandomAccessFile(args[0], "r"));
      System.out.println();
      FileOutputStream os = new FileOutputStream(new File("d:/out.jpg"));
      os.write(id3v2Decoder.getImageBytes());
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getImageMimeType() {
    return imageMimeType;
  }

  public void setImageMimeType(String imageMimeType) {
    this.imageMimeType = imageMimeType;
  }

  public byte[] getImageBytes() {
    return imageBytes;
  }

  public void setImageBytes(byte[] imageBytes) {
    this.imageBytes = imageBytes;
  }

  public String getCommentLanguage() {
    return commentLanguage;
  }

  public void setCommentLanguage(String commentLanguage) {
    this.commentLanguage = commentLanguage;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getAlbum() {
    return album;
  }

  public void setAlbum(String album) {
    this.album = album;
  }

  public String getInterpret() {
    return interpret;
  }

  public void setInterpret(String interpret) {
    this.interpret = interpret;
  }

  public String getAlbumInterpret() {
    return albumInterpret;
  }

  public void setAlbumInterpret(String albumInterpret) {
    this.albumInterpret = albumInterpret;
  }

  public String getComponist() {
    return componist;
  }

  public void setComponist(String componist) {
    this.componist = componist;
  }

  public String getCdNumber() {
    return cdNumber;
  }

  public void setCdNumber(String cdNumber) {
    this.cdNumber = cdNumber;
  }

  public String getGenre() {
    return genre;
  }

  public void setGenre(String genre) {
    this.genre = genre;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTrack() {
    return track;
  }

  public void setTrack(String track) {
    this.track = track;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public final void read(RandomAccessFile is) throws IOException {
    byte[] type = new byte[4];
    is.readFully(type);
    if (type[0] == 'I' && type[1] == 'D' && type[2] == '3') {
      byte[] pad = new byte[6];
      is.readFully(pad);
      pad[2] &= 127;
      pad[3] &= 127;
      pad[4] &= 127;
      pad[5] &= 127;
      int len = (((((pad[2] << 7) + pad[3]) << 7) + pad[4]) << 7)
          + pad[5];
      readTags(is, len);
    }
  }

  private void readTags(final RandomAccessFile is, int len)
      throws IOException {
    while (is.getFilePointer() < len) {
      if (readTag(is)) {
        break;
      }
    }
  }

  private boolean readTag(final RandomAccessFile is) throws IOException {

    // currently unsupported
    byte[] type = new byte[4];
    is.readFully(type);
    if (type[0] == 0) {
      // padding bytes? We are done.
      return true;
    }
    String entryType = getType(type);

    byte[] lenbuf = new byte[4];
    is.readFully(lenbuf);
    is.skipBytes(2);
    if ("APIC".equals(entryType)) {
      readImage(is, readLength(lenbuf));
    } else if ("COMM".equals(entryType)) {
      // not yet supported
      is.skipBytes((int) readLength(lenbuf));
      // readComment(is, readLength(lenbuf), enc);
    } else {
      int enc = is.read();
      String fieldValue = readField(is, entryType,
          (int) readLength(lenbuf), enc);
      if (entryType.equals("TALB")) {
        setAlbum(fieldValue);
      } else if (entryType.equals("TPE1")) {
        setInterpret(fieldValue);
      } else if (entryType.equals("TPE2")) {
        setAlbumInterpret(fieldValue);
      } else if (entryType.equals("TCOM")) {
        setComponist(fieldValue);
      } else if (entryType.equals("TPOS")) {
        setCdNumber(fieldValue);
      } else if (entryType.equals("TCON")) {
        setGenre(fieldValue);
      } else if (entryType.equals("TIT2")) {
        setTitle(fieldValue);
      } else if (entryType.equals("TRCK")) {
        setTrack(fieldValue);
      } else if (entryType.equals("TYER")) {
        setYear(fieldValue);
      }
    }
    return false;
  }

  private long readLength(byte[] lenbuf) {
    long val = (lenbuf[0] & 0xff) << 24;
    val += (lenbuf[1] & 0xff) << 16;
    val += (lenbuf[2] & 0xff) << 8;
    val += (lenbuf[3] & 0xff);
    return val;
  }

  private String readField(final RandomAccessFile is, String type, int len,
                           int enc) throws IOException {
    byte[] field = new byte[len];
    is.readFully(field, 0, len - 1);
    if (enc == 0) {
      return new String(field, 0, len - 1, "ISO-8859-1");
    } else if (enc == 1) {
      return new String(field, 0, len - 1, "UTF-16");
    } else if (enc == 3) {
      return new String(field, 0, len - 1, "UTF-8");
    }
    return "";
  }

  private void readImage(final RandomAccessFile is, long len)
      throws IOException, FileNotFoundException {
    is.read();
    int ch;
    StringBuilder mime = new StringBuilder();
    do {
      ch = is.read();
      if (ch > 0) {
        mime.append((char) ch);
      }
    } while (ch > 0);
    setImageMimeType(mime.toString());
    is.skipBytes(2);
    byte[] pic = new byte[(int) (len - mime.length() - 4)];
    is.readFully(pic);
    setImageBytes(pic);
  }

  private String getType(byte[] type) {
    return String.valueOf((char) type[0]) + String.valueOf((char) type[1])
        + String.valueOf((char) type[2])
        + String.valueOf((char) type[3]);
  }
}
