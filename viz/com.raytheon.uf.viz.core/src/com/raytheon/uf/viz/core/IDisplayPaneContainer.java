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

import java.util.List;

import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.viz.core.IPane.CanvasType;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener.DisplayChangeType;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler.InputPriority;

/**
 * IDisplayPaneContainer
 *
 * Describes a view, etc that contains one or more displays
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Jul 30, 2007             chammack    Initial Creation.
 * Sep 08, 2022 8792        mapeters    Added getCanvasesCompatibleWithActive,
 *                                      getPanes, getActivePane
 * Oct 12, 2022 8946        mapeters    Added getCanvases, getMainCanvases
 *
 * </pre>
 *
 * @author chammack
 */
public interface IDisplayPaneContainer {

    /**
     * Get the contained display canvases that match the general active type
     * (e.g. main canvas or inset)
     *
     * @return the display canvases
     */
    IDisplayPane[] getDisplayPanes();

    /**
     * Get the contained display canvases that are compatible with the active
     * canvas (including the active canvas). This differentiates between general
     * canvas types (e.g. main vs. inset) as well as specific display types
     * (e.g. map vs. cross section)
     *
     * @return the compatible canvases
     */
    List<IDisplayPane> getCanvasesCompatibleWithActive();

    /**
     * Get the contained display canvases of the given type.
     *
     * @param type
     *            the canvas type (e.g. main or inset)
     * @return the canvases
     */
    IDisplayPane[] getCanvases(CanvasType type);

    /**
     * Get the main canvases of the contained panes.
     *
     * @return the main canvases
     */
    default IDisplayPane[] getMainCanvases() {
        return getCanvases(CanvasType.MAIN);
    }

    /**
     * @return the contained panes
     */
    List<IPane> getPanes();

    /**
     * Get the contained pane that is currently active.
     *
     * @return the active pane
     */
    IPane getActivePane();

    /**
     * @return the loopProperties
     */
    LoopProperties getLoopProperties();

    /**
     * @param loopProperties
     *            the loopProperties to set
     */
    void setLoopProperties(LoopProperties loopProperties);

    /**
     * @return the active display pane
     */
    IDisplayPane getActiveDisplayPane();

    /**
     * Refresh all panes
     */
    void refresh();

    /**
     * Translate a click in screen space into "world" coordinates
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @return the corresponding world coordinate
     */
    Coordinate translateClick(double x, double y);

    /**
     * Translate a world screen coordinate to screen (x,y) coordinates.
     *
     *
     * @param c
     *            world coordinate of the click
     * @return the visible screen pixel value
     */
    double[] translateInverseClick(Coordinate c);

    /**
     * Add a renderable display change listener
     *
     * @param displayChangedListener
     */
    void addRenderableDisplayChangedListener(
            IRenderableDisplayChangedListener displayChangedListener);

    /**
     * Remove a renderable display change listener
     *
     * @param displayChangedListener
     */
    void removeRenderableDisplayChangedListener(
            IRenderableDisplayChangedListener displayChangedListener);

    /**
     * Notify the renderable display listeners that the display has changed
     *
     * @param pane
     * @param display
     */
    void notifyRenderableDisplayChangedListeners(IDisplayPane pane,
            IRenderableDisplay display, DisplayChangeType type);

    /**
     * Register a input handler on the container at the given priority
     *
     * @param handler
     * @param priority
     */
    void registerMouseHandler(IInputHandler handler, InputPriority priority);

    /**
     * Register a input handler on the container at the default priority
     *
     * @param handler
     **/
    void registerMouseHandler(IInputHandler handler);

    /**
     * Unregister the input handler
     *
     * @param handler
     */
    void unregisterMouseHandler(IInputHandler handler);
}
