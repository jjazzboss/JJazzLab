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
 * A startup task which will be executed by StartupManager in priority ascending order when platform
 * is ready (see {@link org.openide.modules.OnStart}).
 * <p>
 * If order of execution does not matter, you might directly use @OnStart instead.
 * <p>
 * NOTE: Do not directly use NotifyDialog in these tasks, use OnStartMessageNotifier instead.
 *
 * @see org.jjazz.startup.api.OnStartMessageNotifier;
 */
public interface OnStartTask
{

    /**
     * This method is called upon startup when UI is ready.
     * <p>
     */
    void run();

    /**
     * Get the priority of the task.
     * <p>
     * When UI is ready tasks are executed one after the other by priority ascending order.
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
