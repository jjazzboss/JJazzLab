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
package org.jjazz.spteditor;

import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.spteditor.api.SptEditorSettings;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SptEditorSettings.class)
public class SptEditorSettingsImpl extends SptEditorSettings
{

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(SptEditorSettingsImpl.class);
    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    @Override
    public void setNameFont(Font font)
    {
        Font old = getNameFont();
        String strFont = Utilities.fontAsString(font);
        prefs.put(PROP_RHYTHM_FONT, strFont);
        pcs.firePropertyChange(PROP_RHYTHM_FONT, old, font);
    }

    @Override
    public Font getNameFont()
    {
        String strFont = prefs.get(PROP_RHYTHM_FONT, "Helvetica-BOLD-15");
        return Font.decode(strFont);
    }

    @Override
    public void setRhythmFont(Font font)
    {
        Font old = getRhythmFont();
        String strFont = Utilities.fontAsString(font);
        prefs.put(PROP_RHYTHM_FONT, strFont);
        pcs.firePropertyChange(PROP_RHYTHM_FONT, old, font);
    }

    @Override
    public Font getRhythmFont()
    {
        String strFont = prefs.get(PROP_RHYTHM_FONT, "Helvetica-PLAIN-10");
        return Font.decode(strFont);
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }




}
