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
package org.jjazz.cl_editorimpl.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;

/**
 * Allow user to change color of selected chords via a JPopupMenu.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.SetChordColor")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 2000, separatorBefore = 1999)
        })
public final class SetChordColor extends AbstractAction implements Presenter.Popup, ContextAwareAction
{


    private JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(SetChordColor.class.getSimpleName());


    public SetChordColor()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public SetChordColor(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(getClass(), "CTL_SetChordColor"));


        var selection = new CL_Selection(context);
        boolean b = selection.isChordSymbolSelected();
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }

        prepareMenu(menu, selection.getSelectedChordSymbols());
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SetChordColor(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // Not used
    }

    /**
     * Colors for user to choose from.
     * <p>
     *
     * @return An array with at least 4 values.
     */
    static public final Color[] getColors()
    {
        Color[] res = new Color[]
        {
            IR_ChordSymbolSettings.getDefault().getColor(),
            IR_ChordSymbolSettings.getDefault().getSubstituteFontColor(),
            new Color(0xb73003),
            new Color(0x004699)
        };
        return res;
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        return menu;
    }


    // ============================================================================================= 
    // Private methods
    // =============================================================================================    
    private void prepareMenu(JMenu menu, List<CLI_ChordSymbol> chordSymbols)
    {
        for (final Color c : getColors())
        {
            JMenuItem mi = new JMenuItem("    ");
            mi.setEnabled(true);
            mi.setOpaque(true);
            mi.setBackground(c);
            mi.addActionListener(ae -> setColor(c, chordSymbols));
            menu.add(mi);
        }
    }

    private void setColor(Color c, List<CLI_ChordSymbol> chordSymbols)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        for (var cliCs : chordSymbols)
        {
            Color cc = c == IR_ChordSymbolSettings.getDefault().getColor() ? null : c;
            CL_EditorClientProperties.setChordSymbolUserColor(cliCs, cc);
        }
        editor.getSongModel().setSaveNeeded(true);
        Analytics.logEvent("Set chord color");
    }

}
