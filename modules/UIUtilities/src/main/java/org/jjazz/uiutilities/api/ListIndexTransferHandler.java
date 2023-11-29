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
package org.jjazz.uiutilities.api;

import java.awt.datatransfer.*;
import java.util.StringJoiner;
import javax.swing.*;

/**
 * A TransferHandler to reorder items in a DefaultListModel.
 * <p>
 * This differs from a typical TransferHandler which normally transfers the data. This handler transfers the "indexes" of the
 * items to be moved. This approach should work for any Object that is contained in the ListModel.
 * 
 * Copied from Stackoverflow.
 */
public class ListIndexTransferHandler extends TransferHandler
{

    private static final String DELIMITER = ",";

    /**
     * Perform the actual data import.
     *
     * @param info
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
        //  If we can't handle the import, bail now.

        if (!canImport(info) || !info.isDrop())
        {
            return false;
        }

        //Fetch the data -- bail if this fails

        String data = null;

        try
        {
            data = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
        } catch (Exception e)
        {
            System.out.println("importData: " + e.toString());
            return false;
        }

        //  Parse the data to rebuild array of selected indices

        String[] rowValues = data.split(DELIMITER);
        int[] rowIndices = new int[rowValues.length];

        for (int i = 0; i < rowValues.length; i++)
        {
            rowIndices[i] = Integer.parseInt(rowValues[i]);
        }

        //  The model to update

        JList list = (JList) info.getComponent();
        DefaultListModel model = (DefaultListModel) list.getModel();

        //  Determine drop location
        // (negative value implies drop at the end of JList, when using DropMode.ON)

        JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
        int dropIndex = dl.getIndex();

        if (dropIndex < 0)
        {
            dropIndex = model.getSize();
        }

        //  Drop before selected items

        if (dropIndex <= rowIndices[0])
        {
            for (int modelIndex : rowIndices)
            {
                Object item = model.get(modelIndex);
                model.remove(modelIndex);
                model.add(dropIndex, item);
                dropIndex++;
            }

            return true;
        }

        //  Drop after after selected items

        if (dropIndex > rowIndices[rowIndices.length - 1])
        {
            for (int i = rowIndices.length - 1; i >= 0; i--)
            {
                int modelIndex = rowIndices[i];
                Object item = model.get(modelIndex);
                model.add(dropIndex, item);
                model.remove(modelIndex);
                dropIndex--;
            }

            return true;
        }

        return false;
    }

    /**
     ** Create a string of the indices for each item to be moved.
     *
     * @param c
     * @return
     */
    @Override
    protected Transferable createTransferable(JComponent c)
    {
        JList list = (JList) c;
        int[] indices = list.getSelectedIndices();

        StringJoiner joiner = new StringJoiner(DELIMITER);

        for (int index : indices)
        {
            joiner.add(index + "");
        }

        return new StringSelection(joiner.toString());
    }

    /**
     * Items can only be moved to a different location in the model.
     */
    @Override
    public int getSourceActions(JComponent c)
    {
        return MOVE;
    }

    /**
     * No cleanup required since items must be dropped in same DefaultListModel
     *
     * @param c
     * @param data
     * @param action
     */
    @Override
    protected void exportDone(JComponent c, Transferable data, int action)
    {
    }

    /**
     * Any Object can be transfered (since we don't actually transfer the data)
     */
    @Override
    public boolean canImport(TransferHandler.TransferSupport support)
    {
        return true;
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI()
    {

        DefaultListModel<Object> model = new DefaultListModel<>();

        for (int i = 0; i < 20; i++)
        {
            model.addElement("Item " + i);
        }

        JList<Object> list = new JList<>(model);
        // list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        // list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setTransferHandler(new ListIndexTransferHandler());
        list.setDropMode(DropMode.INSERT);
        list.setVisibleRowCount(5);
        list.setVisibleRowCount(-1);

        JFrame frame = new JFrame("ListDrop");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new JScrollPane(list));
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
}
