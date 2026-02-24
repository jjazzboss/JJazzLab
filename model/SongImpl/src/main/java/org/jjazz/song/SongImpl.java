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
package org.jjazz.song;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.songstructure.SongStructureImpl;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.StringProperties;
import org.jjazz.utilities.api.ThrowingSupplier;
import org.jjazz.xstream.api.XStreamInstancesManager;
import org.jjazz.xstream.spi.XStreamConfigurator;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;


/**
 * Song implementation.
 */
public class SongImpl implements Serializable, PropertyChangeListener, Song
{
    private final SongStructure songStructure;
    private final ChordLeadSheet chordLeadSheet;
    private String name;
    private String comments = DEFAULT_COMMENTS;
    private int tempo = 120;
    private List<String> tags = new ArrayList<>();
    private Map<String, Phrase> mapUserPhrases = new HashMap<>();
    private final StringProperties clientProperties = new StringProperties(this);
    private volatile transient File file;
    private volatile transient boolean saveNeeded = false;
    private volatile transient boolean closed;
    private transient final ExecutionManager executionManager;
    protected transient CopyOnWriteArrayList<UndoableEditListener> undoListeners = new CopyOnWriteArrayList<>();
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final transient VetoableChangeSupport vcs = new VetoableChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Song.class.getSimpleName());

    /**
     * Use the SongFactory to create song instances.
     * <p>
     *
     * @param name
     * @param sgs
     * @param disableSongInternalUpdates
     */
    public SongImpl(String name, SongStructure sgs, boolean disableSongInternalUpdates)
    {
        Preconditions.checkArgument(name != null && !name.isBlank(), "name=%s", name);
        Objects.requireNonNull(sgs);

        setName(name);


        chordLeadSheet = sgs.getParentChordLeadSheet();
        ((ChordLeadSheetImpl) chordLeadSheet).setSong(this);
        songStructure = sgs;
        ((SongStructureImpl) songStructure).setSong(this);


        // All Song components share the same ExecutionManager instance
        executionManager = new ExecutionManager(this, disableSongInternalUpdates);
        ((ChordLeadSheetImpl) chordLeadSheet).setExecutionManager(executionManager);
        ((SongStructureImpl) songStructure).setExecutionManager(executionManager);


        // Mark song as modified if cls/sgs change, or if client properties are changed
        chordLeadSheet.addClsChangeListener(e -> fireIsModified());
        songStructure.addSgsChangeListener(e -> fireIsModified());
        clientProperties.addPropertyChangeListener(e -> fireIsModified());
    }

    public ExecutionManager getExecutionManager()
    {
        return executionManager;
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public Song getDeepCopy(boolean disableSongInternalUpdates)
    {
        return performReadAPImethod(() -> 
        {
            var cls = chordLeadSheet.getDeepCopy();
            var sgs = songStructure.getDeepCopy(cls);
            SongImpl res = new SongImpl(name, sgs, disableSongInternalUpdates);
            res.comments = comments;
            res.tempo = tempo;
            res.tags = tags;


            // Clone user phrases
            mapUserPhrases.keySet().stream()
                    .forEach(pName -> 
                    {
                        var p = mapUserPhrases.get(pName);
                        var pNew = p.clone();
                        res.mapUserPhrases.put(pName, pNew);
                    });


            // Copy client properties
            res.getClientProperties().set(getClientProperties());

            return res;
        });

    }

    @Override
    public void renameUserPhrase(final String name, final String newName)
    {
        performWriteAPImethod(renameUserPhraseOperation(name, newName));
    }

    public WriteOperation renameUserPhraseOperation(final String oldName, final String newName)
    {
        Objects.requireNonNull(oldName);
        Objects.requireNonNull(newName);
        Preconditions.checkArgument(!oldName.isBlank() && !newName.isBlank(), "oldName=%s, newName=%s", oldName, newName);


        WriteOperation operation = () -> 
        {
            if (oldName.equals(newName))
            {
                return WriteOperationResults.of(null);
            }


            var p = mapUserPhrases.get(oldName);
            Preconditions.checkArgument(p != null, "oldName=%s mapUserPhrases=%s", oldName, mapUserPhrases);


            // Perform the change
            final var oldMap = new HashMap<>(mapUserPhrases);
            mapUserPhrases.remove(oldName);
            mapUserPhrases.put(newName, p);
            final var newMap = new HashMap<>(mapUserPhrases);


            // Create the undoable event        
            UndoableEdit edit;
            edit = new SimpleEdit("Rename user phrase")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        mapUserPhrases = oldMap;

                        var event = new SongPropertyChangeEvent(SongImpl.this, PROP_PHRASE_NAME, newName, oldName);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        mapUserPhrases = newMap;

                        var event = new SongPropertyChangeEvent(SongImpl.this, PROP_PHRASE_NAME, oldName, newName);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);

            var event = new SongPropertyChangeEvent(SongImpl.this, PROP_PHRASE_NAME, oldName, newName);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setUserPhrase(String name, Phrase p) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(setUserPhraseOperation(name, p));
    }

    public ThrowingWriteOperation setUserPhraseOperation(String name, Phrase p)
    {
        checkNotNull(name);
        checkNotNull(p);
        checkArgument(!name.isBlank(), "name=%s", name);

        ThrowingWriteOperation operation = () -> 
        {
            if (getSongStructure().getSongParts().isEmpty())
            {
                return WriteOperationResults.of(null);
            }

            boolean add = getUserPhrase(name) == null;
            WriteOperationResults results = add ? addNewUserPhrase(name, p) : replaceUserPhrase(name, p);  // throws UnsupportedEditException

            return results;
        };

        return operation;
    }

    @Override
    public Phrase removeUserPhrase(String name)
    {
        return performWriteAPImethod(removeUserPhraseOperation(name));
    }

    public WriteOperation<Phrase> removeUserPhraseOperation(String name)
    {
        checkNotNull(name);

        WriteOperation operation = () -> 
        {

            final Phrase p = mapUserPhrases.get(name);
            if (p == null)
            {
                return WriteOperationResults.of(null);
            }


            // Perform the change
            p.removePropertyChangeListener(this);
            final var oldMap = new HashMap<>(mapUserPhrases);
            mapUserPhrases.remove(name);
            final var newMap = new HashMap<>(mapUserPhrases);


            // Create the undoable event        
            UndoableEdit edit;
            edit = new SimpleEdit("Remove user phrase")
            {
                @Override
                public void undoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        mapUserPhrases = oldMap;
                        p.addPropertyChangeListener(SongImpl.this);

                        var event = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE, p, name);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    performWriteAPImethod(() -> 
                    {
                        mapUserPhrases = newMap;
                        p.removePropertyChangeListener(SongImpl.this);

                        var event = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE, name, p);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);

            var event = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE, name, p);
            return WriteOperationResults.of(event, p);

        };

        return operation;

    }

    /**
     * Get all the names of the user phrases.
     *
     * @return Can't be null.
     */
    @Override
    public Set<String> getUserPhraseNames()
    {
        return performReadAPImethod(() -> new HashSet<>(mapUserPhrases.keySet()));
    }

    /**
     * Get the user phrase associated to specified name.
     * <p>
     * Returned phrase might be longer than the song.
     * <p>
     *
     * @param name
     * @return Null if no phrase associated to name. The Phrase channel should be ignored.
     * @see #setUserPhrase(java.lang.String, org.jjazz.phrase.api.Phrase)
     */
    @Override
    public Phrase getUserPhrase(String name)
    {
        return performReadAPImethod(() -> mapUserPhrases.get(name));
    }

    @Override
    public ChordLeadSheet getChordLeadSheet()
    {
        return chordLeadSheet;
    }

    @Override
    public SongStructure getSongStructure()
    {
        return songStructure;
    }

    /**
     * Convenience method which delegates to getSongStructure().getSizeInBars().
     *
     * @return
     */
    @Override
    public int getSize()
    {
        return songStructure.getSizeInBars();
    }

    /**
     * Get the preferred tempo for this song.
     *
     * @return
     */
    @Override
    public int getTempo()
    {
        return performReadAPImethod(() -> tempo);
    }


    /**
     * Set the preferred tempo for this song.
     * <p>
     * Fires a PROP_TEMPO property change event.
     *
     * @param newTempo
     */
    @Override
    public final void setTempo(final int newTempo)
    {
        Preconditions.checkArgument(TempoRange.checkTempo(newTempo), "newTempo=%s", newTempo);

        performWriteAPImethod(() -> 
        {
            int oldTempo = tempo;
            tempo = newTempo;

            var event = new SongPropertyChangeEvent(this, PROP_TEMPO, oldTempo, newTempo);
            return WriteOperationResults.of(event, null);
        });
    }

    /**
     * Set the list of String tags associated to this song, e.g. "rock", "dance-oriented", etc...
     * <p>
     * Fires a PROP_TAGS property change events.
     *
     * @param newTags Must not be null but can be an empty list. Tags are space-trimmed and converted to lower case.
     */
    @Override
    public void setTags(List<String> newTags)
    {
        Objects.requireNonNull(newTags);

        performWriteAPImethod(() -> 
        {
            final ArrayList<String> oldTags = new ArrayList<>(tags);
            final ArrayList<String> newTagsLowerCase = new ArrayList<>();
            for (String s : newTags)
            {
                newTagsLowerCase.add(s.trim().toLowerCase());
            }
            tags = newTagsLowerCase;

            var event = new SongPropertyChangeEvent(this, PROP_TAGS, oldTags, tags);
            return WriteOperationResults.of(event, null);
        });
    }

    /**
     * @return List can be empty if not tags. Tags are lowercase.
     */
    @Override
    public List<String> getTags()
    {
        return performReadAPImethod(() -> new ArrayList<>(tags));
    }

    /**
     * The song name.
     *
     * @return
     */
    @Override
    public String getName()
    {
        return performReadAPImethod(() -> name);
    }

    /**
     * Set the song name.
     * <p>
     * Fire a PROP_NAME property change event.
     *
     * @param newName A non-empty string.
     */
    @Override
    public final void setName(final String newName)
    {
        Objects.requireNonNull(newName);
        Preconditions.checkArgument(!newName.isBlank(), "newName=%s", newName);

        performWriteAPImethod(() -> 
        {
            final String oldName = name;
            name = newName;

            var event = new SongPropertyChangeEvent(this, PROP_NAME, oldName, newName);
            return WriteOperationResults.of(event, null);
        });
    }

    /**
     * Mark song instance as closed, it should not be used anymore.
     * <p>
     * Fires a PROP_CLOSED property change event.
     *
     * @param releaseRhythmResources True if the method should also call releaseResources() for each used rhythm.
     */
    @Override
    public void close(boolean releaseRhythmResources)
    {
        closed = true;

        var phrases = performReadAPImethod(() -> 
        {
            return new ArrayList<>(mapUserPhrases.values());
        });
        for (var p : phrases)
        {
            p.removePropertyChangeListener(this);
        }

        clientProperties.removePropertyChangeListener(this);

        pcs.firePropertyChange(PROP_CLOSED, false, true);

        if (releaseRhythmResources)
        {
            for (Rhythm r : songStructure.getUniqueRhythms(false, false))
            {
                r.releaseResources();
            }
        }
    }

    /**
     *
     * @return True if close() has been called.
     */
    @Override
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * The comments associated to this song.
     *
     * @return Can be an empty String.
     */
    @Override
    public String getComments()
    {
        return performReadAPImethod(() -> comments);
    }

    /**
     * Set the comments.
     * <p>
     * Fires the PROP_COMMENTS change event.
     *
     * @param newComments
     */
    @Override
    public void setComments(final String newComments)
    {
        Objects.requireNonNull(newComments);

        performWriteAPImethod(() -> 
        {
            final String oldComments = comments;
            comments = newComments;

            var event = new SongPropertyChangeEvent(this, PROP_COMMENTS, oldComments, newComments);
            return WriteOperationResults.of(event, null);
        });
    }

    /**
     * The file where this song is stored.
     *
     * @return Can be null for example if it's a builtin song or created programmatically.
     */
    @Override
    public File getFile()
    {
        return file;
    }

    /**
     * Set the file from which the song can be read/written.
     *
     * @param f Can be null.
     */
    @Override
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
    @Override
    public boolean saveToFileNotify(File f, boolean isCopy)
    {
        Objects.requireNonNull(f);

        boolean b = true;
        if (f.exists() && !f.canWrite())
        {
            String msg = ResUtil.getString(getClass(), "ErrCantOverrideSong", f.getName());
            LOGGER.log(Level.WARNING, "saveToFileNotify() {0}", msg);
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
                LOGGER.log(Level.WARNING, "saveToFileNotify() {0}", msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                b = false;
            }
        }

        return b;
    }

    /**
     * Save this song to a file.
     * <p>
     * Song file/name are set according to songFile. Fires a PROP_MODIFIED_OR_SAVED_OR_RESET property change event with oldValue=true and newValue=false.
     *
     * @param songFile
     * @param isCopy   If true do not update this instance, PROP_MODIFIED_OR_SAVED_OR_RESET change event is not fired.
     * @throws java.io.IOException
     * @see getFile()
     */
    @Override
    public void saveToFile(File songFile, boolean isCopy) throws IOException
    {
        Objects.requireNonNull(songFile);


        if (!isCopy)
        {
            file = songFile;
        }

        try (FileOutputStream fos = new FileOutputStream(songFile))
        {
            XStream xstream = XStreamInstancesManager.getInstance().getSaveSongInstance();
            Writer w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));        // Needed to support special/accented chars
            Song songCopy = getDeepCopy(true);          // capture a clean copy
            xstream.toXML(songCopy, w);
            if (!isCopy)
            {
                setName(removeSongExtension(songFile.getName()));
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
    @Override
    public boolean isSaveNeeded()
    {
        return saveNeeded;
    }

    /**
     * Set or reset the "save needed" status, i.e if song has some unsaved changes or not.
     * <p>
     * Fires a PROP_MODIFIED_OR_SAVED_OR_RESET change event with the relevant values.
     *
     * @param b
     */
    @Override
    public void setSaveNeeded(boolean b)
    {
        if (b == saveNeeded)
        {
            return;
        }
        if (b)
        {
            fireIsModified();
        } else
        {
            saveNeeded = false;
            pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED_OR_RESET, null, false);
        }
    }


    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        Objects.requireNonNull(l);
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        Objects.requireNonNull(l);
        undoListeners.remove(l);
    }

    /**
     * Listen to non-vetoable property changes.
     *
     * @param l
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    /**
     * Listen to the specified non-vetoable property change.
     *
     * @param propertyName
     * @param l
     */
    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propertyName, l);
    }

    /**
     * Fire a non-vetoable change event.
     *
     * @param e
     */
    public void fireChangeEvent(PropertyChangeEvent e)
    {
        Objects.requireNonNull(e);
        pcs.firePropertyChange(e);
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

    static public String removeSongExtension(String str)
    {
        String ext = "." + SONG_EXTENSION;
        int indexExt = str.toLowerCase().lastIndexOf(ext.toLowerCase());
        if (indexExt == -1)
        {
            return str;
        } else
        {
            return str.substring(0, indexExt);
        }
    }


    private <R> R performWriteAPImethod(WriteOperation<R> operation)
    {
        R res = executionManager.executeWriteOperation(operation);
        return res;
    }

    private <R> R performWriteAPImethodThrowing(ThrowingWriteOperation<R> operation) throws UnsupportedEditException
    {
        R res = executionManager.executeWriteOperationThrowing(operation);
        return res;
    }

    public <R> R performReadAPImethod(Supplier<R> operation)
    {
        R res = executionManager.executeReadOperation(operation);
        return res;
    }

    public <R, E extends Exception> R performReadAPImethodThrowing(ThrowingSupplier<R, E> operation) throws E
    {
        R res = executionManager.executeReadOperationThrowing(operation);
        return res;
    }

    public void preCheckChange(SongPropertyChangeEvent event) throws UnsupportedEditException
    {
        executionManager.preCheckChange(event);
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

        if (e.getSource() instanceof Phrase p)
        {
            // Listen to our user phrases changes to fire a PROP_USER_PHRASE_CONTENT event
            if (!Phrase.isAdjustingEvent(e.getPropertyName()))
            {
                String phraseName = performReadAPImethod(() -> getPhraseName(p));
                assert phraseName != null;
                fireChangeEvent(new SongPropertyChangeEvent(this, PROP_USER_PHRASE_CONTENT, null, phraseName));
                fireIsModified();
            }
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
        saveNeeded = true;
        fireChangeEvent(new PropertyChangeEvent(this, PROP_MODIFIED_OR_SAVED_OR_RESET, false, true));
    }

    /**
     * Fire a PROP_MODIFIED_OR_SAVED_OR_RESET property change event with oldValue=true newValue=false
     */
    private void fireSaved()
    {
        saveNeeded = false;
        fireChangeEvent(new PropertyChangeEvent(this, PROP_MODIFIED_OR_SAVED_OR_RESET, true, false));
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        Objects.requireNonNull(edit);
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners)
        {
            l.undoableEditHappened(event);

        }
    }

    private String getPhraseName(Phrase p)
    {
        return mapUserPhrases.keySet().stream()
                .filter(n -> getUserPhrase(n) == p)
                .findAny()
                .orElseThrow();
    }

    /**
     * Add a new phrase.
     *
     * @param name
     * @param p
     * @return
     * @throws UnsupportedEditException If user phase can not be added
     */
    private WriteOperationResults addNewUserPhrase(String name, Phrase p) throws UnsupportedEditException
    {
        // Check for possible veto
        var event = new SongPropertyChangeEvent(this, PROP_USER_PHRASE, p, name);
        preCheckChange(event);   // throws UnsupportedEditException


        // Perform the change
        final var oldMap = new HashMap<>(mapUserPhrases);
        mapUserPhrases.put(name, p);
        final var newMap = new HashMap<>(mapUserPhrases);


        // Listen to phrase changes to forward a PROP_USER_PHRASE_CONTENT change event
        p.addPropertyChangeListener(this);


        // Create the undoable event        
        UndoableEdit edit;
        edit = new SimpleEdit("Add user phrase")
        {
            @Override
            public void undoBody()
            {
                performWriteAPImethod(() -> 
                {
                    mapUserPhrases = oldMap;
                    p.removePropertyChangeListener(SongImpl.this);

                    var evt = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE, name, p);
                    evt.setIsUndo();
                    return WriteOperationResults.of(evt, null);
                });
            }

            @Override
            public void redoBody()
            {
                performWriteAPImethod(() -> 
                {
                    mapUserPhrases = newMap;
                    p.addPropertyChangeListener(SongImpl.this);

                    var evt = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE, p, name);
                    evt.setIsRedo();
                    return WriteOperationResults.of(evt, null);
                });
            }
        };

        fireUndoableEditHappened(edit);

        return WriteOperationResults.of(event, null);

    }

    /**
     * Replace the phrase associated to a name.
     *
     * @param name
     * @param pNew
     * @return
     */
    private WriteOperationResults replaceUserPhrase(String name, Phrase pNew)
    {
        // Perform the change
        final var oldMap = new HashMap<>(mapUserPhrases);
        final var pOld = mapUserPhrases.put(name, pNew);
        assert pOld != null;
        final var newMap = new HashMap<>(mapUserPhrases);


        pOld.removePropertyChangeListener(this);
        pNew.addPropertyChangeListener(this);


        // Create the undoable event        
        UndoableEdit edit;
        edit = new SimpleEdit("Replace user phrase")
        {
            @Override
            public void undoBody()
            {
                performWriteAPImethod(() -> 
                {
                    mapUserPhrases = oldMap;
                    pNew.removePropertyChangeListener(SongImpl.this);
                    pOld.addPropertyChangeListener(SongImpl.this);

                    var event = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE_CONTENT, pNew, name);
                    event.setIsUndo();
                    return WriteOperationResults.of(event, null);
                });
            }

            @Override
            public void redoBody()
            {
                performWriteAPImethod(() -> 
                {
                    mapUserPhrases = newMap;
                    pOld.removePropertyChangeListener(SongImpl.this);
                    pNew.addPropertyChangeListener(SongImpl.this);

                    var event = new SongPropertyChangeEvent(SongImpl.this, PROP_USER_PHRASE_CONTENT, pOld, name);
                    event.setIsRedo();
                    return WriteOperationResults.of(event, null);
                });
            }
        };

        fireUndoableEditHappened(edit);

        // Ideally, to preserve concurrency, we should also embed pNew in the event, so that a listener does not have to call Song.getUserPhrase() to do the required updates.
        // But risk is minimal as we don't expect several concurrent threads to replace a phrase in the same song.
        var event = new SongPropertyChangeEvent(this, PROP_USER_PHRASE_CONTENT, pOld, name);
        return WriteOperationResults.of(event, null);
    }

    // --------------------------------------------------------------------- 
    // Inner classes
    // ---------------------------------------------------------------------

    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    xstream.alias("Song", SongImpl.class);
                    xstream.alias("SongSP", SongImpl.SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spName");
                    xstream.useAttributeFor(SerializationProxy.class, "spTempo");
                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }

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
     * Serialization proxy.
     * <p>
     * spVERSION 2 and 3 changes saved fields, see below.<br>
     * spVERSION 4 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction)
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 571097826016222L;

        private int spVERSION = 4;   // Do not make final!
        private String spName;
        private String spComments;
        private int spTempo;
        private List<String> spTags;
        private ChordLeadSheet spChordLeadSheet;
        private SongStructure spSongStructure;
        // New since spVERSION 2
        private Map<String, Phrase> spMapUserPhrases;
        // Until spVERSION 2
        private Properties spClientProperties;
        // Since spVERSION 3, replace spClientProperties
        private StringProperties spClientPropertiesV3;

        private SerializationProxy(SongImpl s)
        {
            spChordLeadSheet = s.getChordLeadSheet();
            spSongStructure = s.getSongStructure();
            spName = s.getName();
            spComments = s.getComments();
            spTempo = s.getTempo();
            spTags = s.getTags();
            // Since spVERSION 3
            spClientPropertiesV3 = s.clientProperties;
            // Since spVERSION 2
            spMapUserPhrases = s.mapUserPhrases;
        }

        private Object readResolve() throws ObjectStreamException
        {
            assert spChordLeadSheet == spSongStructure.getParentChordLeadSheet();
            Song newSong = new SongImpl(spName, spSongStructure, false);
            newSong.setComments(spComments);
            newSong.setTags(spTags);
            newSong.setTempo(spTempo);

            // Since spVERSION 2
            if (spMapUserPhrases != null)
            {
                for (String name : spMapUserPhrases.keySet())
                {
                    Phrase p = spMapUserPhrases.get(name);
                    try
                    {
                        newSong.setUserPhrase(name, p);
                    } catch (UnsupportedEditException ex)
                    {
                        LOGGER.log(Level.WARNING, "readResolve() Can''t add user phrase for name={0}. ex={1}", new Object[]
                        {
                            name,
                            ex.getMessage()
                        });
                    }
                }
            }

            // Client properties format has changed from spVERSION 3 (JJazzLab 4)
            if (spVERSION <= 2)
            {
                for (String key : spClientProperties.stringPropertyNames())
                {
                    newSong.getClientProperties().put(key, spClientProperties.getProperty(key));
                }
                importV2properties(newSong);

            } else if (spClientPropertiesV3 != null)
            {
                newSong.getClientProperties().set(spClientPropertiesV3);
            } else
            {
                LOGGER.log(Level.WARNING,
                        "readResolve() Unexpected null value for spClientPropertiesV3, ignoring client properties. Song name={0}",
                        newSong.getName());
            }


            return newSong;
        }

        /**
         * Import the old spVERSION 2 song properties.
         * <p>
         * Up to spVERSION 2, all song editor settings were saved as Song client properties. From spVERSION 3 (JJazzLab 4) some of these settings are directly
         * saved with the related model object. This is the case for section quantification and startOnNewLine settings, which are now saved as CLI_Section
         * client properties.
         * <p>
         * IMPORTANT: this is a hack, make sure that CL_Editor PROP_* string values are consistent with what's here (we can't depend on CL_Editor because of
         * circular dependency).
         *
         * @param newSong
         */
        private void importV2properties(Song newSong)
        {
            for (var prop : newSong.getClientProperties().getPropertyNames())
            {

                // Search for pre-spVersion3 client properties
                if (prop.startsWith("SectionDisplayQuantization-"))
                {
                    int indexStar = prop.indexOf('*');
                    if (indexStar == -1)
                    {
                        indexStar = prop.length();
                    }
                    String sectionName = prop.substring("SectionDisplayQuantization-".length(), indexStar);
                    CLI_Section cliSection = newSong.getChordLeadSheet().getSection(sectionName);
                    if (cliSection != null)
                    {
                        // Store the quantization as a CLI_Section client property
                        String qString = newSong.getClientProperties().get(prop);
                        if (Quantization.isValidStringValue(qString))
                        {
                            cliSection.getClientProperties().put("PropSectionQuantization", qString);
                        }
                    } else
                    {
                        LOGGER.log(Level.WARNING,
                                "SerializationProxy.importV2properties() Unexpected null value for cliSection. Ignoring this client property. prop={0}",
                                prop);
                    }

                } else if (prop.startsWith("PropSectionStartOnNewLine-"))
                {
                    String sectionName = prop.substring("PropSectionStartOnNewLine-".length());
                    CLI_Section cliSection = newSong.getChordLeadSheet().getSection(sectionName);
                    if (cliSection != null)
                    {
                        // Store the setting as a CLI_Section client property
                        String qString = newSong.getClientProperties().get(prop);

                        cliSection.getClientProperties().put("PropSectionStartOnNewLine", qString);
                    } else
                    {
                        LOGGER.log(Level.WARNING,
                                "SerializationProxy.importV2properties() Unexpected null value for cliSection. Ignoring this client property. prop={0}",
                                prop);
                    }

                }
            }
        }
    }

}
