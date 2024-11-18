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
 *  
 */
 /*
 *  NOTE: code reused and modified from the TuxGuitar software (GNU Lesser GPL license), author: Julián Gabriel Casadesús
 */
package org.jjazz.instrumentcomponents.guitardiagram.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jjazz.harmony.api.Note;


/**
 * Represent a model for a chord diagram: which notes are pressed, which strings, which fret position.
 */
public class TGChord
{

    private int firstFret;
    private int[] strings;
    private String name;

    public TGChord(int length)
    {
        this.strings = new int[length];
        for (int i = 0; i < this.strings.length; i++)
        {
            this.strings[i] = -1;
        }
    }


    public void addFretValue(int string, int fret)
    {
        if (string >= 0 && string < this.strings.length)
        {
            this.strings[string] = fret;
            if (fret > 0 && (firstFret == 0 || fret < firstFret))
            {
                firstFret = fret;
            }
        }
    }

    public int getFretValue(int string)
    {
        if (string >= 0 && string < this.strings.length)
        {
            return this.strings[string];
        }
        return -1;
    }

    public int getFirstFret()
    {
        return this.firstFret;
    }

    public void setFirstFret(int firstFret)
    {
        this.firstFret = firstFret;
    }

    public int[] getStrings()
    {
        return this.strings;
    }

    public int countStrings()
    {
        return this.strings.length;
    }

    public int countNotes()
    {
        int count = 0;
        for (int i = 0; i < this.strings.length; i++)
        {
            if (this.strings[i] >= 0)
            {
                count++;
            }
        }
        return count;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the list of notes that make this TGChord.
     *
     * @return Can be empty
     */
    public List<Note> getNotes()
    {
        List<Note> res = new ArrayList<>();

        for (int i = 0; i < getStrings().length; i++)
        {
            int fret = getFretValue(i);
            if (fret >= 0)
            {
                TGString string = TGString.createDefaultInstrumentStrings().get(i);
                int sValue = string.getValue();
                Note ne = new Note(sValue + fret);
                res.add(ne);
            }
        }

        return res;
    }

    @Override
    public TGChord clone()
    {
        TGChord chord = new TGChord(this.strings.length);
        chord.setName(getName());
        chord.setFirstFret(getFirstFret());
        for (int i = 0; i < chord.strings.length; i++)
        {
            chord.strings[i] = this.strings[i];
        }
        return chord;
    }

    @Override
    public String toString()
    {
        return "name=" + name + ", firsFret=" + firstFret + ", strings=" + Arrays.toString(strings);
    }

}
