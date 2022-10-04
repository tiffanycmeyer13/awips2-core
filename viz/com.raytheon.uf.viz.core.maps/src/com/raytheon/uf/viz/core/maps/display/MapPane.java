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

import org.eclipse.swt.widgets.Composite;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.InputManager;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.panes.AbstractComboPane;
import com.raytheon.viz.ui.panes.VizDisplayPane;

/**
 *
 * Represents a single map pane. Map panes only have the one main map canvas, so
 * this mostly just wraps that canvas and delegates to it.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Abstract out a lot of functionality
 * Sep 08, 2022 8792       mapeters    Updated registerHandlers() signature,
 *                                     extend new class AbstractComboPane
 *
 * </pre>
 *
 * @author mapeters
 */
public class MapPane extends AbstractComboPane {

    /**
     * Constructor.
     *
     * @param paneContainer
     *            editor containing this pane
     * @param composite
     *            SWT composite for this pane's area
     * @param renderableDisplay
     *            display to initially render on this pane
     * @throws VizException
     */
    public MapPane(IDisplayPaneContainer paneContainer, Composite composite,
            IRenderableDisplay renderableDisplay) throws VizException {
        super(composite);
        IDisplayPane canvas = new VizDisplayPane(paneContainer, composite,
                CanvasType.MAIN, renderableDisplay);
        addCanvas(canvas);
    }

    @Override
    public void registerHandlers(InputManager inputManager) {
        getMainCanvas().addListener(inputManager);
    }
}
