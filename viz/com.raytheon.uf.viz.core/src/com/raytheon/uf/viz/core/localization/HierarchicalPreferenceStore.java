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

package com.raytheon.uf.viz.core.localization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Defines a hierarchical preference store, backed by Apache Commons
 * configurator. A config.xml file is expected in the CAVE_CONFIG hierarchy for
 * the bundleId the store is created with. The config.xml file must conform to
 * the Apache {@link XMLConfiguration} format. This preference store is
 * hierarchical in that it will search for preferences based on the localization
 * search hierarchy.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 16, 2007            chammack    Initial Creation.
 * Feb 5, 2008             chammack    Add API to support set/remove at any tier 
 *                                     and support for listeners
 * Feb 27, 2014 2861       mschenke    Rewrote to add thread safety and handle
 *                                     all LocalizationLevels
 * Aug 18, 2015 3806       njensen     Use SaveableOutputStream to save
 * Dec 09, 2015 4834       njensen     Get latest ILocalizationFile on reload()
 * Jun 12, 2017 6297       bsteffen    Make listeners thread safe.
 * 
 * </pre>
 * 
 * @author chammack
 */
public class HierarchicalPreferenceStore implements IPersistentPreferenceStore {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(HierarchicalPreferenceStore.class, "CAVE");

    private static class LocalizationConfiguration {

        private final LocalizationLevel level;

        private final String configFilePath;

        private XMLConfiguration config;

        private boolean loaded = false;

        private boolean dirty = false;

        public LocalizationConfiguration(LocalizationLevel level,
                String configFilePath) {
            this.level = level;
            this.configFilePath = configFilePath;
            this.config = new XMLConfiguration();
        }

        public synchronized XMLConfiguration accessConfiguration() {
            if (!loaded) {
                // Loaded flag is used for first access
                loaded = true;
                try {
                    reload();
                } catch (LocalizationException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                }
            }
            return config;
        }

        public void markDirty() {
            dirty = true;
        }

        public boolean isDirty() {
            return dirty;
        }

        public void save() throws LocalizationException {
            ILocalizationFile file = getILocalizationFile(level);
            if (isDirty()) {
                try (SaveableOutputStream sos = file.openOutputStream()) {
                    config.save(sos);
                    sos.save();
                    dirty = false;
                } catch (ConfigurationException | IOException e) {
                    throw new LocalizationException(
                            "Error saving config.xml into localization", e);
                }
            }
        }

        public void reload() throws LocalizationException {
            ILocalizationFile file = getILocalizationFile(level);
            if (file.exists()) {
                try (InputStream in = file.openInputStream()) {
                    XMLConfiguration newConfig = new XMLConfiguration();
                    newConfig.load(in);
                    this.config = newConfig;
                } catch (IOException | ConfigurationException e) {
                    throw new LocalizationException(
                            "Error loading localization file into config", e);
                }
            }
        }

        /**
         * Gets the ILocalizationFile for the preferences at the specified
         * LocalizationLevel.
         * 
         * @param level
         * @return
         */
        private ILocalizationFile getILocalizationFile(
                LocalizationLevel level) {
            IPathManager mgr = PathManagerFactory.getPathManager();
            LocalizationFile configFile = mgr.getLocalizationFile(
                    mgr.getContext(LocalizationType.CAVE_CONFIG, level),
                    configFilePath);
            return configFile;
        }
    }

    private static final LocalizationLevel COMBINED = null;

    private final String configFilePath;

    private final LocalizationLevel defaultPersistLevel;

    private final Map<LocalizationLevel, LocalizationConfiguration> configMap = new HashMap<>();

    private MapConfiguration defaults;

    private final Set<IPropertyChangeListener> propertyChangeListeners = new CopyOnWriteArraySet<>();

    /**
     * We can't register the listener in the constructor since the IPathManager
     * may not be ready, so we have to register it on demand once a component
     * actually uses this preference store.
     */
    private volatile boolean listenerRegistered = false;

    public static final String EMPTY_CONFIGURATION = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>\n"
            + "<configuration>\n" + "</configuration>\n";

