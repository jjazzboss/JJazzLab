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
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;


/**
 * A base class for CL_Editor context aware actions.
 * <p>
 * To be used by actions associated to static menus, buttons or keyboard shortcuts, which act on the selection (chord symbols, bars, etc.), and whose enabled
 * state depends on this selection. If your action is only used in a transient popupmenu created by Utilities.actionsToPopup() with no keyboard shorcut, you
 * might prefer to use a simple AbstractAction+ContextAwareAction (see SetChordColor.java for example).
 * <p>
 * If your action is used in a transient popup menu (created by Utilities.actionsToPopup()) and needs to listen to a shared service, use a WeakListener or make
 * your action a singleton (see Paste.java for example).
 * <p>
 *
 * @see CL_ContextActionSupport
 */
public abstract class CL_ContextAction extends AbstractAction implements CL_ContextActionListener, ClsChangeListener, ContextAwareAction
{

    /**
     * Action property which defines the items listened to.
     * <p>
     * Expected value is an EnumSet&lt;ListeningTarget&gt;.
     *
     * @see #selectionChange(org.jjazz.cl_editor.api.CL_SelectionUtilities)
     * @see #chordLeadSheetChanged(org.jjazz.chordleadsheet.api.event.ClsChangeEvent)
     */
    public static final String LISTENING_TARGETS = "ListeningTargets";

    public enum ListeningTarget
    {
        CLS_ITEMS_SELECTION, // CLI_ChordSymbol, CLI_Section, etc.
        BAR_SELECTION,
        ACTIVE_CLS_CHANGES   // Changes of the active ChordLeadSheet
    };

    private Lookup context;
    private CL_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(CL_ContextAction.class.getSimpleName());

    /**
     * Create an action which listens to all possible ListeningTargets in the global context.
     *
     * @see #configureAction()
     */
    public CL_ContextAction()
    {
        context = Utilities.actionsGlobalContext();
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
    protected CL_Selection getSelection()
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
     * Let subclass configure the action created via the constructor, typically using Action.putValue().
     * <p>
     * Default implementation sets the listening targets to all possible values.
     * <p>
     * If the action is registered with lazy=false, you probably need to set at least Action.NAME (and possibly other properties such as Action.ACCELERATOR_KEY,
     * Action.SMALL_ICON or Action.SHORT_DESCRIPTION).
     */
    protected void configureAction()
    {
        putValue(LISTENING_TARGETS, EnumSet.allOf(ListeningTarget.class));
    }

    /**
     * Let subclass do additional configuration for instances created via createContextAwareInstance().
     * <p>
     * ContextAwareAction are used by Utilities.actionsForPath() and Utilities.actionsToPopup().
     * .<p>
     * Default implementation does nothing.
     *
     * @see #createContextAwareInstance(org.openide.util.Lookup)
     *
     */
    protected void configureContextAwareAction()
    {

    }

    /**
     * Perform the action.
     *
     * @param ae        The source ActionEvent
     * @param cls       Can not be null
     * @param selection Can not be null
     */
    abstract protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection);

    /**
     * Called when selection in the provided context has changed.
     * <p>
     * Typically used to update the enabled state.
     *
     * @param selection
     * @see #LISTENING_TARGETS
     */
    @Override
    abstract public void selectionChange(CL_Selection selection);

    // ============================================================================================= 
    // ContextAwareAction
    // =============================================================================================  
    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        CL_ContextAction res = this;
        if (context != lkp)
        {
            try
            {
                res = getClass().getDeclaredConstructor().newInstance();        // So it works with subclass
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
            {
                Exceptions.printStackTrace(ex);
            }
            res.setContext(lkp);
            res.configureContextAwareAction();
        }
        return res;
    }

    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      

    /**
     * Default implementation does nothing.
     *
     * @param event
     * @see #LISTENING_TARGETS
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

    /**
     * Convert the Action property into an EnumSet.
     *
     * @return
     */
    private EnumSet<ListeningTarget> getListeningTargets()
    {
        EnumSet<ListeningTarget> res = EnumSet.noneOf(ListeningTarget.class);
        var value = getValue(LISTENING_TARGETS);
        if (value instanceof EnumSet es)
        {
            res = es;
        }
        return res;
    }

    private void setContext(Lookup context)
    {
        this.context = context;
    }

}
