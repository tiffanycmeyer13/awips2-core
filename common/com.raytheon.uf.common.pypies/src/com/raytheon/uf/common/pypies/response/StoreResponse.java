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
package com.raytheon.uf.common.pypies.response;

import java.util.Arrays;

import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * PyPies store response
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 28, 2010           njensen   Initial creation
 * Mar 29, 2021  8374     randerso  Added toString() for logging. Code cleanup.
 *
 * </pre>
 *
 * @author njensen
 */

@DynamicSerialize
public class StoreResponse extends AbstractResponse {

    @DynamicSerializeElement
    private StorageStatus status;

    @DynamicSerializeElement
    private String[] exceptions;

    @DynamicSerializeElement
    private IDataRecord[] failedRecords;

    public StorageStatus getStatus() {
        return status;
    }

    public void setStatus(StorageStatus status) {
        this.status = status;
    }

    public String[] getExceptions() {
        return exceptions;
    }

    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }

    public IDataRecord[] getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(IDataRecord[] failedRecords) {
        this.failedRecords = failedRecords;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", status[");
        sb.append(status);
        sb.append(", exceptions");
        sb.append(Arrays.toString(exceptions));
        sb.append(", failedRecords");
        sb.append(Arrays.toString(failedRecords));

        return sb.toString();
    }
}
