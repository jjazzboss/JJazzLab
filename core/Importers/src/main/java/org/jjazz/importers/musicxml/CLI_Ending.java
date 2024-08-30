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
    public ChordLeadSheetItem<Ending> getCopy(Position newPos)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isBarSingleItem()
    {
        return true;
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
        this.container = container;
    }

}
