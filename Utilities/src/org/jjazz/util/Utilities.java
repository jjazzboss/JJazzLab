/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.util;

import java.awt.Font;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.openide.filesystems.FileObject;

/**
 * Various convenience functions.
 */
public class Utilities
{

    private static final Logger LOGGER = Logger.getLogger(Utilities.class.getName());

    /**
     * @return Complete absolute path from where the application was initialized.
     */
    public static String getCurrentDir()
    {
        String str = System.getProperty("user.dir");
        return str;
    }

    /**
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
     * @param path
     * @return
     */
    public static String getExtension(String path)
    {
        String extension = "";
        int i = path.lastIndexOf('.');
        if (i >= 0)
        {
            extension = path.substring(i + 1);
        }
        return extension;
    }

    /**
     * Replace the path extension (the trailing ".something") of filename by ext. If filename has no path extension just add ext.
     *
     * @param filename
     * @param ext A string without spaces in it. If ext does not start with "." it will be added.
     * @return The new filename with extension replaced.
     */
    public static String replaceExtension(String filename, String ext)
    {
        if (filename == null || ext == null || ext.isEmpty() || ext.contains(" "))
        {
            throw new IllegalArgumentException("filename=" + filename + " ext=" + ext);
        }
        if (!ext.startsWith("."))
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
     * Return a string based on s whose length can't exceed maxLength, with "..." at the end if s was truncated.
     * <p>
     * If s length is &lt;= maxSize then return s. Otherwise return the first chars and append ".." or "...", in order to have
     * length=maxSize.<p>
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
     * @param smallArray Size must be &lt;= 10, otherwise use too much memory (result size grow like N!).
     * @param size
     * @param n
     * @param result The li s t o f all smallArray permutations.
     */
    public static <T> void heapPermutation(T smallArray[], int size, int n, List<T[]> result)
    {
        if (smallArray.length > 10)
        {
            throw new IllegalArgumentException("smallArray.size()=" + smallArray.length + " size=" + size + " result=" + result);
        }
        if (size == 1)
        {
            T[] perm = smallArray.clone();
            result.add(perm);
        }

        for (int i = 0; i < size; i++)
        {
            heapPermutation(smallArray, size - 1, n, result);

            // if size is odd, swap first and last 
            // element 
            if (size % 2 == 1)
            {
                T temp = smallArray[0];
                smallArray[0] = smallArray[size - 1];
                smallArray[size - 1] = temp;
            } // If size is even, swap ith and last 
            // element 
            else
            {
                T temp = smallArray[i];
                smallArray[i] = smallArray[size - 1];
                smallArray[size - 1] = temp;
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
     * @param o The Object to search.
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
     * @param str The string to search (ignoring case)
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
     * Overwrite existing files.
     *
     * @param <T>
     * @param myClass The class used to find the zipResource.
     * @param zipResource Must end with ".zip".
     * @param destDir The path of the destination directory, which must exist.
     * @return The list of created files in the destination directory.
     */
    public static <T> List<File> extractZipResource(Class<T> myClass, String zipResource, Path destDir)
    {
        if (myClass == null || zipResource == null || !zipResource.toLowerCase().endsWith(".zip") || !Files.isDirectory(destDir))
        {
            throw new IllegalArgumentException("myClass=" + myClass + " zipResource=" + zipResource + " destDir=" + destDir);
        }

        LOGGER.fine("extractZipResource() -- myClass=" + myClass + " zipResource=" + zipResource + " destDir=" + destDir);
        ArrayList<File> res = new ArrayList<>();
        try (InputStream is = myClass.getResourceAsStream(zipResource);
                BufferedInputStream bis = new BufferedInputStream(is);
                ZipInputStream zis = new ZipInputStream(bis))
        {
            ZipEntry entry;
            byte[] buffer = new byte[2048];
            while ((entry = zis.getNextEntry()) != null)
            {
                // Build destination file
                File destFile = destDir.resolve(entry.getName()).toFile();
                LOGGER.fine("extractZipResource() processing zipEntry=" + entry.getName() + " destFile=" + destFile.getAbsolutePath());
                if (entry.isDirectory())
                {
                    // Directory, recreate if not present
                    if (!destFile.exists() && !destFile.mkdirs())
                    {
                        LOGGER.warning("extractZipResource() can't create destination folder : " + destFile.getAbsolutePath());
                    }
                    continue;
                }
                // Plain file, copy it
                try (FileOutputStream fos = new FileOutputStream(destFile);
                        BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length))
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
            LOGGER.log(Level.SEVERE, "extractZipResource() problem extracting resource for myClass=" + myClass + " zipResource=" + zipResource + " ex=" + ex.getLocalizedMessage());
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
                LOGGER.log(Level.SEVERE, "copyResource() resource not found. c=" + c + " resourcePath=" + resourceFilePath);
            }
        } catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "copyResource() problem copying resource. c=" + c + ", resourcePath=" + resourceFilePath + ", targetFile=" + targetFile, ex);
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
     * Gets the base location of the given class. Manage all OS variations and possible problems in characters...
     * <p>
     * If the class is directly on the file system (e.g., "/path/to/my/package/MyClass.class") then it will return the base
     * directory (e.g., "file:/path/to").
     * </p>
     * <p>
     * If the class is within a JAR file (e.g., "/path/to/my-jar.jar!/my/package/MyClass.class") then it will return the path to
     * the JAR (e.g., "file:/path/to/my-jar.jar").
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

    public static boolean isWindows()
    {
        return (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("win") >= 0);
    }

    public static boolean isMac()
    {

        return (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("mac") >= 0);
    }

    public static boolean isUnix()
    {
        String s = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        return (s.indexOf("nix") >= 0 || s.indexOf("nux") >= 0 || s.indexOf("aix") > 0);
    }

    /**
     * Converts the given {@link URL} to its corresponding {@link File}.
     * <p>
     * This method is similar to calling {@code new File(url.toURI())} except that it also handles "jar:file:" U Sgs, returning
     * the path to the JAR file.
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
            if (isWindows() && path.matches("file:[A-Za-z]:.*"))
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
    static private String loadFileAsString(FileObject fo)
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
     * Get all the files matching fnFilter in dirTree (and its subdirectories).
     * <p>
     *
     * @param dirTree
     * @param fnFilter If null accept all files.
     * @param ignoreDirPrefix Subdirs starting with this prefix are not traversed. If null accept all subdirectories.
     * @return
     */
    static public HashSet<Path> listFiles(File dirTree, final FilenameFilter fnFilter, final String ignoreDirPrefix)
    {
        if (dirTree == null)
        {
            throw new IllegalArgumentException("dirTree=" + dirTree + " fnFilter=" + fnFilter + " ignoreDirPrefix=" + ignoreDirPrefix);
        }
        final HashSet<Path> pathSet = new HashSet<>();
        try
        {
            Files.walkFileTree(dirTree.toPath(), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                {
                    return ignoreDirPrefix == null || !dir.getFileName().toString().startsWith(ignoreDirPrefix) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
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
            });
        } catch (IOException ex)
        {
            LOGGER.warning("listFile() IOException ex=" + ex.getLocalizedMessage() + ". Some files may have not been listed.");
        }
        return pathSet;
    }

}
