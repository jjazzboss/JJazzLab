package org.jjazz.songeditormanager;

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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;

/**
 * Save song action.
 * <p>
 * Listen to the song presence in the actionGlobalContext, then listen to song's savedNeeded property to enable/disable the action.
 */
@ActionID(
        category = "File", id = "org.jjazz.songeditormanager.api.Save"
)
@ActionRegistration(displayName = "#CTL_Save", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1500),
            @ActionReference(path = "Toolbars/File", position = 20),
            @ActionReference(path = "Shortcuts", name = "D-S")
        })
public final class Save extends AbstractAction implements PropertyChangeListener
{

    private Song song;
    private final Lookup.Result<Song> songLkpResult;
    private final LookupListener songLkpListener;
    private static final Logger LOGGER = Logger.getLogger(Save.class.getSimpleName());

    public Save()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_Save"));
        // Need this for auto icon size changing to work... (switch to saveAll24.gif) since can't be done using actionRegistration's iconBase=xx
        putValue("iconBase", "org/jjazz/songeditormanager/resources/save.png");


        Lookup context = org.openide.util.Utilities.actionsGlobalContext();
        songLkpListener = le -> songPresenceChanged(le);
        songLkpResult = context.lookupResult(Song.class);
        songLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, songLkpListener, songLkpResult));
        songPresenceChanged(null);
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        File songFile = song.getFile();
        int res;
        if (songFile == null)
        {
            // Do like SaveAs
            res = SaveUtils.SaveAs(song);
        } else
        {
            res = SaveUtils.saveSongAndMix(song, songFile);
        }

        LOGGER.log(Level.FINE, "actionPerformed() song={0} res={1}", new Object[]
        {
            song, res
        });

        if (res == 0)
        {
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_Saved", song.getFile().getAbsolutePath()));
        }
    }

    // ======================================================================================================
    // PropertyChangeListener interface
    // ======================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // LOGGER.fine(Utilities.toDebugString(evt));
        if (evt.getSource() == song)
        {
            if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET))
            {
                setEnabled(evt.getNewValue().equals(Boolean.TRUE));

            } else if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                song.removePropertyChangeListener(this);
            }
        }
    }

    // ======================================================================================================    
    // Private methods
    // ======================================================================================================   
    private void songPresenceChanged(LookupEvent le)
    {
        // Can be sometimes called out of the EDT
        Song newSong;
        if (le != null)
        {
            @SuppressWarnings("unchecked")
            Lookup.Result<Song> leRes = (Lookup.Result<Song>) le.getSource();
            var songs = leRes.allInstances();
            newSong = songs.isEmpty() ? null : songs.iterator().next();

        } else
        {
            newSong = org.openide.util.Utilities.actionsGlobalContext().lookup(Song.class);
        }

        LOGGER.log(Level.FINE, "songPresenceChanged() -- song={0} newSong={1}", new Object[]
        {
            song, newSong
        });

        if (song != null)
        {
            song.removePropertyChangeListener(this);
        }

        song = newSong;
        boolean b = false;
        if (song != null)
        {
            song.addPropertyChangeListener(this);
            b = song.isSaveNeeded();
        }

        final boolean b2 = b;
        org.jjazz.uiutilities.api.UIUtilities.invokeLaterIfNeeded(() -> setEnabled(b2));
    }

}
