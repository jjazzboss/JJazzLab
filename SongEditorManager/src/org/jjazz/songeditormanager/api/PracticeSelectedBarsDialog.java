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
package org.jjazz.songeditormanager.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.jjazz.rhythm.api.TempoRange;
import static org.jjazz.ui.flatcomponents.api.FlatIntegerHorizontalSlider.PROP_COLOR_LEFT;
import static org.jjazz.ui.flatcomponents.api.FlatIntegerHorizontalSlider.PROP_COLOR_RIGHT;
import org.jjazz.ui.utilities.api.StringMetrics;
import org.jjazz.ui.utilities.api.Utilities;
import org.openide.windows.WindowManager;

/**
 * Dialog to adjust the parameters of the practice song.
 */
public class PracticeSelectedBarsDialog extends javax.swing.JDialog
{


    private Config result;
    private final Config model;
    private final ConfigPanel configPanel;
    private static final Logger LOGGER = Logger.getLogger(PracticeSelectedBarsDialog.class.getSimpleName());

    /**
     * Creates new form PracticeSelectedBarsDialog.
     *
     * @param defaultValue Can't be null
     */
    public PracticeSelectedBarsDialog(Config defaultValue)
    {
        super(WindowManager.getDefault().getMainWindow(), true);
        checkNotNull(defaultValue);
        initComponents();

        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        Utilities.installEnterKeyAction(this, () -> btn_CreateActionPerformed(null));
        Utilities.installEscapeKeyAction(this, () -> btn_CancelActionPerformed(null));


        model = new Config(defaultValue.tempoStart, defaultValue.tempoEnd, defaultValue.nbSteps);
        configPanel = new ConfigPanel();
        configPanel.setModel(model);
        configPanelContainer.add(configPanel);
        configPanel.setToolTipText(configPanelContainer.getToolTipText());

        lbl_nbSteps.setText(String.valueOf(model.nbSteps));
        slider_nbSteps.setValue(model.nbSteps);

        pack();
    }

    /**
     * The resulting parameters if user exited with OK, null otherwise.
     *
     * @return Can be null
     */
    public Config getResult()
    {
        return result;
    }


