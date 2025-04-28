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
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;


/**
 * @param startOrEnd True if it's a start/forward repeat
 * @param times      Not used for an end repeat
 */
record Repeat(boolean startOrEnd, int times)
        {

}

/**
 * A start or end repeat bar, with the number of loops (default is 2).
 */
public class CLI_Repeat implements ChordLeadSheetItem<Repeat>, WritableItem<Repeat>
{

    public static final int POSITION_ORDER_START = -800;
    public static final int POSITION_ORDER_END = 2100;


    private ChordLeadSheet container;
    private Position position;
    private Repeat data;


    public CLI_Repeat(Position position, Repeat data)
    {
        this.position = position;
        this.data = data;
    }

    @Override
    public ChordLeadSheet getContainer()
    {
        return container;
    }

    @Override
    public Repeat getData()
    {
        return data;
    }

    @Override
    public Position getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        return "CLI_Repeat[" + position + "] " + data;
    }


    @Override
    public ChordLeadSheetItem<Repeat> getCopy(Repeat newData, Position newPos)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getPositionOrder()
    {
        return data.startOrEnd() ? POSITION_ORDER_START : POSITION_ORDER_END;
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
    public void setData(Repeat data)
    {
        this.data = data;
    }

    @Override
    public void setContainer(ChordLeadSheet cls)
    {
        this.container = cls;
    }

}
