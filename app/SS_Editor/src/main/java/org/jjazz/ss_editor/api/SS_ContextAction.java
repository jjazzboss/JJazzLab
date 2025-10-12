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
package org.jjazz.ss_editor.api;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;


/**
 * A base class for SS_Editor context aware actions.
 * <p>
 * To be used by actions associated to static menus, buttons or keyboard shortcuts, which act on the selection (chord symbols, bars, etc.), and whose enabled
 * state depends on this selection. If your action is only used in a transient popupmenu (created by Utilities.actionsToPopup()) with no keyboard shorcut, you
 * might prefer to use a simple AbstractAction+ContextAwareAction (see SetParentSectionColor.java for example).
 * <p>
 * If your action is used in a transient popup menu (created by Utilities.actionsToPopup()) and needs to listen to a shared service, use a WeakListener or
 * make your action a singleton (see Paste.java for example).
 * <p>
 *
 * @see SS_ContextActionSupport
 */
public abstract class SS_ContextAction extends AbstractAction implements SS_ContextActionListener, ContextAwareAction
{

    /**
     * Action property which defines the items listened to.
     * <p>
     * Expected value is an EnumSet&lt;ListeningTarget&gt;.
     *
     * @see #selectionChange(org.jjazz.ss_editor.api.SS_Selection)
     */
    public static final String LISTENING_TARGETS = "ListeningTargets";

    public enum ListeningTarget
    {
        SONG_PART_SELECTION,
        RHYTHM_PARAMETER_SELECTION
    };

    private Lookup context;
    private SS_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(SS_ContextAction.class.getSimpleName());

    /**
     * Create an action which listens to all possible ListeningTargets in the global context.
     *
     * @see #configureAction()
     */
    public SS_ContextAction()
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
        actionPerformed(ae, cap.getSelection());
    }

    /**
     * The latest selection.
     *
     * @return
     */
    protected SS_Selection getSelection()
    {
        init();
        return cap.getSelection();
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
     * @param selection Can not be null
     */
    abstract protected void actionPerformed(ActionEvent ae, SS_Selection selection);

    /**
     * Called when selection in the provided context has changed.
     * <p>
     * Typically used to update the enabled state.
     *
     * @param selection
     * @see #LISTENING_TARGETS
     */
    @Override
    abstract public void selectionChange(SS_Selection selection);


    // ============================================================================================= 
    // ContextAwareAction   
    // =============================================================================================  
    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        SS_ContextAction res = this;
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
        cap = SS_ContextActionSupport.getInstance(context);
        var targets = getListeningTargets();
        if (targets.contains(ListeningTarget.RHYTHM_PARAMETER_SELECTION) || targets.contains(ListeningTarget.SONG_PART_SELECTION))
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
