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
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_RhythmCombinatorValue;

/**
 * A JTable used to edit RhythmVoice mappings for a RP_RhythmCombinatorValue.
 */

public class RP_SYS_RhythmCombinatorValueTable extends JTable
{

    /**
     * Change event fired when user updated the destination RhythmVoice of a given destination rhythm.
     * <p>
     * oldValue=rvSrc, newValue=newRvDest
     */
    public static final String PROP_RVDEST = "PropRvDest";
    private final CustomTableModel tblModel = new CustomTableModel();
    private final CustomRvCellEditor rvCellEditor;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_RhythmCombinatorValueTable.class.getSimpleName());

    public RP_SYS_RhythmCombinatorValueTable()
    {
        setModel(tblModel);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging
        RhythmVoiceRenderer rvRenderer = new RhythmVoiceRenderer();
        getColumnModel().getColumn(CustomTableModel.COL_SRC_RHYTHM_VOICE).setCellRenderer(rvRenderer);
        RhythmRenderer rRenderer = new RhythmRenderer();
        getColumnModel().getColumn(CustomTableModel.COL_DEST_RHYTHM).setCellRenderer(rRenderer);
        getColumnModel().getColumn(CustomTableModel.COL_DEST_RHYTHM_VOICE).setCellRenderer(rvRenderer);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        rvCellEditor = new CustomRvCellEditor((rvSrc, rvDest) -> firePropertyChange(PROP_RVDEST, rvSrc, rvDest));
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column)
    {
        LOGGER.log(Level.SEVERE, "getCellEditor() -- row={0} column={1}", new Object[]
        {
            row, column
        });
        int modelColumn = convertColumnIndexToModel(column);
        int modelRow = convertRowIndexToModel(row);
        if (modelColumn == CustomTableModel.COL_DEST_RHYTHM_VOICE)
        {
            var rvSrc = tblModel.getSrcRhythmVoice(modelRow);
            var rvDest = rvSrc != null ? tblModel.getRpValue().getDestRhythmVoice(rvSrc) : null;
            assert rvDest != null;
            rvCellEditor.resetSilently(rvSrc, rvDest.getContainer(), rvDest);
            return rvCellEditor;
        } else
        {
            return super.getCellEditor(row, column);
        }
    }

    @Override
    public CustomTableModel getModel()
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
    public RhythmVoice getSelectedBaseRhythmVoice()
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
    public void setSelectedBaseRhythmVoice(RhythmVoice rv)
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


    static public class CustomTableModel extends AbstractTableModel
    {

        public static final int COL_SRC_RHYTHM_VOICE = 0;
        public static final int COL_DEST_RHYTHM = 1;
        public static final int COL_DEST_RHYTHM_VOICE = 2;
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

        public RhythmVoice getSrcRhythmVoice(int row)
        {
            RhythmVoice res = rpValue != null ? rpValue.getBaseRhythm().getRhythmVoices().get(row) : null;
            return res;
        }

        public RhythmVoice getDestRhythmVoice(int row)
        {
            var rvSrc = getSrcRhythmVoice(row);
            RhythmVoice res = rvSrc != null ? rpValue.getDestRhythmVoice(rvSrc) : null;
            return res;
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
                case COL_DEST_RHYTHM ->
                    Rhythm.class;
                case COL_SRC_RHYTHM_VOICE, COL_DEST_RHYTHM_VOICE ->
                    RhythmVoice.class;
                default -> throw new IllegalStateException("columnIndex=" + col);
            };
            return res;
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            boolean b = false;
            if (column == COL_DEST_RHYTHM_VOICE)
            {
                var rvDest = getDestRhythmVoice(row);
                b = rvDest != null;
            }
            return b;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s = switch (columnIndex)
            {
                case COL_SRC_RHYTHM_VOICE ->
                    "Current rhythm / track";
                case COL_DEST_RHYTHM ->
                    "Substitute rhythm";
                case COL_DEST_RHYTHM_VOICE ->
                    "Substitute track";
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
            return 3;
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
                case COL_DEST_RHYTHM ->
                {
                    var rvDest = rpValue.getDestRhythmVoice(rvSrc);
                    yield rvDest != null ? rvDest.getContainer() : null;
                }
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
    static private class RhythmVoiceRenderer extends DefaultTableCellRenderer
    {

        static public String toRvString(RhythmVoice rv, boolean showRhythm)
        {
            Rhythm r = rv.getContainer();
            MidiMix mm = MidiMixManager.getDefault().findMix(r);
            String insStr = "";
            if (mm != null && mm.getInstrumentMix(rv) != null)
            {
                insStr = " (" + mm.getInstrumentMix(rv).getInstrument().getPatchName() + ")";
            } else
            {
                LOGGER.log(Level.WARNING, "toRvString() Unexpected null values. mm={0} rv={1}", new Object[]
                {
                    mm, rv
                });
            }
            String res = (showRhythm ? r.getName() + " / " : "") + rv.getName() + insStr;
            return res;
        }


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
                label.setText(toRvString(rv, col == CustomTableModel.COL_SRC_RHYTHM_VOICE));
            }
            return label;
        }
    }


    static private class RhythmRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // Default JDK TableCellRenderer ignores the enabled/disabled state!            
            Color c = table.isEnabled() ? UIManager.getColor("Table.foreground") : UIManager.getColor("Label.disabledForeground");
            label.setForeground(c);

            Rhythm r = (Rhythm) value;
            if (r != null)
            {
                label.setText(r.getName());
            }

            return label;
        }
    }

    /**
     * A DefaultCellEditor to edit the destination RhythmVoice.
     */
    private class CustomRvCellEditor extends DefaultCellEditor
    {

        private final JComboBox<RhythmVoice> comboBox;
        private final DefaultComboBoxModel comboBoxModel;
        private RhythmVoice currentRvSrc;
        private boolean blockActionListener = false;

        /**
         *
         * @param userChangedValueListener param1=rvSrc, param2=rvDest
         */
        public CustomRvCellEditor(BiConsumer<RhythmVoice, RhythmVoice> userChangedValueListener)
        {
            super(new JComboBox()); // Dummy component for parent constructor

            comboBoxModel = new DefaultComboBoxModel();
            comboBox = new JComboBox(comboBoxModel);
            comboBox.setRenderer(new CmbRvRenderer());


            comboBox.addActionListener(ae -> 
            {
                // User selected a value
                if (!blockActionListener && currentRvSrc != null)
                {
                    RhythmVoice newRvDest = (RhythmVoice) comboBox.getSelectedItem();
                    assert newRvDest != null;
                    userChangedValueListener.accept(currentRvSrc, newRvDest);
                }
            });

        }

        /**
         * Silently configure the DefaultCellEditor for the specified parameters.
         * <p>
         *
         * @param rvSrc
         * @param r
         * @param rvDest
         */
        public void resetSilently(RhythmVoice rvSrc, Rhythm r, RhythmVoice rvDest)
        {
            currentRvSrc = rvSrc;
            blockActionListener = true;
            comboBoxModel.removeAllElements();
            comboBoxModel.addAll(r.getRhythmVoices());
            comboBoxModel.setSelectedItem(rvDest);
            blockActionListener = false;
        }

        public RhythmVoice getRvValue()
        {
            return (RhythmVoice) comboBox.getSelectedItem();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            return comboBox;
        }

        @Override
        public Object getCellEditorValue()
        {
            return getRvValue();
        }

        static private class CmbRvRenderer extends DefaultListCellRenderer
        {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);      // actually c=this !
                RhythmVoice rv = (RhythmVoice) value;
                if (rv != null)
                {
                    setText(RhythmVoiceRenderer.toRvString(rv, false));
                }
                return c;
            }
        }
    }


}
