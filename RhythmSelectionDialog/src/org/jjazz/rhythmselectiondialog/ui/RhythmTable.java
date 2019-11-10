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
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
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
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.FavoriteRhythms;

/**
 * A non-editable JTable to show or select Rhythms.
 * <p>
 * Favorite rhythms are rendered differently.
 */
public class RhythmTable extends JTable implements PropertyChangeListener
{

    private final String[] COL_NAMES = new String[]
    {
        "Name", "Tempo", "Feel", "Tags", "File"
    };
    private final int[] preferredWidth = new int[COL_NAMES.length];
    private List<Rhythm> model;
    private boolean highlightFavorite = false;
    private static final Logger LOGGER = Logger.getLogger(RhythmTable.class.getSimpleName());

    /**
     * By default don't highlight favorite rhythms.
     */
    public RhythmTable()
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
        FavoriteRhythms fr = FavoriteRhythms.getInstance();
        fr.addPropertyListener(this);
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
     * If b is true highlight the display of the favorite rhythms.
     *
     * @param b
     */
    public void setHighlightFavorite(boolean b)
    {
        highlightFavorite = b;
        repaint();
    }

    public boolean isHighlightFavorite()
    {
        return highlightFavorite;
    }

    /**
     * Populate the table with a list of rhythms.
     * <p>
     * Optimize columns width on first use, otherwise reuse previous columns width.<br>
     *
     * @param rhythms
     */
    public void populate(List<Rhythm> rhythms)
    {
        if (rhythms == null)
        {
            throw new IllegalArgumentException("rhythms=" + rhythms);
        }
        LOGGER.log(Level.FINE, "populate() -- rhythms={0}", rhythms);
        model = rhythms;
        Object[][] data = new Object[model.size()][COL_NAMES.length];
        for (int row = 0; row < model.size(); row++)
        {
            Rhythm r = model.get(row);
            data[row][0] = r.getName();
            data[row][1] = Integer.valueOf(r.getPreferredTempo());
            data[row][2] = r.getFeel().toString();
            data[row][3] = Arrays.toString(r.getTags());
            data[row][4] = getFileValue(r.getFile());
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
                        return Integer.class;
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
                preferredWidth[0] = (int) (w * 0.4f);     // Name
                preferredWidth[1] = (int) (w * 0.075f);    // Tempo
                preferredWidth[2] = (int) (w * 0.075f);    // Feel
                preferredWidth[3] = (int) (w * 0.1f);     // Tags
                preferredWidth[4] = (int) (w * 0.35f);     // File                
            }
        }

        // Set (or restore) columns width
        tcm.getColumn(0).setPreferredWidth(preferredWidth[0]);
        tcm.getColumn(1).setPreferredWidth(preferredWidth[1]);
        tcm.getColumn(2).setPreferredWidth(preferredWidth[2]);
        tcm.getColumn(3).setPreferredWidth(preferredWidth[3]);
        tcm.getColumn(4).setPreferredWidth(preferredWidth[4]);

        // Set cell renderers
        MyCellRenderer mcr = new MyCellRenderer(model);
        Enumeration<TableColumn> e = tcm.getColumns();
        while (e.hasMoreElements())
        {
            e.nextElement().setCellRenderer(mcr);
        }
    }

    /**
     * @return The Rhythm corresponding to the selected row, or null if no selection.
     */
    public Rhythm getSelectedRhythm()
    {
        Rhythm r = null;
        if (model != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                r = model.get(modelIndex);
            }
        }
        return r;
    }

    /**
     * Select the row corresponding to specified rhythm.
     *
     * @param r
     */
    public void setSelected(Rhythm r)
    {
        int index = model.indexOf(r);
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

    // =================================================================================
    // PropertyChangeListener methods
    // =================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        FavoriteRhythms fr = FavoriteRhythms.getInstance();
        if (e.getSource() == fr && e.getPropertyName().equals(FavoriteRhythms.PROP_FAVORITE_RHYTHM))
        {
            // A favorite rhythm was removed or added
            // Repaint the corresponding cell
            Rhythm r = (Rhythm) (e.getNewValue() == null ? e.getOldValue() : e.getNewValue());
            int row = model.indexOf(r);
            if (row != -1)
            {
                int vRow = convertRowIndexToView(row);
                repaint(getCellRect(vRow, 0, false));      // Our cell renderer takes care about the favorite status
            }
        }
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================    
    private String getFileValue(File f)
    {
        return "".equals(f.getPath()) ? "buillt-in" : f.getAbsolutePath();
    }

    /**
     * <p>
     * Used to update cell's tooltip.
     */
    private class MyCellRenderer extends DefaultTableCellRenderer
    {

        private final List<Rhythm> rhythms;

        public MyCellRenderer(List<Rhythm> rhythms)
        {
            this.rhythms = rhythms;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (rhythms != null && row < rhythms.size() && col < COL_NAMES.length)
            {
                Rhythm r = rhythms.get(row);
                String tooltip = null;
                switch (col)
                {
                    case 0:
                        tooltip = "Description=" + r.getDescription() + ", Author=" + r.getAuthor() + ", Version=" + r.getVersion();
                        break;
                    case 1:
                        tooltip = r.getTempoRange().toString();
                        break;
                    case 2:
                        tooltip = "Possible values=" + Arrays.asList(Rhythm.Feel.values());
                        break;
                    case 3:
                        tooltip = Arrays.asList(r.getTags()).toString();
                        break;
                    case 4:
                        tooltip = getFileValue(r.getFile());
                        break;
                    default:
                    // Nothing
                }
                c.setToolTipText(tooltip);

                // Different rendering if it's a favorite
                FavoriteRhythms fr = FavoriteRhythms.getInstance();
                if (highlightFavorite && fr.contains(r))
                {
                    Font f = c.getFont();
                    Font newFont = f.deriveFont(Font.BOLD);
                    c.setFont(newFont);
                }
            }
            return c;
        }
    }
}
