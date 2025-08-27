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
package org.jjazz.rhythmcombinator.api;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmcombinator.api.CompositeRhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmselectiondialog.spi.RhythmPreviewer;
import org.jjazz.rhythmselectiondialog.spi.RhythmSelectionDialogProvider;
import org.jjazz.uiutilities.api.ButtonColumn;
import org.openide.windows.WindowManager;

/**
 * A JTable used to edit the mappings of a CompositeRhythm.
 */

public class CompositeRhythmTable extends JTable
{

    private final MyTableModel tblModel = new MyTableModel();
    private static final Logger LOGGER = Logger.getLogger(CompositeRhythmTable.class.getSimpleName());

    public CompositeRhythmTable()
    {
        setModel(tblModel);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging
        RhythmVoiceRenderer renderer = new RhythmVoiceRenderer();
        getColumnModel().getColumn(MyTableModel.COL_SRC_RHYTHM_VOICE).setCellRenderer(renderer);
        getColumnModel().getColumn(MyTableModel.COL_DEST_RHYTHM_VOICE).setCellRenderer(renderer);
        getTableHeader().setDefaultRenderer(new HeaderRenderer(this));

        // Special component for the buttons
        ActionListener changeMappingAction = e -> 
        {
            int modelRow = Integer.parseInt(e.getActionCommand());
            var r = tblModel.getCompositeRhythm();
            if (r == null)
            {
                // Nothing
            } else
            {
                var rvSrc = r.getRhythmVoices().get(modelRow);
                var rvDest = r.getDestRhythmVoice(rvSrc);
                showRhythmSelectionDialog(r, rvSrc, rvDest);
            }
        };
        var btn = new ButtonColumn(this, changeMappingAction, MyTableModel.COL_ACTIONS);
    }

