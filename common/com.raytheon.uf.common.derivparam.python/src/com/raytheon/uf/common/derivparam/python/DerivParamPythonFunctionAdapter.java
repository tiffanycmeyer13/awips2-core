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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;

import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.derivparam.DerivParamFunctionType.FunctionArgument;
import com.raytheon.uf.common.derivparam.IDerivParamFunctionAdapter;
import com.raytheon.uf.common.derivparam.library.DerivedParameterGenerator;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.python.concurrent.PythonJobCoordinator;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Python derived parameter adapter
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 16, 2010            mschenke    Initial creation
 * Jun 04, 2013 2041       bsteffen    Switch derived parameters to use
 *                                     concurrent python for threading.
 * Jan 30, 2014  #2725     ekladstrup  Add name and extension get methods
 *                                     after removing RCP extension point
 * Dec 14, 2015  #4816     dgilling    Support refactored PythonJobCoordinator API.
 * Jan 26, 2022   8741     njensen     Added performance logging
 *
 * </pre>
 *
 * @author mschenke
 */

public class DerivParamPythonFunctionAdapter
        implements IDerivParamFunctionAdapter {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DerivParamPythonFunctionAdapter.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("Derived Parameter Python");

    private static final String PYTHON = "python";

    private static final String TEMPLATE_FILE = DerivedParameterGenerator.DERIV_PARAM_DIR
            + File.separator + PYTHON + File.separator + "functionTemplate.txt";

    private static final int MAX_THREADS = Integer
            .getInteger("com.raytheon.uf.viz.derivparam.python.threads", 3);

    private static final String THREAD_POOL_NAME = "DerivedParameterPython";

    private PythonJobCoordinator<MasterDerivScript> coordinator;

    @Override
    public String createNewFunction(String functionName,
            FunctionArgument[] arguments) {
        File template = PathManagerFactory.getPathManager()
                .getStaticFile(TEMPLATE_FILE);
        try {
            String templateText = new String(
                    Files.readAllBytes(Paths.get(template.getAbsolutePath())));
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            String date = dateFormat.format(new Date());
            StringJoiner argList = new StringJoiner(",");
            for (FunctionArgument arg : arguments) {
                argList.add(arg.name);
            }
            return String.format(templateText, date, argList.toString());
        } catch (IOException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error reading in python template file", e);
        }
        return null;
    }

    @Override
    public String[] getArgumentTypes() {
        return new String[] { "Parameter" };
    }

    @Override
    public void init() {
        if (coordinator != null) {
            coordinator.shutdown();
        }
        coordinator = new PythonJobCoordinator<>(MAX_THREADS, THREAD_POOL_NAME,
                new MasterDerivScriptFactory());
    }

    @Override
    public List<IDataRecord> executeFunction(String name,
            List<Object> arguments) throws ExecutionException {
        try {
            long t0 = System.currentTimeMillis();
            List<IDataRecord> retVal = coordinator
                    .submitJob(new MasterDerivScriptExecutor(name, arguments))
                    .get();
            long t1 = System.currentTimeMillis();
            perfLog.logDuration("Submit Python job for function " + name
                    + " and waited for result", t1 - t0);
            return retVal;
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void shutdown() {
        if (coordinator != null) {
            coordinator.shutdown();
        }
    }

    @Override
    public String getName() {
        return "Python";
    }

    @Override
    public String getExtension() {
        return "py";
    }

}
