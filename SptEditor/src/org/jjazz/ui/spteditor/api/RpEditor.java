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
package org.jjazz.ui.spteditor.api;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;

/**
 * The base class for RpEditors.
 * <p>
 * Combine a label with an editor component adapted to the type of RhythmParameter.<br>
 * RpEditor must fire a PROP_RPVALUE change event when modified.<br>
 * Framework will update the editor value using setRpValue().
 */
public abstract class RpEditor extends JPanel
{
    
    public static final Color MULTI_FOREGROUND_COLOR = new Color(121, 21, 42);      // Dark brown/red

    /**
     * This property change event must be fired each time value is modified by the editor.
     */
    public static final String PROP_RPVALUE = "rpValue";   //NOI18N 
    private SongPart sptModel;
    private RhythmParameter<?> rpModel;
    private boolean isMultiValueMode = false;
    private static final Logger LOGGER = Logger.getLogger(RpEditor.class.getSimpleName());
    
    private RpEditor()
    {
        initComponents();
    }

    /**
     * Initialize the RpEditor for the specified RhythmParameter.
     * <p>
     * Editor value must be set using setRpValue().
     *
     * @param spt
     * @param rp
     * @see setRpValue()
     */
    protected RpEditor(SongPart spt, RhythmParameter<?> rp)
    {
        this();
        if (sptModel != null)
        {
            setHighlighted(false);
        }
        sptModel = spt;
        rpModel = rp;
        lbl_rpName.setText(rpModel.getDisplayName().toLowerCase());
        lbl_rpName.setToolTipText(rpModel.getDescription());
    }
    
    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        getEditor().setEnabled(b);
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
    
    abstract protected JComponent getEditor();
    
    abstract public Object getRpValue();

    /**
     * Update the value in the editor.
     *
     * @param value
     * @param firePropChangeEvent If false don't fire a change event.
     */
    abstract public void setRpValue(Object value, boolean firePropChangeEvent);
    
    abstract public void cleanup();
    
    abstract protected void showMultiValueMode(boolean b);

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
        showMultiValueMode(isMultiValueMode);
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
    
    protected final void setEditor(JComponent editor)
    {
        // Replace the placeHolder by this editor      
        pnl_placeHolder.remove(lbl_placeHolder);
        pnl_placeHolder.add(editor);
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
            jc.setForeground(UIManager.getColor("Textfield.foreground"));
        } else
        {
            f = f.deriveFont(Font.ITALIC);
            jc.setForeground(MULTI_FOREGROUND_COLOR);
        }
        jc.setFont(f);
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
        pnl_rpName = new org.jjazz.ui.utilities.FixedPreferredWidthPanel();
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
    private org.jjazz.ui.utilities.FixedPreferredWidthPanel pnl_rpName;
    // End of variables declaration//GEN-END:variables

}
