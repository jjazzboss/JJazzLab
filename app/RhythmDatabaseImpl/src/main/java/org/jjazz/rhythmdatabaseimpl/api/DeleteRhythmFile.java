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
package org.jjazz.rhythmdatabaseimpl.api;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import static javax.swing.Action.NAME;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * An action to delete a rhythm file.
 */
public class DeleteRhythmFile extends AbstractAction
{

    private static final Logger LOGGER = Logger.getLogger(DeleteRhythmFile.class.getSimpleName());


    public DeleteRhythmFile()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_DeleteRhythmFile"));
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HINT_DeleteRhythmFile"));
    }

    /**
     * Not used.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteRhythmFile(RhythmInfo ri)
    {
        Preconditions.checkNotNull(ri);
        if (ri.file().getName().isBlank())
        {
            return;
        }

        String msg = ResUtil.getString(getClass(), "CTL_ConfirmRhythmFileDelete", ri.file().getAbsolutePath());
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
        Object result = DialogDisplayer.getDefault().notify(d);
        if (NotifyDescriptor.YES_OPTION == result)
        {
            ri.file().deleteOnExit();
            RhythmDatabaseFactoryImpl.getInstance().markForStartupRescan(true);
        }
    }


}
