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
package org.jjazz.ui.spteditor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.rhythm.parameters.RP_StringSet;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;

/**
 * A RpEditor using a JList for RP_StringSet parameters.
 * <p>
 */
public class RpEditorList extends RpEditor implements ListSelectionListener
{

    private final JScrollPane scrollPane;
    private final JList<String> list_rpValue;
    private final List<String> possibleValues = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RpEditorList.class.getSimpleName());

    /**
     * Create a JList renderer for a Rp_StringSet RhythmParameter.
     *
     * @param spt
     * @param rp
     * @param renderer Can be null
     */
    public RpEditorList(SongPart spt, RhythmParameter<?> rp, ListCellRenderer<? super String> renderer)
    {
        super(spt, rp);

        if (!(rp instanceof RP_StringSet))
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp);   //NOI18N
        }

        // Prepare our editor component
        list_rpValue = new JList<>();
        if (renderer != null)
        {
            list_rpValue.setCellRenderer(renderer);
        }

        list_rpValue.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        RP_StringSet rpss = (RP_StringSet) rp;
        possibleValues.addAll(rpss.getPossibleValues().get(0));
        list_rpValue.setListData(possibleValues.toArray(new String[0]));
        list_rpValue.setVisibleRowCount(possibleValues.size());
        list_rpValue.setSelectedValue(spt.getRPValue(rpss), true);

        scrollPane = new JScrollPane(list_rpValue);
        setEditor(scrollPane);
        list_rpValue.addListSelectionListener(this);
    }

    @Override
    protected JComponent getEditorComponent()
    {
        return scrollPane;
    }

    @Override
    public void updateEditorValue(Object value)
    {
        if (!(value instanceof Set<?>))
        {
            throw new IllegalArgumentException("value=" + value);   //NOI18N
        }
        if (value != null && !value.equals(getEditorValue()))
        {
            list_rpValue.removeListSelectionListener(this);
            @SuppressWarnings("unchecked")
            Set<String> values = (Set<String>) value;
            list_rpValue.clearSelection();            
            int[] indices  = values.stream().mapToInt(v -> possibleValues.indexOf(value)).toArray();                    
            list_rpValue.setSelectedIndices(indices);
            list_rpValue.addListSelectionListener(this);
        }
    }

    @Override
    protected void showMultiValueMode(boolean b)
    {
        showMultiModeUsingFont(isMultiValueMode(), list_rpValue);
    }

    @Override
    public Object getEditorValue()
    {
        HashSet<String> set = new HashSet<>();
        List<String> selValues = list_rpValue.getSelectedValuesList();
        set.addAll(selValues);
        return set;
    }

    @Override
    public void cleanup()
    {
        list_rpValue.removeListSelectionListener(this);
    }

    // -----------------------------------------------------------------------------
    // ListSelectionListener interface
    // -----------------------------------------------------------------------------
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            Object newValue = getEditorValue();
            firePropertyChange(PROP_RPVALUE, null, newValue);
        }
    }
}
