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
package org.jjazz.midi.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiAddress;
import org.jjazz.midi.MidiAddress.BankSelectMethod;

/**
 * A JTable to show a list of instruments.
 */
public class InstrumentTable extends JTable
{

    Model tblModel = new Model();
    private static final Logger LOGGER = Logger.getLogger(InstrumentTable.class.getSimpleName());

    public InstrumentTable()
    {
        setModel(tblModel);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging

    }

    /**
     * Set the list of instruments shown in this table.
     *
     * @param instruments
     */
    public void setInstruments(List<Instrument> instruments)
    {
        tblModel.setInstruments(instruments);
        adjustWidths();
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

    private class Model extends AbstractTableModel
    {

        public static final int COL_ID = 0;
        public static final int COL_PATCHNAME = 1;
        public static final int COL_DRUMKIT = 2;
        public static final int COL_PC = 3;
        public static final int COL_MSB = 4;
        public static final int COL_LSB = 5;

        List<? extends Instrument> instruments = new ArrayList<>();

        public void setInstruments(List<Instrument> instruments)
        {
            if (instruments == null)
            {
                throw new NullPointerException("instruments");
            }
            this.instruments = instruments;
            fireTableDataChanged();
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
                    s = "LSB";
                    break;
                case COL_MSB:
                    s = "MSB";
                    break;
                case COL_PC:
                    s = "PC";
                    break;
                case COL_PATCHNAME:
                    s = "Patch Name";
                    break;
                case COL_DRUMKIT:
                    s = "Drum Kit";
                    break;
                case COL_ID:
                    s = "#";
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
            return 6;
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
                case COL_DRUMKIT:
                    DrumKit kit = ins.getDrumKit();
                    return (kit != null) ? kit.getType().toString() + "-" + kit.getKeyMap().getName() : "";
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
    public void adjustWidths()
    {
        final TableColumnModel colModel = getColumnModel();
        final int EXTRA = 5;
        for (int colIndex = 0; colIndex < getColumnCount(); colIndex++)
        {
            // Handle header
            TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(this, tblModel.getColumnName(colIndex), true, true, 0, colIndex);
            int headerWidth = comp.getPreferredSize().width + EXTRA;

            int width = 20; // Min width

            // Handle data
            for (int row = 0; row < getRowCount(); row++)
            {
                renderer = getCellRenderer(row, colIndex);
                comp = prepareRenderer(renderer, row, colIndex);
                width = Math.max(comp.getPreferredSize().width + EXTRA, width);
            }
            width = Math.max(width, headerWidth);
            width = Math.min(width, 400);

            // We have our preferred width
            colModel.getColumn(colIndex).setPreferredWidth(width);

            // Also set max size
            switch (colIndex)
            {
                case Model.COL_LSB:
                case Model.COL_MSB:
                case Model.COL_PC:
                case Model.COL_ID:
                case Model.COL_DRUMKIT:
                    colModel.getColumn(colIndex).setMaxWidth(width);
                    break;
                case Model.COL_PATCHNAME:
                    // Nothing
                    break;
                default:
                    throw new IllegalStateException("col=" + colIndex);
            }
        }
    }

    private class InsCellRenderer extends JLabel implements TableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
//            Instrument ins = instruments.
//            MidiAddress adr = ins.getMidiAddress();
//            MidiAddress.BankSelectMethod bsm = adr.getBankSelectMethod();
//            String text = null;
//            String tooltip = null;
//            switch (col)
//            {
//                case Model.COL_LSB:
//                    text = bsm.equals(MidiAddress.BankSelectMethod.MSB_ONLY) ? "x" : String.valueOf(adr.getBankLSB());
//                    tooltip = bsm.equals(MidiAddress.BankSelectMethod.MSB_ONLY) ? "Not used (bank select method=" + bsm.toString() + ")" : null;
//                    break;
//                case Model.COL_MSB:
//                    text = bsm.equals(MidiAddress.BankSelectMethod.LSB_ONLY) ? "x" : String.valueOf(adr.getBankMSB());
//                    tooltip = bsm.equals(MidiAddress.BankSelectMethod.LSB_ONLY) ? "Not used (bank select method=" + bsm.toString() + ")" : null;
//                    break;
//                case Model.COL_PC:
//                    text = String.valueOf(adr.getProgramChange());
//                    break;
//                case Model.COL_PATCHNAME:
//                    text = ins.getPatchName();
//                    tooltip = ins.getSubstitute() != null ? "GM substitute=" + ins.getSubstitute().getPatchName() : null;
//                    break;
//                case Model.COL_DRUMKIT:
//                    text = ins.getDrumKit() != null ? ins.getDrumKit().toString() : null;
//                    tooltip = "Optional info. for patches corresponding to drums/percussion kits.";
//                    break;
//                case Model.COL_ID:
//                default:
//                    throw new IllegalStateException("col=" + col);
//            }
//            setText(text);
//            setToolTipText(tooltip);
            return this;
        }
    }

}
