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
package org.jjazz.rhythmselectiondialog.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.database.api.FavoriteRhythms;

/**
 * A JTable to show a list of rhythms.
 */
public class RhythmTable extends JTable implements PropertyChangeListener
{

    private Model model = new Model();
    private List<Integer> hiddenColumnIndexes = new ArrayList<Integer>();
    private static final Logger LOGGER = Logger.getLogger(RhythmTable.class.getSimpleName());

    public RhythmTable()
    {
        setModel(model);
        setAutoCreateRowSorter(true);
        setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);               // Prevent column dragging        
        setDefaultRenderer(String.class, new MyCellRenderer());
        setDefaultRenderer(Integer.class, new MyCellRenderer());

        FavoriteRhythms fr = FavoriteRhythms.getInstance();
        fr.addPropertyListener(this);
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
        return model;
    }

    /**
     * @return The Rhythm corresponding to the selected row, or null if no selection.
     */
    public Rhythm getSelectedRhythm()
    {
        Rhythm r = null;
        if (model != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                r = model.getRhythms().get(modelIndex);
            }
        }
        return r;
    }

    /**
     * Select the row corresponding to the specified rhythm.
     * <p>
     * The method also scroll the table to make the selected instrument visible.
     *
     * @param r
     */
    public void setSelectedRhythm(Rhythm r)
    {
        int mIndex = model.getRhythms().indexOf(r);
        if (mIndex != -1)
        {
            TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) this.getRowSorter();
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
        public static final int COL_NAME = 1;
        public static final int COL_FEEL = 3;
        public static final int COL_TEMPO = 2;
        public static final int COL_NB_VOICES = 4;
        public static final int COL_DIR = 5;
        private List<? extends Rhythm> rhythms = new ArrayList<>();

        public void setRhythms(List<Rhythm> rhythms)
        {
            if (rhythms == null)
            {
                throw new NullPointerException("rhythms");
            }
            LOGGER.fine("setRhythms() rhythms.size()=" + rhythms.size());
            this.rhythms = rhythms;
            fireTableDataChanged();
            adjustWidths();
        }

        List<? extends Rhythm> getRhythms()
        {
            return rhythms;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            switch (col)
            {
                case COL_TEMPO:
                case COL_ID:
                case COL_NB_VOICES:
                    return Integer.class;
                case COL_NAME:
                case COL_FEEL:
                case COL_DIR:
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
                case COL_TEMPO:
                    s = "Tempo ";
                    break;
                case COL_NAME:
                    s = "Name ";
                    break;
                case COL_NB_VOICES:
                    s = "Nb Inst. ";
                    break;
                case COL_FEEL:
                    s = "Feel ";
                    break;
                case COL_DIR:
                    s = "Directory ";
                    break;
                case COL_ID:
                    s = "#   ";
                    break;
                default:
                    throw new IllegalStateException("columnIndex=" + columnIndex);
            }
            return s;
        }

        @Override
        public int getRowCount()
        {
            return rhythms.size();
        }

        @Override
        public int getColumnCount()
        {
            return 6;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            Rhythm r = rhythms.get(row);
            switch (col)
            {
                case COL_TEMPO:
                    return r.getPreferredTempo();
                case COL_DIR:
                    // Show the file path relative to the user rhythm directory
                    FileDirectoryManager fdm = FileDirectoryManager.getInstance();
                    if (r.getFile() == null || "".equals(r.getFile().getPath()))
                    {
                        return " - ";
                    } else if (r.getFile().getAbsolutePath().contains(FileDirectoryManager.APP_CONFIG_PREFIX_DIR))
                    {
                        // Don't show path of the builtin files
                        return "builtin";
                    }
                    Path pDir = fdm.getUserRhythmDirectory().toPath();
                    Path pFile = r.getFile().getParentFile().toPath();
                    String s = null;
                    try
                    {
                        Path relPath = pDir.relativize(pFile);
                        s = "./"+relPath.toString();
                    } catch (IllegalArgumentException ex)
                    {
                        LOGGER.warning("getValueAt() Can't relativize pFile=" + pFile + " to pDir=" + pDir);
                        s = pFile.toString();
                    }
                    return s;
                case COL_NB_VOICES:
                    return r.getRhythmVoices().size();
                case COL_NAME:
                    return r.getName();
                case COL_FEEL:
                    return r.getFeel().toString();
                case COL_ID:
                    return row + 1;
                default:
                    throw new IllegalStateException("col=" + col);
            }
        }

    }

    // =================================================================================
    // PropertyChangeListener methods
    // =================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        FavoriteRhythms fr = FavoriteRhythms.getInstance();
        if (e.getSource() == fr && e.getPropertyName().equals(FavoriteRhythms.PROP_FAVORITE_RHYTHM))
        {
            // A favorite rhythm was removed or added
            // Repaint the corresponding cell
            Rhythm r = (Rhythm) (e.getNewValue() == null ? e.getOldValue() : e.getNewValue());
            int row = model.getRhythms().indexOf(r);
            if (row != -1)
            {
                int vRow = convertRowIndexToView(row);
                repaint(getCellRect(vRow, 0, false));      // Our cell renderer takes care about the favorite status
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
            Component comp = renderer.getTableCellRendererComponent(this, model.getColumnName(colIndex), true, true, 0, colIndex);
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
                case Model.COL_NB_VOICES:
                case Model.COL_TEMPO:
                case Model.COL_ID:
                    colModel.getColumn(colIndex).setMaxWidth(width);
                    break;
                case Model.COL_NAME:
                    colModel.getColumn(colIndex).setMaxWidth(width + 20);
                    break;
                case Model.COL_FEEL:
                    colModel.getColumn(colIndex).setMaxWidth(width);
                    break;
                case Model.COL_DIR:
                    // Nothing
                    break;
                default:
                    throw new IllegalStateException("col=" + colIndex);
            }
        }
    }

    /**
     * Get a string representing the instruments used by the specified rhythm.
     *
     * @param r
     * @return
     */
    private String getInstrumentsString(Rhythm r)
    {
        List<String> list = new ArrayList<>();
        for (RhythmVoice rv : r.getRhythmVoices())
        {
            switch (rv.getType())
            {
                case DRUMS:
                    list.add("drums");
                    break;
                case PERCUSSION:
                    list.add("perc.");
                    break;
                default:    // VOICE       
                    list.add(rv.getPreferredInstrument().getSubstitute().getFamily().getShortName());
            }
        }
        return list.toString();
    }

    private class MyCellRenderer extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            // Same component is reused for several cells : need to reset some default settings
            lbl.setToolTipText(null);
            if (value == null)
            {
                return lbl;
            }
            int modelRow = table.convertRowIndexToModel(row);
            Rhythm r = model.getRhythms().get(modelRow);
            switch (col)
            {
                case Model.COL_NB_VOICES:
                    lbl.setToolTipText(RhythmTable.this.getInstrumentsString(r));
                    break;
                case Model.COL_NAME:
                    lbl.setToolTipText("Decription: " + r.getDescription());
                    break;
                case Model.COL_DIR:
                    lbl.setToolTipText("Relative file path to the User Rhythm Directory");
//                    // Left dots and the right part of the string visible in the cell
//                    int availableWidth = table.getColumnModel().getColumn(col).getWidth();
//                    availableWidth -= table.getIntercellSpacing().getWidth();
//                    Insets borderInsets = lbl.getBorder().getBorderInsets((Component) this);
//                    availableWidth -= (borderInsets.left + borderInsets.right);
//                    String cellText = lbl.getText();
//                    FontMetrics fm = lbl.getFontMetrics(getFont());
//                    if (fm.stringWidth(cellText) > availableWidth)
//                    {
//                        String dots = "...";
//                        int textWidth = fm.stringWidth(dots);
//                        int nChars = cellText.length() - 1;
//                        for (; nChars > 0; nChars--)
//                        {
//                            textWidth += fm.charWidth(cellText.charAt(nChars));
//                            if (textWidth > availableWidth)
//                            {
//                                break;
//                            }
//                        }
//                        lbl.setText(dots + cellText.substring(nChars + 1));
//                    }                  
                    break;
                default:
                    break;
            }

            // Different rendering if it's a favorite
            FavoriteRhythms fr = FavoriteRhythms.getInstance();
            if (fr.contains(r))
            {
                Font f = lbl.getFont();
                Font newFont = f.deriveFont(Font.BOLD);
                lbl.setFont(newFont);
            }
            return lbl;
        }
    }
}
