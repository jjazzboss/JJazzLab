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
package org.jjazz.spteditor.spi;

import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.DefaultRpEditorComponentFactoryImpl;
import org.openide.util.Lookup;

/**
 * A special RpEditorComponentFactory only for the default RhythmParameters.
 */
public interface DefaultRpEditorComponentFactory extends RpEditorComponentFactory
{

    /**
     * The types of RP editors supported by this factory.
     */
    public enum Type
    {
        LIST, SPINNER, COMBO, CUSTOM_DIALOG, STUB
    };

    /**
     * The default RpEditorComponentFactory.
     * <p>
     * If an instance is available in the global lookup, return it, otherwise return a default implementation.
     *
     * @return
     */
    static public DefaultRpEditorComponentFactory getDefault()
    {
        DefaultRpEditorComponentFactory result = Lookup.getDefault().lookup(DefaultRpEditorComponentFactory.class);
        if (result == null)
        {
            result = DefaultRpEditorComponentFactoryImpl.getInstance();
        }
        return result;
    }

    /**
     * The DefaultRpEditorComponentFactory must provide a RpEditor for all rps.
     *
     * @param rp
     * @return True
     */
    @Override
    default boolean isRpSupported(RhythmParameter<?> rp)
    {
        return true;
    }

    /**
     * Create a RpEditorComponent of the specified type.
     *
     * @param song Can be null except for type CUSTOM_DIALOG
     * @param type
     * @param spt
     * @param rp
     * @return Can be null if rp is not supported.
     * @throws IllegalArgumentException If rp class does not match to RpEditor type.
     */
    public RpEditorComponent createComponent(Song song, Type type, SongPart spt, RhythmParameter<?> rp);

}
