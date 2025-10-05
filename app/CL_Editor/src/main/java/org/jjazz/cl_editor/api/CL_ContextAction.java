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
package org.jjazz.cl_editor.api;

import java.awt.event.ActionEvent;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.openide.util.Lookup;
import org.openide.util.Utilities;


/**
 * A base class to simplify CL_Editor context aware actions.
 * <p>
 * Relies on CL_ContextActionSupport.
 */
public abstract class CL_ContextAction extends AbstractAction implements CL_ContextActionListener, ClsChangeListener
{
    public enum ListeningTarget
    {
        CLS_ITEMS_SELECTION, // CLI_ChordSymbol, CLI_Section, etc.
        BAR_SELECTION,
        ACTIVE_CLS_CHANGES   // Changes of the active ChordLeadSheet
    };

    private final Lookup context;
    private CL_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(CL_ContextAction.class.getSimpleName());

    public CL_ContextAction()
    {
        this(Utilities.actionsGlobalContext());
    }

    /**
     * Constructor needed if subclass implements ContextAwareAction.
     *
     * @param context
     */
    public CL_ContextAction(Lookup context)
    {
        this.context = context;
        configureAction();
    }

    public Lookup getContext()
    {
        return context;
    }

    /**
     * Convenience method to get the Action.NAME value or "not set".
     *
     * @return
     */
    public String getActionName()
    {
        String res = (String) getValue(NAME);
        return res == null ? "not set" : res;
    }

    /**
     * Overridden to defer initialization as late as possible.
     * <p>
     *
     * @return
     */
    @Override
    public boolean isEnabled()
    {
        init();
        return super.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        init();
        var cls = cap.getActiveChordLeadSheet();
        if (cls != null)
        {
            actionPerformed(ae, cls, cap.getSelection());
        } else
        {
            LOGGER.log(Level.WARNING, "actionPerformed(ActionEvent) cls is null. this={0}", getClass());
        }
    }
    
    /**
     * The latest selection.
     *
     * @return
     */
    protected CL_SelectionUtilities getSelection()
    {
        init();
        return cap.getSelection();
    }

    /**
     *
     * @return Can be null
     */
    protected ChordLeadSheet getActiveChordLeadSheet()
    {
        init();
        return cap.getActiveChordLeadSheet();
    }


    /**
     * Configure the action, typically using Action.putValue().
     * <p>
     * Called by CL_ContextAction constructors. Default implementation does nothing.
     * <p>
     * Override to customize (important if your action is registered with lazy=false).
     */
    protected void configureAction()
    {

    }

    /**
     * Provide the ListeningTarget which needs to be listened to.
     * <p>
     *
     * @return
     */
    abstract protected EnumSet<ListeningTarget> getListeningTargets();

    /**
     * Perform the action.
     *
     * @param ae        The source ActionEvent
     * @param cls       Can not be null
     * @param selection Can not be null
     */
    abstract protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_SelectionUtilities selection);

    /**
     * Called when selection in the provided context has changed.
     * <p>
     * Typically used to update the enabled state.
     *
     * @param selection
     */
    @Override
    abstract public void selectionChange(CL_SelectionUtilities selection);


    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      
    /**
     * Default implementation does nothing.
     *
     * @param e
     * @throws UnsupportedEditException
     */
    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
    }

    /**
     * Default implementation does nothing.
     *
     * @param event
     */
    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
    }


    // ============================================================================================= 
    // Private methods
    // =============================================================================================        
    /**
     * Initialize our CL_ContextActionSupport instance.
     */
    private void init()
    {
        if (cap != null)
        {
            return;
        }
        cap = CL_ContextActionSupport.getInstance(context);
        var targets = getListeningTargets();
        if (targets.contains(ListeningTarget.ACTIVE_CLS_CHANGES))
        {
            cap.addWeakActiveClsChangeListener(this);
        }
        if (targets.contains(ListeningTarget.BAR_SELECTION) || targets.contains(ListeningTarget.CLS_ITEMS_SELECTION))
        {
            cap.addWeakSelectionListener(this);
        }
        selectionChange(cap.getSelection());
    }


}
