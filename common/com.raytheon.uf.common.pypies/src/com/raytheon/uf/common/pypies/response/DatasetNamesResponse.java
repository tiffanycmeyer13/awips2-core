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

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * PyPies dataset names response
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 30, 2021 8374       randerso     Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

@DynamicSerialize
public class DatasetNamesResponse extends AbstractResponse {

    /**
     * Nullary constructor for dynamic serialization
     */
    public DatasetNamesResponse() {

    }

    /**
     * Constructor
     *
     * @param datasets
     *            the dataset names
     */
    public DatasetNamesResponse(String[] datasets) {
        this.datasets = datasets;
    }

    @DynamicSerializeElement
    private String[] datasets;

    /**
     * @return the datasets
     */
    public String[] getDatasets() {
        return datasets;
    }

    /**
     * @param datasets
     *            the datasets to set
     */
    public void setDatasets(String[] datasets) {
        this.datasets = datasets;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", datasets");
        sb.append(Arrays.toString(datasets));

        return sb.toString();
    }
}
