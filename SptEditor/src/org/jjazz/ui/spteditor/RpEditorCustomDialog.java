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
package org.jjazz.ui.spteditor;

import java.awt.FlowLayout;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rpcustomeditor.api.RpCustomEditDialog;
import org.jjazz.rpcustomeditor.spi.RpCustomEditor;
import org.jjazz.rpcustomeditor.spi.RpCustomEditorProvider;
import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.ResUtil;
import org.jjazz.util.Utilities;

/**
 * A RpEditor for RhythmParameters which implement RpCustomEditorProvider.
 * <p>
 */
public class RpEditorCustomDialog extends RpEditor
{

    JPanel panel;
    JLabel label;
    JButton btn_edit;
    Object value;
    private static final Logger LOGGER = Logger.getLogger(RpEditorCustomDialog.class.getSimpleName());

    public RpEditorCustomDialog(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);

        if (!(rp instanceof RpCustomEditorProvider))
        {
            throw new IllegalArgumentException("rp=" + rp + " is not an instance of RpCustomEditorProvider (spt=" + spt + ")");
        }

        // Prepare our editor component        
        FlowLayout layout = new FlowLayout(FlowLayout.LEADING, 0, 0);   // alignment, hgap, vgap
        panel = new JPanel(layout);
        panel.setOpaque(false);
        label = new JLabel();
        btn_edit = new JButton(ResUtil.getString(getClass(), "CTL_Edit"));
        btn_edit.addActionListener(ae -> showCustomEditDialog());
        panel.add(btn_edit);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(label);
        setEditor(panel);

        updateEditorValue(spt.getRPValue(rp));
    }

    @Override
    protected void showMultiValueMode(boolean b)
    {
        showMultiModeUsingFont(isMultiValueMode(), label);
    }

    @Override
    public Object getEditorValue()
    {
        return value;
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    @Override
    protected JComponent getEditorComponent()
    {
        return panel;
    }

    @Override
    public void updateEditorValue(Object value)
    {
        this.value = value;
        label.setText(Utilities.truncateWithDots(value.toString(), 30));
        label.setToolTipText(((RhythmParameter) getRpModel()).getValueDescription(value));
    }

    // ===========================================================================
    // Private methods
    // ===========================================================================
    private void showCustomEditDialog()
    {
        RhythmParameter rp = getRpModel();
        SongPart spt = getSptModel();


        // Prepare the CustomEditor
        RpCustomEditorProvider provider = (RpCustomEditorProvider) getRpModel();
        RpCustomEditor rpEditor = provider.getCustomEditor();
        rpEditor.preset(value, spt);


        // Prepare our dialog
        RpCustomEditDialog dlg = RpCustomEditDialog.getInstance();
        dlg.preset(rpEditor, spt);
        dlg.setLocationRelativeTo(btn_edit);
        dlg.setVisible(true);


        // Process result
        Object newValue = rpEditor.getEditedRpValue();
        if (dlg.isExitOk() && newValue != null && !newValue.equals(value))
        {
            Object old = value;
            value = newValue;
            firePropertyChange(PROP_RPVALUE, old, value);
        }
    }

}
