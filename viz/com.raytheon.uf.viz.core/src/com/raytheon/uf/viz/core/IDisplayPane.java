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

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;

import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;

/**
 *
 * Interface representing a single canvas for displaying data on. Examples
 * include the map canvas within a map pane, the graph canvas within a cross
 * section pane, and the inset map canvas within a cross section pane.
 *
 * TODO rename to IDisplayCanvas
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                                     Initial creation
 * Mar 23, 2022 8790       mapeters    Add addListener(Listener)
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IDisplayPane {

    /**
     * Attach a listener for the specified event
     *
     * @param eventType
     * @param listener
     */
    void addListener(int eventType, Listener listener);

    /**
     * Attach the given listener to various common event types.
     *
     * @param listener
     *            the listener to attach
     */
    void addListener(Listener listener);

    /**
     * Get the IGraphicsTarget for the pane
     *
     * @return the IGraphicsTarget for the pane
     */
    IGraphicsTarget getTarget();

    /**
     * Dispose the pane and all of it's resources
     */
    void dispose();

    /**
     * Move the extent (for panning support)
     *
     * @param startScreen
     *            starting point in screen coordinates
     * @param endScreen
     *            ending point in screen coordinates
     */
    void shiftExtent(double[] startScreen, double[] endScreen);

    /**
     * Set the renderable display object
     *
     * @param renderableRsc
     *            the renderable object
     */
    void setRenderableDisplay(IRenderableDisplay renderableRsc);

    /**
     * Get the renderable object
     *
     * @return the renderable object
     */
    IRenderableDisplay getRenderableDisplay();

    /**
     * Returns the descriptor
     *
     * This is a convenience method that essentially wraps
     * getRenderableDisplay() and performs a cast. If the current displayable
     * object is not a descriptor, this call will return null.
     *
     * @return the descriptor
     */
    IDescriptor getDescriptor();

    /**
     * Perform a refresh asynchronously
     *
     */
    void refresh();

    /**
     * Get bounds of pane
     *
     * @return the size of the drawing pane in pixel space
     */
    Rectangle getBounds();

    /**
     * Set focus on pane
     *
     */
    void setFocus();

    /**
     * Zoom uniformly with mouse bias
     *
     * @param value
     *            zoom value
     * @param mouseX
     *            x mouse bias
     * @param mouseY
     *            y mouse bias
     */
    void zoom(final int value, int mouseX, int mouseY);

    /**
     * Zoom uniformly
     *
     * @param value
     *            zoom value
     */
    void zoom(final int value);

    /**
     * Return the level of zoom of the map
     *
     * @return the zoom level
     */
    double getZoomLevel();

    /**
     * Set the zoom level
     *
     * @param zoomLevel
     */
    void setZoomLevel(double zoomLevel);

    /**
     * @return the lastMouseX
     */
    int getLastMouseX();

    /**
     * @return the lastMouseY
     */
    int getLastMouseY();

    /**
     * @return the X coordinate of the last click
     */
    int getLastClickX();

    /**
     * @return the Y coordinate of the last click
     */
    int getLastClickY();

    /**
     * Return the display associated with the display pane
     *
     * @return the display
     */
    Display getDisplay();

    /**
     * Convert screen space to grid space
     *
     * @param x
     *            screen coordinate
     * @param y
     *            screen coordinate
     * @param depth
     *            range [0.0 - 1.0] where 0.0 is the near plane and 1.0 is the
     *            far plane
     * @return value in grid space
     */
    double[] screenToGrid(double x, double y, double depth);

    /**
     * Convert grid space to screen space
     *
     * @param grid
     * @return screen coordinate
     */
    double[] gridToScreen(double[] grid);

    /**
     * Scale this DisplayPane's view to the display bounds.
     */
    void scaleToClientArea();

    /**
     * Clear the display pane
     */
    void clear();

    /**
     * Set the visibility of the pane
     *
     * @param visible
     */
    void setVisible(boolean visible);

    /**
     * Get the visibility of the pane
     *
     * @return
     */
    boolean isVisible();
}