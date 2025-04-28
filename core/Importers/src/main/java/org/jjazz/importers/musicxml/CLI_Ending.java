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
package org.jjazz.importers.musicxml;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;


enum EndingType
{
    START, STOP, DISCONTINUE
};

record Ending(EndingType type, List<Integer> numbers)
        {

}

public class CLI_Ending implements ChordLeadSheetItem<Ending>, WritableItem<Ending>
{

    public static final int POSITION_ORDER_START = -1000;
    public static final int POSITION_ORDER_END = 2000;
    private ChordLeadSheet container;
    private Position position;
    private Ending data;


    public CLI_Ending(Position position, Ending data)
    {
        this.position = position;
        this.data = data;
    }

    public boolean isStartType()
    {
        return data.type().equals(EndingType.START);
    }

    public boolean isStopType()
    {
        return data.type().equals(EndingType.STOP);
    }

    @Override
    public ChordLeadSheet getContainer()
    {
        return container;
    }

    @Override
    public int getPositionOrder()
    {
        return isStartType() ? POSITION_ORDER_START : POSITION_ORDER_END;
    }

    /**
     * Overridden because order matters with other navigation items which are on the same position.
     * <p>
     * @param other
     * @return
     */
    @Override
    public int compareTo(ChordLeadSheetItem<?> other)
    {
        if (this == other)
        {
            return 0;
        }

        int res = getPosition().compareTo(other.getPosition());
        if (res != 0)
        {
            return res;
        }

        if (other instanceof CLI_Repeat cliRepeat)
        {
            // Ending start must be before a repeat-start
            // Ending stop/discontinue must be before a repeat-backward
            assert isStartType() == cliRepeat.getData().startOrEnd();
            res = -1;

        } else if (other instanceof CLI_NavigationItem cliNavItem)
        {
            // Ending start must be before "left" navigation mark  (CODA or SEGNO)
            // Ending stop/discontinue must be after "right" navigation marks (FINE, TOCODA, DACAPO..., DALSEGNO...)
            res = isStartType() ? -1 : 1;

        } else if (other instanceof CLI_Section cliSection)
        {
            // Ending start must be before a section
            res = -1;
        } else if (other instanceof CLI_ChordSymbol)
        {
            res = -1;  // eg CLI_ChordSymbol
        } else
        {
            throw new IllegalStateException("other=" + other);
        }
        return res;
    }

    @Override
    public String toString()
    {
        return "CLI_Ending[" + position + "] " + data;
    }

    @Override
    public Ending getData()
    {
        return data;
    }

    @Override
    public Position getPosition()
    {
        return position;
    }

    @Override
    public ChordLeadSheetItem<Ending> getCopy(Ending newData,Position newPos)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isBarSingleItem()
    {
        return false;
    }


    @Override
    public StringProperties getClientProperties()
    {
        return new StringProperties();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setPosition(Position pos)
    {
        this.position = pos;
    }

    @Override
    public void setData(Ending data)
    {
        this.data = data;
    }

    @Override
    public void setContainer(ChordLeadSheet cls)
    {
        this.container = cls;
    }

}
