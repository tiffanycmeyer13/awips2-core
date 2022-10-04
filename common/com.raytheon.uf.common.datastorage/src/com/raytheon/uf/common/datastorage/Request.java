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
package com.raytheon.uf.common.datastorage;

import java.awt.Point;
import java.util.Arrays;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Represents the style of request to perform (whole dataset, line, slab,
 * points, etc.)
 *
 * To retrieve a whole dataset, use Request.ALL.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 27, 2009           chammack  Initial creation
 * Jun 18, 2013  15662    dhuffman  Cross section terrain disappears if baseline
 *                                  is too short.
 * Mar 24, 2021  8374     srahimi   Code cleanup
 * Mar 29, 2021  8374     randerso  Re-implemented copyFrom as shallowCopy.
 *                                  Additional code cleanup.
 * Jan 14, 2021  8741     njensen   Fixed toString() slab case and generated new
 *                                  implementations of hashCode() and equals()
 *
 * </pre>
 *
 * @author chammack
 *
 */
@DynamicSerialize
public class Request {

    @DynamicSerializeElement
    private Point[] points;

    @DynamicSerializeElement
    private int[] indices;

    @DynamicSerializeElement
    private int[] minIndexForSlab;

    @DynamicSerializeElement
    private int[] maxIndexForSlab;

    @DynamicSerializeElement
    private Type type;

    public enum Type {
        POINT,
        XLINE,
        YLINE,
        SLAB,
        ALL
    }

    /**
     * Request for full dataset
     */
    public static final Request ALL = new Request(Type.ALL);

    /**
     * Do NOT use this, only added for dynamic serialization
     */
    public Request() {

    }

    protected Request(Type type) {
        this.type = type;
    }

    /**
     * Build a request that asks for specific points to be returned
     *
     * @param points
     * @return the point request
     */
    public static Request buildPointRequest(Point... points) {
        Request request = new Request(Type.POINT);
        request.points = points;

        return request;
    }

    /**
     * Build a request that asks for specific cross section points to be
     * returned
     *
     * @param points
     * @return the cross section points request
     */
    public static Request buildXsectPointRequest(Point... points) {
        Request request = new Request(Type.POINT);
        request.points = new Point[points.length];
        for (int x = 0; x < points.length; x++) {
            request.points[x] = new Point(points[x]);
        }

        return request;
    }

    /**
     * Build a request that asks for all x values at a provided set of y values.
     *
     * IMPORTANT NOTE: The results are not guaranteed to be in the same order as
     * the indices. The results will be returned in monotonically increasing
     * order of the index.
     *
     * @param yIndices
     * @return the X line request
     */
    public static Request buildXLineRequest(int[] yIndices) {
        Request request = new Request(Type.XLINE);
        request.indices = yIndices;
        Arrays.sort(yIndices);
        return request;
    }

    /**
     * Build a request that asks for all y values at a provided set of x values.
     *
     * IMPORTANT NOTE: The results are not guaranteed to be in the same order as
     * the indices. The results will be returned in monotonically increasing
     * order of the index.
     *
     * @param xIndices
     * @return the Y line request
     */
    public static Request buildYLineRequest(int[] xIndices) {
        Request request = new Request(Type.YLINE);
        request.indices = xIndices;
        Arrays.sort(request.indices);
        return request;
    }

    /**
     * Perform a hyperslab request (effectively a rectangle in 2d space)
     *
     * @param minIndex
     * @param maxIndex
     * @return the slab request
     */
    public static Request buildSlab(int[] minIndex, int[] maxIndex) {
        Request request = new Request(Type.SLAB);
        request.minIndexForSlab = minIndex;
        request.maxIndexForSlab = maxIndex;
        return request;
    }

    /**
     * @return the points
     */
    public Point[] getPoints() {
        if (points == null) {
            points = new Point[0];
        }

        return points;
    }

    /**
     * @return the indices
     */
    public int[] getIndices() {
        return indices;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the minIndexForSlab
     */
    public int[] getMinIndexForSlab() {
        return minIndexForSlab;
    }

    /**
     * @return the maxIndexForSlab
     */
    public int[] getMaxIndexForSlab() {
        return maxIndexForSlab;
    }

    public void setPoints(Point[] points) {
        this.points = points;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public void setMinIndexForSlab(int[] minIndexForSlab) {
        this.minIndexForSlab = minIndexForSlab;
    }

    public void setMaxIndexForSlab(int[] maxIndexForSlab) {
        this.maxIndexForSlab = maxIndexForSlab;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return a shallow copy of this object
     */
    public Request shallowCopy() {
        Request copy = new Request(this.type);
        copy.indices = this.indices;
        copy.maxIndexForSlab = this.maxIndexForSlab;
        copy.minIndexForSlab = this.minIndexForSlab;
        copy.points = this.points;
        copy.type = this.type;

        return copy;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(getType());
        str.append(": ");

        switch (getType()) {
        case POINT: {
            str.append(points.length);
            str.append(" points");
            break;
        }
        case SLAB: {
            str.append(Arrays
                    .toString(new String[] { Arrays.toString(minIndexForSlab),
                            Arrays.toString(maxIndexForSlab) }));
            break;
        }
        case XLINE:
        case YLINE: {
            str.append(indices.length);
            str.append(" indices");
            break;
        }
        default:
            break;
        }
        return str.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(indices);
        result = prime * result + Arrays.hashCode(maxIndexForSlab);
        result = prime * result + Arrays.hashCode(minIndexForSlab);
        result = prime * result + Arrays.hashCode(points);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Request other = (Request) obj;
        if (!Arrays.equals(indices, other.indices)) {
            return false;
        }
        if (!Arrays.equals(maxIndexForSlab, other.maxIndexForSlab)) {
            return false;
        }
        if (!Arrays.equals(minIndexForSlab, other.minIndexForSlab)) {
            return false;
        }
        if (!Arrays.equals(points, other.points)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

}
