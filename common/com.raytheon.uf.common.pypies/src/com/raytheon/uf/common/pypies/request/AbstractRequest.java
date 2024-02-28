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
 *
 **/
package com.raytheon.uf.common.pypies.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Abstract Base Class for PyPies requests
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 20, 2010            njensen     Initial creation
 * Mar 24  2021 8374       srahimi     Added toString for Logging
 * Sep 23, 2021 8608       mapeters    Add {@link RequestType}
 *
 *
 *
 * </pre>
 *
 * @author njensen
 */
@DynamicSerialize
public abstract class AbstractRequest {

    @DynamicSerializeElement
    protected String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String toString() {
        StringBuilder rval = new StringBuilder(getClass().getSimpleName());
        rval.append(" filename[");
        rval.append(filename);
        rval.append("]");
        return rval.toString();
    }

    /**
     * @return the general type of request this is (e.g. store, retrieve, etc.)
     */
    public abstract RequestType getType();

    public enum RequestType {
        STORE, RETRIEVE, DELETE, COPY, REPACK
    }

}