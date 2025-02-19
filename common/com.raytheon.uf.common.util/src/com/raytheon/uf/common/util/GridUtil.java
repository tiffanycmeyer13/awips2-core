/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for grids and images
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 26, 2011            njensen     Initial creation
 * Feb 15, 2013 1638       mschenke    Moved GRID_FILL_VALUE from edex.common Util
 * Jan 09, 2018 7178       randerso    Code cleanup
 *
 * </pre>
 *
 * @author njensen
 * @version 1.0
 */

public class GridUtil {

    /**
     * Moved from edex common util to clean up dependencies. Still need to
     * figure out method for handling fill values and update
     */
    public static final float GRID_FILL_VALUE = -999999f;

    /**
     * Bresenham's algorithm to determine closest points along a line on a grid.
     * This version is designed to add points to an existing ArrayList. The
     * first point is not added so that this routine can be called while walking
     * down a list of points without inserting duplicate points in the list.
     *
     * @param p1
     *            the first point
     * @param p2
     *            the second point
     * @param ptList
     *            list to which resulting points are added
     */
    public static void bresenham(Point p1, Point p2, List<Point> ptList) {

        // I wrote this from pseudo code found online explaining the algorithm

        // The difference between the x's
        int deltax = Math.abs(p2.x - p1.x);

        // The difference between the y's
        int deltay = Math.abs(p2.y - p1.y);

        // Start x off at the first pixel
        int x = p1.x;

        // Start y off at the first pixel
        int y = p1.y;

        int xinc1 = 0;
        int xinc2 = 0;
        int yinc1 = 0;
        int yinc2 = 0;

        if (p2.x >= p1.x) {
            // The x-values are increasing
            xinc1 = 1;
            xinc2 = 1;
        } else {
            // The x-values are decreasing
            xinc1 = -1;
            xinc2 = -1;
        }

        if (p2.y >= p1.y) {
            // The y-values are increasing
            yinc1 = 1;
            yinc2 = 1;
        } else {
            // The y-values are decreasing
            yinc1 = -1;
            yinc2 = -1;
        }

        int den = 0;
        int num = 0;
        int numadd = 0;
        int numpixels = 0;

        if (deltax >= deltay) {
            // There is at least one x-value for every y-value

            // Don't change the x when numerator >= denominator
            xinc1 = 0;

            // Don't change the y for every iteration
            yinc2 = 0;
            den = deltax;
            num = deltax / 2;
            numadd = deltay;

            // There are more x-values than y-values
            numpixels = deltax;
        } else {
            // There is at least one y-value for every x-value

            // Don't change the x for every iteration
            xinc2 = 0;

            // Don't change the y when numerator >= denominator
            yinc1 = 0;
            den = deltay;
            num = deltay / 2;
            numadd = deltax;

            // There are more y-values than x-values
            numpixels = deltay;
        }

        for (int i = 0; i < numpixels; i++) {
            // Increase the numerator by the top of the fraction
            num += numadd;

            // Check if numerator >= denominator
            if (num >= den) {
                // Calculate the new numerator value
                num -= den;

                // Change the x as appropriate
                x += xinc1;

                // Change the y as appropriate
                y += yinc1;
            }

            // Change the x as appropriate
            x += xinc2;
            // Change the y as appropriate
            y += yinc2;
            ptList.add(new Point(x, y));
        }
    }

    /**
     * Bresenham's algorithm to determine closest points along a line on a grid
     *
     * @param p1
     *            the first point
     * @param p2
     *            the second point
     * @return the closest points along the line
     */
    public static List<Point> bresenham(Point p1, Point p2) {
        List<Point> ptList = new ArrayList<>();
        ptList.add(p1);
        bresenham(p1, p2, ptList);
        return ptList;
    }

}