    // =========================================================================
    // Private methods
    // =========================================================================    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        btn_Create = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        lbl_help = new javax.swing.JLabel();
        configPanelContainer = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        slider_nbSteps = new javax.swing.JSlider();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 0), new java.awt.Dimension(3, 32767));
        lbl_nbSteps = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btn_Create, org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.btn_Create.text")); // NOI18N
        btn_Create.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CreateActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btn_Cancel, org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.btn_Cancel.text")); // NOI18N
        btn_Cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btn_CancelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lbl_help, org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.lbl_help.text")); // NOI18N

        configPanelContainer.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        configPanelContainer.setToolTipText(org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.configPanelContainer.toolTipText")); // NOI18N
        configPanelContainer.setOpaque(false);
        configPanelContainer.setLayout(new java.awt.CardLayout());

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        slider_nbSteps.setMajorTickSpacing(1);
        slider_nbSteps.setMaximum(20);
        slider_nbSteps.setMinimum(2);
        slider_nbSteps.setSnapToTicks(true);
        slider_nbSteps.setToolTipText(org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.slider_nbSteps.toolTipText")); // NOI18N
        slider_nbSteps.setValue(6);
        slider_nbSteps.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                slider_nbStepsStateChanged(evt);
            }
        });
        slider_nbSteps.addMouseWheelListener(new java.awt.event.MouseWheelListener()
        {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt)
            {
                slider_nbStepsMouseWheelMoved(evt);
            }
        });
        jPanel1.add(slider_nbSteps);
        jPanel1.add(filler1);

        org.openide.awt.Mnemonics.setLocalizedText(lbl_nbSteps, org.openide.util.NbBundle.getMessage(PracticeSelectedBarsDialog.class, "PracticeSelectedBarsDialog.lbl_nbSteps.text")); // NOI18N
        lbl_nbSteps.setToolTipText(slider_nbSteps.getToolTipText());
        jPanel1.add(lbl_nbSteps);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(configPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(btn_Create)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_help)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Cancel)
                            .addComponent(btn_Create)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbl_help)
                        .addGap(18, 18, 18)
                        .addComponent(configPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_CreateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CreateActionPerformed
    {//GEN-HEADEREND:event_btn_CreateActionPerformed
        result = model;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btn_CreateActionPerformed

    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btn_CancelActionPerformed
    {//GEN-HEADEREND:event_btn_CancelActionPerformed
        result = null;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void slider_nbStepsStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_slider_nbStepsStateChanged
    {//GEN-HEADEREND:event_slider_nbStepsStateChanged
        if (slider_nbSteps.getValueIsAdjusting())
        {
            return;
        }
        model.nbSteps = slider_nbSteps.getValue();
        lbl_nbSteps.setText(String.valueOf(model.nbSteps));
        configPanel.setModel(new Config(model.tempoStart, model.tempoEnd, model.nbSteps));

    }//GEN-LAST:event_slider_nbStepsStateChanged

    private void slider_nbStepsMouseWheelMoved(java.awt.event.MouseWheelEvent evt)//GEN-FIRST:event_slider_nbStepsMouseWheelMoved
    {//GEN-HEADEREND:event_slider_nbStepsMouseWheelMoved
        int value = slider_nbSteps.getValue();
        if (evt.getWheelRotation() < 0 && value < slider_nbSteps.getMaximum())
        {
            slider_nbSteps.setValue(value + 1);
        } else if (evt.getWheelRotation() > 0 && value > slider_nbSteps.getMinimum())
        {
            slider_nbSteps.setValue(value - 1);
        }
    }//GEN-LAST:event_slider_nbStepsMouseWheelMoved


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Create;
    private javax.swing.JPanel configPanelContainer;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbl_help;
    private javax.swing.JLabel lbl_nbSteps;
    private javax.swing.JSlider slider_nbSteps;
    // End of variables declaration//GEN-END:variables


    // =========================================================================
    // Inner classes
    // =========================================================================    
    static public class Config
    {

        public int tempoStart, tempoEnd;
        public int nbSteps;

        public Config(int tempoStart, int tempoEnd, int nbSteps)
        {
            checkArgument(nbSteps >= 2, "nbSteps=%s", nbSteps);
            this.tempoStart = tempoStart;
            this.tempoEnd = tempoEnd;
            this.nbSteps = nbSteps;
        }

        /**
         * Calculate the tempo for stepIndex.
         * <p>
         * If stepIndex==0 then return tempoStart, if stepIndex==nbSteps-1 then return tempoEnd, else do interpolation.
         *
         * @param stepIndex
         * @return
         */
        public int getTempo(int stepIndex)
        {
            checkArgument(stepIndex >= 0 && stepIndex < nbSteps, "stepIndex=%s nbSteps=%s", stepIndex, nbSteps);
            float tempoStep = ((float) tempoEnd - tempoStart) / (nbSteps - 1);
            if (stepIndex == 0)
            {
                return tempoStart;
            } else if (stepIndex == nbSteps - 1)
            {
                return tempoEnd;
            }
            return Math.round(tempoStart + stepIndex * tempoStep);
        }

        @Override
        public String toString()
        {
            return "[" + tempoStart + "," + tempoEnd + "," + nbSteps + "]";
        }

    }

    /**
     * A graphical component to visualize the start/end tempo and the intermediate steps, and to easily change the start/end
     * tempo.
     */
    static private class ConfigPanel extends JPanel
    {

        private final static Color COLUMN_COLOR_BOTTOM = new Color(3, 133, 255);
        private final static Color COLUMN_COLOR_TOP = new Color(116, 73, 255);
        private final static Color TEMPO_COLOR = Color.WHITE;
        private final static Color HANDLE_COLOR = COLUMN_COLOR_BOTTOM.darker();
        private final static int H_GAP = 10;
        private final static int V_GAP = 3;
        private final static int HANDLE_RADIUS = 6;
        private Config model;
        private final List<Rectangle2D.Float> columns;
        private final Ellipse2D.Float handleStart;
        private final Ellipse2D.Float handleEnd;
        private final MyMouseListener mouseListener;


        public ConfigPanel()
        {

            columns = new ArrayList<>();
            handleStart = new Ellipse2D.Float();
            handleEnd = new Ellipse2D.Float();

            mouseListener = new MyMouseListener();
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseListener);
            addMouseWheelListener(mouseListener);
        }

        public void setModel(Config config)
        {
            if (Objects.equals(model, config))
            {
                return;
            }
            model = new Config(config.tempoStart, config.tempoEnd, config.nbSteps);
            repaint();
        }

        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(400, 400);
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);


            updateShapes(model);


            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font font = g2.getFont();
            font = font.deriveFont(font.getSize2D() - 2f);
            g2.setFont(font);
            StringMetrics sm = new StringMetrics(g2, font);


            // Draw columns
            for (int i = 0; i < columns.size(); i++)
            {
                var colShape = columns.get(i);
                GradientPaint gp = new GradientPaint(colShape.x, colShape.y, COLUMN_COLOR_TOP, colShape.x, colShape.y + colShape.height - 1, COLUMN_COLOR_BOTTOM);
                g2.setPaint(gp);

                if (i == 0 || i == columns.size() - 1)
                {
                    g2.fill(colShape);
                } else
                {
                    g2.draw(colShape);
                }


                // Draw tempo
                String txt = String.valueOf(model.getTempo(i));
                var txtBounds = sm.getLogicalBoundsNoLeading(txt);
                float x = colShape.x + colShape.width / 2 - (float) txtBounds.getWidth() / 2;
                float y = colShape.y + HANDLE_RADIUS + 2 + (float) txtBounds.getHeight();
                g2.setColor(TEMPO_COLOR);
                g2.drawString(txt, x, y);
            }

            // Draw handles
//            g2.setColor(HANDLE_COLOR);
//            g2.fill(handleStart);
//            g2.fill(handleEnd);
        }

        /**
         * Recompute the shapes for the specified config and current object size.
         *
         * @param config
         */
        private void updateShapes(Config config)
        {
            // LOGGER.severe("updateShapes() -- config=" + config);
            Rectangle r = Utilities.getUsableArea(this);
            float colWidth = (r.width - 2 * H_GAP - (config.nbSteps - 1) * H_GAP) / (float) config.nbSteps;
            int maxColHeight = r.height - 2 * V_GAP;
            int maxTempo = Math.round(Math.max(config.tempoEnd, config.tempoStart) * 1.1f);
            float tempo2HeightRatio = (float) maxColHeight / maxTempo;
            float x = r.x + H_GAP;
            float y;


            // Add/remove column shapes if required   
            int delta = config.nbSteps - columns.size();
            if (delta < 0)
            {
                columns.subList(0, -delta).clear();
            } else if (delta > 0)
            {
                while (delta > 0)
                {
                    columns.add(new Rectangle2D.Float());
                    delta--;
                }
            }

            // Update shapes
            for (int i = 0; i < config.nbSteps; i++)
            {
                // Columns
                int colTempo = config.getTempo(i);
                float colHeight = tempo2HeightRatio * colTempo;
                y = r.y + V_GAP + maxColHeight - colHeight;
                columns.get(i).setRect(x, y, colWidth, colHeight);

                // Handles
                if (i == 0 || i == config.nbSteps - 1)
                {
                    var handle = (i == 0) ? handleStart : handleEnd;
                    handle.setFrame(x + colWidth / 2 - HANDLE_RADIUS, y - HANDLE_RADIUS, 2 * HANDLE_RADIUS, 2 * HANDLE_RADIUS);
                }

                x += colWidth + H_GAP;
            }
        }

        private class MyMouseListener extends MouseAdapter
        {

            private boolean dragging = false;
            private int saveStartDragY = -1;
            private int saveStartTempo = -1;
            private int saveEndTempo = -1;

            @Override
            public void mouseMoved(MouseEvent e)
            {
                boolean b = isInsideFirstCol(e) || isInsideLastCol(e);
                setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                int newTempoStart = model.tempoStart;
                int newTempoEnd = model.tempoEnd;
                boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
                int step = ctrl ? 4 : 1;

                if (e.getX() < getSize().width / 2)
                {
                    if (e.getWheelRotation() > 0)
                    {
                        newTempoStart = Math.max(TempoRange.TEMPO_MIN, model.tempoStart - step);
                    } else
                    {
                        newTempoStart = Math.min(TempoRange.TEMPO_MAX, model.tempoStart + step);
                    }
                } else if (e.getWheelRotation() > 0)
                {
                    newTempoEnd = Math.max(TempoRange.TEMPO_MIN, model.tempoEnd - step);
                } else
                {
                    newTempoEnd = Math.min(TempoRange.TEMPO_MAX, model.tempoEnd + step);
                }

                setModel(new Config(newTempoStart, newTempoEnd, model.nbSteps));
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (isInsideFirstCol(e))
                {
                    saveStartDragY = e.getY();
                    saveStartTempo = model.tempoStart;
                    saveEndTempo = -1;
                } else if (isInsideLastCol(e))
                {
                    saveStartDragY = e.getY();
                    saveEndTempo = model.tempoEnd;
                    saveStartTempo = -1;
                } else
                {
                    saveStartTempo = -1;
                    saveStartDragY = -1;
                    saveEndTempo = -1;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                if (saveStartDragY == -1)
                {
                    return;
                }

                int newTempoStart = model.tempoStart;
                int newTempoEnd = model.tempoEnd;

                int yDelta = e.getY() - saveStartDragY;
                if (saveStartTempo > 0)
                {
                    newTempoStart = saveStartTempo - yDelta / 3;
                    newTempoStart = Math.min(newTempoStart, TempoRange.TEMPO_MAX);
                    newTempoStart = Math.max(newTempoStart, TempoRange.TEMPO_MIN);
                } else if (saveEndTempo > 0)
                {
                    newTempoEnd = saveEndTempo - yDelta / 3;
                    newTempoEnd = Math.min(newTempoEnd, TempoRange.TEMPO_MAX);
                    newTempoEnd = Math.max(newTempoEnd, TempoRange.TEMPO_MIN);
                }

                setModel(new Config(newTempoStart, newTempoEnd, model.nbSteps));

            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                saveStartTempo = -1;
                saveStartDragY = -1;
                saveEndTempo = -1;
            }

            private boolean isInsideFirstCol(MouseEvent e)
            {
                return columns.get(0).contains(e.getPoint());
            }

            private boolean isInsideLastCol(MouseEvent e)
            {
                return columns.get(columns.size() - 1).contains(e.getPoint());
            }
        }


    }


}
