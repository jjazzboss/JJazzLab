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
package org.jjazz.song.api;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.undomanager.SimpleEdit;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * The song object.
 * <p>
 * Contents are a chord leadsheet, the related song structure, some parameters and some optional properties.<br>
 * Song can be created using the SongFactory methods.
 */
public class Song implements Serializable, ClsChangeListener, SgsChangeListener
{

    public static final String PROP_NAME = "PROP_NAME";
    public static final String PROP_COMMENTS = "PROP_COMMENTS";
    public static final String PROP_TAGS = "PROP_TAGS";
    public static final String PROP_TEMPO = "PROP_TEMPO";
    /**
     * Fired when the close() method is called.
     */
    public static final String PROP_CLOSED = "PROP_CLOSED";
    /**
     * This property changes each time the song is modified (false&gt;true) or saved (true&gt;false).
     */
    public static final String PROP_MODIFIED_OR_SAVED = "PROP_MODIFIED_OR_SAVED";
    private SongStructure songStructure;
    private ChordLeadSheet chordLeadSheet;
    private String name;
    private String comments = "Edit me...";
    private int tempo = 120;
    private ArrayList<String> tags = new ArrayList<>();
    private final Properties clientProperties = new Properties();
    private transient File file;
    private transient boolean needSave = false;
    /**
     * The listeners for undoable edits in this LeadSheet.
     */
    protected transient List<UndoableEditListener> undoListeners = new ArrayList<>();
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Song.class.getSimpleName());

    /**
     * Create a song object.
     * <p>
     * The songStructure will be automatically created from the chordleadsheet.
     *
     * @param name A non-empty string.
     * @param cls
     */
    protected Song(String name, ChordLeadSheet cls) throws UnsupportedEditException
    {
        this(name, cls, SongStructureFactory.getDefault().createSgs(cls, true));
    }

    /**
     * Constructor for the SerializationProxy only.
     * <p>
     *
     * @param name
     * @param cls
     * @param sgs Must be kep consistent with cls changes !
     */
    protected Song(String name, ChordLeadSheet cls, SongStructure sgs)
    {
        if (name == null || name.trim().isEmpty() || cls == null || sgs == null)
        {
            throw new IllegalArgumentException("name=" + name + " cls=" + cls + " sgs=" + sgs);
        }
        setName(name);
        chordLeadSheet = cls;
        songStructure = sgs;
        chordLeadSheet.addClsChangeListener(this);
        songStructure.addSgsChangeListener(this);
    }

    /**
     * Get a client property.
     *
     * @param key
     * @param defaultValue
     * @return the property associated to key, or defaultValue if the property was not found.
     */
    public String getClientProperty(String key, String defaultValue)
    {
        return clientProperties.getProperty(key);
    }

    /**
     * Store a client property.
     * <p>
     * Client properties are serialized. This can be used by other components to store information specific to this object, eg UI
     * settings or others like Section Quantization.<br>
     * A PropertyChangeEvent(property name=key) is fired to listeners. If newValue=null then property is removed.
     *
     * @param key
     * @param value If value==null then property is removed.
     */
    public void putClientProperty(String key, String value)
    {
        if (key == null)
        {
            throw new NullPointerException("key=" + key + " value=" + value);
        }
        String oldValue = clientProperties.getProperty(key);
        if (oldValue == null && value == null)
        {
            return;
        }
        if (value == null)
        {
            clientProperties.remove(key);
        } else
        {
            clientProperties.setProperty(key, value);
        }
        pcs.firePropertyChange(key, oldValue, oldValue);
    }

    public ChordLeadSheet getChordLeadSheet()
    {
        return chordLeadSheet;
    }

    public SongStructure getSongStructure()
    {
        return songStructure;
    }

    public int getTempo()
    {
        return tempo;
    }

    /**
     * Set the preferred tempo for this song.
     * <p>
     * Fire a PROP_TEMPO property change event.
     *
     * @param newTempo
     */
    public final void setTempo(final int newTempo)
    {
        if (!TempoRange.checkTempo(newTempo))
        {
            throw new IllegalArgumentException("newTempo=" + newTempo);
        }
        final int oldTempo = tempo;

        if (oldTempo != newTempo)
        {
            tempo = newTempo;

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Set tempo " + newTempo)
            {
                @Override
                public void undoBody()
                {
                    tempo = oldTempo;
                    pcs.firePropertyChange(PROP_TEMPO, newTempo, oldTempo);
                }

                @Override
                public void redoBody()
                {
                    tempo = newTempo;
                    pcs.firePropertyChange(PROP_TEMPO, oldTempo, newTempo);
                }
            };
            fireUndoableEditHappened(edit);

            pcs.firePropertyChange(PROP_TEMPO, oldTempo, newTempo);
            fireIsModified();
        }
    }

    /**
     * Set the list of String tags associated to this song, e.g. "rock", "dance-oriented", etc...
     * <p>
     * Fire a PROP_TAGS property change events.
     *
     * @param newTags Must not be null but can be an empty list. Tags are space-trimmed and converted to lower case.
     */
    public void setTags(List<String> newTags)
    {
        if (newTags == null)
        {
            throw new NullPointerException("newTags");
        }

        final ArrayList<String> oldTags = new ArrayList<>(tags);
        final ArrayList<String> newTagsLowerCase = new ArrayList<>();
        for (String s : newTags)
        {
            newTagsLowerCase.add(s.trim().toLowerCase());
        }
        if (tags.equals(newTagsLowerCase))
        {
            return;
        }
        tags = newTagsLowerCase;

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Set tags")
        {
            @Override
            public void undoBody()
            {
                tags = oldTags;
                pcs.firePropertyChange(PROP_TAGS, newTagsLowerCase, tags);
            }

            @Override
            public void redoBody()
            {
                tags = newTagsLowerCase;
                pcs.firePropertyChange(PROP_TAGS, oldTags, tags);
            }
        };
        fireUndoableEditHappened(edit);

        pcs.firePropertyChange(PROP_TAGS, oldTags, tags);
        fireIsModified();
    }

    /**
     * @return List can be empty if not tags. Tags are lowercase.
     */
    public List<String> getTags()
    {
        return new ArrayList<>(tags);
    }

    /**
     * The song name.
     *
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the song name.
     * <p>
     * Fire a PROP_NAME property change event.
     *
     * @param newName A non-empty string.
     */
    public final void setName(final String newName)
    {
        if (newName == null || newName.trim().isEmpty())
        {
            throw new IllegalArgumentException("newName=" + newName);
        }
        if (!newName.equals(name))
        {
            final String oldName = name;
            name = newName;

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Set name " + newName)
            {
                @Override
                public void undoBody()
                {
                    name = oldName;
                    pcs.firePropertyChange(PROP_NAME, newName, oldName);
                }

                @Override
                public void redoBody()
                {
                    name = newName;
                    pcs.firePropertyChange(PROP_NAME, oldName, newName);
                }
            };
            fireUndoableEditHappened(edit);

            pcs.firePropertyChange(PROP_NAME, oldName, newName);
            fireIsModified();
        }
    }

    /**
     * To be called to cleanup the song when song will not be used anymore.
     * <p>
     * Fire a PROP_CLOSED property change event.
     *
     * @param releaseRhythmResources True if the method should also call releaseResources() for each used rhythm.
     */
    public void close(boolean releaseRhythmResources)
    {
        chordLeadSheet.removeClsChangeListener(this);
        songStructure.removeSgsChangeListener(this);
        if (releaseRhythmResources)
        {
            for (Rhythm r : songStructure.getUniqueRhythms(false))
            {
                r.releaseResources();
            }
        }
        pcs.firePropertyChange(PROP_CLOSED, false, true);
    }

    /**
     * The comments associated to this song.
     *
     * @return Can be an empty String.
     */
    public String getComments()
    {
        return comments;
    }

    /**
     * Set the comments.
     * <p>
     * Fire the PROP_COMMENTS change event.
     *
     * @param newComments
     */
    public void setComments(final String newComments)
    {
        if (newComments == null)
        {
            throw new IllegalArgumentException("newComments=" + newComments);
        }
        if (!newComments.equals(comments))
        {
            final String oldComments = comments;
            comments = newComments;

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Set comments")
            {
                @Override
                public void undoBody()
                {
                    comments = oldComments;
                    pcs.firePropertyChange(PROP_COMMENTS, newComments, oldComments);
                }

                @Override
                public void redoBody()
                {
                    comments = newComments;
                    pcs.firePropertyChange(PROP_COMMENTS, oldComments, newComments);
                }
            };
            fireUndoableEditHappened(edit);

            pcs.firePropertyChange(PROP_COMMENTS, oldComments, newComments);
            fireIsModified();
        }
    }

    /**
     * The file where this song is stored.
     *
     * @return Can be null for example if it's a builtin song or created programmatically.
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Set the file from which the song can be read/written.
     *
     * @param f Can be null.
     */
    public void setFile(File f)
    {
        file = f;
    }

    /**
     * Same as SaveToFile but notify user if problem.
     * <p>
     *
     * @param f
     * @param isCopy
     * @return False if problem
     */
    public boolean saveToFileNotify(File f, boolean isCopy)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f + " isCopy=" + isCopy);
        }
        boolean b = true;
        if (f.exists() && !f.canWrite())
        {
            String msg = "Can not overwrite " + f.getName();
            LOGGER.warning("saveToFileNotify() " + msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            b = false;
        }
        if (b)
        {
            try
            {
                saveToFile(f, isCopy);
            } catch (IOException ex)
            {
                String msg = "Problem saving song file " + f.getName() + " : " + ex.getLocalizedMessage();
                LOGGER.warning("saveToFileNotify() " + msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                b = false;
            }
        }

        return b;
    }

    /**
     * Save this song to a file (XML format).
     * <p>
     * Song's file and name is set to f and f's name. Fire a PROP_MODIFIED_OR_SAVED property change event with oldValue=true and
     * newValue=false.
     *
     * @param songFile
     * @param isCopy Indicate that the save operation if for a copy, ie just perform the save operation and do nothing else (song
     * name is not set, etc.)
     * @throws java.io.IOException
     * @see getFile()
     */
    public void saveToFile(File songFile, boolean isCopy) throws IOException
    {
        if (songFile == null)
        {
            throw new IllegalArgumentException("songFile=" + songFile + " isCopy=" + isCopy);
        }
        if (!isCopy)
        {
            file = songFile;
        }

        try (FileOutputStream fos = new FileOutputStream(songFile))
        {
            XStream xstream = new XStream();
            xstream.alias("Song", Song.class
            );
            xstream.toXML(this, fos);
            if (!isCopy)
            {
                setName(Song.removeSongExtension(songFile.getName()));
                resetNeedSave();
            }
        } catch (IOException e)
        {
            if (!isCopy)
            {
                file = null;
            }
            throw e;
        } catch (XStreamException e)
        {
            if (!isCopy)
            {
                file = null;
            }
            // Translate into an IOException to be handled by the Netbeans framework 
            throw new IOException("XStream XML marshalling error", e);
        }
    }

    /**
     * @return True if song has some unsaved changes.
     */
    public boolean needSave()
    {
        return needSave;
    }

    /**
     * Reset the need save property.
     * <p>
     * Fire the PROP_MODIFIED_OR_SAVED true-&gt;false
     */
    public void resetNeedSave()
    {
        needSave = false;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, true, false);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

    static protected String removeSongExtension(String str)
    {
        String ext = "." + FileDirectoryManager.SONG_EXTENSION;
        int indexExt = str.toLowerCase().lastIndexOf(ext.toLowerCase());
        if (indexExt == -1)
        {
            return str;
        } else
        {
            return str.substring(0, indexExt);
        }
    }

    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      
       
    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        fireIsModified();
    }


    //------------------------------------------------------------------------------
    // SgsChangeListener interface
    //------------------------------------------------------------------------------
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }


    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        fireIsModified();
    }


    // ----------------------------------------------------------------------------
    // Private functions 
    // ----------------------------------------------------------------------------
    /**
     * Fire a PROP_MODIFIED_OR_SAVED property change event, oldValue=false, newValue=true
     */
    private void fireIsModified()
    {
        needSave = true;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, false, true);
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);
        }
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners.toArray(new UndoableEditListener[undoListeners.size()]))
        {
            l.undoableEditHappened(event);
        }
    }

