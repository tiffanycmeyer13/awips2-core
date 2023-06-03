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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.util.EditorConstants;

/**
 * Contains information about all available descriptors
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 19, 2007            njensen     Initial creation
 * Apr 01, 2022 8790       mapeters    Added pane creators
 * Apr 22, 2022 8791       mapeters    Register pane creators through eclipse
 *                                     extensions, add convenience methods
 * Oct 21, 2022 8956       mapeters    Update getEditorId() to take multiple displays
 *                                     to determine if a Combo editor is needed
 *
 * </pre>
 *
 * @author njensen
 */
public class DescriptorMap {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DescriptorMap.class);

    private static final Map<String, DescriptorInfo> descInfoMap;
    static {
        Map<String, DescriptorInfo> tempDescInfoMap = new HashMap<>();

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
                    IPaneCreator paneCreator = null;
                    if (element.getAttribute("paneCreator") != null) {
                        try {
                            paneCreator = (IPaneCreator) element
                                    .createExecutableExtension("paneCreator");
                        } catch (CoreException e) {
                            statusHandler.error(
                                    "Error registering pane creator for "
                                            + descClass
                                            + "; it won't support loading to a Combo Editor.",
                                    e);
                        }
                    }

                    if (descClass == null) {
                        // Not constructable
                        continue;
                    }

                    DescriptorInfo descInfo = new DescriptorInfo(descEditor,
                            paneCreator);
                    tempDescInfoMap.put(descClass, descInfo);
                }
            }
        }
        descInfoMap = Collections.unmodifiableMap(tempDescInfoMap);
    }

    /**
     * Private constructor to prevent instantiation since everything is static.
     */
    private DescriptorMap() {
    }

    /**
     * Get the default editor ID to load the given descriptor type to.
     *
     * @param descClass
     *            the descriptor type
     * @return the default editor ID (may be null)
     */
    public static String getEditorId(String descClass) {
        if (descClass == null) {
            return null;
        }
        DescriptorInfo descInfo = descInfoMap.get(descClass);
        return descInfo == null ? null : descInfo.editorId;
    }

    /**
     * Get the default editor ID to load the given displays to.
     *
     * @param displays
     *            the renderable displays to load
     * @return the default editor ID (may be null if no editor supports all the
     *         displays)
     */
    public static String getEditorId(IRenderableDisplay... displays) {
        String editorId = null;
        for (IRenderableDisplay display : displays) {
            String displayEditorId = getEditorId(getDescClassName(display));
            if (displayEditorId == null) {
                // Editor has to be able to load all given displays
                return null;
            }

            if (editorId == null) {
                editorId = displayEditorId;
            } else if (!editorId.equals(displayEditorId)) {
                /*
                 * Different displays require different type-specific editors,
                 * must use combo editor if possible
                 */
                editorId = EditorConstants.COMBO_EDITOR_ID;
            }
        }

        if (EditorConstants.COMBO_EDITOR_ID.equals(editorId)) {
            // Make sure combo editor actually supports all the displays
            for (IRenderableDisplay display : displays) {
                if (getPaneCreator(display) == null) {
                    return null;
                }
            }
        }

        return editorId;
    }

    /**
     * Get an object for creating combo editor panes that can display
     * descriptors of the given type.
     *
     * @param descClass
     *            the descriptor type
     * @return the combo editor pane creator (may be null)
     */
    public static IPaneCreator getPaneCreator(String descClass) {
        if (descClass == null) {
            return null;
        }
        DescriptorInfo descInfo = descInfoMap.get(descClass);
        return descInfo == null ? null : descInfo.paneCreator;
    }

    /**
     * Get an object for creating combo editor panes that can display the given
     * renderable display.
     *
     * @param display
     *            the renderable display
     * @return the combo editor pane creator (may be null)
     */
    public static IPaneCreator getPaneCreator(IRenderableDisplay display) {
        return getPaneCreator(getDescClassName(display));
    }

    private static String getDescClassName(IRenderableDisplay display) {
        if (display == null || display.getDescriptor() == null) {
            return null;
        }
        return display.getDescriptor().getClass().getName();
    }

    private static class DescriptorInfo {

        private final String editorId;

        private final IPaneCreator paneCreator;

        private DescriptorInfo(String editorId, IPaneCreator paneCreator) {
            this.editorId = editorId;
            this.paneCreator = paneCreator;
        }
    }
}
