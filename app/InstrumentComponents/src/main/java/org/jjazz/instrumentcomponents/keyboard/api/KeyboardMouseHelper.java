/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.instrumentcomponents.keyboard.api;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A helper class to listen to all PianoKeys mouse events.
 */
public class KeyboardMouseHelper extends MouseAdapter
{

    public interface Listener
    {

        public void mouseClicked(int pitch, MouseEvent me);

        public void mousePressed(int pitch, MouseEvent me);
    }

    static public class ListenerAdapter implements Listener
    {

        @Override
        public void mouseClicked(int pitch, MouseEvent me)
        {
            // Nothing
        }

        @Override
        public void mousePressed(int pitch, MouseEvent me)
        {
            // Nothing
        }

    }
    private final List<Listener> listeners;
    private final KeyboardComponent keyboard;

    public KeyboardMouseHelper(KeyboardComponent keyboard)
    {
        Objects.requireNonNull(keyboard);
        this.keyboard = keyboard;
        this.listeners = new ArrayList<>();
        keyboard.getAllKeys().forEach(pk -> pk.addMouseListener(this));
    }

    public void addListener(Listener listener)
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener)
    {
        listeners.remove(listener);
    }

    public void cleanup()
    {
        keyboard.getAllKeys().forEach(pk -> pk.removeMouseListener(this));
    }

    @Override
    public void mouseClicked(MouseEvent me)
    {
        PianoKey pk = (PianoKey) me.getSource();
        listeners.forEach(l -> l.mouseClicked(pk.getPitch(), me));
    }

    @Override
    public void mousePressed(MouseEvent me)
    {
        PianoKey pk = (PianoKey) me.getSource();
        listeners.forEach(l -> l.mousePressed(pk.getPitch(), me));
    }
}
