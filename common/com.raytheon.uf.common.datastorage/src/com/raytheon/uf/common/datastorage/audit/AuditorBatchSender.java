/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastorage.audit;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that sends batched events for an
 * {@link AbstractDataStorageAuditerProxy} on a timer.
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 14, 2023   9076      smoorthy    initial creation
 * </pre>
 * 
 * @author smoorthy
 */
class AuditorBatchSender {

    private final AbstractDataStorageAuditerProxy auditor;

    private static Timer sendBatchTimer = new Timer(true);

    public static final int RATE = Integer
            .getInteger("data.storage.auditer.rate", 5000);


    public AuditorBatchSender(AbstractDataStorageAuditerProxy auditor) {
        this.auditor = auditor;
        BatchTask task = new BatchTask();
        long safeRate = RATE <= 0 ? 5000: RATE;
        sendBatchTimer.scheduleAtFixedRate(task, 1000, safeRate);
    }

    private class BatchTask extends TimerTask {

        @Override
        public void run() {
            auditor.sendBatchedEvents();
        }
    }
}
