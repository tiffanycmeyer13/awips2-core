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
 * IDataRecord implementation for 16-bit signed integer data
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
 * Mar 23, 2023  2031674  mapeters     Support shallow cloning
 *
 * </pre>
 *
 * @author chammack
 */
@DynamicSerialize
public class ShortDataRecord extends AbstractStorageRecord {

    private static final long serialVersionUID = -5483540322235896581L;

    @DynamicSerializeElement
    protected short[] shortData;

    /**
     * Nullary constructor for Dynamic Serialization
     */
    public ShortDataRecord() {
        super();
    }

    /**
     *
     * @param name
     * @param group
     * @param shortData
     * @param dimension
     * @param sizes
     */
    public ShortDataRecord(String name, String group, short[] shortData,
            int dimension, long[] sizes) {
        super(name, DataStoreFactory.normalizeAttributeName(group), dimension,
                sizes);
        this.shortData = shortData;
    }

    /**
     * Convenience constructor for single dimension short data
     *
     * @param name
     * @param group
     * @param shortData
     */
    public ShortDataRecord(String name, String group, short[] shortData) {
        this(name, group, shortData, 1, new long[] { shortData.length });
    }

    /**
     * @return the shortData
     */
    public short[] getShortData() {
        return shortData;
    }

    /**
     * @param shortData
     *            the shortData to set
     */
    public void setShortData(short[] shortData) {
        this.shortData = shortData;
    }

    @Override
    public Object getDataObject() {
        return this.shortData;
    }

    @Override
    public boolean validateDataSet() {

        long size = 1;

        for (int i = 0; i < this.dimension; i++) {
            size *= this.sizes[i];
        }

        if (size == this.shortData.length) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void reduce(int[] indices) {
        short[] reducedData = new short[indices.length];
        for (int i = 0; i < reducedData.length; i++) {
            if (indices[i] >= 0) {
                reducedData[i] = shortData[indices[i]];
            } else {
                reducedData[i] = -9999;
            }
        }
        this.shortData = reducedData;
        setDimension(1);
        setSizes(new long[] { indices.length });
    }

    @Override
    protected AbstractStorageRecord cloneInternal(boolean deep) {
        ShortDataRecord record = new ShortDataRecord();
        if (shortData != null) {
            if (deep) {
                record.shortData = Arrays.copyOf(shortData, shortData.length);
            } else {
                record.shortData = shortData;
            }
        }
        return record;
    }

    @Override
    public int getSizeInBytes() {
        return shortData == null ? 0 : shortData.length * 2;
    }

}
