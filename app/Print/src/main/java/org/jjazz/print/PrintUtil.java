/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2001 - 2013 Object Refinery Ltd, Hitachi Vantara and Contributors.  All rights reserved.
 *
 */
package org.jjazz.print;

import java.awt.print.PageFormat;
import java.awt.print.Paper;

/**
 * Helper methods for printing.
 * <p>
 * Reused from PageFormatFactory.java from the pentaho-reporting project on GitHub.
 */
public class PrintUtil
{

    /**
     * Returns the left margin of the given paper.
     *
     * @param p the paper that defines the margins.
     * @return the left margin.
     */
    static public double getLeftMargin(final Paper p)
    {
        return p.getImageableX();
    }

    /**
     * Returns the right margin of the given paper.
     *
     * @param p the paper that defines the margins.
     * @return the right margin.
     */
    static public double getRightMargin(final Paper p)
    {
        return p.getWidth() - (p.getImageableX() + p.getImageableWidth());
    }

    /**
     * Returns the top margin of the given paper.
     *
     * @param p the paper that defines the margins.
     * @return the top margin.
     */
    static public double getTopMargin(final Paper p)
    {
        return p.getImageableY();
    }

    /**
     * Returns the bottom margin of the given paper.
     *
     * @param p the paper that defines the margins.
     * @return the bottom margin.
     */
    static public double getBottomMargin(final Paper p)
    {
        return p.getHeight() - (p.getImageableY() + p.getImageableHeight());
    }

    static public void setPageMargins(final PageFormat pageFormat, double top, double left, double bottom, double right)
    {
        final Paper paper = pageFormat.getPaper();
        setPaperMargins(paper, top, left, bottom, right);
        pageFormat.setPaper(paper);
    }

    /**
     * Defines the imageable area of the given paper by adjusting the margin around the imagable area.
     * <p>
     * The margin sizes are given in millimeters.
     *
     * @param paper the paper that should be modified
     * @param top the margin size of the top-margin
     * @param left the margin in points in the left
     * @param bottom the margin in points in the bottom
     * @param right the margin in points in the right
     */
    static public void setPaperMarginsMm(final Paper paper, final double top, final double left, final double bottom, final double right)
    {
        setPaperMargins(paper, convertMmToPoints(top), convertMmToPoints(left), convertMmToPoints(bottom), convertMmToPoints(right));
    }

    /**
     * Defines the imageable area of the given paper by adjusting the margin around the imagable area.
     * <p>
     * The margin sizes are given in points.
     *
     * @param paper the paper that should be modified
     * @param top the margin size of the top-margin
     * @param left the margin in points in the left
     * @param bottom the margin in points in the bottom
     * @param right the margin in points in the right
     */
    static public void setPaperMargins(final Paper paper, final double top, final double left, final double bottom, final double right)
    {
        final double w = paper.getWidth() - (right + left);
        final double h = paper.getHeight() - (bottom + top);
        paper.setImageableArea(left, top, w, h);
    }

    /**
     * Converts the given inch value to a valid point-value.
     *
     * @param inches the size in inch
     * @return the size in points
     */
    static public double convertInchToPoints(final double inches)
    {
        return inches * 72.0f;
    }

    /**
     * Converts the given millimeter value to a valid point-value.
     *
     * @param mm the size in mm
     * @return the size in points
     */
    static public double convertMmToPoints(final double mm)
    {
        return mm * (72.0d / 254.0d) * 10;
    }

    /**
     * Converts point-value to millimiter value.
     *
     * @param pts Size in points
     * @return the size in mm
     */
    static public double convertPointsToMm(final double pts)
    {
        return pts * 25.4f / 72f;
    }

    /**
     * Converts millimeters (mm) to inches (in)
     *
     * @param mm the value in mm
     * @return the value in inches
     */
    static public double convertMmToInches(double mm)
    {
        return mm / 25.4f;
    }

    /**
     * Converts millimeters (mm) to pixels (px)
     *
     * @param mm the value in mm
     * @param resolution the resolution in dpi (dots per inch)
     * @return the value in pixels
     */
    static public double convertMmToPixels(double mm, int resolution)
    {
        return convertMmToInches(mm) * resolution;
    }

}
