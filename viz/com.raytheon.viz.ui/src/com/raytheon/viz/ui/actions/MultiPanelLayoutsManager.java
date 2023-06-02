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
package com.raytheon.viz.ui.actions;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Manages the editor panel layouts to support.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 30, 2023 2029803    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class MultiPanelLayoutsManager {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MultiPanelLayoutsManager.class);

    private static final String LOC_PATH = LocalizationUtil.join("panelLayouts",
            "panelLayouts.xml");

    private static final Object LOCK = new Object();

    private static MultiPanelLayouts instance;

    private static ILocalizationPathObserver observer;

    /**
     * Get an unmodifiable list of the editor panel layouts to support.
     *
     * @return the editor panel layouts
     */
    public static List<MultiPanelLayout> getLayouts() {
        synchronized (LOCK) {
            if (instance == null) {
                IPathManager pm = PathManagerFactory.getPathManager();

                // Add localization path observer the first time this is called
                if (observer == null) {
                    observer = file -> {
                        synchronized (LOCK) {
                            instance = null;
                        }
                    };
                    pm.addLocalizationPathObserver(LOC_PATH, observer);
                }

                // Load layouts from localization
                ILocalizationFile lf = pm.getStaticLocalizationFile(LOC_PATH);
                try (InputStream is = lf.openInputStream()) {
                    instance = new SingleTypeJAXBManager<>(
                            MultiPanelLayouts.class)
                                    .unmarshalFromInputStream(is);
                } catch (Exception e) {
                    statusHandler.error(
                            "Error loading multi-panel layouts configuration: "
                                    + lf,
                            e);
                }
            }

            if (instance == null || instance.getLayouts() == null) {
                return List.of();
            }
            return Collections.unmodifiableList(instance.getLayouts());
        }
    }
}
