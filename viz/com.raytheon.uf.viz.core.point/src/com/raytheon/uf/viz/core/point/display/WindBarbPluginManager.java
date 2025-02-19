package com.raytheon.uf.viz.core.point.display;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Manages the configuration file for WindBarbPlugin.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 14, 2018  57905    edebebe   Initial creation
 * Mar 14, 2019  7713     tjensen   Force default config to be reread to get
 *                                  updates
 * Oct 22, 2019  7950     tjensen   Only initialize jaxbManager if not yet
 *                                  initialized
 *
 * </pre>
 *
 * @author edebebe
 */
public class WindBarbPluginManager {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    /**
     * The localization path of the default GlobalWindBarbConfig.xml file.
     */
    public static final String DEFAULT_CONFIG_PATH = "windBarb"
            + IPathManager.SEPARATOR + "GlobalWindBarbConfig.xml";

    private SingleTypeJAXBManager<WindBarbPluginConfig> jaxbManager;

    public WindBarbPluginManager() {
    }

    /**
     * Read the values from a config file
     *
     * @return
     */
    private WindBarbPluginConfig readConfig(ILocalizationFile configFile) {

        WindBarbPluginConfig config = null;
        try (InputStream is = configFile.openInputStream()) {
            SingleTypeJAXBManager<WindBarbPluginConfig> jaxb = getJaxb();
            config = jaxb.unmarshalFromInputStream(is);
        } catch (IOException | LocalizationException | SerializationException
                | JAXBException e) {
            statusHandler.error("Error in reading wind barb config file: "
                    + configFile.getPath(), e);
        }

        return config;
    }

    private SingleTypeJAXBManager<WindBarbPluginConfig> getJaxb()
            throws JAXBException {
        if (jaxbManager == null) {
            jaxbManager = new SingleTypeJAXBManager<>(
                    WindBarbPluginConfig.class);
        }

        return jaxbManager;

    }

    /**
     * Gets the Plugin specific WindBarbPlugin config object
     *
     * @param pluginName
     *            name of Plugin.
     * @param className
     *            name of Class within the specified Plugin.
     *
     * @return the default config object
     */
    public WindBarbPlugin getWindBarbPlugin(String pluginName,
            String className) {

        String pluginConfigPath = getPluginConfigPath(pluginName);

        ILocalizationFile pluginConfigFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(pluginConfigPath);

        // Read the config file for a specific Plugin
        WindBarbPluginConfig pluginConfig = null;
        if (pluginConfigFile != null) {
            pluginConfig = readConfig(pluginConfigFile);
        }

        // Get the WindBarbPlugin for the specific className
        WindBarbPlugin windBarbPluginFound = null;
        if (pluginConfig != null) {
            List<WindBarbPlugin> windBarbPluginList = pluginConfig
                    .getWindBarbPluginList();

            for (WindBarbPlugin windBarbPlugin : windBarbPluginList) {

                if (className.equals(windBarbPlugin.getClassName())) {

                    windBarbPluginFound = windBarbPlugin;
                    break;
                }
            }
        }

        // Return WindBarbPlugin object with all config values
        return windBarbPluginFound;
    }

    /**
     * Gets the default WindBarbPlugin config object
     *
     * @return the default config object
     */
    public WindBarbPlugin getDefaultWindBarbPlugin() {
        WindBarbPlugin windBarbPlugin = null;

        // Get WindBarbPlugin with default values
        ILocalizationFile defaultConfigFile = PathManagerFactory
                .getPathManager().getStaticLocalizationFile(
                        WindBarbPluginManager.DEFAULT_CONFIG_PATH);

        WindBarbPluginConfig defaultConfig = null;

        // Check if the default config file exists
        if (defaultConfigFile != null) {
            defaultConfig = readConfig(defaultConfigFile);
        }

        if (defaultConfig != null) {
            List<WindBarbPlugin> windBarbPluginList = defaultConfig
                    .getWindBarbPluginList();

            // Get first and only entry from default config file
            windBarbPlugin = windBarbPluginList.get(0);
        }
        return windBarbPlugin;
    }

    /**
     * Checks if a Plugin Specific Wind Barb config file exists
     *
     * @param pluginName
     *            name of Plugin.
     * @return a boolean value indicating whether or not the config file exists.
     */
    public static boolean pluginConfigFileExists(String pluginName) {

        String pluginConfigPath = getPluginConfigPath(pluginName);

        // Check if a Wind Barb config file exists for a specific Plugin
        ILocalizationFile pluginConfigFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(pluginConfigPath);

        return (pluginConfigFile != null);
    }

    /**
     * Returns the file path to a Plugin specific Wind Barb Configuration File
     *
     * @param pluginName
     *            name of Plugin.
     * @return a String representation of the config file path.
     */
    public static String getPluginConfigPath(String pluginName) {

        String pluginConfigPath = "windBarb" + IPathManager.SEPARATOR
                + pluginName + "WindBarbConfig.xml";

        return pluginConfigPath;
    }
}
