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

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * IDataRecord implementation for 32-bit floating point data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Feb 08, 2007           chammack     Initial Creation.
 * Nov 24, 2007  555      garmendariz  Added method to check dataset dimensions
 *                                     and override toString
 * Mar 29, 2021  8374     randerso     Removed toString() in favor of method in
 *                                     AbstractStoreageRecord. Code cleanup.
 * Jun 10, 2021  8450     mapeters     Add serialVersionUID
 * Nov 03, 2022  8931     smoorthy     Add group name normalization
 *
 *
 * </pre>
 *
 * @author chammack
 */
@DynamicSerialize
public class FloatDataRecord extends AbstractStorageRecord {

    private static final long serialVersionUID = 7556166315570512849L;

    @DynamicSerializeElement
    protected float[] floatData;

    /**
     * Nullary constructor for Dynamic Serialization
     */
    public FloatDataRecord() {
        super();
    }

    /**
     * Constructor
     *
     * @param name
     *            the name of the data
     * @param group
     *            the group inside the file
     * @param floatData
     *            the float data as 1d array
     * @param dimension
     *            the dimension of the data
     * @param sizes
     *            the length of each dimension
     */
    public FloatDataRecord(String name, String group, float[] floatData,
            int dimension, long[] sizes) {
        super(name, DataStoreFactory.normalizeAttributeName(group), dimension, sizes);
        this.floatData = floatData;
    }

    /**
     * Convenience constructor for single dimension float data
     *
     * @param name
     *            name of the data
     * @param group
     *            the group inside the file
     * @param floatData
     *            the one dimensional float data
     */
    public FloatDataRecord(String name, String group, float[] floatData) {
        this(name, group, floatData, 1, new long[] { floatData.length });
    }

    /**
     * @return the floatData
     */
    public float[] getFloatData() {
        return floatData;
    }

    /**
     * @param floatData
     *            the floatData to set
     */
    public void setFloatData(float[] floatData) {
        this.floatData = floatData;
    }

    @Override
    public Object getDataObject() {
        return this.floatData;
    }

    @Override
    public boolean validateDataSet() {

        long size = 1;

        for (int i = 0; i < this.dimension; i++) {
            size *= this.sizes[i];
        }

        if (size == this.floatData.length) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void reduce(int[] indices) {
        float[] reducedData = new float[indices.length];
        for (int i = 0; i < reducedData.length; i++) {
            if (indices[i] >= 0) {
                reducedData[i] = floatData[indices[i]];
            } else {
                reducedData[i] = -9999;
            }
        }
        this.floatData = reducedData;
        setDimension(1);
        setSizes(new long[] { indices.length });
    }

    @Override
    protected AbstractStorageRecord cloneInternal() {
        FloatDataRecord record = new FloatDataRecord();
        if (floatData != null) {
            record.floatData = Arrays.copyOf(floatData, floatData.length);
        }
        return record;
    }

    @Override
    public int getSizeInBytes() {
        return floatData == null ? 0 : floatData.length * 4;
    }
}
