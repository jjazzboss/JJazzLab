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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.dumpallkbdshortcuts")
//@ActionRegistration(displayName = "All NB kbd shortcuts", lazy = false)
public final class DumpNbKbdShortcuts extends AbstractAction implements ContextAwareAction
{

    private static DumpNbKbdShortcuts INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(DumpNbKbdShortcuts.class.getSimpleName());
    private Lookup context;

//    @ActionID(category = "Edit", id = "com.example.MyFactoryAction")
//    @ActionRegistration(displayName = "CTL_MyFactoryAction", lazy = true)
//    @ActionReferences(
//            {
//                @ActionReference(path = "Actions/ChordSymbol", position = 28700),
//                @ActionReference(path = "Menu/Edit", position = 80012),
//                @ActionReference(path = "Shortcuts", name = "O-Y")      // ctrl Y
//            })
    public static DumpNbKbdShortcuts myFactoryAction()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new DumpNbKbdShortcuts();
        }
        return INSTANCE;
    }


    public DumpNbKbdShortcuts()
    {
        LOGGER.info(() -> "DumpNbKbdShortcuts() -- this=" + this);
    }

    public DumpNbKbdShortcuts(Lookup context)
    {
        LOGGER.log(Level.INFO, "DumpNbKbdShortcuts() -- this={0} context={1}", new Object[]
        {
            this, context
        });
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "actionPerformed() -- this={0}", this);
        FileObject shortcutsFolder = FileUtil.getConfigFile("Shortcuts");
        if (shortcutsFolder != null)
        {
            for (FileObject fo : shortcutsFolder.getChildren())
            {
                String keystroke = fo.getName(); // e.g., "D-S" for Ctrl+S
                String targetPath = (String) fo.getAttribute("originalFile");
                LOGGER.log(Level.INFO, "{0} -> {1}", new Object[]
                {
                    keystroke, targetPath
                });
            }
        }

    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        LOGGER.info("createContextAwareInstance() --");
        var a = new DumpNbKbdShortcuts();
        a.context = lkp;
        return a;
    }

}
