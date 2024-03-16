/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.zoomablesliders;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * A dummy class just to use annotations in order to create a Netbeans callback action for our ZoomFitWidth action, which does
 * different things according we're on a ChordLeadSheet or a SongStructure.
 *
 * See http://bits.netbeans.org/dev/javadoc/org-openide-awt/org/openide/awt/Actions.html
 */
public class ZoomFitWidth
{
    @ActionRegistration(displayName = "#CTL_ZoomToFit")
    @ActionID(category = "JJazz", id = "org.jjazz.zoomablesliders.zoomfitwidth")
    @ActionReferences(
            {
                @ActionReference(path = "Actions/RL_Editor", position = 3100),
                @ActionReference(path = "Shortcuts", name = "D-F")
            })
    // The below key will be searched in the TopComponent's active ActionMap in order to get the action to use.
    public static final String ACTION_MAP_KEY = "jjazz-zoomfitwidth";
}
