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
package org.jjazz.cl_editorimpl.actions;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.cl_editor.spi.ChordTypeSelectorUIProvider;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.spi.ChordTypeDatabase;

// @ServiceProvider(service = ChordTypeSelectorUIProvider.class)
public class ChordTypeSelectorUIProviderImpl extends JPanel implements ChordTypeSelectorUIProvider
{

    private JButton btnSouth, btnWest, btnEast, btnNorth, btnCancel;
    private Consumer<ChordType> chordTypeSetter;
    private static final Logger LOGGER = Logger.getLogger(ChordTypeSelectorUIProviderImpl.class.getSimpleName());

    public ChordTypeSelectorUIProviderImpl()
    {
        setLayout(new BorderLayout());
        btnSouth = new JButton("South");
        btnSouth.addActionListener(e -> ctSelected("South"));
        add(btnSouth, BorderLayout.SOUTH);
        btnNorth = new JButton("North");
        btnNorth.addActionListener(e -> ctSelected("North"));
        add(btnNorth, BorderLayout.NORTH);
        btnWest = new JButton("West");
        btnWest.addActionListener(e -> ctSelected("West"));
        add(btnWest, BorderLayout.WEST);
        btnEast = new JButton("East");
        btnEast.addActionListener(e -> ctSelected("East"));
        add(btnEast, BorderLayout.EAST);
        btnCancel = new JButton("CANCEL");
        btnCancel.addActionListener(e -> ctSelected(null));
        add(btnCancel, BorderLayout.CENTER);
    }

    @Override
    public JComponent getUI(CLI_ChordSymbol presetCliCs, Consumer<ChordType> ctSetter)
    {
        Objects.requireNonNull(ctSetter);
        btnEast.setText(presetCliCs.toString());
        this.chordTypeSetter = ctSetter;
        return this;
    }

    private void ctSelected(String ctStr)
    {
        LOGGER.log(Level.SEVERE, "ctSelected() -- {0}", ctStr);
        var ct = ctStr == null ? null : ChordTypeDatabase.getDefault().getChordType(3);
        chordTypeSetter.accept(ct);
    }

}
