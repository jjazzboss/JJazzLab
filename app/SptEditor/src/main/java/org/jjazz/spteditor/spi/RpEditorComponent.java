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
package org.jjazz.spteditor.spi;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;

/**
 * A RhythmParameter value editor component for RpEditors.
 * <p>
 * Subclass must fire a RpEditor.PROP_RP_VALUE property change event each time user modifies the value with the editor.
 *
 * @param <E>
 */
public abstract class RpEditorComponent<E> extends JComponent implements PropertyChangeListener
{

    /**
     * Our model.
     */
    protected final SongPart songPart;
    /**
     * Our model.
     */
    protected final RhythmParameter<E> rp;

    /**
     * Create a RpEditorComponent.
     * <p>
     * Editor will listen to SongPart value changes and call updateEditorValue() accordingly.
     *
     * @param spt
     * @param rp
     */
    protected RpEditorComponent(SongPart spt, RhythmParameter<E> rp)
    {
        if (spt == null || rp == null)
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp);
        }
        songPart = spt;
        this.rp = rp;
        songPart.addPropertyChangeListener(this);
    }

    /**
     * Get the current value from this editor.
     *
     * @return Can't be null
     */
    abstract public E getEditorValue();

    /**
     * Adapt the display to show that we are in multi-value mode.
     *
     * @param b
     */
    abstract public void showMultiValueMode(boolean b);

    /**
     * Override and call super.cleanup() if you need to add additional cleanup stuff.
     */
    public void cleanup()
    {
        songPart.removePropertyChangeListener(this);
    }

    /**
     * Listen to model RpValue changes.
     *
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == songPart)
        {
            if (SongPart.PROP_RP_VALUE.equals(evt.getPropertyName()) && evt.getOldValue() == rp)
            {
                updateEditorValue((E) evt.getNewValue());
            }
        }
    }

    /**
     * By default call setEnabled() on direct children.
     * <p>
     * Subclasses should override the method if another behaviour is needed.
     *
     * @param b
     */
    @Override
    public void setEnabled(boolean b)
    {
        for (Component c : getComponents())
        {
            c.setEnabled(b);
        }
    }

    /**
     * Update the editor (without triggering a property change event).
     *
     * @param value
     */
    abstract public void updateEditorValue(E value);

}
