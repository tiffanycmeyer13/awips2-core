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

import java.util.Map;

import com.raytheon.uf.common.datastorage.StorageProperties;

/**
 * Data Record Interface
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Feb 08, 2007           chammack     Initial Check-in
 * Nov 24, 2007  555      garmendariz  Added method to check dataset dimensions
 * Mar 29, 2021  8374     randerso     Renamed get/setProperties to get/setProps
 *                                     to match the underlying field name in
 *                                     AbstractStorageRecord and all the Python
 *                                     record classes.
 *
 * </pre>
 *
 * @author bphillip
 */

public interface IDataRecord {

    /**
     * @return the storage properties
     */
    public abstract StorageProperties getProps();

    /**
     * @param props
     *            the properties to set
     */
    public abstract void setProps(StorageProperties props);

    /**
     * @return the dimension
     */
    public abstract int getDimension();

    /**
     * @param dimension
     *            the dimension to set
     */
    public abstract void setDimension(int dimension);

    /**
     * @return the starting indices of sub area
     */
    public abstract long[] getMinIndex();

    /**
     * @param minIndex
     *            starting indices of sub area
     */
    public abstract void setMinIndex(long[] minIndex);

    /**
     * @return the name
     */
    public abstract String getName();

    /**
     * @param name
     *            the name to set
     */
    public abstract void setName(String name);

    /**
     * @return the sizes
     */
    public abstract long[] getSizes();

    /**
     * @param sizes
     *            the sizes as 64-bit integers
     */
    public abstract void setSizes(long[] sizes);

    /**
     * Generic type interface to the data
     *
     * Subinterfaces will also likely implement type-safe equivalents of this
     * method
     *
     * @return the data object
     */
    public abstract Object getDataObject();

    /**
     * Data type specific check to verify that dimensions are appropriate for
     * data object content.
     *
     * @return true if dimensions are valid
     */
    public boolean validateDataSet();

    /**
     * Get the group
     *
     * @return the group
     */
    public String getGroup();

    /**
     * Set the group
     *
     * @param group
     */
    public void setGroup(String group);

    /**
     * @return the correlationObject
     */
    public Object getCorrelationObject();

    /**
     * @param correlationObject
     *            the correlationObject to set
     */
    public void setCorrelationObject(Object correlationObject);

    /**
     * @return the dataAttributes
     */
    public Map<String, Object> getDataAttributes();

    /**
     * @param dataAttributes
     *            the dataAttributes to set
     */
    public void setDataAttributes(Map<String, Object> dataAttributes);

    /**
     * Reduces the dataset into a smaller dataset with only the indices
     * specified. All other indices are dropped from the data.
     *
     * The data is converted to a one dimensional array
     *
     * Indices should be expressed in 1d notation.
     *
     * @param indices
     *            the indices
     */
    public abstract void reduce(int[] indices);

    /**
     * @return the fillValue
     */
    public Number getFillValue();

    /**
     * @param fillValue
     *            the fillValue to set
     */
    public void setFillValue(Number fillValue);

    /**
     * @param sizes
     *            as 32-bit integers
     */
    public void setIntSizes(int[] sizes);

    /**
     *
     * Return the maximum size (the size that the dataset can maximally be
     * expanded to)
     *
     * @return the maxSizes
     */
    public long[] getMaxSizes();

    /**
     * Set the maximum size (the size that the dataset can maximally be expanded
     * to)
     *
     * @param maxSizes
     *            the maxSizes to set
     */
    public void setMaxSizes(long[] maxSizes);

    /**
     * @return the maxChunkSize
     */
    public int getMaxChunkSize();

    /**
     * @param maxChunkSize
     *            the maxChunkSize to set
     */
    public void setMaxChunkSize(int maxChunkSize);

    /**
     * Get the size of the data record given the dimensions, sizes of each
     * dimension and the type of data it is (float,byte,int,etc)
     *
     * @return size of record in bytes
     */
    public int getSizeInBytes();

    /**
     * Clone the record
     *
     * @return a deep copy of this record
     */
    public IDataRecord clone();
}