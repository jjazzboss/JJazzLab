/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth.ui;

import java.awt.Component;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.jjazz.outputsynth.GM1RemapTable;
import org.jjazz.rhythm.api.Rhythm;

/**
 * A JTable to edit a GM1RemapTable.
 */
public class RemapTableUI extends JTable
{

    private static final Logger LOGGER = Logger.getLogger(RemapTableUI.class.getSimpleName());
    RemapTableUIModel tblModel = new RemapTableUIModel();

    /**
     * By default don't highlight favorite rhythms.
     */
    public RemapTableUI()
    {
        // setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        // Prevent column dragging
        getTableHeader().setReorderingAllowed(false);
        setModel(tblModel);
    }

    @Override
    public RemapTableUIModel getModel()
    {
        return tblModel;
    }

    public void setPrimaryModel(GM1RemapTable m)
    {
        tblModel.setPrimaryModel(m);
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================    
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
//            if (rhythms != null && row < rhythms.size() && col < COL_NAMES.length)
//            {
//                Rhythm r = rhythms.get(row);
//                String tooltip = null;
//                switch (col)
//                {
//                    case 0:
//                        tooltip = "Description=" + r.getDescription() + ", Author=" + r.getAuthor() + ", Version=" + r.getVersion();
//                        break;
//                    case 1:
//                        tooltip = r.getTempoRange().toString();
//                        break;
//                    case 2:
//                        tooltip = "Possible values=" + Arrays.asList(Rhythm.Feel.values());
//                        break;
//                    case 3:
//                        tooltip = Arrays.asList(r.getTags()).toString();
//                        break;
//                    case 4:
//                        tooltip = getFileValue(r.getFile());
//                        break;
//                    default:
//                    // Nothing
//                }
//                c.setToolTipText(tooltip);
//
//                // Different rendering if it's a favorite
//                FavoriteRhythms fr = FavoriteRhythms.getInstance();
//                if (highlightFavorite && fr.contains(r))
//                {
//                    Font f = c.getFont();
//                    Font newFont = f.deriveFont(Font.BOLD);
//                    c.setFont(newFont);
//                }
//            }
            return c;
        }
    }
}
