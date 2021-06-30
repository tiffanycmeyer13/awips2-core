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
package com.raytheon.uf.edex.database.handlers;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.dataquery.requests.TimeQueryRequest;
import com.raytheon.uf.common.dataquery.requests.TimeQueryRequestSet;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.database.dao.IDaoConfigFactory;

/**
 * TODO Add Description
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 30, 2011            rjpeter     Initial creation
 * Apr 21, 2021 7849       mapeters    Add {@link IDaoConfigFactory} constructor arg
 *
 * </pre>
 *
 * @author rjpeter
 */
public class TimeQueryHandlerSet
        implements IRequestHandler<TimeQueryRequestSet> {

    private final IDaoConfigFactory daoConfigFactory;

    public TimeQueryHandlerSet(IDaoConfigFactory daoConfigFactory) {
        this.daoConfigFactory = daoConfigFactory;
    }

    @Override
    public List<List<DataTime>> handleRequest(TimeQueryRequestSet request)
            throws Exception {
        TimeQueryRequest[] queries = request.getRequests();
        List<List<DataTime>> results = new ArrayList<>(queries.length);
        TimeQueryHandler handler = new TimeQueryHandler(daoConfigFactory);
        for (TimeQueryRequest element : queries) {
            results.add(handler.handleRequest(element));
        }
        return results;
    }
}
