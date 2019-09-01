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
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.rhythm.parameters.RP_StringSet;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.spteditor.spi.RpEditor;
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

    public RpEditorList(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);

        if (!(rp instanceof RP_StringSet))
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp);
        }

        // Prepare our editor component
        list_rpValue = new JList<>();

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
    protected JComponent getEditor()
    {
        return scrollPane;
    }

    /**
     * Update the value in the editor.
     *
     * @param value
     * @param firePropChangeEvent If false don't fire a change event.
     */
    @Override
    public void setRpValue(Object value, boolean firePropChangeEvent)
    {
        if (!(value instanceof Set<?>))
        {
            throw new IllegalArgumentException("value=" + value);
        }
        if (value != null && !value.equals(getRpValue()))
        {
            if (!firePropChangeEvent)
            {
                list_rpValue.removeListSelectionListener(this);
            }
            @SuppressWarnings("unchecked")
            Set<String> values = (Set<String>) value;
            list_rpValue.clearSelection();
            int[] indices = new int[values.size()];
            int i = 0;
            for (String v : values)
            {
                indices[i] = possibleValues.indexOf(v);
                i++;
            }
            list_rpValue.setSelectedIndices(indices);
            if (!firePropChangeEvent)
            {
                list_rpValue.addListSelectionListener(this);
            }
        }
    }

    @Override
    protected void showMultiValueMode(boolean b)
    {
        showMultiModeUsingFont(isMultiValueMode(), list_rpValue);
    }

    @Override
    public Object getRpValue()
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
            Object newValue = getRpValue();
            firePropertyChange(PROP_RPVALUE, null, newValue);
        }
    }
}
