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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.logging.Logger;
import javax.swing.JButton;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rpcustomeditor.api.RpCustomEditDialog;
import org.jjazz.rpcustomeditor.spi.RpCustomEditor;
import org.jjazz.rpcustomeditor.spi.RpCustomEditorProvider;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.util.ResUtil;
import org.jjazz.util.Utilities;
import org.jjazz.ui.spteditor.spi.RpEditorComponent;

/**
 * An editor component for RhythmParameters which implement RpCustomEditorProvider.
 * <p>
 */
public class RpEditorCustom extends RpEditorComponent
{

    JButton btn_edit;
    Object value;
    private static final Logger LOGGER = Logger.getLogger(RpEditorCustom.class.getSimpleName());

    public RpEditorCustom(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);

        if (!(rp instanceof RpCustomEditorProvider))
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp + " is not an instanceof RpCustomEditorProvider");
        }

        btn_edit = new JButton(ResUtil.getString(getClass(), "CTL_Edit"));
        btn_edit.addActionListener(ae -> showCustomEditDialog());
        updateEditorValue(spt.getRPValue(rp));

        setLayout(new BorderLayout());
        add(btn_edit, BorderLayout.CENTER);
    }

    @Override
    public void showMultiValueMode(boolean b)
    {
        RpEditor.showMultiModeUsingFont(b, btn_edit);
    }

    @Override
    public Object getEditorValue()
    {
        return value;
    }

    @Override
    public void updateEditorValue(Object value)
    {
        this.value = value;
        btn_edit.setText(Utilities.truncateWithDots(value.toString(), 30));
        btn_edit.setToolTipText(rp.getValueDescription(value) + " - Click to edit");
    }

    // ===========================================================================
    // Private methods
    // ===========================================================================
    private void showCustomEditDialog()
    {

        // Prepare the CustomEditor
        RpCustomEditorProvider provider = (RpCustomEditorProvider) rp;
        RpCustomEditor rpEditor = provider.getCustomEditor();
        rpEditor.preset(value, songPart);


        // Prepare our dialog
        RpCustomEditDialog dlg = RpCustomEditDialog.getInstance();
        dlg.preset(rpEditor, songPart);
        Rectangle r = btn_edit.getBounds();
        Point p = r.getLocation();
        int x = Math.max(10, p.x + dlg.getWidth() + 5);
        int y = Math.max(10, p.y + r.height / 2 - dlg.getHeight() / 2);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        x = Math.min(x, screen.width - dlg.getWidth());
        y = Math.min(y, screen.height - dlg.getHeight());
        dlg.setLocation(x, y);
        dlg.setVisible(true);


        // Process result
        Object newValue = rpEditor.getEditedRpValue();
        if (dlg.isExitOk() && newValue != null && !newValue.equals(value))
        {
            Object old = value;
            value = newValue;
            firePropertyChange(RpEditor.PROP_RP_VALUE, old, value);
        }
    }

}
