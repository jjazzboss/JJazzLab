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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.harmony.api.ChordType.Family.DIMINISHED;
import static org.jjazz.harmony.api.ChordType.Family.MAJOR;
import static org.jjazz.harmony.api.ChordType.Family.MINOR;
import static org.jjazz.harmony.api.ChordType.Family.SEVENTH;
import static org.jjazz.harmony.api.ChordType.Family.SUS;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.jjazz.cl_editor.spi.ChordTypeSelectorUIProvider;
import org.jjazz.harmony.api.Note;
import org.jjazz.musiccontrol.api.PlaybackSettings;

/**
 * Action menu to select chord type.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.SetChordType")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 200)
        })
public final class SetChordTypeActionMenu extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(SetChordTypeActionMenu.class.getSimpleName());

    public SetChordTypeActionMenu()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public SetChordTypeActionMenu(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(SetChordTypeActionMenu.class, "CTL_SetChordType"));


        var selection = new CL_Selection(context);
        boolean b = selection.isChordSymbolSelected();
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }


        // Update menu
        var ctSelector = ChordTypeSelectorUIProvider.getDefault();
        if (ctSelector != null)
        {
            // Construct a menu item using the selector component

            var t = PlaybackSettings.getInstance().getChordSymbolsDisplayTransposition();
            var cs0 = selection.getSelectedChordSymbols().get(0).getData().getTransposedChordSymbol(t, null);

            JMenuItem miCustom = new JMenuItem();
            var selectorComp = ctSelector.getUI(cs0, new MyChordTypeSetter(miCustom));
            updateMenuItemCustom(miCustom, selectorComp);
            menu.add(miCustom);
        } else
        {
            // Use standard menu items
            updateMenuStd(menu);
        }

    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SetChordTypeActionMenu(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Useless
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        return menu;
    }


    // =============================================================================================================================
    // Private methods
    // =============================================================================================================================    
    /**
     * Use standard submenus per chord type family.
     *
     * @param menu
     */
    private void updateMenuStd(JMenu menu)
    {
        JMenu menuMajor = new JMenu(ResUtil.getString(SetChordTypeActionMenu.class, "CTL_Major"));
        JMenu menuMinor = new JMenu(ResUtil.getString(SetChordTypeActionMenu.class, "CTL_Minor"));
        JMenu menuSeventh = new JMenu(ResUtil.getString(SetChordTypeActionMenu.class, "CTL_Seventh"));
        JMenu menuDiminished = new JMenu(ResUtil.getString(SetChordTypeActionMenu.class, "CTL_Diminished"));
        JMenu menuSus4 = new JMenu(ResUtil.getString(SetChordTypeActionMenu.class, "CTL_Sus4"));

        menu.add(menuMajor);
        menu.add(menuSeventh);
        menu.add(menuMinor);
        menu.add(menuDiminished);
        menu.add(menuSus4);

        ChordTypeDatabase ctdb = ChordTypeDatabase.getDefault();
        var myChordTypeSetter = new MyChordTypeSetter(null);
        for (final ChordType ct : ctdb.getChordTypes())
        {
            JMenuItem mi = new JMenuItem((ct.toString().length() == 0) ? " " : ct.toString());
            mi.setToolTipText(ct.toDegreeString());
            mi.addActionListener(e -> myChordTypeSetter.accept(ct));

            switch (ct.getFamily())
            {
                case MAJOR -> menuMajor.add(mi);
                case MINOR -> menuMinor.add(mi);
                case SEVENTH -> menuSeventh.add(mi);
                case DIMINISHED -> menuDiminished.add(mi);
                case SUS -> menuSus4.add(mi);
                default -> throw new IllegalStateException("ct=" + ct);
            }
        }
    }

    /**
     * Use a custom ChordType selector component.
     *
     * @param mi
     * @param selectorComp
     */
    private void updateMenuItemCustom(JMenuItem mi, JComponent selectorComp)
    {
        mi.removeAll();
        mi.setLayout(new BorderLayout());
        mi.add(selectorComp);
        var pd = selectorComp.getPreferredSize();
        pd.width += 15;     // Required otherwise selectorComp is always a bit too small due to the JScrollBars
        pd.height += 15;
        mi.setPreferredSize(pd);
    }

    // =============================================================================================================================
    // Inner classes
    // =============================================================================================================================    
    private class MyChordTypeSetter implements Consumer<ChordType>
    {

        JMenuItem menuItem;

        public MyChordTypeSetter(JMenuItem mi)
        {
            this.menuItem = mi;
        }

        /**
         * Perform the action if ct is not null.
         *
         * @param ct
         */
        @Override
        public void accept(ChordType ct)
        {
            if (ct != null)
            {
                CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
                ChordLeadSheet cls = editor.getModel();
                CL_Selection selection = new CL_Selection(editor.getLookup());

                JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(menu.getText());
                setChordType(selection, ct);
                JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(menu.getText());
            }


            if (menuItem != null)
            {
                // Close the whole popup menu -tried various things, this is the only way to do it
                menuItem.doClick();
                Container parent = menu.getParent();
                while (parent != null && !(parent instanceof JPopupMenu))
                {
                    parent = parent.getParent();
                }
                if (parent instanceof JPopupMenu popupParent)
                {
                    popupParent.setVisible(false);
                } else
                {
                    throw new IllegalStateException("parent=" + parent);
                }
            }
        }

        private void setChordType(CL_Selection selection, ChordType ct)
        {
            var cls = selection.getChordLeadSheet();        // not null if not empty
            for (var cliCs : selection.getSelectedChordSymbols())
            {
                ExtChordSymbol oldEcs = cliCs.getData();
                ChordSymbol newCs = new ChordSymbol(oldEcs.getRootNote(), oldEcs.getBassNote(), ct);
                ChordRenderingInfo newCri = new ChordRenderingInfo(oldEcs.getRenderingInfo(), (StandardScaleInstance) null); // Discard scale             
                ExtChordSymbol newEcs = oldEcs.getCopy(newCs, newCri, null, null);
                cls.changeItem(cliCs, newEcs);
            }
        }
    }


}
