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
import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IScalableRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
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
 * Sep 08, 2022 8792       mapeters    Moved default map scale determination to
 *                                     MapScalesManager
 * Nov 03, 2022 8958       mapeters    Try to use the display's scale and the
 *                                     global property scale before the default
 *
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
         * get those separately. Try to use the display's scale first, then the
         * global property scale, then the default scale.
         */
        String scale = null;
        if (display instanceof IScalableRenderableDisplay) {
            scale = ((IScalableRenderableDisplay) display).getScale();
        }

        if (scale == null) {
            scale = (String) VizGlobalsManager.getCurrentInstance()
                    .getProperty(VizConstants.MAP_SCALE_ID);
        }

        if (scale != null) {
            try {
                return MapScalesManager.getInstance().getScaleByName(scale)
                        .getScaleBundle().getDisplays()[0];
            } catch (Exception e) {
                statusHandler.error("Error loading map scale display: " + scale,
                        e);
            }
        }

        return MapScalesManager.getInstance().getDefaultScaleDisplay();
    }
}
