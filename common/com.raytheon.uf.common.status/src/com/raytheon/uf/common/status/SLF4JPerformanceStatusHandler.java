package com.raytheon.uf.common.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slf4j Performance status handler.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 14, 2013   1584     mpduff      Initial creation
 * Jun 27, 2013   2142     njensen     Switched to SLF4J
 * Jan 26, 2022   8741     njensen     Renamed class, formerly known as
 *                                     Log4JPerformanceStatusHandler
 *
 * </pre>
 *
 * @author mpduff
 */

public class SLF4JPerformanceStatusHandler
        implements IPerformanceStatusHandler {

    /** Logger */
    private final Logger perfLog = LoggerFactory.getLogger("PerformanceLogger");

    /** Prefix to append to all log messages */
    private final String prefix;

    /**
     * Constructor.
     *
     * @param prefix
     *            Message prefix
     */
    public SLF4JPerformanceStatusHandler(String prefix) {
        this.prefix = prefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(String message) {
        perfLog.info(prefix + " " + message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logDuration(String message, long timeMs) {
        perfLog.info(prefix + " " + message + " took " + timeMs + " ms");
    }
}
