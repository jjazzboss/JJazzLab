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
package org.jjazz.spteditor.api;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.spi.RpEditorComponent;

/**
 * RpEditor combines a label with an editor component adapted to the type of RhythmParameter.<br>
 * <p>
 */
public final class RpEditor extends JPanel implements PropertyChangeListener
{

    /**
     * This property change event must be fired each time value is modified by the editor.
     */
    public static final String PROP_RP_VALUE = "rpValue";        

    public static final Color MULTI_FOREGROUND_COLOR = new Color(121, 21, 42);      // Dark brown/red

    private SongPart sptModel;
    private RhythmParameter<?> rpModel;
    private boolean isMultiValueMode = false;
    private RpEditorComponent editorComponent;
    private static final Logger LOGGER = Logger.getLogger(RpEditor.class.getSimpleName());

    /**
     * Initialize the RpEditor for the specified RhythmParameter with the specified editor.
     * <p>
     *
     * @param spt
     * @param rp
     * @param editor
     */
    public RpEditor(SongPart spt, RhythmParameter<?> rp, RpEditorComponent editor)
    {
        if (spt == null || rp == null || editor == null)
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp + " editor=" + editor);
        }
        sptModel = spt;
        rpModel = rp;
        editorComponent = editor;
        editorComponent.addPropertyChangeListener(this);

        // Prepare UI
        initComponents();
        pnl_placeHolder.remove(lbl_placeHolder);  // Used only to properly visualize design when using the Netbeans UI form designer!
        pnl_placeHolder.add(editorComponent);
        if (sptModel != null)
        {
            setHighlighted(false);
        }
        lbl_rpName.setText(rpModel.getDisplayName().toLowerCase());
        lbl_rpName.setToolTipText(rpModel.getDescription());
    }

    public void cleanup()
    {
        editorComponent.cleanup();
    }

    public void updateEditorValue(Object value)
    {
        editorComponent.updateEditorValue(value);
    }

    public Object getEditorValue()
    {
        return editorComponent.getEditorValue();
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        lbl_rpName.setEnabled(b);
        editorComponent.setEnabled(b);
    }

    public final SongPart getSptModel()
    {
        return sptModel;
    }

    public final RhythmParameter<?> getRpModel()
    {
        return rpModel;
    }

    public JLabel getRpNameLabel()
    {
        return lbl_rpName;
    }

    /**
     * Fix the width of the column used for the RpNames.
     *
     * @param w
     */
    public void setRpNameColumnWidth(int w)
    {
        pnl_rpName.setFixedPreferredWidth(w);
    }

    /**
     * Change graphics to show that the displayed value correspond to the multi-value mode.
     *
     * @param b
     */
    public void setMultiValueMode(boolean b)
    {
        if (b == isMultiValueMode)
        {
            return;
        }
        isMultiValueMode = b;
        editorComponent.showMultiValueMode(isMultiValueMode);
    }

    public boolean isMultiValueMode()
    {
        return isMultiValueMode;
    }

    public void setHighlighted(boolean b)
    {
        String txt = lbl_rpName.getText();
        txt = txt.replace("<HTML><U>", "");
        txt = txt.replace("</U></HTML>", "");
        if (b)
        {
            lbl_rpName.setText("<HTML><U>" + txt + "</U></HTML>");
        } else
        {
            lbl_rpName.setText(txt);
        }
    }

    public boolean isHighlighted()
    {
        return lbl_rpName.getText().contains("</U></HTML>");
    }

    /**
     * Show multi value mode on a JComponent using font attributes (italic, color...).
     *
     * @param b MultiValueMode value
     * @param jc
     */
    static public void showMultiModeUsingFont(boolean b, JComponent jc)
    {
        Font f = jc.getFont();
        if (!b)
        {
            f = f.deriveFont(Font.PLAIN);
        } else
        {
            f = f.deriveFont(Font.ITALIC);
        }
        jc.setFont(f);
    }
    //------------------------------------------------------------------------------
    // PropertyChangeListener interface
    //------------------------------------------------------------------------------

    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        if (evt.getSource() == editorComponent)
        {
            if (PROP_RP_VALUE.equals(evt.getPropertyName()))
            {
                // Forward the change event to our listeners
                firePropertyChange(PROP_RP_VALUE, null, evt.getNewValue());
            }
        }
    }

    // -----------------------------------------------------------------------------
    // Private functions
    // -----------------------------------------------------------------------------    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        pnl_placeHolder = new javax.swing.JPanel();
        lbl_placeHolder = new javax.swing.JLabel();
        pnl_rpName = new org.jjazz.flatcomponents.api.FixedPreferredWidthPanel();
        lbl_rpName = new javax.swing.JLabel();

        setMaximumSize(new java.awt.Dimension(30000, 3000));

        pnl_placeHolder.setLayout(new javax.swing.BoxLayout(pnl_placeHolder, javax.swing.BoxLayout.LINE_AXIS));

        lbl_placeHolder.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_placeHolder, "Editor Placeholder"); // NOI18N
        pnl_placeHolder.add(lbl_placeHolder);

        pnl_rpName.setFixedPreferredWidth(60);
        pnl_rpName.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 5, 0));

        lbl_rpName.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_rpName, "RP name"); // NOI18N
        pnl_rpName.add(lbl_rpName);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl_rpName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(pnl_placeHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnl_rpName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pnl_placeHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lbl_placeHolder;
    private javax.swing.JLabel lbl_rpName;
    private javax.swing.JPanel pnl_placeHolder;
    private org.jjazz.flatcomponents.api.FixedPreferredWidthPanel pnl_rpName;
    // End of variables declaration//GEN-END:variables

}
