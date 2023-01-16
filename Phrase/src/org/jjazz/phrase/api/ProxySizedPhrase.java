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
package org.jjazz.phrase.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ConcurrentModificationException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.util.api.FloatRange;

/**
 * A special SizedPhrase which proxies a part of a Phrase.
 * <p>
 * A Phrase can have multiple TimeSignatures, a SizedPhrase only one. ProxySizedPhrase proxies a Phrase part with one
 * TimeSignature. All changes made to the ProxySizedPhrase are mirrored in the source Phrase. If the source Phrase is modified
 * externally (i.e. not via its ProxySizedPhrase) a ConcurrentModificationException is thrown.
 */
public class ProxySizedPhrase extends SizedPhrase implements PropertyChangeListener
{

    private static final String PROP_SRC_NOTE = "ProxySrcNote";
    private final Phrase phrase;
    private boolean validChange;

    /**
     * Create a ProxySizedPhrase.
     *
     * @param p
     * @param beatRange
     * @param ts
     */
    public ProxySizedPhrase(Phrase p, FloatRange beatRange, TimeSignature ts)
    {
        super(p.getChannel(), beatRange, ts, p.isDrums());
        this.phrase = p;


        // Add the notes within beatRange, updating the PROP_SRC_NOTE client property
        for (var srcNe : this.phrase)
        {
            var pos = srcNe.getPositionInBeats();
            if (pos < beatRange.from)
            {
                continue;
            } else if (pos >= beatRange.to)
            {
                break;
            }

            var dur = srcNe.getDurationInBeats();
            if (pos + dur > beatRange.to)
            {
                // Trim if note is too long
                dur = beatRange.to - pos;
            }
            var newNe = srcNe.getCopyDurPos(dur, pos);
            newNe.putClientProperty(PROP_SRC_NOTE, srcNe);
            add(newNe);
        }


        this.phrase.addPropertyChangeListener(this);
    }


    /**
     * Discard this SizedPhrase so that directly modifying the source Phrase will not throw an IllegalStateException anymore.
     */
    public void discard()
    {
        phrase.removePropertyChangeListener(this);
    }

    @Override
    public String toString()
    {
        return "Proxy-" + super.toString();
    }

    // =============================================================================================
    // PropertyChangeListener interface
    // =============================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == phrase)
        {
            if (!validChange)
            {
                throw new ConcurrentModificationException("Source phrase for this=" + this + " was modified externally. evt=" + evt);
            }
        } else if (evt.getSource() == this)
        {
            validChange = true;
            switch (evt.getPropertyName())
            {
                case Phrase.PROP_NOTE_ADDED ->
                {
                    NoteEvent ne = (NoteEvent) evt.getNewValue();
                    NoteEvent srcNe = ne.clone();
                    ne.putClientProperty(PROP_SRC_NOTE, srcNe);
                    phrase.add(srcNe);
                }
                case Phrase.PROP_NOTE_REMOVED ->
                {
                    NoteEvent ne = (NoteEvent) evt.getNewValue();
                    NoteEvent srcNe = (NoteEvent) ne.getClientProperty(PROP_SRC_NOTE);
                    phrase.remove(srcNe);
                }
                case Phrase.PROP_NOTE_MOVED ->
                {
                    NoteEvent oldNe = (NoteEvent) evt.getOldValue();
                    NoteEvent newNe = (NoteEvent) evt.getNewValue();
                    NoteEvent oldSrcNe = (NoteEvent) oldNe.getClientProperty(PROP_SRC_NOTE);
                    NoteEvent newSrcNe = phrase.move(oldSrcNe, newNe.getPositionInBeats());
                    newNe.putClientProperty(PROP_SRC_NOTE, newSrcNe);
                }
                case Phrase.PROP_NOTE_REPLACED ->
                {
                    NoteEvent oldNe = (NoteEvent) evt.getOldValue();
                    NoteEvent newNe = (NoteEvent) evt.getNewValue();
                    NoteEvent oldSrcNe = (NoteEvent) oldNe.getClientProperty(PROP_SRC_NOTE);
                    NoteEvent newSrcNe = newNe.clone();
                    newNe.putClientProperty(PROP_SRC_NOTE, newSrcNe);
                    phrase.replace(oldSrcNe, newSrcNe);
                }
                default ->
                {
                    throw new IllegalStateException("evt.getPropertyName()=" + evt.getPropertyName());
                }
            }
            validChange = false;
        }
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================    

}
