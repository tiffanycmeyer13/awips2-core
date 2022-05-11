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
package com.raytheon.uf.common.derivparam.python;

import java.util.List;

import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.python.concurrent.IPythonExecutor;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;

import jep.JepException;

/**
 * Executor for calling executeFunction on a MasterDerivScript
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 04, 2013 2041       bsteffen    Initial creation
 * Jan 26, 2022 8741       njensen     Added performance logging
 *
 * </pre>
 *
 * @author bsteffen
 */

public class MasterDerivScriptExecutor
        implements IPythonExecutor<MasterDerivScript, List<IDataRecord>> {

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("Derived Parameter Script Executor");

    private final String name;

    private final List<Object> arguments;

    public MasterDerivScriptExecutor(String name, List<Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<IDataRecord> execute(MasterDerivScript script)
            throws JepException {
        long t0 = System.currentTimeMillis();
        List<IDataRecord> retVal = (List<IDataRecord>) script
                .executeFunction(name, arguments);
        long t1 = System.currentTimeMillis();
        perfLog.logDuration("Executing function " + name, t1 - t0);

        return retVal;
    }

}
