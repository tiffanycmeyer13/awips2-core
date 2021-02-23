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
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 04, 2021 8337       mchan       Initial creation
 *
 * </pre>
 *
 * @author mchan
 */
@DynamicSerialize
public class ResponseWrapper {

    @DynamicSerializeElement
    private Object response;

    @DynamicSerializeElement
    private String host;

    public ResponseWrapper() {
    }

    public ResponseWrapper(Object response, String host) {
        this.response = response;
        this.host = host;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    /**
     * The response to the request.
     *
     * @return response object
     */
    public Object getResponse() {
        return response;
    }

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

}