    @Override
    public MyTableModel getModel()
    {
        return tblModel;
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
                res = tblModel.getCompositeRhythm().getRhythmVoices().get(modelIndex);
            }
        }
        return res;
    }

    /**
     * Select the row corresponding to the specified base RhythmVoice.
     * <p>
     * The method also scroll the table to make the selected instrument visible.
     *
     * @param rv
     */
    public void setSelectedRhythmVoice(RhythmVoice rv)
    {
        int mIndex = tblModel.getCompositeRhythm().getRhythmVoices().indexOf(rv);
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
     * Our composite rhythm model.
     */
    public class MyTableModel extends AbstractTableModel implements PropertyChangeListener
    {

        public static final int COL_SRC_RHYTHM_VOICE = 0;
        public static final int COL_DEST_RHYTHM_VOICE = 1;
        public static final int COL_ACTIONS = 2;
        private CompositeRhythm compositeRhythm;

        public void setCompositeRhythm(CompositeRhythm r)
        {
            if (compositeRhythm != null)
            {
                compositeRhythm.removePropertyChangeListener(this);
            }
            this.compositeRhythm = r;
            if (r != null)
            {
                this.compositeRhythm.addPropertyChangeListener(this);
            }
            fireTableDataChanged();
        }

        public CompositeRhythm getCompositeRhythm()
        {
            return compositeRhythm;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            Class<?> res = switch (col)
            {
                case COL_SRC_RHYTHM_VOICE, COL_DEST_RHYTHM_VOICE ->
                    RhythmVoice.class;
                case COL_ACTIONS ->
                    String.class;
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
                    "Base track";
                case COL_DEST_RHYTHM_VOICE ->
                    "Replacement rhythm/track ";
                case COL_ACTIONS ->
                    "Set replacement";
                default -> throw new IllegalStateException("columnIndex=" + columnIndex);
            };
            return s;
        }

        @Override
        public int getRowCount()
        {
            return compositeRhythm != null ? compositeRhythm.getRhythmVoices().size() : 8;
        }

        @Override
        public int getColumnCount()
        {
            return COL_ACTIONS + 1;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            if (compositeRhythm == null)
            {
                return null;
            }
            RhythmVoice rvSrc = compositeRhythm.getRhythmVoices().get(row);
            Object res = switch (col)
            {
                case COL_SRC_RHYTHM_VOICE ->
                    rvSrc;
                case COL_DEST_RHYTHM_VOICE ->
                    compositeRhythm.getDestRhythmVoice(rvSrc);
                case COL_ACTIONS ->
                    null;
                default -> throw new IllegalStateException("col=" + col);
            };

            return res;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            if (e.getSource() == compositeRhythm && e.getPropertyName().equals(CompositeRhythm.PROP_RESOURCES_LOADED))
            {
                var rvSrc = (RhythmVoice) e.getOldValue();
                int modelRow = compositeRhythm.getRhythmVoices().indexOf(rvSrc);
                assert modelRow != -1 : "rvSrc=" + rvSrc;
                int row = convertRowIndexToView(modelRow);
                fireTableCellUpdated(row, COL_DEST_RHYTHM_VOICE);
            }
        }


        /**
         * Pre-adjust the columns size parameters to have a correct display.
         */
        private void adjustWidths()
        {
            final TableColumnModel colModel = getColumnModel();
            final int EXTRA = 5;
            for (int colIndex = 0; colIndex < getColumnCount(); colIndex++)
            {
                // Handle header
//                TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
//                Component comp = renderer.getTableCellRendererComponent(this, tblModel.getColumnName(colIndex), true, true, 0, colIndex);
//                int headerWidth = comp.getPreferredSize().width;
//
//                int width = 20; // Min width
//
//                // Handle data
//                for (int row = 0; row < getRowCount(); row++)
//                {
//                    renderer = getCellRenderer(row, colIndex);
//                    comp = prepareRenderer(renderer, row, colIndex);
//                    width = Math.max(comp.getPreferredSize().width, width);
//                }
//                width = Math.max(width, headerWidth);
//                width = Math.min(width, 400);
//                width += EXTRA;
//
//                // We have our preferred width
//                colModel.getColumn(colIndex).setPreferredWidth(width);

                // Also set max size
//            switch (colIndex)
//            {
//                case Model.COL_LSB:
//                case Model.COL_MSB:
//                case Model.COL_PC:
//                case Model.COL_ID:
//                    colModel.getColumn(colIndex).setMaxWidth(width);
//                    break;
//                case Model.COL_DRUMKIT:
//                case Model.COL_SYNTH:
//                case Model.COL_BANK:
//                    // colModel.getColumn(colIndex).setMaxWidth(width);
//                    break;
//                case Model.COL_PATCHNAME:
//                    // Nothing
//                    break;
//                default:
//                    throw new IllegalStateException("col=" + colIndex);
//            }
            }
        }

    }


    // ============================================================================
    // Private methods
    // ============================================================================
    private void showRhythmSelectionDialog(CompositeRhythm r, RhythmVoice rvSrc, RhythmVoice rvDest)
    {
        var dlg = RhythmSelectionDialogProvider.getDefault().getDialog();
        var rdb = RhythmDatabase.getDefault();

        var riBase = rdb.getRhythm(r.getBaseRhythm().getUniqueId());
        RhythmInfo ri = (rvDest == null) ? riBase : rdb.getRhythm(rvDest.getContainer().getUniqueId());
        RhythmPreviewer rp = null;
        dlg.preset(ri, rp);
        dlg.setTitleText("Select a " + ri.timeSignature() + " rhythm");
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        // Dialog exited
        if (rp != null)
        {
            rp.cleanup();
        }
        if (!dlg.isExitOk())
        {
            dlg.cleanup();
            return;
        }

        var riDest = dlg.getSelectedRhythm();
        if (riDest == riBase)        
        {
            rvDest = null;
        } else
        {
            
        }
    }


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
            label.setText(rv.getContainer().getName() + "/" + rv.getName());

            return label;
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
