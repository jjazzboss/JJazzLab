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
package org.jjazz.pianoroll;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;

/**
 * A specialization of NoteView for drums notes (no duration).
 */
public class NoteViewDrum extends NoteView
{

    public static final float DURATION = 0.05f;

    public NoteViewDrum(NoteEvent ne)
    {
        super(ne);
        setOpaque(false);       // We only draw a diamond, not the complete bounds
    }

    /**
     * Override paint because we don't want the border to be painted on the rectangle bounds.
     *
     * @param g
     */
    @Override
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Polygon p = new Polygon();
        p.addPoint(0, h / 2);
        p.addPoint(w / 2, h - 1);
        p.addPoint(w - 1, h / 2);
        p.addPoint(w / 2, 0);
        
        g2.setColor(getNoteColor());
        g2.fill(p);
        g2.setColor(getNoteBorderColor());
        g2.draw(p);
    }
}
