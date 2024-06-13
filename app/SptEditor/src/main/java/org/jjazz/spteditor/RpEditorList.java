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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jjazz.rhythm.api.RP_StringSet;
import org.jjazz.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.spi.RpEditorComponent;

/**
 * A RpEditor using a JList for RP_StringSet parameters.
 * <p>
 */
public class RpEditorList extends RpEditorComponent<Set<String>> implements ListSelectionListener
{
    
    JScrollPane scrollPane;
    JList<String> list_rpValue;
    private final List<String> possibleValues = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RpEditorList.class.getSimpleName());

    /**
     * Create a JList renderer for a Rp_StringSet RhythmParameter.
     *
     * @param spt
     * @param rp
     * @param renderer Can be null
     */
    public RpEditorList(SongPart spt, RP_StringSet rp, ListCellRenderer<? super String> renderer)
    {
        super(spt, rp);

        // Prepare our editor component
        list_rpValue = new JList<>();
        list_rpValue.addListSelectionListener(this);
        if (renderer != null)
        {
            list_rpValue.setCellRenderer(renderer);
        }
        list_rpValue.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        possibleValues.addAll(rp.getPossibleValues().get(0));   // see special return value of RP_StringSet.getPossibleValues() 
        list_rpValue.setListData(possibleValues.toArray(new String[0]));
        list_rpValue.setVisibleRowCount(possibleValues.size());
        list_rpValue.setSelectedValue(spt.getRPValue(rp), true);


        // Add it
        setLayout(new BorderLayout());
        scrollPane = new JScrollPane(list_rpValue);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Overridden because we need to reach the JList in the JScrollPane.
     *
     * @param b
     */
    @Override
    public void setEnabled(boolean b)
    {
        scrollPane.setEnabled(b);
        list_rpValue.setEnabled(b);
    }
    
    @Override
    public void updateEditorValue(Set<String> value)
    {
        if (value != null && !value.equals(getEditorValue()))
        {
            list_rpValue.removeListSelectionListener(this);
            list_rpValue.clearSelection();
            int[] indices = value.stream().mapToInt(v -> possibleValues.indexOf(v)).toArray();
            list_rpValue.setSelectedIndices(indices);
            list_rpValue.addListSelectionListener(this);
        }
    }
    
    @Override
    public void showMultiValueMode(boolean b)
    {
        RpEditor.showMultiModeUsingFont(b, list_rpValue);
    }
    
    @Override
    public Set<String> getEditorValue()
    {
        HashSet<String> set = new HashSet<>();
        var selValues = list_rpValue.getSelectedValuesList();
        set.addAll(selValues);
        return set;
    }

    // -----------------------------------------------------------------------------
    // ListSelectionListener interface
    // -----------------------------------------------------------------------------
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            firePropertyChange(RpEditor.PROP_RP_VALUE, null, getEditorValue());
        }
    }
}
