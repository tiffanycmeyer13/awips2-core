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

import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * PyPies retrieve response
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 21, 2010           njensen   Initial creation
 * Mar 29, 2021  8374     randerso  Added toString() for logging. Code cleanup.
 *
 * </pre>
 *
 * @author njensen
 */

@DynamicSerialize
public class RetrieveResponse extends AbstractResponse {

    @DynamicSerializeElement
    protected IDataRecord[] records;

    public IDataRecord[] getRecords() {
        return records;
    }

    public void setRecords(IDataRecord[] records) {
        this.records = records;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", records");
        sb.append(Arrays.toString(records));

        return sb.toString();
    }
}
