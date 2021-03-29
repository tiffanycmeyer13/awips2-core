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

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * PyPies delete response
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 12, 2010           njensen   Initial creation
 * Mar 29, 2021  8374     randerso  Added toString() for logging. Code cleanup.
 *
 * </pre>
 *
 * @author njensen
 */

@DynamicSerialize
public class DeleteResponse extends AbstractResponse {

    @DynamicSerializeElement
    private boolean success;

    /**
     * @return true if delete action was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param success
     *            true if delete action was successful
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", success[");
        sb.append(success);
        sb.append("]");

        return sb.toString();
    }
}
