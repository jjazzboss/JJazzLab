/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.proswing.walkingbass.explorer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;
import org.jjazz.proswing.walkingbass.WbpSource;
import org.jjazz.proswing.walkingbass.DefaultWbpsaScorer;
import org.jjazz.proswing.walkingbass.WbpSourceAdaptation;
import org.jjazz.proswing.walkingbass.WbpsaScorer;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.IntRange;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * A dialog to query the WbpDatabase.
 */
public class WbpDatabaseExplorerDialog extends javax.swing.JDialog
{

    private static final Logger LOGGER = Logger.getLogger(WbpDatabaseExplorerDialog.class.getSimpleName());

    public WbpDatabaseExplorerDialog(boolean modal)
    {
        super(WindowManager.getDefault().getMainWindow(), modal);
        setAlwaysOnTop(modal);
        initComponents();

        prepareTable();
        list_Styles.setSelectedValue(BassStyle.WALKING, true);
        list_Styles.addListSelectionListener(e -> doUpdate());


        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        UIUtilities.installEscapeKeyAction(this, () -> exit());
        UIUtilities.installEnterKeyAction(this, () -> doUpdate());

        // System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "[%1$s p=%2$.1f d=%3$.1f]");
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s p=%2$.1f d=%3$.1f");


        doUpdate();

        setSize(1600, 600);
        setLocationRelativeTo(null);
    }

    public List<WbpSource> getSelectedWbpSources()
    {
        return getSelectedWbpSourceAdaptations().stream()
                .map(wbpsa -> wbpsa.getWbpSource())
                .toList();
    }

    public List<WbpSourceAdaptation> getSelectedWbpSourceAdaptations()
    {
        List<WbpSourceAdaptation> res = new ArrayList<>();
        for (int row : tbl_wbpSources.getSelectedRows())
        {
            int modelIndex = tbl_wbpSources.convertRowIndexToModel(row);
            res.add(getTableModel().getWbpSourceAdaptation(modelIndex));
        }
        return res;
    }


    // ========================================================================================================
    // Private methods
    // ========================================================================================================
    private MyModel getTableModel()
    {
        return (MyModel) tbl_wbpSources.getModel();
    }

    private int getTempo()
    {
        return (Integer) spn_tempo.getValue();
    }

    private BassStyle[] getBassStyles()
    {
        var res = list_Styles.getSelectedValuesList().toArray(new BassStyle[0]);
        if (res.length == 0)
        {
            res = BassStyle.values();
        }
        return res;
    }

    private void doUpdate()
    {
        var wbpsas = getWbpSourceAdaptations(getTempo(), getBassStyles());
        getTableModel().setWbpSourceAdaptations(wbpsas);
        // adjustWidths();
        lbl_info.setText(wbpsas.size() + " WbpSource(s)");

    }

    private List<WbpSourceAdaptation> getWbpSourceAdaptations(int tempo, BassStyle[] bassStyles)
    {
        // Compute the root profile
        SimpleChordSequence scs;
        var wbpDb = WbpSourceDatabase.getInstance();
        try
        {
            scs = computeSimpleChordSequence();
        } catch (ParseException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return Collections.EMPTY_LIST;
        }

        List<WbpSourceAdaptation> res;
        if (scs.isEmpty())
        {
            // Special show all
            var wbpSources = wbpDb.getWbpSources(getNbBars());
            res = wbpSources.stream()
                    .map(wbps -> new WbpSourceAdaptation(wbps, scs))
                    .toList();
        } else if (rb_rootProfile.isSelected())
        {
            var rpScs = scs.getRootProfile();
            res = wbpDb.getWbpSources().stream()
                    .filter(s -> rpScs.equals(s.getRootProfile()))
                    .map(wbps -> new WbpSourceAdaptation(wbps, scs))
                    .toList();
        } else
        {
            WbpsaScorer wbpsaScorer = new DefaultWbpsaScorer(null, tempo, null, bassStyles);
            res = new ArrayList<>(wbpsaScorer.getWbpSourceAdaptations(scs, null).values());
        }


        return res;

    }

