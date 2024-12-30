/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.utilities.api;

import com.google.common.base.Preconditions;
import java.awt.Desktop;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.*;
import org.openide.filesystems.FileObject;

/**
 * Various convenience functions.
 */
public class Utilities
{

    private static final Logger LOGGER = Logger.getLogger(Utilities.class.getName());
    private static long firstTimeLogStampEpochMillis = -1;
    private static boolean changedRootLogger = false;

    /**
     * Make logging message include a time stamp in milliseconds, relative to the time of the first logged message in the application.
     * <p>
     * Note: this will impact other Logging of other modules as well, but if not registered explicitly, class will be displayed as null.
     *
     * @param logger The logger for which to apply the new logging message
     */
    public static void installTimeStampLogging(Logger logger)
    {
        Formatter formatter = new Formatter()
        {
            @Override
            public String format(LogRecord lr)
            {
                if (firstTimeLogStampEpochMillis == -1)
                {
                    firstTimeLogStampEpochMillis = lr.getMillis();
                }
                return String.format("%s-%06d [%s]: %s\n",
                        lr.getLevel(),
                        lr.getMillis() - firstTimeLogStampEpochMillis,
                        removeLeadingPackageName(lr.getSourceClassName()),
                        lr.getMessage());
            }
        };

        // Need custom handler to get class/method names !
        // LogRecord API doc: Therefore, if a logging Handler wants to pass off a LogRecord to another thread, or to transmit it over RMI, and 
        // if it wishes to subsequently obtain method name or class name information it should call one of getSourceClassName or getSourceMethodName 
        // to force the values to be filled in.        
        Handler handler = new Handler()
        {
            @Override
            public void publish(LogRecord lr)
            {
                lr.getSourceMethodName();
            }

            @Override
            public void flush()
            {
            }

            @Override
            public void close() throws SecurityException
            {
            }
        };
        logger.addHandler(handler);


        if (!changedRootLogger)
        {
            for (Handler h : Logger.getLogger("").getHandlers())
            {
                // Actions to be taken on the root loggers
                h.setFormatter(formatter);
            }
            changedRootLogger = true;
        }
    }

    public static boolean isRunFromNetbeansIDE()
    {
        return System.getProperty("run.from.ide") != null;
    }

    public static boolean isWindows()
    {
        return OSType.DETECTED.equals(OSType.Windows);
    }

    public static boolean isMac()
    {
        return OSType.DETECTED.equals(OSType.MacOS);
    }

    public static boolean isLinux()
    {
        return OSType.DETECTED.equals(OSType.Linux);
    }

    /**
     * Check if a class is a static class.
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public static <T> boolean isStatic(Class<T> clazz)
    {
        return Modifier.isStatic(clazz.getModifiers());
    }

    /**
     * Check if a class is an inner class (ie a non-static nested class).
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public static <T> boolean isInnerClass(Class<T> clazz)
    {
        return clazz.getDeclaringClass() != null && !isStatic(clazz);
    }

    /**
     * Downloads from a (http/https) URL and saves to a file.
     * <p>
     *
     * @param file               File to write. Parent directory will be created if necessary
     * @param url                http/https url to connect
     * @param secsConnectTimeout Seconds to wait for connection establishment
     * @param secsReadTimeout    Read timeout in seconds - trasmission will abort if it freezes more than this
     * @param ph                 If not null, starts and updates it while download is progressing using expectedFileSizeKb
     * @param expectedFileSizeKb
     * @throws IOException
     */
    public static void loadFileFromURL(final Path file, final URL url, int secsConnectTimeout, int secsReadTimeout, ProgressHandle ph, int expectedFileSizeKb) throws IOException
    {
        Files.createDirectories(file.getParent()); // make sure parent dir exists , this can throw exception

        LOGGER.log(Level.INFO, "loadFileFromURL() file={0} url={1}", new Object[]
        {
            file, url
        });

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // can throw exception if bad url
        if (secsConnectTimeout > 0)
        {
            conn.setConnectTimeout(secsConnectTimeout * 1000);
        }
        if (secsReadTimeout > 0)
        {
            conn.setReadTimeout(secsReadTimeout * 1000);
        }


        // Get file size to update the ProgressHandler
        if (ph != null)
        {
            ph.switchToDeterminate(expectedFileSizeKb);
        }


        try (InputStream is = conn.getInputStream())
        {
            try (BufferedInputStream in = new BufferedInputStream(is); OutputStream fout = Files.newOutputStream(file))
            {
                final byte data[] = new byte[8192];
                int count;
                int countTotal = 0;
                while ((count = in.read(data)) > 0)
                {
                    fout.write(data, 0, count);
                    countTotal += count;
                    if (ph != null)
                    {
                        ph.progress(countTotal / 1024);
                    }
                }
            }
        } catch (java.io.IOException ex)
        {
            throw ex;
        }

    }


