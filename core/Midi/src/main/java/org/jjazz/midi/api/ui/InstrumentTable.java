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
package org.jjazz.midi.api.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.synths.GM1Instrument;

/**
 * A JTable to show a list of instruments.
 */
public class InstrumentTable extends JTable
{

    private final Model tblModel = new Model();
    private List<Integer> hiddenColumnIndexes = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(InstrumentTable.class.getSimpleName());

    /**
     * Create an InstrumentTable.
     * <p>
     */
    public InstrumentTable()
    {
        setModel(tblModel);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging
        PatchNameRenderer renderer = new PatchNameRenderer();
        for (var col : Collections.list(getColumnModel().getColumns()))
        {
            col.setCellRenderer(renderer);
        }
        getTableHeader().setDefaultRenderer(new HeaderRenderer(this));
    }

    /**
     *
     * @param hiddenColumns The indexes of the columns to hide. Can be null.
     */
    public void setHiddenColumns(List<Integer> hiddenColumns)
    {
        hiddenColumnIndexes = (hiddenColumns == null) ? new ArrayList<Integer>() : hiddenColumns;
        adjustWidths();
    }

    @Override
    public Model getModel()
    {
        return tblModel;
    }

    /**
     * The selected instrument.
     *
     * @return Can be null if no selection
     */
    public Instrument getSelectedInstrument()
    {
        Instrument ins = null;
        if (tblModel != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                ins = tblModel.getInstruments().get(modelIndex);
            }
        }
        return ins;
    }

    /**
     * Select the row corresponding to the specified instrument.
     * <p>
     * The method also scroll the table to make the selected instrument visible.
     *
     * @param ins
     */
    public void setSelectedInstrument(Instrument ins)
    {
        int mIndex = tblModel.getInstruments().indexOf(ins);
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

    public class Model extends AbstractTableModel
    {

        public static final int COL_ID = 0;
        public static final int COL_PATCHNAME = 3;
        public static final int COL_DRUMKIT = 4;
        public static final int COL_PC = 5;
        public static final int COL_MSB = 6;
        public static final int COL_LSB = 7;
        public static final int COL_SYNTH = 1;
        public static final int COL_BANK = 2;
        private List<? extends Instrument> instruments = new ArrayList<>();

        public void setInstruments(List<? extends Instrument> instruments)
        {
            if (instruments == null)
            {
                throw new NullPointerException("instruments");   
            }
            this.instruments = instruments;
            fireTableDataChanged();
            adjustWidths();
        }

        List<? extends Instrument> getInstruments()
        {
            return instruments;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            switch (col)
            {
                case COL_LSB:
                case COL_MSB:
                case COL_PC:
                case COL_ID:
                    return Integer.class;
                case COL_PATCHNAME:
                case COL_DRUMKIT:
                case COL_SYNTH:
                case COL_BANK:
                    return String.class;
                default:
                    throw new IllegalStateException("columnIndex=" + col);   
            }
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s;
            switch (columnIndex)
            {
                case COL_LSB:
                    s = "LSB ";
                    break;
                case COL_MSB:
                    s = "MSB ";
                    break;
                case COL_PC:
                    s = "PC  ";
                    break;
                case COL_PATCHNAME:
                    s = "Patch Name";
                    break;
                case COL_DRUMKIT:
                    s = "Drum Kit";
                    break;
                case COL_ID:
                    s = "#   ";
                    break;
                case COL_SYNTH:
                    s = "Synth ";
                    break;
                case COL_BANK:
                    s = "Bank ";
                    break;
                default:
                    throw new IllegalStateException("columnIndex=" + columnIndex);   
            }
            return s;
        }

        @Override
        public int getRowCount()
        {
            return instruments.size();
        }

        @Override
        public int getColumnCount()
        {
            return 8;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            Instrument ins = instruments.get(row);
            MidiAddress adr = ins.getMidiAddress();
            BankSelectMethod bsm = adr.getBankSelectMethod();
            switch (col)
            {
                case COL_LSB:
                    return bsm.equals(BankSelectMethod.MSB_ONLY) || bsm.equals(BankSelectMethod.PC_ONLY) ? null : adr.getBankLSB();
                case COL_MSB:
                    return bsm.equals(BankSelectMethod.LSB_ONLY) || bsm.equals(BankSelectMethod.PC_ONLY) ? null : adr.getBankMSB();
                case COL_PC:
                    return adr.getProgramChange();
                case COL_PATCHNAME:
                    return ins.getPatchName();
                case COL_BANK:
                    return ins.getBank() != null ? ins.getBank().getName() : null;
                case COL_SYNTH:
                    return (ins.getBank() != null && ins.getBank().getMidiSynth() != null) ? ins.getBank().getMidiSynth().getName() : null;
                case COL_DRUMKIT:
                    DrumKit kit = ins.getDrumKit();
                    return (kit != null) ? kit.getType().toString() + "-" + kit.getKeyMap().getName() : null;
                case COL_ID:
                    return row + 1;
                default:
                    throw new IllegalStateException("col=" + col);   
            }
        }
    }

    // ============================================================================
    // Private methods
    // ============================================================================
    /**
     * Pre-adjust the columns size parameters to have a correct display.
     */
    private void adjustWidths()
    {
        final TableColumnModel colModel = getColumnModel();
        final int EXTRA = 5;
        for (int colIndex = 0; colIndex < getColumnCount(); colIndex++)
        {
            if (hiddenColumnIndexes.contains(colIndex))
            {
                colModel.getColumn(colIndex).setMinWidth(0);
                colModel.getColumn(colIndex).setMaxWidth(0);
                colModel.getColumn(colIndex).setPreferredWidth(0);
                continue;
            }
            // Handle header
            TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(this, tblModel.getColumnName(colIndex), true, true, 0, colIndex);
            int headerWidth = comp.getPreferredSize().width;

            int width = 20; // Min width

            // Handle data
            for (int row = 0; row < getRowCount(); row++)
            {
                renderer = getCellRenderer(row, colIndex);
                comp = prepareRenderer(renderer, row, colIndex);
                width = Math.max(comp.getPreferredSize().width, width);
            }
            width = Math.max(width, headerWidth);
            width = Math.min(width, 400);
            width += EXTRA;

            // We have our preferred width
            colModel.getColumn(colIndex).setPreferredWidth(width);

            // Also set max size
            switch (colIndex)
            {
                case Model.COL_LSB:
                case Model.COL_MSB:
                case Model.COL_PC:
                case Model.COL_ID:
                    colModel.getColumn(colIndex).setMaxWidth(width);
                    break;
                case Model.COL_DRUMKIT:
                case Model.COL_SYNTH:
                case Model.COL_BANK:
                    // colModel.getColumn(colIndex).setMaxWidth(width);
                    break;
                case Model.COL_PATCHNAME:
                    // Nothing
                    break;
                default:
                    throw new IllegalStateException("col=" + colIndex);   
            }
        }
    }

    private class PatchNameRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // Default JDK TableCellRenderer ignores the enabled/disabled state!            
            Color c = table.isEnabled() ? UIManager.getColor("Table.foreground") : UIManager.getColor("Label.disabledForeground");
            label.setForeground(c);


            // Same component is reused for several cells : need to reset some default settings
            label.setToolTipText(null);
            if (value == null)
            {
                return label;
            }
            Instrument ins = tblModel.getInstruments().get(table.convertRowIndexToModel(row));
            String tt = buildToolTipText(ins);
            label.setToolTipText(tt);


            return label;
        }


        private String buildToolTipText(Instrument ins)
        {
            StringBuilder sb = new StringBuilder();
            if (ins.getSubstitute() != null && !(ins instanceof GM1Instrument))
            {
                GM1Instrument gm1Ins = ins.getSubstitute();
                sb.append("GM Substitute=").append(gm1Ins.getPatchName());
                sb.append(", Family=").append(gm1Ins.getFamily().toString());
            } else if (ins.isDrumKit())
            {
                sb.append("DrumKit=").append(ins.getDrumKit().toString());
            }
            if (sb.length() != 0)
            {
                sb.append(", ");
            }
            sb.append(ins.getMidiAddress().toString());

            return sb.length() == 0 ? null : sb.toString();
        }
    }

    /**
     * Needed because default renderer also ignores the enabled/disabled state (like the default TableCellRenderer).
     */
    private static class HeaderRenderer implements TableCellRenderer
    {

        DefaultTableCellRenderer renderer;

        public HeaderRenderer(JTable table)
        {
            renderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel label = (JLabel) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            Color c = table.isEnabled() ? UIManager.getColor("Table.foreground") : UIManager.getColor("Label.disabledForeground");
            label.setForeground(c);
            return label;
        }
    }

}
