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
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.jjazz.midi.synths.GM1Instrument;
import org.jjazz.rhythm.database.api.FavoriteRhythms;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.RhythmVoiceInfo;
import org.jjazz.util.ResUtil;

/**
 * A JTable to show a list of rhythms.
 * <p>
 * Highlight the favorite rhythms.
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
     * The rhythm corresponding to the row which encloses the specified point.
     *
     * @param p
     * @return Can be null.
     */
    public RhythmInfo getRhythm(Point p)
    {
        RhythmInfo ri = null;
        int rowIndex = rowAtPoint(p);
        if (rowIndex != -1)
        {
            int modelIndex = convertRowIndexToModel(rowIndex);
            ri = model.getRhythms().get(modelIndex);
        }

        return ri;
    }

    /**
     * @return The Rhythm corresponding to the selected row, or null if no selection or no data.
     */
    public RhythmInfo getSelectedRhythm()
    {
        RhythmInfo ri = null;
        if (model != null)
        {
            int rowIndex = getSelectedRow();
            if (rowIndex != -1)
            {
                int modelIndex = convertRowIndexToModel(rowIndex);
                if (modelIndex < model.getRowCount())
                {
                    ri = model.getRhythms().get(modelIndex);
                }
            }
        }
        return ri;
    }

    /**
     * Select the row corresponding to the specified rhythm.
     * <p>
     * The method also scroll the table to make the selected instrument visible.
     *
     * @param ri
     */
    public void setSelectedRhythm(RhythmInfo ri)
    {
        int mIndex = model.getRhythms().indexOf(ri);
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
        private List<? extends RhythmInfo> rhythms = new ArrayList<>();
        private Set<RhythmInfo> highlightedRhythms = new HashSet<>();

        public void setRhythms(List<RhythmInfo> rhythms)
        {
            if (rhythms == null)
            {
                throw new NullPointerException("rhythms");   //NOI18N
            }
            LOGGER.fine("setRhythms() rhythms.size()=" + rhythms.size());   //NOI18N
            this.rhythms = new ArrayList<>(rhythms);
            this.rhythms.sort(new RhythmComparator());

            highlightedRhythms.clear();

            fireTableDataChanged();
            adjustWidths();
        }

        List<? extends RhythmInfo> getRhythms()
        {
            return rhythms;
        }

        /**
         * Show specified rhythm as highlighted (e.g. use a different font colour).
         *
         * @param ri
         * @param b Highlighted state
         */
        public void setHighlighted(RhythmInfo ri, boolean b)
        {
            int mIndex = model.getRhythms().indexOf(ri);
            LOGGER.severe("setHighlighted() ri=" + ri + " b=" + b + " mIndex=" + mIndex);   //NOI18N

            if (mIndex == -1)
            {
                return;
            }

            if ((b && highlightedRhythms.contains(ri)) || (!b && !highlightedRhythms.contains(ri)))
            {
                return;
            }


            if (b)
            {
                highlightedRhythms.add(ri);
            } else
            {
                highlightedRhythms.remove(ri);
            }

            fireTableRowsUpdated(mIndex, mIndex);
        }

        public boolean isHighlighted(RhythmInfo ri)
        {
            return highlightedRhythms.contains(ri);
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
                    throw new IllegalStateException("columnIndex=" + col);   //NOI18N
            }
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            String s;
            switch (columnIndex)
            {
                case COL_TEMPO:
                    s = ResUtil.getString(getClass(), "COL_Tempo");
                    break;
                case COL_NAME:
                    s = ResUtil.getString(getClass(), "COL_Name");
                    break;
                case COL_NB_VOICES:
                    s = ResUtil.getString(getClass(), "COL_NbInstruments");
                    break;
                case COL_FEEL:
                    s = ResUtil.getString(getClass(), "COL_Feel");
                    break;
                case COL_DIR:
                    s = ResUtil.getString(getClass(), "COL_Directory");
                    break;
                case COL_ID:
                    s = "#   ";
                    break;
                default:
                    throw new IllegalStateException("columnIndex=" + columnIndex);   //NOI18N
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
            if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount())
            {
                return null;
            }
            RhythmInfo ri = rhythms.get(row);
            switch (col)
            {
                case COL_TEMPO:
                    return ri.getPreferredTempo();
                case COL_DIR:
                    // Show the file path relative to the user rhythm directory
                    FileDirectoryManager fdm = FileDirectoryManager.getInstance();
                    if (ri.getFile() == null || "".equals(ri.getFile().getPath()))
                    {
                        return " - ";
                    } else if (ri.getFile().getAbsolutePath().contains(FileDirectoryManager.APP_CONFIG_PREFIX_DIR))
                    {
                        // Don't show path of the builtin files
                        return ResUtil.getString(getClass(), "DefaultRhythmsPath");
                    }
                    Path pDir = fdm.getUserRhythmDirectory().toPath();
                    Path pFile = ri.getFile().getParentFile().toPath();
                    String s = null;
                    try
                    {
                        Path relPath = pDir.relativize(pFile);
                        s = "./" + relPath.toString();
                    } catch (IllegalArgumentException ex)
                    {
                        LOGGER.warning("getValueAt() Can't relativize pFile=" + pFile + " to pDir=" + pDir);   //NOI18N
                        s = pFile.toString();
                    }
                    return s;
                case COL_NB_VOICES:
                    return ri.getRhythmVoiceInfos().size();
                case COL_NAME:
                    return ri.getName();
                case COL_FEEL:
                    return ri.getFeatures().getFeel().toString();
                case COL_ID:
                    return row + 1;
                default:
                    throw new IllegalStateException("col=" + col);   //NOI18N
            }
        }

        /**
         * Sort first by directory then by alphabetical order.
         */
        private class RhythmComparator implements Comparator<RhythmInfo>
        {

            @Override
            public int compare(RhythmInfo ri1, RhythmInfo ri2)
            {
                File pf1 = ri1.getFile().getParentFile();
                File pf2 = ri2.getFile().getParentFile();
                if (pf1.equals(pf2))
                {
                    // Sort by name if same directory
                    return ri1.getName().compareTo(ri2.getName());
                } else
                {
                    // Sort by directory
                    return pf1.getPath().compareTo(pf2.getPath());
                }
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
            // Update the corresponding row           
            RhythmInfo r = (RhythmInfo) (e.getNewValue() == null ? e.getOldValue() : e.getNewValue());
            int row = model.getRhythms().indexOf(r);
            if (row != -1)
            {
                model.fireTableRowsUpdated(row, row);
            }

//            if (row != -1)
//            {
//                int vRow = convertRowIndexToView(row);
//                repaint(getCellRect(vRow, 0, false));      // Our cell renderer takes care about the favorite status
//            }
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
                    throw new IllegalStateException("col=" + colIndex);   //NOI18N
            }
        }
    }

    /**
     * Get a string representing the instruments used by the specified rhythm.
     *
     * @param ri
     * @return
     */
    private String getInstrumentsString(RhythmInfo ri)
    {
        List<String> list = new ArrayList<>();
        for (RhythmVoiceInfo rvi : ri.getRhythmVoiceInfos())
        {
            switch (rvi.getType())
            {
                case DRUMS:
                    list.add("drums");
                    break;
                case PERCUSSION:
                    list.add("perc.");
                    break;
                default:    // VOICE     
                    GM1Instrument substitute = rvi.getPreferredInstrument().getSubstitute();
                    list.add(substitute == null ? "?????" : substitute.getFamily().getShortName());
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
            RhythmInfo ri = model.getRhythms().get(modelRow);
            File f = ri.getFile();
            switch (col)
            {
                case Model.COL_NB_VOICES:
                    lbl.setToolTipText(RhythmTable.this.getInstrumentsString(ri));
                    break;
                case Model.COL_NAME:
                    String s = f == null ? "" : ", " + ResUtil.getString(getClass(), "COL_NameToolTip", f.getName());
                    lbl.setToolTipText(ResUtil.getString(getClass(), "COL_DescToolTip", ri.getDescription() + s));
                    break;
                case Model.COL_DIR:
                    lbl.setToolTipText(ResUtil.getString(getClass(), "COL_DirTooltip"));
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
            if (fr.contains(ri))
            {
                Font font = lbl.getFont();
                Font newFont = font.deriveFont(Font.BOLD);
                lbl.setFont(newFont);
            }

            // Highlight rendering
            if (model.isHighlighted(ri))
            {
                switch (col)
                {
                    case Model.COL_ID:
                        lbl.setText(">>>");
                        break;
                    case Model.COL_NAME:
                        lbl.setText(">>> " + lbl.getText());
                        break;
                    default:
                    // Nothing
                }
            }

            return lbl;
        }
    }
}
