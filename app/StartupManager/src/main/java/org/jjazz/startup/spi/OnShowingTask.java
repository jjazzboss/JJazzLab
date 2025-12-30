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
package org.jjazz.startup.spi;

/**
 * A startup task which will be executed by StartupManager in priority ascending order when Netbeans UI is ready.
 * <p>
 * NOTE: OnShowingTasks are run one by one in a dedicated thread, so they can be (reasonably) long. The @onShowing position attribute also allows ordering
 * but it executes tasks on the EDT so tasks must be short.
 */
public interface OnShowingTask
{
    /**
     * This method is called upon startup when UI is ready.
     * <p>
     * NOTE: call is made on a dedicated thread which is not the EDT: if your task updates UI using e.g. SwingUtilities.invokeLater() might be required in some cases.
     */
    void run();

    /**
     * Get the priority of the task.
     * <p>
     * When Netbeans UI is ready tasks are executed one after the other by priority ascending order.
     *
     * @return
     */
    int getPriority();

    /**
     * Name of the task.
     * <p>
     * Used for logging.
     *
     * @return
     */
    String getName();

}
