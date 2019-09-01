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
package org.jjazz.ui.cl_editor.barrenderer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltDataFilter;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * Represent the insertion point for a chord symbol.
 * <p>
 * This is a CLI_ChordSymbol item decorated to give control on its position.
 */
public class IP_ChordSymbol implements CLI_ChordSymbol
{

    /**
     * The decorated object.
     */
    private CLI_ChordSymbol cli;
    /**
     * The position which can be changed via the public method setPosition().
     */
    private Position position;
    /**
     * The listeners for changes in this ChordLeadSheetItem.
     */
    protected transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    public IP_ChordSymbol(CLI_ChordSymbol item)
    {
        if (item == null)
        {
            throw new NullPointerException("item=" + item);
        }
        cli = item;
        position = cli.getPosition();
    }

    @Override
    public ChordLeadSheet getContainer()
    {
        return cli.getContainer();
    }

    @Override
    public ChordLeadSheetItem<ExtChordSymbol> getCopy(ChordLeadSheet cls, Position newPos)
    {
        return cli.getCopy(cls, newPos);
    }

    @Override
    public boolean isBarSingleItem()
    {
        return cli.isBarSingleItem();
    }

    @Override
    public ExtChordSymbol getData()
    {
        return cli.getData();
    }

    @Override
    public Position getPosition()
    {
        return position;
    }

    /**
     * New capability to standard CLI_ChordSymbol we get from the factory.
     *
     * @param pos
     */
    public void setPosition(Position pos)
    {
        if (!position.equals(pos))
        {
            Position oldPos = position;
            position = pos;
            pcs.firePropertyChange(PROP_ITEM_POSITION, oldPos, position);
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    // ------------------------------------------------------------------------------
    // Implementation of interface Transferable
    // ------------------------------------------------------------------------------
    private static DataFlavor flavor
            = new DataFlavor(CLI_ChordSymbol.class, "Chord Symbol");

    private static DataFlavor[] supportedFlavors =
    {
        flavor,
        DataFlavor.stringFlavor,
    };

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return supportedFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor fl)
    {
        if (fl.equals(flavor)
                || fl.equals(DataFlavor.stringFlavor))
        {
            return true;
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor fl)
            throws UnsupportedFlavorException
    {
        if (fl.equals(flavor))
        {
            return this;
        } else if (fl.equals(DataFlavor.stringFlavor))
        {
            return toString();
        } else
        {
            throw new UnsupportedFlavorException(fl);
        }
    }

}
