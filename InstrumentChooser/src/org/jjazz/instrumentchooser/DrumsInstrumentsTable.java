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
package org.jjazz.instrumentchooser;

import java.awt.Component;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.DrumsInstrument;

/**
 * A non-editable JTable to show or select drums instruments.
 * <p>
 * Favorite rhythms are rendered differently.
 */
public class DrumsInstrumentsTable extends JTable
{

    private final String[] COL_NAMES = new String[]
    {
        "Midi Synth", "Bank", "Instrument", "Type", "Key Map"
    };
    private List<DrumsInstrument> model;
    private final int[] preferredWidth = new int[COL_NAMES.length];
    private static final Logger LOGGER = Logger.getLogger(DrumsInstrumentsTable.class.getSimpleName());

    public DrumsInstrumentsTable()
    {
        setDefaultEditor(Object.class, null);       // Prevent cell editing
        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        // Prevent column dragging
        getTableHeader().setReorderingAllowed(false);
        // To detect 1st display of the dialog
        preferredWidth[0] = -1;
    }

    /**
     * Clear all the table contents.
     */
    public void clear()
    {
        DefaultTableModel dtm = (DefaultTableModel) getModel();
        dtm.setRowCount(0);
        dtm.fireTableDataChanged();
        preferredWidth[0] = -1;     // To force reapplying column width setting next time populate() is called
    }

    /**
     * Populate the table with a list of rhythms.
     * <p>
     * Optimize columns width on first use, otherwise reuse previous columns width.<br>
     *
     * @param rhythms
     */
    public void populate(List<DrumsInstrument> instruments)
    {
        if (instruments == null)
        {
            throw new IllegalArgumentException("instruments=" + instruments);
        }
        LOGGER.log(Level.FINE, "populate() -- instruments={0}", instruments);
        model = instruments;
        Object[][] data = new Object[model.size()][COL_NAMES.length];
        for (int row = 0; row < model.size(); row++)
        {
            DrumsInstrument di = model.get(row);
            data[row][0] = di;
            data[row][1] = di;
            data[row][2] = di;
            data[row][3] = di;
            data[row][4] = di;
        }

        TableColumnModel tcm = getColumnModel();
        if (preferredWidth[0] != -1)
        {
            // Dialog was shown before, save preferred columns width
            LOGGER.fine("populate() NOT first, save column width");
            preferredWidth[0] = tcm.getColumn(0).getWidth();
            preferredWidth[1] = tcm.getColumn(1).getWidth();
            preferredWidth[2] = tcm.getColumn(2).getWidth();
            preferredWidth[3] = tcm.getColumn(3).getWidth();
            preferredWidth[4] = tcm.getColumn(4).getWidth();
        }

        // Update the table content
        DefaultTableModel tm = new DefaultTableModel(data, COL_NAMES)
        {
            @Override
            public Class getColumnClass(int column)
            {
                switch (column)
                {
                    case 0:
                        return String.class;
                    case 1:
                        return String.class;
                    case 2:
                        return String.class;
                    default:
                        return String.class;
                }
            }
        };
        setModel(tm);

        // Refresh the column model
        tcm = getColumnModel();

        // If it's the first time display set the column size as a percentage
        if (preferredWidth[0] == -1)
        {
            int w = getWidth();
            LOGGER.log(Level.FINE, "populate() FIRST, set percent column width w={0}", w);
            if (w == 0)
            {
                w = 450;          // HACK! in Options Panel w=0 I don't understand why... (though in JDialog it works OK)
            }
            for (int i = 0; i < COL_NAMES.length; i++)
            {
                preferredWidth[0] = (int) (w * 0.2f);     // Synth
                preferredWidth[1] = (int) (w * 0.2f);    // Bank
                preferredWidth[2] = (int) (w * 0.4f);    // DrumsInstrument
                preferredWidth[3] = (int) (w * 0.1f);     // DrumsKitType
                preferredWidth[4] = (int) (w * 0.1f);     // DrumKitKeyMap         
            }
        }

        // Set (or restore) columns width
        tcm.getColumn(0).setPreferredWidth(preferredWidth[0]);
        tcm.getColumn(1).setPreferredWidth(preferredWidth[1]);
        tcm.getColumn(2).setPreferredWidth(preferredWidth[2]);
        tcm.getColumn(3).setPreferredWidth(preferredWidth[3]);
        tcm.getColumn(4).setPreferredWidth(preferredWidth[4]);

        // Set cell renderers
        MyCellRenderer mcr = new MyCellRenderer();
        Enumeration<TableColumn> e = tcm.getColumns();
        while (e.hasMoreElements())
        {
            e.nextElement().setCellRenderer(mcr);
        }
    }

    /**
     * @return The Rhythm corresponding to the selected row, or null if no selection.
     */
    public DrumsInstrument getSelectedInstrument()
    {
        DrumsInstrument di = null;
        if (model != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                di = model.get(modelIndex);
            }
        }
        return di;
    }

    /**
     * Select the row corresponding to specified DrumsInstrument.
     *
     * @param di
     */
    public void setSelected(DrumsInstrument di)
    {
        int index = model.indexOf(di);
        if (index != -1)
        {
            int vIndex = convertRowIndexToView(index);      // Take into account sorting/filtering 
            if (vIndex != -1)
            {
                // Select if row is not filtered out
                setRowSelectionInterval(vIndex, vIndex);
            }
        }
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================    
    /**
     * Used to update cell's tooltip.
     */
    private class MyCellRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            DrumsInstrument di = (DrumsInstrument) value;
            if (di != null)
            {
                String tooltip = null;
                String text = null;
                switch (col)
                {
                    case 0:
                        text = di.getBank().getMidiSynth().getName();
                        // tooltip = "Description=" + di.getDescription() + ", Author=" + di.getAuthor() + ", Version=" + di.getVersion();
                        break;
                    case 1:
                        text = di.getBank().getName();
                        break;
                    case 2:
                        text = di.getPatchName();
                        tooltip = di.toString();
                        break;
                    case 3:
                        text = di.getDrumKitType().toString();
                        //
                        break;
                    case 4:
                        text = di.getDrumMap().toString();
                        // tooltip = getFileValue(di.getFile());
                        break;
                    default:
                    // Nothing
                }
                c.setText(text);
                c.setToolTipText(tooltip);
            }

            return c;
        }
    }
}
