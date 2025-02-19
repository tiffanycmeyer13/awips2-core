package com.raytheon.uf.viz.spring.dm;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * 
 * Custom version of Spring OSGi ContextLoaderListener to turn off xml
 * validation
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 12, 2010            mschenke     Initial creation
 * Jan 24, 2013 1522       bkowal       Halt initialization if a p2 installation 
 *                                      has been started
 * Mar 05, 2013 1754       djohnson     Catch exceptions and allow as much of the Spring container to boot as possible.
 * May 23, 2013 2005       njensen      Added springSuccess flag
 * Nov 12, 2013 2361       njensen      Print out time spent on each spring context
 * Jun 27, 2017 6316       njensen      Track bundle start time
 * Dec 11, 2017            mjames      Less logging (re-implemented 3/15/23)
 * 
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class Activator implements BundleActivator {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.raytheon.uf.viz.spring.dm";

    private static final String SPRING_PATH = "res" + IPath.SEPARATOR
            + "spring";

    private static final String SPRING_FILE_EXT = "*.xml";

    private static final Pattern COMMA_SPLIT = Pattern.compile("[,]");

    private static final Pattern SEMICOLON_SPLIT = Pattern.compile("[;]");

    // The shared instance
    private static Activator plugin;

    private boolean springSuccess = true;

    /**
     * Start time of this plugin. Since this plugin is usually the first plugin
     * started outside of Eclipse's plugins, this works as a good measure for
     * total startup time.
     */
    private long startTime;

    /**
     * The constructor
     */
    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        startTime = System.currentTimeMillis();
        if (this.isInstallOperation()) {
            return;
        }
        plugin = this;

        Map<String, OSGIXmlApplicationContext> contextMap = new HashMap<>();
        Set<String> processing = new HashSet<>();
        Bundle[] bundles = context.getBundles();
        Map<String, Bundle> bundleMap = new HashMap<>();
        for (Bundle b : bundles) {
            bundleMap.put(b.getSymbolicName(), b);
        }
        for (Bundle b : bundles) {
            createContext(bundleMap, contextMap, b, processing);
        }
        System.out.println("Spring initialization took: "
                + (System.currentTimeMillis() - startTime) + " ms");
    }

    private OSGIXmlApplicationContext createContext(
            Map<String, Bundle> bundles,
            Map<String, OSGIXmlApplicationContext> contextMap, Bundle bundle,
            Set<String> processing) {
        BundleResolver bundleResolver = new BundleResolver();
        String bundleName = bundle.getSymbolicName();
        OSGIXmlApplicationContext appCtx = contextMap.get(bundleName);
        if (!contextMap.containsKey(bundleName)
                && !bundleName.contains(".edex.")) {
            if (processing.contains(bundleName)) {
                springSuccess = false;
                throw new RuntimeException(
                        "Found recursive spring dependency while processing plugins: "
                                + bundleName);
            }
            processing.add(bundleName);

            // No context created yet and not edex project, check for files
            Enumeration<?> entries = bundle.findEntries(SPRING_PATH,
                    SPRING_FILE_EXT, true);
            if (entries != null) {
                List<String> files = new ArrayList<>();
                while (entries.hasMoreElements()) {
                    URL url = (URL) entries.nextElement();
                    try {
                        url = FileLocator.toFileURL(url);
                        files.add(url.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Error resolving spring file: " + url, e);
                    }
                }
                if (!files.isEmpty()) {
                    // Files found, check for dependencies
                    Collection<Bundle> requiredBundles = bundleResolver
                            .getRequiredBundles(bundle);
                    List<OSGIXmlApplicationContext> parentContexts = new ArrayList<>();
                    for (Bundle requiredBundle : requiredBundles) {
                        // Found bundle, process context for bundle
                        OSGIXmlApplicationContext parent = createContext(
                                bundles, contextMap, requiredBundle, processing);
                        if (parent != null) {
                            // Context found, add to list
                            parentContexts.add(parent);
                        }
                    }

                    try {
                        long t0 = System.currentTimeMillis();
                        if (!parentContexts.isEmpty()) {
                            // Context with parent context
                            appCtx = new OSGIXmlApplicationContext(
                                    new OSGIGroupApplicationContext(bundle,
                                            parentContexts),
                                    files.toArray(new String[0]), bundle);
                        } else {
                            // No parent context required
                            appCtx = new OSGIXmlApplicationContext(
                                    files.toArray(new String[0]), bundle);
                        }
                    } catch (Throwable t) {
                        // No access to the statusHandler yet, so print the
                        // stack trace to the console. By catching this, we also
                        // allow as many beans as possible to continue to be
                        // created
                        System.err
                                .println("Errors booting the Spring container.  CAVE will not be fully functional.");
                        t.printStackTrace();
                        springSuccess = false;
                    }
                }
            }
            contextMap.put(bundleName, appCtx);
        }
        processing.remove(bundleName);
        return appCtx;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Based on the command line arguments, determine whether or not an Eclipse
     * p2 repository will be installed
     * 
     * @return true if an Eclipse p2 repository is going to be installed, false
     *         otherwise
     */
    private boolean isInstallOperation() {
        final String P2_DIRECTOR = "org.eclipse.equinox.p2.director";

        /**
         * We look at the command line arguments instead of the program
         * arguments (com.raytheon.uf.viz.application.ProgramArguments) because
         * the command line arguments include almost everything that was passed
         * as an argument to the Eclipse executable instead of just what CAVE is
         * interested in.
         */
        for (String argument : Platform.getCommandLineArgs()) {
            if (P2_DIRECTOR.equals(argument)) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    public boolean isSpringInitSuccessful() {
        return springSuccess;
    }

    public long getApplicationStartTime() {
        return startTime;
    }
}
