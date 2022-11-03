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
package org.jjazz.options;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.outputsynth.api.OutputSynthManager;

/**
 * A JTable to show the list of OUT MidiDevice and corresponding OutputSynth.
 */
public class MidiDeviceSynthTable extends JTable
{

    private Model tblModel = new Model();
    private static final Logger LOGGER = Logger.getLogger(MidiDeviceSynthTable.class.getSimpleName());


    public MidiDeviceSynthTable()
    {
        setModel(tblModel);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging
        MidiDeviceTcRenderer mdRenderer = new MidiDeviceTcRenderer();
        getColumnModel().getColumn(Model.COL_MIDI_DEVICE).setCellRenderer(mdRenderer);
    }

    /**
     * The selected MidiDevice.
     *
     * @return Can be null if no selection
     */
    public MidiDevice getSelectedMidiDevice()
    {
        MidiDevice md = null;
        if (tblModel != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                md = tblModel.getMidiDevices().get(modelIndex);
            }
        }
        return md;
    }

    /**
     * Select the row corresponding to the specified MidiDevice.
     * <p>
     * The method also scroll the table to make the selected MidiDevice visible.
     *
     * @param md
     */
    public void setSelectedMidiDevice(MidiDevice md)
    {
        int mIndex = tblModel.getMidiDevices().indexOf(md);
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


    // ============================================================================
    // Private methods
    // ============================================================================
    // ============================================================================
    // Private classes
    // ============================================================================
    /**
     * TableCell renderer for MidiDevices.
     */
    private class MidiDeviceTcRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            MidiDevice md = (MidiDevice) value;
            String txt = JJazzMidiSystem.getInstance().getDeviceFriendlyName(md);
            label.setText(txt);
            label.setToolTipText(md.getDeviceInfo().getDescription());
            return label;
        }
    }

    private class Model extends AbstractTableModel
    {

        public static final int COL_MIDI_DEVICE = 0;
        public static final int COL_OUTPUT_SYNTH = 1;
        private List<MidiDevice> midiDevices = new ArrayList<>();

        public void setMidiDevices(List<MidiDevice> mdList)
        {
            Preconditions.checkNotNull(mdList);
            this.midiDevices.clear();
            this.midiDevices.addAll(mdList);
            fireTableDataChanged();
        }

        List<MidiDevice> getMidiDevices()
        {
            return midiDevices;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            switch (col)
            {
                case COL_MIDI_DEVICE:
                    return String.class;
                case COL_OUTPUT_SYNTH:
                    return String.class;
                default:
                    throw new IllegalStateException("columnIndex=" + col);   //NOI18N
            }
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s;
            switch (columnIndex)
            {
                case COL_OUTPUT_SYNTH:
                    s = "Output Synth";
                    break;
                case COL_MIDI_DEVICE:
                    s = "Midi Device";
                    break;
                default:
                    throw new IllegalStateException("columnIndex=" + columnIndex);   //NOI18N
            }
            return s;
        }

        @Override
        public int getRowCount()
        {
            return midiDevices.size();
        }

        @Override
        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            MidiDevice md = midiDevices.get(row);
            switch (col)
            {
                case COL_OUTPUT_SYNTH:
                    return OutputSynthManager.getInstance().getOutputSynth(md.getDeviceInfo().getName());
                case COL_MIDI_DEVICE:
                    return md;
                default:
                    throw new IllegalStateException("col=" + col);   //NOI18N
            }
        }
    }


}
