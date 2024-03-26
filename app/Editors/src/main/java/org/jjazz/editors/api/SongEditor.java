package org.jjazz.editors.api;

import javax.swing.JPanel;
import org.jjazz.song.api.Song;

/**
 * An editor to edit a song (or part of a song).
 */
public interface SongEditor
{

    /**
     * The ui component of the editor.
     *
     * @return
     */
    JPanel getComponent();

    /**
     * The song model.
     *
     * @return
     */
    Song getSongModel();
}