    private int getNbBars()
    {
        return (Integer) spn_barSize.getValue();
    }

    private SimpleChordSequence computeSimpleChordSequence() throws ParseException
    {
        int nbBars = getNbBars();

        SimpleChordSequence res = new SimpleChordSequence(new IntRange(0, nbBars - 1), TimeSignature.FOUR_FOUR);
        switch (nbBars)
        {
            case 4:
                var str = tf_bar3.getText();
                List<CLI_ChordSymbol> cliList;
                if (!str.isBlank())
                {
                    cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 3, false);
                    res.addAll(cliList);
                }
            case 3:
                str = tf_bar2.getText();
                if (!str.isBlank())
                {
                    cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 2, false);
                    res.addAll(cliList);
                }
            case 2:
                str = tf_bar1.getText();
                if (!str.isBlank())
                {
                    cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 1, false);
                    res.addAll(cliList);
                }
            case 1:
                str = tf_bar0.getText();
                if (!str.isBlank())
                {
                    cliList = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(str, TimeSignature.FOUR_FOUR, null, 0, false);
                    res.addAll(cliList);
                }
                break;
            default:
                throw new IllegalStateException("nbBars=" + nbBars);
        }
        res.removeRedundantChords();

        return res;
    }

    private void prepareTable()
    {
        tbl_wbpSources.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_ID).setPreferredWidth(170);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_ID).setMinWidth(170);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_SCORE).setPreferredWidth(60);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_SCORE).setMinWidth(60);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_SCORE).setMaxWidth(60);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_CHORDS).setPreferredWidth(170);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_CHORDS).setMinWidth(150);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_PHRASE).setPreferredWidth(500);
        tbl_wbpSources.getColumnModel().getColumn(MyModel.COL_PHRASE).setMinWidth(200);


        tbl_wbpSources.setAutoCreateRowSorter(true);


        JPopupMenu pMenu = new JPopupMenu();
        pMenu.add(new JMenuItem(new PrintPhrasesAction(this)));
        pMenu.add(new JMenuItem(new EvaluateScoreAction(this)));
        pMenu.add(new JMenuItem(new DumpWbpSourceAction(this)));
        tbl_wbpSources.setComponentPopupMenu(pMenu);

    }

    private void exit()
    {
        setVisible(false);
        dispose();
    }

    /**
     * Note, NotePos, NotePosDur
     *
     * @return
     */
    private String getNoteRepresentationDetail()
    {
        return cmb_noteDetail.getSelectedItem().toString();
    }


    // ========================================================================================================
    // Inner classes
    // ========================================================================================================
    private class MyModel extends AbstractTableModel
    {

        public static final int COL_ID = 0;
        public static final int COL_STYLE = 1;
        public static final int COL_SCORE = 2;
        public static final int COL_SCORE_DETAILS = 3;
        public static final int COL_CHORDS = 4;
        public static final int COL_STATS = 5;
        public static final int COL_PHRASE = 6;
        private static final int NB_COLS = 7;


        private final List<WbpSourceAdaptation> wbpsas = new ArrayList<>();
        private final List<WbpSourceAdaptation> oldWbpsas = new ArrayList<>();

        public MyModel()
        {
        }

        public void setWbpSourceAdaptations(List<WbpSourceAdaptation> wbpsas)
        {
            Objects.requireNonNull(wbpsas);
            oldWbpsas.clear();
            this.oldWbpsas.addAll(this.wbpsas);
            this.wbpsas.clear();
            this.wbpsas.addAll(wbpsas);
            fireTableDataChanged();
        }

        /**
         * A string provinding added/removed WbpSourceAdaptations from the last call to setWbpSourceAdaptations().
         *
         * @return
         */
        public String getWhatChangedString()
        {
            var added = new ArrayList<>(wbpsas);
            added.removeIf(wbpsa -> oldWbpsas.stream()
                    .anyMatch(wbpsaOld -> wbpsa.getWbpSource().getId().equals(wbpsaOld.getWbpSource().getId())));
            var removed = new ArrayList<>(oldWbpsas);
            removed.removeIf(wbpsaOld -> wbpsas.stream()
                    .anyMatch(wbpsa -> wbpsa.getWbpSource().getId().equals(wbpsaOld.getWbpSource().getId())));

            StringBuilder sb = new StringBuilder();
            sb.append("== What changed ADDED:\n");
            for (var wbpsa : added)
            {
                sb.append(String.format(" %s  p=%s\n", wbpsa, wbpsa.getWbpSource().getSizedPhrase().toStringSimple(true)));
            }
            sb.append("\n== What changed REMOVED:\n");
            for (var wbpsa : removed)
            {
                sb.append(String.format(" %s  p=%s\n", wbpsa, wbpsa.getWbpSource().getSizedPhrase().toStringSimple(true)));
            }

            return sb.toString();
        }


        WbpSourceAdaptation getWbpSourceAdaptation(int modelRow)
        {
            return wbpsas.get(modelRow);
        }

        @Override
        public int getRowCount()
        {
            return wbpsas.size();
        }

        @Override
        public int getColumnCount()
        {
            return NB_COLS;
        }

        @Override
        public Class<?> getColumnClass(int col)
        {
            var res = switch (col)
            {
                case COL_ID, COL_SCORE_DETAILS, COL_CHORDS, COL_PHRASE, COL_STYLE, COL_STATS ->
                    String.class;
                case COL_SCORE ->
                    Float.class;
                default -> throw new IllegalStateException("col=" + col);
            };
            return res;
        }

        @Override
        public String getColumnName(int col)
        {
            String s = switch (col)
            {
                case COL_ID ->
                    "Id";
                case COL_STYLE ->
                    "Style";
                case COL_SCORE ->
                    "Score";
                case COL_STATS ->
                    "Stats";
                case COL_SCORE_DETAILS ->
                    "Score details";
                case COL_CHORDS ->
                    "Chords";
                case COL_PHRASE ->
                    "Phrase";
                default -> throw new IllegalStateException("columnIndex=" + col);
            };
            return s;
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            var wbpsa = getWbpSourceAdaptation(row);
            var wbpSource = wbpsa.getWbpSource();
            Object res = switch (col)
            {
                case COL_ID ->
                    wbpSource.getId();
                case COL_STYLE ->
                    wbpSource.getBassStyle().toString();
                case COL_STATS ->
                {
                    var stats = wbpSource.getStats();
                    var s = String.format("_sh=%d, _8th=%d, sh=%d, 8th=%d, 4th=%d, .4th=%d, lg=%d, 1b=%b",
                            stats.nbMaxSuccessiveShortNotes(), stats.nbMaxSuccessiveDottedEighthNotes(), stats.nbShortNotes(),
                            stats.nbDottedEighthNotes(), stats.nbQuarterNotes(), stats.nbDottedQuarterNotes(), stats.nbLongNotes(), stats.isOneNotePerBeat());
                    yield s;
                }
                case COL_SCORE ->
                    wbpsa.getCompatibilityScore().overall();
                case COL_SCORE_DETAILS ->
                    wbpsa.getCompatibilityScore().toString();
                case COL_CHORDS ->
                    new ArrayList<>(wbpSource.getSimpleChordSequence()).toString();
                case COL_PHRASE ->
                {
                    yield switch (getNoteRepresentationDetail())
                    {
                        case "Note" ->
                            wbpSource.getSizedPhrase().toStringSimple(false);
                        case "NotePos" ->
                            wbpSource.getSizedPhrase().toStringSimple(true);
                        case "NotePosDur" ->
                            new ArrayList<>(wbpSource.getSizedPhrase()).toString();
                        default -> throw new IllegalStateException(getNoteRepresentationDetail());
                    };
                }
                default -> throw new IllegalStateException("columnIndex=" + col);
            };
            return res;
        }
    }


    // ========================================================================================================
    // UI
    // ========================================================================================================
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        buttonGroup1 = new javax.swing.ButtonGroup();
        spn_barSize = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        tf_bar0 = new javax.swing.JTextField();
        tf_bar1 = new javax.swing.JTextField();
        tf_bar2 = new javax.swing.JTextField();
        tf_bar3 = new javax.swing.JTextField();
        btn_clear = new javax.swing.JButton();
        lbl_chordSeq = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_wbpSources = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        lbl_info = new javax.swing.JLabel();
        btn_update = new javax.swing.JButton();
        rb_rootProfile = new javax.swing.JRadioButton();
        rb_rpChordTypes = new javax.swing.JRadioButton();
        btn_whatChanged = new javax.swing.JButton();
        cmb_noteDetail = new javax.swing.JComboBox<>();
        spn_tempo = new org.jjazz.flatcomponents.api.WheelSpinner();
        lbl_Tempo = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        list_Styles = new JList(BassStyle.values());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.title")); // NOI18N

        spn_barSize.setModel(new javax.swing.SpinnerNumberModel(4, 1, 4, 1));
        spn_barSize.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_barSizeStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, "WbpSource size in bars"); // NOI18N

        tf_bar0.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar0.text")); // NOI18N
        tf_bar0.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar0ActionPerformed(evt);
            }
        });

        tf_bar1.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar1.text")); // NOI18N
        tf_bar1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar1ActionPerformed(evt);
            }
        });

        tf_bar2.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar2.text")); // NOI18N
        tf_bar2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar2ActionPerformed(evt);
            }
        });

        tf_bar3.setText(org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.tf_bar3.text")); // NOI18N
        tf_bar3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                tf_bar3ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_clear, "Clear"); // NOI18N
        btn_clear.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_clearActionPerformed(evt);
            }
        });

        lbl_chordSeq.setFont(lbl_chordSeq.getFont().deriveFont(lbl_chordSeq.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(lbl_chordSeq, "Enter 0, 1 or 2 chords per bar. Use e.g. C7*Phrygian to set a scale to the a chord"); // NOI18N

        tbl_wbpSources.setModel(new MyModel());
        jScrollPane1.setViewportView(tbl_wbpSources);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, "Matching goal:"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lbl_info, org.openide.util.NbBundle.getMessage(WbpDatabaseExplorerDialog.class, "WbpDatabaseExplorerDialog.lbl_info.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_update, "Update"); // NOI18N
        btn_update.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_updateActionPerformed(evt);
            }
        });

        buttonGroup1.add(rb_rootProfile);
        org.openide.awt.Mnemonics.setLocalizedText(rb_rootProfile, "root profile"); // NOI18N
        rb_rootProfile.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rb_rootProfileActionPerformed(evt);
            }
        });

        buttonGroup1.add(rb_rpChordTypes);
        rb_rpChordTypes.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rb_rpChordTypes, "rp + chord types"); // NOI18N
        rb_rpChordTypes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rb_rpChordTypesActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_whatChanged, "Log what changed"); // NOI18N
        btn_whatChanged.setToolTipText("Log  added/removed WbpSources since last update"); // NOI18N
        btn_whatChanged.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_whatChangedActionPerformed(evt);
            }
        });

        cmb_noteDetail.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Note", "NotePos", "NotePosDur", " " }));
        cmb_noteDetail.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cmb_noteDetailActionPerformed(evt);
            }
        });

        spn_tempo.setModel(new javax.swing.SpinnerNumberModel(120, 50, 250, 5));
        spn_tempo.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_tempoStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_Tempo, "Tempo"); // NOI18N

        jScrollPane2.setViewportView(list_Styles);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_info, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addGap(18, 18, 18)
                                        .addComponent(rb_rootProfile)
                                        .addGap(2, 2, 2))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(tf_bar0, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(tf_bar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addComponent(rb_rpChordTypes))
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(lbl_Tempo)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(spn_tempo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(tf_bar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(tf_bar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addComponent(lbl_chordSeq)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(spn_barSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmb_noteDetail, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_whatChanged, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btn_update, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btn_clear, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {tf_bar0, tf_bar1, tf_bar2, tf_bar3});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spn_barSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(spn_tempo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_Tempo))
                        .addGap(18, 18, 18)
                        .addComponent(lbl_chordSeq)
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tf_bar0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tf_bar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tf_bar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tf_bar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(rb_rootProfile)
                            .addComponent(rb_rpChordTypes))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_info))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(cmb_noteDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_clear)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_update)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_whatChanged))
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void spn_barSizeStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_barSizeStateChanged
    {//GEN-HEADEREND:event_spn_barSizeStateChanged
        int value = (Integer) spn_barSize.getValue();
        tf_bar1.setEnabled(value >= 2);
        tf_bar2.setEnabled(value >= 3);
        tf_bar3.setEnabled(value >= 4);
        doUpdate();
    }//GEN-LAST:event_spn_barSizeStateChanged

    private void btn_clearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_clearActionPerformed
    {//GEN-HEADEREND:event_btn_clearActionPerformed
        tf_bar0.setText("");
        tf_bar1.setText("");
        tf_bar2.setText("");
        tf_bar3.setText("");
        doUpdate();
    }//GEN-LAST:event_btn_clearActionPerformed

    private void tf_bar1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar1ActionPerformed
    {//GEN-HEADEREND:event_tf_bar1ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar1ActionPerformed

    private void btn_updateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_updateActionPerformed
    {//GEN-HEADEREND:event_btn_updateActionPerformed
        doUpdate();
    }//GEN-LAST:event_btn_updateActionPerformed

    private void tf_bar0ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar0ActionPerformed
    {//GEN-HEADEREND:event_tf_bar0ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar0ActionPerformed

    private void tf_bar2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar2ActionPerformed
    {//GEN-HEADEREND:event_tf_bar2ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar2ActionPerformed

    private void tf_bar3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tf_bar3ActionPerformed
    {//GEN-HEADEREND:event_tf_bar3ActionPerformed
        doUpdate();
    }//GEN-LAST:event_tf_bar3ActionPerformed

    private void rb_rootProfileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rb_rootProfileActionPerformed
    {//GEN-HEADEREND:event_rb_rootProfileActionPerformed
        doUpdate();
    }//GEN-LAST:event_rb_rootProfileActionPerformed

    private void rb_rpChordTypesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rb_rpChordTypesActionPerformed
    {//GEN-HEADEREND:event_rb_rpChordTypesActionPerformed
        doUpdate();
    }//GEN-LAST:event_rb_rpChordTypesActionPerformed

    private void btn_whatChangedActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_whatChangedActionPerformed
    {//GEN-HEADEREND:event_btn_whatChangedActionPerformed
        LOGGER.info(getTableModel().getWhatChangedString());
    }//GEN-LAST:event_btn_whatChangedActionPerformed

    private void cmb_noteDetailActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmb_noteDetailActionPerformed
    {//GEN-HEADEREND:event_cmb_noteDetailActionPerformed
        doUpdate();
    }//GEN-LAST:event_cmb_noteDetailActionPerformed

    private void spn_tempoStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_tempoStateChanged
    {//GEN-HEADEREND:event_spn_tempoStateChanged
        doUpdate();
    }//GEN-LAST:event_spn_tempoStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_clear;
    private javax.swing.JButton btn_update;
    private javax.swing.JButton btn_whatChanged;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<String> cmb_noteDetail;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbl_Tempo;
    private javax.swing.JLabel lbl_chordSeq;
    private javax.swing.JLabel lbl_info;
    private javax.swing.JList<BassStyle> list_Styles;
    private javax.swing.JRadioButton rb_rootProfile;
    private javax.swing.JRadioButton rb_rpChordTypes;
    private javax.swing.JSpinner spn_barSize;
    private org.jjazz.flatcomponents.api.WheelSpinner spn_tempo;
    private javax.swing.JTable tbl_wbpSources;
    private javax.swing.JTextField tf_bar0;
    private javax.swing.JTextField tf_bar1;
    private javax.swing.JTextField tf_bar2;
    private javax.swing.JTextField tf_bar3;
    // End of variables declaration//GEN-END:variables


}
