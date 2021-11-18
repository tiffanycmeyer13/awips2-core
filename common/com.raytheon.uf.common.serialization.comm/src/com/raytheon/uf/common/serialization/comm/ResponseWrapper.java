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
package com.raytheon.uf.common.serialization.comm;

import java.lang.reflect.Array;
import java.util.List;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Decorates response with more information. Currently the host that served the
 * request is added.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------------
 * Feb 04, 2021  8337     mchan     Initial creation
 * Nov 18, 2021  8399     randerso  Fixed handling of array response
 *
 * </pre>
 *
 * @author mchan
 */
@DynamicSerialize
public class ResponseWrapper {

    @DynamicSerializeElement
    private String host;

    @DynamicSerializeElement
    private Object response;

    @DynamicSerializeElement
    private String arrayClassName;

    /**
     * ONLY FOR DYNAMIC SERIALIZATION, DO NOT USE!
     */
    public ResponseWrapper() {
    }

    /**
     *
     * @param response
     *            the request response
     * @param host
     *            name of host were request was processed
     */
    public ResponseWrapper(Object response, String host) {
        this.host = host;
        this.response = response;
        if (response != null) {
            Class<?> clazz = response.getClass();
            if (clazz.isArray()) {
                this.arrayClassName = clazz.getName();
            }
        }
    }

    /**
     * ONLY FOR DYNAMIC SERIALIZATION, DO NOT USE!
     *
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * The host name of the server that serviced the request.
     *
     * @return server host name
     */
    public String getHost() {
        return host;
    }

    /**
     * ONLY FOR DYNAMIC SERIALIZATION, DO NOT USE!
     *
     * @param response
     */
    public void setResponse(Object response) {
        this.response = response;
    }

    /**
     * The response to the request.
     *
     * @return response object
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("rawtypes")
    public Object getResponse() throws ClassNotFoundException {
        if (arrayClassName != null && response instanceof List) {

            /* Suppress SonarQube for use of dynamic class loading */
            @SuppressWarnings("squid:S2658")
            Class clazz = Class.forName(arrayClassName);
            Object[] array = (Object[]) Array
                    .newInstance(clazz.getComponentType(), 0);
            response = ((List<?>) response).toArray(array);
        }
        return response;
    }

    /**
     * ONLY FOR DYNAMIC SERIALIZATION, DO NOT USE!
     *
     * @return array class name or null if response is not an array
     */
    public String getArrayClassName() {
        return arrayClassName;
    }

    /**
     * ONLY FOR DYNAMIC SERIALIZATION, DO NOT USE!
     *
     * @param arrayClassName
     */
    public void setArrayClassName(String arrayClassName) {
        this.arrayClassName = arrayClassName;
    }

}
