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
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracksValue;
import org.jjazz.utilities.api.ResUtil;

/**
 * A JTable used to edit the RhythmVoice mappings of a RP_SYS_OverrideTracksValue.
 */
public class RP_SYS_OverrideTracksValueTable extends JTable
{

    /**
     * Change event fired when user changed a dest. rhythm voice
     * <p>
     * oldValue=rvSrc, newValue=newRvDest.
     * <p>
     * NOTE: as an exception, if newValue=null, it means newRvDest=rvSrc (trick required because if both oldValue and newValue are equals(),
     * firePropertyChange() does nothing).
     */
    public static final String PROP_RV_DEST = "PropRvDest";
    private final CustomTableModel tblModel = new CustomTableModel();
    private final CustomRvCellEditor rvCellEditor;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_OverrideTracksValueTable.class.getSimpleName());

    public RP_SYS_OverrideTracksValueTable()
    {
        setModel(tblModel);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging

        RhythmVoiceRenderer rvRenderer = new RhythmVoiceRenderer();
        getColumnModel().getColumn(CustomTableModel.COL_SRC_RHYTHM_VOICE).setCellRenderer(rvRenderer);
        getColumnModel().getColumn(CustomTableModel.COL_DEST_RHYTHM_VOICE).setCellRenderer(rvRenderer);

        RhythmRenderer rRenderer = new RhythmRenderer();
        getColumnModel().getColumn(CustomTableModel.COL_SRC_RHYTHM).setCellRenderer(rRenderer);
        getColumnModel().getColumn(CustomTableModel.COL_DEST_RHYTHM).setCellRenderer(rRenderer);

        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        rvCellEditor = new CustomRvCellEditor((rvSrc, rvDest) -> firePropertyChange(PROP_RV_DEST, rvSrc, rvDest == rvSrc ? null : rvDest));
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column)
    {
        LOGGER.log(Level.FINE, "getCellEditor() -- row={0} column={1}", new Object[]
        {
            row, column
        });
        int modelColumn = convertColumnIndexToModel(column);
        int modelRow = convertRowIndexToModel(row);
        if (modelColumn == CustomTableModel.COL_DEST_RHYTHM_VOICE)
        {
            var rvSrc = tblModel.getSrcRhythmVoice(modelRow);
            var override = rvSrc != null ? tblModel.getRpValue().getOverride(rvSrc) : null;
            assert override != null;
            rvCellEditor.resetSilently(rvSrc, override.rvDest().getContainer(), override.rvDest());
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

    public RP_SYS_OverrideTracksValue getRpValue()
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

        public static final int COL_SRC_RHYTHM = 0;
        public static final int COL_SRC_RHYTHM_VOICE = 1;
        public static final int COL_DEST_RHYTHM = 2;
        public static final int COL_DEST_RHYTHM_VOICE = 3;
        private String srcVariationValue;
        private RP_SYS_OverrideTracksValue rpValue;

        /**
         *
         * @param rpValue Can be null
         */
        public void setRpValue(RP_SYS_OverrideTracksValue rpValue)
        {
            this.rpValue = rpValue;
            fireTableDataChanged();
        }

        public RP_SYS_OverrideTracksValue getRpValue()
        {
            return rpValue;
        }

        public String getSrcVariationValue()
        {
            return srcVariationValue;
        }

        public void setSrcVariationValue(String value)
        {
            srcVariationValue = value;
            fireTableDataChanged();
        }


        public RhythmVoice getSrcRhythmVoice(int row)
        {
            RhythmVoice res = rpValue != null ? rpValue.getBaseRhythm().getRhythmVoices().get(row) : null;
            return res;
        }

        public RP_SYS_OverrideTracksValue.Override getOverride(int row)
        {
            var rvSrc = getSrcRhythmVoice(row);
            RP_SYS_OverrideTracksValue.Override res = rvSrc != null ? rpValue.getOverride(rvSrc) : null;
            return res;
        }


        @Override
        public Class<?> getColumnClass(int col)
        {
            Class<?> res = switch (col)
            {
                case COL_SRC_RHYTHM, COL_DEST_RHYTHM ->
                    Rhythm.class;
                case COL_SRC_RHYTHM_VOICE, COL_DEST_RHYTHM_VOICE ->
                    RhythmVoice.class;
                default ->
                    throw new IllegalStateException("columnIndex=" + col);
            };
            return res;
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            boolean b = false;
            if (column == COL_DEST_RHYTHM_VOICE)
            {
                var o = getOverride(row);
                b = o != null;
            }
            return b;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s = switch (columnIndex)
            {
                case COL_SRC_RHYTHM ->
                    ResUtil.getString(getClass(), "ColumnHeaderOriginalRhythm");
                case COL_SRC_RHYTHM_VOICE ->
                    ResUtil.getString(getClass(), "ColumnHeaderOriginalRhythmTrack");
                case COL_DEST_RHYTHM ->
                    ResUtil.getString(getClass(), "ColumnHeaderOverrideRhythm");
                case COL_DEST_RHYTHM_VOICE ->
                    ResUtil.getString(getClass(), "ColumnHeaderOverrideTrack");
                default ->
                    throw new IllegalStateException("columnIndex=" + columnIndex);
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
            return 4;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            if (rpValue == null)
            {
                return null;
            }

            RhythmVoice rvSrc = rpValue.getBaseRhythm().getRhythmVoices().get(row);
            var override = rpValue.getOverride(rvSrc);

            Object res = switch (col)
            {
                case COL_SRC_RHYTHM ->
                    rvSrc.getContainer();
                case COL_SRC_RHYTHM_VOICE ->
                    rvSrc;
                case COL_DEST_RHYTHM ->
                    override != null ? override.rvDest().getContainer() : null;
                case COL_DEST_RHYTHM_VOICE ->
                    override != null ? override.rvDest() : null;
                default ->
                    throw new IllegalStateException("col=" + col);
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
            if (rv instanceof RhythmVoiceDelegate rvd)
            {
                rv = rvd.getSource();
            }
            MidiMix mm = MidiMixManager.getDefault().findMix(r instanceof AdaptedRhythm ar ? ar.getSourceRhythm() : r);
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
                label.setText(toRvString(rv, false));
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
                // Customize string depending on src/dest rhythm
                CustomTableModel model = (CustomTableModel) table.getModel();
                RP_SYS_OverrideTracksValue.Override override = model.getOverride(row);
                String variationValue = col == CustomTableModel.COL_SRC_RHYTHM ? model.getSrcVariationValue() : override.variation();
                String strVariation = variationValue != null ? " /" + variationValue : "";
                label.setText(r.getName() + strVariation);
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
         * @param userChangedValueListener param1=rvSrc, param2=newRvDest
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
