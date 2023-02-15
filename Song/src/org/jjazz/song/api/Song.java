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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * The song object.
 * <p>
 * Contents are a chord leadsheet, the related song structure, some parameters and some optional properties.<br>
 * Song can be created using the SongFactory methods.
 */
public class Song implements Serializable, ClsChangeListener, SgsChangeListener, PropertyChangeListener
{

    public static final String PROP_NAME = "PROP_NAME";
    public static final String PROP_COMMENTS = "PROP_COMMENTS";
    public static final String PROP_TAGS = "PROP_TAGS";
    public static final String PROP_TEMPO = "PROP_TEMPO";
    public static final String PROP_VETOABLE_PHRASE_NAME = "PROP_PHRASE_NAME";
    /**
     * newValue = new size in bars. OldValue=old size in bars
     */
    public static final String PROP_SIZE_IN_BARS = "PROP_SIZE_IN_BARS";
    /**
     * If a user phrase is removed: oldValue=name_of_removed_phrase and newValue=removed_phrase.<br>
     * If a user phrase is added, oldValue=null and newValue=name_of_new_phrase<br>
     */
    public static final String PROP_VETOABLE_USER_PHRASE = "PROP_VETOABLE_USER_PHRASE";   //NOI18N
    /**
     * An existing phrase was replaced by another. oldValue=old_phrase, newValue=name_of_new_phrase.
     */
    public static final String PROP_VETOABLE_USER_PHRASE_CONTENT = "PROP_VETOABLE_USER_PHRASE_CONTENT";   //NOI18N 
    /**
     * Fired when the close() method is called.
     */
    public static final String PROP_CLOSED = "PROP_CLOSED";   //NOI18N 
    /**
     * This property changes each time the song is modified (oldValue=false, newValue=true) or saved (oldValue=true,
     * newValue=false) or Song.resetNeedSave() is called (oldValue=null, newValue=false)
     */
    public static final String PROP_MODIFIED_OR_SAVED_OR_RESET = "PROP_MODIFIED_OR_SAVED_OR_RESET";   //NOI18N 
    /**
     * Fired each time the musical content of the song is modified.
     * <p>
     * OldValue=false, newValue=the property name or ClsActionEvent/SgsActionEvent actionId that triggered the musical change.
     * <p>
     * Use PROP_MODIFIED_OR_SAVED_OR_RESET to get notified of any song change, including non-musical ones like tempo change,
     * phrase name change, etc.
     */
    public static final String PROP_MUSIC_GENERATION = "SongMusicGeneration";
    private SongStructure songStructure;
    private ChordLeadSheet chordLeadSheet;
    private String name;
    private String comments = ResUtil.getString(getClass(), "EDIT_ME");
    private int tempo = 120;
    private List<String> tags = new ArrayList<>();
    private Map<String, Phrase> mapUserPhrases = new HashMap<>();
    private final Properties clientProperties = new Properties();
    private transient File file;
    private transient boolean needSave = false;
    private transient int lastSize;
    /**
     * The listeners for undoable edits in this LeadSheet.
     */
    protected transient List<UndoableEditListener> undoListeners = new ArrayList<>();
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private final transient VetoableChangeSupport vcs = new VetoableChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Song.class.getSimpleName());

    /**
     * Create a song object.
     * <p>
     * The songStructure will be automatically created from the chordleadsheet. Use SongFactory to create song instances.
     *
     * @param name A non-empty string.
     * @param cls
     * @throws org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException
     */
    protected Song(String name, ChordLeadSheet cls) throws UnsupportedEditException
    {
        this(name, cls, SongStructureFactory.getDefault().createSgs(cls, true));
    }

    /**
     * Constructor for the SerializationProxy only.
     * <p>
     * Use SongFactory to create song instances.
     *
     * @param name
     * @param cls
     * @param sgs Must be kept consistent with cls changes (sgs.getParentChordLeadSheet() must return cls)
     */
    protected Song(String name, ChordLeadSheet cls, SongStructure sgs)
    {
        if (name == null || name.trim().isEmpty() || cls == null || sgs == null || sgs.getParentChordLeadSheet() != cls)
        {
            throw new IllegalArgumentException(
                    "name=" + name + " cls=" + cls + " sgs=" + sgs + " sgs.getParentChordLeadSheet()=" + sgs.getParentChordLeadSheet());   //NOI18N
        }
        setName(name);
        chordLeadSheet = cls;
        songStructure = sgs;
        chordLeadSheet.addClsChangeListener(this);
        songStructure.addSgsChangeListener(this);

        lastSize = songStructure.getSizeInBars();
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
        String res = clientProperties.getProperty(key);
        return res != null ? res : defaultValue;
    }

    /**
     * Store a client property.
     * <p>
     * Client properties are serialized. This can be used by other components to store information specific to this object, eg UI
     * settings or others like quantization display of a section, zoom factors, etc.<br>
     * A PropertyChangeEvent(property name=key) is fired to song listeners. If newValue=null then property is removed.<p>
     * This will fire a song modified event.
     *
     * @param key
     * @param value If value==null then property is removed.
     */
    public void putClientProperty(String key, String value)
    {
        checkNotNull(key);

        String oldValue = clientProperties.getProperty(key);
        if (Objects.equals(oldValue, value))
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

        pcs.firePropertyChange(key, oldValue, value);
        fireIsModified();

    }

    /**
     * Rename a user phrase.
     * <p>
     * Fire a PROP_VETOABLE_PHRASE_NAME change event (actually this property change event should never been vetoed, but this
     * allows caller to use a single vetoable listener for all user phrase events).
     *
     * @param name Must be the name of an existing phrase
     * @param newName
     */
    public void renameUserPhrase(String name, String newName)
    {
        var p = getUserPhrase(name);
        if (p == null)
        {
            throw new IllegalArgumentException("name=" + name + " mapUserPhrases=" + mapUserPhrases);
        }
        if (name.equals(newName))
        {
            return;
        }

        // Perform the change
        final var oldMap = new HashMap<>(mapUserPhrases);
        mapUserPhrases.remove(name);
        mapUserPhrases.put(newName, p);
        final var newMap = new HashMap<>(mapUserPhrases);


        // Create the undoable event        
        UndoableEdit edit;
        edit = new SimpleEdit("Rename user phrase")
        {
            @Override
            public void undoBody()
            {
                mapUserPhrases = oldMap;
                try
                {
                    vcs.fireVetoableChange(PROP_VETOABLE_PHRASE_NAME, newName, name);
                } catch (PropertyVetoException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
                fireIsModified();
            }

            @Override
            public void redoBody()
            {
                mapUserPhrases = newMap;
                try
                {
                    vcs.fireVetoableChange(PROP_VETOABLE_PHRASE_NAME, name, newName);
                } catch (PropertyVetoException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
                fireIsModified();
            }
        };

        fireUndoableEditHappened(edit);
        try
        {
            vcs.fireVetoableChange(PROP_VETOABLE_PHRASE_NAME, name, newName);
        } catch (PropertyVetoException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
        fireIsModified();
    }

    /**
     * Set the user phrase for the specified name.
     * <p>
     * If a user phrase was already associated to name, it's replaced. Fire a VeotableChange PROP_VETOABLE_USER_PHRASE if no
     * phrase is replaced, otherwise use PROP_VETOABLE_USER_PHRASE_CONTENT. Actually the possibility of a veto is only when a new
     * phrase is added (e.g. if MidiMix does not have an available Midi channel). Other user phrase PROP_ events for simplicity
     * only (one listener required).
     * <p>
     * This song will listen to p's changes and fire a PROP_MODIFIED_OR_SAVED_OR_RESET change event when a non-adjusting change is
     * made.
     * <p>
     * @param name Can't be blank.
     * @param p Can't be null. No defensive copy is done, p is directly reused. No control is done on the phrase consistency Vs
     * the song.
     * @throws PropertyVetoException If no Midi channel available for the user phrase
     * @see Song#PROP_VETOABLE_USER_PHRASE
     * @see Song#PROP_VETOABLE_USER_PHRASE_CONTENT
     * @see Song#PROP_MUSIC_GENERATION
     */
    public void setUserPhrase(String name, Phrase p) throws PropertyVetoException
    {
        checkNotNull(name);
        checkNotNull(p);
        checkArgument(!name.isBlank(), "name=%s", name);


        if (getSongStructure().getSongParts().isEmpty())
        {
            return;
        }


        final Phrase oldPhrase = getUserPhrase(name);
        final Phrase newPhrase = p;


        // Perform the change
        final var oldMap = new HashMap<>(mapUserPhrases);
        mapUserPhrases.put(name, newPhrase);
        final var newMap = new HashMap<>(mapUserPhrases);


        // Listen to the phrase changes in order to update the modified status of the song
        newPhrase.addPropertyChangeListener(this);


        // Create the undoable event        
        UndoableEdit edit;
        if (oldPhrase == null)
        {
            // First time adding this user phrase
            edit = new SimpleEdit("Add user phrase")
            {
                @Override
                public void undoBody()
                {
                    mapUserPhrases = oldMap;
                    newPhrase.removePropertyChangeListener(Song.this);
                    try
                    {
                        vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE, name, newPhrase);
                    } catch (PropertyVetoException ex)
                    {
                        // Should never happen
                        Exceptions.printStackTrace(ex);
                    }
                    fireIsMusicallyModified(PROP_VETOABLE_USER_PHRASE);
                    fireIsModified();
                }

                @Override
                public void redoBody()
                {
                    mapUserPhrases = newMap;
                    newPhrase.addPropertyChangeListener(Song.this);
                    try
                    {
                        vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE, null, name);
                    } catch (PropertyVetoException ex)
                    {
                        // Should never happen
                        Exceptions.printStackTrace(ex);
                    }
                    fireIsMusicallyModified(PROP_VETOABLE_USER_PHRASE);
                    fireIsModified();
                }
            };

            fireUndoableEditHappened(edit);
            vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE, null, name);          // throws PropertyVetoException

        } else
        {
            // User phrase is replaced
            oldPhrase.removePropertyChangeListener(this);

            edit = new SimpleEdit("Update user phrase")
            {
                @Override
                public void undoBody()
                {
                    mapUserPhrases = oldMap;
                    newPhrase.removePropertyChangeListener(Song.this);
                    oldPhrase.addPropertyChangeListener(Song.this);
                    try
                    {
                        vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE_CONTENT, newPhrase, name);
                    } catch (PropertyVetoException ex)
                    {
                        // Should never happen
                        Exceptions.printStackTrace(ex);
                    }
                    fireIsMusicallyModified(PROP_VETOABLE_USER_PHRASE_CONTENT);
                    fireIsModified();
                }

                @Override
                public void redoBody()
                {
                    mapUserPhrases = newMap;
                    oldPhrase.removePropertyChangeListener(Song.this);
                    newPhrase.addPropertyChangeListener(Song.this);
                    try
                    {
                        vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE_CONTENT, oldPhrase, name);
                    } catch (PropertyVetoException ex)
                    {
                        // Should never happen
                        Exceptions.printStackTrace(ex);
                    }
                    fireIsMusicallyModified(PROP_VETOABLE_USER_PHRASE_CONTENT);
                    fireIsModified();
                }
            };

            fireUndoableEditHappened(edit);
            vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE_CONTENT, oldPhrase, name);          // throws PropertyVetoException

        }

        fireIsMusicallyModified(oldPhrase == null ? PROP_VETOABLE_USER_PHRASE : PROP_VETOABLE_USER_PHRASE_CONTENT);
        fireIsModified();

    }

    /**
     * Remove the user phrase associated to name.
     * <p>
     * Fire a PROP_VETOABLE_USER_PHRASE event.
     *
     * @param name
     * @return The removed phrase or null
     */
    public Phrase removeUserPhrase(String name)
    {
        checkNotNull(name);

        Phrase p = mapUserPhrases.get(name);
        if (p == null)
        {
            return null;
        }


        p.removePropertyChangeListener(this);


        // Perform the change
        final var oldMap = new HashMap<>(mapUserPhrases);
        mapUserPhrases.remove(name);
        final var newMap = new HashMap<>(mapUserPhrases);


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove user phrase")
        {
            @Override
            public void undoBody()
            {
                mapUserPhrases = oldMap;
                p.addPropertyChangeListener(Song.this);
                try
                {
                    vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE, null, name);
                } catch (PropertyVetoException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
                fireIsModified();
            }

            @Override
            public void redoBody()
            {
                mapUserPhrases = newMap;
                p.removePropertyChangeListener(Song.this);
                try
                {
                    vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE, name, p);
                } catch (PropertyVetoException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
                fireIsModified();
            }
        };
        fireUndoableEditHappened(edit);


        try
        {
            vcs.fireVetoableChange(PROP_VETOABLE_USER_PHRASE, name, p);
        } catch (PropertyVetoException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
        fireIsModified();

        return p;
    }

    /**
     * Get all the names of the user phrases.
     *
     * @return Can't be null.
     */
    public Set<String> getUserPhraseNames()
    {
        return mapUserPhrases.keySet();
    }

    /**
     * Get the user phrase associated to specified name.
     * <p>
     * Returned phrase might be longer than the song.
     * <p>
     * The song listens to the returned phrase so that if a significant change (non-adusting) is made to it, the song will fire a
     * PROP_MODIFIED_OR_SAVED_OR_RESET change event.
     *
     * @param name
     * @return Null if no phrase associated to name. The Phrase channel should be ignored.
     */
    public Phrase getUserPhrase(String name)
    {
        return mapUserPhrases.get(name);
    }


    public ChordLeadSheet getChordLeadSheet()
    {
        return chordLeadSheet;
    }

    public SongStructure getSongStructure()
    {
        return songStructure;
    }

    /**
     * Convenience method which delegates to getSongStructure().getSizeInBars().
     *
     * @return
     */
    public int getSize()
    {
        return songStructure.getSizeInBars();
    }

    /**
     * Get the preferred tempo for this song.
     *
     * @return
     */
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
            throw new IllegalArgumentException("newTempo=" + newTempo);   //NOI18N
        }
        final int oldTempo = tempo;

        if (oldTempo != newTempo)
        {
            tempo = newTempo;
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
            throw new NullPointerException("newTags");   //NOI18N
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
            throw new IllegalArgumentException("newName=" + newName);   //NOI18N
        }
        if (!newName.equals(name))
        {
            final String oldName = name;
            name = newName;

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
            for (Rhythm r : songStructure.getUniqueRhythms(false, false))
            {
                r.releaseResources();
            }
        }
        for (var p : mapUserPhrases.values())
        {
            p.removePropertyChangeListener(this);
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
            throw new IllegalArgumentException("newComments=" + newComments);   //NOI18N
        }
        if (!newComments.equals(comments))
        {
            final String oldComments = comments;
            comments = newComments;

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
            throw new IllegalArgumentException("f=" + f + " isCopy=" + isCopy);   //NOI18N
        }
        boolean b = true;
        if (f.exists() && !f.canWrite())
        {
            String msg = ResUtil.getString(getClass(), "CAN NOT OVERWRITE", f.getName());
            LOGGER.warning("saveToFileNotify() " + msg);   //NOI18N
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
                String msg = ResUtil.getString(getClass(), "ERR_ProblemSavingSongFile", f.getName());
                msg += " : " + ex.getLocalizedMessage();
                LOGGER.warning("saveToFileNotify() " + msg);   //NOI18N
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
     * Song's file and name is set to f and f's name. Fire a PROP_MODIFIED_OR_SAVED_OR_RESET property change event with
     * oldValue=true and newValue=false.
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
            throw new IllegalArgumentException("songFile=" + songFile + " isCopy=" + isCopy);   //NOI18N
        }


        doAnalytics();


        if (!isCopy)
        {
            file = songFile;
        }

        try (FileOutputStream fos = new FileOutputStream(songFile))
        {
            XStream xstream = new XStream();
            xstream
                    .alias("Song", Song.class
                    );
            Writer w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));        // Needed to support special/accented chars
            xstream.toXML(this, w);
            if (!isCopy)
            {
                setName(Song.removeSongExtension(songFile.getName()));
                fireSaved();
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
     * Fire the PROP_MODIFIED_OR_SAVED_OR_RESET with oldValue=null newValue=false
     */
    public void resetNeedSave()
    {
        needSave = false;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED_OR_RESET, null, false);
    }

    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propertyName, l);
    }

    public synchronized void addVetoableChangeListener(VetoableChangeListener listener)
    {
        vcs.addVetoableChangeListener(listener);
    }

    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener)
    {
        vcs.removeVetoableChangeListener(listener);
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
    //-----------------------------------------------------------------------
    // PropertiesListener interface
    //-----------------------------------------------------------------------

    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() source={0} prop={1} newValue={2}", new Object[]
        {
            e.getSource().getClass(), e.getPropertyName(), e.getNewValue()
        });


        if (e.getSource() instanceof Phrase)
        {
            // Listen to User phrases significant changes to mark the song as modified 
            if (!Phrase.isAdjustingEvent(e.getPropertyName()))
            {
                fireIsMusicallyModified(PROP_VETOABLE_USER_PHRASE_CONTENT);
                fireIsModified();
            }
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
        if (event instanceof ClsActionEvent ae && ae.isActionComplete())
        {
            fireIsMusicallyModified(ae.getActionId());
            fireIsModified();
        }
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
        if (e instanceof SgsActionEvent ae && ae.isActionComplete())
        {
            String actionId = ae.getActionId();
            switch (actionId)
            {
                case "addSongParts", "removeSongParts", "resizeSongParts" ->
                {
                    int newSize = getSize();
                    pcs.firePropertyChange(PROP_SIZE_IN_BARS, lastSize, newSize);
                    lastSize = newSize;
                }
                default ->
                {
                }
            }

            fireIsMusicallyModified(actionId);
            fireIsModified();
        }
    }
    // ----------------------------------------------------------------------------
    // Private methods 
    // ----------------------------------------------------------------------------

    /**
     * Fire a PROP_MODIFIED_OR_SAVED_OR_RESET property change event with oldValue=false, newValue=true
     */
    private void fireIsModified()
    {
        needSave = true;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED_OR_RESET, false, true);
    }

    /**
     * Fire a PROP_MUSIC_GENERATION property change event with oldValue=false and the specified newValue.
     */
    private void fireIsMusicallyModified(String newValue)
    {
        needSave = true;
        pcs.firePropertyChange(PROP_MUSIC_GENERATION, false, newValue);
    }

    /**
     * Fire a PROP_MODIFIED_OR_SAVED_OR_RESET property change event with oldValue=true newValue=false
     */
    private void fireSaved()
    {
        needSave = false;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED_OR_RESET, true, false);
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);   //NOI18N
        }
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners.toArray(new UndoableEditListener[undoListeners.size()]))
        {
            l.undoableEditHappened(event);

        }
    }

    /**
     * Compute some anonymous stats about feature usage.
     */
    private void doAnalytics()
    {
        var ecss = chordLeadSheet.getItems(CLI_ChordSymbol.class
        ).stream().map(cli -> cli.getData()).toList();
        var cris = ecss.stream().map(ecs -> ecs.getRenderingInfo()).collect(Collectors.toList());

        HashMap<String, Object> map = new HashMap<>();
        map.put("Memo Char Size", getComments().length());
        map.put("Nb Chord Symbols", cris.stream().count());
        map.put("Nb Song Parts", songStructure.getSongParts().size());
        map.put("LeadSheet Bar Size", chordLeadSheet.getSizeInBars());
        map.put("Song Structure Bar Size", songStructure.getSizeInBars());
        map.put("Use Bass Pedal Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.PEDAL_BASS)));
        map.put("Use Accent Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.ACCENT)));
        map.put("Use Stronger Accent Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(
                ChordRenderingInfo.Feature.ACCENT_STRONGER)));
        map.put("Use Crash Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.CRASH)));
        map.put("Use No Crash Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.NO_CRASH)));
        map.put("Use Extended Hold/Shot Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(
                ChordRenderingInfo.Feature.EXTENDED_HOLD_SHOT)));
        map.put("Use Shot Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.SHOT)));
        map.put("Use Hold Chord", cris.stream().anyMatch(cri -> cri.hasOneFeature(ChordRenderingInfo.Feature.HOLD)));
        map.put("Use Scale Chord", cris.stream().anyMatch(cri -> cri.getScaleInstance() != null));
        map.put("Use Substitute Chord", ecss.stream().anyMatch(ecs -> ecs.getAlternateChordSymbol() != null));


        Analytics.logEvent("Save Song", map);
        Analytics.incrementProperties("Nb Save Song", 1);
        Analytics.setPropertiesOnce(Analytics.buildMap("First Save", Analytics.toStdDateTimeString()));
    }


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
        private final int spVERSION = 2;
        private final String spName;
        private final String spComments;
        private final int spTempo;
        private final Properties spClientProperties;
        private final List<String> spTags;
        private final ChordLeadSheet spChordLeadSheet;
        private final SongStructure spSongStructure;
        // Since spVERSION 2
        private final Map<String, Phrase> spMapUserPhrases;

        private SerializationProxy(Song s)
        {
            spChordLeadSheet = s.getChordLeadSheet();
            spSongStructure = s.getSongStructure();
            spName = s.getName();
            spComments = s.getComments();
            spTempo = s.getTempo();
            spTags = s.getTags();
            spClientProperties = s.clientProperties;
            // Since spVERSION 2
            spMapUserPhrases = s.mapUserPhrases;
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

            // Since spVERSION 2
            if (spMapUserPhrases != null)
            {
                for (String name : spMapUserPhrases.keySet())
                {
                    Phrase p = spMapUserPhrases.get(name);
                    try
                    {
                        newSong.setUserPhrase(name, p);
                    } catch (PropertyVetoException ex)
                    {
                        LOGGER.warning("readResolve() Can't add user phrase for name=" + name + ". ex=" + ex.getMessage());
                    }
                }
            }


            return newSong;
        }
    }

}
