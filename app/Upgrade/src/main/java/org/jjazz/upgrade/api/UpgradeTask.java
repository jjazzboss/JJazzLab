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
package org.jjazz.upgrade.api;

/**
 * An upgrade task.
 * <p>
 * All UpgradeTasks found in the global Lookup will be called by the UpgradeManager upon a fresh JJazzLab start, upon module installation (this means UI is not
 * available yet).
 * <p>
 */
public interface UpgradeTask
{

    /**
     * Perform an upgrade task, typically import settings from oldVersion.
     * <p>
     *
     * @param oldVersion The reference JJazzLab version from which upgrade should be done. Can be null if no previous version was found on the system.
     */
    void upgrade(String oldVersion);

}
