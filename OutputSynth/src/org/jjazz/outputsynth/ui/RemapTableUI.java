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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.GM1Instrument;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.StdSynth;
import org.jjazz.outputsynth.GMRemapTable;

/**
 * A JTable to edit a GM1RemapTable.
 */
public class RemapTableUI extends JTable
{

    public static final Instrument DRUMS_INSTRUMENT = new Instrument(0, "Drums");
    public static final Instrument PERCUSSION_INSTRUMENT = new Instrument(0, "Percussion");
    private static final Logger LOGGER = Logger.getLogger(RemapTableUI.class.getSimpleName());
    Model tblModel = new Model();

    /**
     * By default don't highlight favorite rhythms.
     */
    public RemapTableUI()
    {
        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        // Prevent column dragging
        getTableHeader().setReorderingAllowed(false);
        setModel(tblModel);
        MyCellRenderer renderer = new MyCellRenderer();
        setDefaultRenderer(String.class, renderer);
        setDefaultRenderer(Instrument.class, renderer);
        JTableHeader header = getTableHeader();
        // header.setBackground(new Color(236, 236, 255));
        // header.setOpaque(false);    // Required for setBackground() to work !!!!???
    }

    @Override
    public Model getModel()
    {
        return tblModel;
    }

    public void setPrimaryModel(GMRemapTable m)
    {
        tblModel.setPrimaryModel(m);
        adjustWidths();
    }

