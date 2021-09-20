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
package org.jjazz.ui.musiccontrolactions.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jerome
 */
public class RemoteActionManager
{

    private static RemoteActionManager INSTANCE = null;
    public final static String PREF_NB_REMOTE_ACTIONS = "NbRemoteActions";
    public final static String PREF_PREFIX = "RemoteAction";
    

    private int nbRemoteActions;
    private List<RemoteAction> remoteActions=new ArrayList<>();
    private static final Preferences prefs = NbPreferences.forModule(RemoteActionManager.class);
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(RemoteActionManager.class.getSimpleName());

    static public RemoteActionManager getInstance()
    {
        synchronized (RemoteActionManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RemoteActionManager();
            }
        }
        return INSTANCE;
    }

    private RemoteActionManager()
    {
        nbRemoteActions = prefs.getInt(PREF_NB_REMOTE_ACTIONS, 0);
        
        // Load each RemoteAction
        for (int i=0; i<nbRemoteActions; i++)
        {
            String s = prefs.get(PREF_PREFIX+i, null);
            if (s!=null)
            {
                
            }
        }        
    }
    
    public List<RemoteAction> getRemoteActions()
    {
        
    }
    
    public void addRemoteAction(RemoteAction ra)
    {
        
    }

    // ================================================================================================
    // Private methods
    // ================================================================================================
    
    private String getRemoteActionKey()
    {
        
    }
}
