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
package com.raytheon.viz.ui.panes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.coverage.grid.GridEnvelope;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener.DisplayChangeType;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;
import com.raytheon.viz.ui.editor.ISelectedPanesChangedListener;
import com.raytheon.viz.ui.input.InputAdapter;
import com.raytheon.viz.ui.input.InputManager;

/**
 * Abstract class for managing display panes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation (abstracted from PaneManager)
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractPaneManager extends InputAdapter
        implements IMultiPaneEditor {

    protected final InputManager inputManager;

    protected IDisplayPaneContainer paneContainer;

    protected Composite composite;

    protected final Set<ISelectedPanesChangedListener> listeners = new HashSet<>();

    protected AbstractPaneManager() {
        inputManager = new InputManager(this);

        // Add us as input handler, virtual cursor
        inputManager.registerMouseHandler(this, InputPriority.PART);
    }

    /**
     * Set focus on the managed panes. What exactly gets focus is up to the
     * implementation.
     */
    public abstract void setFocus();

    /**
     * Initialize components for the panes' area.
     *
     * @param container
     *            the pane container/editor that we are managing panes for
     * @param comp
     *            the SWT composite for all panes' area
     */
    public abstract void initializeComponents(IDisplayPaneContainer container,
            Composite comp);

    /**
     * @return the overall input/mouse manager
     */
    public InputManager getMouseManager() {
        return inputManager;
    }

    @Override
    public void registerMouseHandler(IInputHandler handler,
            InputPriority priority) {
        inputManager.registerMouseHandler(handler, priority);
    }

    /**
     * Register a handler for input events in the managed panes.
     *
     * @param handler
     *            the handler to register
     */
    @Override
    public void registerMouseHandler(IInputHandler handler) {
        inputManager.registerMouseHandler(handler);
    }

    /**
     * Unregister a handler for input events in the managed panes.
     *
     * @param handler
     *            the handler to unregister
     */
    @Override
    public void unregisterMouseHandler(IInputHandler handler) {
        inputManager.unregisterMouseHandler(handler);
    }

    @Override
    public LoopProperties getLoopProperties() {
        return paneContainer.getLoopProperties();
    }

    @Override
    public void setLoopProperties(LoopProperties loopProperties) {
        paneContainer.setLoopProperties(loopProperties);
    }

    /**
     * Take a screenshot of all managed panes that are visible.
     *
     * @return the screenshot
     */
    public BufferedImage screenshot() {
        if (composite == null || composite.isDisposed()) {
            return null;
        }
        int[] numRowsColumns = getNumRowsColumns();
        int numRows = numRowsColumns[0];
        int numColumns = numRowsColumns[1];

        BufferedImage[] screens = screenshots();
        BufferedImage retval = new BufferedImage(
                screens[0].getWidth() * numColumns,
                screens[0].getHeight() * numRows, screens[0].getType());

        int column = 0;
        int row = 0;
        int shotHeight = screens[0].getHeight();
        int shotWidth = screens[0].getWidth();

        for (BufferedImage currentPane : screens) {
            retval.createGraphics().drawImage(currentPane, shotWidth * column,
                    shotHeight * row, null);
            ++column;
            if (column == numColumns) {
                column = 0;
                ++row;
            }
        }
        return retval;
    }

    /**
     * Take a screenshot of each managed pane that is visible.
     *
     * @return the screenshots, one per visible pane
     */
    private BufferedImage[] screenshots() {
        IDisplayPane[] panes = getDisplayPanes();
        List<BufferedImage> images = new ArrayList<>();
        for (IDisplayPane pane : panes) {
            if (pane.isVisible()) {
                images.add(pane.getTarget().screenshot());
            }
        }
        return images.toArray(new BufferedImage[0]);
    }

    /**
     * Translate a current (x,y) screen coordinate to world coordinates.
     *
     * The container using this manager should not call this method as it will
     * become recursive
     *
     * @param x
     *            a visible x screen coordinate
     * @param y
     *            a visible y screen coordinate
     * @return the lat lon value of the coordinate
     */
    @Override
    public Coordinate translateClick(double x, double y) {
        IDisplayPane pane = getActiveDisplayPane();
        // Convert the screen coordinates to grid space
        double[] world = pane.screenToGrid(x, y, 0);
        GridEnvelope ge = pane.getDescriptor().getGridGeometry().getGridRange();
        IExtent extent = new PixelExtent(ge);
        // Verify grid space is within the extent, otherwiser return null
        if (world == null || !extent.contains(world)) {
            return null;
        }
        // use descriptor to convert pixel world to CRS world space
        world = pane.getDescriptor().pixelToWorld(world);
        // Check for null
        if (world == null) {
            return null;
        }
        return new Coordinate(world[0], world[1], world[2]);
    }

    /**
     * Translate a world coordinate to screen coordinates (x,y).
     *
     * The container using this manager should not call this method as it will
     * become recursive
     *
     * @param c
     *            Coordinate to convert
     * @return the world coordinates for the display
     */
    @Override
    public double[] translateInverseClick(Coordinate c) {
        if (c == null) {
            return null;
        }
        IDisplayPane pane = getActiveDisplayPane();
        double[] grid = pane.getDescriptor()
                .worldToPixel(new double[] { c.x, c.y, c.z });
        if (grid == null) {
            return null;
        }
        return pane.gridToScreen(grid);
    }

    @Override
    public void addSelectedPaneChangedListener(
            ISelectedPanesChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSelectedPaneChangedListener(
            ISelectedPanesChangedListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addRenderableDisplayChangedListener(
            IRenderableDisplayChangedListener displayChangedListener) {
        // no-op
    }

    @Override
    public void notifyRenderableDisplayChangedListeners(IDisplayPane pane,
            IRenderableDisplay display, DisplayChangeType type) {
        // no-op
    }

    @Override
    public void removeRenderableDisplayChangedListener(
            IRenderableDisplayChangedListener displayChangedListener) {
        // no-op
    }

    /**
     * Get the number of rows and columns to organize the managed panes in.
     *
     * @return int array consisting of { numRows, numColumns }
     */
    protected int[] getNumRowsColumns() {
        int paneCount = displayedPaneCount();
        int numColums = (int) Math.ceil(Math.sqrt(paneCount));
        int numRows = (int) Math.ceil(paneCount / (double) numColums);
        return new int[] { numRows, numColums };
    }
}
