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
package org.jjazz.base.api.actions;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeListener;

/**
 * How to use:
 * <p>
 * - Create a Savable instance<br>
 * - Add it to the ToBeSavedList =&gt; it will be used by SaveAll, and also to confirm exit with unsaved changes<br>
 * - Add it in the TopComponent's lookup =&gt; Save action will be enabled if TopComponent is active<br>
 * - When save is done (e.g. end of save() method), remove the Savable from the ToBeSavedList and the TopComponent's lookup.<br>
 * <p>
 */
public interface Savable
{

    /**
     * Perfom the save operation.
     *
     * @return An error code different from 0 if problem occured.
     */
    public int save();

    /**
     * A string describing the object saved by this Savable instance.
     *
     * @return
     */
    @Override
    public String toString();

    /**
     * The list of files that need to be saved.
     * <p>
     */
    static public final class ToBeSavedList
    {

        static private ArrayList<Savable> savables = new ArrayList<>();
        static private ArrayList<ChangeListener> listeners = new ArrayList<>();

        /**
         * Add a Savable to the list if not already present.
         *
         * @param s
         */
        static public void add(Savable s)
        {
            if (!savables.contains(s))
            {
                savables.add(s);
                fireChanged();
            }
        }

        static public void remove(Savable s)
        {
            if (savables.contains(s))
            {
                savables.remove(s);
                fireChanged();
            }

        }

        static private void fireChanged()
        {
            for (ChangeListener cl : listeners)
            {
                cl.stateChanged(null);
            }
        }

        static public List<Savable> getSavables()
        {
            return new ArrayList<Savable>(savables);
        }

        static public void addListener(ChangeListener l)
        {
            if (!listeners.contains(l))
            {
                listeners.add(l);
            }
        }

        static public void removeListener(ChangeListener l)
        {
            listeners.remove(l);
        }
    }
}
