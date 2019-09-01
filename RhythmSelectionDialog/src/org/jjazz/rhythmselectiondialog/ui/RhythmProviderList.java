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
package org.jjazz.rhythmselectiondialog.ui;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.FavoriteRhythmProvider;
import org.jjazz.rhythm.database.api.FavoriteRhythms;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.openide.util.WeakListeners;

/**
 * A special JList to show RhythmProviders.
 * <p>
 */
public class RhythmProviderList extends JList<RhythmProvider> implements ChangeListener, PropertyChangeListener
{

    // private static final Logger LOGGER = Logger.getLogger(RhythmProviderList.class.getSimpleName());
    private TimeSignature tsFilter = null;

    /**
     * A list with no time signature filter.
     */
    public RhythmProviderList()
    {
        setCellRenderer(new RhythmProviderRenderer());
        RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
        // WeakListener just in case, I don't how this component will be used
        rdb.addChangeListener(WeakListeners.change(this, rdb));
        FavoriteRhythms fr = FavoriteRhythms.getInstance();
        fr.addPropertyListener(WeakListeners.propertyChange(this, fr));
    }

    /**
     * Show RhythmProvider data only for the specified TimeSignature.
     * <p>
     * E.g. this will change the nb of rhythms available for each RhythmProvider.
     *
     * @param ts If null all TimeSignature are used
     */
    public void setTimeSignatureFilter(TimeSignature ts)
    {
        tsFilter = ts;
        repaint();
    }

    /**
     *
     * @return can be null if no filter set.
     */
    public TimeSignature getTimeSignatureFilter()
    {
        return tsFilter;
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        // The rhythmdatabase has changed, force a refresh
        repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // Nb of favorite rhythms has changed, force a refresh
        repaint();
    }

    private class RhythmProviderRenderer extends DefaultListCellRenderer
    {

        /**
         * Add the nb of the rhythms of the RhythmProvider and a tooltip.
         * <p>
         * Also handle the special case of the FavoriteRhythmProvider instance.
         *
         * @param list
         * @param value
         * @param index
         * @param isSelected
         * @param cellHasFocus
         * @return
         */
        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
            RhythmProvider rp = (RhythmProvider) value;
            RhythmProvider.Info rpi = rp.getInfo();
            List<Rhythm> rhythms = (rp == FavoriteRhythmProvider.getInstance()) ? FavoriteRhythmProvider.getInstance().getBuiltinRhythms() : rdb.getRhythms(rp);
            int size = tsFilter == null ? rhythms.size() : rdb.getRhythms(tsFilter, rhythms).size();
            setText(rpi.getName() + " (" + size + ")");
            setToolTipText(rpi.getDescription() + " - version " + rpi.getVersion());
            return c;
        }
    }
}
