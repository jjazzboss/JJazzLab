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
package org.jjazz.popupmenuwindowmenupatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.core.windows.actions.ActionsFactory;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Only to fix a Netbeans bug ! Some actions like Move Group, Size Group, actions which appear in window tabs context menus, are hardcoded in Netbeans, and can
 * not be disabled by using the branding settings for window system
 * <p>
 * See https://blogs.oracle.com/geertjan/entry/removing_menu_items_from_window
 * <p>
 * Reuse implementation code from the core.windows module : ActionsUtils.java Warning it's not official API !!
 * <p>
 * <p>
 * Todo: Implementation dependency, check if problem is fixed in future Netbeans RCP releases
 * <p>
 */
@ServiceProvider(service = ActionsFactory.class)
public class JJazzActionsFactory extends ActionsFactory
{

    private static final Logger LOGGER = Logger.getLogger(JJazzActionsFactory.class.getSimpleName());


    public JJazzActionsFactory()
    {
        LOGGER.info("Using custom ActionsFactory for TopComponent tab popup menu.");
    }

    /**
     * Provide actions to be shown in popup menu on TopComponent tab.
     *
     * @param tc
     * @param actions The default proposed actions.
     * @return
     */
    @Override
    public Action[] createPopupActions(TopComponent tc, Action[] actions)
    {
        LOGGER.log(Level.FINE, "createPopupActions() tc={0} actions.length={1}", new Object[]
        {
            tc, actions.length
        });
        List<Action> newActions = new ArrayList<>(List.of(actions));
        LOGGER.fine("createPopupActions() Actions IN:");
        logActions(actions);
        Mode mode = WindowManager.getDefault().findMode(tc);
        assert mode != null;
        switch (mode.getName())
        {
            case "editor" ->
            {
                /*
                * On the CL_Editor tab (a document TopComponent in editor mode) popup menu, remove the following actions:
                * Clone, maximize, minimize, New Domcument Tab Group, CLose Document Tab Groups, shift left, shift right
                * Preserve the following actions: Close, Close All, Close Other
                *
                * The action[] we receive for right click on the CL_Editor * TopComponent (editor mode):
                * a=&Close class=CloseWindowAction
                * a=Close &All class=CloseAllDocumentsAction
                * a=Close &Other class=CloseAllButThisAction
                * a=null
                * a=Maximi&ze class=MaximizeWindowAction
                * a=Shift Left class=MoveWindowWithinModeAction
                * a=Shift Right class=MoveWindowWithinModeAction
                * a=null
                * a=C&lone class=CloneDocumentAction
                * a=New Document Ta&b Group class=NewTabGroupAction
                * a=&Collapse Document Tab Group class=CollapseTabGroupAction
                 */
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
            case "output" ->
            {
                /*
                * On the RL_Editor tab (non-document TopComponent) popup menu, remove: Minimize Group, Move Group, Size Groupe, Shift
                * left/right
                * Preserve : close, float, dock
                *
                * The action[] we receive for right click on the output mode bar (right to RL_Editor):
                * a=&Close class=DisabledAction
                * a=null
                * a=Maximi&ze class=DisabledAction
                * a=M&inimize class=DisabledAction
                * a=Minimize Grou&p class=MinimizeModeAction
                * a=&Float class=DisabledAction
                * a=Doc&k class=DisabledAction
                * a=null
                * a=&Move class=DisabledAction
                * a=Mo&ve Group class=MoveModeAction
                * a=&Size Group class=ResizeModeAction
                 */
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
            case "explorer" ->
            {
                /*
                *
                * On the SptEditor tab or MixConsole (non-document TopComponent) popup menu, remove: Minimize, Move, Move Group, Size Group
                * Preserve: Float, Dock, shift left, shift right
                *
                * The action[] method argument we receive for right click on SptEditor or MixConsole tabs (explorer mode?) :
                * a=null
                * a=M&inimize class=AutoHideWindowAction
                * a=Minimize Grou&p class=MinimizeModeAction
                * a=&Float class=UndockWindowAction
                * a=Doc&k class=DockWindowAction
                * a=null
                * a=&Move class=MoveWindowAction
                * a=Shift Left class=MoveWindowWithinModeAction
                * a=Shift Right class=MoveWindowWithinModeAction
                * a=Mo&ve Group class=MoveModeAction
                * a=&Size Group class=ResizeModeAction
                 */
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                // removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
            default ->
            {
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone         
            }
        }

        removeUselessSeparators(newActions);
        LOGGER.fine("createPopupActions(tc) Actions OUT:");
        Action[] newActionsArray = newActions.toArray(new Action[0]);
        logActions(newActionsArray);
        return newActionsArray;
    }

    /**
     * Adjust the list of actions for Mode's popup menu (when user right-clicks into Mode's header but not into a particular TopComponent tab).
     *
     * @param mode
     * @param actions
     * @return
     */
    @Override
    public Action[] createPopupActions(Mode mode, Action[] actions)
    {
        LOGGER.log(Level.FINE, "createPopupActions() mode={0} actions.length={1}", new Object[]
        {
            mode, actions.length
        });
        LOGGER.fine("createPopupActions() Actions IN:");
        logActions(actions);
        List<Action> newActions = new ArrayList<>(List.of(actions));
        // logActions(actions);
        switch (mode.getName())
        {
            case "editor" ->
            {
                // e.g. right-click next to a CL_EditorTopComponent
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
            case "output" ->
            {
                // e.g. right-click next to a RL_editor TopComponent
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
            case "explorer" ->
            {
                // E.g. right-click next to a MixConsole or SptEditor
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
            default ->
            {
                removeMatchingActions(newActions, "(?i).*group.*");
                removeMatchingActions(newActions, "(?i).*imi.*");    // Minimize or Maximize
                removeMatchingActions(newActions, "(?i).*mo&?ve.*");    // Move or Mo&ve
                removeMatchingActions(newActions, "(?i).*size.*");      // Size Group
                removeMatchingActions(newActions, "(?i).*shift.*");     // shift left / right
                removeMatchingActions(newActions, "(?i).*c&?lone.*");   // C&lone or clone
            }
        }

        removeUselessSeparators(newActions);
        LOGGER.fine("createPopupActions(mode) Actions OUT:");
        Action[] newActionsArray = newActions.toArray(new Action[0]);
        logActions(newActionsArray);
        return newActionsArray;
    }

    /**
     * Remove all the actions whose NAME matches the specified regular expression.
     *
     * @param actions
     * @param regexp
     */
    private void removeMatchingActions(List<Action> actions, String regexp)
    {
        actions.removeIf(a -> a != null && ((String) a.getValue(Action.NAME)).matches(regexp));
    }

    private void logActions(Action[] actions)
    {
        if (LOGGER.isLoggable(Level.FINE))
        {
            for (Action a : actions)
            {
                if (a == null)
                {
                    LOGGER.log(Level.FINE, " a={0}", a);
                } else
                {
                    LOGGER.log(Level.FINE, " a={0} class={1}", new Object[]
                    {
                        a.getValue(Action.NAME), a.getClass().getSimpleName()
                    });
                }
            }
        }
    }

    private void removeUselessSeparators(List<Action> actions)
    {
        int lastIndex = actions.size() - 1;
        int lastNullIndex = actions.size();
        for (int i = lastIndex; i >= 0; i--)
        {
            Action a = actions.get(i);
            if (a == null)
            {
                if (lastNullIndex == i + 1)
                {
                    actions.remove(i);
                }
                lastNullIndex = i;
            }
        }
        if (!actions.isEmpty() && actions.get(0) == null)
        {
            actions.remove(0);
        }
    }
}
