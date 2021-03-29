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

package com.raytheon.uf.common.datastorage.records;

import java.util.Arrays;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * IDataRecord implementation for 128-bit floating point data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 08, 2014           kustert   Initial Creation.
 * Apr 24, 2015  4425     nabowle   Bring in.
 * Mar 29, 2021  8374     randerso  Removed toString() in favor of method in
 *                                  AbstractStoreageRecord. Code cleanup.
 *
 * </pre>
 *
 * @author kustert
 */
@DynamicSerialize
public class DoubleDataRecord extends AbstractStorageRecord {

    @DynamicSerializeElement
    protected double[] doubleData;

    /**
     * Nullary constructor for Dynamic Serialization
     */
    public DoubleDataRecord() {
        super();
    }

    /**
     * Constructor
     *
     * @param name
     *            the name of the data
     * @param group
     *            the group inside the file
     * @param doubleData
     *            the double data as 1d array
     * @param dimension
     *            the dimension of the data
     * @param sizes
     *            the length of each dimension
     */
    public DoubleDataRecord(String name, String group, double[] doubleData,
            int dimension, long[] sizes) {
        super(name, group, dimension, sizes);
        this.doubleData = doubleData;
    }

    /**
     * Convenience constructor for single dimension double data
     *
     * @param name
     *            name of the data
     * @param group
     *            the group inside the file
     * @param doubleData
     *            the one dimensional double data
     */
    public DoubleDataRecord(String name, String group, double[] doubleData) {
        this(name, group, doubleData, 1, new long[] { doubleData.length });
    }

    /**
     * @return the doubleData
     */
    public double[] getDoubleData() {
        return doubleData;
    }

    /**
     * @param doubleData
     *            - the doubleData to set
     */
    public void setDoubleData(double[] doubleData) {
        this.doubleData = doubleData;
    }

    @Override
    public Object getDataObject() {
        return this.doubleData;
    }

    @Override
    public boolean validateDataSet() {

        long size = 1;

        for (int i = 0; i < this.dimension; i++) {
            size *= this.sizes[i];
        }

        if (size == this.doubleData.length) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reduce(int[] indices) {
        double[] reducedData = new double[indices.length];
        for (int i = 0; i < reducedData.length; i++) {
            if (indices[i] >= 0) {
                reducedData[i] = doubleData[indices[i]];
            } else {
                reducedData[i] = -9999;
            }
        }
        this.doubleData = reducedData;
        setDimension(1);
        setSizes(new long[] { indices.length });
    }

    @Override
    protected AbstractStorageRecord cloneInternal() {
        DoubleDataRecord record = new DoubleDataRecord();
        if (doubleData != null) {
            record.doubleData = Arrays.copyOf(doubleData, doubleData.length);
        }
        return record;
    }

    @Override
    public int getSizeInBytes() {
        return doubleData == null ? 0 : doubleData.length * 8;
    }

}
