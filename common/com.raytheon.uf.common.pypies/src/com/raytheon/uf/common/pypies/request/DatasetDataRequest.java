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
package com.raytheon.uf.common.pypies.request;

import java.util.Arrays;

import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * PyPies dataset data request
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 20, 2010            njensen     Initial creation
 * Mar 24  2021   8374     srahimi     Added toString Method for Logging
 *
 *
 *
 * </pre>
 *
 * @author njensen
 *
 */

@DynamicSerialize
public class DatasetDataRequest extends AbstractRequest {

    @DynamicSerializeElement
    private String[] datasetGroupPath;

    @DynamicSerializeElement
    private Request request;

    public String[] getDatasetGroupPath() {
        return datasetGroupPath;
    }

    public void setDatasetGroupPath(String[] datasetGroupPath) {
        this.datasetGroupPath = datasetGroupPath;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public String toString() {
        StringBuilder rval = new StringBuilder(super.toString());
        rval.append(", datasetGroupPath");
        rval.append(Arrays.toString(datasetGroupPath));
        rval.append(", request[");
        rval.append(request);
        rval.append("]");
        return rval.toString();

    }

}
