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
package org.jjazz.rpcustomeditorfactoryimpl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_RhythmCombinatorValue;

/**
 * A JTable used to edit RhythmVoice mappings for a RP_RhythmCombinatorValue.
 */

public class RP_SYS_RhythmCombinatorValueTable extends JTable
{

    private final RhythmCombinatorValueTableModel tblModel = new RhythmCombinatorValueTableModel();
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_RhythmCombinatorValueTable.class.getSimpleName());

    public RP_SYS_RhythmCombinatorValueTable()
    {
        setModel(tblModel);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging
        RhythmVoiceRenderer renderer = new RhythmVoiceRenderer();
        getColumnModel().getColumn(RhythmCombinatorValueTableModel.COL_SRC_RHYTHM_VOICE).setCellRenderer(renderer);
        getColumnModel().getColumn(RhythmCombinatorValueTableModel.COL_DEST_RHYTHM_VOICE).setCellRenderer(renderer);

    }

    @Override
    public RhythmCombinatorValueTableModel getModel()
    {
        return tblModel;
    }

    public RP_SYS_RhythmCombinatorValue getRpValue()
    {
        return tblModel.getRpValue();
    }

    /**
     * The selected base RhythmVoice.
     *
     * @return Can be null if no selection
     */
    public RhythmVoice getSelectedRhythmVoice()
    {
        RhythmVoice res = null;
        if (tblModel != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                res = tblModel.getRpValue().getBaseRhythm().getRhythmVoices().get(modelIndex);
            }
        }
        return res;
    }

    /**
     * Select the row corresponding to the specified base RhythmVoice.
     * <p>
     *
     * @param rv
     */
    public void setSelectedRhythmVoice(RhythmVoice rv)
    {
        var rpValue = tblModel.getRpValue();
        if (rpValue == null)
        {
            return;
        }
        int mIndex = rpValue.getBaseRhythm().getRhythmVoices().indexOf(rv);
        if (mIndex != -1)
        {
            int vIndex = convertRowIndexToView(mIndex);      // Take into account sorting/filtering 
            if (vIndex != -1)
            {
                // Select if row is not filtered out
                setRowSelectionInterval(vIndex, vIndex);
                // Scroll to make it visible
                Rectangle cellRect = getCellRect(vIndex, 1, true);
                scrollRectToVisible(cellRect);
            }
        }
    }


    /**
     * Our model.
     */
    static public class RhythmCombinatorValueTableModel extends AbstractTableModel
    {

        public static final int COL_SRC_RHYTHM_VOICE = 0;
        public static final int COL_DEST_RHYTHM_VOICE = 1;
        private RP_SYS_RhythmCombinatorValue rpValue;

        /**
         *
         * @param rpValue Can be null
         */
        public void setRpValue(RP_SYS_RhythmCombinatorValue rpValue)
        {
            this.rpValue = rpValue;
            fireTableDataChanged();
        }

        public RP_SYS_RhythmCombinatorValue getRpValue()
        {
            return rpValue;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            Class<?> res = switch (col)
            {
                case COL_SRC_RHYTHM_VOICE, COL_DEST_RHYTHM_VOICE ->
                    RhythmVoice.class;
                default -> throw new IllegalStateException("columnIndex=" + col);
            };
            return res;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s = switch (columnIndex)
            {
                case COL_SRC_RHYTHM_VOICE ->
                    "Track";
                case COL_DEST_RHYTHM_VOICE ->
                    "Replacement";
                default -> throw new IllegalStateException("columnIndex=" + columnIndex);
            };
            return s;
        }

        @Override
        public int getRowCount()
        {
            return rpValue != null ? rpValue.getBaseRhythm().getRhythmVoices().size() : 8;
        }

        @Override
        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            if (rpValue == null)
            {
                return null;
            }
            RhythmVoice rvSrc = rpValue.getBaseRhythm().getRhythmVoices().get(row);
            Object res = switch (col)
            {
                case COL_SRC_RHYTHM_VOICE ->
                    rvSrc;
                case COL_DEST_RHYTHM_VOICE ->
                    rpValue.getDestRhythmVoice(rvSrc);
                default -> throw new IllegalStateException("col=" + col);
            };

            return res;
        }
    }


    // ============================================================================
    // Private methods
    // ============================================================================
    private class RhythmVoiceRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // Default JDK TableCellRenderer ignores the enabled/disabled state!            
            Color c = table.isEnabled() ? UIManager.getColor("Table.foreground") : UIManager.getColor("Label.disabledForeground");
            label.setForeground(c);

            RhythmVoice rv = (RhythmVoice) value;
            if (rv != null)
            {
                String text;
                if (col == RhythmCombinatorValueTableModel.COL_DEST_RHYTHM_VOICE)
                {
                    text = rv.getContainer().getName() + " > " + rv.getName();
                } else
                {
                    text = rv.getName();
                }
                label.setText(text);
            }

            return label;
        }
    }


}
