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
package org.jjazz.ui.ss_editor.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.util.SmallMap;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

//@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.showhiderps")
//@ActionRegistration(displayName = "#CTL_ShowHideRp", lazy=false) // lazy=false to have the tooltip defined
//@ActionReferences(
//        {
////            @ActionReference(path = "Actions/SongPart", position = 3000, separatorBefore = 2990),
////            @ActionReference(path = "Actions/RhythmParameter", position = 3000, separatorBefore = 2990),
////            @ActionReference(path = "Actions/SS_Editor", position = 3000, separatorBefore = 2990),
//            @ActionReference(path = "Actions/SS_EditorToolBar", position = 200, separatorAfter = 201)
//        })

/**
 * The action can't be instanciated declaratively because there must be one action per editor and action is stateful.
 */
@NbBundle.Messages(
        {
            "CTL_ShowHideRp=Parameters",
            "DESC_ShowHideRp=Show/Hide Parameters"
        })
public class ShowHideRp extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(ShowHideRp.class.getSimpleName());
    private static final ImageIcon ICON = new ImageIcon(ShowHideRp.class.getResource("/org/jjazz/ui/ss_editor/actions/resources/VisibleRps.png"));
    private static final ImageIcon ICON_BIS = new ImageIcon(ShowHideRp.class.getResource("/org/jjazz/ui/ss_editor/actions/resources/VisibleRpsBis.png"));
    private SS_Editor editor;

    public ShowHideRp(SS_Editor editor)
    {
        if (editor == null)
        {
            throw new NullPointerException("editor");
        }
        this.editor = editor;
        this.editor.addPropertyChangeListener(SS_Editor.PROP_VISIBLE_RPS, evt -> updateIcon());

        putValue("hideActionText", true);
        putValue(NAME, CTL_ShowHideRp());
        putValue(SHORT_DESCRIPTION, Bundle.DESC_ShowHideRp());
        updateIcon();

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.log(Level.FINE, "actionPerformed()");
        SongStructure sgs = editor.getModel();
        SmallMap<Rhythm, List<RhythmParameter<?>>> map = new SmallMap<>();
        for (Rhythm r : SongStructure.getUniqueRhythms(sgs))
        {
            map.putValue(r, editor.getVisibleRps(r));
        }
        ShowHideRpsDialog dlg = ShowHideRpsDialog.getInstance();
        dlg.setModel(map);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        if (dlg.isExitOk())
        {
            for (Rhythm r : map.getKeys())
            {
                editor.setVisibleRps(r, map.getValue(r));
            }
        }
    }

    private int getNbHiddenParameters()
    {
        int res = 0;
        for (Rhythm r : SongStructure.getUniqueRhythms(editor.getModel()))
        {
            res = Math.max(res, r.getRhythmParameters().size() - editor.getVisibleRps(r).size());
        }
        return res;
    }

    private void updateIcon()
    {
        int hidden = getNbHiddenParameters();
        if (hidden > 0)
        {
            putValue(SMALL_ICON, ICON_BIS);
            putValue(SHORT_DESCRIPTION, Bundle.DESC_ShowHideRp() + " (" + hidden + " hidden parameters)");
        } else
        {
            putValue(SMALL_ICON, ICON);
            putValue(SHORT_DESCRIPTION, Bundle.DESC_ShowHideRp());
        }
    }

}
