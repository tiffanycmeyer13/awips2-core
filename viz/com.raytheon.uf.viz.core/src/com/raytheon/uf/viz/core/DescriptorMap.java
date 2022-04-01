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
package com.raytheon.uf.viz.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * Contains information about all available descriptors
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 19, 2007            njensen     Initial creation
 * Apr 01, 2022 8790       mapeters    Added pane creators
 *
 * </pre>
 *
 * @author njensen
 */
public class DescriptorMap {

    private static Map<String, String> descToEditorMap;

    private static final Map<String, IPaneCreator> descToPaneCreatorMap = new HashMap<>();

    static {
        // TODO register these via spring as well so everything's in one spot
        descToEditorMap = new HashMap<>();

        // Construct the resource mapping from Eclipse plugins
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry
                .getExtensionPoint("com.raytheon.uf.viz.core.descriptor");
        if (point != null) {
            IExtension[] extensions = point.getExtensions();
            for (IExtension extension : extensions) {
                IConfigurationElement[] config = extension
                        .getConfigurationElements();

                for (IConfigurationElement element : config) {
                    String descClass = element.getAttribute("class");
                    String descEditor = element.getAttribute("editor");

                    if (descClass == null) {
                        // Not constructable
                        continue;
                    }

                    descToEditorMap.put(descClass, descEditor);
                }
            }
        }
    }

    public static String getEditorId(String descClass) {
        return descToEditorMap.get(descClass);
    }

    public static IPaneCreator registerPaneCreator(String descClass,
            IPaneCreator creator) {
        return descToPaneCreatorMap.put(descClass, creator);
    }

    public static IPaneCreator getPaneCreator(String descClass) {
        return descToPaneCreatorMap.get(descClass);
    }
}
