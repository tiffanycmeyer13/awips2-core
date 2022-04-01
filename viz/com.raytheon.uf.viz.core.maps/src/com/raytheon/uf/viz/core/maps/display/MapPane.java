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
import org.eclipse.swt.widgets.Listener;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.viz.ui.panes.AbstractPane;

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
 *
 * </pre>
 *
 * @author mapeters
 */
public class MapPane extends AbstractPane {

    private IDisplayPane canvas;

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
        this.canvas = new MapCanvas(paneContainer, composite,
                renderableDisplay);
    }

    @Override
    public boolean isVisible() {
        return canvas.isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        canvas.setVisible(visible);
    }

    @Override
    public boolean containsCanvas(IDisplayPane canvas) {
        return this.canvas == canvas;
    }

    @Override
    public CanvasType getActiveCanvasType() {
        return CanvasType.MAIN;
    }

    @Override
    public IDisplayPane getCanvas(CanvasType type) {
        if (type == CanvasType.MAIN) {
            return canvas;
        }
        return null;
    }

    @Override
    public void refresh() {
        canvas.refresh();
    }

    @Override
    public void setFocus() {
        canvas.setFocus();
    }

    @Override
    public void clear() {
        canvas.clear();
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.PLAN_VIEW;
    }

    @Override
    public void dispose() {
        canvas.dispose();
    }

    @Override
    public void registerHandlers(Listener listener) {
        canvas.addListener(listener);
    }
}
