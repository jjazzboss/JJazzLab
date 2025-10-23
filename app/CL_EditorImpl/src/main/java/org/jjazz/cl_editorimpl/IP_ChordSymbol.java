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
package org.jjazz.cl_editorimpl;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;

/**
 * Represent the insertion point for a chord symbol.
 * <p>
 * This is a CLI_ChordSymbol item decorated to give control on its position.
 */
public class IP_ChordSymbol implements CLI_ChordSymbol
{

    /**
     * The decorated chord.
     */
    private final CLI_ChordSymbol cli;
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
        Preconditions.checkNotNull(item);
        cli = item;
        position = cli.getPosition();
    }

    @Override
    public ChordLeadSheet getContainer()
    {
        return cli.getContainer();
    }

    @Override
    public ChordLeadSheetItem<ExtChordSymbol> getCopy(ExtChordSymbol newData, Position newPos)
    {
        return cli.getCopy(newData, newPos);
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
    public int getPositionOrder()
    {
        return CLI_ChordSymbol.POSITION_ORDER - 1;
    }

    @Override
    public Position getPosition()
    {
        return new Position(position);
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
    public StringProperties getClientProperties()
    {
        return cli.getClientProperties();
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
        return fl.equals(flavor) || fl.equals(DataFlavor.stringFlavor);
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
