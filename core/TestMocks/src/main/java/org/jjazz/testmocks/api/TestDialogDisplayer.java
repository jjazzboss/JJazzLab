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
package org.jjazz.testmocks.api;

import java.awt.Dialog;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * A DialogDisplayer implementation which logs or discards the notification messages.
 */
@ServiceProvider(service = DialogDisplayer.class, position = -100000)       // position needed to override default NB instance
public class TestDialogDisplayer extends DialogDisplayer
{

    private boolean discard = false;
    private static final Logger LOGGER = Logger.getLogger(TestDialogDisplayer.class.getSimpleName());


    /**
     * Create an instance which logs the notification messages.
     */
    public TestDialogDisplayer()
    {
        LOGGER.info("TestDialogDisplayer() instance registered");
    }


    /**
     * Check if messages are discarded.
     * <p>
     * If false, messages are logged.
     *
     * @return
     */
    public boolean isDiscardMessages()
    {
        return discard;
    }

    /**
     * If true notification messages are discarded.
     *
     * @param discard
     */
    public void setDiscardMessages(boolean discard)
    {
        this.discard = discard;
        LOGGER.log(Level.INFO, "setDiscardMessages() discard={0}", discard);
    }


    @Override
    public Object notify(NotifyDescriptor nd)
    {
        if (!discard)
        {
            String msg = nd.getMessage().toString();
            switch (nd.getMessageType())
            {
                case NotifyDescriptor.WARNING_MESSAGE ->
                    LOGGER.warning(msg);
                case NotifyDescriptor.ERROR_MESSAGE ->
                    LOGGER.severe(msg);
                case NotifyDescriptor.QUESTION_MESSAGE ->
                    throw new IllegalArgumentException("nd=" + nd);
                default ->
                    LOGGER.info(msg);
            }
        }

        return NotifyDescriptor.CLOSED_OPTION;
    }

    @Override
    public Dialog createDialog(DialogDescriptor dd)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