    /**
     * Overridden to manage the display of cell borders.
     *
     * @param renderer
     * @param row
     * @param column
     * @return
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
    {
        JComponent c = (JComponent) super.prepareRenderer(renderer, row, column);
        if (row==0 || (row - Model.ROW_GMVOICE_START) % 8 == 0)
        {
            // It's a new family, border on the top and 
            c.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        }
        return c;
    }

    /**
     * Get the selected reference Instrument.
     * <p>
     * Get a GM1Instrument or the special DRUMS_INS or PERCUSSION_INS instances.
     *
     * @return Can be null if no selection
     */
    public Instrument getSelectedInstrument()
    {
        Instrument ins = null;
        int rowIndex = getSelectedRow();
        if (rowIndex != -1)
        {
            int pc = convertRowIndexToModel(rowIndex) - Model.ROW_GMVOICE_START;
            if (pc == -2)
            {
                ins = DRUMS_INSTRUMENT;
            } else if (pc == -1)
            {
                ins = PERCUSSION_INSTRUMENT;
            } else
            {
                ins = StdSynth.getGM1Bank().getInstrument(pc);
            }
        }
        return ins;
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================    
    private GM1Bank.Family getFamily(int row)
    {
        if (row < Model.ROW_GMVOICE_START)
        {
            return null;
        }
        return GM1Bank.Family.values()[(row - Model.ROW_GMVOICE_START) / 8];
    }

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
            Component comp = renderer.getTableCellRendererComponent(this, tblModel.getColumnName(colIndex), false, false, 0, colIndex);
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
                case Model.COL_ID:
                case Model.COL_FAM:
                case Model.COL_INS:
                    colModel.getColumn(colIndex).setMaxWidth(width);
                    break;
                case Model.COL_INS_MAP:
                    // Nothing
                    break;
                default:
                    throw new IllegalStateException("col=" + colIndex);

            }
        }
    }

    // ============================================================================================
    // Private classes
    // ============================================================================================    
    public class Model extends AbstractTableModel implements PropertyChangeListener
    {

        public static final int ROW_DRUMS = 0;
        public static final int ROW_PERC = 1;
        public static final int ROW_GMVOICE_START = 2;
        public static final int COL_ID = 0;
        public static final int COL_FAM = 1;
        public static final int COL_INS = 2;
        public static final int COL_INS_MAP = 3;

        private GMRemapTable remapTable = null;

        public void setPrimaryModel(GMRemapTable model)
        {
            if (model == null)
            {
                throw new NullPointerException("model");
            }
            if (remapTable != null)
            {
                remapTable.removePropertyChangeListener(this);
            }
            this.remapTable = model;
            this.remapTable.addPropertyChangeListener(this);
            this.fireTableDataChanged();
        }

        @Override
        public int getRowCount()
        {
            return 128 + ROW_GMVOICE_START; // 128 + Drums + Percussion

        }

        @Override
        public int getColumnCount()
        {
            return 4;
        }

        @Override
        public String getColumnName(int col)
        {
            String res;
            switch (col)
            {
                case COL_FAM:
                    res = "GM Family";
                    break;
                case COL_INS:
                    res = "GM Instrument";
                    break;
                case COL_INS_MAP:
                    res = "Remapped Instrument (bold=also default ins. for the whole family)";
                    break;
                case COL_ID:
                    res = "#";
                    break;
                default:
                    throw new IllegalArgumentException("col=" + col);
            }
            return res;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            Class<?> res = Object.class;
            switch (col)
            {
                case COL_FAM:
                    res = String.class;
                    break;
                case COL_INS:
                    res = String.class;
                    break;
                case COL_INS_MAP:
                    res = Instrument.class;
                    break;
                case COL_ID:
                    res = Integer.class;
                    break;
                default:
                    throw new IllegalArgumentException("col=" + col);
            }
            return res;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            Object res = null;
            if (remapTable == null)
            {
                return null;
            }
            int pc = row - ROW_GMVOICE_START;
            if (col == COL_ID)
            {
                return pc < 0 ? null : pc + 1;
            }
            switch (row)
            {
                case ROW_DRUMS:
                    switch (col)
                    {
                        case COL_FAM:
                            res = "-";
                            break;
                        case COL_INS:
                            res = "Drums";
                            break;
                        case COL_INS_MAP:
                            res = remapTable.getDrumsInstrument();
                            break;
                        default:
                            throw new IllegalArgumentException("row=" + row + " col=" + col);
                    }
                    break;
                case ROW_PERC:
                    switch (col)
                    {
                        case COL_FAM:
                            res = "-";
                            break;
                        case COL_INS:
                            res = "Percussion";
                            break;
                        case COL_INS_MAP:
                            res = remapTable.getPercussionInstrument();
                            break;
                        default:
                            throw new IllegalArgumentException("row=" + row + " col=" + col);
                    }
                    break;
                default:
                    // GM1 voices
                    switch (col)
                    {
                        case COL_FAM:
                            res = getFamily(row).toString();
                            break;
                        case COL_INS:
                            res = StdSynth.getGM1Bank().getInstrument(pc).getPatchName();
                            break;
                        case COL_INS_MAP:
                            GM1Instrument insGM1 = StdSynth.getGM1Bank().getInstrument(pc);
                            res = remapTable.getInstrument(insGM1);
                            break;
                        default:
                            throw new IllegalArgumentException("row=" + row + " col=" + col);
                    }
            }
            // LOGGER.fine("getValueAt() row=" + row + " col" + col + " res=" + res);

            return res;
        }

        // =================================================================================
        // PropertyChangeListener methods
        // =================================================================================    
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            switch (e.getPropertyName())
            {
                case "PROP_FAMILY":
                    GM1Bank.Family f = (GM1Bank.Family) e.getOldValue();
                    int row = f.getFirstProgramChange() + Model.ROW_GMVOICE_START;
                    fireTableRowsUpdated(row, row + 7);
                    break;
                case "PROP_INSTRUMENT":
                    GM1Instrument insGM1 = (GM1Instrument) e.getOldValue();
                    fireTableCellUpdated(insGM1.getMidiAddress().getProgramChange() + ROW_GMVOICE_START, COL_INS_MAP);
                    break;
                case "PROP_DRUMS_INSTRUMENT":
                    fireTableCellUpdated(ROW_DRUMS, COL_INS_MAP);
                    break;
                case "PROP_PERC_INSTRUMENT":
                    fireTableCellUpdated(ROW_PERC, COL_INS_MAP);
                    break;
                default:
                    throw new IllegalArgumentException("e=" + e);
            }
        }
    }

    private class MyCellRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            // Same component is reused for several cells : need to reset some default settings
            c.setToolTipText(null);
            if (value == null)
            {
                return c;
            }
            int pc = row - Model.ROW_GMVOICE_START;
            if (col == Model.COL_INS_MAP)
            {
                Instrument ins = (Instrument) value;
                if (pc >= 0 && ins == tblModel.remapTable.getInstrument(StdSynth.getGM1Bank().getInstrument(pc).getFamily()))
                {
                    // Special display if Instrument is also the default instrument for the family                
                    c.setFont(getFont().deriveFont(Font.BOLD));
                }
                String text = getInstrumentText(ins);
                String tt = ins.isDrumKit() ? ins.getDrumKit().toString() + " " + ins.getMidiAddress().toString() : ins.getMidiAddress().toString();
                c.setText(text);
                c.setToolTipText(tt);
            } else if (col == Model.COL_FAM && pc >= 0)
            {
                // Show only the first row
                if (pc % 8 != 0)
                {
                    c.setText(null);
                } else
                {
                    c.setFont(getFont().deriveFont(Font.BOLD));
                }
            }
            return c;
        }

        private String getInstrumentText(Instrument ins)
        {
            StringBuilder sb = new StringBuilder(ins.getPatchName());
            sb.append("    ");
            if (ins.getBank() != null)
            {
                sb.append("(");
                if (ins.getBank().getMidiSynth() != null)
                {
                    sb.append("synth=").append(ins.getBank().getMidiSynth().getName()).append(", ");
                }
                sb.append("bank=").append(ins.getBank().getName());
                sb.append(")");
            }
            return sb.toString();
        }
    }
}
