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
package com.raytheon.uf.common.dataaccess;

import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.exception.InvalidIdentifiersException;
import com.raytheon.uf.common.dataaccess.exception.TimeAgnosticDataException;
import com.raytheon.uf.common.dataaccess.exception.UnsupportedOutputTypeException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;

/**
 * Factory interface for producing data objects that implement IData, hiding the
 * details of the underlying data implementation object and how it was
 * retrieved.
 * 
 * Implementations of IDataFactory should strive to handle all their processing
 * based on the request interface that was received. If at all possible, they
 * should not cast the request to a specific request implementation, ensuring
 * that they will work with multiple request implementations.
 * 
 * The return types of the getData() methods should be geared towards returning
 * what a caller of the API would expect. For example, a request for county
 * polygons may not explicitly request that it wants the US state name as an
 * attribute on the returned object. However, a caller could reasonably expect
 * when requesting counties that the returned object includes state name. This
 * must be balanced carefully since a caller of the API does not see the factory
 * implementation but has expectations about what will be returned.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Oct 10, 2012           njensen     Initial creation
 * Feb 14, 2013  1614     bsteffen    Refactor data access framework to use
 *                                     single request.
 * Mar 03, 2014  2673     bsteffen    Add ability to query only ref times.
 * Jul 14, 2014  3184     njensen     Added new methods
 * Jul 30, 2014  3184     njensen     Renamed valid identifiers to optional
 * Apr 13, 2016  5379     tgurney     Add getIdentifierValues()
 * Jun 07, 2016  5587     tgurney     Change get*Identifiers() to take
 *                                    IDataRequest
 * Aug 01, 2016  2416     tgurney     Add getNotificationFilter()
 * 
 * </pre>
 * 
 * @author njensen
 */

public interface IDataFactory {

    /**
     * Gets the available times that match the request. Implementations should
     * throw TimeAgnosticDataException if the datatype is time agnostic.
     * 
     * @param request
     *            the request to find matching times for
     * @param refTimeOnly
     *            true if only unique refTimes should be returned(without a
     *            forecastHr)
     * @return the times that have data available for this request
     * @throws TimeAgnosticDataException
     */
    public DataTime[] getAvailableTimes(IDataRequest request,
            boolean refTimeOnly) throws TimeAgnosticDataException;

    /**
     * Gets the available times that match the request within the BinOffset.
     * Implementations should throw TimeAgnosticDataException if the datatype is
     * time agnostic.
     * 
     * @param request
     *            the request to find matching times for
     * @param binOffset
     *            the bin offset to limit the returned times
     * @return the times with the bin offset applied that have data available
     *         for this request
     * @throws TimeAgnosticDataException
     */
    public DataTime[] getAvailableTimes(IDataRequest request,
            BinOffset binOffset) throws TimeAgnosticDataException;

    /**
     * Gets the available data that matches the request at the specified times.
     * If data is time agnostic, use getData(R).
     * 
     * @param request
     *            the request to get matching data for
     * @param times
     *            the times to get data for. If data is time agnostic, use
     *            getData(R)
     * @return the data that matches the request at the specified times
     * @throws UnsupportedOutputTypeException
     *             if this factory cannot produce IGridData
     */
    public IGridData[] getGridData(IDataRequest request, DataTime... times);

    /**
     * Gets the available data that matches the request and is within the time
     * range. If data is time agnostic, use getData(R).
     * 
     * @param request
     *            the request to get matching data for
     * @param timeRange
     *            the time range to return data for. If data is time agnostic,
     *            use getData(R).
     * @return the data that matches the request within the time range
     * @throws UnsupportedOutputTypeException
     *             if this factory cannot produce IGridData
     */
    public IGridData[] getGridData(IDataRequest request, TimeRange timeRange);

    /**
     * Gets the available data that matches the request at the specified times.
     * If data is time agnostic, use getData(R).
     * 
     * @param request
     *            the request to get matching data for
     * @param times
     *            the times to get data for. If data is time agnostic, use
     *            getData(R)
     * @return the data that matches the request at the specified times
     * @throws UnsupportedOutputTypeException
     *             if this factory cannot produce IGeometryData
     */
    public IGeometryData[] getGeometryData(IDataRequest request,
            DataTime... times);

    /**
     * Gets the available data that matches the request and is within the time
     * range. If data is time agnostic, use getData(R).
     * 
     * @param request
     *            the request to get matching data for
     * @param timeRange
     *            the time range to return data for. If data is time agnostic,
     *            use getData(R).
     * @return the data that matches the request within the time range
     * @throws UnsupportedOutputTypeException
     *             if this factory cannot produce IGeometryData
     */
    public IGeometryData[] getGeometryData(IDataRequest request,
            TimeRange timeRange);

    /**
     * Gets the available location names that match the request. Implementations
     * should throw IncompatibleRequestException if location names do not apply
     * to their datatype.
     * 
     * @param request
     *            the request to find matching location names for
     * @return the available location names that match the request
     * @throws IncompatibleRequestException
     *             if this factory does not support the concept of location
     *             names
     */
    public String[] getAvailableLocationNames(IDataRequest request);

    /**
     * Gets the available parameter names that match the request.
     * 
     * @param request
     *            the request to find matching parameter names for
     * @return the available parameter names that match the request
     */
    public String[] getAvailableParameters(IDataRequest request);

    /**
     * Gets the available levels that match the request. Implementations should
     * throw IncompatibleRequestException if levels do not apply to their
     * datatype.
     * 
     * @param request
     *            the request to find matching levels for
     * @return the available levels that match the request
     * @throws IncompatibleRequestException
     *             if this factory does not support the concept of levels
     * 
     */
    public Level[] getAvailableLevels(IDataRequest request);

    /**
     * Gets the identifiers that must be put on an IDataRequest for this
     * datatype
     * 
     * @param request
     *            Request to get required identifiers for
     * 
     * @return the identifiers that are required for this datatype's requests
     */
    public String[] getRequiredIdentifiers(IDataRequest request);

    /**
     * Gets the optional identifiers that will be recognized by requests for
     * this datatype.
     * 
     * @param request
     *            Request to get optional identifiers for
     * 
     * @return the optional identifiers that are recognized by requests for this
     *         datatype
     */
    public String[] getOptionalIdentifiers(IDataRequest request);

    /**
     * Gets the allowed values for a particular identifier recognized by this
     * datatype.
     * 
     * @param request
     *            the datatype to get identifier values for
     * @param identifierKey
     *            the identifier to find all allowed values for
     * @return the allowed values for the specified identifier recognized by
     *         this datatype
     * @throws InvalidIdentifiersException
     *             if the specified identifier is not a recognized identifier
     *             for this datatype
     */
    public String[] getIdentifierValues(IDataRequest request,
            String identifierKey);

    /**
     * Gets an INotificationFilter to be applied to dataURIs for update
     * notification.
     * 
     * @param request
     * @return
     */
    public INotificationFilter getNotificationFilter(IDataRequest request);
}
