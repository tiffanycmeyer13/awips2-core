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
package com.raytheon.uf.viz.personalities.cave.workbench;

import java.time.Duration;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;

/**
 * The UIPingJob periodically updates lastPingTimeMillis from the a job on the
 * UI thread.
 * <p>
 * The UIResponseMonitor job periodically checks lastPingTimeMillis to ensure it
 * has been updated within the response threshold and if not logs a stack trace
 * of the UI thread to help identify what may be tying up the UI thread.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 05, 2021  8339     mchan     Initial creation
 * Feb 25, 2021  8339     randerso  Moved to com.raytheon.uf.viz.personalities.
 *                                  cave.workbench
 * Dec 16, 2021  8341     randerso  Changed to use performance logging
 * Feb 01, 2022  8341     randerso  Move Startup message to the perfLog
 *
 * </pre>
 *
 * @author mchan
 */
public class UIThreadMonitor {
    private static class UIPingJob extends UIJob {

        private boolean canceled = false;

        public UIPingJob() {
            super("UIPingJob");
            setSystem(true);
        }

        @Override
        protected void canceling() {
            super.canceling();
            canceled = true;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            lastPingTimeMillis = System.currentTimeMillis();

            if (!canceled) {
                schedule(PING_INTERVAL.toMillis());
            }
            return Status.OK_STATUS;
        }
    }

    private static class MonitorJob extends Job {
        private boolean canceled = false;

        public MonitorJob() {
            super("UIResponseMonitor");
            setSystem(true);
        }

        @Override
        protected void canceling() {
            super.canceling();
            canceled = true;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            long elapsedTimeMillis = System.currentTimeMillis()
                    - lastPingTimeMillis;
            if (elapsedTimeMillis > RESPONSE_THRESHOLD.toMillis()) {
                Thread uiThread = Display.getDefault().getThread();
                StackTraceElement[] stackTrace = uiThread.getStackTrace();
                StringBuilder stackTraceBuilder = new StringBuilder(String
                        .format("UI Thread stalled for more than %d ms: \n",
                                RESPONSE_THRESHOLD.toMillis()));
                for (StackTraceElement traceElement : stackTrace) {
                    stackTraceBuilder.append("\tat " + traceElement + "\n");
                }
                perfLog.log(stackTraceBuilder.toString());
            }

            if (!canceled) {
                schedule(RESPONSE_THRESHOLD.toMillis());
            }
            return Status.OK_STATUS;
        }
    }

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(UIThreadMonitor.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("UIThreadMonitor");

    private static final long DEFAULT_RESPONSE_THRESHOLD = 500L;

    private static final long DIVISOR = 2L;

    private static final Duration RESPONSE_THRESHOLD;

    private static final Duration PING_INTERVAL;

    /**
     * Check the ui.thread.monitor.threshold.millis system property value is
     * greater than the divisor used to derived the ping interval. If it is not
     * then ignore the provided value and set the threshold to the default
     * value.
     */
    static {
        long threshold = Long.getLong("ui.thread.monitor.threshold.millis",
                DEFAULT_RESPONSE_THRESHOLD);
        if (DIVISOR > threshold) {
            threshold = DEFAULT_RESPONSE_THRESHOLD;
            statusHandler.error(
                    "The ui.thread.monitor.threshold.millis property must be at least "
                            + DIVISOR
                            + ". Property value will not be used. Default value will be used instead.");
        }
        RESPONSE_THRESHOLD = Duration.ofMillis(threshold);
        PING_INTERVAL = RESPONSE_THRESHOLD.dividedBy(DIVISOR);
    }

    private static volatile long lastPingTimeMillis;

    private static UIPingJob pingJob = new UIPingJob();

    private static MonitorJob monitorJob = new MonitorJob();

    /**
     * Start up the UIPingJob and UIResponseMonitor job to detect when the UI is
     * slow to responding.
     */
    public static void start() {
        lastPingTimeMillis = System.currentTimeMillis();
        pingJob.schedule();

        monitorJob.schedule(RESPONSE_THRESHOLD.toMillis());
        perfLog.log("UIThreadMonitor started. Threshold is "
                + RESPONSE_THRESHOLD.toMillis() + " ms, ping interval is "

                + PING_INTERVAL.toMillis() + " ms");
    }

    /**
     * Stop the UIPingJob and UIResponseMonitor jobs
     */
    public static void stop() {
        /* Cancel the monitor job so it doesn't log UI stalls during shutdown */
        monitorJob.cancel();

        /* Wait to be sure monitor job is stopped */
        try {
            monitorJob.join();
        } catch (InterruptedException e) {
            statusHandler.error(e.getLocalizedMessage(), e);
        }

        /* Stop the ping job */
        pingJob.cancel();
    }

}
