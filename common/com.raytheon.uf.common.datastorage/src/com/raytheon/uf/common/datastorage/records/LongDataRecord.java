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
 * IDataRecord implementation for 64-bit signed integer data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * 20070913      379      jkorman      Initial Creation.
 * Nov 24, 2007  555      garmendariz  Added method to check dataset dimensions
 *                                     and override toString
 * Mar 29, 2021  8374     randerso     Removed toString() in favor of method in
 *                                     AbstractStoreageRecord. Code cleanup.
 *
 * </pre>
 *
 * @author jkorman
 */
@DynamicSerialize
public class LongDataRecord extends AbstractStorageRecord {

    @DynamicSerializeElement
    protected long[] longData;

    /**
     * Nullary constructor for Dynamic Serialization
     */
    public LongDataRecord() {
        super();
    }

    /**
     *
     * @param name
     * @param group
     * @param longData
     * @param dimension
     * @param sizes
     */
    public LongDataRecord(String name, String group, long[] longData,
            int dimension, long[] sizes) {
        super(name, group, dimension, sizes);
        this.longData = longData;
    }

    /**
     * Convenience constructor for single dimension long data
     *
     * @param name
     * @param group
     * @param longData
     *            A long data array reference.
     */
    public LongDataRecord(String name, String group, long[] longData) {
        this(name, group, longData, 1, new long[] { longData.length });
    }

    /**
     * Get a reference to the internal data array.
     *
     * @return A reference to the internal data array.
     */
    public long[] getLongData() {
        return longData;
    }

    /**
     * Set the data array.
     *
     * @param longData
     *            The internal data to set.
     */
    public void setLongData(long[] longData) {
        this.longData = longData;
    }

    /**
     * Get a reference to the internal data.
     *
     * @return The internal data reference.
     */
    @Override
    public Object getDataObject() {
        return longData;
    }

    @Override
    public boolean validateDataSet() {

        long size = 1;

        for (int i = 0; i < this.dimension; i++) {
            size *= this.sizes[i];
        }

        if (size == this.longData.length) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void reduce(int[] indices) {
        long[] reducedData = new long[indices.length];
        for (int i = 0; i < reducedData.length; i++) {
            if (indices[i] >= 0) {
                reducedData[i] = longData[indices[i]];
            } else {
                reducedData[i] = -9999;
            }
        }
        this.longData = reducedData;
        setDimension(1);
        setSizes(new long[] { indices.length });
    }

    @Override
    protected AbstractStorageRecord cloneInternal() {
        LongDataRecord record = new LongDataRecord();
        if (longData != null) {
            record.longData = Arrays.copyOf(longData, longData.length);
        }
        return record;
    }

    @Override
    public int getSizeInBytes() {
        return longData == null ? 0 : longData.length * 8;
    }
}
