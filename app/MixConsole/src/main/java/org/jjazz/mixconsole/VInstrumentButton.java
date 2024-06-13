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
package org.jjazz.mixconsole;

import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.flatcomponents.api.VTextIcon;

/**
 * A flat button with vertical text (rotated on the left)
 */
public class VInstrumentButton extends FlatButton
{

    private VTextIcon verticalIcon;
    private String vLabel;

    public VInstrumentButton()
    {
        verticalIcon = new VTextIcon(this, "Text", VTextIcon.ROTATE_LEFT);
        setIcon(verticalIcon);
        setDisabledIcon(verticalIcon);
    }

    /**
     * @return the vLabel
     */
    public String getvLabel()
    {
        return vLabel;
    }

    /**
     * @param vl the vLabel to set
     */
    public void setvLabel(String vl)
    {
        if (vl == null)
        {
            if (vLabel == null)
            {
                return;
            }
        } else if (vl.equals(vLabel))
        {
            return;
        }
        this.vLabel = vl;
        this.verticalIcon.setLabel(vl);
        revalidate();
        repaint();
    }
       
}
