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
package com.raytheon.uf.common.status;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Abstract implementation of the Handler Factories.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 10, 2011            bgonzale    Initial creation
 * Sep 09, 2014 3549       njensen     log() uses Priority to pick PrintStream
 * Jul 18, 2017 6316       njensen     Include timestamp on PrintStream log()
 * 
 * </pre>
 * 
 * @author bgonzale
 */

public abstract class AbstractHandlerFactory implements
        IUFStatusHandlerFactory, Observer {

    private final String defaultCategory;

    private final Map<String, IUFStatusHandler> namedHandlers = new HashMap<>();

    private FilterPatternContainer sourceFilters;

    private ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            return df;
        }
    };

    /**
     * 
     */
    public AbstractHandlerFactory(String category) {
        this.defaultCategory = category;
    }

    @Override
    public IUFStatusHandler hasHandler(String name) {
        return namedHandlers.get(name);
    }

    @Override
    public IUFStatusHandler getInstance(String name) {
        IUFStatusHandler handler = namedHandlers.get(name);

        if (handler == null) {
            handler = getInstance(name, null, name);
            namedHandlers.put(name, handler);
        }
        return handler;
    }

    @Override
    public IUFStatusHandler getInstance(Class<?> cls) {
        return getInstance(cls, null, null);
    }

    @Override
    public IUFStatusHandler getInstance(Class<?> cls, String source) {
        return getInstance(cls, null, source);
    }

    @Override
    public IUFStatusHandler getInstance(Class<?> cls, String category,
            String source) {
        String pluginId = cls.getName();
        return getInstance(pluginId, category, source);
    }

    @Override
    public IUFStatusHandler getInstance(String pluginId, String source) {
        return getInstance(pluginId, null, source);
    }

    @Override
    public IUFStatusHandler getInstance(String pluginId, String category,
            String source) {
        if (source == null) {
            return createInstance(this, pluginId, getCategory(category));
        } else {
            return createInstance(pluginId, getCategory(category), source);
        }
    }

    public String getSource(String source, String pluginId) {
        if (source == null) {
            source = getSource(pluginId);
        }
        return source;
    }

    public String getCategory(String category) {
        return category == null ? defaultCategory : category;
    }

    @Override
    public IUFStatusHandler getMonitorInstance(Class<?> cls) {
        return getMonitorInstance(cls, null);
    }

    @Override
    public IUFStatusHandler getMonitorInstance(Class<?> cls,
            String monitorSource) {
        String pluginId = cls.getName();
        monitorSource = (monitorSource == null) ? getSource(pluginId)
                : monitorSource;
        return createMonitorInstance(pluginId, monitorSource);
    }

    private String getSource(String pluginId) {
        String source = null;
        FilterPattern fPattern = getSourceFilters().findFilter(pluginId);

        if (fPattern != null) {
            source = fPattern.getName();
        }
        return source;
    }

    private FilterPatternContainer getSourceFilters() {
        if (sourceFilters == null) {
            sourceFilters = createSourceContainer();
        }
        return sourceFilters;
    }

    @Override
    public void update(Observable o, Object arg) {
        getSourceFilters();
    }

    @Override
    public void log(Priority priority, StatusHandler statusHandler,
            String message, Throwable throwable) {
        String source = statusHandler.getSource();
        String pluginId = statusHandler.getPluginId();
        if (source == null) {
            source = getSource(source, pluginId);
            statusHandler.setSource(source);
        }
        this.log(priority, pluginId, statusHandler.getCategory(), source,
                message, throwable);
    }

    protected void log(Priority priority, String pluginId, String category,
            String source, String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(priority).append(' ');
        sb.append(sdf.get().format(System.currentTimeMillis())).append(" ");
        // sb.append("[").append(Thread.currentThread().getName()).append("] ");
        sb.append(pluginId).append(": ");
        if (category != null) {
            sb.append(category);
            if (source != null) {
                sb.append(": ");
                sb.append(source);
            }
            sb.append(" - ");
        }
        sb.append(message);

        PrintStream ps = null;
        switch (priority) {
        case CRITICAL:
        case SIGNIFICANT:
        case PROBLEM:
            ps = System.err;
            break;
        default:
            ps = System.out;
            break;
        }

        ps.println(sb.toString());
        if (throwable != null) {
            throwable.printStackTrace(ps);
        }
    }

    protected abstract IUFStatusHandler createMonitorInstance(String pluginId,
            String monitorSource);

    protected abstract IUFStatusHandler createInstance(String pluginId,
            String category, String source);

    protected abstract FilterPatternContainer createSourceContainer();
}
