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
package org.jjazz.cl_editor.spi;

import javax.swing.JDialog;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * A JDialog used to edit a Section item.
 * <p>
 * The Dialog should not directly change the model, it should just return the proposed changes. The calling application will
 * update the model if OK and manage the undo/redo aspects.
 * <p>
 */
public abstract class SectionEditorDialog extends JDialog
{

    /**
     * Search the global lookup for an instance.
     *
     * @return Can be null if no instance found.
     */
    public static SectionEditorDialog getDefault()
    {
        SectionEditorDialog o = Lookup.getDefault().lookup(SectionEditorDialog.class);
        return o;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected SectionEditorDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Preset the dialog to edit a CLI_Section.
     *
     * @param item CLI_Section
     * @param key If not 0, means user has typed key to initiate the dialog.
     */
    abstract public void preset(CLI_Section item, char key);

    /**
     * @return True if dialog was exited OK, false if dialog operation was cancelled.
     */
    abstract public boolean exitedOk();

    /**
     * The returned CLI_Section data.
     *
     * @return Non null only if exitedOk() was true.
     */
    abstract public Section getNewData();
}
