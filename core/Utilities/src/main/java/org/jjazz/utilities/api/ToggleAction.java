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
package org.jjazz.utilities.api;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

/**
 * An Action which can be selected or not.
 * <p>
 * The Action.SELECTED_KEY property value represents the selected state. <br>
 * The selected icon is stored in the Action.LARGE_ICON_KEY property.
 */
public abstract class ToggleAction extends AbstractAction
{

    /**
     * Create a non selected action.
     */
    public ToggleAction()
    {
        this(false);
    }

    /**
     * Create a selected or non-selected action.
     *
     * @param selected
     */
    public ToggleAction(boolean selected)
    {
        putValue(Action.SELECTED_KEY, selected);

        // Make sure selectedStateChanged() is called before other listeners
        addPropertyChangeListener(evt -> 
        {
            if (evt.getPropertyName().equals(Action.SELECTED_KEY))
            {
                selectedStateChanged((boolean) evt.getNewValue());
            }
        });
    }

    /**
     * Default implementation just toggles the selected state.
     *
     * @param ae
     */
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        setSelected(!isSelected());
    }

    public void setSelected(boolean b)
    {
        boolean old = isSelected();
        if (b != old)
        {
            putValue(Action.SELECTED_KEY, b);
        }
    }

    public boolean isSelected()
    {
        return (Boolean) getValue(Action.SELECTED_KEY);
    }

    public void setSelectedIcon(Icon icon)
    {
        putValue(Action.LARGE_ICON_KEY, icon);
    }

    public Icon getSelectedIcon()
    {
        return (Icon) getValue(Action.LARGE_ICON_KEY);
    }

    /**
     * Subclasses may override this method to be notified first (before other listeners) of the selected state change.
     * <p>
     * Default implementation does nothing.
     *
     * @param selected
     */
    protected void selectedStateChanged(boolean selected)
    {
        // Nothing
    }


}