//    private static boolean checkRhythmVoiceExists(Song s, RhythmVoice rv)
//    {
//        for (SongPart spt : s.songStructure.getSongParts())
//        {
//            if (spt.getRhythm().getRhythmVoices().contains(rv))
//            {
//                return true;
//            }
//        }
//        return false;
//    }
    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");

    }

    /**
     * RhythmVoices must be stored in a simplified way in order to avoid storing rhythm stuff which depend on InstrumentBanks
     * which are themselves system dependent.
     * <p>
     * Also need to do some cleaning: mapInstruments can contain useless entries if some songparts have been removed .
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 571097826016222L;
        private final int spVERSION = 1;
        private final String spName;
        private final String spComments;
        private final int spTempo;
        private final Properties spClientProperties;
        private final List<String> spTags;
        private final ChordLeadSheet spChordLeadSheet;
        private final SongStructure spSongStructure;

        private SerializationProxy(Song s)
        {
            spChordLeadSheet = s.getChordLeadSheet();
            spSongStructure = s.getSongStructure();
            spName = s.getName();
            spComments = s.getComments();
            spTempo = s.getTempo();
            spTags = s.getTags();
            spClientProperties = s.clientProperties;
        }

        private Object readResolve() throws ObjectStreamException
        {
            Song newSong = new Song(spName, spChordLeadSheet, spSongStructure);
            newSong.setComments(spComments);
            newSong.setTags(spTags);
            newSong.setTempo(spTempo);
            for (String key : spClientProperties.stringPropertyNames())
            {
                newSong.putClientProperty(key, spClientProperties.getProperty(key));
            }
            return newSong;
        }
    }

}