    /**
     * Constructor
     * 
     * @param activator
     */
    public HierarchicalPreferenceStore(Plugin activator) {
        this(activator.getBundle().getSymbolicName());
    }

    /**
     * 
     * @param bundleId
     */
    public HierarchicalPreferenceStore(String bundleId) {
        this(bundleId, LocalizationLevel.USER);
    }

    /**
     * 
     * @param activator
     * @param defaultLevel
     */
    public HierarchicalPreferenceStore(Plugin activator,
            LocalizationLevel defaultLevel) {
        this(activator.getBundle().getSymbolicName(), defaultLevel);
    }

    /**
     * 
     * @param bundleId
     * @param defaultLevel
     */
    public HierarchicalPreferenceStore(String bundleId,
            LocalizationLevel defaultLevel) {
        this.configFilePath = bundleId + IPathManager.SEPARATOR + "config.xml";
        this.defaultPersistLevel = defaultLevel;
    }

    private synchronized MapConfiguration getDefaultConfig() {
        if (defaults == null) {
            defaults = new MapConfiguration(new HashMap<>());
            // Populate defaults with base first, setDefault* method will
            // override what was in BASE
            LocalizationConfiguration baseConfig = new LocalizationConfiguration(
                    LocalizationLevel.BASE, configFilePath);
            defaults.append(baseConfig.accessConfiguration());
        }
        return defaults;
    }

    /**
     * @return The configuration search hierarchy
     */
    private Collection<LocalizationConfiguration> getSearchHierarchy() {
        if (!listenerRegistered) {
            synchronized (this) {
                if (!listenerRegistered) {
                    PathManagerFactory.getPathManager()
                            .addLocalizationPathObserver(configFilePath,
                                    new ILocalizationPathObserver() {
                                        @Override
                                        public void fileChanged(
                                                ILocalizationFile file) {
                                            reloadConfig(file.getContext()
                                                    .getLocalizationLevel());
                                        }
                                    });
                }
                listenerRegistered = true;
            }
        }

        LocalizationLevel[] levels = PathManagerFactory.getPathManager()
                .getAvailableLevels();
        List<LocalizationConfiguration> configs = new ArrayList<>(
                levels.length);
        for (int i = levels.length - 1; i >= 0; i--) {
            LocalizationLevel level = levels[i];
            // Skip BASE as it is loaded in defaults
            if (level != LocalizationLevel.BASE) {
                configs.add(getConfigurationForLevel(level));
            }
        }
        return configs;
    }

