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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.GM1Instrument;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.StdSynth;
import org.jjazz.outputsynth.GMRemapTable;
import org.jjazz.rhythm.api.Rhythm;

/**
 * A JTable to edit a GM1RemapTable.
 */
public class RemapTableUI extends JTable
{

    private static final Logger LOGGER = Logger.getLogger(RemapTableUI.class.getSimpleName());
    Model tblModel = new Model();

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
        setShowGrid(false);
        this.setRowMargin(0);
    }

    @Override
    public Model getModel()
    {
        return tblModel;
    }

    public void setPrimaryModel(GMRemapTable m)
    {
        tblModel.setPrimaryModel(m);
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
        if (column == Model.COL_INS || column == Model.COL_INS_MAP)
        {
            c.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        } else if ((row - Model.ROW_GMVOICE_START) % 8 == 0)
        {
            // It's a new family, border on the top and 
            c.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, Color.LIGHT_GRAY));
        } else
        {
            c.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.LIGHT_GRAY));
        }              
        return c;
    }

    public class Model extends AbstractTableModel implements PropertyChangeListener
    {

        public static final int ROW_DRUMS = 0;
        public static final int ROW_PERC = 1;
        public static final int ROW_GMVOICE_START = 2;
        public static final int COL_INS = 0;
        public static final int COL_INS_MAP = 1;
        public static final int COL_FAM = 2;
        public static final int COL_FAM_MAP = 3;

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
                    res = "Remapped Instrument";
                    break;
                case COL_FAM_MAP:
                    res = "Remapped Instrument";
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
                    res = GM1Bank.Family.class;
                    break;
                case COL_INS:
                    res = Instrument.class;
                    break;
                case COL_INS_MAP:
                    res = Instrument.class;
                    break;
                case COL_FAM_MAP:
                    res = Instrument.class;
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
            switch (row)
            {
                case ROW_DRUMS:
                    switch (col)
                    {
                        case COL_FAM:
                            res = null;
                            break;
                        case COL_INS:
                            res = new Instrument(0, "Drums");
                            break;
                        case COL_INS_MAP:
                            res = remapTable.getDrumsInstrument();
                            break;
                        case COL_FAM_MAP:
                            res = null;
                            break;
                        default:
                            throw new IllegalArgumentException("row=" + row + " col=" + col);
                    }
                    break;
                case ROW_PERC:
                    switch (col)
                    {
                        case COL_FAM:
                            res = null;
                            break;
                        case COL_INS:
                            res = new Instrument(0, "Percussion");
                            break;
                        case COL_INS_MAP:
                            res = remapTable.getPercussionInstrument();
                            break;
                        case COL_FAM_MAP:
                            res = null;
                            break;
                        default:
                            throw new IllegalArgumentException("row=" + row + " col=" + col);
                    }
                    break;
                default:
                    // GM1 voices
                    GM1Bank.Family f = getFamily(row);
                    int pc = row - ROW_GMVOICE_START;
                    boolean familyRow = (pc % 8) == 0;
                    switch (col)
                    {
                        case COL_FAM:
                            res = familyRow ? f : null;
                            break;
                        case COL_FAM_MAP:
                            res = familyRow ? remapTable.getInstrument(f) : null;
                            break;
                        case COL_INS:
                            res = StdSynth.getGM1Bank().getInstrument(pc);
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
                    fireTableCellUpdated(row, COL_FAM_MAP);
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

        // =============================================================================
        // Private methods
        // =============================================================================       
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
