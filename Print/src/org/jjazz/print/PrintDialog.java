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
package org.jjazz.print;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.jjazz.song.api.Song;
import org.jjazz.ui.utilities.SingleComponentAspectRatioKeeperLayout;
import org.openide.*;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * Dialog to print a song.
 */
public class PrintDialog extends javax.swing.JDialog
{

    static private final String PREF_PRINT_MODE = "PrintMode";
    static private final String PREF_DEVELOP_LEADSHEET = "DevelopLeadSheet";
    static private PrintDialog INSTANCE;
    private PrinterJob job;
    private PageFormat pageFormat;
    private Pageable pageable;
    private int previewedPageIndex;
    private Song song;
    private static Preferences prefs = NbPreferences.forModule(PrintDialog.class);

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


        // Preferences
        restorePrintMode(prefs.get(PREF_PRINT_MODE, null));
        cb_developLeadSheet.setSelected(prefs.getBoolean(PREF_DEVELOP_LEADSHEET, false));
        
    }

    public void preset(Song song)
    {
        this.song = song;
        job = PrinterJob.getPrinterJob();

        if (pageFormat == null)
        {
            // First default pageFormat should not have too big margins
            pageFormat = checkMargins(job.defaultPage(), 15);
        }

        pageable = new SongPrinter(song, pageFormat);

        pnl_previewComponent.setPageable(pageable, 0);

        printModeChanged();
        previewContextChanged();

        pack();
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
                new PrintAction().actionPerformed(e);
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

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("PAGE_UP"), "PreviousPreviewPageAction");
        contentPane.getActionMap().put("PreviousPreviewPageAction", new PreviousPreviewPageAction());

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("PAGE_DOWN"), "NextPreviewPageAction");
        contentPane.getActionMap().put("NextPreviewPageAction", new NextPreviewPageAction());


        return contentPane;
    }

    // =========================================================================================
    // Private methods
    // =========================================================================================
    private void printModeChanged()
    {
        prefs.put(PREF_PRINT_MODE, getPrintModeString());
        cb_developLeadSheet.setEnabled(!rbtn_printSongStructureOnly.isSelected());

    }

    private void previewContextChanged()
    {
        if (pageable != null)
        {
            lbl_pageNb.setText((previewedPageIndex + 1) + " / " + pageable.getNumberOfPages());
        }
    }

    private String getPrintModeString()
    {
        String s = "both";
        if (rbtn_printChordLeadsheetOnly.isSelected())
        {
            s = "leadsheetOnly";
        } else if (rbtn_printSongStructureOnly.isSelected())
        {
            s = "songStructureOnly";
        }
        return s;
    }

    private void restorePrintMode(String s)
    {
        if ("leadsheetOnly".equals(s))
        {
            rbtn_printChordLeadsheetOnly.setSelected(true);
        } else if ("songStructureOnly".equals(s))
        {
            rbtn_printSongStructureOnly.setSelected(true);
        } else
        {
            rbtn_printBoth.setSelected(true);
        }
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
            if (previewedPageIndex < pageable.getNumberOfPages() - 1)
            {
                previewedPageIndex++;
                pnl_previewComponent.setPageIndex(previewedPageIndex);
            }
            previewContextChanged();
        }

    }

    private class PreviousPreviewPageAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (previewedPageIndex > 0)
            {
                previewedPageIndex--;
                pnl_previewComponent.setPageIndex(previewedPageIndex);
            }
            previewContextChanged();
        }

    }

    private class PrintAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            job.setPageable(pageable);
            job.setJobName(song.getName());

            if (job.printDialog())
            {
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
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        rbtnGroup_printScope = new javax.swing.ButtonGroup();
        pnl_main = new javax.swing.JPanel();
        pnl_settings = new javax.swing.JPanel();
        cb_developLeadSheet = new javax.swing.JCheckBox();
        rbtn_printBoth = new javax.swing.JRadioButton();
        rbtn_printChordLeadsheetOnly = new javax.swing.JRadioButton();
        rbtn_printSongStructureOnly = new javax.swing.JRadioButton();
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

        org.openide.awt.Mnemonics.setLocalizedText(cb_developLeadSheet, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.cb_developLeadSheet.text")); // NOI18N
        cb_developLeadSheet.setToolTipText(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.cb_developLeadSheet.toolTipText")); // NOI18N

        rbtnGroup_printScope.add(rbtn_printBoth);
        rbtn_printBoth.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_printBoth, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.rbtn_printBoth.text")); // NOI18N
        rbtn_printBoth.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_printBothActionPerformed(evt);
            }
        });

        rbtnGroup_printScope.add(rbtn_printChordLeadsheetOnly);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_printChordLeadsheetOnly, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.rbtn_printChordLeadsheetOnly.text")); // NOI18N
        rbtn_printChordLeadsheetOnly.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_printChordLeadsheetOnlyActionPerformed(evt);
            }
        });

        rbtnGroup_printScope.add(rbtn_printSongStructureOnly);
        org.openide.awt.Mnemonics.setLocalizedText(rbtn_printSongStructureOnly, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.rbtn_printSongStructureOnly.text")); // NOI18N
        rbtn_printSongStructureOnly.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                rbtn_printSongStructureOnlyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_settingsLayout = new javax.swing.GroupLayout(pnl_settings);
        pnl_settings.setLayout(pnl_settingsLayout);
        pnl_settingsLayout.setHorizontalGroup(
            pnl_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_settingsLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(pnl_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbtn_printSongStructureOnly)
                    .addComponent(rbtn_printChordLeadsheetOnly)
                    .addComponent(rbtn_printBoth)
                    .addComponent(cb_developLeadSheet))
                .addContainerGap(11, Short.MAX_VALUE))
        );
        pnl_settingsLayout.setVerticalGroup(
            pnl_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_settingsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(rbtn_printBoth)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_printChordLeadsheetOnly)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbtn_printSongStructureOnly)
                .addGap(34, 34, 34)
                .addComponent(cb_developLeadSheet)
                .addContainerGap(145, Short.MAX_VALUE))
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
        btn_previousPage.setLabel(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_previousPage.label")); // NOI18N
        btn_previousPage.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel1.add(btn_previousPage);

        lbl_pageNb.setFont(lbl_pageNb.getFont());
        org.openide.awt.Mnemonics.setLocalizedText(lbl_pageNb, org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.lbl_pageNb.text")); // NOI18N
        jPanel1.add(lbl_pageNb);

        btn_nextPage.setAction(new NextPreviewPageAction());
        btn_nextPage.setFont(btn_nextPage.getFont().deriveFont(btn_nextPage.getFont().getSize()-1f));
        btn_nextPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jjazz/print/resources/RightArrow14.png"))); // NOI18N
        btn_nextPage.setToolTipText(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_nextPage.toolTipText")); // NOI18N
        btn_nextPage.setLabel(org.openide.util.NbBundle.getMessage(PrintDialog.class, "PrintDialog.btn_nextPage.label")); // NOI18N
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
                    .addComponent(pnl_preview, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE))
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
                        .addComponent(pnl_preview, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
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

        btn_print.setAction(new PrintAction());
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
        pageable = new SongPrinter(song, pageFormat);
        previewedPageIndex = Math.min(previewedPageIndex, pageable.getNumberOfPages() - 1);
        pnl_previewComponent.setPageable(pageable, previewedPageIndex);
        previewContextChanged();
    }//GEN-LAST:event_btn_pageSetupActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_cancelActionPerformed
    {//GEN-HEADEREND:event_btn_cancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void rbtn_printBothActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_printBothActionPerformed
    {//GEN-HEADEREND:event_rbtn_printBothActionPerformed
        printModeChanged();
    }//GEN-LAST:event_rbtn_printBothActionPerformed

    private void rbtn_printChordLeadsheetOnlyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_printChordLeadsheetOnlyActionPerformed
    {//GEN-HEADEREND:event_rbtn_printChordLeadsheetOnlyActionPerformed
        printModeChanged();
    }//GEN-LAST:event_rbtn_printChordLeadsheetOnlyActionPerformed

    private void rbtn_printSongStructureOnlyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rbtn_printSongStructureOnlyActionPerformed
    {//GEN-HEADEREND:event_rbtn_printSongStructureOnlyActionPerformed
        printModeChanged();
    }//GEN-LAST:event_rbtn_printSongStructureOnlyActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_nextPage;
    private javax.swing.JButton btn_pageSetup;
    private javax.swing.JButton btn_previousPage;
    private javax.swing.JButton btn_print;
    private javax.swing.JCheckBox cb_developLeadSheet;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lbl_pageNb;
    private javax.swing.JPanel pnl_main;
    private javax.swing.JPanel pnl_preview;
    private org.jjazz.print.PreviewPanel pnl_previewComponent;
    private javax.swing.JPanel pnl_settings;
    private javax.swing.ButtonGroup rbtnGroup_printScope;
    private javax.swing.JRadioButton rbtn_printBoth;
    private javax.swing.JRadioButton rbtn_printChordLeadsheetOnly;
    private javax.swing.JRadioButton rbtn_printSongStructureOnly;
    // End of variables declaration//GEN-END:variables
}