    /**
     * @param name
     * @return The highest {@link LocalizationConfiguration} in the search
     *         hierarchy that contains the key name or null if none
     */
    private LocalizationConfiguration getConfigurationForLevel(
            LocalizationLevel level, String name) {
        if (level == COMBINED) {
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                if (xmlConfig.containsKey(name)) {
                    return config;
                }
            }
        } else {
            return getConfigurationForLevel(level);
        }
        return null;
    }

    /**
     * @param localizationLevel
     * @return {@link LocalizationConfiguration} for the specified level
     */
    private LocalizationConfiguration getConfigurationForLevel(
            LocalizationLevel localizationLevel) {
        LocalizationConfiguration config;
        synchronized (configMap) {
            config = configMap.get(localizationLevel);
            if (config == null) {
                config = new LocalizationConfiguration(localizationLevel,
                        configFilePath);
                configMap.put(localizationLevel, config);
            }
        }
        return config;
    }

    private void reloadConfig(LocalizationLevel level) {
        LocalizationConfiguration config;
        synchronized (configMap) {
            config = configMap.get(level);
        }
        if (config != null && config.loaded) {
            // Capture old properties
            Map<String, Object> oldPropertyMapping = new HashMap<>();
            for (String key : getKeys()) {
                oldPropertyMapping.put(key, getProperty(key));
            }

            try {
                config.reload();
            } catch (LocalizationException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }

            // Update changed properties
            for (String key : getKeys()) {
                Object newProperty = getProperty(key);
                Object oldProperty = oldPropertyMapping.get(key);
                if ((oldProperty != null && newProperty == null)
                        || (oldProperty == null && newProperty != null)
                        || (oldProperty != null
                                && !oldProperty.equals(newProperty))) {
                    firePropertyChangeEvent(key, oldProperty, newProperty);
                }
            }
        }
    }

    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        this.propertyChangeListeners.add(listener);
    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {
        this.propertyChangeListeners.remove(listener);
    }

    @Override
    public boolean contains(String name) {
        for (LocalizationConfiguration config : getSearchHierarchy()) {
            if (config.accessConfiguration().containsKey(name)) {
                return true;
            }
        }
        return getDefaultConfig().containsKey(name);
    }

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue,
            Object newValue) {
        // The following criteria meets the Eclipse contract
        if (oldValue == null || oldValue.equals(newValue)) {
            return;
        }

        final PropertyChangeEvent pe = new PropertyChangeEvent(this, name,
                oldValue, newValue);
        for (final IPropertyChangeListener listener : this.propertyChangeListeners) {
            SafeRunnable.run(new SafeRunnable(
                    JFaceResources.getString("PreferenceStore.changeError")) {
                @Override
                public void run() {
                    listener.propertyChange(pe);
                }
            });

        }
    }

    @Override
    public boolean getDefaultBoolean(String name) {
        return getDefaultConfig().getBoolean(name,
                IPreferenceStore.BOOLEAN_DEFAULT_DEFAULT);
    }

    @Override
    public double getDefaultDouble(String name) {
        return getDefaultConfig().getDouble(name,
                IPreferenceStore.DOUBLE_DEFAULT_DEFAULT);
    }

    @Override
    public float getDefaultFloat(String name) {
        return getDefaultConfig().getFloat(name,
                IPreferenceStore.FLOAT_DEFAULT_DEFAULT);
    }

    @Override
    public int getDefaultInt(String name) {
        return getDefaultConfig().getInt(name,
                IPreferenceStore.INT_DEFAULT_DEFAULT);
    }

    @Override
    public long getDefaultLong(String name) {
        return getDefaultConfig().getLong(name,
                IPreferenceStore.LONG_DEFAULT_DEFAULT);
    }

    @Override
    public String getDefaultString(String name) {
        return getDefaultConfig().getString(name,
                IPreferenceStore.STRING_DEFAULT_DEFAULT);
    }

    /**
     * Return a boolean value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public boolean getBoolean(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getBoolean(name);
            }
        }
        return getDefaultBoolean(name);
        
    }

    @Override
    public boolean getBoolean(String name) {
        return getBoolean(COMBINED, name);
    }

    /**
     * Return a double value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public double getDouble(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getDouble(name);
            }
        }
        return getDefaultDouble(name);
    }

    @Override
    public double getDouble(String name) {
        return getDouble(COMBINED, name);
    }

    /**
     * Return a float value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public float getFloat(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getFloat(name);
            }
        }
        return getDefaultFloat(name);
    }

    @Override
    public float getFloat(String name) {
        return getFloat(COMBINED, name);
    }

    public float[] getFloatArray(LocalizationLevel level, String name) {
        String[] s = getStringArray(level, name);
        float[] ret = new float[s.length];

        for (int i = 0; i < s.length; i++) {
            try {
                ret[i] = Float.parseFloat(s[i]);
            } catch (NumberFormatException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Invalid float value in preference " + name, e);
            }
        }
        return ret;
    }

    public float[] getFloatArray(String name) {
        return getFloatArray(COMBINED, name);
    }

    /**
     * Return a integer value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public int getInt(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getInt(name);
            }
        }
        return getDefaultInt(name);
    }

    @Override
    public int getInt(String name) {
        return getInt(COMBINED, name);
    }

    /**
     * Return a long value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public long getLong(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getLong(name);
            }
        }
        return getDefaultLong(name);
    }

    @Override
    public long getLong(String name) {
        return getLong(COMBINED, name);
    }

    /**
     * Return a String value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public String getString(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getString(name);
            }
        }
        return getDefaultString(name);
    }

    public String[] getStringArray(String name) {
        return getStringArray(COMBINED, name);
    }

    /**
     * Return a String value given a specific level
     * 
     * @param level
     *            the hierarchical level
     * @param name
     *            the parameter name
     * @return the value
     */
    public String[] getStringArray(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getStringArray(name);
            }
        }
        return getDefaultConfig().getStringArray(name);
}

    @Override
    public String getString(String name) {
        return getString(COMBINED, name);
    }

    /**
     * Gets the property Object for the key name at the level specified
     * 
     * @param level
     * @param name
     * @return
     */
    public Object getProperty(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            if (xmlConfig.containsKey(name)) {
                return xmlConfig.getProperty(name);
            }
        }
        return getDefaultConfig().getProperty(name);
    }

    /**
     * Gets the hierarchical property Object for the key name
     * 
     * @param name
     * @return
     */
    public Object getProperty(String name) {
        return getProperty(COMBINED, name);
    }

    /**
     * Checks if the property value for the level is the same as the default
     * value
     * 
     * @param level
     * @param name
     * @return
     */
    public boolean isDefault(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level,
                name);
        if (config != null) {
            Object object = config.accessConfiguration().getProperty(name);
            Object defaultObject = getDefaultConfig().getProperty(name);
            return (object == defaultObject) || (object != null
                    && defaultObject != null && object.equals(defaultObject));
        }
        return true;
    }

    @Override
    public boolean isDefault(String name) {
        return isDefault(COMBINED, name);
    }

    @Override
    public boolean needsSaving() {
        for (LocalizationConfiguration config : getSearchHierarchy()) {
            if (config.isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void putValue(String name, String value) {
        LocalizationConfiguration config = getConfigurationForLevel(
                defaultPersistLevel);
        XMLConfiguration xmlConfig = config.accessConfiguration();
        xmlConfig.setProperty(name, value);
        config.markDirty();
    }

    @Override
    public void setDefault(String name, boolean value) {
        getDefaultConfig().setProperty(name, value);
    }

    @Override
    public void setDefault(String name, double value) {
        getDefaultConfig().setProperty(name, value);
    }

    @Override
    public void setDefault(String name, float value) {
        getDefaultConfig().setProperty(name, value);
    }

    @Override
    public void setDefault(String name, int value) {
        getDefaultConfig().setProperty(name, value);
    }

    @Override
    public void setDefault(String name, long value) {
        getDefaultConfig().setProperty(name, value);
    }

    @Override
    public void setDefault(String name, String defaultObject) {
        getDefaultConfig().setProperty(name, defaultObject);
    }

    @Override
    public void setToDefault(String name) {
        removeFromLevel(defaultPersistLevel, name);
    }

    public void setValue(LocalizationLevel level, String name, boolean value) {
        boolean oldValue;
        if (level == COMBINED) {
            oldValue = getBoolean(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getBoolean(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }
        if (oldValue != value) {
            firePropertyChangeEvent(name, new Boolean(oldValue),
                    new Boolean(value));
        }
    }

    @Override
    public void setValue(String name, boolean value) {
        setValue(defaultPersistLevel, name, value);
    }

    public void setValue(LocalizationLevel level, String name, double value) {
        double oldValue;
        if (level == COMBINED) {
            oldValue = getDouble(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getDouble(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }
        if (oldValue != value) {
            firePropertyChangeEvent(name, new Double(oldValue),
                    new Double(value));
        }
    }

    @Override
    public void setValue(String name, double value) {
        setValue(defaultPersistLevel, name, value);
    }

    public void setValue(LocalizationLevel level, String name, float value) {
        float oldValue;
        if (level == COMBINED) {
            oldValue = getFloat(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getFloat(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }
        if (oldValue != value) {
            firePropertyChangeEvent(name, new Float(oldValue),
                    new Float(value));
        }
    }

    @Override
    public void setValue(String name, float value) {
        setValue(defaultPersistLevel, name, value);
    }

    public void setValue(LocalizationLevel level, String name, int value) {
        int oldValue;
        if (level == COMBINED) {
            oldValue = getInt(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getInt(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }
        if (oldValue != value) {
            firePropertyChangeEvent(name, new Integer(oldValue),
                    new Integer(value));
        }
    }

    @Override
    public void setValue(String name, int value) {
        setValue(defaultPersistLevel, name, value);
    }

    public void setValue(LocalizationLevel level, String name, long value) {
        long oldValue;
        if (level == COMBINED) {
            oldValue = getLong(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getLong(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }

        if (oldValue != value) {
            firePropertyChangeEvent(name, new Long(oldValue), new Long(value));
        }
    }

    @Override
    public void setValue(String name, long value) {
        setValue(defaultPersistLevel, name, value);
    }

    public void setValue(LocalizationLevel level, String name, String value) {
        String oldValue;
        if (level == COMBINED) {
            oldValue = getString(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getString(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }

        if ((oldValue == null && value != null)
                || (oldValue != null && value == null)
                || (oldValue != null && !oldValue.equals(value))) {
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(LocalizationLevel level, String name, String[] value) {
        String[] oldValue;
        if (level == COMBINED) {
            oldValue = getStringArray(name);
            for (LocalizationConfiguration config : getSearchHierarchy()) {
                XMLConfiguration xmlConfig = config.accessConfiguration();
                xmlConfig.setProperty(name, value);
                config.markDirty();
            }
        } else {
            oldValue = getStringArray(level, name);
            LocalizationConfiguration config = getConfigurationForLevel(level);
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.setProperty(name, value);
            config.markDirty();
        }

        if ((oldValue == null && value != null)
                || (oldValue != null && value == null)
                || !Arrays.equals(oldValue, value)) {
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public String[] getKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (LocalizationConfiguration config : getSearchHierarchy()) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            Iterator<?> configKeys = xmlConfig.getKeys();
            while (configKeys.hasNext()) {
                keys.add((String) configKeys.next());
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String[] getKeys(String prefix) {
        Set<String> keys = new LinkedHashSet<>();
        for (LocalizationConfiguration config : getSearchHierarchy()) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            Iterator<?> configKeys = xmlConfig.getKeys(prefix);
            while (configKeys.hasNext()) {
                keys.add((String) configKeys.next());
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    @Override
    public void setValue(String name, String value) {
        setValue(defaultPersistLevel, name, value);
    }

    public void setValue(String name, String[] value) {
        setValue(defaultPersistLevel, name, value);
    }

    @Override
    public void save() throws IOException {
        for (LocalizationConfiguration config : getSearchHierarchy()) {
            try {
                config.save();
            } catch (LocalizationException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
    }

    /**
     * Checks to see if a particular parameter is available at a certain level
     * 
     * @param level
     *            the level to check
     * @param name
     *            the key to check
     * @return if a value is defined for the key at a particular level
     */
    public boolean isAvailableAtLevel(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level);
        return config.accessConfiguration().containsKey(name);
    }

    /**
     * Removes a value in the preference store at a given level
     * 
     * @param level
     *            the level
     * @param name
     *            the key
     */
    public void removeFromLevel(LocalizationLevel level, String name) {
        LocalizationConfiguration config = getConfigurationForLevel(level);
        XMLConfiguration xmlConfig = config.accessConfiguration();
        if (xmlConfig.containsKey(name)) {
            xmlConfig.clearProperty(name);
            config.markDirty();
        }
    }

    /**
     * Clears all user overrides starting with or including this key
     * 
     * @param key
     *            the key to clear overrides
     */
    public void clearUserOverrides(String key) {
        LocalizationConfiguration config = getConfigurationForLevel(
                LocalizationLevel.USER);
        XMLConfiguration xmlConfig = config.accessConfiguration();
        Iterator<?> keys = xmlConfig.getKeys(key);
        if (keys.hasNext()) {
            config.markDirty();
        }
        while (keys.hasNext()) {
            xmlConfig.clearProperty((String) keys.next());
        }
    }

    /**
     * Clear all values from all levels of the preference store.
     * 
     * USE THIS METHOD WITH CAUTION. THIS IS PRIMARILY INTENDED TO AIDE TESTING.
     */
    public void clear() {
        for (LocalizationConfiguration config : getSearchHierarchy()) {
            XMLConfiguration xmlConfig = config.accessConfiguration();
            xmlConfig.clear();
            config.markDirty();
        }
    }

}
