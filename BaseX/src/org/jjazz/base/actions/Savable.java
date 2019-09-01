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
package org.jjazz.base.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.event.ChangeListener;
import static org.jjazz.base.actions.Bundle.CTL_ConfirmExit;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.OnStop;
import org.openide.util.NbBundle;

/**
 * How to use:
 * <p>
 * - Create the Savable<br>
 * - Add it to the ToBeSavedList => will be used saved by SaveAll, and to confirm exit with unsaved changes<br>
 * - Add it in the TopComponent's lookup => Save action will be enabled if TopComponent is active<br>
 * - When save is done (e.g. end of save() method), remove the Savable from the ToBeSavedList and the TopComponent's lookup.<br>
 * <p>
 */
@NbBundle.Messages(
        {
            "CTL_ConfirmExit=Unsaved changes in the files below. OK to exit anyway ?"
        })
public interface Savable
{

    /**
     * Perfom the save operation.
     *
     * @return An error code different from 0 if problem occured.
     */
    public int save();

    /**
     * The list of files that need to be saved.
     * <p>
     * On shutdown pop up a warning to confirm "quit with unsaved changes" if the list is not empty
     */
    @OnStop
    static public final class ToBeSavedList implements Callable<Boolean>
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

        @Override
        public Boolean call()
        {
            // Ask user confirmation if there are still files to be saved
            if (!savables.isEmpty())
            {
                StringBuilder msg = new StringBuilder();
                msg.append(CTL_ConfirmExit()).append("\n");
                for (Savable s : savables)
                {
                    msg.append(s.toString()).append("\n");
                }

                NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg.toString(), NotifyDescriptor.OK_CANCEL_OPTION);
                Object result = DialogDisplayer.getDefault().notify(nd);
                if (result != NotifyDescriptor.OK_OPTION)
                {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }
    }
}
