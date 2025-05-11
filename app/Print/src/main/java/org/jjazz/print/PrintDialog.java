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
package org.jjazz.print;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongUtilities;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.uiutilities.api.SingleComponentAspectRatioKeeperLayout;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * Dialog to print a song.
 */
public class PrintDialog extends javax.swing.JDialog implements ChangeListener
{

    static private PrintDialog INSTANCE;
    private PrinterJob job;
    private PageFormat pageFormat;
    private final int saveLeadSheetOrientation = PageFormat.PORTRAIT;
    private final int saveSongStructureOrientation = PageFormat.LANDSCAPE;
    private LeadSheetPrinter leadsheetPrinter;
    private SongStructurePrinter songStructurePrinter;
    private Pageable currentPageable;
    private int previewedPageIndex;
    private Song workSong;
    private Song refSong;
    private CL_Editor actualClEditor;
    private SS_Editor actualSsEditor;

    public static PrintDialog getInstance()
    {
        synchronized (PrintDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new PrintDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    private PrintDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();


        // Use our own layout for the preview panel
        var layout = new SingleComponentAspectRatioKeeperLayout();
        pnl_preview.setLayout(layout);

    }

    /**
     * Use this method to initialize dialog before being made visible.
     *
     * @param sg
     */
    public void preset(Song sg)
    {
        if (sg == null)
        {
            throw new IllegalArgumentException("sg=" + sg);
        }

        this.refSong = sg;
        this.workSong = buildWorkSong();        // Can be simplified or developped song

        job = PrinterJob.getPrinterJob();

        if (pageFormat == null)
        {
            // First default pageFormat should not have too big margins
            pageFormat = checkMargins(job.defaultPage(), 15);
        }


        // Create/update our LeadSheetPrinter pageable
        var res = SongEditorManager.getDefault().getSongEditorSet(refSong);
        actualClEditor = res.getCL_EditorTc().getEditor();
        actualSsEditor = res.getSS_EditorTc().getEditor();
        int zoomVFactor = CL_EditorClientProperties.getZoomYFactor(refSong);
        int nbColumns = actualClEditor.getNbColumns();

        // Update UI
        spn_nbColumns.setValue(nbColumns);
        spn_zoomVFactor.setValue(zoomVFactor - 50);


        previewedPageIndex = 0;


        printTargetChanged();
        previewContextChanged();

        pack();
    }

    private void updateSongStructurePrinter()
    {
        if (songStructurePrinter != null)
        {
            songStructurePrinter.removeChangeListener(this);
        }
        songStructurePrinter = new SongStructurePrinter(actualSsEditor, workSong, pageFormat);
        currentPageable = songStructurePrinter;
        songStructurePrinter.addChangeListener(this);
    }

    private void updateLeadSheetPrinter(int zoomVFactor, int nbColumns)
    {
        if (leadsheetPrinter != null)
        {
            leadsheetPrinter.removeChangeListener(this);
        }
        leadsheetPrinter = new LeadSheetPrinter(actualClEditor, workSong, pageFormat, zoomVFactor, nbColumns);
        currentPageable = leadsheetPrinter;
        leadsheetPrinter.addChangeListener(this);
    }

    private Song buildWorkSong()
    {
        Song res = refSong;


        if (cb_simplifyLeadSheet.isSelected())
        {
            res = SongUtilities.getSimplifiedLeadSheet(refSong, false);
        }


        if (cb_developLeadSheet.isSelected())
        {
            res = SongUtilities.getLinearizedSong(res, false);
        }


        if (res != refSong)
        {
            res.getClientProperties().set(refSong.getClientProperties());
        }
        
        return res;
    }

    /**
     * Overridden to add global key bindings
     *
     * @return
     */
    @Override
    protected JRootPane createRootPane()
    {
        JRootPane contentPane = new JRootPane();
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");
        contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                new DoPrintAction().actionPerformed(e);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                btn_cancelActionPerformed(e);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("PAGE_UP"),
                "PreviousPreviewPageAction");
        contentPane.getActionMap().put("PreviousPreviewPageAction", new PreviousPreviewPageAction());

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("PAGE_DOWN"),
                "NextPreviewPageAction");
        contentPane.getActionMap().put("NextPreviewPageAction", new NextPreviewPageAction());

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("HOME"), "FirstPageAction");
        contentPane.getActionMap().put("FirstPageAction", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setPreviewPageIndex(0);
            }

        });
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("END"), "LastPageAction");
        contentPane.getActionMap().put("LastPageAction", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setPreviewPageIndex(leadsheetPrinter.getNumberOfPages() - 1);
            }

        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("PLUS"), "BarHeightPlus");
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ADD"), "BarHeightPlus");
        contentPane.getActionMap().put("BarHeightPlus", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                spn_zoomVFactor.setValue(spn_zoomVFactor.getModel().getNextValue());
            }

        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("MINUS"), "BarHeightMinus");
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("SUBTRACT"), "BarHeightMinus");
        contentPane.getActionMap().put("BarHeightMinus", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                spn_zoomVFactor.setValue(spn_zoomVFactor.getModel().getPreviousValue());
            }

        });

        return contentPane;
    }

    // =========================================================================================
    // ChangeListener implementation
    // =========================================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        pnl_previewComponent.revalidate();
        pnl_previewComponent.repaint();
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
    private void printTargetChanged()
    {
        UIUtilities.setRecursiveEnabled(rbtn_printChordLeadsheet.isSelected(), pnl_leadsheet_settings);
        if (rbtn_printChordLeadsheet.isSelected())
        {
            updateLeadSheetPrinter((Integer) spn_zoomVFactor.getValue() + 50, (Integer) spn_nbColumns.getValue());
        } else
        {
            updateSongStructurePrinter();
        }
        previewedPageIndex = 0;
        pnl_previewComponent.setPageable(currentPageable, previewedPageIndex);
        previewContextChanged();

    }

    private void previewContextChanged()
    {
        lbl_pageNb.setText((previewedPageIndex + 1) + " / " + currentPageable.getNumberOfPages());
        btn_nextPage.setEnabled(previewedPageIndex < (currentPageable.getNumberOfPages() - 1));
        btn_previousPage.setEnabled(previewedPageIndex > 0);
    }

    private void songOrPageFormatChanged()
    {
        if (rbtn_printChordLeadsheet.isSelected())
        {
            updateLeadSheetPrinter(leadsheetPrinter.getEditorZoomFactor(), leadsheetPrinter.getNbColumns());
        } else
        {
            updateSongStructurePrinter();
        }

        previewedPageIndex = Math.min(previewedPageIndex, currentPageable.getNumberOfPages() - 1);
        pnl_previewComponent.setPageable(currentPageable, previewedPageIndex);
        previewContextChanged();
    }

    private void setPreviewPageIndex(int pgIndex)
    {
        if (pgIndex >= 0 && pgIndex < currentPageable.getNumberOfPages())
        {
            previewedPageIndex = pgIndex;
            pnl_previewComponent.setPageIndex(previewedPageIndex);
            previewContextChanged();
        }
    }

    private void setZoomVFactor(int value)
    {
        leadsheetPrinter.setEditorZoomVFactor(value + 50);

        // This might have changed total nb of pages
        if (previewedPageIndex >= leadsheetPrinter.getNumberOfPages())
        {
            setPreviewPageIndex(leadsheetPrinter.getNumberOfPages() - 1);
        }
        previewContextChanged();
    }

    /**
     * Ensure that pf does not have margins bigger than marginMax.
     *
     * @param pf
     * @param MarginMmMax In millimeters
     * @return A new PageFormat with margins set at marginMax or below.
     */
    private PageFormat checkMargins(PageFormat pf, int MarginMmMax)
    {

        double left = PrintUtil.getLeftMargin(pf.getPaper());
        double right = PrintUtil.getRightMargin(pf.getPaper());
        double top = PrintUtil.getTopMargin(pf.getPaper());
        double bottom = PrintUtil.getBottomMargin(pf.getPaper());


        if (PrintUtil.convertPointsToMm(left) > MarginMmMax)
        {
            left = PrintUtil.convertMmToPoints(MarginMmMax);
            right = left;
        }


        if (PrintUtil.convertPointsToMm(top) > MarginMmMax)
        {
            top = PrintUtil.convertMmToPoints(MarginMmMax);
            bottom = top;
        }


        PrintUtil.setPageMargins(pf, top, left, bottom, right);
        return job.validatePage(pf);


    }

    private class NextPreviewPageAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            setPreviewPageIndex(previewedPageIndex + 1);
        }

    }

    private class PreviousPreviewPageAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            setPreviewPageIndex(previewedPageIndex - 1);
        }

    }

    private class DoPrintAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            job.setPageable(currentPageable);
            job.setJobName(workSong.getName());

            if (job.printDialog())
            {
                // Log event
                Analytics.logEvent("Print");

                try
                {
                    job.print();
                } catch (PrinterException ex)
                {
                    String msg = ex.getLocalizedMessage();  // Can be null in some cases
                    if (msg != null && !msg.isBlank())
                    {
                        NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notify(d);
                    }
                    return;
                }
                setVisible(false);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        rbtnGroup_printScope = new javax.swing.ButtonGroup();
        pnl_main = new javax.swing.JPanel();
        pnl_settings = new javax.swing.JPanel();
        rbtn_printChordLeadsheet = new javax.swing.JRadioButton();
        rbtn_printSongStructure = new javax.swing.JRadioButton();
        pnl_leadsheet_settings = new javax.swing.JPanel();
        cb_developLeadSheet = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        cb_simplifyLeadSheet = new javax.swing.JCheckBox();
        spn_nbColumns = new javax.swing.JSpinner();
        spn_zoomVFactor = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        pnl_preview = new javax.swing.JPanel();
        pnl_previewComponent = new org.jjazz.print.PreviewPanel();
        jPanel1 = new javax.swing.JPanel();
        btn_previousPage = new javax.swing.JButton();
        lbl_pageNb = new javax.swing.JLabel();
        btn_nextPage = new javax.swing.JButton();
        btn_cancel = new javax.swing.JButton();
        btn_print = new javax.swing.JButton();
        btn_pageSetup = new javax.swing.JButton();

        setTitle(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosed(java.awt.event.WindowEvent evt)
            {
                formWindowClosed(evt);
            }
        });

        rbtnGroup_printScope.add(rbtn_printChordLeadsheet);
        rbtn_printChordLeadsheet.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_printChordLeadsheet, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.rbtn_printChordLeadsheet.text")); // NOI18N
        rbtn_printChordLeadsheet.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_printChordLeadsheetActionPerformed(evt);
            }
        });

        rbtnGroup_printScope.add(rbtn_printSongStructure);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_printSongStructure, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.rbtn_printSongStructure.text")); // NOI18N
        rbtn_printSongStructure.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_printSongStructureActionPerformed(evt);
            }
        });

        pnl_leadsheet_settings.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.pnl_leadsheet_settings.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_developLeadSheet, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.cb_developLeadSheet.text")); // NOI18N
        cb_developLeadSheet.setToolTipText(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.cb_developLeadSheet.toolTipText")); // NOI18N
        cb_developLeadSheet.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_developLeadSheetActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cb_simplifyLeadSheet, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.cb_simplifyLeadSheet.text")); // NOI18N
        cb_simplifyLeadSheet.setToolTipText(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.cb_simplifyLeadSheet.toolTipText")); // NOI18N
        cb_simplifyLeadSheet.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cb_simplifyLeadSheetActionPerformed(evt);
            }
        });

        spn_nbColumns.setModel(new javax.swing.SpinnerNumberModel(4, 1, 16, 1));
        spn_nbColumns.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_nbColumnsStateChanged(evt);
            }
        });

        spn_zoomVFactor.setModel(new javax.swing.SpinnerNumberModel(0, -50, 50, 5));
        spn_zoomVFactor.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                spn_zoomVFactorStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout pnl_leadsheet_settingsLayout = new javax.swing.GroupLayout(pnl_leadsheet_settings);
        pnl_leadsheet_settings.setLayout(pnl_leadsheet_settingsLayout);
        pnl_leadsheet_settingsLayout.setHorizontalGroup(
            pnl_leadsheet_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_leadsheet_settingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_leadsheet_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cb_developLeadSheet)
                    .addGroup(pnl_leadsheet_settingsLayout.createSequentialGroup()
                        .addComponent(spn_zoomVFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2))
                    .addGroup(pnl_leadsheet_settingsLayout.createSequentialGroup()
                        .addComponent(spn_nbColumns, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3))
                    .addComponent(cb_simplifyLeadSheet))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnl_leadsheet_settingsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spn_nbColumns, spn_zoomVFactor});

        pnl_leadsheet_settingsLayout.setVerticalGroup(
            pnl_leadsheet_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_leadsheet_settingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cb_developLeadSheet)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cb_simplifyLeadSheet)
                .addGap(18, 18, 18)
                .addGroup(pnl_leadsheet_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_nbColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_leadsheet_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spn_zoomVFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnl_settingsLayout = new javax.swing.GroupLayout(pnl_settings);
        pnl_settings.setLayout(pnl_settingsLayout);
        pnl_settingsLayout.setHorizontalGroup(
            pnl_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_settingsLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(pnl_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbtn_printSongStructure)
                    .addComponent(rbtn_printChordLeadsheet)
                    .addComponent(pnl_leadsheet_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );
        pnl_settingsLayout.setVerticalGroup(
            pnl_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_settingsLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(rbtn_printChordLeadsheet)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_printSongStructure)
                .addGap(37, 37, 37)
                .addComponent(pnl_leadsheet_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(272, Short.MAX_VALUE))
        );

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.jLabel1.text")); // NOI18N

        pnl_preview.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        pnl_preview.setLayout(new javax.swing.BoxLayout(pnl_preview, javax.swing.BoxLayout.LINE_AXIS));

        pnl_previewComponent.setBackground(new java.awt.Color(255, 255, 255));
        pnl_previewComponent.setLayout(null);
        pnl_preview.add(pnl_previewComponent);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 0));

        btn_previousPage.setAction(new PreviousPreviewPageAction());
        btn_previousPage.setFont(btn_previousPage.getFont().deriveFont(btn_previousPage.getFont().getSize()-1f));
        btn_previousPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/print/resources/LeftArrow14.png"))); // NOI18N
        btn_previousPage.setToolTipText(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_previousPage.toolTipText")); // NOI18N
        btn_previousPage.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel1.add(btn_previousPage);

        lbl_pageNb.setFont(lbl_pageNb.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_pageNb, "jLabel2"); // NOI18N
        jPanel1.add(lbl_pageNb);

        btn_nextPage.setAction(new NextPreviewPageAction());
        btn_nextPage.setFont(btn_nextPage.getFont().deriveFont(btn_nextPage.getFont().getSize()-1f));
        btn_nextPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/print/resources/RightArrow14.png"))); // NOI18N
        btn_nextPage.setToolTipText(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_nextPage.toolTipText")); // NOI18N
        btn_nextPage.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel1.add(btn_nextPage);

        javax.swing.GroupLayout pnl_mainLayout = new javax.swing.GroupLayout(pnl_main);
        pnl_main.setLayout(pnl_mainLayout);
        pnl_mainLayout.setHorizontalGroup(
            pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_mainLayout.createSequentialGroup()
                .addComponent(pnl_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(pnl_preview, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_mainLayout.setVerticalGroup(
            pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_mainLayout.createSequentialGroup()
                        .addGroup(pnl_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnl_mainLayout.createSequentialGroup()
                                .addGap(9, 9, 9)
                                .addComponent(jLabel1)
                                .addGap(11, 11, 11))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_mainLayout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addComponent(pnl_preview, javax.swing.GroupLayout.DEFAULT_SIZE, 521, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(jSeparator1)))
            .addGroup(pnl_mainLayout.createSequentialGroup()
                .addComponent(pnl_settings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.openide.awt.Mnemonics.setLocalizedText(btn_cancel, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_cancel.text")); // NOI18N
        btn_cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_cancelActionPerformed(evt);
            }
        });

        btn_print.setAction(new DoPrintAction());
        org.openide.awt.Mnemonics.setLocalizedText(btn_print, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_print.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_pageSetup, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_pageSetup.text")); // NOI18N
        btn_pageSetup.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_pageSetupActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnl_main, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_pageSetup)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_print)
                .addGap(18, 18, 18)
                .addComponent(btn_cancel)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btn_cancel, btn_print});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnl_main, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_pageSetup)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_print)
                            .addComponent(btn_cancel))
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_pageSetupActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_pageSetupActionPerformed
    {//GEN-HEADEREND:event_btn_pageSetupActionPerformed
        pageFormat = job.pageDialog(pageFormat);
        pageFormat = job.validatePage(pageFormat);
        songOrPageFormatChanged();
    }//GEN-LAST:event_btn_pageSetupActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void rbtn_printChordLeadsheetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_printChordLeadsheetActionPerformed
    {//GEN-HEADEREND:event_rbtn_printChordLeadsheetActionPerformed
        printTargetChanged();
    }//GEN-LAST:event_rbtn_printChordLeadsheetActionPerformed

    private void rbtn_printSongStructureActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_printSongStructureActionPerformed
    {//GEN-HEADEREND:event_rbtn_printSongStructureActionPerformed
        printTargetChanged();
    }//GEN-LAST:event_rbtn_printSongStructureActionPerformed

    private void cb_developLeadSheetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_developLeadSheetActionPerformed
    {//GEN-HEADEREND:event_cb_developLeadSheetActionPerformed
        workSong = buildWorkSong();
        songOrPageFormatChanged();
    }//GEN-LAST:event_cb_developLeadSheetActionPerformed

    private void cb_simplifyLeadSheetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cb_simplifyLeadSheetActionPerformed
    {//GEN-HEADEREND:event_cb_simplifyLeadSheetActionPerformed
        workSong = buildWorkSong();
        songOrPageFormatChanged();
    }//GEN-LAST:event_cb_simplifyLeadSheetActionPerformed

    private void spn_nbColumnsStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_nbColumnsStateChanged
    {//GEN-HEADEREND:event_spn_nbColumnsStateChanged
        if (leadsheetPrinter != null)
        {
            leadsheetPrinter.setNbColumns((int) spn_nbColumns.getValue());
        }
    }//GEN-LAST:event_spn_nbColumnsStateChanged

    private void spn_zoomVFactorStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spn_zoomVFactorStateChanged
    {//GEN-HEADEREND:event_spn_zoomVFactorStateChanged
        if (leadsheetPrinter != null)
        {
            setZoomVFactor((int) spn_zoomVFactor.getValue());
        }
    }//GEN-LAST:event_spn_zoomVFactorStateChanged

    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed
        if (leadsheetPrinter != null)
        {
            leadsheetPrinter.cleanup();
        }
    }//GEN-LAST:event_formWindowClosed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_nextPage;
    private javax.swing.JButton btn_pageSetup;
    private javax.swing.JButton btn_previousPage;
    private javax.swing.JButton btn_print;
    private javax.swing.JCheckBox cb_developLeadSheet;
    private javax.swing.JCheckBox cb_simplifyLeadSheet;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lbl_pageNb;
    private javax.swing.JPanel pnl_leadsheet_settings;
    private javax.swing.JPanel pnl_main;
    private javax.swing.JPanel pnl_preview;
    private org.jjazz.print.PreviewPanel pnl_previewComponent;
    private javax.swing.JPanel pnl_settings;
    private javax.swing.ButtonGroup rbtnGroup_printScope;
    private javax.swing.JRadioButton rbtn_printChordLeadsheet;
    private javax.swing.JRadioButton rbtn_printSongStructure;
    private javax.swing.JSpinner spn_nbColumns;
    private javax.swing.JSpinner spn_zoomVFactor;
    // End of variables declaration//GEN-END:variables
}