    /**
     * Get file size from an URL.
     *
     * @param url
     * @return File size in bytes, or -1 if file content length is unknown
     * @throws java.io.IOException
     */
    static public long getFileSize(URL url) throws IOException
    {
        HttpURLConnection conn = null;

        try
        {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getContentLengthLong();
        } finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
    }

    /**
     * @return Complete absolute path from where the application was initialized.
     */
    public static String getCurrentDir()
    {
        String str = System.getProperty("user.dir");
        return str;
    }

    /**
     * Check if a locale is using latin alphabet.
     *
     * @param locale
     * @return
     */
    public static final boolean isLatin(Locale locale)
    {
        return !(locale.getLanguage().equals("zh") 
                || locale.getLanguage().equals("ja")
                || locale.getLanguage().equals("ru")
                || locale.getLanguage().equals("uk")
                );
    }

    /**
     * Check if a directory is empty.
     * <p>
     * Best performance as the Java 8 Files.list() returns a lazily populated stream (avoid reading all files).
     *
     * @param dirPath
     * @return
     * @throws IOException
     */
    public static boolean isEmpty(Path dirPath) throws IOException
    {
        if (Files.isDirectory(dirPath))
        {
            try (Stream<Path> entries = Files.list(dirPath))
            {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }

    /**
     * Extract from text all http://xxx or https://xxx URL strings as URLs.
     * <p>
     * Malformed URLs are ignored.
     *
     * @param text
     * @return
     */
    public static List<URL> extractHttpURLs(String text)
    {
        List<URL> res = new ArrayList<>();

        Scanner s = new Scanner(text);
        s.findAll("https?://.*").forEach(r -> 
        {
            String str = r.group();
            try
            {
                URL url = new URL(str);
                res.add(url);
            } catch (MalformedURLException ex)
            {
                LOGGER.log(Level.WARNING, "extractHttpURLs() Invalid internet link={0} in text={1}. ex={2}", new Object[]
                {
                    str, text,
                    ex.getMessage()
                });
            }
        });
        s.close();

        return res;
    }

    /**
     * Extract from text all file:/xxx URL strings as Files.
     * <p>
     * Malformed URLs are ignored.
     *
     * @param text
     * @return
     */
    public static List<File> extractFileURLsAsFiles(String text)
    {
        List<File> res = new ArrayList<>();

        Scanner s = new Scanner(text);
        s.findAll("file:/.*").forEach(r -> 
        {
            String str = r.group();
            try
            {
                File f = new File(new URL(str).toURI());
                res.add(f);
            } catch (MalformedURLException | URISyntaxException ex)
            {
                LOGGER.log(Level.WARNING, "extractFileURIsAsFiles() Invalid file URL/URI={0} in text={1}, ex={2}", new Object[]
                {
                    str, text,
                    ex.getMessage()
                });
            }
        });
        s.close();

        return res;
    }

    /**
     * @param str
     * @param ext
     * @return True if str ends with ext, ignoring case.
     */
    public static boolean endsWithIgnoreCase(String str, String ext)
    {
        if (str.trim().length() < ext.length())
        {
            return false;
        }
        return str.substring(str.length() - ext.length()).compareToIgnoreCase(ext) == 0;
    }

    /**
     * Return the extension (without the ".")
     *
     * @param fileName
     * @return Empty string if no dot.
     */
    public static String getExtension(String fileName)
    {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i >= 0)
        {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    /**
     * Replace the path extension (the trailing ".something") of filename by ext.
     * <p>
     * If filename has no path extension just add ext.
     *
     * @param filename
     * @param ext      A string without spaces in it. If ext does not start with "." it will be added. If ext is empty then extension is removed.
     * @return The new filename with extension replaced.
     */
    public static String replaceExtension(String filename, String ext)
    {
        Objects.requireNonNull(filename);
        Objects.requireNonNull(ext);
        if (ext.contains(" ") || ext.equals("."))
        {
            throw new IllegalArgumentException("filename=" + filename + " ext=" + ext);
        }
        if (!ext.isEmpty() && !ext.startsWith("."))
        {
            ext = "." + ext;
        }
        int index = filename.lastIndexOf('.');
        if (index == -1)
        {
            index = filename.length();
        }
        return filename.substring(0, index) + ext;
    }

    /**
     * Return a new File with the extension set to ext.
     * <p>
     *
     * @param file
     * @param ext  A string without spaces in it. If ext does not start with "." it will be added. If ext is empty then extension is removed.
     * @return The new File with extension replaced.
     * @see #replaceExtension(java.lang.String, java.lang.String)
     */
    public static File replaceExtension(File file, String ext)
    {
        Objects.requireNonNull(file);
        File res = new File(replaceExtension(file.getAbsolutePath(), ext));
        return res;
    }

    /**
     * Get the string of all the collection elements in brackets [], but limited to maxLength.
     *
     * @param collection
     * @param maxLength  Must be &gt;= 5
     * @return A string like [one,two,th...] if maxLength is 15
     */
    public static String truncateWithDots(Collection<?> collection, int maxLength)
    {
        if (maxLength < 5)
        {
            throw new IllegalArgumentException("collection=" + collection + " maxLength=" + maxLength);
        }
        String s = collection.toString();
        String prefix = "", suffix = "";
        if (s.startsWith("["))
        {
            s = s.substring(1, s.length() - 1);   // Remove the []      
            prefix = "[";
            suffix = "]";
        }
        return prefix + Utilities.truncateWithDots(s, maxLength - 2) + suffix;
    }

    /**
     * Return a string based on s whose length can't exceed maxLength, with "..." at the end if s was truncated.
     * <p>
     * If s length is &lt;= maxSize then return s. Otherwise return the first chars and append ".." or "...", in order to have length=maxSize.<p>
     * Example: return "Clav..." for s="Clavinet" and maxLength=7
     *
     * @param s
     * @param maxLength Must be &gt;= 3
     * @return
     */
    public static String truncateWithDots(String s, int maxLength)
    {
        if (maxLength < 3)
        {
            throw new IllegalArgumentException("s=" + s + " maxLength=" + maxLength);
        }
        String res = s;
        if (s.length() > maxLength)
        {
            if (maxLength == 3)
            {
                res = s.substring(0, 1) + "..";
            } else
            {
                res = s.substring(0, maxLength - 3) + "...";
            }
        }
        return res;
    }

    /**
     * Return the truncated string if s exceeds max chars.
     *
     * @param s
     * @param max
     * @return
     */
    static public String truncate(String s, int max)
    {
        if (s.length() > max)
        {
            s = s.substring(0, max);
        }
        return s;
    }


    /**
     * Generate all the permutations of the specified smallArray in the result list.
     * <p>
     *
     * @param <T>
     * @param smallArray Size must be &lt;= 9, otherwise use too much memory (result size grow like N!).
     * @param n          Nb of elements to be considered
     * @param result     The list of all smallArray permutations.
     */
    public static <T> void heapPermutation(T smallArray[], int n, List<T[]> result)
    {
        if (smallArray.length > 9)
        {
            throw new IllegalArgumentException("smallArray.size()=" + smallArray.length + " n=" + n + " result=" + result);
        }
        // Reference: https://stackoverflow.com/questions/29042819/heaps-algorithm-permutation-generator
        if (n == 1)
        {
            T[] perm = smallArray.clone();
            result.add(perm);
        } else
        {
            heapPermutation(smallArray, n - 1, result);
            for (int i = 0; i < n - 1; i++)
            {
                if (n % 2 == 1)
                {
                    // if size is odd, swap first and last element                 
                    T temp = smallArray[0];
                    smallArray[0] = smallArray[n - 1];
                    smallArray[n - 1] = temp;
                } else
                {
                    // If size is even, swap ith and last element 
                    T temp = smallArray[i];
                    smallArray[i] = smallArray[n - 1];
                    smallArray[n - 1] = temp;
                }
                heapPermutation(smallArray, n - 1, result);
            }
        }
    }

    static public String toString(byte[] buf)
    {
        char[] chars = new char[buf.length];
        for (int i = 0; i < buf.length; i++)
        {
            chars[i] = (char) buf[i];
        }
        return String.valueOf(chars);

    }

    public static class StringDelegate
    {

        public String toString(Object o)
        {
            return o.toString();
        }
    }

    /**
     * Swap the contents of 2 lists.
     *
     * @param l1
     * @param l2
     */
    public static <T> void swapList(List<T> l1, List<T> l2)
    {
        ArrayList<T> tmp = new ArrayList<>();
        tmp.addAll(l1);
        l1.clear();
        l1.addAll(l2);
        l2.clear();
        l2.addAll(tmp);
    }

    /**
     * Return the first key of a map for which value = v.
     *
     * @param map Map
     * @param v
     * @return Object Null if found no value=v
     */
    public static Object reverseGet(Map<?, ?> map, Object v)
    {
        for (Object key : map.keySet())
        {
            if (map.get(key) == v)
            {
                return key;
            }
        }
        return null;
    }

    /**
     * Get the list index of the first object which is instance of a specified class.
     *
     * @param list
     * @param clazz
     * @return int -1 if no object is instance of clazz.
     */
    public static int indexOfInstance(List<?> list, Class<?> clazz)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (clazz.isInstance(list.get(i)))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return a new string which is a copy of s with as many as required es strings appended to reach length l (or more).
     * <p>
     * If s length is more than l nothing is done.
     *
     * @param s
     * @param l
     * @param es
     * @return
     */
    public static String expand(String s, int l, String es)
    {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < l)
        {
            sb.append(es);
        }
        return sb.toString();
    }

    /**
     * Get the index of an object reference in a List. The search uses direct equality '==', NOT the 'equals' function.
     *
     * @param o     The Object to search.
     * @param array The List of Objects to be searched.
     * @return The index of object o, -1 if not found.
     */
    public static int getObjectRefIndex(Object o, List<? extends Object> array)
    {
        for (int i = 0; i < array.size(); i++)
        {
            if (array.get(i) == o)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the index of the first object whose toString() function match str (ignoring case).
     *
     * @param list A list of object.
     * @param str  The string to search (ignoring case)
     * @return The index of matching string, -1 if not found.
     */
    public static int indexOfStringIgnoreCase(List<? extends Object> list, String str)
    {
        int index = 0;
        for (Object o : list)
        {
            if (o.toString().compareToIgnoreCase(str) == 0)
            {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Extract the contents of a .zip resource file to a destination directory.
     * <p>
     *
     * @param <T>
     * @param myClass     The class used to find the zipResource.
     * @param zipResource Must end with ".zip".
     * @param destDir     The path of the destination directory, which must exist.
     * @param overwrite   If true overwrite files in the destination directory
     * @return The list of created files in the destination directory.
     */
    public static <T> List<File> extractZipResource(Class<T> myClass, String zipResource, Path destDir, boolean overwrite)
    {
        if (myClass == null || zipResource == null || !zipResource.toLowerCase().endsWith(".zip") || !Files.isDirectory(destDir))
        {
            throw new IllegalArgumentException("myClass=" + myClass + " zipResource=" + zipResource + " destDir=" + destDir);
        }

        LOGGER.log(Level.FINE, "extractZipResource() -- myClass={0} zipResource={1} destDir={2}", new Object[]
        {
            myClass, zipResource, destDir
        });
        ArrayList<File> res = new ArrayList<>();
        try (InputStream is = myClass.getResourceAsStream(zipResource); BufferedInputStream bis = new BufferedInputStream(is); ZipInputStream zis = new ZipInputStream(
                bis))
        {
            ZipEntry entry;
            byte[] buffer = new byte[2048];
            while ((entry = zis.getNextEntry()) != null)
            {
                // Build destination file
                File destFile = destDir.resolve(entry.getName()).toFile();
                LOGGER.log(Level.FINE, "extractZipResource() processing zipEntry={0} destFile={1}", new Object[]
                {
                    entry.getName(), destFile.getAbsolutePath()
                });
                if (entry.isDirectory())
                {
                    // Directory, recreate if not present
                    if (!destFile.exists() && !destFile.mkdirs())
                    {
                        LOGGER.log(Level.WARNING, "extractZipResource() can''t create destination folder : {0}", destFile.getAbsolutePath());
                    }
                    continue;
                }
                // Plain file, copy it
                if (!overwrite && destFile.exists())
                {
                    continue;
                }
                try (FileOutputStream fos = new FileOutputStream(destFile); BufferedOutputStream bos = new BufferedOutputStream(fos,
                        buffer.length))
                {
                    int len;
                    while ((len = zis.read(buffer)) > 0)
                    {
                        bos.write(buffer, 0, len);
                    }
                }
                res.add(destFile);
            }
        } catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "extractZipResource() problem extracting resource for myClass={0} zipResource={1} ex={2}", new Object[]
            {
                myClass,
                zipResource, ex.getMessage()
            });
        }
        return res;
    }

    /**
     * Copy a resource file to a target file.
     * <p>
     * Replace existing target file if already present.
     *
     * @param <T>
     * @param c
     * @param resourceFilePath The resource path for class c
     * @param targetFile
     * @return False if a problem occured
     */
    public static <T> boolean copyResource(Class<T> c, String resourceFilePath, Path targetFile)
    {
        if (c == null || resourceFilePath == null || resourceFilePath.isEmpty() || targetFile == null)
        {
            throw new IllegalArgumentException("c=" + c + " resourceFilePath=" + resourceFilePath + " targetFile=" + targetFile);
        }
        boolean b = false;
        try (InputStream in = c.getResourceAsStream(resourceFilePath))
        {
            if (in != null)
            {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                b = true;
            } else
            {
                LOGGER.log(Level.SEVERE, "copyResource() resource not found. c={0} resourcePath={1}", new Object[]
                {
                    c, resourceFilePath
                });
            }
        } catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE,
                    "copyResource() problem copying resource. c=" + c + ", resourcePath=" + resourceFilePath + ", targetFile=" + targetFile,
                    ex);
        }

        return b;
    }

    /**
     * Convert a font into a string that can be decoded by Font.decode()
     *
     * @param font
     * @return
     */
    public static String fontAsString(Font font)
    {
        String style = "PLAIN";
        if (font.isItalic() && font.isBold())
        {
            style = "BOLDITALIC";
        } else if (font.isItalic())
        {
            style = "ITALIC";
        } else if (font.isBold())
        {
            style = "BOLD";
        }
        return font.getFamily() + "-" + style + "-" + font.getSize();
    }

    /**
     * Get each element toString() called, one per line.
     *
     * @param <K>
     * @param <V>
     * @param map           If it's a NavigableMap, use its ascending order.
     * @param prefixPostfix 1st/2nd string is a prefix/postfix to add on each line
     * @return
     */
    public static <K, V> String toMultilineString(Map<K, V> map, String... prefixPostfix)
    {
        if (map == null)
        {
            return "null";
        }
        String prefix = prefixPostfix.length >= 1 ? prefixPostfix[0] : "";
        String postfix = prefixPostfix.length >= 2 ? prefixPostfix[1] : "";
        var joiner = new StringJoiner("\n", "[", "]");
        if (map instanceof NavigableMap nMap)
        {
            nMap.navigableKeySet().forEach(k -> joiner.add(prefix + (k == null ? "null" : k.toString() + " -> " + nMap.get(k)) + postfix));
        } else
        {
            map.keySet().forEach(k -> joiner.add(prefix + (k == null ? "null" : k.toString() + " -> " + map.get(k)) + postfix));
        }

        return joiner.toString();
    }

    /**
     * Get each element toString() called, one per line.
     *
     * @param col
     * @param prefixPostfix 1st/2nd string is a prefix/postfix to add on each line
     * @return
     */
    public static String toMultilineString(Collection<?> col, String... prefixPostfix)
    {
        Preconditions.checkNotNull(col);
        String prefix = prefixPostfix.length >= 1 ? prefixPostfix[0] : "";
        String postfix = prefixPostfix.length >= 2 ? prefixPostfix[1] : "";
        var joiner = new StringJoiner("\n", "[", "]");
        col.forEach(e -> joiner.add(prefix + (e == null ? "null" : e.toString()) + postfix));
        return joiner.toString();
    }

    /**
     * Get each element toString() called, one per line.
     *
     * @param array
     * @return
     */
    public static <T> String toMultilineString(T array[])
    {
        return toMultilineString(Arrays.asList(array));
    }

    /**
     * A debug string for a PropertyChangeEvent.
     *
     * @param e
     * @param maxStringLength Truncate oldValue/newValue string representation to maxStringLength. 30 is used if not specified.
     * @return
     */
    public static String toDebugString(PropertyChangeEvent e, int... maxStringLength)
    {
        int max = maxStringLength.length == 0 ? 30 : maxStringLength[0];
        return "e.src=" + e.getSource().getClass().getSimpleName()
                + "   e.prop=" + e.getPropertyName()
                + "   e.old=" + (e.getOldValue() == null ? "null" : truncateWithDots(e.getOldValue().toString(), max))
                + "   e.new=" + (e.getNewValue() == null ? "null" : truncateWithDots(e.getNewValue().toString(), max));
    }

    /**
     * Gets the base location of the given class. Manage all OS variations and possible problems in characters...
     * <p>
     * If the class is directly on the file system (e.g., "/path/to/my/package/MyClass.class") then it will return the base directory (e.g., "file:/path/to").
     * </p>
     * <p>
     * If the class is within a JAR file (e.g., "/path/to/my-jar.jar!/my/package/MyClass.class") then it will return the path to the JAR (e.g.,
     * "file:/path/to/my-jar.jar").
     * </p>
     *
     * @param c The class whose location is desired.
     */
    public static URL getLocation(final Class<?> c)
    {
        if (c == null)
        {
            return null; // could not load the class
        }
        // try the easy way first
        try
        {
            final URL codeSourceLocation = c.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceLocation != null)
            {
                return codeSourceLocation;
            }
        } catch (final SecurityException e)
        {
            // NB: Cannot access protection domain.
        } catch (final NullPointerException e)
        {
            // NB: Protection domain or code source is null.
        }

        // NB: The easy way failed, so we try the hard way. We ask for the class
        // itself as a resource, then strip the class's path from the URL string,
        // leaving the base path.
        // get the class's raw resource path
        final URL classResource = c.getResource(c.getSimpleName() + ".class");
        if (classResource == null)
        {
            return null; // cannot find class resource
        }
        final String url = classResource.toString();
        final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
        if (!url.endsWith(suffix))
        {
            return null; // weird URL
        }
        // strip the class's path from the URL string
        final String base = url.substring(0, url.length() - suffix.length());

        String path = base;

        // remove the "jar:" prefix and "!/" suffix, if present
        if (path.startsWith("jar:"))
        {
            path = path.substring(4, path.length() - 2);
        }

        try
        {
            return new URL(path);
        } catch (final MalformedURLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts the given {@link URL} to its corresponding {@link File}.
     * <p>
     * This method is similar to calling {@code new File(url.toURI())} except that it also handles "jar:file:" U Sgs, returning the path to the JAR file.
     * </p>
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a file.
     */
    public static File urlToFile(final URL url)
    {
        return url == null ? null : urlToFile(url.toString());
    }

    /**
     * Converts the given URL string to its corresponding {@link File}.
     *
     * @param url The URL to convert.
     * @return A file path suitable for use with e.g. {@link FileInputStream}
     * @throws IllegalArgumentException if the URL does not correspond to a file.
     */
    public static File urlToFile(final String url)
    {
        String path = url;
        if (path.startsWith("jar:"))
        {
            // remove "jar:" prefix and "!/" suffix
            final int index = path.indexOf("!/");
            path = path.substring(4, index);
        }
        try
        {
            if (org.openide.util.Utilities.isWindows() && path.matches("file:[A-Za-z]:.*"))
            {
                path = "file:/" + path.substring(5);
            }
            return new File(new URL(path).toURI());
        } catch (final MalformedURLException e)
        {
            // NB: URL is not completely well-formed.
        } catch (final URISyntaxException e)
        {
            // NB: URL is not completely well-formed.
        }
        if (path.startsWith("file:"))
        {
            // pass through the URL as-is, minus "file:" prefix
            path = path.substring(5);
            return new File(path);
        }
        throw new IllegalArgumentException("Invalid URL: " + url);
    }

    /**
     * Load a file content as a string.
     *
     * @param fo
     * @return Null if problem reading file
     */
    static public String loadFileAsString(FileObject fo)
    {
        StringWriter result = new StringWriter();
        int curChar;
        try (InputStream is = fo.getInputStream(); BufferedReader in = new BufferedReader(new InputStreamReader(is)))
        {
            while ((curChar = in.read()) != -1)
            {
                result.write(curChar);
            }
        } catch (IOException ex)
        {
            return null;
        }

        return result.toString();
    }

    /**
     * Get the longest key which is contained in text.
     *
     * @param text If null return null.
     * @param keys
     * @return Can be null if none of the keys are contained in text.
     */
    public static String getLongestMatch(String text, Collection<String> keys)
    {
        Objects.requireNonNull(keys);
        if (text == null)
        {
            return null;
        }
        String res = null;
        int maxKeySize = 0;
        for (String key : keys)
        {
            if (text.contains(key) && key.length() > maxKeySize)
            {
                maxKeySize = key.length();
                res = key;
            }
        }
        return res;
    }

    /**
     * Finds the first occurrence of the pattern in the text.
     * <p>
     * Use Knuth-Morris-Pratt Algorithm for Pattern Matching.
     *
     * @return -1 if not found.
     */
    public static int indexOf(byte[] data, byte[] pattern)
    {
        if (data.length == 0)
        {
            return -1;
        }

        int[] failure = computeFailure(pattern);
        int j = 0;

        for (int i = 0; i < data.length; i++)
        {
            while (j > 0 && pattern[j] != data[i])
            {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i])
            {
                j++;
            }
            if (j == pattern.length)
            {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Get all the files matching fnFilter in dirTree (and its subdirectories).
     * <p>
     * Hidden subdirectories are not searched.
     *
     * @param dirTree
     * @param fnFilter        If null accept all files.
     * @param ignoreDirPrefix Subdirs starting with this prefix are not traversed. If null accept all subdirectories.
     * @param maxDepth
     * @return
     */
    static public HashSet<Path> listFiles(File dirTree, final FilenameFilter fnFilter, final String ignoreDirPrefix, int maxDepth)
    {
        if (dirTree == null || maxDepth < 0)
        {
            throw new IllegalArgumentException("dirTree=" + dirTree + " fnFilter=" + fnFilter + " ignoreDirPrefix=" + ignoreDirPrefix);
        }
        final HashSet<Path> pathSet = new HashSet<>();

        try
        {
            Files.walkFileTree(dirTree.toPath(), EnumSet.noneOf(FileVisitOption.class
            ), maxDepth, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                {
                    // Readable dir ?
                    File fDir = dir.toFile();
                    if (fDir.isHidden() || (ignoreDirPrefix != null && dir.getFileName().toString().startsWith(ignoreDirPrefix)))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                {
                    if (fnFilter == null || fnFilter.accept(filePath.getParent().toFile(), filePath.getFileName().toString()))
                    {
                        pathSet.add(filePath);
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException ex)
                {
                    LOGGER.log(Level.WARNING, "visitFileFailed() file={0}, ex={1}", new Object[]
                    {
                        file, ex.getLocalizedMessage()
                    });
                    return CONTINUE;
                }
            });
        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "listFile() IOException ex={0}. Some files may have not been listed.", ex.getMessage());
        }
        return pathSet;
    }

    /**
     * Open an URL in the system's external browser.
     * <p>
     * Unless silentError is true, user is notified if an error occured.
     *
     * @param url
     * @param silentError Do not notify user if error occured
     * @return False if an error occured
     */
    public static boolean openInBrowser(URL url, boolean silentError)
    {
        String errMsg = null;
        if (Desktop.isDesktopSupported())
        {
            try
            {
                Desktop.getDesktop().browse(url.toURI());
            } catch (URISyntaxException | IOException | UnsupportedOperationException ex)
            {
                errMsg = ex.getMessage();

            }
        } else
        {
            errMsg = ResUtil.getString(Utilities.class, "ErrNoExternalCommand");
        }

        if (errMsg != null)
        {
            LOGGER.log(Level.WARNING, "openInBrowser() url={0}  ex={1}", new Object[]
            {
                url, errMsg
            });

            if (!silentError)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(errMsg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }


        return errMsg == null;
    }

    /**
     * Open a file in an external editor.
     * <p>
     * Unless silentError is true, user is notified if an error occured.
     *
     * @param file
     * @param silentError Do not notify user if error occured
     * @return False if an error occured
     */
    public static boolean openFile(File file, boolean silentError)
    {
        String errMsg = null;
        if (Desktop.isDesktopSupported())
        {
            try
            {
                Desktop.getDesktop().open(file);

            } catch (IOException | UnsupportedOperationException | IllegalArgumentException ex)
            {
                errMsg = ex.getLocalizedMessage();

            }
        } else
        {
            errMsg = ResUtil.getString(Utilities.class, "ErrNoExternalCommand");
        }

        if (errMsg != null)
        {
            LOGGER.log(Level.WARNING, "openFile() file={0}  ex={1}", new Object[]
            {
                file, errMsg
            });

            if (!silentError)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(errMsg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }

        return errMsg == null;
    }

    /**
     * Browse in a file browser a folder containing the specified file .
     * <p>
     * Unless silentError is true, user is notified if an error occured.
     *
     * @param file
     * @param silentError Do not notify user if error occured
     * @return False if an error occured
     */
    public static boolean browseFileDirectory(File file, boolean silentError)
    {
        String errMsg = null;
        if (org.openide.util.Utilities.isWindows())
        {
            // Desktop.browseFileDirectory is not implemented on WIN10 !!
            String path = file.getParentFile().getAbsolutePath();
            String completeCmd = "explorer.exe /select," + path;
            try
            {
                new ProcessBuilder(("explorer.exe " + completeCmd).split(" ")).start();
            } catch (IOException ex)
            {
                errMsg = ex.getLocalizedMessage();
            }
        } else if (Desktop.isDesktopSupported())
        {
            try
            {
                Desktop.getDesktop().browseFileDirectory(file);

            } catch (UnsupportedOperationException ex)
            {
                errMsg = ex.getLocalizedMessage();

            }
        } else
        {
            errMsg = ResUtil.getString(Utilities.class, "ErrNoExternalCommand");
        }

        if (errMsg != null)
        {
            LOGGER.log(Level.WARNING, "browseFileDirectory() file={0}  ex={1}", new Object[]
            {
                file, errMsg
            });

            if (!silentError)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(errMsg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }

        return errMsg == null;
    }

    /**
     * Shutdown an executor service in a clean way.
     * <p>
     * From https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html
     *
     * @param pool
     * @param waitTerminationTimeMs Time in milliseconds to wait for pool tasks to terminate themselves
     * @param waitCancelTimeMs      Time in milliseconds to wait for pool tasks to handle the cancel requests
     */
    static public void shutdownAndAwaitTermination(ExecutorService pool, long waitTerminationTimeMs, long waitCancelTimeMs)
    {
        pool.shutdown(); // Disable new tasks from being submitted
        try
        {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(waitTerminationTimeMs, TimeUnit.MILLISECONDS))
            {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(waitCancelTimeMs, TimeUnit.MILLISECONDS))
                {
                    LOGGER.log(Level.WARNING, "shutdownAndAwaitTermination() Pool did not terminate with waitTerminationTimeMs={0} and waitCancelTimeMs={1}",
                            new Object[]
                            {
                                waitTerminationTimeMs,
                                waitCancelTimeMs
                            });
                }
            }
        } catch (InterruptedException ie)
        {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }


    // ========================================================================
    // Private methods
    // ========================================================================
    /**
     * Computes the failure function using a boot-strapping process, where the pattern is matched against itself.
     */
    private static int[] computeFailure(byte[] pattern)
    {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++)
        {
            while (j > 0 && pattern[j] != pattern[i])
            {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i])
            {
                j++;
            }
            failure[i] = j;
        }
        return failure;
    }

    /**
     * If s=="org.jjazz.name", return "name".
     *
     * @param s
     * @return
     */
    private static String removeLeadingPackageName(String s)
    {
        int index = s.lastIndexOf(".");
        if (index == -1 || index == s.length() - 1)
        {
            return s;
        }
        return s.substring(index + 1);

    }

    /**
     * Get the OS type.
     */
    private enum OSType
    {
        Windows, MacOS, Linux, Other;
        public static final OSType DETECTED;

        static
        {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.contains("mac")) || (OS.contains("darwin")))
            {
                DETECTED = OSType.MacOS;
            } else if (OS.contains("win"))
            {
                DETECTED = OSType.Windows;
            } else if (OS.contains("nux"))
            {
                DETECTED = OSType.Linux;
            } else
            {
                DETECTED = OSType.Other;
            }
        }
    }

// ==============================================================================================
// Inner classes
// ==============================================================================================

}
