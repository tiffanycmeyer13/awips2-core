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
package com.raytheon.uf.viz.core.maps.display;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.IPaneCreator;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.maps.scales.MapScalesManager;

/**
 * {@link IPaneCreator} implementation for creating map panes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 23, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Added getDefaultBackgroundDisplay,
 *                                     removed getResourceType
 *
 * </pre>
 *
 * @author mapeters
 */
public class MapPaneCreator implements IPaneCreator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MapPaneCreator.class);

    @Override
    public IPane createPane(IDisplayPaneContainer paneContainer, Composite comp,
            IRenderableDisplay display, List<IPane> panes) throws VizException {
        return new MapPane(paneContainer, comp, display);
    }

    @Override
    public IRenderableDisplay getDefaultBackgroundDisplay(
            IRenderableDisplay display) {
        /*
         * Map data displays don't include the background maps here, so need to
         * get those separately.
         */
        IRenderableDisplay bgDisplay = null;
        try {
            bgDisplay = MapScalesManager.getInstance().findEditorScale()
                    .getScaleBundle().getDisplays()[0];
        } catch (Exception e) {
            statusHandler.error("Error loading default background map display",
                    e);
        }

        if (bgDisplay == null) {
            bgDisplay = MapScalesManager.getInstance()
                    .getLastResortScaleDisplay();
        }
        return bgDisplay;
    }
}
