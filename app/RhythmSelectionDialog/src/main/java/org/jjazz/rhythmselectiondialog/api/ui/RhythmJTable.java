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
package org.jjazz.rhythmselectiondialog.api.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabaseimpl.api.FavoriteRhythms;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.uiutilities.api.StringMetrics;
import org.jjazz.utilities.api.ResUtil;

/**
 * A JTable to show a list of rhythms.
 * <p>
 * Highlight the favorite rhythms.
 */

public class RhythmJTable extends JTable implements PropertyChangeListener
{

    private final RhythmTableModel model = new RhythmTableModel();
    private List<Integer> hiddenColumnIndexes = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmJTable.class.getSimpleName());

    public RhythmJTable()
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
     * @param hiddenColumns The indexes of the columns to hide. If null all columns are shown.
     */
    public void setHiddenColumns(List<Integer> hiddenColumns)
    {
        hiddenColumnIndexes = (hiddenColumns == null) ? new ArrayList<>() : hiddenColumns;
        adjustWidths();
    }

    @Override
    public RhythmTableModel getModel()
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
     * Overridden to add tooltips to some column headers
     *
     * @return
     */
    @Override
    protected JTableHeader createDefaultTableHeader()
    {
        return new JTableHeader(getColumnModel())
        {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                String res = null;
                if (index != -1)
                {
                    int realIndex = columnModel.getColumn(index).getModelIndex();
                    res = model.COLUMN_HEADER_TOOLTIPS[realIndex];
                }
                return res;
            }
        };
    }


    public class RhythmTableModel extends AbstractTableModel
    {

        public static final int COL_ID = 0;
        public static final int COL_NAME = 1;
        public static final int COL_TEMPO = 2;
        public static final int COL_DIVISION = 3;
        public static final int COL_NB_VOICES = 4;
        public static final int COL_DEFAULT_MIX = 5;
        public static final int COL_DIR = 6;
        protected String[] COLUMN_HEADER_TOOLTIPS =
        {
            null,
            null,
            null,
            "Binary, Shuffle or Triplet",
            ResUtil.getString(getClass(), "COL_NbInstrumentsToolTip"),
            ResUtil.getString(getClass(), "COL_DefaultMixTooltip"),
            ResUtil.getString(getClass(), "COL_DirTooltip")
        };
        private List<RhythmInfo> rhythms = new ArrayList<>();
        private final Set<RhythmInfo> highlightedRhythms = new HashSet<>();
        private final Map<RhythmInfo, File> mapRhythmDefaultMixFile = new HashMap<>();

        public RhythmTableModel()
        {
            // Consistency check
            assert COLUMN_HEADER_TOOLTIPS.length == getColumnCount() : "COLUMN_HEADER_TOOLTIPS.length=" + COLUMN_HEADER_TOOLTIPS.length;
        }

        public void setRhythms(List<RhythmInfo> rhythms)
        {
            if (rhythms == null)
            {
                throw new NullPointerException("rhythms");
            }
            LOGGER.log(Level.FINE, "setRhythms() rhythms.size()={0}", rhythms.size());
            this.rhythms = new ArrayList<>(rhythms);
            this.rhythms.sort(new RhythmComparator());

            highlightedRhythms.clear();

            fireTableDataChanged();
            SwingUtilities.invokeLater(() -> adjustWidths());

            // Start a background task to update the default rhythm mix column
            Executors.newSingleThreadExecutor().submit((Runnable) () -> updateDefaultMixValues(this.rhythms));
        }

        public List<? extends RhythmInfo> getRhythms()
        {
            return rhythms;
        }

        /**
         * Show specified rhythm as highlighted (e.g. use a different font colour).
         *
         * @param ri
         * @param b  Highlighted state
         */
        public void setHighlighted(RhythmInfo ri, boolean b)
        {
            int mIndex = model.getRhythms().indexOf(ri);
            LOGGER.log(Level.FINE, "setHighlighted() ri={0} b={1} mIndex={2}", new Object[]
            {
                ri, b, mIndex
            });

            if (mIndex == -1)
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

        public String getLongestDataAsString(int col)
        {
            String res = " ";
            for (int row = 0; row < rhythms.size(); row++)
            {
                String s = getValueAt(row, col).toString();
                if (s == null)
                {
                    continue;
                }
                if (s.length() > res.length())
                {
                    res = s;
                }
            }
            return res;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            Class<?> res = switch (col)
            {
                case COL_TEMPO, COL_ID, COL_NB_VOICES ->
                    Integer.class;
                case COL_NAME, COL_DIVISION, COL_DEFAULT_MIX, COL_DIR ->
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
                case COL_TEMPO ->
                    ResUtil.getString(getClass(), "COL_Tempo");
                case COL_NAME ->
                    ResUtil.getString(getClass(), "COL_Name");
                case COL_NB_VOICES ->
                    ResUtil.getString(getClass(), "COL_NbInstruments");
                case COL_DIVISION ->
                    ResUtil.getString(getClass(), "COL_Feel");  // division was previously called Feel, don't want to change the bundle key
                case COL_DEFAULT_MIX ->
                    ResUtil.getString(getClass(), "COL_DefaultMix");
                case COL_DIR ->
                    ResUtil.getString(getClass(), "COL_Directory");
                case COL_ID ->
                    "#   ";
                default -> throw new IllegalStateException("columnIndex=" + columnIndex);
            };
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
            return COL_DIR + 1;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount())
            {
                return null;
            }
            RhythmInfo ri = rhythms.get(row);

            Object value = switch (col)
            {
                case COL_TEMPO ->
                    ri.preferredTempo();

                case COL_DIR ->
                {
                    // Show the file path relative to the user rhythm directory
                    if (ri.file() == null || "".equals(ri.file().getPath()))
                    {
                        // Not file based
                        yield " - ";
                    }


                    Path pUserRhythmDir = RhythmDirsLocator.getDefault().getUserRhythmsDirectory().toPath();
                    Path pFile = ri.file().toPath();
                    if (!pFile.startsWith(pUserRhythmDir))
                    {
                        // File-based but builtin rhythm: don't show path
                        yield ResUtil.getString(getClass(), "DefaultRhythmsPath");
                    }


                    // User-defined file-based, show path relative to user rhythm directory
                    Path pParentFile = ri.file().getParentFile().toPath();
                    String s;
                    try
                    {
                        Path relPath = pUserRhythmDir.relativize(pParentFile);
                        s = "./" + relPath.toString();
                    } catch (IllegalArgumentException ex)
                    {
                        LOGGER.log(Level.WARNING, "getValueAt() Can''t relativize pFile={0} to pDir={1}", new Object[]
                        {
                            pParentFile,
                            pUserRhythmDir
                        });
                        s = pParentFile.toString();
                    }
                    yield s;
                }

                case COL_NB_VOICES ->
                    ri.rvInfos().size();

                case COL_NAME ->
                    ri.name();

                case COL_DIVISION ->
                {
                    var d = ri.rhythmFeatures().division();
                    var sd = switch (d)
                    {
                        case BINARY ->
                            "Binary";
                        case EIGHTH_SHUFFLE ->
                            "Shuffle";
                        case EIGHTH_TRIPLET ->
                            "Triplet";
                        case UNKNOWN ->
                            "Unknown";
                        default -> throw new IllegalArgumentException("division=" + d);
                    };
                    yield sd;
                }

                case COL_DEFAULT_MIX ->
                    getDefaultMix(ri) != null ? "Yes" : "   ";

                case COL_ID ->
                    row + 1;

                default -> throw new IllegalStateException("col=" + col);
            };

            return value;
        }


        /**
         * Get the optional default rhythm mix file associated to ri.
         *
         * @param ri
         * @return Can be null
         */
        synchronized private File getDefaultMix(RhythmInfo ri)
        {
            return mapRhythmDefaultMixFile.get(ri);
        }

        /**
         * Associate a defaultMixFile to a rhythm.
         *
         * @param ri
         * @param defaultMixFile Can be null
         */
        synchronized private void setDefaultMix(RhythmInfo ri, File defaultMixFile)
        {
            var old = mapRhythmDefaultMixFile.put(ri, defaultMixFile);
            if (Objects.equals(old, defaultMixFile))
            {
                int row = rhythms.indexOf(ri);
                if (row == -1)
                {
                    LOGGER.log(Level.WARNING, "setDefaultMix() Unexpected ri={0} not found", ri);
                    return;
                }
                fireTableCellUpdated(row, COL_DEFAULT_MIX);
            }
        }


        private void updateDefaultMixValues(List<RhythmInfo> ris)
        {
            LOGGER.fine("updateDefaultMixValues() -- starting updating default mix values");
            for (var ri : ris)
            {
                var rhythmMixFile = MidiMix.getRhythmMixFile(ri.name(), ri.file());
                setDefaultMix(ri, rhythmMixFile.exists() ? rhythmMixFile : null);
            }
            LOGGER.fine("updateDefaultMixValues() finished");
        }

        /**
         * Sort first by directory then by alphabetical order.
         */
        private class RhythmComparator implements Comparator<RhythmInfo>
        {

            @Override
            public int compare(RhythmInfo ri1, RhythmInfo ri2)
            {
                File pf1 = ri1.file().getParentFile();   // Can be null for builtin rhythms
                File pf2 = ri2.file().getParentFile();   // Can be null for builtin rhythms 
                if ((pf1 == null && pf2 == null) || (pf1 != null && pf1.equals(pf2)))
                {
                    // Sort by name if same directory
                    return ri1.name().compareTo(ri2.name());
                } else if (pf1 == null)
                {
                    return -1;
                } else if (pf2 == null)
                {
                    return 1;
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

        final StringMetrics stringMetrics = StringMetrics.create(getFont());
        final int charPixelWidth = (int) stringMetrics.getWidth(">");
        final int maxPreferredWidth = (int) stringMetrics.getWidth("this is quite a loooooooooooooooooong string no?");

        for (int colIndex = 0; colIndex < getColumnCount(); colIndex++)
        {
            TableColumn tableColumn = colModel.getColumn(colIndex);

            if (hiddenColumnIndexes.contains(colIndex))
            {
                tableColumn.setMinWidth(0);
                tableColumn.setMaxWidth(0);
                tableColumn.setPreferredWidth(0);
                continue;
            }

            // Header
            int headerWidth = (int) stringMetrics.getWidth(model.getColumnName(colIndex)) + 2 * charPixelWidth;


            // Data
            String longestColString = model.getLongestDataAsString(colIndex);
            int width = (int) stringMetrics.getWidth(longestColString) + charPixelWidth;
            width = Math.max(width, headerWidth);                   // no smaller than header
            width = Math.min(width, maxPreferredWidth);           // not too wide


            switch (colIndex)
            {
                case RhythmTableModel.COL_NB_VOICES, RhythmTableModel.COL_TEMPO, RhythmTableModel.COL_DIVISION, RhythmTableModel.COL_DEFAULT_MIX ->
                {
                    int pw = width;
                    tableColumn.setMaxWidth(pw);
                    tableColumn.setPreferredWidth(pw);      // must be done after setMaxWidth
                }
                case RhythmTableModel.COL_ID ->
                {
                    int pw = width + charPixelWidth;    // if highlighted contains additional chars
                    tableColumn.setMaxWidth(pw);
                    tableColumn.setPreferredWidth(pw);      // must be done after setMaxWidth
                }
                case RhythmTableModel.COL_NAME ->
                {
                    int pw = width + 4 * charPixelWidth;    // if highlighted contains additional chars
                    tableColumn.setMaxWidth(pw);
                    tableColumn.setPreferredWidth(pw);      // must be done after setMaxWidth
                }
                case RhythmTableModel.COL_DIR ->
                {
                    tableColumn.setPreferredWidth(width);
                    // Don't set max
                }
                default -> throw new IllegalStateException("col=" + colIndex);
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
        for (RhythmInfo.RvInfo rvi : ri.rvInfos())
        {
            switch (rvi.type())
            {
                case DRUMS -> list.add("drums");
                case PERCUSSION -> list.add("perc.");
                default ->
                {
                    // VOICE     
                    GM1Instrument substitute = rvi.gmSubstitute();
                    list.add(substitute == null ? rvi.type().toString() : substitute.getFamily().getShortName());
                }
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
            File f = ri.file();

            switch (col)
            {
                case RhythmTableModel.COL_NB_VOICES -> lbl.setToolTipText(RhythmJTable.this.getInstrumentsString(ri));

                case RhythmTableModel.COL_NAME ->
                {
                    String s = f == null ? "" : ", " + ResUtil.getString(getClass(), "COL_NameToolTip", f.getName());
                    lbl.setToolTipText(ResUtil.getString(getClass(), "COL_DescToolTip", ri.description() + s));
                }

                case RhythmTableModel.COL_DEFAULT_MIX ->
                {
                    File rdm = model.getDefaultMix(ri);
                    lbl.setToolTipText(rdm != null ? rdm.getAbsolutePath() : ResUtil.getString(getClass(), "NoRhythmDefaultMix", ri.name()));
                }

                case RhythmTableModel.COL_DIR -> lbl.setToolTipText(ResUtil.getString(getClass(), "COL_DirTooltip"));
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
                default ->
                {
                }
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
                    case RhythmTableModel.COL_ID -> lbl.setText(">>>");
                    case RhythmTableModel.COL_NAME -> lbl.setText(">>> " + lbl.getText());
                    default ->
                    {
                        // Nothing
                    }
                }
            }

            return lbl;
        }
    }
}
