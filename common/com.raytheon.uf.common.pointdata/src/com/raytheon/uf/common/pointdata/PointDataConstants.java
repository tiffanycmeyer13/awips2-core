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
package com.raytheon.uf.common.pointdata;

/**
 * Constants used in pointdata
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 14, 2009            chammack    Initial creation
 * Apr 10, 2017      6110  tgurney     Add LOCATION_STATIONID
 *
 * </pre>
 *
 * @author chammack
 */

public class PointDataConstants {

    private PointDataConstants() {

    }

    public static final String DATASET_STATIONID = "stationId";

    public static final String DATASET_REFTIME = "refTime";

    public static final String DATASET_FORECASTHR = "forecastHr";

    public static final String LOCATION_STATIONID = "location.stationId";

}
